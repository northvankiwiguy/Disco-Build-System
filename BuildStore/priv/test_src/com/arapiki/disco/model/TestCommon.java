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

package com.arapiki.disco.model;


/**
 * Common methods for testing BuildStore and related classes.
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestCommon {

	/** The name of the temporary database file */
	private static String tempDbFile = "/tmp/testBuildStore";
	
	/**
	 * Create a new empty BuildStore, with an empty database. For
	 * testing purposes only.
	 * @return The empty BuildStore database
	 */
	public static BuildStore getEmptyBuildStore() {
		BuildStore bs = new BuildStore(tempDbFile);
		
		// force the schema to be dropped and recreated.
		bs.forceInitialize();
		
		return bs;
	}	
}
