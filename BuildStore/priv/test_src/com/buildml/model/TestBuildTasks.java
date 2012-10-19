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

import com.buildml.model.CommonTestUtils;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.utils.errors.ErrorCode;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestBuildTasks {

	/** Our BuildStore object, used in many test cases */
	private IBuildStore bs;

	/** The FileMgr object associated with this BuildStore */
	IFileMgr fileMgr;
	
	/** The ActionMgr object associated with this BuildStore */
	IActionMgr actionMgr;
	
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
		fileMgr = bs.getFileMgr();
		
		/* fetch the associated BuildTasks object */
		actionMgr = bs.getActionMgr();
		
		/* if we don't care about each new task's parents, we'll use the root task */
		rootTaskId = actionMgr.getRootAction("root");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.ActionMgr#addAction(int, int, String)}.
	 */
	@Test
	public void testAddBuildTask() {
		
		/* test that each new build task is assigned a unique ID number */
		int task1 = actionMgr.addAction(rootTaskId, 0, "gcc -o test.o test.c");
		int task2 = actionMgr.addAction(rootTaskId, 0, "gcc -o main.o main.c");
		int task3 = actionMgr.addAction(rootTaskId, 0, "gcc -o tree.o tree.c");
		assertNotSame(task1, task2);
		assertNotSame(task1, task3);
		assertNotSame(task2, task3);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.ActionMgr#getCommand(int)}
	 */
	@Test
	public void testGetCommand() {
		int task1 = actionMgr.addAction(rootTaskId, 0, "gcc -o test.o test.c");
		int task2 = actionMgr.addAction(rootTaskId, 0, "gcc -o main.o main.c");
		int task3 = actionMgr.addAction(rootTaskId, 0, "gcc -o tree.o tree.c");
		assertEquals("gcc -o tree.o tree.c", actionMgr.getCommand(task3));
		assertEquals("gcc -o main.o main.c", actionMgr.getCommand(task2));
		assertEquals("gcc -o test.o test.c", actionMgr.getCommand(task1));
		
		/* an invalid task ID should return null */
		assertNull(actionMgr.getCommand(100));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.ActionMgr#getCommandSummary(int, int)}
	 */
	@Test
	public void testGetCommandSummary() {
	
		/* create a single task, with a long command string */
		int mytask = actionMgr.addAction(actionMgr.getRootAction(""), 0,
				"gcc -Ipath1/include -Ipath2/include -Ipath3/include -DFOO -DBAR " +
				"-o myfile.o -c myfile.c");
		
		/* fetch the summary at various widths */
		assertEquals("gcc -Ipath1/...", actionMgr.getCommandSummary(mytask, 15));
		assertEquals("gcc -Ipath1/include -Ipath2...", actionMgr.getCommandSummary(mytask, 30));
		assertEquals("gcc -Ipath1/include -Ipath2/include -Ipath3/inc...", actionMgr.getCommandSummary(mytask, 50));
		assertEquals("gcc -Ipath1/include -Ipath2/include -Ipath3/include -DFOO -DBAR " +
				"-o myfile.o -c myfile....", actionMgr.getCommandSummary(mytask, 89));
		assertEquals("gcc -Ipath1/include -Ipath2/include -Ipath3/include -DFOO -DBAR " +
				"-o myfile.o -c myfile.c", actionMgr.getCommandSummary(mytask, 100));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.ActionMgr#getParent(int)}
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testGetParent() throws Exception {
		
		/* add a bunch of tasks in a hierarchy */
		int task1 = actionMgr.addAction(rootTaskId, 0, "/bin/sh");
		int task2 = actionMgr.addAction(task1, 0, "gcc -o main.o main.c");
		int task3 = actionMgr.addAction(task1, 0, "/bin/sh");
		int task4 = actionMgr.addAction(task3, 0, "gcc -o tree.o tree.c");
		int task5 = actionMgr.addAction(task3, 0, "gcc -o bark.o bark.c");
		
		/* the parent of the root is ErrorCode.NOT_FOUND */
		assertEquals(ErrorCode.NOT_FOUND, actionMgr.getParent(rootTaskId));
		
		/* all the other new tasks have valid parents */
		assertEquals(rootTaskId, actionMgr.getParent(task1));
		assertEquals(task1, actionMgr.getParent(task2));
		assertEquals(task1, actionMgr.getParent(task3));
		assertEquals(task3, actionMgr.getParent(task4));
		assertEquals(task3, actionMgr.getParent(task5));
		
		/* inquiring about the parent of an invalid task Id is ErrorCode.BAD_VALUE */
		assertEquals(ErrorCode.BAD_VALUE, actionMgr.getParent(1000));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.ActionMgr#getDirectory(int)}
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testGetDirectory() throws Exception {
		
		/* add a bunch of tasks in a hierarchy, each with a different directory */
		int task1 = actionMgr.addAction(rootTaskId, 0, "/bin/sh");
		int task2 = actionMgr.addAction(task1, 10, "gcc -o main.o main.c");
		int task3 = actionMgr.addAction(task1, 20, "/bin/sh");
		int task4 = actionMgr.addAction(task3, 25, "gcc -o tree.o tree.c");
		int task5 = actionMgr.addAction(task3, 30, "gcc -o bark.o bark.c");
		
		/* check that the directories are stored correctly */
		assertEquals(0, actionMgr.getDirectory(task1));
		assertEquals(10, actionMgr.getDirectory(task2));
		assertEquals(20, actionMgr.getDirectory(task3));
		assertEquals(25, actionMgr.getDirectory(task4));
		assertEquals(30, actionMgr.getDirectory(task5));
		
		/* invalid task IDs should return an error */
		assertEquals(ErrorCode.NOT_FOUND, actionMgr.getDirectory(1000));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.ActionMgr#getParent(int)}
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testGetChildren() throws Exception {

		/* add a bunch of tasks in a hierarchy */
		int task1 = actionMgr.addAction(rootTaskId, 0, "/bin/sh");
		int task2 = actionMgr.addAction(task1, 0, "gcc -o main.o main.c");
		int task3 = actionMgr.addAction(task1, 0, "/bin/sh");
		int task4 = actionMgr.addAction(task3, 0, "gcc -o tree.o tree.c");
		int task5 = actionMgr.addAction(task3, 0, "gcc -o bark.o bark.c");
		int task6 = actionMgr.addAction(task3, 0, "gcc -o woof.o woof.c");
		
		/* test valid parent/child relationships */
		assertTrue(CommonTestUtils.sortedArraysEqual(actionMgr.getChildren(rootTaskId), new Integer[] {task1}));
		assertTrue(CommonTestUtils.sortedArraysEqual(actionMgr.getChildren(task1), new Integer[] {task2, task3 }));
		assertTrue(CommonTestUtils.sortedArraysEqual(actionMgr.getChildren(task2), new Integer[] {}));
		assertTrue(CommonTestUtils.sortedArraysEqual(actionMgr.getChildren(task3), new Integer[] {task4, task5, task6 }));
		assertTrue(CommonTestUtils.sortedArraysEqual(actionMgr.getChildren(task4), new Integer[] {}));
		assertTrue(CommonTestUtils.sortedArraysEqual(actionMgr.getChildren(task5), new Integer[] {}));
		assertTrue(CommonTestUtils.sortedArraysEqual(actionMgr.getChildren(task6), new Integer[] {}));
		
		/* the children of non-existent tasks is the empty list */
		assertTrue(CommonTestUtils.sortedArraysEqual(actionMgr.getChildren(1000), new Integer[] {}));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.ActionMgr#addFileAccess(int, int, OperationType)}.
	 */
	@Test
	public void testAddGetFileAccess() {
		/* create a new task */
		int task = actionMgr.addAction(rootTaskId, 0, "gcc -o foo foo.c");
		
		/* create a number of new files */
		int fileFooC = fileMgr.addFile("/a/b/c/foo.c");
		int fileFooH = fileMgr.addFile("/a/b/c/foo.h");
		int fileFooO = fileMgr.addFile("/a/b/c/foo.o");
		int fileFoo = fileMgr.addFile("/a/b/c/foo");
		
		/* record that these files are accessed by the task */
		actionMgr.addFileAccess(task, fileFooC, OperationType.OP_READ);
		actionMgr.addFileAccess(task, fileFooH, OperationType.OP_READ);
		actionMgr.addFileAccess(task, fileFooO, OperationType.OP_WRITE);
		actionMgr.addFileAccess(task, fileFoo, OperationType.OP_WRITE);
		
		/* now check that the records are correct */
		Integer allAccesses[] = actionMgr.getFilesAccessed(task, OperationType.OP_UNSPECIFIED);
		assertTrue(CommonTestUtils.sortedArraysEqual(allAccesses, new Integer[] { fileFooH, fileFooO, fileFooC, fileFoo }));

		/* now, just the reads */
		Integer readAccesses[] = actionMgr.getFilesAccessed(task, OperationType.OP_READ);
		assertTrue(CommonTestUtils.sortedArraysEqual(readAccesses, new Integer[] { fileFooC, fileFooH }));
		
		/* and just the writes */
		Integer writeAccesses[] = actionMgr.getFilesAccessed(task, OperationType.OP_WRITE);
		assertTrue(CommonTestUtils.sortedArraysEqual(writeAccesses, new Integer[] { fileFooO, fileFoo }));

		/* check an empty task - should return no results */
		int emptyTask = actionMgr.addAction(rootTaskId, 0, "echo Hi");
		Integer emptyAccesses[] = actionMgr.getFilesAccessed(emptyTask, OperationType.OP_UNSPECIFIED);
		assertEquals(0, emptyAccesses.length);
		
		/* check with an invalid task number - should return no results */
		Integer noAccesses[] = actionMgr.getFilesAccessed(100, OperationType.OP_UNSPECIFIED);
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
		
		int task = actionMgr.addAction(rootTaskId, 0, "my mystery task");
		
		/* create a number of new files */
		int file1 = fileMgr.addFile("/file1");
		int file2 = fileMgr.addFile("/file2");
		int file3 = fileMgr.addFile("/file3");
		int file4 = fileMgr.addFile("/file4");
		int file5 = fileMgr.addFile("/file5");
		int file6 = fileMgr.addFile("/file6");
		int file7 = fileMgr.addFile("/file7");
		int file8 = fileMgr.addFile("/file8");
		int file9 = fileMgr.addFile("/file9");
				
		/* test read, read => read */
		actionMgr.addFileAccess(task, file1, OperationType.OP_READ);
		actionMgr.addFileAccess(task, file1, OperationType.OP_READ);
		assertEquals(1, actionMgr.getActionsThatAccess(file1, OperationType.OP_READ).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file1, OperationType.OP_WRITE).length);
		
		/* test read, write => modify */
		actionMgr.addFileAccess(task, file2, OperationType.OP_READ);
		actionMgr.addFileAccess(task, file2, OperationType.OP_WRITE);
		assertEquals(1, actionMgr.getActionsThatAccess(file2, OperationType.OP_MODIFIED).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file2, OperationType.OP_READ).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file2, OperationType.OP_WRITE).length);
		
		/* test write, write => write */
		actionMgr.addFileAccess(task, file3, OperationType.OP_WRITE);
		actionMgr.addFileAccess(task, file3, OperationType.OP_WRITE);
		assertEquals(0, actionMgr.getActionsThatAccess(file3, OperationType.OP_READ).length);
		assertEquals(1, actionMgr.getActionsThatAccess(file3, OperationType.OP_WRITE).length);
		
		/* test write, modify => write */
		actionMgr.addFileAccess(task, file4, OperationType.OP_WRITE);
		actionMgr.addFileAccess(task, file4, OperationType.OP_MODIFIED);
		assertEquals(0, actionMgr.getActionsThatAccess(file4, OperationType.OP_MODIFIED).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file4, OperationType.OP_READ).length);
		assertEquals(1, actionMgr.getActionsThatAccess(file4, OperationType.OP_WRITE).length);

		/* test delete, read => read */
		actionMgr.addFileAccess(task, file5, OperationType.OP_DELETE);
		actionMgr.addFileAccess(task, file5, OperationType.OP_READ);
		assertEquals(0, actionMgr.getActionsThatAccess(file5, OperationType.OP_MODIFIED).length);
		assertEquals(1, actionMgr.getActionsThatAccess(file5, OperationType.OP_READ).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file5, OperationType.OP_WRITE).length);
		
		/* test delete, modify => modify */
		actionMgr.addFileAccess(task, file6, OperationType.OP_DELETE);
		actionMgr.addFileAccess(task, file6, OperationType.OP_MODIFIED);
		assertEquals(1, actionMgr.getActionsThatAccess(file6, OperationType.OP_MODIFIED).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file6, OperationType.OP_READ).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file6, OperationType.OP_WRITE).length);

		/* test read, write, delete => delete */
		actionMgr.addFileAccess(task, file7, OperationType.OP_READ);
		actionMgr.addFileAccess(task, file7, OperationType.OP_WRITE);
		actionMgr.addFileAccess(task, file7, OperationType.OP_DELETE);
		assertEquals(0, actionMgr.getActionsThatAccess(file7, OperationType.OP_MODIFIED).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file7, OperationType.OP_READ).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file7, OperationType.OP_WRITE).length);
		assertEquals(1, actionMgr.getActionsThatAccess(file7, OperationType.OP_DELETE).length);
		
		/* test delete, read, write => modify */
		actionMgr.addFileAccess(task, file8, OperationType.OP_DELETE);
		actionMgr.addFileAccess(task, file8, OperationType.OP_READ);
		actionMgr.addFileAccess(task, file8, OperationType.OP_WRITE);
		assertEquals(1, actionMgr.getActionsThatAccess(file8, OperationType.OP_MODIFIED).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file8, OperationType.OP_READ).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file8, OperationType.OP_WRITE).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file8, OperationType.OP_DELETE).length);
		
		/* test write, read, delete => temporary - and the file is deleted */
		assertEquals(file9, fileMgr.getPath("/file9"));
		actionMgr.addFileAccess(task, file9, OperationType.OP_WRITE);
		actionMgr.addFileAccess(task, file9, OperationType.OP_READ);
		actionMgr.addFileAccess(task, file9, OperationType.OP_DELETE);
		assertEquals(0, actionMgr.getActionsThatAccess(file9, OperationType.OP_MODIFIED).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file9, OperationType.OP_READ).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file9, OperationType.OP_WRITE).length);
		assertEquals(0, actionMgr.getActionsThatAccess(file9, OperationType.OP_DELETE).length);
		assertEquals(ErrorCode.BAD_PATH, fileMgr.getPath("/file9"));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.ActionMgr#getActionsThatAccess(int, OperationType)}.
	 */
	@Test
	public void testGetTasksThatAccess() {

		/* create some tasks */
		int task1 = actionMgr.addAction(rootTaskId, 0, "gcc -o clock.o clock.c");
		int task2 = actionMgr.addAction(rootTaskId, 0, "gcc -o banner.o banner.c");
		int task3 = actionMgr.addAction(rootTaskId, 0, "gcc -o mult.o mult.c");

		/* and a bunch of files that access those tasks */
		int file1 = fileMgr.addFile("/clock.o");
		int file2 = fileMgr.addFile("/clock.c");
		int file3 = fileMgr.addFile("/banner.o");
		int file4 = fileMgr.addFile("/banner.c");
		int file5 = fileMgr.addFile("/mult.o");
		int file6 = fileMgr.addFile("/mult.c");
		int file7 = fileMgr.addFile("/stdio.h");
		
		/* now register each task's file accesses */
		actionMgr.addFileAccess(task1, file1, OperationType.OP_WRITE);
		actionMgr.addFileAccess(task1, file2, OperationType.OP_READ);
		actionMgr.addFileAccess(task1, file7, OperationType.OP_READ);

		actionMgr.addFileAccess(task2, file3, OperationType.OP_WRITE);
		actionMgr.addFileAccess(task2, file4, OperationType.OP_READ);
		actionMgr.addFileAccess(task2, file7, OperationType.OP_READ);		

		actionMgr.addFileAccess(task3, file5, OperationType.OP_WRITE);
		actionMgr.addFileAccess(task3, file6, OperationType.OP_READ);
		actionMgr.addFileAccess(task3, file7, OperationType.OP_READ);		

		/* 
		 * Finally, fetch the list of tasks that each file is reference by.
		 * There are numerous combinations to test here.
		 */

		/* for clock.o */
		Integer results[] = actionMgr.getActionsThatAccess(file1, OperationType.OP_UNSPECIFIED);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { task1 }));

		results = actionMgr.getActionsThatAccess(file1, OperationType.OP_WRITE);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { task1 }));

		results = actionMgr.getActionsThatAccess(file1, OperationType.OP_READ);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] {} ));

		/* for clock.c */
		results = actionMgr.getActionsThatAccess(file2, OperationType.OP_UNSPECIFIED);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { task1 } ));

		results = actionMgr.getActionsThatAccess(file2, OperationType.OP_WRITE);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { } ));

		results = actionMgr.getActionsThatAccess(file2, OperationType.OP_READ);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { task1 } ));

		/* for mult.o */
		results = actionMgr.getActionsThatAccess(file5, OperationType.OP_UNSPECIFIED);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { task3 } ));

		results = actionMgr.getActionsThatAccess(file5, OperationType.OP_WRITE);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { task3 } ));

		results = actionMgr.getActionsThatAccess(file5, OperationType.OP_READ);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { } ));

		/* for stdio.h */
		results = actionMgr.getActionsThatAccess(file7, OperationType.OP_READ);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { task1, task2, task3 } ));

		results = actionMgr.getActionsThatAccess(file7, OperationType.OP_WRITE);
		assertTrue(CommonTestUtils.sortedArraysEqual(results, new Integer[] { } ));
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for {@link com.buildml.model.impl.ActionMgr#getActionsInDirectory(int)}.
	 */
	@Test
	public void testGetTasksInDirectory() {

		/* create a couple of directories */
		int dir1 = fileMgr.addDirectory("/dir1");
		int dir2 = fileMgr.addDirectory("/dir2");
		int dir3 = fileMgr.addDirectory("/dir2/dir3");
		
		/* create a number of tasks, each executing in one of those directories */
		int rootTask = actionMgr.getRootAction("root");
		int task11 = actionMgr.addAction(rootTask, dir1, "true");
		int task12 = actionMgr.addAction(rootTask, dir1, "true");
		int task21 = actionMgr.addAction(rootTask, dir2, "true");
		int task22 = actionMgr.addAction(rootTask, dir2, "true");
		int task23 = actionMgr.addAction(rootTask, dir2, "true");
		int task24 = actionMgr.addAction(rootTask, dir2, "true");
		int task13 = actionMgr.addAction(rootTask, dir1, "true");
	
		/* fetch the list of tasks that execute in the first directory */
		assertTrue(CommonTestUtils.sortedArraysEqual(actionMgr.getActionsInDirectory(dir1),
				new Integer[] {task11, task12, task13}));
		
		/* repeat for the second directory */
		assertTrue(CommonTestUtils.sortedArraysEqual(actionMgr.getActionsInDirectory(dir2),
				new Integer[] {task21, task22, task23, task24}));
		
		/* and the third should be empty */
		assertEquals(0, actionMgr.getActionsInDirectory(dir3).length);
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
			 * Each path "portion" can be 5-10 characters long. To ensure
			 * we get some degree of consistency in path names, we'll use
			 * the names listed in the portionNames variable.
			 */
			sb.delete(0, sb.length());
			for (int j = 0; j != numChars; j++) {
				sb.append((char)(r.nextInt(26) + 65));
			}
			
			//System.out.println("Adding " + sb);

			/* add the file name to the FileSpace */
			int taskId = actionMgr.addAction(rootTaskId, 0, sb.toString());
			
			/* now add files to this tasks */
			for (int k = 0; k != 200; k++) {
				actionMgr.addFileAccess(taskId, r.nextInt(100), OperationType.OP_READ);
			}
			
			/* now read the files that were added */
			actionMgr.getFilesAccessed(taskId, OperationType.OP_UNSPECIFIED);
		}
		bs.setFastAccessMode(false);
	}
	
	/*-------------------------------------------------------------------------------------*/

}
