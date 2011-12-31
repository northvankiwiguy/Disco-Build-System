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

package com.arapiki.disco.scanner.legacy;


import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Basic testing that the LegacyBuildScanner can produce a valid
 * BuildStore. There are many test cases, split over multiple
 * test case files, with this file testing C Functions that
 * manipulate directories.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestCFuncDir {

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the chdir() C function.
	 * @throws Exception
	 */
	@Test
	public void testChdir() throws Exception {
		// int chdir(const char *path)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the fchdir() C function.
	 * @throws Exception
	 */
	@Test
	public void testFchdir() throws Exception {
		// int fchdir(int fd)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the mkdir() C function.
	 * @throws Exception
	 */
	@Test
	public void testMkdir() throws Exception {
		// int mkdir(const char *path, mode_t mode)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the mkdirat() C function.
	 * @throws Exception
	 */
	@Test
	public void testMkdirat() throws Exception {
		// int mkdirat(int dirfd, const char *pathname, mode_t mode)
		fail("Not implemented.");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the rmdir() C function.
	 * @throws Exception
	 */
	@Test
	public void testRmdir() throws Exception {
		//int rmdir(const char *dirname)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/
}


