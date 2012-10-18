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

package com.buildml.scanner.legacy;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.buildml.model.impl.BuildStore;
import com.buildml.model.CommonTestUtils;
import com.buildml.scanner.legacy.LegacyBuildScanner;

/**
 * Test methods for validating the LegacyBuildScanner class. Note that
 * since the legacy build scanner is very complex, we have multiple
 * test files (in this directory). This test class only tests the basic
 * accessor methods.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestLegacyBuildScanner {

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the setTraceFile()/getTraceFile() methods
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testSetTraceFile() throws Exception {
		
		/* create a new LegacyBuildScanner, with default settings */
		LegacyBuildScanner buildScanner = new LegacyBuildScanner();
		
		/* the default trace file name */
		assertEquals("cfs.trace", buildScanner.getTraceFile());
		
		/* set the trace file name to something else */
		buildScanner.setTraceFile("my_path_name.trace");
		assertEquals("my_path_name.trace", buildScanner.getTraceFile());
		buildScanner.setTraceFile("another_path_name");
		assertEquals("another_path_name", buildScanner.getTraceFile());
		
		/* set the default again */
		buildScanner.setTraceFile(null);
		assertEquals("cfs.trace", buildScanner.getTraceFile());
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the setBuildStore()/getBuildStore() methods
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testSetBuildStore() throws Exception {
		
		/* create a new LegacyBuildScanner, with default settings */
		LegacyBuildScanner buildScanner = new LegacyBuildScanner();
	
		/* create an empty build store */
		BuildStore buildStore = CommonTestUtils.getEmptyBuildStore();
		
		/* by default, there's no BuildStore */
		assertNull(buildScanner.getBuildStore());
		
		/* set the BuildStore, and check it's correct */
		buildScanner.setBuildStore(buildStore);
		assertEquals(buildStore, buildScanner.getBuildStore());
		
		/* now set it back to null */
		buildScanner.setBuildStore(null);
		assertNull(buildScanner.getBuildStore());		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the setDebugLevel()/getDebugLevel() methods
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testSetDebugLevel() throws Exception {
		
		/* create a new LegacyBuildScanner, with default settings */
		LegacyBuildScanner buildScanner = new LegacyBuildScanner();
		
		/* check the default is 0 */
		assertEquals(0, buildScanner.getDebugLevel());
		
		/* set to 0 */
		buildScanner.setDebugLevel(0);
		assertEquals(0, buildScanner.getDebugLevel());
		
		/* set to 1 */
		buildScanner.setDebugLevel(1);
		assertEquals(1, buildScanner.getDebugLevel());
		
		/* set to 2 */
		buildScanner.setDebugLevel(2);
		assertEquals(2, buildScanner.getDebugLevel());

		/* set to 3 - should revert to 2 */
		buildScanner.setDebugLevel(3);
		assertEquals(2, buildScanner.getDebugLevel());

		/* set to -1 - should revert to 0*/
		buildScanner.setDebugLevel(-1);
		assertEquals(0, buildScanner.getDebugLevel());
	}
	
	/*-------------------------------------------------------------------------------------*/
	
}
