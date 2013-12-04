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
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMemberMgr.PackageDesc;
import com.buildml.model.IPackageMgr;
import com.buildml.utils.errors.ErrorCode;


/**
 * Test cases for the FileUndoOp class.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestFileUndoOp {

	/** Our BuildStore object, used in many test cases */
	private IBuildStore buildStore;

	/** The managers associated with this BuildStore */
	IFileMgr fileMgr;
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		buildStore = CommonTestUtils.getEmptyBuildStore();
		
		/* fetch the associated manager objects */
		fileMgr = buildStore.getFileMgr();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test undo/redo of removing a path
	 */
	@Test
	public void testRemovePath() {
		int pathId = fileMgr.addFile("/a/b/file.c");
		assertFalse(fileMgr.isPathTrashed(pathId));
		
		/* create and execute a remove operation */
		FileUndoOp op = new FileUndoOp(buildStore, pathId);
		op.recordRemovePath();
		op.redo();
		
		/* test that the path is now gone */
		assertTrue(fileMgr.isPathTrashed(pathId));
		
		/* undo the delete */
		op.undo();
		assertFalse(fileMgr.isPathTrashed(pathId));
		
		/* redo the delete */
		op.redo();
		assertTrue(fileMgr.isPathTrashed(pathId));
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test undo/redo of change a path's package.
	 */
	@Test
	public void testChangePackage() {
		
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		IPackageMemberMgr pkgMemberMgr = buildStore.getPackageMemberMgr();
		
		/* create a file and a couple of packages, and set the file's initial package */
		int pathId = fileMgr.addFile("@workspace/a/file.c");
		int pkgA = pkgMgr.addPackage("PkgA");
		int pkgB = pkgMgr.addPackage("PkgB");
		assertEquals(ErrorCode.OK, pkgMemberMgr.setPackageOfMember(
				IPackageMemberMgr.TYPE_FILE, pathId, pkgA, IPackageMemberMgr.SCOPE_PRIVATE));
		
		/* create an operation that moves it to pkgB/Public */
		FileUndoOp op = new FileUndoOp(buildStore, pathId);
		op.recordChangePackage(pkgA, IPackageMemberMgr.SCOPE_PRIVATE, pkgB, IPackageMemberMgr.SCOPE_PUBLIC);
		
		/* do the operation */
		op.redo();
		PackageDesc desc = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE, pathId);
		assertEquals(pkgB, desc.pkgId);
		assertEquals(IPackageMemberMgr.SCOPE_PUBLIC, desc.pkgScopeId);
	
		/* undo the operation */
		op.undo();
		desc = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE, pathId);
		assertEquals(pkgA, desc.pkgId);
		assertEquals(IPackageMemberMgr.SCOPE_PRIVATE, desc.pkgScopeId);
		
		/* redo the operation */
		op.redo();
		desc = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE, pathId);
		assertEquals(pkgB, desc.pkgId);
		assertEquals(IPackageMemberMgr.SCOPE_PUBLIC, desc.pkgScopeId);
	}

	/*-------------------------------------------------------------------------------------*/
}
