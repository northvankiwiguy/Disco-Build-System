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

package com.buildml.model;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.buildml.utils.errors.ErrorCode;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestFileMgrRoots {


	/** Our BuildStore object, used in many test cases */
	private IBuildStore bs;

	/** The managers associated with this BuildStore */
	private IFileMgr fileMgr;
	private IPackageMgr pkgMgr;
	private IPackageRootMgr pkgRootMgr;
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		bs = CommonTestUtils.getEmptyBuildStore();
		
		/* fetch the associated FileMgr, PackageMgr and PackageRootMgr */
		fileMgr = bs.getFileMgr();
		pkgMgr = bs.getPackageMgr();
		pkgRootMgr = bs.getPackageRootMgr();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.FileMgr#addNewRoot(String, int)}.
	 */
	@Test
	public void testAddNewRoot() {

		/* to start with, these roots shouldn't exist */
		assertEquals(ErrorCode.NOT_FOUND, fileMgr.getRootPath("Root1"));
		assertEquals(ErrorCode.NOT_FOUND, fileMgr.getRootPath("Root2"));
		
		/* let's add them to some new paths */
		int path1 = fileMgr.addDirectory("/a/b/c");
		int path2 = fileMgr.addDirectory("/a/b/c/d");		
		assertEquals(ErrorCode.OK, fileMgr.addNewRoot("Root1", path1));
		assertEquals(ErrorCode.OK, fileMgr.addNewRoot("Root2", path2));
		
		/* check that they point to their respective path IDs */
		assertEquals(path1, fileMgr.getRootPath("Root1"));
		assertEquals(path2, fileMgr.getRootPath("Root2"));

		/* Check that other names didn't suddenly appear */
		assertEquals(ErrorCode.NOT_FOUND, fileMgr.getRootPath("Root3"));
		
		/* 
		 * Can't add names with '@', ':', or ' ' in the name, since these symbols are used for
		 * displaying and inputing root names.
		 */
		int path3 = fileMgr.addDirectory("/a/b/c/d/e");
		assertEquals(ErrorCode.INVALID_NAME, fileMgr.addNewRoot("badRoot:Name", path3));
		assertEquals(ErrorCode.INVALID_NAME, fileMgr.addNewRoot("@badRoot", path3));
		assertEquals(ErrorCode.INVALID_NAME, fileMgr.addNewRoot("badRoot Name", path3));
		assertEquals(ErrorCode.INVALID_NAME, fileMgr.addNewRoot("really Bad Name:", path3));
		
		/* adding a root to a file should fail */
		int file1 = fileMgr.addFile("/adam/barney/charlie.c");
		assertEquals(ErrorCode.NOT_A_DIRECTORY, fileMgr.addNewRoot("rootOnFile", file1));
		
		/* adding the same root name to two different paths should fail */
		int path4 = fileMgr.addDirectory("/a/b/c/d/e/f1");
		int path5 = fileMgr.addDirectory("/a/b/c/d/e/f2");
		assertEquals(ErrorCode.OK, fileMgr.addNewRoot("duplicateRoot", path4));
		assertEquals(ErrorCode.ALREADY_USED, fileMgr.addNewRoot("duplicateRoot", path5));
		
		/* adding two roots to the same directory should fail */
		int path6 = fileMgr.addDirectory("/a/b/c/g");
		assertEquals(ErrorCode.OK, fileMgr.addNewRoot("root6", path6));
		assertEquals(ErrorCode.ONLY_ONE_ALLOWED, fileMgr.addNewRoot("root7", path6));
		
		/* adding a root to a non-existent path should fail */
		assertEquals(ErrorCode.BAD_PATH, fileMgr.addNewRoot("root8", 1000));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method to validate that paths are accessible via multiple roots.
	 */
	@Test
	public void testPathAccessViaRoots() {
	
		/* a file added within a root should be accessible via many roots */
		int path1 = fileMgr.addFile("/aardvark/bear/cat/dog/eel.c");
		int path2 = fileMgr.addDirectory("/aardvark/bear");
		int path3 = fileMgr.addDirectory("/aardvark/bear/cat/dog");
		assertEquals(ErrorCode.OK, fileMgr.addNewRoot("bearRoot", path2));
		assertEquals(ErrorCode.OK, fileMgr.addNewRoot("dogRoot", path3));

		/* accessing via the "root" should return the original path ID */
		assertEquals(path1, fileMgr.getPath("/aardvark/bear/cat/dog/eel.c"));
		
		/* so should accessing via the lower-level roots */
		assertEquals(path1, fileMgr.getPath("@bearRoot/cat/dog/eel.c"));
		assertEquals(path1, fileMgr.getPath("@dogRoot/eel.c"));
	
		/* files with the same name, but under different roots should have different IDs */
		int path4 = fileMgr.addFile("@root/test.h");
		int path5 = fileMgr.addFile("@bearRoot/test.h");
		int path6 = fileMgr.addFile("@dogRoot/test.h");
		assertNotSame(path4, path5);
		assertNotSame(path4, path6);
		assertNotSame(path5, path6);
		
		/* but all should be accessible via other roots */
		assertEquals(path4, fileMgr.getPath("@root/test.h"));
		assertEquals(path5, fileMgr.getPath("@root/aardvark/bear/test.h"));
		assertEquals(path6, fileMgr.getPath("@root/aardvark/bear/cat/dog/test.h"));
		assertEquals(path5, fileMgr.getPath("@bearRoot/test.h"));
		assertEquals(path6, fileMgr.getPath("@bearRoot/cat/dog/test.h"));
		assertEquals(path6, fileMgr.getPath("@dogRoot/test.h"));
		
		// TODO: should we be able to ../../ above a root?
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.FileMgr#moveRootToPath}.
	 */
	@Test
	public void testMoveRootToPath() {
		int path1 = fileMgr.addFile("@root/a/b/c/d.c");
		int path2 = fileMgr.getPath("@root/a/b/c");
		int path3 = fileMgr.getPath("@root/a/b");
		int path4 = fileMgr.getPath("@root/a");
		
		/* make sure there's no root at path2, until we add it */
		assertNull(fileMgr.getRootAtPath(path2));
		assertEquals(ErrorCode.OK, fileMgr.addNewRoot("newRoot", path2));
		assertEquals("newRoot", fileMgr.getRootAtPath(path2));
		
		/* now, let's move the root to path3 */
		assertEquals(ErrorCode.OK, fileMgr.moveRootToPath("newRoot", path3));
		
		/* make sure it actually moved */
		assertEquals("newRoot", fileMgr.getRootAtPath(path3));
		assertEquals(path3, fileMgr.getRootPath("newRoot"));
		
		/* try to move a root that doesn't yet exist */
		assertEquals(ErrorCode.NOT_FOUND, fileMgr.moveRootToPath("nonRoot", path4));
		
		/* try to move a root onto a non-existent path */
		assertEquals(ErrorCode.BAD_PATH, fileMgr.moveRootToPath("newRoot", 1000));
		
		/* try to move a root onto a file (not a directory) */
		assertEquals(ErrorCode.NOT_A_DIRECTORY, fileMgr.moveRootToPath("newRoot", path1));
		
		/* try to move a root onto a path that already has a root */
		int path5 = fileMgr.addDirectory("/1/2/3");
		int path6 = fileMgr.addDirectory("/1/2/3/4");
		
		assertEquals(ErrorCode.OK, fileMgr.addNewRoot("root1", path5));
		assertEquals(ErrorCode.OK, fileMgr.addNewRoot("root2", path6));
		assertEquals(ErrorCode.ONLY_ONE_ALLOWED, fileMgr.moveRootToPath("root2", path5));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.FileMgr#getRootAtPath}.
	 */
	@Test
	public void testGetRootAtPath() {
		assertNotSame(ErrorCode.BAD_PATH, fileMgr.addFile("@root/a/b/c/d.c"));
		int path2 = fileMgr.getPath("@root/a/b/c");
		int path3 = fileMgr.getPath("@root/a/b");
		int path4 = fileMgr.getPath("@root/a");
		
		/* make sure there's no root at path2, until we add it */
		assertNull(fileMgr.getRootAtPath(path2));
		assertEquals(ErrorCode.OK, fileMgr.addNewRoot("newRoot", path2));
		assertEquals("newRoot", fileMgr.getRootAtPath(path2));

		/* make sure there's no root at path3, until we add it */
		assertNull(fileMgr.getRootAtPath(path3));
		assertEquals(ErrorCode.OK, fileMgr.addNewRoot("newRoot2", path3));
		assertEquals("newRoot2", fileMgr.getRootAtPath(path3));

		/* make sure that newRoot2 is still on path2 */
		assertEquals("newRoot", fileMgr.getRootAtPath(path2));
		
		/* make sure that path4 still has no root */
		assertNull(fileMgr.getRootAtPath(path4));
		
		/* the / path should be attached to "root" */
		assertEquals("root", fileMgr.getRootAtPath(fileMgr.getPath("@root/")));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.FileMgr#getEnclosingRoot}.
	 */
	@Test
	public void testGetEnclosingRoot() {
		int path1 = fileMgr.addFile("@root/a/b/c/d.c");
		int path2 = fileMgr.getPath("@root/a/b/c");
		int path3 = fileMgr.getPath("@root/a/b");
		int path4 = fileMgr.getPath("@root/a");
		
		/* to start with, "root" will be the enclosing root of path1 */
		assertEquals("root", fileMgr.getEnclosingRoot(path1));
		
		/* add an "aroot", which becomes the enclosing root */
		fileMgr.addNewRoot("aroot", path4);
		assertEquals("aroot", fileMgr.getEnclosingRoot(path1));
		
		/* add a "croot" */
		fileMgr.addNewRoot("croot", path2);
		assertEquals("croot", fileMgr.getEnclosingRoot(path1));
		
		/* adding a "broot" makes no difference */
		fileMgr.addNewRoot("broot", path3);
		assertEquals("croot", fileMgr.getEnclosingRoot(path1));
		
		/* for an invalid path ID, expect null */
		assertNull(fileMgr.getEnclosingRoot(1000));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.FileMgr#deleteRoot(String)}.
	 */
	@Test
	public void testDeleteRoot() {
		
		/* add some basic paths and roots */
		assertNotSame(ErrorCode.BAD_PATH, fileMgr.addFile("@root/a/b/c/d.c"));
		int path2 = fileMgr.getPath("@root/a/b/c");
		int path3 = fileMgr.getPath("@root/a/b");
		int path4 = fileMgr.getPath("@root/a");
		assertEquals(ErrorCode.OK, fileMgr.addNewRoot("newRoot2", path2));
		assertEquals(ErrorCode.OK, fileMgr.addNewRoot("newRoot3", path3));
		assertEquals(ErrorCode.OK, fileMgr.addNewRoot("newRoot4", path4));
		
		/* check that the roots all exist */
		assertEquals("newRoot2", fileMgr.getRootAtPath(path2));
		assertEquals("newRoot3", fileMgr.getRootAtPath(path3));		
		assertEquals("newRoot4", fileMgr.getRootAtPath(path4));
		
		/* delete one of the roots, but not the others */
		assertEquals(ErrorCode.OK, fileMgr.deleteRoot("newRoot3"));
		
		/* check that newRoot3 has gone, but the others remain */
		assertEquals("newRoot2", fileMgr.getRootAtPath(path2));
		assertEquals(null, fileMgr.getRootAtPath(path3));
		assertEquals(ErrorCode.NOT_FOUND, fileMgr.getRootPath("newRoot3"));
		assertEquals("newRoot4", fileMgr.getRootAtPath(path4));
		
		/* now delete newRoot4 */
		assertEquals(ErrorCode.OK, fileMgr.deleteRoot("newRoot4"));
		assertEquals("newRoot2", fileMgr.getRootAtPath(path2));
		assertEquals(null, fileMgr.getRootAtPath(path3));
		assertEquals(ErrorCode.NOT_FOUND, fileMgr.getRootPath("newRoot3"));
		assertEquals(null, fileMgr.getRootAtPath(path4));
		assertEquals(ErrorCode.NOT_FOUND, fileMgr.getRootPath("newRoot4"));
		
		/* try to delete a root that doesn't already exist */
		assertEquals(ErrorCode.NOT_FOUND, fileMgr.deleteRoot("fakeRoot"));
		
		/* it shouldn't be possible to delete the "root" root - it's always required */
		assertEquals(ErrorCode.CANT_REMOVE, fileMgr.deleteRoot("root"));
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for display a path name, prepended with root name.
	 */
	@Test
	public void testGetPathName() {
				
		int path1 = fileMgr.addFile("/a/b/c/d/e.h");
		int path2 = fileMgr.addFile("/a/b/c/d/f.h");
		int path3 = fileMgr.addFile("/g/h/i.h");

		/* show the path, without roots */
		assertEquals("/a/b/c/d/e.h", fileMgr.getPathName(path1));

		/* now again, with the top level root */
		assertEquals("@root/a/b/c/d/e.h", fileMgr.getPathName(path1, true));
		
		/* set the workspace root to /a/b */
		assertEquals(ErrorCode.OK, pkgRootMgr.setWorkspaceRoot(fileMgr.getPath("/a/b")));
		
		/* test again, but this time we should see @workspace, not @root */
		assertEquals("/a/b/c/d/e.h", fileMgr.getPathName(path1));
		assertEquals("@workspace/c/d/e.h", fileMgr.getPathName(path1, true));
		
		/* except when the path is truly outside the workspace */
		assertEquals("@root/g/h/i.h", fileMgr.getPathName(path3, true));
		
		/* add a package, and set the root to /a/b/c */
		int pkgAId = pkgMgr.addPackage("pkgA");
		assertEquals(ErrorCode.OK, 
				pkgRootMgr.setPackageRoot(pkgAId, IPackageRootMgr.SOURCE_ROOT, 
						                  fileMgr.getPath("/a/b/c")));
		
		/* test again, but this time we should still see @workspace */
		assertEquals("/a/b/c/d/e.h", fileMgr.getPathName(path1));
		assertEquals("@workspace/c/d/e.h", fileMgr.getPathName(path1, true));
		assertEquals("@workspace/c/d/f.h", fileMgr.getPathName(path2, true));
		
		/* now add path1 (e.h) into this new package and test again */
		assertEquals(ErrorCode.OK, 
					 pkgMgr.setFilePackage(path1, pkgAId, IPackageMgr.SCOPE_PUBLIC));
		assertEquals("@pkgA_src/d/e.h", fileMgr.getPathName(path1, true));
		assertEquals("@workspace/c/d/f.h", fileMgr.getPathName(path2, true));

		/* now add path2 (f.h) into this new package and test again */
		assertEquals(ErrorCode.OK, 
					pkgMgr.setFilePackage(path2, pkgAId, IPackageMgr.SCOPE_PUBLIC));
		assertEquals("@pkgA_src/d/e.h", fileMgr.getPathName(path1, true));
		assertEquals("@pkgA_src/d/f.h", fileMgr.getPathName(path2, true));

		/* remove path1 from the package */
		assertEquals(ErrorCode.OK, 
				 pkgMgr.setFilePackage(path1, pkgMgr.getImportPackage(), 
						               IPackageMgr.SCOPE_PUBLIC));
		assertEquals("@workspace/c/d/e.h", fileMgr.getPathName(path1, true));
		assertEquals("@pkgA_src/d/f.h", fileMgr.getPathName(path2, true));
		
		/* test that the "/" case is handled properly */
		int rootPath = fileMgr.getPath("/");
		assertEquals("/", fileMgr.getPathName(rootPath, false));
		assertEquals("@root", fileMgr.getPathName(rootPath, true));		
	}
	
	/*-------------------------------------------------------------------------------------*/
}
