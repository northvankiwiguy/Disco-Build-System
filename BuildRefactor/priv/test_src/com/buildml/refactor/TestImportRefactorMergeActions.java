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
import com.buildml.refactor.CanNotRefactorException.Cause;
import com.buildml.refactor.imports.ImportRefactorer;

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

		actionA = actionMgr.addAction(action, dirWork, "make all");
		actionMgr.addFileAccess(actionA, fileWorkMakefile, OperationType.OP_READ);

		actionA1 = actionMgr.addAction(actionA, dirWork, "gcc -c foo.o foo.c");
		
		actionA1a = actionMgr.addAction(actionA1, dirWork, "cc1 -o /tmp/aaa.s foo.c");
		actionMgr.addFileAccess(actionA1a, fileWorkFooC, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA1a, fileTmpAaaS, OperationType.OP_WRITE);

		actionA1b = actionMgr.addAction(actionA1, dirWork, "as -o foo.o /tmp/aaa.s");
		actionMgr.addFileAccess(actionA1b, fileTmpAaaS, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA1b, fileWorkFooO, OperationType.OP_WRITE);
		actionMgr.addFileAccess(actionA1,  fileTmpAaaS, OperationType.OP_DELETE);
	
		actionA2 = actionMgr.addAction(actionA, dirWork, "gcc -c bar.o bar.c");
	
		actionA2a = actionMgr.addAction(actionA2, dirWork, "cc1 -o /tmp/bbb.s bar.c");
		actionMgr.addFileAccess(actionA2a, fileWorkBarC, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA2a, fileTmpBbbS, OperationType.OP_WRITE);

		actionA2b = actionMgr.addAction(actionA2, dirWork, "as -o bar.o /tmp/bbb.s");
		actionMgr.addFileAccess(actionA2b, fileTmpBbbS, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA2b, fileWorkBarO, OperationType.OP_WRITE);
		
		actionA3 = actionMgr.addAction(actionA, dirWork, "ar c lib.a foo.o bar.o");
		actionMgr.addFileAccess(actionA3, fileWorkFooO, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA3, fileWorkBarO, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA3, fileWorkLibA, OperationType.OP_WRITE);
		actionMgr.addFileAccess(actionA3,  fileTmpBbbS, OperationType.OP_DELETE);

		actionA4 = actionMgr.addAction(actionA, dirWork, "gcc -o main main.c lib.a");
		
		actionA4a = actionMgr.addAction(actionA4, dirWork, "cc1 -o /tmp/ccc.s main.c");
		actionMgr.addFileAccess(actionA4a, fileWorkMainC, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA4a, fileTmpCccS, OperationType.OP_WRITE);
		
		actionA4b = actionMgr.addAction(actionA4, dirWork, "as -o main.o /tmp/ccc.s");
		actionMgr.addFileAccess(actionA4b, fileTmpCccS, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA4b, fileWorkMainO, OperationType.OP_WRITE);
		
		actionA4c = actionMgr.addAction(actionA4, dirWork, "ld -o main /usr/crt0.o main.o lib.a /usr/crtend.o");
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
		System.out.println("Making atomic");
		
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

}
