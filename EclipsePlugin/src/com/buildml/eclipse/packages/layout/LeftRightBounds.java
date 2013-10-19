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

package com.buildml.eclipse.packages.layout;

/**
 * A return value specifying the left and right X coordinates for a range on
 * a diagram's canvas.
 */
public class LeftRightBounds {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** the left X coordinate of the range */
	public int leftBound;
		
	/** the right X coordinate of the range */
	public int rightBound;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new LeftRightBounds object.
	 * 
	 * @param leftBound		The left X coordinate of the range.
	 * @param rightBound	The right X coordinate of the range.
	 */
	public LeftRightBounds(int leftBound, int rightBound) {
		this.leftBound = leftBound;
		this.rightBound = rightBound;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
