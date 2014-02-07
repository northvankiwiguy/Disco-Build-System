/*******************************************************************************
 * Copyright (c) 2014 Arapiki Solutions Inc.
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

import com.buildml.utils.string.PathUtils;

/**
 * This class provides unit tests for the BuildStoreUtils class.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestBuildStoreUtils {

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
	 * Test the isValidSlotName() method
	 * @throws Exception
	 */
	@Test
	public void testIsValidSlotName() throws Exception {

		/* test valid slot names */
		assertTrue(BuildStoreUtils.isValidSlotName("cmdSlot"));
		assertTrue(BuildStoreUtils.isValidSlotName("cmdSlot3"));
		assertTrue(BuildStoreUtils.isValidSlotName("slot_name"));
		assertTrue(BuildStoreUtils.isValidSlotName("slot-name"));
		
		/* test invalid slot names */
		assertFalse(BuildStoreUtils.isValidSlotName(null));
		assertFalse(BuildStoreUtils.isValidSlotName(""));
		assertFalse(BuildStoreUtils.isValidSlotName("a"));
		assertFalse(BuildStoreUtils.isValidSlotName("bc"));
		assertFalse(BuildStoreUtils.isValidSlotName("3abc"));
		assertFalse(BuildStoreUtils.isValidSlotName("abcd&e"));
		assertFalse(BuildStoreUtils.isValidSlotName("abcd&"));
	}

	/*-------------------------------------------------------------------------------------*/

}
