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

import org.junit.Before;
import org.junit.Test;

import com.arapiki.utils.errors.ErrorCode;

/**
 * Unit tests for the FileAttributes class.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestFileAttributes {

	/*-------------------------------------------------------------------------------------*/
	
	/* our BuildStore, used for testing */
	private BuildStore bs;
	
	/* our FileAttributes object, used for testing */
	private FileAttributes fattrs;
	
	/* our FileNameSpaces object, used for testing */
	private FileNameSpaces fns;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		bs = CommonTestUtils.getEmptyBuildStore();
		
		/* fetch the associated FileAttributes and FileNameSpaces */
		fattrs = bs.getFileAttributes();
		fns = bs.getFileNameSpaces();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileAttributes#newAttrName(java.lang.String)}.
	 */
	@Test
	public void testNewAttrName() {
		
		/* add some attribute names, and make sure the returned IDs are unique */
		int sizeId = fattrs.newAttrName("size");
		int blockId = fattrs.newAttrName("blocks");
		int componentId = fattrs.newAttrName("component");
		int executableId = fattrs.newAttrName("executable");
		assertNotSame(sizeId, blockId);
		assertNotSame(sizeId, componentId);
		assertNotSame(sizeId, executableId);
		assertNotSame(blockId, componentId);
		assertNotSame(blockId, executableId);
		assertNotSame(componentId, executableId);

		/* try to add a pre-existing attribute name, it should fail */
		int blockId2 = fattrs.newAttrName("blocks");
		int executableId2 = fattrs.newAttrName("executable");
		assertEquals(ErrorCode.ALREADY_USED, blockId2);
		assertEquals(ErrorCode.ALREADY_USED, executableId2);
		
		/* adding a similar (but non-identical) name will return a unique value */
		int blockId3 = fattrs.newAttrName("block");
		assertNotSame(blockId, blockId3);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileAttributes#getAttrIdFromName(java.lang.String)}.
	 */
	@Test
	public void testGetAttrIdFromName() {

		/* getting ID for non-existent attributes should return an error */
		assertEquals(ErrorCode.NOT_FOUND, fattrs.getAttrIdFromName("blocks"));
		assertEquals(ErrorCode.NOT_FOUND, fattrs.getAttrIdFromName("size"));
		
		/* now add some names */
		int sizeId = fattrs.newAttrName("size");
		int blockId = fattrs.newAttrName("blocks");
		int componentId = fattrs.newAttrName("component");
		int executableId = fattrs.newAttrName("executable");
		
		/* now the names should exist */
		assertEquals(executableId, fattrs.getAttrIdFromName("executable"));
		assertEquals(blockId, fattrs.getAttrIdFromName("blocks"));
		assertEquals(componentId, fattrs.getAttrIdFromName("component"));
		assertEquals(sizeId, fattrs.getAttrIdFromName("size"));
		
		/* but these other names shouldn't */
		assertEquals(ErrorCode.NOT_FOUND, fattrs.getAttrIdFromName("popsicles"));
		assertEquals(ErrorCode.NOT_FOUND, fattrs.getAttrIdFromName("length"));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileAttributes#getAttrNameFromId(int)}.
	 */
	@Test
	public void testGetAttrNameFromId() {

		/* getting the name of non-existent attributes should return null */
		assertNull(fattrs.getAttrNameFromId(10));
		assertNull(fattrs.getAttrNameFromId(20));
		
		/* now add some names */
		int sizeId = fattrs.newAttrName("size");
		int blockId = fattrs.newAttrName("blocks");
		int componentId = fattrs.newAttrName("component");
		int executableId = fattrs.newAttrName("executable");
		
		/* now the names should exist */
		assertEquals("executable", fattrs.getAttrNameFromId(executableId));
		assertEquals("blocks", fattrs.getAttrNameFromId(blockId));
		assertEquals("component", fattrs.getAttrNameFromId(componentId));
		assertEquals("size", fattrs.getAttrNameFromId(sizeId));
		
		/* but these other names shouldn't */
		assertNull(fattrs.getAttrNameFromId(21));
		assertNull(fattrs.getAttrNameFromId(20));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileAttributes#getAttrNames()}.
	 */
	@Test
	public void testGetAttrNames() {

		/* to start with, there shouldn't be any names */
		String emptyNames [] = fattrs.getAttrNames();
		assertEquals(0, emptyNames.length);
		
		/* add a number of attribute names, and check the list each time */
		fattrs.newAttrName("size");
		String names[] = fattrs.getAttrNames();
		CommonTestUtils.sortedArraysEqual(names, new String[] {"size"});
		fattrs.newAttrName("blocks");
		CommonTestUtils.sortedArraysEqual(names, new String[] {"blocks", "size"});
		fattrs.newAttrName("component");
		CommonTestUtils.sortedArraysEqual(names, new String[] {"blocks", "component", "size"});
		fattrs.newAttrName("executable");
		CommonTestUtils.sortedArraysEqual(names, new String[] {"blocks", "component", "executable", "size"});
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileAttributes#deleteAttrName(java.lang.String)}.
	 */
	@Test
	public void testDeleteAttrName() {
		
		/* add a bunch of records */
		fattrs.newAttrName("size");
		fattrs.newAttrName("blocks");
		fattrs.newAttrName("component");
		fattrs.newAttrName("executable");
		String names [] = fattrs.getAttrNames();
		CommonTestUtils.sortedArraysEqual(names, new String[] {"blocks", "component", "executable", "size"});

		/* delete them, one by one */
		assertEquals(ErrorCode.OK, fattrs.deleteAttrName("component"));
		names = fattrs.getAttrNames();
		CommonTestUtils.sortedArraysEqual(names, new String[] {"blocks", "executable", "size"});
		assertEquals(ErrorCode.OK, fattrs.deleteAttrName("size"));
		names = fattrs.getAttrNames();
		CommonTestUtils.sortedArraysEqual(names, new String[] {"blocks", "executable"});
		assertEquals(ErrorCode.OK, fattrs.deleteAttrName("blocks"));
		names = fattrs.getAttrNames();
		CommonTestUtils.sortedArraysEqual(names, new String[] {"executable"});
		assertEquals(ErrorCode.OK, fattrs.deleteAttrName("executable"));
		names = fattrs.getAttrNames();
		CommonTestUtils.sortedArraysEqual(names, new String[] {""});

		/* try to delete an attribute name that did exist, but doesn't any more */
		assertEquals(ErrorCode.NOT_FOUND, fattrs.deleteAttrName("size"));
		assertEquals(ErrorCode.NOT_FOUND, fattrs.deleteAttrName("blocks"));

		/* try to delete an attribute name that never existed */
		assertEquals(ErrorCode.NOT_FOUND, fattrs.deleteAttrName("nonexistent"));
		assertEquals(ErrorCode.NOT_FOUND, fattrs.deleteAttrName("notexistatall"));

		/* add attributes to a path, and ensure that the attribute name can't be deleted */
		int path1 = fns.addFile("/z/y/x");
		int path2 = fns.addFile("/m/n/o");
		int usedAttr = fattrs.newAttrName("used");
		fattrs.setAttr(path1, usedAttr, 100);
		fattrs.setAttr(path2, usedAttr, 1234);
		assertEquals(ErrorCode.CANT_REMOVE, fattrs.deleteAttrName("used"));
		fattrs.deleteAttr(path1, usedAttr);
		assertEquals(ErrorCode.CANT_REMOVE, fattrs.deleteAttrName("used"));
		fattrs.deleteAttr(path2, usedAttr);
		assertEquals(ErrorCode.OK, fattrs.deleteAttrName("used"));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileAttributes#setAttr(int, int, java.lang.String)}.
	 */
	@Test
	public void testSetAttrIntIntString() {
		
		/* add some paths and a couple of attribute names */
		int path1 = fns.addFile("/a/b/c/d");
		int path2 = fns.addFile("/a/b/c/e");
		int path3 = fns.addFile("/a/b/c/f");
		int attrComponent = fattrs.newAttrName("component");
		int attrCountry = fattrs.newAttrName("country");
		
		/* get the value of an attribute that's not set on the specified path - returns null */
		assertNull(fattrs.getAttrAsString(path1, attrComponent));
		assertNull(fattrs.getAttrAsString(path1, attrCountry));
		assertNull(fattrs.getAttrAsString(path2, attrComponent));
		assertNull(fattrs.getAttrAsString(path2, attrCountry));
		
		/* set some attributes and check their values */
		fattrs.setAttr(path1, attrComponent, "top");
		fattrs.setAttr(path1, attrCountry, "canada");
		fattrs.setAttr(path2, attrComponent, "bottom");
		fattrs.setAttr(path2, attrCountry, "england");
		assertEquals("top", fattrs.getAttrAsString(path1, attrComponent));
		assertEquals("canada", fattrs.getAttrAsString(path1, attrCountry));
		assertEquals("bottom", fattrs.getAttrAsString(path2, attrComponent));
		assertEquals("england", fattrs.getAttrAsString(path2, attrCountry));

		/* check that the other path wasn't impacted */
		assertNull(fattrs.getAttrAsString(path3, attrComponent));
		assertNull(fattrs.getAttrAsString(path3, attrCountry));
		
		/* change the value of some attributes */
		fattrs.setAttr(path1, attrComponent, "left");
		fattrs.setAttr(path2, attrComponent, "right");
		assertEquals("left", fattrs.getAttrAsString(path1, attrComponent));
		assertEquals("canada", fattrs.getAttrAsString(path1, attrCountry));
		assertEquals("right", fattrs.getAttrAsString(path2, attrComponent));
		assertEquals("england", fattrs.getAttrAsString(path2, attrCountry));
		
		/* set an attribute with a null string value - this should delete the attribute */
		fattrs.setAttr(path1, attrComponent, null);
		assertNull(fattrs.getAttrAsString(path1, attrComponent));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileAttributes#setAttr(int, int, int)}.
	 */
	@Test
	public void testSetAttrIntIntInt() {
		
		/* add some paths and a couple of attribute names */
		int path1 = fns.addFile("/a/b/c/d");
		int path2 = fns.addFile("/a/b/c/e");
		int path3 = fns.addFile("/a/b/c/f");
		int attrSize = fattrs.newAttrName("size");
		int attrBlocks = fattrs.newAttrName("blocks");
		
		/* get the value of an attribute that's not set */
		assertEquals(ErrorCode.NOT_FOUND, fattrs.getAttrAsInteger(path1, attrSize));
		assertEquals(ErrorCode.NOT_FOUND, fattrs.getAttrAsInteger(path1, attrBlocks));
		assertEquals(ErrorCode.NOT_FOUND, fattrs.getAttrAsInteger(path2, attrSize));
		assertEquals(ErrorCode.NOT_FOUND, fattrs.getAttrAsInteger(path2, attrBlocks));
		
		/* set some attributes and check their values */
		fattrs.setAttr(path1, attrSize, 100);
		fattrs.setAttr(path1, attrBlocks, 2);
		fattrs.setAttr(path2, attrSize, 54627);
		fattrs.setAttr(path2, attrBlocks, 123);
		assertEquals(100, fattrs.getAttrAsInteger(path1, attrSize));
		assertEquals(2, fattrs.getAttrAsInteger(path1, attrBlocks));
		assertEquals(54627, fattrs.getAttrAsInteger(path2, attrSize));
		assertEquals(123, fattrs.getAttrAsInteger(path2, attrBlocks));

		/* check that the other path wasn't impacted */
		assertNull(fattrs.getAttrAsString(path3, attrSize));
		assertNull(fattrs.getAttrAsString(path3, attrBlocks));
		
		/* change the value of some attributes */
		fattrs.setAttr(path1, attrSize, 200);
		fattrs.setAttr(path2, attrSize, 4627);
		assertEquals(200, fattrs.getAttrAsInteger(path1, attrSize));
		assertEquals(2, fattrs.getAttrAsInteger(path1, attrBlocks));
		assertEquals(4627, fattrs.getAttrAsInteger(path2, attrSize));
		assertEquals(123, fattrs.getAttrAsInteger(path2, attrBlocks));
		
		/* Try to add negative values - should fail */
		assertEquals(ErrorCode.BAD_VALUE, fattrs.setAttr(path1, attrSize, -10));
		
		/* Set an attribute as a string, and read it as an integer */
		fattrs.setAttr(path1, attrSize, "42");
		assertEquals(42, fattrs.getAttrAsInteger(path1, attrSize));
		fattrs.setAttr(path1, attrSize, "foo");
		assertEquals(ErrorCode.BAD_VALUE, fattrs.getAttrAsInteger(path1, attrSize));		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileAttributes#getAttrAsString(int, int)}.
	 */
	@Test
	public void testGetAttrAsString() {
		
		int path1 = fns.addFile("/p1/p2/p3");
		int attr1 = fattrs.newAttrName("attr1");
		int attr2 = fattrs.newAttrName("attr2");
		
		/* get attributes that aren't assigned yet - should return null */
		assertNull(fattrs.getAttrAsString(path1, attr1));
		assertNull(fattrs.getAttrAsString(path1, attr2));
		
		/* set some attributes and check that the values are correct */
		fattrs.setAttr(path1, attr1, "Hello");
		fattrs.setAttr(path1, attr2, "Goodbye");
		assertEquals("Hello", fattrs.getAttrAsString(path1, attr1));
		assertEquals("Goodbye", fattrs.getAttrAsString(path1, attr2));
		
		/* set the same attributes with a different value */
		fattrs.setAttr(path1, attr1, "Horse");
		fattrs.setAttr(path1, attr2, "Donkey");
		assertEquals("Horse", fattrs.getAttrAsString(path1, attr1));
		assertEquals("Donkey", fattrs.getAttrAsString(path1, attr2));
		
		/* set it as an integer, and retrieve it as a string */
		fattrs.setAttr(path1, attr1, 123);
		fattrs.setAttr(path1, attr2, 1000);
		assertEquals("123", fattrs.getAttrAsString(path1, attr1));
		assertEquals("1000", fattrs.getAttrAsString(path1, attr2));
		
		/* delete them, and try to get the values - should be null again */
		fattrs.deleteAttr(path1, attr1);
		assertNull(fattrs.getAttrAsString(path1, attr1));
		assertEquals("1000", fattrs.getAttrAsString(path1, attr2));

		fattrs.deleteAttr(path1, attr2);
		assertNull(fattrs.getAttrAsString(path1, attr1));
		assertNull(fattrs.getAttrAsString(path1, attr2));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileAttributes#getAttrAsInteger(int, int)}.
	 */
	@Test
	public void testGetAttrAsInteger() {
		
		/* create a path and two attributes */
		int path1 = fns.addFile("/p1/p2/p3");
		int attr1 = fattrs.newAttrName("attr1");
		int attr2 = fattrs.newAttrName("attr2");
		
		/* get attributes that aren't assigned yet - should return an error */
		assertEquals(ErrorCode.NOT_FOUND, fattrs.getAttrAsInteger(path1, attr1));
		assertEquals(ErrorCode.NOT_FOUND, fattrs.getAttrAsInteger(path1, attr2));
		
		/* set an attribute and check that the value is correct */
		fattrs.setAttr(path1, attr1, 1234);
		fattrs.setAttr(path1, attr2, 5678);
		assertEquals(1234, fattrs.getAttrAsInteger(path1, attr1));
		assertEquals(5678, fattrs.getAttrAsInteger(path1, attr2));
		
		/* set the same attributes with a different value */
		fattrs.setAttr(path1, attr1, 999);
		fattrs.setAttr(path1, attr2, 888);
		assertEquals(999, fattrs.getAttrAsInteger(path1, attr1));
		assertEquals(888, fattrs.getAttrAsInteger(path1, attr2));
		
		/* set it as a string, and retrieve it as an integer */
		fattrs.setAttr(path1, attr1, "166");
		fattrs.setAttr(path1, attr2, "256");
		assertEquals(166, fattrs.getAttrAsInteger(path1, attr1));
		assertEquals(256, fattrs.getAttrAsInteger(path1, attr2));
		
		/* create invalid strings and try to retrieve them as integers */
		fattrs.setAttr(path1, attr1, "dog");
		assertEquals(ErrorCode.BAD_VALUE, fattrs.getAttrAsInteger(path1, attr1));
		fattrs.setAttr(path1, attr1, "-10");
		assertEquals(ErrorCode.BAD_VALUE, fattrs.getAttrAsInteger(path1, attr1));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileAttributes#deleteAttr(int, int)}.
	 */
	@Test
	public void testDeleteAttr() {
		
		/* create a path and three attributes */
		int path1 = fns.addFile("/p1/p2/p3");
		int attr1 = fattrs.newAttrName("attr1");
		int attr2 = fattrs.newAttrName("attr2");
		
		/* set the attributes on the paths */
		fattrs.setAttr(path1, attr1, 128);
		fattrs.setAttr(path1, attr2, 256);
		assertEquals(128, fattrs.getAttrAsInteger(path1, attr1));		
		assertEquals(256, fattrs.getAttrAsInteger(path1, attr2));

		/* delete them, and try to get the values - should be null again */
		fattrs.deleteAttr(path1, attr1);
		assertEquals(ErrorCode.NOT_FOUND, fattrs.getAttrAsInteger(path1, attr1));
		assertEquals(256, fattrs.getAttrAsInteger(path1, attr2));
		fattrs.deleteAttr(path1, attr2);
		assertEquals(ErrorCode.NOT_FOUND, fattrs.getAttrAsInteger(path1, attr1));
		assertEquals(ErrorCode.NOT_FOUND, fattrs.getAttrAsInteger(path1, attr2));

		/* delete an attribute that was never assigned */
		int attr3 = fattrs.newAttrName("attr3");
		assertEquals(ErrorCode.NOT_FOUND, fattrs.getAttrAsInteger(path1, attr3));
		fattrs.deleteAttr(path1, attr3);
		assertEquals(ErrorCode.NOT_FOUND, fattrs.getAttrAsInteger(path1, attr3));		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileAttributes#getAttrsOnPath(int)}.
	 */
	@Test
	public void testGetAttrsOnPath() {

		/* create a bunch of attributes */
		int attrHeight = fattrs.newAttrName("height");
		int attrWeight = fattrs.newAttrName("weight");
		int attrWidth = fattrs.newAttrName("width");
		int attrAge = fattrs.newAttrName("age");

		/* and a bunch of paths */
		int path1 = fns.addFile("/p1/p2/p3");
		int path2 = fns.addFile("/p1/p4/p5");
		int path3 = fns.addFile("/p2/p2/p3");

		/* to start with, none of the paths have attributes */
		assertEquals(0, fattrs.getAttrsOnPath(path1).length);
		assertEquals(0, fattrs.getAttrsOnPath(path2).length);
		assertEquals(0, fattrs.getAttrsOnPath(path3).length);
		
		/* add some of the attributes to some of the paths */
		fattrs.setAttr(path1, attrHeight, 186);
		fattrs.setAttr(path1, attrWeight, 210);
		fattrs.setAttr(path1, attrAge, 40);
		fattrs.setAttr(path1, attrWidth, 20);
		fattrs.setAttr(path2, attrWeight, 150);
		fattrs.setAttr(path2, attrAge, 34);
		
		/* now test the results */
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] {attrHeight, attrWeight, attrAge, attrWidth}, 
				fattrs.getAttrsOnPath(path1)));
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] {attrWeight, attrAge}, 
				fattrs.getAttrsOnPath(path2)));
		assertEquals(0, fattrs.getAttrsOnPath(path3).length);
		
		/* add some more attributes, and take some away */
		fattrs.deleteAttr(path1, attrHeight);
		fattrs.setAttr(path2, attrHeight, 200);
		fattrs.setAttr(path3, attrHeight, 201);
		
		/* check again */
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] {attrWeight, attrAge, attrWidth}, 
				fattrs.getAttrsOnPath(path1)));
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] {attrWeight, attrAge, attrHeight}, 
				fattrs.getAttrsOnPath(path2)));
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] {attrHeight}, 
				fattrs.getAttrsOnPath(path3)));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileAttributes#getPathsWithAttr(int)}
	 * @return
	 */
	@Test
	public void testGetPathsWithAttrInt() {

		/* define some attributes */
		int attrWidth = fattrs.newAttrName("width");
		int attrAge = fattrs.newAttrName("age");
		int attrUnused = fattrs.newAttrName("unused");

		/* and a bunch of paths */
		int path1 = fns.addFile("/p1/p2/p3");
		int path2 = fns.addFile("/p1/p4/p5");
		int path3 = fns.addFile("/p2/p2/p3");

		/* assign the attributes to the paths */
		fattrs.setAttr(path1, attrWidth, 200);
		fattrs.setAttr(path2, attrWidth, 100);
		fattrs.setAttr(path3, attrWidth, 300);
		fattrs.setAttr(path1, attrAge, 20);
		fattrs.setAttr(path2, attrAge, 40);
		
		/* test that the correct paths show up */
		FileSet fs = fattrs.getPathsWithAttr(attrUnused);
		assertEquals(0, fs.size());
		
		fs = fattrs.getPathsWithAttr(attrWidth);
		assertEquals(3, fs.size());
		assertTrue(fs.isMember(path1));
		assertTrue(fs.isMember(path2));
		assertTrue(fs.isMember(path3));

		fs = fattrs.getPathsWithAttr(attrAge);
		assertEquals(2, fs.size());
		assertTrue(fs.isMember(path1));
		assertTrue(fs.isMember(path2));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileAttributes#getPathsWithAttr(int, String)}
	 */
	@Test
	public void testGetPathsWithAttrIntString() {
		
		/* define some attributes */
		int attrWord = fattrs.newAttrName("word");
		int attrFruit = fattrs.newAttrName("fruit");
		int attrUnused = fattrs.newAttrName("unused");

		/* and a bunch of paths */
		int path1 = fns.addFile("/p1/p2/p3");
		int path2 = fns.addFile("/p1/p4/p5");
		int path3 = fns.addFile("/p2/p2/p3");

		/* assign the attributes to the paths */
		fattrs.setAttr(path1, attrWord, "Hello");
		fattrs.setAttr(path2, attrWord, "There");
		fattrs.setAttr(path3, attrWord, "Hello");
		fattrs.setAttr(path1, attrFruit, "Banana");
		fattrs.setAttr(path2, attrFruit, "Apple");
		
		/* test that the correct paths show up */
		FileSet fs = fattrs.getPathsWithAttr(attrUnused, "Hello");
		assertEquals(0, fs.size());
		
		fs = fattrs.getPathsWithAttr(attrWord, "Hello");
		assertEquals(2, fs.size());
		assertTrue(fs.isMember(path1));
		assertTrue(fs.isMember(path3));

		fs = fattrs.getPathsWithAttr(attrFruit, "Banana");
		assertEquals(1, fs.size());
		assertTrue(fs.isMember(path1));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.FileAttributes#getPathsWithAttr(int, int)}
	 */
	@Test
	public void testGetPathsWithAttrIntInt() {

		/* define some attributes */
		int attrShoeSize = fattrs.newAttrName("shoe-size");
		int attrSpeed = fattrs.newAttrName("speed");
		int attrUnused = fattrs.newAttrName("unused");

		/* and a bunch of paths */
		int path1 = fns.addFile("/p1/p2/p3");
		int path2 = fns.addFile("/p1/p4/p5");
		int path3 = fns.addFile("/p2/p2/p3");

		/* assign the attributes to the paths */
		fattrs.setAttr(path1, attrShoeSize, 13);
		fattrs.setAttr(path2, attrShoeSize, 13);
		fattrs.setAttr(path3, attrShoeSize, 13);
		fattrs.setAttr(path1, attrSpeed, 80);
		fattrs.setAttr(path2, attrSpeed, 50);
		
		/* test that the correct paths show up */
		FileSet fs = fattrs.getPathsWithAttr(attrUnused, 0);
		assertEquals(0, fs.size());
		
		fs = fattrs.getPathsWithAttr(attrShoeSize, 13);
		assertEquals(3, fs.size());
		assertTrue(fs.isMember(path1));
		assertTrue(fs.isMember(path2));
		assertTrue(fs.isMember(path3));

		fs = fattrs.getPathsWithAttr(attrSpeed, 50);
		assertEquals(1, fs.size());
		assertTrue(fs.isMember(path2));
	}
	
	/*-------------------------------------------------------------------------------------*/
}
