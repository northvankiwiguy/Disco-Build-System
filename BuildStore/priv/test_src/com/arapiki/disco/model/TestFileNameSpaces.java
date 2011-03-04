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

import com.arapiki.disco.model.FileNameSpaces.PathType;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestFileNameSpaces {

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
		bs = CommonTestUtils.getEmptyBuildStore();
		
		/* fetch the associated FileSpace */
		bsfs = bs.getFileNameSpaces();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the getRootPath() method - by default there's always a "root" root, associated
	 * with path ID 0.
	 */
	@Test
	public void testGetRootPath() throws Exception {
		
		int root = bsfs.getRootPath("root");		
		assertEquals(root, 0);
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * Test the getChildofPath() method.
	 */
	@Test
	public void testGetChildOfPath() throws Exception {
		
		int rootPath = bsfs.getRootPath("root");
		
		/* check that a new path starts out as missing */
		int missingPath = bsfs.getChildOfPath(rootPath, "apple");
		assertEquals(-1, missingPath);
		
		/* now add the path, and make sure it's no longer missing */
		int newPath = bsfs.addChildOfPath(rootPath, PathType.TYPE_DIR, "apple");
		int existingPath = bsfs.getChildOfPath(rootPath, "apple");
		assertEquals(newPath, existingPath);
		
		/* it shouldn't be the same ID as the root */
		assertNotSame(newPath, rootPath);
		
		/* let's add a sub-path, but at first it's considered missing */
		int missingSubPath = bsfs.getChildOfPath(newPath, "banana");
		assertEquals(-1, missingSubPath);
		
		/* now add the sub-path, and make sure it's no longer missing */
		int newSubPath = bsfs.addChildOfPath(newPath, PathType.TYPE_DIR, "banana");
		int existingSubPath = bsfs.getChildOfPath(newPath, "banana");
		assertEquals(newSubPath, existingSubPath);
		
		/* ensure that the IDs are different */
		assertNotSame(newPath, newSubPath);
		
		/* ensure that similar names aren't matched by mistake */
		int badMatch1 = bsfs.getChildOfPath(rootPath, "banan");
		int badMatch2 = bsfs.getChildOfPath(rootPath, "banana!");
		assertEquals(-1, badMatch1);
		assertEquals(-1, badMatch2);
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * Test the addChildOfPath() method. 
	 */
	@Test
	public void testAddChildOfPath() throws Exception {
		
		/*
		 * Test adding files underneath a parent. Adding files that have
		 * already been added should provide the same ID back again.
		 * If file names are not the same, they should have different IDs.
		 */
		int rootPath = bsfs.getRootPath("root");
		int newPath1 = bsfs.addChildOfPath(rootPath, PathType.TYPE_DIR, "cmpt1");
		int newPath2 = bsfs.addChildOfPath(rootPath, PathType.TYPE_DIR, "cmpt2");
		int newPath3 = bsfs.addChildOfPath(rootPath, PathType.TYPE_DIR, "cmpt1");
		assertEquals(newPath1, newPath3);
		assertNotSame(newPath1, newPath2);
		
		/*
		 * Add some files at the second level of hierarchy. The same file name
		 * is used, but they're in different directories and must have different IDs.
		 */
		int newPath1a = bsfs.addChildOfPath(newPath1, PathType.TYPE_DIR, "childA");
		int newPath2a = bsfs.addChildOfPath(newPath2, PathType.TYPE_DIR, "childA");
		assertNotSame(newPath1a, newPath2a);
		
		/*
		 * Try to add some more paths - adding an existing path again should return
		 * the same ID number.
		 */
		int newPath1b = bsfs.addChildOfPath(newPath1, PathType.TYPE_DIR, "childB");
		int newPath1a2 = bsfs.addChildOfPath(newPath1, PathType.TYPE_DIR, "childA");
		assertEquals(newPath1a, newPath1a2);
		
		int newPath1b2 = bsfs.addChildOfPath(newPath1, PathType.TYPE_DIR, "childB");
		assertEquals(newPath1b, newPath1b2);
		assertNotSame(newPath1a, newPath1b);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/*
	 * Test the addFile() method.
	 */
	@Test
	public void testAddFile() throws Exception {
		int path1 = bsfs.addFile("/aardvark/beaver/camel/dog");
		int path2 = bsfs.addFile("/aardvark/beaver/camel/doggish");
		int path3 = bsfs.addFile("/aardvark/beaver/cat/dog");
		assertNotSame(path1, path2);
		assertNotSame(path2, path3);

		int path4 = bsfs.addFile("/aardvark/beaver/cat/dog");
		assertEquals(path3, path4);
		
		/* test case where non-absolute path is provided */
		int path5 = bsfs.addFile("a/b/c");
		assertEquals(-1, path5);
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * Test the addDirectory() method.
	 */
	@Test
	public void testAddDirectory() throws Exception {
		
		/* Add some directories and make sure the names are correct */
		int path1 = bsfs.addDirectory("/aardvark/beaver/camel/dog");
		int path2 = bsfs.addDirectory("/aardvark/beaver/camel/doggish");
		int path3 = bsfs.addDirectory("/aardvark/beaver/cat/dog");
		assertNotSame(path1, path2);
		assertNotSame(path2, path3);

		int path4 = bsfs.addDirectory("/aardvark/beaver/cat/dog");
		assertEquals(path3, path4);
		
		/* Adding a directory within a directory is OK */
		int path5 = bsfs.addDirectory("/p1/p2");
		assertNotSame(-1, path5);
		int path6 = bsfs.addDirectory("/p1/p2/p3");
		assertNotSame(-1, path6);
		int path7 = bsfs.addDirectory("/p1");
		assertNotSame(-1, path7);
		
		/* Adding a directory within a file is NOT OK */
		int path8 = bsfs.addFile("/p1/p2/p3/myfile");
		assertNotSame(-1, path8);
		int path9 = bsfs.addDirectory("/p1/p2/p3/myfile/mydir");
		assertEquals(-1, path9);
		
		/* Trying to add an existing file as a directory is NOT OK */
		int path10 = bsfs.addFile("/x/y/z/file");
		assertNotSame(-1, path10);
		int path11 = bsfs.addDirectory("/x/y/z/file");
		assertEquals(-1, path11);
		
		/* but getting the path is OK */
		int path12 = bsfs.getPath("x/y/z/file");
		assertEquals(path12, path10);

		/* Trying to add an existing directory as a file is NOT OK */
		int path13 = bsfs.addDirectory("/x/y/z/dir");
		assertNotSame(-1, path13);
		int path14 = bsfs.addFile("/x/y/z/dir");
		assertEquals(-1, path14);
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * Test the getPathType() method.
	 */
	@Test
	public void testGetPathType() throws Exception {
		
		/* add a file, and check that it's a file */
		int path1 = bsfs.addFile("/antarctica/brazil/chile/denmark");
		assertEquals(PathType.TYPE_FILE, bsfs.getPathType(path1));

		/* check that all the parent paths are of type TYPE_DIR */
		int path2 = bsfs.addDirectory("/antarctica/brazil/chile");
		int path3 = bsfs.getPath("/antarctica/brazil");
		int path4 = bsfs.getPath("/antarctica");
		int path5 = bsfs.getPath("/");

		assertEquals(PathType.TYPE_DIR, bsfs.getPathType(path2));
		assertEquals(PathType.TYPE_DIR, bsfs.getPathType(path3));
		assertEquals(PathType.TYPE_DIR, bsfs.getPathType(path4));
		assertEquals(PathType.TYPE_DIR, bsfs.getPathType(path5));
		
		/* test that you can't add a file within a file */
		int path6 = bsfs.addFile("/a/b/c");
		int path7 = bsfs.addFile("/a/b/c/d");
		assertEquals(-1, path7);
		
		/* test adding at the top level */
		int path8 = bsfs.addFile("/moose");
		assertEquals(PathType.TYPE_FILE, bsfs.getPathType(path8));
	}

	/*-------------------------------------------------------------------------------------*/
	
	/*
	 * Test the getPath() method.
	 */
	@Test
	public void testGetPath() throws Exception {

		/* check that the first path we lookup from an empty database is missing */
		int missingPath1 = bsfs.getPath("/anchovy/bacon/cheddar/dill");
		assertEquals(-1, missingPath1);
		
		/* now add it, and check it again */
		int newPath1 = bsfs.addFile("/anchovy/bacon/cheddar/dill");
		int existingPath1 = bsfs.getPath("/anchovy/bacon/cheddar/dill");
		assertEquals(newPath1, existingPath1);
		assertNotSame(-1, existingPath1);

		/* now check for a second path, it should be missing */
		int missingPath2 = bsfs.getPath("/anchovy/bacon/cottage/cheese");
		assertEquals(-1, missingPath2);
		
		int newPath2 = bsfs.addFile("/anchovy/bacon/cottage/cheese");
		int existingPath2 = bsfs.getPath("/anchovy/bacon/cottage/cheese");
		assertEquals(newPath2, existingPath2);
		assertNotSame(existingPath1, existingPath2);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/*
	 * Test the getBaseName() method.
	 */
	@Test
	public void testGetBaseName() throws Exception {

		int path1 = bsfs.addFile("/pen");
		int path2 = bsfs.addFile("/apple/blueberry/carrot");
		int path3 = bsfs.addFile("/ear/foot/knee");
		int path4 = bsfs.getRootPath("root");

		assertEquals("pen", bsfs.getBaseName(path1));
		assertEquals("carrot", bsfs.getBaseName(path2));
		assertEquals("knee", bsfs.getBaseName(path3));
		assertEquals("/", bsfs.getBaseName(path4));
		
		/* an invalid path should return NULL */
		assertNull(bsfs.getBaseName(1000));
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * Test test getParentPath() method.
	 */
	@Test
	public void testGetParentPath() throws Exception {

		/* Create a single file, then check that all parents match up */
		int path1 = bsfs.addFile("/albania/bolivia/canada/denmark");
		int path2 = bsfs.getPath("/albania/bolivia/canada");
		int path3 = bsfs.getPath("/albania/bolivia");		
		int path4 = bsfs.getPath("/albania");
		int path5 = bsfs.getRootPath("root");
		
		assertEquals(path2, bsfs.getParentPath(path1));
		assertEquals(path3, bsfs.getParentPath(path2));
		assertEquals(path4, bsfs.getParentPath(path3));
		assertEquals(path5, bsfs.getParentPath(path4));
		
		/* special case for the root */
		assertEquals(path5, bsfs.getParentPath(path5));
		
		/* an invalid path should return -1 */
		assertEquals(-1, bsfs.getParentPath(1000));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/*
	 * Test the getPathName() method.
	 */
	@Test
	public void testGetPathName() throws Exception {

		int path1 = bsfs.addFile("/pen");
		int path2 = bsfs.addFile("/apple/blueberry/carrot");
		int path3 = bsfs.addFile("/ear/foot/knee");
		int path4 = bsfs.addFile("/");

		String path1name = bsfs.getPathName(path1);
		String path2name = bsfs.getPathName(path2);
		String path3name = bsfs.getPathName(path3);
		String path4name = bsfs.getPathName(path4);

		assertEquals("/pen", path1name);
		assertEquals("/apple/blueberry/carrot", path2name);
		assertEquals("/ear/foot/knee", path3name);
		assertEquals("/", path4name);
	}	
	
	/*-------------------------------------------------------------------------------------*/

	/*
	 * Test the getChildPaths() method.
	 */
	@Test
	public void testGetChildPaths() throws Exception {
		
		/* case 1 - child of root */
		int path1 = bsfs.getRootPath("root");
		Integer children[] = bsfs.getChildPaths(path1);
		assertEquals(0, children.length);
		
		/* 
		 * Add a couple of paths and check that they're returned
		 * in alphabetical order.
		 */
		int path2 = bsfs.addFile("/banana");
		int path3 = bsfs.addFile("/aardvark");
		int path4 = bsfs.addDirectory("/cello");
		children = bsfs.getChildPaths(path1);
		assertEquals(path3, children[0].intValue());
		assertEquals(path2, children[1].intValue());
		assertEquals(path4, children[2].intValue());
		
		/*
		 * Add some more paths, underneath an existing path.
		 */
		int path5 = bsfs.addFile("/cello/gold");
		int path6 = bsfs.addFile("/cello/silver");
		int path7 = bsfs.addFile("/cello/bronze");
		children = bsfs.getChildPaths(path4);
		assertEquals(path7, children[0].intValue());
		assertEquals(path5, children[1].intValue());
		assertEquals(path6, children[2].intValue());
	}
	
	/*-------------------------------------------------------------------------------------*/

	//@Test
	//public void testGetChildPathsWithAttrs() throws Exception {
	//	fail("Not yet implemented");
	//}
	
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
			bsfs.addFile(sb.toString());
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
		Integer children [] = bsfs.getChildPaths(parentID);
		for (int i = 0; i < children.length; i++) {
			int thisID = children[i];
			String name = bsfs.getPathName(thisID);
			//System.out.println(name);
			traverseAndDisplay(thisID);
		}
	}

	/*-------------------------------------------------------------------------------------*/	
}
