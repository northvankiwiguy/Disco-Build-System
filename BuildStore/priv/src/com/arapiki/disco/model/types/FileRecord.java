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

import com.buildml.utils.types.IntegerTreeRecord;

/**
 * This class contains a summary of a single path with the BuildStore. FileRecord is 
 * used as the element type in a FileSet, as well as other "report" methods that return 
 * information about a collection of files.
 *
 * The set of FileRecord fields that are filled out by the report depends on the nature
 * of the report. For example, if the 'size' field isn't provided by the report, it's
 * left empty.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class FileRecord extends IntegerTreeRecord {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/**
	 * Used whenever a report returns the count of something (such as number of occurrences). This
	 * may vary depending on the report.
	 */
	private int count;
	
	/**
	 * Used whenever a report returns the size of something. This may vary depending on the report.
	 */
	private int size;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new instance of FileRecord, with all files set to default values.
	 */
	public FileRecord() {
		id = 0;
		count = 0;
		size = 0;
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Create a new instance of FileRecord, with the id field set.
	 * 
	 * @param id The ID number of this FileRecord
	 */
	public FileRecord(int id) {
		this();
		this.id = id;
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Return the count field.
	 * @return The count field.
	 */
	public int getCount() {
		return count;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set the count field.
	 * 
	 * @param count The count value to set.
	 */
	public void setCount(int count) {
		this.count = count;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the size field.
	 * @return The size value.
	 */
	public int getSize() {
		return size;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set the size field.
	 * @param size The size value to set.
	 */
	public void setSize(int size) {
		this.size = size;
	}

	/*-------------------------------------------------------------------------------------*/
}

