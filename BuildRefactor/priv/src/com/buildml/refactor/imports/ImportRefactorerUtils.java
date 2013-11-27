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
import com.buildml.model.IActionMgr.FileAccess;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IFileMgr.PathType;
import com.buildml.model.undo.ActionUndoOp;
import com.buildml.model.undo.FileUndoOp;
import com.buildml.model.undo.MultiUndoOp;
import com.buildml.refactor.CanNotRefactorException;
import com.buildml.refactor.CanNotRefactorException.Cause;

/**
 * A class of static utility methods to support commands in ImportRefactorer().
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class ImportRefactorerUtils {

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Collect together a list of all an action's child action IDs. For complex actions, this
	 * may involve traversing multiple levels of the action tree.
	 * 
	 * @param actionMgr The action mgr that owns the actions.
	 * @param actionId The parent action ID.
	 * @param actionsToMerge A list to which the action's descendents will be added.
	 */
	/* package */ static void collectChildren(IActionMgr actionMgr, 
												int actionId, List<Integer> actionsToMerge) {		
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
	 * @param actionMgr The IActionMgr we should query from.
	 * @param actions The array of actions that we're testing.
	 * @throws CanNotRefactorException If one or more actions is still in use.
	 */
	/* package */ static void validateActionsNotInUse(IActionMgr actionMgr, Integer[] actions)
				throws CanNotRefactorException {
		
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
	 * @param buildStore The IBuildStore we can query for information.
	 * @param multiOp    The undo/redo operation that we'll use for scheduling.
	 * @param actions    The actions to be removed.
	 */
	/* package */ static void scheduleRemoveAction(IBuildStore buildStore,
											MultiUndoOp multiOp, Integer[] actions) {
		
		IActionMgr actionMgr = buildStore.getActionMgr();
		
		/* in addition to removing actions, we also need to remove generated files */
		List<Integer> writtenFilesToRemove = new ArrayList<Integer>();	
			
		/* 
		 * Remove links between this action and all paths it accesses. We need information on
		 * which operation is used to access the path, so we break this out.
		 */
		FileAccess[] fileAccesses = actionMgr.getSequencedFileAccesses(actions);
		for (FileAccess fileAccess : fileAccesses) {
			ActionUndoOp actionOp = new ActionUndoOp(buildStore, fileAccess.actionId);
			actionOp.recordRemovePathAccess(fileAccess.seqno, fileAccess.pathId, fileAccess.opType);
			multiOp.add(actionOp);
			if (fileAccess.opType == OperationType.OP_WRITE){
				writtenFilesToRemove.add(fileAccess.pathId);
			}
		}
			
		/* Move the actions into the trash.*/
		for (int actionId : actions) {
			ActionUndoOp actionOp = new ActionUndoOp(buildStore, actionId);
			actionOp.recordMoveToTrash();
			multiOp.add(actionOp);
		}

		/* remove all written files - we can only do this once all action-path links are removed. */
		for (int writtenPathId : writtenFilesToRemove) {
			FileUndoOp fileOp = new FileUndoOp(buildStore, writtenPathId);
			fileOp.recordRemovePath();
			multiOp.add(fileOp);
		}
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * A helper function, shared by deletePath() and deletePathTree().
	 * 
	 * @param buildStore		The IBuildStore to query/update.
	 * @param pathId			The path to be deleted.
	 * @param alsoDeleteAction  True if we should also delete actions that generate the path.
	 * @param multiOp			Undo/redo history item to add operation steps to.
	 * @throws CanNotRefactorException Something went wrong.
	 */
	/* package */ static void deletePathHelper(IBuildStore buildStore,
				int pathId, boolean alsoDeleteAction, MultiUndoOp multiOp)
			throws CanNotRefactorException {
		
		IActionMgr actionMgr = buildStore.getActionMgr();
		IFileMgr fileMgr = buildStore.getFileMgr();
		
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
		ImportRefactorerUtils.validateActionsNotInUse(actionMgr, actionsUsingPath);
		
		/*
		 * All is good, now go ahead and start deleting things. We can now build up a history item
		 * of the changes to be made to the BuildStore.
		 */

		/* remove the actions, and any associated file accesses */
		ImportRefactorerUtils.scheduleRemoveAction(buildStore, multiOp, actionsUsingPath);
		
		/* 
		 * If we didn't already delete the action (and the paths it generates), we need to
		 * explicitly delete it (this is the case where it's an unused path we're deleting).
		 */
		if (actionsUsingPath.length == 0) {
			FileUndoOp fileOp = new FileUndoOp(buildStore, pathId);
			fileOp.recordRemovePath();
			multiOp.add(fileOp);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * A helper method for deletePathTree(). Performs a bottom up traversal of a directory
	 * hierarchy.
	 * 
	 * @param buildStore		 The IBuildStore to query/update.
	 * @param pathId			 The path (file or directory) to be deleted.
	 * @param alsoDeleteActions  True if we should also delete actions that generate the path.
	 * @param multiOp			 Undo/redo history item to add operation steps to.
	 * @throws CanNotRefactorException 
	 */
	/* package */ static void deletePathTreeHelper(IBuildStore buildStore,
				int pathId, boolean alsoDeleteActions, MultiUndoOp multiOp) 
							throws CanNotRefactorException {
		
		IActionMgr actionMgr = buildStore.getActionMgr();
		IFileMgr fileMgr = buildStore.getFileMgr();
		
		/* delete children first */
		Integer children[] = fileMgr.getChildPaths(pathId);
		for (int childId : children) {
			deletePathTreeHelper(buildStore, childId, alsoDeleteActions, multiOp);
		}
		
		/* now delete the current path */
		deletePathHelper(buildStore, pathId, alsoDeleteActions, multiOp);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
}
