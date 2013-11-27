/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.refactor;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.buildml.model.CommonTestUtils;
import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IActionMgr;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileGroupMgr;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMemberMgr.*;
import com.buildml.model.IPackageMgr;
import com.buildml.refactor.CanNotRefactorException.Cause;
import com.buildml.refactor.imports.ImportRefactorer;

/**
 * Test cases to verify the IImportRefactorer implementation. These tests focus
 * on moving members to other packages.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class TestImportRefactorMoveToPackage {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	private IBuildStore buildStore;
	private IFileMgr fileMgr;
	private IActionMgr actionMgr;
	private IPackageMgr pkgMgr;
	private IPackageMemberMgr pkgMemberMgr;
	private IFileGroupMgr fileGroupMgr;
	private IImportRefactorer refactor;
	
	/** The empty package we're moving members into */
	private int destPkgId;
	
	/** The source package that we create file groups in (they can't be in import) */
	private int srcPkgId;
	
	/** IDs of all the files that we're creating in our test harness */
	private int f1, f2, f3, f4, f5, f6, f7, f8;
	
	/** IDs of all the actions that we're creating in our test harness */
	private int a1, a2, a3;
	
	/** IDs of all the file groups that we're creating in our test harness */
	private int fg1, fg2;

	/*=====================================================================================*
	 * SETUP/TEARDOWN
	 *=====================================================================================*/

	/**
	 * Method called before each test case - sets up default configuration.
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		/* get a new empty BuildStore */
		buildStore = CommonTestUtils.getEmptyBuildStore();
		
		/* fetch the associated managers */
		fileMgr = buildStore.getFileMgr();
		actionMgr = buildStore.getActionMgr();
		pkgMgr = buildStore.getPackageMgr();
		pkgMemberMgr = buildStore.getPackageMemberMgr();
		fileGroupMgr = buildStore.getFileGroupMgr();
		
		/* this is the object under test */
		refactor = new ImportRefactorer(buildStore);
		
		/* source package for file groups (they can't exist in the import package) */
		srcPkgId = pkgMgr.addPackage("srcPkg");
		
		/* we're moving to this package */
		destPkgId = pkgMgr.addPackage("MyPkg");
		
		/*
		 * Create the following "import" structure, from which all of our tests will operate
		 * 
		 * f1
		 * f2
		 * f3
		 * f4 - a1
		 * f5 - a1
		 * f6 - a2
		 * f7 - a2
		 * f8 - a2
		 * 
		 * fg1
		 * fg2
		 * 
		 */
		int parentActionId = actionMgr.getRootAction("root");
		int actionDirId = fileMgr.getPath("/");
		f1 = fileMgr.addFile("/a/b/file1");
		f2 = fileMgr.addFile("/a/b/file2");
		f3 = fileMgr.addFile("/a/b/file3");
		f4 = fileMgr.addFile("/a/b/file4");
		f5 = fileMgr.addFile("/a/b/file5");
		f6 = fileMgr.addFile("/a/b/file6");
		f7 = fileMgr.addFile("/a/b/file7");
		f8 = fileMgr.addFile("/a/b/file8");
		a1 = actionMgr.addShellCommandAction(parentActionId, actionDirId, "a1");
		actionMgr.addFileAccess(a1, f4, OperationType.OP_READ);
		actionMgr.addFileAccess(a1, f5, OperationType.OP_READ);
		actionMgr.addFileAccess(a2, f6, OperationType.OP_READ);
		actionMgr.addFileAccess(a2, f7, OperationType.OP_READ);
		a2 = actionMgr.addShellCommandAction(parentActionId, actionDirId, "a2");
		fg1 = fileGroupMgr.newSourceGroup(srcPkgId);
		fg2 = fileGroupMgr.newSourceGroup(srcPkgId);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Method called after each test cases - closes the BuildStore.
	 * @throws Exception
     */	
	@After
	public void tearDown() throws Exception {
		buildStore.close();
	}

	/*=====================================================================================*
	 * HELPER METHODS
	 *=====================================================================================*/
	
	/**
	 * @return The members that were actually refactored into our destination package.
	 */
	private MemberDesc[] getResult() {
		return pkgMemberMgr.getMembersInPackage(destPkgId, 
				IPackageMemberMgr.SCOPE_NONE, IPackageMemberMgr.TYPE_ANY);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper method for building a MemberDesc list. Used only for simplifying test cases.
	 * 
	 * @param args Any number of pairs of elements. The first of each pair is the type of
	 *             the member ('a' for action, 'f' for file, 'g' for group). The second
	 *             of each pair is the ID for the action, file or group.
	 * @return The MemberDesc list.
	 */
	private List<MemberDesc> buildList(Object... args) {

		if ((args.length % 2) != 0) {
			throw new FatalBuildStoreError("Must have an even number of arguments");
		}
		
		List<MemberDesc> resultList = new ArrayList<MemberDesc>();
		for (int i = 0; i != args.length; ) {
			char type = (Character)args[i++];
			int id = (Integer)args[i++];
			
			int memberType;
			if (type == 'a') {
				memberType = IPackageMemberMgr.TYPE_ACTION;
			} else if (type == 'f') {
				memberType = IPackageMemberMgr.TYPE_FILE;				
			} else if (type == 'g') {
				memberType = IPackageMemberMgr.TYPE_FILE_GROUP;				
			} else {
				throw new FatalBuildStoreError("Invalid member type: " + type);
			}
			
			MemberDesc newMember = new MemberDesc(memberType, id, 0, 0);
			resultList.add(newMember);
		}
		return resultList;
	}
	

	/**
	 * Helper method for comparing a file group's content against it's expected
	 * membership. The ordering of the members is ignored, and duplicates will
	 * not be accounted for correctly.
	 * 
	 * @param fileGroupId	The ID of the file group.
	 * @param expected		An array of expected ID values.
	 */
	private void validateFileGroupContains(int fileGroupId, Integer[] expected) {

		Integer actual[] = fileGroupMgr.getPathIds(fileGroupId);
		assertNotNull(actual);
		assertEquals(expected.length, actual.length);
		
		/* now look for the presence of all the members */
		for (int i = 0; i < expected.length; i++) {
			boolean found = false;
			for (int j = 0; j < actual.length; j++) {
				if (actual[j] == expected[i]) {
					found = true;
				}
			}
			if (!found) {
				fail("Unable to find expected file group member: " + expected[i]);
			}
		}
	}
	
	/*=====================================================================================*
	 * TEST METHODS
	 *=====================================================================================*/

	/**
	 * Test base cases for MoveToPackage. This includes basic error checking and validation
	 * of input.
	 */
	@Test
	public void testBaseCases() {
		
		/* test with empty list */
		try {
			refactor.moveMembersToPackage(destPkgId, new ArrayList<MemberDesc>());
			assertEquals(0, getResult().length);
		} catch (CanNotRefactorException e) {
			fail();
		}
		
		/* test with null list */
		try {
			refactor.moveMembersToPackage(destPkgId, null);
			fail();
		} catch (CanNotRefactorException e) {
			assertEquals(Cause.INVALID_MEMBER, e.getCauseCode());
			assertEquals(-1, (int)e.getCauseIDs()[0]);
		}
		
		/* test with invalid package ID */
		try {
			refactor.moveMembersToPackage(1389, buildList('f', f1));
			fail();
		} catch (CanNotRefactorException e) {
			assertEquals(Cause.INVALID_PACKAGE, e.getCauseCode());
			assertEquals(1389, (int)e.getCauseIDs()[0]);
		}
		
		/* test with an invalid member type */		
		try {
			List<MemberDesc> members = new ArrayList<MemberDesc>();
			members.add(new MemberDesc(IPackageMemberMgr.TYPE_FILE, f1, 0, 0));
			members.add(new MemberDesc(1357, f1, 0, 0));			
			refactor.moveMembersToPackage(destPkgId, members);
			fail();
		} catch (CanNotRefactorException e) {
			assertEquals(Cause.INVALID_MEMBER, e.getCauseCode());
			assertEquals(1357, (int)e.getCauseIDs()[0]);
		}
		
		/* test with two invalid file IDs (mixed in with other valid members) */
		try {
			refactor.moveMembersToPackage(destPkgId, 
					buildList('a', a1, 'f', f2, 'f', 1000, 'f', f3, 'f', 2000, 'a', a2));
			fail();
		} catch (CanNotRefactorException e) {
			assertEquals(Cause.INVALID_PATH, e.getCauseCode());
			Integer badPaths[] = e.getCauseIDs();
			assertEquals(2, badPaths.length);
			assertEquals(1000, (int)badPaths[0]);
			assertEquals(2000, (int)badPaths[1]);
		}
		
		/* test with two invalid actions (and other valid members) */
		try {
			refactor.moveMembersToPackage(destPkgId, 
					buildList('a', a1, 'f', f2, 'a', 1234, 'f', f3, 'a', 5678, 'a', a2));
			fail();
		} catch (CanNotRefactorException e) {
			assertEquals(Cause.INVALID_ACTION, e.getCauseCode());
			Integer badActions[] = e.getCauseIDs();
			assertEquals(2, badActions.length);
			assertEquals(1234, (int)badActions[0]);
			assertEquals(5678, (int)badActions[1]);
		}
		
		/* test with two invalid file groups (and other valid members) */
		try {
			refactor.moveMembersToPackage(destPkgId, 
					buildList('a', a1, 'g', 1359, 'g', fg2, 'g', fg1, 'g', 1357, 'g', 1358));
			fail();
		} catch (CanNotRefactorException e) {
			assertEquals(Cause.INVALID_FILE_GROUP, e.getCauseCode());
			Integer badGroups[] = e.getCauseIDs();
			assertEquals(3, badGroups.length);
			assertEquals(1359, (int)badGroups[0]);
			assertEquals(1357, (int)badGroups[1]);
			assertEquals(1358, (int)badGroups[2]);
		}
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test that any "loose" files that aren't attached to any actions are placed into a single
	 * file group in the destination package.
	 */
	@Test
	public void testLooseFiles() {
		try {
			refactor.moveMembersToPackage(destPkgId, buildList('f', f3, 'f', f1, 'f', f2));
		} catch (CanNotRefactorException e) {
			fail();
		}
		MemberDesc members[] = getResult();
		assertEquals(1, members.length);
		MemberDesc fileGroup = members[0];
		assertEquals(IPackageMemberMgr.TYPE_FILE_GROUP, fileGroup.memberType);
		validateFileGroupContains(fileGroup.memberId, new Integer[] {f1, f2, f3});
	}
	
	/*-------------------------------------------------------------------------------------*/

}
