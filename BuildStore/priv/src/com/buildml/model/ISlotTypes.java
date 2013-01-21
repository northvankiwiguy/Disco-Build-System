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

/**
 * This interface contains constants used when defining or querying action
 * or sub-package slots.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface ISlotTypes {

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
		
		/** The name of the slot */
		public String slotName;
		
		/** The type of slot (SLOT_TYPE_FILEGROUP, etc). */
		public int slotType;
		
		/** The position of the slot (SLOT_POS_INPUT, etc) */
		public int slotPos;
		
		/** True if the slot is mandatory (actions/sub-packages must define it) */
		public boolean isRequired;
		
		/** If not mandatory, what is the default value */
		public Object defaultValue;
		
		/** For slotType == SLOT_TYPE_ENUMERATION, what are the legal values */
		public String [] enumValues;
		
		@SuppressWarnings("javadoc")
		public SlotDetails(int slotId, String slotName, int slotType, int slotPos, 
							boolean isRequired, Object defaultValue, String [] enumValues) {
			this.slotId = slotId;
			this.slotName = slotName;
			this.slotType = slotType;
			this.slotPos = slotPos;
			this.isRequired = isRequired;
			this.defaultValue = defaultValue;
			this.enumValues = enumValues;
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
}

