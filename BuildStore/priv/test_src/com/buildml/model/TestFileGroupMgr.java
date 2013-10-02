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

import com.buildml.model.IPackageMemberMgr.MemberDesc;
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
	IPackageMemberMgr pkgMemberMgr;
	
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
		pkgMemberMgr = buildStore.getPackageMemberMgr();
		
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
		assertEquals(pkg1Id, pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE_GROUP, group1Id).pkgId);
		assertEquals(pkg2Id, pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE_GROUP, group2Id).pkgId);
		assertEquals(pkg3Id, pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE_GROUP, group3Id).pkgId);
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
		assertNull(pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE_GROUP, 1000));
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
		assertEquals(pkg1Id, pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE_GROUP, group1Id).pkgId);
		assertEquals(pkg1Id, pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE_GROUP, group2Id).pkgId);
		
		/* change one of the groups to another package */
		assertEquals(ErrorCode.OK, 
				pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE_GROUP, group2Id, pkg2Id));
		assertEquals(pkg1Id, pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE_GROUP, group1Id).pkgId);
		assertEquals(pkg2Id, pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE_GROUP, group2Id).pkgId);
		
		/* try changing with an invalid group ID */
		assertEquals(ErrorCode.NOT_FOUND, 
				pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE_GROUP, 100, pkg2Id));
		
		/* try changing with an invalid package ID */
		assertEquals(ErrorCode.BAD_VALUE, 
				pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE_GROUP, group2Id, 100));
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
		assertEquals(pkg1Id, pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE_GROUP, groupId).pkgId);
		assertEquals(0, fileGroupMgr.getGroupSize(groupId));
		
		/* delete the group and test again */
		assertEquals(ErrorCode.OK, fileGroupMgr.removeGroup(groupId));
		assertEquals(ErrorCode.NOT_FOUND, fileGroupMgr.getGroupType(groupId));
		assertNull(pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE_GROUP, groupId));
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
		assertEquals(0, fileGroupMgr.addPathId(groupId, file1));
		assertEquals(1, fileGroupMgr.addPathId(groupId, file2));
		assertEquals(2, fileGroupMgr.addPathId(groupId, file3));
		assertEquals(3, fileGroupMgr.getGroupSize(groupId));
		assertEquals(file3, fileGroupMgr.getPathId(groupId, 2));
		assertEquals(file2, fileGroupMgr.getPathId(groupId, 1));
		assertEquals(file1, fileGroupMgr.getPathId(groupId, 0));
		
		/* insert some files at specific indices within the group - test size and positions */
		assertEquals(2, fileGroupMgr.addPathId(groupId, file4, 2));
		assertEquals(0, fileGroupMgr.addPathId(groupId, file5, 0));
		assertEquals(2, fileGroupMgr.addPathId(groupId, file6, 2));
		assertEquals(6, fileGroupMgr.getGroupSize(groupId));
		
		/* confirm ordering is now: file5, file1, file6, file2, file4, file3 */
		assertEquals(file5, fileGroupMgr.getPathId(groupId, 0));
		assertEquals(file1, fileGroupMgr.getPathId(groupId, 1));
		assertEquals(file6, fileGroupMgr.getPathId(groupId, 2));
		assertEquals(file2, fileGroupMgr.getPathId(groupId, 3));
		assertEquals(file4, fileGroupMgr.getPathId(groupId, 4));
		assertEquals(file3, fileGroupMgr.getPathId(groupId, 5));
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
		assertEquals(ErrorCode.OUT_OF_RANGE, fileGroupMgr.addPathId(groupId, file1, -2));
		assertEquals(ErrorCode.OUT_OF_RANGE, fileGroupMgr.addPathId(groupId, file1, 1));
		
		/* these are valid */
		assertEquals(0, fileGroupMgr.addPathId(groupId, file1, 0));
		assertEquals(1, fileGroupMgr.addPathId(groupId, file2, -1));
		
		/* test addition source files into non-source group */
		int generatedGroupId = fileGroupMgr.newGeneratedGroup(pkg1Id);
		int mergeGroupId = fileGroupMgr.newMergeGroup(pkg1Id);
		assertEquals(ErrorCode.INVALID_OP, fileGroupMgr.addPathId(generatedGroupId, file1));
		assertEquals(ErrorCode.INVALID_OP, fileGroupMgr.addPathId(mergeGroupId, file1));
		
		/* test addition of invalid path into source group */
		assertEquals(ErrorCode.BAD_VALUE, fileGroupMgr.addPathId(groupId, 100));
		
		/* test getting of paths with invalid group */
		assertEquals(ErrorCode.NOT_FOUND, fileGroupMgr.getPathId(-1, 0));
		assertEquals(ErrorCode.NOT_FOUND, fileGroupMgr.getPathId(123, 0));
		
		/* test getting of paths with invalid index */
		assertEquals(ErrorCode.OUT_OF_RANGE, fileGroupMgr.getPathId(groupId, -1));
		assertEquals(ErrorCode.OUT_OF_RANGE, fileGroupMgr.getPathId(groupId, 3));
		
		/* test getting from non-source file groups - error */
		assertEquals(ErrorCode.INVALID_OP, fileGroupMgr.getPathId(generatedGroupId, 0));
		assertEquals(ErrorCode.INVALID_OP, fileGroupMgr.getPathId(mergeGroupId, 0));
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
		assertEquals(0, fileGroupMgr.addPathId(groupId, file1));
		assertEquals(1, fileGroupMgr.addPathId(groupId, file6));
		assertEquals(2, fileGroupMgr.addPathId(groupId, file3));
		assertEquals(3, fileGroupMgr.addPathId(groupId, file5));
		assertEquals(4, fileGroupMgr.addPathId(groupId, file4));
		assertEquals(5, fileGroupMgr.addPathId(groupId, file2));
		assertEquals(6, fileGroupMgr.addPathId(groupId, file3));

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
		assertEquals(0, fileGroupMgr.addPathId(groupId, file1));
		assertEquals(1, fileGroupMgr.addPathId(groupId, file2));
		assertEquals(2, fileGroupMgr.addPathId(groupId, file3));
		assertEquals(3, fileGroupMgr.addPathId(groupId, file4));
		assertEquals(4, fileGroupMgr.getGroupSize(groupId));
		assertEquals(ErrorCode.CANT_REMOVE, fileGroupMgr.removeGroup(groupId));
		
		/* attempt remove an entry from an invalid index */
		assertEquals(ErrorCode.OUT_OF_RANGE, fileGroupMgr.removeEntry(groupId, 4));
		assertEquals(ErrorCode.OUT_OF_RANGE, fileGroupMgr.removeEntry(groupId, -1));
		
		/* remove file3, group now has file1, file2, file4 */
		assertEquals(ErrorCode.OK, fileGroupMgr.removeEntry(groupId, 2));
		assertEquals(3, fileGroupMgr.getGroupSize(groupId));
		assertEquals(file1, fileGroupMgr.getPathId(groupId, 0));
		assertEquals(file2, fileGroupMgr.getPathId(groupId, 1));
		assertEquals(file4, fileGroupMgr.getPathId(groupId, 2));
		assertEquals(ErrorCode.CANT_REMOVE, fileGroupMgr.removeGroup(groupId));
				
		/* remove file4, group now has file1, file2 */
		assertEquals(ErrorCode.OK, fileGroupMgr.removeEntry(groupId, 2));
		assertEquals(2, fileGroupMgr.getGroupSize(groupId));
		assertEquals(file1, fileGroupMgr.getPathId(groupId, 0));
		assertEquals(file2, fileGroupMgr.getPathId(groupId, 1));
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
		assertEquals(0, fileGroupMgr.addPathId(groupId, file1));
		assertEquals(1, fileGroupMgr.addPathId(groupId, file2));
		assertEquals(2, fileGroupMgr.addPathId(groupId, file3));
		assertEquals(3, fileGroupMgr.addPathId(groupId, file4));
		assertEquals(4, fileGroupMgr.getGroupSize(groupId));

		/* attempt to move paths within an invalid group */
		assertEquals(ErrorCode.NOT_FOUND, fileGroupMgr.moveEntry(1000, 0, 0));

		/* attempt to move paths from/to invalid indices */
		assertEquals(ErrorCode.OUT_OF_RANGE, fileGroupMgr.moveEntry(groupId, 0, 5));
		assertEquals(ErrorCode.OUT_OF_RANGE, fileGroupMgr.moveEntry(groupId, 42, 2));
		
		/* move index 1 to index 1 - no effect */
		assertEquals(ErrorCode.OK, fileGroupMgr.moveEntry(groupId, 1, 1));
		assertEquals(4, fileGroupMgr.getGroupSize(groupId));
		assertEquals(file1, fileGroupMgr.getPathId(groupId, 0));
		assertEquals(file2, fileGroupMgr.getPathId(groupId, 1));
		assertEquals(file3, fileGroupMgr.getPathId(groupId, 2));
		assertEquals(file4, fileGroupMgr.getPathId(groupId, 3));
		
		/* move index 0 to index 3 */
		assertEquals(ErrorCode.OK, fileGroupMgr.moveEntry(groupId, 0, 3));
		assertEquals(4, fileGroupMgr.getGroupSize(groupId));
		assertEquals(file2, fileGroupMgr.getPathId(groupId, 0));
		assertEquals(file3, fileGroupMgr.getPathId(groupId, 1));
		assertEquals(file4, fileGroupMgr.getPathId(groupId, 2));
		assertEquals(file1, fileGroupMgr.getPathId(groupId, 3));
		
		/* move index 3 to index 1 */
		assertEquals(ErrorCode.OK, fileGroupMgr.moveEntry(groupId, 3, 1));
		assertEquals(4, fileGroupMgr.getGroupSize(groupId));
		assertEquals(file2, fileGroupMgr.getPathId(groupId, 0));
		assertEquals(file1, fileGroupMgr.getPathId(groupId, 1));
		assertEquals(file3, fileGroupMgr.getPathId(groupId, 2));
		assertEquals(file4, fileGroupMgr.getPathId(groupId, 3));
		
		/* move index 2 to index 1 */
		assertEquals(ErrorCode.OK, fileGroupMgr.moveEntry(groupId, 2, 1));
		assertEquals(4, fileGroupMgr.getGroupSize(groupId));
		assertEquals(file2, fileGroupMgr.getPathId(groupId, 0));
		assertEquals(file3, fileGroupMgr.getPathId(groupId, 1));
		assertEquals(file1, fileGroupMgr.getPathId(groupId, 2));
		assertEquals(file4, fileGroupMgr.getPathId(groupId, 3));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add paths to a generated file group.
	 */
	@Test
	public void testAddGeneratedPaths() {
		
		/* create a new source file group */
		int groupId = fileGroupMgr.newGeneratedGroup(pkg1Id);
		assertTrue(groupId >= 0);
		
		/* append some permanent paths to the end of the group - test size and positions */
		assertEquals(0, fileGroupMgr.addPathString(groupId, "@pkg1_src/src/path1"));
		assertEquals(1, fileGroupMgr.addPathString(groupId, "@pkg1_gen/obj/path1"));
		assertEquals(2, fileGroupMgr.addPathString(groupId, "@pkg2_src/src/path2"));
		assertEquals(3, fileGroupMgr.addPathString(groupId, "@pkg2_gen/obj/path2"));
		assertEquals(2, fileGroupMgr.addPathString(groupId, "@pkg3_src/src/path3", 2));
		assertEquals(ErrorCode.OK, fileGroupMgr.moveEntry(groupId, 2, 0));
		assertEquals(ErrorCode.OK, fileGroupMgr.removeEntry(groupId, 1));
		assertEquals(4, fileGroupMgr.getGroupSize(groupId));
		
		/* 
		 * Check ordering - @pkg3_src/src/path3, @pkg1_gen/obj/path1, @pkg2_src/src/path2, 
		 * @pkg2_gen/obj/path2.
		 */
		assertEquals("@pkg3_src/src/path3", fileGroupMgr.getPathString(groupId, 0));
		assertEquals("@pkg1_gen/obj/path1", fileGroupMgr.getPathString(groupId, 1));
		assertEquals("@pkg2_src/src/path2", fileGroupMgr.getPathString(groupId, 2));
		assertEquals("@pkg2_gen/obj/path2", fileGroupMgr.getPathString(groupId, 3));
		
		/* expand the group into paths */
		String results[] = fileGroupMgr.getExpandedGroupFiles(groupId);
		assertEquals("@pkg3_src/src/path3", results[0]);
		assertEquals("@pkg1_gen/obj/path1", results[1]);
		assertEquals("@pkg2_src/src/path2", results[2]);
		assertEquals("@pkg2_gen/obj/path2", results[3]);
		
		/* append some transient paths to end of the group */
		assertEquals(ErrorCode.OK, fileGroupMgr.addTransientPathString(groupId, "@pkg1_src/my/path"));
		assertEquals(ErrorCode.OK, fileGroupMgr.addTransientPathString(groupId, "@pkg2_src/my/2ndpath"));
		assertEquals(ErrorCode.OK, fileGroupMgr.addTransientPathString(groupId, "@pkg2_gen/my/3rdpath"));
				
		/* confirm ordering */
		results = fileGroupMgr.getExpandedGroupFiles(groupId);
		assertEquals("@pkg3_src/src/path3",  results[0]);
		assertEquals("@pkg1_gen/obj/path1",  results[1]);
		assertEquals("@pkg2_src/src/path2",  results[2]);
		assertEquals("@pkg2_gen/obj/path2",  results[3]);
		assertEquals("@pkg1_src/my/path",    results[4]);
		assertEquals("@pkg2_src/my/2ndpath", results[5]);
		assertEquals("@pkg2_gen/my/3rdpath", results[6]);
				
		/* clear the transient members and test again */
		assertEquals(ErrorCode.OK, fileGroupMgr.clearTransientPathStrings(groupId));
		results = fileGroupMgr.getExpandedGroupFiles(groupId);
		assertEquals("@pkg3_src/src/path3", results[0]);
		assertEquals("@pkg1_gen/obj/path1", results[1]);
		assertEquals("@pkg2_src/src/path2", results[2]);
		assertEquals("@pkg2_gen/obj/path2", results[3]);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add paths to a generated file group, but with errors.
	 */
	@Test
	public void testAddGeneratedPathErrors() {

		int sourceGroupId = fileGroupMgr.newSourceGroup(pkg1Id);
		int genGroupId = fileGroupMgr.newGeneratedGroup(pkg1Id);
		int mergeGroupId = fileGroupMgr.newMergeGroup(pkg1Id);
		
		/* try to add/get path strings in source or merge file groups - fail */
		assertEquals(ErrorCode.INVALID_OP, fileGroupMgr.addPathString(sourceGroupId, "@root/path"));
		assertEquals(null, fileGroupMgr.getPathString(sourceGroupId, 0));
		assertEquals(ErrorCode.INVALID_OP, fileGroupMgr.addTransientPathString(sourceGroupId, "@root/path"));
		assertEquals(ErrorCode.INVALID_OP, fileGroupMgr.addPathString(mergeGroupId, "@root/path"));
		assertEquals(null, fileGroupMgr.getPathString(mergeGroupId, 0));
		assertEquals(ErrorCode.INVALID_OP, fileGroupMgr.addTransientPathString(mergeGroupId, "@root/path"));
		
		/* try to add permanent and transient with invalid group ID - fail */
		assertEquals(ErrorCode.NOT_FOUND, fileGroupMgr.addPathString(1234, "@root/path"));
		assertEquals(ErrorCode.NOT_FOUND, fileGroupMgr.addTransientPathString(1234, "@root/path"));
		
		/* add invalid path strings - missing a valid root name - fail */
		assertEquals(ErrorCode.BAD_VALUE, fileGroupMgr.addPathString(genGroupId, "@bad_root/path"));
		assertEquals(ErrorCode.BAD_VALUE, fileGroupMgr.addTransientPathString(genGroupId, "@pkg1_gon/"));		
		
		/* add some valid path strings to the generated group */
		assertEquals(0, fileGroupMgr.addPathString(genGroupId, "@root/path1"));
		assertEquals(1, fileGroupMgr.addPathString(genGroupId, "@root/path2"));
		assertEquals("@root/path1", fileGroupMgr.getPathString(genGroupId, 0));
		assertEquals("@root/path2", fileGroupMgr.getPathString(genGroupId, 1));
		
		/* add path strings at invalid locations */
		assertEquals(ErrorCode.OUT_OF_RANGE, fileGroupMgr.addPathString(genGroupId, "@root/path1", -2));
		assertEquals(ErrorCode.OUT_OF_RANGE, fileGroupMgr.addPathString(genGroupId, "@root/path1", 3));
		
		/* get path string for invalid group ID, or invalid index */
		assertEquals(null, fileGroupMgr.getPathString(genGroupId, -1));
		assertEquals(null, fileGroupMgr.getPathString(genGroupId, 2));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create a merge file group containing two source groups and a generated group.
	 */
	@Test
	public void testNewMergeGroup() {
		
		/* create a top-level merge group, to be populated by other groups */
		int mergeGroup = fileGroupMgr.newMergeGroup(pkg1Id);
		
		/* create and populate sub groups */
		int sourceGroup1 = fileGroupMgr.newSourceGroup(pkg2Id);
		int sourceGroup2 = fileGroupMgr.newSourceGroup(pkg2Id);
		int generatedGroup = fileGroupMgr.newGeneratedGroup(pkg3Id);

		assertEquals(0, fileGroupMgr.addPathId(sourceGroup1, file1));
		assertEquals(1, fileGroupMgr.addPathId(sourceGroup1, file2));
		assertEquals(2, fileGroupMgr.addPathId(sourceGroup1, file3));
		assertEquals(0, fileGroupMgr.addPathId(sourceGroup2, file4));
		assertEquals(1, fileGroupMgr.addPathId(sourceGroup2, file5));
		assertEquals(2, fileGroupMgr.addPathId(sourceGroup2, file6));
		assertEquals(0, fileGroupMgr.addPathString(generatedGroup, "@root/path1"));
		assertEquals(1, fileGroupMgr.addPathString(generatedGroup, "@root/path2"));
		assertEquals(ErrorCode.OK, fileGroupMgr.addTransientPathString(generatedGroup, "@root/path3"));
		assertEquals(ErrorCode.OK, fileGroupMgr.addTransientPathString(generatedGroup, "@root/path4"));
		
		/* add the sub-groups into the merge group */
		assertEquals(0, fileGroupMgr.addSubGroup(mergeGroup, sourceGroup1));
		assertEquals(1, fileGroupMgr.addSubGroup(mergeGroup, generatedGroup));
		assertEquals(2, fileGroupMgr.addSubGroup(mergeGroup, sourceGroup2));
		assertEquals(ErrorCode.OK, fileGroupMgr.moveEntry(mergeGroup, 2, 1));
		
		/* validate the sub-groups of the merge group */
		assertEquals(sourceGroup1, fileGroupMgr.getSubGroup(mergeGroup, 0));
		assertEquals(sourceGroup2, fileGroupMgr.getSubGroup(mergeGroup, 1));
		assertEquals(generatedGroup, fileGroupMgr.getSubGroup(mergeGroup, 2));
		
		/* validate the expanded content */
		String results[] = fileGroupMgr.getExpandedGroupFiles(mergeGroup);
		assertEquals("@root/a/b/file1",        results[0]);
		assertEquals("@root/a/b/c/file2",      results[1]);
		assertEquals("@root/a/b/d/file3",      results[2]);
		assertEquals("@root/a/b/c/d/e/file4",  results[3]);
		assertEquals("@root/a/b/c/file5",      results[4]);
		assertEquals("@root/a/b/z/file6",      results[5]);
		assertEquals("@root/path1",            results[6]);
		assertEquals("@root/path2",            results[7]);
		assertEquals("@root/path3",            results[8]);
		assertEquals("@root/path4",            results[9]);
		
		/* remove one of the entries, and re-check the content */
		assertEquals(ErrorCode.OK, fileGroupMgr.removeEntry(mergeGroup, 1));
		results = fileGroupMgr.getExpandedGroupFiles(mergeGroup);
		assertEquals("@root/a/b/file1",        results[0]);
		assertEquals("@root/a/b/c/file2",      results[1]);
		assertEquals("@root/a/b/d/file3",      results[2]);
		assertEquals("@root/path1",            results[3]);
		assertEquals("@root/path2",            results[4]);
		assertEquals("@root/path3",            results[5]);
		assertEquals("@root/path4",            results[6]);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create a merge file group containing an embedded merge group.
	 */
	@Test
	public void testNewNestedMergeGroup() {
		
		/* create a top-level merge group, to be populated by other groups */
		int mergeGroup = fileGroupMgr.newMergeGroup(pkg1Id);
		
		/* create and populate sub groups */
		int sourceGroup = fileGroupMgr.newSourceGroup(pkg2Id);
		int subMergeGroup = fileGroupMgr.newMergeGroup(pkg2Id);
		int generatedGroup = fileGroupMgr.newGeneratedGroup(pkg3Id);
		
		assertEquals(0, fileGroupMgr.addPathId(sourceGroup, file1));
		assertEquals(0, fileGroupMgr.addPathString(generatedGroup, "@root/path1"));
		
		/* add the sourceGroup and the subMergeGroup into the top-level group */
		assertEquals(0, fileGroupMgr.addSubGroup(mergeGroup, sourceGroup));
		assertEquals(1, fileGroupMgr.addSubGroup(mergeGroup, subMergeGroup));
		
		/* add the sourceGroup (again) and the generatedGroup into the subMergeGroup */
		assertEquals(0, fileGroupMgr.addSubGroup(subMergeGroup, sourceGroup));
		assertEquals(1, fileGroupMgr.addSubGroup(subMergeGroup, generatedGroup));
		
		/* validate the content */
		String results[] = fileGroupMgr.getExpandedGroupFiles(mergeGroup);
		assertEquals(3, results.length);
		assertEquals("@root/a/b/file1", results[0]);
		assertEquals("@root/a/b/file1", results[1]);
		assertEquals("@root/path1", results[2]);
		
		/* try to add the main group into the subMergeGroup - should fail */
		assertEquals(ErrorCode.BAD_PATH, fileGroupMgr.addSubGroup(subMergeGroup, mergeGroup));	

		/* try to add the main group into itself - should fail */
		assertEquals(ErrorCode.BAD_PATH, fileGroupMgr.addSubGroup(mergeGroup, mergeGroup));	
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Create a merge file group containing two source groups and a generated group.
	 */
	@Test
	public void testNewMergeGroupErrors() {

		int sourceGroup = fileGroupMgr.newSourceGroup(pkg2Id);
		int mergeGroup = fileGroupMgr.newMergeGroup(pkg2Id);
		int generatedGroup = fileGroupMgr.newGeneratedGroup(pkg3Id);
		
		/* try to add sub-groups to non-merge groups */
		assertEquals(ErrorCode.INVALID_OP, fileGroupMgr.addSubGroup(sourceGroup, generatedGroup));
		assertEquals(ErrorCode.INVALID_OP, fileGroupMgr.addSubGroup(generatedGroup, sourceGroup));

		/* try to add to an invalid group ID, and to add an invalid subgroup */
		assertEquals(ErrorCode.NOT_FOUND, fileGroupMgr.addSubGroup(1000, generatedGroup));
		assertEquals(ErrorCode.BAD_VALUE, fileGroupMgr.addSubGroup(mergeGroup, 2000));
	}
		
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add groups into packages, and query for those groups.
	 */
	@Test
	public void testGetGroupsInPackage() {
		
		/* test empty group */
		MemberDesc[] results = pkgMemberMgr.getMembersInPackage(
				pkg1Id, IPackageMemberMgr.SCOPE_NONE, IPackageMemberMgr.TYPE_FILE_GROUP);
		assertEquals(0, results.length);
		
		/* add a group, and test again */
		int group1 = fileGroupMgr.newSourceGroup(pkg1Id);
		results = pkgMemberMgr.getMembersInPackage(
				pkg1Id, IPackageMemberMgr.SCOPE_NONE, IPackageMemberMgr.TYPE_FILE_GROUP);
		assertEquals(1, results.length);
		assertEquals(group1, results[0].memberId);

		/* add a second group, and test again - ordering of result is undefined. */
		int group2 = fileGroupMgr.newSourceGroup(pkg1Id);
		results = pkgMemberMgr.getMembersInPackage(
				pkg1Id, IPackageMemberMgr.SCOPE_NONE, IPackageMemberMgr.TYPE_FILE_GROUP);
		assertEquals(2, results.length);
		assertTrue(((results[0].memberId == group1) && (results[1].memberId == group2)) ||
				((results[0].memberId == group2) && (results[1].memberId == group1)));

		/* add a third group, in a different package. */
		int group3 = fileGroupMgr.newSourceGroup(pkg2Id);
		results = pkgMemberMgr.getMembersInPackage(
				pkg1Id, IPackageMemberMgr.SCOPE_NONE, IPackageMemberMgr.TYPE_FILE_GROUP);
		assertEquals(2, results.length);
		assertTrue(((results[0].memberId == group1) && (results[1].memberId == group2)) ||
				((results[0].memberId == group2) && (results[1].memberId == group1)));

		/* invalid package ID */
		assertEquals(0, pkgMemberMgr.getMembersInPackage(
				1000, IPackageMemberMgr.SCOPE_NONE, IPackageMemberMgr.TYPE_FILE_GROUP).length);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	private static int notifyId;
	private static int notifyHow;
	
	/**
	 * Test the various ways in which IFileGroupMgr can send notifications.
	 */
	@Test
	public void testNotifications() {
		
		/* set up a listener for changes to the IFileGroupMgr */
		IFileGroupMgrListener listener = new IFileGroupMgrListener() {
			@Override
			public void fileGroupChangeNotification(int fileGroupId, int how) {
				TestFileGroupMgr.notifyId = fileGroupId;
				TestFileGroupMgr.notifyHow = how;
			}
		};
		fileGroupMgr.addListener(listener);
		
		int pkgId = pkgMgr.addPackage("MyPkg");
		int srcGroup = fileGroupMgr.newSourceGroup(pkgId);
		int genGroup = fileGroupMgr.newGeneratedGroup(pkgId);
		int mergeGroup = fileGroupMgr.newMergeGroup(pkgId);
		
		int fileId1 = fileMgr.addFile("/file123");
		
		/* test addPathId */
		assertEquals(ErrorCode.OK, fileGroupMgr.addPathId(srcGroup, fileId1));
		assertEquals(srcGroup, notifyId);
		assertEquals(IFileGroupMgrListener.CHANGED_MEMBERSHIP, notifyHow);
		
		/* test addPathString */
		notifyId = notifyHow = 0;
		assertEquals(ErrorCode.OK, fileGroupMgr.addPathString(genGroup, "@root/file123"));
		assertEquals(genGroup, notifyId);
		assertEquals(IFileGroupMgrListener.CHANGED_MEMBERSHIP, notifyHow);
		
		/* test addTransientPathString */
		notifyId = notifyHow = 0;
		assertEquals(ErrorCode.OK, fileGroupMgr.addTransientPathString(genGroup, "@root/file567"));
		assertEquals(genGroup, notifyId);
		assertEquals(IFileGroupMgrListener.CHANGED_MEMBERSHIP, notifyHow);
		
		/* test clearTransientPathStrings */
		notifyId = notifyHow = 0;
		assertEquals(ErrorCode.OK, fileGroupMgr.clearTransientPathStrings(genGroup));
		assertEquals(genGroup, notifyId);
		assertEquals(IFileGroupMgrListener.CHANGED_MEMBERSHIP, notifyHow);
		
		/* test addSubGroup */
		notifyId = notifyHow = 0;
		assertEquals(ErrorCode.OK, fileGroupMgr.addSubGroup(mergeGroup, srcGroup));
		assertEquals(mergeGroup, notifyId);
		assertEquals(IFileGroupMgrListener.CHANGED_MEMBERSHIP, notifyHow);
		
		/* test moveEntry */
		assertTrue(fileGroupMgr.addPathString(genGroup, "@root/file123") >= 0);
		assertTrue(fileGroupMgr.addPathString(genGroup, "@root/file123") >= 0);
		assertTrue(fileGroupMgr.addPathString(genGroup, "@root/file123") >= 0);
		notifyId = notifyHow = 0;
		assertEquals(ErrorCode.OK, fileGroupMgr.moveEntry(genGroup, 1, 2));
		assertEquals(genGroup, notifyId);
		assertEquals(IFileGroupMgrListener.CHANGED_MEMBERSHIP, notifyHow);
		
		/* test removeEntry */
		notifyId = notifyHow = 0;
		assertEquals(ErrorCode.OK, fileGroupMgr.removeEntry(genGroup, 1));
		assertEquals(genGroup, notifyId);
		assertEquals(IFileGroupMgrListener.CHANGED_MEMBERSHIP, notifyHow);
				
		fileGroupMgr.removeListener(listener);
	}

	/*-------------------------------------------------------------------------------------*/

}
