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

package com.arapiki.utils.os;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestShellResult {


	/**
	 * Test method for {@link com.arapiki.utils.os.ShellResult#getStdout()}.
	 */
	@Test
	public void testGetStdout() {
		ShellResult sr = new ShellResult("This is my stdout", "This is my stderr", 123);
		assertEquals("This is my stdout", sr.getStdout());
	}

	/**
	 * Test method for {@link com.arapiki.utils.os.ShellResult#getStderr()}.
	 */
	@Test
	public void testGetStderr() {
		ShellResult sr = new ShellResult("This is my other stdout", "This is my second stderr", 123);
		assertEquals("This is my second stderr", sr.getStderr());
	}

	/**
	 * Test method for {@link com.arapiki.utils.os.ShellResult#getReturnCode()}.
	 */
	@Test
	public void testGetReturnCode() {
		ShellResult sr = new ShellResult("This is my final stdout", "This is my last stderr", 102);
		assertEquals(102, sr.getReturnCode());
	}

}
