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

package com.arapiki.disco.model;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.arapiki.utils.errors.ErrorCode;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestFileNameSpacesRoots {


	/** Our BuildStore object, used in many test cases */
	private BuildStore bs;

	/** The BuildStoreFileSpace associated with this BuildStore */
	FileNameSpaces bsfs;
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		bs = TestCommon.getEmptyBuildStore();
		
		/* fetch the associated FileSpace */
		bsfs = bs.getFileNameSpaces();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileNameSpaces#addNewRoot(java.lang.String)}.
	 */
	@Test
	public void testAddNewRoot() {

		/* to start with, these roots shouldn't exist */
		assertEquals(ErrorCode.NOT_FOUND, bsfs.getRootPath("Root1"));
		assertEquals(ErrorCode.NOT_FOUND, bsfs.getRootPath("Root2"));
		
		/* let's add them to some new paths */
		int path1 = bsfs.addDirectory("/a/b/c");
		int path2 = bsfs.addDirectory("/a/b/c/d");		
		assertEquals(ErrorCode.OK, bsfs.addNewRoot("Root1", path1));
		assertEquals(ErrorCode.OK, bsfs.addNewRoot("Root2", path2));
		
		/* check that they point to their respective path IDs */
		assertEquals(path1, bsfs.getRootPath("Root1"));
		assertEquals(path2, bsfs.getRootPath("Root2"));

		/* Check that other names didn't suddenly appear */
		assertEquals(ErrorCode.NOT_FOUND, bsfs.getRootPath("Root3"));
		
		/* 
		 * Can't add names with ':', or ' ' in the name, since these symbols are used for
		 * displaying and inputing root names.
		 */
		int path3 = bsfs.addDirectory("/a/b/c/d/e");
		assertEquals(ErrorCode.INVALID_NAME, bsfs.addNewRoot("badRoot:Name", path3));
		assertEquals(ErrorCode.INVALID_NAME, bsfs.addNewRoot("badRoot Name", path3));
		assertEquals(ErrorCode.INVALID_NAME, bsfs.addNewRoot("really Bad Name:", path3));
		
		/* adding a root to a file should fail */
		int file1 = bsfs.addFile("/adam/barney/charlie.c");
		assertEquals(ErrorCode.NOT_A_DIRECTORY, bsfs.addNewRoot("rootOnFile", file1));
		
		/* adding the same root name to two different paths should fail */
		int path4 = bsfs.addDirectory("/a/b/c/d/e/f1");
		int path5 = bsfs.addDirectory("/a/b/c/d/e/f2");
		assertEquals(ErrorCode.OK, bsfs.addNewRoot("duplicateRoot", path4));
		assertEquals(ErrorCode.ALREADY_USED, bsfs.addNewRoot("duplicateRoot", path5));
		
		/* adding two roots to the same directory should fail */
		int path6 = bsfs.addDirectory("/a/b/c/g");
		assertEquals(ErrorCode.OK, bsfs.addNewRoot("root6", path6));
		assertEquals(ErrorCode.ONLY_ONE_ALLOWED, bsfs.addNewRoot("root7", path6));
		
		/* adding a root to a non-existent path should fail */
		assertEquals(ErrorCode.BAD_PATH, bsfs.addNewRoot("root8", 1000));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method to validate that paths are accessible via multiple roots.
	 */
	@Test
	public void testPathAccessViaRoots() {
	
		/* a file added within a root should be accessible via many roots */
		int path1 = bsfs.addFile("/aardvark/bear/cat/dog/eel.c");
		int path2 = bsfs.addDirectory("/aardvark/bear");
		int path3 = bsfs.addDirectory("/aardvark/bear/cat/dog");
		assertEquals(ErrorCode.OK, bsfs.addNewRoot("bearRoot", path2));
		assertEquals(ErrorCode.OK, bsfs.addNewRoot("dogRoot", path3));

		/* accessing via the "root" should return the original path ID */
		assertEquals(path1, bsfs.getPath("/aardvark/bear/cat/dog/eel.c"));
		
		/* so should accessing via the lower-level roots */
		assertEquals(path1, bsfs.getPath("bearRoot:/cat/dog/eel.c"));
		assertEquals(path1, bsfs.getPath("dogRoot:/eel.c"));
	
		/* files with the same name, but under different roots should have different IDs */
		int path4 = bsfs.addFile("root:/test.h");
		int path5 = bsfs.addFile("bearRoot:/test.h");
		int path6 = bsfs.addFile("dogRoot:/test.h");
		assertNotSame(path4, path5);
		assertNotSame(path4, path6);
		assertNotSame(path5, path6);
		
		/* but all should be accessible via other roots */
		assertEquals(path4, bsfs.getPath("root:/test.h"));
		assertEquals(path5, bsfs.getPath("root:/aardvark/bear/test.h"));
		assertEquals(path6, bsfs.getPath("root:/aardvark/bear/cat/dog/test.h"));
		assertEquals(path5, bsfs.getPath("bearRoot:/test.h"));
		assertEquals(path6, bsfs.getPath("bearRoot:/cat/dog/test.h"));
		assertEquals(path6, bsfs.getPath("dogRoot:/test.h"));
		
		// TODO: should we be able to ../../ above a root?
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileNameSpaces#getRoots()}.
	 */
	@Test
	public void testGetRoots() {
		
		/* by default, there should be one root only */
		String roots[] = bsfs.getRoots();
		TestCommon.sortedArraysEqual(roots, new String[] { "root" });
		
		/* add one with a low-alphabetical name */
		int path1 = bsfs.addDirectory("root:/aardvark/bear/cat/donkey");
		assertEquals(ErrorCode.OK, bsfs.addNewRoot("aroot", path1));
		roots = bsfs.getRoots();
		TestCommon.sortedArraysEqual(roots, new String[] { "aroot", "root" });

		/* add one with a high-alphabetical name */
		int path2 = bsfs.addDirectory("root:/aardvark/bear/cat/deer");
		assertEquals(ErrorCode.OK, bsfs.addNewRoot("zebraroot", path2));
		roots = bsfs.getRoots();
		TestCommon.sortedArraysEqual(roots, new String[] { "aroot", "root", "zebraroot" });

		/* adding an invalid root doesn't change anything */
		int path3 = bsfs.addDirectory("root:/aardvark/bear/cat/dragon");
		assertEquals(ErrorCode.INVALID_NAME, bsfs.addNewRoot("zebra root", path3));
		roots = bsfs.getRoots();
		TestCommon.sortedArraysEqual(roots, new String[] { "aroot", "root", "zebraroot" });		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileNameSpaces#moveRootToPath}.
	 */
	@Test
	public void testMoveRootToPath() {
		int path1 = bsfs.addFile("root:/a/b/c/d.c");
		int path2 = bsfs.getPath("root:/a/b/c");
		int path3 = bsfs.getPath("root:/a/b");
		int path4 = bsfs.getPath("root:/a");
		
		/* make sure there's no root at path2, until we add it */
		assertNull(bsfs.getRootAtPath(path2));
		assertEquals(ErrorCode.OK, bsfs.addNewRoot("newRoot", path2));
		assertEquals("newRoot", bsfs.getRootAtPath(path2));
		
		/* now, let's move the root to path3 */
		assertEquals(ErrorCode.OK, bsfs.moveRootToPath("newRoot", path3));
		
		/* make sure it actually moved */
		assertEquals("newRoot", bsfs.getRootAtPath(path3));
		assertEquals(path3, bsfs.getRootPath("newRoot"));
		
		/* try to move a root that doesn't yet exist */
		assertEquals(ErrorCode.NOT_FOUND, bsfs.moveRootToPath("nonRoot", path4));
		
		/* try to move a root onto a non-existent path */
		assertEquals(ErrorCode.BAD_PATH, bsfs.moveRootToPath("newRoot", 1000));
		
		/* try to move a root onto a file (not a directory) */
		assertEquals(ErrorCode.NOT_A_DIRECTORY, bsfs.moveRootToPath("newRoot", path1));
		
		/* try to move a root onto a path that already has a root */
		int path5 = bsfs.addDirectory("/1/2/3");
		int path6 = bsfs.addDirectory("/1/2/3/4");
		
		assertEquals(ErrorCode.OK, bsfs.addNewRoot("root1", path5));
		assertEquals(ErrorCode.OK, bsfs.addNewRoot("root2", path6));
		assertEquals(ErrorCode.ONLY_ONE_ALLOWED, bsfs.moveRootToPath("root2", path5));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileNameSpaces#getRootAtPath}.
	 */
	@Test
	public void testGetRootAtPath() {
		assertNotSame(-1, bsfs.addFile("root:/a/b/c/d.c"));
		int path2 = bsfs.getPath("root:/a/b/c");
		int path3 = bsfs.getPath("root:/a/b");
		int path4 = bsfs.getPath("root:/a");
		
		/* make sure there's no root at path2, until we add it */
		assertNull(bsfs.getRootAtPath(path2));
		assertEquals(ErrorCode.OK, bsfs.addNewRoot("newRoot", path2));
		assertEquals("newRoot", bsfs.getRootAtPath(path2));

		/* make sure there's no root at path3, until we add it */
		assertNull(bsfs.getRootAtPath(path3));
		assertEquals(ErrorCode.OK, bsfs.addNewRoot("newRoot2", path3));
		assertEquals("newRoot2", bsfs.getRootAtPath(path3));

		/* make sure that newRoot2 is still on path2 */
		assertEquals("newRoot", bsfs.getRootAtPath(path2));
		
		/* make sure that path4 still has no root */
		assertNull(bsfs.getRootAtPath(path4));
		
		/* the / path should be attached to "root" */
		assertEquals("root", bsfs.getRootAtPath(bsfs.getPath("root:/")));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileNameSpaces#getEnclosingRoot}.
	 */
	@Test
	public void testGetEnclosingRoot() {
		int path1 = bsfs.addFile("root:/a/b/c/d.c");
		int path2 = bsfs.getPath("root:/a/b/c");
		int path3 = bsfs.getPath("root:/a/b");
		int path4 = bsfs.getPath("root:/a");
		
		/* to start with, "root" will be the enclosing root of path1 */
		assertEquals("root", bsfs.getEnclosingRoot(path1));
		
		/* add an "aroot", which becomes the enclosing root */
		bsfs.addNewRoot("aroot", path4);
		assertEquals("aroot", bsfs.getEnclosingRoot(path1));
		
		/* add a "croot" */
		bsfs.addNewRoot("croot", path2);
		assertEquals("croot", bsfs.getEnclosingRoot(path1));
		
		/* adding a "broot" makes no difference */
		bsfs.addNewRoot("broot", path3);
		assertEquals("croot", bsfs.getEnclosingRoot(path1));
		
		/* for an invalid path ID, expect null */
		assertNull(bsfs.getEnclosingRoot(1000));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileNameSpaces#deleteRoot(int)}.
	 */
	@Test
	public void testDeleteRoot() {
		
		/* add some basic paths and roots */
		assertNotSame(-1, bsfs.addFile("root:/a/b/c/d.c"));
		int path2 = bsfs.getPath("root:/a/b/c");
		int path3 = bsfs.getPath("root:/a/b");
		int path4 = bsfs.getPath("root:/a");
		assertEquals(ErrorCode.OK, bsfs.addNewRoot("newRoot2", path2));
		assertEquals(ErrorCode.OK, bsfs.addNewRoot("newRoot3", path3));
		assertEquals(ErrorCode.OK, bsfs.addNewRoot("newRoot4", path4));
		
		/* check that the roots all exist */
		assertEquals("newRoot2", bsfs.getRootAtPath(path2));
		assertEquals("newRoot3", bsfs.getRootAtPath(path3));		
		assertEquals("newRoot4", bsfs.getRootAtPath(path4));
		
		/* delete one of the roots, but not the others */
		assertEquals(ErrorCode.OK, bsfs.deleteRoot("newRoot3"));
		
		/* check that newRoot3 has gone, but the others remain */
		assertEquals("newRoot2", bsfs.getRootAtPath(path2));
		assertEquals(null, bsfs.getRootAtPath(path3));
		assertEquals(ErrorCode.NOT_FOUND, bsfs.getRootPath("newRoot3"));
		assertEquals("newRoot4", bsfs.getRootAtPath(path4));
		
		/* now delete newRoot4 */
		assertEquals(ErrorCode.OK, bsfs.deleteRoot("newRoot4"));
		assertEquals("newRoot2", bsfs.getRootAtPath(path2));
		assertEquals(null, bsfs.getRootAtPath(path3));
		assertEquals(ErrorCode.NOT_FOUND, bsfs.getRootPath("newRoot3"));
		assertEquals(null, bsfs.getRootAtPath(path4));
		assertEquals(ErrorCode.NOT_FOUND, bsfs.getRootPath("newRoot4"));
		
		/* try to delete a root that doesn't already exist */
		assertEquals(ErrorCode.NOT_FOUND, bsfs.deleteRoot("fakeRoot"));
		
		/* it shouldn't be possible to delete the "root" root - it's always required */
		assertEquals(ErrorCode.CANT_REMOVE, bsfs.deleteRoot("root"));
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for display a path name, prepended with root name.
	 */
	@Test
	public void testGetPathName() {
		int path1 = bsfs.addFile("root:/a/b/c/d/e.h");

		/* show the path, without roots */
		assertEquals("/a/b/c/d/e.h", bsfs.getPathName(path1));

		/* now again, with the top level root */
		assertEquals("root:/a/b/c/d/e.h", bsfs.getPathName(path1, true));
		
		/* add a subroot, and test again */
		bsfs.addNewRoot("subRoot", bsfs.addDirectory("root:/a/b"));
		assertEquals("subRoot:/c/d/e.h", bsfs.getPathName(path1, true));

		/* add a subsubroot, and test again */
		bsfs.addNewRoot("subSubRoot", bsfs.addDirectory("root:/a/b/c"));
		assertEquals("subSubRoot:/d/e.h", bsfs.getPathName(path1, true));
		
		/* the full path, without roots, hasn't changed */
		assertEquals("/a/b/c/d/e.h", bsfs.getPathName(path1));
		
		/* delete the subsubroot, so the subroot is now returned */
		assertEquals(ErrorCode.OK, bsfs.deleteRoot("subSubRoot"));
		assertEquals("subRoot:/c/d/e.h", bsfs.getPathName(path1, true));
	}
}
