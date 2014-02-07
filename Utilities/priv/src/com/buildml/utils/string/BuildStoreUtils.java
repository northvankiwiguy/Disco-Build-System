/*******************************************************************************
 * Copyright (c) 2014 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.utils.string;

/**
 * This class provides various utility functions for manipulating BuildStore-related
 * data. All the methods in this class are static, so they're essentially just worker
 * functions without any state.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class BuildStoreUtils {

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether a potential slot name is valid (at least 3 characters, starts with
	 * a letter, contains only letters, digits, underscore or dash).
	 * @param slotName The proposed slot name.
	 * @return True if the name is valid, else false.
	 */
	public static boolean isValidSlotName(String slotName) {
		int length;
		
		/* check minimum length requirements */
		if ((slotName == null) || (length = slotName.length()) < 3) {
			return false;
		}
		
		/* first character must be a letter */
		char firstCh = slotName.charAt(0);
		if (!Character.isLetter(firstCh)) {
			return false;
		}
		
		/* remaining characters must be letters, digits, _ or - */
		for (int i = 1; i != length; i++) {
			char ch = slotName.charAt(i);
			boolean validChar = Character.isLetter(ch) || Character.isDigit(ch) ||
										(ch == '-') || (ch == '_');
			if (!validChar) {
				return false;
			}
		}
		return true;
	}
	
	/*-------------------------------------------------------------------------------------*/
	


	
	/*-------------------------------------------------------------------------------------*/
}

