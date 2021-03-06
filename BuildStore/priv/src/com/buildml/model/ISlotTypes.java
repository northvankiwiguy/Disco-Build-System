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

package com.buildml.model;

import java.util.Arrays;

/**
 * This interface contains constants used when defining or querying action
 * or sub-package slots.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface ISlotTypes {

	/*=====================================================================================*
	 * SLOT OWNERS - Defines whether this slot belongs to a "package" and or "action type"
	 *=====================================================================================*/

	/** The slot is owned by an actionType */
	final static int SLOT_OWNER_ACTION = 1;

	/** The slot is owned by a package */
	final static int SLOT_OWNER_PACKAGE = 2;	
	
	/*=====================================================================================*
	 * SLOT POSITIONS - Defines the location within an actionType or package, where a
	 * slot can appear.
	 *=====================================================================================*/

	/** The slot is in any position (used for searching) */
	public static final int SLOT_POS_ANY 			= 0;
	
	/** The slot is an "input" slot, providing an input FileGroup */
	public static final int SLOT_POS_INPUT 			= 1;
	
	/** The slot is an "output" slot, providing an output/generated FileGroup */	
	public static final int SLOT_POS_OUTPUT 		= 2;
	
	/** The slot is a "parameter" slot, used to customize the action/package's behaviour */
	public static final int SLOT_POS_PARAMETER 		= 3;
	
	/** The slot is a "local" slot, used internally to the action/package */	
	public static final int SLOT_POS_LOCAL	 		= 4;

	/*=====================================================================================*
	 * SLOT TYPES - Defines which values are legal in a particular slot.
	 *=====================================================================================*/

	/** The slot holds FileGroup references */
	public static final int SLOT_TYPE_FILEGROUP 	= 1;
	
	/** The slot holds a boolean value: true/false, yes/no, on/off, 0/non-0 */
	public static final int SLOT_TYPE_BOOLEAN   	= 2;
	
	/** The slot holds an integer value */
	public static final int SLOT_TYPE_INTEGER 		= 4;
	
	/** The slot holds a free-text value, possibly spread over multiple lines */
	public static final int SLOT_TYPE_TEXT      	= 5;
	
	/** The slot holds a value from a specific enumeration */
	public static final int SLOT_TYPE_ENUMERATION   = 6;
	
	/** The slot holds the pathID of a directory */
	public static final int SLOT_TYPE_DIRECTORY     = 7;

	/** The slot holds the pathID of a file */
	public static final int SLOT_TYPE_FILE          = 8;

	/*=====================================================================================*
	 * SLOT CARD - Defines the cardinality of the slot. That is, how many values
	 * can/should this slot contain.
	 *=====================================================================================*/
	
	/** The slot is optional and is not required to contain a value */
	final static int SLOT_CARD_OPTIONAL = 1;
	
	/** There must be a single value in this slot */
	final static int SLOT_CARD_REQUIRED = 2;
	
	/** Each value in this slot will be passed to a unique instance of the action */
	final static int SLOT_CARD_MULTI = 3;
	
	/*=====================================================================================*
	 * SLOT INTERPRETER - Defines which language interpreters can be used to evaluate
	 * a slot.
	 *=====================================================================================*/
	
	/** The slot value is plain text, and is interpreted verbatim */
	public static final String SLOT_INTERPRETER_TEXT 		= "!Text";

	/** The slot value is shell code, with the output of the shell command used as the slot's value */
	public static final String SLOT_INTERPRETER_SHELL 		= "!Shell";

	/** The slot value is Java code, with standard output used as the slot's value. */
	public static final String SLOT_INTERPRETER_JAVA 		= "!Java";

	/** The slot value is python code, with standard output used as the slot's value */
	public static final String SLOT_INTERPRETER_PYTHON 		= "!Python";
	
	/*=====================================================================================*
	 * SLOT DETAILS - Returned by methods that query slot details
	 *=====================================================================================*/
	
	/**
	 * A class specifically used for returning slot details.
	 */
	public class SlotDetails {
		
		/** The slot's ID number */
		public int slotId;
		
		/** The owner type of the slot (attached to action = 1, attached to package = 2) */
		public int ownerType;
		
		/** The actionId or packageId of the owner of this slot */
		public int ownerId;
		
		/** The name of the slot */
		public String slotName;
		
		/** The textual description of this slot (can be long and multi-line) */
		public String slotDescr;
		
		/** The type of slot (SLOT_TYPE_FILEGROUP, etc). */
		public int slotType;
		
		/** The position of the slot (SLOT_POS_INPUT, etc) */
		public int slotPos;
		
		/** Cardinality of this slot (SLOT_CARD_OPTIONAL, SLOT_CARD_REQUIRED, SLOT_CARD_MULTI */
		public int slotCard;
		
		/** If not mandatory, what is the default value */
		public Object defaultValue;
		
		/** For slotType == SLOT_TYPE_ENUMERATION, what are the legal values */
		public String [] enumValues;
		
		/**
		 * Create a new SlotDetails, from the constituent fields.
		 * 
		 * @param slotId		The new slotId.
		 * @param ownerType		The new ownerType.
		 * @param ownerId		The new ownerId.
		 * @param slotName		The new slotName.
		 * @param slotDescr		The new slotDescr.
		 * @param slotType		The new slotType.
		 * @param slotPos		The new slotPos.
		 * @param slotCard		The new slotCard.
		 * @param defaultValue	The new defaultValue.
		 * @param enumValues	The new enumValues.
		 */
		public SlotDetails(int slotId, int ownerType, int ownerId, String slotName, String slotDescr,
							int slotType, int slotPos, int slotCard, Object defaultValue, String [] enumValues) {
			this.slotId = slotId;
			this.ownerType = ownerType;
			this.ownerId = ownerId;
			this.slotName = slotName;
			this.slotDescr = slotDescr;
			this.slotType = slotType;
			this.slotPos = slotPos;
			this.slotCard = slotCard;
			this.defaultValue = defaultValue;
			this.enumValues = enumValues;
		}
		
		/**
		 * Copy constructor.
		 * @param other The existing SlotDetails object to copy.
		 */
		public SlotDetails(SlotDetails other) {
			this.slotId = other.slotId;
			this.ownerType = other.ownerType;
			this.ownerId = other.ownerId;
			this.slotName = other.slotName;
			this.slotDescr = other.slotDescr;
			this.slotType = other.slotType;
			this.slotPos = other.slotPos;
			this.slotCard = other.slotCard;
			this.defaultValue = other.defaultValue;
			if (other.enumValues != null) {
				this.enumValues = Arrays.copyOf(other.enumValues, other.enumValues.length);
			}
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
}

