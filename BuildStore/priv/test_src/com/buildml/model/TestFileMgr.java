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

import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.IFileMgr.PathType;
import com.buildml.utils.errors.ErrorCode;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestFileMgr {

	/** Our BuildStore object, used in many test cases */
	private IBuildStore bs;

	/** The FileMgr associated with this BuildStore */
	IFileMgr fileMgr;
	
	/** The ActionMgr associated with this BuildStore */
	IActionMgr actionMgr;
	
	/** The FileAttributeMgr associated with this BuildStore */
	IFileAttributeMgr fileAttrMgr;

	/** The FileIncludes associated with this BuildStore */
	IFileIncludeMgr fileIncludeMgr;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		bs = CommonTestUtils.getEmptyBuildStore();
		
		/* fetch the associated FileMgr, ActionMgr, etc. */
		fileMgr = bs.getFileMgr();
		actionMgr = bs.getActionMgr();
		fileAttrMgr = bs.getFileAttributeMgr();
		fileIncludeMgr = bs.getFileIncludeMgr();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the getRootPath() method - by default there's always a "root" root, associated
	 * with path ID 0.
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testGetRootPath() throws Exception {
		
		int root = fileMgr.getRootPath("root");		
		assertEquals(root, 0);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the getChildofPath() method.
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testGetChildOfPath() throws Exception {
		
		int rootPath = fileMgr.getRootPath("root");
		
		/* check that a new path starts out as missing */
		int missingPath = fileMgr.getChildOfPath(rootPath, "apple");
		assertEquals(ErrorCode.NOT_FOUND, missingPath);
		
		/* now add the path, and make sure it's no longer missing */
		int newPath = fileMgr.addChildOfPath(rootPath, PathType.TYPE_DIR, "apple");
		int existingPath = fileMgr.getChildOfPath(rootPath, "apple");
		assertEquals(newPath, existingPath);
		
		/* it shouldn't be the same ID as the root */
		assertNotSame(newPath, rootPath);
		
		/* let's add a sub-path, but at first it's considered missing */
		int missingSubPath = fileMgr.getChildOfPath(newPath, "banana");
		assertEquals(ErrorCode.NOT_FOUND, missingSubPath);
		
		/* now add the sub-path, and make sure it's no longer missing */
		int newSubPath = fileMgr.addChildOfPath(newPath, PathType.TYPE_DIR, "banana");
		int existingSubPath = fileMgr.getChildOfPath(newPath, "banana");
		assertEquals(newSubPath, existingSubPath);
		
		/* ensure that the IDs are different */
		assertNotSame(newPath, newSubPath);
		
		/* ensure that similar names aren't matched by mistake */
		int badMatch1 = fileMgr.getChildOfPath(rootPath, "banan");
		int badMatch2 = fileMgr.getChildOfPath(rootPath, "banana!");
		assertEquals(ErrorCode.NOT_FOUND, badMatch1);
		assertEquals(ErrorCode.NOT_FOUND, badMatch2);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the addChildOfPath() method. 
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testAddChildOfPath() throws Exception {
		
		/*
		 * Test adding files underneath a parent. Adding files that have
		 * already been added should provide the same ID back again.
		 * If file names are not the same, they should have different IDs.
		 */
		int rootPath = fileMgr.getRootPath("root");
		int newPath1 = fileMgr.addChildOfPath(rootPath, PathType.TYPE_DIR, "cmpt1");
		int newPath2 = fileMgr.addChildOfPath(rootPath, PathType.TYPE_DIR, "cmpt2");
		int newPath3 = fileMgr.addChildOfPath(rootPath, PathType.TYPE_DIR, "cmpt1");
		assertEquals(newPath1, newPath3);
		assertNotSame(newPath1, newPath2);
		
		/*
		 * Add some files at the second level of hierarchy. The same file name
		 * is used, but they're in different directories and must have different IDs.
		 */
		int newPath1a = fileMgr.addChildOfPath(newPath1, PathType.TYPE_DIR, "childA");
		int newPath2a = fileMgr.addChildOfPath(newPath2, PathType.TYPE_DIR, "childA");
		assertNotSame(newPath1a, newPath2a);
		
		/*
		 * Try to add some more paths - adding an existing path again should return
		 * the same ID number.
		 */
		int newPath1b = fileMgr.addChildOfPath(newPath1, PathType.TYPE_DIR, "childB");
		int newPath1a2 = fileMgr.addChildOfPath(newPath1, PathType.TYPE_DIR, "childA");
		assertEquals(newPath1a, newPath1a2);
		
		int newPath1b2 = fileMgr.addChildOfPath(newPath1, PathType.TYPE_DIR, "childB");
		assertEquals(newPath1b, newPath1b2);
		assertNotSame(newPath1a, newPath1b);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the addFile() method.
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testAddFile() throws Exception {
		int path1 = fileMgr.addFile("/aardvark/beaver/camel/dog");
		int path2 = fileMgr.addFile("/aardvark/beaver/camel/doggish");
		int path3 = fileMgr.addFile("/aardvark/beaver/cat/dog");
		assertNotSame(path1, path2);
		assertNotSame(path2, path3);

		int path4 = fileMgr.addFile("/aardvark/beaver/cat/dog");
		assertEquals(path3, path4);
		
		/* test case where non-absolute path is provided */
		int path5 = fileMgr.addFile("a/b/c");
		assertEquals(ErrorCode.BAD_PATH, path5);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the addDirectory() method.
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testAddDirectory() throws Exception {
		
		/* Add some directories and make sure the names are correct */
		int path1 = fileMgr.addDirectory("/aardvark/beaver/camel/dog");
		int path2 = fileMgr.addDirectory("/aardvark/beaver/camel/doggish");
		int path3 = fileMgr.addDirectory("/aardvark/beaver/cat/dog");
		assertNotSame(path1, path2);
		assertNotSame(path2, path3);

		int path4 = fileMgr.addDirectory("/aardvark/beaver/cat/dog");
		assertEquals(path3, path4);
		
		/* Adding a directory within a directory is OK */
		int path5 = fileMgr.addDirectory("/p1/p2");
		assertNotSame(ErrorCode.BAD_PATH, path5);
		int path6 = fileMgr.addDirectory("/p1/p2/p3");
		assertNotSame(ErrorCode.BAD_PATH, path6);
		int path7 = fileMgr.addDirectory("/p1");
		assertNotSame(ErrorCode.BAD_PATH, path7);
		
		/* Adding a directory within a file is NOT OK */
		int path8 = fileMgr.addFile("/p1/p2/p3/myfile");
		assertNotSame(ErrorCode.BAD_PATH, path8);
		int path9 = fileMgr.addDirectory("/p1/p2/p3/myfile/mydir");
		assertEquals(ErrorCode.BAD_PATH, path9);
		
		/* Trying to add an existing file as a directory is NOT OK */
		int path10 = fileMgr.addFile("/x/y/z/file");
		assertNotSame(ErrorCode.BAD_PATH, path10);
		int path11 = fileMgr.addDirectory("/x/y/z/file");
		assertEquals(ErrorCode.BAD_PATH, path11);
		
		/* but getting the path is OK */
		int path12 = fileMgr.getPath("x/y/z/file");
		assertEquals(path12, path10);

		/* Trying to add an existing directory as a file is NOT OK */
		int path13 = fileMgr.addDirectory("/x/y/z/dir");
		assertNotSame(ErrorCode.BAD_PATH, path13);
		int path14 = fileMgr.addFile("/x/y/z/dir");
		assertEquals(ErrorCode.BAD_PATH, path14);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the getPathType() method.
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testGetPathType() throws Exception {
		
		/* add a file, and check that it's a file */
		int path1 = fileMgr.addFile("/antarctica/brazil/chile/denmark");
		assertEquals(PathType.TYPE_FILE, fileMgr.getPathType(path1));

		/* check that all the parent paths are of type TYPE_DIR */
		int path2 = fileMgr.addDirectory("/antarctica/brazil/chile");
		int path3 = fileMgr.getPath("/antarctica/brazil");
		int path4 = fileMgr.getPath("/antarctica");
		int path5 = fileMgr.getPath("/");

		assertEquals(PathType.TYPE_DIR, fileMgr.getPathType(path2));
		assertEquals(PathType.TYPE_DIR, fileMgr.getPathType(path3));
		assertEquals(PathType.TYPE_DIR, fileMgr.getPathType(path4));
		assertEquals(PathType.TYPE_DIR, fileMgr.getPathType(path5));
		
		/* test that you can't add a file within a file */
		fileMgr.addFile("/a/b/c");
		int path7 = fileMgr.addFile("/a/b/c/d");
		assertEquals(ErrorCode.BAD_PATH, path7);
		
		/* test adding at the top level */
		int path8 = fileMgr.addFile("/moose");
		assertEquals(PathType.TYPE_FILE, fileMgr.getPathType(path8));
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the getPath() method.
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testGetPath() throws Exception {

		/* check that the first path we lookup from an empty database is missing */
		int missingPath1 = fileMgr.getPath("/anchovy/bacon/cheddar/dill");
		assertEquals(ErrorCode.BAD_PATH, missingPath1);
		
		/* now add it, and check it again */
		int newPath1 = fileMgr.addFile("/anchovy/bacon/cheddar/dill");
		int existingPath1 = fileMgr.getPath("/anchovy/bacon/cheddar/dill");
		assertEquals(newPath1, existingPath1);
		assertNotSame(ErrorCode.BAD_PATH, existingPath1);

		/* now check for a second path, it should be missing */
		int missingPath2 = fileMgr.getPath("/anchovy/bacon/cottage/cheese");
		assertEquals(ErrorCode.BAD_PATH, missingPath2);
		
		int newPath2 = fileMgr.addFile("/anchovy/bacon/cottage/cheese");
		int existingPath2 = fileMgr.getPath("/anchovy/bacon/cottage/cheese");
		assertEquals(newPath2, existingPath2);
		assertNotSame(existingPath1, existingPath2);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the getBaseName() method.
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testGetBaseName() throws Exception {

		int path1 = fileMgr.addFile("/pen");
		int path2 = fileMgr.addFile("/apple/blueberry/carrot");
		int path3 = fileMgr.addFile("/ear/foot/knee");
		int path4 = fileMgr.getRootPath("root");

		assertEquals("pen", fileMgr.getBaseName(path1));
		assertEquals("carrot", fileMgr.getBaseName(path2));
		assertEquals("knee", fileMgr.getBaseName(path3));
		assertEquals("/", fileMgr.getBaseName(path4));
		
		/* an invalid path should return NULL */
		assertNull(fileMgr.getBaseName(1000));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test test getParentPath() method.
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testGetParentPath() throws Exception {

		/* Create a single file, then check that all parents match up */
		int path1 = fileMgr.addFile("/albania/bolivia/canada/denmark");
		int path2 = fileMgr.getPath("/albania/bolivia/canada");
		int path3 = fileMgr.getPath("/albania/bolivia");		
		int path4 = fileMgr.getPath("/albania");
		int path5 = fileMgr.getRootPath("root");
		
		assertEquals(path2, fileMgr.getParentPath(path1));
		assertEquals(path3, fileMgr.getParentPath(path2));
		assertEquals(path4, fileMgr.getParentPath(path3));
		assertEquals(path5, fileMgr.getParentPath(path4));
		
		/* special case for the root */
		assertEquals(path5, fileMgr.getParentPath(path5));
		
		/* an invalid path should return -1 */
		assertEquals(ErrorCode.NOT_FOUND, fileMgr.getParentPath(1000));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the getPathName() method.
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testGetPathName() throws Exception {

		int path1 = fileMgr.addFile("/pen");
		int path2 = fileMgr.addFile("/apple/blueberry/carrot");
		int path3 = fileMgr.addFile("/ear/foot/knee");
		int path4 = fileMgr.addFile("/");

		String path1name = fileMgr.getPathName(path1);
		String path2name = fileMgr.getPathName(path2);
		String path3name = fileMgr.getPathName(path3);
		String path4name = fileMgr.getPathName(path4);

		assertEquals("/pen", path1name);
		assertEquals("/apple/blueberry/carrot", path2name);
		assertEquals("/ear/foot/knee", path3name);
		assertEquals("/", path4name);
	}	
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the getChildPaths() method.
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testGetChildPaths() throws Exception {
		
		/* case 1 - child of root */
		int path1 = fileMgr.getRootPath("root");
		Integer children[] = fileMgr.getChildPaths(path1);
		assertEquals(0, children.length);
		
		/* 
		 * Add a couple of paths and check that they're returned
		 * in alphabetical order.
		 */
		int path2 = fileMgr.addFile("/banana");
		int path3 = fileMgr.addFile("/aardvark");
		int path4 = fileMgr.addDirectory("/cello");
		children = fileMgr.getChildPaths(path1);
		assertEquals(path3, children[0].intValue());
		assertEquals(path2, children[1].intValue());
		assertEquals(path4, children[2].intValue());
		
		/*
		 * Add some more paths, underneath an existing path.
		 */
		int path5 = fileMgr.addFile("/cello/gold");
		int path6 = fileMgr.addFile("/cello/silver");
		int path7 = fileMgr.addFile("/cello/bronze");
		children = fileMgr.getChildPaths(path4);
		assertEquals(path7, children[0].intValue());
		assertEquals(path5, children[1].intValue());
		assertEquals(path6, children[2].intValue());
	}
	
	/*-------------------------------------------------------------------------------------*/

	@Test
	public void testRemovePath()
	{
		/* remove a path that's not in use anywhere - should succeed. */
		int path1 = fileMgr.addFile("/april/may/june");
		int path2 = fileMgr.getPath("/april/may");
		int path3 = fileMgr.addFile("/april/may/august");
		assertNotSame(ErrorCode.BAD_PATH, path1);
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] { path1, path3 }, fileMgr.getChildPaths(path2)));
		assertEquals(ErrorCode.OK, fileMgr.movePathToTrash(path1));
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] { path3 }, fileMgr.getChildPaths(path2)));
		
		/* test that we can't remove paths that have children */
		assertEquals(ErrorCode.CANT_REMOVE, fileMgr.movePathToTrash(path2));
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] { path3 }, fileMgr.getChildPaths(path2)));

		/* test that we can't remove a directory that an action was executed in */
		int path4 = fileMgr.addFile("/april/may/september");
		int myBuildAction = actionMgr.addAction(actionMgr.getRootAction("root"), path4, "/bin/true");
		assertEquals(ErrorCode.CANT_REMOVE, fileMgr.movePathToTrash(path4));
		
		/* test that we can't remove a path that's referenced by an action */
		int path5 = fileMgr.addFile("/april/may/october");
		actionMgr.addFileAccess(myBuildAction, path5, OperationType.OP_READ);
		assertEquals(ErrorCode.CANT_REMOVE, fileMgr.movePathToTrash(path5));
		
		/* test that we can't remove a path that's attached to root */
		int path6 = fileMgr.addDirectory("/april/may/november");
		assertEquals(ErrorCode.OK, fileMgr.addNewRoot("myroot", path6));
		assertEquals(ErrorCode.CANT_REMOVE, fileMgr.movePathToTrash(path6));
		
		/* test that any attributes on that path have been removed. */
		int path7 = fileMgr.addFile("/april/may/december");
		int myAttrId = fileAttrMgr.newAttrName("myattr");
		fileAttrMgr.setAttr(path7, myAttrId, 1);
		assertEquals(1, fileAttrMgr.getAttrsOnPath(path7).length);
		assertEquals(ErrorCode.OK, fileMgr.movePathToTrash(path7));
		assertEquals(0, fileAttrMgr.getAttrsOnPath(path7).length);
		
		/* test that we can't remove a path that is included by another path */
		int path8 = fileMgr.addFile("/april/may/january");
		int path9 = fileMgr.addFile("/april/may/february");
		fileIncludeMgr.addFileIncludes(path8, path9);
		assertEquals(ErrorCode.CANT_REMOVE, fileMgr.movePathToTrash(path9));
		assertTrue(CommonTestUtils.sortedArraysEqual(
				fileIncludeMgr.getFilesIncludedBy(path8), new Integer[] { path9 }));
		
		/* and we can't remove a path that does the including */
		assertEquals(ErrorCode.CANT_REMOVE, fileMgr.movePathToTrash(path8));
		assertTrue(CommonTestUtils.sortedArraysEqual(
				fileIncludeMgr.getFilesThatInclude(path9), new Integer[] { path8 }));
		
		/* remove the include relationship, then the remove is possible */
		fileIncludeMgr.removeFileIncludes(path8, path9);
		assertEquals(ErrorCode.OK, fileMgr.movePathToTrash(path8));
		assertEquals(ErrorCode.OK, fileMgr.movePathToTrash(path9));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test removing and then reviving a path. It should only work if there are no connections
	 * between the path and other objects. These connections must first be removed.
	 * @throws Exception
	 */
	@Test
	public void testRevivePath1() throws Exception {

		/* add some paths - we'll remove "june" later. */
		int pathJune = fileMgr.addFile("/april/may/june");
		int pathAugust = fileMgr.addFile("/april/may/august");
		int pathMay = fileMgr.getPath("/april/may");
		
		/* add an action that writes to (generates) "june" */
		int action1 = actionMgr.addAction(actionMgr.getRootAction("root"), 
											fileMgr.getPath("/"), "");
		actionMgr.addFileAccess(action1, pathJune, OperationType.OP_WRITE);
		
		/* indicate that "june" includes "august" */
		fileIncludeMgr.addFileIncludes(pathJune, pathAugust);
		
		/* 
		 * Validate that all our queries work as expected. First, assert that "june" is 
		 * not garbage (yet).
		 */
		assertFalse(fileMgr.isPathTrashed(pathJune));

		/* test the relationship between "june" and the action */
		Integer[] actions = actionMgr.getActionsThatAccess(pathJune, OperationType.OP_UNSPECIFIED);
		assertArrayEquals(new Integer[] { action1 }, actions);
		Integer[] files = actionMgr.getFilesAccessed(action1, OperationType.OP_UNSPECIFIED);
		assertArrayEquals(new Integer[] { pathJune }, files);
		
		/* test the relationship between "june" and "august" */
		assertArrayEquals(new Integer[] { pathAugust }, fileIncludeMgr.getFilesIncludedBy(pathJune));	
		assertArrayEquals(new Integer[] { pathJune }, fileIncludeMgr.getFilesThatInclude(pathAugust));

		/* try to delete the file "june", even though it's in use - should fail. */
		assertEquals(ErrorCode.CANT_REMOVE, fileMgr.movePathToTrash(pathJune));

		/* OK, remove the relationships first, then it can be removed */
		actionMgr.removeFileAccess(action1, pathJune);
		fileIncludeMgr.removeFileIncludes(pathJune, pathAugust);
		assertEquals(ErrorCode.OK, fileMgr.movePathToTrash(pathJune));
		
		/* Now test that the file is trashed */
		assertTrue(fileMgr.isPathTrashed(pathJune));
		
		/* revive the path */
		fileMgr.revivePathFromTrash(pathJune);
		assertFalse(fileMgr.isPathTrashed(pathJune));
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test removing a path to check that it's no longer visible in searches.
	 * @throws Exception
	 */
	@Test
	public void testRevivePath2() throws Exception {

		/* add some paths - we'll remove "june" later. */
		int pathJune = fileMgr.addFile("/april/may/june");
		int pathAugust = fileMgr.addFile("/april/may/august");
		int pathMay = fileMgr.getPath("/april/may");

		/* test that the "june" file exists */
		assertNotSame(ErrorCode.BAD_PATH, fileMgr.getPath("/april/may/june"));
		assertNotSame(ErrorCode.NOT_FOUND, fileMgr.getChildOfPath(pathMay, "june"));
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] { pathJune, pathAugust }, fileMgr.getChildPaths(pathMay)));	
		assertEquals(PathType.TYPE_FILE, fileMgr.getPathType(pathJune));

		/* move "june" to trash */
		assertEquals(ErrorCode.OK, fileMgr.movePathToTrash(pathJune));

		/* the "june" file doesn't show up when accessed via an absolute or relative path */
		assertEquals(ErrorCode.BAD_PATH, fileMgr.getPath("/april/may/june"));
		assertEquals(ErrorCode.NOT_FOUND, fileMgr.getChildOfPath(pathMay, "june"));
		assertArrayEquals(new Integer[] { pathAugust }, fileMgr.getChildPaths(pathMay));	

		/* we can't add a new path with the same name */
		assertEquals(ErrorCode.BAD_PATH, fileMgr.addFile("/april/may/june"));
		
		/* revive the path */
		assertEquals(ErrorCode.OK, fileMgr.revivePathFromTrash(pathJune));
		assertFalse(fileMgr.isPathTrashed(pathJune));
		
		/* test all the above queries again - they should work */
		assertNotSame(ErrorCode.BAD_PATH, fileMgr.getPath("/april/may/june"));
		assertNotSame(ErrorCode.NOT_FOUND, fileMgr.getChildOfPath(pathMay, "june"));
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] { pathJune, pathAugust }, fileMgr.getChildPaths(pathMay)));
		assertEquals(PathType.TYPE_FILE, fileMgr.getPathType(pathJune));
	}	

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test removing a path to check that it's no longer visible in searches.
	 * @throws Exception
	 */
	@Test
	public void testRevivePath3() throws Exception {
		
		/* set up some directories, and add a root */
		int pathA = fileMgr.addDirectory("/a");
		int pathB = fileMgr.addDirectory("/a/b");
		int pathC = fileMgr.addDirectory("/a/b/c");
		fileMgr.addNewRoot("myRoot", pathB);
		
		/* delete the pathC directory */
		assertEquals(ErrorCode.OK, fileMgr.movePathToTrash(pathC));
		
		/* attempt to add a new root to this path - should fail because path doesn't exist */
		assertEquals(ErrorCode.BAD_PATH, fileMgr.addNewRoot("yourRoot", pathC));
		
		/* attempt to move an existing root to this path - should fail */
		assertEquals(ErrorCode.BAD_PATH, fileMgr.moveRootToPath("myRoot", pathC));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Used by the testAddManyFiles test case.
	 */
	String portionNames[] = {
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
			/* file paths can be 5-20 path portions long */
			int numPathPortions = r.nextInt(15) + 5;
			
			/* 
			 * Each path portion can be 5-10 characters long. To ensure
			 * we get some degree of consistency in path names, we'll use
			 * the names listed in the portionNames variable.
			 */
			sb.delete(0, sb.length());
			for (int j = 0; j != numPathPortions; j++) {
				sb.append("/");
				sb.append(portionNames[r.nextInt(6)]);
			}
			
			//System.out.println("Adding " + sb);

			/* add the file name to the FileSpace */
			fileMgr.addFile(sb.toString());
		}
		bs.setFastAccessMode(false);
	}

	/*-------------------------------------------------------------------------------------*/	
}
