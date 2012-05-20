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

#include <fcntl.h>
#include <limits.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>

#include "test_helpers.h"

/*======================================================================*
 * th_malloc()
 *
 * Return the full path name of the current working directory into a newly
 * allocated malloc buffer. It's the caller responsibility to free the
 * memory once they're done with it.
 *
 * Return the pointer to the malloc'd buffer, or NULL on error.
 *======================================================================*/

char *
th_getcwd()
{
	char *new_ptr = malloc(PATH_MAX);
	if (new_ptr == NULL) {
		return NULL;
	}

	return getcwd(new_ptr, PATH_MAX);
}

/*======================================================================*
 * th_create_empty_file(char *name, int perms)
 *
 * Create a new empty file, with the specified name, and the specified
 * permission bits.
 *
 * Returns TRUE on success, else FALSE
 *======================================================================*/

int
th_create_empty_file(char *name, int perms)
{
	int fd = open(name, O_CREAT|O_RDWR, perms);
	if (fd == -1) {
		return FALSE;
	}
	close(fd);
	return TRUE;
}

/*======================================================================*
 * th_create_nonempty_file(char *name, int perms, char *content)
 *
 * Create a new file, with the specified name, and the specified
 * permission bits. Insert the pre-defined content into the file.
 *
 * Returns TRUE on success, else FALSE
 *======================================================================*/

int
th_create_nonempty_file(char *name, int perms, char *content)
{
	int fd = open(name, O_CREAT|O_RDWR, perms);
	if (fd == -1) {
		return FALSE;
	}
	write(fd, content, strlen(content));
	close(fd);
	return TRUE;
}

/*======================================================================*
 * th_get_file_perms(char *name)
 *
 * Return the permission bits for the specified file, or -1 on error.
 *
 *======================================================================*/

int
th_get_file_perms(char *name)
{
	struct stat buf;

    if (stat(name, &buf) != 0){
    	return -1;
    }

    return buf.st_mode & 0777;
}

/*======================================================================*
 * th_get_file_size(char *name)
 *
 * Return the size of the specified file, or -1 on error.
 *
 *======================================================================*/

int
th_get_file_size(char *name)
{
	struct stat buf;

    if (stat(name, &buf) != 0){
    	return -1;
    }

    return buf.st_size;
}

/*======================================================================*/
