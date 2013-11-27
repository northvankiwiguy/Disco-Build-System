/*******************************************************************************
 * Copyright (c) 2013 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.refactor.imports;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileGroupMgr;
import com.buildml.model.IFileMgr;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.IFileMgr.PathType;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMemberMgr.MemberDesc;
import com.buildml.refactor.CanNotRefactorException;
import com.buildml.refactor.CanNotRefactorException.Cause;
import com.buildml.utils.errors.ErrorCode;

/**
 * A class of static utility methods to support commands in ImportRefactorer(). These
 * methods focus on moving package members to other packages.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class MoveRefactorerUtils {

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Validate the list of members that is input into moveMembersToPackage(). There are
	 * many possible errors, including invalid member types, or underfined action, file,
	 * or fileGroup ID numbers. Errors are report via exceptions.
	 * 
	 * @param buildStore 	The IBuildStore to query from.
	 * @param members 		The list of MemberDesc to be validated.
	 * @throws CanNotRefactorException The reason why the members list is invalid.
	 */
	/* package */ static void validateMembersList(IBuildStore buildStore, List<MemberDesc> members) 
								throws CanNotRefactorException {

		IFileMgr fileMgr = buildStore.getFileMgr();
		IFileGroupMgr fileGroupMgr = buildStore.getFileGroupMgr();
		IActionMgr actionMgr = buildStore.getActionMgr();
		
		/* can't be null! */
		if (members == null) {
			throw new CanNotRefactorException(Cause.INVALID_MEMBER, -1);
		}
		
		/* keep a lists of invalid "things" - we can provide the whole list in the exception */
		List<Integer> invalidFiles = new ArrayList<Integer>();
		List<Integer> invalidFileGroups = new ArrayList<Integer>();
		List<Integer> invalidActions = new ArrayList<Integer>();
		
		/*
		 * Scan the list of members, validating their type, and whether each ID is valid.
		 * Invalid entries are logged, and will be reported as exceptions once we've seen
		 * all the members.
		 */
		for (MemberDesc member: members) {
			int id = member.memberId;
			switch (member.memberType) {
			case IPackageMemberMgr.TYPE_FILE:
				PathType pathType = fileMgr.getPathType(id);
				if (pathType == PathType.TYPE_INVALID) {
					invalidFiles.add(id);
				}
				break;
					
			case IPackageMemberMgr.TYPE_FILE_GROUP:
				if (fileGroupMgr.getGroupType(id) == ErrorCode.NOT_FOUND) {
					invalidFileGroups.add(id);
				}
				break;
					
			case IPackageMemberMgr.TYPE_ACTION:
				if (!actionMgr.isActionValid(id)) {
					invalidActions.add(id);
				}
				break;
					
			default:
				throw new CanNotRefactorException(Cause.INVALID_MEMBER, member.memberType);
			}
		}
		
		/* Thrown exceptions if we found anything invalid */
		if (!invalidFiles.isEmpty()) {
			throw new CanNotRefactorException(Cause.INVALID_PATH, invalidFiles.toArray(new Integer[0]));
		}
		if (!invalidActions.isEmpty()) {
			throw new CanNotRefactorException(Cause.INVALID_ACTION, invalidActions.toArray(new Integer[0]));
		}
		if (!invalidFileGroups.isEmpty()) {
			throw new CanNotRefactorException(Cause.INVALID_FILE_GROUP, invalidFileGroups.toArray(new Integer[0]));
		}
		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * For all "loose" files in the list of members, convert them into a single file group in
	 * the destination package. A "loose" file is one that isn't generated by any actions.
	 * Non-loose files are still important, but they aren't handled by this method.
	 * 
	 * All loose files are removed from the member list upon return.
	 * 
	 * @param buildStore	The IBuildStore that holds the database.
	 * @param destPkgId		The ID of the package to move files into.
	 * @param members		The list of members to be moved.
	 */
	public static void convertLooseFilesToFileGroup(
			IBuildStore buildStore, int destPkgId, List<MemberDesc> members) {
		
		IActionMgr actionMgr = buildStore.getActionMgr();
		IFileGroupMgr fileGroupMgr = buildStore.getFileGroupMgr();
		
		/* we'll collect up all the loose paths */
		List<MemberDesc> looseMembers = new ArrayList<MemberDesc>();
		
		/*
		 * Separate the "loose" members from the non-"loose" members.
		 */
		int index = 0;
		while (index != members.size()) {
			MemberDesc member = members.get(index);
			if (member.memberType == IPackageMemberMgr.TYPE_FILE) {
				int pathId = member.memberId;
				Integer writeActions[] = actionMgr.getActionsThatAccess(pathId, OperationType.OP_WRITE);
				
				/* this path is not written by any actions */
				if (writeActions.length == 0) {
					looseMembers.add(member);
					members.remove(index);
				} else {
					index++;
				}
			}
		}
		
		/*
		 * If there are any loose files, create a file group.
		 */
		if (looseMembers.size() > 0) {
			int fileGroupId = fileGroupMgr.newSourceGroup(destPkgId);
			for (MemberDesc member : looseMembers) {
				fileGroupMgr.addPathId(fileGroupId, member.memberId);
			}
		}
		
		// TODO: move the files into the new package.
		// TODO: undo/redo.
	}
	
	/*-------------------------------------------------------------------------------------*/
	
}