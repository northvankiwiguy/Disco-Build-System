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

package com.buildml.eclipse.actions;

import com.buildml.model.types.TaskRecord;

/**
 * A type of ActionRecord that's specifically used for displaying in the UI.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class UIActionRecord extends TaskRecord {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** 
	 * Records whether this tree element is currently "expanded". That is, is the text
	 * command for this action compressed onto one line (expandedText == false), or is
	 * it expanded to cover multiple lines (expandedText == true).
	 */
	private boolean expandedText = false;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new UIActionRecord, with the specified "id".
	 * @param id The unique ID that describes this UIActionRecord.
	 */
	public UIActionRecord(int id) {
		super(id);
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * @return the expandedText
	 */
	public boolean isExpandedText() {
		return expandedText;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @param expandedText the expandedText to set
	 */
	public void setExpandedText(boolean expandedText) {
		this.expandedText = expandedText;
	}
	
	/*-------------------------------------------------------------------------------------*/	
}
