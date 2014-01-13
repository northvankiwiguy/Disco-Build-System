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

	/** Standard slot number for the "Input" slot */
	public static final int INPUT_SLOT_ID = 1;

	/** Standard slot number for the "Command" slot */
	public static final int COMMAND_SLOT_ID = 2;

	/** Standard slot number for the "Directory" slot */
	public static final int DIRECTORY_SLOT_ID = 3;

	/** Standard slot number for the "Output" slot */
	public static final int OUTPUT_SLOT_ID = 4;

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
	 * A structure, returned by getSequencedFileAccesses, describing an individual
	 * file access.
	 */
	public class FileAccess {
		/** The globally-unique sequence number of this access */
		public int seqno;
		
		/** The action that accessed the file */
		public int actionId;
		
		/** The file that has been accessed */
		public int pathId;
		
		/** The mode in which the file was access (read, write, etc) */
		public OperationType opType;
	}
	
	/**
	 * Add a new action, of the specified type, returning the new action ID number.
	 * 
	 * @param actionTypeId The ID of the action's type.
	 * @return The new action's ID, or ErrorCode.NOT_FOUND if actionTypeId is invalid
	 * or is the ID for an action type folder.
	 */
	public abstract int addAction(int actionTypeId);
	
	/**
	 * Add a new build action of the "Shell Command" type, returning the new 
	 * action ID number. This method is most commonly used when importing a
	 * legacy build, rather than when manually creating an action.
	 * 
	 * @param parentActionId The ID of the new action's parent.
	 * @param actionDirId The ID of the path (a directory) in which this 
	 *                    action was executed.
	 * @param command The shell command associated with this action.
	 * @return The new action's ID.
	 */
	public abstract int addShellCommandAction(int parentActionId, int actionDirId,
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
	 * Record the fact that the specific build action accessed the specified file. 
	 * This is similar to addFileAccess(), but allows specific control over the
	 * sequence number of the operation. Instead of adding the file access information
	 * to the end of the list, it can be inserted at an arbitrary location.
	 * 
	 * This method should primarily be used when undoing a deletion operation. Normally,
	 * file accesses should be added in the order they're performed, by calling
	 * addFileAccess().
	 * 
	 * @param seqno The sequence position in which the file access will be added.
	 * @param actionId The ID of the build action that accessed the file.
	 * @param fileNumber The file's ID number.
	 * @param newOperation How the action accessed the file (read, write, delete, etc).
	 * @return ErrorCode.OK on success, or ErrorCode.ONLY_ONE_ALLOWED if there's
	 * already a file-access operation at this sequence number.
	 */
	public abstract int addSequencedFileAccess(int seqno, int actionId, 
			int fileNumber,	OperationType newOperation);
	
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
	 * Return an array of files that were access by this set of actions, including
	 * details of the sequence order (in which the accesses occurred) and the type of
	 * operation that occurred.
	 * 
	 * @param actionIds An array of the IDs of actions to query.
	 * @return An ordered array of file access information.
	 */
	public abstract FileAccess[] getSequencedFileAccesses(Integer actionIds[]);

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
	 * Remove all file-access information between the specified action and path. If there
	 * is no pre-existing relationship between the action and path, no change is made.
	 * No checking is done to ensure that the action and path are defined.
	 * 
	 * @param actionId The action that is linked to the specified file.
	 * @param pathId The path that is linked to the specified action.
	 */
	public abstract void removeFileAccess(int actionId, int pathId);

	/**
	 * Return the ID of this action's type (these ID are managed by ActionTypeMgr).
	 * 
	 * @param actionId The ID of the action to find the type of.
	 * @return The action's type, or ErrorCode.NOT_FOUND if actionId is invalid.
	 */
	public int getActionType(int actionId);

	/**
	 * Given the ID of a action, return the ID of the action's parent.
	 * 
	 * @param actionId The action to return the parent of.
	 * @return The ID of the action's parent, or NOT_FOUND if the action is at the root, or
	 * BAD_VALUE if the action ID is invalid.
	 */
	public abstract int getParent(int actionId);

	/**
	 * Move an action so it resides underneath a different parent action.
	 * 
	 * @param actionId     The action to be moved.
	 * @param newParentId  The action's new parent.
	 * @return ErrorCode.OK on success, or BAD_VALUE if either ID is invalid
     * or if the new parent-child relationship would cause a loop in the
     * action tree.
	 */
	public abstract int setParent(int actionId, int newParentId);

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
	 * Remove a specific action from the build store. This operation can be only be performed
	 * on actions that are unused. That is, there must be no files that depend on this action.
	 * A trashed action can later be revived by calling the reviveActionFromTrash() method.
	 * 
	 * @param actionId The ID of the action to be move to the trash.
	 * 
	 * @return ErrorCode.OK on successful removal, or ErrorCode.CANT_REMOVE if the
	 * action is still used in some way.
	 */
	public abstract int moveActionToTrash(int actionId);

	/**
	 * Revive an action that had previously been deleted by the moveActionToTrash() method.
	 * @param actionId The ID of the action to be revived.
	 * @return ErrorCode.OK on successful revival, or ErrorCode.CANT_REVIVE if
	 * for some reason the action can't be revived.
	 */
	public abstract int reviveActionFromTrash(int actionId);

	/**
	 * Determine whether an actionID is valid. Note that a trashed action is still
     * considered valid.
	 * 
	 * @param actionId The ID of the action we are querying.
	 * @return true if the actionId is valid, else false.
	 */
	public abstract boolean isActionValid(int actionId);

	
	/**
	 * Determine whether an action is currently marked as "trash" (to be deleted
	 * when the BuildStore is next closed).
	 * 
	 * @param actionId The ID of the action we are querying.
	 * @return true if the action has been marked as trash, else false.
	 */
	public abstract boolean isActionTrashed(int actionId);
	
	/**
	 * For this specific action, return the ID of the named slot. This is simply a short-cut
	 * approach, instead of getting the slotID from the IActionTypeMgr.
	 * 
	 * @param actionId	The action that contains the slot.
	 * @param slotName	The name of the slot.
	 * @return The slot's ID, or ErrorCode.NOT_FOUND if the action is invalid, or the slot
	 * name is not valid for this action.
	 */
	public abstract int getSlotByName(int actionId, String slotName);
	
	/**
	 * For the specified action, set the specified slot to the given value.
	 * 
	 * @param actionId The action that the slot is attached to.
	 * @param slotId   The slot that's connected to the action.
	 * @param value	   The new value to be set (typically an Integer or String).
	 * @return ErrorCode.OK on success, ErrorCode.NOT_FOUND if actionId is invalid, slotId
	 *         is invalid, or slotId isn't attached to actionId, ErrorCode.BAD_VALUE if
	 *         the value can't be assigned to the specified slot, or ErrorCode.LOOP_DETECTED
	 *         if we're setting an input or output slot and a cycle would be created.
	 */
	public abstract int setSlotValue(int actionId, int slotId, Object value);
	
	/**
	 * For the specified action, retrieve the specified slot's value. If the value
	 * has not been explicitly set for this action, the slot default value will be returned.
	 * 
	 * @param actionId	The action that the slot is attached to.
	 * @param slotId	The slot that's connected to the action.
	 * @return The slot's value (typically Integer or String), or null if actionId/slotId can't
	 * be mapped to a valid slot.
	 */
	public abstract Object getSlotValue(int actionId, int slotId);	
	
	/**
	 * Determine whether the specified slot currently holds a value.
	 * @param actionId	The action that the slot is attached to.
	 * @param slotId	The slot that's connected to the action.
	 * @return True if there's an explicit (non-default) value in this slot, else false.
	 *		   Also return false if actionId/slotId are invalid.
	 */
	public abstract boolean isSlotSet(int actionId, int slotId);
	
	/**
	 * Remove the value (if any) that has been inserted into this slot, therefore setting
	 * this slot to its default value. If actionId or slotId is invalid, silently do nothing.
	 * @param actionId	The action that the slot is attached to.
	 * @param slotId	The slot that's connected to the action.
	 */
	public abstract void clearSlotValue(int actionId, int slotId);
	
	/**
	 * Return an array of action IDs for all actions where the specified slot
	 * matches an expected pattern (using % as the wildcard character). This
	 * uses the underlying database "like" operator.
	 * 
	 * @param slotId	ID of the slot to query (only actions that have this slot are considered).
	 * @param match		The match string (using % as the wildcard).
	 * @return			An array of action IDs that match, or null if invalid inputs are provided.
	 */
	public Integer[] getActionsWhereSlotIsLike(int slotId, String match);
	
	/**
	 * Return an array of action IDs for all actions where the specified slot
	 * exactly matches the expected value.
	 * 
	 * @param slotId	ID of the slot to query (only actions that have this slot are considered).
	 * @param match		The exact match object (with a type that's relevant to the slot type).
	 * @return			An array of action IDs that match, or null if invalid inputs are provided.
	 */	
	public Integer[] getActionsWhereSlotEquals(int slotId, Object match);
	
	/**
	 * Return the BuildStore object that owns this IActionMgr object.
	 *
	 * @return The BuildStore object that owns this IActionMgr object.
	 */
	public abstract IBuildStore getBuildStore();
	
	/**
	 * Add the specified listener to the list of objects that are notified when
	 * an action changes in some way.
	 * 
	 * @param listener The object to be added as a listener.
	 */
	public void addListener(IActionMgrListener listener);

	/**
	 * Remove the specified listener from the list of objects to be notified when
	 * an action changes in some way.
	 * 
	 * @param listener The object to be removed from the list of listeners.
	 */
	public void removeListener(IActionMgrListener listener);
}