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

package com.arapiki.disco.model;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import com.arapiki.disco.model.BuildTasks.OperationType;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestReports {

	
	/* our BuildStore, and sub-objects, used for testing */
	private BuildStore bs;
	private FileNameSpaces fns;
	private BuildTasks bts;
	private FileIncludes fis;
	
	/* our Reports object, used for testing */
	private Reports reports;
	
	/* our rootTaskId, used for creating new build tasks */
	private int rootTaskId;
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		bs = CommonTestUtils.getEmptyBuildStore();
		fns = bs.getFileNameSpaces();
		bts = bs.getBuildTasks();
		fis = bs.getFileIncludes();
		reports = bs.getReports();
		rootTaskId = bts.getRootTask("root");
	}
	
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper function for creating a new FileRecord and adding it to a FileSet
	 * @param fileSet The FileSet to add the new record to
	 * @param pathId The new FileRecord's pathId
	 */
	private void addFileRecord(FileSet fileSet,int pathId) {
		FileRecord fr = new FileRecord(pathId);
		fileSet.add(fr);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.Reports#reportMostCommonlyAccessedFiles()}.
	 */
	@Test
	public void testReportMostCommonlyAccessedFiles() {
		
		/* add some files */
		int foxFile = fns.addFile("/mydir/fox");
		int boxFile = fns.addFile("/mydir/box");
		int soxFile = fns.addFile("/mydir/sox");
		int roxFile = fns.addFile("/mydir/rox");
		int dir = fns.addDirectory("myEmptydir");

		/* create three different tasks */
		int task1 = bts.addBuildTask(rootTaskId, 0, "command");
		int task2 = bts.addBuildTask(rootTaskId, 0, "command");		
		int task3 = bts.addBuildTask(rootTaskId, 0, "command");
		int task4 = bts.addBuildTask(rootTaskId, 0, "command");
		
		/* add references from the tasks to the files - task1 uses three of them */
		bts.addFileAccess(task1, foxFile, OperationType.OP_READ);
		bts.addFileAccess(task1, boxFile, OperationType.OP_READ);
		bts.addFileAccess(task1, soxFile, OperationType.OP_READ);
		
		/* task2 only uses 2 files */
		bts.addFileAccess(task2, foxFile, OperationType.OP_READ);
		bts.addFileAccess(task2, boxFile, OperationType.OP_READ);
		
		/* task3 uses 1 file - note that task4 uses none */
		bts.addFileAccess(task3, foxFile, OperationType.OP_READ);

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
		bts.addFileAccess(task1, roxFile, OperationType.OP_READ);
		bts.addFileAccess(task2, roxFile, OperationType.OP_READ);
		bts.addFileAccess(task3, roxFile, OperationType.OP_READ);
		bts.addFileAccess(task4, roxFile, OperationType.OP_READ);
		
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
		bts.addFileAccess(task1, dir, OperationType.OP_READ);
		results = reports.reportMostCommonlyAccessedFiles();
		assertEquals(4, results.length);		
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for {@link com.arapiki.disco.model.Reports#reportMostCommonlyAccessedFiles()}.
	 */
	@Test
	public void testScalabilityOfReportMostCommonlyAccessedFiles() {

		int numFiles = 10000;
		int numTasks = 100;
		int filesPerTask = 2000;
		
		bs.setFastAccessMode(true);
		Random r = new Random();
		
		/* add a bunch of files */
		for (int i = 0; i != numFiles; i++) {
			fns.addFile(String.valueOf(r.nextInt()));
		}
		
		/* add a (small) bunch of tasks, and associate files with them. */
		for (int i = 0; i != numTasks; i++) {
			int taskId = bts.addBuildTask(rootTaskId, 0, "command");
			
			for (int j = 0; j != filesPerTask; j++) {
				bts.addFileAccess(taskId, r.nextInt(numFiles), OperationType.OP_READ);
			}
		}
		bs.setFastAccessMode(false);

		/* now, run a report - we don't care about the results, just the response time */
		reports.reportMostCommonlyAccessedFiles();
	}
	
	/*-------------------------------------------------------------------------------------*/	
	
	/**
	 * Test method for {@link com.arapiki.disco.model.Reports#reportMostCommonIncludersOfFile()}.
	 */
	@Test
	public void testreportMostCommonIncludersOfFile() throws Exception {
	
		/* create some files */
		int file1 = fns.addFile("/mydir/files/fileA.h");
		int file2 = fns.addFile("/mydir/files/fileB.h");
		int file3 = fns.addFile("/mydir/files/fileC.h");
		int file4 = fns.addFile("/mydir/files/fileD.h");
		int file5 = fns.addFile("/mydir/files/fileE.h");
	
		/* register the include relationships, all for file2 */
		fis.addFileIncludes(file1, file2);
		fis.addFileIncludes(file3, file2);
		fis.addFileIncludes(file1, file4);
		fis.addFileIncludes(file1, file2);
		fis.addFileIncludes(file3, file2);
		fis.addFileIncludes(file4, file2);
		fis.addFileIncludes(file2, file4);
		fis.addFileIncludes(file1, file2);
		fis.addFileIncludes(file1, file2);

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
	 * Test method for {@link com.arapiki.disco.model.Reports#reportMostCommonlyAccessedFiles()}.
	 */
	@Test
	public void testScalabilityOfReportMostCommonIncludesOfFile() {

		int numFiles = 20000;
		int numIncludes = 200000;
		
		bs.setFastAccessMode(true);
		Random r = new Random();
		
		int file1 = fns.addFile("/file1");
		int file2 = fns.addFile("/file2");
		
		/* add a bunch of files - some include file*/
		for (int i = 0; i != numIncludes; i++) {
			fis.addFileIncludes(r.nextInt(numFiles), file1);
			fis.addFileIncludes(r.nextInt(numFiles), file2);
		}
		
		bs.setFastAccessMode(false);

		/* now, run a report - we don't care about the results, just the response time */
		FileRecord results [] = reports.reportMostCommonIncludersOfFile(file1);
	}
	
	/*-------------------------------------------------------------------------------------*/	

	/**
	 * Test method for {@link com.arapiki.disco.model.Reports#reportFilesNeverAccessed()}.
	 */
	@Test
	public void testFilesNeverAccessed() throws Exception {
		
		/* without any files in the database, return the empty list */
		FileSet results = reports.reportFilesNeverAccessed();
		assertEquals(0, results.size());
		
		/* add some files and a couple of tasks */
		int file1 = fns.addFile("/home/psmith/myfile1");
		int file2 = fns.addFile("/home/psmith/myfile2");
		int file3 = fns.addFile("/home/psmith/myfile3");
		int file4 = fns.addFile("/home/psmith/myfile4");
		
		int task1 = bts.addBuildTask(rootTaskId, 0, "task1");
		int task2 = bts.addBuildTask(rootTaskId, 0, "task2");

		/* access some */
		bts.addFileAccess(task1, file1, OperationType.OP_READ);
		bts.addFileAccess(task1, file2, OperationType.OP_WRITE);
		
		/* file3 and file4 should both be in the results, but we don't know the order */
		results = reports.reportFilesNeverAccessed();
		assertEquals(2, results.size());
		assertTrue(results.isMember(file3));
		assertTrue(results.isMember(file4));
				
		/* another task should access those same files, and the results will be the same */
		bts.addFileAccess(task2, file2, OperationType.OP_READ);
		bts.addFileAccess(task2, file1, OperationType.OP_WRITE);
		results = reports.reportFilesNeverAccessed();
		assertEquals(2, results.size());
		assertTrue(results.isMember(file3));
		assertTrue(results.isMember(file4));
				
		/* now access file 3 */
		bts.addFileAccess(task1, file3, OperationType.OP_READ);
		results = reports.reportFilesNeverAccessed();
		assertEquals(1, results.size());
		assertTrue(results.isMember(file4));
		assertFalse(results.isMember(file3));
		
		/* finally access file 3 */
		bts.addFileAccess(task2, file4, OperationType.OP_READ);
		results = reports.reportFilesNeverAccessed();
		assertEquals(0, results.size());
	}

	/*-------------------------------------------------------------------------------------*/	

	/**
	 * Test method for {@link com.arapiki.disco.model.Reports#reportFilesThatMatchName()}.
	 */
	@Test
	public void testReportFilesThatMatchName() throws Exception {

		/* the null argument should return the empty set */
		FileSet results = reports.reportFilesThatMatchName(null);
		assertEquals(0, results.size());

		/* without any files in the database, return the empty list, no matter what the argument */
		results = reports.reportFilesThatMatchName("");
		assertEquals(0, results.size());
		results = reports.reportFilesThatMatchName("Makefile");
		assertEquals(0, results.size());

		/* add some files */		
		int file1 = fns.addFile("/home/psmith/myfile");
		int file2 = fns.addFile("/home/psmith/src/myfile");
		int file3 = fns.addFile("/home/psmith/src/myfile2");
		int file4 = fns.addFile("/home/psmith/src/lib/myfile");
		int file5 = fns.addFile("/home/psmith/src/lib/myfile2");
		
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
	 * Test method for {@link com.arapiki.disco.model.Reports#reportTasksThatMatchName()}.
	 */
	@Test
	public void testReportTasksThatMatchName() throws Exception {
	
		int task1 = bts.addBuildTask(0, 0, "My command number 1");
		int task2 = bts.addBuildTask(0, 0, "My command number 2");
		int task3 = bts.addBuildTask(0, 0, "Another with number 1 in it");
		int task4 = bts.addBuildTask(0, 0, "A completely different task");
		int task5 = bts.addBuildTask(0, 0, "A final command with 1 in it");
	
		/* match commands that contain "command" */
		TaskSet results = reports.reportTasksThatMatchName("%command%");
		CommonTestUtils.treeSetEqual(results, new Integer[] {task1, task2, task5});
		
		/* match commands with the number 1 in them */
		results = reports.reportTasksThatMatchName("%1%");
		CommonTestUtils.treeSetEqual(results, new Integer[] {task1, task3, task5});
		
		/* match command that start with the letter A */
		results = reports.reportTasksThatMatchName("A%");
		CommonTestUtils.treeSetEqual(results, new Integer[] {task3, task4, task5});
		
		/* match commands containing the word "task" */
		results = reports.reportTasksThatMatchName("%task%");
		CommonTestUtils.treeSetEqual(results, new Integer[] {task4});
		
		/* match commands containing "elephant" */
		results = reports.reportTasksThatMatchName("%elephant%");
		CommonTestUtils.treeSetEqual(results, new Integer[] {});
		
		/* test with the empty pattern */
		results = reports.reportTasksThatMatchName("");
		CommonTestUtils.treeSetEqual(results, new Integer[] {});		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.Reports#reportDerivedFiles()}.
	 */
	@Test
	public void testReportDerivedFiles() throws Exception {

		/*
		 * Create a bunch of files. Each of these .c files is compiled into .o file, which
		 * is archived into its own .a file. All .a files are linked into the .exe file.
		 * Note that each of cat.c, dog.c and bunny.c includes pets.h.
		 * For example, for cat.c the dependency graph is:
		 * 
		 *     pets.h ->
		 *     cat.c  -> cat.o -> cat.a -> animals.exe
		 */
		int filePetH = fns.addFile("/home/pets.h");
		int fileCatC = fns.addFile("/home/cat.c");
		int fileDogC = fns.addFile("/home/dog.c");
		int fileBunnyC = fns.addFile("/home/bunny.c");
		int fileCatO = fns.addFile("/home/cat.o");
		int fileDogO = fns.addFile("/home/dog.o");
		int fileBunnyO = fns.addFile("/home/bunny.o");
		int fileCatA = fns.addFile("/home/cat.a");
		int fileDogA = fns.addFile("/home/dog.a");
		int fileBunnyA = fns.addFile("/home/bunny.a");
		int fileAnimalsExe = fns.addFile("/home/animals.exe");
		
		/* what directory were these tasks executed in? */
		int dirHome = fns.getPath("/home");
		
		/* add all tasks underneath the root */
		int rootTask = bts.getRootTask("");
		
		/* compile cat.c -> cat.o */
		int taskCompCat = bts.addBuildTask(rootTask, dirHome, "gcc -c cat.c");
		bts.addFileAccess(taskCompCat, filePetH, OperationType.OP_READ);
		bts.addFileAccess(taskCompCat, fileCatC, OperationType.OP_READ);
		bts.addFileAccess(taskCompCat, fileCatO, OperationType.OP_WRITE);

		/* compile dog.c -> dog.o */
		int taskCompDog = bts.addBuildTask(rootTask, dirHome, "gcc -c dog.c");
		bts.addFileAccess(taskCompDog, filePetH, OperationType.OP_READ);
		bts.addFileAccess(taskCompDog, fileDogC, OperationType.OP_READ);
		bts.addFileAccess(taskCompDog, fileDogO, OperationType.OP_WRITE);
		
		/* compile bunny.c -> bunny.o */
		int taskCompBunny = bts.addBuildTask(rootTask, dirHome, "gcc -c bunny.c");
		bts.addFileAccess(taskCompBunny, filePetH, OperationType.OP_READ);
		bts.addFileAccess(taskCompBunny, fileBunnyC, OperationType.OP_READ);
		bts.addFileAccess(taskCompBunny, fileBunnyO, OperationType.OP_WRITE);
		
		/* archive cat.o -> cat.a */
		int taskArchCat = bts.addBuildTask(rootTask, dirHome, "ar c cat.a cat.o");
		bts.addFileAccess(taskArchCat, fileCatO, OperationType.OP_READ);
		bts.addFileAccess(taskArchCat, fileCatA, OperationType.OP_WRITE);

		/* archive dog.o -> dog.a */
		int taskArchDog = bts.addBuildTask(rootTask, dirHome, "ar c dog.a dog.o");
		bts.addFileAccess(taskArchDog, fileDogO, OperationType.OP_READ);
		bts.addFileAccess(taskArchDog, fileDogA, OperationType.OP_WRITE);
		
		/* archive bunny.o -> bunny.a */
		int taskArchBunny = bts.addBuildTask(rootTask, dirHome, "ar c bunny.a bunny.o");
		bts.addFileAccess(taskArchBunny, fileBunnyO, OperationType.OP_READ);
		bts.addFileAccess(taskArchBunny, fileBunnyA, OperationType.OP_WRITE);

		/* link cat.a, dog.a and bunny.a -> animals.exe */
		int taskLinkAnimals = bts.addBuildTask(rootTask, dirHome, "ln -o animals.exe cat.a dog.a bunny.a");
		bts.addFileAccess(taskLinkAnimals, fileCatA, OperationType.OP_READ);
		bts.addFileAccess(taskLinkAnimals, fileDogA, OperationType.OP_READ);
		bts.addFileAccess(taskLinkAnimals, fileBunnyA, OperationType.OP_READ);
		bts.addFileAccess(taskLinkAnimals, fileAnimalsExe, OperationType.OP_WRITE);

		/*
		 * Test directly derived relationships
		 */

		/* test empty FileSet -> empty FileSet */		
		FileSet source = new FileSet(fns);
		FileSet result = reports.reportDerivedFiles(source, false);
		assertEquals(0, result.size());
		
		/* test cat.c -> cat.o */
		source = new FileSet(fns);
		addFileRecord(source, fileCatC);
		result = reports.reportDerivedFiles(source, false);
		assertEquals(1, result.size());
		assertTrue(result.isMember(fileCatO));
		
		/* test dog.c -> dog.o */
		source = new FileSet(fns);
		addFileRecord(source, fileDogC);
		result = reports.reportDerivedFiles(source, false);
		assertEquals(1, result.size());
		assertTrue(result.isMember(fileDogO));
		
		/* test pets.h -> cat.o, dog.o, bunny.o */
		source = new FileSet(fns);
		addFileRecord(source, filePetH);
		result = reports.reportDerivedFiles(source, false);
		assertEquals(3, result.size());
		assertTrue(result.isMember(fileCatO));
		assertTrue(result.isMember(fileDogO));
		assertTrue(result.isMember(fileBunnyO));

		/* test dog.o -> dog.a */
		source = new FileSet(fns);
		addFileRecord(source, fileDogO);
		result = reports.reportDerivedFiles(source, false);
		assertEquals(1, result.size());
		assertTrue(result.isMember(fileDogA));
		
		/* test dog.a -> animals.exe */
		source = new FileSet(fns);
		addFileRecord(source, fileDogA);
		result = reports.reportDerivedFiles(source, false);
		assertEquals(1, result.size());
		assertTrue(result.isMember(fileAnimalsExe));

		/* test cat.c, dog.c -> cat.o, dog.o */
		source = new FileSet(fns);
		addFileRecord(source, fileCatC);
		addFileRecord(source, fileDogC);
		result = reports.reportDerivedFiles(source, false);
		assertEquals(2, result.size());
		assertTrue(result.isMember(fileCatO));
		assertTrue(result.isMember(fileDogO));

		/*
		 * Test indirectly derived relationships
		 */
		
		/* test cat.c -> cat.o, cat.a, animals.exe */
		source = new FileSet(fns);
		addFileRecord(source, fileCatC);
		result = reports.reportDerivedFiles(source, true);
		assertEquals(3, result.size());
		assertTrue(result.isMember(fileCatO));
		assertTrue(result.isMember(fileCatA));
		assertTrue(result.isMember(fileAnimalsExe));
		
		/* test dog.c -> dog.o, dog.a, animals.exe */
		source = new FileSet(fns);
		addFileRecord(source, fileDogC);
		result = reports.reportDerivedFiles(source, true);
		assertEquals(3, result.size());
		assertTrue(result.isMember(fileDogO));
		assertTrue(result.isMember(fileDogA));
		assertTrue(result.isMember(fileAnimalsExe));
		
		/* test bunny.o -> bunny.a, animals.exe */
		source = new FileSet(fns);
		addFileRecord(source, fileBunnyO);
		result = reports.reportDerivedFiles(source, true);
		assertEquals(2, result.size());
		assertTrue(result.isMember(fileBunnyA));
		assertTrue(result.isMember(fileAnimalsExe));

		/* test pets.h -> cat.o, dog.o, bunny.o, cat.a, dog.a, bunny.a, animals.exe */
		source = new FileSet(fns);
		addFileRecord(source, filePetH);
		result = reports.reportDerivedFiles(source, true);
		assertEquals(7, result.size());
		assertTrue(result.isMember(fileCatO));
		assertTrue(result.isMember(fileDogO));
		assertTrue(result.isMember(fileBunnyO));
		assertTrue(result.isMember(fileCatA));
		assertTrue(result.isMember(fileDogA));
		assertTrue(result.isMember(fileBunnyA));
		assertTrue(result.isMember(fileAnimalsExe));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.Reports#reportTasksThatAccessFiles()}.
	 */
	@Test
	public void testReportTasksThatAccessFiles() throws Exception {

		/* Create a bunch of files */
		int file1a = fns.addFile("/file1a");
		int file1b = fns.addFile("/file1b");
		int file2a = fns.addFile("/file2a");
		int file2b = fns.addFile("/file2b");
		int file3a = fns.addFile("/file3a");
		int file3b = fns.addFile("/file3b");

		int rootTask = bts.getRootTask("");
		
		/* task 1 reads/writes file1a and file1b */
		int task1a = bts.addBuildTask(rootTask, 0, "task1 command reads file1a");
		int task1b = bts.addBuildTask(rootTask, 0, "task1 command writes file1b");
		bts.addFileAccess(task1a, file1a, OperationType.OP_READ);
		bts.addFileAccess(task1b, file1b, OperationType.OP_WRITE);

		/* task 2 reads/writes file2a and file2b */
		int task2a = bts.addBuildTask(rootTask, 0, "task2 command reads file2a");
		int task2b = bts.addBuildTask(rootTask, 0, "task2 command writes file2b");
		bts.addFileAccess(task2a, file2a, OperationType.OP_READ);
		bts.addFileAccess(task2b, file2b, OperationType.OP_WRITE);

		/* task 3 reads/writes file3a and file3b */
		int task3a = bts.addBuildTask(rootTask, 0, "task3 command reads file3a");
		int task3b = bts.addBuildTask(rootTask, 0, "task3 command writes file3b");
		bts.addFileAccess(task3a, file3a, OperationType.OP_READ);
		bts.addFileAccess(task3b, file3b, OperationType.OP_WRITE);

		/* test the report for an empty FileSet */
		FileSet source = new FileSet(fns);
		TaskSet resultReads = reports.reportTasksThatAccessFiles(source, OperationType.OP_READ);
		TaskSet resultWrites = reports.reportTasksThatAccessFiles(source, OperationType.OP_WRITE);
		TaskSet resultUses = reports.reportTasksThatAccessFiles(source, OperationType.OP_UNSPECIFIED);
		assertEquals(0, resultReads.size());
		assertEquals(0, resultWrites.size());
		assertEquals(0, resultUses.size());
		
		/* test with only file1a */
		source = new FileSet(fns);
		addFileRecord(source, file1a);
		resultReads = reports.reportTasksThatAccessFiles(source, OperationType.OP_READ);
		resultWrites = reports.reportTasksThatAccessFiles(source, OperationType.OP_WRITE);
		resultUses = reports.reportTasksThatAccessFiles(source, OperationType.OP_UNSPECIFIED);
		assertEquals(1, resultReads.size());
		assertTrue(resultReads.isMember(task1a));
		assertEquals(0, resultWrites.size());
		assertEquals(1, resultUses.size());
		assertTrue(resultUses.isMember(task1a));

		/* test with only file1b */
		source = new FileSet(fns);
		addFileRecord(source, file1b);
		resultReads = reports.reportTasksThatAccessFiles(source, OperationType.OP_READ);
		resultWrites = reports.reportTasksThatAccessFiles(source, OperationType.OP_WRITE);
		resultUses = reports.reportTasksThatAccessFiles(source, OperationType.OP_UNSPECIFIED);
		assertEquals(0, resultReads.size());
		assertEquals(1, resultWrites.size());
		assertTrue(resultWrites.isMember(task1b));
		assertEquals(1, resultUses.size());
		assertTrue(resultUses.isMember(task1b));
		
		/* test with both file1a and file1b */
		source = new FileSet(fns);
		addFileRecord(source, file1a);
		addFileRecord(source, file1b);
		resultReads = reports.reportTasksThatAccessFiles(source, OperationType.OP_READ);
		resultWrites = reports.reportTasksThatAccessFiles(source, OperationType.OP_WRITE);
		resultUses = reports.reportTasksThatAccessFiles(source, OperationType.OP_UNSPECIFIED);
		assertEquals(1, resultReads.size());
		assertTrue(resultReads.isMember(task1a));
		assertEquals(1, resultWrites.size());
		assertTrue(resultWrites.isMember(task1b));
		assertEquals(2, resultUses.size());
		assertTrue(resultUses.isMember(task1a));
		assertTrue(resultUses.isMember(task1b));
		
		/* test with file1a and file2a */
		source = new FileSet(fns);
		addFileRecord(source, file1a);
		addFileRecord(source, file2a);
		resultReads = reports.reportTasksThatAccessFiles(source, OperationType.OP_READ);
		resultWrites = reports.reportTasksThatAccessFiles(source, OperationType.OP_WRITE);
		resultUses = reports.reportTasksThatAccessFiles(source, OperationType.OP_UNSPECIFIED);
		assertEquals(2, resultReads.size());
		assertTrue(resultReads.isMember(task1a));
		assertTrue(resultReads.isMember(task2a));
		assertEquals(0, resultWrites.size());
		assertEquals(2, resultUses.size());
		assertTrue(resultUses.isMember(task1a));
		assertTrue(resultUses.isMember(task2a));

		/* test with file1a, file2a and file3b */
		source = new FileSet(fns);
		addFileRecord(source, file1a);
		addFileRecord(source, file2a);
		addFileRecord(source, file3b);
		resultReads = reports.reportTasksThatAccessFiles(source, OperationType.OP_READ);
		resultWrites = reports.reportTasksThatAccessFiles(source, OperationType.OP_WRITE);
		resultUses = reports.reportTasksThatAccessFiles(source, OperationType.OP_UNSPECIFIED);
		assertEquals(2, resultReads.size());
		assertTrue(resultReads.isMember(task1a));
		assertTrue(resultReads.isMember(task2a));
		assertEquals(1, resultWrites.size());
		assertTrue(resultWrites.isMember(task3b));
		assertEquals(3, resultUses.size());
		assertTrue(resultUses.isMember(task1a));
		assertTrue(resultUses.isMember(task2a));
		assertTrue(resultUses.isMember(task3b));
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for {@link com.arapiki.disco.model.Reports#reportFilesAccessedByTasks()}.
	 */
	@Test
	public void testFilesAccessedByTasks() throws Exception {
		
		/* create some tasks, and some file accesses for each */
		int file1 = fns.addFile("/a/b/c.java");
		int file2 = fns.addFile("/a/b/d.java");
		int file3 = fns.addFile("/a/b/e.java");

		int root = bts.getRootTask("");
		int task1 = bts.addBuildTask(root, 0, "");
		bts.addFileAccess(task1, file1, OperationType.OP_WRITE);
		bts.addFileAccess(task1, file2, OperationType.OP_READ);

		int task2 = bts.addBuildTask(root, 0, "");
		bts.addFileAccess(task2, file1, OperationType.OP_READ);
		bts.addFileAccess(task2, file3, OperationType.OP_READ);

		int task3 = bts.addBuildTask(root, 0, "");
		bts.addFileAccess(task3, file3, OperationType.OP_WRITE);
		bts.addFileAccess(task3, file3, OperationType.OP_WRITE);

		/* test with the empty TaskSet - should be no files returned */
		TaskSet ts = new TaskSet(bts);
		FileSet result = reports.reportFilesAccessedByTasks(ts, OperationType.OP_UNSPECIFIED);
		assertEquals(0, result.size());
		
		/* test with a single task, looking for all accessed files */
		ts.add(new TaskRecord(task1));
		result = reports.reportFilesAccessedByTasks(ts, OperationType.OP_UNSPECIFIED);
		assertEquals(2, result.size());
		assertTrue(result.isMember(file1));
		assertTrue(result.isMember(file2));
		
		/* test with a single task, looking for all read files */
		result = reports.reportFilesAccessedByTasks(ts, OperationType.OP_READ);
		assertEquals(1, result.size());
		assertTrue(result.isMember(file2));

		/* test with a single task, looking for all written files */
		result = reports.reportFilesAccessedByTasks(ts, OperationType.OP_WRITE);
		assertEquals(1, result.size());
		assertTrue(result.isMember(file1));

		/* test with two tasks, looking for all accessed files */
		ts.add(new TaskRecord(task2));
		result = reports.reportFilesAccessedByTasks(ts, OperationType.OP_UNSPECIFIED);
		assertEquals(3, result.size());
		assertTrue(result.isMember(file1));
		assertTrue(result.isMember(file2));
		assertTrue(result.isMember(file3));
		
		/* test with two tasks, looking for all read files */
		result = reports.reportFilesAccessedByTasks(ts, OperationType.OP_READ);
		assertEquals(3, result.size());
		assertTrue(result.isMember(file1));
		assertTrue(result.isMember(file2));
		assertTrue(result.isMember(file3));

		/* test with two tasks, looking for all written files */
		result = reports.reportFilesAccessedByTasks(ts, OperationType.OP_WRITE);
		assertEquals(1, result.size());
		assertTrue(result.isMember(file1));

		/* test with three tasks, looking for all written files */
		ts.add(new TaskRecord(task3));
		result = reports.reportFilesAccessedByTasks(ts, OperationType.OP_WRITE);
		assertEquals(2, result.size());
		assertTrue(result.isMember(file1));
		assertTrue(result.isMember(file3));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.Reports#reportWriteOnlyFiles()}.
	 */
	@Test
	public void testWriteOnlyFiles() throws Exception {
	
		/* 
		 * Create a bunch of files, with different characteristics.
		 * But first, run the report - should be empty.
		 */
		FileSet result = reports.reportWriteOnlyFiles();
		assertEquals(0, result.size());
		
		/* a.java will be compiled into a.class, which goes into prog.jar (see later) */
		int fileAJava = fns.addFile("/a.java");
		int fileAClass = fns.addFile("/a.class");
		int taskA = bts.addBuildTask(0, 0, "javac a.java");
		bts.addFileAccess(taskA, fileAJava, OperationType.OP_READ);
		bts.addFileAccess(taskA, fileAClass, OperationType.OP_WRITE);

		/* run the report - should show a.class */
		result = reports.reportWriteOnlyFiles();
		assertEquals(1, result.size());
		assertTrue(result.isMember(fileAClass));		
		
		/* b.java will be compiled into b.class, which goes into prog.jar (see later) */		
		int fileBJava = fns.addFile("/b.java");
		int fileBClass = fns.addFile("/b.class");
		int taskB = bts.addBuildTask(0, 0, "javac b.java");
		bts.addFileAccess(taskB, fileBJava, OperationType.OP_READ);
		bts.addFileAccess(taskB, fileBClass, OperationType.OP_WRITE);
		
		/* Run the report - a.class and b.class should be listed */
		result = reports.reportWriteOnlyFiles();
		assertEquals(2, result.size());
		assertTrue(result.isMember(fileAClass));
		assertTrue(result.isMember(fileBClass));
		
		/* c.java will be compiled into c.class, but no further */
		int fileCJava = fns.addFile("/c.java");
		int fileCClass = fns.addFile("/c.class");
		int taskC = bts.addBuildTask(0, 0, "javac c.java");
		bts.addFileAccess(taskC, fileCJava, OperationType.OP_READ);
		bts.addFileAccess(taskC, fileCClass, OperationType.OP_WRITE);
		
		/* d.java is not read at all */
		int fileDJava = fns.addFile("/d.java");		

		/* Run the report - a.class, b.class and c.class should be listed */
		result = reports.reportWriteOnlyFiles();
		assertEquals(3, result.size());
		assertTrue(result.isMember(fileAClass));
		assertTrue(result.isMember(fileBClass));
		assertTrue(result.isMember(fileCClass));
		
		/* now put A.class and B.class into prog.jar */
		int fileProgJar = fns.addFile("/prog.jar");
		int taskProg = bts.addBuildTask(0, 0, "jar cf prog.jar a.class b.class");
		bts.addFileAccess(taskProg, fileAClass, OperationType.OP_READ);
		bts.addFileAccess(taskProg, fileBClass, OperationType.OP_READ);
		bts.addFileAccess(taskProg, fileProgJar, OperationType.OP_WRITE);
	
		/* Run the report - only c.class and prog.jar should be listed */
		result = reports.reportWriteOnlyFiles();
		assertEquals(2, result.size());
		assertTrue(result.isMember(fileCClass));
		assertTrue(result.isMember(fileProgJar));
	}
	
	/*-------------------------------------------------------------------------------------*/		
}