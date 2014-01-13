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

package com.buildml.model;

import static org.junit.Assert.*;


import org.junit.Before;
import org.junit.Test;

import com.buildml.model.CommonTestUtils;
import com.buildml.model.ISlotTypes.SlotDetails;
import com.buildml.utils.errors.ErrorCode;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestActionTypeMgr {

	/** Our BuildStore object, used in many test cases */
	private IBuildStore bs;

	/** The ActionTypeMgr object associated with this BuildStore */
	IActionTypeMgr actionTypeMgr;
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		bs = CommonTestUtils.getEmptyBuildStore();
		
		/* fetch the associated ActionMgr object */
		actionTypeMgr = bs.getActionTypeMgr();		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Various tests on the root folder (which is statically defined).
	 */
	@Test
	public void testRootFolder() {
		
		int rootFolderId = actionTypeMgr.getRootFolder();
		
		/* check validity, and name/description */
		assertTrue(actionTypeMgr.isFolder(rootFolderId));
		assertTrue(actionTypeMgr.isValid(rootFolderId));
		assertEquals("All Action Types", actionTypeMgr.getName(rootFolderId));
		assertNotNull(actionTypeMgr.getDescription(rootFolderId));
		
		/* the root folder is its own parent */
		assertEquals(rootFolderId, actionTypeMgr.getParent(rootFolderId));
		
		/* must have at least one child (the "Shell Command" action type) */
		Integer [] children = actionTypeMgr.getFolderChildren(rootFolderId);
		assertNotNull(children);
		assertTrue(children.length >= 1);
		
		/* we can't delete the root folder */
		assertEquals(ErrorCode.CANT_REMOVE, actionTypeMgr.remove(rootFolderId));
		
		/* we can't fetch the ID by name, since it's a folder */
		assertEquals(ErrorCode.INVALID_NAME, actionTypeMgr.getActionTypeByName("All Action Types"));

		/* A folder has no commandlet */
		assertNull(actionTypeMgr.getCommandlet(rootFolderId));
		
		/* A folder has no slots */
		assertNull(actionTypeMgr.getSlots(rootFolderId, ISlotTypes.SLOT_POS_ANY));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Various tests on the root folder (which is statically defined).
	 */
	@Test
	public void testShellCommandActionType() {

		int rootFolderId = actionTypeMgr.getRootFolder();
		int shellActionId = actionTypeMgr.getActionTypeByName("Shell Command");
		assertTrue(shellActionId >= 0);
		
		/* check validity, and name/description */
		assertFalse(actionTypeMgr.isFolder(shellActionId));
		assertTrue(actionTypeMgr.isValid(shellActionId));
		assertEquals("Shell Command", actionTypeMgr.getName(shellActionId));
		assertNotNull(actionTypeMgr.getDescription(shellActionId));
		
		/* the root folder is its own parent */
		assertEquals(rootFolderId, actionTypeMgr.getParent(shellActionId));
		
		/* must have no children */
		Integer [] children = actionTypeMgr.getFolderChildren(shellActionId);
		assertNull(children);
		
		/* we can't delete the shell action type */
		assertEquals(ErrorCode.CANT_REMOVE, actionTypeMgr.remove(shellActionId));
		
		/* The action type must have a non-null commandlet */
		assertNotNull(actionTypeMgr.getCommandlet(shellActionId));
		
		/* TODO: validate the slots */
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Tests for the default slots (Input, Command, OutputN).
	 */
	@Test
	public void testDefaultSlots() {
		
		int shellActionId = actionTypeMgr.getActionTypeByName("Shell Command");
		
		/* test the input slots */
		SlotDetails[] results = actionTypeMgr.getSlots(shellActionId, ISlotTypes.SLOT_POS_INPUT);
		assertEquals(1, results.length);
		assertEquals("Input", results[0].slotName);
		assertEquals(ISlotTypes.SLOT_POS_INPUT, results[0].slotPos);
		assertEquals(ISlotTypes.SLOT_TYPE_FILEGROUP, results[0].slotType);

		/* test the parameter slots */
		results = actionTypeMgr.getSlots(shellActionId, ISlotTypes.SLOT_POS_PARAMETER);
		assertEquals(2, results.length);
		assertEquals("Command", results[0].slotName);
		assertEquals(ISlotTypes.SLOT_POS_PARAMETER, results[0].slotPos);
		assertEquals(ISlotTypes.SLOT_TYPE_TEXT, results[0].slotType);
		assertEquals("Directory", results[1].slotName);
		assertEquals(ISlotTypes.SLOT_POS_PARAMETER, results[1].slotPos);
		assertEquals(ISlotTypes.SLOT_TYPE_DIRECTORY, results[1].slotType);

		/* test the local slots - there aren't any */
		results = actionTypeMgr.getSlots(shellActionId, ISlotTypes.SLOT_POS_LOCAL);
		assertEquals(0, results.length);

		/* test the output slots */
		results = actionTypeMgr.getSlots(shellActionId, ISlotTypes.SLOT_POS_OUTPUT);
		assertEquals(1, results.length);
		assertEquals("Output", results[0].slotName);
		assertEquals(ISlotTypes.SLOT_POS_OUTPUT, results[0].slotPos);
		assertEquals(ISlotTypes.SLOT_TYPE_FILEGROUP, results[0].slotType);

		/* test fetching all slot types at once */
		results = actionTypeMgr.getSlots(shellActionId, ISlotTypes.SLOT_POS_ANY);
		assertEquals(4, results.length);
		
		/* fetch by name - "Input" */
		SlotDetails result = actionTypeMgr.getSlotByName(shellActionId, "Input");
		assertEquals("Input", result.slotName);
		assertEquals(ISlotTypes.SLOT_POS_INPUT, result.slotPos);
		assertEquals(ISlotTypes.SLOT_TYPE_FILEGROUP, result.slotType);
		
		/* fetch by name - "Output" */
		result = actionTypeMgr.getSlotByName(shellActionId, "Output");
		assertEquals("Output", result.slotName);
		assertEquals(ISlotTypes.SLOT_POS_OUTPUT, result.slotPos);
		assertEquals(ISlotTypes.SLOT_TYPE_FILEGROUP, result.slotType);
		
		/* fetch by name - "Command" */
		result = actionTypeMgr.getSlotByName(shellActionId, "Command");
		assertEquals("Command", result.slotName);
		assertEquals(ISlotTypes.SLOT_POS_PARAMETER, result.slotPos);
		assertEquals(ISlotTypes.SLOT_TYPE_TEXT, result.slotType);
		
		/* fetch by name - "Directory" */
		result = actionTypeMgr.getSlotByName(shellActionId, "Directory");
		assertEquals("Directory", result.slotName);
		assertEquals(ISlotTypes.SLOT_POS_PARAMETER, result.slotPos);
		assertEquals(ISlotTypes.SLOT_TYPE_DIRECTORY, result.slotType);
		
		/* fetch an invalid name */
		result = actionTypeMgr.getSlotByName(shellActionId, "BADNAME");
		assertNull(result);
		
		/* test that folders don't have slots */
		results = actionTypeMgr.getSlots(actionTypeMgr.getRootFolder(), ISlotTypes.SLOT_POS_ANY);
		assertNull(results);
	}
	
	/*-------------------------------------------------------------------------------------*/

}
