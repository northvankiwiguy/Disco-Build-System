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
 * Test methods for validating the ActionSet class. These test are
 * very simplistic, since the functionality is largely shared with FileSet,
 * which is tested extensively.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestActionSet {
	
	/** Our test ActionSet object */
	private ActionSet ts;
	
	/** Our test BuildStore object */
	private IBuildStore bs;

	/** Our test ActionMgr object */
	private IActionMgr actionMgr;
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Setup() method, run before each test case is executed. Creates a new BuildStore
	 * and a new empty ActionSet.
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
		
		/* check that the ActionSet contains those elements */
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
		
		/* create a bunch of actions in a tree structure */
		int action1 = actionMgr.addAction(actionMgr.getRootAction(""), 0, "top command");
		int action2 = actionMgr.addAction(action1, 0, "second command");
		int action3 = actionMgr.addAction(action1, 0, "second command as well");
		int action4 = actionMgr.addAction(action3, 0, "third command");
		actionMgr.addAction(action4, 0, "fourth command");
		int action6 = actionMgr.addAction(action2, 0, "second command's child");

		/* add only one of them to the ActionSet */
		ts.add(action6);		

		/* check that it's added */
		assertEquals(1, ts.size());
		assertTrue(ts.isMember(action6));
		
		/* populate the set with parents - this adds action2 and action1, and the root action */
		ts.populateWithParents();
		
		/* now check again */
		assertEquals(4, ts.size());
		assertTrue(ts.isMember(action6));
		assertTrue(ts.isMember(action2));
		assertTrue(ts.isMember(action1));
		
		/* now add another action */
		ts.add(action4);		
		assertEquals(5, ts.size());
		
		/* populate it's parents - this adds action3 (action1 is already in the set) */
		ts.populateWithParents();
		assertEquals(6, ts.size());
		assertTrue(ts.isMember(action3));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.types.ActionSet#mergeSet(ActionSet)}.
	 */
	@Test
	public void testMergeSet() {

		/* create and populate a new ActionSet to merge in */
		ActionSet mainActionSet = new ActionSet(actionMgr);
		mainActionSet.add(1);
		mainActionSet.add(10);
		mainActionSet.add(100);
		
		/* merge it in */
		ts.mergeSet(mainActionSet);

		/* check the content of our ActionSet */
		assertEquals(3, ts.size());
		assertTrue(ts.isMember(1));
		assertTrue(ts.isMember(10));
		assertTrue(ts.isMember(100));

		/* create a new set, with new content to merge in */
		mainActionSet = new ActionSet(actionMgr);
		mainActionSet.add(23);
		mainActionSet.add(56);
		mainActionSet.add(100);
		
		/* merge it in */
		ts.mergeSet(mainActionSet);
		
		/* check the content of our ActionSet */
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
		
		/* create a bunch of actions */
		int root = actionMgr.getRootAction("");
		int action1 = actionMgr.addAction(root, 0, "");
		int action2 = actionMgr.addAction(root, 0, "");
		int action3 = actionMgr.addAction(root, 0, "");
		
		/* add them to the ActionSet, testing the size as we go along */ 
		assertEquals(0, ts.size());
		ts.add(action1);		
		assertEquals(1, ts.size());
		ts.add(action2);
		assertEquals(2, ts.size());
		ts.add(action3);
		assertEquals(3, ts.size());
				
		/* now remove some of the entries, testing the size as we go */
		ts.remove(action1);		
		assertEquals(2, ts.size());
		ts.remove(action2);
		assertEquals(1, ts.size());
		ts.remove(action3);
		assertEquals(0, ts.size());
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.types.ActionSet#populateWithActions(String[])}.
	 */
	@Test
	public void testPopulateWithActions() {
		
		/* add some actions in a tree hierarchy */
		int root = actionMgr.getRootAction("");
		int actionA = actionMgr.addAction(root, 0, "top-level-action-A");
		int actionA1 = actionMgr.addAction(actionA, 0, "second-level-action-under-A");
		int actionA2 = actionMgr.addAction(actionA, 0, "second-level-action-under-A");
		int actionA3 = actionMgr.addAction(actionA, 0, "second-level-action-under-A");
		int actionA31 = actionMgr.addAction(actionA3, 0, "third-level-action-under-A3");
		int actionA32 = actionMgr.addAction(actionA3, 0, "third-level-action-under-A3");
		int actionA321 = actionMgr.addAction(actionA32, 0, "fourth-level-action-under-A32");
		actionMgr.addAction(actionA, 0, "second-level-action-under-A");
		int actionB = actionMgr.addAction(root, 0, "top-level-action-B");
		actionMgr.addAction(root, 0, "top-level-action-C");
	
		/* populate with an empty specification string array*/
		ts.populateWithActions(new String[0]);
		assertEquals(0, ts.size());
		
		/* populate with a single action, without any of its descendants. Format is "<actionA3>" */
		assertEquals(ErrorCode.OK, ts.populateWithActions(new String[] { Integer.valueOf(actionA3).toString()}));
		assertEquals(1, ts.size());
		assertTrue(ts.isMember(actionA3));

		/* populate with a complete subtree. Format is "<actionA3>/" */
		ts = new ActionSet(actionMgr);
		assertEquals(ErrorCode.OK, ts.populateWithActions(new String[] { Integer.valueOf(actionA3).toString() + "/" }));
		assertEquals(4, ts.size());
		assertTrue(ts.isMember(actionA3));
		assertTrue(ts.isMember(actionA31));
		assertTrue(ts.isMember(actionA32));
		assertTrue(ts.isMember(actionA321));
		
		/* populate with a complete subtree, of depth 2. Format is "<actionA3>/2" */
		ts = new ActionSet(actionMgr);
		assertEquals(ErrorCode.OK, ts.populateWithActions(new String[] { Integer.valueOf(actionA3).toString() + "/2" }));
		assertEquals(3, ts.size());
		assertTrue(ts.isMember(actionA3));
		assertTrue(ts.isMember(actionA31));
		assertTrue(ts.isMember(actionA32));
		
		/* similar complete subtree of depth 2, but with different actions. Format is "<actionA>/2" */
		ts = new ActionSet(actionMgr);
		assertEquals(ErrorCode.OK, ts.populateWithActions(new String[] { Integer.valueOf(actionA).toString() + "/2" }));
		assertEquals(5, ts.size());
		assertTrue(ts.isMember(actionA));
		assertTrue(ts.isMember(actionA1));
		assertTrue(ts.isMember(actionA2));
		assertTrue(ts.isMember(actionA3));
	
		/* populate the full tree, down four levels (this excludes only one action) */
		ts = new ActionSet(actionMgr);
		assertEquals(ErrorCode.OK, ts.populateWithActions(new String[] { Integer.valueOf(root).toString() + "/4" }));
		assertEquals(10, ts.size());
		assertFalse(ts.isMember(actionA321));
		
		/* populate with two different actions */
		ts = new ActionSet(actionMgr);
		assertEquals(ErrorCode.OK, ts.populateWithActions(new String[] { Integer.valueOf(actionA1).toString(),
					Integer.valueOf(actionB).toString()}));
		assertEquals(2, ts.size());
		assertTrue(ts.isMember(actionA1));
		assertTrue(ts.isMember(actionB));
		
		/* populate the full tree (all levels), then remove a subtree (actionA32 and below) */
		ts = new ActionSet(actionMgr);
		assertEquals(ErrorCode.OK, ts.populateWithActions(new String[] { Integer.valueOf(root).toString() + "/",
				"-" + Integer.valueOf(actionA32).toString() + "/" }));
		assertEquals(9, ts.size());
		assertFalse(ts.isMember(actionA32));
		assertFalse(ts.isMember(actionA321));
		
		/* create a package, add a couple of actions to it, then query the package */
		IPackageMgr pkgMgr = bs.getPackageMgr();
		int fooPkgId = pkgMgr.addPackage("foo");
		assertEquals(ErrorCode.OK, pkgMgr.setActionPackage(actionA1, fooPkgId));
		assertEquals(ErrorCode.OK, pkgMgr.setActionPackage(actionA2, fooPkgId));
		
		ts = new ActionSet(actionMgr);
		assertEquals(ErrorCode.OK, ts.populateWithActions(new String[] { "%pkg/foo" }));
		assertEquals(2, ts.size());
		assertTrue(ts.isMember(actionA1));
		assertTrue(ts.isMember(actionA2));

		ts = new ActionSet(actionMgr);
		assertEquals(ErrorCode.OK, ts.populateWithActions(new String[] { "%not-pkg/foo" }));
		assertEquals(8, ts.size());
		assertFalse(ts.isMember(actionA1));
		assertFalse(ts.isMember(actionA2));

		/* test invalid syntax */
		assertEquals(ErrorCode.BAD_VALUE, ts.populateWithActions(new String[] { "*123" }));
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

		int action1 = actionMgr.addAction(0, 0, "a action is a action, of : course, of course");
		int action2 = actionMgr.addAction(0, 0, "another action");
		int action3 = actionMgr.addAction(0, 0, "gcc -c -o foo.o foo.c");
		int action4 = actionMgr.addAction(0, 0, "gcc -c -o bah.o bah.c");
		int action5 = actionMgr.addAction(0, 0, "gcc -c -o bling.o bling.c");

		/* look for gcc commands */
		ActionSet results = new ActionSet(actionMgr);
		results.populateWithActions(new String[] {"%match/gcc *"});
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action3, action4, action5}));
		
		/* same, but use %m instead of %match */
		results = new ActionSet(actionMgr);
		results.populateWithActions(new String[] {"%m/gcc *"});
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action3, action4, action5}));
		
		/* look for the word "action" */
		results = new ActionSet(actionMgr);
		results.populateWithActions(new String[] {"%match/action"});
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action2}));
		
		/* try with an empty pattern */
		results = new ActionSet(actionMgr);
		results.populateWithActions(new String[] {"%m/"});
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action2, action3, action4, action5}));
	
		/* try with a single * */
		results = new ActionSet(actionMgr);
		results.populateWithActions(new String[] {"%m/*"});
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action2, action3, action4, action5}));
		
		/* try to match an embedded : */
		results = new ActionSet(actionMgr);
		results.populateWithActions(new String[] {"%match/*of \\: course*"});
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1}));
	}
	
	/*-------------------------------------------------------------------------------------*/	
}
