/*******************************************************************************
 * Copyright (c) 2010 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.arapiki.disco.model;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.arapiki.utils.version.Version;


/**
 * Test cases for validating the BuildStore class.
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestBuildStore {

	/** Our BuildStore object, used in many test cases */
	private BuildStore bs;

	/** The BuildStoreFileSpace associated with this BuildStore */
	FileNameSpaces bsfs;

	/**
	 * Setup code - create a new/empty BuildStore.
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	
		/* get a new empty BuildStore */
		bs = CommonTestUtils.getEmptyBuildStore();
		
		/* fetch the associated FileSpace */
		bsfs = bs.getFileNameSpaces();
	}

	/**
	 * Test method for {@link com.arapiki.disco.model.BuildStore#getBuildStoreVersion()}.
	 */
	@Test
	public void testGetBuildStoreVersion() {
		
		/*
		 * Test that a new BuildStore has the correct schema version.
		 */
		try {
			assertEquals(Version.getVersionNumberAsInt(), bs.getBuildStoreVersion());
		} catch (FatalBuildStoreError e) {
			e.printStackTrace();
			fail("Unexpected BuildStoreException");
		}
	}

}
