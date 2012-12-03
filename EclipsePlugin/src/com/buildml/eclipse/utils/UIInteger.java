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

package com.buildml.eclipse.utils;

/**
 * A simple class that holds a single integer value. This is somewhat similar to the
 * standard Integer class, but is not marked as "final" and can therefore be sub-classed.
 * Objects of this class (and sub-classes) can be used as the elements in a SWT tree/table
 * viewer, therefore being used to identify items by their ID.
 * 
 * For example, the UIPackage and UIPackageFolder classes are both derived from UIInteger.
 * The UIPackage class is used to represent BuildML packages, whereas UIPackageFolder
 * represents BuildML package folders. We can use "instanceof" to distinguish between these
 * types when an element is selected in the Eclipse UI.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class UIInteger {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The integer ID number for this UIInteger object */
	private int id;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new UIInteger object, which is typically used as a member of an SWT
	 * tree/table viewer.
	 *  
	 * @param id The integer ID number to associate with this tree/table entry.
	 */
	public UIInteger(int id) {
		this.id = id;
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * @return The integer ID for this object.
	 */
	public int getId() {
		return id;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set this object's integer ID.
	 * @param id The integer ID for this object.
	 */
	public void setId(int id) {
		this.id = id;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof UIInteger)) {
			return false;
		}
		return ((UIInteger)obj).getId() == id;	
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return id;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
