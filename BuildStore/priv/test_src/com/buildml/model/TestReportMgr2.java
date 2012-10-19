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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.buildml.model.IReportMgr;
import com.buildml.model.impl.ActionMgr.OperationType;
import com.buildml.model.types.PackageSet;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.TaskSet;

/**
 * The class contains test cases for some of the methods in the Report class
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestReportMgr2 {

	/** Our test BuildStore object */
	private IBuildStore bs;

	/** Our test FileMgr object */
	private IFileMgr fileMgr;
	
	/** Our test ActionMgr object */
	private IActionMgr actionMgr;
	
	/** Our test Reports object */
	private IReportMgr reports;
	
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
		fileMgr = bs.getFileMgr();
		actionMgr = bs.getActionMgr();
		reports = bs.getReportMgr();
		
		/* add a realistic-looking set of files, including .h, .c, .o, .a and .exe files */
		filePetH = fileMgr.addFile("/home/pets.h");
		fileHousePetH = fileMgr.addFile("/home/house-pets.h");
		fileCatC = fileMgr.addFile("/home/cat.c");
		fileDogC = fileMgr.addFile("/home/dog.c");
		fileBunnyC = fileMgr.addFile("/home/bunny.c");
		fileGiraffeC = fileMgr.addFile("/home/giraffe.c");
		fileCatO = fileMgr.addFile("/home/cat.o");
		fileDogO = fileMgr.addFile("/home/dog.o");
		fileBunnyO = fileMgr.addFile("/home/bunny.o");
		fileGiraffeO = fileMgr.addFile("/home/giraffe.o");
		fileCatA = fileMgr.addFile("/home/cat.a");
		fileDogA = fileMgr.addFile("/home/dog.a");
		fileBunnyA = fileMgr.addFile("/home/bunny.a");
		fileGiraffeA = fileMgr.addFile("/home/giraffe.a");
		fileAnimalsExe = fileMgr.addFile("/home/animals.exe");
		
		/* what directory were these tasks executed in? */
		int dirHome = fileMgr.getPath("/home");
		
		/* add all tasks underneath the root */
		int rootTask = actionMgr.getRootTask("");
		
		/* compile cat.c -> cat.o */
		int taskPkgCat = actionMgr.addBuildTask(rootTask, dirHome, "gcc -c cat.c");
		actionMgr.addFileAccess(taskPkgCat, filePetH, OperationType.OP_READ);
		actionMgr.addFileAccess(taskPkgCat, fileHousePetH, OperationType.OP_READ);
		actionMgr.addFileAccess(taskPkgCat, fileCatC, OperationType.OP_READ);
		actionMgr.addFileAccess(taskPkgCat, fileCatO, OperationType.OP_WRITE);

		/* compile dog.c -> dog.o */
		int taskPkgDog = actionMgr.addBuildTask(rootTask, dirHome, "gcc -c dog.c");
		actionMgr.addFileAccess(taskPkgDog, filePetH, OperationType.OP_READ);
		actionMgr.addFileAccess(taskPkgDog, fileHousePetH, OperationType.OP_READ);
		actionMgr.addFileAccess(taskPkgDog, fileDogC, OperationType.OP_READ);
		actionMgr.addFileAccess(taskPkgDog, fileDogO, OperationType.OP_WRITE);
		
		/* compile bunny.c -> bunny.o */
		int taskPkgBunny = actionMgr.addBuildTask(rootTask, dirHome, "gcc -c bunny.c");
		actionMgr.addFileAccess(taskPkgBunny, filePetH, OperationType.OP_READ);
		actionMgr.addFileAccess(taskPkgBunny, fileHousePetH, OperationType.OP_READ);
		actionMgr.addFileAccess(taskPkgBunny, fileBunnyC, OperationType.OP_READ);
		actionMgr.addFileAccess(taskPkgBunny, fileBunnyO, OperationType.OP_WRITE);
		
		/* compile giraffe.c -> giraffe.o */
		int taskPkgGiraffe = actionMgr.addBuildTask(rootTask, dirHome, "gcc -c giraffe.c");
		actionMgr.addFileAccess(taskPkgGiraffe, filePetH, OperationType.OP_READ);
		actionMgr.addFileAccess(taskPkgGiraffe, fileGiraffeC, OperationType.OP_READ);
		actionMgr.addFileAccess(taskPkgGiraffe, fileGiraffeO, OperationType.OP_WRITE);
		
		/* archive cat.o -> cat.a */
		int taskArchCat = actionMgr.addBuildTask(rootTask, dirHome, "ar c cat.a cat.o");
		actionMgr.addFileAccess(taskArchCat, fileCatO, OperationType.OP_READ);
		actionMgr.addFileAccess(taskArchCat, fileCatA, OperationType.OP_WRITE);

		/* archive dog.o -> dog.a */
		int taskArchDog = actionMgr.addBuildTask(rootTask, dirHome, "ar c dog.a dog.o");
		actionMgr.addFileAccess(taskArchDog, fileDogO, OperationType.OP_READ);
		actionMgr.addFileAccess(taskArchDog, fileDogA, OperationType.OP_WRITE);
		
		/* archive bunny.o -> bunny.a */
		int taskArchBunny = actionMgr.addBuildTask(rootTask, dirHome, "ar c bunny.a bunny.o");
		actionMgr.addFileAccess(taskArchBunny, fileBunnyO, OperationType.OP_READ);
		actionMgr.addFileAccess(taskArchBunny, fileBunnyA, OperationType.OP_WRITE);
		
		/* archive giraffe.o -> giraffe.a */
		int taskArchGiraffe = actionMgr.addBuildTask(rootTask, dirHome, "ar c giraffe.a giraffe.o");
		actionMgr.addFileAccess(taskArchGiraffe, fileGiraffeO, OperationType.OP_READ);
		actionMgr.addFileAccess(taskArchGiraffe, fileGiraffeA, OperationType.OP_WRITE);

		/* link cat.a, dog.a, giraffe.a and bunny.a -> animals.exe */
		int taskLinkAnimals = actionMgr.addBuildTask(rootTask, dirHome, "ln -o animals.exe cat.a dog.a bunny.a giraffe.a");
		actionMgr.addFileAccess(taskLinkAnimals, fileCatA, OperationType.OP_READ);
		actionMgr.addFileAccess(taskLinkAnimals, fileDogA, OperationType.OP_READ);
		actionMgr.addFileAccess(taskLinkAnimals, fileBunnyA, OperationType.OP_READ);
		actionMgr.addFileAccess(taskLinkAnimals, fileGiraffeA, OperationType.OP_READ);
		actionMgr.addFileAccess(taskLinkAnimals, fileAnimalsExe, OperationType.OP_WRITE);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IReportMgr#reportDerivedFiles(FileSet, boolean)}.
	 */
	@Test
	public void testReportDerivedFiles() {

		/*
		 * Test directly derived relationships
		 */

		/* test empty FileSet -> empty FileSet */		
		FileSet source = new FileSet(fileMgr);
		FileSet result = reports.reportDerivedFiles(source, false);
		assertEquals(0, result.size());
		
		/* test cat.c -> cat.o */
		source = new FileSet(fileMgr);
		source.add(fileCatC);
		result = reports.reportDerivedFiles(source, false);
		assertEquals(1, result.size());
		assertTrue(result.isMember(fileCatO));
				
		/* test dog.c -> dog.o */
		source = new FileSet(fileMgr);
		source.add(fileDogC);
		result = reports.reportDerivedFiles(source, false);
		assertEquals(1, result.size());
		assertTrue(result.isMember(fileDogO));

		/* test pets.h -> cat.o, dog.o, bunny.o, giraffe.o */
		source = new FileSet(fileMgr);
		source.add(filePetH);
		result = reports.reportDerivedFiles(source, false);
		assertEquals(4, result.size());
		assertTrue(result.isMember(fileCatO));
		assertTrue(result.isMember(fileDogO));
		assertTrue(result.isMember(fileBunnyO));
		assertTrue(result.isMember(fileGiraffeO));
		
		/* test dog.o -> dog.a */
		source = new FileSet(fileMgr);
		source.add(fileDogO);
		result = reports.reportDerivedFiles(source, false);
		assertEquals(1, result.size());
		assertTrue(result.isMember(fileDogA));
				
		/* test dog.a -> animals.exe */
		source = new FileSet(fileMgr);
		source.add(fileDogA);
		result = reports.reportDerivedFiles(source, false);
		assertEquals(1, result.size());
		assertTrue(result.isMember(fileAnimalsExe));
		
		/* test cat.c, dog.c -> cat.o, dog.o */
		source = new FileSet(fileMgr);
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
		source = new FileSet(fileMgr);
		source.add(fileCatC);
		result = reports.reportDerivedFiles(source, true);
		assertEquals(3, result.size());
		assertTrue(result.isMember(fileCatO));
		assertTrue(result.isMember(fileCatA));
		assertTrue(result.isMember(fileAnimalsExe));
		
		/* test dog.c -> dog.o, dog.a, animals.exe */
		source = new FileSet(fileMgr);
		source.add(fileDogC);
		result = reports.reportDerivedFiles(source, true);
		assertEquals(3, result.size());
		assertTrue(result.isMember(fileDogO));
		assertTrue(result.isMember(fileDogA));
		assertTrue(result.isMember(fileAnimalsExe));
		
		/* test bunny.o -> bunny.a, animals.exe */
		source = new FileSet(fileMgr);
		source.add(fileBunnyO);
		result = reports.reportDerivedFiles(source, true);
		assertEquals(2, result.size());
		assertTrue(result.isMember(fileBunnyA));
		assertTrue(result.isMember(fileAnimalsExe));

		/* 
		 * Test pets.h -> cat.o, dog.o, bunny.o, giraffe.o, cat.a, 
		 * dog.a, bunny.a, giraffe.a, animals.exe
		 */
		source = new FileSet(fileMgr);
		source.add(filePetH);
		result = reports.reportDerivedFiles(source, true);
		assertTrue(CommonTestUtils.treeSetEqual(result, 
				new Integer[] {fileCatO, fileDogO, fileBunnyO, fileGiraffeO, fileCatA, fileDogA, 
				fileBunnyA, fileGiraffeA, fileAnimalsExe}));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IReportMgr#reportInputFiles(FileSet, boolean)}.
	 */
	@Test
	public void testReportInputFiles() {

		/*
		 * Test directly input relationships
		 */

		/* test empty FileSet -> empty FileSet */
		FileSet dest = new FileSet(fileMgr);
		FileSet result = reports.reportInputFiles(dest, false);
		assertEquals(0, result.size());
		
		/* test {house-pet.h, pet.h, cat.c} <- cat.o */
		dest = new FileSet(fileMgr);
		dest.add(fileCatO);
		result = reports.reportInputFiles(dest, false);
		assertEquals(3, result.size());
		assertTrue(result.isMember(fileCatC));
		assertTrue(result.isMember(filePetH));
		assertTrue(result.isMember(fileHousePetH));
		
		/* test {house-pet.h, pet.h, dog.c} <- dog.o */
		dest = new FileSet(fileMgr);
		dest.add(fileDogO);
		result = reports.reportInputFiles(dest, false);
		assertEquals(3, result.size());
		assertTrue(result.isMember(fileDogC));
		assertTrue(result.isMember(filePetH));
		assertTrue(result.isMember(fileHousePetH));
		
		/* test {pet.h, giraffe.c} <- giraffe.o */
		dest = new FileSet(fileMgr);
		dest.add(fileGiraffeO);
		result = reports.reportInputFiles(dest, false);
		assertEquals(2, result.size());
		assertTrue(result.isMember(fileGiraffeC));
		assertTrue(result.isMember(filePetH));

		/* test {} <- pets.h */
		dest = new FileSet(fileMgr);
		dest.add(filePetH);
		result = reports.reportInputFiles(dest, false);
		assertEquals(0, result.size());

		/* test dog.o <- dog.a */
		dest = new FileSet(fileMgr);
		dest.add(fileDogA);
		result = reports.reportInputFiles(dest, false);
		assertEquals(1, result.size());
		assertTrue(result.isMember(fileDogO));
		
		/* test dog.a, cat.a, bunny.a, giraffe.a <- animals.exe */
		dest = new FileSet(fileMgr);
		dest.add(fileAnimalsExe);
		result = reports.reportInputFiles(dest, false);
		assertEquals(4, result.size());
		assertTrue(result.isMember(fileDogA));
		assertTrue(result.isMember(fileCatA));
		assertTrue(result.isMember(fileBunnyA));
		assertTrue(result.isMember(fileGiraffeA));

		/* test cat.c, dog.c, pets.h, house-pets.h <- cat.o, dog.o */
		dest = new FileSet(fileMgr);
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
		dest = new FileSet(fileMgr);
		dest.add(fileBunnyA);
		result = reports.reportInputFiles(dest, true);
		assertEquals(4, result.size());
		assertTrue(result.isMember(filePetH));
		assertTrue(result.isMember(fileBunnyC));
		assertTrue(result.isMember(fileBunnyO));
		assertTrue(result.isMember(fileHousePetH));
		
		/* test pets.h, house-pets.h, *.c, *.o, *.a <- animals.exe */
		dest = new FileSet(fileMgr);
		dest.add(fileAnimalsExe);
		result = reports.reportInputFiles(dest, true);
		assertEquals(14, result.size());
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IReportMgr#reportFilesFromPackageSet}.
	 */
	@Test
	public void testReportFilesFromPackageSet() {
		
		/* create a new package set and add some packages */
		PackageSet cs = new PackageSet(bs);
		IPackageMgr pkgMgr = bs.getPackageMgr();
		int pkg1Id = pkgMgr.addPackage("Pkg1");
		int pkg2Id = pkgMgr.addPackage("Pkg2");		
		int pkg3Id = pkgMgr.addPackage("Pkg3");
		int pkg4Id = pkgMgr.addPackage("Pkg4");

		/* initially, no paths should be present (not even /) */
		FileSet fs = reports.reportFilesFromPackageSet(cs);
		assertEquals(0, fs.size());
		
		/* add a bunch of files - they should all be in the None package */
		int file1 = fileMgr.addFile("/file1");
		int file2 = fileMgr.addFile("/file2");
		int file3 = fileMgr.addFile("/file3");
		int file4 = fileMgr.addFile("/file4");
		int file5 = fileMgr.addFile("/file5");
		int file6 = fileMgr.addFile("/file6");
		int file7 = fileMgr.addFile("/file7");
		int file8 = fileMgr.addFile("/file8");
		
		/* empty package set still gives an empty FileSet */
		fs = reports.reportFilesFromPackageSet(cs);
		assertEquals(0, fs.size());
		
		/* map the files into packages */
		pkgMgr.setFilePackage(file1, pkg1Id, IPackageMgr.SCOPE_PRIVATE);
		pkgMgr.setFilePackage(file2, pkg1Id, IPackageMgr.SCOPE_PUBLIC);
		pkgMgr.setFilePackage(file3, pkg2Id, IPackageMgr.SCOPE_PRIVATE);
		pkgMgr.setFilePackage(file4, pkg2Id, IPackageMgr.SCOPE_PUBLIC);
		pkgMgr.setFilePackage(file5, pkg3Id, IPackageMgr.SCOPE_PRIVATE);
		pkgMgr.setFilePackage(file6, pkg3Id, IPackageMgr.SCOPE_PUBLIC);
		pkgMgr.setFilePackage(file7, pkg4Id, IPackageMgr.SCOPE_PRIVATE);
		pkgMgr.setFilePackage(file8, pkg4Id, IPackageMgr.SCOPE_PUBLIC);
	
		/* empty package set still gives an empty FileSet */
		fs = reports.reportFilesFromPackageSet(cs);
		assertEquals(0, fs.size());
		
		/* add Pkg1/Private into the package set */
		cs.add(pkg1Id, IPackageMgr.SCOPE_PRIVATE);
		fs = reports.reportFilesFromPackageSet(cs);
		assertEquals(1, fs.size());
		assertTrue(fs.isMember(file1));

		/* add Pkg1/Public into the package set */
		cs.add(pkg1Id, IPackageMgr.SCOPE_PUBLIC);
		fs = reports.reportFilesFromPackageSet(cs);
		assertEquals(2, fs.size());
		assertTrue(fs.isMember(file1));
		assertTrue(fs.isMember(file2));

		/* add Pkg2 into the package set */
		cs.add(pkg2Id);
		fs = reports.reportFilesFromPackageSet(cs);
		assertEquals(4, fs.size());
		assertTrue(fs.isMember(file1));
		assertTrue(fs.isMember(file2));
		assertTrue(fs.isMember(file3));
		assertTrue(fs.isMember(file4));
		
		/* add Pkg8/Public into the package set */
		cs.add(pkg4Id, IPackageMgr.SCOPE_PUBLIC);
		fs = reports.reportFilesFromPackageSet(cs);
		assertEquals(5, fs.size());
		assertTrue(fs.isMember(file1));
		assertTrue(fs.isMember(file2));
		assertTrue(fs.isMember(file3));
		assertTrue(fs.isMember(file4));
		assertTrue(fs.isMember(file8));
		
		/* add a few hundred packages, and check scalability */
		for (int i = 5; i != 200; i++) {
			int id = pkgMgr.addPackage("Pkg" + i);
			cs.add(id, 1 + (i % 2));
		}
		
		/* the simple fact that this doesn't crash is enough to pass the test */
		fs = reports.reportFilesFromPackageSet(cs);	
	}
	
	/*-------------------------------------------------------------------------------------*/
	

	/**
	 * Test method for {@link com.buildml.model.IReportMgr#reportActionsFromPackageSet}.
	 */
	@Test
	public void testReportActionsFromPackageSet() {
		
		/* create a new package set and add some packages */
		PackageSet cs = new PackageSet(bs);
		IPackageMgr pkgMgr = bs.getPackageMgr();
		int pkg1Id = pkgMgr.addPackage("Pkg1");
		int pkg2Id = pkgMgr.addPackage("Pkg2");		
		int pkg3Id = pkgMgr.addPackage("Pkg3");
		int pkg4Id = pkgMgr.addPackage("Pkg4");

		/* initially, no actions should be present */
		TaskSet tset = reports.reportActionsFromPackageSet(cs);
		assertEquals(0, tset.size());
		
		/* add a bunch of actions - they should all be in the None package */
		int action1 = actionMgr.addBuildTask(0, 0, "command 1");
		int action2 = actionMgr.addBuildTask(0, 0, "command 2");
		int action3 = actionMgr.addBuildTask(0, 0, "command 3");
		int action4 = actionMgr.addBuildTask(0, 0, "command 4");
		int action5 = actionMgr.addBuildTask(0, 0, "command 5");
		int action6 = actionMgr.addBuildTask(0, 0, "command 6");
		int action7 = actionMgr.addBuildTask(0, 0, "command 7");
		int action8 = actionMgr.addBuildTask(0, 0, "command 8");
		
		/* empty package set still gives an empty ActionSet */
		tset = reports.reportActionsFromPackageSet(cs);
		assertEquals(0, tset.size());
		
		/* map the actions into packages */
		pkgMgr.setTaskPackage(action1, pkg1Id);
		pkgMgr.setTaskPackage(action2, pkg2Id);
		pkgMgr.setTaskPackage(action3, pkg3Id);
		pkgMgr.setTaskPackage(action4, pkg4Id);
		pkgMgr.setTaskPackage(action5, pkg1Id);
		pkgMgr.setTaskPackage(action6, pkg2Id);
		pkgMgr.setTaskPackage(action7, pkg3Id);
		pkgMgr.setTaskPackage(action8, pkg4Id);

		/* empty package set still gives an empty ActionSet */
		tset = reports.reportActionsFromPackageSet(cs);
		assertEquals(0, tset.size());
		
		/* add Pkg1 into the package set */
		cs.add(pkg1Id, IPackageMgr.SCOPE_PUBLIC);
		tset = reports.reportActionsFromPackageSet(cs);
		assertEquals(2, tset.size());
		assertTrue(tset.isMember(action1));
		assertTrue(tset.isMember(action5));

		/* add Pkg2 into the package set */
		cs.add(pkg2Id);
		tset = reports.reportActionsFromPackageSet(cs);
		assertEquals(4, tset.size());
		assertTrue(tset.isMember(action1));
		assertTrue(tset.isMember(action2));
		assertTrue(tset.isMember(action5));
		assertTrue(tset.isMember(action6));

		/* add Pkg4 into the package set */
		cs.add(pkg4Id, IPackageMgr.SCOPE_PUBLIC);
		tset = reports.reportActionsFromPackageSet(cs);
		assertEquals(6, tset.size());
		assertTrue(tset.isMember(action1));
		assertTrue(tset.isMember(action2));
		assertTrue(tset.isMember(action4));
		assertTrue(tset.isMember(action5));
		assertTrue(tset.isMember(action6));
		assertTrue(tset.isMember(action8));
		
		/* add a few hundred packages, and check scalability */
		for (int i = 5; i != 200; i++) {
			int id = pkgMgr.addPackage("Pkg" + i);
			cs.add(id, IPackageMgr.SCOPE_PUBLIC);
		}
		
		/* the simple fact that this doesn't crash is enough to pass the test */
		tset = reports.reportActionsFromPackageSet(cs);	
	}
	
	/*-------------------------------------------------------------------------------------*/
}
