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

package com.buildml.refactor.imports;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IActionMgr.FileAccess;
import com.buildml.model.IFileMgr.PathType;
import com.buildml.model.IPackageMemberMgr.MemberDesc;
import com.buildml.model.IPackageMgr;
import com.buildml.model.types.ActionSet;
import com.buildml.model.undo.ActionUndoOp;
import com.buildml.model.undo.MultiUndoOp;
import com.buildml.refactor.CanNotRefactorException;
import com.buildml.refactor.CanNotRefactorException.Cause;
import com.buildml.refactor.IImportRefactorer;
import com.buildml.utils.errors.ErrorCode;

/**
 * An implementation of the IImportRefactorer class. See that class for usage details.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class ImportRefactorer implements IImportRefactorer {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/** The BuildStore that all our refactorings will operate on. */
	private IBuildStore buildStore;
	
	/** The FileMgr used by these refactorings. */
	private IFileMgr fileMgr;

	/** The ActionMgr used by these refactorings. */
	private IActionMgr actionMgr;
	
	/** The PackageMgr used by these refactorings. */
	private IPackageMgr pkgMgr;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new ImportRefactorer object which will perform operations on the
	 * specified BuildStore
	 * 
	 * @param buildStore The BuildStore on which to perform refactoring operations.
	 */
	public ImportRefactorer(IBuildStore buildStore) {
		this.buildStore = buildStore;
		this.fileMgr = buildStore.getFileMgr();
		this.actionMgr = buildStore.getActionMgr();
		this.pkgMgr = buildStore.getPackageMgr();
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.refactor.IImportRefactorer#deleteFile(int)
	 */
	@Override
	public void deletePath(MultiUndoOp multiOp, int pathId, boolean alsoDeleteAction) 
			throws CanNotRefactorException {

		/* the path must not be a non-empty directory - otherwise give an error */
		PathType pathType = fileMgr.getPathType(pathId);
		if (pathType == PathType.TYPE_DIR) {
			if (fileMgr.getChildPaths(pathId).length != 0) {
				throw new CanNotRefactorException(Cause.DIRECTORY_NOT_EMPTY, new Integer[] { pathId });
			}
		}

		/* the helper does most of the work */
		ImportRefactorerUtils.deletePathHelper(buildStore, pathId, alsoDeleteAction, multiOp);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.refactor.IImportRefactorer#deleteFileTree(int)
	 */
	@Override
	public void deletePathTree(MultiUndoOp multiOp, int dirId, boolean alsoDeleteActions)
			throws CanNotRefactorException {

		/* the helper does most of the work */
		ImportRefactorerUtils.deletePathTreeHelper(buildStore, dirId, alsoDeleteActions, multiOp);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.refactor.IImportRefactorer#makeActionAtomic(int)
	 */
	@Override
	public void makeActionAtomic(MultiUndoOp multiOp, int actionId) throws CanNotRefactorException {
		
		/* if the action has been trashed, that's a problem */
		if (actionMgr.isActionTrashed(actionId)) {
			throw new CanNotRefactorException(Cause.ACTION_IS_TRASHED, new Integer[] { actionId });
		}
		
		/* if this action has no children, it's already atomic */
		Integer children[] = actionMgr.getChildren(actionId);
		if (children.length == 0) {
			return;
		}
		
		/* figure out the complete list of actions that we'll be merging */
		List<Integer> actionsToMerge = new ArrayList<Integer>();
		ImportRefactorerUtils.collectChildren(actionMgr, actionId, actionsToMerge);
		
		/* obtain the complete list of file access across those actions */
		FileAccess[] fileAccesses = 
				actionMgr.getSequencedFileAccesses(actionsToMerge.toArray(new Integer[0]));
		
		/* schedule all existing file accesses for removal */
		for (FileAccess fileAccess : fileAccesses) {
			ActionUndoOp actionOp = new ActionUndoOp(buildStore, fileAccess.actionId);
			actionOp.recordRemovePathAccess(fileAccess.seqno, fileAccess.pathId, fileAccess.opType);
			multiOp.add(actionOp);
		}
		
		/* schedule those same file access to be performed by the parent action */
		for (FileAccess fileAccess : fileAccesses) {
			ActionUndoOp actionOp = new ActionUndoOp(buildStore, actionId);
			actionOp.recordAddPathAccess(fileAccess.seqno, fileAccess.pathId, fileAccess.opType);
			multiOp.add(actionOp);
		}
		
		/* move the child actions to the trash */
		for (int childActionId : actionsToMerge) {
			if (childActionId != actionId) {
				ActionUndoOp actionOp = new ActionUndoOp(buildStore, childActionId);
				actionOp.recordMoveToTrash();
				multiOp.add(actionOp);
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.refactor.IImportRefactorer#deleteAction(int)
	 */
	@Override
	public void deleteAction(MultiUndoOp multiOp, int actionId) throws CanNotRefactorException {
		
		/* check that we're not trying to delete an invalid action, or the root action */
		int parentActionId = actionMgr.getParent(actionId);
		Integer actions[] = new Integer[] { actionId };
		if ((parentActionId == ErrorCode.BAD_VALUE) || 
			(parentActionId == ErrorCode.NOT_FOUND)) {
			throw new CanNotRefactorException(Cause.INVALID_ACTION, actions);
		}
		
		/* 
		 * Check that the action to be deleted is not "in use". This will throw an
		 * exception if it's in use.
		 */
		ImportRefactorerUtils.validateActionsNotInUse(actionMgr, actions);

		/* re-parent all of the immediate child actions */
		Integer childActions[] = actionMgr.getChildren(actionId);
		for (int childActionId : childActions) {
			ActionUndoOp actionOp = new ActionUndoOp(buildStore, childActionId);
			actionOp.recordParentChange(actionId, parentActionId);
			multiOp.add(actionOp);
		}
		
		/* schedule the action to be trashed (along with file-access links) */
		ImportRefactorerUtils.scheduleRemoveAction(buildStore, multiOp, actions);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.refactor.IImportRefactorer#mergeActions(int[])
	 */
	@Override
	public void mergeActions(MultiUndoOp multiOp, ActionSet actionIds) throws CanNotRefactorException {
		
		/* check that all actions are valid and non-atomic */
		List<Integer> invalidActions = new ArrayList<Integer>();
		List<Integer> trashedActions = new ArrayList<Integer>();
		List<Integer> nonAtomicActions = new ArrayList<Integer>();
		SortedSet<Integer> actionIdSet = new TreeSet<Integer>();
		
		for (int actionId : actionIds) {		
			if (!actionMgr.isActionValid(actionId)) {
				invalidActions.add(actionId); 
			}
			else if (actionMgr.isActionTrashed(actionId)) {
				trashedActions.add(actionId); 
			}
			else if (actionMgr.getChildren(actionId).length != 0) {
				nonAtomicActions.add(actionId);
			}
			actionIdSet.add(actionId);
		}
		
		if (invalidActions.size() != 0) {
			throw new CanNotRefactorException(
					Cause.INVALID_ACTION, invalidActions.toArray(new Integer[0]));
		}
				
		if (trashedActions.size() != 0) {
			throw new CanNotRefactorException(
					Cause.ACTION_IS_TRASHED, trashedActions.toArray(new Integer[0]));
		}
				
		if (nonAtomicActions.size() != 0) {
			throw new CanNotRefactorException(
					Cause.ACTION_NOT_ATOMIC, nonAtomicActions.toArray(new Integer[0]));
		}		
		
		/* if there's only one action (or no actions), there's nothing to do */
		if (actionIds.size() < 2) {
			return;
		}
		
		/* obtain a sorted array of actions, with duplicates removed */
		Integer actionsArray[] = actionIdSet.toArray(new Integer[actionIdSet.size()]);
		int firstActionId = actionsArray[0];

		/* 
		 * Foreach action, schedule the file-access links for removal. This will ensure
		 * that the file-access links can be recovered after an "undo", even if some
		 * of the links are dissolved due to merging the actions (i.e. temporary files
		 * being dissolved).
		 */
		FileAccess[] fileAccesses = actionMgr.getSequencedFileAccesses(actionsArray);
		for (FileAccess fileAccess : fileAccesses) {
			ActionUndoOp actionOp = new ActionUndoOp(buildStore, fileAccess.actionId);
			actionOp.recordRemovePathAccess(fileAccess.seqno, fileAccess.pathId, fileAccess.opType);
			multiOp.add(actionOp);
		}
		
		/*
		 * Schedule all the file-access links to be added to the first action. This may
		 * cause some of the links to be dissolved (if they're repetitions, or if they
		 * cancel each other out).
		 */
		for (FileAccess fileAccess : fileAccesses) {
			ActionUndoOp actionOp = new ActionUndoOp(buildStore, firstActionId);
			actionOp.recordAddPathAccess(fileAccess.seqno, fileAccess.pathId, fileAccess.opType);
			multiOp.add(actionOp);
		}
		
		/*
		 * Schedule the shell command for the first action to be the concatenation of
		 * all shell commands, across all merged actions.
		 */
		StringBuilder newShellCommand = new StringBuilder();
		String firstActionCommand = null;
		for (int i = 0; i < actionsArray.length; i++) {
			String actionCommand = actionMgr.getCommand(actionsArray[i]);
			if (i == 0) {
				firstActionCommand = actionCommand;
			} else {
				/* insert \n between merged commands */
				newShellCommand.append('\n');
			}			
			newShellCommand.append(actionCommand);
		}
		ActionUndoOp actionOp = new ActionUndoOp(buildStore, firstActionId);
		actionOp.recordCommandChange(firstActionCommand, newShellCommand.toString());
		multiOp.add(actionOp);
		
		/*
		 * Schedule all actions to be trashed, except for the first.
		 */
		for (int i = 1; i < actionsArray.length; i++) {
			ActionUndoOp deleteActionOp = new ActionUndoOp(buildStore, actionsArray[i]);
			deleteActionOp.recordMoveToTrash();
			multiOp.add(deleteActionOp);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.refactor.IImportRefactorer#moveMembersToPackage()
	 */
	@Override
	public void moveMembersToPackage(int destPkgId, List<MemberDesc> members) 
			throws CanNotRefactorException {

		/** Validate the destination package ID */
		if (!pkgMgr.isValid(destPkgId)) {
			throw new CanNotRefactorException(Cause.INVALID_PACKAGE, destPkgId);
		}

		/* validate inputs - this will throw an exception if there's an error */
		MoveRefactorerUtils.validateMembersList(buildStore, members);

		System.out.println("Moving the following members into package " + destPkgId);
		for (MemberDesc member : members) {
			System.out.println("Type: " + member.memberType + " value " + member.memberId);
		}
		
		/* 
		 * Any files that aren't generated by an action should be converted into
		 * a single file group. They'll also be removed from the list of members.
		 */
		MoveRefactorerUtils.convertLooseFilesToFileGroup(buildStore, destPkgId, members);
		
		// TODO: validate that all members are already in the same package.
		// They could be any of FILE, FILE_GROUP, ACTION, SUB_PACKAGE.
		// Initially, focus on moving things from import.
		// Note: this method does not focus on layout, but the UI layer should call
		// this method, then call layout as a multiOp.
			
	}
	
	/*-------------------------------------------------------------------------------------*/

	
}
