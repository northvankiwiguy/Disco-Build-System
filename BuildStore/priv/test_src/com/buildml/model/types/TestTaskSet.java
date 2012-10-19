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

package com.buildml.model.types;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import com.buildml.model.CommonTestUtils;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.model.types.ActionSet;
import com.buildml.utils.errors.ErrorCode;

/**
 * Test methods for validating the TaskSet class. These test are
 * very simplistic, since the functionality is largely shared with FileSet,
 * which is tested extensively.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestTaskSet {
	
	/** Our test TaskSet object */
	private ActionSet ts;
	
	/** Our test BuildStore object */
	private IBuildStore bs;

	/** Our test ActionMgr object */
	private IActionMgr actionMgr;
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Setup() method, run before each test case is executed. Creates a new BuildStore
	 * and a new empty TaskSet.
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		bs = CommonTestUtils.getEmptyBuildStore();
		actionMgr = bs.getActionMgr();
		ts = new ActionSet(actionMgr);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.types.ActionSet#isMember(int)}.
	 */
	@Test
	public void testIsMember() {
		
		/* add some elements */
		ts.add(134);
		ts.add(256);
		ts.add(23);
		
		/* check that the TaskSet contains those elements */
		assertTrue(ts.isMember(134));
		assertTrue(ts.isMember(256));
		assertTrue(ts.isMember(23));
		
		/* but doesn't contain elements we didn't add */
		assertFalse(ts.isMember(34));
		assertFalse(ts.isMember(1));
		assertFalse(ts.isMember(2000));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.types.ActionSet#remove(int)}.
	 */
	@Test
	public void testRemove() {
		
		/* add some elements */
		ts.add(34);
		ts.add(9275);
		ts.add(3643);
		
		/* check they're present */
		assertTrue(ts.isMember(34));
		assertTrue(ts.isMember(9275));
		assertTrue(ts.isMember(3643));
		
		/* remove one of them, and check membership */
		ts.remove(9275);
		assertTrue(ts.isMember(34));
		assertFalse(ts.isMember(9275));
		assertTrue(ts.isMember(3643));

		/* remove another */
		ts.remove(34);
		assertFalse(ts.isMember(34));
		assertFalse(ts.isMember(9275));
		assertTrue(ts.isMember(3643));

		/* and another - should now be empty again */
		ts.remove(3643);
		assertFalse(ts.isMember(34));
		assertFalse(ts.isMember(9275));
		assertFalse(ts.isMember(3643));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.types.ActionSet#iterator()}.
	 */
	@Test
	public void testIterator() {
		
		/* add a bunch of elements */
		ts.add(134);
		ts.add(256);
		ts.add(23);
		ts.add(34);
		ts.add(9275);
		ts.add(3643);

		/* check that the iterator returns all the members (not in any particular order) */
		ArrayList<Integer> returnedList = new ArrayList<Integer>();
		for (Integer pathId : ts) {
			returnedList.add(pathId);
		}
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] {23, 34, 134, 256, 3643, 9275}, 
				returnedList.toArray(new Integer[0])));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.types.ActionSet#populateWithParents()}.
	 */
	@Test
	public void testPopulateWithParents() {
		
		/* create a bunch of tasks in a tree structure */
		int task1 = actionMgr.addAction(actionMgr.getRootAction(""), 0, "top command");
		int task2 = actionMgr.addAction(task1, 0, "second command");
		int task3 = actionMgr.addAction(task1, 0, "second command as well");
		int task4 = actionMgr.addAction(task3, 0, "third command");
		actionMgr.addAction(task4, 0, "fourth command");
		int task6 = actionMgr.addAction(task2, 0, "second command's child");

		/* add only one of them to the TaskSet */
		ts.add(task6);		

		/* check that it's added */
		assertEquals(1, ts.size());
		assertTrue(ts.isMember(task6));
		
		/* populate the set with parents - this adds task2 and task1, and the root task */
		ts.populateWithParents();
		
		/* now check again */
		assertEquals(4, ts.size());
		assertTrue(ts.isMember(task6));
		assertTrue(ts.isMember(task2));
		assertTrue(ts.isMember(task1));
		
		/* now add another task */
		ts.add(task4);		
		assertEquals(5, ts.size());
		
		/* populate it's parents - this adds task3 (task1 is already in the set) */
		ts.populateWithParents();
		assertEquals(6, ts.size());
		assertTrue(ts.isMember(task3));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.types.ActionSet#mergeSet(ActionSet)}.
	 */
	@Test
	public void testMergeSet() {

		/* create and populate a new TaskSet to merge in */
		ActionSet mainTaskSet = new ActionSet(actionMgr);
		mainTaskSet.add(1);
		mainTaskSet.add(10);
		mainTaskSet.add(100);
		
		/* merge it in */
		ts.mergeSet(mainTaskSet);

		/* check the content of our TaskSet */
		assertEquals(3, ts.size());
		assertTrue(ts.isMember(1));
		assertTrue(ts.isMember(10));
		assertTrue(ts.isMember(100));

		/* create a new set, with new content to merge in */
		mainTaskSet = new ActionSet(actionMgr);
		mainTaskSet.add(23);
		mainTaskSet.add(56);
		mainTaskSet.add(100);
		
		/* merge it in */
		ts.mergeSet(mainTaskSet);
		
		/* check the content of our TaskSet */
		assertEquals(5, ts.size());
		assertTrue(ts.isMember(1));
		assertTrue(ts.isMember(10));
		assertTrue(ts.isMember(23));
		assertTrue(ts.isMember(56));
		assertTrue(ts.isMember(100));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.types.ActionSet#size()}.
	 */
	@Test
	public void testSize() {
		
		/* create a bunch of tasks */
		int root = actionMgr.getRootAction("");
		int task1 = actionMgr.addAction(root, 0, "");
		int task2 = actionMgr.addAction(root, 0, "");
		int task3 = actionMgr.addAction(root, 0, "");
		
		/* add them to the TaskSet, testing the size as we go along */ 
		assertEquals(0, ts.size());
		ts.add(task1);		
		assertEquals(1, ts.size());
		ts.add(task2);
		assertEquals(2, ts.size());
		ts.add(task3);
		assertEquals(3, ts.size());
				
		/* now remove some of the entries, testing the size as we go */
		ts.remove(task1);		
		assertEquals(2, ts.size());
		ts.remove(task2);
		assertEquals(1, ts.size());
		ts.remove(task3);
		assertEquals(0, ts.size());
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.types.ActionSet#populateWithActions(String[])}.
	 */
	@Test
	public void testPopulateWithTasks() {
		
		/* add some tasks in a tree hierarchy */
		int root = actionMgr.getRootAction("");
		int taskA = actionMgr.addAction(root, 0, "top-level-task-A");
		int taskA1 = actionMgr.addAction(taskA, 0, "second-level-task-under-A");
		int taskA2 = actionMgr.addAction(taskA, 0, "second-level-task-under-A");
		int taskA3 = actionMgr.addAction(taskA, 0, "second-level-task-under-A");
		int taskA31 = actionMgr.addAction(taskA3, 0, "third-level-task-under-A3");
		int taskA32 = actionMgr.addAction(taskA3, 0, "third-level-task-under-A3");
		int taskA321 = actionMgr.addAction(taskA32, 0, "fourth-level-task-under-A32");
		actionMgr.addAction(taskA, 0, "second-level-task-under-A");
		int taskB = actionMgr.addAction(root, 0, "top-level-task-B");
		actionMgr.addAction(root, 0, "top-level-task-C");
	
		/* populate with an empty specification string array*/
		ts.populateWithActions(new String[0]);
		assertEquals(0, ts.size());
		
		/* populate with a single task, without any of its descendants. Format is "<taskA3>" */
		assertEquals(ErrorCode.OK, ts.populateWithActions(new String[] { Integer.valueOf(taskA3).toString()}));
		assertEquals(1, ts.size());
		assertTrue(ts.isMember(taskA3));

		/* populate with a complete subtree. Format is "<taskA3>/" */
		ts = new ActionSet(actionMgr);
		assertEquals(ErrorCode.OK, ts.populateWithActions(new String[] { Integer.valueOf(taskA3).toString() + "/" }));
		assertEquals(4, ts.size());
		assertTrue(ts.isMember(taskA3));
		assertTrue(ts.isMember(taskA31));
		assertTrue(ts.isMember(taskA32));
		assertTrue(ts.isMember(taskA321));
		
		/* populate with a complete subtree, of depth 2. Format is "<taskA3>/2" */
		ts = new ActionSet(actionMgr);
		assertEquals(ErrorCode.OK, ts.populateWithActions(new String[] { Integer.valueOf(taskA3).toString() + "/2" }));
		assertEquals(3, ts.size());
		assertTrue(ts.isMember(taskA3));
		assertTrue(ts.isMember(taskA31));
		assertTrue(ts.isMember(taskA32));
		
		/* similar complete subtree of depth 2, but with different tasks. Format is "<taskA>/2" */
		ts = new ActionSet(actionMgr);
		assertEquals(ErrorCode.OK, ts.populateWithActions(new String[] { Integer.valueOf(taskA).toString() + "/2" }));
		assertEquals(5, ts.size());
		assertTrue(ts.isMember(taskA));
		assertTrue(ts.isMember(taskA1));
		assertTrue(ts.isMember(taskA2));
		assertTrue(ts.isMember(taskA3));
	
		/* populate the full tree, down four levels (this excludes only one task) */
		ts = new ActionSet(actionMgr);
		assertEquals(ErrorCode.OK, ts.populateWithActions(new String[] { Integer.valueOf(root).toString() + "/4" }));
		assertEquals(10, ts.size());
		assertFalse(ts.isMember(taskA321));
		
		/* populate with two different tasks */
		ts = new ActionSet(actionMgr);
		assertEquals(ErrorCode.OK, ts.populateWithActions(new String[] { Integer.valueOf(taskA1).toString(),
					Integer.valueOf(taskB).toString()}));
		assertEquals(2, ts.size());
		assertTrue(ts.isMember(taskA1));
		assertTrue(ts.isMember(taskB));
		
		/* populate the full tree (all levels), then remove a subtree (taskA32 and below) */
		ts = new ActionSet(actionMgr);
		assertEquals(ErrorCode.OK, ts.populateWithActions(new String[] { Integer.valueOf(root).toString() + "/",
				"-" + Integer.valueOf(taskA32).toString() + "/" }));
		assertEquals(9, ts.size());
		assertFalse(ts.isMember(taskA32));
		assertFalse(ts.isMember(taskA321));
		
		/* create a package, add a couple of tasks to it, then query the package */
		IPackageMgr pkgMgr = bs.getPackageMgr();
		int fooPkgId = pkgMgr.addPackage("foo");
		assertEquals(ErrorCode.OK, pkgMgr.setActionPackage(taskA1, fooPkgId));
		assertEquals(ErrorCode.OK, pkgMgr.setActionPackage(taskA2, fooPkgId));
		
		ts = new ActionSet(actionMgr);
		assertEquals(ErrorCode.OK, ts.populateWithActions(new String[] { "%pkg/foo" }));
		assertEquals(2, ts.size());
		assertTrue(ts.isMember(taskA1));
		assertTrue(ts.isMember(taskA2));

		ts = new ActionSet(actionMgr);
		assertEquals(ErrorCode.OK, ts.populateWithActions(new String[] { "%not-pkg/foo" }));
		assertEquals(8, ts.size());
		assertFalse(ts.isMember(taskA1));
		assertFalse(ts.isMember(taskA2));

		/* test invalid syntax */
		assertEquals(ErrorCode.BAD_VALUE, ts.populateWithActions(new String[] { "+123" }));
		assertEquals(ErrorCode.BAD_VALUE, ts.populateWithActions(new String[] { "X" }));
		assertEquals(ErrorCode.BAD_VALUE, ts.populateWithActions(new String[] { "123-" }));
		assertEquals(ErrorCode.BAD_VALUE, ts.populateWithActions(new String[] { "123/foo" }));
		assertEquals(ErrorCode.BAD_VALUE, ts.populateWithActions(new String[] { "/1" }));
		assertEquals(ErrorCode.BAD_VALUE, ts.populateWithActions(new String[] { "1//" }));
		assertEquals(ErrorCode.BAD_VALUE, ts.populateWithActions(new String[] { "%c/pkg" }));
		assertEquals(ErrorCode.BAD_VALUE, ts.populateWithActions(new String[] { "%c/foo/private" }));
		assertEquals(ErrorCode.BAD_VALUE, ts.populateWithActions(new String[] { "%c" }));
		assertEquals(ErrorCode.BAD_VALUE, ts.populateWithActions(new String[] { "%nc" }));
		assertEquals(ErrorCode.BAD_VALUE, ts.populateWithActions(new String[] { "%c/" }));
		assertEquals(ErrorCode.BAD_VALUE, ts.populateWithActions(new String[] { "%nc/" }));
		assertEquals(ErrorCode.BAD_VALUE, ts.populateWithActions(new String[] { "%badvalue/" }));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.types.ActionSet#populateWithActions(String[])}.
	 */
	@Test
	public void testMatchingCommandNames() {

		int task1 = actionMgr.addAction(0, 0, "a task is a task, of : course, of course");
		int task2 = actionMgr.addAction(0, 0, "another task");
		int task3 = actionMgr.addAction(0, 0, "gcc -c -o foo.o foo.c");
		int task4 = actionMgr.addAction(0, 0, "gcc -c -o bah.o bah.c");
		int task5 = actionMgr.addAction(0, 0, "gcc -c -o bling.o bling.c");

		/* look for gcc commands */
		ActionSet results = new ActionSet(actionMgr);
		results.populateWithActions(new String[] {"%match/gcc *"});
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {task3, task4, task5}));
		
		/* same, but use %m instead of %match */
		results = new ActionSet(actionMgr);
		results.populateWithActions(new String[] {"%m/gcc *"});
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {task3, task4, task5}));
		
		/* look for the word "task" */
		results = new ActionSet(actionMgr);
		results.populateWithActions(new String[] {"%match/task"});
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {task1, task2}));
		
		/* try with an empty pattern */
		results = new ActionSet(actionMgr);
		results.populateWithActions(new String[] {"%m/"});
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {task1, task2, task3, task4, task5}));
	
		/* try with a single * */
		results = new ActionSet(actionMgr);
		results.populateWithActions(new String[] {"%m/*"});
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {task1, task2, task3, task4, task5}));
		
		/* try to match an embedded : */
		results = new ActionSet(actionMgr);
		results.populateWithActions(new String[] {"%match/*of \\: course*"});
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {task1}));
	}
	
	/*-------------------------------------------------------------------------------------*/	
}
