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

import com.buildml.model.IActionTypeMgr;
import com.buildml.model.IPackageMgr;
import com.buildml.model.ISlotTypes;
import com.buildml.model.ISlotTypes.SlotDetails;
import com.buildml.model.impl.BuildStore;
import com.buildml.utils.errors.ErrorCode;

/**
 * Test cases for SlotMgr. These tests are invoked via the ActionTypeMgr or the PackageMgr,
 * rather than SlotMgr directly.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestSlotMgr {

	/** Our BuildStore object, used in many test cases */
	private BuildStore bs;

	/** The managers associated with this BuildStore */
	IActionTypeMgr actionTypeMgr;
	IActionMgr actionMgr;
	IPackageMgr pkgMgr;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		bs = (BuildStore)CommonTestUtils.getEmptyBuildStore();
		
		/* fetch the associated manager objects */
		actionTypeMgr = bs.getActionTypeMgr();
		actionMgr = bs.getActionMgr();
		pkgMgr = bs.getPackageMgr();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper method for validating the content of a SlotDetails object.
	 * 
	 * @param details		The SlotDetails to validate.
	 * @param slotId		The expected slotId.
	 * @param slotName		The expected slotName
	 * @param slotDescr		The expected description (or null to not check)
	 * @param slotType		The expected slotType	
	 * @param slotPos		The expected slotPos
	 * @param slotCard		The expected slotCard
	 */
	private void validateDetails(SlotDetails details, int slotId, String slotName, String slotDescr,
						int slotType, int slotPos, int slotCard) {
		
		assertEquals(slotId, details.slotId);
		assertEquals(slotName, details.slotName);
		if (slotDescr != null) {
			assertEquals(slotDescr, details.slotDescr);
		}
		assertEquals(slotType, details.slotType);
		assertEquals(slotPos, details.slotPos);
		assertEquals(slotCard, details.slotCard);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for getSlotByID(), using the default slots that are part of the
	 * "Shell Command" action type.
	 */
	@Test
	public void testGetSlotByID() {
		
		/* slot 1 is "Input" */
		SlotDetails details = actionTypeMgr.getSlotByID(1);
		validateDetails(details, 1, "Input", null, ISlotTypes.SLOT_TYPE_FILEGROUP,
				ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_OPTIONAL);
		
		/* slot 2 is "Command" */
		details = actionTypeMgr.getSlotByID(2);
		validateDetails(details, 2, "Command", null, ISlotTypes.SLOT_TYPE_TEXT, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED);

		/* slot 3 is "Directory" */
		details = actionTypeMgr.getSlotByID(3);
		validateDetails(details, 3, "Directory", null, ISlotTypes.SLOT_TYPE_DIRECTORY, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED);

		/* slot 4 is "Output" */
		details = actionTypeMgr.getSlotByID(4);
		validateDetails(details, 4, "Output", null, ISlotTypes.SLOT_TYPE_FILEGROUP, 
				ISlotTypes.SLOT_POS_OUTPUT, ISlotTypes.SLOT_CARD_OPTIONAL);
		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for getSlots(). For now we only support the "Shell Command" action,
	 * but with INPUT, PARAMETER and OUTPUT slot types.
	 */
	@Test
	public void testGetSlotsDefault() {
		
		int shellCmdId = actionTypeMgr.getActionTypeByName("Shell Command");
		
		/* get the input slots */
		SlotDetails details[];
		details = actionTypeMgr.getSlots(shellCmdId, ISlotTypes.SLOT_POS_INPUT);
		assertEquals(1, details.length);
		validateDetails(details[0], 1, "Input", null, ISlotTypes.SLOT_TYPE_FILEGROUP, 
				ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_OPTIONAL);
		
		/* get the parameter slots */
		details = actionTypeMgr.getSlots(shellCmdId, ISlotTypes.SLOT_POS_PARAMETER);
		assertEquals(2, details.length);
		validateDetails(details[0], 2, "Command", null, ISlotTypes.SLOT_TYPE_TEXT, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED);
		validateDetails(details[1], 3, "Directory", null, ISlotTypes.SLOT_TYPE_DIRECTORY, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED);
		
		/* get the output slots - don't bother checking the details */
		details = actionTypeMgr.getSlots(shellCmdId, ISlotTypes.SLOT_POS_OUTPUT);
		assertEquals(1, details.length);
		validateDetails(details[0], 4, "Output", null, ISlotTypes.SLOT_TYPE_FILEGROUP, 
				ISlotTypes.SLOT_POS_OUTPUT, ISlotTypes.SLOT_CARD_OPTIONAL);
		
		/* get all the slots - don't bother checking the details */
		details = actionTypeMgr.getSlots(shellCmdId, ISlotTypes.SLOT_POS_ANY);
		assertEquals(4, details.length);
		
		/* get slots for an invalid action type - invalid */
		details = actionTypeMgr.getSlots(1000, ISlotTypes.SLOT_POS_ANY);
		assertNull(details);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for getSlotByName() using default slots.
	 */
	@Test
	public void testGetSlotByName() {
		
		int shellCmdId = actionTypeMgr.getActionTypeByName("Shell Command");

		/* try an invalid name */
		SlotDetails details = actionTypeMgr.getSlotByName(shellCmdId, "Invalid");
		assertNull(details);
		
		/* slot 1 is "Input" */
		details = actionTypeMgr.getSlotByName(shellCmdId, "Input");
		validateDetails(details, 1, "Input", null, ISlotTypes.SLOT_TYPE_FILEGROUP, 
				ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_OPTIONAL);
		
		/* slot 2 is "Command" */
		details = actionTypeMgr.getSlotByName(shellCmdId, "Command");
		validateDetails(details, 2, "Command", null, ISlotTypes.SLOT_TYPE_TEXT, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED);

		/* slot 3 is "Directory" */
		details = actionTypeMgr.getSlotByName(shellCmdId, "Directory");
		validateDetails(details, 3, "Directory", null, ISlotTypes.SLOT_TYPE_DIRECTORY, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED);
		
		/* slot 4 is "Output" */
		details = actionTypeMgr.getSlotByName(shellCmdId, "Output");
		validateDetails(details, 4, "Output", null, ISlotTypes.SLOT_TYPE_FILEGROUP, 
				ISlotTypes.SLOT_POS_OUTPUT, ISlotTypes.SLOT_CARD_OPTIONAL);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for newSlot(). Create and validate a number of valid slot types, for
	 * both packages and action types.
	 */
	@Test
	public void testNewSlot() {
		
		/* 
		 * Create two new packages, which we'll add slots to. For now we'll only use
		 * the "Shell Command" action type.
		 */
		int pkgAId = pkgMgr.addPackage("packageA");
		int pkgBId = pkgMgr.addPackage("packageB");
		int actionTypeId = actionTypeMgr.getActionTypeByName("Shell Command");
		
		/*
		 * Add all the new slots upfront, then validate them afterwards once they're all
		 * in the database.
		 */
		int slotId1 = actionTypeMgr.newSlot(actionTypeId, "fileGroup1", "slotId1", ISlotTypes.SLOT_TYPE_FILEGROUP, 
				ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		int slotId2 = pkgMgr.newSlot(pkgAId, "fileGroup1", "slotId2", ISlotTypes.SLOT_TYPE_FILEGROUP, 
				ISlotTypes.SLOT_POS_OUTPUT, ISlotTypes.SLOT_CARD_OPTIONAL, null, null);
		int slotId3 = pkgMgr.newSlot(pkgBId, "fileGroup2", "slotId3", ISlotTypes.SLOT_TYPE_FILEGROUP,
				ISlotTypes.SLOT_POS_OUTPUT, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		int slotId4 = pkgMgr.newSlot(pkgAId, "param1", "slotId4", ISlotTypes.SLOT_TYPE_BOOLEAN,
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		int slotId5 = actionTypeMgr.newSlot(actionTypeId, "param2", "slotId5", ISlotTypes.SLOT_TYPE_INTEGER,
				ISlotTypes.SLOT_POS_LOCAL, ISlotTypes.SLOT_CARD_OPTIONAL, null, null);
		int slotId6 = actionTypeMgr.newSlot(actionTypeId, "param3", "slotId6", ISlotTypes.SLOT_TYPE_TEXT,
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		int slotId7 = actionTypeMgr.newSlot(actionTypeId, "multiFileGroup", "slotId7", ISlotTypes.SLOT_TYPE_FILEGROUP,
				ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_MULTI, null, null);
		
		/* 
		 * Validate a required SLOT_TYPE_FILEGROUP as a SLOT_POS_INPUT for shell actions.
		 */
		assertTrue(slotId1 >= 0);
		SlotDetails details = actionTypeMgr.getSlotByName(actionTypeId, "fileGroup1");
		assertNotNull(details);
		validateDetails(details, slotId1, "fileGroup1", "slotId1",
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_REQUIRED);
		details = actionTypeMgr.getSlotByID(slotId1);
		assertNotNull(details);
		validateDetails(details, slotId1, "fileGroup1", "slotId1",
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_REQUIRED);
		
		/* 
		 * Validate a non-required SLOT_TYPE_FILEGROUP as a SLOT_POS_OUTPUT for pkgA.
		 */
		assertTrue(slotId2 >= 0);
		assertTrue(slotId1 != slotId2);
		details = pkgMgr.getSlotByName(pkgAId, "fileGroup1");
		assertNotNull(details);
		validateDetails(details, slotId2, "fileGroup1", "slotId2",
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_OUTPUT, ISlotTypes.SLOT_CARD_OPTIONAL);
		details = pkgMgr.getSlotByID(slotId2);
		assertNotNull(details);
		validateDetails(details, slotId2, "fileGroup1", "slotId2",
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_OUTPUT, ISlotTypes.SLOT_CARD_OPTIONAL);
		
		/* 
		 * Validate a required SLOT_TYPE_FILEGROUP as a SLOT_POS_OUTPUT for pkgB
		 */
		assertTrue(slotId3 >= 0);
		assertTrue(slotId1 != slotId3);
		assertTrue(slotId1 != slotId2);
		details = pkgMgr.getSlotByName(pkgBId, "fileGroup2");
		assertNotNull(details);
		validateDetails(details, slotId3, "fileGroup2", "slotId3",
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_OUTPUT, ISlotTypes.SLOT_CARD_REQUIRED);
		details = pkgMgr.getSlotByID(slotId3);
		assertNotNull(details);
		validateDetails(details, slotId3, "fileGroup2", "slotId3",
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_OUTPUT, ISlotTypes.SLOT_CARD_REQUIRED);

		/* 
		 * Validate a required SLOT_TYPE_BOOLEAN as a SLOT_POS_PARAMETER and a SLOT_POS_LOCAL for pkgA
		 */
		assertTrue(slotId4 >= 0);
		details = pkgMgr.getSlotByName(pkgAId, "param1");
		assertNotNull(details);
		validateDetails(details, slotId4, "param1", "slotId4",
				ISlotTypes.SLOT_TYPE_BOOLEAN, ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED);
		details = pkgMgr.getSlotByID(slotId4);
		assertNotNull(details);
		validateDetails(details, slotId4, "param1", "slotId4",
				ISlotTypes.SLOT_TYPE_BOOLEAN, ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED);
		
		/* 
		 * Validate a non-required SLOT_TYPE_INTEGER as a SLOT_POS_LOCAL for shell actions 
		 */
		assertTrue(slotId5 >= 0);
		details = actionTypeMgr.getSlotByName(actionTypeId, "param2");
		assertNotNull(details);
		validateDetails(details, slotId5, "param2", "slotId5",
				ISlotTypes.SLOT_TYPE_INTEGER, ISlotTypes.SLOT_POS_LOCAL, ISlotTypes.SLOT_CARD_OPTIONAL);
		details = actionTypeMgr.getSlotByID(slotId5);
		assertNotNull(details);
		validateDetails(details, slotId5, "param2", "slotId5",
				ISlotTypes.SLOT_TYPE_INTEGER, ISlotTypes.SLOT_POS_LOCAL, ISlotTypes.SLOT_CARD_OPTIONAL);
		
		/* 
		 * Validate a required SLOT_TYPE_TEXT as a SLOT_POS_PARAMETER  for shell actions 
		 */
		assertTrue(slotId6 >= 0);
		details = actionTypeMgr.getSlotByName(actionTypeId, "param3");
		assertNotNull(details);
		validateDetails(details, slotId6, "param3", "slotId6",
				ISlotTypes.SLOT_TYPE_TEXT, ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED);
		details = actionTypeMgr.getSlotByID(slotId6);
		assertNotNull(details);
		validateDetails(details, slotId6, "param3", "slotId6",
				ISlotTypes.SLOT_TYPE_TEXT, ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED);
		
		/* 
		 * Validate a SLOT_TYPE_FILEGROUP as a multi-file group as SLOT_POS_INPUT for shell actions 
		 */
		assertTrue(slotId7 >= 0);
		details = actionTypeMgr.getSlotByName(actionTypeId, "multiFileGroup");
		assertNotNull(details);
		validateDetails(details, slotId7, "multiFileGroup", "slotId7",
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_MULTI);
		details = actionTypeMgr.getSlotByID(slotId7);
		assertNotNull(details);
		validateDetails(details, slotId7, "multiFileGroup", "slotId7",
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_MULTI);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for newSlot() to validate the "description" values.
	 */
	@Test
	public void testDescriptions() {
		
		int pkgId = pkgMgr.addPackage("myPkg");
		
		/* add some valid descriptions that fit on a single line */
		int slot1Id = pkgMgr.newSlot(pkgId, "MySlot1", "This slot description is on a single line.", 
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_OUTPUT, 
				ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		int slot2Id = pkgMgr.newSlot(pkgId, "MySlot2", "This slot description is also on one line.", 
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_OUTPUT, 
				ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		assertTrue(slot1Id > 0);
		assertTrue(slot2Id > 0);
		SlotDetails details = pkgMgr.getSlotByID(slot1Id);
		assertEquals("This slot description is on a single line.", details.slotDescr);
		details = pkgMgr.getSlotByID(slot2Id);
		assertEquals("This slot description is also on one line.", details.slotDescr);
		
		/* add a valid descriptions that fit on multiple lines */
		int slot3Id = pkgMgr.newSlot(pkgId, "MySlot3", 
				"This slot description that goes\nover multiple lines\nand continues for a while.", 
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_OUTPUT, 
				ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		assertTrue(slot3Id > 0);
		details = pkgMgr.getSlotByID(slot3Id);
		assertEquals("This slot description that goes\nover multiple lines\nand continues for a while.", 
				details.slotDescr);

		/* add both empty string and null descriptions, which are also valid */
		int slot4Id = pkgMgr.newSlot(pkgId, "MySlot5",
				"",
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_OUTPUT, 
				ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		int slot5Id = pkgMgr.newSlot(pkgId, "MySlot6",
				null,
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_OUTPUT, 
				ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		assertTrue(slot4Id > 0);
		assertTrue(slot5Id > 0);
		details = pkgMgr.getSlotByID(slot4Id);
		assertEquals("", details.slotDescr);
		details = pkgMgr.getSlotByID(slot5Id);
		assertEquals(null, details.slotDescr);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for changeSlot()
	 */
	@Test
	public void testChangeSlot() {
		
		/* create two new slots that we can manipulate */
		int pkgId = pkgMgr.addPackage("myPkg");
		int slot1Id = pkgMgr.newSlot(pkgId, "MySlot1", "Descr1", 
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_OUTPUT, 
				ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		int slot2Id = pkgMgr.newSlot(pkgId, "MySlot2", "Descr2", 
				ISlotTypes.SLOT_TYPE_TEXT, ISlotTypes.SLOT_POS_PARAMETER, 
				ISlotTypes.SLOT_CARD_OPTIONAL, "Default", null);
		assertTrue(slot1Id > 0);
		assertTrue(slot2Id > 0);
		
		/* test a valid slot change */
		SlotDetails detail = pkgMgr.getSlotByID(slot1Id);
		detail.slotName = "MyNewSlot1";
		detail.slotCard = ISlotTypes.SLOT_CARD_OPTIONAL;
		detail.slotDescr = "My new Slot1 descr";
		assertEquals(ErrorCode.OK, pkgMgr.changeSlot(detail));
		detail = pkgMgr.getSlotByID(slot1Id);
		validateDetails(detail, slot1Id, "MyNewSlot1", "My new Slot1 descr", 
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_OUTPUT,
				ISlotTypes.SLOT_CARD_OPTIONAL);
		
		/* test a second valid slot change - change slot1 again */
		detail = pkgMgr.getSlotByID(slot1Id);
		detail.slotName = "MyNewSlot1Again";
		detail.slotCard = ISlotTypes.SLOT_CARD_REQUIRED;
		detail.slotDescr = "Another description";
		assertEquals(ErrorCode.OK, pkgMgr.changeSlot(detail));
		detail = pkgMgr.getSlotByID(slot1Id);
		validateDetails(detail, slot1Id, "MyNewSlot1Again", "Another description", 
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_OUTPUT,
				ISlotTypes.SLOT_CARD_REQUIRED);
		
		/* test changes to invalid names (slotName) -> INVALID_NAME */
		detail = pkgMgr.getSlotByID(slot2Id);
		detail.slotName = "Bad name!";
		assertEquals(ErrorCode.INVALID_NAME, pkgMgr.changeSlot(detail));
		
		/* test changing to a name that's already in use (slotName) -> ALREADY_USED */
		detail = pkgMgr.getSlotByID(slot2Id);
		detail.slotName = "MyNewSlot1Again";
		assertEquals(ErrorCode.ALREADY_USED, pkgMgr.changeSlot(detail));
		
		/* test changing slotType or slotPos to any new value -> INVALID_OP */
		detail = pkgMgr.getSlotByID(slot2Id);
		detail.slotType = ISlotTypes.SLOT_TYPE_DIRECTORY;
		assertEquals(ErrorCode.INVALID_OP, pkgMgr.changeSlot(detail));
		detail = pkgMgr.getSlotByID(slot2Id);
		detail.slotPos = ISlotTypes.SLOT_POS_LOCAL;
		assertEquals(ErrorCode.INVALID_OP, pkgMgr.changeSlot(detail));

		/* test changing to invalid slotCard value -> OUT_OF_RANGE */
		detail = pkgMgr.getSlotByID(slot2Id);
		detail.slotCard = ISlotTypes.SLOT_CARD_MULTI;
		assertEquals(ErrorCode.OUT_OF_RANGE, pkgMgr.changeSlot(detail));
		
		/* test change to invalid default value -> BAD_VALUE */
		detail = pkgMgr.getSlotByID(slot2Id);
		detail.defaultValue = Integer.valueOf(1);
		assertEquals(ErrorCode.BAD_VALUE, pkgMgr.changeSlot(detail));
		
		/* test with invalid slotId -> NOT_FOUND */
		detail = pkgMgr.getSlotByID(slot2Id);
		detail.slotId = 1234;
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.changeSlot(detail));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for newSlot() with invalid parameters that must be caught.
	 */
	@Test
	public void testNewSlotErrors() {
		
		/*
		 * Tests for new action type slots.
		 */
		int err;
		int actionTypeId = actionTypeMgr.getActionTypeByName("Shell Command");

		/* test with undefined typeId (actionId) - returns ErrorCode.NOT_FOUND */
		err = actionTypeMgr.newSlot(-2, "MySlot", "", ISlotTypes.SLOT_TYPE_BOOLEAN, ISlotTypes.SLOT_POS_PARAMETER, 
									ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		assertEquals(ErrorCode.NOT_FOUND, err);
		err = actionTypeMgr.newSlot(1000, "MySlot", "", ISlotTypes.SLOT_TYPE_BOOLEAN, ISlotTypes.SLOT_POS_PARAMETER, 
				ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		assertEquals(ErrorCode.NOT_FOUND, err);
		
		/* test with a null name - returns ErrorCode.INVALID_NAME */
		err = actionTypeMgr.newSlot(actionTypeId, null, "", ISlotTypes.SLOT_TYPE_BOOLEAN, ISlotTypes.SLOT_POS_PARAMETER, 
				ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		assertEquals(ErrorCode.INVALID_NAME, err);

		/* test with a name that has invalid syntax - returns ErrorCode.INVALID_NAME */
		err = actionTypeMgr.newSlot(actionTypeId, "Bad Name", "", ISlotTypes.SLOT_TYPE_BOOLEAN, ISlotTypes.SLOT_POS_PARAMETER, 
				ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		assertEquals(ErrorCode.INVALID_NAME, err);
		err = actionTypeMgr.newSlot(actionTypeId, "Bad*Name", "", ISlotTypes.SLOT_TYPE_BOOLEAN, ISlotTypes.SLOT_POS_PARAMETER, 
				ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		assertEquals(ErrorCode.INVALID_NAME, err);

		/* add the same name twice with the same typeId - returns ErrorCode.ALREADY_USED */
		err = actionTypeMgr.newSlot(actionTypeId, "Input", "", ISlotTypes.SLOT_TYPE_BOOLEAN, ISlotTypes.SLOT_POS_PARAMETER, 
				ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		assertEquals(ErrorCode.ALREADY_USED, err);
		
		/* add a slotType that's out of range - ErrorCode.INVALID_OP */
		err = actionTypeMgr.newSlot(actionTypeId, "Test1", "", -1, ISlotTypes.SLOT_POS_PARAMETER, 
				ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		assertEquals(ErrorCode.INVALID_OP, err);
		err = actionTypeMgr.newSlot(actionTypeId, "Test2", "", 100, ISlotTypes.SLOT_POS_PARAMETER, 
				ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		assertEquals(ErrorCode.INVALID_OP, err);

		/* add a slotPos that's out of range (includes SLOT_POS_ANY) - ErrorCode.INVALID_OP */
		err = actionTypeMgr.newSlot(actionTypeId, "Test3", "", ISlotTypes.SLOT_TYPE_BOOLEAN, 
				ISlotTypes.SLOT_POS_ANY, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		assertEquals(ErrorCode.INVALID_OP, err);
		err = actionTypeMgr.newSlot(actionTypeId, "Test4", "", ISlotTypes.SLOT_TYPE_BOOLEAN, 
				100, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		assertEquals(ErrorCode.INVALID_OP, err);

		/* add a SLOT_TYPE_FILEGROUP as a SLOT_POS_PARAMETER or SLOT_POS_LOCAL - ErrorCode.INVALID_OP */
		err = actionTypeMgr.newSlot(actionTypeId, "Test5", "", ISlotTypes.SLOT_TYPE_FILEGROUP, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		assertEquals(ErrorCode.INVALID_OP, err);
		err = actionTypeMgr.newSlot(actionTypeId, "Test6", "", ISlotTypes.SLOT_TYPE_FILEGROUP, 
				ISlotTypes.SLOT_POS_LOCAL, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		assertEquals(ErrorCode.INVALID_OP, err);

		/* add a SLOT_TYPE_INTEGER as a SLOT_POS_INPUT or SLOT_POS_OUTPUT - ErrorCode.INVALID_OP */
		err = actionTypeMgr.newSlot(actionTypeId, "Test7", "", ISlotTypes.SLOT_TYPE_INTEGER, 
				ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		assertEquals(ErrorCode.INVALID_OP, err);
		err = actionTypeMgr.newSlot(actionTypeId, "Test8", "", ISlotTypes.SLOT_TYPE_INTEGER, 
				ISlotTypes.SLOT_POS_OUTPUT, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		assertEquals(ErrorCode.INVALID_OP, err);
		
		/* add a slotCard that's out of range - ErrorCode.OUT_OF_RANGE */
		err = actionTypeMgr.newSlot(actionTypeId, "Test9", "", ISlotTypes.SLOT_TYPE_INTEGER, 
				ISlotTypes.SLOT_POS_LOCAL, -1, null, null);
		assertEquals(ErrorCode.OUT_OF_RANGE, err);
		err = actionTypeMgr.newSlot(actionTypeId, "Test10", "", ISlotTypes.SLOT_TYPE_INTEGER, 
				ISlotTypes.SLOT_POS_LOCAL, 10, null, null);
		assertEquals(ErrorCode.OUT_OF_RANGE, err);
		
		/* add a multi-slot of type SLOT_TYPE_INTEGER - ErrorCode.OUT_OF_RANGE */
		err = actionTypeMgr.newSlot(actionTypeId, "Test11", "", ISlotTypes.SLOT_TYPE_INTEGER, 
				ISlotTypes.SLOT_POS_LOCAL, ISlotTypes.SLOT_CARD_MULTI, null, null);
		assertEquals(ErrorCode.OUT_OF_RANGE, err);
		
		/* add a multi-slot of type SLOT_POS_OUTPUT - ErrorCode.OUT_OF_RANGE */
		err = actionTypeMgr.newSlot(actionTypeId, "Test12", "", ISlotTypes.SLOT_TYPE_FILEGROUP, 
				ISlotTypes.SLOT_POS_OUTPUT, ISlotTypes.SLOT_CARD_MULTI, null, null);
		assertEquals(ErrorCode.OUT_OF_RANGE, err);
		
		/* add multiple multi-slots to the same action - returns ErrorCode.OUT_OF_RANGE */
		err = actionTypeMgr.newSlot(actionTypeId, "Test13", "", ISlotTypes.SLOT_TYPE_FILEGROUP, 
				ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_MULTI, null, null);
		assertTrue(err > 0);
		err = actionTypeMgr.newSlot(actionTypeId, "Test14", "", ISlotTypes.SLOT_TYPE_FILEGROUP, 
				ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_MULTI, null, null);
		assertEquals(ErrorCode.OUT_OF_RANGE, err);

		/*
		 * Tests for package type slots - These are practically the same as actionType slots,
		 * with a few minor restrictions.
		 */
		int pkgId = pkgMgr.addPackage("packageA");
		int folderId = pkgMgr.addFolder("folderA");
		
		/* we can't add slots to folders - ErrorCode.NOT_FOUND */
		err = pkgMgr.newSlot(folderId, "Test15", "", ISlotTypes.SLOT_TYPE_BOOLEAN, ISlotTypes.SLOT_POS_PARAMETER, 
				ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		assertEquals(ErrorCode.NOT_FOUND, err);

		/* add a SLOT_TYPE_FILEGROUP as a SLOT_POS_INPUT in a package type - ErrorCode.INVALID_OP */
		err = pkgMgr.newSlot(pkgId, "Test16", "", ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_INPUT, 
				ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		assertEquals(ErrorCode.INVALID_OP, err);		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Try to add two slots with the same name, for the same package. Now try to add the
	 * same name for a different package.
	 */
	@Test
	public void testDuplicateSlotNames() {
		
		int pkg1Id = pkgMgr.addPackage("pkg1");
		int pkg2Id = pkgMgr.addPackage("pkg2");
		
		/* add the slot to pkg1 */
		int slot1Id = pkgMgr.newSlot(pkg1Id, "MySlot", "My Slot", ISlotTypes.SLOT_TYPE_BOOLEAN, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		assertTrue(slot1Id >= 0);
		
		/* try to add it again to pkg1 - should fail */
		int err = pkgMgr.newSlot(pkg1Id, "MySlot", "My Slot (again)", ISlotTypes.SLOT_TYPE_BOOLEAN, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		assertEquals(ErrorCode.ALREADY_USED, err);
		
		/* now try to add it to pkg2 - should succeed */
		int slot2Id = pkgMgr.newSlot(pkg2Id, "MySlot", "My Slot (in a different pkg)", ISlotTypes.SLOT_TYPE_BOOLEAN, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		assertTrue(slot2Id >= 0);
		assertNotSame(slot1Id, slot2Id);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for getter methods, with invalid input values.
	 */
	@Test
	public void testGetsWithErrors() {

		int shellCmdActionId = actionTypeMgr.getActionTypeByName("Shell Command");
		
		/*  validate getSlots() for action types */
		assertNull(actionTypeMgr.getSlots(1234, ISlotTypes.SLOT_POS_INPUT));
		assertNull(actionTypeMgr.getSlots(-4, ISlotTypes.SLOT_POS_INPUT));
		assertNull(actionTypeMgr.getSlots(shellCmdActionId, -1));
		assertNull(actionTypeMgr.getSlots(shellCmdActionId, 1357));
		
		/* validate getSlots() for packages */
		int pkgId = pkgMgr.addPackage("MyPackage");
		assertNull(pkgMgr.getSlots(1234, ISlotTypes.SLOT_POS_INPUT));
		assertNull(pkgMgr.getSlots(-4, ISlotTypes.SLOT_POS_INPUT));
		assertNull(pkgMgr.getSlots(pkgId, -1));
		assertNull(pkgMgr.getSlots(pkgId, 1357));

		/* validate getSlotById() for packages and action types */
		assertNull(actionTypeMgr.getSlotByID(-1));
		assertNull(actionTypeMgr.getSlotByID(1234));
		assertNull(pkgMgr.getSlotByID(-10));
		assertNull(pkgMgr.getSlotByID(12340));
		
		/* validate getSlotByName() for action types */
		assertNull(actionTypeMgr.getSlotByName(1234, "Input"));
		assertNull(actionTypeMgr.getSlotByName(-1, "Input"));
		assertNull(actionTypeMgr.getSlotByName(shellCmdActionId, null));
		assertNull(actionTypeMgr.getSlotByName(shellCmdActionId, "Foo"));

		/* validate getSlotByName() for packages */
		assertNull(pkgMgr.getSlotByName(1234, "Input"));
		assertNull(pkgMgr.getSlotByName(-1, "Input"));
		assertNull(pkgMgr.getSlotByName(pkgId, null));
		assertNull(pkgMgr.getSlotByName(pkgId, "Foo"));	
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for newSlot() to validate that default values are correct, and that
	 * appropriate values can be placed in the slots.
	 */
	@Test
	public void testNewSlotValues() {
	
		/* 
		 * Add two slots of each slot type to the shell action type. One will have null default, the other
		 * will have a reasonable default value.
		 */
		int typeId = actionTypeMgr.getActionTypeByName("Shell Command");
		int fg1SlotId = actionTypeMgr.newSlot(typeId, "FileGroup1", null, ISlotTypes.SLOT_TYPE_FILEGROUP,
							ISlotTypes.SLOT_POS_OUTPUT, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		int fg2SlotId = actionTypeMgr.newSlot(typeId, "FileGroup2", null, ISlotTypes.SLOT_TYPE_FILEGROUP,
							ISlotTypes.SLOT_POS_OUTPUT, ISlotTypes.SLOT_CARD_REQUIRED, Integer.valueOf(5), null);
		int int1SlotId = actionTypeMgr.newSlot(typeId, "Integer1", null, ISlotTypes.SLOT_TYPE_INTEGER,
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		int int2SlotId = actionTypeMgr.newSlot(typeId, "Integer2", null, ISlotTypes.SLOT_TYPE_INTEGER,
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, Integer.valueOf(50), null);
		int text1SlotId = actionTypeMgr.newSlot(typeId, "Text1", null, ISlotTypes.SLOT_TYPE_TEXT,
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		int text2SlotId = actionTypeMgr.newSlot(typeId, "Text2", null, ISlotTypes.SLOT_TYPE_TEXT,
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, "DefaultValue", null);
		int bool1SlotId = actionTypeMgr.newSlot(typeId, "Bool1", null, ISlotTypes.SLOT_TYPE_BOOLEAN,
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		int bool2SlotId = actionTypeMgr.newSlot(typeId, "Bool2", null, ISlotTypes.SLOT_TYPE_BOOLEAN,
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, "true", null);
		int file1SlotId = actionTypeMgr.newSlot(typeId, "File1", null, ISlotTypes.SLOT_TYPE_FILE,
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		int file2SlotId = actionTypeMgr.newSlot(typeId, "File2", null, ISlotTypes.SLOT_TYPE_FILE,
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, Integer.valueOf(25), null);
		int dir1SlotId = actionTypeMgr.newSlot(typeId, "Directory1", null, ISlotTypes.SLOT_TYPE_DIRECTORY,
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		int dir2SlotId = actionTypeMgr.newSlot(typeId, "Directory2", null, ISlotTypes.SLOT_TYPE_DIRECTORY,
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, Integer.valueOf(43), null);
		
		/*
		 * Test the default values of each slot.
		 */
		int actionId = actionMgr.addShellCommandAction(actionMgr.getRootAction("root"), 0, "command");
		assertNull(actionMgr.getSlotValue(actionId, fg1SlotId));
		assertEquals(Integer.valueOf(5), actionMgr.getSlotValue(actionId, fg2SlotId));
		assertNull(actionMgr.getSlotValue(actionId, int1SlotId));
		assertEquals(Integer.valueOf(50), actionMgr.getSlotValue(actionId, int2SlotId));
		assertNull(actionMgr.getSlotValue(actionId, text1SlotId));
		assertEquals("DefaultValue", actionMgr.getSlotValue(actionId, text2SlotId));
		assertNull(actionMgr.getSlotValue(actionId, bool1SlotId));
		assertEquals(Boolean.TRUE, actionMgr.getSlotValue(actionId, bool2SlotId));
		assertNull(actionMgr.getSlotValue(actionId, file1SlotId));
		assertEquals(Integer.valueOf(25), actionMgr.getSlotValue(actionId, file2SlotId));
		assertNull(actionMgr.getSlotValue(actionId, dir1SlotId));
		assertEquals(Integer.valueOf(43), actionMgr.getSlotValue(actionId, dir2SlotId));
		
		/*
		 * Set a range of valid values for each slot type.
		 */
		
		/* SLOT_TYPE_FILEGROUP can have value null or Integer */
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, fg1SlotId, null));
		assertNull(actionMgr.getSlotValue(actionId, fg1SlotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, fg1SlotId, Integer.valueOf(5)));
		assertEquals(Integer.valueOf(5), actionMgr.getSlotValue(actionId, fg1SlotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, fg1SlotId, Integer.valueOf(10)));
		assertEquals(Integer.valueOf(10), actionMgr.getSlotValue(actionId, fg1SlotId));
		
		/* SLOT_TYPE_INTEGER can have value null, Integer or String, but will always return null/Integer */
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, int1SlotId, null));
		assertNull(actionMgr.getSlotValue(actionId, int1SlotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, int1SlotId, Integer.valueOf(15)));
		assertEquals(Integer.valueOf(15), actionMgr.getSlotValue(actionId, int1SlotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, int1SlotId, "20"));
		assertEquals(Integer.valueOf(20), actionMgr.getSlotValue(actionId, int1SlotId));
		
		/* SLOT_TYPE_TEXT can have value null or String */
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, text1SlotId, null));
		assertNull(actionMgr.getSlotValue(actionId, text1SlotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, text1SlotId, "Hello"));
		assertEquals("Hello", actionMgr.getSlotValue(actionId, text1SlotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, text1SlotId, "Test"));
		assertEquals("Test", actionMgr.getSlotValue(actionId, text1SlotId));

		/* SLOT_TYPE_BOOLEAN can have value null, Integer, Boolean or String, but will always return null/Boolean */
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, bool1SlotId, null));
		assertNull(actionMgr.getSlotValue(actionId, bool1SlotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, bool1SlotId, 0));
		assertEquals(Boolean.FALSE, actionMgr.getSlotValue(actionId, bool1SlotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, bool1SlotId, 100));
		assertEquals(Boolean.TRUE, actionMgr.getSlotValue(actionId, bool1SlotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, bool1SlotId, true));
		assertEquals(Boolean.TRUE, actionMgr.getSlotValue(actionId, bool1SlotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, bool1SlotId, false));
		assertEquals(Boolean.FALSE, actionMgr.getSlotValue(actionId, bool1SlotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, bool1SlotId, "true"));
		assertEquals(Boolean.TRUE, actionMgr.getSlotValue(actionId, bool1SlotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, bool1SlotId, "False"));
		assertEquals(Boolean.FALSE, actionMgr.getSlotValue(actionId, bool1SlotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, bool1SlotId, "on"));
		assertEquals(Boolean.TRUE, actionMgr.getSlotValue(actionId, bool1SlotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, bool1SlotId, "off"));
		assertEquals(Boolean.FALSE, actionMgr.getSlotValue(actionId, bool1SlotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, bool1SlotId, "yes"));
		assertEquals(Boolean.TRUE, actionMgr.getSlotValue(actionId, bool1SlotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, bool1SlotId, "no"));
		assertEquals(Boolean.FALSE, actionMgr.getSlotValue(actionId, bool1SlotId));
		
		/* SLOT_TYPE_FILE can have value null or Integer */
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, file1SlotId, null));
		assertNull(actionMgr.getSlotValue(actionId, file1SlotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, file1SlotId, Integer.valueOf(5)));
		assertEquals(Integer.valueOf(5), actionMgr.getSlotValue(actionId, file1SlotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, file1SlotId, Integer.valueOf(10)));
		assertEquals(Integer.valueOf(10), actionMgr.getSlotValue(actionId, file1SlotId));

		/* SLOT_TYPE_DIRECTORY can have value null or Integer */
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, dir1SlotId, null));
		assertNull(actionMgr.getSlotValue(actionId, dir1SlotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, dir1SlotId, Integer.valueOf(51)));
		assertEquals(Integer.valueOf(51), actionMgr.getSlotValue(actionId, dir1SlotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, dir1SlotId, Integer.valueOf(101)));
		assertEquals(Integer.valueOf(101), actionMgr.getSlotValue(actionId, dir1SlotId));

		/*
		 * Test a range of invalid values.
		 */
		
		/* SLOT_TYPE_FILEGROUP */
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setSlotValue(actionId, fg1SlotId, "32x"));
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setSlotValue(actionId, fg1SlotId, "Hello"));
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setSlotValue(actionId, fg1SlotId, false));
		
		/* SLOT_TYPE_INTEGER */
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setSlotValue(actionId, int1SlotId, "640+2"));
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setSlotValue(actionId, int1SlotId, Boolean.FALSE));
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setSlotValue(actionId, int1SlotId, "hello"));
		
		/* SLOT_TYPE_TEXT */
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setSlotValue(actionId, text1SlotId, 5));
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setSlotValue(actionId, text1SlotId, 'a'));
		
		/* SLOT_TYPE_BOOLEAN */
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setSlotValue(actionId, bool1SlotId, "maybe"));
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setSlotValue(actionId, bool1SlotId, 'b'));
		
		/* SLOT_TYPE_FILE */
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setSlotValue(actionId, file1SlotId, "123ab"));
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setSlotValue(actionId, file1SlotId, "Fred"));
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setSlotValue(actionId, file1SlotId, false));

		/* SLOT_TYPE_DIRECTORY */
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setSlotValue(actionId, dir1SlotId, "123ab"));
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setSlotValue(actionId, dir1SlotId, "Fred"));
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setSlotValue(actionId, dir1SlotId, false));

		/*
		 * Set some invalid default values for each type of slot.
		 */
		
		/* SLOT_TYPE_FILEGROUP */
		assertEquals(ErrorCode.BAD_VALUE, actionTypeMgr.newSlot(typeId, "FileGroup101", null,
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_REQUIRED, "135", null));
		assertEquals(ErrorCode.BAD_VALUE, actionTypeMgr.newSlot(typeId, "FileGroup102", null,
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_REQUIRED, true, null));
		
		/* SLOT_TYPE_INTEGER */
		assertEquals(ErrorCode.BAD_VALUE, actionTypeMgr.newSlot(typeId, "Integer101", null,
				ISlotTypes.SLOT_TYPE_INTEGER, ISlotTypes.SLOT_POS_LOCAL, ISlotTypes.SLOT_CARD_REQUIRED, "Five", null));
		assertEquals(ErrorCode.BAD_VALUE, actionTypeMgr.newSlot(typeId, "Integer102", null,
				ISlotTypes.SLOT_TYPE_INTEGER, ISlotTypes.SLOT_POS_LOCAL, ISlotTypes.SLOT_CARD_REQUIRED, 'c', null));
		
		/* SLOT_TYPE_TEXT */
		assertEquals(ErrorCode.BAD_VALUE, actionTypeMgr.newSlot(typeId, "Text101", null,
				ISlotTypes.SLOT_TYPE_TEXT, ISlotTypes.SLOT_POS_LOCAL, ISlotTypes.SLOT_CARD_REQUIRED, 5, null));
		assertEquals(ErrorCode.BAD_VALUE, actionTypeMgr.newSlot(typeId, "Text102", null,
				ISlotTypes.SLOT_TYPE_TEXT, ISlotTypes.SLOT_POS_LOCAL, ISlotTypes.SLOT_CARD_REQUIRED, 'a', null));
				
		/* SLOT_TYPE_BOOLEAN */
		assertEquals(ErrorCode.BAD_VALUE, actionTypeMgr.newSlot(typeId, "Bool101", null,
				ISlotTypes.SLOT_TYPE_BOOLEAN, ISlotTypes.SLOT_POS_LOCAL, ISlotTypes.SLOT_CARD_REQUIRED, "maybe", null));
		assertEquals(ErrorCode.BAD_VALUE, actionTypeMgr.newSlot(typeId, "Bool102", null,
				ISlotTypes.SLOT_TYPE_BOOLEAN, ISlotTypes.SLOT_POS_LOCAL, ISlotTypes.SLOT_CARD_REQUIRED, 'c', null));
		
		/* SLOT_TYPE_FILE */
		assertEquals(ErrorCode.BAD_VALUE, actionTypeMgr.newSlot(typeId, "File101", null,
				ISlotTypes.SLOT_TYPE_FILE, ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, "123", null));
		assertEquals(ErrorCode.BAD_VALUE, actionTypeMgr.newSlot(typeId, "File102", null,
				ISlotTypes.SLOT_TYPE_FILE, ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, true, null));
		
		/* SLOT_TYPE_DIRECTORY */
		assertEquals(ErrorCode.BAD_VALUE, actionTypeMgr.newSlot(typeId, "Dir101", null,
				ISlotTypes.SLOT_TYPE_DIRECTORY, ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, "hello", null));
		assertEquals(ErrorCode.BAD_VALUE, actionTypeMgr.newSlot(typeId, "Dir102", null,
				ISlotTypes.SLOT_TYPE_DIRECTORY, ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, "123", null));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for isSlotSet() and clearSlotValue().
	 */
	@Test
	public void testIsSlotSet() {
		
		/* create a couple of action slots */
		int actionTypeId = actionTypeMgr.getActionTypeByName("Shell Command");
		int slot1Id = actionTypeMgr.newSlot(actionTypeId, "Value1", "Slot 1", ISlotTypes.SLOT_TYPE_BOOLEAN, 
				ISlotTypes.SLOT_POS_LOCAL, ISlotTypes.SLOT_CARD_REQUIRED, true, null);
		int slot2Id = actionTypeMgr.newSlot(actionTypeId, "Value2", "Slot 2", ISlotTypes.SLOT_TYPE_INTEGER, 
				ISlotTypes.SLOT_POS_LOCAL, ISlotTypes.SLOT_CARD_REQUIRED, 5, null);
		
		/* test the default values */
		int actionId = actionMgr.addShellCommandAction(actionMgr.getRootAction("root"), 0, "command");
		assertEquals(Boolean.TRUE, actionMgr.getSlotValue(actionId, slot1Id));
		assertEquals(Integer.valueOf(5), actionMgr.getSlotValue(actionId, slot2Id));
		
		/* confirm that the slots are currently NOT set */
		assertFalse(actionMgr.isSlotSet(actionId, slot1Id));
		assertFalse(actionMgr.isSlotSet(actionId, slot2Id));
		
		/* set slot1, but not slot 2 - repeat the same tests */
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, slot1Id, false));
		assertEquals(Boolean.FALSE, actionMgr.getSlotValue(actionId, slot1Id));
		assertEquals(Integer.valueOf(5), actionMgr.getSlotValue(actionId, slot2Id));
		assertTrue(actionMgr.isSlotSet(actionId, slot1Id));
		assertFalse(actionMgr.isSlotSet(actionId, slot2Id));
		
		/* set slot 2, so both are now set */
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, slot2Id, 10));
		assertEquals(Boolean.FALSE, actionMgr.getSlotValue(actionId, slot1Id));
		assertEquals(Integer.valueOf(10), actionMgr.getSlotValue(actionId, slot2Id));
		assertTrue(actionMgr.isSlotSet(actionId, slot1Id));
		assertTrue(actionMgr.isSlotSet(actionId, slot2Id));
		
		/* now clear slot1, test it's no longer set, and that the value is back at the default */
		actionMgr.clearSlotValue(actionId, slot1Id);
		assertEquals(Boolean.TRUE, actionMgr.getSlotValue(actionId, slot1Id));
		assertEquals(Integer.valueOf(10), actionMgr.getSlotValue(actionId, slot2Id));
		assertFalse(actionMgr.isSlotSet(actionId, slot1Id));
		assertTrue(actionMgr.isSlotSet(actionId, slot2Id));
				
		/* now clear slot2, test it's no longer set, and that the value is back at the default */
		actionMgr.clearSlotValue(actionId, slot2Id);
		assertEquals(Boolean.TRUE, actionMgr.getSlotValue(actionId, slot1Id));
		assertEquals(Integer.valueOf(5), actionMgr.getSlotValue(actionId, slot2Id));
		assertFalse(actionMgr.isSlotSet(actionId, slot1Id));
		assertFalse(actionMgr.isSlotSet(actionId, slot2Id));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for removeSlot()
	 */
	@Test
	public void testRemoveSlot() {
		
		/* add two new slots to the default shell command action type */
		int actionTypeId = actionTypeMgr.getActionTypeByName("Shell Command");
		int slot1Id = actionTypeMgr.newSlot(actionTypeId, "Value1", "Slot 1", ISlotTypes.SLOT_TYPE_INTEGER, 
				ISlotTypes.SLOT_POS_LOCAL, ISlotTypes.SLOT_CARD_REQUIRED, 5, null);
		int slot2Id = actionTypeMgr.newSlot(actionTypeId, "Value2", "Slot 2", ISlotTypes.SLOT_TYPE_INTEGER, 
				ISlotTypes.SLOT_POS_LOCAL, ISlotTypes.SLOT_CARD_REQUIRED, 10, null);
		
		/* validate that the slots are defined - can be fetched by name */
		assertNotNull(actionTypeMgr.getSlotByName(actionTypeId, "Value1"));
		assertNotNull(actionTypeMgr.getSlotByName(actionTypeId, "Value2"));
		
		/* remove one of those slots - validate that it's no longer available */
		assertEquals(ErrorCode.OK, actionTypeMgr.trashSlot(slot1Id));
		
		/* Use getSlotByName to prove it no longer exists */
		assertNull(actionTypeMgr.getSlotByName(actionTypeId, "Value1"));
		assertNotNull(actionTypeMgr.getSlotByName(actionTypeId, "Value2"));
		
		/* Use getSlotById to prove that slot1Id no longer exists */
		assertNull(actionTypeMgr.getSlotByID(slot1Id));
		assertNotNull(actionTypeMgr.getSlotByID(slot2Id));
		
		/* Use getSlots to prove slot1Id no longer exists */
		SlotDetails slots[] = actionTypeMgr.getSlots(actionTypeId, ISlotTypes.SLOT_POS_LOCAL);
		assertEquals(1, slots.length);
		assertEquals(slot2Id, slots[0].slotId);
		
		/* create an action and set a value for the slot */
		int actionId = actionMgr.addShellCommandAction(actionMgr.getRootAction("root"), 0, "command");
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId, slot2Id, 20));
		
		/* try removing the slot - should fail */
		assertEquals(ErrorCode.CANT_REMOVE, actionTypeMgr.trashSlot(slot2Id));
		assertNull(actionTypeMgr.getSlotByName(actionTypeId, "Value1"));
		assertNotNull(actionTypeMgr.getSlotByName(actionTypeId, "Value2"));

		/* clear the action's slot value */
		actionMgr.clearSlotValue(actionId, slot2Id);
		
		/* try removing the slot - should succeed */
		assertEquals(ErrorCode.OK, actionTypeMgr.trashSlot(slot2Id));

		/* confirm that the slots are not searchable any more */
		assertNull(actionTypeMgr.getSlotByName(actionTypeId, "Value1"));
		assertNull(actionTypeMgr.getSlotByName(actionTypeId, "Value2"));
		
		/* revive slot1Id and test that all accessors can now see it again */
		assertEquals(ErrorCode.OK, actionTypeMgr.reviveSlot(slot1Id));
		assertNotNull(actionTypeMgr.getSlotByName(actionTypeId, "Value1"));
		assertNotNull(actionTypeMgr.getSlotByID(slot1Id));
		slots = actionTypeMgr.getSlots(actionTypeId, ISlotTypes.SLOT_POS_LOCAL);
		assertEquals(1, slots.length);
		
		/* try to revive it again - should fail */
		assertEquals(ErrorCode.CANT_REVIVE, actionTypeMgr.reviveSlot(slot1Id));

		/* try removing a non-existent slot - returns ErrorCode.NOT_FOUND */
		assertEquals(ErrorCode.NOT_FOUND, actionTypeMgr.trashSlot(1000));

		/* try reviving a non-existent slot - returns ErrorCode.NOT_FOUND */
		assertEquals(ErrorCode.NOT_FOUND, actionTypeMgr.reviveSlot(1000));
	}
		
	/*-------------------------------------------------------------------------------------*/

}
