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
 * Contains a full description of the content of a trace file. The cfs (Component File
 * System) generates a trace file in this format. Other programs can read and interpret
 * the trace file using this format information.
 */

#ifndef TRACE_FILE_FORMAT_H_
#define TRACE_FILE_FORMAT_H_

// TODO: define a header, including a magic number and a version number.

/*
 * Entries in the trace buffer start with a single byte to state what the action being executed is.
 */
#define TRACE_FILE_WRITE 1  		/* followed by a single string giving the file name */
#define TRACE_FILE_READ 2   		/* followed by a single string giving the file name */
#define TRACE_FILE_REMOVE 3 		/* followed by a single string giving the file name */

#endif /* TRACE_FILE_FORMAT_H_ */
