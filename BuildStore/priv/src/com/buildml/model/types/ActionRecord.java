/*******************************************************************************
 * Copyright (c) 2011 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.model.types;

import com.buildml.utils.types.IntegerTreeRecord;

/**
 * This class contains a summary of a single action. ActionRecord is used as the element
 * type in a ActionSet, as well as other methods that return information about a collection
 * of tasks.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class ActionRecord extends IntegerTreeRecord {

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new instance of ActionRecord, with the id field set.
	 * 
	 * @param id The ID number of this ActionRecord.
	 */
	public ActionRecord(int id) {
		super();
		this.id = id;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
