/*******************************************************************************
 * Copyright (c) 2014 Arapiki Solutions Inc.
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
import com.buildml.model.IPackageMemberMgr.MemberLocation;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageRootMgr;
import com.buildml.model.ISubPackageMgr;
import com.buildml.utils.errors.ErrorCode;


/**
 * Test cases for the PackageUndoOp class.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestSubPackageUndoOp {

	/** Our BuildStore object, used in many test cases */
	private IBuildStore buildStore;

	/** The managers associated with this BuildStore */
	IPackageMgr pkgMgr;
	ISubPackageMgr subPkgMgr;

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
		subPkgMgr = buildStore.getSubPackageMgr();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test undo/redo of making new sub-packages
	 */
	@Test
	public void testNewSubPackage() {

		int pkgA = pkgMgr.addPackage("PkgA");
		int subPkg = subPkgMgr.newSubPackage(pkgMgr.getMainPackage(), pkgA);
	
		/* record creation of the new sub-package */
		SubPackageUndoOp op = new SubPackageUndoOp(buildStore, subPkg);
		op.recordNewSubPackage();
		op.redo();
		
		/* test it exists and is not trashed */
		assertTrue(subPkgMgr.isSubPackageValid(subPkg));
		assertFalse(subPkgMgr.isSubPackageTrashed(subPkg));
	
		/* undo creation, and test again */
		op.undo();
		assertTrue(subPkgMgr.isSubPackageValid(subPkg));
		assertTrue(subPkgMgr.isSubPackageTrashed(subPkg));
		
		/* redo creation, and test again */
		op.redo();
		assertTrue(subPkgMgr.isSubPackageValid(subPkg));
		assertFalse(subPkgMgr.isSubPackageTrashed(subPkg));
	}
	

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test undo/redo of trashing sub-packages
	 */
	@Test
	public void testRemoveSubPackage() {

		int pkgA = pkgMgr.addPackage("PkgA");
		int subPkg = subPkgMgr.newSubPackage(pkgMgr.getMainPackage(), pkgA);
	
		/* record creation of the new sub-package */
		SubPackageUndoOp op = new SubPackageUndoOp(buildStore, subPkg);
		op.recordRemoveSubPackage();
		op.redo();
		
		/* test it exists and is now trashed */
		assertTrue(subPkgMgr.isSubPackageValid(subPkg));
		assertTrue(subPkgMgr.isSubPackageTrashed(subPkg));
	
		/* undo trashing, and test again */
		op.undo();
		assertTrue(subPkgMgr.isSubPackageValid(subPkg));
		assertFalse(subPkgMgr.isSubPackageTrashed(subPkg));
		
		/* redo trashing, and test again */
		op.redo();
		assertTrue(subPkgMgr.isSubPackageValid(subPkg));
		assertTrue(subPkgMgr.isSubPackageTrashed(subPkg));
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test changing the x,y of sub-packages.
	 */
	@Test
	public void testMoveSubPackage() {

		IPackageMemberMgr pkgMemberMgr = buildStore.getPackageMemberMgr();
		
		/* create a new sub-package with initial location (100, 200) */
		int pkgA = pkgMgr.addPackage("PkgA");
		int subPkgId = subPkgMgr.newSubPackage(pkgMgr.getMainPackage(), pkgA);
		assertEquals(ErrorCode.OK, 
				pkgMemberMgr.setMemberLocation(IPackageMemberMgr.TYPE_SUB_PACKAGE, subPkgId, 100, 200));
		
		/* schedule an operation to move it to (150, 250) */
		SubPackageUndoOp op = new SubPackageUndoOp(buildStore, subPkgId);
		op.recordLocationChange(100, 200, 150, 250);
		op.redo();
		
		/* test that it moved to (150, 250) */
		MemberLocation loc = pkgMemberMgr.getMemberLocation(IPackageMemberMgr.TYPE_SUB_PACKAGE, subPkgId);
		assertNotNull(loc);
		assertEquals(150, loc.x);
		assertEquals(250, loc.y);
		
		/* now undo, and test that it's back to (100, 200) */
		op.undo();
		loc = pkgMemberMgr.getMemberLocation(IPackageMemberMgr.TYPE_SUB_PACKAGE, subPkgId);
		assertNotNull(loc);
		assertEquals(100, loc.x);
		assertEquals(200, loc.y);
		
		/* now redo again */
		op.redo();
		loc = pkgMemberMgr.getMemberLocation(IPackageMemberMgr.TYPE_SUB_PACKAGE, subPkgId);
		assertNotNull(loc);
		assertEquals(150, loc.x);
		assertEquals(250, loc.y);
	}

	/*-------------------------------------------------------------------------------------*/
}
