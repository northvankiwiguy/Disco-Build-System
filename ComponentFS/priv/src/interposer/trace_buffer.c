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
 * trace_buffer.c
 *
 * This is an abstract data type that implements a shared producer/consumer buffer.
 * There can be multiple producers that write into a shared in-memory buffer (implemented
 * as a Linux shared-memory segment), but only one consumer. Each of the producers
 * writes trace data into the trace buffer, whereas the consumer periodically saves the
 * buffer content to a disk file, then resets the buffer so that more data can be written
 * to it.
 *
 * It would normally be the parent process (cfs) that creates the trace buffer, then passes
 * the buffer's ID in the CFS_ID environment variables. When child processes start executing,
 * they use the definition of CFS_ID to determine which Linux shared memory segment to attach
 * to.
 *
 * To control access to the trace buffer, a couple of Linux semaphores are used. For more
 * information on the structure of the trace buffer, see trace_buffer.h.
 */

#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <unistd.h>
#include <sys/ipc.h>
#include <sys/types.h>
#include <sys/sem.h>
#include <sys/shm.h>

#include "trace_buffer.h"

/*
 * If there's a trace buffer attached to the current process, these variables
 * refer to it. All other parameters (size, locking etc) are stored within the trace buffer
 * itself. The 'our_trace_buffer_id' variable is the Linux ID number for the shared memory
 * segment. The 'trace_buffer' pointer is the in-memory address of the buffer, but only for
 * this process (it could differ between processes).
 */
trace_buffer_id our_trace_buffer_id = -1;
trace_buffer_header *trace_buffer = NULL;

/*======================================================================
 * sigchild_handler
 *
 * This function is called by the Linux signal handler mechanism if
 * a child process terminates. We set the child_terminated variable
 * to TRUE to indicate that the parent's child has died. This is
 * important to know, otherwise the parent may wait indefinitely for
 * more trace data to be written to the trace buffer.
 *======================================================================*/

static int child_terminated = FALSE;

void sigchild_handler(int n)
{
	child_terminated = TRUE;
}

/*======================================================================
 * trace_buffer_create()
 *
 * Create a new trace_buffer and return a trace buffer ID. If the
 * trace buffer can't be created for some reason, return -1. Only one
 * trace buffer can be used at one time, so also return -1 if a buffer
 * already exists.
 *======================================================================*/

trace_buffer_id trace_buffer_create(void)
{
	int shm_id = -1, sem_id = -1;

	/* is there already a trace buffer attach to this process? */
	if (trace_buffer != NULL) {
		return -1;
	}

	/* allocate a new Linux shared memory segment */
	shm_id = shmget(IPC_PRIVATE, TRACE_BUFFER_SIZE, 0600);
	if (shm_id == -1) {
		goto error;
	}

	/* allocate a new set of semaphores for controlling access to the buffer */
	sem_id = semget(IPC_PRIVATE, TB_SEM_MAX, IPC_CREAT|0600);
	if (sem_id == -1) {
		goto error;
	}

	/*
	 * Initialize the TB_SEM_MASTER semaphore to 1, meaning that only one process can be
	 * using the trace buffer at one time. As a process is accessing the trace buffer,
	 * this semaphore is set to 0. If it's already 0, the process must block until it
	 * returns to value 1.
	 */
	semctl(sem_id, TB_SEM_MASTER, SETVAL, 1);

	/*
	 * Initialize the TB_SEM_FULL semaphore to 1 to indicate that it's not yet full
	 * and the consumer process should block until it is. If this semaphore is 0,
	 * this indicates that the consumer is currently writing data to the disk and
	 * the producer processes should block.
	 */
	semctl(sem_id, TB_SEM_FULL, SETVAL, 1);

	/*
	 * Initialize the TB_SEM_LOG_FILE semaphore to 1, meaning that only one process can
	 * be writing to the log file at one time. The avoids have different processes
	 * stomp on each other's log messages.
	 */
	semctl(sem_id, TB_SEM_LOG_FILE, SETVAL, 1);

	/*
	 * Attach this memory segment to our current process. We don't know exactly where
	 * in memory it'll end up, so it's not meaningful to store absolute pointers inside
	 * the buffer.
	 */
	trace_buffer = (trace_buffer_header *)shmat(shm_id, NULL, 0);
	if (trace_buffer == (trace_buffer_header *)-1){
		trace_buffer = NULL;
		goto error;
	}

	/*
	 * Configure this shared memory segment so that when the last process detaches
	 * from it (probably by exiting), the segment will be removed from memory. If
	 * we don't do this, it'll stay around in the system indefinitely.
	 */
	if (shmctl(shm_id, IPC_RMID, NULL) == -1){
		goto error;
	}

	/* initialize the header of the newly created trace buffer */
	trace_buffer->tb_magic = TB_MAGIC;        /* for identification purposes */
	trace_buffer->tb_size = 0;                /* current no data has been written to it */
	trace_buffer->tb_sem_id = sem_id;         /* the ID of our semaphore set */
	trace_buffer->tb_creator_pid = getpid(); /* the PID of the process that created the buffer */
	trace_buffer->tb_process_number = 1;	  /* each new process must have a unique process number */

	/* remember the shared memory ID for later */
	our_trace_buffer_id = shm_id;

	/*
	 * Make sure we catch SIGCHLD so that trace_buffer_wait_until_full() will
	 * unblock if the child process dies while the parent is waiting. We also
	 * set the child_terminated variable inside sigchild_handler() just in
	 * case the child dies immediately before trace_buffer_wait_until_full()
	 * is called.
	 */
	signal(SIGCHLD, sigchild_handler);
	child_terminated = 0;  /* reset everytime, to make sure unit tests work */

	/* return the ID. To the end user, this is just an ID number with no other meaning */
	return shm_id;

error:
	/*
	 * Something bad happened. Deallocate everything that was previously allocated.
	 */
	if (shm_id != -1) {
		shmctl(shm_id, IPC_RMID, NULL);
	}
	if (sem_id != -1) {

	}
	if (trace_buffer != NULL) {
		shmdt(trace_buffer);
	}

	trace_buffer = NULL;
	our_trace_buffer_id = -1;
	return -1;
}

/*======================================================================
 * trace_buffer_use_existing()
 *
 * Given the ID of an existing trace buffer (created by some other
 * process), attach it into our own address space. Return 0 on success,
 * or -1 on failure.
 *======================================================================*/

int trace_buffer_use_existing(trace_buffer_id id)
{
	/* attempt to attach to the Linux shared memory segment */
	trace_buffer = (trace_buffer_header *)shmat(id, NULL, 0);

	/*
	 * Did we succeed on connecting to the shared memory?
	 * Is the magic number on the trace buffer correct?
	 */
	if ((trace_buffer == (trace_buffer_header *)-1) ||
			(trace_buffer->tb_magic != TB_MAGIC)){
		trace_buffer = NULL;
		our_trace_buffer_id = -1;
		return -1;
	}

	/* all OK */
	our_trace_buffer_id = id;
	return 0;
}

/*======================================================================
 * trace_buffer_delete()
 *
 * Completely remove the trace buffer. This should only be done by
 * the original parent process that created the trace buffer in the
 * first place. The trace buffer may still exist in other processes,
 * but will be unusable. Return 0 on success, or -1 on failure.
 *======================================================================*/

int trace_buffer_delete(void)
{
	if (our_trace_buffer_id == -1){
		return -1;
	}

	/*
	 * Deallocate the semaphores, but only if we're the process that created
	 * them in the first place
	 */
	if (trace_buffer->tb_creator_pid == getpid()){
		if (semctl(trace_buffer->tb_sem_id, 0, IPC_RMID, 0) == -1){
			return -1;
		}
	}

	/*
	 * Detach the trace buffer from our address space. Note that the trace buffer
	 * may still exist in other process address spaces, but will eventually be
	 * deallocated when all those processes exit.
	 */
	if (shmdt(trace_buffer) != 0){
		return -1;
	}

	/* record that we no longer have a trace buffer */
	our_trace_buffer_id = -1;
	trace_buffer = NULL;
	return 0;
}

/*======================================================================
 * trace_buffer_empty_content()
 *
 * This function empties the content of the trace buffer. This would
 * typically be called by the consumer once the content had been
 * written to disk.
 *======================================================================*/

void trace_buffer_empty_content()
{
	/* only empty the content, if there's a trace buffer attached */
	if (our_trace_buffer_id != -1){
		trace_buffer->tb_size = 0;
	}
}

/*======================================================================
 * trace_buffer_fetch()
 *
 * Fetch the content of the trace buffer. This function takes two
 * reference parameters (base_ptr and size_ptr) which upon return
 * will contain the memory address of the trace buffer data, and the
 * number of bytes of data provided. Unless the caller has the buffer
 * locked, these values will quickly become meaningless if producers
 * are still writing into the buffer.
 *
 * Return 0 on success or -1 to indicate failure.
 *======================================================================*/

int trace_buffer_fetch(void **base_ptr, unsigned long *size_ptr)
{
	if (our_trace_buffer_id == -1){
		return -1;
	}
	*base_ptr = trace_buffer + 1; /* skip over the header struct */
	*size_ptr = trace_buffer->tb_size;
	return 0;
}

/*======================================================================
 * trace_buffer_get_pos()
 *
 * This internal function is used as a helper by a number of functions
 * that write to the trace buffer. The goal is to make sure there's
 * enough room in the trace buffer to write the new data, as well as to
 * figure out where the data should be written. If there's not enough
 * data, this function signals the consumer process to write the
 * existing data to disk. When a child process calls this function, they
 * may block for a period of time.
 *
 * Returns the memory address where the new data can be written. If an
 * error occurs, NULL is returned.
 *======================================================================*/

static char *trace_buffer_get_pos(int size)
{
	/*
	 * Will the new bytes we're about to write cause a buffer overflow?
	 * If so, block this thread until the consumer can empty the buffer
	 * again.
	 */
	if (trace_buffer->tb_size + sizeof(trace_buffer_header) + size
			>= TRACE_BUFFER_SIZE){
		if (trace_buffer_mark_full(TRUE) != 0) {
			return NULL;
		}
		/*
		 * when we get here, the consumer has emptied the buffer and
		 * trace_buffer->tb_size is now 0.
		 */
	}

	/* compute where the new data should be written */
	char *new_base = (char *)trace_buffer + sizeof(trace_buffer_header)
			+ trace_buffer->tb_size;

	/* increment the buffer size, now that we have room for the new data */
	trace_buffer->tb_size += size;
	return new_base;
}

/*======================================================================
 * trace_buffer_write_string()
 *
 * Write a NUL-terminated string to the trace buffer. Return 0 on success
 * or -1 if anything goes wrong.
 *======================================================================*/

int trace_buffer_write_string(const char *string)
{
	/* make sure there's room in the buffer */
	char *ptr = trace_buffer_get_pos(strlen(string) + 1);
	if (!ptr){
		return -1;
	}

	/* copy the string */
	while ((*ptr++ = *string++) != 0){
		/* empty */
	}

	return 0;
}

/*======================================================================
 * trace_buffer_write_bytes()
 *
 * Write the specified number of bytes to the trace buffer. Return 0 on success
 * or -1 if anything goes wrong.
 *======================================================================*/

int trace_buffer_write_bytes(void *bytes, unsigned long size)
{
	/* make sure there's room in the buffer */
	char *out_ptr = trace_buffer_get_pos(size);
	if (!out_ptr){
		return -1;
	}
	char *in_ptr = (char *)bytes;

	while (size--) {
		*out_ptr++ = *in_ptr++;
	}
	return 0;
}


/*======================================================================
 * trace_buffer_write_int()
 *
 * Write a 4-byte integer to the trace buffer. Return 0 on success
 * or -1 if anything goes wrong. The integer is written in little-endian
 * format.
 *======================================================================*/

int trace_buffer_write_int(int value)
{
	/* make sure there's room in the buffer */
	char *out_ptr = trace_buffer_get_pos(sizeof(int));
	if (!out_ptr){
		return -1;
	}

	/* write to the buffer in little endian format */
	*out_ptr++ = value & 0xff;
	value >>= 8;
	*out_ptr++ = value & 0xff;
	value >>= 8;
	*out_ptr++ = value & 0xff;
	value >>= 8;
	*out_ptr++ = value & 0xff;

	return 0;
}

/*======================================================================
 * trace_buffer_write_byte()
 *
 * Write a 1-byte character to the trace buffer. Return 0 on success
 * or -1 if anything goes wrong.
 *======================================================================*/

int trace_buffer_write_byte(char value)
{
	/* make sure there's room in the buffer */
	char *out_ptr = trace_buffer_get_pos(sizeof(char));
	if (!out_ptr){
		return -1;
	}

	*out_ptr++ = value;
	return 0;
}

/*======================================================================
 * trace_buffer_lock_common()
 *
 * Helper function for locking something, based on the specified
 * semaphore name.
 *
 * Return 0 on success (after the lock is acquired), or -1 if an error
 * occurred (and the lock wasn't obtained).
 *======================================================================*/

static int trace_buffer_lock_common(int semId)
{
	/* if there's no trace buffer, return -1 */
	if (our_trace_buffer_id == -1){
		return -1;
	}

	// wait until the trace buffer semaphore is available.
	struct sembuf sem_ops[1];
	sem_ops[0].sem_num = semId;
	sem_ops[0].sem_op = -1;
	sem_ops[0].sem_flg = SEM_UNDO;
	if (semop(trace_buffer->tb_sem_id, sem_ops, 1) == -1){
		return -1;
	}

	return 0;
}

/*======================================================================
 * trace_buffer_unlock_common()
 *
 * Helper function for unlocking something, based on a semaphore.
 *
 * The current process has the trace buffer locked, but now it
 * wants to release the lock. Return 0 on success, or -1 on error.
 *======================================================================*/

static int trace_buffer_unlock_common(int semId)
{
	// if there's no trace buffer, return FALSE
	if (our_trace_buffer_id == -1){
		return -1;
	}
	// mark the trace buffer as being available.
	struct sembuf sem_ops[1];
	sem_ops[0].sem_num = semId;
	sem_ops[0].sem_op = 1;
	sem_ops[0].sem_flg = SEM_UNDO;
	if (semop(trace_buffer->tb_sem_id, sem_ops, 1) == -1){
		return -1;
	}

	return 0;
}

/*======================================================================
 * trace_buffer_lock()
 *
 * When a process wishes to access the trace buffer, it must first
 * acquire a lock. If the lock's not available, the process should block.
 *
 * Return 0 on success (after the lock is acquired), or -1 if an error
 * occurred (and the lock wasn't obtained).
 *======================================================================*/

int trace_buffer_lock()
{
	return trace_buffer_lock_common(TB_SEM_MASTER);
}

/*======================================================================
 * trace_buffer_unlock()
 *
 * Release the lock on the trace buffer.
 *
 * The current process has the trace buffer locked, but now it
 * wants to release the lock. Return 0 on success, or -1 on error.
 *======================================================================*/

int trace_buffer_unlock()
{
	return trace_buffer_unlock_common(TB_SEM_MASTER);
}

/*======================================================================
 * trace_buffer_lock_logfile()
 *
 * When a process wishes to write to the log file, it must first
 * acquire a lock. If the lock's not available, the process should block.
 *
 * Return 0 on success (after the lock is acquired), or -1 if an error
 * occurred (and the lock wasn't obtained).
 *======================================================================*/

int trace_buffer_lock_logfile()
{
	return trace_buffer_lock_common(TB_SEM_LOG_FILE);
}

/*======================================================================
 * trace_buffer_unlock()
 *
 * Release the lock on the logfile.
 *
 * The current process has the trace buffer locked, but now it
 * wants to release the lock. Return 0 on success, or -1 on error.
 *======================================================================*/

int trace_buffer_unlock_logfile()
{
	return trace_buffer_unlock_common(TB_SEM_LOG_FILE);
}

/*======================================================================
 * trace_buffer_mark_full()
 *
 * This function can be called by either the producer or the consumer.
 * If it's the producer, it's because they've noticed the trace buffer is
 * full and it needs to be empty. In this scenario, the value of 'state'
 * should be TRUE.
 * If it's the consumer, 'state' should be FALSE, to indicate they've emptied
 * the trace buffer and it's ready to take more data.
 * Returns 0 on success, or -1 on failure.
 *
 *======================================================================*/

int trace_buffer_mark_full(int state)
{
	struct sembuf sem_ops[1];
	sem_ops[0].sem_num = TB_SEM_FULL;
	sem_ops[0].sem_flg = 0;

	/*
	 * When called with state==TRUE, we are the producer and we've noticed that the buffer
	 * is full. We need to inform the consumer that the buffer must be cleaned up.
	 */
	if (state == 1) {

		/*
		 * The producer signals the consumer process. Both producer and consumer are now unblocked
		 * as the semaphore is now 0.
		 */
		sem_ops[0].sem_op = -1;
		if (semop(trace_buffer->tb_sem_id, sem_ops, 1) == -1){
			return -1;
		}

		/*
		 * The producer must block until the consumer wakes it up again. The semaphore is 0, so we'll
		 * need to block until it's > 0 again.
		 */
		sem_ops[0].sem_op = -1;
		if (semop(trace_buffer->tb_sem_id, sem_ops, 1) == -1){
			return -1;
		}

	} else {
		/*
		 * When called with state==FALSE, we are the consumer. Wake up the producer to let
		 * it know we've emptied the buffer. Note that only one producer will need to
		 * be awoken, since only one can hold the master lock. All others may be blocked
		 * too, but they're waiting for the master lock first.
		 * By adding 2 to the semaphore, we unblock the producer, but also block ourselves
		 * next time we call trace_buffer_wait_until_full()
		 */
		sem_ops[0].sem_op = 2;
		if (semop(trace_buffer->tb_sem_id, sem_ops, 1) == -1){
			return -1;
		}
	}
	return 0;
}

/*======================================================================
 * trace_buffer_wait_until_full()
 *
 * The consumer process calls this function when they're waiting for
 * the trace buffer to fill up. This function blocks until there's
 * enough data in the buffer. The function returns when the buffer is full,
 * or when the child process terminates after having written some data.
 *
 * Return codes:
 *   0 - The buffer is full and there's data to be read.
 *   1 - Our child process has terminated, and there may (or may not)
 *       be content in the buffer.
 *  -1 - Some other error occurred.
 *
 *======================================================================*/

int trace_buffer_wait_until_full()
{
	/*
	 * TODO: make this function more intelligent by having two buffers and alternating
	 * between them. The producer can be writing to one while the consumer empties
	 * the other. With the current implementation, the producers must block while the
	 * consumer dumps the data to disk.
	 */

	/* if there's no trace buffer, return an error */
	if (our_trace_buffer_id == -1){
		return -1;
	}

	/*
	 * If our immediate child process has terminated, we simply return 1 to let
	 * the caller know that there might be content in the buffer, but the child
	 * has terminated. Clearly, no new content will be added after this point in time.
	 */
	if (child_terminated) {
		return 1;
	}

	/*
	 * We are the consumer. Block until a producer tells us the buffer is
	 * full, or until our child process has exited. A zero-valued semaphore
	 * will wake us up.
	 */
	struct sembuf sem_ops[1];
	sem_ops[0].sem_num = TB_SEM_FULL;
	sem_ops[0].sem_op = 0;
	sem_ops[0].sem_flg = 0;
	if (semop(trace_buffer->tb_sem_id, sem_ops, 1) == -1){
		/*
		 * For some reason the semop() call failed. If errno is EINTR, it was
		 * because our child process died. That's OK (and normal), so return 1.
		 * On the other hand, any other error should be reported differently.
		 */
		if (errno == EINTR) {
			return 1;
		} else {
			return -1;
		}
	}

	/* we've unblocked, but the child is still alive - proceed normally */
	return 0;
}


/*======================================================================
 * trace_buffer_next_process_number()
 *
 * Allocate and return the next unique process number. The trace buffer
 * must be locked before calling this function. Return -1 if the trace
 * buffer isn't initialized properly.
 *======================================================================*/

int trace_buffer_next_process_number(void)
{
	if (our_trace_buffer_id == -1){
		return -1;
	}

	return trace_buffer->tb_process_number++;
}

/*======================================================================*/

