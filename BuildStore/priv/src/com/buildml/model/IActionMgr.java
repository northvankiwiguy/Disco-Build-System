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

package com.buildml.model;

/**
 * The interface conformed-to by any ActionMgr object, which represents a
 * subset of the functionality managed by a BuildStore object. An ActionMgr
 * deals with all information related to BuildML "actions".
 * <p>
 * There should be exactly one ActionMgr object per BuildStore object. Use the
 * BuildStore's getActionMgr() method to obtain that one instance.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface IActionMgr {

	/**
	 * The maximum number of actions that this ActionMgr object can handle.
	 */
	public static final int MAX_ACTIONS = 16777216;

	/** Data type for specifying the type of a file access that an action performs. */
	public enum OperationType {
		/** An unspecified operation for when we don't care which operation is performed. */
		OP_UNSPECIFIED,		
		
		/** The file was read by the action. */
		OP_READ,
		
		/** The file was written by the action. */
		OP_WRITE,
		
		/** The file was read and written by the same action. */
		OP_MODIFIED,
		
		/** The file was deleted by the action. */
		OP_DELETE
	};
	
	/**
	 * Add a new build action, returning the new action ID number.
	 * 
	 * @param parentActionId The ID of the new action's parent.
	 * @param actionDirId The ID of the path (a directory) in which this 
	 *                    action was executed.
	 * @param command The shell command associated with this action.
	 * @return The new action's ID.
	 */
	public abstract int addAction(int parentActionId, int actionDirId,
			String command);

	/**
	 * Record the fact that the specific build action accessed the specified file. 
	 * Adding the same relationship a second or successive time has no effect.
	 * 
	 * @param buildActionId The ID of the build action that accessed the file.
	 * @param fileNumber The file's ID number.
	 * @param newOperation How the action accessed the file (read, write, delete, etc).
	 */
	public abstract void addFileAccess(int buildActionId, int fileNumber,
			OperationType newOperation);

	/**
	 * Return an array of files that were accessed by this build action.
	 * 
	 * @param actionId The build action that accessed the files.
	 * @param operation The type of operation we're interested in (such as OP_READ,
	 *    OP_WRITE, or OP_UNSPECIFIED if you don't care).
	 * @return An array of file IDs.
	 */
	public abstract Integer[] getFilesAccessed(int actionId,
			OperationType operation);

	/**
	 * Return an array of actions that accessed a specific file.
	 * 
	 * @param fileId The file we're interested in querying for.
	 * @param operation The operation that the actions perform on this file (such as OP_READ,
	 *    OP_WRITE, or OP_UNSPECIFIED if you don't care).
	 * @return An array of IDs of actions that access this file.
	 */
	public abstract Integer[] getActionsThatAccess(int fileId,
			OperationType operation);

	/**
	 * Return the list of all actions that execute within the file system directory 
	 * specified by pathId.
	 * @param pathId The directory in which the actions must be executed.
	 * @return The list of actions.
	 */
	public abstract Integer[] getActionsInDirectory(int pathId);

	/**
	 * Fetch a build action's command line string.
	 * 
	 * @param actionId The action we're querying.
	 * @return The action's command line string.
	 */
	public abstract String getCommand(int actionId);

	/**
	 * Fetch a summary of this action's command. The summary string is designed to give a
	 * high-level overview of what the command does. The summary string for certain commands
	 * may contain the command name and most important parameters, whereas for other commands
	 * it may just be the first 'width' characters of the shell command.
	 * 
	 * @param actionId The ID of the action.
	 * @param width The maximum number of characters in the summary string.
	 * @return The summary string for this action's command.
	 */
	public abstract String getCommandSummary(int actionId, int width);

	/**
	 * Given the ID of a action, return the ID of the action's parent.
	 * 
	 * @param actionId The action to return the parent of.
	 * @return The ID of the action's parent, or NOT_FOUND if the action is at the root, or
	 * BAD_VALUE if the action ID is invalid.
	 */
	public abstract int getParent(int actionId);

	/**
	 * Return the path ID of the directory in which this action was executed.
	 * 
	 * @param actionId The ID of the action.
	 * @return The path ID of the directory in which this action was executed.
	 */
	public abstract int getDirectory(int actionId);

	/**
	 * Given the ID of an action, return an array of the action's children (possibly empty).
	 * 
	 * @param actionId The parent action of the children to be returned.
	 * @return An array of child action IDs (in no particular order). Or the empty array if there
	 * are no children.
	 */
	public abstract Integer[] getChildren(int actionId);

	/**
	 * Return the ID of the action with the associated root name.
	 * 
	 * @param rootName The name of the root, which is attached to an action.
	 * @return The root action's ID.
	 */
	public abstract int getRootAction(String rootName);

	/**
	 * Return the BuildStore object that owns this IActionMgr object.
	 *
	 * @return The BuildStore object that owns this IActionMgr object.
	 */
	public abstract IBuildStore getBuildStore();

}