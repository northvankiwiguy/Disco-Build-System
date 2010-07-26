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

#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

#include "cunit_helper.h"
#include "trace_buffer.h"
#include "trace_file_format.h"

/*
 * This test suite validates that glibc functions are being properly recorded
 * in the trace buffer.
 */

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
 * test_open
 *
 * The that the open() function is properly traced.
 *======================================================================*/

static void test_open(void)
{
	char *file_name = "/tmp/test-file";
	char *buffer_base;
	unsigned long buffer_size;

	/* create a new trace buffer */
	trace_buffer_create();

	/* perform the operation we're tracing */
	int fd1 = open(file_name, O_CREAT|O_RDWR, 0644);

	/* validate that the trace buffer content is correct */
	trace_buffer_fetch((void **)&buffer_base, &buffer_size);
	CU_ASSERT_EQUAL(buffer_size, 16);
	CU_ASSERT_EQUAL(buffer_base[0], TRACE_FILE_WRITE);
	CU_ASSERT_EQUAL(memcmp(&buffer_base[1], file_name, strlen(file_name) + 1), 0);
	close(fd1);

	/* perform a second open() call */
	int fd2 = open(file_name, O_RDONLY);

	/* validate both the new and old content is correct (from both open() calls) */
	trace_buffer_fetch((void **)&buffer_base, &buffer_size);
	CU_ASSERT_EQUAL(buffer_size, 32);
	CU_ASSERT_EQUAL(buffer_base[0], TRACE_FILE_WRITE);
	CU_ASSERT_EQUAL(memcmp(&buffer_base[1], file_name, strlen(file_name) + 1), 0);
	CU_ASSERT_EQUAL(buffer_base[16], TRACE_FILE_READ);
	CU_ASSERT_EQUAL(memcmp(&buffer_base[17], file_name, strlen(file_name) + 1), 0);
	close(fd2);

	/* clean up */
	unlink(file_name);
	trace_buffer_delete();
}


/*======================================================================
 * init_trace_glibc_suite - main entry point for initializing this test suite
 *======================================================================*/

int init_trace_glibc_suite()
{
	/* add a suite to the registry */
	NEW_TEST_SUITE("Test that glibc operations are being traced correctly.");

	/* add the test cases */
	ADD_TEST_CASE(test_open, "open()");

	return 0;
}

/*======================================================================*/
