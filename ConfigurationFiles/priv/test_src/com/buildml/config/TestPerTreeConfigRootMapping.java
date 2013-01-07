/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.config;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.buildml.config.PerTreeConfigFile;
import com.buildml.model.CommonTestUtils;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.utils.errors.ErrorCode;
import com.buildml.utils.os.SystemUtils;

/**
 * Unit tests for the PerTreeConfigFile class.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class TestPerTreeConfigRootMapping {

	/** The object under test */
	private PerTreeConfigFile config = null;
	
	/** The BuildStore associated with the object under test */
	private IBuildStore buildStore = null;
	
	/** The temporary config file, used for testing */
	private File tmpConfigFile = null;
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		/* create a temporary BuildStore that this config file is associated with */
		buildStore = CommonTestUtils.getEmptyBuildStore();
		
		/* add some default packages */
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		pkgMgr.addPackage("PkgA");
		pkgMgr.addPackage("PkgB");
		
		/* create a temporary file that we'll save config information to */
		tmpConfigFile = File.createTempFile("bmlconfig", null);
		tmpConfigFile.delete();
		
		/* finally, create the object under test */
		config = new PerTreeConfigFile(buildStore, tmpConfigFile);
		
		tmpConfigFile.deleteOnExit();
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
	 * Test the addition of native path mappings. 
	 * @throws IOException 
	 */
	@Test
	public void testAddMapping() throws IOException {
		
		/* create some directory to which the root will be pointed */
		File dir1 = SystemUtils.createTempDir();
		dir1.deleteOnExit();
		File dir2 = SystemUtils.createTempDir();
		dir2.deleteOnExit();
		
		/* initially, the roots are not mapped */
		assertNull(config.getNativeRootMapping("PkgA_src"));
		assertNull(config.getNativeRootMapping("PkgA_gen"));

		/* map a single root, and check the mappings. */
		assertEquals(ErrorCode.OK, config.addNativeRootMapping("PkgA_src", dir1.toString()));
		assertEquals(dir1.toString(), config.getNativeRootMapping("PkgA_src"));
		assertNull(config.getNativeRootMapping("PkgA_gen"));
		
		/* map the other root, and check the mappings */
		assertEquals(ErrorCode.OK, config.addNativeRootMapping("PkgA_gen", dir2.toString()));
		assertEquals(dir1.toString(), config.getNativeRootMapping("PkgA_src"));
		assertEquals(dir2.toString(), config.getNativeRootMapping("PkgA_gen"));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the removal of a native path mapping.
	 * @throws IOException 
	 */
	@Test
	public void testRemoveMapping() throws IOException {
		File dir1 = SystemUtils.createTempDir();
		dir1.deleteOnExit();

		/* add the mapping */
		assertEquals(ErrorCode.OK, config.addNativeRootMapping("PkgA_src", dir1.toString()));
		assertEquals(dir1.toString(), config.getNativeRootMapping("PkgA_src"));

		/* remove the mapping */
		assertEquals(ErrorCode.OK, config.clearNativeRootMapping("PkgA_src"));
		assertNull(config.getNativeRootMapping("PkgA_src"));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the addition of invalid arguments.
	 * @throws IOException 
	 */
	@Test
	public void testBadMapping() throws IOException {
		File dir1 = SystemUtils.createTempDir();
		dir1.deleteOnExit();
		
		/* test adding with a bad root name (bad package, and bad suffix) */
		assertEquals(ErrorCode.NOT_FOUND, config.addNativeRootMapping("PkgC_src", dir1.toString()));
		assertEquals(ErrorCode.NOT_FOUND, config.addNativeRootMapping("PkgA_soc", dir1.toString()));
		assertEquals(ErrorCode.NOT_FOUND, config.addNativeRootMapping("", dir1.toString()));
		assertEquals(ErrorCode.NOT_FOUND, config.addNativeRootMapping(null, dir1.toString()));

		/* test with bad native path */
		assertEquals(ErrorCode.BAD_PATH, config.addNativeRootMapping("PkgA_src", "/bad-path"));
		assertEquals(ErrorCode.BAD_PATH, config.addNativeRootMapping("PkgA_src", "/etc/passwd"));		
		
		/* test clearing the mapping with invalid names */
		assertEquals(ErrorCode.NOT_FOUND, config.clearNativeRootMapping("PkgC_src"));
		assertEquals(ErrorCode.NOT_FOUND, config.clearNativeRootMapping("PkgA_soc"));
		assertEquals(ErrorCode.NOT_FOUND, config.clearNativeRootMapping(""));
		assertEquals(ErrorCode.NOT_FOUND, config.clearNativeRootMapping(null));
		
		/* test getting with invalid names */
		assertNull(config.getNativeRootMapping("PkgC_src"));
		assertNull(config.getNativeRootMapping("PkgA_soc"));
		assertNull(config.getNativeRootMapping(""));
		assertNull(config.getNativeRootMapping(null));
	}
		
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the saving and loading of root mappings.
	 * @throws IOException 
	 */
	@Test
	public void testLoadSave() throws IOException {
		
		File dir1 = SystemUtils.createTempDir();
		dir1.deleteOnExit();
		File dir2 = SystemUtils.createTempDir();
		dir2.deleteOnExit();
		
		/* add some mappings */
		assertEquals(ErrorCode.OK, config.addNativeRootMapping("PkgA_src", dir1.toString()));
		assertEquals(ErrorCode.OK, config.addNativeRootMapping("PkgB_gen", dir2.toString()));
		
		/* save the file */
		try {
			config.save();
		} catch (IOException e) {
			fail("Failed to save config file content.");
		}
		
		/* load the file into another config object */
		PerTreeConfigFile newConfig = null;
		try {
			newConfig = new PerTreeConfigFile(buildStore, tmpConfigFile);
		} catch (IOException e) {
			fail("Failed to load configuration file content.");
		}
				
		/* verify the content */
		assertEquals(dir1.toString(), newConfig.getNativeRootMapping("PkgA_src"));
		assertEquals(dir2.toString(), newConfig.getNativeRootMapping("PkgB_gen"));
	}

	/*-------------------------------------------------------------------------------------*/

}
