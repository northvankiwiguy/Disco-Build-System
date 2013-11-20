/*******************************************************************************
 * Copyright (c) 2013 Arapiki Solutions Inc.
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

import org.eclipse.emf.ecore.impl.EObjectImpl;

import com.buildml.utils.errors.ErrorCode;

/**
 * A "business object" representing a connection between two objects on a package diagram.
 * The Graphiti framework recognizes this object and something that can be rendered on
 * a package diagram. This class is intended to be subclassed to represent different types
 * of connection.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public abstract class UIConnection extends EObjectImpl {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/** The filter group's ID, or ErrorCode.NOT_FOUND if none attached */
	protected int filterGroupId = ErrorCode.NOT_FOUND;
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * @return whether this connection has a filter group attached.
	 */
	public boolean hasFilter() {
		return (filterGroupId != ErrorCode.NOT_FOUND);
	}

	/*-------------------------------------------------------------------------------------*/	

	/**
	 * Remove the attached filter group.
	 */
	public void removeFilter() {
		filterGroupId = ErrorCode.NOT_FOUND;
	}

	/*-------------------------------------------------------------------------------------*/	

	/**
	 * @return the ID of attached filter group, or ErrorCode.NOT_FOUND if not attached.
	 */
	public int getFilterGroupId() {
		return filterGroupId;
	}

	/*-------------------------------------------------------------------------------------*/	

	/**
	 * Set the attached filter group ID.
	 * @param filterGroupId ID of the filter group to attach to this connection.
	 */
	public void setFilterGroupId(int filterGroupId) {
		this.filterGroupId = filterGroupId;
	}

	/*-------------------------------------------------------------------------------------*/	
}
