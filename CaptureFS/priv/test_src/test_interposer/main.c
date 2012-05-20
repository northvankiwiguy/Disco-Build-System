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
 * This is the main entry point for all unit tests related to the CFS interposer.
 * The test suites themselves are in other source files, including:
 * 		- regress_glibc_suite.c - make sure our interposers don't break the standard
 *        glibc functionality.
 *      - trace_buffer_suite.c - test the operation of the trace buffer.
 *      - trace_glibc_suite.c - test that calling glibc functions generates the
 *        correct trace output.
 */

/* define helper macros for using CUnit */
#include "cunit_helper.h"

/* each suite initializes itself via one of these functions */
extern int init_file_name_utils_suite();
extern int init_regress_glibc_suite();
extern int init_trace_buffer_suite();

/*======================================================================
 * main - The main entry point for unit-testing the CFS interposer.
 *======================================================================*/

int main(int argc, char *argv[])
{

	/* initialize the CUnit test registry */
	NEW_REGISTRY();

	/*
	 * Test our file_name_utils helper functions.
	 */
	if (init_file_name_utils_suite() != CUE_SUCCESS) {
		return CU_get_error();
	}

	/*
	 * Make sure glibc calls don't lose their normal behavior when accessing files
	 * outside of the build tree.
	 */
	if (init_regress_glibc_suite() != CUE_SUCCESS) {
		return CU_get_error();
	}

	/*
	 * Directly test the trace buffer by invoking the trace buffer's API.
	 */
	if (init_trace_buffer_suite() != CUE_SUCCESS) {
		return CU_get_error();
	}

	/* Run all tests using the CUnit Basic interface */
	RUN_TESTS();

	/* clean up, and return a non-zero exit code if there were failures */
	int failures = 	CU_get_number_of_tests_failed();
	CU_cleanup_registry();
	return failures != 0;
}

/*======================================================================*/
