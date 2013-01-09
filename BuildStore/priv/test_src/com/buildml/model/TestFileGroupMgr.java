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
	
	/** Files, added to fileMgr before each test starts */
	int file1, file2, file3, file4, file5, file6;
	
	/** Packages, added to the packageMgr before each test starts */
	int pkg1Id, pkg2Id, pkg3Id;
	
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
		
		/* add a bunch of files */
		file1 = fileMgr.addFile("/a/b/file1");
		file2 = fileMgr.addFile("/a/b/c/file2");
		file3 = fileMgr.addFile("/a/b/d/file3");
		file4 = fileMgr.addFile("/a/b/c/d/e/file4");
		file5 = fileMgr.addFile("/a/b/c/file5");
		file6 = fileMgr.addFile("/a/b/z/file6");
		
		pkg1Id = pkgMgr.addPackage("pkg1");
		pkg2Id = pkgMgr.addPackage("pkg2");
		pkg3Id = pkgMgr.addPackage("pkg3");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test allocation of new file groups.
	 * @throws Exception 
	 */
	@Test
	public void testNewFileGroup() throws Exception {
		
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

		/* test with invalid group type */
		assertEquals(ErrorCode.BAD_VALUE, fileGroupMgr.newGroup(pkg1Id, -1));
		assertEquals(ErrorCode.BAD_VALUE, fileGroupMgr.newGroup(pkg1Id, 5));
		
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
		
		int groupId = fileGroupMgr.newGeneratedGroup(pkg1Id);
		
		/* test parameters of group, before it's deleted */
		assertEquals(IFileGroupMgr.GENERATED_GROUP, fileGroupMgr.getGroupType(groupId));
		assertEquals(pkg1Id, fileGroupMgr.getGroupPkg(groupId));
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

	/**
	 * Add source paths to a source file group.
	 */
	@Test
	public void testAddSourcePaths() {
		
		/* create a new source file group */
		int groupId = fileGroupMgr.newSourceGroup(pkg1Id);
		assertTrue(groupId >= 0);
		
		/* append some files to the end of the group - test size and positions */
		assertEquals(0, fileGroupMgr.addSourcePath(groupId, file1));
		assertEquals(1, fileGroupMgr.addSourcePath(groupId, file2));
		assertEquals(2, fileGroupMgr.addSourcePath(groupId, file3));
		assertEquals(3, fileGroupMgr.getGroupSize(groupId));
		assertEquals(file3, fileGroupMgr.getSourcePathAt(groupId, 2));
		assertEquals(file2, fileGroupMgr.getSourcePathAt(groupId, 1));
		assertEquals(file1, fileGroupMgr.getSourcePathAt(groupId, 0));
		
		/* insert some files at specific indices within the group - test size and positions */
		assertEquals(2, fileGroupMgr.addSourcePath(groupId, file4, 2));
		assertEquals(0, fileGroupMgr.addSourcePath(groupId, file5, 0));
		assertEquals(2, fileGroupMgr.addSourcePath(groupId, file6, 2));
		assertEquals(6, fileGroupMgr.getGroupSize(groupId));
		
		/* confirm ordering is now: file5, file1, file6, file2, file4, file3 */
		assertEquals(file5, fileGroupMgr.getSourcePathAt(groupId, 0));
		assertEquals(file1, fileGroupMgr.getSourcePathAt(groupId, 1));
		assertEquals(file6, fileGroupMgr.getSourcePathAt(groupId, 2));
		assertEquals(file2, fileGroupMgr.getSourcePathAt(groupId, 3));
		assertEquals(file4, fileGroupMgr.getSourcePathAt(groupId, 4));
		assertEquals(file3, fileGroupMgr.getSourcePathAt(groupId, 5));
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test adding of paths into a source file group, with errors.
	 */
	@Test
	public void testAddSourcePathsError() {
		
		/* create a new source file group */
		int groupId = fileGroupMgr.newSourceGroup(pkg1Id);
		assertTrue(groupId >= 0);
		
		/* test addition of files at invalid locations */
		assertEquals(ErrorCode.OUT_OF_RANGE, fileGroupMgr.addSourcePath(groupId, file1, -2));
		assertEquals(ErrorCode.OUT_OF_RANGE, fileGroupMgr.addSourcePath(groupId, file1, 1));
		
		/* these are valid */
		assertEquals(0, fileGroupMgr.addSourcePath(groupId, file1, 0));
		assertEquals(1, fileGroupMgr.addSourcePath(groupId, file2, -1));
		
		/* test addition source files into non-source group */
		int generatedGroupId = fileGroupMgr.newGeneratedGroup(pkg1Id);
		int mergeGroupId = fileGroupMgr.newMergeGroup(pkg1Id);
		assertEquals(ErrorCode.INVALID_OP, fileGroupMgr.addSourcePath(generatedGroupId, file1));
		assertEquals(ErrorCode.INVALID_OP, fileGroupMgr.addSourcePath(mergeGroupId, file1));
		
		/* test addition of invalid path into source group */
		assertEquals(ErrorCode.BAD_VALUE, fileGroupMgr.addSourcePath(groupId, 100));
		
		/* test getting of paths with invalid group */
		assertEquals(ErrorCode.NOT_FOUND, fileGroupMgr.getSourcePathAt(-1, 0));
		assertEquals(ErrorCode.NOT_FOUND, fileGroupMgr.getSourcePathAt(123, 0));
		
		/* test getting of paths with invalid index */
		assertEquals(ErrorCode.OUT_OF_RANGE, fileGroupMgr.getSourcePathAt(generatedGroupId, -1));
		assertEquals(ErrorCode.OUT_OF_RANGE, fileGroupMgr.getSourcePathAt(generatedGroupId, 3));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Expand a source file group to
	 */
	@Test
	public void testExpandSourceGroup() {
		
		/* create a new group and populate it with paths */
		int groupId = fileGroupMgr.newSourceGroup(pkg1Id);
		assertTrue(groupId >= 0);
		assertEquals(0, fileGroupMgr.addSourcePath(groupId, file1));
		assertEquals(1, fileGroupMgr.addSourcePath(groupId, file6));
		assertEquals(2, fileGroupMgr.addSourcePath(groupId, file3));
		assertEquals(3, fileGroupMgr.addSourcePath(groupId, file5));
		assertEquals(4, fileGroupMgr.addSourcePath(groupId, file4));
		assertEquals(5, fileGroupMgr.addSourcePath(groupId, file2));
		assertEquals(6, fileGroupMgr.addSourcePath(groupId, file3));

		/* expand the content into path strings, and validate the entries */
		String results[] = fileGroupMgr.getExpandedGroupFiles(groupId);
		assertNotNull(results);
		assertEquals(7, results.length);
		assertEquals("@root/a/b/file1", 		results[0]);
		assertEquals("@root/a/b/z/file6", 		results[1]);
		assertEquals("@root/a/b/d/file3", 		results[2]);
		assertEquals("@root/a/b/c/file5", 		results[3]);
		assertEquals("@root/a/b/c/d/e/file4", 	results[4]);
		assertEquals("@root/a/b/c/file2", 		results[5]);
		assertEquals("@root/a/b/d/file3", 		results[6]);

		/* try with an invalid group */
		assertNull(fileGroupMgr.getExpandedGroupFiles(100));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create a source group, add paths, then remove some of them.
	 */
	@Test
	public void testRemoveSourcePaths() {

		/* create a new group and populate it with paths */
		int groupId = fileGroupMgr.newSourceGroup(pkg1Id);
		assertTrue(groupId >= 0);
		assertEquals(0, fileGroupMgr.addSourcePath(groupId, file1));
		assertEquals(1, fileGroupMgr.addSourcePath(groupId, file2));
		assertEquals(2, fileGroupMgr.addSourcePath(groupId, file3));
		assertEquals(3, fileGroupMgr.addSourcePath(groupId, file4));
		assertEquals(4, fileGroupMgr.getGroupSize(groupId));
		assertEquals(ErrorCode.CANT_REMOVE, fileGroupMgr.removeGroup(groupId));
		
		/* attempt remove an entry from an invalid index */
		assertEquals(ErrorCode.OUT_OF_RANGE, fileGroupMgr.removeEntry(groupId, 4));
		assertEquals(ErrorCode.OUT_OF_RANGE, fileGroupMgr.removeEntry(groupId, -1));
		
		/* remove file3, group now has file1, file2, file4 */
		assertEquals(ErrorCode.OK, fileGroupMgr.removeEntry(groupId, 2));
		assertEquals(3, fileGroupMgr.getGroupSize(groupId));
		assertEquals(file1, fileGroupMgr.getSourcePathAt(groupId, 0));
		assertEquals(file2, fileGroupMgr.getSourcePathAt(groupId, 1));
		assertEquals(file4, fileGroupMgr.getSourcePathAt(groupId, 2));
		assertEquals(ErrorCode.CANT_REMOVE, fileGroupMgr.removeGroup(groupId));
				
		/* remove file4, group now has file1, file2 */
		assertEquals(ErrorCode.OK, fileGroupMgr.removeEntry(groupId, 2));
		assertEquals(2, fileGroupMgr.getGroupSize(groupId));
		assertEquals(file1, fileGroupMgr.getSourcePathAt(groupId, 0));
		assertEquals(file2, fileGroupMgr.getSourcePathAt(groupId, 1));
		assertEquals(ErrorCode.CANT_REMOVE, fileGroupMgr.removeGroup(groupId));

		/* remove file1, group now has file2 */
		assertEquals(ErrorCode.OK, fileGroupMgr.removeEntry(groupId, 0));
		assertEquals(1, fileGroupMgr.getGroupSize(groupId));
		assertEquals(ErrorCode.CANT_REMOVE, fileGroupMgr.removeGroup(groupId));
		
		/* remove file2, group is now empty and can be removed. */
		assertEquals(ErrorCode.OK, fileGroupMgr.removeEntry(groupId, 0));		
		assertEquals(0, fileGroupMgr.getGroupSize(groupId));
		assertEquals(ErrorCode.OK, fileGroupMgr.removeGroup(groupId));
		assertEquals(ErrorCode.NOT_FOUND, fileGroupMgr.getGroupSize(groupId));
		
		/* attempt to remove an entry from an invalid group */
		assertEquals(ErrorCode.NOT_FOUND, fileGroupMgr.removeEntry(100, 0));		
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Create a source group, add paths, then reorder them.
	 */
	@Test
	public void testReorderSourcePaths() {

		/* create a new group and populate it with paths */
		int groupId = fileGroupMgr.newSourceGroup(pkg1Id);
		assertTrue(groupId >= 0);
		assertEquals(0, fileGroupMgr.addSourcePath(groupId, file1));
		assertEquals(1, fileGroupMgr.addSourcePath(groupId, file2));
		assertEquals(2, fileGroupMgr.addSourcePath(groupId, file3));
		assertEquals(3, fileGroupMgr.addSourcePath(groupId, file4));
		assertEquals(4, fileGroupMgr.getGroupSize(groupId));

		/* attempt to move paths within an invalid group */
		assertEquals(ErrorCode.NOT_FOUND, fileGroupMgr.moveEntry(1000, 0, 0));

		/* attempt to move paths from/to invalid indices */
		assertEquals(ErrorCode.OUT_OF_RANGE, fileGroupMgr.moveEntry(groupId, 0, 5));
		assertEquals(ErrorCode.OUT_OF_RANGE, fileGroupMgr.moveEntry(groupId, 42, 2));
		
		/* move index 1 to index 1 - no effect */
		assertEquals(ErrorCode.OK, fileGroupMgr.moveEntry(groupId, 1, 1));
		assertEquals(4, fileGroupMgr.getGroupSize(groupId));
		assertEquals(file1, fileGroupMgr.getSourcePathAt(groupId, 0));
		assertEquals(file2, fileGroupMgr.getSourcePathAt(groupId, 1));
		assertEquals(file3, fileGroupMgr.getSourcePathAt(groupId, 2));
		assertEquals(file4, fileGroupMgr.getSourcePathAt(groupId, 3));
		
		/* move index 0 to index 3 */
		assertEquals(ErrorCode.OK, fileGroupMgr.moveEntry(groupId, 0, 3));
		assertEquals(4, fileGroupMgr.getGroupSize(groupId));
		assertEquals(file2, fileGroupMgr.getSourcePathAt(groupId, 0));
		assertEquals(file3, fileGroupMgr.getSourcePathAt(groupId, 1));
		assertEquals(file4, fileGroupMgr.getSourcePathAt(groupId, 2));
		assertEquals(file1, fileGroupMgr.getSourcePathAt(groupId, 3));
		
		/* move index 3 to index 1 */
		assertEquals(ErrorCode.OK, fileGroupMgr.moveEntry(groupId, 3, 1));
		assertEquals(4, fileGroupMgr.getGroupSize(groupId));
		assertEquals(file2, fileGroupMgr.getSourcePathAt(groupId, 0));
		assertEquals(file1, fileGroupMgr.getSourcePathAt(groupId, 1));
		assertEquals(file3, fileGroupMgr.getSourcePathAt(groupId, 2));
		assertEquals(file4, fileGroupMgr.getSourcePathAt(groupId, 3));
		
		/* move index 2 to index 1 */
		assertEquals(ErrorCode.OK, fileGroupMgr.moveEntry(groupId, 2, 1));
		assertEquals(4, fileGroupMgr.getGroupSize(groupId));
		assertEquals(file2, fileGroupMgr.getSourcePathAt(groupId, 0));
		assertEquals(file3, fileGroupMgr.getSourcePathAt(groupId, 1));
		assertEquals(file1, fileGroupMgr.getSourcePathAt(groupId, 2));
		assertEquals(file4, fileGroupMgr.getSourcePathAt(groupId, 3));
	}
	
	/*-------------------------------------------------------------------------------------*/
}
