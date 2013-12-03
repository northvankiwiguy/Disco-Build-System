/*******************************************************************************
 * Copyright (c) 2013 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.model.undo;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.buildml.model.CommonTestUtils;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageRootMgr;
import com.buildml.utils.errors.ErrorCode;


/**
 * Test cases for the PackageUndoOp class.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestPackageUndoOp {

	/** Our BuildStore object, used in many test cases */
	private IBuildStore buildStore;

	/** The managers associated with this BuildStore */
	IPackageMgr pkgMgr;
	IPackageRootMgr pkgRootMgr;
	
	/** The package we're testing */
	int pkgId;
	
	/** The folder that this package resides within */
	int folderId;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		buildStore = CommonTestUtils.getEmptyBuildStore();
		
		/* fetch the associated manager objects */
		pkgMgr = buildStore.getPackageMgr();
		pkgRootMgr = buildStore.getPackageRootMgr();
		
		/* package we do testing on */
		folderId = pkgMgr.addFolder("MyFolder");
		pkgId = pkgMgr.addPackage("MyPackage");
		pkgMgr.setParent(pkgId, folderId);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test undo/redo of making new packages
	 */
	@Test
	public void testNewPackage() {
		
		PackageUndoOp op = new PackageUndoOp(buildStore, pkgId);
		op.recordNewPackage("MyPackage", folderId);
		
		/* the package already exists */
		assertTrue(pkgMgr.isValid(pkgId));
		assertEquals(folderId, pkgMgr.getParent(pkgId));
		
		/* remove it */
		op.undo();
		assertFalse(pkgMgr.isValid(pkgId));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getId("MyPackage"));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getParent(pkgId));

		/* recreate it - although it'll have a different ID */
		op.redo();
		int newPkgId = pkgMgr.getId("MyPackage");
		assertTrue(newPkgId >= 0);
		assertEquals(folderId, pkgMgr.getParent(newPkgId));
		
		/* remove it again */
		op.undo();
		assertFalse(pkgMgr.isValid(newPkgId));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getId("MyPackage"));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getParent(newPkgId));

		/* recreate it - although it'll have a different ID */
		op.redo();
		int newPkgId2 = pkgMgr.getId("MyPackage");
		assertTrue(newPkgId2 >= 0);
		assertEquals(folderId, pkgMgr.getParent(newPkgId2));
	}
		
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test undo/redo of making new folders
	 */
	@Test
	public void testNewFolder() {
		
		int secondFolderId = pkgMgr.addFolder("SecondFolder");
		pkgMgr.setParent(secondFolderId, folderId);
		
		PackageUndoOp op = new PackageUndoOp(buildStore, secondFolderId);
		op.recordNewFolder("SecondFolder", folderId);
		
		/* the folder already exists */
		assertTrue(pkgMgr.isValid(secondFolderId));
		assertEquals(folderId, pkgMgr.getParent(secondFolderId));
		
		/* remove it */
		op.undo();
		assertFalse(pkgMgr.isValid(secondFolderId));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getId("SecondFolder"));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getParent(secondFolderId));

		/* recreate it - although it'll have a different ID */
		op.redo();
		int newFolderId = pkgMgr.getId("SecondFolder");
		assertTrue(newFolderId >= 0);
		assertEquals(folderId, pkgMgr.getParent(newFolderId));
		
		/* remove it again */
		op.undo();
		assertFalse(pkgMgr.isValid(newFolderId));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getId("SecondFolder"));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getParent(newFolderId));

		/* recreate it - although it'll have a different ID */
		op.redo();
		int newFolderId2 = pkgMgr.getId("SecondFolder");
		assertTrue(newFolderId2 >= 0);
		assertEquals(folderId, pkgMgr.getParent(newFolderId2));
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test undo/redo of removing packages
	 */
	@Test
	public void testRemovePackage() {
	
		PackageUndoOp op = new PackageUndoOp(buildStore, pkgId);
		op.recordRemovePackage("MyPackage", folderId);
		
		/* do the operation and ensure that it no longer exists */
		op.redo();
		assertFalse(pkgMgr.isValid(pkgId));	
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getId("MyPackage"));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getParent(pkgId));
		
		/* undo the removal - although it'll have a different ID */
		op.undo();
		int newPkgId = pkgMgr.getId("MyPackage");
		assertTrue(newPkgId >= 0);
		assertEquals(folderId, pkgMgr.getParent(newPkgId));
		
		/* redo the operation and ensure that it no longer exists */
		op.redo();
		assertFalse(pkgMgr.isValid(newPkgId));	
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getId("MyPackage"));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getParent(newPkgId));
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test undo/redo of removing folders
	 */
	@Test
	public void testRemoveFolder() {
		
		int myFolderId = pkgMgr.addFolder("FolderToDelete");
		pkgMgr.setParent(myFolderId, folderId);

		PackageUndoOp op = new PackageUndoOp(buildStore, myFolderId);
		op.recordRemoveFolder("FolderToDelete", folderId);
		
		/* do the operation and ensure that it no longer exists */
		op.redo();
		assertFalse(pkgMgr.isValid(myFolderId));	
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getId("FolderToDelete"));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getParent(myFolderId));
		
		/* undo the removal - although it'll have a different ID */
		op.undo();
		int newFolderId = pkgMgr.getId("FolderToDelete");
		assertTrue(newFolderId >= 0);
		assertEquals(folderId, pkgMgr.getParent(newFolderId));
		
		/* redo the operation and ensure that it no longer exists */
		op.redo();
		assertFalse(pkgMgr.isValid(newFolderId));	
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getId("FolderToDelete"));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getParent(newFolderId));
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test undo/redo of renaming
	 */
	@Test
	public void testRename() {
		PackageUndoOp op = new PackageUndoOp(buildStore, pkgId);
		op.recordRename("MyPackage", "NewName");
		
		/* do the operation */
		op.redo();
		assertEquals("NewName", pkgMgr.getName(pkgId));
		
		/* undo the operation */
		op.undo();
		assertEquals("MyPackage", pkgMgr.getName(pkgId));
		
		/* redo the operation */
		op.redo();
		assertEquals("NewName", pkgMgr.getName(pkgId));
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test undo/redo of moving
	 */
	@Test
	public void testMove() {
		
		int newFolderId = pkgMgr.addFolder("NewParent");
		
		PackageUndoOp op = new PackageUndoOp(buildStore, pkgId);
		op.recordMove(folderId, newFolderId);
		
		/* do the operation */
		op.redo();
		assertEquals(newFolderId, pkgMgr.getParent(pkgId));
		
		/* undo the operation */
		op.undo();
		assertEquals(folderId, pkgMgr.getParent(pkgId));
		
		/* redo the operation */
		op.redo();
		assertEquals(newFolderId, pkgMgr.getParent(pkgId));		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test undo/redo of changing path roots.
	 */
	@Test
	public void testChangeRoots() {
		
		IFileMgr fileMgr = buildStore.getFileMgr();
		
		int dirId = fileMgr.addDirectory("@MyPackage_src");
		int subDirId1 = fileMgr.addDirectory("@MyPackage_src/subdir1");
		int subDirId2 = fileMgr.addDirectory("@MyPackage_src/subdir2");

		assertTrue(dirId >= 0);
		assertTrue(subDirId1 >= 0);
		assertTrue(subDirId2 >= 0);
		
		PackageUndoOp op = new PackageUndoOp(buildStore, pkgId);
		op.recordRootChange(dirId, dirId, subDirId1, subDirId2);
		
		/* check the initial state */
		assertEquals(dirId, pkgRootMgr.getPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT));
		assertEquals(dirId, pkgRootMgr.getPackageRoot(pkgId, IPackageRootMgr.GENERATED_ROOT));
		
		/* do the operation */
		op.redo();
		assertEquals(subDirId1, pkgRootMgr.getPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT));
		assertEquals(subDirId2, pkgRootMgr.getPackageRoot(pkgId, IPackageRootMgr.GENERATED_ROOT));
		
		/* undo the operation */
		op.undo();
		assertEquals(dirId, pkgRootMgr.getPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT));
		assertEquals(dirId, pkgRootMgr.getPackageRoot(pkgId, IPackageRootMgr.GENERATED_ROOT));
		
		/* do the operation */
		op.redo();
		assertEquals(subDirId1, pkgRootMgr.getPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT));
		assertEquals(subDirId2, pkgRootMgr.getPackageRoot(pkgId, IPackageRootMgr.GENERATED_ROOT));		
	}

	/*-------------------------------------------------------------------------------------*/
	
	
}
