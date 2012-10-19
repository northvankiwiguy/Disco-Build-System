/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.model.types;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.buildml.model.CommonTestUtils;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.model.types.PackageSet;


/**
 * Test methods for validating the PackageSet class.
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestPackageSet {

	/** The BuildStore that contains the packages */
	private IBuildStore bs;
	
	/** The corresponding Packages class */
	private IPackageMgr pkgMgr;
	
	/** The PackageSet under test */
	private PackageSet pkgSet;
	
	/** Various package IDs */
	private int idPkg1, idPkg2, idGreenPkg, idBluePkg, idYellowPkg, idMauvePkg;
	
	/** The scope values */
	private final int ID_SCOPE_NONE = 0;
	private final int ID_SCOPE_PRIVATE = 1;
	private final int ID_SCOPE_PUBLIC = 2;
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Setup() method, run before each test case is executed. Creates a new BuildStore
	 * and a new empty PackageSet that's tied to that BuildStore.
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		bs = CommonTestUtils.getEmptyBuildStore();
		pkgMgr = bs.getPackageMgr();
		pkgSet = new PackageSet(bs);
		
		/* add a bunch of packages to the BuildStore */
		idPkg1 = pkgMgr.addPackage("Pkg1");
		idPkg2 = pkgMgr.addPackage("Pkg2");
		idGreenPkg = pkgMgr.addPackage("GreenPkg");
		idBluePkg = pkgMgr.addPackage("BluePkg");
		idYellowPkg = pkgMgr.addPackage("YellowPkg");
		idMauvePkg = pkgMgr.addPackage("MauvePkg");
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the setDefault() method.
	 */
	@Test
	public void testSetDefault() {
	
		/* start with default of false, and check that nothing is present */
		assertFalse(pkgSet.isMember(idPkg1));
		assertFalse(pkgSet.isMember(idPkg1, ID_SCOPE_PRIVATE));
		assertFalse(pkgSet.isMember(idPkg1, ID_SCOPE_PUBLIC));
		
		/* switch over to default of true*/
		pkgSet.setDefault(true);
		assertTrue(pkgSet.isMember(idPkg1));
		assertTrue(pkgSet.isMember(idPkg1, ID_SCOPE_PRIVATE));
		assertTrue(pkgSet.isMember(idPkg1, ID_SCOPE_PUBLIC));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the add() and isMember methods.
	 */
	@Test
	public void testAddIsMember() {

		/* add some packages/scopes */
		pkgSet.add(idGreenPkg);
		pkgSet.add(idYellowPkg, ID_SCOPE_PRIVATE);
		pkgSet.add(idBluePkg, ID_SCOPE_PUBLIC);

		/* validate membership */
		assertTrue(pkgSet.isMember(idGreenPkg));
		assertTrue(pkgSet.isMember(idGreenPkg, ID_SCOPE_NONE));
		assertTrue(pkgSet.isMember(idGreenPkg, ID_SCOPE_PRIVATE));
		assertTrue(pkgSet.isMember(idGreenPkg, ID_SCOPE_PUBLIC));
		
		/* if one or more scopes are members, then so is the whole package */
		assertTrue(pkgSet.isMember(idYellowPkg));
		assertFalse(pkgSet.isMember(idYellowPkg, ID_SCOPE_NONE));
		assertTrue(pkgSet.isMember(idYellowPkg, ID_SCOPE_PRIVATE));
		assertFalse(pkgSet.isMember(idYellowPkg, ID_SCOPE_PUBLIC));
		
		assertTrue(pkgSet.isMember(idBluePkg));
		assertFalse(pkgSet.isMember(idBluePkg, ID_SCOPE_NONE));
		assertFalse(pkgSet.isMember(idBluePkg, ID_SCOPE_PRIVATE));
		assertTrue(pkgSet.isMember(idBluePkg, ID_SCOPE_PUBLIC));
	
		/* if the package was never added/remove, it'll default to false */
		assertFalse(pkgSet.isMember(idPkg1));
		assertFalse(pkgSet.isMember(idPkg1, ID_SCOPE_NONE));
		assertFalse(pkgSet.isMember(idPkg1, ID_SCOPE_PRIVATE));
		assertFalse(pkgSet.isMember(idPkg1, ID_SCOPE_PUBLIC));
	
		/* unless we set the default to true */
		pkgSet.setDefault(true);
		assertTrue(pkgSet.isMember(idPkg1));
		assertTrue(pkgSet.isMember(idPkg1, ID_SCOPE_NONE));
		assertTrue(pkgSet.isMember(idPkg1, ID_SCOPE_PRIVATE));
		assertTrue(pkgSet.isMember(idPkg1, ID_SCOPE_PUBLIC));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the remove() and isMember methods.
	 */
	@Test
	public void testRemoveIsMember() {

		/* add some packages/scopes */
		pkgSet.add(idPkg1);
		pkgSet.add(idPkg2, ID_SCOPE_PRIVATE);
		pkgSet.add(idYellowPkg, ID_SCOPE_PUBLIC);

		/* test their initial membership */
		assertTrue(pkgSet.isMember(idPkg1));
		assertTrue(pkgSet.isMember(idPkg1, ID_SCOPE_PRIVATE));
		assertTrue(pkgSet.isMember(idPkg1, ID_SCOPE_PUBLIC));		
		assertTrue(pkgSet.isMember(idPkg2));
		assertTrue(pkgSet.isMember(idPkg2, ID_SCOPE_PRIVATE));
		assertFalse(pkgSet.isMember(idPkg2, ID_SCOPE_PUBLIC));
		assertTrue(pkgSet.isMember(idYellowPkg));
		assertFalse(pkgSet.isMember(idYellowPkg, ID_SCOPE_PRIVATE));
		assertTrue(pkgSet.isMember(idYellowPkg, ID_SCOPE_PUBLIC));
		
		/* remove idPkg2/private */
		pkgSet.remove(idPkg2, ID_SCOPE_PRIVATE);
		assertFalse(pkgSet.isMember(idPkg2));
		assertFalse(pkgSet.isMember(idPkg2, ID_SCOPE_PRIVATE));
		assertFalse(pkgSet.isMember(idPkg2, ID_SCOPE_PUBLIC));
		
		/* remove idYellowPkg */
		pkgSet.remove(idYellowPkg);
		assertFalse(pkgSet.isMember(idYellowPkg));
		assertFalse(pkgSet.isMember(idYellowPkg, ID_SCOPE_PRIVATE));
		assertFalse(pkgSet.isMember(idYellowPkg, ID_SCOPE_PUBLIC));
		
		/* remove greenPkg */
		pkgSet.remove(idGreenPkg);
		assertFalse(pkgSet.isMember(idGreenPkg));
		assertFalse(pkgSet.isMember(idGreenPkg, ID_SCOPE_PRIVATE));
		assertFalse(pkgSet.isMember(idGreenPkg, ID_SCOPE_PUBLIC));
		
		/* set default to true - test BluePkg, then remove BluePkg */
		pkgSet.setDefault(true);
		assertTrue(pkgSet.isMember(idBluePkg));
		assertTrue(pkgSet.isMember(idBluePkg, ID_SCOPE_PRIVATE));
		assertTrue(pkgSet.isMember(idBluePkg, ID_SCOPE_PUBLIC));
		pkgSet.remove(idBluePkg);
		assertFalse(pkgSet.isMember(idBluePkg));
		assertFalse(pkgSet.isMember(idBluePkg, ID_SCOPE_PRIVATE));
		assertFalse(pkgSet.isMember(idBluePkg, ID_SCOPE_PUBLIC));
		
		/* remove a single scope from a package that was true by default */
		pkgSet.remove(idMauvePkg, ID_SCOPE_PRIVATE);
		assertTrue(pkgSet.isMember(idMauvePkg));
		assertFalse(pkgSet.isMember(idMauvePkg, ID_SCOPE_PRIVATE));
		assertTrue(pkgSet.isMember(idMauvePkg, ID_SCOPE_PUBLIC));
	}
	
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the clone operation, which provides a deep copy of the object.
	 */
	@Test
	public void testClone() {
		
		PackageSet newCs = null;
		
		/* add a single package */
		pkgSet.add(idBluePkg);
		
		/* clone the object (deeply) */
		try {
			newCs = (PackageSet)pkgSet.clone();
		} catch (CloneNotSupportedException e) {
			fail("cloning failed. " + e.getMessage());
		}
		
		/* add a second package to our original object.*/
		pkgSet.add(idGreenPkg);
		
		/* add a second package to our clone */
		newCs.add(idYellowPkg);
		
		/* validate the original object */
		assertTrue(pkgSet.isMember(idBluePkg));
		assertTrue(pkgSet.isMember(idGreenPkg));
		assertFalse(pkgSet.isMember(idYellowPkg));

		/* validate the clone object */
		assertTrue(newCs.isMember(idBluePkg));
		assertFalse(newCs.isMember(idGreenPkg));
		assertTrue(newCs.isMember(idYellowPkg));	
	}

	/*-------------------------------------------------------------------------------------*/
}
