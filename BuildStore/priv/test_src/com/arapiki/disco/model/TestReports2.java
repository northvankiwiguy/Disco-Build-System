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

import org.junit.Before;
import org.junit.Test;

import com.arapiki.disco.model.BuildTasks.OperationType;
import com.arapiki.disco.model.types.ComponentSet;
import com.arapiki.disco.model.types.FileSet;

/**
 * The class contains test cases for some of the methods in the Report class
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestReports2 {

	/** Our test BuildStore object */
	private BuildStore bs;

	/** Our test FileNameSpaces object */
	private FileNameSpaces fns;
	
	/** Our test BuildTasks object */
	private BuildTasks bts;
	
	/** Our test Reports object */
	private Reports reports;
	
	/** various file IDs and task IDs */
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
	 * Test method for {@link com.arapiki.disco.model.Reports#reportDerivedFiles(FileSet, boolean)}.
	 */
	@Test
	public void testReportDerivedFiles() {

		/*
		 * Test directly derived relationships
		 */

		/* test empty FileSet -> empty FileSet */		
		FileSet source = new FileSet(fns);
		FileSet result = reports.reportDerivedFiles(source, false);
		assertEquals(0, result.size());
		
		/* test cat.c -> cat.o */
		source = new FileSet(fns);
		source.add(fileCatC);
		result = reports.reportDerivedFiles(source, false);
		assertEquals(1, result.size());
		assertTrue(result.isMember(fileCatO));
				
		/* test dog.c -> dog.o */
		source = new FileSet(fns);
		source.add(fileDogC);
		result = reports.reportDerivedFiles(source, false);
		assertEquals(1, result.size());
		assertTrue(result.isMember(fileDogO));

		/* test pets.h -> cat.o, dog.o, bunny.o, giraffe.o */
		source = new FileSet(fns);
		source.add(filePetH);
		result = reports.reportDerivedFiles(source, false);
		assertEquals(4, result.size());
		assertTrue(result.isMember(fileCatO));
		assertTrue(result.isMember(fileDogO));
		assertTrue(result.isMember(fileBunnyO));
		assertTrue(result.isMember(fileGiraffeO));
		
		/* test dog.o -> dog.a */
		source = new FileSet(fns);
		source.add(fileDogO);
		result = reports.reportDerivedFiles(source, false);
		assertEquals(1, result.size());
		assertTrue(result.isMember(fileDogA));
				
		/* test dog.a -> animals.exe */
		source = new FileSet(fns);
		source.add(fileDogA);
		result = reports.reportDerivedFiles(source, false);
		assertEquals(1, result.size());
		assertTrue(result.isMember(fileAnimalsExe));
		
		/* test cat.c, dog.c -> cat.o, dog.o */
		source = new FileSet(fns);
		source.add(fileCatC);
		source.add(fileDogC);
		result = reports.reportDerivedFiles(source, false);
		assertEquals(2, result.size());
		assertTrue(result.isMember(fileCatO));
		assertTrue(result.isMember(fileDogO));

		/*
		 * Test indirectly derived relationships
		 */
		
		/* test cat.c -> cat.o, cat.a, animals.exe */
		source = new FileSet(fns);
		source.add(fileCatC);
		result = reports.reportDerivedFiles(source, true);
		assertEquals(3, result.size());
		assertTrue(result.isMember(fileCatO));
		assertTrue(result.isMember(fileCatA));
		assertTrue(result.isMember(fileAnimalsExe));
		
		/* test dog.c -> dog.o, dog.a, animals.exe */
		source = new FileSet(fns);
		source.add(fileDogC);
		result = reports.reportDerivedFiles(source, true);
		assertEquals(3, result.size());
		assertTrue(result.isMember(fileDogO));
		assertTrue(result.isMember(fileDogA));
		assertTrue(result.isMember(fileAnimalsExe));
		
		/* test bunny.o -> bunny.a, animals.exe */
		source = new FileSet(fns);
		source.add(fileBunnyO);
		result = reports.reportDerivedFiles(source, true);
		assertEquals(2, result.size());
		assertTrue(result.isMember(fileBunnyA));
		assertTrue(result.isMember(fileAnimalsExe));

		/* 
		 * Test pets.h -> cat.o, dog.o, bunny.o, giraffe.o, cat.a, 
		 * dog.a, bunny.a, giraffe.a, animals.exe
		 */
		source = new FileSet(fns);
		source.add(filePetH);
		result = reports.reportDerivedFiles(source, true);
		assertTrue(CommonTestUtils.treeSetEqual(result, 
				new Integer[] {fileCatO, fileDogO, fileBunnyO, fileGiraffeO, fileCatA, fileDogA, 
				fileBunnyA, fileGiraffeA, fileAnimalsExe}));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.Reports#reportInputFiles(FileSet, boolean)}.
	 */
	@Test
	public void testReportInputFiles() {

		/*
		 * Test directly input relationships
		 */

		/* test empty FileSet -> empty FileSet */
		FileSet dest = new FileSet(fns);
		FileSet result = reports.reportInputFiles(dest, false);
		assertEquals(0, result.size());
		
		/* test {house-pet.h, pet.h, cat.c} <- cat.o */
		dest = new FileSet(fns);
		dest.add(fileCatO);
		result = reports.reportInputFiles(dest, false);
		assertEquals(3, result.size());
		assertTrue(result.isMember(fileCatC));
		assertTrue(result.isMember(filePetH));
		assertTrue(result.isMember(fileHousePetH));
		
		/* test {house-pet.h, pet.h, dog.c} <- dog.o */
		dest = new FileSet(fns);
		dest.add(fileDogO);
		result = reports.reportInputFiles(dest, false);
		assertEquals(3, result.size());
		assertTrue(result.isMember(fileDogC));
		assertTrue(result.isMember(filePetH));
		assertTrue(result.isMember(fileHousePetH));
		
		/* test {pet.h, giraffe.c} <- giraffe.o */
		dest = new FileSet(fns);
		dest.add(fileGiraffeO);
		result = reports.reportInputFiles(dest, false);
		assertEquals(2, result.size());
		assertTrue(result.isMember(fileGiraffeC));
		assertTrue(result.isMember(filePetH));

		/* test {} <- pets.h */
		dest = new FileSet(fns);
		dest.add(filePetH);
		result = reports.reportInputFiles(dest, false);
		assertEquals(0, result.size());

		/* test dog.o <- dog.a */
		dest = new FileSet(fns);
		dest.add(fileDogA);
		result = reports.reportInputFiles(dest, false);
		assertEquals(1, result.size());
		assertTrue(result.isMember(fileDogO));
		
		/* test dog.a, cat.a, bunny.a, giraffe.a <- animals.exe */
		dest = new FileSet(fns);
		dest.add(fileAnimalsExe);
		result = reports.reportInputFiles(dest, false);
		assertEquals(4, result.size());
		assertTrue(result.isMember(fileDogA));
		assertTrue(result.isMember(fileCatA));
		assertTrue(result.isMember(fileBunnyA));
		assertTrue(result.isMember(fileGiraffeA));

		/* test cat.c, dog.c, pets.h, house-pets.h <- cat.o, dog.o */
		dest = new FileSet(fns);
		dest.add(fileCatO);
		dest.add(fileDogO);
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
		dest.add(fileBunnyA);
		result = reports.reportInputFiles(dest, true);
		assertEquals(4, result.size());
		assertTrue(result.isMember(filePetH));
		assertTrue(result.isMember(fileBunnyC));
		assertTrue(result.isMember(fileBunnyO));
		assertTrue(result.isMember(fileHousePetH));
		
		/* test pets.h, house-pets.h, *.c, *.o, *.a <- animals.exe */
		dest = new FileSet(fns);
		dest.add(fileAnimalsExe);
		result = reports.reportInputFiles(dest, true);
		assertEquals(14, result.size());
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.Reports#reportFilesFromComponentSet}.
	 */
	@Test
	public void testReportFilesFromComponentSet() {
		
		/* create a new component set and add some components */
		ComponentSet cs = new ComponentSet(bs);
		Components comps = bs.getComponents();
		int comp1Id = comps.addComponent("Comp1");
		int comp2Id = comps.addComponent("Comp2");		
		int comp3Id = comps.addComponent("Comp3");
		int comp4Id = comps.addComponent("Comp4");

		/* initially, no paths should be present (not even /) */
		FileSet fs = reports.reportFilesFromComponentSet(cs);
		assertEquals(0, fs.size());
		
		/* add a bunch of files - they should all be in the None component */
		int file1 = fns.addFile("/file1");
		int file2 = fns.addFile("/file2");
		int file3 = fns.addFile("/file3");
		int file4 = fns.addFile("/file4");
		int file5 = fns.addFile("/file5");
		int file6 = fns.addFile("/file6");
		int file7 = fns.addFile("/file7");
		int file8 = fns.addFile("/file8");
		
		/* empty component set still gives an empty FileSet */
		fs = reports.reportFilesFromComponentSet(cs);
		assertEquals(0, fs.size());
		
		/* map the files into components */
		comps.setFileComponent(file1, comp1Id, Components.SCOPE_PRIVATE);
		comps.setFileComponent(file2, comp1Id, Components.SCOPE_PUBLIC);
		comps.setFileComponent(file3, comp2Id, Components.SCOPE_PRIVATE);
		comps.setFileComponent(file4, comp2Id, Components.SCOPE_PUBLIC);
		comps.setFileComponent(file5, comp3Id, Components.SCOPE_PRIVATE);
		comps.setFileComponent(file6, comp3Id, Components.SCOPE_PUBLIC);
		comps.setFileComponent(file7, comp4Id, Components.SCOPE_PRIVATE);
		comps.setFileComponent(file8, comp4Id, Components.SCOPE_PUBLIC);
	
		/* empty component set still gives an empty FileSet */
		fs = reports.reportFilesFromComponentSet(cs);
		assertEquals(0, fs.size());
		
		/* add Comp1/Private into the component set */
		cs.add(comp1Id, Components.SCOPE_PRIVATE);
		fs = reports.reportFilesFromComponentSet(cs);
		assertEquals(1, fs.size());
		assertTrue(fs.isMember(file1));

		/* add Comp1/Public into the component set */
		cs.add(comp1Id, Components.SCOPE_PUBLIC);
		fs = reports.reportFilesFromComponentSet(cs);
		assertEquals(2, fs.size());
		assertTrue(fs.isMember(file1));
		assertTrue(fs.isMember(file2));

		/* add Comp2 into the component set */
		cs.add(comp2Id);
		fs = reports.reportFilesFromComponentSet(cs);
		assertEquals(4, fs.size());
		assertTrue(fs.isMember(file1));
		assertTrue(fs.isMember(file2));
		assertTrue(fs.isMember(file3));
		assertTrue(fs.isMember(file4));
		
		/* add Comp8/Public into the component set */
		cs.add(comp4Id, Components.SCOPE_PUBLIC);
		fs = reports.reportFilesFromComponentSet(cs);
		assertEquals(5, fs.size());
		assertTrue(fs.isMember(file1));
		assertTrue(fs.isMember(file2));
		assertTrue(fs.isMember(file3));
		assertTrue(fs.isMember(file4));
		assertTrue(fs.isMember(file8));
		
		/* add a few hundred components, and check scalability */
		for (int i = 5; i != 200; i++) {
			int id = comps.addComponent("Comp" + i);
			cs.add(id, 1 + (i % 2));
		}
		
		/* the simple fact that this doesn't crash is enough to pass the test */
		fs = reports.reportFilesFromComponentSet(cs);	
	}
	
	/*-------------------------------------------------------------------------------------*/
}
