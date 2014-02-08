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
import com.buildml.model.IActionTypeMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.ISlotTypes.SlotDetails;
import com.buildml.model.IPackageMgr;
import com.buildml.model.ISlotTypes;
import com.buildml.model.ISubPackageMgr;


/**
 * Test cases for the SlotUndoOp class.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestSlotUndoOp {

	/** Our BuildStore object, used in many test cases */
	private IBuildStore buildStore;

	/** The managers associated with this BuildStore */
	IPackageMgr pkgMgr;
	IActionTypeMgr actionTypeMgr;
	ISubPackageMgr subPkgMgr;

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
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		buildStore = CommonTestUtils.getEmptyBuildStore();
		
		/* fetch the associated manager objects */
		pkgMgr = buildStore.getPackageMgr();
		actionTypeMgr = buildStore.getActionTypeMgr();
		subPkgMgr = buildStore.getSubPackageMgr();
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test undo/redo of creating new package slots
	 */
	@Test
	public void testNewPackageSlot() {
		
		/* create a new slot, and check it exists */
		int pkgId = pkgMgr.addPackage("MyPkg");
		assertTrue(pkgId > 0);
		int slotId = pkgMgr.newSlot(pkgId, "mySlot", null, ISlotTypes.SLOT_TYPE_TEXT, 
							ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, "Hello", null);
		assertTrue(slotId > 0);
		
		/* validate its fields */
		SlotDetails details = pkgMgr.getSlotByID(slotId);
		validateDetails(details, slotId, "mySlot", null, ISlotTypes.SLOT_TYPE_TEXT, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED);
		
		/* record creation of the new slot. */
		SlotUndoOp op = new SlotUndoOp(buildStore, ISlotTypes.SLOT_OWNER_PACKAGE);
		op.recordNewSlot(slotId);
		
		/* now undo creation and that it disappears */
		op.undo();
		assertNull(pkgMgr.getSlotByID(slotId));

		/* redo creation and check that it's back again */
		op.redo();
		details = pkgMgr.getSlotByID(slotId);
		validateDetails(details, slotId, "mySlot", null, ISlotTypes.SLOT_TYPE_TEXT, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED);
		
		/* an undo, one more time */
		op.undo();
		assertNull(pkgMgr.getSlotByID(slotId));		
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test changing package slots
	 */
	@Test
	public void testChangePackageSlot() {

		/* create a new slot, and check it exists */
		int pkgId = pkgMgr.addPackage("MyPkg");
		assertTrue(pkgId > 0);
		int slotId = pkgMgr.newSlot(pkgId, "mySlot", "oldDescr", ISlotTypes.SLOT_TYPE_TEXT, 
							ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, "Hello", null);
		assertTrue(slotId > 0);
	
		/* fetch the current slot details, and make up some new details that we'll change to */
		SlotDetails oldDetails = pkgMgr.getSlotByID(slotId);
		SlotDetails newDetails = new SlotDetails(slotId, ISlotTypes.SLOT_OWNER_PACKAGE, pkgId, 
				"yourSlot", "Description", ISlotTypes.SLOT_TYPE_TEXT, ISlotTypes.SLOT_POS_PARAMETER, 
				ISlotTypes.SLOT_CARD_OPTIONAL, "Goodbye", null);
		
		/* schedule the slot details to be changed */
		SlotUndoOp op = new SlotUndoOp(buildStore, ISlotTypes.SLOT_OWNER_PACKAGE);
		op.recordChangeSlot(oldDetails, newDetails);
		op.redo();
		
		/* validate that it's changed to the new details */
		SlotDetails details = pkgMgr.getSlotByID(slotId);
		validateDetails(details, slotId, "yourSlot", "Description", ISlotTypes.SLOT_TYPE_TEXT, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_OPTIONAL);
		
		/* undo the change */
		op.undo();
		details = pkgMgr.getSlotByID(slotId);
		validateDetails(details, slotId, "mySlot", "oldDescr", ISlotTypes.SLOT_TYPE_TEXT, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED);
		
		/* redo the change */
		op.redo();
		details = pkgMgr.getSlotByID(slotId);
		validateDetails(details, slotId, "yourSlot", "Description", ISlotTypes.SLOT_TYPE_TEXT, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_OPTIONAL);
	
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test removing package slots
	 */
	@Test
	public void testRemovePackageSlot() {
		/* create a new slot, and check it exists */
		int pkgId = pkgMgr.addPackage("MyPkg");
		assertTrue(pkgId > 0);
		int slotId = pkgMgr.newSlot(pkgId, "mySlot", null, ISlotTypes.SLOT_TYPE_TEXT, 
							ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, "Hello", null);
		assertTrue(slotId > 0);
		
		/* schedule it to be trashed */
		SlotUndoOp op = new SlotUndoOp(buildStore, ISlotTypes.SLOT_OWNER_PACKAGE);
		op.recordRemoveSlot(slotId);
		op.redo();
		
		/* validate that it's gone */
		assertNull(pkgMgr.getSlotByID(slotId));

		/* undo the trashing, and validate that its back */
		op.undo();
		SlotDetails details = pkgMgr.getSlotByID(slotId);
		validateDetails(details, slotId, "mySlot", null, ISlotTypes.SLOT_TYPE_TEXT, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED);
		
		/* redo the trashing and re-validate */
		op.redo();
		assertNull(pkgMgr.getSlotByID(slotId));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test changing a slot's value.
	 */
	@Test
	public void testChangeSlotValue() {
		
		/* create a package */
		int pkgId = pkgMgr.addPackage("NewPackage");
		assertTrue(pkgId > 0);
		
		/* add a text slot, an integer slot and a boolean slot */
		int textSlotId = pkgMgr.newSlot(pkgId, "text", "text", ISlotTypes.SLOT_TYPE_TEXT, 
						ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, "Default", null);
		int intSlotId = pkgMgr.newSlot(pkgId, "int", "int", ISlotTypes.SLOT_TYPE_INTEGER, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, 42, null);
		int boolSlotId = pkgMgr.newSlot(pkgId, "bool", "bool", ISlotTypes.SLOT_TYPE_BOOLEAN, 
				ISlotTypes.SLOT_POS_PARAMETER, ISlotTypes.SLOT_CARD_REQUIRED, false, null);
		assertTrue(textSlotId > 0 && intSlotId > 0 && boolSlotId > 0);
		
		
		/* create two sub-packages (subPkg1 and subPkg2) */
		int subPkg1Id = subPkgMgr.newSubPackage(pkgMgr.getMainPackage(), pkgId);
		int subPkg2Id = subPkgMgr.newSubPackage(pkgMgr.getMainPackage(), pkgId);
		assertTrue(subPkg1Id > 0 && subPkg2Id > 0);
		
		/* change the value in the text slot for subPkg1 */
		SlotUndoOp op1 = new SlotUndoOp(buildStore, ISlotTypes.SLOT_OWNER_PACKAGE);
		op1.recordChangeSlotValue(subPkg1Id, textSlotId, false, null, true, "Value 1");
		op1.redo();
		
		/* change the value in the integer slot in subPkg2 */
		SlotUndoOp op2 = new SlotUndoOp(buildStore, ISlotTypes.SLOT_OWNER_PACKAGE);
		op2.recordChangeSlotValue(subPkg2Id, intSlotId, false, null, true, 1);
		op2.redo();
		
		/* test the status/values for all 6 slots */
		assertTrue(subPkgMgr.isSlotSet(subPkg1Id, textSlotId));
		assertEquals("Value 1", subPkgMgr.getSlotValue(subPkg1Id, textSlotId));
		assertFalse(subPkgMgr.isSlotSet(subPkg1Id, intSlotId));
		assertFalse(subPkgMgr.isSlotSet(subPkg1Id, boolSlotId));
		assertFalse(subPkgMgr.isSlotSet(subPkg2Id, textSlotId));
		assertTrue(subPkgMgr.isSlotSet(subPkg2Id, intSlotId));
		assertEquals(1, subPkgMgr.getSlotValue(subPkg2Id, intSlotId));
		assertFalse(subPkgMgr.isSlotSet(subPkg2Id, boolSlotId));
		
		/* change the value in the text slot for subPkg1, again */
		SlotUndoOp op3 = new SlotUndoOp(buildStore, ISlotTypes.SLOT_OWNER_PACKAGE);
		op3.recordChangeSlotValue(subPkg1Id, textSlotId, true, "Value 1", true, "Value 2");
		op3.redo();
		
		/* clear the value in the integer slot in subPkg2 */
		SlotUndoOp op4 = new SlotUndoOp(buildStore, ISlotTypes.SLOT_OWNER_PACKAGE);
		op4.recordChangeSlotValue(subPkg2Id, intSlotId, true, 1, false, null);
		op4.redo();
		
		/* test status/values for all 6 slots, again */
		assertTrue(subPkgMgr.isSlotSet(subPkg1Id, textSlotId));
		assertEquals("Value 2", subPkgMgr.getSlotValue(subPkg1Id, textSlotId));
		assertFalse(subPkgMgr.isSlotSet(subPkg1Id, intSlotId));
		assertFalse(subPkgMgr.isSlotSet(subPkg1Id, boolSlotId));
		assertFalse(subPkgMgr.isSlotSet(subPkg2Id, textSlotId));
		assertFalse(subPkgMgr.isSlotSet(subPkg2Id, intSlotId));
		assertFalse(subPkgMgr.isSlotSet(subPkg2Id, boolSlotId));
		
		/* undo the change to the two sub-packages, and test again */
		op3.undo();
		op4.undo();
		assertTrue(subPkgMgr.isSlotSet(subPkg1Id, textSlotId));
		assertEquals("Value 1", subPkgMgr.getSlotValue(subPkg1Id, textSlotId));
		assertFalse(subPkgMgr.isSlotSet(subPkg1Id, intSlotId));
		assertFalse(subPkgMgr.isSlotSet(subPkg1Id, boolSlotId));
		assertFalse(subPkgMgr.isSlotSet(subPkg2Id, textSlotId));
		assertTrue(subPkgMgr.isSlotSet(subPkg2Id, intSlotId));
		assertEquals(1, subPkgMgr.getSlotValue(subPkg2Id, intSlotId));
		assertFalse(subPkgMgr.isSlotSet(subPkg2Id, boolSlotId));
		
		/* undo the changes to the two sub-packages, again */
		op1.undo();
		op2.undo();
		assertFalse(subPkgMgr.isSlotSet(subPkg1Id, textSlotId));
		assertFalse(subPkgMgr.isSlotSet(subPkg1Id, intSlotId));
		assertFalse(subPkgMgr.isSlotSet(subPkg1Id, boolSlotId));
		assertFalse(subPkgMgr.isSlotSet(subPkg2Id, textSlotId));
		assertFalse(subPkgMgr.isSlotSet(subPkg2Id, intSlotId));
		assertFalse(subPkgMgr.isSlotSet(subPkg2Id, boolSlotId));
		
		/* redo the changes */
		op1.redo();
		op2.redo();
		assertTrue(subPkgMgr.isSlotSet(subPkg1Id, textSlotId));
		assertEquals("Value 1", subPkgMgr.getSlotValue(subPkg1Id, textSlotId));
		assertFalse(subPkgMgr.isSlotSet(subPkg1Id, intSlotId));
		assertFalse(subPkgMgr.isSlotSet(subPkg1Id, boolSlotId));
		assertFalse(subPkgMgr.isSlotSet(subPkg2Id, textSlotId));
		assertTrue(subPkgMgr.isSlotSet(subPkg2Id, intSlotId));
		assertEquals(1, subPkgMgr.getSlotValue(subPkg2Id, intSlotId));
		assertFalse(subPkgMgr.isSlotSet(subPkg2Id, boolSlotId));
	}

	/*-------------------------------------------------------------------------------------*/

}
