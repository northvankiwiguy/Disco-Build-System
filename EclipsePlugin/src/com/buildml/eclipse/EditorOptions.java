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

package com.buildml.eclipse;

/**
 * Selectable options that one or more BuildML editors can be affected by.
 * Not all editors support all of these options, but they're enumerated
 * here to provide a central registry of option numbers.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public interface EditorOptions {
	
	/*-------------------------------------------------------------------------------------*/

	/** 
	 * Option to enable coalescing of folders in the file editor. That is, if a folder
	 * contains a single child which is itself a folder, we display both of them on the
	 * same line. For example, if folder "A" has a single child, "B", we display "A/B".
	 */
	public static final int OPT_COALESCE_DIRS = 1;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Option to display file roots as the top-level items in the editor. Without this
	 * feature enabled, the top-level directory names will be shown.
	 */
	public static final int OPT_SHOW_ROOTS = 2;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Option to display each items package name, alongside the item name itself.
	 */
	public static final int OPT_SHOW_PACKAGES = 4;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Option to reveal hidden items in the viewer, displaying them in a greyed-out style.
	 */
	public static final int OPT_SHOW_HIDDEN = 8;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * How many options currently exist?
	 */
	public static final int NUM_OPTIONS = 4;

	/*-------------------------------------------------------------------------------------*/
}
