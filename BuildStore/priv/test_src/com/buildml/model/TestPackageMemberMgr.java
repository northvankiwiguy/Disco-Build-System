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
		Integer results[] = pkgMemberMgr.parsePkgSpec("pkg1");
		assertEquals(pkg1, results[0].intValue());
		assertEquals(0, results[1].intValue());
		
		results = pkgMemberMgr.parsePkgSpec("pkg2");
		assertEquals(pkg2, results[0].intValue());
		assertEquals(0, results[1].intValue());
		
		/* test pkgSpecs with both package and scope names */
		results = pkgMemberMgr.parsePkgSpec("pkg1/private");
		assertEquals(pkg1, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_PRIVATE, results[1].intValue());
		
		results = pkgMemberMgr.parsePkgSpec("pkg2/public");
		assertEquals(pkg2, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_PUBLIC, results[1].intValue());
		
		/* test invalid pkgSpecs */
		results = pkgMemberMgr.parsePkgSpec("badname");
		assertEquals(ErrorCode.NOT_FOUND, results[0].intValue());
		assertEquals(0, results[1].intValue());
		
		results = pkgMemberMgr.parsePkgSpec("pkg1/missing");
		assertEquals(pkg1, results[0].intValue());
		assertEquals(ErrorCode.NOT_FOUND, results[1].intValue());
		
		results = pkgMemberMgr.parsePkgSpec("badname/missing");
		assertEquals(ErrorCode.NOT_FOUND, results[0].intValue());
		assertEquals(ErrorCode.NOT_FOUND, results[1].intValue());
		
		results = pkgMemberMgr.parsePkgSpec("badname/public");
		assertEquals(ErrorCode.NOT_FOUND, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_PUBLIC, results[1].intValue());
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
		Integer results[] = pkgMemberMgr.getFilePackage(path1);
		assertEquals(pkgImport, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_NONE, results[1].intValue());
		results = pkgMemberMgr.getFilePackage(path2);
		assertEquals(pkgImport, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_NONE, results[1].intValue());
		results = pkgMemberMgr.getFilePackage(path3);
		assertEquals(pkgImport, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_NONE, results[1].intValue());

		/* set one of the files into PkgA/public */
		assertEquals(ErrorCode.OK, pkgMemberMgr.setFilePackage(path1, pkgA, IPackageMgr.SCOPE_PUBLIC));
		results = pkgMemberMgr.getFilePackage(path1);
		assertEquals(pkgA, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_PUBLIC, results[1].intValue());
		results = pkgMemberMgr.getFilePackage(path2);
		assertEquals(pkgImport, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_NONE, results[1].intValue());
		results = pkgMemberMgr.getFilePackage(path3);
		assertEquals(pkgImport, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_NONE, results[1].intValue());
		
		/* set another file to another package */
		assertEquals(ErrorCode.OK, pkgMemberMgr.setFilePackage(path3, pkgB, IPackageMgr.SCOPE_PRIVATE));
		results = pkgMemberMgr.getFilePackage(path1);
		assertEquals(pkgA, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_PUBLIC, results[1].intValue());
		results = pkgMemberMgr.getFilePackage(path2);
		assertEquals(pkgImport, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_NONE, results[1].intValue());
		results = pkgMemberMgr.getFilePackage(path3);
		assertEquals(pkgB, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_PRIVATE, results[1].intValue());
		
		/* set a file's package back to <import>/None */
		assertEquals(ErrorCode.OK, pkgMemberMgr.setFilePackage(path1, pkgImport, IPackageMgr.SCOPE_NONE));
		results = pkgMemberMgr.getFilePackage(path1);
		assertEquals(pkgImport, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_NONE, results[1].intValue());
		
		/* try to set a non-existent file */
		assertEquals(ErrorCode.NOT_FOUND, pkgMemberMgr.setFilePackage(1000, pkgA, IPackageMgr.SCOPE_PUBLIC));
		
		/* try to get a non-existent file */
		assertNull(pkgMemberMgr.getFilePackage(2000));
		
		/* try to place a file into a folder - should fail */
		int folder = pkgMgr.addFolder("Folder");
		assertEquals(ErrorCode.BAD_VALUE, pkgMemberMgr.setFilePackage(path1, folder, IPackageMgr.SCOPE_NONE));
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
		pkgMemberMgr.setFilePackage(file1, pkgA, sectPriv);
		
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
		pkgMemberMgr.setFilePackage(file2, pkgA, sectPriv);
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
		pkgMemberMgr.setFilePackage(file3, pkgA, sectPub);
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
		pkgMemberMgr.setFilePackage(file1, pkgImport, sectPriv);
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
		assertEquals(ErrorCode.OK, pkgMemberMgr.setFilePackage(f1path, pkgFooId, IPackageMgr.SCOPE_PUBLIC));
		assertEquals(ErrorCode.OK, pkgMemberMgr.setFilePackage(f2path, pkgFooId, IPackageMgr.SCOPE_PRIVATE));
		assertEquals(ErrorCode.OK, pkgMemberMgr.setFilePackage(f4path, pkgFooId, IPackageMgr.SCOPE_PRIVATE));
		assertEquals(ErrorCode.OK, pkgMemberMgr.setFilePackage(f5path, pkgFooId, IPackageMgr.SCOPE_PRIVATE));

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
		assertEquals(pkgImport, pkgMemberMgr.getActionPackage(action1));
		assertEquals(pkgImport, pkgMemberMgr.getActionPackage(action2));
		assertEquals(pkgImport, pkgMemberMgr.getActionPackage(action3));
		
		/* add an action to PkgA and check the actions */
		pkgMemberMgr.setActionPackage(action1, pkgA);
		assertEquals(pkgA, pkgMemberMgr.getActionPackage(action1));
		assertEquals(pkgImport, pkgMemberMgr.getActionPackage(action2));
		assertEquals(pkgImport, pkgMemberMgr.getActionPackage(action3));
		
		/* add a different action to PkgB and check the actions */
		pkgMemberMgr.setActionPackage(action2, pkgB);
		assertEquals(pkgA, pkgMemberMgr.getActionPackage(action1));
		assertEquals(pkgB, pkgMemberMgr.getActionPackage(action2));
		assertEquals(pkgImport, pkgMemberMgr.getActionPackage(action3));
		
		/* revert one of the actions back to <import>, and check the actions */
		pkgMemberMgr.setActionPackage(action1, pkgImport);
		assertEquals(pkgImport, pkgMemberMgr.getActionPackage(action1));
		assertEquals(pkgB, pkgMemberMgr.getActionPackage(action2));
		assertEquals(pkgImport, pkgMemberMgr.getActionPackage(action3));
		
		/* check an invalid action - should return ErrorCode.NOT_FOUND */
		assertEquals(ErrorCode.NOT_FOUND, pkgMemberMgr.getActionPackage(1000));
		
		/* try to place an action into a folder - should fail */
		int folder = pkgMgr.addFolder("Folder");
		assertEquals(ErrorCode.BAD_VALUE, pkgMemberMgr.setActionPackage(action1, folder));
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
		pkgMemberMgr.setActionPackage(action1, pkgA);
		results = pkgMemberMgr.getActionsInPackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1}));
		results = pkgMemberMgr.getActionsInPackage("PkgA");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1}));
		results = pkgMemberMgr.getActionsOutsidePackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action2, action3}));
		results = pkgMemberMgr.getActionsOutsidePackage("PkgA");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action2, action3}));

		/* add another action to pkgA */
		pkgMemberMgr.setActionPackage(action3, pkgA);
		results = pkgMemberMgr.getActionsInPackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action3}));
		results = pkgMemberMgr.getActionsInPackage("PkgA");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action3}));
		results = pkgMemberMgr.getActionsOutsidePackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action2}));
		results = pkgMemberMgr.getActionsOutsidePackage("PkgA");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action2}));

		/* Add a third */
		pkgMemberMgr.setActionPackage(action2, pkgA);
		results = pkgMemberMgr.getActionsInPackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action2, action3}));
		results = pkgMemberMgr.getActionsInPackage("PkgA");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action2, action3}));
		results = pkgMemberMgr.getActionsOutsidePackage(pkgA);
		assertEquals(0, results.size());
		results = pkgMemberMgr.getActionsOutsidePackage("PkgA");
		assertEquals(0, results.size());

		/* move the second action into pkgB */
		pkgMemberMgr.setActionPackage(action2, pkgB);
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
	private int notifyPkgValue = 0;
	private int notifyHowValue = 0;
	
	/**
	 * Test listener notifications
	 */
	@Test
	public void testNotify() {

		/* set up a listener for the pkgMgr */
		IPackageMgrListener pkgListener = new IPackageMgrListener() {
			@Override
			public void packageChangeNotification(int pkgId, int how) {
				TestPackageMemberMgr.this.notifyPkgValue = pkgId;
				TestPackageMemberMgr.this.notifyHowValue = how;
			}
		};
		pkgMgr.addListener(pkgListener);

		/* set up a listener for the pkgMemberMgr */
		IPackageMemberMgrListener pkgMemberListener = new IPackageMemberMgrListener() {
			@Override
			public void packageMemberChangeNotification(int pkgId, int how) {
				TestPackageMemberMgr.this.notifyPkgValue = pkgId;
				TestPackageMemberMgr.this.notifyHowValue = how;
			}
		};
		pkgMemberMgr.addListener(pkgMemberListener);

		notifyPkgValue = 0;
		notifyHowValue = 0;
		int pkgA = pkgMgr.addPackage("PkgA");
		assertEquals(pkgA, notifyPkgValue);
		assertEquals(IPackageMgrListener.ADDED_PACKAGE, notifyHowValue);		
		
		/* Changing a name to itself doesn't trigger the notification */
		notifyPkgValue = 0;
		notifyHowValue = 0;
		assertEquals(ErrorCode.OK, pkgMgr.setName(pkgA, "PkgA"));
		assertEquals(0, notifyPkgValue);
		assertEquals(0, notifyHowValue);

		/* Changing a name to something new will trigger the notification */
		assertEquals(ErrorCode.OK, pkgMgr.setName(pkgA, "PkgB"));
		assertEquals(pkgA, notifyPkgValue);
		assertEquals(IPackageMgrListener.CHANGED_NAME, notifyHowValue);
		
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
		assertEquals(ErrorCode.OK, pkgMemberMgr.setActionPackage(actionId, pkgD));
		assertEquals(pkgD, notifyPkgValue);
		assertEquals(IPackageMemberMgrListener.CHANGED_MEMBERSHIP, notifyHowValue);
		
		/* Changing it to the same thing, will not */
		notifyPkgValue = 0;
		notifyHowValue = 0;
		assertEquals(ErrorCode.OK, pkgMemberMgr.setActionPackage(actionId, pkgD));
		assertEquals(0, notifyPkgValue);
		assertEquals(0, notifyHowValue);		

		/* remove the pkgListener, and change the package again */
		pkgMgr.removeListener(pkgListener);
		notifyPkgValue = 0;
		notifyHowValue = 0;
		assertEquals(ErrorCode.OK, pkgMgr.setName(pkgA, "PkgC"));
		assertEquals(0, notifyPkgValue);
		assertEquals(0, notifyHowValue);
		
		/* add the listener back, then remove the package */
		pkgMgr.addListener(pkgListener);
		assertEquals(ErrorCode.OK, pkgMgr.remove(pkgA));
		assertEquals(pkgA, notifyPkgValue);
		assertEquals(IPackageMgrListener.REMOVED_PACKAGE, notifyHowValue);		
	}
	
	/*-------------------------------------------------------------------------------------*/
}
