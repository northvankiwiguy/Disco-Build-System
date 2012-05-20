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

#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/wait.h>

/* include helper macros */
#include "cunit_helper.h"

/* include the trace buffer API functions */
#include "trace_buffer.h"

/*======================================================================
 * setup - Set-up function for this suite
 *======================================================================*/

static int setup(void)
{
	return 0;
}

/*======================================================================
 * teardown - Tear-down function for this suite
 *======================================================================*/

static int teardown(void)
{
	return 0;
}

/*======================================================================
 * test_trace_buffer_create()
 *
 * Test the trace_buffer_create() function.
 *======================================================================*/

static void test_trace_buffer_create(void)
{
	/* Create a new buffer - should succeed */
	trace_buffer_id id1 = trace_buffer_create();
	CU_ASSERT_NOT_EQUAL(id1, -1);

	/* Try to create a second buffer - should fail since only one is allowed. */
	trace_buffer_id id2 = trace_buffer_create();
	CU_ASSERT_EQUAL(id2, -1);

	/* Now delete the trace buffer - should succeed */
	CU_ASSERT_EQUAL(trace_buffer_delete(), 0);
}

/*======================================================================
 * test_trace_buffer_use_existing()
 *
 * Test the trace_buffer_use_existing() function.
 *
 *======================================================================*/

static void test_trace_buffer_use_existing(void)
{
	int status;

	/* Create a new buffer - should succeed */
	trace_buffer_id id1 = trace_buffer_create();
	CU_ASSERT_NOT_EQUAL(id1, -1);

	/* this test requires a producer and consumer */
	switch (fork()){
	case 0:
		/* child */

		/*
		 * remove the trace buffer from the child process, but it still
		 * exists in the parent.
		 */
		trace_buffer_delete();

		/* attach to the existing trace buffer - this is the function we're testing. */
		CU_ASSERT_NOT_EQUAL(trace_buffer_use_existing(id1), -1);

		/*
		 * write some data for the parent to read upon attaching
		 * to the trace buffer
		 */
		trace_buffer_write_int(0x13572468);

		/* return to the parent, but with the data written to the trace buffer. */
		trace_buffer_delete();
		exit(0);

	case -1:
		/* error */
		CU_FAIL("Failed to fork a child process.");
		break;

	default:
		/* parent */

		/* wait for the child to exit. */
		wait(&status);

		/* validate we can see the data written by the child. */
		char *ptr;
		unsigned long size;
		trace_buffer_fetch((void **)&ptr, &size);
		CU_ASSERT_EQUAL(size, sizeof(int));
		CU_ASSERT_TRUE((ptr[0] == 0x68) && (ptr[1] == 0x24) &&
				(ptr[2] == 0x57) && (ptr[3] == 0x13));
		break;
	}

	/* completely remove the trace buffer. */
	trace_buffer_delete();
}

/*======================================================================
 * test_trace_buffer_delete()
 *
 * Test the trace_buffer_delete() function.
 *======================================================================*/

static void test_trace_buffer_delete(void)
{
	/* Delete a trace buffer that isn't connected. Should fail. */
	CU_ASSERT_EQUAL(trace_buffer_delete(), -1);

	/* Create a new buffer - should succeed */
	trace_buffer_id id1 = trace_buffer_create();
	CU_ASSERT_NOT_EQUAL(id1, -1);

	/* Now delete the trace buffer - should succeed */
	CU_ASSERT_EQUAL(trace_buffer_delete(), 0);

	/* Try again to delete it - should fail */
	CU_ASSERT_EQUAL(trace_buffer_delete(), -1);
}

/*======================================================================
 * test_trace_buffer_fetch()
 *
 * Test the trace_buffer_fetch() function.
 *======================================================================*/

static void test_trace_buffer_fetch(void)
{
	void *buffer_base;
	unsigned long buffer_size;

	/* Try fetching a buffer that doesn't exist - should fail */
	CU_ASSERT_EQUAL(trace_buffer_fetch(&buffer_base, &buffer_size), -1);

	/* Create a new buffer - should succeed */
	trace_buffer_id id1 = trace_buffer_create();
	CU_ASSERT_NOT_EQUAL(id1, -1);

	/*
	 * Fetch the trace buffer's body (excluding the header).
	 * Should have a valid base pointer, and a zero size.
	 */
	CU_ASSERT_EQUAL(trace_buffer_fetch(&buffer_base, &buffer_size), 0);
	CU_ASSERT_NOT_EQUAL(buffer_base, NULL);
	CU_ASSERT_EQUAL(buffer_size, 0);

	/* Delete the trace buffer - should succeed */
	CU_ASSERT_EQUAL(trace_buffer_delete(), 0);

	/* Try again to fetch the content - should fail. */
	CU_ASSERT_EQUAL(trace_buffer_fetch(&buffer_base, &buffer_size), -1);
}

/*======================================================================
 * test_trace_buffer_write_string()
 *
 * Test the trace_buffer_write_string() function.
 *======================================================================*/

static void test_trace_buffer_write_string(void)
{
	void *buffer_base;
	unsigned long buffer_size;

	/* Create a new buffer - should succeed */
	trace_buffer_id id1 = trace_buffer_create();
	CU_ASSERT_NOT_EQUAL(id1, -1);

	/* write a string */
	trace_buffer_write_string("Hello World");

	/* validate that the string was written correctly. */
	CU_ASSERT_EQUAL(trace_buffer_fetch(&buffer_base, &buffer_size), 0);
	CU_ASSERT_EQUAL(buffer_size, 12);
	CU_ASSERT_EQUAL(memcmp(buffer_base, "Hello World", 12), 0);

	/* write another string to the same buffer */
	trace_buffer_write_string("Hi World");

	/* validate that the strings are now both in the buffer */
	CU_ASSERT_EQUAL(trace_buffer_fetch(&buffer_base, &buffer_size), 0);
	CU_ASSERT_EQUAL(buffer_size, 21);
	CU_ASSERT_EQUAL(memcmp(buffer_base, "Hello World\0Hi World", 21), 0);

	/* Delete the trace buffer */
	CU_ASSERT_EQUAL(trace_buffer_delete(), 0);
}

/*======================================================================
 * test_trace_buffer_write_bytes()
 *
 * Test the trace_buffer_write_bytes() function.
 *======================================================================*/

static void test_trace_buffer_write_bytes(void)
{
	void *buffer_base;
	unsigned long buffer_size;

	/* we'll write these byte arrays */
	char buf1[10] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
	char buf2[10] = {10, 9, 12, 14, 65, 3, 2, -2, 8, 3};
	char buf3[20] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 9, 12, 14, 65, 3, 2, -2, 8, 3};

	/* Create a new buffer - should succeed */
	trace_buffer_id id1 = trace_buffer_create();
	CU_ASSERT_NOT_EQUAL(id1, -1);

	/* write a sequence of bytes to the trace buffer. */
	trace_buffer_write_bytes(buf1, 10);

	/* validate that the bytes were written correctly. */
	CU_ASSERT_EQUAL(trace_buffer_fetch(&buffer_base, &buffer_size), 0);
	CU_ASSERT_EQUAL(buffer_size, 10);
	CU_ASSERT_EQUAL(memcmp(buffer_base, buf1, 10), 0);

	/* write a second sequence of bytes to the trace buffer. */
	trace_buffer_write_bytes(buf2, 10);

	/* validate that both byte arrays were written correctly. */
	CU_ASSERT_EQUAL(trace_buffer_fetch(&buffer_base, &buffer_size), 0);
	CU_ASSERT_EQUAL(buffer_size, 20);
	CU_ASSERT_EQUAL(memcmp(buffer_base, buf3, 20), 0);

	/* Delete the trace buffer */
	CU_ASSERT_EQUAL(trace_buffer_delete(), 0);
}


/*======================================================================
 * test_trace_buffer_write_int()
 *
 * Test the trace_buffer_write_int() function.
 *======================================================================*/

static void test_trace_buffer_write_int(void)
{
	void *buffer_base;
	unsigned long buffer_size;

	/* these byte arrays are used for validation. */
	char buf1[4] = {0x78, 0x56, 0x34, 0x12};
	char buf2[8] = {0x78, 0x56, 0x34, 0x12, 0xfe, 0x01, 0x00, 0xff};

	/* Create a new buffer - should succeed */
	trace_buffer_id id1 = trace_buffer_create();
	CU_ASSERT_NOT_EQUAL(id1, -1);

	/* write a 4-byte integer to the trace buffer. */
	trace_buffer_write_int(0x12345678);

	/* validate that the bytes were written correctly. */
	CU_ASSERT_EQUAL(trace_buffer_fetch(&buffer_base, &buffer_size), 0);
	CU_ASSERT_EQUAL(buffer_size, 4);
	CU_ASSERT_EQUAL(memcmp(buffer_base, buf1, sizeof(int)), 0);

	/* write another 4-byte integer to the trace buffer. */
	trace_buffer_write_int(0xff0001fe);

	/* validate that all the bytes were written correctly. */
	CU_ASSERT_EQUAL(trace_buffer_fetch(&buffer_base, &buffer_size), 0);
	CU_ASSERT_EQUAL(buffer_size, 8);
	CU_ASSERT_EQUAL(memcmp(buffer_base, buf2, 2 * sizeof(int)), 0);

	/* Delete the trace buffer */
	CU_ASSERT_EQUAL(trace_buffer_delete(), 0);
}

/*======================================================================
 * test_trace_buffer_write_byte()
 *
 * Test the trace_buffer_write_byte() function.
 *======================================================================*/

static void test_trace_buffer_write_byte(void)
{
	char *buffer_base;
	unsigned long buffer_size;
	char byte1 = 0x42, byte2 = 0x82;

	/* Create a new buffer - should succeed */
	trace_buffer_id id1 = trace_buffer_create();
	CU_ASSERT_NOT_EQUAL(id1, -1);

	/* write a single byte to the trace buffer. */
	trace_buffer_write_byte(byte1);

	/* validate that the bytes were written correctly. */
	CU_ASSERT_EQUAL(trace_buffer_fetch((void **)&buffer_base, &buffer_size), 0);
	CU_ASSERT_EQUAL(buffer_size, 1);
	CU_ASSERT_EQUAL(buffer_base[0], byte1);

	/* write another byte to the trace buffer. */
	trace_buffer_write_byte(byte2);

	/* validate that the bytes were written correctly. */
	CU_ASSERT_EQUAL(trace_buffer_fetch((void **)&buffer_base, &buffer_size), 0);
	CU_ASSERT_EQUAL(buffer_size, 2);
	CU_ASSERT_EQUAL(buffer_base[0], byte1);
	CU_ASSERT_EQUAL(buffer_base[1], byte2);

	/* Delete the trace buffer */
	CU_ASSERT_EQUAL(trace_buffer_delete(), 0);
}

/*======================================================================
 * test_trace_buffer_lock()
 *
 * Test the trace_buffer_lock() function.
 *======================================================================*/

static void test_trace_buffer_lock(void)
{
	/* Create a new buffer - should succeed */
	trace_buffer_id id1 = trace_buffer_create();
	CU_ASSERT_NOT_EQUAL(id1, -1);

	/* we need both a producer and a consumer */
	switch (fork()){
	case 0:
		/* child */

		/* acquire the lock, sleep, then release the lock */
		CU_ASSERT_EQUAL(trace_buffer_lock(), 0);
		usleep(100000);
		trace_buffer_unlock();
		exit(0);

	case -1:
		/* error */
		CU_FAIL("Failed to fork a child process.");
		break;

	default:
		/* parent */

		/* wait just long enough for the child to acquire the lock. */
		usleep(10000);

		/*
		 * acquire the lock - this could take a while, since the child has it locked
		 * already. We need to test whether the duration for acquiring the lock is
		 * at least as long as the child held it (50ms).
		 */
		struct timeval before, after;
		gettimeofday(&before, NULL);
		CU_ASSERT_EQUAL(trace_buffer_lock(), 0);
		gettimeofday(&after, NULL);
		unsigned long duration = (after.tv_sec - before.tv_sec) * 1000000 +
				(after.tv_usec - before.tv_usec);
		CU_ASSERT_TRUE(duration > 50000);

		/* release the lock. Test is complete. */
		trace_buffer_unlock();

		/* wait for the child to exit. */
		int status;
		wait(&status);
		break;
	}

	/* completely remove the trace buffer. */
	trace_buffer_delete();
}


/*======================================================================
 * test_trace_buffer_mark_full()
 *
 * Test the trace_buffer_mark_full() function.
 *======================================================================*/

static void test_trace_buffer_mark_full(void)
{
	/* Create a new buffer - should succeed */
	trace_buffer_id id1 = trace_buffer_create();
	CU_ASSERT_NOT_EQUAL(id1, -1);

	switch (fork()){
	case 0:
		/* child - note, we can't use CU_ASSERT_x in the child process */

		/* wait long enough for the parent to block. */
		usleep(100000);

		/* write a byte of data to the trace buffer, to make it non-empty. */
		trace_buffer_write_byte(1);

		/*
		 * Let's pretend it's now full. This should block until the parent
		 * cleans up the buffer.
		 */
		struct timeval before, after;
		gettimeofday(&before, NULL);
		trace_buffer_mark_full(TRUE);
		gettimeofday(&after, NULL);
		unsigned long duration = (after.tv_sec - before.tv_sec) * 1000000 +
				(after.tv_usec - before.tv_usec);

		/*
		 * if we ended up waiting for > 90ms, write another byte. The parent
		 * will therefore see 2 bytes in the buffer on success, or 1 on failure.
		 * We do this as a work-around for not being able to CU_ASSERT in the
		 * child process.
		 */
		if (duration > 90000) {
			trace_buffer_write_byte(2);
		}

		/* exit - this should cause the parent to unblock. */
		usleep(50000);
		exit(0);

	case -1:
		/* error */
		CU_FAIL("Failed to fork a child process.");
		break;

	default:
		/* parent */

		/*
		 * wait until the child tells us the buffer is full.
		 * The return value should be 0 to indicate that data is
		 * available.
		 */
		CU_ASSERT_EQUAL(trace_buffer_wait_until_full(), 0);

		/* There should be content, assuming we blocked successfully. */
		void *ptr;
		unsigned long size;
		trace_buffer_fetch(&ptr, &size);
		CU_ASSERT_EQUAL(size, 1);

		/*
		 * Wait for a while, to make sure the child blocks for a measurable
		 * period of time, the mark then trace buffer as being empty (this
		 * should wake up the child).
		 */
		usleep(100000);
		CU_ASSERT_EQUAL(trace_buffer_mark_full(FALSE), 0);

		/*
		 * Wait again, but this time the child terminated.
		 * The return value should be 1 to indicate EOF.
		 */
		CU_ASSERT_EQUAL(trace_buffer_wait_until_full(), 1);

		/*
		 * Check the final buffer - on success there should be 2 bytes.
		 * note that the second byte was put there because the child
		 * blocked long enough while it waited for the parent
		 */
		trace_buffer_fetch(&ptr, &size);
		CU_ASSERT_EQUAL(size, 2);
		break;
	}

	/* Completely remove the trace buffer. */
	trace_buffer_delete();
}

/*======================================================================
 * test_trace_buffer_large_writes()
 *
 * Test that large writes of data (many times larger than the trace
 * buffer) will succeed.
 *======================================================================*/

static void test_trace_buffer_large_writes(void)
{
	/* Create a new buffer - should succeed */
	trace_buffer_id id1 = trace_buffer_create();
	CU_ASSERT_NOT_EQUAL(id1, -1);

	/* we need a producer and a consumer */
	switch (fork()){
	case 0:
		/* child */ ;
		int i;

		CU_ASSERT_EQUAL(trace_buffer_lock(), 0);

		/*
		 * Generate a very long sequential stream of numbers.
		 * With each iteration, we write a 4-byte word into buffer. This
		 * will overflow the buffer multiple times, allowing us to test
		 * whether the data is passed through to the parent correctly.
		 */
		for (i = 0; i != TRACE_BUFFER_SIZE * 20; i++){
			trace_buffer_write_int(i);
		}

		CU_ASSERT_TRUE(trace_buffer_unlock());

		/* exit - the parent's loop should now terminate normally. */
		exit(0);

	case -1:
		/* error */
		CU_FAIL("Failed to fork a child process.");
		break;

	default:
		/* parent */ ;
		int child_done = FALSE;
		int counter = 0;

		/*
		 * Loop indefinitely, until the child process exits. Our 'counter'
		 * starts and 0 and increments as we receive data from the child.
		 * Naturally, since the child's counter also starts from 0, we should
		 * expect the same sequence of numbers.
		 */
		do {
			/* Wait until the child tells us the buffer is full */
			child_done = trace_buffer_wait_until_full();
			if (child_done == -1) {
				CU_FAIL("trace_buffer_wait_until_full returned -1");
			}

			/* fetch the buffer base and size */
			int *buf_ptr;
			unsigned long buf_size;
			trace_buffer_fetch((void **)&buf_ptr, &buf_size);

			/* validate the content of the buffer */
			CU_ASSERT_TRUE(buf_size % sizeof(int) == 0);
			while (buf_size != 0) {
				if (*buf_ptr++ != counter++) {
					CU_FAIL("trace buffer content is incorrect");
					break;
				}
				buf_size -= sizeof(int);
			}

			/* mark the buffer as empty, and start the child again */
			trace_buffer_empty_content();
			CU_ASSERT_EQUAL(trace_buffer_mark_full(FALSE), 0);

		} while (!child_done);
	}

	/* completely remove the trace buffer. */
	trace_buffer_delete();
}

/*======================================================================
 * test_trace_buffer_next_process_number()
 *
 * Test that allocation of unique process numbers works.
 *======================================================================*/

static void test_trace_buffer_next_process_number(void)
{
	/* Create a new buffer - should succeed */
	trace_buffer_id id1 = trace_buffer_create();
	CU_ASSERT_NOT_EQUAL(id1, -1);

	CU_ASSERT_EQUAL(trace_buffer_next_process_number(), 1);
	CU_ASSERT_EQUAL(trace_buffer_next_process_number(), 2);
	CU_ASSERT_EQUAL(trace_buffer_next_process_number(), 3);

	/* completely remove the trace buffer. */
	trace_buffer_delete();
}

/*======================================================================
 * test_trace_buffer_stress()
 *
 * Test that locking works when two or more processes write to the
 * buffer at the same time. Each process writes a predetermine sequence
 * of bytes, and assuming that buffer locking works as expected, each
 * sequence of bytes should be written atomically.
 *======================================================================*/

static void test_trace_buffer_stress(void)
{
	int num_writers = 2;
	int bytes_per_writer = (TRACE_BUFFER_SIZE / num_writers) * 0.9;
	int bytes_written = 0;
	int is_parent;

	/* Create a new buffer - should succeed */
	trace_buffer_id id1 = trace_buffer_create();
	CU_ASSERT_NOT_EQUAL(id1, -1);

	/*
	 * Create a child process that will run in parallel with the parent
	 * process.
	 */
	switch(fork()) {
	case 0:
		/* child process */
		is_parent = FALSE;
		break;

	default:
		/* parent process */
		is_parent = TRUE;
		break;

	case -1:
		/* error */
		CU_FAIL("Unable to fork off a child process.");
		return;
	}

	/*
	 * Both the parent and child will execute this code - filling up the trace buffer.
	 * We follow this algorithm:
	 * 		- Each process picks a random number (which should be different from one the
	 *        other process chose).
	 *      - The process writes that number as a 4-byte integer.
	 *      - The process writes the low-order byte of that number to the trace buffer
	 *        a total of num % 10 times (from 0 to 9 repetitions).
	 * This is repeated until the process has written bytesPerWriter bytes to the buffer
	 * (to ensure we won't run out of buffer space).
	 */
	srand(getpid());
	while (bytes_written < bytes_per_writer) {

		/* pick a random number */
		unsigned int num = rand();
		int count;
		if (trace_buffer_lock() == 0) {

			/* write that integer to the trace buffer */
			trace_buffer_write_int(num);

			/* now write the low-order byte num % 10 times */
			count = num % 10;
			int i = 0;
			while (i++ != count) {
				trace_buffer_write_byte((unsigned char )(num & 0xff));
			}
			if (trace_buffer_unlock() != 0){
				CU_FAIL("Unable to unlock trace buffer.");
			}
		} else {
			CU_FAIL("Unable to lock trace buffer.");
		}
		bytes_written += sizeof(int) + count;
	}

	/*
	 * Both parent and child are done writing, so the parent process must wait
	 * for the child to exit.
	 */
	if (is_parent) {
		int status;
		wait(&status);
	}

	/* the child process exists, passing control back to the parent */
	else {
		exit(0);
	}

	/*
	 * The parent now checks the buffer to make sure it contains valid content.
	 */
	void *ptr;
	unsigned long buffer_size;
	int bytes_checked = 0;
	CU_ASSERT_EQUAL(trace_buffer_fetch((void **)&ptr, &buffer_size), 0);

	while (bytes_checked != buffer_size) {

		/* read the num */
		unsigned int num = *(int *)ptr;
		ptr += sizeof(int);

		/* check that the low-order byte was written enough times */
		int count = num % 10;
		int i = 0;
		while (i++ != count) {
			CU_ASSERT_EQUAL((unsigned char)(num & 0xff), *(unsigned char *)ptr);
			ptr++;
		}
		bytes_checked += sizeof(int) + count;
	}
}

/*======================================================================
 * init_regress_glibc_suite - main entry point for initializing this test suite
 *======================================================================*/

int init_trace_buffer_suite()
{
	/* add a suite to the registry */
	NEW_TEST_SUITE("Tests for the interposer's trace buffer.");

	/* add new test cases */
	ADD_TEST_CASE(test_trace_buffer_create, "Test trace_buffer_create()");
	ADD_TEST_CASE(test_trace_buffer_use_existing, "Test trace_buffer_use_existing()");
	ADD_TEST_CASE(test_trace_buffer_delete, "Test trace_buffer_delete()");
	ADD_TEST_CASE(test_trace_buffer_fetch, "Test trace_buffer_fetch()");
	ADD_TEST_CASE(test_trace_buffer_write_string, "Test trace_buffer_write_string()");
	ADD_TEST_CASE(test_trace_buffer_write_bytes, "Test trace_buffer_write_bytes()");
	ADD_TEST_CASE(test_trace_buffer_write_int, "Test trace_buffer_write_int()");
	ADD_TEST_CASE(test_trace_buffer_write_byte, "Test trace_buffer_write_byte()");
	ADD_TEST_CASE(test_trace_buffer_lock, "Test trace_buffer_lock()");
	ADD_TEST_CASE(test_trace_buffer_mark_full, "Test trace_buffer_mark_full()");
	ADD_TEST_CASE(test_trace_buffer_large_writes, "Test that trace_buffer manages large amounts of data.");
	ADD_TEST_CASE(test_trace_buffer_next_process_number, "Test trace_buffer_next_process_number()");
	ADD_TEST_CASE(test_trace_buffer_stress, "Test trace_buffer_stress()");

	return 0;
}

/*======================================================================*/
