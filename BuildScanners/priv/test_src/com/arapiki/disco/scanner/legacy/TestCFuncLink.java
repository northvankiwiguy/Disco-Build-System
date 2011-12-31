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
 * perform an open()-like operation.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestCFuncLink {

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the link() C function.
	 * @throws Exception
	 */
	@Test
	public void testLink() throws Exception {
		//int link(const char *oldname, const char *newname)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the linkat() C function.
	 * @throws Exception
	 */
	@Test
	public void testLinkat() throws Exception {
		//int linkat(int olddirfd, const char *oldpath,
		//        int newdirfd, const char *newpath, int flags)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the remove() C function.
	 * @throws Exception
	 */
	@Test
	public void testRemove() throws Exception {
		//int remove(const char *path)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the rename() C function.
	 * @throws Exception
	 */
	@Test
	public void testRename() throws Exception {
		//int rename(const char *oldname, const char *newname)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the renameat() C function.
	 * @throws Exception
	 */
	@Test
	public void testRenameat() throws Exception {
		//int renameat(int olddirfd, const char *oldpath, int newdirfd, const char *newpath)
		fail("Not implemented.");
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the symlink() C function.
	 * @throws Exception
	 */
	@Test
	public void testSymlink() throws Exception {
		//int symlink(const char *oldname, const char *newname)
		fail("Not implemented.");
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the symlinkat() C function.
	 * @throws Exception
	 */
	@Test
	public void testSymlinkat() throws Exception {
		//int symlinkat(const char *oldpath, int newdirfd, const char *newpath)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the unlink() C function.
	 * @throws Exception
	 */
	@Test
	public void testUnlink() throws Exception {
		//int unlink(const char *filename)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the unlinkat() C function.
	 * @throws Exception
	 */
	@Test
	public void testUnlinkat() throws Exception {
		//int unlinkat(int dirfd, const char *pathname, int flags)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/

}
