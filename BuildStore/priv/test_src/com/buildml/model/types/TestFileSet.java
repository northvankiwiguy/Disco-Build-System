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

package com.buildml.model.types;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import com.buildml.model.CommonTestUtils;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageRootMgr;
import com.buildml.model.types.FileSet;
import com.buildml.utils.errors.ErrorCode;

/**
 * Test methods for validating the FileSet class.
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestFileSet {
	
	/** Our test FileSet object */
	private FileSet fs;
	
	/** Our test BuildStore object */
	private IBuildStore bs;
	
	/** Our test manager objects */
	private IFileMgr fileMgr;
	private IPackageMgr pkgMgr;
	private IPackageMemberMgr pkgMemberMgr;
	private IPackageRootMgr pkgRootMgr;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper method for checking that IDs *are* in a set.
	 * @param fs The set to check against.
	 * @param ids The array of IDs to check. 
	 */
	private void checkMembers(FileSet fs, Integer[] ids) {
		for (Integer id : ids) {
			assertTrue(fs.isMember(id));
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper method for checking that IDs *are not* in a set.
	 * @param fs The set to check against.
	 * @param ids The array of IDs to check. 
	 */
	private void checkNotMembers(FileSet fs, Integer[] ids) {
		for (Integer id : ids) {
			assertFalse(fs.isMember(id));
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Setup() method, run before each test case is executed. Creates a new BuildStore
	 * and a new empty FileSet.
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		bs = CommonTestUtils.getEmptyBuildStore();
		fileMgr = bs.getFileMgr();
		pkgMgr = bs.getPackageMgr();
		pkgMemberMgr = bs.getPackageMemberMgr();
		pkgRootMgr = bs.getPackageRootMgr();
		fs = new FileSet(fileMgr);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.types.FileSet#isMember(int)}.
	 */
	@Test
	public void testIsMember() {
		
		/* add some elements */
		fs.add(134);
		fs.add(2559);
		fs.add(2560);
		fs.add(23);
		
		/* check that the FileSet contains those elements */
		assertTrue(fs.isMember(134));
		assertTrue(fs.isMember(2559));
		assertTrue(fs.isMember(2560));
		assertTrue(fs.isMember(23));
		
		/* but doesn't contain elements we didn't add */
		assertFalse(fs.isMember(34));
		assertFalse(fs.isMember(1));
		assertFalse(fs.isMember(2561));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.types.FileSet#remove(int)}.
	 */
	@Test
	public void testRemove() {
		
		/* add some elements */
		fs.add(34);
		fs.add(34);
		fs.add(9275);
		fs.add(3643);
		
		/* check they're present */
		assertTrue(fs.isMember(34));
		assertTrue(fs.isMember(9275));
		assertTrue(fs.isMember(3643));
		
		/* check that other entries are not present */
		assertFalse(fs.isMember(33));
		assertFalse(fs.isMember(35));
		assertFalse(fs.isMember(2049));
		assertFalse(fs.isMember(6145));
		assertFalse(fs.isMember(123450));
		
		/* remove one of them, and check membership */
		fs.remove(9275);
		assertTrue(fs.isMember(34));
		assertFalse(fs.isMember(9275));
		assertTrue(fs.isMember(3643));

		/* remove another */
		fs.remove(34);
		assertFalse(fs.isMember(34));
		assertFalse(fs.isMember(9275));
		assertTrue(fs.isMember(3643));

		/* and another - should now be empty again */
		fs.remove(3643);
		assertFalse(fs.isMember(34));
		assertFalse(fs.isMember(9275));
		assertFalse(fs.isMember(3643));
		
		/* remove elements that were never added */
		fs.remove(8192);
		fs.remove(16384);
		fs.remove(9276);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.types.FileSet#iterator()}.
	 */
	@Test
	public void testIterator() {
		
		/* add a bunch of elements */
		fs.add(134);
		fs.add(256);
		fs.add(23);
		fs.add(34);
		fs.add(9275);
		fs.add(3643);
		fs.add(16777215);

		/* check that the iterator returns all the members (not in any particular order) */
		ArrayList<Integer> returnedList = new ArrayList<Integer>();
		for (Integer pathId : fs) {
			returnedList.add(pathId);
		}
		assertTrue(CommonTestUtils.sortedArraysEqual(
				new Integer[] {23, 34, 134, 256, 3643, 9275, 16777215}, 
				returnedList.toArray(new Integer[0])));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.types.FileSet#populateWithParents()}.
	 */
	@Test
	public void testPopulateWithParents() {
		
		/* create a bunch of files */
		int f1path = fileMgr.addFile("/a/b/c/d/e/f1.c");
		int f2path = fileMgr.addFile("/a/b/c/d/e/f2.c");
		int f3path = fileMgr.addFile("/a/b/c/d/g/f3.c");
		int f4path = fileMgr.addFile("/b/c/d/f4.c");
		int f5path = fileMgr.addFile("/b/c/d/f5.c");
		
		/* this one won't be added, neither will its parents */
		fileMgr.addFile("/c/d/e/f6.c");

		/* add them to the file set */ 
		fs.add(f1path);		
		fs.add(f2path);
		fs.add(f3path);
		fs.add(f4path);
		fs.add(f5path);

		/* check that they're added */
		assertTrue(fs.isMember(f1path));
		assertTrue(fs.isMember(f2path));
		assertTrue(fs.isMember(f3path));
		assertTrue(fs.isMember(f4path));
		assertTrue(fs.isMember(f5path));

		/* check that the parent paths aren't added yet */
		int parRoot = fileMgr.getPath("/");
		int parA = fileMgr.getPath("/a");
		int parAB = fileMgr.getPath("/a/b");
		int parABC = fileMgr.getPath("/a/b/c");
		int parABCD = fileMgr.getPath("/a/b/c/d");
		int parABCDE = fileMgr.getPath("/a/b/c/d/e");
		int parABCDG = fileMgr.getPath("/a/b/c/d/g");
		int parB = fileMgr.getPath("/b");
		int parBC = fileMgr.getPath("/b/c");
		int parBCD = fileMgr.getPath("/b/c/d");
		assertFalse(fs.isMember(parRoot));
		assertFalse(fs.isMember(parA));
		assertFalse(fs.isMember(parAB));
		assertFalse(fs.isMember(parABC));
		assertFalse(fs.isMember(parABCD));
		assertFalse(fs.isMember(parABCDE));
		assertFalse(fs.isMember(parABCDG));
		assertFalse(fs.isMember(parB));
		assertFalse(fs.isMember(parBC));
		assertFalse(fs.isMember(parBCD));

		/* these will never be added - make sure they're not there */
		int parCDE = fileMgr.getPath("/c/d/e");
		int parCD = fileMgr.getPath("/c/d");
		int parC = fileMgr.getPath("/c");
		assertFalse(fs.isMember(parCDE));
		assertFalse(fs.isMember(parCD));
		assertFalse(fs.isMember(parC));
		
		/* populate the set with parents */
		fs.populateWithParents();
		
		/* now check again */
		assertTrue(fs.isMember(parRoot));
		assertTrue(fs.isMember(parA));
		assertTrue(fs.isMember(parAB));
		assertTrue(fs.isMember(parABC));
		assertTrue(fs.isMember(parABCD));
		assertTrue(fs.isMember(parABCDE));
		assertTrue(fs.isMember(parABCDG));
		assertTrue(fs.isMember(parB));
		assertTrue(fs.isMember(parBC));
		assertTrue(fs.isMember(parBCD));
		
		/* these still shouldn't be added */
		assertFalse(fs.isMember(parCDE));
		assertFalse(fs.isMember(parCD));
		assertFalse(fs.isMember(parC));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.types.FileSet#mergeSet(FileSet)}.
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testMergeFileSet() throws Exception {
		FileSet mainFileSet = new FileSet(fileMgr);
		
		int file1 = fileMgr.addFile("/apple/banana/carrot/donkey.h");
		int file2 = fileMgr.addFile("/apple/banana/carrot/elephant.h");
		int file3 = fileMgr.addFile("/apple/banana/chilly/fish.h");
		int file4 = fileMgr.addFile("/apple/banana/dragonfruit/goat.h");
		int file5 = fileMgr.addFile("/apple/banana/dragonfruit/hamster.h");
		int file6 = fileMgr.addFile("/apple/banana/dragonfruit/iguana.h");

		/* seed mainFileSet with one file */
		mainFileSet.add(file1);

		/* merge the empty FileSet, and make sure mainFileSet is unchanged */
		FileSet fs2 = new FileSet(fileMgr);
		mainFileSet.mergeSet(fs2);
		assertEquals(1, mainFileSet.size());
		
		/* merge in a single file */
		FileSet fs3 = new FileSet(fileMgr);
		fs3.add(file2);
		mainFileSet.mergeSet(fs3);
		assertEquals(2, mainFileSet.size());
		assertTrue(mainFileSet.isMember(file1));
		assertTrue(mainFileSet.isMember(file2));
		
		/* merge in a file that already exists - this shouldn't change anything */
		FileSet fs4 = new FileSet(fileMgr);
		fs4.add(file1);
		mainFileSet.mergeSet(fs4);
		assertEquals(2, mainFileSet.size());
		assertTrue(mainFileSet.isMember(file1));
		assertTrue(mainFileSet.isMember(file2));
		
		/* merge in three new files */
		FileSet fs5 = new FileSet(fileMgr);
		fs5.add(file4);
		fs5.add(file5);
		fs5.add(file6);
		mainFileSet.mergeSet(fs5);
		assertEquals(5, mainFileSet.size());
		assertTrue(mainFileSet.isMember(file1));
		assertTrue(mainFileSet.isMember(file2));
		assertFalse(mainFileSet.isMember(file3));
		assertTrue(mainFileSet.isMember(file4));
		assertTrue(mainFileSet.isMember(file5));
		assertTrue(mainFileSet.isMember(file6));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link 
	 * com.buildml.model.types.FileSet#extractSet(com.buildml.utils.types.IntegerTreeSet)}.
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testExtractFileSet() throws Exception {
		FileSet mainFileSet = new FileSet(fileMgr);
		
		int file1 = fileMgr.addFile("/apple/banana/carrot/donkey.h");
		int file2 = fileMgr.addFile("/apple/banana/carrot/elephant.h");
		int file3 = fileMgr.addFile("/apple/banana/chilly/fish.h");
		int file4 = fileMgr.addFile("/apple/banana/dragonfruit/goat.h");
		int file5 = fileMgr.addFile("/apple/banana/dragonfruit/hamster.h");
		int file6 = fileMgr.addFile("/apple/banana/dragonfruit/iguana.h");

		/* seed mainFileSet with all files */
		mainFileSet.add(file1);
		mainFileSet.add(file2);
		mainFileSet.add(file3);
		mainFileSet.add(file4);
		mainFileSet.add(file5);
		mainFileSet.add(file6);
		assertEquals(6, mainFileSet.size());

		/* remove the empty FileSet, and make sure mainFileSet is unchanged */
		FileSet fs2 = new FileSet(fileMgr);
		mainFileSet.extractSet(fs2);
		assertEquals(6, mainFileSet.size());
		
		/* remove a single file */
		FileSet fs3 = new FileSet(fileMgr);
		fs3.add(file2);
		mainFileSet.extractSet(fs3);
		assertEquals(5, mainFileSet.size());
		assertFalse(mainFileSet.isMember(file2));
		
		/* remove a file that was already removed - this shouldn't change anything */
		FileSet fs4 = new FileSet(fileMgr);
		fs4.add(file2);
		mainFileSet.extractSet(fs4);
		assertEquals(5, mainFileSet.size());
		assertFalse(mainFileSet.isMember(file2));
		
		/* remove three additional files */
		FileSet fs5 = new FileSet(fileMgr);
		fs5.add(file4);
		fs5.add(file5);
		fs5.add(file6);
		mainFileSet.extractSet(fs5);
		assertEquals(2, mainFileSet.size());
		assertTrue(mainFileSet.isMember(file1));
		assertFalse(mainFileSet.isMember(file2));
		assertTrue(mainFileSet.isMember(file3));
		assertFalse(mainFileSet.isMember(file4));
		assertFalse(mainFileSet.isMember(file5));
		assertFalse(mainFileSet.isMember(file6));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.types.FileSet#populateWithPaths(String[])}.
	 * @throws Exception Something bad happened
	 */
	@Test
	public void testPopulateWithPaths() throws Exception {
		
		/* create a bunch of files */
		int f1path = fileMgr.addFile("/a/b/c/d/e/f1.c");
		int f2path = fileMgr.addFile("/a/b/c/d/e/f2.c");
		int f3path = fileMgr.addFile("/a/b/c/d/g/f3.c");
		int f4path = fileMgr.addFile("/b/c/d/f4.c");
		int f5path = fileMgr.addFile("/b/c/d/f5.c");
		int dirRoot = fileMgr.getPath("/");
		int dirA = fileMgr.getPath("/a");
		int dirAB = fileMgr.getPath("/a/b");
		int dirABC = fileMgr.getPath("/a/b/c");
		int dirABCD = fileMgr.getPath("/a/b/c/d");
		int dirABCDG = fileMgr.getPath("/a/b/c/d/g");
		int dirABCDE = fileMgr.getPath("/a/b/c/d/e");
		int dirB = fileMgr.getPath("/b");
		int dirBC = fileMgr.getPath("/b/c");
		int dirBCD = fileMgr.getPath("/b/c/d");
		
		/* add some file roots */
		assertEquals(ErrorCode.OK, pkgRootMgr.setWorkspaceRoot(dirRoot));
		int pkgA = pkgMgr.addPackage("pkgA");
		assertEquals(ErrorCode.OK, 
				     pkgRootMgr.setPackageRoot(pkgA, IPackageRootMgr.SOURCE_ROOT, dirABC));
		assertEquals(ErrorCode.OK, 
			     pkgRootMgr.setPackageRoot(pkgA, IPackageRootMgr.GENERATED_ROOT, dirB));
		
		/* with no arguments, no paths are added */
		FileSet fs0 = new FileSet(fileMgr);
		assertEquals(ErrorCode.OK, fs0.populateWithPaths(new String [] {}));
		assertEquals(0, fs0.size());
		
		/* add all paths by passing in '/' */
		FileSet fs1 = new FileSet(fileMgr);
		assertEquals(ErrorCode.OK, fs1.populateWithPaths(new String [] {"/"}));
		assertTrue(fs1.isMember(f1path)); assertTrue(fs1.isMember(f2path)); assertTrue(fs1.isMember(f3path)); 
		assertTrue(fs1.isMember(f4path)); assertTrue(fs1.isMember(f5path)); assertTrue(fs1.isMember(dirRoot));
		assertTrue(fs1.isMember(dirA)); assertTrue(fs1.isMember(dirAB)); assertTrue(fs1.isMember(dirABC)); 
		assertTrue(fs1.isMember(dirABCD)); assertTrue(fs1.isMember(dirABCDE)); assertTrue(fs1.isMember(dirABCDG));
		assertTrue(fs1.isMember(dirB)); assertTrue(fs1.isMember(dirBC)); assertTrue(fs1.isMember(dirBCD));
		assertEquals(16, fs1.size()); /* includes /tmp, the location of the build.bml file */
				
		/* add a single directory and it's contents */
		FileSet fs3 = new FileSet(fileMgr);
		assertEquals(ErrorCode.OK, fs3.populateWithPaths(new String[] {"/b/c/d/"}));
		assertTrue(fs3.isMember(f4path)); 
		assertTrue(fs3.isMember(f5path)); 
		assertTrue(fs3.isMember(dirBCD));
		assertEquals(3, fs3.size());

		/* add a directory (/b/c/d) and a file (/a/b/c/d/e/f1.c) */
		FileSet fs4 = new FileSet(fileMgr);
		assertEquals(ErrorCode.OK, fs4.populateWithPaths(new String[] {"/b/c/d/", "/a/b/c/d/e/f1.c"}));
		assertTrue(fs4.isMember(f1path)); 
		assertTrue(fs4.isMember(f4path)); 
		assertTrue(fs4.isMember(f5path)); 
		assertTrue(fs4.isMember(dirBCD));
		assertEquals(4, fs4.size());
		
		/* add a file (/b/c/d/f4.c) and another file (/a/b/c/d/e/f1.c) */
		FileSet fs5 = new FileSet(fileMgr);
		assertEquals(ErrorCode.OK, fs5.populateWithPaths(new String[] {"/b/c/d/f4.c", "/a/b/c/d/e/f1.c"}));
		assertTrue(fs5.isMember(f1path)); 
		assertTrue(fs5.isMember(f4path));
		assertEquals(2, fs5.size());
		
		/* search for all files ending in .c */
		FileSet fs6 = new FileSet(fileMgr);
		assertEquals(ErrorCode.OK, fs6.populateWithPaths(new String[] {"*.c"}));
		assertTrue(fs6.isMember(f1path));
		assertTrue(fs6.isMember(f2path));
		assertTrue(fs6.isMember(f3path)); 
		assertTrue(fs6.isMember(f4path));
		assertTrue(fs6.isMember(f5path));
		assertEquals(5, fs6.size());

		/* search for all files that have 3 in the name */
		FileSet fs7 = new FileSet(fileMgr);
		assertEquals(ErrorCode.OK, fs7.populateWithPaths(new String[] {"*3*"}));
		assertTrue(fs7.isMember(f3path)); 
		assertEquals(1, fs7.size());
		
		/* test pkgA_src */
		FileSet fs8 = new FileSet(fileMgr);
		assertEquals(ErrorCode.OK, fs8.populateWithPaths(new String[] {"@pkgA_src"}));
		assertEquals(7, fs8.size());
		assertTrue(fs8.isMember(f1path)); assertTrue(fs8.isMember(f2path)); assertTrue(fs8.isMember(f3path));
		assertTrue(fs8.isMember(dirABC)); assertTrue(fs8.isMember(dirABCD)); assertTrue(fs8.isMember(dirABCDE));
		assertTrue(fs8.isMember(dirABCDG));

		/* test @pkgA_gen with a path following it */
		FileSet fs9 = new FileSet(fileMgr);
		assertEquals(ErrorCode.OK, fs9.populateWithPaths(new String[] {"@pkgA_gen/c/d/f4.c"}));
		assertEquals(1, fs9.size());
		assertTrue(fs9.isMember(f4path));
		
		/* create a new package, named "foo", with one item in foo/public and three in foo/private */
		IPackageMgr pkgMgr = bs.getPackageMgr();
		int pkgFooId = pkgMgr.addPackage("foo");
		int sectPublic = pkgMemberMgr.getScopeId("public");
		int sectPrivate = pkgMemberMgr.getScopeId("private");
		assertEquals(ErrorCode.OK, 
						pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.MEMBER_TYPE_FILE, f1path, 
														pkgFooId, sectPublic));
		assertEquals(ErrorCode.OK,
						pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.MEMBER_TYPE_FILE, f2path,
														pkgFooId, sectPrivate));
		assertEquals(ErrorCode.OK, 
						pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.MEMBER_TYPE_FILE, f4path,
														pkgFooId, sectPrivate));
		assertEquals(ErrorCode.OK, 
						pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.MEMBER_TYPE_FILE, f5path,
														pkgFooId, sectPrivate));
		
		/* test @foo/public membership */
		FileSet fs10 = new FileSet(fileMgr);
		assertEquals(ErrorCode.OK, fs10.populateWithPaths(new String[] {"%pkg/foo/public"}));
		assertEquals(1, fs10.size());
		assertTrue(fs10.isMember(f1path));

		/* test @foo/private membership */
		FileSet fs11 = new FileSet(fileMgr);
		assertEquals(ErrorCode.OK, fs11.populateWithPaths(new String[] {"%p/foo/private"}));
		assertEquals(3, fs11.size());
		assertTrue(fs11.isMember(f2path));
		assertTrue(fs11.isMember(f4path));
		assertTrue(fs11.isMember(f5path));

		/* test @foo membership */
		FileSet fs12 = new FileSet(fileMgr);
		assertEquals(ErrorCode.OK, fs12.populateWithPaths(new String[] {"%pkg/foo"}));
		assertEquals(4, fs12.size());
		assertTrue(fs12.isMember(f1path));
		assertTrue(fs12.isMember(f2path));
		assertTrue(fs12.isMember(f4path));
		assertTrue(fs12.isMember(f5path));

		/* test ^@foo/public membership - includes directories */
		FileSet fs13 = new FileSet(fileMgr);
		assertEquals(ErrorCode.OK, fs13.populateWithPaths(new String[] {"%not-pkg/foo/public"}));
		assertEquals(15, fs13.size());
		assertTrue(fs13.isMember(f2path));
		assertTrue(fs13.isMember(f3path));
		assertTrue(fs13.isMember(f4path));
		assertTrue(fs13.isMember(f5path));
		
		/* test ^@foo/private membership - includes directories */
		FileSet fs14 = new FileSet(fileMgr);
		assertEquals(ErrorCode.OK, fs14.populateWithPaths(new String[] {"%np/foo/private"}));
		assertEquals(13, fs14.size());   /* includes /tmp */
		assertTrue(fs14.isMember(f1path));
		assertTrue(fs14.isMember(f3path));

		/* test ^@foo membership  - includes directories */
		FileSet fs15 = new FileSet(fileMgr);
		assertEquals(ErrorCode.OK, fs15.populateWithPaths(new String[] {"%np/foo"}));
		assertEquals(12, fs15.size());  /* includes /tmp */
		assertTrue(fs15.isMember(f3path));
		
		/* test with invalid paths */
		FileSet fs16 = new FileSet(fileMgr);
		assertEquals(ErrorCode.BAD_PATH, fs16.populateWithPaths(new String[] {"/a/b/x/y/z/"}));	
		assertEquals(ErrorCode.BAD_PATH, fs6.populateWithPaths(new String[] {"%p/pkg"}));
		assertEquals(ErrorCode.BAD_PATH, fs6.populateWithPaths(new String[] {"%p/foo/badsect"}));
		assertEquals(ErrorCode.BAD_PATH, fs6.populateWithPaths(new String[] {"%np/pkg"}));
		assertEquals(ErrorCode.BAD_PATH, fs6.populateWithPaths(new String[] {"%np/foo/badsect"}));
		assertEquals(ErrorCode.BAD_PATH, fs6.populateWithPaths(new String[] {"%p"}));
		assertEquals(ErrorCode.BAD_PATH, fs6.populateWithPaths(new String[] {"%np"}));
		assertEquals(ErrorCode.BAD_PATH, fs6.populateWithPaths(new String[] {"%p/"}));
		assertEquals(ErrorCode.BAD_PATH, fs6.populateWithPaths(new String[] {"%np/"}));
		assertEquals(ErrorCode.BAD_PATH, fs6.populateWithPaths(new String[] {"%badcommand/"}));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.buildml.model.types.FileSet#size()}.
	 */
	@Test
	public void testSize() {
		
		/* create a bunch of files */
		int f1path = fileMgr.addFile("/a/b/c/d/e/f1.c");
		int f2path = fileMgr.addFile("/a/b/c/d/e/f2.c");
		int f3path = fileMgr.addFile("/a/b/c/d/g/f3.c");
		int f4path = fileMgr.addFile("/b/c/d/f4.c");
		int f5path = fileMgr.addFile("/b/c/d/f5.c");		
		int f6path = fileMgr.addFile("/c/d/e/f6.c");

		/* add them to the file set, testing the size as we go along */ 
		assertEquals(0, fs.size());
		fs.add(f1path);		
		assertEquals(1, fs.size());
		fs.add(f2path);
		assertEquals(2, fs.size());
		fs.add(f3path);
		assertEquals(3, fs.size());
		fs.add(f4path);
		fs.add(f3path);
		fs.add(f2path);
		assertEquals(4, fs.size());
		fs.add(f5path);
		assertEquals(5, fs.size());
		fs.add(f6path);
		assertEquals(6, fs.size());
		
		/* populate with parents - should add 13 new paths */
		fs.populateWithParents();
		assertEquals(19, fs.size());
		
		/* now remove some of the entries, testing the size as we go */
		fs.remove(f1path);		
		assertEquals(18, fs.size());
		fs.remove(f2path);
		assertEquals(17, fs.size());
		fs.remove(f3path);
		assertEquals(16, fs.size());
		fs.remove(f4path);
		assertEquals(15, fs.size());
		fs.remove(f5path);
		assertEquals(14, fs.size());
		fs.remove(f6path);
		assertEquals(13, fs.size());
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the scalability of the FileSet data structure.
	 */
	@Test
	public void testScalability() {

		/* fill the FileSet with every third path ID */
		for (int i = 0; i != 1000000; i++) {
			fs.add(i * 3);
		}
		
		/* now test that everything that should be there is in place, but not the others */
		for (int i = 0; i != 1000000; i++) {
			assertTrue(fs.isMember(i * 3));
			assertFalse(fs.isMember(i * 3 + 1));
			assertFalse(fs.isMember(i * 3 + 2));			
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the initialization of a FileSet from an array
	 */
	@Test
	public void testInitialization() {
	
		Integer array[] = {1, 13, 145, 7626, 23232};
		
		FileSet fs = new FileSet(fileMgr, array);
		assertEquals(5, fs.size());
		assertTrue(fs.isMember(1));
		assertTrue(fs.isMember(13));
		assertTrue(fs.isMember(145));
		assertTrue(fs.isMember(7626));
		assertTrue(fs.isMember(23232));
		assertFalse(fs.isMember(42));		
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the addSubTree() method.
	 */
	@Test
	public void testAddSubTree() {
		
		int fileA = fileMgr.addFile("/1/2/3/4/A.c");
		int fileB = fileMgr.addFile("/1/2/3/4/B.c");
		int fileC = fileMgr.addFile("/1/2/3/4/C.c");
		int fileD = fileMgr.addFile("/1/2/3/4/5/D.c");
		int fileE = fileMgr.addFile("/1/2/3/4/5/E.c");
		int fileF = fileMgr.addFile("/1/2/3/4/5/6/F.c");
		int fileG = fileMgr.addFile("/1/2/7/G.c");
		int fileH = fileMgr.addFile("/1/2/7/H.c");
		int fileI = fileMgr.addFile("/1/2/7/I.c");
		int fileJ = fileMgr.addFile("/1/2/7/8/J.c");
		int fileK = fileMgr.addFile("/1/2/7/8/9/K.c");
		int dirRoot = fileMgr.getPath("/");
		int dir1 = fileMgr.getPath("/1");
		int dir2 = fileMgr.getPath("/1/2");
		int dir3 = fileMgr.getPath("/1/2/3");
		int dir4 = fileMgr.getPath("/1/2/3/4");
		int dir5 = fileMgr.getPath("/1/2/3/4/5");
		int dir6 = fileMgr.getPath("/1/2/3/4/5/6");
		int dir7 = fileMgr.getPath("/1/2/7");
		int dir8 = fileMgr.getPath("/1/2/7/8");
		int dir9 = fileMgr.getPath("/1/2/7/8/9");
		
		/* initially, the set is empty */
		assertEquals(0, fs.size());
		
		/* 
		 * add the sub-tree rooted at 5 (D.c, E.c, F.c and all the paths above should be added).
		 */
		fs.addSubTree(dir5);
		assertEquals(10, fs.size());
		checkMembers(fs, 
				new Integer[] { dirRoot, dir1, dir2, dir3, dir4, dir5, dir6, fileD, fileE, fileF });

		/* Nothing else should have been added	*/
		checkNotMembers(fs, 
				new Integer[] { dir7, dir8, dir9, fileA, fileB, fileC, fileG, fileH, fileI, fileJ, fileK});		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the removeSubTree() method.
	 */
	@Test
	public void testRemoveSubTree() {
		
		/* add a bunch of files to the fileMgr, then populate the FileSet with them all */
		int fileA = fileMgr.addFile("/1/2/3/4/A.c");
		int fileB = fileMgr.addFile("/1/2/3/4/B.c");
		int fileC = fileMgr.addFile("/1/2/3/4/C.c");
		int fileD = fileMgr.addFile("/1/2/3/4/5/D.c");
		int fileE = fileMgr.addFile("/1/2/3/4/5/E.c");
		int fileF = fileMgr.addFile("/1/2/3/4/5/6/F.c");
		int fileG = fileMgr.addFile("/1/2/7/G.c");
		int fileH = fileMgr.addFile("/1/2/7/H.c");
		int fileI = fileMgr.addFile("/1/2/7/I.c");
		int fileJ = fileMgr.addFile("/1/2/7/8/J.c");
		int fileK = fileMgr.addFile("/1/2/7/8/9/K.c");
		int dir7 = fileMgr.getPath("/1/2/7");
		int dir8 = fileMgr.getPath("/1/2/8");
		int dir9 = fileMgr.getPath("/1/2/9");
		fs = bs.getReportMgr().reportAllFiles();
		
		/* all files should be present now */
		assertEquals(22, fs.size());  /* includes /tmp */
		
		/* remove the sub-tree, rooted at directory 7 */
		fs.removeSubTree(dir7);
		assertEquals(14, fs.size());  /* includes /tmp */
		checkNotMembers(fs, new Integer[] {dir7, dir8, dir9, fileG, fileH, fileI, fileJ, fileK});
	}
	
	/*-------------------------------------------------------------------------------------*/

}
