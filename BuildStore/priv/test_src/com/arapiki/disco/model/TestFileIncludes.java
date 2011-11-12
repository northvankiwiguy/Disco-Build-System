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

import org.junit.Before;
import org.junit.Test;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestFileIncludes {

	/*-------------------------------------------------------------------------------------*/
	
	/** our BuildStore, used for testing */
	private BuildStore bs;
	
	/** our FileDependencies object, used for testing */
	private FileIncludes fdeps;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		bs = CommonTestUtils.getEmptyBuildStore();
		
		/* fetch the associated FileDependencies */
		fdeps = bs.getFileIncludes();
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for {@link com.arapiki.disco.model.FileIncludes#addFileIncludes(int, int)}.
	 */
	@Test
	public void testAddFileIncludes() {

		/* without adding anything, check that the count is 0 */
		assertEquals(0, fdeps.getFileIncludesCount(1, 2));
		
		/* add something once */
		fdeps.addFileIncludes(1, 2);
		assertEquals(1, fdeps.getFileIncludesCount(1, 2));

		/* add something again */
		fdeps.addFileIncludes(1, 2);
		assertEquals(2, fdeps.getFileIncludesCount(1, 2));

		/* add something different */
		fdeps.addFileIncludes(1, 3);
		assertEquals(1, fdeps.getFileIncludesCount(1, 3));
		assertEquals(2, fdeps.getFileIncludesCount(1, 2));
		assertEquals(0, fdeps.getFileIncludesCount(1, 4));
	
		/* add another different one */
		fdeps.addFileIncludes(2, 2);
		assertEquals(1, fdeps.getFileIncludesCount(1, 3));
		assertEquals(2, fdeps.getFileIncludesCount(1, 2));
		assertEquals(0, fdeps.getFileIncludesCount(1, 4));
		assertEquals(1, fdeps.getFileIncludesCount(2, 2));	
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for {@link com.arapiki.disco.model.FileIncludes#getTotalFileIncludedCount(int)}.
	 */
	@Test
	public void testGetTotalFileIncludesCount() {

		/* before ever being accessed, the count should be 0 */
		assertEquals(0, fdeps.getTotalFileIncludedCount(2));
		
		/* access it once */
		fdeps.addFileIncludes(1, 2);
		assertEquals(1, fdeps.getTotalFileIncludedCount(2));

		/* and again, from the same parent file */
		fdeps.addFileIncludes(1, 2);
		assertEquals(2, fdeps.getTotalFileIncludedCount(2));

		/* now again from a different parent file */
		fdeps.addFileIncludes(3, 2);
		assertEquals(3, fdeps.getTotalFileIncludedCount(2));

		/* accessing a different file shouldn't change the count */
		fdeps.addFileIncludes(1, 3);
		assertEquals(3, fdeps.getTotalFileIncludedCount(2));
	}
	
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for {@link com.arapiki.disco.model.FileIncludes#getFilesThatInclude(int)}.
	 */
	@Test
	public void testGetFilesThatInclude() {
		
		/* add a number of file relationships */
		fdeps.addFileIncludes(2, 1);
		fdeps.addFileIncludes(3, 1);
		fdeps.addFileIncludes(4, 1);
		fdeps.addFileIncludes(5, 2);
		fdeps.addFileIncludes(6, 2);
		fdeps.addFileIncludes(7, 1);
		fdeps.addFileIncludes(8, 3);
		fdeps.addFileIncludes(9, 3);
		fdeps.addFileIncludes(10, 1);
		
		/* search for the files that include file 1 */
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] {2, 3, 4, 7, 10}, fdeps.getFilesThatInclude(1)));

		/* search for the files that include file 2 */
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] {5, 6}, fdeps.getFilesThatInclude(2)));

		/* search for the files that include file 3 */
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] {8, 9}, fdeps.getFilesThatInclude(3)));
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for {@link com.arapiki.disco.model.FileIncludes#getFilesIncludedBy(int)}.
	 */
	@Test
	public void testGetFilesIncludedBy() {
		
		/* add a number of file relationships */
		fdeps.addFileIncludes(2, 1);
		fdeps.addFileIncludes(2, 3);
		fdeps.addFileIncludes(2, 4);
		fdeps.addFileIncludes(2, 10);
		fdeps.addFileIncludes(3, 2);
		fdeps.addFileIncludes(3, 3);
		fdeps.addFileIncludes(3, 9);
		fdeps.addFileIncludes(3, 10);
		fdeps.addFileIncludes(10, 1);
		
		/* search for the files that are included by file 10 */
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { 1 }, fdeps.getFilesIncludedBy(10)));

		/* search for the files that are included by 3 */
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { 2, 3, 9, 10 }, fdeps.getFilesIncludedBy(3)));

		/* search for the files that are include by 2 */
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { 1, 3, 4, 10 }, fdeps.getFilesIncludedBy(2)));
		
		/* search for the files that are include by 11 */
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] { }, fdeps.getFilesIncludedBy(11)));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileIncludes#getTotalFileIncludedCount(int)}.
	 */
	@Test
	public void testScalability() {
		
		bs.setFastAccessMode(true);

		/* create a large number of randomly-generated file relationships */
		Random r = new Random();
		for (int i = 0; i != 50000; i++) {
			fdeps.addFileIncludes(r.nextInt(100), r.nextInt(100));
		}
		bs.setFastAccessMode(false);
		
		/*
		 * Now, fetch them in various ways.
		 */
		for (int i = 0; i != 20000; i++) {
			fdeps.getFileIncludesCount(r.nextInt(100), r.nextInt(100));
			fdeps.getFilesIncludedBy(r.nextInt(100));
			fdeps.getFilesThatInclude(r.nextInt(100));
			fdeps.getTotalFileIncludedCount(r.nextInt());
		}
		
	}

	/*-------------------------------------------------------------------------------------*/

}
