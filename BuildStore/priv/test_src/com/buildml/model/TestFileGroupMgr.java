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

import com.buildml.utils.errors.ErrorCode;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestFileGroupMgr {

	/** Our BuildStore object, used in many test cases */
	private IBuildStore buildStore;

	/** The managers associated with this BuildStore */
	IFileMgr fileMgr;
	IFileGroupMgr fileGroupMgr;
	IPackageMgr pkgMgr;
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		buildStore = CommonTestUtils.getEmptyBuildStore();
		
		/* fetch the associated managers */
		fileMgr = buildStore.getFileMgr();
		fileGroupMgr = buildStore.getFileGroupMgr();
		pkgMgr = buildStore.getPackageMgr();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test allocation of new file groups.
	 * @throws Exception 
	 */
	@Test
	public void testNewFileGroup() throws Exception {
	
		int pkg1Id = pkgMgr.addPackage("pkg1");
		int pkg2Id = pkgMgr.addPackage("pkg2");
		int pkg3Id = pkgMgr.addPackage("pkg3");
		
		/* add a new file group of each type */
		int group1Id = fileGroupMgr.newSourceGroup(pkg1Id);
		assertNotSame(ErrorCode.NOT_FOUND, group1Id);
		int group2Id = fileGroupMgr.newGeneratedGroup(pkg2Id);
		assertNotSame(ErrorCode.NOT_FOUND, group2Id);		
		int group3Id = fileGroupMgr.newMergeGroup(pkg3Id);
		assertNotSame(ErrorCode.NOT_FOUND, group3Id);
		
		/* test that the allocated IDs are unique */
		assertNotSame(group1Id, group2Id);
		assertNotSame(group2Id, group3Id);
		assertNotSame(group1Id, group3Id);
		
		/* verify the type of each group */
		assertEquals(IFileGroupMgr.SOURCE_GROUP, fileGroupMgr.getGroupType(group1Id));
		assertEquals(IFileGroupMgr.GENERATED_GROUP, fileGroupMgr.getGroupType(group2Id));
		assertEquals(IFileGroupMgr.MERGE_GROUP, fileGroupMgr.getGroupType(group3Id));
		
		/* test that the package IDs are correct */
		assertEquals(pkg1Id, fileGroupMgr.getGroupPkg(group1Id));
		assertEquals(pkg2Id, fileGroupMgr.getGroupPkg(group2Id));
		assertEquals(pkg3Id, fileGroupMgr.getGroupPkg(group3Id));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test adding new file group, but with errored parameters.
	 */
	@Test
	public void testNewFileGroupErrors() {

		int pkgId = pkgMgr.addPackage("pkg");

		/* test with invalid group type */
		assertEquals(ErrorCode.BAD_VALUE, fileGroupMgr.newGroup(pkgId, -1));
		assertEquals(ErrorCode.BAD_VALUE, fileGroupMgr.newGroup(pkgId, 5));
		
		/* test with invalid package ID */
		assertEquals(ErrorCode.NOT_FOUND, fileGroupMgr.newGroup(-1, IFileGroupMgr.SOURCE_GROUP));
		assertEquals(ErrorCode.NOT_FOUND, fileGroupMgr.newGroup(1000, IFileGroupMgr.SOURCE_GROUP));

		/* test get of file group type with bad group Id */
		assertEquals(ErrorCode.NOT_FOUND, fileGroupMgr.getGroupType(1000));
		
		/* test get of file group package with bad group Id. */
		assertEquals(ErrorCode.NOT_FOUND, fileGroupMgr.getGroupPkg(1000));
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Change the package associated with a file group.
	 * @throws Exception 
	 */
	@Test
	public void testChangeGroupPackage() throws Exception {

		int pkg1Id = pkgMgr.addPackage("pkg1");
		int pkg2Id = pkgMgr.addPackage("pkg2");
		
		/* create a couple of file groups in one package */
		int group1Id = fileGroupMgr.newSourceGroup(pkg1Id);
		int group2Id = fileGroupMgr.newSourceGroup(pkg1Id);
		assertEquals(pkg1Id, fileGroupMgr.getGroupPkg(group1Id));
		assertEquals(pkg1Id, fileGroupMgr.getGroupPkg(group2Id));
		
		/* change one of the groups to another package */
		assertEquals(ErrorCode.OK, fileGroupMgr.setGroupPkg(group2Id, pkg2Id));
		assertEquals(pkg1Id, fileGroupMgr.getGroupPkg(group1Id));
		assertEquals(pkg2Id, fileGroupMgr.getGroupPkg(group2Id));
		
		/* try changing with an invalid group ID */
		assertEquals(ErrorCode.NOT_FOUND, fileGroupMgr.setGroupPkg(100, pkg2Id));
		
		/* try changing with an invalid package ID */
		assertEquals(ErrorCode.BAD_VALUE, fileGroupMgr.setGroupPkg(group2Id, 100));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	  * Test removing a newly-added (empty) group.
	  */
	@Test
	public void testRemoveNewGroup() {
		
		int pkgId = pkgMgr.addPackage("pkg");
		int groupId = fileGroupMgr.newGeneratedGroup(pkgId);
		
		/* test parameters of group, before it's deleted */
		assertEquals(IFileGroupMgr.GENERATED_GROUP, fileGroupMgr.getGroupType(groupId));
		assertEquals(pkgId, fileGroupMgr.getGroupPkg(groupId));
		assertEquals(0, fileGroupMgr.getGroupSize(groupId));
		
		/* delete the group and test again */
		assertEquals(ErrorCode.OK, fileGroupMgr.removeGroup(groupId));
		assertEquals(ErrorCode.NOT_FOUND, fileGroupMgr.getGroupType(groupId));
		assertEquals(ErrorCode.NOT_FOUND, fileGroupMgr.getGroupPkg(groupId));
		assertEquals(ErrorCode.NOT_FOUND, fileGroupMgr.getGroupSize(groupId));
		
		/* try to delete a second time - should fail */
		assertEquals(ErrorCode.NOT_FOUND, fileGroupMgr.removeGroup(groupId));
		
		/* try to delete with a bad group ID - should fail */
		assertEquals(ErrorCode.NOT_FOUND, fileGroupMgr.removeGroup(1234));
	}
	
	/*-------------------------------------------------------------------------------------*/

	// TODO: try to remove a non-empty group.
	
	/*-------------------------------------------------------------------------------------*/
}
