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
import java.util.Stack;

import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IActionMgr.FileAccess;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.IFileMgr.PathType;
import com.buildml.refactor.CanNotRefactorException;
import com.buildml.refactor.CanNotRefactorException.Cause;
import com.buildml.refactor.IImportRefactorer;
import com.buildml.refactor.imports.ImportHistoryItem.ItemOpType;
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
	
	/** The stack of history operations that have been executed, and can now be undone. */
	private Stack<ImportHistoryItem> undoStack;

	/** The stack of history operations that have been undone, and can be redone. */
	private Stack<ImportHistoryItem> redoStack;
	
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
		
		undoStack = new Stack<ImportHistoryItem>();
		redoStack = new Stack<ImportHistoryItem>();
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.refactor.IImportRefactorer#deleteFile(int)
	 */
	@Override
	public void deletePath(int pathId, boolean alsoDeleteAction) 
			throws CanNotRefactorException {

		/* the path must not be a non-empty directory - otherwise give an error */
		PathType pathType = fileMgr.getPathType(pathId);
		if (pathType == PathType.TYPE_DIR) {
			if (fileMgr.getChildPaths(pathId).length != 0) {
				throw new CanNotRefactorException(Cause.DIRECTORY_NOT_EMPTY, new Integer[] { pathId });
			}
		}
		
		/* we'll add delete operation steps to this operation */
		ImportHistoryItem historyItem = new ImportHistoryItem(buildStore);

		/* the helper does most of the work */
		deletePathHelper(pathId, alsoDeleteAction, historyItem);
		
		/* success */
		invokeHistoryItem(historyItem);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.refactor.IImportRefactorer#deleteFileTree(int)
	 */
	@Override
	public void deletePathTree(int dirId, boolean alsoDeleteActions)
			throws CanNotRefactorException {

		/* we'll add delete operation steps to this operation */
		ImportHistoryItem historyItem = new ImportHistoryItem(buildStore);

		/* the helper does most of the work */
		deletePathTreeHelper(dirId, alsoDeleteActions, historyItem);
		
		/* success */
		invokeHistoryItem(historyItem);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.refactor.IImportRefactorer#makeActionAtomic(int)
	 */
	@Override
	public void makeActionAtomic(int actionId) throws CanNotRefactorException {
		
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
		collectChildren(actionMgr, actionId, actionsToMerge);
		
		/* obtain the complete list of file access across those actions */
		FileAccess[] fileAccesses = 
				actionMgr.getSequencedFileAccesses(actionsToMerge.toArray(new Integer[0]));
		
		/* schedule all existing file accesses for removal */
		ImportHistoryItem historyItem = new ImportHistoryItem(buildStore);
		for (FileAccess fileAccess : fileAccesses) {
			historyItem.addPathAccessOp(ItemOpType.REMOVE_ACTION_PATH_LINK, 
					fileAccess.seqno, fileAccess.actionId, 
					fileAccess.pathId, fileAccess.opType);
		}
		
		/* schedule those same file access to be performed by the parent action */
		for (FileAccess fileAccess : fileAccesses) {
			historyItem.addPathAccessOp(ItemOpType.ADD_ACTION_PATH_LINK, 
					fileAccess.seqno, actionId, fileAccess.pathId, fileAccess.opType);
		}
		
		/* move the child actions to the trash */
		for (int childActionId : actionsToMerge) {
			if (childActionId != actionId) {
				historyItem.addActionOp(ItemOpType.REMOVE_ACTION, childActionId);
			}
		}
		
		/* success */
		invokeHistoryItem(historyItem);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.refactor.IImportRefactorer#deleteAction(int)
	 */
	@Override
	public void deleteAction(int actionId) throws CanNotRefactorException {
		
		
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
		validateActionsNotInUse(actions);

		/* schedule the operation steps to happen. */
		ImportHistoryItem historyItem = new ImportHistoryItem(buildStore);

		/* re-parent all of the immediate child actions */
		Integer childActions[] = actionMgr.getChildren(actionId);
		for (int childActionId : childActions) {
			historyItem.addReparentOp(
					ItemOpType.REPARENT_ACTION, childActionId, actionId, parentActionId);
		}
		
		/* schedule the action to be trashed (along with file-access links) */
		scheduleRemoveAction(historyItem, actions);
		
		/* success - make it all happen */
		invokeHistoryItem(historyItem);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.refactor.IImportRefactorer#deleteActionSubTree(int)
	 */
	@Override
	public void deleteActionTree(int actionId)
			throws CanNotRefactorException {

		// Mark start of history record
		
		// Do a bottom-up traversal of the action's children.
		// Invoke deleteAction() on each children.
		// On error, roll back history and return an error.
		
		// Mark end of history record
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.refactor.IImportRefactorer#mergeActions(int[])
	 */
	@Override
	public void mergeActions(int[] actionIds) throws CanNotRefactorException {
		
		// check that there are multiple actionId specified (else, return success).
		// check that all actions have the same parent (else, return error).
		// check that all actions are atomic (else, return error).
		
		// clone the first action, and mark the original as garbage.
		// for each remaining action:
		//     merge the shell command into the cloned action.
		//     add the action's file access to the cloned action.
		//     mark the action as garbage.
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.refactor.IImportRefactorer#undoRefactoring()
	 */
	@Override
	public void undoRefactoring() throws CanNotRefactorException {
		
		/* if there's something we can undo... */
		if (!undoStack.isEmpty()) {

			// System.out.println("UNDO:");

			/* undo all the operations in this history item */
			ImportHistoryItem item = undoStack.pop();
			item.undo();
			
			/* add it to redo stack, so we can "undo the undo" */
			redoStack.push(item);
		}
		
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.refactor.IImportRefactorer#redoRefactoring()
	 */
	@Override
	public void redoRefactoring() throws CanNotRefactorException {
		
		/* if there is something to redo... */
		if (!redoStack.isEmpty()) {
			
			// System.out.println("REDO:");
			
			/* redo all the operations in this history item */
			ImportHistoryItem item = redoStack.pop();
			item.redo();
			
			/* push back on top of the undo stack */
			undoStack.push(item);
		}
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Given a recently created history item, invoke all the operations in that item,
	 * then add the item to our history stack.
	 * 
	 * @param item The history item to be invoked, then added to the stack.
	 */
	private void invokeHistoryItem(ImportHistoryItem item) {
		
		/* perform the item, then add it to our undo stack */
		item.redo();
		undoStack.push(item);
		
		/* this operation invalidates everything on the redo stack */
		redoStack.clear();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Collect together a list of all an action's child action IDs. For complex actions, this
	 * may involve traversing multiple levels of the action tree.
	 * 
	 * @param actionMgr The action mgr that owns the actions.
	 * @param actionId The parent action ID.
	 * @param actionsToMerge A list to which the action's descendents will be added.
	 */
	private void collectChildren(IActionMgr actionMgr, int actionId, List<Integer> actionsToMerge) {		
		Integer children[] = actionMgr.getChildren(actionId);
		for (int childActionId : children) {
			collectChildren(actionMgr, childActionId, actionsToMerge);
		}
		actionsToMerge.add(actionId);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Helper method for validating whether an action (or array of actions) is in use
	 * by downstream files. That is, these actions generate files which may then be used
	 * as input into other actions. This function returns silently if the actions are NOT
	 * in use, or throws an exception if one or more generated files are in used.
	 * 
	 * @param actions The array of actions that we're testing.
	 * @throws CanNotRefactorException If one or more actions is still in use.
	 */
	private void validateActionsNotInUse(Integer[] actions)	throws CanNotRefactorException {
		
		List<Integer> allPathsInUse = new ArrayList<Integer>();
		for (int actionId : actions) {
			
			/* For each path that this action generates... */
			Integer filesWrittenByAction[] = actionMgr.getFilesAccessed(actionId, OperationType.OP_WRITE);
			for (int writtenPathId : filesWrittenByAction) {
				
				/* are there other actions (other than us) that use this generated path? */
				// TODO: this should really look at the action IDs, rather than just the length.
				if (actionMgr.getActionsThatAccess(writtenPathId, OperationType.OP_UNSPECIFIED).length != 
						actions.length) {
					allPathsInUse.add(writtenPathId);
				}
			}
		}
		if (allPathsInUse.size() != 0) {
			throw new CanNotRefactorException(Cause.ACTION_IN_USE, allPathsInUse.toArray(new Integer[0]));
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper method for scheduling an action to be removed. This must also remove any
	 * file-access links that are currently associated with the action.
	 *  
	 * @param historyItem The undo/redo operation that we'll use for scheduling.
	 * @param actions The actions to be removed.
	 */
	private void scheduleRemoveAction(ImportHistoryItem historyItem, Integer[] actions) {
		
		/* in addition to removing actions, we also need to remove generated files */
		List<Integer> writtenFilesToRemove = new ArrayList<Integer>();	
			
		/* 
		 * Remove links between this action and all paths it accesses. We need information on
		 * which operation is used to access the path, so we break this out.
		 */
		FileAccess[] fileAccesses = actionMgr.getSequencedFileAccesses(actions);
		for (FileAccess fileAccess : fileAccesses) {
			historyItem.addPathAccessOp(ItemOpType.REMOVE_ACTION_PATH_LINK, fileAccess.seqno, 
					fileAccess.actionId, fileAccess.pathId, fileAccess.opType);
			if (fileAccess.opType == OperationType.OP_WRITE){
				writtenFilesToRemove.add(fileAccess.pathId);
			}
		}
			
		/* Move the actions into the trash.*/
		for (int actionId : actions) {
			historyItem.addActionOp(ItemOpType.REMOVE_ACTION, actionId);
		}

		/* remove all written files - we can only do this once all action-path links are removed. */
		for (int writtenPathId : writtenFilesToRemove) {
			historyItem.addPathOp(ItemOpType.REMOVE_PATH, writtenPathId);			
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * A helper function, shared by deletePath() and deletePathTree().
	 * 
	 * @param pathId			The path to be deleted.
	 * @param alsoDeleteAction  True if we should also delete actions that generate the path.
	 * @param historyItem		Undo/redo history item to add operation steps to.
	 * @throws CanNotRefactorException Something went wrong.
	 */
	private void deletePathHelper(int pathId, boolean alsoDeleteAction, ImportHistoryItem historyItem)
			throws CanNotRefactorException {
		
		/* the path must exist and must not be trashed - otherwise give an error */
		PathType pathType = fileMgr.getPathType(pathId);
		if ((pathType == PathType.TYPE_INVALID) ||
			(fileMgr.isPathTrashed(pathId))) {
			throw new CanNotRefactorException(Cause.INVALID_PATH, pathId);
		}		
		
		/* the path must not currently be used as input to an action - otherwise give an error */
		Integer actionsReadingPath[] = actionMgr.getActionsThatAccess(pathId, OperationType.OP_READ);
		if (actionsReadingPath.length != 0) {
			throw new CanNotRefactorException(Cause.PATH_IN_USE, actionsReadingPath);
		}

		/* the path must not be the "current directory" for any actions */
		Integer actionsExecutingInDir[] = actionMgr.getActionsInDirectory(pathId);
		if (actionsExecutingInDir.length != 0) {
			throw new CanNotRefactorException(Cause.DIRECTORY_CONTAINS_ACTIONS, actionsExecutingInDir);
		}
		
		/* 
		 * If there are actions that generate (not READ) this path, then we must delete those
		 * actions too. However, only delete them if "alsoDeleteActions" is set.
		 */
		Integer actionsUsingPath[] = actionMgr.getActionsThatAccess(pathId, OperationType.OP_UNSPECIFIED);
		if ((actionsUsingPath.length != 0) && !alsoDeleteAction) {
			throw new CanNotRefactorException(Cause.PATH_IS_GENERATED, actionsUsingPath);
		}
		
		/*
		 * Make sure that all the actions that generate the path are atomic.
		 */
		for (int actionId: actionsUsingPath) {
			if (actionMgr.getChildren(actionId).length != 0) {
				throw new CanNotRefactorException(Cause.ACTION_NOT_ATOMIC, actionId);
			}
		}
		
		/*
		 * For the action (or actions) that we'll now need to delete, check to see if any of their generated
		 * paths are used as input into other actions. If so, the output paths are considered "in use".
		 * Calling this method will throw a CanNotRefactorException if anything goes wrong.
		 */
		validateActionsNotInUse(actionsUsingPath);
		
		/*
		 * All is good, now go ahead and start deleting things. We can now build up a history item
		 * of the changes to be made to the BuildStore.
		 */

		/* remove the actions, and any associated file accesses */
		scheduleRemoveAction(historyItem, actionsUsingPath);
		
		/* 
		 * If we didn't already delete the action (and the paths it generates), we need to
		 * explicitly delete it (this is the case where it's an unused path we're deleting).
		 */
		if (actionsUsingPath.length == 0) {
			historyItem.addPathOp(ItemOpType.REMOVE_PATH, pathId);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * A helper method for deletePathTree(). Performs a bottom up traversal of a directory
	 * hierarchy.
	 * 
	 * @param pathId			 The path (file or directory) to be deleted.
	 * @param alsoDeleteActions  True if we should also delete actions that generate the path.
	 * @param historyItem		 Undo/redo history item to add operation steps to.
	 * @throws CanNotRefactorException 
	 */
	private void deletePathTreeHelper(int pathId, boolean alsoDeleteActions, 
									  ImportHistoryItem historyItem) 
							throws CanNotRefactorException {
		
		/* delete children first */
		Integer children[] = fileMgr.getChildPaths(pathId);
		for (int childId : children) {
			deletePathTreeHelper(childId, alsoDeleteActions, historyItem);
		}
		
		/* now delete the current path */
		deletePathHelper(pathId, alsoDeleteActions, historyItem);
	}
	
	/*-------------------------------------------------------------------------------------*/
}
