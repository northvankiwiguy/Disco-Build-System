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

import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.IReportMgr;
import com.buildml.model.types.FileRecord;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.ActionSet;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestReportMgr {

	
	/** our test BuildStore object */
	private IBuildStore bs;

	/** our test FileMgr object */
	private IFileMgr fileMgr;
	
	/** our test ActionMgr object */
	private IActionMgr actionMgr;
	
	/** our test FileIncludes object */
	private IFileIncludeMgr fileIncludeMgr;
	
	/** our test Reports object */
	private IReportMgr reports;
	
	/** Our rootActionId, used for creating new build actions */
	private int rootActionId;
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		bs = CommonTestUtils.getEmptyBuildStore();
		fileMgr = bs.getFileMgr();
		actionMgr = bs.getActionMgr();
		fileIncludeMgr = bs.getFileIncludeMgr();
		reports = bs.getReportMgr();
		rootActionId = actionMgr.getRootAction("root");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IReportMgr#reportMostCommonlyAccessedFiles()}.
	 */
	@Test
	public void testReportMostCommonlyAccessedFiles() {
		
		/* add some files */
		int foxFile = fileMgr.addFile("/mydir/fox");
		int boxFile = fileMgr.addFile("/mydir/box");
		int soxFile = fileMgr.addFile("/mydir/sox");
		int roxFile = fileMgr.addFile("/mydir/rox");
		int dir = fileMgr.addDirectory("myEmptydir");

		/* create three different actions */
		int action1 = actionMgr.addShellCommandAction(rootActionId, 0, "command");
		int action2 = actionMgr.addShellCommandAction(rootActionId, 0, "command");		
		int action3 = actionMgr.addShellCommandAction(rootActionId, 0, "command");
		int action4 = actionMgr.addShellCommandAction(rootActionId, 0, "command");
		
		/* add references from the actions to the files - action1 uses three of them */
		actionMgr.addFileAccess(action1, foxFile, OperationType.OP_READ);
		actionMgr.addFileAccess(action1, boxFile, OperationType.OP_READ);
		actionMgr.addFileAccess(action1, soxFile, OperationType.OP_READ);
		
		/* action2 only uses 2 files */
		actionMgr.addFileAccess(action2, foxFile, OperationType.OP_READ);
		actionMgr.addFileAccess(action2, boxFile, OperationType.OP_READ);
		
		/* action3 uses 1 file - note that action4 uses none */
		actionMgr.addFileAccess(action3, foxFile, OperationType.OP_READ);

		/*
		 * The results of the search should be:
		 * 		foxFile - 3
		 * 		boxFile - 2
		 * 		soxFile - 1
		 */
		FileRecord results [] = reports.reportMostCommonlyAccessedFiles();
		
		/* should be 3 results passed back - the total number of files accessed */
		assertEquals(3, results.length);
		
		/* validate the order in which they were returned */
		assertEquals(foxFile, results[0].getId());
		assertEquals(3, results[0].getCount());
		assertEquals(boxFile, results[1].getId());
		assertEquals(2, results[1].getCount());
		assertEquals(soxFile, results[2].getId());
		assertEquals(1, results[2].getCount());
		
		/* now add roxFile into the mix - access it lots */
		actionMgr.addFileAccess(action1, roxFile, OperationType.OP_READ);
		actionMgr.addFileAccess(action2, roxFile, OperationType.OP_READ);
		actionMgr.addFileAccess(action3, roxFile, OperationType.OP_READ);
		actionMgr.addFileAccess(action4, roxFile, OperationType.OP_READ);
		
		/*
		 * Check again, roxFile should now be the most popular.
		 */
		results = reports.reportMostCommonlyAccessedFiles();
		assertEquals(4, results.length);
		
		/* validate the order in which they were returned */
		assertEquals(roxFile, results[0].getId());
		assertEquals(4, results[0].getCount());
		assertEquals(foxFile, results[1].getId());
		assertEquals(3, results[1].getCount());
		assertEquals(boxFile, results[2].getId());
		assertEquals(2, results[2].getCount());
		assertEquals(soxFile, results[3].getId());
		assertEquals(1, results[3].getCount());
				
		/*
		 * Access a directory, this shouldn't be in the results.
		 */
		actionMgr.addFileAccess(action1, dir, OperationType.OP_READ);
		results = reports.reportMostCommonlyAccessedFiles();
		assertEquals(4, results.length);		
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for {@link com.buildml.model.IReportMgr#reportMostCommonlyAccessedFiles()}.
	 */
	@Test
	public void testScalabilityOfReportMostCommonlyAccessedFiles() {

		int numFiles = 10000;
		int numActions = 100;
		int filesPerAction = 2000;
		
		boolean prevState = bs.setFastAccessMode(true);
		Random r = new Random();
		
		/* add a bunch of files */
		for (int i = 0; i != numFiles; i++) {
			fileMgr.addFile(String.valueOf(r.nextInt()));
		}
		
		/* add a (small) bunch of actions, and associate files with them. */
		for (int i = 0; i != numActions; i++) {
			int actionId = actionMgr.addShellCommandAction(rootActionId, 0, "command");
			
			for (int j = 0; j != filesPerAction; j++) {
				actionMgr.addFileAccess(actionId, r.nextInt(numFiles), OperationType.OP_READ);
			}
		}
		bs.setFastAccessMode(prevState);

		/* now, run a report - we don't care about the results, just the response time */
		reports.reportMostCommonlyAccessedFiles();
	}
	
	/*-------------------------------------------------------------------------------------*/	
	
	/**
	 * Test method for {@link com.buildml.model.IReportMgr#reportMostCommonIncludersOfFile(int)}.
	 */
	@Test
	public void testreportMostCommonIncludersOfFile() {
	
		/* create some files */
		int file1 = fileMgr.addFile("/mydir/files/fileA.h");
		int file2 = fileMgr.addFile("/mydir/files/fileB.h");
		int file3 = fileMgr.addFile("/mydir/files/fileC.h");
		int file4 = fileMgr.addFile("/mydir/files/fileD.h");
		fileMgr.addFile("/mydir/files/fileE.h");
	
		/* register the include relationships, all for file2 */
		fileIncludeMgr.addFileIncludes(file1, file2);
		fileIncludeMgr.addFileIncludes(file3, file2);
		fileIncludeMgr.addFileIncludes(file1, file4);
		fileIncludeMgr.addFileIncludes(file1, file2);
		fileIncludeMgr.addFileIncludes(file3, file2);
		fileIncludeMgr.addFileIncludes(file4, file2);
		fileIncludeMgr.addFileIncludes(file2, file4);
		fileIncludeMgr.addFileIncludes(file1, file2);
		fileIncludeMgr.addFileIncludes(file1, file2);

		/* 
		 * Results should be:
		 * 		file1  - 4 times
		 *      file3  - 2 times
		 *      file4  - 1 time
		 */
		FileRecord results[] = reports.reportMostCommonIncludersOfFile(file2);
		assertEquals(3, results.length);
		assertEquals(file1, results[0].getId());
		assertEquals(4, results[0].getCount());
		assertEquals(file3, results[1].getId());
		assertEquals(2, results[1].getCount());
		assertEquals(file4, results[2].getId());
		assertEquals(1, results[2].getCount());
		
		/* Check for file3 which isn't included by anybody - should be empty list */
		results = reports.reportMostCommonIncludersOfFile(file3);		
		assertEquals(0, results.length);
		
		/* Check for a file that doesn't exist at all - should be empty list */
		results = reports.reportMostCommonIncludersOfFile(file3);		
		assertEquals(0, results.length);
	}
	
	/*-------------------------------------------------------------------------------------*/	

	/**
	 * Test method for {@link com.buildml.model.IReportMgr#reportMostCommonlyAccessedFiles()}.
	 */
	@Test
	public void testScalabilityOfReportMostCommonIncludesOfFile() {

		int numFiles = 20000;
		int numIncludes = 200000;
		
		boolean prevState = bs.setFastAccessMode(true);
		Random r = new Random();
		
		int file1 = fileMgr.addFile("/file1");
		int file2 = fileMgr.addFile("/file2");
		
		/* add a bunch of files - some include file*/
		for (int i = 0; i != numIncludes; i++) {
			fileIncludeMgr.addFileIncludes(r.nextInt(numFiles), file1);
			fileIncludeMgr.addFileIncludes(r.nextInt(numFiles), file2);
		}
		
		bs.setFastAccessMode(prevState);

		/* now, run a report - we don't care about the results, just the response time */
		reports.reportMostCommonIncludersOfFile(file1);
	}
	
	/*-------------------------------------------------------------------------------------*/	

	/**
	 * Test method for {@link com.buildml.model.IReportMgr#reportFilesNeverAccessed()}.
	 */
	@Test
	public void testFilesNeverAccessed() {
		
		/* without any files in the database, return the empty list */
		FileSet results = reports.reportFilesNeverAccessed();
		assertEquals(0, results.size());
		
		/* add some files and a couple of actions */
		int file1 = fileMgr.addFile("/home/psmith/myfile1");
		int file2 = fileMgr.addFile("/home/psmith/myfile2");
		int file3 = fileMgr.addFile("/home/psmith/myfile3");
		int file4 = fileMgr.addFile("/home/psmith/myfile4");
		
		int action1 = actionMgr.addShellCommandAction(rootActionId, 0, "action1");
		int action2 = actionMgr.addShellCommandAction(rootActionId, 0, "action2");

		/* access some */
		actionMgr.addFileAccess(action1, file1, OperationType.OP_READ);
		actionMgr.addFileAccess(action1, file2, OperationType.OP_WRITE);
		
		/* file3 and file4 should both be in the results, but we don't know the order */
		results = reports.reportFilesNeverAccessed();
		assertEquals(2, results.size());
		assertTrue(results.isMember(file3));
		assertTrue(results.isMember(file4));
				
		/* another action should access those same files, and the results will be the same */
		actionMgr.addFileAccess(action2, file2, OperationType.OP_READ);
		actionMgr.addFileAccess(action2, file1, OperationType.OP_WRITE);
		results = reports.reportFilesNeverAccessed();
		assertEquals(2, results.size());
		assertTrue(results.isMember(file3));
		assertTrue(results.isMember(file4));
				
		/* now access file 3 */
		actionMgr.addFileAccess(action1, file3, OperationType.OP_READ);
		results = reports.reportFilesNeverAccessed();
		assertEquals(1, results.size());
		assertTrue(results.isMember(file4));
		assertFalse(results.isMember(file3));
		
		/* finally access file 3 */
		actionMgr.addFileAccess(action2, file4, OperationType.OP_READ);
		results = reports.reportFilesNeverAccessed();
		assertEquals(0, results.size());
	}

	/*-------------------------------------------------------------------------------------*/	

	/**
	 * Test method for {@link com.buildml.model.IReportMgr#reportFilesThatMatchName(String)}.
	 */
	@Test
	public void testReportFilesThatMatchName() {

		/* the null argument should return the empty set */
		FileSet results = reports.reportFilesThatMatchName(null);
		assertEquals(0, results.size());

		/* without any files in the database, return the empty list, no matter what the argument */
		results = reports.reportFilesThatMatchName("");
		assertEquals(0, results.size());
		results = reports.reportFilesThatMatchName("Makefile");
		assertEquals(0, results.size());

		/* add some files */		
		int file1 = fileMgr.addFile("/home/psmith/myfile");
		int file2 = fileMgr.addFile("/home/psmith/src/myfile");
		int file3 = fileMgr.addFile("/home/psmith/src/myfile2");
		int file4 = fileMgr.addFile("/home/psmith/src/lib/myfile");
		int file5 = fileMgr.addFile("/home/psmith/src/lib/myfile2");
		
		/* search for something that doesn't exist at all */
		results = reports.reportFilesThatMatchName("Makefile");
		assertEquals(0, results.size());
		
		/* search for things that exist */
		results = reports.reportFilesThatMatchName("myfile");
		assertEquals(3, results.size());
		assertTrue(results.isMember(file1));
		assertTrue(results.isMember(file2));
		assertTrue(results.isMember(file4));
		
		results = reports.reportFilesThatMatchName("myfile2");
		assertEquals(2, results.size());
		assertTrue(results.isMember(file3));
		assertTrue(results.isMember(file5));

		/* directories shouldn't match - files only */
		results = reports.reportFilesThatMatchName("home");
		assertEquals(0, results.size());
		results = reports.reportFilesThatMatchName("src");
		assertEquals(0, results.size());		
	}
	
	/*-------------------------------------------------------------------------------------*/	

	/**
	 * Test method for {@link com.buildml.model.IReportMgr#reportActionsThatMatchName(String)}.
	 */
	@Test
	public void testReportActionsThatMatchName() {
	
		int action1 = actionMgr.addShellCommandAction(0, 0, "My command number 1");
		int action2 = actionMgr.addShellCommandAction(0, 0, "My command number 2");
		int action3 = actionMgr.addShellCommandAction(0, 0, "Another with number 1 in it");
		int action4 = actionMgr.addShellCommandAction(0, 0, "A completely different action");
		int action5 = actionMgr.addShellCommandAction(0, 0, "A final command with 1 in it");
	
		/* match commands that contain "command" */
		ActionSet results = reports.reportActionsThatMatchName("%command%");
		CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action2, action5});
		
		/* match commands with the number 1 in them */
		results = reports.reportActionsThatMatchName("%1%");
		CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action3, action5});
		
		/* match command that start with the letter A */
		results = reports.reportActionsThatMatchName("A%");
		CommonTestUtils.treeSetEqual(results, new Integer[] {action3, action4, action5});
		
		/* match commands containing the word "action" */
		results = reports.reportActionsThatMatchName("%action%");
		CommonTestUtils.treeSetEqual(results, new Integer[] {action4});
		
		/* match commands containing "elephant" */
		results = reports.reportActionsThatMatchName("%elephant%");
		CommonTestUtils.treeSetEqual(results, new Integer[] {});
		
		/* test with the empty pattern */
		results = reports.reportActionsThatMatchName("");
		CommonTestUtils.treeSetEqual(results, new Integer[] {});		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IReportMgr#reportActionsThatAccessFiles(FileSet, OperationType)}.
	 */
	@Test
	public void testReportActionsThatAccessFiles() {

		/* Create a bunch of files */
		int file1a = fileMgr.addFile("/file1a");
		int file1b = fileMgr.addFile("/file1b");
		int file2a = fileMgr.addFile("/file2a");
		int file2b = fileMgr.addFile("/file2b");
		int file3a = fileMgr.addFile("/file3a");
		int file3b = fileMgr.addFile("/file3b");

		int rootAction = actionMgr.getRootAction("");
		
		/* action 1 reads/writes file1a and file1b */
		int action1a = actionMgr.addShellCommandAction(rootAction, 0, "action1 command reads file1a");
		int action1b = actionMgr.addShellCommandAction(rootAction, 0, "action1 command writes file1b");
		actionMgr.addFileAccess(action1a, file1a, OperationType.OP_READ);
		actionMgr.addFileAccess(action1b, file1b, OperationType.OP_WRITE);

		/* action 2 reads/writes file2a and file2b */
		int action2a = actionMgr.addShellCommandAction(rootAction, 0, "action2 command reads file2a");
		int action2b = actionMgr.addShellCommandAction(rootAction, 0, "action2 command writes file2b");
		actionMgr.addFileAccess(action2a, file2a, OperationType.OP_READ);
		actionMgr.addFileAccess(action2b, file2b, OperationType.OP_WRITE);

		/* action 3 reads/writes file3a and file3b */
		int action3a = actionMgr.addShellCommandAction(rootAction, 0, "action3 command reads file3a");
		int action3b = actionMgr.addShellCommandAction(rootAction, 0, "action3 command writes file3b");
		actionMgr.addFileAccess(action3a, file3a, OperationType.OP_READ);
		actionMgr.addFileAccess(action3b, file3b, OperationType.OP_WRITE);

		/* test the report for an empty FileSet */
		FileSet source = new FileSet(fileMgr);
		ActionSet resultReads = reports.reportActionsThatAccessFiles(source, OperationType.OP_READ);
		ActionSet resultWrites = reports.reportActionsThatAccessFiles(source, OperationType.OP_WRITE);
		ActionSet resultUses = reports.reportActionsThatAccessFiles(source, OperationType.OP_UNSPECIFIED);
		assertEquals(0, resultReads.size());
		assertEquals(0, resultWrites.size());
		assertEquals(0, resultUses.size());
		
		/* test with only file1a */
		source = new FileSet(fileMgr);
		source.add(file1a);
		resultReads = reports.reportActionsThatAccessFiles(source, OperationType.OP_READ);
		resultWrites = reports.reportActionsThatAccessFiles(source, OperationType.OP_WRITE);
		resultUses = reports.reportActionsThatAccessFiles(source, OperationType.OP_UNSPECIFIED);
		assertEquals(1, resultReads.size());
		assertTrue(resultReads.isMember(action1a));
		assertEquals(0, resultWrites.size());
		assertEquals(1, resultUses.size());
		assertTrue(resultUses.isMember(action1a));

		/* test with only file1b */
		source = new FileSet(fileMgr);
		source.add(file1b);
		resultReads = reports.reportActionsThatAccessFiles(source, OperationType.OP_READ);
		resultWrites = reports.reportActionsThatAccessFiles(source, OperationType.OP_WRITE);
		resultUses = reports.reportActionsThatAccessFiles(source, OperationType.OP_UNSPECIFIED);
		assertEquals(0, resultReads.size());
		assertEquals(1, resultWrites.size());
		assertTrue(resultWrites.isMember(action1b));
		assertEquals(1, resultUses.size());
		assertTrue(resultUses.isMember(action1b));
		
		/* test with both file1a and file1b */
		source = new FileSet(fileMgr);
		source.add(file1a);
		source.add(file1b);
		resultReads = reports.reportActionsThatAccessFiles(source, OperationType.OP_READ);
		resultWrites = reports.reportActionsThatAccessFiles(source, OperationType.OP_WRITE);
		resultUses = reports.reportActionsThatAccessFiles(source, OperationType.OP_UNSPECIFIED);
		assertEquals(1, resultReads.size());
		assertTrue(resultReads.isMember(action1a));
		assertEquals(1, resultWrites.size());
		assertTrue(resultWrites.isMember(action1b));
		assertEquals(2, resultUses.size());
		assertTrue(resultUses.isMember(action1a));
		assertTrue(resultUses.isMember(action1b));
		
		/* test with file1a and file2a */
		source = new FileSet(fileMgr);
		source.add(file1a);
		source.add(file2a);
		resultReads = reports.reportActionsThatAccessFiles(source, OperationType.OP_READ);
		resultWrites = reports.reportActionsThatAccessFiles(source, OperationType.OP_WRITE);
		resultUses = reports.reportActionsThatAccessFiles(source, OperationType.OP_UNSPECIFIED);
		assertEquals(2, resultReads.size());
		assertTrue(resultReads.isMember(action1a));
		assertTrue(resultReads.isMember(action2a));
		assertEquals(0, resultWrites.size());
		assertEquals(2, resultUses.size());
		assertTrue(resultUses.isMember(action1a));
		assertTrue(resultUses.isMember(action2a));

		/* test with file1a, file2a and file3b */
		source = new FileSet(fileMgr);
		source.add(file1a);
		source.add(file2a);
		source.add(file3b);
		resultReads = reports.reportActionsThatAccessFiles(source, OperationType.OP_READ);
		resultWrites = reports.reportActionsThatAccessFiles(source, OperationType.OP_WRITE);
		resultUses = reports.reportActionsThatAccessFiles(source, OperationType.OP_UNSPECIFIED);
		assertEquals(2, resultReads.size());
		assertTrue(resultReads.isMember(action1a));
		assertTrue(resultReads.isMember(action2a));
		assertEquals(1, resultWrites.size());
		assertTrue(resultWrites.isMember(action3b));
		assertEquals(3, resultUses.size());
		assertTrue(resultUses.isMember(action1a));
		assertTrue(resultUses.isMember(action2a));
		assertTrue(resultUses.isMember(action3b));
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for {@link com.buildml.model.IReportMgr#reportFilesAccessedByActions(ActionSet, OperationType)}.
	 */
	@Test
	public void testFilesAccessedByActions() {
		
		/* create some actions, and some file accesses for each */
		int file1 = fileMgr.addFile("/a/b/c.java");
		int file2 = fileMgr.addFile("/a/b/d.java");
		int file3 = fileMgr.addFile("/a/b/e.java");

		int root = actionMgr.getRootAction("");
		int action1 = actionMgr.addShellCommandAction(root, 0, "");
		actionMgr.addFileAccess(action1, file1, OperationType.OP_WRITE);
		actionMgr.addFileAccess(action1, file2, OperationType.OP_READ);

		int action2 = actionMgr.addShellCommandAction(root, 0, "");
		actionMgr.addFileAccess(action2, file1, OperationType.OP_READ);
		actionMgr.addFileAccess(action2, file3, OperationType.OP_READ);

		int action3 = actionMgr.addShellCommandAction(root, 0, "");
		actionMgr.addFileAccess(action3, file3, OperationType.OP_WRITE);
		actionMgr.addFileAccess(action3, file3, OperationType.OP_WRITE);

		/* test with the empty ActionSet - should be no files returned */
		ActionSet ts = new ActionSet(actionMgr);
		FileSet result = reports.reportFilesAccessedByActions(ts, OperationType.OP_UNSPECIFIED);
		assertEquals(0, result.size());
		
		/* test with a single action, looking for all accessed files */
		ts.add(action1);
		result = reports.reportFilesAccessedByActions(ts, OperationType.OP_UNSPECIFIED);
		assertEquals(2, result.size());
		assertTrue(result.isMember(file1));
		assertTrue(result.isMember(file2));
		
		/* test with a single action, looking for all read files */
		result = reports.reportFilesAccessedByActions(ts, OperationType.OP_READ);
		assertEquals(1, result.size());
		assertTrue(result.isMember(file2));

		/* test with a single action, looking for all written files */
		result = reports.reportFilesAccessedByActions(ts, OperationType.OP_WRITE);
		assertEquals(1, result.size());
		assertTrue(result.isMember(file1));

		/* test with two actions, looking for all accessed files */
		ts.add(action2);
		result = reports.reportFilesAccessedByActions(ts, OperationType.OP_UNSPECIFIED);
		assertEquals(3, result.size());
		assertTrue(result.isMember(file1));
		assertTrue(result.isMember(file2));
		assertTrue(result.isMember(file3));
		
		/* test with two actions, looking for all read files */
		result = reports.reportFilesAccessedByActions(ts, OperationType.OP_READ);
		assertEquals(3, result.size());
		assertTrue(result.isMember(file1));
		assertTrue(result.isMember(file2));
		assertTrue(result.isMember(file3));

		/* test with two actions, looking for all written files */
		result = reports.reportFilesAccessedByActions(ts, OperationType.OP_WRITE);
		assertEquals(1, result.size());
		assertTrue(result.isMember(file1));

		/* test with three actions, looking for all written files */
		ts.add(action3);
		result = reports.reportFilesAccessedByActions(ts, OperationType.OP_WRITE);
		assertEquals(2, result.size());
		assertTrue(result.isMember(file1));
		assertTrue(result.isMember(file3));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IReportMgr#reportActionsInDirectory(FileSet)}.
	 */
	@Test
	public void testActionsInDirectory() {
		
		/*
		 * Create a couple of directory, and execute actions in those directories.
		 */
		int dirIdA = fileMgr.addDirectory("/dirA");
		int dirIdB = fileMgr.addDirectory("/dirB");
		int fileIdC = fileMgr.addFile("/file");
		int rootAction = actionMgr.getRootAction("root");
		int action1 = actionMgr.addShellCommandAction(rootAction, dirIdA, "");
		int action2 = actionMgr.addShellCommandAction(rootAction, dirIdA, "");
		int action3 = actionMgr.addShellCommandAction(rootAction, dirIdB, "");
		int action4 = actionMgr.addShellCommandAction(rootAction, dirIdB, "");
	
		FileSet dirs = new FileSet(fileMgr);
		
		/* empty directory list => no actions */
		ActionSet results = reports.reportActionsInDirectory(dirs);
		assertEquals(0, results.size());
		
		/* files (non-directories) provide no results */
		dirs.add(fileIdC);
		results = reports.reportActionsInDirectory(dirs);
		assertEquals(0, results.size());
		
		/* with one directory => two of the actions */
		dirs.add(dirIdB);
		results = reports.reportActionsInDirectory(dirs);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] { action3, action4}));

		/* with two directories => four of the actions */
		dirs.add(dirIdA);
		results = reports.reportActionsInDirectory(dirs);
		assertTrue(CommonTestUtils.treeSetEqual(results, 
				new Integer[] { action1, action2, action3, action4}));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IReportMgr#reportWriteOnlyFiles()}.
	 */
	@Test
	public void testWriteOnlyFiles() {
	
		/* 
		 * Create a bunch of files, with different characteristics.
		 * But first, run the report - should be empty.
		 */
		FileSet result = reports.reportWriteOnlyFiles();
		assertEquals(0, result.size());
		
		/* a.java will be compiled into a.class, which goes into prog.jar (see later) */
		int fileAJava = fileMgr.addFile("/a.java");
		int fileAClass = fileMgr.addFile("/a.class");
		int actionA = actionMgr.addShellCommandAction(0, 0, "javac a.java");
		actionMgr.addFileAccess(actionA, fileAJava, OperationType.OP_READ);
		actionMgr.addFileAccess(actionA, fileAClass, OperationType.OP_WRITE);

		/* run the report - should show a.class */
		result = reports.reportWriteOnlyFiles();
		assertEquals(1, result.size());
		assertTrue(result.isMember(fileAClass));		
		
		/* b.java will be compiled into b.class, which goes into prog.jar (see later) */		
		int fileBJava = fileMgr.addFile("/b.java");
		int fileBClass = fileMgr.addFile("/b.class");
		int actionB = actionMgr.addShellCommandAction(0, 0, "javac b.java");
		actionMgr.addFileAccess(actionB, fileBJava, OperationType.OP_READ);
		actionMgr.addFileAccess(actionB, fileBClass, OperationType.OP_WRITE);
		
		/* Run the report - a.class and b.class should be listed */
		result = reports.reportWriteOnlyFiles();
		assertEquals(2, result.size());
		assertTrue(result.isMember(fileAClass));
		assertTrue(result.isMember(fileBClass));
		
		/* c.java will be compiled into c.class, but no further */
		int fileCJava = fileMgr.addFile("/c.java");
		int fileCClass = fileMgr.addFile("/c.class");
		int actionC = actionMgr.addShellCommandAction(0, 0, "javac c.java");
		actionMgr.addFileAccess(actionC, fileCJava, OperationType.OP_READ);
		actionMgr.addFileAccess(actionC, fileCClass, OperationType.OP_WRITE);
		
		/* d.java is not read at all */
		fileMgr.addFile("/d.java");		

		/* Run the report - a.class, b.class and c.class should be listed */
		result = reports.reportWriteOnlyFiles();
		assertEquals(3, result.size());
		assertTrue(result.isMember(fileAClass));
		assertTrue(result.isMember(fileBClass));
		assertTrue(result.isMember(fileCClass));
		
		/* now put A.class and B.class into prog.jar */
		int fileProgJar = fileMgr.addFile("/prog.jar");
		int actionProg = actionMgr.addShellCommandAction(0, 0, "jar cf prog.jar a.class b.class");
		actionMgr.addFileAccess(actionProg, fileAClass, OperationType.OP_READ);
		actionMgr.addFileAccess(actionProg, fileBClass, OperationType.OP_READ);
		actionMgr.addFileAccess(actionProg, fileProgJar, OperationType.OP_WRITE);
	
		/* Run the report - only c.class and prog.jar should be listed */
		result = reports.reportWriteOnlyFiles();
		assertEquals(2, result.size());
		assertTrue(result.isMember(fileCClass));
		assertTrue(result.isMember(fileProgJar));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IReportMgr#reportAllFiles()}.
	 */
	@Test
	public void testAllFiles() {

		int dirRoot = fileMgr.getPath("/");

		/* an empty file store still has the "/" path in it */
		FileSet result = reports.reportAllFiles();
		assertEquals(2, result.size());		/* includes /tmp */
		assertTrue(result.isMember(dirRoot));
		
		/* add a bunch of paths and test that their in the set */
		int fileAJava = fileMgr.addFile("/a.java");
		int fileBJava = fileMgr.addFile("/b.java");
		int fileCJava = fileMgr.addFile("/c.java");
		int fileAC = fileMgr.addFile("/a/A.c");
		int fileBC = fileMgr.addFile("/a/b/B.c");
		int fileCC = fileMgr.addFile("/a/b/c/C.c");
		int dirA = fileMgr.getPath("/a");
		int dirB = fileMgr.getPath("/a/b");
		int dirC = fileMgr.getPath("/a/b/c");
		
		result = reports.reportAllFiles();
		assertEquals(11, result.size());		/* includes /tmp */
		assertTrue(result.isMember(fileAJava));
		assertTrue(result.isMember(fileBJava));
		assertTrue(result.isMember(fileCJava));
		assertTrue(result.isMember(fileAC));
		assertTrue(result.isMember(fileBC));
		assertTrue(result.isMember(fileCC));
		assertTrue(result.isMember(dirA));
		assertTrue(result.isMember(dirB));
		assertTrue(result.isMember(dirC));
		
		/* delete a few paths and try again */
		fileMgr.movePathToTrash(fileAC);
		result = reports.reportAllFiles();
		assertEquals(10, result.size());
		assertFalse(result.isMember(fileAC));

		fileMgr.movePathToTrash(fileCC);
		result = reports.reportAllFiles();
		assertEquals(9, result.size());
		assertFalse(result.isMember(fileCC));

		fileMgr.movePathToTrash(dirC);
		result = reports.reportAllFiles();
		assertEquals(8, result.size());
		assertFalse(result.isMember(dirC));
	}
	
	/*-------------------------------------------------------------------------------------*/		
}