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
 * This class contains the version information for Disco. The YEAR and VERSION
 * tags are replaced at compile time by the current values.
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class Version {

	/**
	 * @return the copyright year for this version of Disco.
	 */
	public static String getYear() {
		return "@YEAR@";
	}
	
	/**
	 * @return the numeric version number of this version of Disco.
	 */
	public static String getVersionNumber() {
		return "@VERSION@";
	}
	
	/**
	 * @return the full version string for this version of Disco.
	 */
	public static String getVersion()  {
		return "Disco build tool - Version " + getVersionNumber() + 
			". Copyright " + getYear() + " Arapiki Solutions Inc.";
	}
}
