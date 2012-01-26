/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and
 *        implementation and/or initial documentation
 *******************************************************************************/

/*
 * glibc_interpose.c
 *
 * This file is the main entry point for a number of interposed glibc library functions.
 * That is, by setting the LD_PRELOAD environment variable, the Linux dynamic loading system
 * loads this file (as part of the libcfs.so library) ahead of the real libc.so file.
 * Therefore, any user-level program that calls a library function (e.g. read, write, open, etc)
 * will end up executing in this file. This allows us to perform additional tracing and/or
 * manipulation of parameters and return values to implement the CFS (Component File System).
 * After performing this extra work, each function proceeds to invoke the "real" library
 * function from the glibc library.
 */

#define _GNU_SOURCE
#include <errno.h>
#include <fcntl.h>
#include <spawn.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/param.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/ipc.h>

#include "trace_buffer.h"
#include "trace_file_format.h"
#include "file_name_utils.h"
#include "interpose_utils.h"

/*======================================================================
 * Global variables
 *======================================================================*/

/*
 * Each process must have a unique process number. This is not the same
 * as a Unix process ID, since those are only 16-bit in size and may easily
 * be reused during a single build process. We instead need a non-reusable
 * 32-bit process number.
 */
int _cfs_my_process_number;

/*
 * Same, but for this process's parent. This is passed to each new process
 * via the CFS_PARENT_ID environment variable.
 */
int _cfs_my_parent_process_number;

/*
 * The shared memory segment ID, used for sharing the trace buffer between
 * multiple processes. Passed to each new process via the CFS_ID environment
 * variable.
 */
trace_buffer_id _cfs_id;

/*
 * The value of the LD_PRELOAD environment variable that caused this
 * interposer library to be loaded. We need to ensure that all child
 * processes also have this set.
 */
char *_cfs_ld_preload;

/*======================================================================
 * _cfs_init_interposer()
 *
 * When this dynamic library is first loaded, the _cfs_init_interposer function
 * is called as a constructor. This is where we allocate the shared memory
 * segment for cfs, as well as perform other start-up tasks.
 *======================================================================*/

void _cfs_init_interposer() __attribute__ ((constructor));

void _cfs_init_interposer()
{
	FETCH_REAL_FN(int, real_open, "open");

	static char argv_and_envp[NCARGS];

	/* disable debugging for now */
	_cfs_set_debug_level(0);

	/*
	 * If the CFS_ID environment variable is defined, then this current process
	 * is running within the cfs environment. If not, just return silently.
	 * Note that the various interposed functions are able to test cfs_id != 0
	 * to see if this initialization worked, or not.
	 */
	_cfs_id = 0;
	char *cfs_id_string = getenv("CFS_ID");
	if (!cfs_id_string){
		return;
	}

	/*
	 * Enable debugging, if desired. An non-numeric value in the CFS_DEBUG environment
	 * variable will be dealt with by disabling debugging. Debug levels that are too
	 * high will be reduce to level 2. Also, if CFS_LOG_FILE is set, use that as the
	 * debug log file.
	 */
	char *cfs_debug_str = getenv("CFS_DEBUG");
	if (cfs_debug_str != NULL) {
		_cfs_set_debug_level(atoi(cfs_debug_str));
	}
	char *cfs_log_file = getenv("CFS_LOG_FILE");
	if (cfs_log_file != NULL) {
		_cfs_set_log_file(cfs_log_file);
	}

	/* ensure that we have an up-to-date copy of the process's working directory */
	_cfs_get_cwd(FALSE);

	/* determine the absolute path of the executable that is currently running */
	int abs_path_size = readlink("/proc/self/exe", argv_and_envp, NCARGS);
	if (abs_path_size == -1) {
		_cfs_debug(0, "Error: cfs couldn't determine absolute path to running executable.\n");
		exit(1);
	}

	/* Grab a copy of the command line arguments and environment (argv/envp) */
	int fd = real_open("/proc/self/cmdline", O_RDONLY);
	int argv_size = read(fd, &argv_and_envp[abs_path_size], NCARGS - abs_path_size);
	if (argv_size == -1) {
		_cfs_debug(0, "Error: cfs couldn't determine command line arguments.\n");
		exit(1);
	}
	close(fd);

	/*
	 * The /proc/self/cmdline string doesn't always end with a \0, so add one if
	 * necessary.
	 */
	if (argv_and_envp[abs_path_size + argv_size - 1] != '\0'){
		argv_and_envp[abs_path_size + argv_size] = '\0';
		argv_size++;
	}

	/* determine how many arguments were passed */
	int argv_count = 0;
	int i;
	for (i = 0; i != argv_size; i++){
		if (argv_and_envp[abs_path_size + i] == '\0') {
			argv_count++;
		}
	}

	/*
	 * Now that we know the absolute path to the currently running executable,
	 * and we know the command's arguments (including argv[0]), we need to
	 * merge them together. The goal is to discard argv[0] (the command name)
	 * since it's probably a relative path, rather than an absolute path.
	 *
	 * We do this by finding the NUL-byte at the end of argv[0], and moving
	 * everything after that earlier in the array. This essentially does
	 * a "shift argv" operation.
	 */
	int first_nul_index = strlen(argv_and_envp); /* 0-byte after argv[0] */
	int count = argv_size - (first_nul_index - abs_path_size);
	char *src_ptr = &argv_and_envp[first_nul_index];
	char *dst_ptr = &argv_and_envp[abs_path_size];
	while (count-- != 0){
		*dst_ptr++ = *src_ptr++;
	}
	argv_size = dst_ptr - argv_and_envp;

	/* now read the environment variables */
	fd = real_open("/proc/self/environ", O_RDONLY);
	int envp_size = read(fd, &argv_and_envp[argv_size], NCARGS - argv_size - 1);
	if (envp_size == -1) {
		_cfs_debug(0, "Error: cfs couldn't determine command environment.\n");
		exit(1);
	}
	close(fd);

	/* Terminate the environment array */
	argv_and_envp[argv_size + 1 + envp_size] = '\0';

	/* Yes, there's an existing CFS trace buffer. Attach to it */
	_cfs_id = atoi(cfs_id_string);
	if (trace_buffer_use_existing(_cfs_id) == -1){
		_cfs_debug(0, "Error: couldn't attach to cfs trace buffer\n");
		exit(1);
	}

	/*
	 * Determine our parent's process number by examining the CFS_PARENT_ID
	 * environment variable. If this isn't defined, use ID number 0.
	 */
	char *cfs_parent_id = getenv("CFS_PARENT_ID");
	if (cfs_parent_id == NULL) {
		_cfs_my_parent_process_number = 0;
	} else {
		/*
		 * Convert to an integer. Note: atoi returns 0 on an error, but that's
		 * what we want.
		 */
		_cfs_my_parent_process_number = atoi(cfs_parent_id);
	}

	/*
	 * Allocate a process number for this newly started process. This process
	 * number is added to each trace message.
	 */
	if (trace_buffer_lock() == 0){
		_cfs_my_process_number = trace_buffer_next_process_number();

		/* trace the argv and envp arrays */
		trace_buffer_write_byte(TRACE_FILE_NEW_PROGRAM);
		trace_buffer_write_int(_cfs_my_process_number);
		trace_buffer_write_int(_cfs_my_parent_process_number);
		trace_buffer_write_string(_cfs_get_cwd(TRUE));
		trace_buffer_write_int(argv_count);
		trace_buffer_write_bytes(argv_and_envp, argv_size + envp_size + 1);
		trace_buffer_unlock();
	}

	// TODO: should there be an else here?

	/*
	 * Save our LD_PRELOAD environment variable. We need to ensure that any
	 * child processes we create also have this LD_PRELOAD set, so save it
	 * away for later (using LD_PRELOAD=xxxx format, which is required to
	 * add it back into the environment later).
	 */
	char *ld_preload = getenv("LD_PRELOAD");
	if (ld_preload == NULL) {
		_cfs_debug(0, "Error: cfs can't access LD_PRELOAD environment variable.\n");
		exit(1);
	}
	_cfs_ld_preload = _cfs_malloc(strlen("LD_PRELOAD=") + strlen(ld_preload) + 1);
	sprintf(_cfs_ld_preload, "LD_PRELOAD=%s", ld_preload);
}

/*======================================================================
 * Interposer functions
 *
 * Each of these functions interposes the "real" glibc calls. Each
 * function starts by getting a handle to the real function, then
 * adds extra functionality around the basic call.
 * Notes:
 *   - The real_xxx functions must be determined inside the interposer
 *     function. It's not possible to compute them from inside the
 *     constructor function (_cfs_init_interposer), since some of the system
 *     calls will be executed before the constructor is called.
 *   - Functions are listed alphabetically, purely for convenience.
 *======================================================================*/

// TODO: ensure that errno is saved in every function where it needs to be.
/*
 * TODO: many of these interposed functions only exist since we will one
 * day want to validate the pathname argument to ensure that the program
 * is not violating component boundaries. For now, we just silently
 * pass through the arguments and return value.
 */

/*======================================================================
 * Interposed - access()
 *======================================================================*/

int access(const char *pathname, int mode)
{
	FETCH_REAL_FN(int, real_access, "access");

	_cfs_debug(1, "access(\"%s\", %d)", pathname, mode);

	return real_access(pathname, mode);
}

/*======================================================================
 * Interposed - catopen
 *
 * nl_catd catopen(const char *name, int flag)
 *
 * Unsure whether this is useful. Implement in future if needed.
 *======================================================================*/

/*======================================================================
 * Interposed - chdir()
 *======================================================================*/

int
chdir(const char *path)
{
	FETCH_REAL_FN(int, real_chdir, "chdir");

	_cfs_debug(1, "chdir(\"%s\")", path);

	/*
	 * Perform the real chdir() operation. If it fails, then we
	 * fail too.
	 */
	if (real_chdir(path) != 0){
		return -1;
	}

	/*
	 * Else, we must figure out our new current directory (which, due to
	 * ., .., and symlinks, may not be what the user called chdir() with).
	 * If the getcwd fails, we completely abort the process (we're lost without
	 * a cwd).
	 */

	/* discard the cached copy, then re-cache the new copy. Abort on error. */
	_cfs_get_cwd(FALSE);

	/* all is good, we're now in the new directory, and we've saved its path */
	return 0;
}

/*======================================================================
 * Interposed - chmod()
 *======================================================================*/

int
chmod(const char *path, mode_t mode)
{
	FETCH_REAL_FN(int, real_chmod, "chmod");

	_cfs_debug(1, "chmod(\"%s\", 0%o)", path, mode);

	int rc = real_chmod(path, mode);
	if (rc != -1) {
		/* mark the file as "modified" */
		if (_cfs_open_common(path, O_RDWR, TRUE) == -1) {
			return -1;
		}
	}
	return rc;
}

/*======================================================================
 * Interposed - chown()
 *======================================================================*/

int
chown(const char *path, uid_t owner, gid_t group)
{
	FETCH_REAL_FN(int, real_chown, "chown");

	_cfs_debug(1, "chown(\"%s\", %d, %d)", path, owner, group);

	int rc = real_chown(path, owner, group);
	if (rc != -1) {
		/* mark the file as "modified" */
		if (_cfs_open_common(path, O_RDWR, TRUE) == -1) {
			return -1;
		}
	}
	return rc;
}

/*======================================================================
 * Interposed - chroot
 *
 * chroot(const char *path)
 *
 * Not supported due to complexity, and the fact that it's hardly
 * ever used.
 *======================================================================*/

/*======================================================================
 * Interposed - clearenv
 *
 * int clearenv(void);
 *
 * Not supported. Will clear the entire environment, but when we next
 * exec() a new program, we'll make sure that LD_PRELOAD is added
 * so the next process image has the correct value.
 *======================================================================*/

/*======================================================================
 * Interposed - clone
 *
 * Not necessary - this is similar to fork(), but is typically used
 * for implementing threads. We don't care about this action, but
 * instead only care if one of the threads does an exec(). If there
 * are multiple concurrent threads, our trace file will show all
 * file-access activity as if it was a single process.
 *======================================================================*/

/*======================================================================
 * Interposed - creat
 *======================================================================*/

int
creat(const char *path, mode_t mode)
{
	FETCH_REAL_FN(int, real_creat, "creat");

	_cfs_debug(1, "creat(\"%s\", 0%o)", path, mode);

	int fd = real_creat(path, mode);

	/* on success, log the access to the trace file */
	if (fd != -1) {
		if (_cfs_open_common(path, O_WRONLY | O_CREAT | O_TRUNC, TRUE) != 0){
			return -1;
		}
	}
	return fd;
}

/*======================================================================
 * Interposed - creat64
 *======================================================================*/

int
creat64(const char *path, mode_t mode)
{
	FETCH_REAL_FN(int, real_creat64, "creat64");

	_cfs_debug(1, "creat64(\"%s\", 0%o)", path, mode);

	int fd = real_creat64(path, mode);

	/* on success, log the access to the trace file */
	if (fd != -1) {
		if (_cfs_open_common(path, O_WRONLY | O_CREAT | O_TRUNC, TRUE) != 0){
			return -1;
		}
	}
	return fd;
}

/*======================================================================
 * Interposed - dlopen
 *
 * It's not clear if this needs to be interposed. We'll deal with it
 * if a situation arises.
 *======================================================================*/

#if DISABLED

void *
dlopen(const char *file, int mode)
{
	FETCH_REAL_FN(void *, real_dlopen, "dlopen");

	_cfs_debug(1, "dlopen(\"%s\", 0%o)", file, mode);

	/*
	 * TODO: log the fact that the a new file has been read.
	 */
	return real_dlopen(file, mode);
}
#endif /* DISABLED */

/*======================================================================
 * Interposed - eaccess
 *======================================================================*/

int eaccess(const char *pathname, int mode)
{
	FETCH_REAL_FN(int, real_eaccess, "eaccess");

	_cfs_debug(1, "eaccess(\"%s\", 0%o)", pathname, mode);

	return real_eaccess(pathname, mode);
}

/*======================================================================
 * Interposed - euidaccess
 *======================================================================*/

int euidaccess(const char *pathname, int mode)
{
	FETCH_REAL_FN(int, real_euidaccess, "euidaccess");

	_cfs_debug(1, "euidaccess(\"%s\", 0%o)", pathname, mode);

	return real_euidaccess(pathname, mode);
}

/*======================================================================
 * Interposed - execl()
 *======================================================================*/

int
execl(const char *path, const char *arg0, ...)
{
	extern char **environ;

	_cfs_debug(1, "execl(\"%s\", ..., ...)", path);

	/* execl is simply a wrapper for execve()s */
	return _cfs_execve_common(path, (char * const *)&arg0, environ);
}

/*======================================================================
 * Interposed - execle()
 *======================================================================*/

int
execle(const char *path, const char *arg0, ... /*, 0, char *const envp[] */)
{
	_cfs_debug(1, "execle(\"%s\", ..., ...)", path);

	/*
	 * execle is a wrapper for execve, but we first need to identify
	 * the envp pointer. It should be immediately beyond the NULL pointer
	 * at the end of the argv array.
	 */
	char * const *p = (char * const *)&arg0;

	/* skip through the argv array */
	while (*p++ != NULL) { /* empty */ }

	/* finally, grab the environment pointer */
	char * const *envp = (char * const *)*p;

	return _cfs_execve_common(path, (char * const *)&arg0, envp);
}

/*======================================================================
 * Interposed - execlp()
 *======================================================================*/

int
execlp(const char *file, const char *arg0, ...)
{
	extern char **environ;

	_cfs_debug(1, "execlp(\"%s\", ..., ...)", file);

	/*
	 * execlp is a wrapper for execvpe().
	 */
	return _cfs_execvpe_common(file, (char * const *)&arg0, environ);
}

/*======================================================================
 * Interposed - execv()
 *======================================================================*/

int
execv(const char *path, char *const argv[])
{
	extern char **environ;
	_cfs_debug(1, "execv(\"%s\", ...)", path);

	/*
	 * execv is a wrapper for execve().
	 */
	return _cfs_execve_common(path, argv, environ);
}

/*======================================================================
 * Interposed - execve()
 *======================================================================*/

int
execve(const char *filename, char *const argv[], char *const envp[])
{
	_cfs_debug(1, "execve(\"%s\", ..., ...)", filename);

	return _cfs_execve_common(filename, argv, envp);
}

/*======================================================================
 * Interposed - execvp()
 *======================================================================*/

int
execvp(const char *file, char *const argv[])
{
	extern char **environ;
	_cfs_debug(1, "execvp(\"%s\", ...)", file);

	/*
	 * execvp is a wrapper for execvpe().
	 */
	return execvpe(file, argv, environ);
}


/*======================================================================
 * Interposed - execvpe()
 *======================================================================*/

int
execvpe(const char *file, char *const argv[], char *const envp[])
{
	_cfs_debug(1, "execvpe(\"%s\", ..., ...)", file);

	return _cfs_execvpe_common(file, argv, envp);
}

/*======================================================================
 * Interposed - exit()
 *======================================================================*/

void
exit(int status)
{
	FETCH_REAL_FN(int, real_exit, "exit");

	_cfs_debug(1, "exit(%d)", status);

	// TODO: log the fact that the process is exiting (helps with detecting
	// parallel processes.
	real_exit(status);

	 /*
	  * This line isn't executed, but it stops the compiler complaining
	  * that we return from "real_exit".
	  */
	exit(status);
}

/*======================================================================
 * Interposed - _exit()
 *======================================================================*/

void _exit(int status)
{
	FETCH_REAL_FN(int, real__exit, "_exit");

	_cfs_debug(1, "_exit(%d)", status);

	// TODO: log fact that the process is exiting (helps with detecting
	// parallel processes.
	real__exit(status);

	 /*
	  * This line isn't executed, but it stops the compiler complaining
	  * that we return from "real__exit".
	  */
	_exit(status);
}

/*======================================================================
 * Interposed - _Exit()
 *======================================================================*/

void _Exit(int status)
{
	FETCH_REAL_FN(int, real__Exit, "_Exit");

	_cfs_debug(1, "_Exit(%d)", status);

	// TODO: log fact that the process is exiting (helps with detecting
	// parallel processes.
	real__Exit(status);

	 /*
	  * This line isn't executed, but it stops the compiler complaining
	  * that we return from "_Exit".
	  */
	_Exit(status);
}

/*======================================================================
 * Interposed - faccessat()
 *======================================================================*/

int
faccessat(int dirfd, const char *pathname, int mode, int flags)
{
	FETCH_REAL_FN(int, real_faccessat, "faccessat");

	_cfs_debug(1, "faccessat(%d, \"%s\", 0%o, %d)", dirfd, pathname, mode, flags);

	return real_faccessat(dirfd, pathname, mode, flags);
}


/*======================================================================
 * Interposed - fattach()
 *
 * int fattach(int fildes, const char *path);
 *
 * Ignore for now. Seems a little obscure, but might need to think about
 * it more in the future.
 *======================================================================*/

/*======================================================================
 * Interposed - fchdir()
 *======================================================================*/

int
fchdir(int fd)
{
	FETCH_REAL_FN(int, real_fchdir, "fchdir");

	_cfs_debug(1, "fchdir(%d)", fd);

	/* attempt the real fchdir operation */
	if (real_fchdir(fd) != 0){
		return -1;
	}

	/*
	 * Now that we've successfully changed directory, cache the
	 * new current working directory.
	 */
	_cfs_get_cwd(FALSE);
	return 0;
}

/*======================================================================
 * Interposed - fchmod()
 *======================================================================*/

int
fchmod(int fd, mode_t mode)
{
	FETCH_REAL_FN(int, real_fchmod, "fchmod");

	_cfs_debug(1, "fchmod(%d, 0%o)", fd, mode);

	int rc = real_fchmod(fd, mode);
	if (rc != -1) {
		/*
		 * Mark the file as "modified" - start by figuring out the
		 * path associated with this fd. Note that a failure to
		 * get this information should result in a failure to log
		 * the information. The actual fchmod should still succeed.
		 * (not all OS's may be able to return this information).
		 */
		char path[PATH_MAX];
		if (_cfs_get_path_of_fd(fd, path) == -1) {
			return rc;
		}
		if (_cfs_open_common(path, O_RDWR, TRUE) == -1) {
			return -1;
		}
	}
	return rc;
}

/*======================================================================
 * Interposed - fchmodat()
 *======================================================================*/

int
fchmodat(int dirfd, const char *pathname, mode_t mode, int flags)
{
	FETCH_REAL_FN(int, real_fchmodat, "fchmodat");

	_cfs_debug(1, "fchmodat(%d, \"%s\", 0%o, %d)", dirfd, pathname, mode, flags);

	int rc = real_fchmodat(dirfd, pathname, mode, flags);

	/* on success, trace the pathname information */
	if (rc != -1) {
		char converted_path[PATH_MAX];

		if (_cfs_convert_pathat_to_path(dirfd, pathname, converted_path) == -1) {
			return -1;
		}
		if (_cfs_open_common(converted_path, O_RDWR, TRUE) == -1) {
			return -1;
		}
	}

	return rc;
}

/*======================================================================
 * Interposed - fchown()
 *======================================================================*/

int
fchown(int fd, uid_t owner, gid_t group)
{
	FETCH_REAL_FN(int, real_fchown, "fchown");

	_cfs_debug(1, "fchown(%d, %d, %d)", fd, owner, group);

	int rc = real_fchown(fd, owner, group);
	if (rc != -1) {
		/*
		 * Mark the file as "modified" - start by figuring out the
		 * path associated with this fd. Note that a failure to
		 * get this information should result in a failure to log
		 * the information. The actual fchmod should still succeed.
		 * (not all OS's may be able to return this information).
		 */
		char path[PATH_MAX];
		if (_cfs_get_path_of_fd(fd, path) == -1) {
			return rc;
		}
		if (_cfs_open_common(path, O_RDWR, TRUE) == -1) {
			return -1;
		}
	}
	return rc;
}

/*======================================================================
 * Interposed - fchownat()
 *======================================================================*/

int
fchownat(int dirfd, const char *pathname, uid_t owner, gid_t group, int flags)
{
	FETCH_REAL_FN(int, real_fchownat, "fchownat");

	_cfs_debug(1, "fchownat(%d, \"%s\", %d, %d, %d)",
			dirfd, pathname, owner, group, flags);

	int rc = real_fchownat(dirfd, pathname, owner, group, flags);

	/* on success, trace the pathname information */
	if (rc != -1) {
		char converted_path[PATH_MAX];

		if (_cfs_convert_pathat_to_path(dirfd, pathname, converted_path) == -1) {
			return -1;
		}
		if (_cfs_open_common(converted_path, O_RDWR, TRUE) == -1) {
			return -1;
		}
	}

	return rc;
}

/*======================================================================
 * Interposed - fdetach()
 *
 * int fdetach(const char *path);
 *
 * Ignore for now. Seems a little obscure, but might need to think about
 * it more in the future.
 *======================================================================*/

/*======================================================================
 * Interposed - fdopen()
 *
 * FILE *fdopen(int fd, const char *mode);
 * Probably don't need this, unless we need to track FILE * for some
 * reason. We already have the file access information because of the
 * open fd.
 *======================================================================*/

/*======================================================================
 * Interposed - fdopendir()
 *
 * DIR *fdopendir(int fd);
 * Probably don't need this, unless I need to track DIR * for some
 * reason. We already have the file access information because of the
 * open fd.
 *======================================================================*/

/*======================================================================
 * Interposed - fexecve()
 *======================================================================*/

int
fexecve(int fd, char *const argv[], char *const envp[])
{
	FETCH_REAL_FN(int, real_fexecve, "fexecve");

	_cfs_debug(1, "fexecve(%d, ..., ...)", fd);

	/*
	 * Before calling the real fexecve, make sure our custom environment
	 * variables are in place. Note that the child process will generate
	 * the trace information once it's exec'ed. We therefore don't need
	 * to know the name of the program being started.
	 */
	char * const *new_envp = _cfs_modify_envp(envp);
	int rc = real_fexecve(fd, argv, new_envp);

	/* if we get here, the fexecve failed */
	_cfs_cleanup_envp(envp);
	return rc;
}

/*======================================================================
 * Interposed - fgetxattr()
 *
 * ssize_t fgetxattr(int fd, const char *name, void *value, size_t size);
 *
 * Not implemented for now. Extended attribute are an advanced concept.
 *======================================================================*/

/*======================================================================
 * Interposed - flistxattr()
 *
 * ssize_t flistxattr(int fd, char *list, size_t size);
 *
 * Not implemented for now. Extended attribute are an advanced concept.
 *======================================================================*/

/*======================================================================
 * Interposed - fopen()
 *======================================================================*/

FILE *
fopen(const char *filename, const char *mode)
{
	FETCH_REAL_FN(FILE *, real_fopen, "fopen");

	_cfs_debug(1, "fopen(\"%s\", \"%s\")", filename, mode);
	FILE *f = real_fopen(filename, mode);

	/* on success, log the access to the trace file. */
	if (f != NULL) {
		if (_cfs_fopen_common(filename, mode) != 0){
			return NULL;
		}
	}
	return f;
}

/*======================================================================
 * Interposed - fopen64()
 *======================================================================*/

FILE *
fopen64(const char *filename, const char *mode)
{
	FETCH_REAL_FN(FILE *, real_fopen64, "fopen64");

	_cfs_debug(1, "fopen64(\"%s\", \"%s\")", filename, mode);
	FILE *f = real_fopen64(filename, mode);

	/* on success, log the access to the trace file. */
	if (f != NULL) {
		if (_cfs_fopen_common(filename, mode) != 0){
			return NULL;
		}
	}
	return f;
}

/*======================================================================
 * Interposed - fork()
 *======================================================================*/

pid_t
fork(void)
{
	FETCH_REAL_FN(pid_t, real_fork, "fork");

	_cfs_debug(1, "fork()");

	// TODO: register the new process as being identical to the existing
	// process. Any file accesses from the child should be considered
	// as coming from the parent.
	return real_fork();
}

/*======================================================================
 * Interposed - fpathconf()
 *
 * long fpathconf(int fd, int name);
 *
 * This function doesn't do anything interesting, from a disco
 * perspective.
 *======================================================================*/

/*======================================================================
 * Interposed - fremovexattr()
 *
 * int fremovexattr(int fd, const char *name);
 *
 * Not implemented for now. Extended attribute are an advanced concept.
 *======================================================================*/

/*======================================================================
 * Interposed - freopen()
 *======================================================================*/

FILE *
freopen(const char *path, const char *mode, FILE *stream)
{
	FETCH_REAL_FN(FILE *, real_freopen, "freopen");

	_cfs_debug(1, "freopen(\"%s\", \"%s\", %p)", path, mode, stream);

	FILE *f = real_freopen(path, mode, stream);
	if ((f != NULL) && (path != NULL)) {
		/*
		 * TODO: if path is NULL then we're actually reopening the
		 * same file with a different mode. To correctly handle
		 * this situation, we need a way of mapping a FILE * back
		 * to the path of the path of the file that was opened.
		 * This can possibly be done by accessing /proc/self/fd/<fd>,
		 * but that's not very portable.
		 */
		if (_cfs_fopen_common(path, mode) != 0){
			return NULL;
		}
	}
	return f;
}

/*======================================================================
 * Interposed - freopen64
 *======================================================================*/

FILE *
freopen64(const char *path, const char *mode, FILE *stream)
{
	FETCH_REAL_FN(FILE *, real_freopen64, "freopen64");

	_cfs_debug(1, "freopen64(\"%s\", \"%s\", %p)", path, mode, stream);

	FILE *f = real_freopen64(path, mode, stream);
	if ((f != NULL) && (path != NULL)) {
		/* TODO: see comment in freopen() function */
		if (_cfs_fopen_common(path, mode) != 0){
			return NULL;
		}
	}
	return f;
}

/*======================================================================
 * Interposed - fsetxattr()
 *
 * int fsetxattr(int fd, const char *name,
 * 				const void *value, size_t size, int flags);
 *
 * Not implemented for now. Extended attribute are an advanced concept.
 *======================================================================*/

/*======================================================================
 * Interposed - fstat()
 *
 * int fstat(int fildes, struct stat *buf);
 *
 * No need to implement. We'll already have information about the file
 * when we opened the filedes.
 *======================================================================*/

/*======================================================================
 * Interposed - fstat64()
 *
 * int fstat64(int fildes, struct stat64 *buf);
 *
 * No need to implement. We'll already have information about the file
 * when we opened the filedes.
 *======================================================================*/

/*======================================================================
 * Interposed - fstatat()
 *
 * int fstatat(int dirfd, const char *pathname, struct stat *buf,
 *                int flags);
 *
 * Probably not required. We should assume the file is only interesting
 * if it's actually opened.
 *======================================================================*/

/*======================================================================
 * Interposed - fstatat64()
 *
 * int fstatat(int dirfd, const char *pathname, struct stat64 *buf,
 *                int flags);
 *
 * Probably not required. We should assume the file is only interesting
 * if it's actually opened.
 *======================================================================*/

/*======================================================================
 * Interposed - fstatfs()
 *
 * int fstatfs(int fd, struct statfs *buf);
 *
 * Ignore for now. Too advanced to be concerned with file system
 * statistics.
 *======================================================================*/

/*======================================================================
 * Interposed - fstatfs64()
 *
 * int fstatfs(int fd, struct statfs64 *buf);
 *
 * Ignore for now. Too advanced to be concerned with file system
 * statistics.
 *======================================================================*/

/*======================================================================
 * Interposed - fstatvfs()
 *
 * int fstatvfs(int fd, struct statvfs *buf);
 *
 * Ignore for now. Too advanced to be concerned with file system
 * statistics.
 *======================================================================*/

/*======================================================================
 * Interposed - fstatvfs64()
 *
 * int fstatvfs(int fd, struct statvfs64 *buf);
 *
 * Ignore for now. Too advanced to be concerned with file system
 * statistics.
 *======================================================================*/

/*======================================================================
 * Interposed - ftok()
 *======================================================================*/

key_t
ftok(const char *pathname, int proj_id)
{
	FETCH_REAL_FN(key_t, real_ftok, "ftok");

	_cfs_debug(1, "ftok(\"%s\", %d)", pathname, proj_id);

	key_t key = real_ftok(pathname, proj_id);

	/* on success, log the access to the trace file. */
	if (key != (key_t)-1) {
		if (_cfs_open_common(pathname, O_RDONLY, TRUE) != 0){
			return -1;
		}
	}
	return key;
}

/*======================================================================
 * Interposed - fts_children()
 * Interposed - fts_close()
 * Interposed - fts_open()
 * Interposed - fts_read()
 * Interposed - fts_set()
 *
 * None of these functions are interposed, since they don't actually
 * open files. When we do an open() call, we'll capture the file access
 * at that time.
 *======================================================================*/

/*======================================================================
 * Interposed - futimesat()
 *
 * int futimesat(int dirfd, const char *pathname,
 *                    const struct timeval times[2]);
 *
 * It's not clear if timestamp changes are interesting. This would
 * probably be used by a touch command, but that's not interesting
 * from Disco's perspective.
 *======================================================================*/

/*======================================================================
 * Interposed - futimens()
 *
 * int futimens(int fd, const struct timespec times[2]);
 *
 * It's not clear if timestamp changes are interesting. This would
 * probably be used by a touch command, but that's not interesting
 * from Disco's perspective.
 *======================================================================*/

/*======================================================================
 * Interposed - ftw()
 *
 * int ftw(const char *filename, __ftw_func_t func, int descriptors)
 *
 * Probably no need to implement. If a file within the tree is opened,
 * we'll find out when the open() call is issued.
 *======================================================================*/

/*======================================================================
 * Interposed - ftw64
 *
 * int ftw64(const char *filename, __ftw64_func_t func, int descriptors)
 *
 * Probably no need to implement. If a file within the tree is opened,
 * we'll find out when the open() call is issued.
 *======================================================================*/

/*======================================================================
 * Interposed - get_current_dir_name()
 *
 * char *get_current_dir_name (void)
 *
 * Doesn't provide any useful information for Disco.
 *======================================================================*/

/*======================================================================
 * Interposed - getcwd()
 *
 * char *getcwd(char *buffer, size_t size)
 *
 * Doesn't provide any useful information for Disco.
 *======================================================================*/

/*======================================================================
 * Interposed - getdirentries()
 *
 * ssize_t getdirentries(int fd, char *buf, size_t nbytes , off_t *basep);
 *
 * Not interesting to implement. Files within the directory will be
 * tracked if they are opened.
 *======================================================================*/

/*======================================================================
 * Interposed - getdirentries64()
 *
 * ssize_t getdirentries(int fd, char *buf, size_t nbytes , off_t *basep);
 *
 * Not interesting to implement. Files within the directory will be
 * tracked if they are opened.
 *======================================================================*/

/*======================================================================
 * Interposed - getenv()
 *
 * Not supported. Could potentially be used to fetch the LD_PRELOAD,
 * CFS_ID or CFS_PARENT_ID environment variables, but we'll assume
 * there's no problem with the caller seeing these.
 *======================================================================*/

/*======================================================================
 * Interposed - getwd()
 *
 * char *getwd(char *buf);
 *
 * Doesn't provide any useful information for Disco.
 *======================================================================*/

/*======================================================================
 * Interposed - getxattr()
 *
 * ssize_t getxattr(const char *path, const char *name,
 *                      void *value, size_t size);
 *
 * Not implemented for now. Extended attribute are an advanced concept.
 *======================================================================*/

/*======================================================================
 * Interposed - glob()
 * Interposed - globfree()
 * Interposed - globfree64()
 *
 * int glob(const char *pattern, int flags,
 *            int (*errfunc) (const char *epath, int eerrno),
 *            glob_t *pglob);
 *      void globfree(glob_t *pglob);
 *
 * Not implemented. Any file names returned from this call will be
 * registered if they are opened at a later time.
 *======================================================================*/

/*======================================================================
 * Interposed - lchown()
 *======================================================================*/

int
lchown(const char *path, uid_t owner, gid_t group)
{
	FETCH_REAL_FN(int, real_lchown, "lchown");

	_cfs_debug(1, "lchown(\"%s\", %d, %d)", path, owner, group);

	/*
	 * TODO: consider how to handle this case, once symlinks are
	 * properly handled by disco.
	 */
	return real_lchown(path, owner, group);
}

/*======================================================================
 * Interposed - lgetxattr()
 *
 * ssize_t lgetxattr(const char *path, const char *name,
 *                       void *value, size_t size);
 *
 * Not implemented for now. Extended attribute are an advanced concept.
 *======================================================================*/

/*======================================================================
 * Interposed - link()
 *======================================================================*/

int
link(const char *oldname, const char *newname)
{
	FETCH_REAL_FN(int, real_link, "link");

	_cfs_debug(1, "link(\"%s\", \"%s\")", oldname, newname);

	int rc = real_link(oldname, newname);
	if (rc != -1) {
		/* mark the existing file as having being "read" */
		if (_cfs_open_common(oldname, O_RDONLY, TRUE) == -1) {
			return -1;
		}

		/* mark the new file (the link) as being "created" */
		if (_cfs_open_common(newname, O_CREAT, TRUE) == -1) {
			return -1;
		}
	}
	return rc;
}

/*======================================================================
 * Interposed - linkat()
 *======================================================================*/

int
linkat(int olddirfd, const char *oldpath,
           int newdirfd, const char *newpath, int flags)
{
	FETCH_REAL_FN(int, real_linkat, "linkat");

	_cfs_debug(1, "linkat(%d, \"%s\", %d, \"%s\", %d)", olddirfd, oldpath,
			newdirfd, newpath, flags);

	int rc = real_linkat(olddirfd, oldpath, newdirfd, newpath, flags);
	if (rc != -1) {
		char converted_path[PATH_MAX];

		/* mark the old path as being "read" */
		if (_cfs_convert_pathat_to_path(olddirfd, oldpath, converted_path) == -1) {
			return -1;
		}
		if (_cfs_open_common(converted_path, O_RDONLY, TRUE) == -1) {
			return -1;
		}

		/* mark the new path as being "created" */
		if (_cfs_convert_pathat_to_path(newdirfd, newpath, converted_path) == -1) {
			return -1;
		}
		if (_cfs_open_common(converted_path, O_CREAT, TRUE) == -1) {
			return -1;
		}
	}
	return rc;
}

/*======================================================================
 * Interposed - listxattr()
 *
 * ssize_t listxattr(const char *path, char *list, size_t size)
 *
 * Not implemented for now. Extended attribute are an advanced concept.
 *======================================================================*/

/*======================================================================
 * Interposed - llistxattr()
 *
 * ssize_t llistxattr(const char *path, char *list, size_t size);
 *
 * Not implemented for now. Extended attribute are an advanced concept.
 *======================================================================*/

/*======================================================================
 * Interposed - lremovexattr()
 *
 * int lremovexattr(const char *path, const char *name);
 *
 * Not implemented for now. Extended attribute are an advanced concept.
 *======================================================================*/

/*======================================================================
 * Interposed - lsetxattr()
 *
 * int lremovexattr(const char *path, const char *name);
 *
 * Not implemented for now. Extended attribute are an advanced concept.
 *======================================================================*/

/*======================================================================
 * Interposed - lstat()
 *
 * int lstat(const char *path, struct stat *buf)
 *
 * No need to implement. We'll get information about the file if it
 * gets opened.
 *======================================================================*/

/*======================================================================
 * Interposed - lstat64()
 *
 * int lstat64(const char *filename, struct stat64 *buf)
 *
 * No need to implement. We'll get information about the file if it
 * gets opened.
 *======================================================================*/

/*======================================================================
 * Interposed - lutimes()
 *
 * int lutimes(const char *filename, struct timeval tvp[2])
 *
 * It's not clear if timestamp changes are interesting. This would
 * probably be used by a touch command, but that's not interesting
 * from Disco's perspective.
 *======================================================================*/

/*======================================================================
 * Interposed - mkdir()
 *======================================================================*/

int
mkdir(const char *path, mode_t mode)
{
	FETCH_REAL_FN(int, real_mkdir, "mkdir");

	_cfs_debug(1, "mkdir(\"%s\", 0%o)", path, mode);

	/* call the real mkdir function */
	int rc = real_mkdir(path, mode);

	/* assuming it succeeds, log the directory creation */
	if (rc != -1) {
		if (_cfs_open_common(path, O_CREAT, TRUE) != 0){
			return -1;
		}
	}
	return rc;
}

/*======================================================================
 * Interposed - mkdirat()
 *======================================================================*/

int
mkdirat(int dirfd, const char *pathname, mode_t mode)
{
	FETCH_REAL_FN(int, real_mkdirat, "mkdirat");

	_cfs_debug(1, "mkdirat(%d, \"%s\", 0%o)", dirfd, pathname, mode);

	int rc = real_mkdirat(dirfd, pathname, mode);

	/* on success, trace the pathname information */
	if (rc != -1) {
		char converted_path[PATH_MAX];

		if (_cfs_convert_pathat_to_path(dirfd, pathname, converted_path) == -1) {
			return -1;
		}
		if (_cfs_open_common(converted_path, O_CREAT, TRUE) == -1) {
			return -1;
		}
	}
	return rc;
}

/*======================================================================
 * Interposed - mkdtemp()
 *
 * char * mkdtemp(char *template)
 *
 * Not implemented. We don't care much about directories, especially
 * temporary directories. If we open files in this directory, they'll
 * be noticed.
 *======================================================================*/

/*======================================================================
 * Interposed - mkfifo()
 * Interposed - mkfifoat()
 *
 * Not implemented. Name pipes are too advanced to care about.
 *======================================================================*/

/*======================================================================
 * Interposed - mknod()
 *
 * int mknod(const char *filename, int mode, int dev)
 *
 * Ignored. This is a very advanced feature, and we don't normally care
 * about creating devices in a build system.
 *======================================================================*/

/*======================================================================
 * Interposed - mkostemp()
 * Interposed - mkostemp64()
 * Interposed - mkostemps()
 * Interposed - mkostemps64
 * Interposed - mkstemp()
 * Interposed - mkstemp64()
 * Interposed - mkstemps()
 * Interposed - mkstemps64()
 * Interposed - mktemp()
 *
 * Not implemented. We'll ignore temporary files, since they're usually
 * not user-visible and aren't usually input or output from the build
 * system.
 *======================================================================*/

/*======================================================================
 * Interposed - nftw()
 *
 * int nftw(const char *filename, __nftw_func_t func,
 * 					int descriptors, int flag)
 *
 * Probably no need to implement. If a file within the tree is opened,
 * we'll find out when the open() call is issued.
 *======================================================================*/

/*======================================================================
 * Interposed - nftw64()
 *
 * int nftw64(const char *filename, __nftw64_func_t func,
 * 					int descriptors, int flag)
 *
 * Probably no need to implement. If a file within the tree is opened,
 * we'll find out when the open() call is issued.
 *======================================================================*/

/*======================================================================
 * Interposed - open()
 *======================================================================*/

int
open(const char *filename, int flags, ...)
{
	FETCH_REAL_FN(int, real_open, "open");

	/*
	 * Fetch the optional mode argument. If it wasn't
	 * provided, we'll get a junk value, but let's just
	 * pass that junk value onto the real library function.
	 */
	va_list ap;
	va_start(ap, flags);
	mode_t mode = va_arg(ap, mode_t);
	va_end(ap);

	_cfs_debug(1, "open(\"%s\", 0x%x, 0%o)", filename, flags, mode);

	int fd = real_open(filename, flags, mode);

	/* on success, log the access to the trace file. */
	if (fd != -1) {
		if (_cfs_open_common(filename, flags, TRUE) != 0){
			return -1;
		}
	}
	return fd;
}

/*======================================================================
 * Interposed - open64()
 *======================================================================*/

int
open64(const char *filename, int flags, ...)
{
	FETCH_REAL_FN(int, real_open64, "open64");

	/*
	 * Fetch the optional mode argument. If it wasn't
	 * provided, we'll get a junk value, but let's just
	 * pass that junk value onto the real library function.
	 */
	va_list ap;
	va_start(ap, flags);
	mode_t mode = va_arg(ap, mode_t);
	va_end(ap);

	_cfs_debug(1, "open64(\"%s\", 0x%x, 0%o)", filename, flags, mode);

	int fd = real_open64(filename, flags, mode);

	/* on success, log the access to the trace file. */
	if (fd != -1) {
		if (_cfs_open_common(filename, flags, TRUE) != 0){
			return -1;
		}
	}
	return fd;
}

/*======================================================================
 * Interposed - openat()
 *======================================================================*/

int
openat(int dirfd, const char *pathname, int flags, ...)
{
	FETCH_REAL_FN(int, real_openat, "openat");

	/*
	 * Fetch the optional mode argument. If it wasn't
	 * provided, we'll get a junk value, but let's just
	 * pass that junk value onto the real library function.
	 */
	va_list ap;
	va_start(ap, flags);
	mode_t mode = va_arg(ap, mode_t);
	va_end(ap);

	_cfs_debug(1, "openat(%d, \"%s\", 0x%x, 0%o)", dirfd, pathname, flags, mode);

	/* call the real openat function */
	int fd = real_openat(dirfd, pathname, flags, mode);

	/* on success, trace the pathname information */
	if (fd != -1) {
		char converted_path[PATH_MAX];

		/* convert to a regular path name */
		if (_cfs_convert_pathat_to_path(dirfd, pathname, converted_path) == -1) {
			return -1;
		}

		if (_cfs_open_common(converted_path, flags, TRUE) == -1) {
			return -1;
		}
	}
	return fd;
}

/*======================================================================
 * Interposed - openat64()
 *======================================================================*/

int
openat64(int dirfd, const char *pathname, int flags, ...)
{
	FETCH_REAL_FN(int, real_openat64, "openat64");

	/*
	 * Fetch the optional mode argument. If it wasn't
	 * provided, we'll get a junk value, but let's just
	 * pass that junk value onto the real library function.
	 */
	va_list ap;
	va_start(ap, flags);
	mode_t mode = va_arg(ap, mode_t);
	va_end(ap);

	_cfs_debug(1, "openat64(%d, \"%s\", 0x%x, 0%o)", dirfd, pathname, flags, mode);

	int fd = real_openat64(dirfd, pathname, flags, mode);

	/* on success, trace the pathname information */
	if (fd != -1) {
		char converted_path[PATH_MAX];

		/* convert to a regular path name */
		if (_cfs_convert_pathat_to_path(dirfd, pathname, converted_path) == -1) {
			return -1;
		}

		if (_cfs_open_common(converted_path, flags, TRUE) == -1) {
			return -1;
		}
	}
	return fd;
}

/*======================================================================
 * Interposed - opendir()
 *
 * DIR *opendir(const char *dirname)
 *
 * Not implemented. We don't record directory accesses, but if there
 * are files within that directory that are accessed later, we'll
 * log those later.
 *======================================================================*/

/*======================================================================
 * Interposed - pathconf()
 *
 * long pathconf(char *path, int name);
 *
 * This function doesn't do anything interesting, from a disco
 * perspective.
 *======================================================================*/

/*======================================================================
 * Interposed - popen()
 *======================================================================*/

FILE *
popen(const char *command, const char *mode)
{
	extern char **environ;
	char **old_environ = environ;

	FETCH_REAL_FN(FILE *, real_popen, "popen");

	_cfs_debug(1, "popen(\"%s\", \"%s\")", command, mode);

	/* make sure that CFS_ID, LD_PRELOAD and CFS_PARENT_ID are correct */
	environ = (char **)_cfs_modify_envp(environ);

	/* now invoke the original popen() call */
	FILE *result = real_popen(command, mode);

	/* restore our original environment */
	char **tmp_environ = environ;
	environ = old_environ;
	_cfs_cleanup_envp(tmp_environ);

	return result;
}

/*======================================================================
 * Interposed - posix_spawn()
 *======================================================================*/

int
posix_spawn(pid_t *pid, const char *path,
       const posix_spawn_file_actions_t *file_actions,
       const posix_spawnattr_t *attrp,
       char *const argv[], char *const envp[])
{
	FETCH_REAL_FN(int, real_posix_spawn, "posix_spawn");

	_cfs_debug(1, "posix_spawn(%p, \"%s\", %p, %p, %p, %p)",
			pid, path, file_actions, attrp, argv, envp);

	/* make sure that CFS_ID, LD_PRELOAD and CFS_PARENT_ID are correct */
	char **new_envp = (char **)_cfs_modify_envp(environ);
	int result = real_posix_spawn(pid, path, file_actions, attrp, argv, new_envp);

	/* restore our original environment */
	_cfs_cleanup_envp(new_envp);
	return result;
}

/*======================================================================
 * Interposed - posix_spawnp()
 *======================================================================*/

int
posix_spawnp(pid_t *pid, const char *file,
       const posix_spawn_file_actions_t *file_actions,
       const posix_spawnattr_t *attrp,
       char *const argv[], char * const envp[])
{
	FETCH_REAL_FN(int, real_posix_spawnp, "posix_spawnp");

	_cfs_debug(1, "posix_spawnp(%p, \"%s\", %p, %p, %p, %p)",
			pid, file, file_actions, attrp, argv, envp);

	/* make sure that CFS_ID, LD_PRELOAD and CFS_PARENT_ID are correct */
	char **new_envp = (char **)_cfs_modify_envp(environ);
	int result = real_posix_spawnp(pid, file, file_actions, attrp, argv, new_envp);

	/* restore our original environment */
	_cfs_cleanup_envp(new_envp);
	return result;
}

/*======================================================================
 * Interposed - putenv()
 *
 * int putenv(char *string);
 *
 * No need to implement this function. Although this might modify
 * our LD_PRELOAD, CFS_ID or CFS_PARENT_ID environment variables, we'll
 * simply set them back again when we do an exec() call.
 *======================================================================*/

/*======================================================================
 * Interposed - readdir()
 *
 * struct dirent *readdir(DIR *dirstream)
 *
 * No need to implement. We don't care about directory access, but we
 * will care if one of the files in that directory is accessed.
 *======================================================================*/

/*======================================================================
 * Interposed - readdir64()
 *
 * struct dirent64 *readdir64(DIR *dirstream)
 *
 * No need to implement. We don't care about directory access, but we
 * will care if one of the files in that directory is accessed.
 *======================================================================*/

/*======================================================================
 * Interposed - readdir_r()
 *
 * int readdir_r(DIR *dirstream, struct dirent *entry,
 * 		struct dirent **result)
 *
 * No need to implement. We don't care about directory access, but we
 * will care if one of the files in that directory is accessed.
 *======================================================================*/

/*======================================================================
 * Interposed - readdir64_r()
 *
 * int readdir64_r(DIR *dirstream, struct dirent64 *entry,
 * 		struct dirent64 **result)
 *
 * No need to implement. We don't care about directory access, but we
 * will care if one of the files in that directory is accessed.
 *======================================================================*/

/*======================================================================
 * Interposed - readlink()
 *
 * int readlink(const char *filename, char *buffer, size_t size)
 *
 * Ignore for now. It doesn't seem useful to know that the program
 * is trying to see where a link points. We only care that the file
 * itself is accessed.
 *======================================================================*/

/*======================================================================
 * Interposed - readlinkat()
 *
 * int readlinkat(int dirfd, const char *pathname,
 *                     char *buf, size_t bufsiz);
 *
 * Ignore for now. It doesn't seem useful to know that the program
 * is trying to see where a link points. We only care that the file
 * itself is accessed.
 *======================================================================*/

/*======================================================================
 * Interposed - remove()
 *======================================================================*/

int
remove(const char *path)
{
	FETCH_REAL_FN(int, real_remove, "remove");

	_cfs_debug(1, "remove(\"%s\")", path);

	/*
	 * Check whether this path is a directory. Note, we're about to
	 * delete it, so we need to find out before it's gone.
	 */
	int is_dir = _cfs_isdirectory(path);

	/* now perform the deletion, and log the access */
	int rc = real_remove(path);
	if (rc != -1) {
		if (_cfs_delete_common(path, is_dir) == -1) {
			return -1;
		}
	}
	return rc;
}

/*======================================================================
 * Interposed - removexattr()
 *
 * int removexattr(const char *path, const char *name);
 *
 * Not implemented for now. Extended attribute are an advanced concept.
 *======================================================================*/

/*======================================================================
 * Interposed - rename()
 *======================================================================*/

int
rename(const char *oldname, const char *newname)
{
	FETCH_REAL_FN(int, real_rename, "rename");

	_cfs_debug(1, "rename(\"%s\", \"%s\")", oldname, newname);

	/*
	 * Is oldname a directory? We need to find out now, since we're
	 * about to delete it.
	 */
	int is_dir = _cfs_isdirectory(oldname);

	int rc = real_rename(oldname, newname);
	if (rc != -1) {

		/* Mark the oldname as deleted */
		if (_cfs_delete_common(oldname, is_dir) == -1){
			return -1;
		}

		/*
		 * Mark the newname as created. It could be either a file, or a
		 * directory, but _cfs_open_common will figure that out.
		 */
		if (_cfs_open_common(newname, O_CREAT, TRUE) == -1) {
			return -1;
		}
	}
	return rc;
}

/*======================================================================
 * Interposed - renameat
 *======================================================================*/

int
renameat(int olddirfd, const char *oldpath,
             int newdirfd, const char *newpath)
{
	FETCH_REAL_FN(int, real_renameat, "renameat");

	_cfs_debug(1, "renameat(%d, \"%s\", %d, \"%s\")", olddirfd, oldpath,
			newdirfd, newpath);

	/* figure out if oldpath is a file or directory, before we delete it */
	char converted_path[PATH_MAX];
	if (_cfs_convert_pathat_to_path(olddirfd, oldpath, converted_path) == -1) {
		return -1;
	}
	int is_dir = _cfs_isdirectory(converted_path);

	/* perform the real rename operation */
	int rc = real_renameat(olddirfd, oldpath, newdirfd, newpath);
	if (rc != -1) {

		/* Mark the oldname as deleted */
		if (_cfs_delete_common(converted_path, is_dir) == -1){
			return -1;
		}

		/*
		 * Mark the newname as created. It could be either a file, or a
		 * directory, but _cfs_open_common will figure that out.
		 */
		if (_cfs_convert_pathat_to_path(newdirfd, newpath, converted_path) == -1) {
			return -1;
		}
		if (_cfs_open_common(converted_path, O_CREAT, TRUE) == -1) {
			return -1;
		}
	}
	return rc;
}

/*======================================================================
 * Interposed - rewinddir()
 *
 * void rewinddir(DIR *dirstream)
 *
 * No need to implement. We don't care about directory access, but we
 * will care if one of the files in that directory is accessed.
 *======================================================================*/

/*======================================================================
 * Interposed - rexec
 *
 * int rexec(char **ahost, int inport, char *user,
 *                char *passwd, char *cmd, int *fd2p);
 *
 * Not implemented. Probably too obscure for a build system.
 *======================================================================*/

/*======================================================================
 * Interposed - rmdir()
 *======================================================================*/

int
rmdir(const char *dirname)
{
	FETCH_REAL_FN(int, real_rmdir, "rmdir");

	_cfs_debug(1, "rmdir(\"%s\")", dirname);

	int rc = real_rmdir(dirname);
	if (rc != -1) {
		_cfs_delete_common(dirname, TRUE);
	}
	return rc;
}

/*======================================================================
 * Interposed - scandir()
 * Interposed - scandir64()
 * Interposed - seekdir()
 *
 * No need to implement. We don't care about directory access, but we
 * will care if one of the files in that directory is accessed.
 *======================================================================*/

/*======================================================================
 * Interposed - setenv
 *
 * int setenv(const char *name, const char *value, int overwrite);
 *
 * Not supported. The caller could change the LD_PRELOAD environment
 * variable, but when we next make an exec() call, we'll make sure our
 * interposer library is included in the LD_PRELOAD path.
 *======================================================================*/

/*======================================================================
 * Interposed - setxattr
 *
 * int setxattr(const char *path, const char *name,
 *                    const void *value, size_t size, int flags);
 *
 * Not implemented for now. Extended attribute are an advanced concept.
 *======================================================================*/

/*======================================================================
 * Interposed - stat()
 *
 * int stat(const char *filename, struct stat *buf)
 *
 * No need to implement. We'll already have information about the file
 * when we opened the filedes.
 *======================================================================*/

/*======================================================================
 * Interposed - stat64()
 *
 * int stat64(const char *filename, struct stat64 *buf)
 *
 * No need to implement. We'll already have information about the file
 * when we opened the filedes.
 *======================================================================*/

/*======================================================================
 * Interposed - statfs()
 *
 * int statfs(const char *path, struct statfs *buf);
 *
 * Ignore for now. Too advanced to be concerned with file system
 * statistics.
 *======================================================================*/

/*======================================================================
 * Interposed - statfs64()
 *
 * int statfs(const char *path, struct statfs64 *buf);
 *
 * Ignore for now. Too advanced to be concerned with file system
 * statistics.
 *======================================================================*/

/*======================================================================
 * Interposed - statvfs()
 *
 * int statvfs(const char *path, struct statvfs *buf);
 *
 * Ignore for now. Too advanced to be concerned with file system
 * statistics.
 *======================================================================*/

/*======================================================================
 * Interposed - statvfs64()
 *
 * int statvfs(const char *path, struct statvfs64 *buf)
 *
 * Ignore for now. Too advanced to be concerned with file system
 * statistics.
 *======================================================================*/

/*======================================================================
 * Interposed - symlink()
 *======================================================================*/

int
symlink(const char *oldname, const char *newname)
{
	FETCH_REAL_FN(int, real_symlink, "symlink");

	_cfs_debug(1, "symlink(\"%s\", \"%s\")", oldname, newname);

	/*
	 * Compute the normalized name for the target file. We need to do
	 * this now (before it's created), otherwise the normalizing function
	 * will simply refer us back to the file we created a link to.
	 */
	char converted_path[PATH_MAX];
	if (_cfs_combine_paths(_cfs_get_cwd(TRUE), newname, converted_path) == -1) {
		return -1;
	}

	int rc = real_symlink(oldname, newname);
	if (rc != -1) {
		/* mark the existing file as having being "read" */
		if (_cfs_open_common(oldname, O_RDONLY, TRUE) == -1) {
			return -1;
		}

		/*
		 * Mark the symlink file as having being "created". We are using
		 * a pre-normalized path, so ask _cfs_open_common() not to normalize
		 * it again. This would be a major problem, since converted_path
		 * now points to a symlink, and we want to report the symlink's name,
		 * not the file that it points to.
		 */
		if (_cfs_open_common(converted_path, O_CREAT, FALSE) == -1) {
			return -1;
		}
	}
	return rc;
}

/*======================================================================
 * Interposed - symlinkat()
 *======================================================================*/

int
symlinkat(const char *oldpath, int newdirfd, const char *newpath)
{
	FETCH_REAL_FN(int, real_symlinkat, "symlinkat");

	_cfs_debug(1, "symlinkat(\"%s\", %d, \"%s\")", oldpath, newdirfd, newpath);

	/*
	 * Compute the normalized name for the target file. We need to do
	 * this now (before it's created), otherwise the normalizing function
	 * will simply refer us back to the file we created a link to. We
	 * start by converting the dirfd/path to a real path, then we normalize
	 * it.
	 */
	char converted_path1[PATH_MAX];
	char converted_path2[PATH_MAX];

	if (_cfs_convert_pathat_to_path(newdirfd, newpath, converted_path1) == -1) {
		return -1;
	}
	if (_cfs_combine_paths(_cfs_get_cwd(TRUE), converted_path1, converted_path2) == -1) {
		return -1;
	}

	int rc = real_symlinkat(oldpath, newdirfd, newpath);
	if (rc != -1) {
	
		/* mark the old file as being read */
		if (_cfs_open_common(oldpath, O_RDONLY, TRUE) == -1) {
			return -1;
		}

		/*
		 * Mark the symlink file as having being "created". We are using
		 * a pre-normalized path, so ask _cfs_open_common() not to normalize
		 * it again. This would be a major problem, since converted_path
		 * now points to a symlink, and we want to report the symlink's name,
		 * not the file that it points to.
		 */
		if (_cfs_open_common(converted_path2, O_CREAT, FALSE) == -1) {
			return -1;
		}
	}
	return rc;
}

/*======================================================================
 * Interposed - system()
 *======================================================================*/

int
system(const char *command)
{
	extern char **environ;
	char **old_environ = environ;

	FETCH_REAL_FN(int, real_system, "system");

	_cfs_debug(1, "system(\"%s\")", command);

	/* make sure that CFS_ID, LD_PRELOAD and CFS_PARENT_ID are correct */
	environ = (char **)_cfs_modify_envp(environ);

	/* now invoke the original system() call */
	int result = real_system(command);

	/* restore our original environment */
	char **tmp_environ = environ;
	environ = old_environ;
	_cfs_cleanup_envp(tmp_environ);

	return result;
}

/*======================================================================
 * Interposed - telldir()
 *
 * long int telldir(DIR *dirstream)
 *
 * No need to implement. We don't care about directory access, but we
 * will care if one of the files in that directory is accessed.
 *======================================================================*/

/*======================================================================
 * Interposed - tempnam()
 * Interposed - tmpfile()
 * Interposed - tmpfile64()
 * Interposed - tmpnam()
 * Interposed - tmpnam_r()
 *
 * Not implemented. We normally don't care about temporary files.
 *======================================================================*/

/*======================================================================
 * Interposed - truncate()
 *======================================================================*/

int
truncate(const char *filename, off_t length)
{
	FETCH_REAL_FN(int, real_truncate, "truncate");

	_cfs_debug(1, "truncate(\"%s\", %d)", filename, length);

	/*
	 * Call the real truncate function, and if it succeeds,
	 * we trace that the file was "opened for read/write".
	 */
	int status = real_truncate(filename, length);
	if (status != -1) {
		if (_cfs_open_common(filename, O_RDWR, TRUE) != 0){
			return -1;
		}
	}
	return status;
}

/*======================================================================
 * Interposed - truncate64()
 *======================================================================*/

int
truncate64(const char *filename, off64_t length)
{
	/*
	 * We can't use the FETCH_REAL_FN macro here, since we need
	 * the prototype of real_truncate64 to include a 64-bit offset.
	 */
	static int (*(real_truncate64))(const char *, off64_t) = NULL;
	if (!real_truncate64){
		real_truncate64 = dlsym(RTLD_NEXT, ("truncate64"));
	}

	_cfs_debug(1, "truncate64(\"%s\", %lld)", filename, length);

	/*
	 * Call the real truncate64 function, and if it succeeds,
	 * we trace that the file was "opened for read/write".
	 */
	int status = real_truncate64(filename, (off_t)length);
	if (status != -1) {
		if (_cfs_open_common(filename, O_RDWR, TRUE) != 0){
			return -1;
		}
	}
	return status;
}

/*======================================================================
 * Interposed - unlink()
 *======================================================================*/

int
unlink(const char *filename)
{
	FETCH_REAL_FN(int, real_unlink, "unlink");

	_cfs_debug(1, "unlink(\"%s\")", filename);

	/* perform the real "unlink" operation */
	int rc = real_unlink(filename);
	if (rc != -1) {
		if (_cfs_delete_common(filename, FALSE) == -1) {
			return -1;
		}
	}
	return rc;
}

/*======================================================================
 * Interposed - unlinkat()
 *======================================================================*/

int
unlinkat(int dirfd, const char *pathname, int flags)
{
	FETCH_REAL_FN(int, real_unlinkat, "unlinkat");

	_cfs_debug(1, "unlinkat(%d, \"%s\", 0x%x)", dirfd, pathname, flags);

	int rc = real_unlinkat(dirfd, pathname, flags);
	if (rc != -1) {
		char converted_path[PATH_MAX];
		if (_cfs_convert_pathat_to_path(dirfd, pathname, converted_path) == -1) {
			return -1;
		}
		if (_cfs_delete_common(converted_path, flags & AT_REMOVEDIR) == -1) {
			return -1;
		}
	}
	return rc;
}

/*======================================================================
 * Interposed - unsetenv()
 *
 * Not supported. Could remove LD_PRELOAD, but when we next call exec(),
 * we'll make sure that LD_PRELOAD is set correctly for the new process
 * image.
 *======================================================================*/

/*======================================================================
 * Interposed - uselib()
 *
 * int uselib(const char *library);
 *
 * No implemented. Too obscure.
 *======================================================================*/

/*======================================================================
 * Interposed - utime()
 *
 * int utime(const char *filename, const struct utimbuf *times)
 *
 * It's not clear if timestamp changes are interesting. This would
 * probably be used by a touch command, but that's not interesting
 * from Disco's perspective.
 *======================================================================*/

/*======================================================================
 * Interposed - utimes()
 *
 * int utimes(const char *filename, struct timeval tvp[2])
 *
 * It's not clear if timestamp changes are interesting. This would
 * probably be used by a touch command, but that's not interesting
 * from Disco's perspective.
 *======================================================================*/

/*======================================================================
 * Interposed - utimensat()
 *
 * int utimensat(int dirfd, const char *pathname,
 *                  const struct timespec times[2], int flags)
 *
 * It's not clear if timestamp changes are interesting. This would
 * probably be used by a touch command, but that's not interesting
 * from Disco's perspective.
 *======================================================================*/

/*======================================================================
 * Interposed - vfork()
 *
 * pid_t vfork(void)
 *
 * According to the man page, this function is only called when the child
 * is about to invoke an exec() call. The child must not call any other
 * functions, since it shares an address space (and stack!) with the
 * parent. Our problem is that we've modified the exec() calls, so
 * automatically we'll have problems.
 *
 * To get around this, we replace vfork() with forK(). The only difference
 * is performance.
 *======================================================================*/

pid_t
vfork(void)
{
	FETCH_REAL_FN(pid_t, real_fork, "fork");

	_cfs_debug(1, "vfork()");

	/* call fork, which isn't interposed */
	return real_fork();
}

/*======================================================================*/
