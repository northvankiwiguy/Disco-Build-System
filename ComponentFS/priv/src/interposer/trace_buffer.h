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
 * trace_buffer.h
 *
 * A "trace-buffer" is a shared buffer of bytes, containing cfs trace information.
 * A trace buffer is created by the parent process (the "cfs" process) and is shared with
 * all child processes (typically a full set of Linux processes that perform a build process).
 * The child processes write trace data into the trace buffer at a very high speed, with
 * the parent process periodically dumping the data to disk in the background.
 * This file contains information on the format of the in-memory buffer and how it's
 * managed (which producers writing into the buffer, and the one consumer dumping it to disk).
 * For detail of the actual trace information that's written into the buffer,
 * see trace_file_format.h.
 */

#ifndef TRACE_BUFFER_H_
#define TRACE_BUFFER_H_

#ifndef TRUE
#define TRUE 1
#define FALSE 0
#endif

/*
 * Trace buffers are identified by a unique ID number. Although the
 * end user shouldn't care, these IDs are actually the Linux shared memory ID.
 */
typedef unsigned long trace_buffer_id;

/* Trace buffers are this large (in bytes), which includes a small header */
#define TRACE_BUFFER_SIZE 1048576

/* Trace buffers start with the following header structure */
typedef struct trace_buffer_header {
	unsigned long tb_magic;    	/* must be TB_MAGIC for this trace buffer to be considered valid. */
	unsigned long tb_size;			/* how many bytes are used so far. */
	int	tb_sem_id;					/* ID of the semaphore for controlling access to this trace buffer. */
	int tb_creator_pid;				/* PID of the process that created this trace buffer. */
} trace_buffer_header;

/*
 * In order for the multiple producers (child processes) and the one consumer (parent process) to
 * control access to the trace buffer, we use a couple of different semaphore. The "master" semaphore
 * is held for a very short period of time as the trace buffer is being read or written. The "full"
 * semaphore is used by the producer to indicate to the consumer that the buffer is full.
 */
#define TB_SEM_MASTER 		0		/* The master semaphore for controlling who writes to the trace buffer. */
#define TB_SEM_FULL			1		/* Indicates if the buffer is full and needs to be emptied. */
#define TB_SEM_MAX			2		/* There are this many semaphores */


/* The magic number to identify a valid trace buffer */
#define TB_MAGIC 0x13572468

/* The API functions for managing trace buffers. These are all defined in trace_buffer.c */
extern trace_buffer_id trace_buffer_create(void);
extern int trace_buffer_use_existing(trace_buffer_id id);
extern int trace_buffer_delete(void);
extern void trace_buffer_empty_content(void);
extern int trace_buffer_fetch(void **base_ptr, unsigned long *size_ptr);
extern int trace_buffer_write_string(const char *string);
extern int trace_buffer_write_bytes(void *bytes, unsigned long size);
extern int trace_buffer_write_int(int value);
extern int trace_buffer_write_byte(char value);
extern int trace_buffer_lock(void);
extern int trace_buffer_unlock(void);
extern int trace_buffer_mark_full(int);
extern int trace_buffer_wait_until_full(void);

#endif /* TRACE_BUFFER_H_ */
