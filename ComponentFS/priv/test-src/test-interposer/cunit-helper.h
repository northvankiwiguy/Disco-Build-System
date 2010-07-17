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

#ifndef CUNITHELPER_H_
#define CUNITHELPER_H_

/*
 * This file contains macros and other definitions for helping to write cunit test cases.
 */

#include "CUnit/Basic.h"

/*
 * A macro for creating a new test registry.
 */
#define NEW_REGISTRY() \
	if (CU_initialize_registry() != CUE_SUCCESS) { \
		return CU_get_error(); \
	}

/*
 * A macro for creating a new test suite
 */
#define NEW_TEST_SUITE(suite_name) \
	CU_pSuite pSuite = CU_add_suite((suite_name), setup, teardown); \
	if (pSuite == NULL) { \
		CU_cleanup_registry(); \
	    return CU_get_error(); \
	}

/*
 * A macro for adding a new test case to a test suite
 */
#define ADD_TEST_CASE(fn, descr) \
	if (CU_add_test(pSuite, (descr), (fn)) == NULL) { \
		CU_cleanup_registry(); \
		return CU_get_error(); \
	}

/*
 * A macro for running tests.
 */
#define RUN_TESTS() \
	CU_basic_set_mode(CU_BRM_VERBOSE); \
	CU_basic_run_tests(); \
	CU_cleanup_registry(); \
	return CU_get_error();

#endif /* CUNITHELPER_H_ */
