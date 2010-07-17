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

#include <stdio.h>
#include "cunit-helper.h"

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
 * test_first
 *======================================================================*/

static void test_first(void)
{
}

/*======================================================================
 * test_second
 *======================================================================*/

static void test_second(void)
{

}

/*======================================================================
 * init_suite_1 - main entry point for initializing this test suite
 *======================================================================*/

int init_regress_syscalls_suite()
{
	/* add a suite to the registry */
	NEW_TEST_SUITE("Regress syscalls");

	ADD_TEST_CASE(test_first, "My first test case");
	ADD_TEST_CASE(test_second, "My 2nd test case");
}

/*======================================================================*/

