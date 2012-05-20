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
 * trace_file_format.h
 *
 * Contains a full description of the content of a trace file. The cfs (Capture File
 * System) generates a trace file in this format. Other programs can read and interpret
 * the trace file using this format information.
 */

#ifndef TRACE_FILE_FORMAT_H_
#define TRACE_FILE_FORMAT_H_

/*
 * Each trace file starts with the following short header
 */
typedef struct trace_file_header {
	unsigned int tf_magic;		/* magic number to identify this as a trace file */
	unsigned int tf_version;	/* file format version - for ensuring file/tool compatibility */
} trace_file_header;

/* the trace file magic number */
#define TF_MAGIC	0xBEEFFEED

/* the current trace file version - to be updated whenever the file format changes */
#define TF_VERSION	1

/*
 * The remainder of the trace file is free-form (can't be described with structs).
 * Each entry starts with a single byte to state the action. Note that process numbers
 * are not the same as Unix process IDs, since they're never intended to be reused
 * (therefore only 4 billion processes can be mentioned in a trace file).
 */

/*
 * TRACE_FILE_REGISTER - register the existence of a file on the file system.
 * 		- 1 byte : TRACE_FILE_REGISTER
 *      - 4 bytes : process number (typically 0)
 *      - nul-terminated string : the file's absolute path name.
 */
#define TRACE_FILE_REGISTER 		1

/*
 * TRACE_FILE_WRITE - a file has been opened for writing.
 * 		- 1 byte : TRACE_FILE_WRITE
 * 		- 4 bytes : process number (doing the writing)
 *      - nul-terminated string : the file's absolute path name.
 */
#define TRACE_FILE_WRITE 			2

/*
 * TRACE_FILE_READ - a file has been opened for reading.
 * 		- 1 byte : TRACE_FILE_READ
 * 		- 4 bytes : process number (doing the reading)
 *      - nul-terminated string : the file's absolute path name.
 */
#define TRACE_FILE_READ 			3

/*
 * TRACE_FILE_MODIFY - a file has been opened for reading or writing.
 * 		- 1 byte : TRACE_FILE_MODIFY
 * 		- 4 bytes : process number (doing the reading)
 *      - nul-terminated string : the file's absolute path name.
 */
#define TRACE_FILE_MODIFY 			4

/*
 * TRACE_FILE_DELETE - a file has been deleted.
 * 		- 1 byte : TRACE_FILE_DELETE
 * 		- 4 bytes : process number (doing the remove)
 *      - nul-terminated string : the file's absolute path name.
 */
#define TRACE_FILE_DELETE 			5

/*
 * TRACE_FILE_RENAME - a file has been renamed.
 * 		- 1 byte : TRACE_FILE_RENAME
 * 		- 4 bytes : process number (doing the rename)
 *      - nul-terminated string : the file's original absolute path name.
 *      - nul-terminated string : the file's new absolute path name.
 */
#define TRACE_FILE_RENAME			6

/*
 * TRACE_FILE_NEW_LINK - a file has been hard/soft linked to a new location.
 * 		- 1 byte : TRACE_FILE_NEW_LINK
 * 		- 4 bytes : process number (creating the link)
 *      - nul-terminated string : the file's source absolute path name.
 *      - nul-terminated string : the absolute path name of the link.
 */
#define TRACE_FILE_NEW_LINK			7

/*
 * TRACE_FILE_NEW_PROGRAM - a new program has been started
 * 		- 1 byte : TRACE_FILE_NEW_PROGRAM
 * 		- 4 bytes : process number of the new program
 *      - 4 bytes : process number of the parent process
 *      - a nul-terminated string: the process's current working directory.
 *      - 4 bytes : the number of NUL-terminated argument strings.
 *      - sequence of nul-terminated strings : ARGV[0] .. [ARGV[n-1]]
 *      - sequence of nul-terminated strings : ENVP[0] .. [ENVP[n-1]]
 *      - a final empty string, to indicate the end of environment variables.
 */
#define TRACE_FILE_NEW_PROGRAM 		8

/*
 * TRACE_DIR_WRITE - a directory has been opened for writing.
 * 		- 1 byte : TRACE_DIR_WRITE
 * 		- 4 bytes : process number (doing the writing)
 *      - nul-terminated string : the directory's absolute path name.
 */
#define TRACE_DIR_WRITE 			9

/*
 * TRACE_DIR_READ - a directory has been opened for reading.
 * 		- 1 byte : TRACE_DIR_READ
 * 		- 4 bytes : process number (doing the reading)
 *      - nul-terminated string : the directory's absolute path name.
 */
#define TRACE_DIR_READ 				10

/*
 * TRACE_DIR_MODIFY - a directory has been opened for reading or writing.
 * 		- 1 byte : TRACE_DIR_MODIFY
 * 		- 4 bytes : process number (doing the reading)
 *      - nul-terminated string : the directory's absolute path name.
 */
#define TRACE_DIR_MODIFY 			11

/*
 * TRACE_DIR_DELETE - a directory has been deleted.
 * 		- 1 byte : TRACE_DIR_DELETE
 * 		- 4 bytes : process number (doing the remove)
 *      - nul-terminated string : the file's absolute path name.
 */
#define TRACE_DIR_DELETE 			12

#endif /* TRACE_FILE_FORMAT_H_ */
