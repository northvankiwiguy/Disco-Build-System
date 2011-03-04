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
 * Test methods for validating the FileSet class.
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestFileSet {
	
	private FileSet fs;
	private BuildStore bs;
	private FileNameSpaces fns;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper function for creating a new FileRecord and populating some of the fields.
	 * @param pathId The new FileRecord's pathId
	 * @param count The new FileRecord's count field
	 * @param size The new FileRecord's size field
	 * @return
	 */
	private FileRecord newFileRecord(int pathId, int count, int size) {
		FileRecord fr = new FileRecord();
		fr.pathId = pathId;
		fr.count = count;
		fr.size = size;
		return fr;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Setup() method, run before each test case is executed. Creates a new BuildStore
	 * and a new empty FileSet.
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		bs = CommonTestUtils.getEmptyBuildStore();
		fns = bs.getFileNameSpaces();
		fs = new FileSet(fns);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileSet#get(int)}.
	 */
	@Test
	public void testGet() {
		
		/* add a few FileRecord entries to the FileSet */
		fs.add(newFileRecord(1, 100, 1000));
		fs.add(newFileRecord(2, 200, 2000));
		fs.add(newFileRecord(2000, 2, 2000000));
		
		/* fetch FileRecord #1, and validate the fields */
		FileRecord fr = fs.get(1);
		assertEquals(1, fr.pathId);
		assertEquals(100, fr.count);
		assertEquals(1000, fr.size);

		/* same for FileRecord #2 */
		fr = fs.get(2);
		assertEquals(2, fr.pathId);
		assertEquals(200, fr.count);
		assertEquals(2000, fr.size);

		/* same for FileRecord #2000 */
		fr = fs.get(2000);
		assertEquals(2000, fr.pathId);
		assertEquals(2, fr.count);
		assertEquals(2000000, fr.size);
		
		/* this record doesn't exist - should return null */
		fr = fs.get(100);
		assertNull(fr);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileSet#isMember(int)}.
	 */
	@Test
	public void testIsMember() {
		
		/* add some records */
		fs.add(newFileRecord(134, 1, 232));
		fs.add(newFileRecord(256, 2, 76));
		fs.add(newFileRecord(23, 200, 111));
		
		/* check that the FileSet contains those records */
		assertTrue(fs.isMember(134));
		assertTrue(fs.isMember(256));
		assertTrue(fs.isMember(23));
		
		/* but doesn't contain records we didn't add */
		assertFalse(fs.isMember(34));
		assertFalse(fs.isMember(1));
		assertFalse(fs.isMember(2000));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileSet#remove(int)}.
	 */
	@Test
	public void testRemove() {
		
		/* add some records */
		fs.add(newFileRecord(34, 182, 2325));
		fs.add(newFileRecord(9275, 214, 7236));
		fs.add(newFileRecord(3643, 2210, 411));
		
		/* check they're present */
		assertTrue(fs.isMember(34));
		assertTrue(fs.isMember(9275));
		assertTrue(fs.isMember(3643));
		
		/* remove one of them, and check membership */
		fs.remove(9275);
		assertTrue(fs.isMember(34));
		assertFalse(fs.isMember(9275));
		assertTrue(fs.isMember(3643));

		/* remove another */
		fs.remove(34);
		assertFalse(fs.isMember(34));
		assertFalse(fs.isMember(9275));
		assertTrue(fs.isMember(3643));

		/* and another - should now be empty again */
		fs.remove(3643);
		assertFalse(fs.isMember(34));
		assertFalse(fs.isMember(9275));
		assertFalse(fs.isMember(3643));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileSet#iterator()}.
	 */
	@Test
	public void testIterator() {
		
		/* add a bunch of records */
		fs.add(newFileRecord(134, 1, 232));
		fs.add(newFileRecord(256, 2, 76));
		fs.add(newFileRecord(23, 200, 111));
		fs.add(newFileRecord(34, 182, 2325));
		fs.add(newFileRecord(9275, 214, 7236));
		fs.add(newFileRecord(3643, 2210, 411));

		/* check that the iterator returns all the members (not in any particular order) */
		ArrayList<Integer> returnedList = new ArrayList<Integer>();
		for (Integer pathId : fs) {
			returnedList.add(pathId);
		}
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] {23, 34, 134, 256, 3643, 9275}, 
				returnedList.toArray(new Integer[0])));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileSet#populateWithParents()}.
	 */
	@Test
	public void testPopulateWithParents() {
		
		/* create a bunch of files */
		int f1path = fns.addFile("/a/b/c/d/e/f1.c");
		int f2path = fns.addFile("/a/b/c/d/e/f2.c");
		int f3path = fns.addFile("/a/b/c/d/g/f3.c");
		int f4path = fns.addFile("/b/c/d/f4.c");
		int f5path = fns.addFile("/b/c/d/f5.c");
		
		/* this one won't be added, neither will its parents */
		int f6path = fns.addFile("/c/d/e/f6.c");

		/* add them to the file set */ 
		fs.add(newFileRecord(f1path, 0, 0));		
		fs.add(newFileRecord(f2path, 0, 0));
		fs.add(newFileRecord(f3path, 0, 0));
		fs.add(newFileRecord(f4path, 0, 0));
		fs.add(newFileRecord(f5path, 0, 0));

		/* check that they're added */
		assertTrue(fs.isMember(f1path));
		assertTrue(fs.isMember(f2path));
		assertTrue(fs.isMember(f3path));
		assertTrue(fs.isMember(f4path));
		assertTrue(fs.isMember(f5path));

		/* check that the parent paths aren't added yet */
		int parRoot = fns.getPath("/");
		int parA = fns.getPath("/a");
		int parAB = fns.getPath("/a/b");
		int parABC = fns.getPath("/a/b/c");
		int parABCD = fns.getPath("/a/b/c/d");
		int parABCDE = fns.getPath("/a/b/c/d/e");
		int parABCDG = fns.getPath("/a/b/c/d/g");
		int parB = fns.getPath("/b");
		int parBC = fns.getPath("/b/c");
		int parBCD = fns.getPath("/b/c/d");
		assertFalse(fs.isMember(parRoot));
		assertFalse(fs.isMember(parA));
		assertFalse(fs.isMember(parAB));
		assertFalse(fs.isMember(parABC));
		assertFalse(fs.isMember(parABCD));
		assertFalse(fs.isMember(parABCDE));
		assertFalse(fs.isMember(parABCDG));
		assertFalse(fs.isMember(parB));
		assertFalse(fs.isMember(parBC));
		assertFalse(fs.isMember(parBCD));

		/* these will never be added - make sure they're not there */
		int parCDE = fns.getPath("/c/d/e");
		int parCD = fns.getPath("/c/d");
		int parC = fns.getPath("/c");
		assertFalse(fs.isMember(parCDE));
		assertFalse(fs.isMember(parCD));
		assertFalse(fs.isMember(parC));
		
		/* populate the set with parents */
		fs.populateWithParents();
		
		/* now check again */
		assertTrue(fs.isMember(parRoot));
		assertTrue(fs.isMember(parA));
		assertTrue(fs.isMember(parAB));
		assertTrue(fs.isMember(parABC));
		assertTrue(fs.isMember(parABCD));
		assertTrue(fs.isMember(parABCDE));
		assertTrue(fs.isMember(parABCDG));
		assertTrue(fs.isMember(parB));
		assertTrue(fs.isMember(parBC));
		assertTrue(fs.isMember(parBCD));
		
		/* these still shouldn't be added */
		assertFalse(fs.isMember(parCDE));
		assertFalse(fs.isMember(parCD));
		assertFalse(fs.isMember(parC));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileSet#size()}.
	 */
	@Test
	public void testSize() {
		
		/* create a bunch of files */
		int f1path = fns.addFile("/a/b/c/d/e/f1.c");
		int f2path = fns.addFile("/a/b/c/d/e/f2.c");
		int f3path = fns.addFile("/a/b/c/d/g/f3.c");
		int f4path = fns.addFile("/b/c/d/f4.c");
		int f5path = fns.addFile("/b/c/d/f5.c");		
		int f6path = fns.addFile("/c/d/e/f6.c");

		/* add them to the file set, testing the size as we go along */ 
		assertEquals(0, fs.size());
		fs.add(newFileRecord(f1path, 0, 0));		
		assertEquals(1, fs.size());
		fs.add(newFileRecord(f2path, 0, 0));
		assertEquals(2, fs.size());
		fs.add(newFileRecord(f3path, 0, 0));
		assertEquals(3, fs.size());
		fs.add(newFileRecord(f4path, 0, 0));
		assertEquals(4, fs.size());
		fs.add(newFileRecord(f5path, 0, 0));
		assertEquals(5, fs.size());
		fs.add(newFileRecord(f6path, 0, 0));
		assertEquals(6, fs.size());
		
		/* populate with parents - should add 13 new paths */
		fs.populateWithParents();
		assertEquals(19, fs.size());
		
		/* now remove some of the entries, testing the size as we go */
		fs.remove(f1path);		
		assertEquals(18, fs.size());
		fs.remove(f2path);
		assertEquals(17, fs.size());
		fs.remove(f3path);
		assertEquals(16, fs.size());
		fs.remove(f4path);
		assertEquals(15, fs.size());
		fs.remove(f5path);
		assertEquals(14, fs.size());
		fs.remove(f6path);
		assertEquals(13, fs.size());
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/*
	 * Test the scalability of the FileSet data structure.
	 */
	@Test
	public void testScalability() throws Exception {

		/* fill the FileSet with every third path ID */
		for (int i = 0; i != 1000000; i++) {
			fs.add(newFileRecord(i * 3, 0, 0));
		}
		
		/* now test that everything that should be there is in place, but not the others */
		for (int i = 0; i != 1000000; i++) {
			assertTrue(fs.isMember(i * 3));
			assertFalse(fs.isMember(i * 3 + 1));
			assertFalse(fs.isMember(i * 3 + 2));
			
			assertEquals(i * 3, fs.get(i * 3).pathId);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
}
