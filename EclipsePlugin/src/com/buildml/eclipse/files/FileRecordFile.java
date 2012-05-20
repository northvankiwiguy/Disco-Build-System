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

package com.buildml.eclipse.files;

import com.buildml.model.types.FileRecord;

/**
 * A subclass of FileRecord used by the UI to distinguish FileRecords that represent
 * files, versus those that represent directories (or other types).
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class FileRecordFile extends FileRecord {

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new FileRecordFile
	 * @param parentId The ID of this FileRecordFile
	 */
	public FileRecordFile(int parentId) {
		super(parentId);
	}

	/*-------------------------------------------------------------------------------------*/
}
