/*******************************************************************************
 * Copyright (c) 2011 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.arapiki.utils.string;

/**
 * General purpose methods for manipulating values of the String[] data type.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class StringArray {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Shift the entire String [] left by 'count' elements, discarding the left-most
	 * 'count' elements, then return the resulting String[]. This is similar to the "shift" 
	 * command in most shell languages.
	 * 
	 * @param input The input String array.
	 * @param count The number of elements to shift by.
	 * @return A copy of the input array, but with the left-most 'count' elements removed.
	 */
	public static String[] shiftLeft(String [] input, int count) {
		
		/* 
		 * Shifting by zero or negative numbers is not allowed - just return
		 * a copy of the original array
		 */
		if (count < 1) {
			return input.clone();
		}
		
		/* the case where shifting would create an empty list */
		if (count >= input.length){
			return new String[0];
		}
		
		/* else, we need to move the array elements over */
		String output[] = new String[input.length - count];
		System.arraycopy(input, count, output, 0, input.length - count);
		return output;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Shift the entire String [] left by one element, discarding the left-most
	 * element, then return the resulting String[]. This is similar to the "shift" 
	 * command in most shell languages.
	 * @param input The input String array
	 * @return The same array, but with the left-most element removed.
	 */
	public static String[] shiftLeft(String[] input) {
		return shiftLeft(input, 1);
	}

	/*-------------------------------------------------------------------------------------*/
}
