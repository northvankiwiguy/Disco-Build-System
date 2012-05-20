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

#include <limits.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifndef TRUE
#define TRUE 1
#endif
#ifndef FALSE
#define FALSE 0
#endif

/*
 * This file contains functions for normalizing file names, as well as filtering
 * out files that we don't care to monitor.
 */

/*======================================================================
 * _cfs_combine_paths - Give a parent path, and an extra portion of
 * a path, combine and normalize the path into a single return buffer.
 *
 * "Combining" means:
 *   - If 'extra_path' is relative, join the two paths together.
 *   - If 'extra_path' is absolute, the parent is considered to be '/'
 *
 * "Normalizing" means we remove ".", ".." from paths, and follow
 * symlinks to find the absolute path they refer to.
 *
 * - parent_path - The parent path to which we're concatenating onto
 * - extra_path - The extra directories/files we're appending
 * - combined-path - Pointer to a buffer (of PATH_MAX size) into which
 *     we'll write the combined path.
 *
 * - returns:
 *     0 on success, or -1 on failure and set errno to one of:
 *
 *     EACCES Read or search permission was denied for a component of the path prefix.
 *     EIO    An I/O error occurred while reading from the file system.
 *     ELOOP  Too many symbolic links were encountered in normalizing the pathname.
 *     ENAMETOOLONG A component of a pathname exceeded NAME_MAX characters,
 *		   or an entire pathname exceeded PATH_MAX characters.
 *	   ENOENT A component of the path (other than the last part) is missing.
 *     ENOTDIR A component of the path prefix is not a directory
 *     EINVAL Any of the input pointers were NULL
 *
 *======================================================================*/

int _cfs_combine_paths(
		char const *parent_path,
		char const *extra_path,
		char *combined_path)
{
	/* for holding our temporary combined path */
	char temp_buffer[PATH_MAX];

	// printf("_cfs_combine_paths(%s, %s)\n", parent_path, extra_path);

	/* validate inputs */
	if ((parent_path == NULL) ||
			(extra_path == NULL) ||
			(combined_path == NULL)){
		errno = EINVAL;
		return -1;
	}

	/*
	 * If extra_path is absolute, we don't care about parent_path. Just
	 * reset it to the empty string.
	 */
	if (extra_path[0] == '/') {
		parent_path = "";
	}

	/*
	 * Now do an optimized copy. This is like strncpy, but much faster.
	 */
	char *out_ptr = temp_buffer;					/* starting point */
	char *end_ptr = &temp_buffer[PATH_MAX-1];		/* don't go beyond here */

	/* copy the parent_path, taking care not to overrun the buffer */
	char const *src_ptr = parent_path;
	while (*src_ptr != '\0'){
		*out_ptr++ = *src_ptr++;
		if (out_ptr == end_ptr){
			errno = ENAMETOOLONG;
			return -1;
		}
	}

	/* insert a '/' separator (if needed) */
	if ((out_ptr == temp_buffer) || (out_ptr[-1] != '/')) {
		*out_ptr++ = '/';
	}
	if (out_ptr == end_ptr){
		errno = ENAMETOOLONG;
		return -1;
	}

	/* copy extra_path, taking care not to overrun the buffer */
	src_ptr = extra_path;
	while (*src_ptr != '\0'){
		*out_ptr++ = *src_ptr++;
		if (out_ptr == end_ptr){
			errno = ENAMETOOLONG;
			return -1;
		}
	}

	/* All is good, insert a terminating \0 (this won't overflow the buffer) */
	*out_ptr = '\0';

	/* Finally, normalize with realpath(), and pass on any error codes */
	if (realpath(temp_buffer, combined_path) != NULL){
		// printf("=> %s\n", combined_path);
		return 0;

	} else {

		/*
		 * If real_path failed with the ENOENT error code, it simply means that
		 * the file/directory didn't exist. This is OK for our purposes, since we
		 * want to be able to compute the path to a non-existent file. However,
		 * the important factor is that the file's parent directory exists.
		 */
		if (errno == ENOENT) {

			/* rewind temp_buffer, up to the parent directory - ignoring any trailing '/' */
			if (*(--out_ptr) == '/') {
				out_ptr--;
			}
			while ((out_ptr > temp_buffer) && (*out_ptr != '/')) {
				out_ptr--;
			}

			/*
			 * If we found the '/' at the very start of the string, we're done. We know that
			 * '/' (root) is a valid path, and whatever remains in temp_buffer will be the
			 * not-yet-existing path that we'll add onto that. This is good, so return 0.
			 */
			if (out_ptr == temp_buffer) {
				/* combined_path already contains the file */
				return 0;
			}

			/*
			 * Now we must validate the parent directory (using real_path), and tack on the
			 * remaining (non-existing path) onto the end of it (assuming it was successful).
			 */
			*out_ptr = '\0';
			if (realpath(temp_buffer, combined_path) == NULL){
				/* no, still invalid */
				return -1;
			}
			*out_ptr = '/';
			strcat(combined_path, out_ptr);
			return 0;
		}
		return -1;
	}
}

/*======================================================================
 *
 * _cfs_basename
 *
 * A thread-safe implementation of the basename() function that stores
 * output in a user-allocated buffer (as opposed to returning static
 * storage, making it non-threadsafe).
 *
 * - orig_path - the path for which we're computing the basename.
 * - base_path - the buffer into which the basename is stored (must be
 *               at least as many bytes as orig_path).
 *
 *======================================================================*/

void _cfs_basename(const char *orig_path, char *base_path)
{
	const char *src_ptr = orig_path;
	char *dest_ptr = base_path;
	char *possible_last_slash = NULL;
	char *definite_last_slash = base_path;
	char ch;
	int last_was_slash = FALSE;

	/*
	 * Traverse the orig_path (as we copy it), taking note of the last
	 * '/' character we see that potentially marks the end of the basename
	 * string. We need to handle situations such as multiple '/' in a row,
	 * as well as trailing '/' on the end of a path.
	 */
	while ((ch = *src_ptr++) != '\0') {

		if (ch == '/') {
			/*
			 * Assuming the previous char wasn't also a /, move our
			 * last_slash pointer.
			 */
			if (!last_was_slash) {
				possible_last_slash = dest_ptr;
			}
			last_was_slash = TRUE;

		} else {

			/*
			 * we're now looking at a non-slash character, which means that
			 * our "possible" last slash is now definite. For example, if
			 * we see "/a/b/c/d" then the '/' after the 'c' has now become
			 * definite, whereas with "/a/b/c/", the '/' after the 'b' is
			 * still definite.
			 */
			if (last_was_slash) {
				definite_last_slash = possible_last_slash;
			}

			/* no, not a /, this is a regular character to be copied */
			last_was_slash = FALSE;
		}
		*dest_ptr++ = ch;
	}

	/*
	 * Now back up to the last / and mark it as '\0' to terminate
	 * the string. However, if the last / was at the first position
	 * in the path, we must leave it there. That is, basename("/") => "/".
	 */
	if (definite_last_slash == base_path) {
		base_path[0] = '/';
		base_path[1] = '\0';
	} else {
		*definite_last_slash = '\0';
	}
}

/*======================================================================*/


