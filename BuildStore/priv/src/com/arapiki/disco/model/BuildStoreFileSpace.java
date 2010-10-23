/*******************************************************************************
 * Copyright (c) 2010 Arapiki Solutions Inc.
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
 * A helper class to manage the file tree structures within a BuildStore.
 * The class encapsulates everything that's known about the content of the
 * file system during the build process (although nothing about the relationship
 * between files, or the commands used to create them).
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class BuildStoreFileSpace {

	/**
	 * Our database manager, used to access the database content. This is provided 
	 * to us when the BuildStoreFileSpace is first instantiated.
	 */
	private BuildStoreDB db = null;
	
	/**
	 * Create a new BuildStoreFileSpace object.
	 * @param db The BuildStoreDB object to use when accessing the underlying database.
	 */
	public BuildStoreFileSpace(BuildStoreDB db) {
		this.db = db;
	}
			
	/**
	 * Add a new file into the database.
	 * @param string The full path of the file.
	 */
	public void addFile(String string) {
		db.executeUpdate("insert into files values (null, \"" + string + "\")");
	}
}
