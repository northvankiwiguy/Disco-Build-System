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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.arapiki.disco.model.BuildTasks.OperationType;
import com.arapiki.disco.model.TestCommon;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestBuildTasks {

	/** Our BuildStore object, used in many test cases */
	private BuildStore bs;

	/** The FileNameSpace object associated with this BuildStore */
	FileNameSpaces bsfs;
	
	/** The BuildTasks object associated with this BuildStore */
	BuildTasks bts;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		bs = TestCommon.getEmptyBuildStore();
		
		/* fetch the associated FileNameSpace object */
		bsfs = bs.getFileNameSpaces();
		
		/* fetch the associated BuildTasks object */
		bts = bs.getBuildTasks();	
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.BuildTasks#addBuildTask(java.lang.String)}.
	 */
	@Test
	public void testAddBuildTask() {
		
		/* test that each new build task is assigned a unique ID number */
		int task1 = bts.addBuildTask("gcc -o test.o test.c");
		int task2 = bts.addBuildTask("gcc -o main.o main.c");
		int task3 = bts.addBuildTask("gcc -o tree.o tree.c");
		assertNotSame(task1, task2);
		assertNotSame(task1, task3);
		assertNotSame(task2, task3);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.BuildTasks#getCommand(int)}
	 */
	@Test
	public void testGetCommand() {
		int task1 = bts.addBuildTask("gcc -o test.o test.c");
		int task2 = bts.addBuildTask("gcc -o main.o main.c");
		int task3 = bts.addBuildTask("gcc -o tree.o tree.c");
		assertEquals("gcc -o tree.o tree.c", bts.getCommand(task3));
		assertEquals("gcc -o main.o main.c", bts.getCommand(task2));
		assertEquals("gcc -o test.o test.c", bts.getCommand(task1));
		
		/* an invalid task ID should return null */
		assertNull(bts.getCommand(100));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.BuildTasks#addFileAccess(int, int, char)}.
	 */
	@Test
	public void testAddGetFileAccess() {
		/* create a new task */
		int task = bts.addBuildTask("gcc -o foo foo.c");
		
		/* create a number of new files */
		int fileFooC = bsfs.addFile("root", "/a/b/c/foo.c");
		int fileFooH = bsfs.addFile("root", "/a/b/c/foo.h");
		int fileFooO = bsfs.addFile("root", "/a/b/c/foo.o");
		int fileFoo = bsfs.addFile("root", "/a/b/c/foo");
		
		/* record that these files are accessed by the task */
		bts.addFileAccess(task, fileFooC, OperationType.OP_READ);
		bts.addFileAccess(task, fileFooH, OperationType.OP_READ);
		bts.addFileAccess(task, fileFooO, OperationType.OP_WRITE);
		bts.addFileAccess(task, fileFoo, OperationType.OP_WRITE);
		
		/* now check that the records are correct */
		Integer allAccesses[] = bts.getFilesAccessed(task, OperationType.OP_UNSPECIFIED);
		assertTrue(TestCommon.sortedArraysEqual(allAccesses, new Integer[] { fileFooH, fileFooO, fileFooC, fileFoo }));

		/* now, just the reads */
		Integer readAccesses[] = bts.getFilesAccessed(task, OperationType.OP_READ);
		assertTrue(TestCommon.sortedArraysEqual(readAccesses, new Integer[] { fileFooC, fileFooH }));
		
		/* and just the writes */
		Integer writeAccesses[] = bts.getFilesAccessed(task, OperationType.OP_WRITE);
		assertTrue(TestCommon.sortedArraysEqual(writeAccesses, new Integer[] { fileFooO, fileFoo }));

		/* check an empty task - should return no results */
		int emptyTask = bts.addBuildTask("echo Hi");
		Integer emptyAccesses[] = bts.getFilesAccessed(emptyTask, OperationType.OP_UNSPECIFIED);
		assertEquals(0, emptyAccesses.length);
		
		/* check with an invalid task number - should return no results */
		Integer noAccesses[] = bts.getFilesAccessed(100, OperationType.OP_UNSPECIFIED);
		assertEquals(0, noAccesses.length);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test what happens when a single file is accessed multiple times, in many different
	 * modes (e.g. reading, then writing, then delete, etc).
	 */
	@Test
	public void testMultipleFilesAccesses() {
		fail("Not yet implemented");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.BuildTasks#getTasksThatAccess(int, char)}.
	 */
	@Test
	public void testGetTasksThatAccess() {

		/* create some tasks */
		int task1 = bts.addBuildTask("gcc -o clock.o clock.c");
		int task2 = bts.addBuildTask("gcc -o banner.o banner.c");
		int task3 = bts.addBuildTask("gcc -o mult.o mult.c");

		/* and a bunch of files that access those tasks */
		int file1 = bsfs.addFile("root", "clock.o");
		int file2 = bsfs.addFile("root", "clock.c");
		int file3 = bsfs.addFile("root", "banner.o");
		int file4 = bsfs.addFile("root", "banner.c");
		int file5 = bsfs.addFile("root", "mult.o");
		int file6 = bsfs.addFile("root", "mult.c");
		int file7 = bsfs.addFile("root", "stdio.h");
		
		/* now register each task's file accesses */
		bts.addFileAccess(task1, file1, OperationType.OP_WRITE);
		bts.addFileAccess(task1, file2, OperationType.OP_READ);
		bts.addFileAccess(task1, file7, OperationType.OP_READ);

		bts.addFileAccess(task2, file3, OperationType.OP_WRITE);
		bts.addFileAccess(task2, file4, OperationType.OP_READ);
		bts.addFileAccess(task2, file7, OperationType.OP_READ);		

		bts.addFileAccess(task3, file5, OperationType.OP_WRITE);
		bts.addFileAccess(task3, file6, OperationType.OP_READ);
		bts.addFileAccess(task3, file7, OperationType.OP_READ);		

		/* 
		 * Finally, fetch the list of tasks that each file is reference by.
		 * There are numerous combinations to test here.
		 */

		/* for clock.o */
		Integer results[] = bts.getTasksThatAccess(file1, OperationType.OP_UNSPECIFIED);
		assertTrue(TestCommon.sortedArraysEqual(results, new Integer[] { task1 }));

		results = bts.getTasksThatAccess(file1, OperationType.OP_WRITE);
		assertTrue(TestCommon.sortedArraysEqual(results, new Integer[] { task1 }));

		results = bts.getTasksThatAccess(file1, OperationType.OP_READ);
		assertTrue(TestCommon.sortedArraysEqual(results, new Integer[] {} ));

		/* for clock.c */
		results = bts.getTasksThatAccess(file2, OperationType.OP_UNSPECIFIED);
		assertTrue(TestCommon.sortedArraysEqual(results, new Integer[] { task1 } ));

		results = bts.getTasksThatAccess(file2, OperationType.OP_WRITE);
		assertTrue(TestCommon.sortedArraysEqual(results, new Integer[] { } ));

		results = bts.getTasksThatAccess(file2, OperationType.OP_READ);
		assertTrue(TestCommon.sortedArraysEqual(results, new Integer[] { task1 } ));

		/* for mult.o */
		results = bts.getTasksThatAccess(file5, OperationType.OP_UNSPECIFIED);
		assertTrue(TestCommon.sortedArraysEqual(results, new Integer[] { task3 } ));

		results = bts.getTasksThatAccess(file5, OperationType.OP_WRITE);
		assertTrue(TestCommon.sortedArraysEqual(results, new Integer[] { task3 } ));

		results = bts.getTasksThatAccess(file5, OperationType.OP_READ);
		assertTrue(TestCommon.sortedArraysEqual(results, new Integer[] { } ));

		/* for stdio.h */
		results = bts.getTasksThatAccess(file7, OperationType.OP_READ);
		assertTrue(TestCommon.sortedArraysEqual(results, new Integer[] { task1, task2, task3 } ));

		results = bts.getTasksThatAccess(file7, OperationType.OP_WRITE);
		assertTrue(TestCommon.sortedArraysEqual(results, new Integer[] { } ));
	}

	/*-------------------------------------------------------------------------------------*/
	
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
			 * Each path component can be 5-10 characters long. To ensure
			 * we get some degree of consistency in path names, we'll use
			 * the names listed in the componentNames variable.
			 */
			sb.delete(0, sb.length());
			for (int j = 0; j != numChars; j++) {
				sb.append((char)(r.nextInt(26) + 65));
			}
			
			//System.out.println("Adding " + sb);

			/* add the file name to the FileSpace */
			int taskId = bts.addBuildTask(sb.toString());
			
			/* now add files to this tasks */
			for (int k = 0; k != 200; k++) {
				bts.addFileAccess(taskId, r.nextInt(100), OperationType.OP_READ);
			}
			
			/* now read the files that were added */
			Integer result[] = bts.getFilesAccessed(taskId, OperationType.OP_UNSPECIFIED);
		}
		bs.setFastAccessMode(false);
	}
	
	/*-------------------------------------------------------------------------------------*/

}
