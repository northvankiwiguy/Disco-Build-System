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

package com.arapiki.utils.types;

/**
 * This is the base class of any Record types (such as FileRecord or TaskRecord) that
 * can be managed by a IntegerTreeSet object. Unless you only want to store ID numbers
 * in the record, you'll need to subclass IntegerTreeRecord.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class IntegerTreeRecord {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	protected int id;

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/*-------------------------------------------------------------------------------------*/
}
