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
public class TestFileNameSpace {

	/** Our BuildStore object, used in many test cases */
	private BuildStore bs;

	/** The BuildStoreFileSpace associated with this BuildStore */
	FileNameSpaces bsfs;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		bs = TestCommon.getEmptyBuildStore();
		
		/* fetch the associated FileSpace */
		bsfs = bs.getFileNameSpaces();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test that the root path of a name space has the correct name space ID
	 * and the path ID of 0.
	 * TODO: test this with multiple roots
	 */
	@Test
	public void testGetRootPath() throws Exception {
		
		int root1 = bsfs.getRootPath("root1");
		int root2 = bsfs.getRootPath("root2");
		int root3 = bsfs.getRootPath("root3");
		
		assertEquals(root1, 0);
		assertEquals(root2, 0);
		assertEquals(root3, 0);
	}

	/*-------------------------------------------------------------------------------------*/

	@Test
	public void testGetChildUnderPath() throws Exception {
		
		int rootPath = bsfs.getRootPath("root");
		
		/* check that a new path starts out as missing */
		int missingPath = bsfs.getChildUnderPath(rootPath, "apple");
		assertEquals(-1, missingPath);
		
		/* now add the path, and make sure it's no longer missing */
		int newPath = bsfs.addChildUnderPath(rootPath, "apple");
		int existingPath = bsfs.getChildUnderPath(rootPath, "apple");
		assertEquals(newPath, existingPath);
		
		/* it shouldn't be the same ID as the root */
		assertNotSame(newPath, rootPath);
		
		/* let's add a sub-path, but at first it's considered missing */
		int missingSubPath = bsfs.getChildUnderPath(newPath, "banana");
		assertEquals(-1, missingSubPath);
		
		/* now add the sub-path, and make sure it's no longer missing */
		int newSubPath = bsfs.addChildUnderPath(newPath, "banana");
		int existingSubPath = bsfs.getChildUnderPath(newPath, "banana");
		assertEquals(newSubPath, existingSubPath);
		
		/* ensure that the IDs are different */
		assertNotSame(newPath, newSubPath);
		
		/* ensure that similar names aren't matched by mistake */
		int badMatch1 = bsfs.getChildUnderPath(rootPath, "banan");
		int badMatch2 = bsfs.getChildUnderPath(rootPath, "banana!");
		assertEquals(-1, badMatch1);
		assertEquals(-1, badMatch2);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test adding files underneath a parent. Adding files that have
	 * already been added should provide the same ID back again.
	 * If file names are not the same, they should have different IDs.
	 * @throws Exception
	 */
	@Test
	public void testAddChildUnderPath() throws Exception {
		
		int rootPath = bsfs.getRootPath("root");
		int newPath1 = bsfs.addChildUnderPath(rootPath, "cmpt1");
		int newPath2 = bsfs.addChildUnderPath(rootPath, "cmpt2");
		int newPath3 = bsfs.addChildUnderPath(rootPath, "cmpt1");
		assertEquals(newPath1, newPath3);
		
		int newPath1a = bsfs.addChildUnderPath(newPath1, "childA");
		int newPath2a = bsfs.addChildUnderPath(newPath2, "childA");
		assertNotSame(newPath1a, newPath2a);
		
		int newPath1b = bsfs.addChildUnderPath(newPath1, "childB");
		int newPath1a2 = bsfs.addChildUnderPath(newPath1, "childA");
		assertEquals(newPath1a, newPath1a2);
		
		int newPath1b2 = bsfs.addChildUnderPath(newPath1, "childB");
		assertEquals(newPath1b, newPath1b2);
		assertNotSame(newPath1a, newPath1b);
	}
	
	/*-------------------------------------------------------------------------------------*/

	@Test
	public void testAddFullPath() throws Exception {
		int path1 = bsfs.addFullPath("root", "/aardvark/beaver/camel/dog");
		int path2 = bsfs.addFullPath("root", "/aardvark/beaver/camel/doggish");
		int path3 = bsfs.addFullPath("root", "/aardvark/beaver/cat/dog");
		assertNotSame(path1, path2);
		assertNotSame(path2, path3);

		int path4 = bsfs.addFullPath("root", "/aardvark/beaver/cat/dog");
		assertEquals(path3, path4);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	@Test
	public void testLookupFullPath() throws Exception {

		/* check that the first path is missing */
		int missingPath1 = bsfs.lookupFullPath("root", "/anchovy/bacon/cheddar/dill");
		assertEquals(-1, missingPath1);
		
		/* now add it, and check it again */
		int newPath1 = bsfs.addFullPath("root", "/anchovy/bacon/cheddar/dill");
		int existingPath1 = bsfs.lookupFullPath("root", "/anchovy/bacon/cheddar/dill");
		assertEquals(newPath1, existingPath1);
		assertNotSame(-1, existingPath1);

		/* now check for a second path, it should be missing */
		int missingPath2 = bsfs.lookupFullPath("root", "/anchovy/bacon/cottage/cheese");
		assertEquals(-1, missingPath2);
		
		int newPath2 = bsfs.addFullPath("root", "/anchovy/bacon/cottage/cheese");
		int existingPath2 = bsfs.lookupFullPath("root", "/anchovy/bacon/cottage/cheese");
		assertEquals(newPath2, existingPath2);
		assertNotSame(existingPath1, existingPath2);
	}
	
	/*-------------------------------------------------------------------------------------*/

	@Test
	public void testGetBasePathName() throws Exception {

		int path1 = bsfs.addFullPath("root", "/pen");
		int path2 = bsfs.addFullPath("root", "/apple/blueberry/carrot");
		int path3 = bsfs.addFullPath("root", "/ear/foot/knee");
		int path4 = bsfs.getRootPath("root");

		assertEquals("pen", bsfs.getBasePathName(path1));
		assertEquals("carrot", bsfs.getBasePathName(path2));
		assertEquals("knee", bsfs.getBasePathName(path3));
		assertEquals("/", bsfs.getBasePathName(path4));
	}

	/*-------------------------------------------------------------------------------------*/

	@Test
	public void testGetParentPathID() throws Exception {

		int path1 = bsfs.addFullPath("root", "/albania/bolivia/canada/denmark");
		int path2 = bsfs.addFullPath("root", "/albania/bolivia/canada");
		int path3 = bsfs.addFullPath("root", "/albania/bolivia");		
		int path4 = bsfs.addFullPath("root", "/albania");
		int path5 = bsfs.getRootPath("root");
		
		assertEquals(path2, bsfs.getParentPathID(path1));
		assertEquals(path3, bsfs.getParentPathID(path2));
		assertEquals(path4, bsfs.getParentPathID(path3));
		assertEquals(path5, bsfs.getParentPathID(path4));
		
		/* special case for the root */
		assertEquals(path5, bsfs.getParentPathID(path5));
	}
	
	/*-------------------------------------------------------------------------------------*/

	@Test
	public void testGetFullPathName() throws Exception {

		int path1 = bsfs.addFullPath("root", "/pen");
		int path2 = bsfs.addFullPath("root", "/apple/blueberry/carrot");
		int path3 = bsfs.addFullPath("root", "/ear/foot/knee");

		String path1name = bsfs.getFullPathName(path1);
		String path2name = bsfs.getFullPathName(path2);
		String path3name = bsfs.getFullPathName(path3);

		assertEquals("/pen", path1name);
		assertEquals("/apple/blueberry/carrot", path2name);
		assertEquals("/ear/foot/knee", path3name);		
	}	
	
	/*-------------------------------------------------------------------------------------*/

	@Test
	public void testGetChildPathIDs() throws Exception {
		
		/* case 1 - child of root */
		int path1 = bsfs.getRootPath("root");
		Integer children[] = bsfs.getChildPathIDs(path1);
		assertEquals(0, children.length);
		
		/* 
		 * Add a couple of paths and check that they're returned
		 * in alphabetical order.
		 */
		int path2 = bsfs.addFullPath("root", "/banana");
		int path3 = bsfs.addFullPath("root", "/aardvark");
		int path4 = bsfs.addFullPath("root", "/cello");
		children = bsfs.getChildPathIDs(path1);
		assertEquals(path3, children[0].intValue());
		assertEquals(path2, children[1].intValue());
		assertEquals(path4, children[2].intValue());
		
		/*
		 * Add some more paths, underneath an existing path.
		 */
		int path5 = bsfs.addFullPath("root", "/cello/gold");
		int path6 = bsfs.addFullPath("root", "/cello/silver");
		int path7 = bsfs.addFullPath("root", "/cello/bronze");
		children = bsfs.getChildPathIDs(path4);
		assertEquals(path7, children[0].intValue());
		assertEquals(path5, children[1].intValue());
		assertEquals(path6, children[2].intValue());
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Used by the testAddManyFiles test case.
	 */
	String componentNames[] = {
			"aaaaa", "bbbbbb", "ccccccc", "dddddddd", "eeeeeeeee", "ffffffffff"
	};
	
	/**
	 * Test that we can add 20,000 file names into the file table. This
	 * is more of a timing test than a functionality test. In the end,
	 * you'll have 100,000+ entries in the files table, which is a good
	 * measure of scalability.
	 */
	@Test
	public void testAddManyFiles() {
		
		bs.setFastAccessMode(true);

		/* create a large number of randomly-generated file names */
		Random r = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i != 2000; i++) {
			/* file paths can be 5-20 path components long */
			int numPathComponents = r.nextInt(15) + 5;
			
			/* 
			 * Each path component can be 5-10 characters long. To ensure
			 * we get some degree of consistency in path names, we'll use
			 * the names listed in the componentNames variable.
			 */
			sb.delete(0, sb.length());
			for (int j = 0; j != numPathComponents; j++) {
				sb.append("/");
				sb.append(componentNames[r.nextInt(6)]);
			}
			
			//System.out.println("Adding " + sb);

			/* add the file name to the FileSpace */
			bsfs.addFullPath("root", sb.toString());
		}
		bs.setFastAccessMode(false);

		/*
		 * Now traverse the list of files.
		 */
		traverseAndDisplay(bsfs.getRootPath("root"));
	}

	/**
	 * For debugging purposes, traverse and display all the paths in the file system.
	 * @param rootPath
	 */
	private void traverseAndDisplay(int parentID) {
		Integer children [] = bsfs.getChildPathIDs(parentID);
		for (int i = 0; i < children.length; i++) {
			int thisID = children[i];
			String name = bsfs.getFullPathName(thisID);
			//System.out.println(name);
			traverseAndDisplay(thisID);
		}
	}

	/*-------------------------------------------------------------------------------------*/	
}
