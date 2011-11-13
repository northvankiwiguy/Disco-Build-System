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

package com.arapiki.disco.model.types;

import com.arapiki.utils.types.IntegerTreeRecord;

/**
 * This class is used when returning results from a report query, provided by methods 
 * in the Reports class. Each report returns 0 or more of these objects, either as an array
 * or within a TaskSet. The set of TaskRecord fields that are filled out by the report
 * depends on the nature of the report.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TaskRecord extends IntegerTreeRecord {

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new instance of TaskRecord, with the id field set
	 * @param id The ID number of this TaskRecord
	 */
	public TaskRecord(int id) {
		super();
		this.id = id;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
