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
 * cfs.c - This is the main entry point for the cfs (Component File System). The
 * cfs implements two main features:
 *
 *   1) Allows monitoring of all file system access, by all sub-processes.
 *   2) Provides a virtual file system where files are located within components,
 *      regardless of how they're physically stored on the underlying file system.
 *
 * If cfs is invoked without any arguments, a new shell is started (based on the
 * user's default shell settings). However, if a command name and arguments
 * are provided, the command is invoked within the cfs environment. When the
 * shell, or the user-specified command terminates, cfs also terminates.
 *
 * The optional arguments are:
 *    -o <trace-file> - write the file system monitoring information to the
 *    trace file. If -o is not specified, write the data to "cfs.trace" by default.
 *    -h - displays help information.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <limits.h>
#include <zlib.h>
#include <sys/types.h>
#include <sys/wait.h>

#include "trace_buffer.h"

/* functions in other source files */
extern int traverse_and_trace_source();

/*======================================================================
 * Global variables
 *======================================================================*/

/* the default file name for trace output */
static char *trace_file_name = "cfs.trace";

/*
 * Should cfs first traverse the file system and report all source files
 * that it finds?
 */
int traverse_source = FALSE;

/*======================================================================
 * parse_args()
 *
 * Parse and process the user-supplied command line arguments.
 * Returns the "argv-style" array of command line arguments to be
 * executed.
 *
 *======================================================================*/

static char **parse_options(int argc, char *argv[])
{
	int opt;
	while ((opt = getopt(argc, argv, "+hro:")) != -1){
		switch (opt){
		case 'o':
			/* -o <trace-file> */
			trace_file_name = optarg;
			break;
		case 'r':
			/* -r - traverse the directory hierarchy to locate source files */
			traverse_source = TRUE;
			break;

		case '?':
		case 'h':
			fprintf(stderr, "Usage:\n");
			fprintf(stderr, "    cfs [-h | -o trace-file] [ command args ...]\n");
			exit(-1);
		default:
			/* ignore */
			break;
		}
	}

	/*
	 * The remainder of the arguments (if any) provide the command line
	 * to execute in the cfs environment.
	 */
	if (optind < argc) {
		/* arguments remain, interpret them as the command to execute and it's arguments */
		return &argv[optind];

	} else {
		/*
		 * Form the argv array for the default shell. This is static so we can return
		 * it to the called.
		 */
		static char *default_shell[2] = {NULL, NULL};

		/* else, detect the user's default shell */
		default_shell[0] = getenv("SHELL");
		if (default_shell[0] == NULL){
			fprintf(stderr, "Error: cfs unable to start - can't detect your default shell.\n");
			exit(-1);
		}
		return default_shell;
	}
}

/*======================================================================
 * main
 *
 * The main entry point for the cfs program. The main purpose here is to
 * create a trace_buffer (shared memory region) that all child processes
 * can write their trace information into, then start the first child process
 * executing. We set the CFS_ID to indicate the shared memory region
 * for the child processes to attach to. We also set the LD_PRELOAD
 * environment variable to force the child processes to use a special
 * interposition library in front of the standard glibc. This interposition
 * library traces and modifies the arguments and return values for each
 * of the glibc library's functions, and causes the child processes to
 * store their trace data in the trace buffer.
 *
 *======================================================================*/

int main(int argc, char *argv[], char *envp[])
{
	/*
	 * Check whether we're already running a cfs command. If so, don't proceed.
	 * The CFS_ID environment is set by the originating cfs process, so if it's
	 * already set to something, we're already running within the cfs environment.
	 */
	if (getenv("CFS_ID") != NULL){
		fprintf(stderr, "Error: already running a cfs command, can't continue.\n");
		exit(-1);
	}

	/*
	 * Parse and process the command line arguments. The return value contains an
	 * argv-style array with the name of the sub-command to execute, and the list
	 * of arguments. If the user didn't explicitly provide a command name, 'program_args'
	 * will refer to the user's default shell.
	 */
	char **program_args = parse_options(argc, argv);

	/* open the trace output file, using compression to keep the output small */
	gzFile trace_file_h = gzopen(trace_file_name, "wb");
	if (!trace_file_h) {
		fprintf(stderr, "Error: unable to create trace file %s: ", trace_file_name);
		perror("");
		exit(-1);
	}

	printf("Starting ComponentFS shell. Writing trace output to %s.\n", trace_file_name);

	/*
	 * Set the LD_PRELOAD variable to refer to our libcfs.so library. This forces the child
	 * process to preload our cfs interpose library whenever it starts executing. Therefore,
	 * any calls to the standard C library are interposed so we can add our own tracing
	 * or transformation features to each library call.
	 */
	char libcfs_path[PATH_MAX];
	sprintf(libcfs_path, "%s/lib/libcfs.so", INSTALL_PATH);
	setenv("LD_PRELOAD", libcfs_path, 1);

	/*
	 * Create a new trace buffer to capture the tracing information provided by each
	 * child process. All child processes attach to this same buffer and act as
	 * producers. This (parent) process acts as a consumer, writing the buffer's content
	 * to disk.
	 */
	int trace_buffer_id = trace_buffer_create();

	/* start the child process */
	switch(fork()) {
	case 0:
		{
			/* child */

			if (traverse_source) {
				printf("Searching for source files... ");
				traverse_and_trace_source();
				printf("done.\n");
			}

			/*
			 * Export the CFS_ID environment variable so that child processes know our
			 * trace buffer ID.
			 */
			char trace_buffer_id_string[strlen("nnnnnnnnnnn")];
			sprintf(trace_buffer_id_string, "%d", trace_buffer_id);
			setenv("CFS_ID", trace_buffer_id_string, 0);

			/* display the command to be executed */
			printf("Executing ");
			char **ptr = program_args;
			while (*ptr != NULL) {
				printf("%s ", *ptr);
				ptr++;
			}
			printf("\n");

			/* execute the sub command that the user selected (or their default shell) */
			execvp(program_args[0], program_args);

			/* if execvp returns, it's an error */
			exit(-1);
		}
	case -1:
		{
			perror("Failed to start a child process.");
			exit(-1);
		}
	default:
		{
			/*
			 * Parent - continuously wait until the trace buffer is full, or until
			 * the child process terminates. When data is available, write it to
			 * disk and reset the buffer to empty. This loop continues indefinitely
			 * until the child process terminates.
			 */
			int status;
			do {
				/*
				 * Wait until a child process (a producer) tells us the buffer is
				 * full. Note that 'status == 0' indicates that the child is still
				 * alive and is ready to produce more data. If 'status == 1', then
				 * the child has terminated, and there might be data in the buffer.
				 * If 'status == -1', something bad happened.
				 */
				status = trace_buffer_wait_until_full();
				if (status == -1) {
					perror("Fatal error while waiting for trace buffer data.");
					exit(-1);
				}

				/*
				 * Find out how much data is in the buffer, then write it to disk.
				 * This step could happen, regardless of whether the child is still
				 * alive, or not.
				 */
				void *ptr;
				unsigned long size;
				trace_buffer_fetch(&ptr, &size);
				if (size != 0) {
					if (gzwrite(trace_file_h, ptr, size) <= 0){
						perror("Fatal error while writing trace data to file");
						exit(-1);
					}
				}

				/* empty the buffer and signal the child to continue */
				trace_buffer_empty_content();
				if (trace_buffer_mark_full(FALSE) != 0){
					perror("Fatal error while restart child process after writing data to disk.");
					exit(-1);
				}

				/* continue until the child process dies */
			} while (status == 0);
		}
	}

	/* we're done - the child has terminated, and the resources should be deallocated. */
	printf("ComponentFS terminated\n");
	gzclose(trace_file_h);

	if (trace_buffer_delete() != 0){
		perror("Fatal error removing the trace buffer");
		exit(-1);
	}
	return 0;
}

/*======================================================================*/
