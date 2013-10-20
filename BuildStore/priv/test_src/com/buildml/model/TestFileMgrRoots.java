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
	private IPackageMemberMgr pkgMemberMgr;
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
		pkgMemberMgr = bs.getPackageMemberMgr();
		pkgRootMgr = bs.getPackageRootMgr();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method to validate that paths are accessible via multiple roots.
	 */
	@Test
	public void testPathAccessViaRoots() {
	
		/* a file added within a root should be accessible via many roots */
		pkgRootMgr.setWorkspaceRoot(fileMgr.getPath("/"));
		int path1 = fileMgr.addFile("/aardvark/bear/cat/dog/eel.c");
		int path2 = fileMgr.addDirectory("/aardvark/bear");
		int path3 = fileMgr.addDirectory("/aardvark/bear/cat/dog");
		int pkgA = pkgMgr.addPackage("pkgA");
		assertEquals(ErrorCode.OK,
				pkgRootMgr.setPackageRoot(pkgA, IPackageRootMgr.SOURCE_ROOT, path2));
		assertEquals(ErrorCode.OK,
				pkgRootMgr.setPackageRoot(pkgA, IPackageRootMgr.GENERATED_ROOT, path3));

		/* accessing via the "root" should return the original path ID */
		assertEquals(path1, fileMgr.getPath("/aardvark/bear/cat/dog/eel.c"));
		
		/* so should accessing via the lower-level roots */
		assertEquals(path1, fileMgr.getPath("@pkgA_src/cat/dog/eel.c"));
		assertEquals(path1, fileMgr.getPath("@pkgA_gen/eel.c"));
	
		/* files with the same name, but under different roots should have different IDs */
		int path4 = fileMgr.addFile("@root/test.h");
		int path5 = fileMgr.addFile("@pkgA_src/test.h");
		int path6 = fileMgr.addFile("@pkgA_gen/test.h");
		assertNotSame(path4, path5);
		assertNotSame(path4, path6);
		assertNotSame(path5, path6);
		
		/* but all should be accessible via other roots */
		assertEquals(path4, fileMgr.getPath("@root/test.h"));
		assertEquals(path5, fileMgr.getPath("@root/aardvark/bear/test.h"));
		assertEquals(path6, fileMgr.getPath("@root/aardvark/bear/cat/dog/test.h"));
		assertEquals(path5, fileMgr.getPath("@pkgA_src/test.h"));
		assertEquals(path6, fileMgr.getPath("@pkgA_src/cat/dog/test.h"));
		assertEquals(path6, fileMgr.getPath("@pkgA_gen/test.h"));
		
		// TODO: should we be able to ../../ above a root?
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for display a path name, prepended with root name.
	 */
	@Test
	public void testGetPathName() {
				
		int mainPkgId = pkgMgr.getMainPackage();
		assertEquals(ErrorCode.OK, pkgRootMgr.setWorkspaceRoot(fileMgr.getPath("/")));
		
		int path1 = fileMgr.addFile("/a/b/c/d/e.h");
		int path2 = fileMgr.addFile("/a/b/c/d/f.h");
		int path3 = fileMgr.addFile("/g/h/i.h");

		/* show the path, without roots */
		assertEquals("/a/b/c/d/e.h", fileMgr.getPathName(path1));

		/* now again, with the top level root */
		assertEquals("@root/a/b/c/d/e.h", fileMgr.getPathName(path1, true));
		
		/* set the workspace root to /a/b, but only after Main_src and Main_gen have moved below /a/b/ */
		int newMainRootId = fileMgr.getPath("/a/b/c");
		assertEquals(ErrorCode.OK, pkgRootMgr.setPackageRoot(mainPkgId, IPackageRootMgr.SOURCE_ROOT, newMainRootId));
		assertEquals(ErrorCode.OK, pkgRootMgr.setPackageRoot(mainPkgId, IPackageRootMgr.GENERATED_ROOT, newMainRootId));
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
				 pkgMemberMgr.setPackageOfMember(
						 IPackageMemberMgr.TYPE_FILE, path1, pkgAId, IPackageMemberMgr.SCOPE_PUBLIC));
		
		assertEquals("@pkgA_src/d/e.h", fileMgr.getPathName(path1, true));
		assertEquals("@workspace/c/d/f.h", fileMgr.getPathName(path2, true));

		/* now add path2 (f.h) into this new package and test again */
		assertEquals(ErrorCode.OK, 
				pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE, path2, 
												pkgAId, IPackageMemberMgr.SCOPE_PUBLIC));
		assertEquals("@pkgA_src/d/e.h", fileMgr.getPathName(path1, true));
		assertEquals("@pkgA_src/d/f.h", fileMgr.getPathName(path2, true));

		/* remove path1 from the package */
		assertEquals(ErrorCode.OK, 
				 pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE, path1, 
						 					pkgMgr.getImportPackage(), IPackageMemberMgr.SCOPE_PUBLIC));
		assertEquals("@workspace/c/d/e.h", fileMgr.getPathName(path1, true));
		assertEquals("@pkgA_src/d/f.h", fileMgr.getPathName(path2, true));
		
		/* test that the "/" case is handled properly */
		int rootPath = fileMgr.getPath("/");
		assertEquals("/", fileMgr.getPathName(rootPath, false));
		assertEquals("@root", fileMgr.getPathName(rootPath, true));		
	}
	
	/*-------------------------------------------------------------------------------------*/
}
