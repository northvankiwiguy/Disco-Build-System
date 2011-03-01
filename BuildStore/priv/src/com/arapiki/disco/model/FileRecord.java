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

package com.arapiki.disco.model;

/**
 * This class is used when returning results from a report query, provided by methods 
 * in the Reports class. Each report returns 0 or more of these objects, either as an array
 * or within a FileSet. The set of FileRecord fields that are filled out by the report
 * depends on the nature of the report. For example, if the 'size' field isn't provided
 * by the report, it's left empty.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class FileRecord {
	
	/**
	 * The path's ID number
	 */
	public int pathId;	
	
	/**
	 * Used whenever a report returns the count of something (such as number of occurrences). This
	 * may vary depending on the report.
	 */
	public int count;
	
	/**
	 * Used whenever a report returns the size of something. This may vary depending on the report.
	 */
	public int size;
}

