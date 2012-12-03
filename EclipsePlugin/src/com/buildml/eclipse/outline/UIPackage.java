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

package com.buildml.eclipse.outline;

import com.buildml.eclipse.utils.UIInteger;

/**
 * A type used in Eclipse TreeViewers to represent a "package".
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class UIPackage extends UIInteger {

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new UIPackage, which represents a BuildML package as displayed in an SWT
	 * tree/table.
	 * 
	 * @param id The underlying ID number for the BuildML package.
	 */
	public UIPackage(int id) {
		super(id);
	}
	
	/*-------------------------------------------------------------------------------------*/
}
