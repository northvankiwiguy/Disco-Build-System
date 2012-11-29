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

import com.buildml.model.types.ActionSet;

/**
 * Various refactoring operations that can be performed on an imported BuildStore.
 * That is, when we use a build scanner to import a legacy build, it's often necessary
 * to clean up the imported information. This includes deleting unused files and
 * actions, and merging multiple actions into a single action.
 * 
 * This class sits on top of the standard BuildStore infrastructure and provides
 * an intelligence layer that determines the correct sequence of adds/deletes/moves.
 * This code ensures that consistency rules are maintained and that the refactoring
 * operations can always be undone and redone as requested by the user.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface IImportRefactorer {
	
	/**
	 * Delete a path from the BuildStore. A path can only be deleted under the following
	 * situations:
	 *   - The path exists and has not already been deleted.
	 *   - The path is not a non-empty directory.
	 *   - The path is not being used as input to one or more actions.
	 *   - The path is not generated by an action (unless alsoDeleteAction is true)
	 *   - The path is generated by an atomic action.
	 *   - The path is not generated by an action that also generates other files that
	 *     are still in use (the action can't be deleted).
	 * 
	 * @param pathId The path (file/directory) to be deleted.
	 * @param alsoDeleteAction If true, also proceed to delete the action(s) that generate
	 * this file.
	 * @throws CanNotRefactorException If the refactoring can not proceed for some reason.
	 */
	public void deletePath(int pathId, boolean alsoDeleteAction)
			throws CanNotRefactorException;
	
	/**
	 * @param dirId
	 * @param alsoDeleteActions 
	 * @throws CanNotRefactorException
	 */
	public void deletePathTree(int dirId, boolean alsoDeleteActions)
			throws CanNotRefactorException;
	
	/**
	 * Modify a build action so that it no longer has child actions, yet has the same
	 * effect as the original action hierarchy. For many tools, a process may create
	 * additional sub-processes to complete the job. If we are not interested in
	 * knowing about these sub-processes, the parent action can be made "atomic".
	 * 
	 * @param actionId ID of the action to be made atomic.
	 * @throws CanNotRefactorException If the refactoring can not proceed for some reason.
	 */
	public void makeActionAtomic(int actionId) throws CanNotRefactorException;
	
	/**
	 * @param actionId
	 * @throws CanNotRefactorException
	 */
	public void deleteAction(int actionId) throws CanNotRefactorException;
	
	/**
	 * Merge two or more actions into a single action, with the shell commands
	 * from each action merged into a single shell command. The actions must
	 * all be atomic.
	 * 
	 * @param actionIds The actions to be merged.
	 * @throws CanNotRefactorException If the refactoring can not proceed for some reason.
	 */
	public void mergeActions(ActionSet actionIds) throws CanNotRefactorException;
	
	/**
	 * Undo the previous refactoring operation. Operations are held on a stack,
	 * so calling this method multiple times will undo multiple refactorings.
	 * 
	 * @throws CanNotRefactorException For some reason, the refactoring could not
	 * be undone.
	 */
	public void undoRefactoring() throws CanNotRefactorException;
	
	/**
	 * Redo the previous refactoring operation that has just been undone by the
	 * undoRefactoring() method. Operations are held on a stack, so calling this
	 * method multiple times can redo multiple operations that had previously
	 * been undone.
	 * 
	 * @throws CanNotRefactorException For some reason, the refactoring could not
	 * be redone.
	 */
	public void redoRefactoring() throws CanNotRefactorException;
}
