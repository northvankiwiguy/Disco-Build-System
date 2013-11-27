/*******************************************************************************
 * Copyright (c) 2013 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.model.undo;

/**
 * This interface must be implemented by all undo/redo operations.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface IUndoOp {

	/*=====================================================================================*
	 * INTERFACE METHODS
	 *=====================================================================================*/

	/**
	 * Reverse the effects of the operation on the underlying IBuildStore.
	 * 
	 * @return True if a change actually took place, else false.
	 */
	public boolean undo();
	
	/**
	 * Perform (or re-perform) the operation on the underlying IBuildStore.
	 * 
	 * @return True if a change actually took place, else false.
	 */	
	public boolean redo();
	
	/*-------------------------------------------------------------------------------------*/
}
