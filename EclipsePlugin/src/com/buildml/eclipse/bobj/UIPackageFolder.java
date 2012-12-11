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

package com.buildml.eclipse.bobj;


/**
 * A type used in Eclipse TreeViewers to represent a "package folder".
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class UIPackageFolder extends UIInteger {

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new UIPackageFolder, which represents a BuildML package folder as displayed
	 * in an SWT tree/table.
	 * 
	 * @param id The underlying ID number for the BuildML package folder.
	 */
	public UIPackageFolder(int id) {
		super(id);
	}
	
	/*-------------------------------------------------------------------------------------*/
}
