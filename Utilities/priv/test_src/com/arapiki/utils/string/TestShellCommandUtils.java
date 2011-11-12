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
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestShellCommandUtils {

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the joinCommandLine method()
	 */
	@Test
	public void testJoinCommandLine() {
		
		/* test the empty command */
		assertEquals("", ShellCommandUtils.joinCommandLine(""));

		/* test the command with only one \n */
		assertEquals("", ShellCommandUtils.joinCommandLine("\n"));

		/* test a single line command */
		assertEquals("gcc -c -o test.o test.c",
				ShellCommandUtils.joinCommandLine("gcc -c -o test.o test.c"));
		
		/* test a two line command (one command per line) */
		assertEquals("gcc -c -o test.o test.c && gcc -c -o foo.o foo.c",
				ShellCommandUtils.joinCommandLine("gcc -c -o test.o test.c\ngcc -c -o foo.o foo.c"));
				
		/* test a three line command */
		assertEquals("gcc -c -o test.o test.c && gcc -c -o foo.o foo.c && gcc -c -o bar.o bar.c",
				ShellCommandUtils.joinCommandLine("gcc -c -o test.o test.c\n" +
						"gcc -c -o foo.o foo.c\ngcc -c -o bar.o bar.c"));
		
		/* test a command that's split over two lines (and has \ at the end of the first line */
		assertEquals("gcc -c -o test.o test.c",
				ShellCommandUtils.joinCommandLine("gcc -c -o \\\ntest.o test.c"));
		
		/* test a command that's split over three lines */
		assertEquals("gcc -c -o long_file_name.o long_file_name.c",
				ShellCommandUtils.joinCommandLine("gcc -c -o \\\nlong_file_name.o \\\nlong_file_name.c"));
		
		/* test a two line command that has leading spaces */
		assertEquals("gcc -c -o test.o test.c && gcc -c -o foo.o foo.c",
				ShellCommandUtils.joinCommandLine("  gcc -c -o test.o test.c\n\t \tgcc -c -o foo.o foo.c"));
		
		/* test a command that's split over two lines, and has leading spaces */
		assertEquals("gcc -c -o test.o test.c",
				ShellCommandUtils.joinCommandLine("gcc -c -o \\\n    \ttest.o test.c"));
		
		/* test where the \ character is quoting a space rather than the \n character */
		assertEquals("gcc -c -o test.o test.c\\  && gcc -c -o bar.o bar.c",
				ShellCommandUtils.joinCommandLine("gcc -c -o test.o test.c\\ \ngcc -c -o bar.o bar.c"));

		/* test with one of the lines being empty */
		assertEquals("gcc -c -o test.o test.c && gcc -c -o foo.o foo.c", 
				ShellCommandUtils.joinCommandLine("gcc -c -o test.o test.c\n\ngcc -c -o foo.o foo.c"));

		/* the same, but with a split line */
		assertEquals("gcc -c -o test.o test.c && gcc -c -o foo.o foo.c", 
				ShellCommandUtils.joinCommandLine("gcc -c -o test.o test.c\\\n\ngcc -c -o foo.o foo.c"));
		
		/* a line that contains only \ */
		assertEquals("gcc -c -o test.o test.c && gcc -c -o foo.o foo.c", 
				ShellCommandUtils.joinCommandLine("gcc -c -o test.o test.c\\\n\\\n\ngcc -c -o foo.o foo.c"));
	}
}
