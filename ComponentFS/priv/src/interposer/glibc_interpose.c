/*******************************************************************************
 * Copyright (c) 2011 Arapiki Solutions Inc.
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
#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
#include <nl_types.h>
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

/*
 * A useful macro for fetching a pointer to the real version of the function
 * We search the list of dynamic libraries to find the next occurrence of this symbol,
 * which should be the "real" version of the function. Each interposed function can
 * use this macro to efficiently find the location of the real function. Note that we
 * only need to initialize the variable once.
 * For example:
 * 		FETCH_REAL_FN(FILE *, real_fopen, "fopen");
 * assigns the memory address of the real "fopen" function to the real_fopen variable.
 */
#define FETCH_REAL_FN(type, fn_var, fn_name) \
		static type (*(fn_var))() = NULL; \
		if (!(fn_var)){ \
			(fn_var) = dlsym(RTLD_NEXT, (fn_name)); \
		}

/*======================================================================
 * Global variables
 *======================================================================*/

/*
 * Each process must have a unique process number. This is not the same
 * as a Unix process ID, since those are only 16-bit in size and may easily
 * be reused during a single build process.
 */
static int my_process_number;

/*
 * Same, but for this process's parent.
 */
static int my_parent_process_number;

/*
 * Debug level. Set this by setting the CFS_DEBUG environment variable
 */
static int debug_level;


/*======================================================================
 * _init_interposer()
 *
 * When this dynamic library is first loaded, the _init_interposer function
 * is called as a constructor. This is where we allocate the shared memory
 * segment for cfs, as well as perform other start-up tasks.
 *======================================================================*/

void _init_interposer() __attribute__ ((constructor));

void _init_interposer()
{
	static char argv_and_envp[NCARGS];

	/* disable debugging for now */
	debug_level = 0;

	/*
	 * If the CFS_ID environment variable is defined, then this current process
	 * is running within the cfs environment. If not, just return silently.
	 */
	char *cfs_id = getenv("CFS_ID");
	if (!cfs_id){
		return;
	}

	/* Grab a copy of the command line arguments and environment (argv/envp) */
	int fd = open("/proc/self/cmdline", O_RDONLY);
	int argv_size = read(fd, argv_and_envp, NCARGS);
	if (argv_size == -1) {
		fprintf(stderr, "Error: cfs couldn't determine command line arguments.\n");
		exit(1);
	}
	close(fd);

	/* Write an empty string immediately after the last argument string. */
	argv_and_envp[argv_size] = '\0';
	fd = open("/proc/self/environ", O_RDONLY);
	int envp_size = read(fd, &argv_and_envp[argv_size + 1], NCARGS - argv_size - 2);
	if (envp_size == -1) {
		fprintf(stderr, "Error: cfs couldn't determine command environment.\n");
		exit(1);
	}
	close(fd);

	/* Terminate the environment array */
	argv_and_envp[argv_size + 1 + envp_size] = '\0';

	/* Yes, there's an existing CFS trace buffer. Attach to it */
	trace_buffer_id id = atoi(cfs_id);
	if (trace_buffer_use_existing(id) == -1){
		fprintf(stderr, "Error: couldn't attach to cfs trace buffer\n");
		exit(1);
	}

	/*
	 * Determine our parent's process number by examining the CFS_PARENT_ID
	 * environment variable. If this isn't defined, use ID number 0.
	 */
	char *cfs_parent_id = getenv("CFS_PARENT_ID");
	if (cfs_parent_id == NULL) {
		my_parent_process_number = 0;
	} else {
		/*
		 * Convert to an integer. Note: atoi returns 0 on an error, but that's
		 * what we want.
		 */
		my_parent_process_number = atoi(cfs_parent_id);
	}

	/*
	 * Enable debugging, if desired. An non-numeric value in the CFS_DEBUG environment
	 * variable will be dealt with by disabling debugging. Debug levels that are too
	 * high will be reduce to level 2.
	 */
	char *cfs_debug = getenv("CFS_DEBUG");
	if (cfs_debug != NULL) {
		debug_level = atoi(cfs_debug);
		if (debug_level < 0){
			debug_level = 0;
		} else if (debug_level > 2){
			debug_level = 2;
		}
	}

	/*
	 * Allocate a process number for this newly started process. This process
	 * number is added to each trace message.
	 */
	if (trace_buffer_lock() == 0){
		my_process_number = trace_buffer_next_process_number();

		/* trace the argv and envp arrays */
		trace_buffer_write_byte(TRACE_FILE_NEW_PROGRAM);
		trace_buffer_write_int(my_process_number);
		trace_buffer_write_int(my_parent_process_number);
		trace_buffer_write_bytes(argv_and_envp, argv_size + 1 + envp_size + 1);
		trace_buffer_unlock();

		/*
		 * Write our process number into CFS_PARENT_ID, ready for when we become
		 * the parent.
		 */
		char id_string[strlen("NNNNNNNNNN") + 1];
		sprintf(id_string, "%d", my_process_number);
		if (setenv("CFS_PARENT_ID", id_string, 1) != 0){
			fprintf(stderr, "Error: failed to set the CFS_PARENT_ID variable\n");
			exit(1);
		}
	}
}

/*======================================================================
 * cfs_debug
 * 
 * Display debug output. Only display debug output that's within the
 * current debug_level (set via the CFS_DEBUG environment variable).
 *======================================================================*/

static void cfs_debug(int level, char *string, ...) {
	va_list args;

	va_start(args, string);
	if (level <= debug_level) {
		vfprintf(stderr, string, args);
		fprintf(stderr, "\n");
	}
}

/*======================================================================
 * cfs_debug_env
 *
 * Display the given set of environment variables as a debug message.
 *  - level - The debug level of the output (for filtering whether
 *            the output is displayed).
 *  - envp  - The environment to display.
 *
 *======================================================================*/

static void cfs_debug_env(int level, char * const *envp) {
	if (level <= debug_level) {
		char * const *ptr = envp;
		fprintf(stderr, "Environment Variables:\n");
		while (*ptr != NULL) {
			fprintf(stderr, "  %s\n", *ptr);
			ptr++;
		}
	}
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
 *     constructor function (_init_interposer), since some of the system
 *     calls will be executed before the constructor is called.
 *   - Functions are listed alphabetically, purely for convenience.
 *======================================================================*/

// TODO: ensure that errno is saved in every function where it needs to be.

/*======================================================================
 * Interposed - access()
 *======================================================================*/

int access(const char *pathname, int mode)
{
	FETCH_REAL_FN(int, real_access, "access");

	cfs_debug(1, "access(\"%s\", %d)", pathname, mode);

	// TODO: validate pathname based on component location.
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

	cfs_debug(1, "chdir(\"%s\")", path);

	/*
	 * TODO: validate path based on component location.
	 * TODO: record the new current directory for future use when
	 * handling relative path names.
	 */
	return real_chdir(path);
}

/*======================================================================
 * Interposed - chmod()
 *======================================================================*/

int
chmod(const char *path, mode_t mode)
{
	FETCH_REAL_FN(int, real_chmod, "chmod");

	cfs_debug(1, "chmod(\"%s\", 0%o)", path, mode);

	/*
	 * TODO: validate path based on component location.
	 * TODO: log the fact that the file's attributes have changed. This
	 * is useful in Disco.
	 */
	return real_chmod(path, mode);
}

/*======================================================================
 * Interposed - chown()
 *======================================================================*/

int
chown(const char *path, uid_t owner, gid_t group)
{
	FETCH_REAL_FN(int, real_chown, "chown");

	cfs_debug(1, "chown(\"%s\", %d, %d)", path, owner, group);

	/*
	 * TODO: validate path based on component location.
	 * TODO: log the fact that the file's attributes have changed. This
	 * is useful in Disco.
	 * NOTE: if path is a symlink, it'll be dereferenced. See lchown
	 * for a variant of this command that doesn't dereference.
	 */
	return real_chown(path, owner, group);
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

	cfs_debug(1, "creat(\"%s\", 0%o)", path, mode);

	/*
	 * TODO: validate path based on component location.
	 * TODO: log the fact that the a new file has been created.
	 */
	return real_creat(path, mode);
}

/*======================================================================
 * Interposed - creat64
 *======================================================================*/

int
creat64(const char *path, mode_t mode)
{
	FETCH_REAL_FN(int, real_creat64, "creat64");

	cfs_debug(1, "creat64(\"%s\", 0%o)", path, mode);

	/*
	 * TODO: validate path based on component location.
	 * TODO: log the fact that the a new file has been created.
	 */
	return real_creat64(path, mode);
}

/*======================================================================
 * Interposed - dlopen
 *======================================================================*/

void *
dlopen(const char *file, int mode)
{
	FETCH_REAL_FN(void *, real_dlopen, "dlopen");

	cfs_debug(1, "dlopen(\"%s\", 0%o)", file, mode);

	/*
	 * TODO: validate path based on component location.
	 * TODO: log the fact that the a new file has been read.
	 */
	return real_dlopen(file, mode);
}

/*======================================================================
 * Interposed - eaccess
 *======================================================================*/

int eaccess(const char *pathname, int mode)
{
	FETCH_REAL_FN(int, real_eaccess, "eaccess");

	cfs_debug(1, "eaccess(\"%s\", 0%o)", pathname, mode);

	// TODO: validate path based on component location.
	return real_eaccess(pathname, mode);
}

/*======================================================================
 * Interposed - euidaccess
 *======================================================================*/

int euidaccess(const char *pathname, int mode)
{
	FETCH_REAL_FN(int, real_euidaccess, "euidaccess");

	cfs_debug(1, "euidaccess(\"%s\", 0%o)", pathname, mode);

	// TODO: validate path based on component location.
	return real_euidaccess(pathname, mode);
}

/*======================================================================
 * Helper: modify_envp
 *
 * This function is a helper function for all execXX() functions. It
 * modifies the set of environment variables to ensure that they
 * contain suitable values for:
 *   LD_PRELOAD - must refer to this interceptor library.
 *   CFS_ID - provides the shared memory ID for our trace buffer.
 *   CFS_PARENT_ID - provides our parent process ID.
 * For numerous reasons, these variable could be removed or overwritten,
 * but the success of CFS depends on them being set.
 *======================================================================*/

char * const *
modify_envp(char *const * envp)
{
	extern int errno;
	int tmp_errno = errno; 		/* take care not to disrupt errno */

	// TODO: write this function.
	errno = tmp_errno;			/* restore errno */
	return envp;
}

/*======================================================================
 * Helper: cleanup_envp
 *
 * This function cleans up any memory allocated by modify_envp.
 *======================================================================*/

void
cleanup_envp(char * const *envp)
{
	extern int errno;
	int tmp_errno = errno; 		/* take care not to disrupt errno */

	// TODO: write this function.

	errno = tmp_errno;			/* restore errno */
}

/*======================================================================
 * Helper - execve_common()
 *
 * This function is a helper for many of the interposed exec functions.
 *
 *======================================================================*/

int
execve_common(const char *filename, char *const argv[], char *const envp[])
{
	FETCH_REAL_FN(int, real_execve, "execve");

	/*
	 * Before calling the real execve, make sure our custom environment
	 * variables are in place.
	 */
	cfs_debug_env(2, envp);
	char * const *new_envp = modify_envp(envp);

	int rc = real_execve(filename, argv, new_envp);

	/* if we get here, the execve failed */
	cleanup_envp(new_envp);
	return rc;
}

/*======================================================================
 * Interposed - execl()
 *======================================================================*/

int
execl(const char *path, const char *arg0, ...)
{
	extern char **environ;

	cfs_debug(1, "execl(\"%s\", ..., ...)", path);

	/* execl is simply a wrapper for execve()s */
	return execve_common(path, (char * const *)&arg0, environ);
}

/*======================================================================
 * Interposed - execle()
 *======================================================================*/

int
execle(const char *path, const char *arg0, ... /*, 0, char *const envp[] */)
{
	cfs_debug(1, "execle(\"%s\", ..., ...)", path);

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

	return execve_common(path, (char * const *)&arg0, envp);
}

/*======================================================================
 * Helper - execvpe_common()
 *======================================================================*/

int
execvpe_common(const char *file, char *const argv[], char *const envp[])
{
	FETCH_REAL_FN(int, real_execvpe, "execvpe");

	/*
	 * Before calling the real execvpe, make sure our custom environment
	 * variables are in place.
	 */
	char * const *new_envp = modify_envp(envp);
	int rc = real_execvpe(file, argv, new_envp);

	/* if we get here, the execvpe failed */
	cleanup_envp(new_envp);
	return rc;
}

/*======================================================================
 * Interposed - execlp()
 *======================================================================*/

int execlp(const char *file, const char *arg0, ...)
{
	extern char **environ;

	cfs_debug(1, "execlp(\"%s\", ..., ...)", file);

	/*
	 * execlp is a wrapper for execvpe().
	 */
	return execvpe_common(file, (char * const *)&arg0, environ);
}

/*======================================================================
 * Interposed - execv()
 *======================================================================*/

int
execv(const char *path, char *const argv[])
{
	extern char **environ;
	cfs_debug(1, "execv(\"%s\", ...)", path);

	/*
	 * execv is a wrapper for execve().
	 */
	return execve_common(path, argv, environ);
}

/*======================================================================
 * Interposed - execve()
 *======================================================================*/

int
execve(const char *filename, char *const argv[], char *const envp[])
{
	cfs_debug(1, "execve(\"%s\", ..., ...)", filename);

	return execve_common(filename, argv, envp);
}

/*======================================================================
 * Interposed - execvp()
 *======================================================================*/

int
execvp(const char *file, char *const argv[])
{
	extern char **environ;
	cfs_debug(1, "execvp(\"%s\", ...)", file);

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
	cfs_debug(1, "execvpe(\"%s\", ..., ...)", file);

	return execvpe_common(file, argv, envp);
}

/*======================================================================
 * Interposed - exit()
 *======================================================================*/

void
exit(int status)
{
	FETCH_REAL_FN(int, real_exit, "exit");

	cfs_debug(1, "exit(%d)", status);

	// TODO: log fact that the process is exiting (helps with detecting
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

	cfs_debug(1, "_exit(%d)", status);

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

	cfs_debug(1, "_Exit(%d)", status);

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

int faccessat(int dirfd, const char *pathname, int mode, int flags)
{
	FETCH_REAL_FN(int, real_faccessat, "faccessat");

	cfs_debug(1, "faccessat(%d, \"%s\", 0%o, %d)", dirfd, pathname, mode, flags);

	// TODO: validate access path, given component boundaries.
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

	cfs_debug(1, "fchdir(%d)", fd);

	// TODO: compute the path of the new current working directory,
	// and record it for future use when computing relative path names.
	return real_fchdir(fd);
}

/*======================================================================
 * Interposed - fchmod()
 *======================================================================*/

int
fchmod(int fd, mode_t mode)
{
	FETCH_REAL_FN(int, real_fchmod, "fchmod");

	cfs_debug(1, "fchmod(%d, 0%o)", fd, mode);

	// TODO: log the fact that the file has new permission bits.
	// TODO: how do we find the file name relating to this fd?
	return real_fchmod(fd, mode);
}

/*======================================================================
 * Interposed - fchmodat()
 *======================================================================*/

int
fchmodat(int dirfd, const char *pathname, mode_t mode, int flags)
{
	FETCH_REAL_FN(int, real_fchmodat, "fchmodat");

	cfs_debug(1, "fchmodat(%d, \"%s\", 0%o, %d)", dirfd, pathname, mode, flags);

	// TODO: compute the absolute path of the file (see the man page for special
	// situations to consider).
	// TODO: log the fact that the file has new permission bits.
	return real_fchmodat(dirfd, pathname, mode, flags);
}

/*======================================================================
 * Interposed - fchown()
 *======================================================================*/

int
fchown(int fd, uid_t owner, gid_t group)
{
	FETCH_REAL_FN(int, real_fchown, "fchown");

	cfs_debug(1, "fchown(%d, %d, %d)", fd, owner, group);

	// TODO: how do we find the file name relating to this fd?
	// TODO: log the fact that the file has new attributes set.
	return real_fchown(fd, owner, group);
}

/*======================================================================
 * Interposed - fchownat()
 *======================================================================*/

int
fchownat(int dirfd, const char *pathname, uid_t owner, gid_t group, int flags)
{
	FETCH_REAL_FN(int, real_fchownat, "fchownat");

	cfs_debug(1, "fchownat(%d, \"%s\", %d, %d, %d)",
			dirfd, pathname, owner, group, flags);

	// TODO: compute the absolute path of the file (see the man page for special
	// situations to consider).
	// TODO: log the fact that the file has new attributes.
	return real_fchownat(dirfd, pathname, owner, group, flags);
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

	cfs_debug(1, "fexecve(%d, ..., ...)", fd);

	/*
	 * Before calling the real fexecve, make sure our custom environment
	 * variables are in place. Note that the child process will generate
	 * the trace information once it's exec'ed. We therefore don't need
	 * to know the name of the program being started.
	 */
	char * const *new_envp = modify_envp(envp);
	int rc = real_fexecve(fd, argv, new_envp);

	/* if we get here, the fexecve failed */
	cleanup_envp(envp);
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
 * Helper: fopen_common()
 *
 * A common function for tracing the arguments of fopen, fopen64 and
 * other related commands.
 *======================================================================*/

static void fopen_common(const char *filename, const char *opentype)
{
	/*
	 * Grab a lock on the trace buffer. If it hasn't been initialized yet,
	 * just return and don't trace anything.
	 */
	extern int errno;
	int tmp_errno = errno;

	if (trace_buffer_lock() == 0){
		/* for 'r' and 'rb' modes, the operation is a read, else it's a write */
		if ((strcmp(opentype, "r") == 0) ||
			(strcmp(opentype, "rb") == 0)){
			trace_buffer_write_byte(TRACE_FILE_READ);
		} else {
			trace_buffer_write_byte(TRACE_FILE_WRITE);
		}
		trace_buffer_write_int(my_process_number);
		trace_buffer_write_string(filename);
		trace_buffer_unlock();
	}
	errno = tmp_errno;
}
/*======================================================================
 * Interposed - fopen()
 *======================================================================*/

FILE *fopen(const char *filename, const char *mode)
{
	FETCH_REAL_FN(FILE *, real_fopen, "fopen");

	cfs_debug(1, "fopen(\"%s\", \"%s\")", filename, mode);
	FILE *f = real_fopen(filename, mode);
	if (f != NULL) {
		fopen_common(filename, mode);
	}
	return f;
}

/*======================================================================
 * Interposed - fopen64()
 *======================================================================*/

FILE *fopen64(const char *filename, const char *mode)
{
	FETCH_REAL_FN(FILE *, real_fopen64, "fopen64");

	cfs_debug(1, "fopen64(\"%s\", \"%s\")", filename, mode);
	FILE *f = real_fopen64(filename, mode);
	if (f != NULL) {
		fopen_common(filename, mode);
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

	cfs_debug(1, "fork()");

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

	cfs_debug(1, "freopen(\"%s\", \"%s\", %p)", path, mode, stream);

	FILE *f = real_freopen(path, mode, stream);
	if (f != NULL) {
		fopen_common(path, mode);
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

	cfs_debug(1, "freopen64(\"%s\", \"%s\", %p)", path, mode, stream);

	FILE *f = real_freopen64(path, mode, stream);
	if (f != NULL) {
		fopen_common(path, mode);
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

	cfs_debug(1, "ftok(\"%s\", %d)", pathname, proj_id);

	// TODO: validate pathname with respect to component boundaries.
	// TODO: register that the pathname is accessed

	return real_ftok(pathname, proj_id);
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

	cfs_debug(1, "lchown(\"%s\", %d, %d)", path, owner, group);

	// TODO: log the fact that the file has new attributes set.
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

	cfs_debug(1, "link(\"%s\", \"%s\")", oldname, newname);

	// TODO: log the creation of the link.
	// TODO: validate oldname and new name, based on component
	// boundaries.
	return real_link(oldname, newname);
}

/*======================================================================
 * Interposed - linkat()
 *======================================================================*/

int
linkat(int olddirfd, const char *oldpath,
           int newdirfd, const char *newpath, int flags)
{
	FETCH_REAL_FN(int, real_linkat, "linkat");

	cfs_debug(1, "linkat(%d, \"%s\", %d, \"%s\", %d)", olddirfd, oldpath,
			newdirfd, newpath, flags);

	// TODO: log the creation of the link.
	// TODO: validate oldname and new name, based on component
	// boundaries.
	return real_linkat(olddirfd, oldpath, newdirfd, newpath, flags);
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

	cfs_debug(1, "mkdir(\"%s\", 0%o)", path, mode);

	// TODO: log the creation of the directory
	// TODO: validate path based on component boundaries.
	return real_mkdir(path, mode);
}

/*======================================================================
 * Interposed - mkdirat()
 *======================================================================*/

int
mkdirat(int dirfd, const char *pathname, mode_t mode)
{
	FETCH_REAL_FN(int, real_mkdirat, "mkdirat");

	cfs_debug(1, "mkdirat(%d, \"%s\", 0%o)", dirfd, pathname, mode);

	// TODO: log the creation of the directory
	// TODO: validate path based on component boundaries.
	return real_mkdirat(dirfd, pathname, mode);
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
 * open_common()
 *
 * A common function for tracing detail of open, open64 or related library
 * calls.
 *======================================================================*/

static void open_common(const char *filename, int flags)
{
	/* if there's a trace buffer, write the file name to it */
	extern int errno;
	int tmp_errno = errno; 				/* save errno, in case we change it */
	if (trace_buffer_lock() == 0){
		if ((flags & (O_APPEND|O_CREAT|O_WRONLY|O_RDWR)) != 0) {
			trace_buffer_write_byte(TRACE_FILE_WRITE);
		} else {
			trace_buffer_write_byte(TRACE_FILE_READ);
		}
		trace_buffer_write_int(my_process_number);
		trace_buffer_write_string(filename);
		trace_buffer_unlock();
	}
	errno = tmp_errno;					/* restore original errno value */
}

/*======================================================================
 * Interposed - open()
 *======================================================================*/

int open(const char *filename, int flags, ...)
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

	cfs_debug(1, "open(\"%s\", 0x%x, 0%o)", filename, flags, mode);

	int fd = real_open(filename, flags, mode);
	if (fd != -1) {
		open_common(filename, flags);
	}
	return fd;
}

/*======================================================================
 * Interposed - open64()
 *======================================================================*/

int open64(const char *filename, int flags, ...)
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

	cfs_debug(1, "open64(\"%s\", 0x%x, 0%o)", filename, flags, mode);

	int fd = real_open64(filename, flags, mode);
	if (fd != -1) {
		open_common(filename, flags);
	}
	return fd;
}

/*======================================================================
 * Interposed - openat()
 *======================================================================*/

int openat(int dirfd, const char *pathname, int flags, ...)
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

	cfs_debug(1, "openat(%d, \"%s\", 0x%x, 0%o)", dirfd, pathname, flags, mode);

	// TODO: log the access to dirfd + pathname
	// TODO: validate the path, based on component boundaries.

	int fd = real_openat(dirfd, pathname, flags, mode);

	return fd;
}

/*======================================================================
 * Interposed - openat64()
 *======================================================================*/

int openat64(int dirfd, const char *pathname, int flags, ...)
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

	cfs_debug(1, "openat64(%d, \"%s\", 0x%x, 0%o)", dirfd, pathname, flags, mode);

	// TODO: log the access to dirfd + pathname
	// TODO: validate the path, based on component boundaries.

	int fd = real_openat64(dirfd, pathname, flags, mode);

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

FILE *popen(const char *command, const char *mode)
{
	extern char **environ;
	char **old_environ = environ;

	FETCH_REAL_FN(FILE *, real_popen, "popen");

	cfs_debug(1, "popen(\"%s\", 0%x)", command, mode);

	/* make sure that CFS_ID, LD_PRELOAD and CFS_PARENT_ID are correct */
	environ = (char **)modify_envp(environ);

	/* now invoke the original popen() call */
	FILE *result = real_popen(command, mode);

	/* restore our original environment */
	char **tmp_environ = environ;
	environ = old_environ;
	cleanup_envp(tmp_environ);

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

	cfs_debug(1, "posix_spawn(%p, \"%s\", %p, %p, %p, %p)",
			pid, path, file_actions, attrp, argv, envp);

	/* make sure that CFS_ID, LD_PRELOAD and CFS_PARENT_ID are correct */
	char **new_envp = (char **)modify_envp(environ);
	int result = real_posix_spawn(pid, path, file_actions, attrp, argv, new_envp);

	/* restore our original environment */
	cleanup_envp(new_envp);
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

	cfs_debug(1, "posix_spawnp(%p, \"%s\", %p, %p, %p, %p)",
			pid, file, file_actions, attrp, argv, envp);

	/* make sure that CFS_ID, LD_PRELOAD and CFS_PARENT_ID are correct */
	char **new_envp = (char **)modify_envp(environ);
	int result = real_posix_spawnp(pid, file, file_actions, attrp, argv, new_envp);

	/* restore our original environment */
	cleanup_envp(new_envp);
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

int remove(const char *path)
{
	FETCH_REAL_FN(int, real_remove, "remove");

	cfs_debug(1, "remove(\"%s\")", path);

	// TODO: log the removal of this path
	return real_remove(path);
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

int rename(const char *oldname, const char *newname)
{
	FETCH_REAL_FN(int, real_rename, "rename");

	cfs_debug(1, "rename(\"%s\", \"%s\")", oldname, newname);

	// TODO: log the name change for this file/dir.
	return real_rename(oldname, newname);
}

/*======================================================================
 * Interposed - renameat
 *======================================================================*/

int renameat(int olddirfd, const char *oldpath,
             int newdirfd, const char *newpath)
{
	FETCH_REAL_FN(int, real_renameat, "renameat");

	cfs_debug(1, "renameat(%d, \"%s\", %d, \"%s\")", olddirfd, oldpath,
			newdirfd, newpath);

	// TODO: log the name change for this file/dir.
	return real_renameat(olddirfd, oldpath, newdirfd, newpath);
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

	cfs_debug(1, "rmdir(\"%s\")", dirname);

	// TODO: log the removal of this path
	return real_rmdir(dirname);
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

	cfs_debug(1, "symlink(\"%s\", \"%s\")", oldname, newname);

	// TODO: log the creation of the symlink
	return real_symlink(oldname, newname);
}

/*======================================================================
 * Interposed - symlinkat()
 *======================================================================*/

int
symlinkat(const char *oldpath, int newdirfd, const char *newpath)
{
	FETCH_REAL_FN(int, real_symlinkat, "symlinkat");

	cfs_debug(1, "symlink(\"%s\", %d, \"%s\")", oldpath, newdirfd, newpath);

	// TODO: log the creation of the symlink
	return real_symlinkat(oldpath, newdirfd, newpath);
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

	cfs_debug(1, "system(\"%s\")", command);

	/* make sure that CFS_ID, LD_PRELOAD and CFS_PARENT_ID are correct */
	environ = (char **)modify_envp(environ);

	/* now invoke the original system() call */
	int result = real_system(command);

	/* restore our original environment */
	char **tmp_environ = environ;
	environ = old_environ;
	cleanup_envp(tmp_environ);

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

	cfs_debug(1, "truncate(\"%s\", %d)", filename, length);

	// TODO: log the write of this file.
	return real_truncate(filename, length);
}

/*======================================================================
 * Interposed - truncate64()
 *======================================================================*/

int
truncate64(const char *filename, off64_t length)
{
	FETCH_REAL_FN(int, real_truncate64, "truncate64");

	cfs_debug(1, "truncate64(\"%s\", %d)", filename, length);

	// TODO: log the write of this file.
	return real_truncate64(filename, length);
}

/*======================================================================
 * Interposed - unlink()
 *======================================================================*/

int
unlink(const char *filename)
{
	FETCH_REAL_FN(int, real_unlink, "unlink");

	cfs_debug(1, "unlink(\"%s\")", filename);

	// TODO: log the removal of this file.
	return real_unlink(filename);
}

/*======================================================================
 * Interposed - unlinkat()
 *======================================================================*/

int
unlinkat(int dirfd, const char *pathname, int flags)
{
	FETCH_REAL_FN(int, real_unlinkat, "unlinkat");

	cfs_debug(1, "unlinkat(%d, \"%s\", 0x%x)", dirfd, pathname, flags);

	// TODO: log the removal of this file.
	return real_unlinkat(dirfd, pathname, flags);
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

pid_t vfork(void)
{
	FETCH_REAL_FN(pid_t, real_fork, "fork");

	cfs_debug(1, "vfork()");

	/* call fork, which isn't interposed */
	return real_fork();
}

/*======================================================================*/
