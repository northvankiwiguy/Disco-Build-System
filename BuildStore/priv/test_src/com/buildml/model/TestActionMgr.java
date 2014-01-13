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

import java.math.BigDecimal;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import com.buildml.model.CommonTestUtils;
import com.buildml.model.IActionMgr.FileAccess;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.ISlotTypes.SlotDetails;
import com.buildml.utils.errors.ErrorCode;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestActionMgr {

	/** Our BuildStore object, used in many test cases */
	private IBuildStore bs;

	/** The managers associated with this BuildStore */
	IFileMgr fileMgr;
	IActionMgr actionMgr;
	IActionTypeMgr actionTypeMgr;
	
	/** The root action ID */
	private int rootActionId;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		bs = CommonTestUtils.getEmptyBuildStore();
		
		/* fetch the associated manager objects */
		fileMgr = bs.getFileMgr();
		actionMgr = bs.getActionMgr();
		actionTypeMgr = bs.getActionTypeMgr();
		
		/* if we don't care about each new action's parents, we'll use the root action */
		rootActionId = actionMgr.getRootAction("root");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.ActionMgr#addShellCommandAction(int, int, String)}.
	 */
	@Test
	public void testAddBuildAction() {
		
		/* test that each new build action is assigned a unique ID number */
		int action1 = actionMgr.addShellCommandAction(rootActionId, 0, "gcc -o test.o test.c");
		int action2 = actionMgr.addShellCommandAction(rootActionId, 0, "gcc -o main.o main.c");
		int action3 = actionMgr.addShellCommandAction(rootActionId, 0, "gcc -o tree.o tree.c");
		assertNotSame(action1, action2);
		assertNotSame(action1, action3);
		assertNotSame(action2, action3);
		
		/* validate that the action's type is "Shell Command" */
		IActionTypeMgr actionTypeMgr = bs.getActionTypeMgr();
		int shellActionType = actionTypeMgr.getActionTypeByName("Shell Command");
		assertEquals(shellActionType, actionMgr.getActionType(action1));
		assertEquals(shellActionType, actionMgr.getActionType(action2));
		assertEquals(shellActionType, actionMgr.getActionType(action3));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the actionMgr.addAction() method.
	 */
	@Test
	public void testAddAction() {
		
		/* define three shell command actions */
		int actionTypeId = actionTypeMgr.getActionTypeByName("Shell Command");
		int actionId1 = actionMgr.addAction(actionTypeId);
		int actionId2 = actionMgr.addAction(actionTypeId);
		int actionId3 = actionMgr.addAction(actionTypeId);
		
		/* test that their IDs are valid */
		assertTrue(actionId1 >= 0);
		assertTrue(actionId2 >= 0);
		assertTrue(actionId3 >= 0);
		assertTrue(actionId1 != actionId2);
		assertTrue(actionId1 != actionId3);
		assertTrue(actionId2 != actionId3);

		/* test that they're parented at the root */
		int rootId = actionMgr.getRootAction("Root");
		assertEquals(rootId, actionMgr.getParent(actionId1));
		assertEquals(rootId, actionMgr.getParent(actionId2));
		assertEquals(rootId, actionMgr.getParent(actionId3));
		
		/* give each a "Command" slot value */
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId1, IActionMgr.COMMAND_SLOT_ID, "gcc -c test.c"));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId2, IActionMgr.COMMAND_SLOT_ID, "gcc -c add.c"));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId3, IActionMgr.COMMAND_SLOT_ID, "gcc -c sub.c"));
		
		/* give each a "Directory" slot value */
		int dirId1 = fileMgr.addDirectory("/a/b/dirA");
		int dirId2 = fileMgr.addDirectory("/a/b/dirB");
		int dirId3 = fileMgr.addDirectory("/a/b/dirC");
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId1, IActionMgr.DIRECTORY_SLOT_ID, dirId1));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId2, IActionMgr.DIRECTORY_SLOT_ID, dirId2));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId3, IActionMgr.DIRECTORY_SLOT_ID, dirId3));
		
		/* query the "Command" slots */
		assertEquals("gcc -c test.c", actionMgr.getSlotValue(actionId1, IActionMgr.COMMAND_SLOT_ID));
		assertEquals("gcc -c add.c", actionMgr.getSlotValue(actionId2, IActionMgr.COMMAND_SLOT_ID));
		assertEquals("gcc -c sub.c", actionMgr.getSlotValue(actionId3, IActionMgr.COMMAND_SLOT_ID));
		
		/* query the "Directory" slots */
		assertEquals(dirId1, actionMgr.getSlotValue(actionId1, IActionMgr.DIRECTORY_SLOT_ID));
		assertEquals(dirId2, actionMgr.getSlotValue(actionId2, IActionMgr.DIRECTORY_SLOT_ID));
		assertEquals(dirId3, actionMgr.getSlotValue(actionId3, IActionMgr.DIRECTORY_SLOT_ID));
		
		/* test with invalid type IDs */
		assertEquals(ErrorCode.NOT_FOUND, actionMgr.addAction(-1));
		assertEquals(ErrorCode.NOT_FOUND, actionMgr.addAction(1000));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for getting a shell action's "Command" slot.
	 */
	@Test
	public void testGetCommand() {
		int action1 = actionMgr.addShellCommandAction(rootActionId, 0, "gcc -o test.o test.c");
		int action2 = actionMgr.addShellCommandAction(rootActionId, 0, "gcc -o main.o main.c");
		int action3 = actionMgr.addShellCommandAction(rootActionId, 0, "gcc -o tree.o tree.c");
		assertEquals("gcc -o tree.o tree.c", actionMgr.getSlotValue(action3, IActionMgr.COMMAND_SLOT_ID));
		assertEquals("gcc -o main.o main.c", actionMgr.getSlotValue(action2, IActionMgr.COMMAND_SLOT_ID));
		assertEquals("gcc -o test.o test.c", actionMgr.getSlotValue(action1, IActionMgr.COMMAND_SLOT_ID));
		
		/* an invalid action ID should return null */
		assertNull(actionMgr.getSlotValue(100, IActionMgr.COMMAND_SLOT_ID));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for setting an action's "Command" slot.
	 */
	@Test
	public void testSetCommand() {
		int action1 = actionMgr.addShellCommandAction(rootActionId, 0, "A");
		int action2 = actionMgr.addShellCommandAction(rootActionId, 0, "B");
		int action3 = actionMgr.addShellCommandAction(rootActionId, 0, "C");
		
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(action2, IActionMgr.COMMAND_SLOT_ID, "Bprime"));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(action3, IActionMgr.COMMAND_SLOT_ID, "Cprime"));
		
		assertEquals("Bprime", actionMgr.getSlotValue(action2, IActionMgr.COMMAND_SLOT_ID));
		assertEquals("Cprime", actionMgr.getSlotValue(action3, IActionMgr.COMMAND_SLOT_ID));
		assertEquals("A", actionMgr.getSlotValue(action1, IActionMgr.COMMAND_SLOT_ID));
		
		/* an invalid action ID should return an error */
		assertEquals(ErrorCode.NOT_FOUND, actionMgr.setSlotValue(100, IActionMgr.COMMAND_SLOT_ID, "command"));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.ActionMgr#getParent(int)}
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testGetParent() throws Exception {
		
		/* add a bunch of actions in a hierarchy */
		int action1 = actionMgr.addShellCommandAction(rootActionId, 0, "/bin/sh");
		int action2 = actionMgr.addShellCommandAction(action1, 0, "gcc -o main.o main.c");
		int action3 = actionMgr.addShellCommandAction(action1, 0, "/bin/sh");
		int action4 = actionMgr.addShellCommandAction(action3, 0, "gcc -o tree.o tree.c");
		int action5 = actionMgr.addShellCommandAction(action3, 0, "gcc -o bark.o bark.c");
		
		/* the parent of the root is ErrorCode.NOT_FOUND */
		assertEquals(ErrorCode.NOT_FOUND, actionMgr.getParent(rootActionId));
		
		/* all the other new actions have valid parents */
		assertEquals(rootActionId, actionMgr.getParent(action1));
		assertEquals(action1, actionMgr.getParent(action2));
		assertEquals(action1, actionMgr.getParent(action3));
		assertEquals(action3, actionMgr.getParent(action4));
		assertEquals(action3, actionMgr.getParent(action5));
		
		/* inquiring about the parent of an invalid action Id is ErrorCode.BAD_VALUE */
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.getParent(1000));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.ActionMgr#setParent(int, int)}
	 */
	@Test
	public void testSetParent() {
		
		/* 
		 * Set up hierarchy of actions:
		 * 
		 * actionA
		 *   actionA1
		 *     actionA1a
		 *     actionA1b
		 *   actionA2
		 *     actionA2a
		 *     actionA2b
		 */
		int rootAction = actionMgr.getRootAction("root");
		int rootDir = fileMgr.getPath("/");
		int actionA = actionMgr.addShellCommandAction(rootAction, rootDir, "A");
		int actionA1 = actionMgr.addShellCommandAction(actionA, rootDir, "A1");
		int actionA1a = actionMgr.addShellCommandAction(actionA1, rootDir, "A1a");
		int actionA1b = actionMgr.addShellCommandAction(actionA1, rootDir, "A1b");
		int actionA2 = actionMgr.addShellCommandAction(actionA, rootDir, "A1");
		int actionA2a = actionMgr.addShellCommandAction(actionA2, rootDir, "A2a");
		int actionA2b = actionMgr.addShellCommandAction(actionA2, rootDir, "A2b");
		
		/* try to change the parent of the root action - illegal */
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setParent(rootAction, actionA2a));
		
		/* try to change an action's parent to itself - illegal */
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setParent(actionA2, actionA2));

		/* try to change our parent to be our own child - illegal */
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setParent(actionA, actionA2));
		
		/* try to change our parent to be our own grandchild - illegal */
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setParent(actionA, actionA1a));
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setParent(actionA, actionA2b));

		/* perform a legal move - move actionA1b under actionA2 */
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] { actionA1a, actionA1b }, actionMgr.getChildren(actionA1)));
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] { actionA2a, actionA2b }, actionMgr.getChildren(actionA2)));
		assertEquals(ErrorCode.OK, actionMgr.setParent(actionA1b, actionA2));
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] { actionA1a }, actionMgr.getChildren(actionA1)));
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] { actionA1b, actionA2a, actionA2b }, 
				actionMgr.getChildren(actionA2)));
		
		/* perform a legal move - move actionA1a under actionA */
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] { actionA1, actionA2 }, actionMgr.getChildren(actionA)));
		assertEquals(ErrorCode.OK, actionMgr.setParent(actionA1a, actionA));
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] { }, actionMgr.getChildren(actionA1)));
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] { actionA1, actionA2, actionA1a }, 
				actionMgr.getChildren(actionA)));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.ActionMgr#getDirectory(int)}
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testGetDirectory() throws Exception {
		
		/* add a bunch of actions in a hierarchy, each with a different directory */
		int action1 = actionMgr.addShellCommandAction(rootActionId, 0, "/bin/sh");
		int action2 = actionMgr.addShellCommandAction(action1, 10, "gcc -o main.o main.c");
		int action3 = actionMgr.addShellCommandAction(action1, 20, "/bin/sh");
		int action4 = actionMgr.addShellCommandAction(action3, 25, "gcc -o tree.o tree.c");
		int action5 = actionMgr.addShellCommandAction(action3, 30, "gcc -o bark.o bark.c");
		
		/* check that the directories are stored correctly */
		assertEquals(0, actionMgr.getDirectory(action1));
		assertEquals(10, actionMgr.getDirectory(action2));
		assertEquals(20, actionMgr.getDirectory(action3));
		assertEquals(25, actionMgr.getDirectory(action4));
		assertEquals(30, actionMgr.getDirectory(action5));
		
		/* invalid action IDs should return an error */
		assertEquals(ErrorCode.NOT_FOUND, actionMgr.getDirectory(1000));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.ActionMgr#getParent(int)}
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testGetChildren() throws Exception {

		/* add a bunch of actions in a hierarchy */
		int action1 = actionMgr.addShellCommandAction(rootActionId, 0, "/bin/sh");
		int action2 = actionMgr.addShellCommandAction(action1, 0, "gcc -o main.o main.c");
		int action3 = actionMgr.addShellCommandAction(action1, 0, "/bin/sh");
		int action4 = actionMgr.addShellCommandAction(action3, 0, "gcc -o tree.o tree.c");
		int action5 = actionMgr.addShellCommandAction(action3, 0, "gcc -o bark.o bark.c");
		int action6 = actionMgr.addShellCommandAction(action3, 0, "gcc -o woof.o woof.c");
		
		/* test valid parent/child relationships */
		assertTrue(CommonTestUtils.sortedArraysEqual(actionMgr.getChildren(rootActionId), new Integer[] {action1}));
		assertTrue(CommonTestUtils.sortedArraysEqual(actionMgr.getChildren(action1), new Integer[] {action2, action3 }));
		assertTrue(CommonTestUtils.sortedArraysEqual(actionMgr.getChildren(action2), new Integer[] {}));
		assertTrue(CommonTestUtils.sortedArraysEqual(actionMgr.getChildren(action3), new Integer[] {action4, action5, action6 }));
		assertTrue(CommonTestUtils.sortedArraysEqual(actionMgr.getChildren(action4), new Integer[] {}));
		assertTrue(CommonTestUtils.sortedArraysEqual(actionMgr.getChildren(action5), new Integer[] {}));
		assertTrue(CommonTestUtils.sortedArraysEqual(actionMgr.getChildren(action6), new Integer[] {}));
		
		/* the children of non-existent actions is the empty list */
		assertTrue(CommonTestUtils.sortedArraysEqual(actionMgr.getChildren(1000), new Integer[] {}));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.ActionMgr#addFileAccess(int, int, OperationType)}.
	 */
	@Test
	public void testAddGetFileAccess() {
		/* create a new action */
		int action = actionMgr.addShellCommandAction(rootActionId, 0, "gcc -o foo foo.c");
		
		/* create a number of new files */
		int fileFooC = fileMgr.addFile("/a/b/c/foo.c");
		int fileFooH = fileMgr.addFile("/a/b/c/foo.h");
		int fileFooO = fileMgr.addFile("/a/b/c/foo.o");
		int fileFoo = fileMgr.addFile("/a/b/c/foo");
		
		/* record that these files are accessed by the action */
		actionMgr.addFileAccess(action, fileFooC, OperationType.OP_READ);
		actionMgr.addFileAccess(action, fileFooH, OperationType.OP_READ);
		actionMgr.addFileAccess(action, fileFooO, OperationType.OP_WRITE);
		actionMgr.addFileAccess(action, fileFoo, OperationType.OP_WRITE);
		
		/* now check that the records are correct */
		Integer allAccesses[] = actionMgr.getFilesAccessed(action, OperationType.OP_UNSPECIFIED);
		assertTrue(CommonTestUtils.sortedArraysEqual(allAccesses, new Integer[] { fileFooH, fileFooO, fileFooC, fileFoo }));

		/* now, just the reads */
		Integer readAccesses[] = actionMgr.getFilesAccessed(action, OperationType.OP_READ);
		assertTrue(CommonTestUtils.sortedArraysEqual(readAccesses, new Integer[] { fileFooC, fileFooH }));
		
		/* and just the writes */
		Integer writeAccesses[] = actionMgr.getFilesAccessed(action, OperationType.OP_WRITE);
		assertTrue(CommonTestUtils.sortedArraysEqual(writeAccesses, new Integer[] { fileFooO, fileFoo }));

		/* check an empty action - should return no results */
		int emptyAction = actionMgr.addShellCommandAction(rootActionId, 0, "echo Hi");
		Integer emptyAccesses[] = actionMgr.getFilesAccessed(emptyAction, OperationType.OP_UNSPECIFIED);
		assertEquals(0, emptyAccesses.length);
		
		/* check with an invalid action number - should return no results */
		Integer noAccesses[] = actionMgr.getFilesAccessed(100, OperationType.OP_UNSPECIFIED);
		assertEquals(0, noAccesses.length);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.ActionMgr#removeFileAccess(int, int)}.
	 */
	@Test
	public void testRemoveFileAccess() {
		
		/* create a new action */
		int actionFoo = actionMgr.addShellCommandAction(rootActionId, 0, "gcc -o foo foo.c");
		int actionBar = actionMgr.addShellCommandAction(rootActionId, 0, "gcc -o bar bar.c");
		
		/* create some new files */
		int fileFooC = fileMgr.addFile("/a/b/c/foo.c");
		int fileBarC = fileMgr.addFile("/a/b/c/bar.c");
		int fileSharedH = fileMgr.addFile("/a/b/c/shared.h");
		
		/* record that these files are accessed by the action */
		actionMgr.addFileAccess(actionFoo, fileFooC, OperationType.OP_READ);
		actionMgr.addFileAccess(actionFoo, fileSharedH, OperationType.OP_READ);
		actionMgr.addFileAccess(actionBar, fileBarC, OperationType.OP_READ);
		actionMgr.addFileAccess(actionBar, fileSharedH, OperationType.OP_READ);
		
		/* now check that the records are correct */
		Integer accesses[] = actionMgr.getFilesAccessed(actionFoo, OperationType.OP_UNSPECIFIED);
		assertTrue(CommonTestUtils.sortedArraysEqual(accesses, new Integer[] { fileFooC, fileSharedH }));
		accesses = actionMgr.getFilesAccessed(actionBar, OperationType.OP_UNSPECIFIED);
		assertTrue(CommonTestUtils.sortedArraysEqual(accesses, new Integer[] { fileBarC, fileSharedH }));

		/* delete the relationship between actionFoo and fileSharedH */
		actionMgr.removeFileAccess(actionFoo, fileSharedH);
		accesses = actionMgr.getFilesAccessed(actionFoo, OperationType.OP_UNSPECIFIED);
		assertTrue(CommonTestUtils.sortedArraysEqual(accesses, new Integer[] { fileFooC }));
		accesses = actionMgr.getFilesAccessed(actionBar, OperationType.OP_UNSPECIFIED);
		assertTrue(CommonTestUtils.sortedArraysEqual(accesses, new Integer[] { fileBarC, fileSharedH }));

		/* delete the relationship between actionBar and fileBarC */
		actionMgr.removeFileAccess(actionBar, fileBarC);
		accesses = actionMgr.getFilesAccessed(actionFoo, OperationType.OP_UNSPECIFIED);
		assertTrue(CommonTestUtils.sortedArraysEqual(accesses, new Integer[] { fileFooC }));
		accesses = actionMgr.getFilesAccessed(actionBar, OperationType.OP_UNSPECIFIED);
		assertTrue(CommonTestUtils.sortedArraysEqual(accesses, new Integer[] { fileSharedH }));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the addition, removal and re-addition of file-access operations.
	 */
	@Test
	public void testSequenceFileAccesses() {
		
		int parentActionId = actionMgr.getRootAction("root");
		int dirId = fileMgr.getPath("/");
		int fileId = fileMgr.addFile("/test");
		
		int actionId0 = actionMgr.addShellCommandAction(parentActionId, dirId, "");
		int actionId1 = actionMgr.addShellCommandAction(parentActionId, dirId, "");
		int actionId2 = actionMgr.addShellCommandAction(actionId0, dirId, "");
		int actionId3 = actionMgr.addShellCommandAction(actionId1, dirId, "");
		Integer actions[] = new Integer[] { actionId0, actionId1, actionId2, actionId3 };
		
		/* Initially, there are no file accesses */
		FileAccess results[] = actionMgr.getSequencedFileAccesses(actions);
		assertEquals(0, results.length);
		
		/* Add four accesses */
		actionMgr.addFileAccess(actionId0, fileId, OperationType.OP_READ);
		actionMgr.addFileAccess(actionId1, fileId, OperationType.OP_WRITE);
		actionMgr.addFileAccess(actionId2, fileId, OperationType.OP_MODIFIED);
		actionMgr.addFileAccess(actionId3, fileId, OperationType.OP_DELETE);
		
		/* Retrieve the file accesses, they should appear in the order they were added */
		results = actionMgr.getSequencedFileAccesses(actions);
		assertEquals(4, results.length);
		assertEquals(actionId0, results[0].actionId);
		assertEquals(OperationType.OP_READ, results[0].opType);
		assertEquals(actionId1, results[1].actionId);
		assertEquals(OperationType.OP_WRITE, results[1].opType);
		assertEquals(actionId2, results[2].actionId);
		assertEquals(OperationType.OP_MODIFIED, results[2].opType);
		assertEquals(actionId3, results[3].actionId);
		assertEquals(OperationType.OP_DELETE, results[3].opType);
		int savedId0Seqno = results[0].seqno;
		int savedId2Seqno = results[2].seqno;

		/* Remove the actionId2 file access, and test again */
		actionMgr.removeFileAccess(actionId2, fileId);
		results = actionMgr.getSequencedFileAccesses(actions);
		assertEquals(3, results.length);
		assertEquals(actionId0, results[0].actionId);
		assertEquals(OperationType.OP_READ, results[0].opType);
		assertEquals(actionId1, results[1].actionId);
		assertEquals(OperationType.OP_WRITE, results[1].opType);
		assertEquals(actionId3, results[2].actionId);
		assertEquals(OperationType.OP_DELETE, results[2].opType);
		
		/* Remove the actionId1 file access, and test again */
		actionMgr.removeFileAccess(actionId1, fileId);
		results = actionMgr.getSequencedFileAccesses(actions);
		assertEquals(2, results.length);
		assertEquals(actionId0, results[0].actionId);
		assertEquals(OperationType.OP_READ, results[0].opType);
		assertEquals(actionId3, results[1].actionId);
		assertEquals(OperationType.OP_DELETE, results[1].opType);
		
		/* Try to add something with an in-use sequence number - should fail */
		assertEquals(ErrorCode.ONLY_ONE_ALLOWED, 
				actionMgr.addSequencedFileAccess(savedId0Seqno, actionId0, fileId, 
						OperationType.OP_READ));
		
		/* Try to add the actionId2 entry back again - should succeed */
		assertEquals(ErrorCode.OK, 
				actionMgr.addSequencedFileAccess(savedId2Seqno, actionId2, fileId, 
						OperationType.OP_MODIFIED));
		results = actionMgr.getSequencedFileAccesses(actions);
		assertEquals(3, results.length);
		assertEquals(actionId0, results[0].actionId);
		assertEquals(OperationType.OP_READ, results[0].opType);
		assertEquals(actionId2, results[1].actionId);
		assertEquals(OperationType.OP_MODIFIED, results[1].opType);
		assertEquals(actionId3, results[2].actionId);
		assertEquals(OperationType.OP_DELETE, results[2].opType);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test what happens when a single file is accessed multiple times, in many different
	 * modes (e.g. reading, then writing, then delete, etc).
	 */
	@Test
	public void testMultipleFilesAccesses() {
		Integer result[];
		
		int action = actionMgr.addShellCommandAction(rootActionId, 0, "my mystery action");
		
		/* create a number of new files */
		int file1 = fileMgr.addFile("/file1");
		int file2 = fileMgr.addFile("/file2");
		int file3 = fileMgr.addFile("/file3");
		int file4 = fileMgr.addFile("/file4");
		int file5 = fileMgr.addFile("/file5");
		int file6 = fileMgr.addFile("/file6");
		int file7 = fileMgr.addFile("/file7");
		int file8 = fileMgr.addFile("/file8");
		int file9 = fileMgr.addFile("/file9");
				
		/* test read, read => read */
		actionMgr.addFileAccess(action, file1, OperationType.OP_READ);
		actionMgr.addFileAccess(action, file1, OperationType.OP_READ);
		assertEquals(1, actionMgr.getActionsThatAccess(file1, OperationType.OP_READ).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file1, OperationType.OP_WRITE).length);
		
		/* test read, write => modify */
		actionMgr.addFileAccess(action, file2, OperationType.OP_READ);
		actionMgr.addFileAccess(action, file2, OperationType.OP_WRITE);
		assertEquals(1, actionMgr.getActionsThatAccess(file2, OperationType.OP_MODIFIED).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file2, OperationType.OP_READ).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file2, OperationType.OP_WRITE).length);
		
		/* test write, write => write */
		actionMgr.addFileAccess(action, file3, OperationType.OP_WRITE);
		actionMgr.addFileAccess(action, file3, OperationType.OP_WRITE);
		assertEquals(0, actionMgr.getActionsThatAccess(file3, OperationType.OP_READ).length);
		assertEquals(1, actionMgr.getActionsThatAccess(file3, OperationType.OP_WRITE).length);
		
		/* test write, modify => write */
		actionMgr.addFileAccess(action, file4, OperationType.OP_WRITE);
		actionMgr.addFileAccess(action, file4, OperationType.OP_MODIFIED);
		assertEquals(0, actionMgr.getActionsThatAccess(file4, OperationType.OP_MODIFIED).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file4, OperationType.OP_READ).length);
		assertEquals(1, actionMgr.getActionsThatAccess(file4, OperationType.OP_WRITE).length);

		/* test delete, read => read */
		actionMgr.addFileAccess(action, file5, OperationType.OP_DELETE);
		actionMgr.addFileAccess(action, file5, OperationType.OP_READ);
		assertEquals(0, actionMgr.getActionsThatAccess(file5, OperationType.OP_MODIFIED).length);
		assertEquals(1, actionMgr.getActionsThatAccess(file5, OperationType.OP_READ).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file5, OperationType.OP_WRITE).length);
		
		/* test delete, modify => modify */
		actionMgr.addFileAccess(action, file6, OperationType.OP_DELETE);
		actionMgr.addFileAccess(action, file6, OperationType.OP_MODIFIED);
		assertEquals(1, actionMgr.getActionsThatAccess(file6, OperationType.OP_MODIFIED).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file6, OperationType.OP_READ).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file6, OperationType.OP_WRITE).length);

		/* test read, write, delete => delete */
		actionMgr.addFileAccess(action, file7, OperationType.OP_READ);
		actionMgr.addFileAccess(action, file7, OperationType.OP_WRITE);
		actionMgr.addFileAccess(action, file7, OperationType.OP_DELETE);
		assertEquals(0, actionMgr.getActionsThatAccess(file7, OperationType.OP_MODIFIED).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file7, OperationType.OP_READ).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file7, OperationType.OP_WRITE).length);
		assertEquals(1, actionMgr.getActionsThatAccess(file7, OperationType.OP_DELETE).length);
		
		/* test delete, read, write => modify */
		actionMgr.addFileAccess(action, file8, OperationType.OP_DELETE);
		actionMgr.addFileAccess(action, file8, OperationType.OP_READ);
		actionMgr.addFileAccess(action, file8, OperationType.OP_WRITE);
		assertEquals(1, actionMgr.getActionsThatAccess(file8, OperationType.OP_MODIFIED).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file8, OperationType.OP_READ).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file8, OperationType.OP_WRITE).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file8, OperationType.OP_DELETE).length);
		
		/* test write, read, delete => temporary - and the file is deleted */
		assertEquals(file9, fileMgr.getPath("/file9"));
		actionMgr.addFileAccess(action, file9, OperationType.OP_WRITE);
		actionMgr.addFileAccess(action, file9, OperationType.OP_READ);
		actionMgr.addFileAccess(action, file9, OperationType.OP_DELETE);
		assertEquals(0, actionMgr.getActionsThatAccess(file9, OperationType.OP_MODIFIED).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file9, OperationType.OP_READ).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file9, OperationType.OP_WRITE).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file9, OperationType.OP_DELETE).length);
		assertEquals(ErrorCode.BAD_PATH, fileMgr.getPath("/file9"));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.ActionMgr#getActionsThatAccess(int, OperationType)}.
	 */
	@Test
	public void testGetActionsThatAccess() {

		/* create some actions */
		int action1 = actionMgr.addShellCommandAction(rootActionId, 0, "gcc -o clock.o clock.c");
		int action2 = actionMgr.addShellCommandAction(rootActionId, 0, "gcc -o banner.o banner.c");
		int action3 = actionMgr.addShellCommandAction(rootActionId, 0, "gcc -o mult.o mult.c");

		/* and a bunch of files that access those actions */
		int file1 = fileMgr.addFile("/clock.o");
		int file2 = fileMgr.addFile("/clock.c");
		int file3 = fileMgr.addFile("/banner.o");
		int file4 = fileMgr.addFile("/banner.c");
		int file5 = fileMgr.addFile("/mult.o");
		int file6 = fileMgr.addFile("/mult.c");
		int file7 = fileMgr.addFile("/stdio.h");
		
		/* now register each action's file accesses */
		actionMgr.addFileAccess(action1, file1, OperationType.OP_WRITE);
		actionMgr.addFileAccess(action1, file2, OperationType.OP_READ);
		actionMgr.addFileAccess(action1, file7, OperationType.OP_READ);

		actionMgr.addFileAccess(action2, file3, OperationType.OP_WRITE);
		actionMgr.addFileAccess(action2, file4, OperationType.OP_READ);
		actionMgr.addFileAccess(action2, file7, OperationType.OP_READ);		

		actionMgr.addFileAccess(action3, file5, OperationType.OP_WRITE);
		actionMgr.addFileAccess(action3, file6, OperationType.OP_READ);
		actionMgr.addFileAccess(action3, file7, OperationType.OP_READ);		

		/* 
		 * Finally, fetch the list of actions that each file is reference by.
		 * There are numerous combinations to test here.
		 */

		/* for clock.o */
		Integer results[] = actionMgr.getActionsThatAccess(file1, OperationType.OP_UNSPECIFIED);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { action1 }));

		results = actionMgr.getActionsThatAccess(file1, OperationType.OP_WRITE);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { action1 }));

		results = actionMgr.getActionsThatAccess(file1, OperationType.OP_READ);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] {} ));

		/* for clock.c */
		results = actionMgr.getActionsThatAccess(file2, OperationType.OP_UNSPECIFIED);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { action1 } ));

		results = actionMgr.getActionsThatAccess(file2, OperationType.OP_WRITE);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { } ));

		results = actionMgr.getActionsThatAccess(file2, OperationType.OP_READ);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { action1 } ));

		/* for mult.o */
		results = actionMgr.getActionsThatAccess(file5, OperationType.OP_UNSPECIFIED);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { action3 } ));

		results = actionMgr.getActionsThatAccess(file5, OperationType.OP_WRITE);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { action3 } ));

		results = actionMgr.getActionsThatAccess(file5, OperationType.OP_READ);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { } ));

		/* for stdio.h */
		results = actionMgr.getActionsThatAccess(file7, OperationType.OP_READ);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { action1, action2, action3 } ));

		results = actionMgr.getActionsThatAccess(file7, OperationType.OP_WRITE);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { } ));
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for finding actions that reside in a directory.
	 */
	@Test
	public void testGetActionsInDirectory() {

		/* create a couple of directories */
		int dir1 = fileMgr.addDirectory("/dir1");
		int dir2 = fileMgr.addDirectory("/dir2");
		int dir3 = fileMgr.addDirectory("/dir2/dir3");
		
		/* create a number of actions, each executing in one of those directories */
		int rootAction = actionMgr.getRootAction("root");
		int action11 = actionMgr.addShellCommandAction(rootAction, dir1, "true");
		int action12 = actionMgr.addShellCommandAction(rootAction, dir1, "true");
		int action21 = actionMgr.addShellCommandAction(rootAction, dir2, "true");
		int action22 = actionMgr.addShellCommandAction(rootAction, dir2, "true");
		int action23 = actionMgr.addShellCommandAction(rootAction, dir2, "true");
		int action24 = actionMgr.addShellCommandAction(rootAction, dir2, "true");
		int action13 = actionMgr.addShellCommandAction(rootAction, dir1, "true");
	
		/* fetch the list of actions that execute in the first directory */
		assertTrue(CommonTestUtils.sortedArraysEqual(
				actionMgr.getActionsWhereSlotEquals(IActionMgr.DIRECTORY_SLOT_ID, dir1),
				new Integer[] {action11, action12, action13}));
		
		/* repeat for the second directory */
		assertTrue(CommonTestUtils.sortedArraysEqual(
				actionMgr.getActionsWhereSlotEquals(IActionMgr.DIRECTORY_SLOT_ID, dir2),
				new Integer[] {action21, action22, action23, action24}));
		
		/* and the third should be empty */
		assertEquals(0, actionMgr.getActionsWhereSlotEquals(IActionMgr.DIRECTORY_SLOT_ID, dir3).length);
	}
	
	/*-------------------------------------------------------------------------------------*/

	
	/**
	 * Test the scalability of adding build actions and file accesses.
	 */
	@Test
	public void testScalability() {

		boolean prevState = bs.setFastAccessMode(true);

		/* create a large number of randomly-generated file names */
		Random r = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i != 1000; i++) {
			/* commands can be from 10 to 500 characters long */
			int numChars = r.nextInt(490) + 10;
			
			/* 
			 * Each path "portion" can be 5-10 characters long. To ensure
			 * we get some degree of consistency in path names, we'll use
			 * the names listed in the portionNames variable.
			 */
			sb.delete(0, sb.length());
			for (int j = 0; j != numChars; j++) {
				sb.append((char)(r.nextInt(26) + 65));
			}
			
			//System.out.println("Adding " + sb);

			/* add the file name to the FileSpace */
			int actionId = actionMgr.addShellCommandAction(rootActionId, 0, sb.toString());
			
			/* now add files to this actions */
			for (int k = 0; k != 200; k++) {
				actionMgr.addFileAccess(actionId, r.nextInt(100), OperationType.OP_READ);
			}
			
			/* now read the files that were added */
			actionMgr.getFilesAccessed(actionId, OperationType.OP_UNSPECIFIED);
		}
		bs.setFastAccessMode(prevState);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the moveActionToTrash() method.
	 * @throws Exception
	 */
	@Test
	public void testMoveToTrash() throws Exception {
		
		/* for starters, the root action can't be trashed */
		int rootAction = actionMgr.getRootAction("root");
		assertEquals(ErrorCode.CANT_REMOVE, actionMgr.moveActionToTrash(rootAction));
		
		/* add a new action, a sub-action, and a file. The sub-action references the file */
		int rootDir = fileMgr.getPath("/home");
		int fileId = fileMgr.addFile("/home/joe");
		int parentAction = actionMgr.addShellCommandAction(rootAction, rootDir, "parent");
		int childAction = actionMgr.addShellCommandAction(parentAction, rootDir, "child");		
		actionMgr.addFileAccess(childAction, fileId, OperationType.OP_READ);
		
		/* check that the parent has the child and that the child is in the directory */
		Integer children[] = actionMgr.getChildren(parentAction);
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { childAction }, children));
		Integer actions[] = actionMgr.getActionsWhereSlotEquals(IActionMgr.DIRECTORY_SLOT_ID, rootDir);
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { childAction, parentAction }, actions));
		
		/* Attempt to delete the parent action - should fail because of child action. */
		assertEquals(ErrorCode.CANT_REMOVE, actionMgr.moveActionToTrash(parentAction));
		
		/* Attempt to delete the sub-action - should fail because of referenced file */
		assertEquals(ErrorCode.CANT_REMOVE, actionMgr.moveActionToTrash(childAction));
		
		/* remove the action-file relationship, then try again to delete sub-action - success */
		actionMgr.removeFileAccess(childAction, fileId);
		assertEquals(ErrorCode.OK, actionMgr.moveActionToTrash(childAction));
		
		/* query the list of the parent action's children */
		assertEquals(0, actionMgr.getChildren(parentAction).length);
		
		/* query the list of actions in the specified directory */
		actions = actionMgr.getActionsWhereSlotEquals(IActionMgr.DIRECTORY_SLOT_ID, rootDir);
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { parentAction }, actions));
		
		/* now move the parent action into the trash */
		assertEquals(ErrorCode.OK, actionMgr.moveActionToTrash(parentAction));

		/* query the list of actions in the specified directory - should be one fewer. */
		actions = actionMgr.getActionsWhereSlotEquals(IActionMgr.DIRECTORY_SLOT_ID, rootDir);
		assertEquals(0, actions.length);
		
		/* try to revive the child - can't be done because the parent is trashed */
		assertEquals(ErrorCode.CANT_REVIVE, actionMgr.reviveActionFromTrash(childAction));
		
		/* revive the parent - this is allowed */
		assertEquals(ErrorCode.OK, actionMgr.reviveActionFromTrash(parentAction));
		actions = actionMgr.getActionsWhereSlotEquals(IActionMgr.DIRECTORY_SLOT_ID, rootDir);
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { parentAction }, actions));

		/* finally, revive the child - this is now possible. */
		assertEquals(ErrorCode.OK, actionMgr.reviveActionFromTrash(childAction));
		children = actionMgr.getChildren(parentAction);
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { childAction }, children));
		actions = actionMgr.getActionsWhereSlotEquals(IActionMgr.DIRECTORY_SLOT_ID, rootDir);
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { childAction, parentAction }, actions));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test action slots.
	 */
	@Test
	public void testActionSlots() {
		
		/* 
		 * Add a new action, which has "Shell Command" type. This will have "Input", "OutputN"
		 * and "Command" slots.
		 */
		IActionTypeMgr actionTypeMgr = bs.getActionTypeMgr();
		
		/* create an action, and determine its actionType */
		int myActionId = actionMgr.addShellCommandAction(actionMgr.getRootAction("root"),
															fileMgr.getPath("/"), "gcc -o test.c");
		assertTrue(myActionId >= 0);
		
		/* fetch the slot details for Input, Output0 and Command */
		SlotDetails inputSlotDetails = actionTypeMgr.getSlotByName(IActionTypeMgr.BUILTIN_SHELL_COMMAND_ID, "Input");
		SlotDetails outputSlotDetails = actionTypeMgr.getSlotByName(IActionTypeMgr.BUILTIN_SHELL_COMMAND_ID, "Output");
		SlotDetails commandSlotDetails = actionTypeMgr.getSlotByName(IActionTypeMgr.BUILTIN_SHELL_COMMAND_ID, "Command");
		assertNotNull(inputSlotDetails);
		assertNotNull(outputSlotDetails);
		assertNotNull(commandSlotDetails);
		
		/* Try to assign/get to/from a slot with an invalid action ID */
		assertEquals(ErrorCode.NOT_FOUND, actionMgr.setSlotValue(-1, inputSlotDetails.slotId, 100));
		assertEquals(ErrorCode.NOT_FOUND, actionMgr.setSlotValue(1234, inputSlotDetails.slotId, 100));
		assertNull(actionMgr.getSlotValue(-1, inputSlotDetails.slotId));
		assertNull(actionMgr.getSlotValue(1234, inputSlotDetails.slotId));
		
		/* Try to assign/get to/from an invalid slot ID */
		assertEquals(ErrorCode.NOT_FOUND, actionMgr.setSlotValue(myActionId, -2, 100));
		assertEquals(ErrorCode.NOT_FOUND, actionMgr.setSlotValue(myActionId, 1234, 100));
		assertNull(actionMgr.getSlotValue(myActionId, -2));
		assertNull(actionMgr.getSlotValue(myActionId, 1234));		
		
		/* Assign an Integer FileGroup to "Input" slot */
		assertNull(actionMgr.getSlotValue(myActionId, inputSlotDetails.slotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(myActionId, inputSlotDetails.slotId, 10));
		assertEquals(Integer.valueOf(10), actionMgr.getSlotValue(myActionId, inputSlotDetails.slotId));
		
		/* Assign a new Integer value to the "Input" slot - it will be updated */
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(myActionId, inputSlotDetails.slotId, 12));
		assertEquals(Integer.valueOf(12), actionMgr.getSlotValue(myActionId, inputSlotDetails.slotId));

		/* Assign a FileGroup to "Output" slot */
		assertNull(actionMgr.getSlotValue(myActionId, outputSlotDetails.slotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(myActionId, outputSlotDetails.slotId, 123));
		assertEquals(Integer.valueOf(123), actionMgr.getSlotValue(myActionId, outputSlotDetails.slotId));

		/* Recheck the "Input" slot - it shouldn't have changed. */
		assertEquals(Integer.valueOf(12), actionMgr.getSlotValue(myActionId, inputSlotDetails.slotId));
		
		/* Assign a command string to "Command" parameter slot - returned as String */
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(myActionId, commandSlotDetails.slotId, "My command"));
		assertEquals("My command", actionMgr.getSlotValue(myActionId, commandSlotDetails.slotId));
		
		/* Assign a number to "Output" - should be returned as Integer */
		assertEquals(Integer.valueOf(123), actionMgr.getSlotValue(myActionId, outputSlotDetails.slotId));
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(myActionId, outputSlotDetails.slotId, 456));
		assertEquals(Integer.valueOf(456), actionMgr.getSlotValue(myActionId, outputSlotDetails.slotId));
		
		/* Assign a String to "Command" - should be returned as String */
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(myActionId, commandSlotDetails.slotId, "1313"));
		assertEquals("1313", actionMgr.getSlotValue(myActionId, commandSlotDetails.slotId));

		/* Assign a non-numeric string to "Input" - should fail */
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setSlotValue(myActionId, inputSlotDetails.slotId, "invalid-num"));
		assertEquals(Integer.valueOf(12), actionMgr.getSlotValue(myActionId, inputSlotDetails.slotId));
		
		/* Assign "null" to the "Input" slot, then reassign a numberic value. It should be allowed */
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(myActionId, inputSlotDetails.slotId, null));
		assertEquals(null, actionMgr.getSlotValue(myActionId, inputSlotDetails.slotId));		
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(myActionId, inputSlotDetails.slotId, Integer.valueOf(10)));
		assertEquals(Integer.valueOf(10), actionMgr.getSlotValue(myActionId, inputSlotDetails.slotId));	
		
		/* Test getSlotByName */
		assertEquals(inputSlotDetails.slotId, actionMgr.getSlotByName(myActionId, "Input"));
		assertEquals(ErrorCode.NOT_FOUND, actionMgr.getSlotByName(myActionId, "BadSlot"));
		assertEquals(ErrorCode.NOT_FOUND, actionMgr.getSlotByName(10000, "Input"));		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * The the actionMgr.getActionsWhereSlotIsLike() method.
	 */
	@Test
	public void testGetActionsWhereSlotIsLike() {
		
		/* add a bunch of actions that have interesting command strings in the "Command" slot */		
		int actionId1 = actionMgr.addAction(IActionTypeMgr.BUILTIN_SHELL_COMMAND_ID);
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId1, IActionMgr.COMMAND_SLOT_ID, "gcc -c foo.c -o foo.o"));
		int actionId2 = actionMgr.addAction(IActionTypeMgr.BUILTIN_SHELL_COMMAND_ID);
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId2, IActionMgr.COMMAND_SLOT_ID, "gcc -c bah.c -o bah.o"));
		int actionId3 = actionMgr.addAction(IActionTypeMgr.BUILTIN_SHELL_COMMAND_ID);
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId3, IActionMgr.COMMAND_SLOT_ID, "gcc -c goo.c -o goo.o"));
		int actionId4 = actionMgr.addAction(IActionTypeMgr.BUILTIN_SHELL_COMMAND_ID);
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId4, IActionMgr.COMMAND_SLOT_ID, "ar c lib.a foo.o bah.o goo.o"));
		int actionId5 = actionMgr.addAction(IActionTypeMgr.BUILTIN_SHELL_COMMAND_ID);
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId5, IActionMgr.COMMAND_SLOT_ID, "ranlib lib.a"));
		
		/* search for actions, based on command string matching */
		Integer results[] = actionMgr.getActionsWhereSlotIsLike(IActionMgr.COMMAND_SLOT_ID, "gcc%");
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { actionId1, actionId2, actionId3 }, results));
		results = actionMgr.getActionsWhereSlotIsLike(IActionMgr.COMMAND_SLOT_ID, "%foo%");
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { actionId1, actionId4 }, results));
		results = actionMgr.getActionsWhereSlotIsLike(IActionMgr.COMMAND_SLOT_ID, "%-o %.o");
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { actionId1, actionId2, actionId3 }, results));
		results = actionMgr.getActionsWhereSlotIsLike(IActionMgr.COMMAND_SLOT_ID, "ranlib%");
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { actionId5 }, results));
		
		/* trash actionId2, and repeat the above searches */
		assertEquals(ErrorCode.OK, actionMgr.moveActionToTrash(actionId2));
		results = actionMgr.getActionsWhereSlotIsLike(IActionMgr.COMMAND_SLOT_ID, "gcc%");
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { actionId1, actionId3 }, results));
		results = actionMgr.getActionsWhereSlotIsLike(IActionMgr.COMMAND_SLOT_ID, "%foo%");
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { actionId1, actionId4 }, results));
		results = actionMgr.getActionsWhereSlotIsLike(IActionMgr.COMMAND_SLOT_ID, "%-o %.o");
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { actionId1, actionId3 }, results));
		results = actionMgr.getActionsWhereSlotIsLike(IActionMgr.COMMAND_SLOT_ID, "ranlib%");
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { actionId5 }, results));
		
		/* test with invalid input parameters */
		assertNull(actionMgr.getActionsWhereSlotIsLike(1000, "ranlib%"));
		assertNull(actionMgr.getActionsWhereSlotIsLike(IActionMgr.COMMAND_SLOT_ID, null));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/*
	 * The the actionMgr.getActionsWhereSlotEquals() method.
	 */
	@Test
	public void testGetActionsWhereSlotEquals() {

		/* create several directories in which actions will be able to execute */
		int dirId1 = fileMgr.addDirectory("/a/b/cDir");
		int dirId2 = fileMgr.addDirectory("/d/e/fDir");
		int dirId3 = fileMgr.addDirectory("/d/e/gDir");
		int dirId4 = fileMgr.addDirectory("/d/e/hDir");
		
		/* add a bunch of actions that have one of these directories in "Directory" slot */
		int actionId1 = actionMgr.addAction(IActionTypeMgr.BUILTIN_SHELL_COMMAND_ID);
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId1, IActionMgr.DIRECTORY_SLOT_ID, dirId1));
		int actionId2 = actionMgr.addAction(IActionTypeMgr.BUILTIN_SHELL_COMMAND_ID);
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId2, IActionMgr.DIRECTORY_SLOT_ID, dirId1));
		int actionId3 = actionMgr.addAction(IActionTypeMgr.BUILTIN_SHELL_COMMAND_ID);
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId3, IActionMgr.DIRECTORY_SLOT_ID, dirId2));
		int actionId4 = actionMgr.addAction(IActionTypeMgr.BUILTIN_SHELL_COMMAND_ID);
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId4, IActionMgr.DIRECTORY_SLOT_ID, dirId3));
		int actionId5 = actionMgr.addAction(IActionTypeMgr.BUILTIN_SHELL_COMMAND_ID);
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(actionId5, IActionMgr.DIRECTORY_SLOT_ID, dirId2));
		
		/* search for actions, based on exact matching of the "Directory" slot */
		Integer results[] = actionMgr.getActionsWhereSlotEquals(IActionMgr.DIRECTORY_SLOT_ID, dirId2);
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { actionId3, actionId5 }, results));
		results = actionMgr.getActionsWhereSlotEquals(IActionMgr.DIRECTORY_SLOT_ID, dirId1);
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { actionId1, actionId2 }, results));
		results = actionMgr.getActionsWhereSlotEquals(IActionMgr.DIRECTORY_SLOT_ID, dirId3);
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { actionId4 }, results));
		results = actionMgr.getActionsWhereSlotEquals(IActionMgr.DIRECTORY_SLOT_ID, dirId4);
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { }, results));
		
		/* trash an action, and repeat the above searches */
		assertEquals(ErrorCode.OK, actionMgr.moveActionToTrash(actionId2));
		results = actionMgr.getActionsWhereSlotEquals(IActionMgr.DIRECTORY_SLOT_ID, dirId2);
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { actionId3, actionId5 }, results));
		results = actionMgr.getActionsWhereSlotEquals(IActionMgr.DIRECTORY_SLOT_ID, dirId1);
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { actionId1 }, results));
		results = actionMgr.getActionsWhereSlotEquals(IActionMgr.DIRECTORY_SLOT_ID, dirId3);
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { actionId4 }, results));
		results = actionMgr.getActionsWhereSlotEquals(IActionMgr.DIRECTORY_SLOT_ID, dirId4);
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { }, results));
		
		/* test with invalid input parameters - should all return null */
		assertNull(actionMgr.getActionsWhereSlotEquals(IActionMgr.DIRECTORY_SLOT_ID, null));
		assertNull(actionMgr.getActionsWhereSlotEquals(IActionMgr.DIRECTORY_SLOT_ID, new BigDecimal(10)));
		assertNull(actionMgr.getActionsWhereSlotEquals(1000, dirId4));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/** Our tests set these appropriately */
	private int notifyActionValue = 0;
	private int notifyHowValue = 0;
	private int notifySlotValue = 0;
	
	/**
	 * Test listener notifications
	 */
	@Test
	public void testNotify() {

		/* set up a listener */
		IActionMgrListener listener = new IActionMgrListener() {
			@Override
			public void actionChangeNotification(int pkgId, int how, int changeId) {
				TestActionMgr.this.notifyActionValue = pkgId;
				TestActionMgr.this.notifyHowValue = how;
				TestActionMgr.this.notifySlotValue = changeId;
			}
		};
		actionMgr.addListener(listener);
		int rootActionId = actionMgr.getRootAction("root");
		int dirId = fileMgr.getPath("/");

		/* add two shell command actions */
		int action1 = actionMgr.addShellCommandAction(rootActionId, dirId, "command string");
		int action2 = actionMgr.addShellCommandAction(rootActionId, dirId, "command 2");
		
		/* change an action's command to it current value - no notification */
		notifyActionValue = 0;
		notifyHowValue = 0;
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(action1, IActionMgr.COMMAND_SLOT_ID, "command string"));
		assertEquals(0, notifyActionValue);
		assertEquals(0, notifyHowValue);
		
		/* change the command to a new value - notification given */
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(action1, IActionMgr.COMMAND_SLOT_ID, "new command string"));
		assertEquals(action1, notifyActionValue);
		assertEquals(IActionMgrListener.CHANGED_SLOT, notifyHowValue);
		
		/* change a slot value - check for notification */
		notifyActionValue = notifyHowValue = notifySlotValue = 0;
		int actionTypeId = actionMgr.getActionType(action1);
		SlotDetails slotDetails = actionTypeMgr.getSlotByName(actionTypeId, "Input");
		assertNotNull(slotDetails);
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(action1, slotDetails.slotId, Integer.valueOf(10)));
		assertEquals(notifyActionValue, action1);
		assertEquals(notifyHowValue, IActionMgrListener.CHANGED_SLOT);
		assertEquals(notifySlotValue, slotDetails.slotId);
		
		/* change a slot to the same value - no notification */
		notifyActionValue = notifyHowValue = notifySlotValue = 0;
		assertEquals(ErrorCode.OK, actionMgr.setSlotValue(action1, slotDetails.slotId, Integer.valueOf(10)));
		assertEquals(notifyActionValue, 0);
		assertEquals(notifyHowValue, 0);
		assertEquals(notifySlotValue, 0);
		
		/* trash an action - notification */
		assertEquals(ErrorCode.OK, actionMgr.moveActionToTrash(action1));
		assertEquals(notifyActionValue, action1);
		assertEquals(notifyHowValue, IActionMgrListener.TRASHED_ACTION);
		
		/* revive an action - notification */
		notifyActionValue = notifyHowValue = notifySlotValue = 0;
		assertEquals(ErrorCode.OK, actionMgr.reviveActionFromTrash(action1));
		assertEquals(notifyActionValue, action1);
		assertEquals(notifyHowValue, IActionMgrListener.TRASHED_ACTION);		
	}
	
	/*-------------------------------------------------------------------------------------*/
}
