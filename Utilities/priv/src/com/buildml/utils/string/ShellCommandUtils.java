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

package com.buildml.utils.string;

/**
 * Utility methods for manipulating strings that represent shell command lines.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class ShellCommandUtils {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Given a (possibly) multi-line shell command, merge that command into a single line.
	 * If a particular line is terminated by a \ character, the next line is considered
	 * to be the same command. If not, the current line and next line will be joined together
	 * with "&&".
	 * <p>
	 * Finally, leading spaces/tabs are removed off all lines.
	 * 
	 * @param cmdLine The (possibly) multi-line shell command.
	 * @return The joined-together shell command.
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

	/**
	 * Given a string containing a shell command argument, escape the string so that it may be
	 * entered on the shell command line. For example, an argument that contains spaces must
	 * be surrounded by quotation marks. Also, a quotation mark in an argument must be quoted
	 * with a backslash (\").
	 * 
	 * @param input The unescaped shell command arguments.
	 * @return The shell-escaped version of the argument.
	 */
	public static String shellEscapeString(String input) {

		boolean needsQuoting = false;
		int inputLen = input.length();
		for (int i = 0; i != inputLen; i++) {
			char ch = input.charAt(i);
			if (ch == ' ' || ch == '\r' || ch == '\n' || ch == '\t' ||
					ch == '(' || ch == ')' || ch == '<' || ch == '>' ||
					ch == ';' || ch == '&' || ch == '!' || ch == '$' ||
					ch == '!' || ch == '$' || ch == '\'' || ch == '*' ||
					ch == '?' || ch == '[' || ch == ']' || ch == '{' ||
					ch == '}') {
				needsQuoting = true;
				break;
			}
		}
		
		/* 
		 * If there are no special characters, no quoting is required. Simply
		 * return the original string.
		 */
		if (!needsQuoting) {
			return input;
		}
		
		/*
		 * Else, create a replacement string, with added '...' and any internal
		 * ' characters prefixed by \.
		 */
		StringBuffer sb = new StringBuffer(inputLen + 10); /* allow room for quotes */
		sb.append('\'');
		for (int i = 0; i != inputLen; i++) {
			char ch = input.charAt(i);
			
			/*
			 * Single quotes behave oddly. You need to terminate the first quote, then
			 * have the \', then restart the quoted string. Therefore, Hello'World
			 * must be escaped as 'Hello'\''World'
			 */
			if (ch == '\'') {
				sb.append("'\\''");
			} else {
				sb.append(ch);
			}
		}
		sb.append('\'');		
		return sb.toString();	
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Summarize a shell command (possibly multi-line) by truncating it (if needed) to the
	 * specified width and putting "..." to indicate that it continues.
	 * 
	 * @param shellCommand The original shell command string (possibly multi-line).
	 * @param width 	   The number of characters to truncate it to.
	 * @return 			   The summary string.
	 *  
	 */
	public static String getCommandSummary(String shellCommand, int width) {
		
		/* 
		 * For now, we treat all commands as being the same, and simply return the
		 * first 'width' characters from the action's command string.
		 */
		String command = ShellCommandUtils.joinCommandLine(shellCommand);
		
		/* for strings that are longer than 'width', truncate them and suffix them with "..." */
		boolean dotsNeeded = false;
		int stringLen = command.length();
		if (stringLen > width - 3) {
			stringLen = width - 3;
			dotsNeeded = true;
		}
		
		/* form the summary string, possibly with ... */
		StringBuffer sb = new StringBuffer(command.substring(0, stringLen));
		if (dotsNeeded){
			sb.append("...");
		}
		
		/* return the summary string */
		return sb.toString();
	}

	/*-------------------------------------------------------------------------------------*/

}
