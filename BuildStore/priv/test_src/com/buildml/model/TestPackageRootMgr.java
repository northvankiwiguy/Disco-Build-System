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

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.buildml.utils.errors.ErrorCode;
import com.buildml.utils.os.SystemUtils;

/**
 * Unit tests for the PackageRootMgr class.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestPackageRootMgr {

	/** Our BuildStore object, used in many test cases */
	private IBuildStore buildStore;

	/** The FileMgr object associated with this BuildStore */
	private IFileMgr fileMgr;

	/** The Packages object associated with this BuildStore */
	private IPackageMgr pkgMgr;

	/** The PackageRootMgr object associated with this BuildStore */
	private IPackageRootMgr pkgRootMgr;

	/** All tests are done in this directory */
	private File tmpTestDir;
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * All tests are performed within a file system directory structure:
	 *   .../src/dirA
	 *   .../src/dirB
	 *   .../obj/dirA
	 *   .../obj/dirB
	 *   
	 * @throws Exception 
	 */
	@Before
	public void setUp() throws Exception {
		
		tmpTestDir = SystemUtils.createTempDir();
		new File(tmpTestDir, "src/dirA").mkdirs();
		new File(tmpTestDir, "src/dirB").mkdirs();
		new File(tmpTestDir, "obj/dirA").mkdirs();
		new File(tmpTestDir, "obj/dirB").mkdirs();		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Clean up temporary files after the test completes.
	 * 
	 * @throws Exception 
	 */
	@After
	public void tearDown() throws Exception {
		SystemUtils.deleteDirectory(tmpTestDir);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper function for creating a new BuildStore in a specific directory.
	 * @param dir The directory that should contain the build.bml file.
	 * @throws Exception
	 */
	private void getNewBuildStore(File dir) throws Exception {
		/* get a new empty BuildStore */
		buildStore = CommonTestUtils.getEmptyBuildStore(dir, true);
		fileMgr = buildStore.getFileMgr();
		pkgMgr = buildStore.getPackageMgr();
		pkgRootMgr = buildStore.getPackageRootMgr();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test creation of a new empty BuildStore.
	 * @throws Exception 
	 */
	@Test
	public void testNewBuildStore1() throws Exception {
		
		/* create a new BuildStore at the top level of the workspace */
		getNewBuildStore(tmpTestDir);
		
		/* check that the workspace root is set to temp dir (the directory containing build.bml) */
		assertEquals(fileMgr.getPath(tmpTestDir.toString()), pkgRootMgr.getWorkspaceRoot());
		
		/* 
		 * And on the native file system, the "root" root is always at /, and "workspace" 
		 * will also be at the same temporary path (same as virtual).
		 */
		assertEquals("/", pkgRootMgr.getRootNative("root"));
		assertEquals(tmpTestDir.toString(), pkgRootMgr.getRootNative("workspace"));

		/* check for two roots - in alphabetical order */
		assertArrayEquals(new String[] { "root", "workspace" }, pkgRootMgr.getRoots());
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test creation of a new empty BuildStore in src/ subdirectory.
	 * @throws Exception 
	 */
	@Test
	public void testNewBuildStore2() throws Exception {
		
		/* create a new BuildStore at the top level of the workspace */
		getNewBuildStore(new File(tmpTestDir, "src"));
		
		/* check that the workspace root is set to the src dir (the directory containing build.bml) */
		assertEquals(fileMgr.getPath(tmpTestDir.toString() + "/src"), pkgRootMgr.getWorkspaceRoot());
		
		/* 
		 * And on the native file system, the "root" root is always at /, and "workspace" 
		 * will also be at the same temporary path (same as virtual).
		 */
		assertEquals("/", pkgRootMgr.getRootNative("root"));
		assertEquals(tmpTestDir.toString() + "/src", pkgRootMgr.getRootNative("workspace"));

		/* check for two roots - in alphabetical order */
		assertArrayEquals(new String[] { "root", "workspace" }, pkgRootMgr.getRoots());
	}

	/*-------------------------------------------------------------------------------------*/


	/**
     * Create a build.bml file in src, then saveAs it to src/dirA. Close and re-open it,
	 * then check that native workspace is correct.
	 * @throws Exception 
	 */
	@Test
	public void testMoveBuildStore() throws Exception {
		
		/* create a new BuildStore at the second level of the workspace */
		getNewBuildStore(new File(tmpTestDir, "src"));
		pkgRootMgr.setBuildMLFileDepth(1);

		/* save the build store into the third level. */
		buildStore.saveAs(tmpTestDir.toString() + "/src/dirA/testBuildStore.bml");
		buildStore.close();
		
		/* reopen the buildstore in its new location */
		buildStore = BuildStoreFactory.openBuildStore(
							tmpTestDir.toString() + "/src/dirA/testBuildStore.bml");

		pkgRootMgr = buildStore.getPackageRootMgr();
		assertEquals(tmpTestDir.toString(), pkgRootMgr.getWorkspaceRootNative());
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test that native paths are stored are overridden correctly.
	 * @throws Exception 
	 */
	@Test
	public void testSetWorkspaceRootNative() throws Exception {
		getNewBuildStore(new File(tmpTestDir, "src"));

		/* test that trailing / is not returned */
		assertEquals(ErrorCode.OK, pkgRootMgr.setWorkspaceRootNative("/tmp/"));
		assertEquals("/tmp", pkgRootMgr.getWorkspaceRootNative());

		/* completely change the native root */
		assertEquals(ErrorCode.OK, pkgRootMgr.setWorkspaceRootNative("/etc"));
		assertEquals("/etc", pkgRootMgr.getWorkspaceRootNative());
		
		/* try some invalid roots */
		assertEquals(ErrorCode.NOT_A_DIRECTORY, 
				     pkgRootMgr.setWorkspaceRootNative("/etc/passwd"));
		assertEquals(ErrorCode.NOT_A_DIRECTORY, 
			     pkgRootMgr.setWorkspaceRootNative("/blahblahbadpath"));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test moving build.bml above workspace root - error.
	 * @throws Exception
	 */
	@Test
	public void testMoveBuildStoreInError() throws Exception {
		getNewBuildStore(new File(tmpTestDir, "src"));
		pkgRootMgr.setBuildMLFileDepth(1);

		/* save the build store into an invalid location. */
		buildStore.saveAs(tmpTestDir.toString() + "/src/dirA/testBuildStore.bml");
		buildStore.close();

	}

	/*-------------------------------------------------------------------------------------*/

	// TODO: test setDepth() with -1 or with very large number.
	// TODO: test trying to move workspace below build.bml
	// TODO: test trying to move workspace below another root.
	// TODO: test setting of depth, then close and reopen the database. Check native path.
	
	/*-------------------------------------------------------------------------------------*/

}
