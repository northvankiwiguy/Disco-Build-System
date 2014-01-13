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
import com.buildml.model.IPackageRootMgr;
import com.buildml.model.IPackageMemberMgr.*;
import com.buildml.model.undo.MultiUndoOp;
import com.buildml.model.IPackageMgr;
import com.buildml.refactor.CanNotRefactorException.Cause;
import com.buildml.refactor.imports.ImportRefactorer;
import com.buildml.utils.errors.ErrorCode;

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
	
	/** The source package that we create file groups in (file groups can't be in import) */
	private int srcPkgId;
	
	/** IDs of all the files that we're creating in our test harness */
	private int f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16;
	
	/** IDs of all the actions that we're creating in our test harness */
	private int a1, a2, a3, a4, a5;
	
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
		 * f4 - a1 - f9  - a3 - f14
		 *           f9  - a4 - f15
		 *           f9  - a5 - f16
		 * f5 - a1 - f10 - a3
		 *           f10 - a4 - f15
		 *           f10 - a5 - f16
		 * f6 - a2 - f11 - a3
		 * f7 - a2 - f12 - a3
		 * f8 - a2 - f13 - a3
		 * 
		 * fg1
		 * fg2
		 * 
		 */
		int parentActionId = actionMgr.getRootAction("root");
		int actionDirId = fileMgr.getPath("/");
		f1 = fileMgr.addFile("@workspace/a/b/file1");
		f2 = fileMgr.addFile("@workspace/a/b/file2");
		f3 = fileMgr.addFile("@workspace/a/b/file3");
		f4 = fileMgr.addFile("@workspace/a/b/file4");
		f5 = fileMgr.addFile("@workspace/a/c/file5");
		f6 = fileMgr.addFile("@workspace/a/c/file6");
		f7 = fileMgr.addFile("@workspace/a/c/file7");
		f8 = fileMgr.addFile("@workspace/a/c/file8");
		f9 = fileMgr.addFile("@workspace/a/c/file9");
		f10 = fileMgr.addFile("@workspace/a/c/file10");
		f11 = fileMgr.addFile("@workspace/a/c/file11");
		f12 = fileMgr.addFile("@workspace/a/c/file12");
		f13 = fileMgr.addFile("@workspace/a/c/file13");
		f14 = fileMgr.addFile("@workspace/a/c/file14");
		f15 = fileMgr.addFile("@workspace/a/c/file15");
		f16 = fileMgr.addFile("@workspace/a/c/file16");
		a1 = actionMgr.addShellCommandAction(parentActionId, actionDirId, "a1");
		a2 = actionMgr.addShellCommandAction(parentActionId, actionDirId, "a2");
		a3 = actionMgr.addShellCommandAction(parentActionId, actionDirId, "a3");
		a4 = actionMgr.addShellCommandAction(parentActionId, actionDirId, "a4");
		a5 = actionMgr.addShellCommandAction(parentActionId, actionDirId, "a5");
		fg1 = fileGroupMgr.newSourceGroup(srcPkgId);
		fg2 = fileGroupMgr.newSourceGroup(srcPkgId);
		
		/* add file access relationships - as shown in the diagram */
		actionMgr.addFileAccess(a1, f4, OperationType.OP_READ);
		actionMgr.addFileAccess(a1, f5, OperationType.OP_READ);
		actionMgr.addFileAccess(a1, f9, OperationType.OP_WRITE);
		actionMgr.addFileAccess(a1, f10, OperationType.OP_WRITE);
		actionMgr.addFileAccess(a2, f6, OperationType.OP_READ);
		actionMgr.addFileAccess(a2, f7, OperationType.OP_READ);
		actionMgr.addFileAccess(a2, f8, OperationType.OP_READ);
		actionMgr.addFileAccess(a2, f11, OperationType.OP_WRITE);
		actionMgr.addFileAccess(a2, f12, OperationType.OP_WRITE);
		actionMgr.addFileAccess(a2, f13, OperationType.OP_WRITE);
		actionMgr.addFileAccess(a3, f9, OperationType.OP_READ);
		actionMgr.addFileAccess(a3, f10, OperationType.OP_READ);
		actionMgr.addFileAccess(a3, f11, OperationType.OP_READ);
		actionMgr.addFileAccess(a3, f12, OperationType.OP_READ);
		actionMgr.addFileAccess(a3, f13, OperationType.OP_READ);
		actionMgr.addFileAccess(a3, f14, OperationType.OP_WRITE);
		actionMgr.addFileAccess(a4, f9, OperationType.OP_READ);
		actionMgr.addFileAccess(a4, f10, OperationType.OP_READ);
		actionMgr.addFileAccess(a4, f15, OperationType.OP_WRITE);
		actionMgr.addFileAccess(a5, f9, OperationType.OP_READ);
		actionMgr.addFileAccess(a5, f10, OperationType.OP_READ);
		actionMgr.addFileAccess(a5, f16, OperationType.OP_WRITE);
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
	 * Fetch the members in the destination package, to check what was moved.
	 * 
	 * @param type The type of result we're looking for (TYPE_ANY, TYPE_ACTION, etc).
	 * @return The members that were actually refactored into our destination package. We
	 * 		   extract all TYPE_FILE members because they're not interesting.
	 */
	private MemberDesc[] getResult(int type) {
		MemberDesc result[] = pkgMemberMgr.getMembersInPackage(destPkgId, 
				IPackageMemberMgr.SCOPE_NONE, type);
		
		/* remove any files that we may have collected (files aren't shown in a diagram) */
		List<MemberDesc> filtered = new ArrayList<IPackageMemberMgr.MemberDesc>();
		for (int i = 0; i < result.length; i++) {
			if (result[i].memberType != IPackageMemberMgr.TYPE_FILE) {
				filtered.add(result[i]);
			}
		}
		return filtered.toArray(new MemberDesc[filtered.size()]);
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
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper method for comparing a file group's content against it's expected
	 * membership. The ordering of the members is ignored, and duplicates will
	 * not be accounted for correctly.
	 * 
	 * @param fileGroupId	The ID of the file group.
	 * @param expected		An array of expected ID values.
	 */
	private void validateFileGroupContents(int fileGroupId, Integer[] expected) {

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
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Validate that a package member belongs to a specific package.
	 * 
	 * @param pkgId			The package that we expect the member to be in.
	 * @param memberType	The type of member (TYPE_FILE, TYPE_FILE_GROUP, etc).
	 * @param memberId		The ID of the member.
	 */
	private void validatePackage(int pkgId, int memberType, int memberId) {
		PackageDesc desc = pkgMemberMgr.getPackageOfMember(memberType, memberId);
		assertEquals(pkgId, desc.pkgId);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Validate that an action, and its input and output file groups are correct. This method
	 * is called by all of our test* methods.
	 * 
	 * @param destPkgId 	The ID of the package we're moving things to.
	 * @param actionId		The action we're validating
	 * @param inputFiles	The array of files we're expecting to see at the input.
	 * @param outputFiles	The array of files we're expecting to see on the output.
	 */
	private void validateInputsOutputs(int destPkgId, int actionId, Integer[] inputFiles, Integer[] outputFiles) {
		
		validatePackage(destPkgId, IPackageMemberMgr.TYPE_ACTION, actionId);

		Integer inputSlotValue = (Integer) actionMgr.getSlotValue(
				actionId, actionMgr.getSlotByName(actionId, "Input"));
		Integer outputSlotValue = (Integer) actionMgr.getSlotValue(
				actionId, actionMgr.getSlotByName(actionId, "Output"));
		assertNotNull(inputSlotValue);
		assertNotNull(outputSlotValue);
		
		/* confirm that each of the input/output file groups are in destPkgId */
		validatePackage(destPkgId, IPackageMemberMgr.TYPE_FILE_GROUP, inputSlotValue);
		validatePackage(destPkgId, IPackageMemberMgr.TYPE_FILE_GROUP, outputSlotValue);
		
		/* 
		 * Confirm the membership of the input file group. Given that it could be any type
		 * of group, we must expand the content of the group in order to validate it.
		 */
		String actualInputs[] = fileGroupMgr.getExpandedGroupFiles(inputSlotValue);
		List<String> expectedInputs = new ArrayList<String>();
		for (int i = 0; i < inputFiles.length; i++) {
			expectedInputs.add(fileMgr.getPathName(inputFiles[i], true));
		}
		String expectedInputArray[] = expectedInputs.toArray(new String[0]);
		assertTrue(CommonTestUtils.sortedArraysEqual(expectedInputArray, actualInputs));

		/* confirm the membership of the output file group */
		Integer actualOutputs[] = fileGroupMgr.getPathIds(outputSlotValue);
		assertTrue(CommonTestUtils.sortedArraysEqual(outputFiles, actualOutputs));
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
			MultiUndoOp multiOp = new MultiUndoOp();
			refactor.moveMembersToPackage(multiOp, destPkgId, new ArrayList<MemberDesc>());
			multiOp.redo();
			assertEquals(0, getResult(IPackageMemberMgr.TYPE_ANY).length);
		} catch (CanNotRefactorException e) {
			fail();
		}
		
		/* test with null list */
		try {
			MultiUndoOp multiOp = new MultiUndoOp();
			refactor.moveMembersToPackage(multiOp, destPkgId, null);
			multiOp.redo();
			fail();
		} catch (CanNotRefactorException e) {
			assertEquals(Cause.INVALID_MEMBER, e.getCauseCode());
			assertEquals(-1, (int)e.getCauseIDs()[0]);
		}
		
		/* test with invalid package ID */
		try {
			MultiUndoOp multiOp = new MultiUndoOp();
			refactor.moveMembersToPackage(multiOp, 1389, buildList('f', f1));
			multiOp.redo();
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
			MultiUndoOp multiOp = new MultiUndoOp();
			refactor.moveMembersToPackage(multiOp, destPkgId, members);
			multiOp.redo();
			fail();
		} catch (CanNotRefactorException e) {
			assertEquals(Cause.INVALID_MEMBER, e.getCauseCode());
			assertEquals(1357, (int)e.getCauseIDs()[0]);
		}
		
		/* test with two invalid file IDs (mixed in with other valid members) */
		try {
			MultiUndoOp multiOp = new MultiUndoOp();
			refactor.moveMembersToPackage(multiOp, destPkgId, 
					buildList('a', a1, 'f', f2, 'f', 1000, 'f', f3, 'f', 2000, 'a', a2));
			multiOp.redo();
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
			MultiUndoOp multiOp = new MultiUndoOp();
			refactor.moveMembersToPackage(multiOp, destPkgId, 
					buildList('a', a1, 'f', f2, 'a', 1234, 'f', f3, 'a', 5678, 'a', a2));
			multiOp.redo();
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
			MultiUndoOp multiOp = new MultiUndoOp();
			refactor.moveMembersToPackage(multiOp, destPkgId, 
					buildList('a', a1, 'g', 1359, 'g', fg2, 'g', fg1, 'g', 1357, 'g', 1358));
			multiOp.redo();
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
		
		/* move three loose files into a package */
		try {
			MultiUndoOp multiOp = new MultiUndoOp();
			refactor.moveMembersToPackage(multiOp, destPkgId, buildList('f', f3, 'f', f1, 'f', f2));
			multiOp.redo();
		} catch (CanNotRefactorException e) {
			fail();
		}
		
		/* 
		 * Validate that the package now has a single file group containing those three files.
		 * Also, each of the files must have been moved into the new package.
		 */
		MemberDesc members[] = getResult(IPackageMemberMgr.TYPE_ANY);
		assertEquals(1, members.length);
		MemberDesc fileGroup = members[0];
		assertEquals(IPackageMemberMgr.TYPE_FILE_GROUP, fileGroup.memberType);
		validateFileGroupContents(fileGroup.memberId, new Integer[] {f1, f2, f3});
		validatePackage(destPkgId, IPackageMemberMgr.TYPE_FILE, f1);
		validatePackage(destPkgId, IPackageMemberMgr.TYPE_FILE, f2);
		validatePackage(destPkgId, IPackageMemberMgr.TYPE_FILE, f3);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test that any "loose" files which out out of range of the package's root will cause
	 * an exception.
	 */
	@Test
	public void testLooseFilesOutOfRange() {
		
		/* set the source root of the package to @workspace/a/b */
		IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
		assertEquals(ErrorCode.OK, pkgRootMgr.setPackageRoot(
				destPkgId, IPackageRootMgr.SOURCE_ROOT, fileMgr.getPath("@workspace/a/b")));
		
		/* move three loose files into a package - f6 and f7 are in @workspace/a/c */
		try {
			MultiUndoOp multiOp = new MultiUndoOp();
			refactor.moveMembersToPackage(multiOp, destPkgId, buildList('f', f3, 'f', f6, 'f', f7));
			multiOp.redo();
			fail();
		} catch (CanNotRefactorException e) {
			assertEquals(Cause.PATH_OUT_OF_RANGE, e.getCauseCode());
			assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] {f6,  f7}, e.getCauseIDs()));
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test moving of action a1.
	 */
	@Test
	public void testMoveActionA1() {

		try {
			MultiUndoOp multiOp = new MultiUndoOp();
			refactor.moveMembersToPackage(multiOp, destPkgId, buildList('a', a1));
			multiOp.redo();
		} catch (CanNotRefactorException e) {
			fail();
		}
		
		/* 
		 * Package should now contain:
		 * 	- Input file group: {f4, f5}
		 *  - Action a1
		 *  - Output file group: {f9, f10}
		 */
		MemberDesc actions[] = getResult(IPackageMemberMgr.TYPE_ACTION);
		assertEquals(1, actions.length);
		MemberDesc fileGroups[] = getResult(IPackageMemberMgr.TYPE_FILE_GROUP);
		assertEquals(2, fileGroups.length);
		
		/* confirm that action A1 has an input and output slot connected, and that the file groups are correct */
		validateInputsOutputs(destPkgId, a1, new Integer[] {f4, f5}, new Integer[] {f9, f10});
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test moving a2
	 */
	@Test
	public void testMoveActionA2() {

		try {
			MultiUndoOp multiOp = new MultiUndoOp();
			refactor.moveMembersToPackage(multiOp, destPkgId, buildList('a', a2));
			multiOp.redo();
		} catch (CanNotRefactorException e) {
			fail();
		}
		
		/* 
		 * Package should now contain:
		 * 	- Input file group: {f6, f7, f8}
		 *  - Action a2
		 *  - Output file group: {f11, f12, f13}
		 */
		MemberDesc actions[] = getResult(IPackageMemberMgr.TYPE_ACTION);
		assertEquals(1, actions.length);
		MemberDesc fileGroups[] = getResult(IPackageMemberMgr.TYPE_FILE_GROUP);
		assertEquals(2, fileGroups.length);
		
		/* confirm that action A1 has an input and output slot connected, and that the file groups are correct */
		validateInputsOutputs(destPkgId, a2, new Integer[] {f6, f7, f8}, new Integer[] {f11, f12, f13});
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test moving f10
	 */
	@Test
	public void testMoveFileF10() {

		try {
			MultiUndoOp multiOp = new MultiUndoOp();
			refactor.moveMembersToPackage(multiOp, destPkgId, buildList('f', f10));
			multiOp.redo();
		} catch (CanNotRefactorException e) {
			fail();
		}

		/* 
		 * Package should now contain:
		 * 	- Input file group: {f4, f5}
		 *  - Action a1
		 *  - Output file group: {f9, f10}
		 */
		MemberDesc actions[] = getResult(IPackageMemberMgr.TYPE_ACTION);
		assertEquals(1, actions.length);
		MemberDesc fileGroups[] = getResult(IPackageMemberMgr.TYPE_FILE_GROUP);
		assertEquals(2, fileGroups.length);

		/* confirm that action A1 has an input and output slot connected, and that the file groups are correct */
		validateInputsOutputs(destPkgId, a1, new Integer[] {f4, f5}, new Integer[] {f9, f10});
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test moving f12
	 */
	@Test
	public void testMoveFileF12() {

		try {
			MultiUndoOp multiOp = new MultiUndoOp();
			refactor.moveMembersToPackage(multiOp, destPkgId, buildList('f', f12));
			multiOp.redo();
		} catch (CanNotRefactorException e) {
			fail();
		}
		
		/* 
		 * Package should now contain:
		 * 	- Input file group: {f6, f7, f8}
		 *  - Action a2
		 *  - Output file group: {f11, f12, f13}
		 */
		MemberDesc actions[] = getResult(IPackageMemberMgr.TYPE_ACTION);
		assertEquals(1, actions.length);
		MemberDesc fileGroups[] = getResult(IPackageMemberMgr.TYPE_FILE_GROUP);
		assertEquals(2, fileGroups.length);
		
		/* confirm that action A1 has an input and output slot connected, and that the file groups are correct */
		validateInputsOutputs(destPkgId, a2, new Integer[] {f6, f7, f8}, new Integer[] {f11, f12, f13});
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test moving f10 and f12
	 */
	@Test
	public void testMoveFileF1012() {

		try {
			MultiUndoOp multiOp = new MultiUndoOp();
			refactor.moveMembersToPackage(multiOp, destPkgId, buildList('f', f12, 'f', f10));
			multiOp.redo();
		} catch (CanNotRefactorException e) {
			fail();
		}
		
		/* 
		 * Package should now contain:
		 *  - Input file group: {f4, f5}
		 *  - Action a1
		 *  - Output file group: {f9, f10}
		 * AND
		 * 	- Input file group: {f6, f7, f8}
		 *  - Action a2
		 *  - Output file group: {f11, f12, f13}
		 */
		MemberDesc actions[] = getResult(IPackageMemberMgr.TYPE_ACTION);
		assertEquals(2, actions.length);
		MemberDesc fileGroups[] = getResult(IPackageMemberMgr.TYPE_FILE_GROUP);
		assertEquals(4, fileGroups.length);
		
		/* confirm that action A1 has an input and output slot connected, and that the file groups are correct */
		validateInputsOutputs(destPkgId, a1, new Integer[] {f4, f5}, new Integer[] {f9, f10});
		validateInputsOutputs(destPkgId, a2, new Integer[] {f6, f7, f8}, new Integer[] {f11, f12, f13});
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 *  Test chain where multiple actions converge into a single action.
	 */
	@Test
	public void testMoveFileF14() {
		try {
			MultiUndoOp multiOp = new MultiUndoOp();
			refactor.moveMembersToPackage(multiOp, destPkgId, buildList('f', f14));
			multiOp.redo();
		} catch (CanNotRefactorException e) {
			fail();
		}
		
		/* 
		 * Package should now contain:
		 *  - Input file group: {f4, f5}
		 *  - Action a1
		 *  - Output file group: {f9, f10}
		 * AND
		 * 	- Input file group: {f6, f7, f8}
		 *  - Action a2
		 *  - Output file group: {f11, f12, f13}
		 * AND
		 * 	- Input file group: {f9, f10, f11, f12, f13}
		 *  - Action a3
		 *  - Output file group: {f14}
		 */
		MemberDesc actions[] = getResult(IPackageMemberMgr.TYPE_ACTION);
		assertEquals(3, actions.length);
		MemberDesc fileGroups[] = getResult(IPackageMemberMgr.TYPE_FILE_GROUP);
		assertEquals(6, fileGroups.length);
		
		/* confirm that action A1 has an input and output slot connected, and that the file groups are correct */
		validateInputsOutputs(destPkgId, a1, new Integer[] {f4, f5}, new Integer[] {f9, f10});
		validateInputsOutputs(destPkgId, a2, new Integer[] {f6, f7, f8}, new Integer[] {f11, f12, f13});		
		validateInputsOutputs(destPkgId, a3, new Integer[] {f9, f10, f11, f12, f13}, new Integer[] {f14});
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 *  Test chain where a single action splits out into multiple actions..
	 */
	@Test
	public void testMoveFileF14F15() {
		try {
			MultiUndoOp multiOp = new MultiUndoOp();
			refactor.moveMembersToPackage(multiOp, destPkgId, buildList('f', f15, 'f', f16));
			multiOp.redo();
		} catch (CanNotRefactorException e) {
			fail();
		}
		
		/* 
		 * Package should now contain:
		 *  - Input file group: {f4, f5}
		 *  - Action a1
		 *  - Output file group: {f9, f10}
		 * AND
		 * 	- Input file group: {f9, f10}
		 *  - Action a4
		 *  - Output file group: {f15}
		 * AND
		 * 	- Input file group: {f9, f10}
		 *  - Action a5
		 *  - Output file group: {f16}
		 */
		MemberDesc actions[] = getResult(IPackageMemberMgr.TYPE_ACTION);
		assertEquals(3, actions.length);
		MemberDesc fileGroups[] = getResult(IPackageMemberMgr.TYPE_FILE_GROUP);
		assertEquals(4, fileGroups.length);
		
		/* confirm that action A1 has an input and output slot connected, and that the file groups are correct */
		validateInputsOutputs(destPkgId, a1, new Integer[] {f4, f5}, new Integer[] {f9, f10});
		validateInputsOutputs(destPkgId, a4, new Integer[] {f9, f10}, new Integer[] {f15});		
		validateInputsOutputs(destPkgId, a5, new Integer[] {f9, f10}, new Integer[] {f16});
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test moving an action that writes into /home (which is out of range of the package).
	 */
	@Test
	public void testMoveWriteToOutOfRange() {
		
		/* create a new file /tmp/foo, that action a1 writes to */
		int fTmp = fileMgr.addFile("/home/foo");
		assertTrue(fTmp > 0);
		actionMgr.addFileAccess(a1, fTmp, OperationType.OP_WRITE);
		
		/* try the move - it will fail */
		try {
			MultiUndoOp multiOp = new MultiUndoOp();
			refactor.moveMembersToPackage(multiOp, destPkgId, buildList('a', a1));
			multiOp.redo();
			fail();
		} catch (CanNotRefactorException e) {
			assertEquals(Cause.PATH_OUT_OF_RANGE, e.getCauseCode());
			Integer ids[] = e.getCauseIDs();
			assertEquals(1, ids.length);
			assertEquals(Integer.valueOf(fTmp), ids[0]);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test moving a non-atomic action (
	 */
	@Test
	public void testMoveNonAtomic() {
		
		/* create a child action for a1 */
		int actionDirId = fileMgr.getPath("/");
		int a1Child = actionMgr.addShellCommandAction(a1, actionDirId, "a1Child");
		
		/* move a chain, which has a1 in it */
		try {
			MultiUndoOp multiOp = new MultiUndoOp();
			refactor.moveMembersToPackage(multiOp, destPkgId, buildList('f', f15, 'f', f16));
			multiOp.redo();
			fail();
		} catch (CanNotRefactorException e) {
			assertEquals(Cause.ACTION_NOT_ATOMIC, e.getCauseCode());
			assertEquals(a1, (int)(e.getCauseIDs()[0]));
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

}
