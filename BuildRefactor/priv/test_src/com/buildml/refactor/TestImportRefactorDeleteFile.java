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
 * Test cases to verify the IImportRefactorer implementation. These tests focus on
 * deleting paths.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class TestImportRefactorDeleteFile {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	private IBuildStore buildStore;
	private IFileMgr fileMgr;
	private IActionMgr actionMgr;
	
	/* action and file IDs used in the tests */
	private int action, actionA, actionA1, actionA2, actionA3, actionA4, actionA5, actionA6;
	private int dirWork, fileWorkAIn, fileWorkBOut, fileWorkCOut, fileWorkDOut, fileWorkEIn;
	private int fileWorkFOut, fileWorkGOut, fileWorkHIn, fileWorkIOut, fileWorkJOut, fileWorkKOut;
	private int fileWorkMakefile, fileUnused, dirUnused, fileWorkLog;
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
		 *  actionA  : make all > log
		 *  actionA1 :   cp fileA.in fileB.out
		 *  actionA2 :   cp fileA.in fileC.out
		 *  actionA3 :   cp fileB.out fileD.out
		 *  actionA4 :   cp fileE.in fileF.out >fileG.out
		 *  actionA5 :   cp fileH.in fileI.out >fileJ.out
		 *  actionA6 :   cp fileI.out fileK.out
		 *  
		 *  dirWork           /home/work
		 *  fileWorkAIn       /home/work/fileA.in
		 *  fileWorkBOut      /home/work/fileB.out
		 *  fileWorkCOut      /home/work/fileC.out
		 *  fileWorkDOut      /home/work/fileD.out
		 *  fileWorkEIn       /home/work/fileE.in
		 *  fileWorkFOut      /home/work/fileF.out
		 *  fileWorkGOut      /home/work/fileG.out
		 *  fileWorkHIn       /home/work/fileH.in
		 *  fileWorkIOut      /home/work/fileI.out
		 *  fileWorkJOut      /home/work/fileJ.out
		 *  fileWorkKOut      /home/work/fileK.out
		 *  fileWorkMakefile  /home/work/Makefile
		 *  fileWorkLog       /home/work/log
		 *  fileUnused        /home/work/README
		 *  dirUnused         /home/work/unused
		 *                    /home/work/unused/1/2
		 *                    /home/work/unused/1/2/3
         *                    /home/work/unused/1/4
         *                    /home/work/unused/1/4/5
		 */

		dirWork          = fileMgr.addDirectory("/home/work");
		fileWorkAIn      = fileMgr.addFile("/home/work/fileA.in");
		fileWorkBOut     = fileMgr.addFile("/home/work/fileB.out");
		fileWorkCOut     = fileMgr.addFile("/home/work/fileC.out");
		fileWorkDOut     = fileMgr.addFile("/home/work/fileD.out");
		fileWorkEIn      = fileMgr.addFile("/home/work/fileE.in");
		fileWorkFOut     = fileMgr.addFile("/home/work/fileF.out");
		fileWorkGOut     = fileMgr.addFile("/home/work/fileG.out");
		fileWorkHIn      = fileMgr.addFile("/home/work/fileH.in");
		fileWorkIOut     = fileMgr.addFile("/home/work/fileI.out");
		fileWorkJOut     = fileMgr.addFile("/home/work/fileJ.out");
		fileWorkKOut     = fileMgr.addFile("/home/work/fileK.out");
		fileWorkMakefile = fileMgr.addFile("/home/work/Makefile");
		fileWorkLog      = fileMgr.addFile("/home/work/log");
		fileUnused       = fileMgr.addFile("/home/work/README");
		dirUnused        = fileMgr.addDirectory("/home/work/unused");
		fileMgr.addFile("/home/work/unused/1/2/3");
		fileMgr.addFile("/home/work/unused/1/4/5");
		
		action = actionMgr.getRootAction("root");
		
		actionA = actionMgr.addAction(action, dirWork, "make all");
		actionMgr.addFileAccess(actionA, fileWorkMakefile, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA, fileWorkLog, OperationType.OP_WRITE);
		
		actionA1 = actionMgr.addAction(actionA, dirWork, "cp fileA.in fileB.out");
		actionMgr.addFileAccess(actionA1, fileWorkAIn, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA1, fileWorkBOut, OperationType.OP_WRITE);
		
		actionA2 = actionMgr.addAction(actionA, dirWork, "cp fileA.in fileC.out");
		actionMgr.addFileAccess(actionA2, fileWorkAIn, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA2, fileWorkCOut, OperationType.OP_WRITE);
		
		actionA3 = actionMgr.addAction(actionA, dirWork, "cp fileB.out fileD.out");
		actionMgr.addFileAccess(actionA3, fileWorkBOut, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA3, fileWorkDOut, OperationType.OP_WRITE);

		actionA4 = actionMgr.addAction(actionA, dirWork, "cp fileE.in fileF.out > fileG.out");
		actionMgr.addFileAccess(actionA4, fileWorkEIn, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA4, fileWorkFOut, OperationType.OP_WRITE);
		actionMgr.addFileAccess(actionA4, fileWorkGOut, OperationType.OP_WRITE);

		actionA5 = actionMgr.addAction(actionA, dirWork, "cp fileH.in fileI.out > fileJ.out");
		actionMgr.addFileAccess(actionA5, fileWorkHIn, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA5, fileWorkIOut, OperationType.OP_WRITE);
		actionMgr.addFileAccess(actionA5, fileWorkJOut, OperationType.OP_WRITE);
		
		actionA6 = actionMgr.addAction(actionA, dirWork, "cp fileI.out fileK.out");
		actionMgr.addFileAccess(actionA6, fileWorkIOut, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA6, fileWorkKOut, OperationType.OP_WRITE);
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
	 * Test deletion of non-existent path - should fail.
	 * @throws Exception
	 */
	@Test
	public void testDeleteBadPath() throws Exception {
		
		/* 
		 * Delete a non-existent path - should give an error.
		 */
		int badPathId = 10000;
		try {
			importRefactorer.deletePath(badPathId, false);
			fail("Failed when trying to delete non-existent file.");

		} catch (CanNotRefactorException ex) {
			assertEquals(Cause.INVALID_PATH, ex.getCauseCode());
			assertArrayEquals(new Integer[] { badPathId } , ex.getPathIds());
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test deletion of an unused path - should work.
	 * @throws Exception
	 */
	@Test
	public void testDeleteUnusedPath() throws Exception {
		
		/* 
		 * Delete an unused path - there should be no problems doing this.
		 */
		assertFalse(fileMgr.isPathTrashed(fileUnused));
		try {
			importRefactorer.deletePath(fileUnused, false);
		} catch (CanNotRefactorException ex) {
			fail("Failed to delete: " + fileUnused);
		}
		assertTrue(fileMgr.isPathTrashed(fileUnused));
		
		/* Undo, then redo the operation - should succeed */
		importRefactorer.undoRefactoring();
		assertFalse(fileMgr.isPathTrashed(fileUnused));
		importRefactorer.redoRefactoring();
		assertTrue(fileMgr.isPathTrashed(fileUnused));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Attempt to delete a path twice - should fail the second time.
	 */
	@Test
	public void testDeleteUnusedPathTwice() {
		
		/* delete the file for the first time */
		assertFalse(fileMgr.isPathTrashed(fileUnused));
		try {
			importRefactorer.deletePath(fileUnused, false);
		} catch (CanNotRefactorException ex) {
			fail("Failed to delete: " + fileUnused);
		}
		assertTrue(fileMgr.isPathTrashed(fileUnused));

		/* delete the file a second time - should fail */
		try {
			importRefactorer.deletePath(fileUnused, false);
			fail("Failed when attempting to delete a file the second time.");
		} catch (CanNotRefactorException ex) {
			assertEquals(Cause.INVALID_PATH, ex.getCauseCode());
			assertArrayEquals(new Integer[] { fileUnused } , ex.getPathIds());
		}
		assertTrue(fileMgr.isPathTrashed(fileUnused));
	}

	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Attempt to delete a file (fileE.In) that's read by an action - should fail to delete.
	 * This is essentially a source file.
	 */
	@Test
	public void testDeleteSourceFileInUse() {
		
		try {
			importRefactorer.deletePath(fileWorkEIn, false);
			fail("Failed when trying to delete in-use file.");
			
		} catch (CanNotRefactorException ex) {
			assertEquals(Cause.PATH_IN_USE, ex.getCauseCode());
			assertArrayEquals(new Integer[] { actionA4 } , ex.getActionIds());
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Attempt to delete a file (fileB.out) that's read by an action - should fail to delete.
	 * This file is also generated by an action.
	 */
	@Test
	public void testDeleteGeneratedFileInUse() {
		
		try {
			importRefactorer.deletePath(fileWorkBOut, false);
			fail("Failed when trying to delete in-use file.");
			
		} catch (CanNotRefactorException ex) {
			assertEquals(Cause.PATH_IN_USE, ex.getCauseCode());
			assertArrayEquals(new Integer[] { actionA3 } , ex.getActionIds());
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Attempt to delete a file (fileK.out) that's generated, but not used by any actions. Set
	 * alsoDeleteActions to false.
	 */
	@Test
	public void testDeleteGeneratedFile() {
		
		try {
			importRefactorer.deletePath(fileWorkKOut, false);
			fail("Failed when trying to delete in-use file.");
			
		} catch (CanNotRefactorException ex) {
			assertEquals(Cause.PATH_IS_GENERATED, ex.getCauseCode());
			assertArrayEquals(new Integer[] { actionA6 } , ex.getActionIds());
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Attempt to delete a file (fileG.out) that's generated, but not used by any actions. Set
	 * alsoDeleteActions to true so that the generating action will also be deleted.
	 */
	@Test
	public void testDeleteGeneratedFileAndAction() {
		
		try {
			importRefactorer.deletePath(fileWorkGOut, true);			
			
		} catch (CanNotRefactorException ex) {
			fail("Failed to delete file and action.");
		}

		/*
		 * Check that the deletion and file and action, as well as all the action-file
		 * links has truly taken place.
		 */
		assertTrue(fileMgr.isPathTrashed(fileWorkGOut));
		assertTrue(actionMgr.isActionTrashed(actionA4));
		assertEquals(0, actionMgr.getFilesAccessed(actionA4, OperationType.OP_UNSPECIFIED).length);
		
		/*
		 * Undo the operation and check again.
		 */
		try {
			importRefactorer.undoRefactoring();
		} catch (CanNotRefactorException e) {
			fail("Failed to undo delete operation.");
		}
		assertFalse(fileMgr.isPathTrashed(fileWorkGOut));
		assertFalse(actionMgr.isActionTrashed(actionA4));
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] { fileWorkEIn }, 
				actionMgr.getFilesAccessed(actionA4, OperationType.OP_READ)));		
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] { fileWorkFOut, fileWorkGOut }, 
				actionMgr.getFilesAccessed(actionA4, OperationType.OP_WRITE)));		
		
		/*
		 * Finally, redo the operation and check again.
		 */
		try {
			importRefactorer.redoRefactoring();
		} catch (CanNotRefactorException e) {
			fail("Failed to undo delete operation.");
		}
		assertTrue(fileMgr.isPathTrashed(fileWorkGOut));
		assertTrue(actionMgr.isActionTrashed(actionA4));
		assertEquals(0, actionMgr.getFilesAccessed(actionA4, OperationType.OP_UNSPECIFIED).length);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Attempt to delete a file (fileJ.out) that's generated, but not used by any actions. Set
	 * alsoDeleteActions to true so that the generating action will also be deleted. However,
	 * this action also generates a second file (fileI.out) that's still in use. This is an error.
	 */
	@Test
	public void testDeleteGeneratedFileAndActionInUse() {
		
		try {
			importRefactorer.deletePath(fileWorkJOut, true);			
			fail("Failed when deleting an file/action that's in use.");
		} catch (CanNotRefactorException ex) {
			assertEquals(Cause.ACTION_IN_USE, ex.getCauseCode());
			assertArrayEquals(new Integer[] { fileWorkIOut }, ex.getPathIds());
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Attempt to delete a directory that's not empty - should fail to delete.
	 */
	@Test
	public void testDeleteDirectoryNotEmpty() {
		
		try {
			importRefactorer.deletePath(dirWork, false);
			fail("Failed when trying to delete non-empty directory.");
			
		} catch (CanNotRefactorException ex) {
			assertEquals(Cause.DIRECTORY_NOT_EMPTY, ex.getCauseCode());
			assertArrayEquals(new Integer[] { dirWork } , ex.getPathIds());
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Attempt to delete a file that's in use by a non-atomic action - should fail.
	 */
	@Test
	public void testDeleteFileWithNonAtomicAction() {

		try {
			importRefactorer.deletePath(fileWorkLog, true);
			fail("Failed when deleting file used by non-atomic action.");

		} catch (CanNotRefactorException ex) {
			assertEquals(Cause.ACTION_NOT_ATOMIC, ex.getCauseCode());
			assertArrayEquals(new Integer[] { actionA } , ex.getActionIds());
		}
	}

	/*-------------------------------------------------------------------------------------*/

	// TODO: delete a sub-tree where files are in use - fails.

	/*-------------------------------------------------------------------------------------*/

	// TODO: delete a sub-tree where files are all unused - succeeds.

	/*-------------------------------------------------------------------------------------*/

}
