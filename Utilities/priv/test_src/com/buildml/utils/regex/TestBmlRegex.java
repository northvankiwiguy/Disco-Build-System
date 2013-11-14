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

package com.buildml.utils.regex;


import static org.junit.Assert.*;

import java.util.regex.PatternSyntaxException;

import org.junit.Test;

/**
 * Unit tests for the BmlRegex class.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestBmlRegex {

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for the BmlRegex.matchRegex() method.
	 */
	@Test
	public void testMatchRegex() {
		assertTrue(BmlRegex.matchRegex("@work/path/to/file.java", "@.*\\.java"));
		assertTrue(BmlRegex.matchRegex("@work/path/to/file.java", "@.*/.*\\.java"));
		assertTrue(BmlRegex.matchRegex("@work/path/to/file.java", ".*/[^/]*.java"));
		assertFalse(BmlRegex.matchRegex("file.java", ".*/[^/]*.java"));
		assertFalse(BmlRegex.matchRegex("/path/file.java", "[^/]*.java"));
		assertFalse(BmlRegex.matchRegex("@work/path/to/file.java", "@.*/\\.java"));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for the BmlRegex.matchAntRegex() method.
	 */
	@Test
	public void testMatchAntRegex() {
		assertTrue(BmlRegex.matchAntRegex("@work/path/to/file.java", "@work/**/*.java"));
		assertTrue(BmlRegex.matchAntRegex("@work/path/to/file.java", "**/*.java"));
		assertFalse(BmlRegex.matchAntRegex("@work/path/to/filejava", "@work/**/*.java"));
		assertTrue(BmlRegex.matchAntRegex("@work/file.java", "@work/**.java"));
		assertTrue(BmlRegex.matchAntRegex("file.java", "**.java"));
		assertTrue(BmlRegex.matchAntRegex("file.java", "*.java"));
		assertFalse(BmlRegex.matchAntRegex("file.jpg", "*.java"));
		assertFalse(BmlRegex.matchAntRegex("/a/b/c/file.jpg", "/*/*.jpg"));
		assertFalse(BmlRegex.matchAntRegex("/a/b/c/file.jpg", "*.jpg"));
		assertTrue(BmlRegex.matchAntRegex("/a/b/c/file.jpg", "**/????.jpg"));
		assertTrue(BmlRegex.matchAntRegex("/a/b/c/file.jpg", "**file.j**"));
		assertTrue(BmlRegex.matchAntRegex("[file$.jpg", "[file$.jpg"));
		assertFalse(BmlRegex.matchAntRegex("/a/b/c.java", "/a/b"));
		assertTrue(BmlRegex.matchAntRegex("/a/b/c.java", "/a/b/"));
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for the BmlRegex.compileRegexChain() method.
	 */
	@Test
	public void testCompileRegexChain() {
		
		/*
		 * Test 1 - include a couple of subdirectories, but exclude specific suffixes.
		 */
		RegexChain chain = BmlRegex.compileRegexChain(new String[] {
				"ia:@work/mydir/offiles/",
				"ia:@work/yourdir/offiles/",
				"ea:**/*.jpg",
				"ea:**/*.gif"
		});
		assertTrue(BmlRegex.matchRegexChain("@work/mydir/offiles/foo.java", chain));
		assertTrue(BmlRegex.matchRegexChain("@work/mydir/offiles/bah.java", chain));
		assertTrue(BmlRegex.matchRegexChain("@work/yourdir/offiles/bah.java", chain));
		assertFalse(BmlRegex.matchRegexChain("@work/hisdir/offiles/bah.java", chain));
		assertFalse(BmlRegex.matchRegexChain("@work/yourdir/offiles/picture.jpg", chain));
		assertFalse(BmlRegex.matchRegexChain("@work/yourdir/offiles/picture.gif", chain));
		assertTrue(BmlRegex.matchRegexChain("@work/yourdir/offiles/picture.png", chain));

		/*
		 * Test 2 - Explicitly include files by base name.
		 */
		chain = BmlRegex.compileRegexChain(new String[] {
				"ia:**/add.c",
				"ia:**/sub.c",
				"ia:**/mult.c",
				"ia:**/div.c"
		});
		assertTrue(BmlRegex.matchRegexChain("/add.c", chain));
		assertTrue(BmlRegex.matchRegexChain("/path/name1/add.c", chain));
		assertTrue(BmlRegex.matchRegexChain("/path/name1/name2/add.c", chain));
		assertTrue(BmlRegex.matchRegexChain("/div.c", chain));
		assertTrue(BmlRegex.matchRegexChain("/path/name1/div.c", chain));
		assertTrue(BmlRegex.matchRegexChain("/path/name1/name2/div.c", chain));
		assertFalse(BmlRegex.matchRegexChain("/quot.c", chain));
		assertFalse(BmlRegex.matchRegexChain("/path/name1/quot.c", chain));
		assertFalse(BmlRegex.matchRegexChain("/path/name1/name2/quot.c", chain));		
		
		/*
		 * Test 3 - exclude all, then include some. All should always be excluded.
		 */
		chain = BmlRegex.compileRegexChain(new String[] {
				"ea:**",
				"ia:**/*.java",
				"ia:**/*.h",
				"ia:**/*.c"
		});
		assertFalse(BmlRegex.matchRegexChain("/add.c", chain));
		assertFalse(BmlRegex.matchRegexChain("/a/b/foo.java", chain));
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for the BmlRegex.compileRegexChain() method, with error.
	 */
	@Test
	public void testCompileRegexChainErrors() {
		/*
		 * Test 1 - empty or null RegexChain gives no results.
		 */
		RegexChain chain = BmlRegex.compileRegexChain(new String[] {});
		assertEquals(0, chain.getSize());
		chain = BmlRegex.compileRegexChain(null);
		assertEquals(0, chain.getSize());		
				
		/*
		 * Test 2 - catch the exception with invalid formats
		 */
		try {
			chain = BmlRegex.compileRegexChain(new String[] {""});
			fail();
		} catch (PatternSyntaxException ex) {
			/* pass */
		}
		try {
			chain = BmlRegex.compileRegexChain(new String[] {null});
			fail();
		} catch (PatternSyntaxException ex) {
			/* pass */
		}
		try {
			chain = BmlRegex.compileRegexChain(new String[] {"foo.jpg"});
			fail();
		} catch (PatternSyntaxException ex) {
			/* pass */
		}
		try {
			chain = BmlRegex.compileRegexChain(new String[] {"oo:*.jpg"});
			fail();
		} catch (PatternSyntaxException ex) {
			/* pass */
		}
	}

	/*-------------------------------------------------------------------------------------*/
		
	/**
	 * Test method for the BmlRegex.filterRegexChain() method.
	 */
	@Test
	public void testFilterRegexChain() {
		
		/*
		 * Test 1 - Filter all java, h and c files.
		 */
		RegexChain chain = BmlRegex.compileRegexChain(new String[] {
				"ia:**/*.java",
				"ia:**/*.h",
				"ia:**/*.c"});
		String result[] = BmlRegex.filterRegexChain(new String[] {
				"@work/a/b/foo.java",
				"@work/a/b/foo.f",
				"@work/a/c/foo.h",
				"@work/a/c/foo.jpg"
		}, chain);
		assertArrayEquals(new String[] {"@work/a/b/foo.java", "@work/a/c/foo.h"}, result);

		/*
		 * Test 2 - Filter in all files in a specific directory.
		 */
		chain = BmlRegex.compileRegexChain(new String[] {
				"ia:@work/mydir/**"});
		result = BmlRegex.filterRegexChain(new String[] {
				"@work/mydir/foo.java",
				"@work/yourdir/foo.f",
				"@work/hisdir/mydir/foo.h",
				"@work/mydir/subdir/foo.jpg"
		}, chain);
		assertArrayEquals(new String[] {"@work/mydir/foo.java", "@work/mydir/subdir/foo.jpg"}, result);
		
		/*
		 * Test 3 - Filter out all files in a specific directory.
		 */
		chain = BmlRegex.compileRegexChain(new String[] {
				"ia:**",
				"ea:@work/mydir/**"});
		result = BmlRegex.filterRegexChain(new String[] {
				"@work/mydir/foo.java",
				"@work/yourdir/foo.f",
				"@work/hisdir/mydir/foo.h",
				"@work/mydir/subdir/foo.jpg"
		}, chain);
		assertArrayEquals(new String[] {"@work/yourdir/foo.f", "@work/hisdir/mydir/foo.h"}, result);

	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for the BmlRegex.filterRegexChain() method, with error cases.
	 */
	@Test
	public void testFilterRegexChainErrors() {

		/*
		 * Test with a null chain.
		 */
		assertNull(BmlRegex.filterRegexChain(new String[] {
				"@work/a/b/foo.java",
				"@work/a/b/foo.f",
				"@work/a/c/foo.h",
				"@work/a/c/foo.jpg"
		}, null));
		
		/*
		 * Test with an empty list of strings.
		 */
		RegexChain chain = BmlRegex.compileRegexChain(new String[] {
				"ia:**/*.java",
				"ia:**/*.h",
				"ia:**/*.c"});
		String result[] = BmlRegex.filterRegexChain(new String[] {}, chain);
		assertEquals(0, result.length);
		
		/*
		 * Test with a null list of strings.
		 */
		assertNull(BmlRegex.filterRegexChain(null, chain));
	}
	
	/*-------------------------------------------------------------------------------------*/
}
