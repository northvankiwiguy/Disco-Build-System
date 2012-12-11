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
 * A sub-class of UIInteger used to represent "file group" objects in the Eclipse UI.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class UIFileGroup extends UIInteger {

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new UIFileGroup.
	 * 
	 * @param groupId The ID of this file group (as managed by FileGroupMgr).
	 */
	public UIFileGroup(int groupId) {
		super(groupId);
	}
	
	/*-------------------------------------------------------------------------------------*/
}
