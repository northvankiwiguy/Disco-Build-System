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

	/**
	 * Given a multi-line string, display each line of that string after indenting by
	 * the specified number of spaces, and wrapping lines at the specific line width.
	 * Every effort is made to wrap lines at a word boundary, rather than splitting
	 * words across different lines. A line that is wrapped will end with a "\" character.
	 * Note that the wrapWidth includes the number of spaces in indentLevel. That is,
	 * if indentLevel is 20, and wrapWidth is 40, then up to 20 characters will be displayed
	 * per line.
	 * @param out The stream to write to (such as System.out) 
	 * @param string The string to indent, wrap and display
	 * @param indentLevel The number of characters to indent by
	 * @param wrapWidth The column number at which to wrap
	 */
	public static void indentAndWrap(PrintStream out, String string, int indentLevel, int wrapWidth) {
		
		// TODO: implement this properly.
		// TODO: implement tests for this method
		
		int pos = 0;
		int stringLen = string.length();
		
		while (pos < stringLen){
			/* figure out how long the next line is - it might not be \n terminated */
			int newLinePos = string.indexOf('\n', pos);
			if (newLinePos == -1) {
				newLinePos = string.length();
			}
		
			/* display the current line (a substring) */
			indent(out, indentLevel);
			out.println(string.substring(pos, newLinePos));
			
			/* prepare for printing the next line */
			pos = newLinePos + 1;
		}
	}
}
