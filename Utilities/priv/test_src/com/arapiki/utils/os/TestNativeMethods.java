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

import java.io.File;
import java.io.FileNotFoundException;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit tests for validating native methods.
 *
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestNativeMethods {
	
	/**
	 * Test the existence of symlinks using the SystemUtils.isSymlink() method.
	 * @throws Exception
	 */
	@Test
	public void testIsSymlink() throws Exception {
		
		/* Create a temporary file, it's NOT a symlink */
		File tmpFile = File.createTempFile("symlinkTest", null);
		String tmpFileName = tmpFile.toString();
		assertFalse(SystemUtils.isSymlink(tmpFileName));
		
		/* Prepare to create a symlink to that temporary file - before the symlink exists, it's FileNotFoundException */
		String symlinkName = tmpFileName + ".symlink";
		try {
			SystemUtils.isSymlink(symlinkName);
			fail("isSymlink() should have thrown an exception.");
		} catch (FileNotFoundException ex) {
			/* pass */
		}
		
		/* now, create it - it will become a symlink */
		ShellResult sr = SystemUtils.executeShellCmd("ln -s " + tmpFileName + " " + symlinkName, null);
		assertEquals(0, sr.getReturnCode());
		assertTrue(SystemUtils.isSymlink(symlinkName));

		/* check that the original file still isn't a symlink */
		assertFalse(SystemUtils.isSymlink(tmpFileName));

		/* delete the original file - the symlink is broken, but it's still a symlink */
		tmpFile.delete();
		assertTrue(SystemUtils.isSymlink(symlinkName));

		/* delete the symlink - it's no longer a symlink */
		new File(symlinkName).delete();
		try {
			SystemUtils.isSymlink(symlinkName);
			fail("isSymlink() should have thrown an exception.");
		} catch (FileNotFoundException ex) {
			/* pass */
		}
		
		/* null strings not allowed - they should throw an exception */
		try {
			SystemUtils.isSymlink(null);
			fail("isSymlink(null) should have thrown an exception.");
		} catch (NullPointerException ex) {
			/* pass */
		}
	}

	/**
	 * Test the SystemUtils.readSymlink() method.
	 * @throws Exception
	 */
	@Test
	public void testReadSymlink() throws Exception {
		
		/* create a temporary file - it won't be a symlink */
		File tmpFile = File.createTempFile("symlinkTest", null);
		String tmpFileName = tmpFile.toString();
		assertNull(SystemUtils.readSymlink(tmpFileName));
		
		/* Prepare to create a symlink to that temporary file - before it exists, an exception is expected */
		String symlinkName = tmpFileName + ".symlink";
		try {
			SystemUtils.readSymlink(symlinkName);
			fail("readSymlink() should have thrown an exception.");
		} catch (FileNotFoundException ex) {
			/* pass */
		}

		/* now, create it - it will become a symlink */
		ShellResult sr = SystemUtils.executeShellCmd("ln -s " + tmpFileName + " " + symlinkName, null);
		assertEquals(0, sr.getReturnCode());
		assertEquals(tmpFileName, SystemUtils.readSymlink(symlinkName));

		/* check that the original file still isn't a symlink */
		assertNull(SystemUtils.readSymlink(tmpFileName));

		/* delete the original file - the symlink is broken, but it's still a symlink */
		tmpFile.delete();
		assertEquals(tmpFileName, SystemUtils.readSymlink(symlinkName));

		/* delete the symlink - it's no longer a symlink */
		new File(symlinkName).delete();
		try {
			SystemUtils.readSymlink(symlinkName);
			fail("readSymlink() should have thrown an exception.");
		} catch (FileNotFoundException ex) {
			/* pass */
		}
		
		/* null strings not allowed - they should throw an exception */
		try {
			SystemUtils.readSymlink(null);
			fail("readSymlink(null) should have thrown an exception.");
		} catch (NullPointerException ex) {
			/* pass */
		}
	}

	/**
	 * Test method for readSymlink
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testCreateSymlink() throws Exception {
		
		/* create a temporary file - it won't be a symlink */
		File tmpFile = File.createTempFile("symlinkTest", null);
		String tmpFileName = tmpFile.toString();
		assertNull(SystemUtils.readSymlink(tmpFileName));

		/* create a symlink to this file */
		String symlinkName = tmpFileName + ".symlink";
		assertEquals(0, SystemUtils.createSymlink(symlinkName, tmpFileName));

		/* test that it's a symlink that points to the target */
		assertEquals(tmpFileName, SystemUtils.readSymlink(symlinkName));

		/* delete the original file - the symlink is broken, but it's still a symlink */
		tmpFile.delete();
		assertEquals(tmpFileName, SystemUtils.readSymlink(symlinkName));

		/* delete the symlink - it's no longer a symlink */
		new File(symlinkName).delete();
		try {
			SystemUtils.readSymlink(symlinkName);
			fail("readSymlink() should have thrown an exception.");
		} catch (FileNotFoundException ex) {
			/* pass */
		}

		/* create a symlink to a non-existent file - this should work */
		assertEquals(0, SystemUtils.createSymlink(symlinkName, "/non-existent"));
		assertEquals("/non-existent", SystemUtils.readSymlink(symlinkName));
		new File(symlinkName).delete();
		
		/* try to create a symlink to a read-only location - should fail */
		assertNotSame(0, SystemUtils.createSymlink("/non-creatable", "/non-existent"));
		assertFalse(new File("/non-creatable").exists());
	
		/* null strings not allowed - they should throw an exception */
		try {
			SystemUtils.createSymlink(null, tmpFileName);
			fail("createSymlink(null, <name>) should have thrown an exception.");
		} catch (NullPointerException ex) {
			/* pass */
		}
		try {
			SystemUtils.createSymlink(symlinkName, null);
			fail("createSymlink(<name>, null) should have thrown an exception.");
		} catch (NullPointerException ex) {
			/* pass */
		}
	}

}
