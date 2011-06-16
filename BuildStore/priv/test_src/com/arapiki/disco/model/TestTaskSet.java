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

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

/**
 * Test methods for validating the TaskSet class. These test are
 * very simplistic, since the functionality is largely shared with FileSet,
 * which is tested extensively.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestTaskSet {
	
	private TaskSet ts;
	private BuildStore bs;
	private BuildTasks bts;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper function for creating a new TaskRecord and populating the id field.
	 * @param id the TaskRecord's id
	 * @return a new TaskRecord
	 */
	private TaskRecord newTaskRecord(int taskId) {
		TaskRecord tr = new TaskRecord(taskId);
		return tr;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Setup() method, run before each test case is executed. Creates a new BuildStore
	 * and a new empty TaskSet.
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		bs = CommonTestUtils.getEmptyBuildStore();
		bts = bs.getBuildTasks();
		ts = new TaskSet(bts);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.TaskSet#get(int)}.
	 */
	@Test
	public void testGet() {
		
		/* add a couple of TaskRecord entries to the TaskSet */
		ts.add(newTaskRecord(1));
		ts.add(newTaskRecord(2000));
		
		/* fetch TaskRecord #1, and validate the fields */
		TaskRecord fr = ts.get(1);
		assertEquals(1, fr.getId());

		/* same for TaskRecord #2000 */
		fr = ts.get(2000);
		assertEquals(2000, fr.getId());
		
		/* this record doesn't exist - should return null */
		fr = ts.get(100);
		assertNull(fr);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.TaskSet#isMember(int)}.
	 */
	@Test
	public void testIsMember() {
		
		/* add some records */
		ts.add(newTaskRecord(134));
		ts.add(newTaskRecord(256));
		ts.add(newTaskRecord(23));
		
		/* check that the TaskSet contains those records */
		assertTrue(ts.isMember(134));
		assertTrue(ts.isMember(256));
		assertTrue(ts.isMember(23));
		
		/* but doesn't contain records we didn't add */
		assertFalse(ts.isMember(34));
		assertFalse(ts.isMember(1));
		assertFalse(ts.isMember(2000));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.TaskSet#remove(int)}.
	 */
	@Test
	public void testRemove() {
		
		/* add some records */
		ts.add(newTaskRecord(34));
		ts.add(newTaskRecord(9275));
		ts.add(newTaskRecord(3643));
		
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
	 * Test method for {@link com.arapiki.disco.model.TaskSet#iterator()}.
	 */
	@Test
	public void testIterator() {
		
		/* add a bunch of records */
		ts.add(newTaskRecord(134));
		ts.add(newTaskRecord(256));
		ts.add(newTaskRecord(23));
		ts.add(newTaskRecord(34));
		ts.add(newTaskRecord(9275));
		ts.add(newTaskRecord(3643));

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
	 * Test method for {@link com.arapiki.disco.model.TaskSet#populateWithParents()}.
	 */
	@Test
	public void testPopulateWithParents() {
		
		/* create a bunch of tasks in a tree structure */
		int task1 = bts.addBuildTask(bts.getRootTask(""), 0, "top command");
		int task2 = bts.addBuildTask(task1, 0, "second command");
		int task3 = bts.addBuildTask(task1, 0, "second command as well");
		int task4 = bts.addBuildTask(task3, 0, "third command");
		int task5 = bts.addBuildTask(task4, 0, "fourth command");
		int task6 = bts.addBuildTask(task2, 0, "second command's child");

		/* add only one of them to the TaskSet */
		ts.add(newTaskRecord(task6));		

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
		ts.add(newTaskRecord(task4));		
		assertEquals(5, ts.size());
		
		/* populate it's parents - this adds task3 (task1 is already in the set) */
		ts.populateWithParents();
		assertEquals(6, ts.size());
		assertTrue(ts.isMember(task3));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.TaskSet#mergeSet()}.
	 */
	@Test
	public void testMergeSet() throws Exception {

		/* create and populate a new TaskSet to merge in */
		TaskSet mainTaskSet = new TaskSet(bts);
		mainTaskSet.add(newTaskRecord(1));
		mainTaskSet.add(newTaskRecord(10));
		mainTaskSet.add(newTaskRecord(100));
		
		/* merge it in */
		ts.mergeSet(mainTaskSet);

		/* check the content of our TaskSet */
		assertEquals(3, ts.size());
		assertTrue(ts.isMember(1));
		assertTrue(ts.isMember(10));
		assertTrue(ts.isMember(100));

		/* create a new set, with new content to merge in */
		mainTaskSet = new TaskSet(bts);
		mainTaskSet.add(newTaskRecord(23));
		mainTaskSet.add(newTaskRecord(56));
		mainTaskSet.add(newTaskRecord(100));
		
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
	 * Test method for {@link com.arapiki.disco.model.TaskSet#size()}.
	 */
	@Test
	public void testSize() {
		
		/* create a bunch of tasks */
		int root = bts.getRootTask("");
		int task1 = bts.addBuildTask(root, 0, "");
		int task2 = bts.addBuildTask(root, 0, "");
		int task3 = bts.addBuildTask(root, 0, "");
		
		/* add them to the TaskSet, testing the size as we go along */ 
		assertEquals(0, ts.size());
		ts.add(newTaskRecord(task1));		
		assertEquals(1, ts.size());
		ts.add(newTaskRecord(task2));
		assertEquals(2, ts.size());
		ts.add(newTaskRecord(task3));
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

}
