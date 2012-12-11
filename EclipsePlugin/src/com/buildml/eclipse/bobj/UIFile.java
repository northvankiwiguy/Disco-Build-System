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

package com.buildml.eclipse.bobj;


/**
 * A sub-class of UIInteger used to represent "file" objects in the Eclipse UI.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class UIFile extends UIInteger {

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new UIFile.
	 * @param pathId The ID of this directory (as managed by the FileMgr).
	 */
	public UIFile(int pathId) {
		super(pathId);
	}

	/*-------------------------------------------------------------------------------------*/
}
