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

package com.buildml.model.impl;

import com.buildml.model.IActionTypeMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.ISlotTypes.SlotDetails;
import com.buildml.utils.errors.ErrorCode;

/**
 * An implementation of IActionTypeMgr, to manage the various action types available
 * in the BuildML system.
 * 
 * ActionTypes are a mix of the following:
 *   - Built-in action types (such as "Shell Command").
 *   - BuildStore-specific (custom-defined by user, in this BuildStore's database).
 *   - Demand-loaded from XML files (loaded into database).
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class ActionTypeMgr implements IActionTypeMgr {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The hard-coded ID for the root folder */
	static final int BUILTIN_ROOT_FOLDER_ID = 0;
	
	/** The hard-coded name for the root folder */	
	static final String BUILTIN_ROOT_FOLDER_NAME = "All Action Types";

	/** The hard-coded description for the root folder */	
	static final String BUILTIN_ROOT_FOLDER_DESCR = "Root folder for all action types.";

	/** The hard-coded ID for the "Shell Command" action type */
	static final int BUILTIN_SHELL_COMMAND_ID = 1;
	
	/** The hard-coded name for the shell command action type */	
	static final String BUILTIN_SHELL_COMMAND_NAME = "Shell Command";

	/** The hard-coded description for the shell command action type */	
	static final String BUILTIN_SHELL_COMMAND_DESCR = 
									"Simple shell command, typically used when importing actions.";
	
	/** The BuildStore that owns this ActionTypeMgr */
	private BuildStore buildStore;
	
	/** The SlotMgr we delegate work to */
	private SlotMgr slotMgr;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new ActionTypeMgr object.
	 * 
	 * @param buildStore The BuildStore that owns this manager.
	 */
	public ActionTypeMgr(BuildStore buildStore) {
		this.buildStore = buildStore;
		slotMgr = buildStore.getSlotMgr();		
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#getRootFolder()
	 */
	@Override
	public int getRootFolder() {
		return BUILTIN_ROOT_FOLDER_ID;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#newActionType(int, java.lang.String)
	 */
	@Override
	public int newActionType(int parentTypeId, String actionTypeName) {
		/* for now, this operation is not implemented */
		return ErrorCode.INVALID_OP;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#addFolder(java.lang.String)
	 */
	@Override
	public int addFolder(String folderName) {
		/* for now, this operation is not implemented */
		return ErrorCode.INVALID_OP;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#getName(int)
	 */
	@Override
	public String getName(int typeId) {
		
		if (typeId == BUILTIN_ROOT_FOLDER_ID) {
			return BUILTIN_ROOT_FOLDER_NAME;
		} else if (typeId == BUILTIN_SHELL_COMMAND_ID) {
			return BUILTIN_SHELL_COMMAND_NAME;
		}
		
		/* for now, no other action types are supported */
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#getDescription(int)
	 */
	@Override
	public String getDescription(int typeId) {
		
		if (typeId == BUILTIN_ROOT_FOLDER_ID) {
			return BUILTIN_ROOT_FOLDER_DESCR;
		} else if (typeId == BUILTIN_SHELL_COMMAND_ID) {
			return BUILTIN_SHELL_COMMAND_DESCR;
		}

		/* for now, no other action types are supported */
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#deleteActionType(int)
	 */
	@Override
	public int remove(int typeId) {
		
		/* we can't remove built-in types */
		if ((typeId == BUILTIN_ROOT_FOLDER_ID) || (typeId == BUILTIN_SHELL_COMMAND_ID)) {
			return ErrorCode.CANT_REMOVE;			
		}
		
		/* for now, no other action types or folders can be created */
		return ErrorCode.NOT_FOUND;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#getActionTypeByName(java.lang.String)
	 */
	@Override
	public int getActionTypeByName(String typeName) {
		if (typeName.equals(BUILTIN_SHELL_COMMAND_NAME)) {
			return BUILTIN_SHELL_COMMAND_ID;
		} else if (typeName.equals(BUILTIN_ROOT_FOLDER_NAME)) {
			return ErrorCode.INVALID_NAME;
		}
		
		/* for now, no other action types can be defined */
		return ErrorCode.NOT_FOUND;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#getFolderChildren(int)
	 */
	@Override
	public Integer[] getFolderChildren(int folderId) {
		
		/* for now, root folder has only one child */
		if (folderId == BUILTIN_ROOT_FOLDER_ID) {
			return new Integer[] { BUILTIN_SHELL_COMMAND_ID };
		}
		
		/* nothing else has children */
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#getParent(int)
	 */
	@Override
	public int getParent(int folderOrActionTypeId) {
		if (folderOrActionTypeId == BUILTIN_ROOT_FOLDER_ID) {
			return BUILTIN_ROOT_FOLDER_ID;
		} else if (folderOrActionTypeId == BUILTIN_SHELL_COMMAND_ID) {
			return BUILTIN_ROOT_FOLDER_ID;
		}
		
		return ErrorCode.NOT_FOUND;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#setParent(int, int)
	 */
	@Override
	public int setParent(int folderOrActionTypeId, int parentId) {
		
		/* For now, we can't change the parent of anything */
		return ErrorCode.INVALID_OP;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#isFolder(int)
	 */
	@Override
	public boolean isFolder(int folderOrActionTypeId) {
		return (folderOrActionTypeId == BUILTIN_ROOT_FOLDER_ID);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#isValid(int)
	 */
	@Override
	public boolean isValid(int folderOrActionTypeId) {
		if ((folderOrActionTypeId == BUILTIN_ROOT_FOLDER_ID) ||
				(folderOrActionTypeId == BUILTIN_SHELL_COMMAND_ID)){
			return true;
		}
		
		/* no other implemented yet */
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#setCommandlet(int, java.lang.String)
	 */
	@Override
	public int setCommandlet(int typeId, String commandlet) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#getCommandlet(int)
	 */
	@Override
	public String getCommandlet(int typeId) {
		
		/* folders don't have commandlets */
		if (isFolder(typeId)) {
			return null;
		}
		
		/* the shell action has a hard-coded commandlet */
		if (typeId == BUILTIN_SHELL_COMMAND_ID) {
			return "!Shell\n${CommandString}";
		}
		
		/* for now, nothing else is defined */
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#newSlot(int, int, int, int, boolean, java.lang.Object, java.lang.String[])
	 */
	@Override
	public int newSlot(int typeId, int slotName, int slotType, int slotPos,
			boolean isRequired, Object defaultValue, String[] enumValues) {
		
		/* check for invalid typeId, since SlotMgr can't determine this */
		if (isFolder(typeId) || !isValid(typeId)) {
			return ErrorCode.NOT_FOUND;
		}
		
		/* delegate the rest of the work to SlotMgr */
		return slotMgr.newSlot(SlotMgr.SLOT_OWNER_ACTION, typeId, slotName, slotType, slotPos,
								isRequired, defaultValue, enumValues);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#getSlots(int, int)
	 */
	@Override
	public SlotDetails[] getSlots(int typeId, int slotPos) {
		
		/* folder don't have slots */
		if (isFolder(typeId)) {
			return null;
		}
		
		/* delegate to SlotMgr */
		return slotMgr.getSlots(SlotMgr.SLOT_OWNER_ACTION, typeId, slotPos);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#getSlotByName(int, java.lang.String)
	 */
	@Override
	public SlotDetails getSlotByName(int typeId, String slotName) {
		
		/* all work can be delegated */
		return slotMgr.getSlotByName(SlotMgr.SLOT_OWNER_ACTION, typeId, slotName);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#removeSlot(int, int)
	 */
	@Override
	public int removeSlot(int typeId, int slotId) {

		/* all work can be delegated */
		return slotMgr.removeSlot(SlotMgr.SLOT_OWNER_ACTION, typeId, slotId);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#getBuildStore()
	 */
	@Override
	public IBuildStore getBuildStore() {
		return buildStore;
	}

	/*-------------------------------------------------------------------------------------*/
}
