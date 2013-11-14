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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A static utility class for handling regular expressions.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class BmlRegex {
	
	/*=====================================================================================*
	 * PUBLIC STATIC METHODS
	 *=====================================================================================*/

	/**
	 * Given a regular expression in Ant syntax, convert it into a Java regular expression
	 * and return it as 
	 * @param antRegex	The regular expression in Ant syntax (using *, ?, **)
	 * @return The equivalent Java regular expression.
	 */
	public static String convertAntToJavaRegex(String antRegex) {

		/*
		 * The rules for conversion are:
		 *   ** - replaced by ".*" - matches multiple path components.
		 *   *  - replaced by "[^/]*" - matches multiple characters, but not across path components.
		 *   ?  - replaced by "." - matches a single character.
		 *   .  - replaced by \. - to stop it functioning as a wildcard.
		 *   [, $, ^, &, + are quoted with \ to stop them being treated as special.
		 */
		
		/* create an output StringBuffer with (hopefully) enough space for the translated string */
		int len = antRegex.length();
		StringBuffer sb = new StringBuffer((int) (len * 1.5));
		
		/* pattern starts from beginning of string */
		sb.append("^");

		/* for each character in the input string, map from Ant syntax to Java syntax */
		for (int i = 0; i != len; i++) {
			char thisCh = antRegex.charAt(i);
			char nextCh = '\0';
			if ((i+1) != len) {
				nextCh = antRegex.charAt(i+1);
			}
			
			switch (thisCh) {
			case '*':
				if (nextCh == '*') {
					sb.append(".*");
				} else {
					sb.append("[^/]*");;
				}
				break;
			case '?':
				sb.append('.');
				break;
			case '.': case '[': case '$':
			case '^': case '&': case '+':
				sb.append('\\');
				sb.append(thisCh);				
				break;
			default:
				sb.append(thisCh);
				break;
			}
		}
		
		/* trailing / implies /.* */
		if (antRegex.charAt(len - 1) == '/') {
			sb.append(".*");
		}
		
		/* pattern must cover the whole string */
		sb.append("$");
		return sb.toString();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Returns true or false to indicate whether a candidate string matches the provided
	 * regular expression.
	 * 
	 * @param stringToMatch	The string to compare against the pattern.
	 * @param regex			The regular expression in Java regex syntax.
	 * @return True if the string matches the pattern, else false.
	 * @throws PatternSyntaxException If regex has an invalid format.
	 */
	public static boolean matchRegex(String stringToMatch, String regex) 
			throws PatternSyntaxException {
		
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(stringToMatch);
		return matcher.matches();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Returns true or false to indicate whether a candidate string matches the provided
	 * regular expression (in Ant regex syntax).
	 * 
	 * @param stringToMatch	The string to compare against the pattern.
	 * @param antRegex		The regular expression in Ant syntax (using *, ?, **)
	 * @return True if the string matches the pattern, else false.
	 * @throws PatternSyntaxException If antRegex has an invalid format.
	 */
	public static boolean matchAntRegex(String stringToMatch, String antRegex) 
			throws PatternSyntaxException {
		
		String regex = convertAntToJavaRegex(antRegex);
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(stringToMatch);
		return matcher.matches();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given an array of regex pattern strings, compile the strings into a single RegexChain
	 * object. A chain is a sequence of regular expressions that can be matched against a
	 * candidate string. When matching a string against a chain, we pay attention to whether
	 * the chain members "include" or "exclude" the string. This will determine whether the
	 * candidate string should match or not-match the whole RegexChain.
	 * 
	 * Each entry in the chain must start with a prefix to state the type of the regex. Valid
	 * prefixes are:
	 *    "ia:" - An "inclusion" rule in Ant syntax.
	 *    "ea:" - An "exclusion" rule in Ant syntax.
	 *    "ij:" - An "inclusion" rule in Java syntax.
	 *    "ej:" - An "exclusion" rule in Java syntax.
	 * 
	 * @param regexChain An array of regular expressions, each string prefixed by one of the
	 * 					 above prefixes.
	 * @return The RegexChain object.
	 * @throws PatternSyntaxException If one of the input patterns has an invalid format.
	 */
	public static RegexChain compileRegexChain(String regexChain[]) 
			throws PatternSyntaxException {
		
		RegexChain chain = new RegexChain();
		if (regexChain == null) {
			return chain;
		}
		
		for (int i = 0; i < regexChain.length; i++) {
			String regexString = regexChain[i];
			if (regexString == null) {
				throw new PatternSyntaxException("Invalid regex string", regexString, 0);
			}
			
			String parts[] = regexString.split(":");
			
			int regexType;
			String regexExpr;

			/* includes regex using Ant syntax */
			if ("ia".equals(parts[0])) {
				regexType = RegexChain.TYPE_INCLUDES;
				regexExpr = convertAntToJavaRegex(parts[1]);
			}
			
			/* excludes regex using Ant syntax */
			else if ("ea".equals(parts[0])) {
				regexType = RegexChain.TYPE_EXCLUDES;
				regexExpr = convertAntToJavaRegex(parts[1]);				
			}
			
			/* includes regex using Java syntax */
			else if ("ij".equals(parts[0])) {
				regexType = RegexChain.TYPE_INCLUDES;
				regexExpr = parts[1];				
			}

			/* excludes regex using Java syntax */
			else if ("ej".equals(parts[0])) {
				regexType = RegexChain.TYPE_EXCLUDES;
				regexExpr = parts[1];				
			}
			
			/* other prefixes are invalid */
			else {
				throw new PatternSyntaxException("Invalid regex prefix", regexString, 0);
			}
			
			/* convert the regex to a Pattern, and add it to our chain */
			Pattern pattern = Pattern.compile(regexExpr);
			chain.addEntry(regexType, pattern);
		}
		
		return chain;
	}
	
	/*-------------------------------------------------------------------------------------*/

	
	/**
	 * Match a string against a regular expression chain. Rules in the chain visited in order
	 * they're provided (within the array). If an "include" matches the pattern, the result
	 * is true, unless there's an "exclude" that later invalidates the match. Strings that
	 * have been explicitly excluded can not be included again by a later pattern.
	 * 
	 * Initially the string will be in the excluded state so if it never matches any "include"
	 * regexes, the result will be false.
	 * 
	 * @param stringToMatch	The string we're matching against the regex chain.
	 * @param regexChain	The chain of regular expressions to match against.
	 * @return True if the stringToMatch is included by the chain, else false.
	 */
	public static boolean matchRegexChain(String stringToMatch, RegexChain regexChain) {

		if (regexChain == null) {
			return false;
		}
		
		/* strings are excluded by default */
		boolean included = false;
		int len = regexChain.getSize();
		
		/* traverse the chain until the end, or until the string is excluded */
		for (int i = 0; i != len; i++) {
			int regexType = regexChain.getType(i);
			Pattern regexPattern = regexChain.getPattern(i);
			Matcher m = regexPattern.matcher(stringToMatch);

			if (m.matches()) {
				
				/* if we've matched an "include", we still need to check for future excludes */
				if (regexType == RegexChain.TYPE_INCLUDES) {
					included = true;
				} 
				
				/* we matched an "exclude", there's no way to re-include at this point, so end now */
				else if (regexType == RegexChain.TYPE_EXCLUDES) {
					return false;
				}
			}
		}
		
		return included;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Match an array of strings against a regular expression chain and return a new array
	 * containing only the strings that match. If an "include" matches the pattern, the result
	 * is true, unless there's an "exclude" that later invalidates the match. Strings that
	 * have been explicitly excluded can not be included again by a later pattern.
	 * 
	 * @param stringsToMatch	The array of string we're matching against the regex chain.
	 * @param regexChain		The chain of regular expressions to match against.
	 * @return The array of strings containing only those input strings that matched the regex chain,
	 * or null if any of the inputs are invalid.
	 */
	public static String[] filterRegexChain(String stringsToMatch[], RegexChain regexChain) {
		
		if ((regexChain == null) || (stringsToMatch == null)) {
			return null;
		}
		
		ArrayList<String> result = new ArrayList<String>();
		for (int i = 0; i < stringsToMatch.length; i++) {
			if (matchRegexChain(stringsToMatch[i], regexChain)) {
				result.add(stringsToMatch[i]);
			}
		}
		return result.toArray(new String[result.size()]);
	}

/*-------------------------------------------------------------------------------------*/

}
