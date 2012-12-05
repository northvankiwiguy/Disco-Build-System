/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.utils.dnd;

import com.buildml.model.IBuildStore;

/**
 * When dragging and dropping an element between BuildML views/editors, this class
 * encapsulates information about the element. Given that elements are only
 * be dragged between views/editors of the same BuildML file, we only need
 * to store the element type (file, view, package, package folder etc), and the
 * internal ID number.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class BuildMLTransferType {

	/*=====================================================================================*
	 * PUBLIC FIELDS
	 *=====================================================================================*/

	/** we're transferring a file */
	public static final int TYPE_FILE = 1;
		
	/** we're transferring an action */
	public static final int TYPE_ACTION = 2;
		
	/** we're transferring a package */
	public static final int TYPE_PACKAGE = 3;
		
	/** we're transferring a package folder */
	public static final int TYPE_PACKAGE_FOLDER = 4;
	
	/** 
	 * The Object ID of the BuildStore that owns this data item. Given that data items
	 * shouldn't be dragged between views/editors from different BuildStores, this field
	 * is used to check that source and destination have the same BuildML file.
	 */
	public String owner;
	
	/** This item's type (TYPE_FILE, TYPE_ACTION, etc) */
	public int type;
	
	/** The internal numeric ID of this item */
	public int id;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new BuildMLTransferType object, which represents an item in the BuildML
	 * system that can be dragged/dropped between views/editors. 
	 * 
	 * @param owner The BuildStore that owns this data item.
	 * @param type  The type of the data item.
	 * @param id    The internal ID number of this data item.
	 */
	public BuildMLTransferType(String owner, int type, int id) {
		this.owner = owner;
		this.type = type;
		this.id = id;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
