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
 * manipulate or access file permissions.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestCFuncPerms {
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the access() C function.
	 * @throws Exception
	 */
	@Test
	public void testAccess() throws Exception {
		//int access(const char *pathname, int mode)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the chmod() C function.
	 * @throws Exception
	 */
	@Test
	public void testChmod() throws Exception {
		//int chmod(const char *path, mode_t mode)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the chown() C function.
	 * @throws Exception
	 */
	@Test
	public void testChown() throws Exception {
		//int chown(const char *path, uid_t owner, gid_t group)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the eaccess() C function.
	 * @throws Exception
	 */
	@Test
	public void testEaccess() throws Exception {
		//int eaccess(const char *pathname, int mode)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the euidaccess() C function.
	 * @throws Exception
	 */
	@Test
	public void testEuidaccess() throws Exception {
		//int euidaccess(const char *pathname, int mode)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the faccessat() C function.
	 * @throws Exception
	 */
	@Test
	public void testFaccessat() throws Exception {
		//int faccessat(int dirfd, const char *pathname, int mode, int flags)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the fchmod() C function.
	 * @throws Exception
	 */
	@Test
	public void testFchmod() throws Exception {
		//int fchmod(int fd, mode_t mode)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the fchmodat() C function.
	 * @throws Exception
	 */
	@Test
	public void testFchmodat() throws Exception {
		//int fchmodat(int dirfd, const char *pathname, mode_t mode, int flags)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the fchown() C function.
	 * @throws Exception
	 */
	@Test
	public void testFchown() throws Exception {
		//int fchown(int fd, uid_t owner, gid_t group)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the fchownat() C function.
	 * @throws Exception
	 */
	@Test
	public void testFchownat() throws Exception {
		//int fchownat(int dirfd, const char *pathname, uid_t owner, gid_t group, int flags)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the lchown() C function.
	 * @throws Exception
	 */
	@Test
	public void testLchown() throws Exception {
		//int lchown(const char *path, uid_t owner, gid_t group)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/
	
}
