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

package com.buildml.model;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.buildml.model.IPackageMemberMgr.MemberDesc;
import com.buildml.model.IPackageMemberMgr.PackageDesc;
import com.buildml.utils.errors.ErrorCode;
import com.buildml.utils.os.SystemUtils;

/**
 * Unit tests for the PackageRootMgr class.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestSubPackageMgr {

	/** Our BuildStore object, used in many test cases */
	private IBuildStore buildStore;

	/** The IPackageMgr object associated with this BuildStore */
	private IPackageMgr pkgMgr;
	
	/** The IPackageMemberMgr object associated with this BuildStore */
	private IPackageMemberMgr pkgMemberMgr;
	
	/** The ISubPackageMgr object associated with this BuildStore */
	private ISubPackageMgr subPkgMgr;
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set up code, executed at the start of every test case.
	 * @throws Exception 
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		buildStore = CommonTestUtils.getEmptyBuildStore();
		subPkgMgr = buildStore.getSubPackageMgr();	
		pkgMgr = buildStore.getPackageMgr();
		pkgMemberMgr = buildStore.getPackageMemberMgr();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test creation of new sub-packages.
	 */
	@Test
	public void testNewSubPackage() {
	
		/* create some new packages (types) */
		int pkgMain = pkgMgr.getMainPackage();
		int pkgA = pkgMgr.addPackage("PkgA");
		int pkgB = pkgMgr.addPackage("PkgB");
		int pkgC = pkgMgr.addPackage("PkgC");
		
		/* create a sub-package of type A and place it in Main */
		int subPkg1 = subPkgMgr.newSubPackage(pkgMain, pkgA);
		assertTrue(subPkg1 >= 0);

		/* create a second sub-package of type A and place it in Main */
		int subPkg2 = subPkgMgr.newSubPackage(pkgMain, pkgA);
		assertTrue(subPkg2 >= 0);
		assertTrue(subPkg1 != subPkg2);
		
		/* create a sub-package of type B and place it inside A */
		int subPkg3 = subPkgMgr.newSubPackage(pkgA, pkgB);
		assertTrue(subPkg3 >= 0);
		assertTrue(subPkg1 != subPkg3);
		assertTrue(subPkg2 != subPkg3);
		
		/* create a sub-package of type C and place it inside B */
		int subPkg4 = subPkgMgr.newSubPackage(pkgB, pkgC);
		assertTrue(subPkg4 >= 0);

		/* create a sub-package of type C and place it inside A */
		int subPkg5 = subPkgMgr.newSubPackage(pkgA, pkgC);
		assertTrue(subPkg5 >= 0);

		/* test that all sub-packages have the correct parent packages (pkgMemberMgr) */
		PackageDesc desc = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_SUB_PACKAGE, subPkg1);
		assertEquals(pkgMain, desc.pkgId);
		desc = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_SUB_PACKAGE, subPkg2);
		assertEquals(pkgMain, desc.pkgId);
		desc = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_SUB_PACKAGE, subPkg3);
		assertEquals(pkgA, desc.pkgId);
		desc = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_SUB_PACKAGE, subPkg4);
		assertEquals(pkgB, desc.pkgId);
		desc = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_SUB_PACKAGE, subPkg5);
		assertEquals(pkgA, desc.pkgId);
		
		/* fetch the members of pkgMain and ensure they're visible */
		MemberDesc members[] = pkgMemberMgr.getMembersInPackage(
				pkgMain, IPackageMemberMgr.SCOPE_NONE, IPackageMemberMgr.TYPE_ANY);
		assertEquals(2, members.length);
		assertEquals(IPackageMemberMgr.TYPE_SUB_PACKAGE, members[0].memberType);
		assertEquals(IPackageMemberMgr.TYPE_SUB_PACKAGE, members[1].memberType);
		assertTrue((members[0].memberId == subPkg1) && (members[1].memberId == subPkg2) ||
				(members[0].memberId == subPkg2) && (members[1].memberId == subPkg1));

		/* fetch the member of pkgB and ensure it's visible */
		members = pkgMemberMgr.getMembersInPackage(
				pkgB, IPackageMemberMgr.SCOPE_NONE, IPackageMemberMgr.TYPE_ANY);
		assertEquals(1, members.length);
		assertEquals(IPackageMemberMgr.TYPE_SUB_PACKAGE, members[0].memberType);
		assertEquals(subPkg4, members[0].memberId);

		/* test with invalid parentPkgId */
		assertEquals(ErrorCode.BAD_VALUE, subPkgMgr.newSubPackage(-1, pkgA));
		assertEquals(ErrorCode.BAD_VALUE, subPkgMgr.newSubPackage(1000, pkgB));
		
		/* test with invalid pkgTypeId */
		assertEquals(ErrorCode.NOT_FOUND, subPkgMgr.newSubPackage(pkgMain, -2));
		assertEquals(ErrorCode.NOT_FOUND, subPkgMgr.newSubPackage(pkgMain, 1234));
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test creation of new sub-packages, with cycle detection.
	 */
	@Test
	public void testNewSubPackageCycleDetection() {
	
		/* create some new packages (types) */
		int pkgMain = pkgMgr.getMainPackage();
		int pkgA = pkgMgr.addPackage("PkgA");
		int pkgB = pkgMgr.addPackage("PkgB");
		int pkgC = pkgMgr.addPackage("PkgC");
		
		/* 
		 * Test that we can't create a sub-package of type "Main" or "<import>". This 
		 * must always be true, even if there's no cycle yet.
		 */
		assertEquals(ErrorCode.NOT_FOUND, subPkgMgr.newSubPackage(pkgA, pkgMgr.getMainPackage()));
		assertEquals(ErrorCode.NOT_FOUND, subPkgMgr.newSubPackage(pkgA, pkgMgr.getImportPackage()));
		
		/* 
		 * Create a sub-package of type A and place it in Main, a sub-package of type B
		 * within A, and a sub-package of type C within B.
		 */
		int subPkg1 = subPkgMgr.newSubPackage(pkgMain, pkgA);
		assertTrue(subPkg1 >= 0);
		int subPkg2 = subPkgMgr.newSubPackage(pkgA, pkgB);
		assertTrue(subPkg2 >= 0);
		int subPkg3 = subPkgMgr.newSubPackage(pkgB, pkgC);
		assertTrue(subPkg3 >= 0);

		/*
		 * Now try to insert a sub-package of type A within A.
		 */
		assertEquals(ErrorCode.LOOP_DETECTED, subPkgMgr.newSubPackage(pkgA, pkgA));
		
		/*
		 * Now try to insert a sub-package of type A within C.
		 */
		assertEquals(ErrorCode.LOOP_DETECTED, subPkgMgr.newSubPackage(pkgC, pkgA));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test removal of sub-packages.
	 */
	@Test
	public void testRemoveSubPackages() {
		
		/* create some new packages (types) */
		int pkgMain = pkgMgr.getMainPackage();
		int pkgA = pkgMgr.addPackage("PkgA");
		int pkgB = pkgMgr.addPackage("PkgB");
		int pkgC = pkgMgr.addPackage("PkgC");

		/* add sub-packages into packages */
		int subPkg1 = subPkgMgr.newSubPackage(pkgMain, pkgA);
		int subPkg2 = subPkgMgr.newSubPackage(pkgA, pkgB);
		int subPkg3 = subPkgMgr.newSubPackage(pkgB, pkgC);

		/* validate subPkg2 */
		PackageDesc desc = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_SUB_PACKAGE, subPkg2);
		assertEquals(pkgA, desc.pkgId);
		assertEquals(pkgB, subPkgMgr.getSubPackageType(subPkg2));
		
		/* remove subPkg2 */
		assertEquals(ErrorCode.OK, subPkgMgr.removeSubPackage(subPkg2));
		
		/* test that subPkg2 is no longer a member of PkgA */
		desc = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_SUB_PACKAGE, subPkg2);
		assertNull(desc);
		
		/* test that subPkg2 no longer has a type */
		assertEquals(ErrorCode.NOT_FOUND, subPkgMgr.getSubPackageType(subPkg2));

		/* test that subPkg2 can't be deleted twice */
		assertEquals(ErrorCode.NOT_FOUND, subPkgMgr.removeSubPackage(subPkg2));
		
		/* test that subPkg1 and subPkg3 are still valid */
		assertEquals(pkgA, subPkgMgr.getSubPackageType(subPkg1));
		assertEquals(pkgC, subPkgMgr.getSubPackageType(subPkg3));
		
		/* try to remove invalid sub-package IDs */
		assertEquals(ErrorCode.NOT_FOUND, subPkgMgr.removeSubPackage(-10));
		assertEquals(ErrorCode.NOT_FOUND, subPkgMgr.removeSubPackage(567));
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test fetching of sub-packages types.
	 */
	@Test
	public void testSubPackageTypes() {
		
		/* create some new packages (types) */
		int pkgMain = pkgMgr.getMainPackage();
		int pkgA = pkgMgr.addPackage("PkgA");
		int pkgB = pkgMgr.addPackage("PkgB");
		int pkgC = pkgMgr.addPackage("PkgC");

		/* add sub-packages into packages */
		int subPkg1 = subPkgMgr.newSubPackage(pkgMain, pkgA);
		int subPkg2 = subPkgMgr.newSubPackage(pkgMain, pkgA);
		int subPkg3 = subPkgMgr.newSubPackage(pkgA, pkgB);
		int subPkg4 = subPkgMgr.newSubPackage(pkgB, pkgC);
		int subPkg5 = subPkgMgr.newSubPackage(pkgA, pkgC);
		
		/* validate each sub-package's type */
		assertEquals(pkgA, subPkgMgr.getSubPackageType(subPkg1));
		assertEquals(pkgA, subPkgMgr.getSubPackageType(subPkg2));
		assertEquals(pkgB, subPkgMgr.getSubPackageType(subPkg3));
		assertEquals(pkgC, subPkgMgr.getSubPackageType(subPkg4));
		assertEquals(pkgC, subPkgMgr.getSubPackageType(subPkg5));

		/* test error conditions - invalid sub-package ID */
		assertEquals(ErrorCode.NOT_FOUND, subPkgMgr.getSubPackageType(-1));
		assertEquals(ErrorCode.NOT_FOUND, subPkgMgr.getSubPackageType(1234));		
	}
	
	/*-------------------------------------------------------------------------------------*/
}
