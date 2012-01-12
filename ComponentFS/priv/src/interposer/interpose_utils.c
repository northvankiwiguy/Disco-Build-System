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
 * This file contains a number of utilities functions that are used by the
 * glibc interposer.
 */

#define _GNU_SOURCE
#include <errno.h>
#include <fcntl.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/param.h>
#include <sys/stat.h>

#include "interpose_utils.h"
#include "trace_buffer.h"

/*
 * The current working directory of this process.
 */
static char saved_cwd[PATH_MAX];

/*
 * Debug level. Set this by setting the CFS_DEBUG environment variable
 * (which will be passed from each process to all its children).
 */
static int cfs_debug_level;

/*======================================================================
 * cfs_get_cwd
 *
 * Return the absolute path to this process's current working directory.
 * If use_cache is TRUE, using a cached version of the cwd is acceptable.
 *
 *======================================================================*/

char *cfs_get_cwd(int use_cache)
{

	/*
	 * If we don't already have the cwd cached, we need to ask the OS, and then
	 * save it in a buffer. This function will be called whenever a relative
	 * path is accessed, so it must be fast.
	 */
	if ((saved_cwd[0] == '\0') || !use_cache) {
		if (getcwd(saved_cwd, PATH_MAX) == NULL){
			fprintf(stderr, "Error: cfs couldn't determine current working directory.\n");
			exit(1);
		}
	}
	return saved_cwd;
}

/*======================================================================
 * cfs_malloc
 *
 * Wrapper around the standard malloc function that terminates the
 * program if there's a shortage of memory.
 *======================================================================*/

void *cfs_malloc(size_t size)
{
	void *ptr = malloc(size);
	if (ptr == NULL) {
		fprintf(stderr, "Error: cfs couldn't allocate memory.\n");
		exit(1);
	}

	return ptr;
}

/*======================================================================
 * cfs_get_debug_level
 *
 * Return the current debug level setting.
 *
 *======================================================================*/

int cfs_get_debug_level()
{
	return cfs_debug_level;
}

/*======================================================================
 * cfs_set_debug_level
 *
 * Set the debug level for CFS debugging.
 *======================================================================*/

void cfs_set_debug_level(int level)
{
	if (level < 0){
		level = 0;
	} else if (level > 2){
		level = 2;
	}
	cfs_debug_level = level;
}

/*======================================================================
 * cfs_debug
 *
 * Display debug output. Only display debug output that's within the
 * current debug_level (set via the CFS_DEBUG environment variable).
 *======================================================================*/

void cfs_debug(int level, char *string, ...)
{
	va_list args;

	va_start(args, string);
	if (level <= cfs_debug_level) {
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

void cfs_debug_env(int level, char * const *envp)
{
	if (level <= cfs_debug_level) {
		char * const *ptr = envp;
		fprintf(stderr, "Environment Variables:\n");
		while (*ptr != NULL) {
			fprintf(stderr, "  %s\n", *ptr);
			ptr++;
		}
	}
}

/*======================================================================
 * cfs_get_path_of_dirfd
 *
 * Given a directory file descriptor ("dirfd"), determine the path of the
 * directory it refers to. Append to this the content of the "pathname"
 * string, and write the concatenated result into "result_path".
 *
 * Return 0 on success, or -1 on failure.
 *======================================================================*/

int cfs_get_path_of_dirfd(char *result_path, int dirfd, const char *pathname)
{
	FETCH_REAL_FN(int, real_open, "open");
	FETCH_REAL_FN(int, real_fchdir, "fchdir");

	/* save the current directory */
	int saved_dir = real_open(".", O_RDONLY);
	if (saved_dir == -1) {
		return -1;
	}

	/* change to the "dirfd" directory */
	if (real_fchdir(dirfd) == -1){
		return -1;
	}

	/* fetch the current working directory into the user-supplied buffer */
	if (getcwd(result_path, PATH_MAX) == NULL){
		real_fchdir(saved_dir);
		close(saved_dir);
		return -1;
	}

	/* switch back to the real current directory */
	if (real_fchdir(saved_dir) == -1){
		close(saved_dir);
		return -1;
	}

	/* we're done with the saved directory fd */
	close(saved_dir);

	/* append the "pathname" onto the directory (assuming it's not too long). */
	if (strlen(result_path) + strlen(pathname) + 2 >= PATH_MAX){
		errno = ENAMETOOLONG;
		return -1;
	}

	/* TODO: make this more efficient */
	strcat(result_path, "/");
	strcat(result_path, pathname);
	return 0;
}

/*======================================================================
 * Helper - cfs_isdirectory(char *path)
 *
 * Return TRUE if the "path" refers to a directory, or FALSE if it's
 * not a directory, or if it doesn't exist.
 *
 *======================================================================*/

int cfs_isdirectory(const char *pathname)
{
	struct stat buf;
	if (stat(pathname, &buf) == -1){
		return FALSE;
	}
	return S_ISDIR(buf.st_mode);
}

/*======================================================================*/
