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
 *     0 on success
 *     EACCES Read or search permission was denied for a component of the path prefix.
 *     EIO    An I/O error occurred while reading from the file system.
 *     ELOOP  Too many symbolic links were encountered in normalizing the pathname.
 *     ENAMETOOLONG A component of a pathname exceeded NAME_MAX characters,
 *		   or an entire pathname exceeded PATH_MAX characters.
 *     ENOENT The named file does not exist.
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

	/* validate inputs */
	if ((parent_path == NULL) ||
			(extra_path == NULL) ||
			(combined_path == NULL)){
		return EINVAL;
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
			return ENAMETOOLONG;
		}
	}

	/* insert a '/' separator */
	*out_ptr++ = '/';
	if (out_ptr == end_ptr){
		return ENAMETOOLONG;
	}

	/* copy extra_path, taking care not to overrun the buffer */
	src_ptr = extra_path;
	while (*src_ptr != '\0'){
		*out_ptr++ = *src_ptr++;
		if (out_ptr == end_ptr){
			return ENAMETOOLONG;
		}
	}

	/* All is good, insert a terminating \0 (this won't overflow the buffer) */
	*out_ptr = '\0';

	/* Finally, normalize with realpath(), and pass on any error codes */
	if (realpath(temp_buffer, combined_path) == NULL){
		return errno;
	} else {
		return 0;
	}
}

/*======================================================================*/


