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

package com.arapiki.disco.eclipse.utils;

/**
 * An interface used in conjunction with the VisibilityTreeViewer class to
 * provide knowledge about which TreeViewer elements should be visible (in
 * the Tree widget). An implementor of this interface is expected to
 * query the underlying model to get and set the visibility information.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public interface IVisibilityProvider {

	/*=====================================================================================*
	 * INTERFACE METHODS
	 *=====================================================================================*/

	/**
	 * Queries the underlying model to see whether the specified TreeViewer
	 * element should be marked as visible.
	 * @param element The TreeViewer element
	 * @return True if the element is visible, else false.
	 */
	public boolean isVisible(Object element);
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Update the underlying model to the visibility state of the specified 
	 * TreeViewer element.
	 * @param element The TreeViewer element for which visibility should be set.
	 * @param visible True if the element should visible, else false.
	 */
	public void setVisibility(Object element, boolean visible);
	
	/*-------------------------------------------------------------------------------------*/
}
