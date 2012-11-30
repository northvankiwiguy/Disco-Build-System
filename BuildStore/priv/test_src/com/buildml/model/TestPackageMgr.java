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
		
		/* validate that these are packages (not folders) */
		assertFalse(pkgMgr.isFolder(c1));
		assertFalse(pkgMgr.isFolder(c2));
		assertFalse(pkgMgr.isFolder(c3));
		
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
		
		/* try to add the <import> package, which exists by default */
		int cImport = pkgMgr.addPackage("<import>");
		assertEquals(ErrorCode.ALREADY_USED, cImport);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IPackageMgr#addFolder(java.lang.String)}.
	 */
	@Test
	public void testAddFolder() {
	
		/* We can't re-add the root folder */
		assertEquals(ErrorCode.ALREADY_USED, pkgMgr.addFolder("Root"));
		
		/* We can't add a folder with the name "<import>" (pre-defined) */
		assertEquals(ErrorCode.ALREADY_USED, pkgMgr.addFolder("<import>"));
		
		/* can't add a folder with the same name as an existing package. */
		pkgMgr.addPackage("MyPackage");
		assertEquals(ErrorCode.ALREADY_USED, pkgMgr.addFolder("MyPackage"));
		
		/* Successfully add several folders, making sure they get unique IDs */
		int folderA = pkgMgr.addFolder("FolderA");
		int folderB = pkgMgr.addFolder("FolderB");
		int folderC = pkgMgr.addFolder("FolderC");
		assertTrue(folderA > 0);
		assertTrue(folderB > 0);
		assertTrue(folderC > 0);
		assertNotSame(folderA, folderB);
		assertNotSame(folderB, folderC);
		assertNotSame(folderA, folderC);
		
		/* validate that these are folders (not packages) */
		assertTrue(pkgMgr.isFolder(folderA));
		assertTrue(pkgMgr.isFolder(folderB));
		assertTrue(pkgMgr.isFolder(folderC));
		
		/* We can't add the same folder twice */
		assertEquals(ErrorCode.ALREADY_USED, pkgMgr.addFolder("FolderB"));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IPackageMgr#getName(int)} and
	 * {@link com.buildml.model.IPackageMgr#setName(int, String)}.
	 */
	@Test
	public void testGetSetName() {
		
		/* add package/folder names, then make sure their names are correct */
		int c1 = pkgMgr.addPackage("PkgA");
		assertEquals("PkgA", pkgMgr.getName(c1));
		int c2 = pkgMgr.addPackage("my-other-package");
		assertEquals("my-other-package", pkgMgr.getName(c2));
		int c3 = pkgMgr.addPackage("and_a_3rd");
		assertEquals("and_a_3rd", pkgMgr.getName(c3));
		int f1 = pkgMgr.addFolder("MyFolder");
		assertEquals("MyFolder", pkgMgr.getName(f1));
		
		/* try to fetch names for invalid package IDs */
		assertEquals(null, pkgMgr.getName(1000));		
		assertEquals(null, pkgMgr.getName(-1));
		
		/* test for the default "<import>" package */
		assertEquals("<import>", pkgMgr.getName(pkgMgr.getImportPackage()));
		
		/* now change some names are check them again */
		assertEquals(ErrorCode.OK, pkgMgr.setName(c1, "my_new_package_name"));	
		assertEquals("my_new_package_name", pkgMgr.getName(c1));
		assertEquals(ErrorCode.OK, pkgMgr.setName(f1, "RevisedFolderName"));	
		assertEquals("RevisedFolderName", pkgMgr.getName(f1));
		
		/* try with names that are already in use */
		assertEquals(ErrorCode.ALREADY_USED, pkgMgr.setName(c1, "and_a_3rd"));
		
		/* try with invalid names */
		assertEquals(ErrorCode.INVALID_NAME, pkgMgr.setName(c1, "+"));
		assertEquals(ErrorCode.INVALID_NAME, pkgMgr.setName(c1, "f"));
		assertEquals(ErrorCode.INVALID_NAME, pkgMgr.setName(c1, "long name with spaces"));
		
		/* try to set the name of an invalid ID */
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.setName(1234, "valid_name"));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IPackageMgr#getId(String)}.
	 */
	@Test
	public void testGetId() {
		
		/* get the ID for the "<import>" package */
		assertEquals(pkgMgr.getImportPackage(), pkgMgr.getId("<import>"));

		/* fetch package names that haven't been added - should be NOT_FOUND */
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getId("foo"));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getId("my_package"));
		
		/* add package names, then make sure their IDs are correct */
		int c1 = pkgMgr.addPackage("my_pkg_A");
		int c2 = pkgMgr.addPackage("ourpackage");
		int f1 = pkgMgr.addFolder("MyFolder");
		assertEquals(c1, pkgMgr.getId("my_pkg_A"));
		assertEquals(f1, pkgMgr.getId("MyFolder"));
		assertEquals(c2, pkgMgr.getId("ourpackage"));
		
		/* add duplicate names, and make sure the old ID is returned */
		int c4 = pkgMgr.addPackage("ourpackage");
		assertEquals(ErrorCode.ALREADY_USED, c4);
		int c5 = pkgMgr.addPackage("MyFolder");
		assertEquals(ErrorCode.ALREADY_USED, c5);
		assertEquals(f1, pkgMgr.getId("MyFolder"));
		assertEquals(c2, pkgMgr.getId("ourpackage"));
				
		/* Names that haven't been added will return NOT_FOUND */
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getId("invalid"));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getId("missing"));
		
		/* try to fetch IDs for invalid package names - returns NOT_FOUND */
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getId("miss*ing"));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getId("invalid name"));		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IPackageMgr#remove(int)}.
	 */
	@Test
	public void testRemove() {
		
		IFileMgr fileMgr = bs.getFileMgr();
		IActionMgr actionMgr = bs.getActionMgr();
		
		/* try to remove package ID that haven't been added */
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.remove(1234));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.remove(5678));
		
		/* try to remove the root folder or import package - should fail */
		assertEquals(ErrorCode.CANT_REMOVE, pkgMgr.remove(pkgMgr.getRootFolder()));
		assertEquals(ErrorCode.CANT_REMOVE, pkgMgr.remove(pkgMgr.getImportPackage()));

		/* add package names, then remove them */
		int pkgAId = pkgMgr.addPackage("PkgA"); 
		int myPackageId = pkgMgr.addPackage("my_package");
		assertTrue(pkgAId > 0);
		assertTrue(myPackageId > 0);	
		assertEquals(ErrorCode.OK, pkgMgr.remove(pkgAId));
		assertEquals(ErrorCode.OK, pkgMgr.remove(myPackageId));
		
		/* try to remove the same names again - should fail */
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.remove(pkgAId));
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.remove(myPackageId));
		
		/* add the same package names again - should work */
		assertTrue(pkgMgr.addPackage("PkgA") > 0);
		assertTrue(pkgMgr.addPackage("my_package") > 0);	
		
		/* assign a package to files, then try to remove the name */
		int pkgA = pkgMgr.getId("PkgA");
		int file1 = fileMgr.addFile("/aardvark/bunny");
		pkgMgr.setFilePackage(file1, pkgA, IPackageMgr.SCOPE_PRIVATE);
		assertEquals(ErrorCode.CANT_REMOVE, pkgMgr.remove(pkgA));
		
		/* remove the package from the file, then try again to remove the package name */
		int pkgImport = pkgMgr.getId("<import>");
		pkgMgr.setFilePackage(file1, pkgImport, IPackageMgr.SCOPE_PRIVATE);
		assertEquals(ErrorCode.OK, pkgMgr.remove(pkgA));
		
		/* assign them to actions, then try to remove the name */
		int my_pkg = pkgMgr.getId("my_package");
		int action1 = actionMgr.addAction(0, 0, "action1");
		pkgMgr.setActionPackage(action1, my_pkg);
		assertEquals(ErrorCode.CANT_REMOVE, pkgMgr.remove(my_pkg));
		
		/* remove them from actions, then try again to remove the package name */
		pkgMgr.setActionPackage(action1, pkgImport);
		assertEquals(ErrorCode.OK, pkgMgr.remove(my_pkg));
		
		/* test removal of an empty folder */
		int f1 = pkgMgr.addFolder("MyEmptyFolder");
		assertEquals(ErrorCode.OK, pkgMgr.remove(f1));
		
		/* test removal of a folder that contains packages */
		int f2 = pkgMgr.addFolder("MyNonEmptyFolder");
		int f2p1 = pkgMgr.addPackage("child_package");
		assertEquals(ErrorCode.OK, pkgMgr.setParent(f2p1, f2));
		assertEquals(ErrorCode.CANT_REMOVE, pkgMgr.remove(f2));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IPackageMgr#getPackages()}.
	 */
	@Test
	public void testGetPackages() {
		
		/* fetch the list of packages, before adding any. */
		String results[] = pkgMgr.getPackages();
		assertArrayEquals(new String[] {"<import>"}, results);
		
		/* add some packages, then check the list */
		pkgMgr.addPackage("my_pkg1");
		pkgMgr.addPackage("my_pkg2");
		pkgMgr.addFolder("ignored");
		results = pkgMgr.getPackages();
		assertArrayEquals(new String[] {"<import>", "my_pkg1", "my_pkg2"}, results);		
		
		/* add some more, then check again */
		int linuxId = pkgMgr.addPackage("Linux");
		int freebsdId = pkgMgr.addPackage("freeBSD");
		results = pkgMgr.getPackages();
		assertArrayEquals(new String[] {"<import>", "freeBSD", "Linux", "my_pkg1", 
				"my_pkg2"}, results);		
		
		/* remove some packages, then check the list again */
		pkgMgr.remove(linuxId);
		pkgMgr.remove(freebsdId);
		results = pkgMgr.getPackages();
		assertArrayEquals(new String[] {"<import>", "my_pkg1", "my_pkg2" }, results);		
		
		/* add some names back, and re-check the list */
		pkgMgr.addPackage("Linux");
		pkgMgr.addPackage("MacOS");
		results = pkgMgr.getPackages();
		assertArrayEquals(new String[] {"<import>", "Linux", "MacOS", "my_pkg1", "my_pkg2"}, 
				results);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.IPackageMgr#getParent(int)} and
	 * {@link com.buildml.model.IPackageMgr#setParent(int, int)}
	 */
	@Test
	public void testGetSetParent() {
		
		/* add some packages are folders - add them in non-alphabetical order */
		int fRoot = pkgMgr.getRootFolder();
		int pImport = pkgMgr.getImportPackage();
		int p1 = pkgMgr.addPackage("Package_1");
		int p3 = pkgMgr.addPackage("Package_3");
		int p2 = pkgMgr.addPackage("Package_2");
		int f1 = pkgMgr.addFolder("Folder_1");
		int f3 = pkgMgr.addFolder("Folder_3");
		int f2 = pkgMgr.addFolder("Folder_2");
	
		/* first, check that invalid IDs are captured */
		assertEquals(ErrorCode.NOT_FOUND, pkgMgr.getParent(1234));
		
		/* test that newly added packages are folders are parented under "Root" */
		assertEquals(fRoot, pkgMgr.getParent(p1));
		assertEquals(fRoot, pkgMgr.getParent(p2));
		assertEquals(fRoot, pkgMgr.getParent(p3));
		assertEquals(fRoot, pkgMgr.getParent(f1));
		assertEquals(fRoot, pkgMgr.getParent(f2));
		assertEquals(fRoot, pkgMgr.getParent(f3));
		assertArrayEquals(new Integer[] { f1, f2, f3, pImport, p1, p2, p3 },
						  pkgMgr.getFolderChildren(fRoot));
		assertEquals(0, pkgMgr.getFolderChildren(f1).length);
		assertEquals(0, pkgMgr.getFolderChildren(f2).length);
		assertEquals(0, pkgMgr.getFolderChildren(f3).length);

		/* Try to move a package underneath a package (not a folder) */
		assertEquals(ErrorCode.NOT_A_DIRECTORY, pkgMgr.setParent(p1, p2));
		
		/* Try to move a package underneath an invalid ID */
		assertEquals(ErrorCode.BAD_VALUE, pkgMgr.setParent(p1, 1234));
		assertEquals(ErrorCode.BAD_VALUE, pkgMgr.setParent(1234, f1));

		/* Move package p1 underneath sub-folder f1 and verify structure */
		assertEquals(ErrorCode.OK, pkgMgr.setParent(p1, f1));
		assertArrayEquals(new Integer[] { f1, f2, f3, pImport, p2, p3 },
				  pkgMgr.getFolderChildren(fRoot));
		assertArrayEquals(new Integer[] { p1 },
				  pkgMgr.getFolderChildren(f1));
		
		/* Move the package to another sub-folder, then re-verify */
		assertEquals(ErrorCode.OK, pkgMgr.setParent(p1, f2));
		assertArrayEquals(new Integer[] { f1, f2, f3, pImport, p2, p3 },
				  pkgMgr.getFolderChildren(fRoot));
		assertArrayEquals(new Integer[] { },
				  pkgMgr.getFolderChildren(f1));
		assertArrayEquals(new Integer[] { p1 },
				  pkgMgr.getFolderChildren(f2));
		
		/* Move a folder underneath a sub-folder and verify structure */
		assertEquals(ErrorCode.OK, pkgMgr.setParent(f3, f2));
		assertArrayEquals(new Integer[] { f1, f2, pImport, p2, p3 },
				  pkgMgr.getFolderChildren(fRoot));
		assertArrayEquals(new Integer[] { f3, p1 },
				  pkgMgr.getFolderChildren(f2));
		
		/* Try to move the <import> package to a sub-folder - fails */
		assertEquals(ErrorCode.BAD_PATH, pkgMgr.setParent(pImport, f2));
		
		/* Try to move the root folder - fails */
		assertEquals(ErrorCode.BAD_PATH, pkgMgr.setParent(fRoot, f2));

		/* 
		 * Try to create a cycle in the folder tree (f2 is currently the
		 * parent of f3, so setting f2's parent to be f3 is invalid since
		 * it would create a cycle).
		 */
		assertEquals(ErrorCode.BAD_PATH, pkgMgr.setParent(f2, f3));		
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
		int pkgImport = pkgMgr.getImportPackage();

		/* by default, all files are in <import>/None */
		Integer results[] = pkgMgr.getFilePackage(path1);
		assertEquals(pkgImport, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_NONE, results[1].intValue());
		results = pkgMgr.getFilePackage(path2);
		assertEquals(pkgImport, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_NONE, results[1].intValue());
		results = pkgMgr.getFilePackage(path3);
		assertEquals(pkgImport, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_NONE, results[1].intValue());

		/* set one of the files into PkgA/public */
		assertEquals(ErrorCode.OK, pkgMgr.setFilePackage(path1, pkgA, IPackageMgr.SCOPE_PUBLIC));
		results = pkgMgr.getFilePackage(path1);
		assertEquals(pkgA, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_PUBLIC, results[1].intValue());
		results = pkgMgr.getFilePackage(path2);
		assertEquals(pkgImport, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_NONE, results[1].intValue());
		results = pkgMgr.getFilePackage(path3);
		assertEquals(pkgImport, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_NONE, results[1].intValue());
		
		/* set another file to another package */
		assertEquals(ErrorCode.OK, pkgMgr.setFilePackage(path3, pkgB, IPackageMgr.SCOPE_PRIVATE));
		results = pkgMgr.getFilePackage(path1);
		assertEquals(pkgA, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_PUBLIC, results[1].intValue());
		results = pkgMgr.getFilePackage(path2);
		assertEquals(pkgImport, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_NONE, results[1].intValue());
		results = pkgMgr.getFilePackage(path3);
		assertEquals(pkgB, results[0].intValue());
		assertEquals(IPackageMgr.SCOPE_PRIVATE, results[1].intValue());
		
		/* set a file's package back to <import>/None */
		assertEquals(ErrorCode.OK, pkgMgr.setFilePackage(path1, pkgImport, IPackageMgr.SCOPE_NONE));
		results = pkgMgr.getFilePackage(path1);
		assertEquals(pkgImport, results[0].intValue());
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
		int pkgImport = pkgMgr.getImportPackage();
		
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
		
		/* move file1 back into <import> */
		pkgMgr.setFilePackage(file1, pkgImport, sectPriv);
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
		int pkgImport = pkgMgr.getImportPackage();
		
		/* by default, all actions are in "<import>" */
		assertEquals(pkgImport, pkgMgr.getActionPackage(action1));
		assertEquals(pkgImport, pkgMgr.getActionPackage(action2));
		assertEquals(pkgImport, pkgMgr.getActionPackage(action3));
		
		/* add an action to PkgA and check the actions */
		pkgMgr.setActionPackage(action1, pkgA);
		assertEquals(pkgA, pkgMgr.getActionPackage(action1));
		assertEquals(pkgImport, pkgMgr.getActionPackage(action2));
		assertEquals(pkgImport, pkgMgr.getActionPackage(action3));
		
		/* add a different action to PkgB and check the actions */
		pkgMgr.setActionPackage(action2, pkgB);
		assertEquals(pkgA, pkgMgr.getActionPackage(action1));
		assertEquals(pkgB, pkgMgr.getActionPackage(action2));
		assertEquals(pkgImport, pkgMgr.getActionPackage(action3));
		
		/* revert one of the actions back to <import>, and check the actions */
		pkgMgr.setActionPackage(action1, pkgImport);
		assertEquals(pkgImport, pkgMgr.getActionPackage(action1));
		assertEquals(pkgB, pkgMgr.getActionPackage(action2));
		assertEquals(pkgImport, pkgMgr.getActionPackage(action3));
		
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
