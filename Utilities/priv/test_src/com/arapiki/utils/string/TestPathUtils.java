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

package com.arapiki.utils.string;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This class provides unit tests for the PathUtils class.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestPathUtils {

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test the validatePathComponent() method
	 * @throws Exception
	 */
	@Test
	public void testValidatePathComponent() throws Exception {
		assertFalse(PathUtils.validatePathComponent(null));
		assertFalse(PathUtils.validatePathComponent(""));
		assertFalse(PathUtils.validatePathComponent("."));
		assertFalse(PathUtils.validatePathComponent(".."));
		assertFalse(PathUtils.validatePathComponent("aard/vark"));
		assertFalse(PathUtils.validatePathComponent("aard\\vark"));
		assertTrue(PathUtils.validatePathComponent(".aardvark"));
		assertTrue(PathUtils.validatePathComponent("..aardvark"));
		assertTrue(PathUtils.validatePathComponent("aard..vark"));
		assertTrue(PathUtils.validatePathComponent("aardvark.."));
		assertTrue(PathUtils.validatePathComponent("aardvark"));
	}
	
	/**
	 * Test the normalizeAbsolutePath() method.
	 * @throws Exception
	 */
	@Test
	public void testNormalizeAbsolutePath() throws Exception {
		assertEquals(null, PathUtils.normalizeAbsolutePath(null));
		assertEquals("/", PathUtils.normalizeAbsolutePath("/"));
		assertEquals("/abc/def/ghi", PathUtils.normalizeAbsolutePath("/abc/def/ghi"));
		assertEquals("/abc/def/ghi", PathUtils.normalizeAbsolutePath("/abc/def/ghi/"));
		assertEquals("/abc/def", PathUtils.normalizeAbsolutePath("/abc/./def"));
		assertEquals("/def", PathUtils.normalizeAbsolutePath("/abc/../def"));
		assertEquals("/", PathUtils.normalizeAbsolutePath("/.."));
		assertEquals("/a", PathUtils.normalizeAbsolutePath("/./a/"));
		assertEquals("/", PathUtils.normalizeAbsolutePath("/./a/../.."));
		assertEquals("/aardvark", PathUtils.normalizeAbsolutePath("/aardvark/."));
		assertEquals("/aardvark/camel/bear", PathUtils.normalizeAbsolutePath("/aardvark/camel/.//bear/."));
	}
	
	
	/**
	 * Test the isAbsolutePath() method.
	 * @throws Exception
	 */
	@Test
	public void testIsAbsolutePath() throws Exception {
		assertFalse(PathUtils.isAbsolutePath(null));
		assertFalse(PathUtils.isAbsolutePath(""));
		assertFalse(PathUtils.isAbsolutePath("a"));
		assertFalse(PathUtils.isAbsolutePath("aardvark"));
		assertFalse(PathUtils.isAbsolutePath("a/b/c/d"));
		assertFalse(PathUtils.isAbsolutePath("abcd/"));
		assertTrue(PathUtils.isAbsolutePath("/"));
		assertTrue(PathUtils.isAbsolutePath("\\"));
		assertTrue(PathUtils.isAbsolutePath("/aardvark"));
		assertTrue(PathUtils.isAbsolutePath("/aard/vark"));
		assertTrue(PathUtils.isAbsolutePath("/aardvark/"));
	}

	/**
	 * Test the tokenizePath method.
	 * @throws Exception
	 */
	@Test
	public void testTokenizePath() throws Exception {
		assertArrayEquals(new String[]{}, PathUtils.tokenizePath(null));
		assertArrayEquals(new String[]{}, PathUtils.tokenizePath("/"));
		assertArrayEquals(new String[]{}, PathUtils.tokenizePath("///"));
		assertArrayEquals(new String[]{"aardvark"}, PathUtils.tokenizePath("/aardvark"));
		assertArrayEquals(new String[]{"aardvark"}, PathUtils.tokenizePath("/aardvark/"));
		assertArrayEquals(new String[]{"a", "b", "c"}, PathUtils.tokenizePath("/a/b/c"));
		assertArrayEquals(new String[]{"a", "b", "c"}, PathUtils.tokenizePath("//a/b/c/"));
		assertArrayEquals(new String[]{"a1", "b2", "c3"}, PathUtils.tokenizePath("a1/b2/c3"));
		assertArrayEquals(new String[]{"aardvark", "banana", "camel"}, PathUtils.tokenizePath("/aardvark//banana/camel"));
	}
}
