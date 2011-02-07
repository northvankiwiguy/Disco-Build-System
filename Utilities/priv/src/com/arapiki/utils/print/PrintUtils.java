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

package com.arapiki.utils.print;

import java.io.PrintStream;

/**
 * Various utilities for printing things. These are all static methods, so there's no need to
 * create an instance of the class.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class PrintUtils {

	
	/* fast access to printing an arbitrary number of spaces. Used by indent() */
	private static String spaces = "                                                                ";

	/**
	 * Utility function for print spaces. This is useful for indented screen output.
	 * @param out The stream to write to (such as System.out).
	 * @param numSpaces The number of spaces to indent by.
	 */
	public static void indent(PrintStream out, int numSpaces) {
		
		while (true) {
			
			/* as much as possible, just write out a substring of our preinitialized spaces string */
			if (numSpaces <= spaces.length()){
				out.print(spaces.substring(0, numSpaces));
				break;	
			} 
			
			/* but for large indentation, perform multiple writes */
			else {
				out.print(spaces);
				numSpaces -= spaces.length();
			}
		}
		
	}
}
