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
import java.util.List;

import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileGroupMgr;
import com.buildml.model.IFileMgr;
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
	
}
