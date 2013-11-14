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

package com.buildml.utils.regex;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * An opaque object for representing compiled chains of regular expressions. Objects
 * of this class should only be created or queried by methods in the BmlRegex class.
 * All other classes should treat this as an opaque reference.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class RegexChain {
	
	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/** This entry is invalid */
	/* package */ final static int TYPE_INVALID  = 0;
	
	/** This entry contains a regular expression matching strings to include */
	/* package */ final static int TYPE_INCLUDES = 1;

	/** This entry contains a regular expression matching strings to exclude */
	/* package */ final static int TYPE_EXCLUDES = 2;

	/** our array (one per entry) of types (TYPE_INCLUDES, TYPE_EXCLUDES, etc). */
	private ArrayList<Integer> typeList;

	/** our array (one per entry) of Patterns */
	private ArrayList<Pattern> patternList;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new RegexChain object.
	 */
	/* package */ RegexChain() {
		typeList = new ArrayList<Integer>();
		patternList = new ArrayList<Pattern>();
	}
	
	/*=====================================================================================*
	 * PACKAGE-SCOPE METHODS
	 *=====================================================================================*/

	/**
	 * Add a new entry at the end of the RegexChain.
	 * @param type		The type of the entry (TYPE_INCLUDES, etc).
	 * @param pattern	The Java Regex pattern associated with this entry.
	 */
	/* package */ void addEntry(int type, Pattern pattern) {
		typeList.add(type);
		patternList.add(pattern);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The number of entries in this RegexChain.
	 */
	/* package */ int getSize() {
		return typeList.size();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the Pattern at the specified index.
	 * 
	 * @param index	The index of the Pattern to fetch.
	 * @return The pattern at this index, or null if index is invalid.
	 */
	/* package */ Pattern getPattern(int index) {
		if ((index < 0) || (index >= patternList.size())) {
			return null;
		}
		return patternList.get(index);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the type of the Pattern at the specified index.
	 * 
	 * @param index The index of the Pattern to fetch.
	 * @return The type of this entry (TYPE_INCLUDES, TYPE_EXCLUDES, etc).
	 */
	/* package */ int getType(int index) {
		if ((index < 0) || (index >= typeList.size())) {
			return TYPE_INVALID;
		}
		return typeList.get(index);
	}

	/*-------------------------------------------------------------------------------------*/
}
