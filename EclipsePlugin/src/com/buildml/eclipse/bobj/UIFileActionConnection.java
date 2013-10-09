/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.bobj;

/**
 * A "business object" representing a connection between a UIFileGroup and a UIAction.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class UIFileActionConnection {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/** The connection is from the file group into the action's slot */
	public static final int INPUT_TO_ACTION = 0;
	
	/** The connection is from the action's slot out to the file group */
	public static final int OUTPUT_FROM_ACTION = 1;
	
	/** The ID of the file group at one end of the connection */
	private int fileGroupId;

	/** The ID of the action at one end of the connection */
	private int actionId;

	/** The ID of the slot, within the action, that this connection joins to */
	private int slotId;
	
	/** Direction of the relation (FILE_TO_ACTION, ACTION_TO_FILE) */
	private int direction;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new UIFileActionConnection, representing a connection between a UIFileGroup
	 * and a UIAction.
	 * 
	 * @param fileGroupId The ID of the file group being connected.
	 * @param actionId The ID of the action being connected.
	 * @param slotId The ID of the slot (within the action).
	 * @param direction Direction of the relation (FILE_TO_ACTION, ACTION_TO_FILE)
	 * 
	 */
	public UIFileActionConnection(int fileGroupId, int actionId, int slotId, int direction) {
		this.fileGroupId = fileGroupId;
		this.actionId = actionId;
		this.slotId = slotId;
		this.direction = direction;
	}

	/*-------------------------------------------------------------------------------------*/	

	/**
	 * @return the fileGroupId
	 */
	public int getFileGroupId() {
		return fileGroupId;
	}

	/*-------------------------------------------------------------------------------------*/	

	/**
	 * @return the actionId
	 */
	public int getActionId() {
		return actionId;
	}

	/*-------------------------------------------------------------------------------------*/	

	/**
	 * @return the slotId
	 */
	public int getSlotId() {
		return slotId;
	}

	/*-------------------------------------------------------------------------------------*/	

	/**
	 * @return the direction
	 */
	public int getDirection() {
		return direction;
	}

	/*-------------------------------------------------------------------------------------*/	
}
