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
 * Represents a full Disco build system.
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class BuildStore {

	/* the BuildStoreDB object that manages our DB access */
	private BuildStoreDB db;
	
	/* the BuildStoreFileSpace object we'll delegate work to */
	private FileNameSpaces fileSpace;

	/**
	 * Open or create a new BuildStore database. If the database already
	 * exists, open it for updating. If there's no database by this name,
	 * create a fresh database.
	 * 
	 * @param databaseName Name of the database to open or create
	 */
	public BuildStore(String buildStoreName) {
		
		/* create a new DB manager to handle all the SQL connection issues */
		db = new BuildStoreDB(buildStoreName);
		
		/* if necessary, initialize the database with required tables */
		if (getBuildStoreVersion() == 0){
			/* changes must be committed promptly */
			db.setFastAccessMode(false); 
			db.initDatabase();
		}
		
		/* create a new BuildStoreFileSpace object to manage our list of files */
		fileSpace = new FileNameSpaces(db);
	}
	
	/**
	 * Return the database's schema version.
	 * @return The schema version as an integer, or 0 if there's no schema in place.
	 */
	public int getBuildStoreVersion() {
		return db.getBuildStoreVersion();
	}

	/**
	 * Fetch the BuildStoreFileSpace object associated with this BuildStore. This object
	 * encapsulates all knowledge about files/directories within the build system.
	 * @return A BuildStoreFileSpace object
	 */
	public FileNameSpaces getFileNameSpaces() {
		return fileSpace;
	}
	
	/**
	 * Specify whether database access should be fast (true) or safe (false). Fast
	 * access is considerably faster than safe access, but won't ensure that
	 * changes are written to the disk. Only use fast access for "large write" operations.
	 * @param fast Set to true to enable fast access, or false for safe access.
	 */
	public void setFastAccessMode(boolean fast) {
		db.setFastAccessMode(fast);
	}

	/**
	 * Force the database to be empty. This is primarily for testing purposes.
	 */
	public void forceInitialize() {
		db.initDatabase();
	}
}
