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

package com.buildml.model;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.impl.BuildStore;
import com.buildml.model.impl.FileNameSpaces;
import com.buildml.utils.errors.ErrorCode;
import com.buildml.utils.version.Version;


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

	/*-------------------------------------------------------------------------------------*/

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

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.impl.BuildStore#getBuildStoreVersion()}.
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

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the creation of a build store with savedRequired turned off
	 */
	@Test
	public void testBuildStoreWithoutSavedRequired() {
		File bsFile = new File("/tmp/testBuildStore.bml");
		bsFile.delete();
		try {
			/* new BuildStore with "savedRequired" turned off */
			bs = new BuildStore(bsFile.toString(), false);
		} catch (Exception e) {
			fail();
		}
		
		FileNameSpaces fileMgr = bs.getFileNameSpaces();
		fileMgr.addFile("/file");		
		bs.close();
		
		/* reopen the BuildStore and see if the modification is intact */
		try {
			bs = new BuildStore(bsFile.toString());
		} catch (Exception e) {
			fail();
		}
		fileMgr = bs.getFileNameSpaces();
		assertNotSame(ErrorCode.BAD_PATH, fileMgr.getPath("/file"));
		bs.close();
		bsFile.delete();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the creation of a build store with savedRequired turned on, where the
	 * BuildStore doesn't get saved.
	 */
	@Test
	public void testBuildStoreWithSavedRequiredWithoutSave() {
		File bsFile = new File("/tmp/testBuildStore.bml");
		bsFile.delete();
		try {
			/* new BuildStore with "savedRequired" turned on */
			bs = new BuildStore(bsFile.toString(), true);
		} catch (Exception e) {
			fail();
		}
		
		FileNameSpaces fileMgr = bs.getFileNameSpaces();
		fileMgr.addFile("/file");
		
		/* NOT saved, changes are discarded */
		bs.close();
		
		/* reopen the BuildStore and see if the modification is intact */
		try {
			bs = new BuildStore(bsFile.toString());
		} catch (Exception e) {
			fail();
		}
		fileMgr = bs.getFileNameSpaces();
		assertEquals(ErrorCode.BAD_PATH, fileMgr.getPath("/file"));
		bs.close();
		bsFile.delete();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the creation of a build store with savedRequired turned on, and the
	 * BuildStore gets saved.
	 */
	@Test
	public void testBuildStoreWithSavedRequiredWithSave() {
		File bsFile = new File("/tmp/testBuildStore.bml");
		bsFile.delete();
		try {
			/* new BuildStore with "savedRequired" turned on */
			bs = new BuildStore(bsFile.toString(), true);
		} catch (Exception e) {
			fail();
		}
		
		FileNameSpaces fileMgr = bs.getFileNameSpaces();
		fileMgr.addFile("/file");
		
		/* Saved, with changes intact */
		try {
			bs.save();
		} catch (IOException e1) {
			fail("Unable to save database.");
		}
		bs.close();
		
		/* reopen the BuildStore and see if the modification is intact */
		try {
			bs = new BuildStore(bsFile.toString());
		} catch (Exception e) {
			fail();
		}
		fileMgr = bs.getFileNameSpaces();
		assertNotSame(ErrorCode.BAD_PATH, fileMgr.getPath("/file"));
		bs.close();
		bsFile.delete();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the creation of a build store with savedRequired turned on, and the
	 * BuildStore gets saved with saveAs();
	 */
	@Test
	public void testBuildStoreWithSavedRequiredWithSaveAs() {
		File bsFile = new File("/tmp/testBuildStore.bml");
		File bsFileSaveAs = new File("/tmp/testBuildStoreSaveAs.bml");
		bsFile.delete();
		bsFileSaveAs.delete();
		
		try {
			/* new BuildStore with "savedRequired" turned on */
			bs = new BuildStore(bsFile.toString(), true);
		} catch (Exception e) {
			fail();
		}
		
		/* make a first change */
		FileNameSpaces fileMgr = bs.getFileNameSpaces();
		fileMgr.addFile("/file1");
		
		/* Save as..., with first change intact */
		try {
			bs.saveAs(bsFileSaveAs.toString());
		} catch (IOException e2) {
			fail("Unable to saveAs database.");
		}
		
		/* make a second change */
		fileMgr.addFile("/file2");
		
		/* save to the new location */
		try {
			bs.save();
		} catch (IOException e1) {
			fail("Unable to save database.");
		}
		bs.close();
		
		/* reopen the BuildStore and see if the modification is intact */
		try {
			bs = new BuildStore(bsFileSaveAs.toString());
		} catch (Exception e) {
			fail();
		}
		fileMgr = bs.getFileNameSpaces();
		assertNotSame(ErrorCode.BAD_PATH, fileMgr.getPath("/file1"));
		assertNotSame(ErrorCode.BAD_PATH, fileMgr.getPath("/file2"));
		bs.close();
		bsFile.delete();
		bsFileSaveAs.delete();
	}
	

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the creation of a build store with savedRequired turned on, and the
	 * BuildStore gets saved to a bad location. Should give an error.
	 */
	@Test
	public void testBuildStoreWithFailedSaveAs() {
		File bsFile = new File("/tmp/testBuildStore.bml");
		File bsFileSaveAs = new File("/bad-file-location");
		bsFile.delete();
		
		try {
			/* new BuildStore with "savedRequired" turned on */
			bs = new BuildStore(bsFile.toString(), true);
		} catch (Exception e) {
			fail();
		}
				
		/* Save as... should fail. */
		try {
			bs.saveAs(bsFileSaveAs.toString());
			fail("Save-as should have failed, but didn't.");
		} catch (IOException e2) {
			/* pass */
		}
		bs.close();
		bsFile.delete();
	}
	
	/*-------------------------------------------------------------------------------------*/
}
