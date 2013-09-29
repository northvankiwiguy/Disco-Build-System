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
import com.buildml.model.IPackageMemberMgr.MemberLocation;
import com.buildml.model.IPackageMemberMgr.PackageDesc;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.ActionSet;
import com.buildml.utils.errors.ErrorCode;

/**
 * Unit tests for the Packages class.
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestPackageMemberMgr {

	/** Our BuildStore object, used in many test cases */
	private IBuildStore bs;

	/** The manager object associated with this BuildStore */
	private IPackageMgr pkgMgr;
	private IPackageMemberMgr pkgMemberMgr;
	private IPackageRootMgr pkgRootMgr;
	private IFileMgr fileMgr;
	private IFileGroupMgr fileGroupMgr;
	private IActionMgr actionMgr;
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		bs = CommonTestUtils.getEmptyBuildStore();
		pkgMgr = bs.getPackageMgr();
		pkgMemberMgr = bs.getPackageMemberMgr();
		pkgRootMgr = bs.getPackageRootMgr();
		fileMgr = bs.getFileMgr();
		fileGroupMgr = bs.getFileGroupMgr();
		actionMgr = bs.getActionMgr();
		
		/* set the workspace root to /, so packages can be added anywhere */
		pkgRootMgr.setWorkspaceRoot(fileMgr.getPath("/"));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IPackageMemberMgr#getScopeName(int)}.
	 */
	@Test
	public void testGetScopeName() {
		
		/* Test valid section IDs */
		assertEquals("None", pkgMemberMgr.getScopeName(0));
		assertEquals("Private", pkgMemberMgr.getScopeName(1));
		assertEquals("Public", pkgMemberMgr.getScopeName(2));

		/* Test invalid section IDs */
		assertNull(pkgMemberMgr.getScopeName(3));
		assertNull(pkgMemberMgr.getScopeName(4));
		assertNull(pkgMemberMgr.getScopeName(100));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IPackageMemberMgr#getScopeId(String)}.
	 */
	@Test
	public void testGetScopeId() {
		
		/* Test valid section names */
		assertEquals(0, pkgMemberMgr.getScopeId("None"));
		assertEquals(1, pkgMemberMgr.getScopeId("priv"));
		assertEquals(1, pkgMemberMgr.getScopeId("private"));
		assertEquals(1, pkgMemberMgr.getScopeId("Private"));
		assertEquals(2, pkgMemberMgr.getScopeId("pub"));
		assertEquals(2, pkgMemberMgr.getScopeId("public"));
		
		/* Test invalid section names */
		assertEquals(ErrorCode.NOT_FOUND, pkgMemberMgr.getScopeId("object"));
		assertEquals(ErrorCode.NOT_FOUND, pkgMemberMgr.getScopeId("obj"));
		assertEquals(ErrorCode.NOT_FOUND, pkgMemberMgr.getScopeId("pretty"));
		assertEquals(ErrorCode.NOT_FOUND, pkgMemberMgr.getScopeId("shiny"));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IPackageMemberMgr#parsePkgSpec(String)}.
	 * @exception Exception Something bad happened
	 */
	@Test
	public void testParsePkgSpec() throws Exception {
		
		/* add a couple of packages */
		int pkg1 = pkgMgr.addPackage("pkg1");
		int pkg2 = pkgMgr.addPackage("pkg2");
		
		/* test the pkgSpecs with only package names */
		PackageDesc r = pkgMemberMgr.parsePkgSpec("pkg1");
		assertEquals(pkg1, r.pkgId);
		assertEquals(0, r.pkgScopeId);
		
		r = pkgMemberMgr.parsePkgSpec("pkg2");
		assertEquals(pkg2, r.pkgId);
		assertEquals(0, r.pkgScopeId);
		
		/* test pkgSpecs with both package and scope names */
		r = pkgMemberMgr.parsePkgSpec("pkg1/private");
		assertEquals(pkg1, r.pkgId);
		assertEquals(IPackageMemberMgr.SCOPE_PRIVATE, r.pkgScopeId);
		
		r = pkgMemberMgr.parsePkgSpec("pkg2/public");
		assertEquals(pkg2, r.pkgId);
		assertEquals(IPackageMemberMgr.SCOPE_PUBLIC, r.pkgScopeId);
		
		/* test invalid pkgSpecs */
		r = pkgMemberMgr.parsePkgSpec("badname");
		assertEquals(ErrorCode.NOT_FOUND, r.pkgId);
		assertEquals(0, r.pkgScopeId);
		
		r = pkgMemberMgr.parsePkgSpec("pkg1/missing");
		assertEquals(pkg1, r.pkgId);
		assertEquals(ErrorCode.NOT_FOUND, r.pkgScopeId);
		
		r = pkgMemberMgr.parsePkgSpec("badname/missing");
		assertEquals(ErrorCode.NOT_FOUND, r.pkgId);
		assertEquals(ErrorCode.NOT_FOUND, r.pkgScopeId);
		
		r = pkgMemberMgr.parsePkgSpec("badname/public");
		assertEquals(ErrorCode.NOT_FOUND, r.pkgId);
		assertEquals(IPackageMemberMgr.SCOPE_PUBLIC, r.pkgScopeId);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the setPackageOfMember() and getPackageOfMember() methods.
	 * @throws Exception
	 */
	@Test
	public void testSetPackageOfMember() throws Exception {
		
		/* define some packages that members can belong to */
		int pkgAId = pkgMgr.addPackage("PkgA");
		assertTrue(pkgAId > 0);
		int pkgBId = pkgMgr.addPackage("PkgB");
		assertTrue(pkgBId > 0);
		
		/* add a new action, a new file, and a new file group */
		int rootActionId = actionMgr.getRootAction("root");
		int rootPathId = fileMgr.getPath("/");
		int actionId = actionMgr.addShellCommandAction(rootActionId, rootPathId, "test");
		assertTrue(actionId > 0);
		int fileId = fileMgr.addFile("/a/b/c.java");
		assertTrue(fileId > 0);
		int fileGroupId = fileGroupMgr.newSourceGroup(pkgAId);
		assertTrue(fileGroupId > 0);
		
		/* check that the action defaults to being in the <import> package, with scope "none" */
		PackageDesc pkgDesc = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_ACTION, actionId);
		assertEquals(pkgMgr.getImportPackage(), pkgDesc.pkgId);
		assertEquals(IPackageMemberMgr.SCOPE_NONE, pkgDesc.pkgScopeId);

		/* check that the file defaults to being in the <import> package, with scope "none" */
		pkgDesc = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE, fileId);
		assertEquals(pkgMgr.getImportPackage(), pkgDesc.pkgId);
		assertEquals(IPackageMemberMgr.SCOPE_NONE, pkgDesc.pkgScopeId);
		
		/* check that the file group is in pkgA (where we initially put it) */		
		pkgDesc = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE_GROUP, fileGroupId);
		assertEquals(pkgAId, pkgDesc.pkgId);
		assertEquals(IPackageMemberMgr.SCOPE_NONE, pkgDesc.pkgScopeId);
				
		/* set/get the pkg/scope of the action */
		assertEquals(ErrorCode.OK, pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_ACTION, 
						actionId, pkgAId, IPackageMemberMgr.SCOPE_NONE));
		pkgDesc = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_ACTION, actionId);
		assertEquals(pkgAId, pkgDesc.pkgId);
		assertEquals(IPackageMemberMgr.SCOPE_NONE, pkgDesc.pkgScopeId);
		
		/* set/get the pkg/scope of the file */
		assertEquals(ErrorCode.OK, pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE, 
						fileId, pkgBId, IPackageMemberMgr.SCOPE_PRIVATE));
		pkgDesc = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE, fileId);
		assertEquals(pkgBId, pkgDesc.pkgId);
		assertEquals(IPackageMemberMgr.SCOPE_PRIVATE, pkgDesc.pkgScopeId);
		
		/* set/get the pkg/scope of the file group */
		assertEquals(ErrorCode.OK, pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE_GROUP, 
						fileGroupId, pkgBId, IPackageMemberMgr.SCOPE_NONE));
		pkgDesc = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE_GROUP, actionId);
		assertEquals(pkgBId, pkgDesc.pkgId);
		assertEquals(IPackageMemberMgr.SCOPE_NONE, pkgDesc.pkgScopeId);

		/* get/set package of invalid memberType (1000) */
		assertEquals(ErrorCode.NOT_FOUND, pkgMemberMgr.setPackageOfMember(1000, 
						fileId, pkgBId, IPackageMemberMgr.SCOPE_PRIVATE));
		pkgDesc = pkgMemberMgr.getPackageOfMember(1000, fileId);
		assertNull(pkgDesc);
		
		/* 
		 * get/set package of invalid memberId for memberType = file
		 * (the same code is tested for other members)
		 */
		assertEquals(ErrorCode.NOT_FOUND, 
						pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE, 
								1000, pkgBId, IPackageMemberMgr.SCOPE_PRIVATE));
		pkgDesc = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE, 1000);
		assertNull(pkgDesc);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the getMembersInPackage() method
	 * @throws Exception
	 */
	@Test
	public void testGetMembersInPackage() throws Exception {
		
		/* define some packages that members can belong to */
		int pkgAId = pkgMgr.addPackage("PkgA");
		assertTrue(pkgAId > 0);
		int pkgBId = pkgMgr.addPackage("PkgB");
		assertTrue(pkgBId > 0);
		
		int rootActionId = actionMgr.getRootAction("root");
		int rootPathId = fileMgr.getPath("/");
		int actionId = actionMgr.addShellCommandAction(rootActionId, rootPathId, "test");
		assertTrue(actionId > 0);
		int fileId = fileMgr.addFile("/a/b/c.java");
		assertTrue(fileId > 0);
		
		/* initially there are no members, of any kind, in any scope, in either package */
		MemberDesc[] members = pkgMemberMgr.getMembersInPackage(pkgAId, 
				IPackageMemberMgr.SCOPE_NONE, IPackageMemberMgr.TYPE_ANY);
		assertEquals(0, members.length);
		members = pkgMemberMgr.getMembersInPackage(pkgBId, 
				IPackageMemberMgr.SCOPE_NONE, IPackageMemberMgr.TYPE_ANY);
		assertEquals(0, members.length);
		
		/* add a file to package A, private scope */
		assertEquals(ErrorCode.OK, pkgMemberMgr.setPackageOfMember(
										IPackageMemberMgr.TYPE_FILE, fileId, 
										pkgAId, IPackageMemberMgr.SCOPE_PRIVATE));
		
		/* now there's one member in package A, and still none in package B */
		members = pkgMemberMgr.getMembersInPackage(pkgAId, 
								IPackageMemberMgr.SCOPE_NONE, IPackageMemberMgr.TYPE_ANY);
		assertEquals(1, members.length);
		assertEquals(fileId, members[0].memberId);
		assertEquals(IPackageMemberMgr.TYPE_FILE, members[0].memberType);
		
		members = pkgMemberMgr.getMembersInPackage(pkgBId, 
								IPackageMemberMgr.SCOPE_NONE, IPackageMemberMgr.TYPE_ANY);
		assertEquals(0, members.length);
		
		/* add an action into package A, and a file group in package B */
		assertEquals(ErrorCode.OK, pkgMemberMgr.setPackageOfMember(
				IPackageMemberMgr.TYPE_ACTION, actionId, 
				pkgAId, IPackageMemberMgr.SCOPE_PUBLIC));
		int fileGroupId = fileGroupMgr.newSourceGroup(pkgBId);
		assertTrue(fileGroupId > 0);
		
		/* check package A membership, in any scope */
		members = pkgMemberMgr.getMembersInPackage(pkgAId, 
				IPackageMemberMgr.SCOPE_NONE, IPackageMemberMgr.TYPE_ANY);
		assertEquals(2, members.length);
		
		/* check package A membership for public scope only */
		members = pkgMemberMgr.getMembersInPackage(pkgAId, 
				IPackageMemberMgr.SCOPE_PUBLIC, IPackageMemberMgr.TYPE_ANY);
		assertEquals(1, members.length);
		assertEquals(actionId, members[0].memberId);
		assertEquals(IPackageMemberMgr.TYPE_ACTION, members[0].memberType);

		/* check package A for only files (not actions) */
		members = pkgMemberMgr.getMembersInPackage(pkgAId, 
				IPackageMemberMgr.SCOPE_NONE, IPackageMemberMgr.TYPE_FILE);
		assertEquals(1, members.length);
		assertEquals(fileId, members[0].memberId);
		assertEquals(IPackageMemberMgr.TYPE_FILE, members[0].memberType);
		
		/* check package B members, in any scope */
		members = pkgMemberMgr.getMembersInPackage(pkgBId, 
				IPackageMemberMgr.SCOPE_NONE, IPackageMemberMgr.TYPE_ANY);
		assertEquals(1, members.length);
		assertEquals(fileGroupId, members[0].memberId);
		assertEquals(IPackageMemberMgr.TYPE_FILE_GROUP, members[0].memberType);

		/* check an undefined package */
		members = pkgMemberMgr.getMembersInPackage(2345, 
				IPackageMemberMgr.SCOPE_NONE, IPackageMemberMgr.TYPE_ANY);
		assertEquals(0, members.length);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the setMemberLocation() and getMemberLocation() methods
	 * @throws Exception
	 */
	@Test
	public void testSetMemberLocation() throws Exception {
		//fail();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the setFilePackage and getFilePackage methods.
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testFilePackages() throws Exception {
		
		IPackageMgr pkgMgr = bs.getPackageMgr();
		IFileMgr fileMgr = bs.getFileMgr();
		
		/* create a few files */
		int path1 = fileMgr.addFile("/banana");
		int path2 = fileMgr.addFile("/aardvark");
		int path3 = fileMgr.addFile("/carrot");
		
		/* create a couple of new packages */
		int pkgA = pkgMgr.addPackage("PkgA");
		int pkgB = pkgMgr.addPackage("PkgB");
		int pkgImport = pkgMgr.getImportPackage();

		/* by default, all files are in <import>/None */
		PackageDesc results = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE, path1);
		assertEquals(pkgImport, results.pkgId);
		assertEquals(IPackageMemberMgr.SCOPE_NONE, results.pkgScopeId);
		results = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE, path2);
		assertEquals(pkgImport, results.pkgId);
		assertEquals(IPackageMemberMgr.SCOPE_NONE, results.pkgScopeId);
		results = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE, path3);
		assertEquals(pkgImport, results.pkgId);
		assertEquals(IPackageMemberMgr.SCOPE_NONE, results.pkgScopeId);

		/* set one of the files into PkgA/public */
		assertEquals(ErrorCode.OK, pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE, path1, 
																	pkgA, IPackageMemberMgr.SCOPE_PUBLIC));
		results = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE, path1);
		assertEquals(pkgA, results.pkgId);
		assertEquals(IPackageMemberMgr.SCOPE_PUBLIC, results.pkgScopeId);
		results = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE, path2);
		assertEquals(pkgImport, results.pkgId);
		assertEquals(IPackageMemberMgr.SCOPE_NONE, results.pkgScopeId);
		results = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE, path3);
		assertEquals(pkgImport, results.pkgId);
		assertEquals(IPackageMemberMgr.SCOPE_NONE, results.pkgScopeId);
		
		/* set another file to another package */
		assertEquals(ErrorCode.OK, 
				pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE, path3, 
												pkgB, IPackageMemberMgr.SCOPE_PRIVATE));
		results = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE, path1);
		assertEquals(pkgA, results.pkgId);
		assertEquals(IPackageMemberMgr.SCOPE_PUBLIC, results.pkgScopeId);
		results = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE, path2);
		assertEquals(pkgImport, results.pkgId);
		assertEquals(IPackageMemberMgr.SCOPE_NONE, results.pkgScopeId);
		results = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE, path3);
		assertEquals(pkgB, results.pkgId);
		assertEquals(IPackageMemberMgr.SCOPE_PRIVATE, results.pkgScopeId);
		
		/* set a file's package back to <import>/None */
		assertEquals(ErrorCode.OK, 
						pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE, path1, 
														pkgImport, IPackageMemberMgr.SCOPE_NONE));
		results = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE, path1);
		assertEquals(pkgImport, results.pkgId);
		assertEquals(IPackageMemberMgr.SCOPE_NONE, results.pkgScopeId);
		
		/* try to set a non-existent file */
		assertEquals(ErrorCode.NOT_FOUND, 
						pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE, 1000, 
														pkgA, IPackageMemberMgr.SCOPE_PUBLIC));
		
		/* try to get a non-existent file */
		assertNull(pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE, 2000));
		
		/* try to place a file into a folder - should fail */
		int folder = pkgMgr.addFolder("Folder");
		assertEquals(ErrorCode.BAD_VALUE, 
					pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE, path1, 
													folder, IPackageMemberMgr.SCOPE_NONE));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the getFilesInPackage(int) and getFilesInPackage(int, int) methods,
	 * as well as the getFilesOutsidePackage(int) and getFilesOutsidePackage(int,int)
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testGetFilesInPackage() throws Exception {

		IFileMgr fileMgr = bs.getFileMgr();
		
		/* define a new package, which we'll add files to */
		int pkgA = pkgMgr.addPackage("PkgA");
		int pkgImport = pkgMgr.getImportPackage();
		
		/* what are the sections? */
		int sectPub = pkgMemberMgr.getScopeId("public");
		int sectPriv = pkgMemberMgr.getScopeId("private");
		
		/* initially, there are no files in the package (public, private, or any) */
		FileSet results = pkgMemberMgr.getFilesInPackage(pkgA);
		assertEquals(0, results.size());
		results = pkgMemberMgr.getFilesInPackage(pkgA, sectPub);
		assertEquals(0, results.size());
		results = pkgMemberMgr.getFilesInPackage(pkgA, sectPriv);
		assertEquals(0, results.size());
		
		/* 
		 * Nothing is outside the package either, since there are no files. However '/'
		 * is implicitly there all the time, so it'll be reported.
		 */
		int rootPathId = fileMgr.getPath("/");
		results = pkgMemberMgr.getFilesOutsidePackage(pkgA);
		assertEquals(2, results.size());		/* includes /tmp */
		results = pkgMemberMgr.getFilesOutsidePackage(pkgA, sectPub);
		assertEquals(2, results.size());		/* includes /tmp */
		results = pkgMemberMgr.getFilesOutsidePackage(pkgA, sectPriv);
		assertEquals(2, results.size());		/* includes /tmp */
		
		/* add a single file to the "private" section of pkgA */
		int file1 = fileMgr.addFile("/myfile1");
		pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE, file1, pkgA, sectPriv);
		
		/* check again - should be one file in pkgA and one in pkgA/priv */
		results = pkgMemberMgr.getFilesInPackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {file1}));
		results = pkgMemberMgr.getFilesInPackage(pkgA, sectPub);
		assertEquals(0, results.size());
		results = pkgMemberMgr.getFilesInPackage(pkgA, sectPriv);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {file1}));

		/* now, we one file in pkgA/priv, we have some files outside the other packages */
		results = pkgMemberMgr.getFilesOutsidePackage(pkgA);
		assertEquals(2, results.size());	/* includes /tmp */
		results = pkgMemberMgr.getFilesOutsidePackage(pkgA, sectPub);
		assertTrue(results.isMember(rootPathId));
		assertTrue(results.isMember(file1));
		results = pkgMemberMgr.getFilesOutsidePackage(pkgA, sectPriv);
		assertEquals(2, results.size());	/* includes /tmp */
		
		/* now add another to pkgA/priv and check again */
		int file2 = fileMgr.addFile("/myfile2");
		pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE, file2, pkgA, sectPriv);
		results = pkgMemberMgr.getFilesInPackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {file1, file2}));
		results = pkgMemberMgr.getFilesInPackage(pkgA, sectPub);
		assertEquals(0, results.size());
		results = pkgMemberMgr.getFilesInPackage(pkgA, sectPriv);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {file1, file2}));
		
		/* now, we two files, we have some more files outside */
		results = pkgMemberMgr.getFilesOutsidePackage(pkgA);
		assertEquals(2, results.size());	/* includes /tmp */
		results = pkgMemberMgr.getFilesOutsidePackage(pkgA, sectPub);
		assertTrue(results.isMember(file1));
		assertTrue(results.isMember(file2));
		results = pkgMemberMgr.getFilesOutsidePackage(pkgA, sectPriv);
		assertEquals(2, results.size());	/* include /tmp */
		
		/* finally, add one to pkgA/pub and check again */
		int file3 = fileMgr.addFile("/myfile3");
		pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE, file3, pkgA, sectPub);
		results = pkgMemberMgr.getFilesInPackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {file1, file2, file3}));
		results = pkgMemberMgr.getFilesInPackage(pkgA, sectPub);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {file3}));
		results = pkgMemberMgr.getFilesInPackage(pkgA, sectPriv);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {file1, file2}));		
		results = pkgMemberMgr.getFilesOutsidePackage(pkgA);
		assertEquals(2, results.size());	/* includes /tmp */
		results = pkgMemberMgr.getFilesOutsidePackage(pkgA, sectPub);
		assertTrue(results.isMember(file1));
		assertTrue(results.isMember(file2));
		results = pkgMemberMgr.getFilesOutsidePackage(pkgA, sectPriv);
		assertTrue(results.isMember(file3));
		
		/* move file1 back into <import> */
		pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE, file1, pkgImport, sectPriv);
		results = pkgMemberMgr.getFilesInPackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {file2, file3}));
		results = pkgMemberMgr.getFilesInPackage(pkgA, sectPub);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {file3}));
		results = pkgMemberMgr.getFilesInPackage(pkgA, sectPriv);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {file2}));
		
		/* now we have a file outside of pkgA */
		results = pkgMemberMgr.getFilesOutsidePackage(pkgA);
		assertTrue(results.isMember(file1));
		results = pkgMemberMgr.getFilesOutsidePackage(pkgA, sectPub);
		assertTrue(results.isMember(file1));
		assertTrue(results.isMember(file2));
		results = pkgMemberMgr.getFilesOutsidePackage(pkgA, sectPriv);
		assertTrue(results.isMember(file1));
		assertTrue(results.isMember(file3));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the getFilesInPackage(String) and getFilesOutsidePackage(String)
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testGetFilesInAndOutsidePackage() throws Exception {

		IFileMgr fileMgr = bs.getFileMgr();

		/* create a bunch of files */
		int f1path = fileMgr.addFile("/a/b/c/d/e/f1.c");
		int f2path = fileMgr.addFile("/a/b/c/d/e/f2.c");
		int f3path = fileMgr.addFile("/a/b/c/d/g/f3.c");
		int f4path = fileMgr.addFile("/b/c/d/f4.c");
		int f5path = fileMgr.addFile("/b/c/d/f5.c");

		/* create a new package, named "foo", with one item in foo/public and three in foo/private */
		IPackageMgr pkgMgr = bs.getPackageMgr();
		int pkgFooId = pkgMgr.addPackage("foo");
		assertEquals(ErrorCode.OK, 
						pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE, f1path, 
														pkgFooId, IPackageMemberMgr.SCOPE_PUBLIC));
		assertEquals(ErrorCode.OK,
						pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE, f2path,
														pkgFooId, IPackageMemberMgr.SCOPE_PRIVATE));
		assertEquals(ErrorCode.OK, 
						pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE, f4path,
														pkgFooId, IPackageMemberMgr.SCOPE_PRIVATE));
		assertEquals(ErrorCode.OK, 
						pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE, f5path,
														pkgFooId, IPackageMemberMgr.SCOPE_PRIVATE));

		/* test @foo/public membership */
		FileSet fs = pkgMemberMgr.getFilesInPackage("foo/public");
		assertEquals(1, fs.size());
		assertTrue(fs.isMember(f1path));

		/* test @foo/private membership */
		fs = pkgMemberMgr.getFilesInPackage("foo/private");
		assertEquals(3, fs.size());
		assertTrue(fs.isMember(f2path));
		assertTrue(fs.isMember(f4path));
		assertTrue(fs.isMember(f5path));

		/* test @foo membership */
		fs = pkgMemberMgr.getFilesInPackage("foo");
		assertEquals(4, fs.size());
		assertTrue(fs.isMember(f1path));
		assertTrue(fs.isMember(f2path));
		assertTrue(fs.isMember(f4path));
		assertTrue(fs.isMember(f5path));

		/* 
		 * Test ^@foo/public membership - will always include "/" and
		 * have a bunch of directories too.
		 */
		fs = pkgMemberMgr.getFilesOutsidePackage("foo/public");
		assertEquals(15, fs.size());		/* includes /tmp */
		assertTrue(fs.isMember(f2path));
		assertTrue(fs.isMember(f3path));
		assertTrue(fs.isMember(f4path));
		assertTrue(fs.isMember(f5path));

		/* test ^@foo/private membership - which includes directories*/
		fs = pkgMemberMgr.getFilesOutsidePackage("foo/private");
		assertEquals(13, fs.size());	/* includes /tmp */
		assertTrue(fs.isMember(f1path));
		assertTrue(fs.isMember(f3path));

		/* test ^@foo membership - which includes directories */
		fs = pkgMemberMgr.getFilesOutsidePackage("foo");
		assertEquals(12, fs.size());		/* includes /tmp */
		assertTrue(fs.isMember(f3path));
		
		/* test bad names */
		assertNull(pkgMemberMgr.getFilesInPackage("foo/badsect"));
		assertNull(pkgMemberMgr.getFilesOutsidePackage("pkg"));
		assertNull(pkgMemberMgr.getFilesOutsidePackage("foo/badsect"));
		assertNull(pkgMemberMgr.getFilesOutsidePackage("foo/"));
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the setActionPackage and getActionPackage methods.
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testActionPackages() throws Exception {
		
		IPackageMgr pkgMgr = bs.getPackageMgr();
		IActionMgr actionMgr = bs.getActionMgr();
		
		/* create a few actions */
		int action1 = actionMgr.addShellCommandAction(0, 0, "action1");
		int action2 = actionMgr.addShellCommandAction(0, 0, "action2");
		int action3 = actionMgr.addShellCommandAction(0, 0, "action3");
		
		/* create a couple of new packages */
		int pkgA = pkgMgr.addPackage("PkgA");
		int pkgB = pkgMgr.addPackage("PkgB");
		int pkgImport = pkgMgr.getImportPackage();
		
		/* by default, all actions are in "<import>" */
		assertEquals(pkgImport, pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_ACTION, action1).pkgId);
		assertEquals(pkgImport, pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_ACTION, action2).pkgId);
		assertEquals(pkgImport, pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_ACTION, action3).pkgId);
		
		/* add an action to PkgA and check the actions */
		pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_ACTION, action1, pkgA);
		assertEquals(pkgA, pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_ACTION, action1).pkgId);
		assertEquals(pkgImport, pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_ACTION, action2).pkgId);
		assertEquals(pkgImport, pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_ACTION, action3).pkgId);
		
		/* add a different action to PkgB and check the actions */
		pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_ACTION, action2, pkgB);
		assertEquals(pkgA, pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_ACTION, action1).pkgId);
		assertEquals(pkgB, pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_ACTION, action2).pkgId);
		assertEquals(pkgImport, pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_ACTION, action3).pkgId);
		
		/* revert one of the actions back to <import>, and check the actions */
		pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_ACTION, action1, pkgImport);
		assertEquals(pkgImport, pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_ACTION, action1).pkgId);
		assertEquals(pkgB, pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_ACTION, action2).pkgId);
		assertEquals(pkgImport, pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_ACTION, action3).pkgId);
		
		/* check an invalid action - should return ErrorCode.NOT_FOUND */
		assertNull(pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_ACTION, 1000));
		
		/* try to place an action into a folder - should fail */
		int folder = pkgMgr.addFolder("Folder");
		assertEquals(ErrorCode.BAD_VALUE, 
				pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_ACTION, action1, folder));
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the getActionsInPackage(int) method
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testGetActionsInPackage() throws Exception {
		IPackageMgr pkgMgr = bs.getPackageMgr();
		IActionMgr actionMgr = bs.getActionMgr();
		
		/* create a few actions */
		int action1 = actionMgr.addShellCommandAction(0, 0, "action1");
		int action2 = actionMgr.addShellCommandAction(0, 0, "action2");
		int action3 = actionMgr.addShellCommandAction(0, 0, "action3");
		
		/* create a couple of new packages */
		int pkgA = pkgMgr.addPackage("PkgA");
		int pkgB = pkgMgr.addPackage("PkgB");
		
		/* initially, pkgA is empty */
		ActionSet results = pkgMemberMgr.getActionsInPackage(pkgA);
		assertEquals(0, results.size());
		results = pkgMemberMgr.getActionsInPackage("PkgA");
		assertEquals(0, results.size());
		results = pkgMemberMgr.getActionsOutsidePackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action2, action3}));
		results = pkgMemberMgr.getActionsOutsidePackage("PkgA");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action2, action3}));		
		
		/* add an action to pkgA */
		pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_ACTION, action1, pkgA);
		results = pkgMemberMgr.getActionsInPackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1}));
		results = pkgMemberMgr.getActionsInPackage("PkgA");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1}));
		results = pkgMemberMgr.getActionsOutsidePackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action2, action3}));
		results = pkgMemberMgr.getActionsOutsidePackage("PkgA");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action2, action3}));

		/* add another action to pkgA */
		pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_ACTION, action3, pkgA);
		results = pkgMemberMgr.getActionsInPackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action3}));
		results = pkgMemberMgr.getActionsInPackage("PkgA");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action3}));
		results = pkgMemberMgr.getActionsOutsidePackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action2}));
		results = pkgMemberMgr.getActionsOutsidePackage("PkgA");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action2}));

		/* Add a third */
		pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_ACTION, action2, pkgA);
		results = pkgMemberMgr.getActionsInPackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action2, action3}));
		results = pkgMemberMgr.getActionsInPackage("PkgA");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action2, action3}));
		results = pkgMemberMgr.getActionsOutsidePackage(pkgA);
		assertEquals(0, results.size());
		results = pkgMemberMgr.getActionsOutsidePackage("PkgA");
		assertEquals(0, results.size());

		/* move the second action into pkgB */
		pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_ACTION, action2, pkgB);
		results = pkgMemberMgr.getActionsInPackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action3}));
		results = pkgMemberMgr.getActionsInPackage("PkgA");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action3}));
		results = pkgMemberMgr.getActionsInPackage(pkgB);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action2}));
		results = pkgMemberMgr.getActionsInPackage("PkgB");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action2}));
		results = pkgMemberMgr.getActionsOutsidePackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action2}));
		results = pkgMemberMgr.getActionsOutsidePackage("PkgA");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action2}));
		results = pkgMemberMgr.getActionsOutsidePackage(pkgB);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action3}));
		results = pkgMemberMgr.getActionsOutsidePackage("PkgB");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action3}));
		
		/* try some bad package names */
		assertNull(pkgMemberMgr.getActionsInPackage("badname"));
		assertNull(pkgMemberMgr.getActionsInPackage("PkgA/private"));
		assertNull(pkgMemberMgr.getActionsInPackage(""));		
	}

	/*-------------------------------------------------------------------------------------*/

	/** Our tests set these appropriately */
	private int notifyPkgValue, notifyHowValue = 0, notifyType = 0, notifyId = 0;
	
	/**
	 * Test listener notifications
	 */
	@Test
	public void testNotifyPackageMembership() {

		/* set up a listener for the pkgMemberMgr */
		IPackageMemberMgrListener pkgMemberListener = new IPackageMemberMgrListener() {
			@Override
			public void packageMemberChangeNotification(int pkgId, int how, int memberType, int memberId) {
				TestPackageMemberMgr.this.notifyPkgValue = pkgId;
				TestPackageMemberMgr.this.notifyHowValue = how;
				TestPackageMemberMgr.this.notifyType = memberType;
				TestPackageMemberMgr.this.notifyId = memberId;
			}
		};
		pkgMemberMgr.addListener(pkgMemberListener);

		/* 
		 * Changing an action's package should trigger a notification. We actually
		 * see two notifications (for old, then new packages), but we only test
		 * for the new package.
		 */
		notifyPkgValue = 0;
		notifyHowValue = 0;
		int actionId = actionMgr.addShellCommandAction(actionMgr.getRootAction("root"), fileMgr.getPath("/"), "");
		int pkgD = pkgMgr.addPackage("PkgD");
		assert(actionId >= 0);
		assertEquals(ErrorCode.OK, pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_ACTION, actionId, pkgD));
		assertEquals(pkgD, notifyPkgValue);
		assertEquals(IPackageMemberMgr.TYPE_ACTION, notifyType);
		assertEquals(actionId, notifyId);
		assertEquals(IPackageMemberMgrListener.CHANGED_MEMBERSHIP, notifyHowValue);
		
		/* Changing it to the same thing, will not trigger a notification */
		notifyPkgValue = notifyHowValue = notifyType = notifyId = 0;
		assertEquals(ErrorCode.OK, pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_ACTION, actionId, pkgD));
		assertEquals(0, notifyPkgValue);
		assertEquals(0, notifyHowValue);	
		assertEquals(0, notifyType);	
		assertEquals(0, notifyId);
	}
		
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IPackageMemberMgr#setMemberLocation(int, int, int, int)}
	 */
	@Test
	public void testSetLocation() {
		
		/* create some default actions */
		int rootAction = actionMgr.getRootAction("root");
		int rootDir = fileMgr.getPath("/");
		int action1 = actionMgr.addShellCommandAction(rootAction, rootDir, "Action 1");
		int action2 = actionMgr.addShellCommandAction(rootAction, rootDir, "Action 2");
	
		/* initially their x and y should be -1 */
		MemberLocation result = pkgMemberMgr.getMemberLocation(IPackageMemberMgr.TYPE_ACTION, action1);
		assertEquals(-1, result.x);
		assertEquals(-1, result.y);
		result = pkgMemberMgr.getMemberLocation(IPackageMemberMgr.TYPE_ACTION, action2);
		assertEquals(-1, result.x);
		assertEquals(-1, result.y);
		
		/* set the coordinates for action 1 to (100, 200) */
		assertEquals(ErrorCode.OK, pkgMemberMgr.setMemberLocation(IPackageMemberMgr.TYPE_ACTION, action1, 100, 200));
		result = pkgMemberMgr.getMemberLocation(IPackageMemberMgr.TYPE_ACTION, action2);
		assertEquals(-1, result.x);
		assertEquals(-1, result.y);
		result = pkgMemberMgr.getMemberLocation(IPackageMemberMgr.TYPE_ACTION, action1);
		assertEquals(100, result.x);
		assertEquals(200, result.y);
		
		/* set the coordinates for action 2 to (76, 34) */
		assertEquals(ErrorCode.OK, pkgMemberMgr.setMemberLocation(IPackageMemberMgr.TYPE_ACTION, action2, 76, 34));
		result = pkgMemberMgr.getMemberLocation(IPackageMemberMgr.TYPE_ACTION, action2);
		assertEquals(76, result.x);
		assertEquals(34, result.y);
		result = pkgMemberMgr.getMemberLocation(IPackageMemberMgr.TYPE_ACTION, action1);
		assertEquals(100, result.x);
		assertEquals(200, result.y);
		
		/* test for invalid action Id */
		assertNull(pkgMemberMgr.getMemberLocation(IPackageMemberMgr.TYPE_ACTION, 1000));
		assertEquals(ErrorCode.BAD_VALUE, pkgMemberMgr.setMemberLocation(IPackageMemberMgr.TYPE_ACTION, 200, 100, 200));	
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test listener notifications for changes in a member's location.
	 */
	@Test
	public void testNotifyMemberLocation() {

		/* set up a listener for the pkgMemberMgr */
		IPackageMemberMgrListener pkgMemberListener = new IPackageMemberMgrListener() {
			@Override
			public void packageMemberChangeNotification(int pkgId, int how, int memberType, int memberId) {
				TestPackageMemberMgr.this.notifyPkgValue = pkgId;
				TestPackageMemberMgr.this.notifyHowValue = how;
				TestPackageMemberMgr.this.notifyType = memberType;
				TestPackageMemberMgr.this.notifyId = memberId;
			}
		};
		pkgMemberMgr.addListener(pkgMemberListener);

		/* Create a FileGroup that we'll move around the package */
		int pkgA = pkgMgr.addPackage("PkgA");
		int fileGroup1 = fileGroupMgr.newSourceGroup(pkgA);
		
		/* Changing an fileGroup's location will trigger a notification */
		notifyPkgValue = notifyHowValue = notifyType = notifyId = 0;
		assertEquals(ErrorCode.OK,
				pkgMemberMgr.setMemberLocation(IPackageMemberMgr.TYPE_FILE_GROUP, fileGroup1, 100, 300));
		assertEquals(pkgA, notifyPkgValue);
		assertEquals(IPackageMemberMgr.TYPE_FILE_GROUP, notifyType);
		assertEquals(fileGroup1, notifyId);
		assertEquals(IPackageMemberMgrListener.CHANGED_LOCATION, notifyHowValue);
		
		/* Changing it to the same thing, will not trigger a notification */
		notifyPkgValue = notifyHowValue = notifyType = notifyId = 0;
		assertEquals(ErrorCode.OK,
				pkgMemberMgr.setMemberLocation(IPackageMemberMgr.TYPE_FILE_GROUP, fileGroup1, 100, 300));
		assertEquals(0, notifyPkgValue);
		assertEquals(0, notifyHowValue);	
		assertEquals(0, notifyType);	
		assertEquals(0, notifyId);
		
		/* change it again, to a different location */
		notifyPkgValue = notifyHowValue = notifyType = notifyId = 0;
		assertEquals(ErrorCode.OK,
				pkgMemberMgr.setMemberLocation(IPackageMemberMgr.TYPE_FILE_GROUP, fileGroup1, 101, 300));
		assertEquals(pkgA, notifyPkgValue);
		assertEquals(IPackageMemberMgr.TYPE_FILE_GROUP, notifyType);
		assertEquals(fileGroup1, notifyId);
		assertEquals(IPackageMemberMgrListener.CHANGED_LOCATION, notifyHowValue);
		
		/* removing the listener causes the notification to stop */
		pkgMemberMgr.removeListener(pkgMemberListener);
		notifyPkgValue = notifyHowValue = notifyType = notifyId = 0;
		assertEquals(ErrorCode.OK,
				pkgMemberMgr.setMemberLocation(IPackageMemberMgr.TYPE_FILE_GROUP, fileGroup1, 101, 301));
		assertEquals(0, notifyPkgValue);
		assertEquals(0, notifyHowValue);	
		assertEquals(0, notifyType);	
		assertEquals(0, notifyId);		
	}
	
	/*-------------------------------------------------------------------------------------*/

}
