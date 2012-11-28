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

import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import com.buildml.model.CommonTestUtils;
import com.buildml.model.IActionMgr.FileAccess;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.utils.errors.ErrorCode;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestActionMgr {

	/** Our BuildStore object, used in many test cases */
	private IBuildStore bs;

	/** The FileMgr object associated with this BuildStore */
	IFileMgr fileMgr;
	
	/** The ActionMgr object associated with this BuildStore */
	IActionMgr actionMgr;
	
	/** The root action ID */
	int rootActionId;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		bs = CommonTestUtils.getEmptyBuildStore();
		
		/* fetch the associated FileNameSpace object */
		fileMgr = bs.getFileMgr();
		
		/* fetch the associated ActionMgr object */
		actionMgr = bs.getActionMgr();
		
		/* if we don't care about each new action's parents, we'll use the root action */
		rootActionId = actionMgr.getRootAction("root");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.ActionMgr#addAction(int, int, String)}.
	 */
	@Test
	public void testAddBuildAction() {
		
		/* test that each new build action is assigned a unique ID number */
		int action1 = actionMgr.addAction(rootActionId, 0, "gcc -o test.o test.c");
		int action2 = actionMgr.addAction(rootActionId, 0, "gcc -o main.o main.c");
		int action3 = actionMgr.addAction(rootActionId, 0, "gcc -o tree.o tree.c");
		assertNotSame(action1, action2);
		assertNotSame(action1, action3);
		assertNotSame(action2, action3);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.ActionMgr#getCommand(int)}
	 */
	@Test
	public void testGetCommand() {
		int action1 = actionMgr.addAction(rootActionId, 0, "gcc -o test.o test.c");
		int action2 = actionMgr.addAction(rootActionId, 0, "gcc -o main.o main.c");
		int action3 = actionMgr.addAction(rootActionId, 0, "gcc -o tree.o tree.c");
		assertEquals("gcc -o tree.o tree.c", actionMgr.getCommand(action3));
		assertEquals("gcc -o main.o main.c", actionMgr.getCommand(action2));
		assertEquals("gcc -o test.o test.c", actionMgr.getCommand(action1));
		
		/* an invalid action ID should return null */
		assertNull(actionMgr.getCommand(100));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.ActionMgr#setCommand(int,String)}
	 */
	@Test
	public void testSetCommand() {
		int action1 = actionMgr.addAction(rootActionId, 0, "A");
		int action2 = actionMgr.addAction(rootActionId, 0, "B");
		int action3 = actionMgr.addAction(rootActionId, 0, "C");
		
		assertEquals(ErrorCode.OK, actionMgr.setCommand(action2, "Bprime"));
		assertEquals(ErrorCode.OK, actionMgr.setCommand(action3, "Cprime"));
		
		assertEquals("Bprime", actionMgr.getCommand(action2));
		assertEquals("Cprime", actionMgr.getCommand(action3));
		assertEquals("A", actionMgr.getCommand(action1));
		
		/* an invalid action ID should return an error */
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.setCommand(100, "command"));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.ActionMgr#getCommandSummary(int, int)}
	 */
	@Test
	public void testGetCommandSummary() {
	
		/* create a single action, with a long command string */
		int myaction = actionMgr.addAction(actionMgr.getRootAction(""), 0,
				"gcc -Ipath1/include -Ipath2/include -Ipath3/include -DFOO -DBAR " +
				"-o myfile.o -c myfile.c");
		
		/* fetch the summary at various widths */
		assertEquals("gcc -Ipath1/...", actionMgr.getCommandSummary(myaction, 15));
		assertEquals("gcc -Ipath1/include -Ipath2...", actionMgr.getCommandSummary(myaction, 30));
		assertEquals("gcc -Ipath1/include -Ipath2/include -Ipath3/inc...", actionMgr.getCommandSummary(myaction, 50));
		assertEquals("gcc -Ipath1/include -Ipath2/include -Ipath3/include -DFOO -DBAR " +
				"-o myfile.o -c myfile....", actionMgr.getCommandSummary(myaction, 89));
		assertEquals("gcc -Ipath1/include -Ipath2/include -Ipath3/include -DFOO -DBAR " +
				"-o myfile.o -c myfile.c", actionMgr.getCommandSummary(myaction, 100));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.ActionMgr#getParent(int)}
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testGetParent() throws Exception {
		
		/* add a bunch of actions in a hierarchy */
		int action1 = actionMgr.addAction(rootActionId, 0, "/bin/sh");
		int action2 = actionMgr.addAction(action1, 0, "gcc -o main.o main.c");
		int action3 = actionMgr.addAction(action1, 0, "/bin/sh");
		int action4 = actionMgr.addAction(action3, 0, "gcc -o tree.o tree.c");
		int action5 = actionMgr.addAction(action3, 0, "gcc -o bark.o bark.c");
		
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
		int actionA = actionMgr.addAction(rootAction, rootDir, "A");
		int actionA1 = actionMgr.addAction(actionA, rootDir, "A1");
		int actionA1a = actionMgr.addAction(actionA1, rootDir, "A1a");
		int actionA1b = actionMgr.addAction(actionA1, rootDir, "A1b");
		int actionA2 = actionMgr.addAction(actionA, rootDir, "A1");
		int actionA2a = actionMgr.addAction(actionA2, rootDir, "A2a");
		int actionA2b = actionMgr.addAction(actionA2, rootDir, "A2b");
		
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
		int action1 = actionMgr.addAction(rootActionId, 0, "/bin/sh");
		int action2 = actionMgr.addAction(action1, 10, "gcc -o main.o main.c");
		int action3 = actionMgr.addAction(action1, 20, "/bin/sh");
		int action4 = actionMgr.addAction(action3, 25, "gcc -o tree.o tree.c");
		int action5 = actionMgr.addAction(action3, 30, "gcc -o bark.o bark.c");
		
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
		int action1 = actionMgr.addAction(rootActionId, 0, "/bin/sh");
		int action2 = actionMgr.addAction(action1, 0, "gcc -o main.o main.c");
		int action3 = actionMgr.addAction(action1, 0, "/bin/sh");
		int action4 = actionMgr.addAction(action3, 0, "gcc -o tree.o tree.c");
		int action5 = actionMgr.addAction(action3, 0, "gcc -o bark.o bark.c");
		int action6 = actionMgr.addAction(action3, 0, "gcc -o woof.o woof.c");
		
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
		int action = actionMgr.addAction(rootActionId, 0, "gcc -o foo foo.c");
		
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
		int emptyAction = actionMgr.addAction(rootActionId, 0, "echo Hi");
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
		int actionFoo = actionMgr.addAction(rootActionId, 0, "gcc -o foo foo.c");
		int actionBar = actionMgr.addAction(rootActionId, 0, "gcc -o bar bar.c");
		
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
		
		int actionId0 = actionMgr.addAction(parentActionId, dirId, "");
		int actionId1 = actionMgr.addAction(parentActionId, dirId, "");
		int actionId2 = actionMgr.addAction(actionId0, dirId, "");
		int actionId3 = actionMgr.addAction(actionId1, dirId, "");
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
		
		int action = actionMgr.addAction(rootActionId, 0, "my mystery action");
		
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
		int action1 = actionMgr.addAction(rootActionId, 0, "gcc -o clock.o clock.c");
		int action2 = actionMgr.addAction(rootActionId, 0, "gcc -o banner.o banner.c");
		int action3 = actionMgr.addAction(rootActionId, 0, "gcc -o mult.o mult.c");

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
	 * Test method for {@link com.buildml.model.impl.ActionMgr#getActionsInDirectory(int)}.
	 */
	@Test
	public void testGetActionsInDirectory() {

		/* create a couple of directories */
		int dir1 = fileMgr.addDirectory("/dir1");
		int dir2 = fileMgr.addDirectory("/dir2");
		int dir3 = fileMgr.addDirectory("/dir2/dir3");
		
		/* create a number of actions, each executing in one of those directories */
		int rootAction = actionMgr.getRootAction("root");
		int action11 = actionMgr.addAction(rootAction, dir1, "true");
		int action12 = actionMgr.addAction(rootAction, dir1, "true");
		int action21 = actionMgr.addAction(rootAction, dir2, "true");
		int action22 = actionMgr.addAction(rootAction, dir2, "true");
		int action23 = actionMgr.addAction(rootAction, dir2, "true");
		int action24 = actionMgr.addAction(rootAction, dir2, "true");
		int action13 = actionMgr.addAction(rootAction, dir1, "true");
	
		/* fetch the list of actions that execute in the first directory */
		assertTrue(CommonTestUtils.sortedArraysEqual(actionMgr.getActionsInDirectory(dir1),
				new Integer[] {action11, action12, action13}));
		
		/* repeat for the second directory */
		assertTrue(CommonTestUtils.sortedArraysEqual(actionMgr.getActionsInDirectory(dir2),
				new Integer[] {action21, action22, action23, action24}));
		
		/* and the third should be empty */
		assertEquals(0, actionMgr.getActionsInDirectory(dir3).length);
	}
	
	/*-------------------------------------------------------------------------------------*/

	
	/**
	 * Test the scalability of adding build actions and file accesses.
	 */
	@Test
	public void testScalability() {

		bs.setFastAccessMode(true);

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
			int actionId = actionMgr.addAction(rootActionId, 0, sb.toString());
			
			/* now add files to this actions */
			for (int k = 0; k != 200; k++) {
				actionMgr.addFileAccess(actionId, r.nextInt(100), OperationType.OP_READ);
			}
			
			/* now read the files that were added */
			actionMgr.getFilesAccessed(actionId, OperationType.OP_UNSPECIFIED);
		}
		bs.setFastAccessMode(false);
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
		int parentAction = actionMgr.addAction(rootAction, rootDir, "parent");
		int childAction = actionMgr.addAction(parentAction, rootDir, "child");		
		actionMgr.addFileAccess(childAction, fileId, OperationType.OP_READ);
		
		/* check that the parent has the child and that the child is in the directory */
		Integer children[] = actionMgr.getChildren(parentAction);
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { childAction }, children));
		Integer actions[] = actionMgr.getActionsInDirectory(rootDir);
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
		actions = actionMgr.getActionsInDirectory(rootDir);
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { parentAction }, actions));
		
		/* now move the parent action into the trash */
		assertEquals(ErrorCode.OK, actionMgr.moveActionToTrash(parentAction));

		/* query the list of actions in the specified directory - should be one fewer. */
		actions = actionMgr.getActionsInDirectory(rootDir);
		assertEquals(0, actions.length);
		
		/* try to revive the child - can't be done because the parent is trashed */
		assertEquals(ErrorCode.CANT_REVIVE, actionMgr.reviveActionFromTrash(childAction));
		
		/* revive the parent - this is allowed */
		assertEquals(ErrorCode.OK, actionMgr.reviveActionFromTrash(parentAction));
		actions = actionMgr.getActionsInDirectory(rootDir);
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { parentAction }, actions));

		/* finally, revive the child - this is now possible. */
		assertEquals(ErrorCode.OK, actionMgr.reviveActionFromTrash(childAction));
		children = actionMgr.getChildren(parentAction);
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { childAction }, children));
		actions = actionMgr.getActionsInDirectory(rootDir);
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { childAction, parentAction }, actions));
	}
	
	/*-------------------------------------------------------------------------------------*/

}
