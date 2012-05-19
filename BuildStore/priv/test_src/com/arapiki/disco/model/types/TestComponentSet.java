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

package com.arapiki.disco.model.types;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.CommonTestUtils;
import com.arapiki.disco.model.Components;
import com.buildml.utils.errors.ErrorCode;


/**
 * Test methods for validating the ComponentSet class.
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestComponentSet {

	/** The BuildStore that contains the components */
	private BuildStore bs;
	
	/** The corresponding Components class */
	private Components compts;
	
	/** The ComponentSet under test */
	private ComponentSet cs;
	
	/** Various component IDs */
	private int idComp1, idComp2, idGreenComp, idBlueComp, idYellowComp, idMauveComp;
	
	/** The scope values */
	private final int ID_SCOPE_NONE = 0;
	private final int ID_SCOPE_PRIVATE = 1;
	private final int ID_SCOPE_PUBLIC = 2;
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Setup() method, run before each test case is executed. Creates a new BuildStore
	 * and a new empty ComponentSet that's tied to that BuildStore.
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		bs = CommonTestUtils.getEmptyBuildStore();
		compts = bs.getComponents();
		cs = new ComponentSet(bs);
		
		/* add a bunch of components to the BuildStore */
		idComp1 = compts.addComponent("Comp1");
		idComp2 = compts.addComponent("Comp2");
		idGreenComp = compts.addComponent("GreenComp");
		idBlueComp = compts.addComponent("BlueComp");
		idYellowComp = compts.addComponent("YellowComp");
		idMauveComp = compts.addComponent("MauveComp");
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the setDefault() method.
	 */
	@Test
	public void testSetDefault() {
	
		/* start with default of false, and check that nothing is present */
		assertFalse(cs.isMember(idComp1));
		assertFalse(cs.isMember(idComp1, ID_SCOPE_PRIVATE));
		assertFalse(cs.isMember(idComp1, ID_SCOPE_PUBLIC));
		
		/* switch over to default of true*/
		cs.setDefault(true);
		assertTrue(cs.isMember(idComp1));
		assertTrue(cs.isMember(idComp1, ID_SCOPE_PRIVATE));
		assertTrue(cs.isMember(idComp1, ID_SCOPE_PUBLIC));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the add() and isMember methods.
	 */
	@Test
	public void testAddIsMember() {

		/* add some components/scopes */
		cs.add(idGreenComp);
		cs.add(idYellowComp, ID_SCOPE_PRIVATE);
		cs.add(idBlueComp, ID_SCOPE_PUBLIC);

		/* validate membership */
		assertTrue(cs.isMember(idGreenComp));
		assertTrue(cs.isMember(idGreenComp, ID_SCOPE_NONE));
		assertTrue(cs.isMember(idGreenComp, ID_SCOPE_PRIVATE));
		assertTrue(cs.isMember(idGreenComp, ID_SCOPE_PUBLIC));
		
		/* if one or more scopes are members, then so is the whole component */
		assertTrue(cs.isMember(idYellowComp));
		assertFalse(cs.isMember(idYellowComp, ID_SCOPE_NONE));
		assertTrue(cs.isMember(idYellowComp, ID_SCOPE_PRIVATE));
		assertFalse(cs.isMember(idYellowComp, ID_SCOPE_PUBLIC));
		
		assertTrue(cs.isMember(idBlueComp));
		assertFalse(cs.isMember(idBlueComp, ID_SCOPE_NONE));
		assertFalse(cs.isMember(idBlueComp, ID_SCOPE_PRIVATE));
		assertTrue(cs.isMember(idBlueComp, ID_SCOPE_PUBLIC));
	
		/* if the component was never added/remove, it'll default to false */
		assertFalse(cs.isMember(idComp1));
		assertFalse(cs.isMember(idComp1, ID_SCOPE_NONE));
		assertFalse(cs.isMember(idComp1, ID_SCOPE_PRIVATE));
		assertFalse(cs.isMember(idComp1, ID_SCOPE_PUBLIC));
	
		/* unless we set the default to true */
		cs.setDefault(true);
		assertTrue(cs.isMember(idComp1));
		assertTrue(cs.isMember(idComp1, ID_SCOPE_NONE));
		assertTrue(cs.isMember(idComp1, ID_SCOPE_PRIVATE));
		assertTrue(cs.isMember(idComp1, ID_SCOPE_PUBLIC));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the remove() and isMember methods.
	 */
	@Test
	public void testRemoveIsMember() {

		/* add some components/scopes */
		cs.add(idComp1);
		cs.add(idComp2, ID_SCOPE_PRIVATE);
		cs.add(idYellowComp, ID_SCOPE_PUBLIC);

		/* test their initial membership */
		assertTrue(cs.isMember(idComp1));
		assertTrue(cs.isMember(idComp1, ID_SCOPE_PRIVATE));
		assertTrue(cs.isMember(idComp1, ID_SCOPE_PUBLIC));		
		assertTrue(cs.isMember(idComp2));
		assertTrue(cs.isMember(idComp2, ID_SCOPE_PRIVATE));
		assertFalse(cs.isMember(idComp2, ID_SCOPE_PUBLIC));
		assertTrue(cs.isMember(idYellowComp));
		assertFalse(cs.isMember(idYellowComp, ID_SCOPE_PRIVATE));
		assertTrue(cs.isMember(idYellowComp, ID_SCOPE_PUBLIC));
		
		/* remove idComp2/private */
		cs.remove(idComp2, ID_SCOPE_PRIVATE);
		assertFalse(cs.isMember(idComp2));
		assertFalse(cs.isMember(idComp2, ID_SCOPE_PRIVATE));
		assertFalse(cs.isMember(idComp2, ID_SCOPE_PUBLIC));
		
		/* remove idYellowComp */
		cs.remove(idYellowComp);
		assertFalse(cs.isMember(idYellowComp));
		assertFalse(cs.isMember(idYellowComp, ID_SCOPE_PRIVATE));
		assertFalse(cs.isMember(idYellowComp, ID_SCOPE_PUBLIC));
		
		/* remove greenComp */
		cs.remove(idGreenComp);
		assertFalse(cs.isMember(idGreenComp));
		assertFalse(cs.isMember(idGreenComp, ID_SCOPE_PRIVATE));
		assertFalse(cs.isMember(idGreenComp, ID_SCOPE_PUBLIC));
		
		/* set default to true - test BlueComp, then remove BlueComp */
		cs.setDefault(true);
		assertTrue(cs.isMember(idBlueComp));
		assertTrue(cs.isMember(idBlueComp, ID_SCOPE_PRIVATE));
		assertTrue(cs.isMember(idBlueComp, ID_SCOPE_PUBLIC));
		cs.remove(idBlueComp);
		assertFalse(cs.isMember(idBlueComp));
		assertFalse(cs.isMember(idBlueComp, ID_SCOPE_PRIVATE));
		assertFalse(cs.isMember(idBlueComp, ID_SCOPE_PUBLIC));
		
		/* remove a single scope from a component that was true by default */
		cs.remove(idMauveComp, ID_SCOPE_PRIVATE);
		assertTrue(cs.isMember(idMauveComp));
		assertFalse(cs.isMember(idMauveComp, ID_SCOPE_PRIVATE));
		assertTrue(cs.isMember(idMauveComp, ID_SCOPE_PUBLIC));
	}
	
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the clone operation, which provides a deep copy of the object.
	 */
	@Test
	public void testClone() {
		
		ComponentSet newCs = null;
		
		/* add a single component */
		cs.add(idBlueComp);
		
		/* clone the object (deeply) */
		try {
			newCs = (ComponentSet)cs.clone();
		} catch (CloneNotSupportedException e) {
			fail("cloning failed. " + e.getMessage());
		}
		
		/* add a second component to our original object.*/
		cs.add(idGreenComp);
		
		/* add a second component to our clone */
		newCs.add(idYellowComp);
		
		/* validate the original object */
		assertTrue(cs.isMember(idBlueComp));
		assertTrue(cs.isMember(idGreenComp));
		assertFalse(cs.isMember(idYellowComp));

		/* validate the clone object */
		assertTrue(newCs.isMember(idBlueComp));
		assertFalse(newCs.isMember(idGreenComp));
		assertTrue(newCs.isMember(idYellowComp));	
	}

	/*-------------------------------------------------------------------------------------*/
}
