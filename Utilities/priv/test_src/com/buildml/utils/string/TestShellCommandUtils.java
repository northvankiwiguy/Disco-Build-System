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

package com.buildml.utils.string;


import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.buildml.utils.string.ShellCommandUtils;

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
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the shellEscapeString method()
	 */
	@Test
	public void testShellEscapeString() {
	
		assertEquals("/bin/rm", ShellCommandUtils.shellEscapeString("/bin/rm"));
		assertEquals("'echo >Hello'", ShellCommandUtils.shellEscapeString("echo >Hello"));
		assertEquals("'echo>Hello'", ShellCommandUtils.shellEscapeString("echo>Hello"));
		assertEquals("'foo*'", ShellCommandUtils.shellEscapeString("foo*"));
		assertEquals("'[foo]'", ShellCommandUtils.shellEscapeString("[foo]"));
		assertEquals("'hello'\\''world'", ShellCommandUtils.shellEscapeString("hello'world"));
		assertEquals("'nested '\\''string'\\'' here'", 
						ShellCommandUtils.shellEscapeString("nested 'string' here"));
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for getCommandSummary().
	 */
	@Test
	public void testGetCommandSummary() {
	
		/* create a single action, with a long command string */
		String shellCmd = "gcc -Ipath1/include -Ipath2/include -Ipath3/include -DFOO -DBAR " +
				"-o myfile.o -c myfile.c";
		
		/* fetch the summary at various widths */
		assertEquals("gcc -Ipath1/...", ShellCommandUtils.getCommandSummary(shellCmd, 15));
		assertEquals("gcc -Ipath1/include -Ipath2...", ShellCommandUtils.getCommandSummary(shellCmd, 30));
		assertEquals("gcc -Ipath1/include -Ipath2/include -Ipath3/inc...", ShellCommandUtils.getCommandSummary(shellCmd, 50));
		assertEquals("gcc -Ipath1/include -Ipath2/include -Ipath3/include -DFOO -DBAR " +
				"-o myfile.o -c myfile....", ShellCommandUtils.getCommandSummary(shellCmd, 89));
		assertEquals("gcc -Ipath1/include -Ipath2/include -Ipath3/include -DFOO -DBAR " +
				"-o myfile.o -c myfile.c", ShellCommandUtils.getCommandSummary(shellCmd, 100));
	}
	
	/*-------------------------------------------------------------------------------------*/
}
