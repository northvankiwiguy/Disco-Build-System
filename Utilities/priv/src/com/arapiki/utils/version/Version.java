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
	 * Return the numeric version number of Disco.
	 * @return the numeric version number of Disco.
	 */
	public static String getVersionNumber() {
		return "@VERSION@";
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
