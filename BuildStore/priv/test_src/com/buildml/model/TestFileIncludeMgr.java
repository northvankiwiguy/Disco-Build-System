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


/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestFileIncludeMgr {

	/*-------------------------------------------------------------------------------------*/
	
	/** our BuildStore, used for testing */
	private IBuildStore bs;
	
	/** our FileDependencies object, used for testing */
	private IFileIncludeMgr fileIncludeMgr;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		bs = CommonTestUtils.getEmptyBuildStore();
		
		/* fetch the associated FileDependencies */
		fileIncludeMgr = bs.getFileIncludeMgr();
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for {@link com.buildml.model.IFileIncludeMgr#addFileIncludes(int, int)}.
	 */
	@Test
	public void testAddFileIncludes() {

		/* without adding anything, check that the count is 0 */
		assertEquals(0, fileIncludeMgr.getFileIncludesCount(1, 2));
		
		/* add something once */
		fileIncludeMgr.addFileIncludes(1, 2);
		assertEquals(1, fileIncludeMgr.getFileIncludesCount(1, 2));

		/* add something again */
		fileIncludeMgr.addFileIncludes(1, 2);
		assertEquals(2, fileIncludeMgr.getFileIncludesCount(1, 2));

		/* add something different */
		fileIncludeMgr.addFileIncludes(1, 3);
		assertEquals(1, fileIncludeMgr.getFileIncludesCount(1, 3));
		assertEquals(2, fileIncludeMgr.getFileIncludesCount(1, 2));
		assertEquals(0, fileIncludeMgr.getFileIncludesCount(1, 4));
	
		/* add another different one */
		fileIncludeMgr.addFileIncludes(2, 2);
		assertEquals(1, fileIncludeMgr.getFileIncludesCount(1, 3));
		assertEquals(2, fileIncludeMgr.getFileIncludesCount(1, 2));
		assertEquals(0, fileIncludeMgr.getFileIncludesCount(1, 4));
		assertEquals(1, fileIncludeMgr.getFileIncludesCount(2, 2));	
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for {@link com.buildml.model.IFileIncludeMgr#getTotalFileIncludedCount(int)}.
	 */
	@Test
	public void testGetTotalFileIncludesCount() {

		/* before ever being accessed, the count should be 0 */
		assertEquals(0, fileIncludeMgr.getTotalFileIncludedCount(2));
		
		/* access it once */
		fileIncludeMgr.addFileIncludes(1, 2);
		assertEquals(1, fileIncludeMgr.getTotalFileIncludedCount(2));

		/* and again, from the same parent file */
		fileIncludeMgr.addFileIncludes(1, 2);
		assertEquals(2, fileIncludeMgr.getTotalFileIncludedCount(2));

		/* now again from a different parent file */
		fileIncludeMgr.addFileIncludes(3, 2);
		assertEquals(3, fileIncludeMgr.getTotalFileIncludedCount(2));

		/* accessing a different file shouldn't change the count */
		fileIncludeMgr.addFileIncludes(1, 3);
		assertEquals(3, fileIncludeMgr.getTotalFileIncludedCount(2));
	}
	
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for {@link com.buildml.model.IFileIncludeMgr#getFilesThatInclude(int)}.
	 */
	@Test
	public void testGetFilesThatInclude() {
		
		/* add a number of file relationships */
		fileIncludeMgr.addFileIncludes(2, 1);
		fileIncludeMgr.addFileIncludes(3, 1);
		fileIncludeMgr.addFileIncludes(4, 1);
		fileIncludeMgr.addFileIncludes(5, 2);
		fileIncludeMgr.addFileIncludes(6, 2);
		fileIncludeMgr.addFileIncludes(7, 1);
		fileIncludeMgr.addFileIncludes(8, 3);
		fileIncludeMgr.addFileIncludes(9, 3);
		fileIncludeMgr.addFileIncludes(10, 1);
		
		/* search for the files that include file 1 */
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] {2, 3, 4, 7, 10}, fileIncludeMgr.getFilesThatInclude(1)));

		/* search for the files that include file 2 */
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] {5, 6}, fileIncludeMgr.getFilesThatInclude(2)));

		/* search for the files that include file 3 */
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] {8, 9}, fileIncludeMgr.getFilesThatInclude(3)));
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for {@link com.buildml.model.IFileIncludeMgr#getFilesIncludedBy(int)}.
	 */
	@Test
	public void testGetFilesIncludedBy() {
		
		/* add a number of file relationships */
		fileIncludeMgr.addFileIncludes(2, 1);
		fileIncludeMgr.addFileIncludes(2, 3);
		fileIncludeMgr.addFileIncludes(2, 4);
		fileIncludeMgr.addFileIncludes(2, 10);
		fileIncludeMgr.addFileIncludes(3, 2);
		fileIncludeMgr.addFileIncludes(3, 3);
		fileIncludeMgr.addFileIncludes(3, 9);
		fileIncludeMgr.addFileIncludes(3, 10);
		fileIncludeMgr.addFileIncludes(10, 1);
		
		/* search for the files that are included by file 10 */
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { 1 }, fileIncludeMgr.getFilesIncludedBy(10)));

		/* search for the files that are included by 3 */
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { 2, 3, 9, 10 }, fileIncludeMgr.getFilesIncludedBy(3)));

		/* search for the files that are include by 2 */
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { 1, 3, 4, 10 }, fileIncludeMgr.getFilesIncludedBy(2)));
		
		/* search for the files that are include by 11 */
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { }, fileIncludeMgr.getFilesIncludedBy(11)));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IFileIncludeMgr#deleteFilesIncludedBy(int)}.
	 */
	@Test
	public void deleteFilesIncludedBy() {
		
		/* add a number of file relationships */
		fileIncludeMgr.addFileIncludes(2, 1);
		fileIncludeMgr.addFileIncludes(2, 3);
		fileIncludeMgr.addFileIncludes(2, 4);
		fileIncludeMgr.addFileIncludes(2, 10);
		fileIncludeMgr.addFileIncludes(3, 2);
		fileIncludeMgr.addFileIncludes(3, 3);
		fileIncludeMgr.addFileIncludes(3, 9);
		fileIncludeMgr.addFileIncludes(3, 10);
		fileIncludeMgr.addFileIncludes(10, 1);
		
		/* delete the files-include relationship for files that are included by file 10 */
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { 1 }, fileIncludeMgr.getFilesIncludedBy(10)));
		fileIncludeMgr.deleteFilesIncludedBy(10);
		assertEquals(0, fileIncludeMgr.getFilesIncludedBy(10).length);

		/* the same for 3 */
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { 2, 3, 9, 10 }, fileIncludeMgr.getFilesIncludedBy(3)));
		fileIncludeMgr.deleteFilesIncludedBy(3);
		assertEquals(0, fileIncludeMgr.getFilesIncludedBy(3).length);

		/* the same for 2 */
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { 1, 3, 4, 10 }, fileIncludeMgr.getFilesIncludedBy(2)));
		fileIncludeMgr.deleteFilesIncludedBy(2);
		assertEquals(0, fileIncludeMgr.getFilesIncludedBy(2).length);

		/* the same for 11 */
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { }, fileIncludeMgr.getFilesIncludedBy(11)));
		fileIncludeMgr.deleteFilesIncludedBy(11);
		assertEquals(0, fileIncludeMgr.getFilesIncludedBy(11).length);	
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IFileIncludeMgr#getTotalFileIncludedCount(int)}.
	 */
	@Test
	public void testScalability() {
		
		bs.setFastAccessMode(true);

		/* create a large number of randomly-generated file relationships */
		Random r = new Random();
		for (int i = 0; i != 50000; i++) {
			fileIncludeMgr.addFileIncludes(r.nextInt(100), r.nextInt(100));
		}
		bs.setFastAccessMode(false);
		
		/*
		 * Now, fetch them in various ways.
		 */
		for (int i = 0; i != 20000; i++) {
			fileIncludeMgr.getFileIncludesCount(r.nextInt(100), r.nextInt(100));
			fileIncludeMgr.getFilesIncludedBy(r.nextInt(100));
			fileIncludeMgr.getFilesThatInclude(r.nextInt(100));
			fileIncludeMgr.getTotalFileIncludedCount(r.nextInt());
		}
		
	}

	/*-------------------------------------------------------------------------------------*/

}
