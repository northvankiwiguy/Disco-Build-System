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
public class TestPackageMgr {

	/** Our BuildStore object, used in many test cases */
	private IBuildStore bs;

	/** The Packages object associated with this BuildStore */
	private IPackageMgr pkgMgr;
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		bs = CommonTestUtils.getEmptyBuildStore();
		pkgMgr = bs.getPackageMgr();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IPackageMgr#addPackage(java.lang.String)}.
	 */
	@Test
	public void testAddPackage() {
		
		/* add valid package names, and check their IDs are unique */
		int c1 = pkgMgr.addPackage("package1");
		assertNotSame(ErrorCode.ALREADY_USED, c1);
		assertNotSame(ErrorCode.INVALID_NAME, c1);

		int c2 = pkgMgr.addPackage("my_second_package");
		assertNotSame(ErrorCode.ALREADY_USED, c2);
		assertNotSame(ErrorCode.INVALID_NAME, c2);
		assertNotSame(c1, c2);

		int c3 = pkgMgr.addPackage("one-more-package");
		assertNotSame(ErrorCode.ALREADY_USED, c3);
		assertNotSame(ErrorCode.INVALID_NAME, c3);
		assertNotSame(c1, c3);
		assertNotSame(c2, c3);
		
		/* add packages that have already been added */
		int c4 = pkgMgr.addPackage("package1");
		assertEquals(ErrorCode.ALREADY_USED, c4);
		int c5 = pkgMgr.addPackage("my_second_package");
		assertEquals(ErrorCode.ALREADY_USED, c5);
		
		/* add packages with invalid names */
		int c6 = pkgMgr.addPackage("pkg*with-bad_name");
		assertEquals(ErrorCode.INVALID_NAME, c6);
		int c7 = pkgMgr.addPackage("pkg with-bad name");
		assertEquals(ErrorCode.INVALID_NAME, c7);
		int c8 = pkgMgr.addPackage("pkg/with-bad name");
		assertEquals(ErrorCode.INVALID_NAME, c8);
		int c9 = pkgMgr.addPackage("pkg+with-bad name");
		assertEquals(ErrorCode.INVALID_NAME, c9);
		int c10 = pkgMgr.addPackage(null);
		assertEquals(ErrorCode.INVALID_NAME, c10);
		int c11 = pkgMgr.addPackage("ab");
		assertEquals(ErrorCode.INVALID_NAME, c11);	
		int c12 = pkgMgr.addPackage("1more");
		assertEquals(ErrorCode.INVALID_NAME, c12);	
		
		/* try to add the None package, which exists by default */
		int cNone = pkgMgr.addPackage("None");
		assertEquals(ErrorCode.ALREADY_USED, cNone);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IPackageMgr#getPackageName(int)}.
	 */
	@Test
	public void testGetPackageName() {
		
		/* add package names, then make sure their names are correct */
		int c1 = pkgMgr.addPackage("PkgA");
		assertEquals("PkgA", pkgMgr.getPackageName(c1));
		int c2 = pkgMgr.addPackage("my-other-package");
		assertEquals("my-other-package", pkgMgr.getPackageName(c2));
		int c3 = pkgMgr.addPackage("and_a_3rd");
		assertEquals("and_a_3rd", pkgMgr.getPackageName(c3));
				
		/* try to fetch names for invalid package IDs */
		assertEquals(null, pkgMgr.getPackageName(1000));		
		assertEquals(null, pkgMgr.getPackageName(-1));
		
		/* test for the default "None" package */
		assertEquals("None", pkgMgr.getPackageName(0));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IPackageMgr#getPackageId(String)}.
	 */
	@Test
	public void testGetPackageId() {
		
		/* get the ID for the "None" package */
		assertEquals(0, pkgMgr.getPackageId("None"));

		/* fetch package names that haven't been added - should be NOT_FOUND */
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getPackageId("foo"));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getPackageId("my_package"));
		
		/* add package names, then make sure their IDs are correct */
		int c1 = pkgMgr.addPackage("my_pkg_A");
		int c2 = pkgMgr.addPackage("ourpackage");
		int c3 = pkgMgr.addPackage("another-package");
		assertEquals(c1, pkgMgr.getPackageId("my_pkg_A"));
		assertEquals(c3, pkgMgr.getPackageId("another-package"));
		assertEquals(c2, pkgMgr.getPackageId("ourpackage"));
		
		/* add duplicate names, and make sure the old ID is returned */
		int c4 = pkgMgr.addPackage("ourpackage");
		assertEquals(ErrorCode.ALREADY_USED, c4);
		int c5 = pkgMgr.addPackage("another-package");
		assertEquals(ErrorCode.ALREADY_USED, c5);
		assertEquals(c3, pkgMgr.getPackageId("another-package"));
		assertEquals(c2, pkgMgr.getPackageId("ourpackage"));
				
		/* Names that haven't been added will return NOT_FOUND */
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getPackageId("invalid"));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getPackageId("missing"));
		
		/* try to fetch IDs for invalid package names - returns NOT_FOUND */
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getPackageId("miss*ing"));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getPackageId("invalid name"));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IPackageMgr#removePackage(java.lang.String)}.
	 */
	@Test
	public void testRemovePackage() {
		
		IFileMgr fileMgr = bs.getFileMgr();
		IActionMgr actionMgr = bs.getActionMgr();
		
		/* try to remove package names that haven't been added */
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.removePackage("PkgA"));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.removePackage("my_package"));
		
		/* try to remove the "None" package - should fail */
		assertEquals(ErrorCode.CANT_REMOVE, pkgMgr.removePackage("None"));

		/* add package names, then remove them */
		assertTrue(pkgMgr.addPackage("PkgA") > 0);
		assertTrue(pkgMgr.addPackage("my_package") > 0);	
		assertEquals(ErrorCode.OK, pkgMgr.removePackage("PkgA"));
		assertEquals(ErrorCode.OK, pkgMgr.removePackage("my_package"));
		
		/* try to remove the same names again - should fail */
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.removePackage("PkgA"));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.removePackage("my_package"));
		
		/* add the same package names again - should work */
		assertTrue(pkgMgr.addPackage("PkgA") > 0);
		assertTrue(pkgMgr.addPackage("my_package") > 0);	
		
		/* assign a package to files, then try to remove the name */
		int pkgA = pkgMgr.getPackageId("PkgA");
		int file1 = fileMgr.addFile("/aardvark/bunny");
		pkgMgr.setFilePackage(file1, pkgA, IPackageMgr.SCOPE_PRIVATE);
		assertEquals(ErrorCode.CANT_REMOVE, pkgMgr.removePackage("PkgA"));
		
		/* remove the package from the file, then try again to remove the package name */
		int pkgNone = pkgMgr.getPackageId("None");
		pkgMgr.setFilePackage(file1, pkgNone, IPackageMgr.SCOPE_PRIVATE);
		assertEquals(ErrorCode.OK, pkgMgr.removePackage("PkgA"));
		
		/* assign them to actions, then try to remove the name */
		int my_pkg = pkgMgr.getPackageId("my_package");
		int action1 = actionMgr.addAction(0, 0, "action1");
		pkgMgr.setActionPackage(action1, my_pkg);
		assertEquals(ErrorCode.CANT_REMOVE, pkgMgr.removePackage("my_package"));
		
		/* remove them from actions, then try again to remove the package name */
		pkgMgr.setActionPackage(action1, pkgNone);
		assertEquals(ErrorCode.OK, pkgMgr.removePackage("my_package"));		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IPackageMgr#getPackages()}.
	 */
	@Test
	public void testGetPackages() {
		
		/* fetch the list of packages, before adding any. */
		String results[] = pkgMgr.getPackages();
		assertArrayEquals(new String[] {"None"}, results);
		
		/* add some packages, then check the list */
		pkgMgr.addPackage("my_pkg1");
		pkgMgr.addPackage("my_pkg2");
		results = pkgMgr.getPackages();
		assertArrayEquals(new String[] {"my_pkg1", "my_pkg2", "None"}, results);		
		
		/* add some more, then check again */
		pkgMgr.addPackage("Linux");
		pkgMgr.addPackage("freeBSD");
		results = pkgMgr.getPackages();
		assertArrayEquals(new String[] {"freeBSD", "Linux", "my_pkg1", 
				"my_pkg2", "None"}, results);		
		
		/* remove some packages, then check the list again */
		pkgMgr.removePackage("Linux");
		pkgMgr.removePackage("my_pkg2");
		results = pkgMgr.getPackages();
		assertArrayEquals(new String[] {"freeBSD", "my_pkg1", "None"}, results);		
		
		/* add some names back, and re-check the list */
		pkgMgr.addPackage("Linux");
		pkgMgr.addPackage("MacOS");
		results = pkgMgr.getPackages();
		assertArrayEquals(new String[] {"freeBSD", "Linux", "MacOS", "my_pkg1", 
				"None"}, results);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test method for {@link com.buildml.model.IPackageMgr#getScopeName(int)}.
	 */
	@Test
	public void testGetScopeName() {
		
		/* Test valid section IDs */
		assertEquals("None", pkgMgr.getScopeName(0));
		assertEquals("Private", pkgMgr.getScopeName(1));
		assertEquals("Public", pkgMgr.getScopeName(2));

		/* Test invalid section IDs */
		assertNull(pkgMgr.getScopeName(3));
		assertNull(pkgMgr.getScopeName(4));
		assertNull(pkgMgr.getScopeName(100));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IPackageMgr#getScopeId(String)}.
	 */
	@Test
	public void testGetScopeId() {
		
		/* Test valid section names */
		assertEquals(0, pkgMgr.getScopeId("None"));
		assertEquals(1, pkgMgr.getScopeId("priv"));
		assertEquals(1, pkgMgr.getScopeId("private"));
		assertEquals(1, pkgMgr.getScopeId("Private"));
		assertEquals(2, pkgMgr.getScopeId("pub"));
		assertEquals(2, pkgMgr.getScopeId("public"));
		
		/* Test invalid section names */
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getScopeId("object"));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getScopeId("obj"));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getScopeId("pretty"));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getScopeId("shiny"));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IPackageMgr#parsePkgSpec(String)}.
	 * @exception Exception Something bad happened
	 */
	@Test
	public void testParsePkgSpec() throws Exception {
		
		/* add a couple of packages */
		int pkg1 = pkgMgr.addPackage("pkg1");
		int pkg2 = pkgMgr.addPackage("pkg2");
		
		/* test the pkgSpecs with only package names */
		Integer results[] = pkgMgr.parsePkgSpec("pkg1");
		assertEquals(pkg1, results[0].intValue());
		assertEquals(0, results[1].intValue());
		
		results = pkgMgr.parsePkgSpec("pkg2");
		assertEquals(pkg2, results[0].intValue());
		assertEquals(0, results[1].intValue());
		
		/* test pkgSpecs with both package and scope names */
		results = pkgMgr.parsePkgSpec("pkg1/private");
		assertEquals(pkg1, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_PRIVATE, results[1].intValue());
		
		results = pkgMgr.parsePkgSpec("pkg2/public");
		assertEquals(pkg2, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_PUBLIC, results[1].intValue());
		
		/* test invalid pkgSpecs */
		results = pkgMgr.parsePkgSpec("badname");
		assertEquals(ErrorCode.NOT_FOUND, results[0].intValue());
		assertEquals(0, results[1].intValue());
		
		results = pkgMgr.parsePkgSpec("pkg1/missing");
		assertEquals(pkg1, results[0].intValue());
		assertEquals(ErrorCode.NOT_FOUND, results[1].intValue());
		
		results = pkgMgr.parsePkgSpec("badname/missing");
		assertEquals(ErrorCode.NOT_FOUND, results[0].intValue());
		assertEquals(ErrorCode.NOT_FOUND, results[1].intValue());
		
		results = pkgMgr.parsePkgSpec("badname/public");
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
		int pkgNone = pkgMgr.getPackageId("None");

		/* by default, all files are in None/None */
		Integer results[] = pkgMgr.getFilePackage(path1);
		assertEquals(pkgNone, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_NONE, results[1].intValue());
		results = pkgMgr.getFilePackage(path2);
		assertEquals(pkgNone, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_NONE, results[1].intValue());
		results = pkgMgr.getFilePackage(path3);
		assertEquals(pkgNone, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_NONE, results[1].intValue());

		/* set one of the files into PkgA/public */
		assertEquals(ErrorCode.OK, pkgMgr.setFilePackage(path1, pkgA, IPackageMgr.SCOPE_PUBLIC));
		results = pkgMgr.getFilePackage(path1);
		assertEquals(pkgA, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_PUBLIC, results[1].intValue());
		results = pkgMgr.getFilePackage(path2);
		assertEquals(pkgNone, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_NONE, results[1].intValue());
		results = pkgMgr.getFilePackage(path3);
		assertEquals(pkgNone, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_NONE, results[1].intValue());
		
		/* set another file to another package */
		assertEquals(ErrorCode.OK, pkgMgr.setFilePackage(path3, pkgB, IPackageMgr.SCOPE_PRIVATE));
		results = pkgMgr.getFilePackage(path1);
		assertEquals(pkgA, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_PUBLIC, results[1].intValue());
		results = pkgMgr.getFilePackage(path2);
		assertEquals(pkgNone, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_NONE, results[1].intValue());
		results = pkgMgr.getFilePackage(path3);
		assertEquals(pkgB, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_PRIVATE, results[1].intValue());
		
		/* set a file's package back to None/None */
		assertEquals(ErrorCode.OK, pkgMgr.setFilePackage(path1, pkgNone, IPackageMgr.SCOPE_NONE));
		results = pkgMgr.getFilePackage(path1);
		assertEquals(pkgNone, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_NONE, results[1].intValue());
		
		/* try to set a non-existent file */
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.setFilePackage(1000, pkgA, IPackageMgr.SCOPE_PUBLIC));
		
		/* try to get a non-existent file */
		assertNull(pkgMgr.getFilePackage(2000));
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
		int pkgNone = pkgMgr.addPackage("None");
		
		/* what are the sections? */
		int sectPub = pkgMgr.getScopeId("public");
		int sectPriv = pkgMgr.getScopeId("private");
		
		/* initially, there are no files in the package (public, private, or any) */
		FileSet results = pkgMgr.getFilesInPackage(pkgA);
		assertEquals(0, results.size());
		results = pkgMgr.getFilesInPackage(pkgA, sectPub);
		assertEquals(0, results.size());
		results = pkgMgr.getFilesInPackage(pkgA, sectPriv);
		assertEquals(0, results.size());
		
		/* 
		 * Nothing is outside the package either, since there are no files. However '/'
		 * is implicitly there all the time, so it'll be reported.
		 */
		int rootPathId = fileMgr.getPath("/");
		results = pkgMgr.getFilesOutsidePackage(pkgA);
		assertEquals(1, results.size());
		results = pkgMgr.getFilesOutsidePackage(pkgA, sectPub);
		assertEquals(1, results.size());
		results = pkgMgr.getFilesOutsidePackage(pkgA, sectPriv);
		assertEquals(1, results.size());
		
		/* add a single file to the "private" section of pkgA */
		int file1 = fileMgr.addFile("/myfile1");
		pkgMgr.setFilePackage(file1, pkgA, sectPriv);
		
		/* check again - should be one file in pkgA and one in pkgA/priv */
		results = pkgMgr.getFilesInPackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {file1}));
		results = pkgMgr.getFilesInPackage(pkgA, sectPub);
		assertEquals(0, results.size());
		results = pkgMgr.getFilesInPackage(pkgA, sectPriv);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {file1}));

		/* now, we one file in pkgA/priv, we have some files outside the other packages */
		results = pkgMgr.getFilesOutsidePackage(pkgA);
		assertEquals(1, results.size());
		results = pkgMgr.getFilesOutsidePackage(pkgA, sectPub);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {rootPathId, file1}));
		results = pkgMgr.getFilesOutsidePackage(pkgA, sectPriv);
		assertEquals(1, results.size());
		
		/* now add another to pkgA/priv and check again */
		int file2 = fileMgr.addFile("/myfile2");
		pkgMgr.setFilePackage(file2, pkgA, sectPriv);
		results = pkgMgr.getFilesInPackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {file1, file2}));
		results = pkgMgr.getFilesInPackage(pkgA, sectPub);
		assertEquals(0, results.size());
		results = pkgMgr.getFilesInPackage(pkgA, sectPriv);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {file1, file2}));
		
		/* now, we two files, we have some more files outside */
		results = pkgMgr.getFilesOutsidePackage(pkgA);
		assertEquals(1, results.size());
		results = pkgMgr.getFilesOutsidePackage(pkgA, sectPub);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {rootPathId, file1, file2}));		
		results = pkgMgr.getFilesOutsidePackage(pkgA, sectPriv);
		assertEquals(1, results.size());
		
		/* finally, add one to pkgA/pub and check again */
		int file3 = fileMgr.addFile("/myfile3");
		pkgMgr.setFilePackage(file3, pkgA, sectPub);
		results = pkgMgr.getFilesInPackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {file1, file2, file3}));
		results = pkgMgr.getFilesInPackage(pkgA, sectPub);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {file3}));
		results = pkgMgr.getFilesInPackage(pkgA, sectPriv);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {file1, file2}));		
		results = pkgMgr.getFilesOutsidePackage(pkgA);
		assertEquals(1, results.size());
		results = pkgMgr.getFilesOutsidePackage(pkgA, sectPub);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {rootPathId, file1, file2}));		
		results = pkgMgr.getFilesOutsidePackage(pkgA, sectPriv);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {rootPathId, file3}));
		
		/* move file1 back into None */
		pkgMgr.setFilePackage(file1, pkgNone, sectPriv);
		results = pkgMgr.getFilesInPackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {file2, file3}));
		results = pkgMgr.getFilesInPackage(pkgA, sectPub);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {file3}));
		results = pkgMgr.getFilesInPackage(pkgA, sectPriv);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {file2}));
		
		/* now we have a file outside of pkgA */
		results = pkgMgr.getFilesOutsidePackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {rootPathId, file1}));		
		results = pkgMgr.getFilesOutsidePackage(pkgA, sectPub);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {rootPathId, file1, file2}));		
		results = pkgMgr.getFilesOutsidePackage(pkgA, sectPriv);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {rootPathId, file1, file3}));		
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
		assertEquals(ErrorCode.OK, pkgMgr.setFilePackage(f1path, pkgFooId, IPackageMgr.SCOPE_PUBLIC));
		assertEquals(ErrorCode.OK, pkgMgr.setFilePackage(f2path, pkgFooId, IPackageMgr.SCOPE_PRIVATE));
		assertEquals(ErrorCode.OK, pkgMgr.setFilePackage(f4path, pkgFooId, IPackageMgr.SCOPE_PRIVATE));
		assertEquals(ErrorCode.OK, pkgMgr.setFilePackage(f5path, pkgFooId, IPackageMgr.SCOPE_PRIVATE));

		/* test @foo/public membership */
		FileSet fs = pkgMgr.getFilesInPackage("foo/public");
		assertEquals(1, fs.size());
		assertTrue(fs.isMember(f1path));

		/* test @foo/private membership */
		fs = pkgMgr.getFilesInPackage("foo/private");
		assertEquals(3, fs.size());
		assertTrue(fs.isMember(f2path));
		assertTrue(fs.isMember(f4path));
		assertTrue(fs.isMember(f5path));

		/* test @foo membership */
		fs = pkgMgr.getFilesInPackage("foo");
		assertEquals(4, fs.size());
		assertTrue(fs.isMember(f1path));
		assertTrue(fs.isMember(f2path));
		assertTrue(fs.isMember(f4path));
		assertTrue(fs.isMember(f5path));

		/* 
		 * Test ^@foo/public membership - will always include "/" and
		 * have a bunch of directories too.
		 */
		fs = pkgMgr.getFilesOutsidePackage("foo/public");
		assertEquals(14, fs.size());
		assertTrue(fs.isMember(f2path));
		assertTrue(fs.isMember(f3path));
		assertTrue(fs.isMember(f4path));
		assertTrue(fs.isMember(f5path));

		/* test ^@foo/private membership - which includes directories*/
		fs = pkgMgr.getFilesOutsidePackage("foo/private");
		assertEquals(12, fs.size());
		assertTrue(fs.isMember(f1path));
		assertTrue(fs.isMember(f3path));

		/* test ^@foo membership - which includes directories */
		fs = pkgMgr.getFilesOutsidePackage("foo");
		assertEquals(11, fs.size());
		assertTrue(fs.isMember(f3path));
		
		/* test bad names */
		assertNull(pkgMgr.getFilesInPackage("foo/badsect"));
		assertNull(pkgMgr.getFilesOutsidePackage("pkg"));
		assertNull(pkgMgr.getFilesOutsidePackage("foo/badsect"));
		assertNull(pkgMgr.getFilesOutsidePackage("foo/"));
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
		int action1 = actionMgr.addAction(0, 0, "action1");
		int action2 = actionMgr.addAction(0, 0, "action2");
		int action3 = actionMgr.addAction(0, 0, "action3");
		
		/* create a couple of new packages */
		int pkgA = pkgMgr.addPackage("PkgA");
		int pkgB = pkgMgr.addPackage("PkgB");
		int pkgNone = pkgMgr.getPackageId("None");
		
		/* by default, all actions are in "None" */
		assertEquals(pkgNone, pkgMgr.getActionPackage(action1));
		assertEquals(pkgNone, pkgMgr.getActionPackage(action2));
		assertEquals(pkgNone, pkgMgr.getActionPackage(action3));
		
		/* add an action to PkgA and check the actions */
		pkgMgr.setActionPackage(action1, pkgA);
		assertEquals(pkgA, pkgMgr.getActionPackage(action1));
		assertEquals(pkgNone, pkgMgr.getActionPackage(action2));
		assertEquals(pkgNone, pkgMgr.getActionPackage(action3));
		
		/* add a different action to PkgB and check the actions */
		pkgMgr.setActionPackage(action2, pkgB);
		assertEquals(pkgA, pkgMgr.getActionPackage(action1));
		assertEquals(pkgB, pkgMgr.getActionPackage(action2));
		assertEquals(pkgNone, pkgMgr.getActionPackage(action3));
		
		/* revert one of the actions back to None, and check the actions */
		pkgMgr.setActionPackage(action1, pkgNone);
		assertEquals(pkgNone, pkgMgr.getActionPackage(action1));
		assertEquals(pkgB, pkgMgr.getActionPackage(action2));
		assertEquals(pkgNone, pkgMgr.getActionPackage(action3));
		
		/* check an invalid action - should return ErrorCode.NOT_FOUND */
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getActionPackage(1000));		
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
		int action1 = actionMgr.addAction(0, 0, "action1");
		int action2 = actionMgr.addAction(0, 0, "action2");
		int action3 = actionMgr.addAction(0, 0, "action3");
		
		/* create a couple of new packages */
		int pkgA = pkgMgr.addPackage("PkgA");
		int pkgB = pkgMgr.addPackage("PkgB");
		
		/* initially, pkgA is empty */
		ActionSet results = pkgMgr.getActionsInPackage(pkgA);
		assertEquals(0, results.size());
		results = pkgMgr.getActionsInPackage("PkgA");
		assertEquals(0, results.size());
		results = pkgMgr.getActionsOutsidePackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action2, action3}));
		results = pkgMgr.getActionsOutsidePackage("PkgA");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action2, action3}));		
		
		/* add an action to pkgA */
		pkgMgr.setActionPackage(action1, pkgA);
		results = pkgMgr.getActionsInPackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1}));
		results = pkgMgr.getActionsInPackage("PkgA");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1}));
		results = pkgMgr.getActionsOutsidePackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action2, action3}));
		results = pkgMgr.getActionsOutsidePackage("PkgA");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action2, action3}));

		/* add another action to pkgA */
		pkgMgr.setActionPackage(action3, pkgA);
		results = pkgMgr.getActionsInPackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action3}));
		results = pkgMgr.getActionsInPackage("PkgA");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action3}));
		results = pkgMgr.getActionsOutsidePackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action2}));
		results = pkgMgr.getActionsOutsidePackage("PkgA");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action2}));

		/* Add a third */
		pkgMgr.setActionPackage(action2, pkgA);
		results = pkgMgr.getActionsInPackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action2, action3}));
		results = pkgMgr.getActionsInPackage("PkgA");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action2, action3}));
		results = pkgMgr.getActionsOutsidePackage(pkgA);
		assertEquals(0, results.size());
		results = pkgMgr.getActionsOutsidePackage("PkgA");
		assertEquals(0, results.size());

		/* move the second action into pkgB */
		pkgMgr.setActionPackage(action2, pkgB);
		results = pkgMgr.getActionsInPackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action3}));
		results = pkgMgr.getActionsInPackage("PkgA");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action3}));
		results = pkgMgr.getActionsInPackage(pkgB);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action2}));
		results = pkgMgr.getActionsInPackage("PkgB");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action2}));
		results = pkgMgr.getActionsOutsidePackage(pkgA);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action2}));
		results = pkgMgr.getActionsOutsidePackage("PkgA");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action2}));
		results = pkgMgr.getActionsOutsidePackage(pkgB);
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action3}));
		results = pkgMgr.getActionsOutsidePackage("PkgB");
		assertTrue(CommonTestUtils.treeSetEqual(results, new Integer[] {action1, action3}));
		
		/* try some bad package names */
		assertNull(pkgMgr.getActionsInPackage("badname"));
		assertNull(pkgMgr.getActionsInPackage("PkgA/private"));
		assertNull(pkgMgr.getActionsInPackage(""));		
	}

	/*-------------------------------------------------------------------------------------*/
}
