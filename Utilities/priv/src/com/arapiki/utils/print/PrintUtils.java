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
	
		/* at which index within the string does the current line start? */
		int startPos = 0;
		
		/* when a line wraps, we'll indent a couple of extra spaces for the next line */
		int extraIndentNextTime = 0;
		
		/* how long is the total string? */
		int stringLen = string.length();
		
		/* how many columns can be used for text (total width - indent) */
		int wrapAtColumn = wrapWidth - indentLevel;
		
		/* repeat until we've displayed every line of text in the string */
		while (startPos < stringLen){
			
			/* 
			 * Display the appropriate indentation, possibly with extra indentation
			 * because the previous line was wrapped.
			 */
			indent(out, indentLevel + extraIndentNextTime);
			
			/* 
			 * Figure out how long the next line is - it might not be \n terminated,
			 * so we must allow for the EOL case.
			 */
			int endPos = string.indexOf('\n', startPos);
			if (endPos == -1) {
				endPos = string.length();
			}
			
			/* would this line need to wrap? That is, is it too long? */
			boolean willWrap = false;
			if ((endPos - startPos) > (wrapAtColumn - extraIndentNextTime)) {
				
				/* yes, we need to wrap */
				willWrap = true;
				
				/* 
				 * Find a suitable place to break the line. We first calculate
				 * the maximum length at which we might break the line, although
				 * this might be in the middle of a word.
				 */
				endPos = startPos + (wrapAtColumn - extraIndentNextTime);
				
				/*
				 * Now check if there's a convenient space (' '), in the second half of this line,
				 * that would be a better place to break things.
				 */
				int spacePos = string.lastIndexOf(' ', endPos);
				if ((spacePos != -1) && (spacePos <= endPos) && (spacePos > ((startPos + endPos) / 2))){
					endPos = spacePos + 1;
				}
				
				/*
				 * Because we wrapped this line, we should indent the following line
				 * by a couple of spaces, just for visual appeal.
				 */
				extraIndentNextTime = 2;	
			} 
			
			/* no wrap this time, so no indentation next time */
			else {
				extraIndentNextTime = 0;
			}
			
			/* display the current line (a substring), with a trailing \ if we wrapped */
			out.print(string.substring(startPos, endPos));
			if (willWrap) {
				out.print("\\");
			}
			out.println();
			
			/* 
			 * Prepare for printing the next line. If we wrapped, continue from the very next
			 * character. If we didn't wrap, then we need to skip over the \n character */
			if (willWrap) {
				startPos = endPos;
			} else {
				startPos = endPos + 1;
			}
		}
	}
}
