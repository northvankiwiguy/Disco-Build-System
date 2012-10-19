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

import org.junit.Before;
import org.junit.Test;

import com.buildml.model.types.FileSet;
import com.buildml.utils.errors.ErrorCode;

/**
 * Unit tests for the FileAttributes class.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestFileAttributeMgr {

	/*-------------------------------------------------------------------------------------*/
	
	/** our BuildStore, used for testing */
	private IBuildStore bs;
	
	/** our FileAttributeMgr object, used for testing */
	private IFileAttributeMgr fileAttrMgr;
	
	/** our FileMgr object, used for testing */
	private IFileMgr fileMgr;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		bs = CommonTestUtils.getEmptyBuildStore();
		
		/* fetch the associated FileAttributes and FileNameSpaces */
		fileAttrMgr = bs.getFileAttributeMgr();
		fileMgr = bs.getFileMgr();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IFileAttributeMgr#newAttrName(java.lang.String)}.
	 */
	@Test
	public void testNewAttrName() {
		
		/* add some attribute names, and make sure the returned IDs are unique */
		int sizeId = fileAttrMgr.newAttrName("size");
		int blockId = fileAttrMgr.newAttrName("blocks");
		int pkgId = fileAttrMgr.newAttrName("package");
		int executableId = fileAttrMgr.newAttrName("executable");
		assertNotSame(sizeId, blockId);
		assertNotSame(sizeId, pkgId);
		assertNotSame(sizeId, executableId);
		assertNotSame(blockId, pkgId);
		assertNotSame(blockId, executableId);
		assertNotSame(pkgId, executableId);

		/* try to add a pre-existing attribute name, it should fail */
		int blockId2 = fileAttrMgr.newAttrName("blocks");
		int executableId2 = fileAttrMgr.newAttrName("executable");
		assertEquals(ErrorCode.ALREADY_USED, blockId2);
		assertEquals(ErrorCode.ALREADY_USED, executableId2);
		
		/* adding a similar (but non-identical) name will return a unique value */
		int blockId3 = fileAttrMgr.newAttrName("block");
		assertNotSame(blockId, blockId3);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IFileAttributeMgr#getAttrIdFromName(java.lang.String)}.
	 */
	@Test
	public void testGetAttrIdFromName() {

		/* getting ID for non-existent attributes should return an error */
		assertEquals(ErrorCode.NOT_FOUND, fileAttrMgr.getAttrIdFromName("blocks"));
		assertEquals(ErrorCode.NOT_FOUND, fileAttrMgr.getAttrIdFromName("size"));
		
		/* now add some names */
		int sizeId = fileAttrMgr.newAttrName("size");
		int blockId = fileAttrMgr.newAttrName("blocks");
		int pkgId = fileAttrMgr.newAttrName("package");
		int executableId = fileAttrMgr.newAttrName("executable");
		
		/* now the names should exist */
		assertEquals(executableId, fileAttrMgr.getAttrIdFromName("executable"));
		assertEquals(blockId, fileAttrMgr.getAttrIdFromName("blocks"));
		assertEquals(pkgId, fileAttrMgr.getAttrIdFromName("package"));
		assertEquals(sizeId, fileAttrMgr.getAttrIdFromName("size"));
		
		/* but these other names shouldn't */
		assertEquals(ErrorCode.NOT_FOUND, fileAttrMgr.getAttrIdFromName("popsicles"));
		assertEquals(ErrorCode.NOT_FOUND, fileAttrMgr.getAttrIdFromName("length"));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IFileAttributeMgr#getAttrNameFromId(int)}.
	 */
	@Test
	public void testGetAttrNameFromId() {

		/* getting the name of non-existent attributes should return null */
		assertNull(fileAttrMgr.getAttrNameFromId(10));
		assertNull(fileAttrMgr.getAttrNameFromId(20));
		
		/* now add some names */
		int sizeId = fileAttrMgr.newAttrName("size");
		int blockId = fileAttrMgr.newAttrName("blocks");
		int pkgId = fileAttrMgr.newAttrName("package");
		int executableId = fileAttrMgr.newAttrName("executable");
		
		/* now the names should exist */
		assertEquals("executable", fileAttrMgr.getAttrNameFromId(executableId));
		assertEquals("blocks", fileAttrMgr.getAttrNameFromId(blockId));
		assertEquals("package", fileAttrMgr.getAttrNameFromId(pkgId));
		assertEquals("size", fileAttrMgr.getAttrNameFromId(sizeId));
		
		/* but these other names shouldn't */
		assertNull(fileAttrMgr.getAttrNameFromId(21));
		assertNull(fileAttrMgr.getAttrNameFromId(20));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IFileAttributeMgr#getAttrNames()}.
	 */
	@Test
	public void testGetAttrNames() {

		/* to start with, there shouldn't be any names */
		String emptyNames [] = fileAttrMgr.getAttrNames();
		assertEquals(0, emptyNames.length);
		
		/* add a number of attribute names, and check the list each time */
		fileAttrMgr.newAttrName("size");
		String names[] = fileAttrMgr.getAttrNames();
		CommonTestUtils.sortedArraysEqual(names, new String[] {"size"});
		fileAttrMgr.newAttrName("blocks");
		CommonTestUtils.sortedArraysEqual(names, new String[] {"blocks", "size"});
		fileAttrMgr.newAttrName("package");
		CommonTestUtils.sortedArraysEqual(names, new String[] {"blocks", "package", "size"});
		fileAttrMgr.newAttrName("executable");
		CommonTestUtils.sortedArraysEqual(names, new String[] {"blocks", "package", "executable", "size"});
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IFileAttributeMgr#deleteAttrName(java.lang.String)}.
	 */
	@Test
	public void testDeleteAttrName() {
		
		/* add a bunch of records */
		fileAttrMgr.newAttrName("size");
		fileAttrMgr.newAttrName("blocks");
		fileAttrMgr.newAttrName("package");
		fileAttrMgr.newAttrName("executable");
		String names [] = fileAttrMgr.getAttrNames();
		CommonTestUtils.sortedArraysEqual(names, new String[] {"blocks", "package", "executable", "size"});

		/* delete them, one by one */
		assertEquals(ErrorCode.OK, fileAttrMgr.deleteAttrName("package"));
		names = fileAttrMgr.getAttrNames();
		CommonTestUtils.sortedArraysEqual(names, new String[] {"blocks", "executable", "size"});
		assertEquals(ErrorCode.OK, fileAttrMgr.deleteAttrName("size"));
		names = fileAttrMgr.getAttrNames();
		CommonTestUtils.sortedArraysEqual(names, new String[] {"blocks", "executable"});
		assertEquals(ErrorCode.OK, fileAttrMgr.deleteAttrName("blocks"));
		names = fileAttrMgr.getAttrNames();
		CommonTestUtils.sortedArraysEqual(names, new String[] {"executable"});
		assertEquals(ErrorCode.OK, fileAttrMgr.deleteAttrName("executable"));
		names = fileAttrMgr.getAttrNames();
		CommonTestUtils.sortedArraysEqual(names, new String[] {""});

		/* try to delete an attribute name that did exist, but doesn't any more */
		assertEquals(ErrorCode.NOT_FOUND, fileAttrMgr.deleteAttrName("size"));
		assertEquals(ErrorCode.NOT_FOUND, fileAttrMgr.deleteAttrName("blocks"));

		/* try to delete an attribute name that never existed */
		assertEquals(ErrorCode.NOT_FOUND, fileAttrMgr.deleteAttrName("nonexistent"));
		assertEquals(ErrorCode.NOT_FOUND, fileAttrMgr.deleteAttrName("notexistatall"));

		/* add attributes to a path, and ensure that the attribute name can't be deleted */
		int path1 = fileMgr.addFile("/z/y/x");
		int path2 = fileMgr.addFile("/m/n/o");
		int usedAttr = fileAttrMgr.newAttrName("used");
		fileAttrMgr.setAttr(path1, usedAttr, 100);
		fileAttrMgr.setAttr(path2, usedAttr, 1234);
		assertEquals(ErrorCode.CANT_REMOVE, fileAttrMgr.deleteAttrName("used"));
		fileAttrMgr.deleteAttr(path1, usedAttr);
		assertEquals(ErrorCode.CANT_REMOVE, fileAttrMgr.deleteAttrName("used"));
		fileAttrMgr.deleteAttr(path2, usedAttr);
		assertEquals(ErrorCode.OK, fileAttrMgr.deleteAttrName("used"));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IFileAttributeMgr#setAttr(int, int, java.lang.String)}.
	 */
	@Test
	public void testSetAttrIntIntString() {
		
		/* add some paths and a couple of attribute names */
		int path1 = fileMgr.addFile("/a/b/c/d");
		int path2 = fileMgr.addFile("/a/b/c/e");
		int path3 = fileMgr.addFile("/a/b/c/f");
		int attrPackage = fileAttrMgr.newAttrName("package");
		int attrCountry = fileAttrMgr.newAttrName("country");
		
		/* get the value of an attribute that's not set on the specified path - returns null */
		assertNull(fileAttrMgr.getAttrAsString(path1, attrPackage));
		assertNull(fileAttrMgr.getAttrAsString(path1, attrCountry));
		assertNull(fileAttrMgr.getAttrAsString(path2, attrPackage));
		assertNull(fileAttrMgr.getAttrAsString(path2, attrCountry));
		
		/* set some attributes and check their values */
		fileAttrMgr.setAttr(path1, attrPackage, "top");
		fileAttrMgr.setAttr(path1, attrCountry, "canada");
		fileAttrMgr.setAttr(path2, attrPackage, "bottom");
		fileAttrMgr.setAttr(path2, attrCountry, "england");
		assertEquals("top", fileAttrMgr.getAttrAsString(path1, attrPackage));
		assertEquals("canada", fileAttrMgr.getAttrAsString(path1, attrCountry));
		assertEquals("bottom", fileAttrMgr.getAttrAsString(path2, attrPackage));
		assertEquals("england", fileAttrMgr.getAttrAsString(path2, attrCountry));

		/* check that the other path wasn't impacted */
		assertNull(fileAttrMgr.getAttrAsString(path3, attrPackage));
		assertNull(fileAttrMgr.getAttrAsString(path3, attrCountry));
		
		/* change the value of some attributes */
		fileAttrMgr.setAttr(path1, attrPackage, "left");
		fileAttrMgr.setAttr(path2, attrPackage, "right");
		assertEquals("left", fileAttrMgr.getAttrAsString(path1, attrPackage));
		assertEquals("canada", fileAttrMgr.getAttrAsString(path1, attrCountry));
		assertEquals("right", fileAttrMgr.getAttrAsString(path2, attrPackage));
		assertEquals("england", fileAttrMgr.getAttrAsString(path2, attrCountry));
		
		/* set an attribute with a null string value - this should delete the attribute */
		fileAttrMgr.setAttr(path1, attrPackage, null);
		assertNull(fileAttrMgr.getAttrAsString(path1, attrPackage));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IFileAttributeMgr#setAttr(int, int, int)}.
	 */
	@Test
	public void testSetAttrIntIntInt() {
		
		/* add some paths and a couple of attribute names */
		int path1 = fileMgr.addFile("/a/b/c/d");
		int path2 = fileMgr.addFile("/a/b/c/e");
		int path3 = fileMgr.addFile("/a/b/c/f");
		int attrSize = fileAttrMgr.newAttrName("size");
		int attrBlocks = fileAttrMgr.newAttrName("blocks");
		
		/* get the value of an attribute that's not set */
		assertEquals(ErrorCode.NOT_FOUND, fileAttrMgr.getAttrAsInteger(path1, attrSize));
		assertEquals(ErrorCode.NOT_FOUND, fileAttrMgr.getAttrAsInteger(path1, attrBlocks));
		assertEquals(ErrorCode.NOT_FOUND, fileAttrMgr.getAttrAsInteger(path2, attrSize));
		assertEquals(ErrorCode.NOT_FOUND, fileAttrMgr.getAttrAsInteger(path2, attrBlocks));
		
		/* set some attributes and check their values */
		fileAttrMgr.setAttr(path1, attrSize, 100);
		fileAttrMgr.setAttr(path1, attrBlocks, 2);
		fileAttrMgr.setAttr(path2, attrSize, 54627);
		fileAttrMgr.setAttr(path2, attrBlocks, 123);
		assertEquals(100, fileAttrMgr.getAttrAsInteger(path1, attrSize));
		assertEquals(2, fileAttrMgr.getAttrAsInteger(path1, attrBlocks));
		assertEquals(54627, fileAttrMgr.getAttrAsInteger(path2, attrSize));
		assertEquals(123, fileAttrMgr.getAttrAsInteger(path2, attrBlocks));

		/* check that the other path wasn't impacted */
		assertNull(fileAttrMgr.getAttrAsString(path3, attrSize));
		assertNull(fileAttrMgr.getAttrAsString(path3, attrBlocks));
		
		/* change the value of some attributes */
		fileAttrMgr.setAttr(path1, attrSize, 200);
		fileAttrMgr.setAttr(path2, attrSize, 4627);
		assertEquals(200, fileAttrMgr.getAttrAsInteger(path1, attrSize));
		assertEquals(2, fileAttrMgr.getAttrAsInteger(path1, attrBlocks));
		assertEquals(4627, fileAttrMgr.getAttrAsInteger(path2, attrSize));
		assertEquals(123, fileAttrMgr.getAttrAsInteger(path2, attrBlocks));
		
		/* Try to add negative values - should fail */
		assertEquals(ErrorCode.BAD_VALUE, fileAttrMgr.setAttr(path1, attrSize, -10));
		
		/* Set an attribute as a string, and read it as an integer */
		fileAttrMgr.setAttr(path1, attrSize, "42");
		assertEquals(42, fileAttrMgr.getAttrAsInteger(path1, attrSize));
		fileAttrMgr.setAttr(path1, attrSize, "foo");
		assertEquals(ErrorCode.BAD_VALUE, fileAttrMgr.getAttrAsInteger(path1, attrSize));		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IFileAttributeMgr#getAttrAsString(int, int)}.
	 */
	@Test
	public void testGetAttrAsString() {
		
		int path1 = fileMgr.addFile("/p1/p2/p3");
		int attr1 = fileAttrMgr.newAttrName("attr1");
		int attr2 = fileAttrMgr.newAttrName("attr2");
		
		/* get attributes that aren't assigned yet - should return null */
		assertNull(fileAttrMgr.getAttrAsString(path1, attr1));
		assertNull(fileAttrMgr.getAttrAsString(path1, attr2));
		
		/* set some attributes and check that the values are correct */
		fileAttrMgr.setAttr(path1, attr1, "Hello");
		fileAttrMgr.setAttr(path1, attr2, "Goodbye");
		assertEquals("Hello", fileAttrMgr.getAttrAsString(path1, attr1));
		assertEquals("Goodbye", fileAttrMgr.getAttrAsString(path1, attr2));
		
		/* set the same attributes with a different value */
		fileAttrMgr.setAttr(path1, attr1, "Horse");
		fileAttrMgr.setAttr(path1, attr2, "Donkey");
		assertEquals("Horse", fileAttrMgr.getAttrAsString(path1, attr1));
		assertEquals("Donkey", fileAttrMgr.getAttrAsString(path1, attr2));
		
		/* set it as an integer, and retrieve it as a string */
		fileAttrMgr.setAttr(path1, attr1, 123);
		fileAttrMgr.setAttr(path1, attr2, 1000);
		assertEquals("123", fileAttrMgr.getAttrAsString(path1, attr1));
		assertEquals("1000", fileAttrMgr.getAttrAsString(path1, attr2));
		
		/* delete them, and try to get the values - should be null again */
		fileAttrMgr.deleteAttr(path1, attr1);
		assertNull(fileAttrMgr.getAttrAsString(path1, attr1));
		assertEquals("1000", fileAttrMgr.getAttrAsString(path1, attr2));

		fileAttrMgr.deleteAttr(path1, attr2);
		assertNull(fileAttrMgr.getAttrAsString(path1, attr1));
		assertNull(fileAttrMgr.getAttrAsString(path1, attr2));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IFileAttributeMgr#getAttrAsInteger(int, int)}.
	 */
	@Test
	public void testGetAttrAsInteger() {
		
		/* create a path and two attributes */
		int path1 = fileMgr.addFile("/p1/p2/p3");
		int attr1 = fileAttrMgr.newAttrName("attr1");
		int attr2 = fileAttrMgr.newAttrName("attr2");
		
		/* get attributes that aren't assigned yet - should return an error */
		assertEquals(ErrorCode.NOT_FOUND, fileAttrMgr.getAttrAsInteger(path1, attr1));
		assertEquals(ErrorCode.NOT_FOUND, fileAttrMgr.getAttrAsInteger(path1, attr2));
		
		/* set an attribute and check that the value is correct */
		fileAttrMgr.setAttr(path1, attr1, 1234);
		fileAttrMgr.setAttr(path1, attr2, 5678);
		assertEquals(1234, fileAttrMgr.getAttrAsInteger(path1, attr1));
		assertEquals(5678, fileAttrMgr.getAttrAsInteger(path1, attr2));
		
		/* set the same attributes with a different value */
		fileAttrMgr.setAttr(path1, attr1, 999);
		fileAttrMgr.setAttr(path1, attr2, 888);
		assertEquals(999, fileAttrMgr.getAttrAsInteger(path1, attr1));
		assertEquals(888, fileAttrMgr.getAttrAsInteger(path1, attr2));
		
		/* set it as a string, and retrieve it as an integer */
		fileAttrMgr.setAttr(path1, attr1, "166");
		fileAttrMgr.setAttr(path1, attr2, "256");
		assertEquals(166, fileAttrMgr.getAttrAsInteger(path1, attr1));
		assertEquals(256, fileAttrMgr.getAttrAsInteger(path1, attr2));
		
		/* create invalid strings and try to retrieve them as integers */
		fileAttrMgr.setAttr(path1, attr1, "dog");
		assertEquals(ErrorCode.BAD_VALUE, fileAttrMgr.getAttrAsInteger(path1, attr1));
		fileAttrMgr.setAttr(path1, attr1, "-10");
		assertEquals(ErrorCode.BAD_VALUE, fileAttrMgr.getAttrAsInteger(path1, attr1));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IFileAttributeMgr#deleteAttr(int, int)}.
	 */
	@Test
	public void testDeleteAttr() {
		
		/* create a path and three attributes */
		int path1 = fileMgr.addFile("/p1/p2/p3");
		int attr1 = fileAttrMgr.newAttrName("attr1");
		int attr2 = fileAttrMgr.newAttrName("attr2");
		
		/* set the attributes on the paths */
		fileAttrMgr.setAttr(path1, attr1, 128);
		fileAttrMgr.setAttr(path1, attr2, 256);
		assertEquals(128, fileAttrMgr.getAttrAsInteger(path1, attr1));		
		assertEquals(256, fileAttrMgr.getAttrAsInteger(path1, attr2));

		/* delete them, and try to get the values - should be null again */
		fileAttrMgr.deleteAttr(path1, attr1);
		assertEquals(ErrorCode.NOT_FOUND, fileAttrMgr.getAttrAsInteger(path1, attr1));
		assertEquals(256, fileAttrMgr.getAttrAsInteger(path1, attr2));
		fileAttrMgr.deleteAttr(path1, attr2);
		assertEquals(ErrorCode.NOT_FOUND, fileAttrMgr.getAttrAsInteger(path1, attr1));
		assertEquals(ErrorCode.NOT_FOUND, fileAttrMgr.getAttrAsInteger(path1, attr2));

		/* delete an attribute that was never assigned */
		int attr3 = fileAttrMgr.newAttrName("attr3");
		assertEquals(ErrorCode.NOT_FOUND, fileAttrMgr.getAttrAsInteger(path1, attr3));
		fileAttrMgr.deleteAttr(path1, attr3);
		assertEquals(ErrorCode.NOT_FOUND, fileAttrMgr.getAttrAsInteger(path1, attr3));		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IFileAttributeMgr#deleteAllAttrOnPath(int)}.
	 */
	@Test
	public void testDeleteAllAttrOnPath() {
		
		/* create two paths */
		int path1 = fileMgr.addFile("/path1");
		int path2 = fileMgr.addFile("/path2");

		/* create a number of attributes */
		int attr1 = fileAttrMgr.newAttrName("attr1");
		int attr2 = fileAttrMgr.newAttrName("attr2");
		int attr3 = fileAttrMgr.newAttrName("attr3");
		
		/* add a bunch of attributes on those paths */
		fileAttrMgr.setAttr(path1, attr1, 11);
		fileAttrMgr.setAttr(path2, attr1, 21);
		fileAttrMgr.setAttr(path1, attr2, 12);
		fileAttrMgr.setAttr(path1, attr3, 13);
		fileAttrMgr.setAttr(path2, attr2, 22);
		fileAttrMgr.setAttr(path2, attr3, 23);
				
		/* check that the attributes are set */
		assertTrue(CommonTestUtils.sortedArraysEqual(fileAttrMgr.getAttrsOnPath(path1),
				new Integer[] { attr1, attr2, attr3 }));
		assertTrue(CommonTestUtils.sortedArraysEqual(fileAttrMgr.getAttrsOnPath(path2),
				new Integer[] { attr1, attr2, attr3 }));
				
		/* delete all the attributes from the first path */
		fileAttrMgr.deleteAllAttrOnPath(path1);
		
		/* check that the attributes on the first path have gone */
		assertEquals(0, fileAttrMgr.getAttrsOnPath(path1).length);
	
		/* check that the remaining path still has attributes set */
		assertTrue(CommonTestUtils.sortedArraysEqual(fileAttrMgr.getAttrsOnPath(path2),
				new Integer[] { attr1, attr2, attr3 }));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IFileAttributeMgr#getAttrsOnPath(int)}.
	 */
	@Test
	public void testGetAttrsOnPath() {

		/* create a bunch of attributes */
		int attrHeight = fileAttrMgr.newAttrName("height");
		int attrWeight = fileAttrMgr.newAttrName("weight");
		int attrWidth = fileAttrMgr.newAttrName("width");
		int attrAge = fileAttrMgr.newAttrName("age");

		/* and a bunch of paths */
		int path1 = fileMgr.addFile("/p1/p2/p3");
		int path2 = fileMgr.addFile("/p1/p4/p5");
		int path3 = fileMgr.addFile("/p2/p2/p3");

		/* to start with, none of the paths have attributes */
		assertEquals(0, fileAttrMgr.getAttrsOnPath(path1).length);
		assertEquals(0, fileAttrMgr.getAttrsOnPath(path2).length);
		assertEquals(0, fileAttrMgr.getAttrsOnPath(path3).length);
		
		/* add some of the attributes to some of the paths */
		fileAttrMgr.setAttr(path1, attrHeight, 186);
		fileAttrMgr.setAttr(path1, attrWeight, 210);
		fileAttrMgr.setAttr(path1, attrAge, 40);
		fileAttrMgr.setAttr(path1, attrWidth, 20);
		fileAttrMgr.setAttr(path2, attrWeight, 150);
		fileAttrMgr.setAttr(path2, attrAge, 34);
		
		/* now test the results */
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] {attrHeight, attrWeight, attrAge, attrWidth}, 
				fileAttrMgr.getAttrsOnPath(path1)));
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] {attrWeight, attrAge}, 
				fileAttrMgr.getAttrsOnPath(path2)));
		assertEquals(0, fileAttrMgr.getAttrsOnPath(path3).length);
		
		/* add some more attributes, and take some away */
		fileAttrMgr.deleteAttr(path1, attrHeight);
		fileAttrMgr.setAttr(path2, attrHeight, 200);
		fileAttrMgr.setAttr(path3, attrHeight, 201);
		
		/* check again */
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] {attrWeight, attrAge, attrWidth}, 
				fileAttrMgr.getAttrsOnPath(path1)));
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] {attrWeight, attrAge, attrHeight}, 
				fileAttrMgr.getAttrsOnPath(path2)));
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] {attrHeight}, 
				fileAttrMgr.getAttrsOnPath(path3)));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IFileAttributeMgr#getPathsWithAttr(int)}
	 */
	@Test
	public void testGetPathsWithAttrInt() {

		/* define some attributes */
		int attrWidth = fileAttrMgr.newAttrName("width");
		int attrAge = fileAttrMgr.newAttrName("age");
		int attrUnused = fileAttrMgr.newAttrName("unused");

		/* and a bunch of paths */
		int path1 = fileMgr.addFile("/p1/p2/p3");
		int path2 = fileMgr.addFile("/p1/p4/p5");
		int path3 = fileMgr.addFile("/p2/p2/p3");

		/* assign the attributes to the paths */
		fileAttrMgr.setAttr(path1, attrWidth, 200);
		fileAttrMgr.setAttr(path2, attrWidth, 100);
		fileAttrMgr.setAttr(path3, attrWidth, 300);
		fileAttrMgr.setAttr(path1, attrAge, 20);
		fileAttrMgr.setAttr(path2, attrAge, 40);
		
		/* test that the correct paths show up */
		FileSet fs = fileAttrMgr.getPathsWithAttr(attrUnused);
		assertEquals(0, fs.size());
		
		fs = fileAttrMgr.getPathsWithAttr(attrWidth);
		assertEquals(3, fs.size());
		assertTrue(fs.isMember(path1));
		assertTrue(fs.isMember(path2));
		assertTrue(fs.isMember(path3));

		fs = fileAttrMgr.getPathsWithAttr(attrAge);
		assertEquals(2, fs.size());
		assertTrue(fs.isMember(path1));
		assertTrue(fs.isMember(path2));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IFileAttributeMgr#getPathsWithAttr(int, String)}
	 */
	@Test
	public void testGetPathsWithAttrIntString() {
		
		/* define some attributes */
		int attrWord = fileAttrMgr.newAttrName("word");
		int attrFruit = fileAttrMgr.newAttrName("fruit");
		int attrUnused = fileAttrMgr.newAttrName("unused");

		/* and a bunch of paths */
		int path1 = fileMgr.addFile("/p1/p2/p3");
		int path2 = fileMgr.addFile("/p1/p4/p5");
		int path3 = fileMgr.addFile("/p2/p2/p3");

		/* assign the attributes to the paths */
		fileAttrMgr.setAttr(path1, attrWord, "Hello");
		fileAttrMgr.setAttr(path2, attrWord, "There");
		fileAttrMgr.setAttr(path3, attrWord, "Hello");
		fileAttrMgr.setAttr(path1, attrFruit, "Banana");
		fileAttrMgr.setAttr(path2, attrFruit, "Apple");
		
		/* test that the correct paths show up */
		FileSet fs = fileAttrMgr.getPathsWithAttr(attrUnused, "Hello");
		assertEquals(0, fs.size());
		
		fs = fileAttrMgr.getPathsWithAttr(attrWord, "Hello");
		assertEquals(2, fs.size());
		assertTrue(fs.isMember(path1));
		assertTrue(fs.isMember(path3));

		fs = fileAttrMgr.getPathsWithAttr(attrFruit, "Banana");
		assertEquals(1, fs.size());
		assertTrue(fs.isMember(path1));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IFileAttributeMgr#getPathsWithAttr(int, int)}
	 */
	@Test
	public void testGetPathsWithAttrIntInt() {

		/* define some attributes */
		int attrShoeSize = fileAttrMgr.newAttrName("shoe-size");
		int attrSpeed = fileAttrMgr.newAttrName("speed");
		int attrUnused = fileAttrMgr.newAttrName("unused");

		/* and a bunch of paths */
		int path1 = fileMgr.addFile("/p1/p2/p3");
		int path2 = fileMgr.addFile("/p1/p4/p5");
		int path3 = fileMgr.addFile("/p2/p2/p3");

		/* assign the attributes to the paths */
		fileAttrMgr.setAttr(path1, attrShoeSize, 13);
		fileAttrMgr.setAttr(path2, attrShoeSize, 13);
		fileAttrMgr.setAttr(path3, attrShoeSize, 13);
		fileAttrMgr.setAttr(path1, attrSpeed, 80);
		fileAttrMgr.setAttr(path2, attrSpeed, 50);
		
		/* test that the correct paths show up */
		FileSet fs = fileAttrMgr.getPathsWithAttr(attrUnused, 0);
		assertEquals(0, fs.size());
		
		fs = fileAttrMgr.getPathsWithAttr(attrShoeSize, 13);
		assertEquals(3, fs.size());
		assertTrue(fs.isMember(path1));
		assertTrue(fs.isMember(path2));
		assertTrue(fs.isMember(path3));

		fs = fileAttrMgr.getPathsWithAttr(attrSpeed, 50);
		assertEquals(1, fs.size());
		assertTrue(fs.isMember(path2));
	}
	
	/*-------------------------------------------------------------------------------------*/
}
