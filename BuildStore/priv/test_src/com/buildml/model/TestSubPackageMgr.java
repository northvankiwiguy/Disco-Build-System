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
		
		/* test that none of the sub-packages are trashed */
		assertFalse(subPkgMgr.isSubPackageTrashed(subPkg1));
		assertFalse(subPkgMgr.isSubPackageTrashed(subPkg2));
		assertFalse(subPkgMgr.isSubPackageTrashed(subPkg3));
		assertFalse(subPkgMgr.isSubPackageTrashed(subPkg4));
		assertFalse(subPkgMgr.isSubPackageTrashed(subPkg5));
		
		/* test validity of sub-packages */
		assertTrue(subPkgMgr.isSubPackageValid(subPkg1));
		assertTrue(subPkgMgr.isSubPackageValid(subPkg2));
		assertFalse(subPkgMgr.isSubPackageValid(-1));
		assertFalse(subPkgMgr.isSubPackageValid(1234));

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
	 * Test trashing of sub-packages.
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
		MemberDesc members[] = pkgMemberMgr.getMembersInPackage(
				pkgA, IPackageMemberMgr.SCOPE_NONE, IPackageMemberMgr.TYPE_ANY);
		assertEquals(1, members.length);
		
		/* remove subPkg2 */
		assertEquals(ErrorCode.OK, subPkgMgr.moveSubPackageToTrash(subPkg2));
		
		/* test that subPkg2 is no longer a member of PkgA */
		members = pkgMemberMgr.getMembersInPackage(
				pkgA, IPackageMemberMgr.SCOPE_NONE, IPackageMemberMgr.TYPE_ANY);
		assertEquals(0, members.length);
		
		/* test that subPkg2 no longer has a type */
		assertEquals(ErrorCode.NOT_FOUND, subPkgMgr.getSubPackageType(subPkg2));

		/* test that subPkg2 can't be deleted twice */
		assertEquals(ErrorCode.NOT_FOUND, subPkgMgr.moveSubPackageToTrash(subPkg2));
		
		/* test that subPkg1 and subPkg3 are still valid */
		assertEquals(pkgA, subPkgMgr.getSubPackageType(subPkg1));
		assertEquals(pkgC, subPkgMgr.getSubPackageType(subPkg3));
		
		/* try to remove invalid sub-package IDs */
		assertEquals(ErrorCode.NOT_FOUND, subPkgMgr.moveSubPackageToTrash(-10));
		assertEquals(ErrorCode.NOT_FOUND, subPkgMgr.moveSubPackageToTrash(567));
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test Reviving of sub-packages.
	 */
	@Test
	public void testReviveSubPackages() {
		
		/* create some new packages (types) */
		int pkgMain = pkgMgr.getMainPackage();
		int pkgA = pkgMgr.addPackage("PkgA");
		int pkgB = pkgMgr.addPackage("PkgB");
		int pkgC = pkgMgr.addPackage("PkgC");

		/* add sub-packages into packages */
		int subPkg1 = subPkgMgr.newSubPackage(pkgMain, pkgA);
		int subPkg2 = subPkgMgr.newSubPackage(pkgA, pkgB);
		int subPkg3 = subPkgMgr.newSubPackage(pkgB, pkgC);
		
		/* validate the validity and trashed-state of all three */
		assertTrue(subPkgMgr.isSubPackageValid(subPkg1));
		assertFalse(subPkgMgr.isSubPackageTrashed(subPkg1));
		assertTrue(subPkgMgr.isSubPackageValid(subPkg2));
		assertFalse(subPkgMgr.isSubPackageTrashed(subPkg2));
		assertTrue(subPkgMgr.isSubPackageValid(subPkg3));
		assertFalse(subPkgMgr.isSubPackageTrashed(subPkg3));
		
		/* trash subPkg1 and subPkg2 */
		assertEquals(ErrorCode.OK, subPkgMgr.moveSubPackageToTrash(subPkg1));
		assertEquals(ErrorCode.OK, subPkgMgr.moveSubPackageToTrash(subPkg2));
		
		/* validate the validity and trashed-state of all three */
		assertTrue(subPkgMgr.isSubPackageValid(subPkg1));
		assertTrue(subPkgMgr.isSubPackageTrashed(subPkg1));
		assertTrue(subPkgMgr.isSubPackageValid(subPkg2));
		assertTrue(subPkgMgr.isSubPackageTrashed(subPkg2));
		assertTrue(subPkgMgr.isSubPackageValid(subPkg3));
		assertFalse(subPkgMgr.isSubPackageTrashed(subPkg3));
		
		/* revive subPkg2 */
		assertEquals(ErrorCode.OK, subPkgMgr.reviveSubPackageFromTrash(subPkg2));

		/* validate the validity and trashed-state of all three */
		assertTrue(subPkgMgr.isSubPackageValid(subPkg1));
		assertTrue(subPkgMgr.isSubPackageTrashed(subPkg1));
		assertTrue(subPkgMgr.isSubPackageValid(subPkg2));
		assertFalse(subPkgMgr.isSubPackageTrashed(subPkg2));
		assertTrue(subPkgMgr.isSubPackageValid(subPkg3));
		assertFalse(subPkgMgr.isSubPackageTrashed(subPkg3));
		
		/* revive subPkg1 */
		assertEquals(ErrorCode.OK, subPkgMgr.reviveSubPackageFromTrash(subPkg1));

		/* validate the validity and trashed-state of all three */
		assertTrue(subPkgMgr.isSubPackageValid(subPkg1));
		assertFalse(subPkgMgr.isSubPackageTrashed(subPkg1));
		assertTrue(subPkgMgr.isSubPackageValid(subPkg2));
		assertFalse(subPkgMgr.isSubPackageTrashed(subPkg2));
		assertTrue(subPkgMgr.isSubPackageValid(subPkg3));
		assertFalse(subPkgMgr.isSubPackageTrashed(subPkg3));
		
		/* re-trash subPkg2 */
		assertEquals(ErrorCode.OK, subPkgMgr.moveSubPackageToTrash(subPkg2));
		assertTrue(subPkgMgr.isSubPackageValid(subPkg2));
		assertTrue(subPkgMgr.isSubPackageTrashed(subPkg2));
		
		/* try to revive invalid sub-packages */
		assertEquals(ErrorCode.NOT_FOUND, subPkgMgr.reviveSubPackageFromTrash(-10));
		assertEquals(ErrorCode.NOT_FOUND, subPkgMgr.reviveSubPackageFromTrash(567));
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
	
	private static int notifyId;
	private static int notifyHow;
	private static int notifyChangeId;
	
	/**
	 * Test the various ways in which ISubPackageMgr can send notifications.
	 */
	@Test
	public void testNotifications() {
		
		/* set up a listener for changes to the ISubPackageMgr */
		ISubPackageMgrListener listener = new ISubPackageMgrListener() {
			@Override
			public void subPackageChangeNotification(int subPkgId, int how, int changeId) {
				TestSubPackageMgr.notifyId = subPkgId;
				TestSubPackageMgr.notifyHow = how;
			}
		};
		subPkgMgr.addListener(listener);

		/* Create a new sub-package */
		int pkgId = pkgMgr.addPackage("MyPkg");
		int subPkgId = subPkgMgr.newSubPackage(pkgMgr.getMainPackage(), pkgId);
		
		/*
		 * Validate that trashing the sub-package provides a notification.
		 */
		notifyHow = notifyId = 0;
		assertEquals(ErrorCode.OK, subPkgMgr.moveSubPackageToTrash(subPkgId));		
		assertEquals(subPkgId, notifyId);
		assertEquals(ISubPackageMgrListener.TRASHED_SUB_PACKAGE, notifyHow);

		/*
		 * Validate that reviving the sub-package provides a notification.
		 */
		notifyHow = notifyId = 0;
		assertEquals(ErrorCode.OK, subPkgMgr.reviveSubPackageFromTrash(subPkgId));		
		assertEquals(subPkgId, notifyId);
		assertEquals(ISubPackageMgrListener.TRASHED_SUB_PACKAGE, notifyHow);
		
		/*
		 * Validate that reviving the sub-package a second time will not have an effect.
		 */
		notifyHow = notifyId = 0;
		assertEquals(ErrorCode.NOT_FOUND, subPkgMgr.reviveSubPackageFromTrash(subPkgId));		
		assertEquals(0, notifyId);
		assertEquals(0, notifyHow);

		/*
		 * Validate that trashing an invalid package will not do anything.
		 */
		notifyHow = notifyId = 0;
		assertEquals(ErrorCode.NOT_FOUND, subPkgMgr.reviveSubPackageFromTrash(12345));		
		assertEquals(0, notifyId);
		assertEquals(0, notifyHow);

		subPkgMgr.removeListener(listener);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test slots that are attached to sub-packages.
	 */
	@Test
	public void testSlots() {
		
		/* create a new package and add three slots to it */
		int pkgId = pkgMgr.addPackage("MyPackage");
		assertTrue(pkgId >= 0);
		int textSlot = pkgMgr.newSlot(pkgId, "textSlot", "This is a text slot", ISlotTypes.SLOT_TYPE_TEXT, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		int intSlot = pkgMgr.newSlot(pkgId, "intSlot", "This is an integer slot", ISlotTypes.SLOT_TYPE_INTEGER, 
				ISlotTypes.SLOT_POS_LOCAL, ISlotTypes.SLOT_CARD_OPTIONAL, null, null);
		int boolSlot = pkgMgr.newSlot(pkgId, "boolSlot", "This is a boolean slot", ISlotTypes.SLOT_TYPE_BOOLEAN, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_OPTIONAL, null, null);
		assertTrue(textSlot > 0 && intSlot > 0 && boolSlot > 0);
		
		/* create a new sub-package */
		int subPkgId = subPkgMgr.newSubPackage(pkgMgr.getMainPackage(), pkgId);
		assertTrue(subPkgId > 0);
		
		/* test getSlotByName */
		assertEquals(textSlot, subPkgMgr.getSlotByName(subPkgId, "textSlot"));
		assertEquals(intSlot, subPkgMgr.getSlotByName(subPkgId, "intSlot"));
		assertEquals(boolSlot, subPkgMgr.getSlotByName(subPkgId, "boolSlot"));
		assertEquals(ErrorCode.NOT_FOUND, subPkgMgr.getSlotByName(subPkgId, "badSlot"));
		assertEquals(ErrorCode.NOT_FOUND, subPkgMgr.getSlotByName(1234, "textSlot"));
		
		/* test setSlotValue */
		assertEquals(ErrorCode.BAD_VALUE, subPkgMgr.setSlotValue(subPkgId, boolSlot, "Invalid"));
		assertEquals(ErrorCode.BAD_VALUE, subPkgMgr.setSlotValue(subPkgId, intSlot, true));
		assertEquals(ErrorCode.NOT_FOUND, subPkgMgr.setSlotValue(1234, textSlot, "Invalid pkgId"));
		assertEquals(ErrorCode.OK, subPkgMgr.setSlotValue(subPkgId, textSlot, "My Value"));
		assertEquals(ErrorCode.OK, subPkgMgr.setSlotValue(subPkgId, intSlot, 5));
		assertEquals(ErrorCode.OK, subPkgMgr.setSlotValue(subPkgId, boolSlot, true));
		
		/* test getSlotValue */
		assertEquals("My Value", subPkgMgr.getSlotValue(subPkgId, textSlot));
		assertEquals(Integer.valueOf(5), subPkgMgr.getSlotValue(subPkgId, intSlot));
		assertEquals(Boolean.TRUE, subPkgMgr.getSlotValue(subPkgId, boolSlot));
		assertNull(subPkgMgr.getSlotValue(subPkgId, 1357));
		assertNull(subPkgMgr.getSlotValue(8987, boolSlot));
	
		/* test isSlotSet and clearSlotValue */
		assertTrue(subPkgMgr.isSlotSet(subPkgId, textSlot));
		assertTrue(subPkgMgr.isSlotSet(subPkgId, intSlot));
		assertTrue(subPkgMgr.isSlotSet(subPkgId, boolSlot));
		subPkgMgr.clearSlotValue(subPkgId, textSlot);
		assertFalse(subPkgMgr.isSlotSet(subPkgId, textSlot));
		assertTrue(subPkgMgr.isSlotSet(subPkgId, intSlot));
		assertTrue(subPkgMgr.isSlotSet(subPkgId, boolSlot));
		subPkgMgr.clearSlotValue(subPkgId, intSlot);
		assertFalse(subPkgMgr.isSlotSet(subPkgId, textSlot));
		assertFalse(subPkgMgr.isSlotSet(subPkgId, intSlot));
		assertTrue(subPkgMgr.isSlotSet(subPkgId, boolSlot));
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test that changes to slot values give notifications.
	 */
	@Test
	public void testSlotNotifications() {
		
		/* set up a listener for changes to the ISubPackageMgr */
		ISubPackageMgrListener listener = new ISubPackageMgrListener() {
			@Override
			public void subPackageChangeNotification(int subPkgId, int how, int changeId) {
				TestSubPackageMgr.notifyId = subPkgId;
				TestSubPackageMgr.notifyHow = how;
				TestSubPackageMgr.notifyChangeId = changeId;
			}
		};
		subPkgMgr.addListener(listener);
		
		/* Create a package and add a slot to that package */
		int pkgId = pkgMgr.addPackage("MyPackage");
		assertTrue(pkgId >= 0);
		int textSlot = pkgMgr.newSlot(pkgId, "textSlot", "This is a text slot", ISlotTypes.SLOT_TYPE_TEXT, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		
		/* Create a new sub-package */
		int subPkgId = subPkgMgr.newSubPackage(pkgMgr.getMainPackage(), pkgId);
		assertTrue(subPkgId > 0);
		
		/* give the slot a value for this sub-package, and check that we get notified */
		notifyId = notifyHow = notifyChangeId = 0;
		assertEquals(ErrorCode.OK, subPkgMgr.setSlotValue(subPkgId, textSlot, "Hello"));
		assertEquals(notifyId, subPkgId);
		assertEquals(notifyHow, ISubPackageMgrListener.CHANGED_SLOT);
		assertEquals(notifyChangeId, textSlot);
		
		/* set a different value, and check for notification */
		notifyId = notifyHow = notifyChangeId = 0;
		assertEquals(ErrorCode.OK, subPkgMgr.setSlotValue(subPkgId, textSlot, "Goodbye"));
		assertEquals(notifyId, subPkgId);
		assertEquals(notifyHow, ISubPackageMgrListener.CHANGED_SLOT);
		assertEquals(notifyChangeId, textSlot);
		
		/* set the same value again, we shouldn't get notified */
		notifyId = notifyHow = notifyChangeId = 0;
		assertEquals(ErrorCode.OK, subPkgMgr.setSlotValue(subPkgId, textSlot, "Goodbye"));
		assertEquals(notifyId, 0);
		assertEquals(notifyHow, 0);
		assertEquals(notifyChangeId, 0);
		
		/* clear the slot value - we should get notified */
		notifyId = notifyHow = notifyChangeId = 0;
		subPkgMgr.clearSlotValue(subPkgId, textSlot);
		assertEquals(notifyId, subPkgId);
		assertEquals(notifyHow, ISubPackageMgrListener.CHANGED_SLOT);
		assertEquals(notifyChangeId, textSlot);
		
		/* clear the slot value again - no notification */
		notifyId = notifyHow = notifyChangeId = 0;
		subPkgMgr.clearSlotValue(subPkgId, textSlot);
		assertEquals(notifyId, 0);
		assertEquals(notifyHow, 0);
		assertEquals(notifyChangeId, 0);
		
		/* we're done */
		subPkgMgr.removeListener(listener);
	}
	
	/*-------------------------------------------------------------------------------------*/


}
