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

import java.io.File;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for validating the BuildStore class.
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestBuildStore {

	/** Our BuildStore object, used in many test cases */
	private BuildStore bs;

	/** The BuildStoreFileSpace associated with this BuildStore */
	BuildStoreFileSpace bsfs;

	/**
	 * Setup code - create a new/empty BuildStore.
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		/* create a totally new BuildStore, after deleting the old file */
		File dbFile = new File("/tmp/MyFirstDB.db");
		dbFile.delete();
		bs = new BuildStore("/tmp/MyFirstDB");
		
		/* fetch the associated FileSpace */
		bsfs = bs.getBuildStoreFileSpace();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
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
			assertEquals(1, bs.getBuildStoreVersion());
		} catch (FatalBuildStoreError e) {
			e.printStackTrace();
			fail("Unexpected BuildStoreException");
		}
	}

	/**
	 * Test that we can add 100,000 file names into the file table. This
	 * is more of a timing test than a functionality test.
	 */
	@Test
	public void testFilesTable() {
		
		bs.setFastAccessMode(true);

		/* create a large number of randomly-generated file names */
		Random r = new Random();
		for (int i = 0; i != 100000; i++) {
			/* file names can be 10-200 characters long. */
			int len = r.nextInt(190) + 10;
			StringBuffer sb = new StringBuffer(40);
			for (int j = 0; j != len; j++) {
				sb.append((char) (r.nextInt(26) + 65));
			}

			/* add the file name to the FileSpace */
			bsfs.addFile(sb.toString());
		}
		bs.setFastAccessMode(false);
		
		// TODO: count the number of records to validate success.
	}
	
}
