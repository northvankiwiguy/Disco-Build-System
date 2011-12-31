/*******************************************************************************
 * Copyright (c) 2011 Arapiki Solutions Inc.
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
#include <limits.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>
#include "file_name_utils.h"

/* include helper macros */
#include "cunit_helper.h"

/*
 * This test suite validates that our file_name_utils functions work
 * correctly.
 */

/* Many tests require a input and output buffers */
static char input_buffer1[PATH_MAX];
static char input_buffer2[PATH_MAX];
static char output_buffer[PATH_MAX];

/*======================================================================
 * fill_string - helper function for creating long strings
 *======================================================================*/

void fill_string(char *buffer, int len)
{
	while (len-- != 0) {
		if (len % 10 == 0) {
			*buffer++ = '/';
		} else {
			*buffer++ = 'A';
		}
	}
	*buffer++ = '\0';
}

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
 * test_simple_paths
 *
 * Note: this test case assumes you have "/usr/bin/zip" installed
 *
 *======================================================================*/

static void test_simple_paths(void)
{
	/* test with . and .. */
	_cfs_combine_paths("/etc/../usr/", "bin/.//zip", output_buffer);
	CU_ASSERT_STRING_EQUAL("/usr/bin/zip", output_buffer);
	_cfs_combine_paths("/./etc/../usr/", "bin/.//zip", output_buffer);
	CU_ASSERT_STRING_EQUAL("/usr/bin/zip", output_buffer);
	_cfs_combine_paths("/etc/../usr/../usr/./", "../usr/bin/.//zip", output_buffer);
	CU_ASSERT_STRING_EQUAL("/usr/bin/zip", output_buffer);

	/* test with too many slashes */
	_cfs_combine_paths("//", "/etc/passwd", output_buffer);
	CU_ASSERT_STRING_EQUAL("/etc/passwd", output_buffer);

	/* test with files that don't exist (yet) - should be OK */
	CU_ASSERT_EQUAL(0, _cfs_combine_paths("/usr", "bin/sillysed", output_buffer));
	CU_ASSERT_STRING_EQUAL("/usr/bin/sillysed", output_buffer);
	CU_ASSERT_EQUAL(0, _cfs_combine_paths("/usr", "bin/sillydir/", output_buffer));
	CU_ASSERT_STRING_EQUAL("/usr/bin/sillydir/", output_buffer);
	CU_ASSERT_EQUAL(0, _cfs_combine_paths("/", "sillyfile", output_buffer));
	CU_ASSERT_STRING_EQUAL("/sillyfile", output_buffer);
}

/*======================================================================
 * test_bad_args
 *
 * Note: this test case assumes you have "/usr/bin/sed" installed
 *
 *======================================================================*/

static void test_bad_args(void)
{
	/* test for NULL input pointers */
	CU_ASSERT_EQUAL(EINVAL,_cfs_combine_paths(NULL, "bin/sed", output_buffer));
	CU_ASSERT_EQUAL(EINVAL,_cfs_combine_paths("usr", NULL, output_buffer));
	CU_ASSERT_EQUAL(EINVAL,_cfs_combine_paths("usr", "bin/sed", NULL));

	/* test with parent/extra paths that would cause the buffer to overflow */
	fill_string(input_buffer1, PATH_MAX-10);
	fill_string(input_buffer2, 20);
	CU_ASSERT_EQUAL(ENAMETOOLONG,
			_cfs_combine_paths(input_buffer1, input_buffer2, output_buffer));

	fill_string(input_buffer1, 100);
	fill_string(input_buffer2, PATH_MAX - 50);
	CU_ASSERT_EQUAL(ENAMETOOLONG,
			_cfs_combine_paths(input_buffer1, input_buffer2, output_buffer));

	fill_string(input_buffer1, PATH_MAX / 2);
	fill_string(input_buffer2, PATH_MAX / 2);
	CU_ASSERT_EQUAL(ENAMETOOLONG,
			_cfs_combine_paths(input_buffer1, input_buffer2, output_buffer));

	/* test with a parent path that doesn't exist - ENOENT */
	CU_ASSERT_EQUAL(ENOENT, _cfs_combine_paths("/sillyusr", "bin/sed", output_buffer));
	CU_ASSERT_EQUAL(ENOENT, _cfs_combine_paths("/sillyusr", "..", output_buffer));
	CU_ASSERT_EQUAL(ENOENT, _cfs_combine_paths("/usr/bin", "missing/dir", output_buffer));

	/* test with a directory name that's actually a file name - ENOTDIR */
	CU_ASSERT_EQUAL(ENOTDIR, _cfs_combine_paths("/etc/passwd", "bin/sed", output_buffer));
}

/*======================================================================
 * test_with_symlinks
 *
 * Test with symlinks.
 *
 *======================================================================*/

static void test_symlinks(void)
{
	/* TODO: add test cases for symlinks */
}

/*======================================================================
 * init_regress_glibc_suite - main entry point for initializing this test suite
 *======================================================================*/

int init_file_name_utils_suite()
{
	/* add a suite to the registry */
	NEW_TEST_SUITE("Regression tests for file name utils");

	/* add test cases */
	ADD_TEST_CASE(test_bad_args, "bad_args()");
	ADD_TEST_CASE(test_simple_paths, "simple_paths()");

	return 0;
}

/*======================================================================*/

