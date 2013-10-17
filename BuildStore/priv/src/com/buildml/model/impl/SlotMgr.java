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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.buildml.model.FatalBuildStoreError;
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
	
	/** Cached-copies of the SlotDetails for the default slots */
	SlotDetails defaultSlots[] = null;
	
	/** The BuildStore that owns this SlotMgr */
	private BuildStore buildStore;
	
	/**
	 * Our database manager object, used to access the database content. This is provided 
	 * to us when the SlotMgr object is first instantiated.
	 */
	private BuildStoreDB db = null;
	
	/** Various prepared statement for database access. */
	private PreparedStatement
				insertTypePrepStmt = null,
				findTypeByNamePrepStmt = null,
				findTypeByIdPrepStmt = null,
				findTypeByPosPrepStmt = null,
				findTypeByAnyPosPrepStmt = null,
				insertValuePrepStmt = null,
				updateValuePrepStmt = null,
				findValuePrepStmt = null,
				deleteValuePrepStmt = null;
		
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
		this.db = buildStore.getBuildStoreDB();
	
		/* prepare the database statements */
		insertTypePrepStmt = db.prepareStatement("insert into slotTypes values (null, ?, ?, ?, ?, ?, ?, ?, ?)");
		findTypeByNamePrepStmt = db.prepareStatement(
				"select slotId, slotType, slotPos, slotCard, defaultValue from slotTypes " +
						"where ownerType = ? and ownerId = ? and slotName = ?");
		findTypeByIdPrepStmt = db.prepareStatement(
				"select slotName, slotType, slotPos, slotCard, defaultValue from slotTypes " +
						"where slotId = ?");
		findTypeByPosPrepStmt = db.prepareStatement(
				"select slotId, slotName, slotType, slotPos, slotCard, defaultValue from slotTypes " +
				"where ownerType = ? and ownerId = ? and slotPos = ?");
		findTypeByAnyPosPrepStmt = db.prepareStatement(
				"select slotId, slotName, slotType, slotPos, slotCard, defaultValue from slotTypes " +
				"where ownerType = ? and ownerId = ?");
		insertValuePrepStmt = db.prepareStatement("insert into slotValues values (?, ?, ?, ?)");
		updateValuePrepStmt = db.prepareStatement("update slotValues set value = ? where ownerType = ? " +
													" and ownerId = ? and slotId = ?");
		findValuePrepStmt = db.prepareStatement("select value from slotValues where ownerType = ? " +
													" and ownerId = ? and slotId = ?");
		deleteValuePrepStmt = db.prepareStatement("delete from slotValues where ownerType = ? and ownerId = ? " +
													"and slotId = ?");
		
		/* define the default slots */
		newSlot(SLOT_OWNER_ACTION, ActionTypeMgr.BUILTIN_SHELL_COMMAND_ID, "Input", 
				ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_INPUT,
				ISlotTypes.SLOT_CARD_OPTIONAL, null, null);
		newSlot(SLOT_OWNER_ACTION, ActionTypeMgr.BUILTIN_SHELL_COMMAND_ID, "Command", 
				ISlotTypes.SLOT_TYPE_TEXT, ISlotTypes.SLOT_POS_PARAMETER,
				ISlotTypes.SLOT_CARD_REQUIRED, null, null);		
		for (int id = 0; id < 10; id++) {
			newSlot(SLOT_OWNER_ACTION, ActionTypeMgr.BUILTIN_SHELL_COMMAND_ID, "Output" + id, 
					ISlotTypes.SLOT_TYPE_FILEGROUP, ISlotTypes.SLOT_POS_OUTPUT,
					ISlotTypes.SLOT_CARD_OPTIONAL, null, null);
		}
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
	 * @param slotCard   	Either SLOT_CARD_OPTIONAL, SLOT_CARD_ONE, SLOT_CARD_MULTI.
	 * @param defaultValue	If not required, a default value.
	 * @param enumValues	For SLOT_TYPE_ENUMERATION, an array of valid values.
	 * @return The newly-added slot ID, or:
	 * 			ErrorCode.INVALID_NAME if slotName is not a valid slot identifier.
	 * 			ErrorCode.ALREADY_USED if slotName is already in use (for this owner).
	 * 			ErrorCode.INVALID_OP if slotType or slotPos are not valid/relevant, or
	 *                    if enumValues does not contain a valid enumeration.
	 *          ErrorCode.OUT_OF_RANGE is the cardinality is invalid, or if a multi-slot
	 *                    is selected and this is not an action slot, or there's already
	 *                    a multi-slot for this action.
	 * 			ErrorCode.BAD_VALUE if the default value is not valid for this type.
	 * 			ErrorCode.NOT_FOUND if ownerType/ownerId are not valid.
	 */
	int newSlot(int ownerType, int ownerId, String slotName, int slotType, int slotPos,
				int slotCard, Object defaultValue, String[] enumValues) {

		/* validate that the slot name is well-formed */
		if (!isValidSlotName(slotName)) {
			return ErrorCode.INVALID_NAME;
		}
		
		/* validate slotType and slotPos */
		if ((slotType < ISlotTypes.SLOT_TYPE_FILEGROUP) || (slotType > ISlotTypes.SLOT_TYPE_ENUMERATION)) {
			return ErrorCode.INVALID_OP;
		}
		if ((slotPos < ISlotTypes.SLOT_POS_INPUT) || (slotPos > ISlotTypes.SLOT_POS_LOCAL)) {
			return ErrorCode.INVALID_OP;
		}
		
		/* validate that the slot name is not already in use for this ownerType/ownerId */
		if (getSlotByName(ownerType, ownerId, slotName) != null) {
			return ErrorCode.ALREADY_USED;
		}
		
		/* 
		 * Validate that file groups can only appear in input/output slots, and that no other types
		 * can appear in these slots.
		 */
		if (slotType == ISlotTypes.SLOT_TYPE_FILEGROUP){
			if ((slotPos != ISlotTypes.SLOT_POS_INPUT) && (slotPos != ISlotTypes.SLOT_POS_OUTPUT)) {
				return ErrorCode.INVALID_OP;
			}
		}		
		else {
			if ((slotPos == ISlotTypes.SLOT_POS_INPUT) || (slotPos == ISlotTypes.SLOT_POS_OUTPUT)) {
				return ErrorCode.INVALID_OP;
			}
		}
		
		/* validate slot cardinality */
		if ((slotCard < ISlotTypes.SLOT_CARD_OPTIONAL) || (slotCard > ISlotTypes.SLOT_CARD_MULTI)) {
			return ErrorCode.OUT_OF_RANGE;
		}
		
		/* special rules apply for multi-slots */
		if (slotCard == ISlotTypes.SLOT_CARD_MULTI) {

			/* only input file groups can have multi cardinality */
			if ((slotPos != ISlotTypes.SLOT_POS_INPUT) || (slotType != ISlotTypes.SLOT_TYPE_FILEGROUP)) {
				return ErrorCode.OUT_OF_RANGE;
			}
	
			/* only one input file group can have this cardinality - check all current slots first */
			SlotDetails[] currentSlots = getSlots(ownerType, ownerId, ISlotTypes.SLOT_POS_INPUT);
			for (int i = 0; i < currentSlots.length; i++) {
				if (currentSlots[i].slotCard == ISlotTypes.SLOT_CARD_MULTI) {
					return ErrorCode.OUT_OF_RANGE;
				}
			}
		}
		
		/* TODO: validate that the default value makes sense */
		
		/* All the inputs are valid, so add the new record to the database */
		String defaultValueString = null;	
		if (defaultValue != null) {
			defaultValueString = defaultValue.toString();
		}
		
		int newSlotId;
		try {
			insertTypePrepStmt.setInt(1, ownerType);
			insertTypePrepStmt.setInt(2, ownerId);
			insertTypePrepStmt.setString(3, slotName);
			insertTypePrepStmt.setInt(4, slotType);
			insertTypePrepStmt.setInt(5, slotPos);
			insertTypePrepStmt.setInt(6, slotCard);
			insertTypePrepStmt.setString(7, defaultValueString);
			insertTypePrepStmt.setInt(8, 0);
			db.executePrepUpdate(insertTypePrepStmt);
			
			newSlotId = db.getLastRowID();
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
				
		return newSlotId;
	}
		
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return a slot's detailed information.
	 * 
	 * @param slotId The slot to query.
	 * @return A SlotDetails structure containing the specified slot's details, or null if
	 * slotId does not refer to a valid slot.
	 */
	SlotDetails getSlotByID(int slotId) {
		
		ResultSet rs = null;
		SlotDetails details = null;
		try {
			findTypeByIdPrepStmt.setInt(1, slotId);
			rs = db.executePrepSelectResultSet(findTypeByIdPrepStmt);
			
			if (rs.next()) {
				String slotName = rs.getString(1);
				int slotType = rs.getInt(2);
				int slotPos = rs.getInt(3);
				int slotCard = rs.getInt(4);
				String defaultValue = rs.getString(5);
				details = new SlotDetails(slotId, slotName, slotType, slotPos, slotCard, defaultValue, null);
			}
			rs.close();
			return details;
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return all the slots associated with an owner.  We assume that all input parameters 
	 * have been validated by the caller (actionTypeMgr or packageMgr).
	 * 
	 * @param ownerType The type of thing that owns this slots (SLOT_OWNER_ACTION, 
	 *                  SLOT_OWNER_PACKAGE)
	 * @param ownerId	The ID of the owner (actionType/package) containing the slots.
	 * @param slotPos	The position (within the actionType) of the slot (SLOT_POS_INPUT, etc).
	 * @return An array of slot details.
	 */
	SlotDetails[] getSlots(int ownerType, int ownerId, int slotPos) {
		
		
		PreparedStatement stmt;
		try {
			/* A slotPos of SLOT_POS_ANY requires a different database query */
			if (slotPos == ISlotTypes.SLOT_POS_ANY) {
				stmt = findTypeByAnyPosPrepStmt;
			} else {
				stmt = findTypeByPosPrepStmt;
				stmt.setInt(3, slotPos);
			}
			stmt.setInt(1, ownerType);
			stmt.setInt(2, ownerId);
			
			/*
             * Fetch the results from the database, and form the SlotDetails[]
             */
			ResultSet rs = db.executePrepSelectResultSet(stmt);
			List<SlotDetails> results = new ArrayList<SlotDetails>();
			while (rs.next()) {
				int slotId = rs.getInt(1);
				String slotName = rs.getString(2);
				int slotType = rs.getInt(3);
				int actualSlotPos = rs.getInt(4);
				int slotCard = rs.getInt(5);
				String defaultValue = rs.getString(6);
				SlotDetails details = 
						new SlotDetails(slotId, slotName, slotType, actualSlotPos, slotCard, defaultValue, null);
				results.add(details);
			}
			rs.close();
			return results.toArray(new SlotDetails[results.size()]);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
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
		
		ResultSet rs = null;
		SlotDetails details = null;
		try {
			findTypeByNamePrepStmt.setInt(1, ownerType);
			findTypeByNamePrepStmt.setInt(2, ownerId);
			findTypeByNamePrepStmt.setString(3, slotName);
			rs = db.executePrepSelectResultSet(findTypeByNamePrepStmt);
			
			/* there should be only one result... */
			if (rs.next()) {
				int slotId = rs.getInt(1);
				int slotType = rs.getInt(2);
				int slotPos = rs.getInt(3);
				int slotCard = rs.getInt(4);
				String defaultValue = rs.getString(5);
				details = new SlotDetails(slotId, slotName, slotType, slotPos, slotCard, defaultValue, null);
			}
			rs.close();
			return details;
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}		
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
	 * @param ownerId   The action/sub-package that the slot is attached to.
	 * @param slotId    The slot that's connected to the action.
	 * @param value	    The new value to be set (typically an Integer or String).
	 * @return ErrorCode.OK on success, ErrorCode.NOT_FOUND if slotId isn't relevant for
	 *         actionType/package, or ErrorCode.BAD_VALUE if the value can't be assigned to
	 *         the specified slot.
	 */
	public int setSlotValue(int ownerType, int ownerId, int slotId, Object value) {
		
		/* 
		 * Assume that ownerType and ownerId was validated by our caller, but we need
		 * to check that slotId is relevant for ownerType/ownerId. If not, return NOT_FOUND.
		 * Note: for now, actionType can only be "Shell Command", so we only need to validate
		 * that slotId is a valid default slot ID.
		 */
		SlotDetails details = getSlotByID(slotId);
		if (details == null) {
			return ErrorCode.NOT_FOUND;
		}
		
		/*
		 * We store all data in string format, but based on the slot's type, we first need
		 * to check whether the value is appropriate.
		 */
		String stringToSet = null;
		
		/* setting a null value will delete the slot record */
		if (value == null) {
			try {
				deleteValuePrepStmt.setInt(1, ownerType);
				deleteValuePrepStmt.setInt(2, ownerId);
				deleteValuePrepStmt.setInt(3, slotId);
				db.executePrepUpdate(deleteValuePrepStmt);				
			} catch (SQLException e) {
				throw new FatalBuildStoreError("Unable to execute SQL statement", e);
			}
			return ErrorCode.OK;
		}
		
		switch (details.slotType) {
		
		/* The input value must be an Integer (or String representing a positive int) */
		case ISlotTypes.SLOT_TYPE_FILEGROUP:
			
			/* integers must be positive */
			if (value instanceof Integer) {
				stringToSet = value.toString();
			} 
			
			/* strings must contain a positive integer */
			else if (value instanceof String) {
				int intVal = 0;
				try {
					intVal = Integer.parseInt((String) value);
				} catch (NumberFormatException ex) {
					return ErrorCode.BAD_VALUE;
				}
				stringToSet = Integer.toString(intVal);
			}
			break;
		
		/* For SLOT_TYPE_TEXT, the input must be an Integer or String. */
		case ISlotTypes.SLOT_TYPE_TEXT:
			stringToSet = value.toString();
			break;
		
		/* all other slotTypes are BAD_VALUE */
		default:
			return ErrorCode.BAD_VALUE;
		}
		
		/*
		 * The value is now valid, so let's insert it into the database. First, try to update
		 * an existing value, but if that fails, add a new entry.
		 */
		try {
			updateValuePrepStmt.setString(1, stringToSet);
			updateValuePrepStmt.setInt(2, ownerType);
			updateValuePrepStmt.setInt(3, ownerId);
			updateValuePrepStmt.setInt(4, slotId);
			int count = db.executePrepUpdate(updateValuePrepStmt);
			
			/* no existing record, insert instead */
			if (count == 0) {
				insertValuePrepStmt.setInt(1, ownerType);
				insertValuePrepStmt.setInt(2, ownerId);
				insertValuePrepStmt.setInt(3, slotId);
				insertValuePrepStmt.setString(4, stringToSet);
				db.executePrepUpdate(insertValuePrepStmt);
			}
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * For the specified action/sub-package, retrieve the specified slot's value. If the value
	 * has not been explicitly set for this action, the slot default value will be returned.
	 * 
	 * @param ownerType Either SLOT_OWNER_ACTION or SLOT_OWNER_PACKAGE.
	 * @param ownerId	The action that the slot is attached to.
	 * @param slotId	The slot that's connected to the action.
	 * @return The slot's value (typically Integer or String), or null if actionId/slotId can't
	 * be mapped to a valid slot.
	 */
	public Object getSlotValue(int ownerType, int ownerId, int slotId) {

		/* get details about this slot (default value, type, etc) */
		SlotDetails slotDetails = getSlotByID(slotId);
		if (slotDetails == null) {
			return null;
		}
		
		/*
		 * Check if there's already a value set of ownerType/actionId/slotId.
		 */
		String results[] = null;
		try {
			findValuePrepStmt.setInt(1, ownerType);
			findValuePrepStmt.setInt(2, ownerId);
			findValuePrepStmt.setInt(3, slotId);
			results = db.executePrepSelectStringColumn(findValuePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/*
		 * We found a result, so convert it to appropriate type.
		 */
		if (results.length == 1) {
			String value = results[0];
			
			switch (slotDetails.slotType) {
			
			/* FileGroups are returned as Integer */
			case ISlotTypes.SLOT_TYPE_FILEGROUP:
				try {
					return Integer.parseInt(value);
				} catch (NumberFormatException e) {
					return null;
				}
				
			/* Text slots are returned as String */
			case ISlotTypes.SLOT_TYPE_TEXT:
				return value;
				
			default:
				return null;
			}
		}
		
		/*
		 * If there's no value set, use the default value for slotId.
		 */
		return slotDetails.defaultValue;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The BuildStore that owns this SlotMgr object.
	 */
	IBuildStore getBuildStore() {
		return buildStore;
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Determine whether a potential slot name is valid (at least 3 characters, starts with
	 * a letter, contains only letters, digits, underscore or dash).
	 * @param slotName The proposed slot name.
	 * @return True if the name is valid, else false.
	 */
	private boolean isValidSlotName(String slotName) {
		int length;
		
		/* check minimum length requirements */
		if ((slotName == null) || (length = slotName.length()) < 3) {
			return false;
		}
		
		/* first character must be a letter */
		char firstCh = slotName.charAt(0);
		if (!Character.isLetter(firstCh)) {
			return false;
		}
		
		/* remaining characters must be letters, digits, _ or - */
		for (int i = 1; i != length - 1; i++) {
			char ch = slotName.charAt(i);
			boolean validChar = Character.isLetter(ch) || Character.isDigit(ch) ||
										(ch == '-') || (ch == '_');
			if (!validChar) {
				return false;
			}
		}
		return true;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
