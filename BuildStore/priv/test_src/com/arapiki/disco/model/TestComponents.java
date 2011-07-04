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

package com.arapiki.disco.model;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.arapiki.utils.errors.ErrorCode;

/**
 * Unit tests for the Components class.
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestComponents {

	/** Our BuildStore object, used in many test cases */
	private BuildStore bs;

	/** The Components object associated with this BuildStore */
	private Components cmpts;
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		bs = CommonTestUtils.getEmptyBuildStore();
		cmpts = bs.getComponents();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.Components#addComponent(java.lang.String)}.
	 */
	@Test
	public void testAddComponent() {
		
		/* add valid component names, and check their IDs are unique */
		int c1 = cmpts.addComponent("component1");
		assertNotSame(ErrorCode.ALREADY_USED, c1);
		assertNotSame(ErrorCode.INVALID_NAME, c1);

		int c2 = cmpts.addComponent("my_second_component");
		assertNotSame(ErrorCode.ALREADY_USED, c2);
		assertNotSame(ErrorCode.INVALID_NAME, c2);
		assertNotSame(c1, c2);

		int c3 = cmpts.addComponent("1more-component");
		assertNotSame(ErrorCode.ALREADY_USED, c3);
		assertNotSame(ErrorCode.INVALID_NAME, c3);
		assertNotSame(c1, c3);
		assertNotSame(c2, c3);
		
		/* add components that have already been added */
		int c4 = cmpts.addComponent("component1");
		assertEquals(ErrorCode.ALREADY_USED, c4);
		int c5 = cmpts.addComponent("my_second_component");
		assertEquals(ErrorCode.ALREADY_USED, c5);
		
		/* add components with invalid names */
		int c6 = cmpts.addComponent("comp*with-bad_name");
		assertEquals(ErrorCode.INVALID_NAME, c6);
		int c7 = cmpts.addComponent("comp with-bad name");
		assertEquals(ErrorCode.INVALID_NAME, c7);
		int c8 = cmpts.addComponent("comp/with-bad name");
		assertEquals(ErrorCode.INVALID_NAME, c8);
		int c9 = cmpts.addComponent("comp+with-bad name");
		assertEquals(ErrorCode.INVALID_NAME, c9);
		int c10 = cmpts.addComponent(null);
		assertEquals(ErrorCode.INVALID_NAME, c10);
		int c11 = cmpts.addComponent("ab");
		assertEquals(ErrorCode.INVALID_NAME, c11);	
		
		/* try to add the None component, which exists by default */
		int cNone = cmpts.addComponent("None");
		assertEquals(ErrorCode.ALREADY_USED, cNone);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.Components#getComponentName(int)}.
	 */
	@Test
	public void testGetComponentName() {
		
		/* add component names, then make sure their names are correct */
		int c1 = cmpts.addComponent("CompA");
		assertEquals("CompA", cmpts.getComponentName(c1));
		int c2 = cmpts.addComponent("my-other-component");
		assertEquals("my-other-component", cmpts.getComponentName(c2));
		int c3 = cmpts.addComponent("and_a_3rd");
		assertEquals("and_a_3rd", cmpts.getComponentName(c3));
				
		/* try to fetch names for invalid component IDs */
		assertEquals(null, cmpts.getComponentName(1000));		
		assertEquals(null, cmpts.getComponentName(-1));
		
		/* test for the default "None" component */
		assertEquals("None", cmpts.getComponentName(0));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.Components#getComponentId(int)}.
	 */
	@Test
	public void testGetComponentId() {
		
		/* get the ID for the "None" component */
		assertEquals(0, cmpts.getComponentId("None"));

		/* fetch component names that haven't been added - should be NOT_FOUND */
		assertEquals(ErrorCode.NOT_FOUND, cmpts.getComponentId("foo"));
		assertEquals(ErrorCode.NOT_FOUND, cmpts.getComponentId("my_component"));
		
		/* add component names, then make sure their IDs are correct */
		int c1 = cmpts.addComponent("my_comp_A");
		int c2 = cmpts.addComponent("ourComponent");
		int c3 = cmpts.addComponent("another-component");
		assertEquals(c1, cmpts.getComponentId("my_comp_A"));
		assertEquals(c3, cmpts.getComponentId("another-component"));
		assertEquals(c2, cmpts.getComponentId("ourComponent"));
		
		/* add duplicate names, and make sure the old ID is returned */
		int c4 = cmpts.addComponent("ourComponent");
		assertEquals(ErrorCode.ALREADY_USED, c4);
		int c5 = cmpts.addComponent("another-component");
		assertEquals(ErrorCode.ALREADY_USED, c5);
		assertEquals(c3, cmpts.getComponentId("another-component"));
		assertEquals(c2, cmpts.getComponentId("ourComponent"));
				
		/* Names that haven't been added will return NOT_FOUND */
		assertEquals(ErrorCode.NOT_FOUND, cmpts.getComponentId("invalid"));
		assertEquals(ErrorCode.NOT_FOUND, cmpts.getComponentId("missing"));
		
		/* try to fetch IDs for invalid component names - returns NOT_FOUND */
		assertEquals(ErrorCode.NOT_FOUND, cmpts.getComponentId("miss*ing"));
		assertEquals(ErrorCode.NOT_FOUND, cmpts.getComponentId("invalid name"));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.Components#removeComponent(java.lang.String)}.
	 */
	@Test
	public void testRemoveComponent() {
		
		/* try to remove component names that haven't been added */
		assertEquals(ErrorCode.NOT_FOUND, cmpts.removeComponent("CompA"));
		assertEquals(ErrorCode.NOT_FOUND, cmpts.removeComponent("my_component"));
		
		/* try to remove the "None" component - should fail */
		assertEquals(ErrorCode.CANT_REMOVE, cmpts.removeComponent("None"));

		/* add component names, then remove them */
		assertTrue(cmpts.addComponent("CompA") > 0);
		assertTrue(cmpts.addComponent("my_component") > 0);	
		assertEquals(ErrorCode.OK, cmpts.removeComponent("CompA"));
		assertEquals(ErrorCode.OK, cmpts.removeComponent("my_component"));
		
		/* try to remove the same names again - should fail */
		assertEquals(ErrorCode.NOT_FOUND, cmpts.removeComponent("CompA"));
		assertEquals(ErrorCode.NOT_FOUND, cmpts.removeComponent("my_component"));
		
		/* add the same component names again - should work */
		assertTrue(cmpts.addComponent("CompA") > 0);
		assertTrue(cmpts.addComponent("my_component") > 0);	
		
		/* TODO: add component names, assign them to files, then try to remove the name */
		fail();
		
		/* TODO: remove them from files, then try again to remove the component name */
		fail();
		
		/* TODO: add component names, assign them to tasks, then try to remove the name */
		fail();
		
		/* TODO: remove them from tasks, then try again to remove the component name */
		fail();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.Components#getComponents()}.
	 */
	@Test
	public void testGetComponents() {
		
		/* fetch the list of components, before adding any. */
		String results[] = cmpts.getComponents();
		assertArrayEquals(new String[] {"None"}, results);
		
		/* add some components, then check the list */
		cmpts.addComponent("my_comp1");
		cmpts.addComponent("my_comp2");
		results = cmpts.getComponents();
		assertArrayEquals(new String[] {"my_comp1", "my_comp2", "None"}, results);		
		
		/* add some more, then check again */
		cmpts.addComponent("Linux");
		cmpts.addComponent("freeBSD");
		results = cmpts.getComponents();
		assertArrayEquals(new String[] {"freeBSD", "Linux", "my_comp1", 
				"my_comp2", "None"}, results);		
		
		/* remove some components, then check the list again */
		cmpts.removeComponent("Linux");
		cmpts.removeComponent("my_comp2");
		results = cmpts.getComponents();
		assertArrayEquals(new String[] {"freeBSD", "my_comp1", "None"}, results);		
		
		/* add some names back, and re-check the list */
		cmpts.addComponent("Linux");
		cmpts.addComponent("MacOS");
		results = cmpts.getComponents();
		assertArrayEquals(new String[] {"freeBSD", "Linux", "MacOS", "my_comp1", 
				"None"}, results);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for {@link com.arapiki.disco.model.Components#getSectionName()}.
	 */
	@Test
	public void testGetSectionName() {
		
		/* Test valid section IDs */
		assertEquals("private", cmpts.getSectionName(0));
		assertEquals("public", cmpts.getSectionName(1));

		/* Test invalid section IDs */
		assertNull(cmpts.getSectionName(2));
		assertNull(cmpts.getSectionName(3));
		assertNull(cmpts.getSectionName(100));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.Components#getSectionId()}.
	 */
	@Test
	public void testGetSectionId() {
		
		/* Test valid section names */
		assertEquals(0, cmpts.getSectionId("priv"));
		assertEquals(0, cmpts.getSectionId("private"));
		assertEquals(1, cmpts.getSectionId("pub"));
		assertEquals(1, cmpts.getSectionId("public"));
		
		/* Test invalid section names */
		assertEquals(ErrorCode.NOT_FOUND, cmpts.getSectionId("object"));
		assertEquals(ErrorCode.NOT_FOUND, cmpts.getSectionId("obj"));
		assertEquals(ErrorCode.NOT_FOUND, cmpts.getSectionId("pretty"));
		assertEquals(ErrorCode.NOT_FOUND, cmpts.getSectionId("shiny"));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.disco.model.Components#getSections()}.
	 */
	@Test
	public void testGetSections() {

		String sections[] = cmpts.getSections();
		assertEquals(2, sections.length);
		assertEquals("private", sections[0]);
		assertEquals("public", sections[1]);	
	}
	
	/*-------------------------------------------------------------------------------------*/

}
