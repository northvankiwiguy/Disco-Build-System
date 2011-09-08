/*******************************************************************************
 * Copyright (c) 2010 Arapiki Solutions Inc.
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
 * loads this file (as part of the libcfs.so library) ahead of the real glibc.so file.
 * Therefore, any user-level program that calls a library function (e.g. read, write, open, etc)
 * will end up executing in this file. This allows us to perform additional tracing and/or
 * manipulation of parameters and return values to implement the CFS (Component File System).
 * After performing this extra work, each function proceeds to invoke the "real" library
 * function from the glibc library.
 */

#define _GNU_SOURCE
#include <dlfcn.h>
#include <fcntl.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/param.h>
#include <sys/stat.h>

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


/*======================================================================
 * _init_interposer()
 *
 * When this dynamic library is first loaded, the _init_interposer function is called
 * as a constructor. This is where we allocate the shared memory segment for cfs and
 * perform any other start-up tasks.
 *======================================================================*/

void _init_interposer() __attribute__ ((constructor));

void _init_interposer()
{
	static char argv_and_envp[NCARGS];

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

/*======================================================================
 * Interposed - access()
 *======================================================================*/

// int access(const char *filename, int how)

/*======================================================================
 * Interposed - canonicalize_file_name()
 *======================================================================*/
// char *canonicalize_file_name (const char *name)

/*======================================================================
 * Interposed - chdir()
 *======================================================================*/
// int chdir(const char *filename)

/*======================================================================
 * Interposed - chmod()
 *======================================================================*/
// int chmod(const char *filename, mode_t mode)

/*======================================================================
 * Interposed - chown()
 *======================================================================*/
// int chown(const char *filename, uid_t owner, gid_t group)

/*======================================================================
 * Interposed - execl()
 *======================================================================*/
// int execl(const char *filename, const char *arg0, ...)

/*======================================================================
 * Interposed - execle()
 *======================================================================*/
// int execle(const char *filename, const char *arg0, char *const env[], ...)

/*======================================================================
 * Interposed - execlp()
 *======================================================================*/
// int execlp(const char *filename, const char *arg0, ...)

/*======================================================================
 * Interposed - execv()
 *======================================================================*/
// int execv(const char *filename, char *const argv[])

/*======================================================================
 * Interposed - execve()
 *======================================================================*/
// int execve(const char *filename, char *const argv[], char *const env[])

/*======================================================================
 * Interposed - execvp()
 *======================================================================*/
// int execvp(const char *filename, char *const argv[])

/*======================================================================
 * Interposed - exit()
 *======================================================================*/
// void exit(int status)

/*======================================================================
 * Interposed - fchdir()
 *======================================================================*/
// int fchdir(int filedes)

/*======================================================================
 * fopen_common()
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
}
/*======================================================================
 * Interposed - fopen()
 *======================================================================*/

FILE *fopen(const char *filename, const char *opentype)
{
	FETCH_REAL_FN(FILE *, real_fopen, "fopen");

	//printf("fopen(%s, %s)\n", filename, opentype);
	fopen_common(filename, opentype);
	FILE *f = real_fopen(filename, opentype);
	return f;
}

/*======================================================================
 * Interposed - fopen64()
 *======================================================================*/

FILE *fopen64(const char *filename, const char *opentype)
{
	FETCH_REAL_FN(FILE *, real_fopen64, "fopen64");

	//printf("fopen64(%s, %s)\n", filename, opentype);
	fopen_common(filename, opentype);
	FILE *f = real_fopen64(filename, opentype);
	return f;
}

/*======================================================================
 * Interposed - freopen()
 *======================================================================*/
// FILE *freopen(const char *filename, const char *opentype, FILE *stream)

/*======================================================================
 * Interposed - freopen64
 *======================================================================*/
// FILE *freopen64(const char *filename, const char *opentype, FILE *stream)

/*======================================================================
 * Interposed - ftw()
 *======================================================================*/
// int ftw(const char *filename, __ftw_func_t func, int descriptors)

/*======================================================================
 * Interposed - ftw64
 *======================================================================*/
// int ftw64(const char *filename, __ftw64_func_t func, int descriptors)

/*======================================================================
 * Interposed - get_current_dir_name()
 *======================================================================*/
// char *get_current_dir_name (void)

/*======================================================================
 * Interposed - getcwd()
 *======================================================================*/
// char *getcwd(char *buffer, size_t size)

/*======================================================================
 * Interposed - getwd()
 *======================================================================*/
// char *getwd (char *buffer)

/*======================================================================
 * Interposed - link()
 *======================================================================*/
// int link(const char *oldname, const char *newname)

/*======================================================================
 * Interposed - lstat()
 *======================================================================*/
// int lstat(const char *filename, struct stat *buf)

/*======================================================================
 * Interposed - lstat64()
 *======================================================================*/
// int lstat64(const char *filename, struct stat64 *buf)

/*======================================================================
 * Interposed - lutimes()
 *======================================================================*/
// int lutimes(const char *filename, struct timeval tvp[2])

/*======================================================================
 * Interposed - mkdir()
 *======================================================================*/
// int mkdir(const char *filename, mode_t mode)

/*======================================================================
 * Interposed - mkdtemp()
 *======================================================================*/
// char * mkdtemp(char *template)

/*======================================================================
 * Interposed - mkfifo()
 *======================================================================*/
// int mkfifo(const char *filename, mode_t mode)

/*======================================================================
 * Interposed - mknod()
 *======================================================================*/
// int mknod(const char *filename, int mode, int dev)

/*======================================================================
 * Interposed - mkstemp()
 *======================================================================*/
// int mkstemp(char *template)

/*======================================================================
 * Interposed - mktemp()
 *======================================================================*/
// char * mktemp(char *template)

/*======================================================================
 * Interposed - nftw()
 *======================================================================*/
// int nftw(const char *filename, __nftw_func_t func, int descriptors, int flag)

/*======================================================================
 * Interposed - nftw64()
 *======================================================================*/
// int nftw64(const char *filename, __nftw64_func_t func, int descriptors, int flag)

/*======================================================================
 * open_common()
 *
 * A common function for tracing detail of open, open64 or related library
 * calls.
 *======================================================================*/

static void open_common(const char *filename, int flags)
{
	/* if there's a trace buffer, write the file name to it */
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

	//printf("open(%s, %d, 0x%x)\n", filename, flags, mode);
	open_common(filename, flags);
	int fd = real_open(filename, flags, mode);
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

	//printf("open64(%s, %d, 0x%x)\n", filename, flags, mode);
	open_common(filename, flags);
	int fd = real_open64(filename, flags, mode);
	return fd;
}

/*======================================================================
 * Interposed - opendir()
 *======================================================================*/

// DIR *opendir(const char *dirname)

/*======================================================================
 * Interposed - popen()
 *======================================================================*/
// FILE *popen(const char *command, const char *mode)

/*======================================================================
 * Interposed - readdir()
 *======================================================================*/
// struct dirent *readdir(DIR *dirstream)

/*======================================================================
 * Interposed - readdir64()
 *======================================================================*/
// struct dirent64 *readdir64(DIR *dirstream)

/*======================================================================
 * Interposed - readdir_r()
 *======================================================================*/
// int readdir_r(DIR *dirstream, struct dirent *entry, struct dirent **result)

/*======================================================================
 * Interposed - readdir64_r()
 *======================================================================*/
// int readdir64_r(DIR *dirstream, struct dirent64 *entry, struct dirent64 **result)

/*======================================================================
 * Interposed - readlink()
 *======================================================================*/
// int readlink(const char *filename, char *buffer, size_t size)

/*======================================================================
 * Interposed - realpath()
 *======================================================================*/
// char *realpath(const char *restrict name, char *restrict resolved)

/*======================================================================
 * Interposed - remove()
 *======================================================================*/
// int remove(const char *filename)

/*======================================================================
 * Interposed - rename()
 *======================================================================*/
// int rename(const char *oldname, const char *newname)

/*======================================================================
 * Interposed - rewinddir()
 *======================================================================*/
// void rewinddir(DIR *dirstream)

/*======================================================================
 * Interposed - rmdir()
 *======================================================================*/
// int rmdir(const char *filename)

/*======================================================================
 * Interposed - scandir()
 *======================================================================*/
// int scandir(const char *dir, struct dirent ***namelist,
//	int (*selector) (const struct dirent *),
//	int (*cmp) (const void *, const void *))

/*======================================================================
 * Interposed - scandir64()
 *======================================================================*/
// int scandir64(const char *dir, struct dirent64 ***namelist,
//    int (*selector) (const struct dirent64 *),
//    int (*cmp) (const void *, const void *))

/*======================================================================
 * Interposed - seekdir()
 *======================================================================*/
// void seekdir(DIR *dirstream, long int pos)

/*======================================================================
 * Interposed - stat()
 *======================================================================*/
// int stat(const char *filename, struct stat *buf)

/*======================================================================
 * Interposed - stat64()
 *======================================================================*/
// int stat64(const char *filename, struct stat64 *buf)

/*======================================================================
 * Interposed - symlink()
 *======================================================================*/
// int symlink(const char *oldname, const char *newname)

/*======================================================================
 * Interposed - telldir()
 *======================================================================*/
// long int telldir(DIR *dirstream)

/*======================================================================
 * Interposed - tempnam()
 *======================================================================*/
// char * tempnam(const char *dir, const char *prefix)

/*======================================================================
 * Interposed - truncate()
 *======================================================================*/
// int truncate(const char *filename, off_t length)

/*======================================================================
 * Interposed - truncate64()
 *======================================================================*/
// int truncate64(const char *name, off64_t length)

/*======================================================================
 * Interposed - unlink()
 *======================================================================*/
// int unlink(const char *filename)

/*======================================================================
 * Interposed - utime()
 *======================================================================*/
// int utime(const char *filename, const struct utimbuf *times)

/*======================================================================
 * Interposed - utimes()
 *======================================================================*/
// int utimes(const char *filename, struct timeval tvp[2])


/*======================================================================*/

