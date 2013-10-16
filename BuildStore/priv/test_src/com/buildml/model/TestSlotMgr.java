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
		pkgMgr = bs.getPackageMgr();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper method for validating the content of a SlotDetails object.
	 * 
	 * @param details		The SlotDetails to validate.
	 * @param slotId		The expected slotId.
	 * @param slotName		The expected slotName
	 * @param slotType		The expected slotType	
	 * @param slotPos		The expected slotPos
	 * @param slotCard		The expected slotCard
	 */
	private void validateDetails(SlotDetails details, int slotId, String slotName, int slotType, 
									int slotPos, int slotCard) {
		
		assertEquals(slotId, details.slotId);
		assertEquals(slotName, details.slotName);
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
		validateDetails(details, 1, "Input", ISlotTypes.SLOT_TYPE_FILEGROUP,
				ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_OPTIONAL);
		
		/* slot 2 is "Command" */
		details = actionTypeMgr.getSlotByID(2);
		validateDetails(details, 2, "Command", ISlotTypes.SLOT_TYPE_TEXT, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED);
		
		for (int id = 0; id < 10; id++) {
			details = actionTypeMgr.getSlotByID(3 + id);
			validateDetails(details, 3 + id, "Output" + id, ISlotTypes.SLOT_TYPE_FILEGROUP, 
					ISlotTypes.SLOT_POS_OUTPUT, ISlotTypes.SLOT_CARD_OPTIONAL);
		}
		
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
		validateDetails(details[0], 1, "Input", ISlotTypes.SLOT_TYPE_FILEGROUP, 
				ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_OPTIONAL);
		
		/* get the parameter slots */
		details = actionTypeMgr.getSlots(shellCmdId, ISlotTypes.SLOT_POS_PARAMETER);
		assertEquals(1, details.length);
		validateDetails(details[0], 2, "Command", ISlotTypes.SLOT_TYPE_TEXT, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED);
		
		/* get the output slots - don't bother checking the details */
		details = actionTypeMgr.getSlots(shellCmdId, ISlotTypes.SLOT_POS_OUTPUT);
		assertEquals(10, details.length);
		
		/* get all the slots - don't bother checking the details */
		details = actionTypeMgr.getSlots(shellCmdId, ISlotTypes.SLOT_POS_ANY);
		assertEquals(12, details.length);
		
		/* get slots for an invalid action type - invalid */
		details = actionTypeMgr.getSlots(1000, ISlotTypes.SLOT_POS_ANY);
		assertNull(details);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test getSlot() with some custom-defined slots.
	 */
	@Test
	public void testGetSlotsCustom() {

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
		validateDetails(details, 1, "Input", ISlotTypes.SLOT_TYPE_FILEGROUP, 
				ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_OPTIONAL);
		
		/* slot 2 is "Command" */
		details = actionTypeMgr.getSlotByName(shellCmdId, "Command");
		validateDetails(details, 2, "Command", ISlotTypes.SLOT_TYPE_TEXT, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED);

		/* check just one of the output slots */
		details = actionTypeMgr.getSlotByName(shellCmdId, "Output3");
		validateDetails(details, 6, "Output3", ISlotTypes.SLOT_TYPE_FILEGROUP, 
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
		int slotId1 = actionTypeMgr.newSlot(actionTypeId, "fileGroup1", ISlotTypes.SLOT_TYPE_FILEGROUP, 
				ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		int slotId2 = pkgMgr.newSlot(pkgAId, "fileGroup1", ISlotTypes.SLOT_TYPE_FILEGROUP, 
				ISlotTypes.SLOT_POS_OUTPUT, ISlotTypes.SLOT_CARD_OPTIONAL, null, null);
		int slotId3 = pkgMgr.newSlot(pkgBId, "fileGroup2", ISlotTypes.SLOT_TYPE_FILEGROUP,
				ISlotTypes.SLOT_POS_OUTPUT, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		int slotId4 = pkgMgr.newSlot(pkgAId, "param1", ISlotTypes.SLOT_TYPE_BOOLEAN,
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		int slotId5 = actionTypeMgr.newSlot(actionTypeId, "param2", ISlotTypes.SLOT_TYPE_INTEGER,
				ISlotTypes.SLOT_POS_LOCAL, ISlotTypes.SLOT_CARD_OPTIONAL, null, null);
		int slotId6 = actionTypeMgr.newSlot(actionTypeId, "param3", ISlotTypes.SLOT_TYPE_TEXT,
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		int slotId7 = actionTypeMgr.newSlot(actionTypeId, "multiFileGroup", ISlotTypes.SLOT_TYPE_FILEGROUP,
				ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_MULTI, null, null);
		
		/* 
		 * Validate a required SLOT_TYPE_FILEGROUP as a SLOT_POS_INPUT for shell actions.
		 */
		assertTrue(slotId1 >= 0);
		SlotDetails details = actionTypeMgr.getSlotByName(actionTypeId, "fileGroup1");
		assertNotNull(details);
		validateDetails(details, slotId1, "fileGroup1", 
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_REQUIRED);
		details = actionTypeMgr.getSlotByID(slotId1);
		assertNotNull(details);
		validateDetails(details, slotId1, "fileGroup1", 
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_REQUIRED);
		
		/* 
		 * Validate a non-required SLOT_TYPE_FILEGROUP as a SLOT_POS_OUTPUT for pkgA.
		 */
		assertTrue(slotId2 >= 0);
		assertTrue(slotId1 != slotId2);
		details = pkgMgr.getSlotByName(pkgAId, "fileGroup1");
		assertNotNull(details);
		validateDetails(details, slotId2, "fileGroup1", 
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_OUTPUT, ISlotTypes.SLOT_CARD_OPTIONAL);
		details = pkgMgr.getSlotByID(slotId2);
		assertNotNull(details);
		validateDetails(details, slotId2, "fileGroup1", 
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_OUTPUT, ISlotTypes.SLOT_CARD_OPTIONAL);
		
		/* 
		 * Validate a required SLOT_TYPE_FILEGROUP as a SLOT_POS_OUTPUT for pkgB
		 */
		assertTrue(slotId3 >= 0);
		assertTrue(slotId1 != slotId3);
		assertTrue(slotId1 != slotId2);
		details = pkgMgr.getSlotByName(pkgBId, "fileGroup2");
		assertNotNull(details);
		validateDetails(details, slotId3, "fileGroup2",
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_OUTPUT, ISlotTypes.SLOT_CARD_REQUIRED);
		details = pkgMgr.getSlotByID(slotId3);
		assertNotNull(details);
		validateDetails(details, slotId3, "fileGroup2", 
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_OUTPUT, ISlotTypes.SLOT_CARD_REQUIRED);

		/* 
		 * Validate a required SLOT_TYPE_BOOLEAN as a SLOT_POS_PARAMETER and a SLOT_POS_LOCAL for pkgA
		 */
		assertTrue(slotId4 >= 0);
		details = pkgMgr.getSlotByName(pkgAId, "param1");
		assertNotNull(details);
		validateDetails(details, slotId4, "param1",
				ISlotTypes.SLOT_TYPE_BOOLEAN, ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED);
		details = pkgMgr.getSlotByID(slotId4);
		assertNotNull(details);
		validateDetails(details, slotId4, "param1",
				ISlotTypes.SLOT_TYPE_BOOLEAN, ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED);
		
		/* 
		 * Validate a non-required SLOT_TYPE_INTEGER as a SLOT_POS_LOCAL for shell actions 
		 */
		assertTrue(slotId5 >= 0);
		details = actionTypeMgr.getSlotByName(actionTypeId, "param2");
		assertNotNull(details);
		validateDetails(details, slotId5, "param2",
				ISlotTypes.SLOT_TYPE_INTEGER, ISlotTypes.SLOT_POS_LOCAL, ISlotTypes.SLOT_CARD_OPTIONAL);
		details = actionTypeMgr.getSlotByID(slotId5);
		assertNotNull(details);
		validateDetails(details, slotId5, "param2",
				ISlotTypes.SLOT_TYPE_INTEGER, ISlotTypes.SLOT_POS_LOCAL, ISlotTypes.SLOT_CARD_OPTIONAL);
		
		/* 
		 * Validate a required SLOT_TYPE_TEXT as a SLOT_POS_PARAMETER  for shell actions 
		 */
		assertTrue(slotId6 >= 0);
		details = actionTypeMgr.getSlotByName(actionTypeId, "param3");
		assertNotNull(details);
		validateDetails(details, slotId6, "param3",
				ISlotTypes.SLOT_TYPE_TEXT, ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED);
		details = actionTypeMgr.getSlotByID(slotId6);
		assertNotNull(details);
		validateDetails(details, slotId6, "param3",
				ISlotTypes.SLOT_TYPE_TEXT, ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED);
		
		/* 
		 * Validate a SLOT_TYPE_FILEGROUP as a multi-file group as SLOT_POS_INPUT for shell actions 
		 */
		assertTrue(slotId7 >= 0);
		details = actionTypeMgr.getSlotByName(actionTypeId, "multiFileGroup");
		assertNotNull(details);
		validateDetails(details, slotId7, "multiFileGroup",
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_MULTI);
		details = actionTypeMgr.getSlotByID(slotId7);
		assertNotNull(details);
		validateDetails(details, slotId7, "multiFileGroup",
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_INPUT, ISlotTypes.SLOT_CARD_MULTI);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for newSlot() with errors
	 */
	@Test
	public void testNewSlotErrors() {
		
		/* TODO: test with invalid ownerType or ownerId - returns ErrorCode.NOT_FOUND */
		
		/* TODO: test with invalid slotType or slotPos - returns ErrorCode.INVALID_OP */
		
		/* TODO: test with a name that has invalid syntax - returns ErrorCode.INVALID_NAME */
		
		/* TODO: try adding the same name twice with the same ownerType/ownerId - returns ErrorCode.ALREADY_USED */
		
		/* TODO: try adding invalid cardinality values for a slot - returns ErrorCode.OUT_OF_RANGE */
		
		/* TODO: try adding a multi-slot to a non-action slot - returns ErrorCode.OUT_OF_RANGE */
		
		/* TODO: try adding multiple multi-slots to the same action - returns ErrorCode.OUT_OF_RANGE */
	
		/* TODO: try adding a multi-slot of type SLOT_TYPE_INTEGER */
		
		/* TODO: try adding a multi-slot of type SLOT_POS_OUTPUT */
		
		/* TODO: try to add a SLOT_TYPE_FILEGROUP as a SLOT_POS_PARAMETER or SLOT_POS_LOCAL - ErrorCode.OUT_OF_RANGE */

		/* TODO: try to add a SLOT_TYPE_INTEGER as a SLOT_POS_INPUT or SLOT_POS_OUTPUT - ErrorCode.OUT_OF_RANGE */

		/* TODO: try to add a SLOT_TYPE_TEXT as a SLOT_POS_INPUT or SLOT_POS_OUTPUT - ErrorCode.OUT_OF_RANGE */
		
		/* TODO: try to add a SLOT_TYPE_FILEGROUP as a SLOT_POS_INPUT in a package type */
	
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for newSlot() to validate that default values are correct, and that
	 * appropriate values can be placed in the slots.
	 */
	@Test
	public void testNewSlotValues() {
	
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for removeSlot()
	 */
	@Test
	public void testRemoveSlot() {
		/* TODO: implement this later - using both actionTypeMgr and packageMgr */
	}
		
	/*-------------------------------------------------------------------------------------*/

}
