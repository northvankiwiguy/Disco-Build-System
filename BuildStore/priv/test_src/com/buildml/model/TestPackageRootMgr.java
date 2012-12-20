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
	 * Test setBuildMLFileDepth() with invalid input.
	 * @throws Exception
	 */
	@Test
	public void testSetBuildMLFileDepthError() throws Exception {
		getNewBuildStore(tmpTestDir);

		assertEquals(ErrorCode.BAD_PATH, pkgRootMgr.setBuildMLFileDepth(-1));
		assertEquals(ErrorCode.BAD_PATH, pkgRootMgr.setBuildMLFileDepth(10));
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
		assertEquals(ErrorCode.OK, pkgRootMgr.setBuildMLFileDepth(1));

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
		assertEquals(ErrorCode.OK, pkgRootMgr.setBuildMLFileDepth(1));

		/* save the build store into an invalid location. */
		buildStore.saveAs(tmpTestDir.toString() + "/src/dirA/testBuildStore.bml");
		buildStore.close();

	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test setWorkspaceRoot with invalid input.
	 * @throws Exception
	 */
	@Test
	public void testSetWorkspaceRootError() throws Exception {
		getNewBuildStore(tmpTestDir);

		/* test with invalid path ID */
		assertEquals(ErrorCode.BAD_PATH, pkgRootMgr.setWorkspaceRoot(10000));

		/* test with file path ID */
		int fileId = fileMgr.addFile("/a/b/file");
		assertTrue(fileId >= 0);
		assertEquals(ErrorCode.NOT_A_DIRECTORY, pkgRootMgr.setWorkspaceRoot(fileId));

		/* test with trashed path */
		int dir1Id = fileMgr.addDirectory("/a/b/dir1");
		assertTrue(dir1Id >= 0);
		assertEquals(ErrorCode.OK, fileMgr.movePathToTrash(dir1Id));
		assertEquals(ErrorCode.BAD_PATH, pkgRootMgr.setWorkspaceRoot(dir1Id));
		
		/* can't trash a path that has a root attached */
		int dir2Id = fileMgr.addDirectory("/a/b/dir2");
		assertTrue(dir2Id >= 0);
		assertEquals(ErrorCode.OK, pkgRootMgr.setWorkspaceRoot(dir2Id));
		assertEquals(ErrorCode.CANT_REMOVE, fileMgr.movePathToTrash(dir2Id));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test that addition of new packages makes new roots available.
	 * @throws Exception
	 */
	@Test
	public void testSetPackageRoot1() throws Exception {
		getNewBuildStore(tmpTestDir);
	
		int pkgId = pkgMgr.addPackage("pkgA");
		assertTrue(pkgId > 0);
		
		int workspacePathId = pkgRootMgr.getWorkspaceRoot();
		assertTrue(workspacePathId > 0);
		
		/* check that the source and generated roots both refer to "@workspace" */
		int pkgSrcPathId = pkgRootMgr.getPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT);
		int pkgGenPathId = pkgRootMgr.getPackageRoot(pkgId, IPackageRootMgr.GENERATED_ROOT);
		assertEquals(workspacePathId, pkgSrcPathId);
		assertEquals(workspacePathId, pkgGenPathId);
		
		/* check that both roots show up in the list of roots */
		assertArrayEquals(new String[] {"pkgA_gen", "pkgA_src", "root", "workspace"}, 
					      pkgRootMgr.getRoots());
		assertArrayEquals(new String[] {"pkgA_gen", "pkgA_src", "workspace"},
						  pkgRootMgr.getRootsAtPath(workspacePathId));
	}	
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test that addition of two new packages makes new roots available, and set two of the
	 * package roots to different directories.
	 * @throws Exception
	 */
	@Test
	public void testSetPackageRoot2() throws Exception {
		getNewBuildStore(tmpTestDir);
		
		int pkgBId = pkgMgr.addPackage("pkgB");
		int pkgAId = pkgMgr.addPackage("pkgA");
		assertTrue(pkgAId > 0);
		assertTrue(pkgBId > 0);
		
		int workspacePathId = pkgRootMgr.getWorkspaceRoot();
		assertTrue(workspacePathId > 0);
		String workspacePath = fileMgr.getPathName(workspacePathId);
		
		/* check that all the source and generated roots both refer to "@workspace" */
		int pkgSrcPathId = pkgRootMgr.getPackageRoot(pkgAId, IPackageRootMgr.SOURCE_ROOT);
		int pkgGenPathId = pkgRootMgr.getPackageRoot(pkgAId, IPackageRootMgr.GENERATED_ROOT);
		assertEquals(workspacePathId, pkgSrcPathId);
		assertEquals(workspacePathId, pkgGenPathId);
		pkgSrcPathId = pkgRootMgr.getPackageRoot(pkgBId, IPackageRootMgr.SOURCE_ROOT);
		pkgGenPathId = pkgRootMgr.getPackageRoot(pkgBId, IPackageRootMgr.GENERATED_ROOT);
		assertEquals(workspacePathId, pkgSrcPathId);
		assertEquals(workspacePathId, pkgGenPathId);
		
		/* check that all roots show up in the list of roots */
		assertArrayEquals(new String[] {"pkgA_gen", "pkgA_src", "pkgB_gen", "pkgB_src", 
										"root", "workspace"}, pkgRootMgr.getRoots());
		assertArrayEquals(new String[] {"pkgA_gen", "pkgA_src", "pkgB_gen", "pkgB_src", 
										"workspace"}, pkgRootMgr.getRootsAtPath(workspacePathId));

		/* create some new directories to move the roots to */
		int objAPathId = fileMgr.addDirectory(workspacePath + "/obj/pkgA");
		int srcAPathId = fileMgr.addDirectory(workspacePath + "/src/pkgA");
		
		/* move the pkgA roots */
		assertEquals(ErrorCode.OK, pkgRootMgr.setPackageRoot(
										pkgAId, IPackageRootMgr.SOURCE_ROOT, srcAPathId));
		assertEquals(ErrorCode.OK, pkgRootMgr.setPackageRoot(
										pkgAId, IPackageRootMgr.GENERATED_ROOT, objAPathId));

		/* now test the roots again */
		assertArrayEquals(new String[] {"pkgA_gen", "pkgA_src", "pkgB_gen", "pkgB_src", 
				"root", "workspace"}, pkgRootMgr.getRoots());
		assertArrayEquals(new String[] {"pkgB_gen", "pkgB_src", "workspace"}, 
										pkgRootMgr.getRootsAtPath(workspacePathId));
		assertArrayEquals(new String[] {"pkgA_gen"}, pkgRootMgr.getRootsAtPath(objAPathId));
		assertArrayEquals(new String[] {"pkgA_src"}, pkgRootMgr.getRootsAtPath(srcAPathId));
		
		/* test each root individually */
		assertEquals(srcAPathId, pkgRootMgr.getPackageRoot(pkgAId, IPackageRootMgr.SOURCE_ROOT));
		assertEquals(objAPathId, pkgRootMgr.getPackageRoot(pkgAId, IPackageRootMgr.GENERATED_ROOT));
		assertEquals(workspacePathId, pkgRootMgr.getPackageRoot(pkgBId, IPackageRootMgr.SOURCE_ROOT));
		assertEquals(workspacePathId, pkgRootMgr.getPackageRoot(pkgBId, IPackageRootMgr.GENERATED_ROOT));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create and remove a package, checking that the roots have disappeared.
	 * @throws Exception
	 */
	@Test
	public void testRemovePackage() throws Exception {
		getNewBuildStore(tmpTestDir);
		
		int pkgAId = pkgMgr.addPackage("pkgA");
		assertTrue(pkgAId > 0);
		
		int workspacePathId = pkgRootMgr.getWorkspaceRoot();
		assertTrue(workspacePathId > 0);
		
		/* check that the source and generated roots both refer to "@workspace" */
		int pkgSrcPathId = pkgRootMgr.getPackageRoot(pkgAId, IPackageRootMgr.SOURCE_ROOT);
		int pkgGenPathId = pkgRootMgr.getPackageRoot(pkgAId, IPackageRootMgr.GENERATED_ROOT);
		assertEquals(workspacePathId, pkgSrcPathId);
		assertEquals(workspacePathId, pkgGenPathId);
		
		/* check that all roots show up in the list of roots */
		assertArrayEquals(new String[] {"pkgA_gen", "pkgA_src", "root", "workspace"}, 
				                        pkgRootMgr.getRoots());
		
		/* remove the package */
		assertEquals(ErrorCode.OK, pkgMgr.remove(pkgAId));
		
		/* check that the roots have disappeared */
		assertArrayEquals(new String[] {"root", "workspace"}, pkgRootMgr.getRoots());
		assertEquals(ErrorCode.NOT_FOUND, pkgRootMgr.getPackageRoot(pkgAId, IPackageRootMgr.SOURCE_ROOT));
		assertEquals(ErrorCode.NOT_FOUND, pkgRootMgr.getPackageRoot(pkgAId, IPackageRootMgr.GENERATED_ROOT));	
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create and rename a package, checking that the roots also change.
	 * @throws Exception
	 */
	@Test
	public void testRenamePackage() throws Exception {
		getNewBuildStore(tmpTestDir);

		/* add a new package */
		int pkgId = pkgMgr.addPackage("zlib");
		assertTrue(pkgId > 0);
		
		/* set the roots to sub-directories */
		int srcDirA = fileMgr.addDirectory("@workspace/src/dirA");
		assertTrue(srcDirA > 0);
		int genDirA = fileMgr.addDirectory("@workspace/obj/dirA");
		assertTrue(genDirA > 0);
		assertEquals(ErrorCode.OK, 
				pkgRootMgr.setPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT, srcDirA));
		assertEquals(ErrorCode.OK, 
				pkgRootMgr.setPackageRoot(pkgId, IPackageRootMgr.GENERATED_ROOT, genDirA));
		assertEquals(srcDirA, fileMgr.getPath("@zlib_src"));
		assertEquals(genDirA, fileMgr.getPath("@zlib_gen"));
		
		/* now rename the package */
		assertEquals(ErrorCode.OK, pkgMgr.setName(pkgId, "qlib"));
		assertEquals(srcDirA, fileMgr.getPath("@qlib_src"));
		assertEquals(genDirA, fileMgr.getPath("@qlib_gen"));		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create and remove a package, checking that the roots have disappeared.
	 * @throws Exception
	 */
	@Test
	public void testSetPackageRootError() throws Exception {
		getNewBuildStore(tmpTestDir);
				
		int dirId = fileMgr.addDirectory("/valid-dir");
		int fileId = fileMgr.addFile("/valid-file");
		int pkgId = pkgMgr.addPackage("validPkg");
		int folderId = pkgMgr.addFolder("validFolder");
		assertTrue(dirId > 0);
		assertTrue(pkgId > 0);
		assertTrue(folderId > 0);
		
		/* test setPackageRoot with <import> package - error */
		assertEquals(ErrorCode.NOT_FOUND, 
				pkgRootMgr.setPackageRoot(pkgMgr.getImportPackage(), IPackageRootMgr.SOURCE_ROOT, dirId));
		
		/* test setPackageRoot with a folder - error */
		assertEquals(ErrorCode.NOT_FOUND, 
				pkgRootMgr.setPackageRoot(folderId, IPackageRootMgr.SOURCE_ROOT, dirId));
		
		/* test setPackageRoot with invalid packageId */
		assertEquals(ErrorCode.NOT_FOUND, 
				pkgRootMgr.setPackageRoot(1000, IPackageRootMgr.SOURCE_ROOT, dirId));

		/* test setPackageRoot with invalid type */
		assertEquals(ErrorCode.NOT_FOUND, pkgRootMgr.setPackageRoot(pkgId, 10, dirId));

		/* test setPackageRoot with invalid path */
		assertEquals(ErrorCode.BAD_PATH, 
				pkgRootMgr.setPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT, 1000));

		/* test setPackageRoot with file (not directory) */
		assertEquals(ErrorCode.NOT_A_DIRECTORY, 
				pkgRootMgr.setPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT, fileId));

		/* test getPackageRoot with invalid packageId */
		assertEquals(ErrorCode.NOT_FOUND, 
				pkgRootMgr.getPackageRoot(1000, IPackageRootMgr.SOURCE_ROOT));

		/* test getPackageRoot with invalid type */
		assertEquals(ErrorCode.NOT_FOUND, pkgRootMgr.getPackageRoot(pkgId, 10));
		
		/* test removePackageRoot with invalid packageId */
		assertEquals(ErrorCode.NOT_FOUND, 
				pkgRootMgr.removePackageRoot(1000, IPackageRootMgr.SOURCE_ROOT));

		/* test removePackageRoot with invalid type */
		assertEquals(ErrorCode.NOT_FOUND, 
				pkgRootMgr.removePackageRoot(pkgId, 10));

		/* test removePackageRoot of the same root twice */
		assertEquals(ErrorCode.OK, 
				pkgRootMgr.removePackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT));
		assertEquals(ErrorCode.NOT_FOUND, 
				pkgRootMgr.removePackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT));
	}
	
	/*-------------------------------------------------------------------------------------*/

	// TODO: try to move a package root above the workspace root - error.
	// TODO: test trying to move workspace below a package root.
	// TODO: test trying to move workspace below build.bml
	// TODO: try to move a package below a file/dir that's in the package.
	// TODO: try to add a file/dir to to package, at a path that's above the root.

	// TODO: create a new package, set overrides for the root, then clear them.
	// TODO: create a new package, set an override for the workspace root, then check the package root.

	/*-------------------------------------------------------------------------------------*/
}
