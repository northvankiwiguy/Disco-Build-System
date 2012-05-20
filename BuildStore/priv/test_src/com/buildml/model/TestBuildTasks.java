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

import com.buildml.model.BuildStore;
import com.buildml.model.BuildTasks;
import com.buildml.model.CommonTestUtils;
import com.buildml.model.FileNameSpaces;
import com.buildml.model.BuildTasks.OperationType;
import com.buildml.utils.errors.ErrorCode;

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
	
	/** The root task ID */
	int rootTaskId;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		bs = CommonTestUtils.getEmptyBuildStore();
		
		/* fetch the associated FileNameSpace object */
		bsfs = bs.getFileNameSpaces();
		
		/* fetch the associated BuildTasks object */
		bts = bs.getBuildTasks();
		
		/* if we don't care about each new task's parents, we'll use the root task */
		rootTaskId = bts.getRootTask("root");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.BuildTasks#addBuildTask(int, int, String)}.
	 */
	@Test
	public void testAddBuildTask() {
		
		/* test that each new build task is assigned a unique ID number */
		int task1 = bts.addBuildTask(rootTaskId, 0, "gcc -o test.o test.c");
		int task2 = bts.addBuildTask(rootTaskId, 0, "gcc -o main.o main.c");
		int task3 = bts.addBuildTask(rootTaskId, 0, "gcc -o tree.o tree.c");
		assertNotSame(task1, task2);
		assertNotSame(task1, task3);
		assertNotSame(task2, task3);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.BuildTasks#getCommand(int)}
	 */
	@Test
	public void testGetCommand() {
		int task1 = bts.addBuildTask(rootTaskId, 0, "gcc -o test.o test.c");
		int task2 = bts.addBuildTask(rootTaskId, 0, "gcc -o main.o main.c");
		int task3 = bts.addBuildTask(rootTaskId, 0, "gcc -o tree.o tree.c");
		assertEquals("gcc -o tree.o tree.c", bts.getCommand(task3));
		assertEquals("gcc -o main.o main.c", bts.getCommand(task2));
		assertEquals("gcc -o test.o test.c", bts.getCommand(task1));
		
		/* an invalid task ID should return null */
		assertNull(bts.getCommand(100));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.BuildTasks#getCommandSummary(int, int)}
	 */
	@Test
	public void testGetCommandSummary() {
	
		/* create a single task, with a long command string */
		int mytask = bts.addBuildTask(bts.getRootTask(""), 0,
				"gcc -Ipath1/include -Ipath2/include -Ipath3/include -DFOO -DBAR " +
				"-o myfile.o -c myfile.c");
		
		/* fetch the summary at various widths */
		assertEquals("gcc -Ipath1/...", bts.getCommandSummary(mytask, 15));
		assertEquals("gcc -Ipath1/include -Ipath2...", bts.getCommandSummary(mytask, 30));
		assertEquals("gcc -Ipath1/include -Ipath2/include -Ipath3/inc...", bts.getCommandSummary(mytask, 50));
		assertEquals("gcc -Ipath1/include -Ipath2/include -Ipath3/include -DFOO -DBAR " +
				"-o myfile.o -c myfile....", bts.getCommandSummary(mytask, 89));
		assertEquals("gcc -Ipath1/include -Ipath2/include -Ipath3/include -DFOO -DBAR " +
				"-o myfile.o -c myfile.c", bts.getCommandSummary(mytask, 100));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.BuildTasks#getParent(int)}
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testGetParent() throws Exception {
		
		/* add a bunch of tasks in a hierarchy */
		int task1 = bts.addBuildTask(rootTaskId, 0, "/bin/sh");
		int task2 = bts.addBuildTask(task1, 0, "gcc -o main.o main.c");
		int task3 = bts.addBuildTask(task1, 0, "/bin/sh");
		int task4 = bts.addBuildTask(task3, 0, "gcc -o tree.o tree.c");
		int task5 = bts.addBuildTask(task3, 0, "gcc -o bark.o bark.c");
		
		/* the parent of the root is ErrorCode.NOT_FOUND */
		assertEquals(ErrorCode.NOT_FOUND, bts.getParent(rootTaskId));
		
		/* all the other new tasks have valid parents */
		assertEquals(rootTaskId, bts.getParent(task1));
		assertEquals(task1, bts.getParent(task2));
		assertEquals(task1, bts.getParent(task3));
		assertEquals(task3, bts.getParent(task4));
		assertEquals(task3, bts.getParent(task5));
		
		/* inquiring about the parent of an invalid task Id is ErrorCode.BAD_VALUE */
		assertEquals(ErrorCode.BAD_VALUE, bts.getParent(1000));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.BuildTasks#getDirectory(int)}
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testGetDirectory() throws Exception {
		
		/* add a bunch of tasks in a hierarchy, each with a different directory */
		int task1 = bts.addBuildTask(rootTaskId, 0, "/bin/sh");
		int task2 = bts.addBuildTask(task1, 10, "gcc -o main.o main.c");
		int task3 = bts.addBuildTask(task1, 20, "/bin/sh");
		int task4 = bts.addBuildTask(task3, 25, "gcc -o tree.o tree.c");
		int task5 = bts.addBuildTask(task3, 30, "gcc -o bark.o bark.c");
		
		/* check that the directories are stored correctly */
		assertEquals(0, bts.getDirectory(task1));
		assertEquals(10, bts.getDirectory(task2));
		assertEquals(20, bts.getDirectory(task3));
		assertEquals(25, bts.getDirectory(task4));
		assertEquals(30, bts.getDirectory(task5));
		
		/* invalid task IDs should return an error */
		assertEquals(ErrorCode.NOT_FOUND, bts.getDirectory(1000));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.BuildTasks#getParent(int)}
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testGetChildren() throws Exception {

		/* add a bunch of tasks in a hierarchy */
		int task1 = bts.addBuildTask(rootTaskId, 0, "/bin/sh");
		int task2 = bts.addBuildTask(task1, 0, "gcc -o main.o main.c");
		int task3 = bts.addBuildTask(task1, 0, "/bin/sh");
		int task4 = bts.addBuildTask(task3, 0, "gcc -o tree.o tree.c");
		int task5 = bts.addBuildTask(task3, 0, "gcc -o bark.o bark.c");
		int task6 = bts.addBuildTask(task3, 0, "gcc -o woof.o woof.c");
		
		/* test valid parent/child relationships */
		assertTrue(CommonTestUtils.sortedArraysEqual(bts.getChildren(rootTaskId), new Integer[] {task1}));
		assertTrue(CommonTestUtils.sortedArraysEqual(bts.getChildren(task1), new Integer[] {task2, task3 }));
		assertTrue(CommonTestUtils.sortedArraysEqual(bts.getChildren(task2), new Integer[] {}));
		assertTrue(CommonTestUtils.sortedArraysEqual(bts.getChildren(task3), new Integer[] {task4, task5, task6 }));
		assertTrue(CommonTestUtils.sortedArraysEqual(bts.getChildren(task4), new Integer[] {}));
		assertTrue(CommonTestUtils.sortedArraysEqual(bts.getChildren(task5), new Integer[] {}));
		assertTrue(CommonTestUtils.sortedArraysEqual(bts.getChildren(task6), new Integer[] {}));
		
		/* the children of non-existent tasks is the empty list */
		assertTrue(CommonTestUtils.sortedArraysEqual(bts.getChildren(1000), new Integer[] {}));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.BuildTasks#addFileAccess(int, int, OperationType)}.
	 */
	@Test
	public void testAddGetFileAccess() {
		/* create a new task */
		int task = bts.addBuildTask(rootTaskId, 0, "gcc -o foo foo.c");
		
		/* create a number of new files */
		int fileFooC = bsfs.addFile("/a/b/c/foo.c");
		int fileFooH = bsfs.addFile("/a/b/c/foo.h");
		int fileFooO = bsfs.addFile("/a/b/c/foo.o");
		int fileFoo = bsfs.addFile("/a/b/c/foo");
		
		/* record that these files are accessed by the task */
		bts.addFileAccess(task, fileFooC, OperationType.OP_READ);
		bts.addFileAccess(task, fileFooH, OperationType.OP_READ);
		bts.addFileAccess(task, fileFooO, OperationType.OP_WRITE);
		bts.addFileAccess(task, fileFoo, OperationType.OP_WRITE);
		
		/* now check that the records are correct */
		Integer allAccesses[] = bts.getFilesAccessed(task, OperationType.OP_UNSPECIFIED);
		assertTrue(CommonTestUtils.sortedArraysEqual(allAccesses, new Integer[] { fileFooH, fileFooO, fileFooC, fileFoo }));

		/* now, just the reads */
		Integer readAccesses[] = bts.getFilesAccessed(task, OperationType.OP_READ);
		assertTrue(CommonTestUtils.sortedArraysEqual(readAccesses, new Integer[] { fileFooC, fileFooH }));
		
		/* and just the writes */
		Integer writeAccesses[] = bts.getFilesAccessed(task, OperationType.OP_WRITE);
		assertTrue(CommonTestUtils.sortedArraysEqual(writeAccesses, new Integer[] { fileFooO, fileFoo }));

		/* check an empty task - should return no results */
		int emptyTask = bts.addBuildTask(rootTaskId, 0, "echo Hi");
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
		Integer result[];
		
		int task = bts.addBuildTask(rootTaskId, 0, "my mystery task");
		
		/* create a number of new files */
		int file1 = bsfs.addFile("/file1");
		int file2 = bsfs.addFile("/file2");
		int file3 = bsfs.addFile("/file3");
		int file4 = bsfs.addFile("/file4");
		int file5 = bsfs.addFile("/file5");
		int file6 = bsfs.addFile("/file6");
		int file7 = bsfs.addFile("/file7");
		int file8 = bsfs.addFile("/file8");
		int file9 = bsfs.addFile("/file9");
				
		/* test read, read => read */
		bts.addFileAccess(task, file1, OperationType.OP_READ);
		bts.addFileAccess(task, file1, OperationType.OP_READ);
		assertEquals(1, bts.getTasksThatAccess(file1, OperationType.OP_READ).length);
		assertEquals(0, bts.getTasksThatAccess(file1, OperationType.OP_WRITE).length);
		
		/* test read, write => modify */
		bts.addFileAccess(task, file2, OperationType.OP_READ);
		bts.addFileAccess(task, file2, OperationType.OP_WRITE);
		assertEquals(1, bts.getTasksThatAccess(file2, OperationType.OP_MODIFIED).length);
		assertEquals(0, bts.getTasksThatAccess(file2, OperationType.OP_READ).length);
		assertEquals(0, bts.getTasksThatAccess(file2, OperationType.OP_WRITE).length);
		
		/* test write, write => write */
		bts.addFileAccess(task, file3, OperationType.OP_WRITE);
		bts.addFileAccess(task, file3, OperationType.OP_WRITE);
		assertEquals(0, bts.getTasksThatAccess(file3, OperationType.OP_READ).length);
		assertEquals(1, bts.getTasksThatAccess(file3, OperationType.OP_WRITE).length);
		
		/* test write, modify => write */
		bts.addFileAccess(task, file4, OperationType.OP_WRITE);
		bts.addFileAccess(task, file4, OperationType.OP_MODIFIED);
		assertEquals(0, bts.getTasksThatAccess(file4, OperationType.OP_MODIFIED).length);
		assertEquals(0, bts.getTasksThatAccess(file4, OperationType.OP_READ).length);
		assertEquals(1, bts.getTasksThatAccess(file4, OperationType.OP_WRITE).length);

		/* test delete, read => read */
		bts.addFileAccess(task, file5, OperationType.OP_DELETE);
		bts.addFileAccess(task, file5, OperationType.OP_READ);
		assertEquals(0, bts.getTasksThatAccess(file5, OperationType.OP_MODIFIED).length);
		assertEquals(1, bts.getTasksThatAccess(file5, OperationType.OP_READ).length);
		assertEquals(0, bts.getTasksThatAccess(file5, OperationType.OP_WRITE).length);
		
		/* test delete, modify => modify */
		bts.addFileAccess(task, file6, OperationType.OP_DELETE);
		bts.addFileAccess(task, file6, OperationType.OP_MODIFIED);
		assertEquals(1, bts.getTasksThatAccess(file6, OperationType.OP_MODIFIED).length);
		assertEquals(0, bts.getTasksThatAccess(file6, OperationType.OP_READ).length);
		assertEquals(0, bts.getTasksThatAccess(file6, OperationType.OP_WRITE).length);

		/* test read, write, delete => delete */
		bts.addFileAccess(task, file7, OperationType.OP_READ);
		bts.addFileAccess(task, file7, OperationType.OP_WRITE);
		bts.addFileAccess(task, file7, OperationType.OP_DELETE);
		assertEquals(0, bts.getTasksThatAccess(file7, OperationType.OP_MODIFIED).length);
		assertEquals(0, bts.getTasksThatAccess(file7, OperationType.OP_READ).length);
		assertEquals(0, bts.getTasksThatAccess(file7, OperationType.OP_WRITE).length);
		assertEquals(1, bts.getTasksThatAccess(file7, OperationType.OP_DELETE).length);
		
		/* test delete, read, write => modify */
		bts.addFileAccess(task, file8, OperationType.OP_DELETE);
		bts.addFileAccess(task, file8, OperationType.OP_READ);
		bts.addFileAccess(task, file8, OperationType.OP_WRITE);
		assertEquals(1, bts.getTasksThatAccess(file8, OperationType.OP_MODIFIED).length);
		assertEquals(0, bts.getTasksThatAccess(file8, OperationType.OP_READ).length);
		assertEquals(0, bts.getTasksThatAccess(file8, OperationType.OP_WRITE).length);
		assertEquals(0, bts.getTasksThatAccess(file8, OperationType.OP_DELETE).length);
		
		/* test write, read, delete => temporary - and the file is deleted */
		assertEquals(file9, bsfs.getPath("/file9"));
		bts.addFileAccess(task, file9, OperationType.OP_WRITE);
		bts.addFileAccess(task, file9, OperationType.OP_READ);
		bts.addFileAccess(task, file9, OperationType.OP_DELETE);
		assertEquals(0, bts.getTasksThatAccess(file9, OperationType.OP_MODIFIED).length);
		assertEquals(0, bts.getTasksThatAccess(file9, OperationType.OP_READ).length);
		assertEquals(0, bts.getTasksThatAccess(file9, OperationType.OP_WRITE).length);
		assertEquals(0, bts.getTasksThatAccess(file9, OperationType.OP_DELETE).length);
		assertEquals(ErrorCode.BAD_PATH, bsfs.getPath("/file9"));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.BuildTasks#getTasksThatAccess(int, OperationType)}.
	 */
	@Test
	public void testGetTasksThatAccess() {

		/* create some tasks */
		int task1 = bts.addBuildTask(rootTaskId, 0, "gcc -o clock.o clock.c");
		int task2 = bts.addBuildTask(rootTaskId, 0, "gcc -o banner.o banner.c");
		int task3 = bts.addBuildTask(rootTaskId, 0, "gcc -o mult.o mult.c");

		/* and a bunch of files that access those tasks */
		int file1 = bsfs.addFile("/clock.o");
		int file2 = bsfs.addFile("/clock.c");
		int file3 = bsfs.addFile("/banner.o");
		int file4 = bsfs.addFile("/banner.c");
		int file5 = bsfs.addFile("/mult.o");
		int file6 = bsfs.addFile("/mult.c");
		int file7 = bsfs.addFile("/stdio.h");
		
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
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { task1 }));

		results = bts.getTasksThatAccess(file1, OperationType.OP_WRITE);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { task1 }));

		results = bts.getTasksThatAccess(file1, OperationType.OP_READ);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] {} ));

		/* for clock.c */
		results = bts.getTasksThatAccess(file2, OperationType.OP_UNSPECIFIED);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { task1 } ));

		results = bts.getTasksThatAccess(file2, OperationType.OP_WRITE);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { } ));

		results = bts.getTasksThatAccess(file2, OperationType.OP_READ);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { task1 } ));

		/* for mult.o */
		results = bts.getTasksThatAccess(file5, OperationType.OP_UNSPECIFIED);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { task3 } ));

		results = bts.getTasksThatAccess(file5, OperationType.OP_WRITE);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { task3 } ));

		results = bts.getTasksThatAccess(file5, OperationType.OP_READ);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { } ));

		/* for stdio.h */
		results = bts.getTasksThatAccess(file7, OperationType.OP_READ);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { task1, task2, task3 } ));

		results = bts.getTasksThatAccess(file7, OperationType.OP_WRITE);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { } ));
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for {@link com.buildml.model.BuildTasks#getTasksInDirectory(int)}.
	 */
	@Test
	public void testGetTasksInDirectory() {

		/* create a couple of directories */
		int dir1 = bsfs.addDirectory("/dir1");
		int dir2 = bsfs.addDirectory("/dir2");
		int dir3 = bsfs.addDirectory("/dir2/dir3");
		
		/* create a number of tasks, each executing in one of those directories */
		int rootTask = bts.getRootTask("root");
		int task11 = bts.addBuildTask(rootTask, dir1, "true");
		int task12 = bts.addBuildTask(rootTask, dir1, "true");
		int task21 = bts.addBuildTask(rootTask, dir2, "true");
		int task22 = bts.addBuildTask(rootTask, dir2, "true");
		int task23 = bts.addBuildTask(rootTask, dir2, "true");
		int task24 = bts.addBuildTask(rootTask, dir2, "true");
		int task13 = bts.addBuildTask(rootTask, dir1, "true");
	
		/* fetch the list of tasks that execute in the first directory */
		assertTrue(CommonTestUtils.sortedArraysEqual(bts.getTasksInDirectory(dir1),
				new Integer[] {task11, task12, task13}));
		
		/* repeat for the second directory */
		assertTrue(CommonTestUtils.sortedArraysEqual(bts.getTasksInDirectory(dir2),
				new Integer[] {task21, task22, task23, task24}));
		
		/* and the third should be empty */
		assertEquals(0, bts.getTasksInDirectory(dir3).length);
	}
	
	/*-------------------------------------------------------------------------------------*/

	
	/**
	 * Test the scalability of adding build tasks and file accesses.
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
			int taskId = bts.addBuildTask(rootTaskId, 0, sb.toString());
			
			/* now add files to this tasks */
			for (int k = 0; k != 200; k++) {
				bts.addFileAccess(taskId, r.nextInt(100), OperationType.OP_READ);
			}
			
			/* now read the files that were added */
			bts.getFilesAccessed(taskId, OperationType.OP_UNSPECIFIED);
		}
		bs.setFastAccessMode(false);
	}
	
	/*-------------------------------------------------------------------------------------*/

}
