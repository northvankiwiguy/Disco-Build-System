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
 * A simple class to represent the width and height of a member's pictogram.
 */
public class PictogramSize {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** the member's width (in pixels) */
	private int width;

	/** the member's height (in pixels) */
	private int height;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new PictogramSize object.
	 * @param width 	The width (in pixels) of the pictogram.
	 * @param height 	The height (in pixels) of the pictogram.
	 * 
	 */
	public PictogramSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * @return the width
	 */
	public int getWidth() {
		return width;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @param width the width to set
	 */
	public void setWidth(int width) {
		this.width = width;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return the height
	 */
	public int getHeight() {
		return height;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @param height the height to set
	 */
	public void setHeight(int height) {
		this.height = height;
	}

	/*-------------------------------------------------------------------------------------*/
}
