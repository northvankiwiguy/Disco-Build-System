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

	/** The BuildStore that owns this ActionTypeMgr */
	private BuildStore buildStore;
	
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
		
		// TODO: add built-in action types.
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#getRootFolder()
	 */
	@Override
	public int getRootFolder() {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#newActionType(int, java.lang.String)
	 */
	@Override
	public int newActionType(int parentTypeId, String actionTypeName) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#addFolder(java.lang.String)
	 */
	@Override
	public int addFolder(String folderName) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#getName(int)
	 */
	@Override
	public String getName(int typeId) {
		// TODO Auto-generated method stub
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#getDescription(int)
	 */
	@Override
	public String getDescription(int typeId) {
		// TODO Auto-generated method stub
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#deleteActionType(int)
	 */
	@Override
	public int deleteActionType(int typeId) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#getActionTypeByName(java.lang.String)
	 */
	@Override
	public int getActionTypeByName(String typeName) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#getFolderChildren(int)
	 */
	@Override
	public Integer[] getFolderChildren(int folderId) {
		// TODO Auto-generated method stub
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#getParent(int)
	 */
	@Override
	public int getParent(int folderOrActionTypeId) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#setParent(int, int)
	 */
	@Override
	public int setParent(int folderOrActionTypeId, int parentId) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#isFolder(int)
	 */
	@Override
	public boolean isFolder(int folderOrActionTypeId) {
		// TODO Auto-generated method stub
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#isValid(int)
	 */
	@Override
	public boolean isValid(int folderOrActionTypeId) {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#newSlot(int, int, int, int, boolean, java.lang.Object, java.lang.String[])
	 */
	@Override
	public int newSlot(int typeId, int slotName, int slotType, int slotPos,
			boolean isRequired, Object defaultValue, String[] enumValues) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#getSlots(int, int)
	 */
	@Override
	public SlotDetails[] getSlots(int typeId, int slotPos) {
		// TODO Auto-generated method stub
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#getSlotByName(int, java.lang.String)
	 */
	@Override
	public SlotDetails getSlotByName(int typeId, String slotName) {
		// TODO Auto-generated method stub
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#removeSlot(int, int)
	 */
	@Override
	public int removeSlot(int typeId, int slotId) {
		// TODO Auto-generated method stub
		return 0;
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
