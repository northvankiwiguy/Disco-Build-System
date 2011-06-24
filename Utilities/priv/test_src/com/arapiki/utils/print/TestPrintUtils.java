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

package com.arapiki.utils.print;


import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Test;

/**
 * Unit tests for the PrintUtils class.
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestPrintUtils {

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper method for comparing a byte array and a string
	 * @param baos The ByteArrayOutStream to compare against
	 * @param string The String to compare against
	 * @return True if they're same, else false.
	 */
	private boolean compareByteArray(ByteArrayOutputStream baos, String string) {
		
		if (baos.size() != string.length()) {
			return false;
		}
		
		byte bytes[] = baos.toByteArray();
		for (int i = 0; i != bytes.length; i++) {
			if (bytes[i] != string.charAt(i)) {
				return false;
			}
		}
		
		return true;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/*
	 * Test the PrintUtils.test() method.
	 */
	@Test
	public void testIndent() throws Exception {
		
		/* we perform the test by writing spaces into a ByteArrayOutputStream */
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintUtils.indent(new PrintStream(baos), 0);
		assertEquals(0, baos.size());
		
		PrintUtils.indent(new PrintStream(baos), 2);
		assertEquals(2, baos.size());

		PrintUtils.indent(new PrintStream(baos), 2);
		assertEquals(4, baos.size());

		PrintUtils.indent(new PrintStream(baos), 20);
		assertEquals(24, baos.size());

		PrintUtils.indent(new PrintStream(baos), 8);
		assertEquals(32, baos.size());
		
		PrintUtils.indent(new PrintStream(baos), 1000);
		assertEquals(1032, baos.size());
		
		/* now check that it's full of spaces */
		byte[] bytes = baos.toByteArray();
		for (int i = 0; i < bytes.length; i++) {
			assertEquals(' ', bytes[i]);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/*
	 * Test the PrintUtils.indentAndWrap() method.
	 */
	@Test
	public void testIndentAndWrap() throws Exception {
		
		/* all output is stored here, so we can easily analysis it */
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		/* Test a basic unwrapped line - without a terminating \n */
		PrintUtils.indentAndWrap(new PrintStream(baos), "This is my basic line", 4, 80);
		assertTrue(compareByteArray(baos, "    This is my basic line\n"));

		/* Test three lines of text, none of them wrapping */
		baos.reset();
		PrintUtils.indentAndWrap(new PrintStream(baos), "This is the first line\nNow the second\nAnd the third.", 4, 80);
		assertTrue(compareByteArray(baos, "    This is the first line\n    Now the second\n    And the third.\n"));
		
		/* Test a long line that wraps once */
		baos.reset();
		PrintUtils.indentAndWrap(new PrintStream(baos), "This is a very long line with lots of text.", 10, 40);
		assertTrue(compareByteArray(baos, "          This is a very long line with \\\n            lots of text.\n"));	
	
		/* Test a long line that wraps once, but has no convenient spaces */
		baos.reset();
		PrintUtils.indentAndWrap(new PrintStream(baos), "This is a very-long-line-with-lots-of-text.", 10, 40);
		assertTrue(compareByteArray(baos, "          This is a very-long-line-with-\\\n            lots-of-text.\n"));	
		
		/* Test a long line that must be split over three lines */
		baos.reset();
		PrintUtils.indentAndWrap(new PrintStream(baos), "This is a very long line with lots of text.", 20, 40);
		assertTrue(compareByteArray(baos, "                    This is a very long \\\n" +
				"                      line with lots of \\\n" +
				"                      text.\n"));
		
		/* Test a line wrap, followed by a non line wrap */
		baos.reset();
		PrintUtils.indentAndWrap(new PrintStream(baos), "This is a very long line with lots of text.\n" +
				"And a short line\nAnd another", 10, 40);
		assertTrue(compareByteArray(baos, "          This is a very long line with \\\n            lots of text.\n" +
				"          And a short line\n          And another\n"));	
		
		/* Test a realistic scenario */
		baos.reset();
		PrintUtils.indentAndWrap(new PrintStream(baos), "gcc -DHAVE_CONFIG_H -I. -I.. -I../src     " +
				"-g -O2 -MT getopt1.o -MD -MP -MF .deps/getopt1.Tpo -c -o getopt1.o getopt1.c", 6, 80);
		assertTrue(compareByteArray(baos, "      gcc -DHAVE_CONFIG_H -I. -I.. -I../src     " +
				"-g -O2 -MT getopt1.o -MD -MP -MF \\\n        .deps/getopt1.Tpo -c -o getopt1.o getopt1.c\n"));
		
		/* Test a scenario where the text is already indented, so wrapping a line should keep that indentation intact */
		baos.reset();
		System.out.println("STARTING FINAL TEST");
		PrintUtils.indentAndWrap(new PrintStream(baos), 
				"if (a == 2){\n" +
				"  if (b == 3){\n" +
				"    printf(\"%d\\n\", a + b + c + d + e + f + g + h + i);\n" +
				"    printf(\"%d\\n\", j + k + l + m + n + o + p + q + r);\n" +
				"  }\n" +
				"}\n", 4, 30);
		System.out.println(baos.toString());
		assertTrue(compareByteArray(baos, 
				"    if (a == 2){\n" +
				"      if (b == 3){\n" +
				"        printf(\"%d\\n\", a + b + \\\n" +
				"          c + d + e + f + g + \\\n" +
				"          h + i);\n" +
				"        printf(\"%d\\n\", j + k + \\\n" +
				"          l + m + n + o + p + \\\n" +
				"          q + r);\n" +
				"      }\n" +
				"    }\n"));
	}
}
