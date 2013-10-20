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
 * Unit tests for the Packages class.
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestPackageMgr {

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

		/* try to add the Main package, which exists by default */
		int cMain = pkgMgr.addPackage("<import>");
		assertEquals(ErrorCode.ALREADY_USED, cMain);
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
		assertEquals(ErrorCode.CANT_REMOVE, pkgMgr.remove(pkgMgr.getId("Main")));

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
		pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE, file1, pkgA, IPackageMemberMgr.SCOPE_PRIVATE);
		assertEquals(ErrorCode.CANT_REMOVE, pkgMgr.remove(pkgA));
		
		/* remove the package from the file, then try again to remove the package name */
		int pkgImport = pkgMgr.getId("<import>");
		pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE, file1, pkgImport, IPackageMemberMgr.SCOPE_PRIVATE);
		assertEquals(ErrorCode.OK, pkgMgr.remove(pkgA));
		
		/* assign them to actions, then try to remove the name */
		int my_pkg = pkgMgr.getId("my_package");
		int action1 = actionMgr.addShellCommandAction(0, 0, "action1");
		pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_ACTION, action1, my_pkg, IPackageMemberMgr.SCOPE_NONE);
		assertEquals(ErrorCode.CANT_REMOVE, pkgMgr.remove(my_pkg));
		
		/* remove them from actions, then try again to remove the package name */
		pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_ACTION, action1, pkgImport, IPackageMemberMgr.SCOPE_NONE);
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
		assertArrayEquals(new String[] {"<import>", "Main"}, results);
		
		/* add some packages, then check the list */
		pkgMgr.addPackage("my_pkg1");
		pkgMgr.addPackage("my_pkg2");
		pkgMgr.addFolder("ignored");
		results = pkgMgr.getPackages();
		assertArrayEquals(new String[] {"<import>", "Main", "my_pkg1", "my_pkg2"}, results);		
		
		/* add some more, then check again */
		int linuxId = pkgMgr.addPackage("Linux");
		int freebsdId = pkgMgr.addPackage("freeBSD");
		results = pkgMgr.getPackages();
		assertArrayEquals(new String[] {"<import>", "freeBSD", "Linux", "Main", "my_pkg1", 
				"my_pkg2"}, results);		
		
		/* remove some packages, then check the list again */
		pkgMgr.remove(linuxId);
		pkgMgr.remove(freebsdId);
		results = pkgMgr.getPackages();
		assertArrayEquals(new String[] {"<import>", "Main", "my_pkg1", "my_pkg2" }, results);		
		
		/* add some names back, and re-check the list */
		pkgMgr.addPackage("Linux");
		pkgMgr.addPackage("MacOS");
		results = pkgMgr.getPackages();
		assertArrayEquals(new String[] {"<import>", "Linux", "MacOS", "Main", "my_pkg1", "my_pkg2"}, 
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
		int pMain = pkgMgr.getId("Main");
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
		assertArrayEquals(new Integer[] { f1, f2, f3, pImport, pMain, p1, p2, p3 },
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
		assertArrayEquals(new Integer[] { f1, f2, f3, pImport, pMain, p2, p3 },
				  pkgMgr.getFolderChildren(fRoot));
		assertArrayEquals(new Integer[] { p1 },
				  pkgMgr.getFolderChildren(f1));
		
		/* Move the package to another sub-folder, then re-verify */
		assertEquals(ErrorCode.OK, pkgMgr.setParent(p1, f2));
		assertArrayEquals(new Integer[] { f1, f2, f3, pImport, pMain, p2, p3 },
				  pkgMgr.getFolderChildren(fRoot));
		assertArrayEquals(new Integer[] { },
				  pkgMgr.getFolderChildren(f1));
		assertArrayEquals(new Integer[] { p1 },
				  pkgMgr.getFolderChildren(f2));
		
		/* Move a folder underneath a sub-folder and verify structure */
		assertEquals(ErrorCode.OK, pkgMgr.setParent(f3, f2));
		assertArrayEquals(new Integer[] { f1, f2, pImport, pMain, p2, p3 },
				  pkgMgr.getFolderChildren(fRoot));
		assertArrayEquals(new Integer[] { f3, p1 },
				  pkgMgr.getFolderChildren(f2));
		
		/* Try to move the <import> package to a sub-folder - fails */
		assertEquals(ErrorCode.BAD_PATH, pkgMgr.setParent(pImport, f2));

		/* Try to move the Main package to a sub-folder - fails */
		assertEquals(ErrorCode.BAD_PATH, pkgMgr.setParent(pkgMgr.getId("Main"), f2));

		/* Try to move the root folder - fails */
		assertEquals(ErrorCode.BAD_PATH, pkgMgr.setParent(fRoot, f2));

		/* 
		 * Try to create a cycle in the folder tree (f2 is currently the
		 * parent of f3, so setting f2's parent to be f3 is invalid since
		 * it would create a cycle).
		 */
		assertEquals(ErrorCode.BAD_PATH, pkgMgr.setParent(f2, f3));
		
		/* try to move a folder into itself */
		assertEquals(ErrorCode.BAD_PATH, pkgMgr.setParent(f2, f2));
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
				TestPackageMgr.this.notifyPkgValue = pkgId;
				TestPackageMgr.this.notifyHowValue = how;
			}
		};
		pkgMgr.addListener(pkgListener);

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

		/* remove the pkgListener, and change the package again - will trigger */
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
