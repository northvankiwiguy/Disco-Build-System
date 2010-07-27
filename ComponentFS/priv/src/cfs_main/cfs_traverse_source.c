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
 * The functions in this file are called into action if the -r option is given to
 * cfs. That is, we scan the current directory (and all subdirectories) to locate
 * all files that already reside on the file system, even before any compilation
 * starts. These files are therefore considered to be "source files".
 *
 * The main entry point is traverse_and_trace_source()
 */

#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <ftw.h>

#include "trace_buffer.h"
#include "trace_file_format.h"

/*======================================================================
 * process_file()
 *
 * For each file within the current directory, send a trace log message
 * to the trace buffer. We only do this for regular files, rather than
 * for directories and symlinks. This function is only used as a callback
 * function for traverse_and_trace_source();
 *======================================================================*/

static int process_file(const char *fpath, const struct stat *sb, int typeflag)
{
	switch (typeflag) {
	case FTW_F:
		trace_buffer_write_byte(TRACE_FILE_REGISTER);
		trace_buffer_write_string(fpath);
		break;
	default:
		;
		/* directories, symlinks etc, are ignored */
	}
	return 0;
}

/*======================================================================
 * traverse_and_trace_source()
 *======================================================================*/

int traverse_and_trace_source()
{
	char cwd[PATH_MAX];

	/* fetch the current working directory */
	if (getcwd(cwd, PATH_MAX) == NULL){
		fprintf(stderr, "Error: unable to determine current working directory.\n");
		exit(-1);
	}

	/* acquire the trace buffer lock */
	if (trace_buffer_lock() != 0){
		fprintf(stderr, "Error: unable to obtain trace buffer lock.\n");
		exit(-1);
	}

	/*
	 * Walk the current subtree and call process_file() for each file/dir.
	 * Use no more than 20 file descriptors.
	 */
	ftw(cwd, process_file, 20);

	if (trace_buffer_unlock() != 0){
		fprintf(stderr, "Error: unable to release the trace buffer lock.\n");
		exit(-1);
	}

	return 0;
}

/*======================================================================*/

