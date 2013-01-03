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

	/** Our tests set these appropriately */
	private int notifyPkgValue = 0;
	private int notifyHowValue = 0;
	
	/**
	 * Test that addition of two new packages makes new roots available, and set two of the
	 * package roots to different directories.
	 * @throws Exception
	 */
	@Test
	public void testSetPackageRootWithListener() throws Exception {
		getNewBuildStore(tmpTestDir);
		
		int pkgBId = pkgMgr.addPackage("pkgB");
		int pkgAId = pkgMgr.addPackage("pkgA");
		assertTrue(pkgAId > 0);
		assertTrue(pkgBId > 0);
		
		/* set up a listener */
		IPackageMgrListener listener = new IPackageMgrListener() {
			@Override
			public void packageChangeNotification(int pkgId, int how) {
				TestPackageRootMgr.this.notifyPkgValue = pkgId;
				TestPackageRootMgr.this.notifyHowValue = how;
			}
		};
		pkgRootMgr.addListener(listener);
		
		int workspacePathId = fileMgr.getPath("@workspace");
		int pathId = fileMgr.addDirectory("@workspace/a/b");
		
		/* no change to path == no notification */
		assertEquals(ErrorCode.OK, 
				pkgRootMgr.setPackageRoot(pkgAId, IPackageRootMgr.SOURCE_ROOT, workspacePathId));
		assertEquals(0, notifyHowValue);
		assertEquals(0, notifyPkgValue);
		
		/* change to path == notification */
		assertEquals(ErrorCode.OK, 
				pkgRootMgr.setPackageRoot(pkgAId, IPackageRootMgr.SOURCE_ROOT, pathId));
		assertEquals(IPackageMgrListener.CHANGED_ROOTS, notifyHowValue);
		assertEquals(pkgAId, notifyPkgValue);
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
		assertTrue(workspacePathId >= 0);
		
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
		
		/* set @workspace to @root so that we can create packages anywhere */
		assertEquals(ErrorCode.OK, pkgRootMgr.setWorkspaceRoot(pkgRootMgr.getRootPath("root")));

		int dirId = fileMgr.addDirectory(tmpTestDir.toString() + "/valid-dir");
		int fileId = fileMgr.addFile(tmpTestDir.toString() + "/valid-file");
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

	/**
	 * Test the setting and clearing of native roots.
	 * @throws Exception 
	 */
	@Test
	public void testSetNativeRoot() throws Exception {
		getNewBuildStore(tmpTestDir);

		/* add packages and their roots */
		int pkgAId = pkgMgr.addPackage("pkgA");
		assertTrue(pkgAId > 0);
		assertEquals(ErrorCode.OK, 
				pkgRootMgr.setPackageRoot(pkgAId, IPackageRootMgr.SOURCE_ROOT, 
						fileMgr.addDirectory(tmpTestDir.toString() + "/src/dirA")));
		assertEquals(ErrorCode.OK, 
				pkgRootMgr.setPackageRoot(pkgAId, IPackageRootMgr.GENERATED_ROOT, 
						fileMgr.addDirectory(tmpTestDir.toString() + "/obj/dirA")));
		
		String wsNative = pkgRootMgr.getWorkspaceRootNative();
		
		/* Check that roots are initially computed relative to the workspace root */
		assertEquals(wsNative + "/src/dirA", 
				pkgRootMgr.getPackageRootNative(pkgAId, IPackageRootMgr.SOURCE_ROOT));
		assertEquals(wsNative + "/obj/dirA", 
			    pkgRootMgr.getPackageRootNative(pkgAId, IPackageRootMgr.GENERATED_ROOT));
		assertEquals(wsNative + "/src/dirA", pkgRootMgr.getRootNative("pkgA_src"));
		assertEquals(wsNative + "/obj/dirA", pkgRootMgr.getRootNative("pkgA_gen"));

		/* override the source root for pkgA, using an absolute path */
		String cwd = System.getProperty("user.dir");
		assertEquals(ErrorCode.OK, 
				pkgRootMgr.setPackageRootNative(pkgAId, IPackageRootMgr.SOURCE_ROOT, cwd));
		assertEquals(cwd, 
				pkgRootMgr.getPackageRootNative(pkgAId, IPackageRootMgr.SOURCE_ROOT));
		assertEquals(wsNative + "/obj/dirA", 
				pkgRootMgr.getPackageRootNative(pkgAId, IPackageRootMgr.GENERATED_ROOT));
		assertEquals(cwd, 
				pkgRootMgr.getRootNative("pkgA_src"));
		assertEquals(wsNative + "/obj/dirA", pkgRootMgr.getRootNative("pkgA_gen"));
		
		/* clear the source root for pkgA */
		assertEquals(ErrorCode.OK, 
				pkgRootMgr.clearPackageRootNative(pkgAId, IPackageRootMgr.SOURCE_ROOT));
		assertEquals(wsNative + "/src/dirA", 
			     pkgRootMgr.getPackageRootNative(pkgAId, IPackageRootMgr.SOURCE_ROOT));
		assertEquals(wsNative + "/src/dirA", pkgRootMgr.getRootNative("pkgA_src"));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test native paths. By setting native roots, compute the true native location of a file.
	 * 
	 * @throws Exception 
	 */
	@Test
	public void testNativePaths() throws Exception {
		getNewBuildStore(tmpTestDir);

		/* add a package, with the source root in the 'dir' subdirectory */
		int pkgAId = pkgMgr.addPackage("pkgA");
		assertTrue(pkgAId > 0);
		assertEquals(ErrorCode.OK, 
				pkgRootMgr.setPackageRoot(pkgAId, IPackageRootMgr.SOURCE_ROOT, 
											fileMgr.addDirectory("@workspace/dir")));
		
		/* add some paths, but only put one of them within the package */
		int file1 = fileMgr.addFile("@workspace/dir/dir/file1");
		int file2 = fileMgr.addFile("@workspace/dir/dir/file2");
		assertTrue(file1 > 0);
		assertTrue(file2 > 0);		
		assertEquals(ErrorCode.OK, pkgMgr.setFilePackage(file1, pkgAId, IPackageMgr.SCOPE_PUBLIC));
		
		/* compute a native path of a path within the root, and the path without a root */
		String wsRootNative = pkgRootMgr.getWorkspaceRootNative();
		assertEquals(wsRootNative + "/dir/dir/file1", fileMgr.getNativePathName(file1));
		assertEquals(wsRootNative + "/dir/dir/file2", fileMgr.getNativePathName(file2));
		
		/* set the package root to a new absolute path */
		String cwd = System.getProperty("user.dir");
		assertEquals(ErrorCode.OK, 
				pkgRootMgr.setPackageRootNative(pkgAId, IPackageRootMgr.SOURCE_ROOT, cwd));

		/* re-compute a native path of a path within the root */
		assertEquals(cwd + "/dir/file1", fileMgr.getNativePathName(file1));
		assertEquals(wsRootNative + "/dir/dir/file2", fileMgr.getNativePathName(file2));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the setting and clearing of native roots, with errored parameters.
	 * @throws Exception 
	 */
	@Test
	public void testSetNativeRootErrors() throws Exception {
		getNewBuildStore(tmpTestDir);

		/* add packages and their roots */
		int pkgAId = pkgMgr.addPackage("pkgA");
		assertTrue(pkgAId > 0);
		
		/* test setPackageRootNative() with errors */
		assertEquals(ErrorCode.NOT_FOUND, 
				     pkgRootMgr.setPackageRootNative(100, IPackageRootMgr.SOURCE_ROOT, "/"));
		assertEquals(ErrorCode.BAD_VALUE, 
			     pkgRootMgr.setPackageRootNative(pkgAId, 27, "/"));
		assertEquals(ErrorCode.BAD_PATH, 
			     pkgRootMgr.setPackageRootNative(pkgAId, IPackageRootMgr.SOURCE_ROOT, "/bad-path"));
		assertEquals(ErrorCode.BAD_PATH, 
			     pkgRootMgr.setPackageRootNative(pkgAId, IPackageRootMgr.SOURCE_ROOT, "/etc/passwd"));
		
		/* test getPackageRootNative() with errors */
		assertNull(pkgRootMgr.getPackageRootNative(100, IPackageRootMgr.SOURCE_ROOT));
		assertNull(pkgRootMgr.getPackageRootNative(pkgAId, 34));
		
		/* test clearPackageRootNative() with errors */
		assertEquals(ErrorCode.NOT_FOUND, 
				   pkgRootMgr.clearPackageRootNative(200, IPackageRootMgr.GENERATED_ROOT));
		assertEquals(ErrorCode.BAD_VALUE,
			       pkgRootMgr.clearPackageRootNative(pkgAId, 123));

		/* test getRootNative() with errors */
		assertNull(pkgRootMgr.getRootNative("bad-value"));
		assertNull(pkgRootMgr.getRootNative(""));
		assertNull(pkgRootMgr.getRootNative("_src"));
		assertNull(pkgRootMgr.getRootNative("pkgA_gon"));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Try to move a package root above the workspace root - error.
	 * @throws Exception
	 */
	@Test
	public void testMovePackageAboveWorkspace() throws Exception {
		getNewBuildStore(tmpTestDir);

		int dirA = fileMgr.addDirectory("/a");
		int dirAB = fileMgr.addDirectory("/a/b");
		int dirABC = fileMgr.addDirectory("/a/b/c");
		
		/* set @workspace to /a/b */
		assertEquals(ErrorCode.OK, pkgRootMgr.setWorkspaceRoot(dirAB));
		
		/* add a new package - roots will default to @workspace */
		int pkg = pkgMgr.addPackage("pkg");
		assertEquals(dirAB, pkgRootMgr.getPackageRoot(pkg, IPackageRootMgr.SOURCE_ROOT));
		
		/* moving to dirABC is acceptable */
		assertEquals(ErrorCode.OK, 
				     pkgRootMgr.setPackageRoot(pkg, IPackageRootMgr.SOURCE_ROOT, dirABC));
		assertEquals(dirABC, pkgRootMgr.getPackageRoot(pkg, IPackageRootMgr.SOURCE_ROOT));
		
		/* moving to dirA is not acceptable */
		assertEquals(ErrorCode.OUT_OF_RANGE, 
			     pkgRootMgr.setPackageRoot(pkg, IPackageRootMgr.SOURCE_ROOT, dirA));		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
  	 * Test trying to move workspace below a package root - error.
	 * @throws Exception
	 */
	@Test
	public void testMoveWorkspaceBelowPackage() throws Exception {
		getNewBuildStore(tmpTestDir);

		int dirA = fileMgr.addDirectory("/a");
		int dirAB = fileMgr.addDirectory("/a/b");
		int dirABC = fileMgr.addDirectory("/a/b/c");
		
		/* set @workspace to /a */
		assertEquals(ErrorCode.OK, pkgRootMgr.setWorkspaceRoot(dirA));
		
		/* add a new package, setting source/generated root to /a/b */
		int pkg = pkgMgr.addPackage("pkg");
		assertEquals(ErrorCode.OK, 
				pkgRootMgr.setPackageRoot(pkg, IPackageRootMgr.SOURCE_ROOT, dirAB));
		assertEquals(ErrorCode.OK, 
				pkgRootMgr.setPackageRoot(pkg, IPackageRootMgr.GENERATED_ROOT, dirAB));
		
		/* moving workspace root to dirAB is acceptable */
		assertEquals(ErrorCode.OK, pkgRootMgr.setWorkspaceRoot(dirAB));		

		/* moving workspace root to dirABC is NOT acceptable */
		assertEquals(ErrorCode.OUT_OF_RANGE, pkgRootMgr.setWorkspaceRoot(dirABC));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Try to move a package below a file/dir that's in the package.
	 * @throws Exception
	 */
	@Test
	public void testMovePackageBelowFile() throws Exception {
		getNewBuildStore(tmpTestDir);

		int dirA = fileMgr.addDirectory("/a");
		int dirAB = fileMgr.addDirectory("/a/b");
		int dirABC = fileMgr.addDirectory("/a/b/c");
		int fileABD = fileMgr.addFile("/a/b/d/file");
		
		/* set @workspace to /a */
		assertEquals(ErrorCode.OK, pkgRootMgr.setWorkspaceRoot(dirA));
		
		/* add a new package - roots will default to @workspace */
		int pkgId = pkgMgr.addPackage("pkg");

		/* add /a/b/d/file into the package (OK, since root is above it) */
		assertEquals(ErrorCode.OK, pkgMgr.setFilePackage(fileABD, pkgId, IPackageMgr.SCOPE_PUBLIC));
		
		/* move package root to /a/b - acceptable */
		assertEquals(ErrorCode.OK, pkgRootMgr.setPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT, dirAB));
		
		/* move package root to /a/b/c - NOT acceptable since /a/b/d/file would be excluded */
		assertEquals(ErrorCode.OUT_OF_RANGE, 
				pkgRootMgr.setPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT, dirABC));
		
		/* remove file from package, and try again to change the root to /a/b/c */
		assertEquals(ErrorCode.OK, 
				pkgMgr.setFilePackage(fileABD, pkgMgr.getImportPackage(), IPackageMgr.SCOPE_PUBLIC));		
		assertEquals(ErrorCode.OK, 
				pkgRootMgr.setPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT, dirABC));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Try to add a file/dir to to package, at a path that's outside the root.
	 * @throws Exception
	 */
	@Test
	public void testMoveFileOutsidePackage() throws Exception {

		getNewBuildStore(tmpTestDir);

		int dirA = fileMgr.addDirectory("/a");
		int dirABC = fileMgr.addDirectory("/a/b/c");
		int fileABD = fileMgr.addFile("/a/b/d/file");
		
		/* set @workspace to /a */
		assertEquals(ErrorCode.OK, pkgRootMgr.setWorkspaceRoot(dirA));
		
		/* add a new package - roots will default to @workspace */
		int pkgId = pkgMgr.addPackage("pkg");
		
		/* move package root to /a/b/c */
		assertEquals(ErrorCode.OK, 
				pkgRootMgr.setPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT, dirABC));
		
		/* add /a/b/d/file into the package - error, since it's not within /a/b/c */
		assertEquals(ErrorCode.OUT_OF_RANGE, 
				pkgMgr.setFilePackage(fileABD, pkgId, IPackageMgr.SCOPE_PUBLIC));
	}

	/*-------------------------------------------------------------------------------------*/

	// TODO: create a new package, set overrides for the root, then clear them.
	// TODO: create a new package, set an override for the workspace root, then check the package root.

	/*-------------------------------------------------------------------------------------*/
}
