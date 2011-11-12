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
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class ShellCommandUtils {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Given a (possibly) multi-line shell command, merge that command into a single line.
	 * If a particular line is terminated by a \ character, the next line is considered
	 * to be the same command. If not, the current line and next line will be joined together
	 * with && (as opposed to ; which would allow the second command to continue, even if 
	 * the first command failed). Finally, leading spaces/tabs are removed off all lines.
	 * @param cmdLine The (possibly) multi-line shell command
	 * @return The joined-together shell command
	 */
	public static String joinCommandLine(String cmdLine) {
		
		/* 
		 * Create a StringBuffer for storing the result. We'll append to this
		 * as we progress through the lines.
		 */
		StringBuffer sb = new StringBuffer(256);
		
		/* 
		 * We'll traverse the string, line by line, with startPos being the start
		 * index of the next line.
		 */
		int startPos = 0;
		
		/* we'll track whether the next line should be prepend by && */
		boolean andAndRequiredNextTime = false;
		
		/* repeat until we've processed every line of the input */
		int cmdLineLen = cmdLine.length();
		while (startPos < cmdLineLen) {
			/*
			 * Find the index of the end of the current line (possibly the end of the whole
			 * string).
			 */
			int nlPos = cmdLine.indexOf('\n', startPos);
			if (nlPos == -1) {
				nlPos = cmdLine.length();
			}
			
			/* trim any leading spaces/tabs */
			while (startPos < nlPos) {
				char ch = cmdLine.charAt(startPos);
				if ((ch != ' ') && (ch != '\t')){
					break;
				}
				startPos++;
			}
			
			/* ignore lines that are empty */
			if (startPos < nlPos) {

				/* if the previous line didn't end with \, we should insert a && */
				if (andAndRequiredNextTime) {
					sb.append(" && ");
				}
				
				/* 
				 * Does this line end with \? If so, we discard the \ and merge this
				 * line with the next (no && required)
				 */
				if (cmdLine.charAt(nlPos - 1) == '\\') {
					sb.append(cmdLine.substring(startPos, nlPos - 1));
					andAndRequiredNextTime = false;
				} 
			
				/* 
				 * No it doesn't, so print a && before the next line
				 */
				else {
					sb.append(cmdLine.substring(startPos, nlPos));
					andAndRequiredNextTime = true;
				}
			} 
			
			/* 
			 * Empty lines require a && before the next command, since it's
			 * definitely the case that the previous command has ended.
			 */
			else {
				andAndRequiredNextTime = true;
			}
			
			/* the next line starts immediately after this one */
			startPos = nlPos + 1;
		}
		
		return sb.toString();
	}
	
	/*-------------------------------------------------------------------------------------*/
}
