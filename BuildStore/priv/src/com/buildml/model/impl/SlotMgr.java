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

import java.util.ArrayList;
import java.util.List;

import com.buildml.model.IBuildStore;
import com.buildml.model.ISlotTypes;
import com.buildml.model.ISlotTypes.SlotDetails;
import com.buildml.utils.errors.ErrorCode;

/**
 * A class for managing slots. This is a "hidden" BuildStore manager. A reference
 * to this class can be obtained from a BuildStore via the getSlotMgr() method,
 * although there's no public-facing interface where client can invoke methods.
 * Instead, only the other BuildStore managers are allowed to access the methods.
 * 
 * All the methods in this class should be package-private, or private.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
class SlotMgr {
	
	/*
	 * Note: the following slot IDs are pre-defined:
	 *   1 - SLOT_OWNER_ACTION / "Shell Command" / "Input" / Input pos / FileGroup type.
	 *   2 - SLOT_OWNER_ACTION / "Shell Command" / "Command" / Param pos / String type.
	 *   3 - SLOT_OWNER_ACTION / "Shell Command" / "Output0" / Output pos / FileGroup type.
	 *   ...
	 *  12 - SLOT_OWNER_ACTION / "Shell Command" / "Output9" / Output pos / FileGroup type.
	 */

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The slot is owned by an action, or actionType */
	final static int SLOT_OWNER_ACTION = 1;

	/** The slot is owned by a package, or sub-package */
	final static int SLOT_OWNER_PACKAGE = 2;
	
	/** The BuildStore that owns this SlotMgr */
	private BuildStore buildStore;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new SlotMgr object.
	 * 
	 * @param buildStore The BuildStore that owns this manager.
	 */
	SlotMgr(BuildStore buildStore) {
		this.buildStore = buildStore;
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Add a new slot to this element (either an actionType or a package).
	 * 
	 * @param ownerType     The type of thing to add this slot to (SLOT_OWNER_ACTION, 
	 *                      SLOT_OWNER_PACKAGE)
	 * @param ownerId		The new owner (actionType or package) to add the slot to.
	 * @param slotName		The name of the slot (must be unique within this actionType).
	 * @param slotType		The slot's type (SLOT_TYPE_FILEGROUP, etc).
	 * @param slotPos		The slot's position (SLOT_POS_INPUT, etc).
	 * @param isRequired	True if actions must provide a value for this slot.
	 * @param defaultValue	If not required, a default value.
	 * @param enumValues	For SLOT_TYPE_ENUMERATION, an array of valid values.
	 * @return The newly-added slot ID, or:
	 * 			ErrorCode.INVALID_NAME if slotName is not a valid slot identifier.
	 * 			ErrorCode.ALREADY_USED if slotName is already in use (for this owner).
	 * 			ErrorCode.INVALID_OP if slotType or slotType are not valid/relevant, or
	 *                    if enumValues does not contain a valid enumeration.
	 * 			ErrorCode.BAD_VALUE if the default value is not valid for this type.
	 */
	int newSlot(int ownerType, int ownerId, int slotName, int slotType, int slotPos,
			boolean isRequired, Object defaultValue, String[] enumValues) {

		/* for now, no new slots can be added */
		return ErrorCode.INVALID_OP;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return all the slots associated with an owner.
	 * 
	 * @param ownerType The type of thing to add this slot to (SLOT_OWNER_ACTION, 
	 *                  SLOT_OWNER_PACKAGE)
	 * @param ownerId	The ID of the owner (actionType/package) containing the slots.
	 * @param slotPos	The position (within the actionType) of the slot (SLOT_POS_INPUT, etc).
	 * @return An array of slot details, or null if typeId or slotPos is invalid, or typeId
	 *         relates to a folder.
	 */
	SlotDetails[] getSlots(int ownerType, int ownerId, int slotPos) {
		
		/* for now, we only have slots for the built-in "Shell Command" action type */
		if ((ownerType == SLOT_OWNER_ACTION) && (ownerId == ActionTypeMgr.BUILTIN_SHELL_COMMAND_ID)) {
			
			List<SlotDetails> results = new ArrayList<SlotDetails>();
			if ((slotPos == ISlotTypes.SLOT_POS_INPUT) || (slotPos == ISlotTypes.SLOT_POS_ANY)) {
				SlotDetails output = 
						new SlotDetails(1, "Input", ISlotTypes.SLOT_TYPE_FILEGROUP, 
										ISlotTypes.SLOT_POS_INPUT, false, null, null);
				results.add(output);
			}
			
			if ((slotPos == ISlotTypes.SLOT_POS_PARAMETER) || (slotPos == ISlotTypes.SLOT_POS_ANY)) {
				SlotDetails output = 
						new SlotDetails(2, "Command", ISlotTypes.SLOT_TYPE_TEXT, 
										ISlotTypes.SLOT_POS_PARAMETER, true, null, null);
				results.add(output);
			}
					
			if ((slotPos == ISlotTypes.SLOT_POS_OUTPUT) || (slotPos == ISlotTypes.SLOT_POS_ANY)) {
				for (int id = 0; id < 10; id++) {
					SlotDetails output = 
						new SlotDetails(3 + id, "Output" + id, ISlotTypes.SLOT_TYPE_FILEGROUP, 
										ISlotTypes.SLOT_POS_OUTPUT, false, null, null);
					results.add(output);
				}
			}
		
			return results.toArray(new SlotDetails[results.size()]);
		}

		/* invalid typeId/slotPos */
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * For the specified owner (actionType/package), return details of the named slot.
	 * 
	 * @param ownerType The type of thing to add this slot to (SLOT_OWNER_ACTION, 
	 *                  SLOT_OWNER_PACKAGE)
	 * @param ownerId	The ID of the owner containing the slot.
	 * @param slotName	The name of the slot (within the scope of the owner).
	 * @return The slot details, or null if ownerType, ownerId or slotName is invalid.
	 */
	SlotDetails getSlotByName(int ownerType, int ownerId, String slotName) {
		
		if ((ownerType == SLOT_OWNER_ACTION) && (ownerId == ActionTypeMgr.BUILTIN_SHELL_COMMAND_ID)) {
			
			if (slotName.equals("Input")) {
				return new SlotDetails(1, "Input", ISlotTypes.SLOT_TYPE_FILEGROUP, 
										ISlotTypes.SLOT_POS_INPUT, false, null, null);
				
			} else if (slotName.equals("Command")) {
				return new SlotDetails(2, "Command", ISlotTypes.SLOT_TYPE_TEXT, 
						ISlotTypes.SLOT_POS_PARAMETER, true, null, null);
				
			} else if (slotName.startsWith("Output") && (slotName.length() == 7)) {

				char which = slotName.charAt(6);
				if ((which >= '0') && (which <= '9')) {
					return new SlotDetails(3 + (which - '0'), slotName, ISlotTypes.SLOT_TYPE_FILEGROUP, 
							ISlotTypes.SLOT_POS_OUTPUT, false, null, null);

				} else {
					/* invalid digit in "OutputN" */
					return null;
				}
			}
		}
		
		/* we only handle the "Shell Command" action - others are an errors */
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Remove a slot from the owner. The slot can only be removed if there are no
	 * actions/packages that define the slot value.
	 * 
	 * @param ownerType The type of thing to add this slot to (SLOT_OWNER_ACTION, 
	 *                  SLOT_OWNER_PACKAGE)
	 * @param ownerId	The ID of the owner containing the slot.
	 * @param slotId	The ID of the slot to be removed.
	 * @return ErrorCode.OK on success,
	 * 		   ErrorCode.BAD_VALUE if slotId is invalid, or
	 * 		   ErrorCode.CANT_REMOVE if the slot is still in use.
	 */
	int removeSlot(int ownerType, int ownerId, int slotId) {
		return ErrorCode.CANT_REMOVE;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * For the specified action/sub-package, set a slot to the given value.
	 * 
	 * @param ownerType Either SLOT_OWNER_ACTION or SLOT_OWNER_PACKAGE.
	 * @param actionId  The action that the slot is attached to.
	 * @param slotId    The slot that's connected to the action.
	 * @param value	    The new value to be set (typically an Integer or String).
	 * @return ErrorCode.OK on success, ErrorCode.NOT_FOUND if actionId is invalid, slotId
	 *         is invalid, or slotId isn't attached to actionId, or ErrorCode.BAD_VALUE if
	 *         the value can't be assigned to the specified slot.
	 */
	public int setSlotValue(int ownerType, int actionId, int slotId,
			Object value) {
		return ErrorCode.INVALID_OP;		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * For the specified action/sub-package, retrieve the specified slot's value. If the value
	 * has not been explicitly set for this action, the slot default value will be returned.
	 * 
	 * @param ownerType Either SLOT_OWNER_ACTION or SLOT_OWNER_PACKAGE.
	 * @param actionId	The action that the slot is attached to.
	 * @param slotId	The slot that's connected to the action.
	 * @return The slot's value (typically Integer or String), or null if actionId/slotId can't
	 * be mapped to a valid slot.
	 */
	public Object getSlotValue(int ownerType, int actionId, int slotId) {
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The BuildStore that owns this SlotMgr object.
	 */
	IBuildStore getBuildStore() {
		return buildStore;
	}

	/*-------------------------------------------------------------------------------------*/
}
