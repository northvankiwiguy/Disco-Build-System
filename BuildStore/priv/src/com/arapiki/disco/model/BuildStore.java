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
	
	/* the FileNameSpaces object we'll delegate work to */
	private FileNameSpaces fileSpaces;

	/* the FileIncludes object we'll delegate work to */
	private FileIncludes fileIncludes;

	/* the BuildTasks object we'll delegate work to */
	private BuildTasks buildTasks;
	
	/* the Reports object we'll delegate work to */
	private Reports reports;
	
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
		
		/* create a new FileNameSpaces object to manage our list of files */
		fileSpaces = new FileNameSpaces(db);
		
		/* create a new FileDepenencies object to manage the relationship between files */
		fileIncludes = new FileIncludes(db);

		/* create a new BuildTasks object to manage the relationship between files */
		buildTasks = new BuildTasks(db);
		
		/* create a new Reports object to provide reporting methods */
		reports = new Reports(db, fileSpaces);
	}
	
	/**
	 * Return the database's schema version.
	 * @return The schema version as an integer, or 0 if there's no schema in place.
	 */
	public int getBuildStoreVersion() {
		return db.getBuildStoreVersion();
	}

	/**
	 * Fetch the FileNamesSpaces object associated with this BuildStore. This object
	 * encapsulates all knowledge about files/directories within the build system.
	 * @return A FileNameSpaces object
	 */
	public FileNameSpaces getFileNameSpaces() {
		return fileSpaces;
	}

	/**
	 * Fetch the FileIncludes object associated with this BuildStore. This object
	 * encapsulates knowledge of which source files textually include which other
	 * source files.
	 * @return A FileIncludes object
	 */
	public FileIncludes getFileIncludes() {
		return fileIncludes;
	}

	/**
	 * Fetch the BuildTasks object associated with this BuildStore. This object
	 * contains a list of all build tasks.
	 * @return A BuildTasks object.
	 */
	public BuildTasks getBuildTasks() {
		return buildTasks;
	}
	
	/**
	 * Fetch the Reports object associated with this BuildStore. This object
	 * contains methods for generating 
	 * @return A BuildTasks object.
	 */
	public Reports getReports() {
		return reports;
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
