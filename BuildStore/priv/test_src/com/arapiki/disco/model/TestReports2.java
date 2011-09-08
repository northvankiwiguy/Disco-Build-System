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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.arapiki.disco.model.BuildTasks.OperationType;

/**
 * The class contains test cases for some of the methods in the Report class
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestReports2 {

	/* our BuildStore, and sub-objects, used for testing */
	private BuildStore bs;
	private FileNameSpaces fns;
	private BuildTasks bts;
	
	/* our Reports object, used for testing */
	private Reports reports;
	
	/* various file IDs and task IDs */
	private int filePetH, fileHousePetH;
	private int fileCatC, fileDogC, fileBunnyC, fileGiraffeC;
	private int fileCatO, fileDogO, fileBunnyO, fileGiraffeO;
	private int fileCatA, fileDogA, fileBunnyA, fileGiraffeA;
	private int fileAnimalsExe;
	
	/**
	 * Set up method for all test cases. This sets up a number of files, as well as
	 * build tasks that access the files (including file-access records). Each test
	 * case in this file uses the same basic file/task relationship in its test.
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		/* get all the objects we need to set up the test scenario */
		bs = CommonTestUtils.getEmptyBuildStore();
		fns = bs.getFileNameSpaces();
		bts = bs.getBuildTasks();
		reports = bs.getReports();
		int rootTaskId = bts.getRootTask("root");
		
		/* add a realistic-looking set of files, including .h, .c, .o, .a and .exe files */
		filePetH = fns.addFile("/home/pets.h");
		fileHousePetH = fns.addFile("/home/house-pets.h");
		fileCatC = fns.addFile("/home/cat.c");
		fileDogC = fns.addFile("/home/dog.c");
		fileBunnyC = fns.addFile("/home/bunny.c");
		fileGiraffeC = fns.addFile("/home/giraffe.c");
		fileCatO = fns.addFile("/home/cat.o");
		fileDogO = fns.addFile("/home/dog.o");
		fileBunnyO = fns.addFile("/home/bunny.o");
		fileGiraffeO = fns.addFile("/home/giraffe.o");
		fileCatA = fns.addFile("/home/cat.a");
		fileDogA = fns.addFile("/home/dog.a");
		fileBunnyA = fns.addFile("/home/bunny.a");
		fileGiraffeA = fns.addFile("/home/giraffe.a");
		fileAnimalsExe = fns.addFile("/home/animals.exe");
		
		/* what directory were these tasks executed in? */
		int dirHome = fns.getPath("/home");
		
		/* add all tasks underneath the root */
		int rootTask = bts.getRootTask("");
		
		/* compile cat.c -> cat.o */
		int taskCompCat = bts.addBuildTask(rootTask, dirHome, "gcc -c cat.c");
		bts.addFileAccess(taskCompCat, filePetH, OperationType.OP_READ);
		bts.addFileAccess(taskCompCat, fileHousePetH, OperationType.OP_READ);
		bts.addFileAccess(taskCompCat, fileCatC, OperationType.OP_READ);
		bts.addFileAccess(taskCompCat, fileCatO, OperationType.OP_WRITE);

		/* compile dog.c -> dog.o */
		int taskCompDog = bts.addBuildTask(rootTask, dirHome, "gcc -c dog.c");
		bts.addFileAccess(taskCompDog, filePetH, OperationType.OP_READ);
		bts.addFileAccess(taskCompDog, fileHousePetH, OperationType.OP_READ);
		bts.addFileAccess(taskCompDog, fileDogC, OperationType.OP_READ);
		bts.addFileAccess(taskCompDog, fileDogO, OperationType.OP_WRITE);
		
		/* compile bunny.c -> bunny.o */
		int taskCompBunny = bts.addBuildTask(rootTask, dirHome, "gcc -c bunny.c");
		bts.addFileAccess(taskCompBunny, filePetH, OperationType.OP_READ);
		bts.addFileAccess(taskCompBunny, fileHousePetH, OperationType.OP_READ);
		bts.addFileAccess(taskCompBunny, fileBunnyC, OperationType.OP_READ);
		bts.addFileAccess(taskCompBunny, fileBunnyO, OperationType.OP_WRITE);
		
		/* compile giraffe.c -> giraffe.o */
		int taskCompGiraffe = bts.addBuildTask(rootTask, dirHome, "gcc -c giraffe.c");
		bts.addFileAccess(taskCompGiraffe, filePetH, OperationType.OP_READ);
		bts.addFileAccess(taskCompGiraffe, fileGiraffeC, OperationType.OP_READ);
		bts.addFileAccess(taskCompGiraffe, fileGiraffeO, OperationType.OP_WRITE);
		
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
		
		/* archive giraffe.o -> giraffe.a */
		int taskArchGiraffe = bts.addBuildTask(rootTask, dirHome, "ar c giraffe.a giraffe.o");
		bts.addFileAccess(taskArchGiraffe, fileGiraffeO, OperationType.OP_READ);
		bts.addFileAccess(taskArchGiraffe, fileGiraffeA, OperationType.OP_WRITE);

		/* link cat.a, dog.a, giraffe.a and bunny.a -> animals.exe */
		int taskLinkAnimals = bts.addBuildTask(rootTask, dirHome, "ln -o animals.exe cat.a dog.a bunny.a giraffe.a");
		bts.addFileAccess(taskLinkAnimals, fileCatA, OperationType.OP_READ);
		bts.addFileAccess(taskLinkAnimals, fileDogA, OperationType.OP_READ);
		bts.addFileAccess(taskLinkAnimals, fileBunnyA, OperationType.OP_READ);
		bts.addFileAccess(taskLinkAnimals, fileGiraffeA, OperationType.OP_READ);
		bts.addFileAccess(taskLinkAnimals, fileAnimalsExe, OperationType.OP_WRITE);
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
	 * Test method for {@link com.arapiki.disco.model.Reports#reportDerivedFiles()}.
	 */
	@Test
	public void testReportDerivedFiles() throws Exception {

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

		/* test pets.h -> cat.o, dog.o, bunny.o, giraffe.o */
		source = new FileSet(fns);
		addFileRecord(source, filePetH);
		result = reports.reportDerivedFiles(source, false);
		assertEquals(4, result.size());
		assertTrue(result.isMember(fileCatO));
		assertTrue(result.isMember(fileDogO));
		assertTrue(result.isMember(fileBunnyO));
		assertTrue(result.isMember(fileGiraffeO));
		
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

		/* 
		 * Test pets.h -> cat.o, dog.o, bunny.o, giraffe.o, cat.a, 
		 * dog.a, bunny.a, giraffe.a, animals.exe
		 */
		source = new FileSet(fns);
		addFileRecord(source, filePetH);
		result = reports.reportDerivedFiles(source, true);
		assertTrue(CommonTestUtils.treeSetEqual(result, 
				new Integer[] {fileCatO, fileDogO, fileBunnyO, fileGiraffeO, fileCatA, fileDogA, 
				fileBunnyA, fileGiraffeA, fileAnimalsExe}));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.Reports#reportInputFiles()}.
	 */
	@Test
	public void testReportInputFiles() throws Exception {

		/*
		 * Test directly input relationships
		 */

		/* test empty FileSet -> empty FileSet */
		FileSet dest = new FileSet(fns);
		FileSet result = reports.reportInputFiles(dest, false);
		assertEquals(0, result.size());
		
		/* test {house-pet.h, pet.h, cat.c} <- cat.o */
		dest = new FileSet(fns);
		addFileRecord(dest, fileCatO);
		result = reports.reportInputFiles(dest, false);
		assertEquals(3, result.size());
		assertTrue(result.isMember(fileCatC));
		assertTrue(result.isMember(filePetH));
		assertTrue(result.isMember(fileHousePetH));
		
		/* test {house-pet.h, pet.h, dog.c} <- dog.o */
		dest = new FileSet(fns);
		addFileRecord(dest, fileDogO);
		result = reports.reportInputFiles(dest, false);
		assertEquals(3, result.size());
		assertTrue(result.isMember(fileDogC));
		assertTrue(result.isMember(filePetH));
		assertTrue(result.isMember(fileHousePetH));
		
		/* test {pet.h, giraffe.c} <- giraffe.o */
		dest = new FileSet(fns);
		addFileRecord(dest, fileGiraffeO);
		result = reports.reportInputFiles(dest, false);
		assertEquals(2, result.size());
		assertTrue(result.isMember(fileGiraffeC));
		assertTrue(result.isMember(filePetH));

		/* test {} <- pets.h */
		dest = new FileSet(fns);
		addFileRecord(dest, filePetH);
		result = reports.reportInputFiles(dest, false);
		assertEquals(0, result.size());

		/* test dog.o <- dog.a */
		dest = new FileSet(fns);
		addFileRecord(dest, fileDogA);
		result = reports.reportInputFiles(dest, false);
		assertEquals(1, result.size());
		assertTrue(result.isMember(fileDogO));
		
		/* test dog.a, cat.a, bunny.a, giraffe.a <- animals.exe */
		dest = new FileSet(fns);
		addFileRecord(dest, fileAnimalsExe);
		result = reports.reportInputFiles(dest, false);
		assertEquals(4, result.size());
		assertTrue(result.isMember(fileDogA));
		assertTrue(result.isMember(fileCatA));
		assertTrue(result.isMember(fileBunnyA));
		assertTrue(result.isMember(fileGiraffeA));

		/* test cat.c, dog.c, pets.h, house-pets.h <- cat.o, dog.o */
		dest = new FileSet(fns);
		addFileRecord(dest, fileCatO);
		addFileRecord(dest, fileDogO);
		result = reports.reportInputFiles(dest, false);
		assertEquals(4, result.size());
		assertTrue(result.isMember(fileCatC));
		assertTrue(result.isMember(fileDogC));
		assertTrue(result.isMember(filePetH));
		assertTrue(result.isMember(fileHousePetH));

		/*
		 * Test indirectly input relationships
		 */
				
		/* test house-pets.h, pets.h, bunny.c, bunny.o <- bunny.a */
		dest = new FileSet(fns);
		addFileRecord(dest, fileBunnyA);
		result = reports.reportInputFiles(dest, true);
		assertEquals(4, result.size());
		assertTrue(result.isMember(filePetH));
		assertTrue(result.isMember(fileBunnyC));
		assertTrue(result.isMember(fileBunnyO));
		assertTrue(result.isMember(fileHousePetH));
		
		/* test pets.h, house-pets.h, *.c, *.o, *.a <- animals.exe */
		dest = new FileSet(fns);
		addFileRecord(dest, fileAnimalsExe);
		result = reports.reportInputFiles(dest, true);
		assertEquals(14, result.size());
	}
	
	/*-------------------------------------------------------------------------------------*/
}