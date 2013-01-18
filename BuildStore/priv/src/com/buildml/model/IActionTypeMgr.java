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

import com.buildml.model.ISlotTypes.SlotDetails;

/**
 * The interface conformed-to by any ActionTypeMgr object, which represents a
 * subset of the functionality managed by a BuildStore object. An ActionTypeMgr
 * deals with the structure (inputs, output, parameters) of BuildML actions.
 * <p>
 * There should be exactly one ActionTypeMgr object per BuildStore object. Use the
 * BuildStore's getActionTypeMgr() method to obtain that one instance.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface IActionTypeMgr {
	
	/*=====================================================================================*
	 * METHODS FOR CREATING/DELETING ACTION TYPES AND FOLDERS
	 *=====================================================================================*/
	
	/**
	 * @return The ID of the root folder. This is the special folder that always exists
	 * (can't be deleted). Newly created folders and actionTypes will be inserted underneath
	 * this folder.
	 */
	public abstract int getRootFolder();
	
	/**
	 * Define a new action type, used when instantiating actions.
	 * 
	 * @param parentTypeId	The action type that this new action type inherits behaviour from.
	 * @param actionTypeName The name of this actionType. 
	 * @return The new actionTypes ID, ErrorCode.NOT_FOUND if parentTypeId is invalid, or
	 * 			ErrorCode.ONLY_ONE_ALLOWED if the name is already in use.
	 */
	public abstract int newActionType(int parentTypeId, String actionTypeName);

	/**
	 * Add a new folder, which may be used to hold actionTypes, or older folders.
	 * @param folderName The name of the new folder. Ideally this name should be unique, but
	 *                   that's not an enforced requirement.
	 * @return The folder's ID, or ErrorCode.ONLY_ONE_ALLOWED if the name is already in use.
	 */
	public abstract int addFolder(String folderName);

	/**
	 * Retrieve the short (one line) name of this action type (or folder)
	 * 
	 * @param typeId The action type to retrieve the name of.
	 * @return The action type's one line name, or null if typeId is invalid.
	 */
	public abstract String getName(int typeId);
		
	/**
	 * Retrieve the long (multi-line) description of this action type (or folder).
	 * 
	 * @param typeId The action type to retrieve the description of.
	 * @return The action type's description, or null if typeId is invalid.
	 */
	public abstract String getDescription(int typeId);

	/**
	 * Delete the specified action type or folder. This is only possible if the type is
	 * not used by any actions, and it's not the parent of any action types. In the
	 * case of a folder, it must not contain any child entries.
	 * 
	 * @param typeId The action type to be deleted.
	 * @return ErrorCode.OK on success or ErrorCode.CANT_REMOVE if the action
	 * 		   type is still in use.
	 */
	public abstract int remove(int typeId);

	/**
	 * Return the ID of the actionType with the specified name.
	 * @param typeName The actionType name to search for.
	 * @return The actionType ID, ErrorCode.NOT_FOUND if the name is not defined,
	 * or ErrorCode.INVALID_NAME if the name related to a folder (not an actionType).
	 */
	public abstract int getActionTypeByName(String typeName);
	
	/**
	 * Return the IDs of the children of the specified folder.
	 * 
	 * @param folderId	The folder to be queried.
	 * @return An array of IDs of child actionTypes (or folder), or null if folderId
	 * isn't a valid folder.
	 */
	public abstract Integer[] getFolderChildren(int folderId);
	
	/**
	 * Retrieve the parent folder of the specified folder or actionType. The parent
	 * of the root folder is itself.
	 * 
	 * @param folderOrActionTypeId The folder or actionType whose parent we want.
	 * @return The parent, or ErrorCode.NOT_FOUND if folderOrActionTypeId is invalid.
	 */
	public abstract int getParent(int folderOrActionTypeId);

	/**
	 * Move the specified action type or folder into a new parent folder. When moving a folder,
	 * it is important that the new parent is not the same as, or a descendant of the 
	 * folder being moved (this would cause a cycle). 
	 * 
	 * @param folderOrActionTypeId The folder or actionType for which the parent is being set.
	 * @param parentId The parent folder into which the folder/actionType will be moved.
	 * @return ErrorCode.OK on success, or one of the following errors:
	 *         ErrorCode.BAD_VALUE - one or both of the IDs is not valid.
	 *         ErrorCode.NOT_A_DIRECTORY - the parentId does not refer to a folder.
	 *         ErrorCode.BAD_PATH - The destination for the package/folder is invalid,
	 *         possibly because a cycle would be created.
	 */
	public abstract int setParent(int folderOrActionTypeId, int parentId);
	
	/**
	 * Indicates whether the folder or actionType ID refers to a folder, or an actionType.
     *
	 * @param folderOrActionTypeId The ID to query.
	 * @return True if the ID relates to a folder, else false.
	 */
	public abstract boolean isFolder(int folderOrActionTypeId);
	
	/**
	 * Indicates whether the ID refers to a valid actionType or folder.
	 *
     * @param folderOrActionTypeId The ID to query.
	 * @return True if the ID refers to a valid actionType or folder.
	 */
	public boolean isValid(int folderOrActionTypeId);
	
	/*=====================================================================================*
	 * METHODS FOR MODIFYING COMMANDLETS
	 *=====================================================================================*/

	/**
	 * Set the body of the commandlet associated with an action type. The commandlet defines
	 * the code to be executed whenever the action is invoked. The commandlet code may be
	 * written as text, Shell code, Java code, Python code, or any additional language that
	 * may be defined in the future.
	 * 
	 * @param typeId	The action type that owns the commandlet.
	 * @param commandlet	The text of the commandlet, which may be many lines long. The first line
	 *                  of the commandlet must be "!Text", "!Shell", "!Java" or "!Python", etc.
	 *                  to specify which interpreter should be used when evaluating the commandlet.
	 * @return ErrorCode.OK on success, ErrorCode.INVALID_OP if the interpreter name is
	 * not recognized.
	 */
	public abstract int setCommandlet(int typeId, String commandlet);	

	/**
	 * Retrieve the body of the commandlet that's associated with an action type.
	 * 
	 * @param typeId	The action type that owns the commandlet.
	 * @return The commandlet body, or null if typeId is invalid, or relates to a folder.
	 */
	public abstract String getCommandlet(int typeId);	
	
	/*=====================================================================================*
	 * METHODS FOR ATTACHING/DETACHING SLOTS TO ACTION TYPES
	 *=====================================================================================*/
	
	/**
	 * Add a new slot to this actionType.
	 * 
	 * @param typeId		The actionType to add the slot to.
	 * @param slotName		The name of the slot (must be unique within this actionType).
	 * @param slotType		The slot's type (SLOT_TYPE_FILEGROUP, etc).
	 * @param slotPos		The slot's position (SLOT_POS_INPUT, etc).
	 * @param isRequired	True if actions must provide a value for this slot.
	 * @param defaultValue	If not required, a default value.
	 * @param enumValues	For SLOT_TYPE_ENUMERATION, an array of valid values.
	 * @return The newly-added slot ID, or:
	 * 			ErrorCode.NOT_FOUND if typeId is invalid.
	 * 			ErrorCode.INVALID_NAME if slotName is not a valid slot identifier.
	 * 			ErrorCode.ALREADY_USED if slotName is already in use (for this actionType).
	 * 			ErrorCode.INVALID_OP if slotType or slotType are not valid/relevant, or
	 *                    if enumValues does not contain a valid enumeration.
	 * 			ErrorCode.BAD_VALUE if the default value is not valid for this type.
	 */
	public abstract int newSlot(int typeId, int slotName, int slotType, int slotPos, 
								boolean isRequired, Object defaultValue, String[] enumValues);

	/**
	 * Return all the slots associated with an actionType.
	 * 
	 * @param typeId	The ID of the actionType containing the slots.
	 * @param slotPos	The position (within the actionType) of the slot (SLOT_POS_INPUT, etc).
	 * @return An array of slot details, or null if typeId or slotPos is invalid, or typeId
	 *         relates to a folder.
	 */
	public abstract SlotDetails[] getSlots(int typeId, int slotPos);

	/**
	 * For the specified actionType, return details of the named slot.
	 * 
	 * @param typeId	The ID of the actionType containing the slot.
	 * @param slotName	The name of the slot (within the scope of the actionType).
	 * @return The slot details, or null if typeId or slotName is invalid.
	 */
	public abstract SlotDetails getSlotByName(int typeId, String slotName);
	
	/**
	 * Remove a slot from the actionType. The slot can only be removed if there are no
	 * actions that define the slot value.
	 * 
	 * @param typeId	The ID of the actionType containing the slot.
	 * @param slotId	The ID of the slot to be removed.
	 * @return ErrorCode.OK on success,
	 * 		   ErrorCode.NOT_FOUND if typeId is invalid,
	 * 		   ErrorCode.BAD_VALUE if slotId is invalid, or
	 * 		   ErrorCode.CANT_REMOVE if the slot is still in use by an action.
	 */
	public abstract int removeSlot(int typeId, int slotId);
	
	/**
	 * Return the BuildStore object that owns this IActionTypeMgr object.
	 *
	 * @return The BuildStore object that owns this IActionTypeMgr object.
	 */
	public abstract IBuildStore getBuildStore();
	
	/*-------------------------------------------------------------------------------------*/
}