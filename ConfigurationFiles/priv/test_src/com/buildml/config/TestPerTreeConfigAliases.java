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

/**
 * Unit tests for the PerTreeConfigFile class.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class TestPerTreeConfigAliases {

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
		pkgMgr.addPackage("PkgC");
		pkgMgr.addPackage("PkgD");
		
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
	 * Test the addition of build aliases. 
	 */
	@Test
	public void testAddAlias() {

		/* initially there are no aliases */
		assertEquals(0, config.getAliases().length);
		
		/* add some aliases */
		assertEquals(ErrorCode.OK, 
				config.addAlias("AliasB", new String[] { "PkgA", "PkgD" }));
		assertEquals(ErrorCode.OK, 
				config.addAlias("AliasA", new String[] { "PkgA", "PkgC" }));
		assertEquals(ErrorCode.OK, 
				config.addAlias("AliasC", new String[] { "PkgB", "PkgA" }));
		
		/* verify the list of aliases - must be in alphabetical order */
		assertArrayEquals(new String[] { "AliasA", "AliasB", "AliasC"},
							config.getAliases());
		
		/* verify that each alias's list of packages is correct, and in alphabetical order */
		assertArrayEquals(new String[] { "PkgA", "PkgC" }, config.getAlias("AliasA"));
		assertArrayEquals(new String[] { "PkgA", "PkgD" }, config.getAlias("AliasB"));
		assertArrayEquals(new String[] { "PkgA", "PkgB" }, config.getAlias("AliasC"));
		
		/* overwrite an existing alias with a new value */
		assertEquals(ErrorCode.OK, config.addAlias("AliasB", new String[] { "PkgC" }));		
		assertArrayEquals(new String[] { "AliasA", "AliasB", "AliasC"},
				config.getAliases());
		assertArrayEquals(new String[] { "PkgC" }, config.getAlias("AliasB"));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the removal of build aliases. 
	 */
	@Test
	public void testRemoveAlias() {

		/* add two aliases */
		assertEquals(ErrorCode.OK, 
				config.addAlias("AliasA", new String[] { "PkgA", "PkgC" }));
		assertEquals(ErrorCode.OK, 
				config.addAlias("AliasB", new String[] { "PkgA", "PkgD" }));
		assertArrayEquals(new String[] { "AliasA", "AliasB" }, config.getAliases());
		
		/* now delete an alias */
		assertEquals(ErrorCode.OK, config.removeAlias("AliasA"));

		/* it's no longer in the list of aliases */
		assertArrayEquals(new String[] { "AliasB" }, config.getAliases());
		
		/* it no longer returns a value */
		assertArrayEquals(null, config.getAlias("AliasA"));		
		assertArrayEquals(new String[] { "PkgA", "PkgD" }, config.getAlias("AliasB"));
		
		/* try to delete an alias that was never defined */
		assertEquals(ErrorCode.NOT_FOUND, config.removeAlias(null));
		assertEquals(ErrorCode.NOT_FOUND, config.removeAlias(""));
		assertEquals(ErrorCode.NOT_FOUND, config.removeAlias("foo"));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the addition of invalid aliases.
	 */
	@Test
	public void testBadAliases() {
		
		/* add aliases with invalid names */
		assertEquals(ErrorCode.INVALID_NAME,
				config.addAlias("badname!", new String[] { "PkgA", "PkgC" }));
		assertEquals(ErrorCode.INVALID_NAME,
				config.addAlias("bad name", new String[] { "PkgA", "PkgC" }));
		assertEquals(ErrorCode.INVALID_NAME,
				config.addAlias("", new String[] { "PkgA", "PkgC" }));
		assertEquals(ErrorCode.INVALID_NAME,
				config.addAlias(null, new String[] { "PkgA", "PkgC" }));
		assertEquals(0, config.getAliases().length);
		
		/* add aliases with invalid package lists */
		assertEquals(ErrorCode.BAD_VALUE, config.addAlias("aliasA", null));
		assertEquals(ErrorCode.BAD_VALUE, config.addAlias("aliasA", new String[] { }));
		assertEquals(ErrorCode.BAD_VALUE, config.addAlias("aliasA", new String[] { "badPkg" }));
		assertEquals(ErrorCode.BAD_VALUE, config.addAlias("aliasA", new String[] { "PkgA", "badPkg" }));
		
		/* try to fetch an undefined alias */
		assertNull(config.getAlias("foo"));
		assertNull(config.getAlias(null));
		assertNull(config.getAlias(""));
	}
		
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the saving and loading of aliases.
	 */
	@Test
	public void testLoadSave() {
		
		/* add some aliases */
		assertEquals(ErrorCode.OK, 
				config.addAlias("all", new String[] { "PkgA", "PkgD", "PkgC", "PkgB" }));
		assertEquals(ErrorCode.OK, 
				config.addAlias("default", new String[] { "PkgA", "PkgC" }));
		assertEquals(ErrorCode.OK, 
				config.addAlias("c", new String[] { "PkgC" }));
		
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
		assertArrayEquals(new String[] { "all", "c", "default" }, newConfig.getAliases());
		assertArrayEquals(new String[] { "PkgA", "PkgB", "PkgC", "PkgD" }, config.getAlias("all"));
		assertArrayEquals(new String[] { "PkgA", "PkgC" }, config.getAlias("default"));
		assertArrayEquals(new String[] { "PkgC" }, config.getAlias("c"));
	}

	/*-------------------------------------------------------------------------------------*/

}
