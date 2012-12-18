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

package com.buildml.model;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the PackageRootMgr class.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestPackageRootMgr {

	/** Our BuildStore object, used in many test cases */
	private IBuildStore buildStore;

	/** The Packages object associated with this BuildStore */
	private IPackageMgr pkgMgr;

	/** The PackageRootMgr object associated with this BuildStore */
	private IPackageRootMgr pkgRootMgr;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		buildStore = CommonTestUtils.getEmptyBuildStore();
		pkgMgr = buildStore.getPackageMgr();
		pkgRootMgr = buildStore.getPackageRootMgr();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test creation of a new empty BuildStore.
	 */
	@Test
	public void testNewBuildStore() {

		/* by default, the workspace directory is the same as the build.bml file's directory */
		assertEquals(0, pkgRootMgr.getRelativeWorkspace());

		/* the "root" root is always at /, and "workspace" will be at "/tmp" */
		assertEquals("/", pkgRootMgr.getRootNativePath("root"));
		assertEquals("/tmp", pkgRootMgr.getRootNativePath("workspace"));

		/* check for two roots - in alphabetical order */
		assertArrayEquals(new String[] { "root", "workspace" }, pkgRootMgr.getRoots());
	}

	/*-------------------------------------------------------------------------------------*/
}
