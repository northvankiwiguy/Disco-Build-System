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

/* include helper macros */
#include "cunit_helper.h"

/*
 * This test suite validates that system calls keep their normal functionality, even
 * when we're monitoring their behavior, or if we're remapping files that are in
 * the build tree.
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
 * test_open_cmn
 *
 * A common test function for any open-like function call.
 *======================================================================*/

static void test_open_cmn(int (*func)(char *, int, ...))
{
	static char *tmp_file = "/tmp/non-existent-file";

	/* open a non-existent temporary file for read - should fail */
	CU_ASSERT_EQUAL(func(tmp_file, O_RDONLY), -1);
	CU_ASSERT_EQUAL(errno, ENOENT);

	/* open a non-existent temporary file for write|create - should succeed */
	int fd1 = func(tmp_file, O_CREAT|O_WRONLY, 0644);
	CU_ASSERT_NOT_EQUAL(fd1, -1);
	close(fd1);

	/* try to recreate the same file with O_EXCL set - should fail. */
	int fd2 = func(tmp_file, O_CREAT|O_WRONLY|O_EXCL, 0644);
	CU_ASSERT_EQUAL(fd2, -1);
	CU_ASSERT_EQUAL(errno, EEXIST);

	/* open the newly created file in read mode - should succeed */
	int fd3 = func(tmp_file, O_RDONLY, 0644);
	CU_ASSERT_NOT_EQUAL(fd3, -1);
	close(fd3);

	/* Check file access bits - should be 0644 */
	struct stat buf;
	CU_ASSERT_EQUAL(stat(tmp_file, &buf), 0);
	CU_ASSERT_EQUAL(buf.st_mode & 0777, 0644);

	/* remove the temporary file. */
	unlink(tmp_file);
}

/*======================================================================
 * test_open
 *======================================================================*/

static void test_open(void)
{
	/* simply defer to a common function for all open-like calls */
	test_open_cmn((void *)open);
}

/*======================================================================
 * test_open64
 *======================================================================*/

static void test_open64(void)
{
	/* simply defer to a common function for all open-like calls */
	extern int open64(char *, int, ...);
	test_open_cmn((void *)open64);
}

/*======================================================================
 * init_regress_glibc_suite - main entry point for initializing this test suite
 *======================================================================*/

int init_regress_glibc_suite()
{
	/* add a suite to the registry */
	NEW_TEST_SUITE("Regression tests for interposed glibc functions.");

	/* add test cases */
	ADD_TEST_CASE(test_open, "open()");
	ADD_TEST_CASE(test_open64, "open64()");

	return 0;
}

/*======================================================================*/

