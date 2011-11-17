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

package com.arapiki.utils.version;

/**
 * This class contains the version information for Disco. At compile time, the
 * source code for this class is duplicated, but with the YEAR and VERSION
 * tags being replace by their correct values. All methods are static, so there's
 * no requirement to instantiate an object.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class Version {

	/**
	 * Return the copyright year for this version of Disco.
	 * @return the copyright year for this version of Disco.
	 */
	public static String getYear() {
		return "@YEAR@";
	}
	
	/**
	 * Return the numeric version number of Disco as a string in the
	 * form X.Y.Z.
	 * @return the numeric version number of Disco.
	 */
	public static String getVersionNumber() {
		return "@VERSION@";
	}
	
	/**
	 * Return the version string as an integer. For example, if the
	 * version is "1.2.3", return "010203". Note that if we build
	 * this class via the Eclipse GUI, the real version number won't
	 * have been embedded into the Version.java file, so we'll instead
	 * return 0.
	 * 
	 * @return The version string in integer form.
	 */
	public static int getVersionNumberAsInt() {
		String verString = getVersionNumber();
		String digits[] = verString.split("\\.");
		
		int num = 0;
		for (String string : digits) {
			int digit = Integer.valueOf(string);
			num = (num * 100) + digit; 
		}
		
		return num;
	}
	
	/**
	 * Return the full version string for Disco.
	 * @return the full version string for Disco.
	 */
	public static String getVersion()  {
		return "Disco build tool - Version " + getVersionNumber() + 
			". Copyright " + getYear() + " Arapiki Solutions Inc.";
	}
}
