/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.refactor;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.buildml.model.CommonTestUtils;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.types.ActionSet;
import com.buildml.refactor.CanNotRefactorException.Cause;
import com.buildml.refactor.imports.ImportRefactorer;
import com.buildml.utils.errors.ErrorCode;

/**
 * Test cases to verify the IImportRefactorer implementation. These tests focus
 * on merging actions.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class TestImportRefactorMergeActions {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	private IBuildStore buildStore;
	private IFileMgr fileMgr;
	private IActionMgr actionMgr;
	
	/* action IDs */
	private int action, actionA, actionA1, actionA1a, actionA1b, actionA2, actionA2a, actionA2b;
	private int actionA3, actionA4, actionA4a, actionA4b, actionA4c;
	private int dirWork, fileWorkFooC, fileWorkFooO, fileWorkBarC, fileWorkBarO, fileWorkMakefile;
	private int fileWorkMainC, fileWorkMainO, fileWorkMain, fileWorkLibA;
	private int fileTmpAaaS, fileTmpBbbS, fileTmpCccS, fileUsrCrt0O, fileUsrCrtendO, fileUnused;
	private IImportRefactorer importRefactorer;

	/*=====================================================================================*
	 * SETUP/TEARDOWN
	 *=====================================================================================*/

	/**
	 * Method called before each test case - sets up default configuration.
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		buildStore = CommonTestUtils.getEmptyBuildStore();
		
		/* fetch the associated managers */
		fileMgr = buildStore.getFileMgr();
		actionMgr = buildStore.getActionMgr();
		
		/* this is the object under test */
		importRefactorer = new ImportRefactorer(buildStore);
		
		/*
		 * Set up a realistic import scenario.
		 *  action   : <action root>
		 *  actionA  : make all
		 *  actionA1 :   gcc -c foo.o foo.c
		 *  actionA1a:     cc1 -o /tmp/aaa.s foo.c
		 *  actionA1b:     as -o foo.o /tmp/aaa.s
		 *  actionA2 :   gcc -c bar.o bar.c
		 *  actionA2a:     cc1 -o /tmp/bbb.s bar.c
		 *  actionA2b:     as -o bar.o /tmp/bbb.s
		 *  actionA3 :   ar c lib.a foo.o bar.o
		 *  actionA4 :   gcc -o main main.c lib.a
		 *  actionA4a:     cc1 -o /tmp/ccc.s main.c
		 *  actionA4b:     as -o main.o /tmp/ccc.s
		 *  actionA4c:     ld -o main /usr/crt0.o main.o lib.a /usr/crtend.o
		 *  
		 *
		 *  fileWork          /home/work
		 *  fileWorkFooC      /home/work/foo.c
		 *  fileWorkFooO      /home/work/foo.o
		 *  fileWorkBarC      /home/work/bar.c
		 *  fileWorkBarO      /home/work/bar.o
		 *  fileWorkMainC     /home/work/main.c
		 *  fileWorkMainO     /home/work/main.o
		 *  fileWorkMain      /home/work/main
		 *  fileWorkLibA      /home/work/lib.a
		 *  fileWorkMakefile  /home/work/Makefile
		 *  fileTmpAaaS       /tmp/aaa.s
		 *  fileTmpBbbS       /tmp/bbb.s
		 *  fileTmpCccS       /tmp/ccc.s
		 *  fileUsrCrt0O      /usr/crt0.o
		 *  fileUsrCrtendO    /usr/crtend.o
		 *  fileUnused        /home/work/README
		 */

		dirWork = fileMgr.addDirectory("/home/work");
		fileWorkFooC = fileMgr.addFile("/home/work/foo.c");
		fileWorkFooO = fileMgr.addFile("/home/work/foo.o");
		fileWorkBarC = fileMgr.addFile("/home/work/bar.c");
		fileWorkBarO = fileMgr.addFile("/home/work/bar.o");
		fileWorkMainC = fileMgr.addFile("/home/work/main.c");
		fileWorkMainO = fileMgr.addFile("/home/work/main.o");
		fileWorkMain = fileMgr.addFile("/home/work/main");
		fileWorkLibA = fileMgr.addFile("/home/work/lib.a");
		fileWorkMakefile = fileMgr.addFile("/home/work/Makefile");
		fileTmpAaaS = fileMgr.addFile("/tmp/aaa.s");
		fileTmpBbbS = fileMgr.addFile("/tmp/bbb.s");
		fileTmpCccS = fileMgr.addFile("/tmp/ccc.s");
		fileUsrCrt0O = fileMgr.addFile("/usr/crt0.o");
		fileUsrCrtendO = fileMgr.addFile("/usr/crtend.o");
		fileUnused = fileMgr.addFile("/home/work/README");

		action = actionMgr.getRootAction("root");

		actionA = actionMgr.addShellCommandAction(action, dirWork, "make all");
		actionMgr.addFileAccess(actionA, fileWorkMakefile, OperationType.OP_READ);

		actionA1 = actionMgr.addShellCommandAction(actionA, dirWork, "gcc -c foo.o foo.c");
		
		actionA1a = actionMgr.addShellCommandAction(actionA1, dirWork, "cc1 -o /tmp/aaa.s foo.c");
		actionMgr.addFileAccess(actionA1a, fileWorkFooC, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA1a, fileTmpAaaS, OperationType.OP_WRITE);

		actionA1b = actionMgr.addShellCommandAction(actionA1, dirWork, "as -o foo.o /tmp/aaa.s");
		actionMgr.addFileAccess(actionA1b, fileTmpAaaS, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA1b, fileWorkFooO, OperationType.OP_WRITE);
		actionMgr.addFileAccess(actionA1,  fileTmpAaaS, OperationType.OP_DELETE);
	
		actionA2 = actionMgr.addShellCommandAction(actionA, dirWork, "gcc -c bar.o bar.c");
	
		actionA2a = actionMgr.addShellCommandAction(actionA2, dirWork, "cc1 -o /tmp/bbb.s bar.c");
		actionMgr.addFileAccess(actionA2a, fileWorkBarC, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA2a, fileTmpBbbS, OperationType.OP_WRITE);

		actionA2b = actionMgr.addShellCommandAction(actionA2, dirWork, "as -o bar.o /tmp/bbb.s");
		actionMgr.addFileAccess(actionA2b, fileTmpBbbS, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA2b, fileWorkBarO, OperationType.OP_WRITE);
		
		actionA3 = actionMgr.addShellCommandAction(actionA, dirWork, "ar c lib.a foo.o bar.o");
		actionMgr.addFileAccess(actionA3, fileWorkFooO, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA3, fileWorkBarO, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA3, fileWorkLibA, OperationType.OP_WRITE);
		actionMgr.addFileAccess(actionA3,  fileTmpBbbS, OperationType.OP_DELETE);

		actionA4 = actionMgr.addShellCommandAction(actionA, dirWork, "gcc -o main main.c lib.a");
		
		actionA4a = actionMgr.addShellCommandAction(actionA4, dirWork, "cc1 -o /tmp/ccc.s main.c");
		actionMgr.addFileAccess(actionA4a, fileWorkMainC, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA4a, fileTmpCccS, OperationType.OP_WRITE);
		
		actionA4b = actionMgr.addShellCommandAction(actionA4, dirWork, "as -o main.o /tmp/ccc.s");
		actionMgr.addFileAccess(actionA4b, fileTmpCccS, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA4b, fileWorkMainO, OperationType.OP_WRITE);
		
		actionA4c = actionMgr.addShellCommandAction(actionA4, dirWork, "ld -o main /usr/crt0.o main.o lib.a /usr/crtend.o");
		actionMgr.addFileAccess(actionA4c, fileUsrCrt0O, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA4c, fileWorkMainO, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA4c, fileWorkLibA, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA4c, fileUsrCrtendO, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA4c, fileWorkMain, OperationType.OP_WRITE);
		actionMgr.addFileAccess(actionA4,  fileTmpCccS, OperationType.OP_DELETE);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Method called after each test cases - closes the BuildStore.
	 * @throws Exception
     */	
	@After
	public void tearDown() throws Exception {
		buildStore.close();
	}
	
	/*=====================================================================================*
	 * TEST METHODS
	 *=====================================================================================*/

	/**
	 * Test the "make atomic" operation.
	 */
	@Test
	public void testMakeAtomic() {
		
		/* actionA4c is already atomic - no work required */
		try {
			importRefactorer.makeActionAtomic(actionA4c);
		} catch (CanNotRefactorException e) {
			fail("Action is already atomic, but got an error.");
		}
		assertEquals(0, actionMgr.getChildren(actionA4c).length);
		CommonTestUtils.sortedArraysEqual(
				new Integer[] { fileUsrCrt0O, fileWorkMainO, fileWorkLibA, fileUsrCrtendO, fileWorkMain },  
				actionMgr.getFilesAccessed(actionA4c, OperationType.OP_UNSPECIFIED));
		
		/* actionA4 has three children */
		assertEquals(3, actionMgr.getChildren(actionA4).length);
		try {
			importRefactorer.makeActionAtomic(actionA4);
		} catch (CanNotRefactorException e) {
			fail("Failed to make action4 atomic.");
		}
		
		/* Check that the former children have been deleted */
		assertEquals(0, actionMgr.getChildren(actionA4).length);
		assertTrue(actionMgr.isActionTrashed(actionA4a));
		assertTrue(actionMgr.isActionTrashed(actionA4b));
		assertTrue(actionMgr.isActionTrashed(actionA4c));
		
		/* Check the new set of file accesses */
		CommonTestUtils.sortedArraysEqual(
				new Integer[] { fileWorkMainC, fileUsrCrt0O, fileWorkLibA, fileUsrCrtendO },  
				actionMgr.getFilesAccessed(actionA4, OperationType.OP_READ));
		CommonTestUtils.sortedArraysEqual(
				new Integer[] { fileWorkMainO, fileWorkMain },  
				actionMgr.getFilesAccessed(actionA4, OperationType.OP_WRITE));
		assertEquals(0, actionMgr.getFilesAccessed(actionA4, OperationType.OP_MODIFIED).length);
		assertEquals(0, actionMgr.getFilesAccessed(actionA4, OperationType.OP_DELETE).length);
		
		/* Try to make a trashed file atomic - should fail */
		try {
			importRefactorer.makeActionAtomic(actionA4a);
			fail("Was able to make a trashed file atomic.");
		} catch (CanNotRefactorException e1) {
			assertEquals(Cause.ACTION_IS_TRASHED, e1.getCauseCode());
		}		
		
		/* under the operation, and check that things are back to normal */
		try {
			importRefactorer.undoRefactoring();
		} catch (CanNotRefactorException e) {
			fail("Failed to undo make atomic operation.");
		}
		assertEquals(3, actionMgr.getChildren(actionA4).length);
		assertFalse(actionMgr.isActionTrashed(actionA4a));
		assertFalse(actionMgr.isActionTrashed(actionA4b));
		assertFalse(actionMgr.isActionTrashed(actionA4c));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the "make atomic" operation, with an "in use" action.
	 */
	@Test
	public void testDeleteActionInUse() {
		
		/* 
		 * Delete actionA2b (left action)
		 */
		assertFalse(actionMgr.isActionTrashed(actionA2b));
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { actionA2a, actionA2b },
													 actionMgr.getChildren(actionA2)));
		try {
			importRefactorer.deleteAction(actionA2b);
			fail("Failed to detect in-use action");
			
		} catch (CanNotRefactorException e) {
			assertEquals(Cause.ACTION_IN_USE, e.getCauseCode());
			assertTrue(CommonTestUtils.sortedArraysEqual(
					new Integer[] { fileWorkBarO }, e.getPathIds()));
		}
		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the "make atomic" operation on actionA4c (a leaf)
	 */
	@Test
	public void testDeleteActionLeaf() {
		
		/* 
		 * Delete actionA4c (leaf action)
		 */
		assertFalse(actionMgr.isActionTrashed(actionA4c));
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] { actionA4a, actionA4b, actionA4c },
				actionMgr.getChildren(actionA4)));
		try {
			importRefactorer.deleteAction(actionA4c);
			/* success */
		} catch (CanNotRefactorException e) {
			fail("Failed to delete actionA4c");
		}
		
		/* check the after state */
		assertTrue(actionMgr.isActionTrashed(actionA4c));
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { actionA4a, actionA4b },
				 actionMgr.getChildren(actionA4)));
		
		/* undo the operation */
		try {
			importRefactorer.undoRefactoring();
		} catch (CanNotRefactorException e) {
			fail("Failed to undo deleteAction");
		}
		assertFalse(actionMgr.isActionTrashed(actionA4c));
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] { actionA4a, actionA4b, actionA4c },
				actionMgr.getChildren(actionA4)));
		
		/* redo the operation */
		try {
			importRefactorer.redoRefactoring();
			/* success */
		} catch (CanNotRefactorException e) {
			fail("Failed to redo delete of actionA4c");
		}
		assertTrue(actionMgr.isActionTrashed(actionA4c));
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { actionA4a, actionA4b },
				 actionMgr.getChildren(actionA4)));
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the "make atomic" operation the root action, or an invalid action - should fail.
	 */
	@Test
	public void testDeleteActionInvalid() {

		/* deleting the root action is illegal */
		int rootAction = actionMgr.getRootAction("root");
		try {
			importRefactorer.deleteAction(rootAction);
			fail("Incorrectly deleted root action");
			
		} catch (CanNotRefactorException e) {
			assertEquals(Cause.INVALID_ACTION, e.getCauseCode());
			assertTrue(CommonTestUtils.sortedArraysEqual(
					new Integer[] { rootAction }, e.getActionIds()));
		}

		/* deleting an invalid actionID is illegal */
		try {
			importRefactorer.deleteAction(1234);
			fail("Incorrectly deleted root action");
			
		} catch (CanNotRefactorException e) {
			assertEquals(Cause.INVALID_ACTION, e.getCauseCode());
			assertTrue(CommonTestUtils.sortedArraysEqual(
					new Integer[] { 1234 }, e.getActionIds()));
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the "make atomic" operation on an unused parent action.
	 */
	@Test
	public void testDeleteActionValidParent() {

		/* 
		 * Delete actionA (parent action)
		 */
		int rootAction = actionMgr.getRootAction("root");
		assertFalse(actionMgr.isActionTrashed(actionA));
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] { actionA }, actionMgr.getChildren(rootAction)));
		try {
			importRefactorer.deleteAction(actionA);
			/* success */
		} catch (CanNotRefactorException e) {
			fail("Failed to delete actionA");
		}
		
		/* check the after state */
		assertTrue(actionMgr.isActionTrashed(actionA));
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] { actionA1, actionA2, actionA3, actionA4 },
				actionMgr.getChildren(rootAction)));
		
		/* undo the operation */
		try {
			importRefactorer.undoRefactoring();
		} catch (CanNotRefactorException e) {
			fail("Failed to undo deleteAction");
		}
		assertFalse(actionMgr.isActionTrashed(actionA));
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] { actionA }, actionMgr.getChildren(rootAction)));
		
		/* redo the operation */
		try {
			importRefactorer.redoRefactoring();
			/* success */
		} catch (CanNotRefactorException e) {
			fail("Failed to redo delete of actionA");
		}
		assertTrue(actionMgr.isActionTrashed(actionA));
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] { actionA1, actionA2, actionA3, actionA4 },
				actionMgr.getChildren(rootAction)));	
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test merging of multiple actions - these test cases should fail.
	 */
	@Test
	public void testMergeActionsFail() {
		
		/* test merging of non-atomic action (actionA1) - should fail */
		try {
			ActionSet set = new ActionSet(actionMgr, new Integer[] { actionA1a, actionA1 }); 
			importRefactorer.mergeActions(set);
			fail("Incorrectly merged an atomic action.");
			
		} catch (CanNotRefactorException e) {
			assertEquals(Cause.ACTION_NOT_ATOMIC, e.getCauseCode());
			assertTrue(CommonTestUtils.sortedArraysEqual(
					new Integer[] { actionA1 }, e.getActionIds()));
		}

		/* test merging of invalid action ID */
		try {
			ActionSet set = new ActionSet(actionMgr, new Integer[] { actionA1a, 1234 });
			importRefactorer.mergeActions(set);
			fail("Incorrectly merged an atomic action.");
			
		} catch (CanNotRefactorException e) {
			assertEquals(Cause.INVALID_ACTION, e.getCauseCode());
			assertTrue(CommonTestUtils.sortedArraysEqual(
					new Integer[] { 1234 }, e.getActionIds()));
		}
		
		/* test merging of trashed action - actionA4c */
		try {
			importRefactorer.deleteAction(actionA4c);
		} catch (CanNotRefactorException e1) {
			fail("Failed to remove actionA4c");
		}
		try {
			ActionSet set = new ActionSet(actionMgr, new Integer[] { actionA4b, actionA4c });
			importRefactorer.mergeActions(set);
			fail("Incorrectly merged a trashed action.");
			
		} catch (CanNotRefactorException e) {
			assertEquals(Cause.ACTION_IS_TRASHED, e.getCauseCode());
			assertTrue(CommonTestUtils.sortedArraysEqual(
					new Integer[] { actionA4c }, e.getActionIds()));
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test merging of multiple actions - these test cases should pass.
	 */
	@Test
	public void testMergeActions() {
		
		/* test merging of a single atomic action (actionA4b) - will succeed */
		try {
			ActionSet set = new ActionSet(actionMgr, new Integer[] { actionA4b });
			importRefactorer.mergeActions(set);
		} catch (CanNotRefactorException e1) {
			fail("Failed to merge single action actionA4b");
		}
		
		/* test merging of two actions - will succeed */
		try {
			ActionSet set = new ActionSet(actionMgr, new Integer[] { actionA1b, actionA1a });
			importRefactorer.mergeActions(set);
			
			assertFalse(actionMgr.isActionTrashed(actionA1a));
			assertTrue(actionMgr.isActionTrashed(actionA1b));
			assertEquals("cc1 -o /tmp/aaa.s foo.c\nas -o foo.o /tmp/aaa.s", 
						 actionMgr.getCommand(actionA1a));
			assertArrayEquals(new Integer[] { fileWorkFooC },
				actionMgr.getFilesAccessed(actionA1a, OperationType.OP_READ));
			assertTrue(CommonTestUtils.sortedArraysEqual(
					new Integer[] { fileWorkFooO, fileTmpAaaS },
					actionMgr.getFilesAccessed(actionA1a, OperationType.OP_WRITE)));
			
		} catch (CanNotRefactorException e1) {
			fail("Failed to merge two actions.");
		}
		
		/* test undo */
		try {
			importRefactorer.undoRefactoring();

			assertFalse(actionMgr.isActionTrashed(actionA1a));
			assertFalse(actionMgr.isActionTrashed(actionA1b));
			assertEquals("cc1 -o /tmp/aaa.s foo.c", actionMgr.getCommand(actionA1a));
			assertEquals("as -o foo.o /tmp/aaa.s", actionMgr.getCommand(actionA1b));			
			assertArrayEquals(new Integer[] { fileWorkFooC },
				actionMgr.getFilesAccessed(actionA1a, OperationType.OP_READ));
			assertArrayEquals(new Integer[] { fileTmpAaaS },
				actionMgr.getFilesAccessed(actionA1a, OperationType.OP_WRITE));
			assertArrayEquals(new Integer[] { fileTmpAaaS },
				actionMgr.getFilesAccessed(actionA1b, OperationType.OP_READ));
			assertArrayEquals(new Integer[] { fileWorkFooO },
				actionMgr.getFilesAccessed(actionA1b, OperationType.OP_WRITE));
		
		} catch (CanNotRefactorException e) {
			fail("Failed to undo merge action.");
		}
		
		/* test redo */
		try {
			importRefactorer.redoRefactoring();

			assertFalse(actionMgr.isActionTrashed(actionA1a));
			assertTrue(actionMgr.isActionTrashed(actionA1b));
			assertEquals("cc1 -o /tmp/aaa.s foo.c\nas -o foo.o /tmp/aaa.s", 
						 actionMgr.getCommand(actionA1a));
			assertArrayEquals(new Integer[] { fileWorkFooC },
				actionMgr.getFilesAccessed(actionA1a, OperationType.OP_READ));
			assertTrue(CommonTestUtils.sortedArraysEqual(
					new Integer[] { fileWorkFooO, fileTmpAaaS },
					actionMgr.getFilesAccessed(actionA1a, OperationType.OP_WRITE)));

		} catch (CanNotRefactorException e) {
			fail("Failed to redo merge action.");
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
