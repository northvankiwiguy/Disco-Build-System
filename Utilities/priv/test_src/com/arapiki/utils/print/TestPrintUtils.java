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
}
