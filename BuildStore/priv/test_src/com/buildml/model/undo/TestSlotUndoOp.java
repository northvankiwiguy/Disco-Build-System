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
}
