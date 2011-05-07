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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.arapiki.disco.model.FileNameCache.FileNameCacheKey;
import com.arapiki.disco.model.FileNameCache.FileNameCacheValue;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestFileNameCache {

	/**
	 * The FileNameCache object under test
	 */
	FileNameCache fnc;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		/* create a cache with maximum size of 5. The perfect size for testing */
		fnc = new FileNameCache(5);
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * Test the basic manipulation of the FileNameCacheKey object type, which is a nested
	 * class within the FileNameCache class.
	 */
	@Test
	public void testFileNameCacheKey() throws Exception {
		
		/* instantiate a bunch of objects */
		FileNameCacheKey key1 = fnc.new FileNameCacheKey(1, "banana");
		FileNameCacheKey key2 = fnc.new FileNameCacheKey(1, "peach");
		FileNameCacheKey key3 = fnc.new FileNameCacheKey(1, "cucumber");
		FileNameCacheKey key4 = fnc.new FileNameCacheKey(223456, "orange");
		FileNameCacheKey key5 = fnc.new FileNameCacheKey(1, "cucumber");
		
		/* test the accessors */
		assertEquals(1, key1.getParentFileId());
		assertEquals("banana", key1.getChildFileName());
		assertEquals(223456, key4.getParentFileId());
		assertEquals("orange", key4.getChildFileName());
		
		/* compare various keys for equality - testing our custom-built equals method */
		assertEquals(key1, key1);
		assertEquals(key2, key2);
		assertNotSame(key1, key2);
		assertNotSame(key2, key3);
		assertEquals(key3, key5);
		
		/* compare hashCode values - testing our custom-built hashCode method */
		assertEquals(key3.hashCode(), key5.hashCode());
		assertNotSame(key1.hashCode(), key2.hashCode());
		assertNotSame(key3.hashCode(), key4.hashCode());
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * Test the creation and use of FileNameCacheValue objects. This is even simpler than
	 * the previous test case because there's no custom equals/hashCode.
	 */
	@Test
	public void testFileNameCacheValue() throws Exception {
		
		/* create a number of objects */
		FileNameCacheValue value1 = fnc.new FileNameCacheValue(1, 1);
		FileNameCacheValue value2 = fnc.new FileNameCacheValue(11, 2);
		FileNameCacheValue value3 = fnc.new FileNameCacheValue(1011, 1);
		
		/* test the accessors */
		assertEquals(1, value1.getChildFileId());
		assertEquals(1, value1.getChildType());
		assertEquals(11, value2.getChildFileId());
		assertEquals(2, value2.getChildType());
		assertEquals(1011, value3.getChildFileId());
		assertEquals(1, value3.getChildType());
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * Test that objects can be added to the cache and then retrieved later.
	 */
	@Test
	public void testSimpleAccess() throws Exception {

		/* check that initially the values don't exist */
		assertNull(fnc.get(1, "womble"));
		assertNull(fnc.get(200, "bungle"));
		
		/* now add a value and make sure it exists */
		fnc.put(1, "womble", 200, 1);
		FileNameCacheValue fncv = fnc.get(1, "womble");
		assertNotNull(fncv);
		assertEquals(200, fncv.getChildFileId());
		assertEquals(1, fncv.getChildType());

		/* other entries should still not exist */
		assertNull(fnc.get(2, "womble"));
		assertNull(fnc.get(1, "womblf"));
		assertNull(fnc.get(2, "womblf"));
		
		/* add the second object to the cache */
		fnc.put(200, "bungle", 234, 0);
		
		/* the first should still exist */
		fncv = fnc.get(1, "womble");
		assertNotNull(fncv);
		assertEquals(200, fncv.getChildFileId());
		assertEquals(1, fncv.getChildType());
		
		/* the second should too */
		fncv = fnc.get(200, "bungle");
		assertNotNull(fncv);
		assertEquals(234, fncv.getChildFileId());
		assertEquals(0, fncv.getChildType());
		
		/* overwrite the first with a new value */
		fnc.put(1, "womble", 34, 2);
		fncv = fnc.get(1, "womble");
		assertNotNull(fncv);
		assertEquals(34, fncv.getChildFileId());
		assertEquals(2, fncv.getChildType());
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * Test the clear() method to ensure it empty the cache.
	 */
	@Test
	public void testClear() throws Exception {
		
		/* add some values */
		fnc.put(1, "womble", 200, 1);
		fnc.put(2, "bungle", 200, 1);
		assertNotNull(fnc.get(1, "womble"));
		assertNotNull(fnc.get(2, "bungle"));
		
		/* clear the cache */
		fnc.clear();
		
		/* make sure they're no longer there */
		assertNull(fnc.get(1, "womble"));
		assertNull(fnc.get(2, "bungle"));		
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * Test the maximum size of the cache. Adding the 5th cache entry will cause the
	 * least recently accessed item to disappear.
	 */
	@Test
	public void testOverflow() throws Exception {
		
		/* add four names, which will max-out the cache */
		fnc.put(1, "womble", 200, 1);
		fnc.put(2, "wamble", 202, 2);
		fnc.put(3, "wimble", 204, 1);
		fnc.put(4, "wemble", 206, 3);
		
		/* check that they're all there */
		assertNotNull(fnc.get(3, "wimble"));
		assertNotNull(fnc.get(1, "womble"));
		assertNotNull(fnc.get(2, "wamble"));
		assertNotNull(fnc.get(4, "wemble"));

		/* now add a fifth name, the LRU should disappear, but the others remain */
		fnc.put(5, "wumble", 208, 2);
		assertNotNull(fnc.get(1, "womble"));
		assertNull(fnc.get(3, "wimble"));
		assertNotNull(fnc.get(2, "wamble"));
		assertNotNull(fnc.get(4, "wemble"));
		assertNotNull(fnc.get(5, "wumble"));
	}

	/*-------------------------------------------------------------------------------------*/
}
