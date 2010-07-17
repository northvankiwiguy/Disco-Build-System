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

#define _GNU_SOURCE
#include <dlfcn.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdint.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

/*
 * Fetch a pointer to the real version of the function (if not already defined).
 * We search the list of dynamic libraries to find the next occurrence of this symbol,
 * which should be the "real" version of the function.
 */
#define FETCH_REAL_FN(fn_var, fn_name) \
		static int (*(fn_var))() = NULL; \
		if (!(fn_var)){ \
			(fn_var) = dlsym(RTLD_NEXT, (fn_name)); \
		}


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
	//printf("_init_interposer\n");
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
 * Interposed - fopen()
 *======================================================================*/
// FILE *fopen(const char *filename, const char *opentype)

/*======================================================================
 * Interposed - fopen()
 *======================================================================*/

//FILE *fopen(const char *filename, const char *opentype)
//{
//	FETCH_REAL_FN(real_fopen, "fopen");
//
//	printf("fopen(%s, %s)\n", filename, opentype);
//	FILE *f = real_fopen(filename, opentype);
//	return f;
//}

/*======================================================================
 * Interposed - fopen64()
 *======================================================================*/

// FILE *fopen64(const char *filename, const char *opentype)

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
 * Interposed - open()
 *======================================================================*/

int open(const char *filename, int flags, ...)
{
	FETCH_REAL_FN(real_open, "open");

	// fetch the optional mode argument.
	va_list ap;
	va_start(ap, &flags);
	mode_t mode = va_arg(ap, mode_t);
	va_end(ap);

	printf("open(%s, %d, 0x%x)\n", filename, flags, mode);
	int fd = real_open(filename, flags, mode);
	return fd;
}

/*======================================================================
 * Interposed - open64()
 *======================================================================*/

//int open64(const char *filename, int flags, ...)
//{
//	FETCH_REAL_FN(real_open64, "open64");
//
//	// fetch the optional mode argument.
//	va_list ap;
//	va_start(ap, &flags);
//	mode_t mode = va_arg(ap, mode_t);
//	va_end(ap);
//
//	printf("open64(%s, %d, 0x%x)\n", filename, flags, mode);
//	int fd = real_open64(filename, flags, mode);
//	return fd;
//}

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

