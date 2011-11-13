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

import java.io.FileNotFoundException;


/**
 * A BuildStore object is the main class that implements a Disco build system. By creating
 * a new BuildStore object, we create all the necessary data structures and databases
 * to store an entire Disco build.
 * 
 * Note that although BuildStore is the main entry point, most of the work is done by
 * its delegate classes, such as FileNameSpaces, BuildTasks etc. These "Managers" each deal
 * with a specific part of the build system, providing business logic and database access
 * to implement features.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class BuildStore {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** 
	 * The BuildStoreDB manager object that manages our connection to the underlying
	 * database.
	 */
	private BuildStoreDB db;
	
	/** The FileNameSpaces manager object we'll delegate work to. */
	private FileNameSpaces fileSpaces;

	/** The FileIncludes manager object we'll delegate work to. */
	private FileIncludes fileIncludes;

	/** The BuildTasks manager object we'll delegate work to. */
	private BuildTasks buildTasks;
	
	/** The Reports manager object we'll delegate work to. */
	private Reports reports;
	
	/** The FileAttributes object we'll delegate work to. */
	private FileAttributes fileAttributes;
	
	/** The Components manager object we'll delegate work to. */
	private Components components;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Open or create a new BuildStore database. If the database already
	 * exists, open it for updating. If there's no database by this name,
	 * create a fresh database.
	 * 
	 * @param buildStoreName Name of the database to open or create.
	 * @throws FileNotFoundException The database file can't be found, or isn't writable.
	 */
	public BuildStore(String buildStoreName) throws FileNotFoundException {
		
		/* create a new DB manager to handle all the SQL connection issues */
		db = new BuildStoreDB(buildStoreName);
		
		/* if necessary, initialize the database with required tables */
		if (getBuildStoreVersion() == 0){
			/* changes must be committed promptly */
			db.setFastAccessMode(false); 
			db.initDatabase();
		}
		
		/* create a new FileNameSpaces object to manage our list of files */
		fileSpaces = new FileNameSpaces(this);
		
		/* create a new FileIncludes object to manage the relationship between files */
		fileIncludes = new FileIncludes(this);

		/* create a new BuildTasks object to manage the relationship between files */
		buildTasks = new BuildTasks(this);
		
		/* create a new Reports object to provide reporting methods */
		reports = new Reports(this);
		
		/* create a new FileAttributes object to manage the attributes on files */
		fileAttributes = new FileAttributes(db, fileSpaces);
		
		/* create a new Components object */
		components = new Components(this);		
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Return the Disco database's schema version.
	 * 
	 * @return The schema version as an integer, or 0 if there's no schema in place.
	 */
	public int getBuildStoreVersion() {
		return db.getBuildStoreVersion();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Fetch the FileNamesSpaces manager associated with this BuildStore. This object
	 * encapsulates all knowledge about files/directories within the build system.
	 * 
	 * @return A FileNameSpaces manager object.
	 */
	public FileNameSpaces getFileNameSpaces() {
		return fileSpaces;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Fetch the FileIncludes manager associated with this BuildStore. This object
	 * encapsulates knowledge of which source files textually include which other
	 * source files.
	 * 
	 * @return A FileIncludes manager object.
	 */
	public FileIncludes getFileIncludes() {
		return fileIncludes;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Fetch the BuildTasks manager associated with this BuildStore. This object
	 * contains information about all build tasks.
	 * 
	 * @return A BuildTasks manager object.
	 */
	public BuildTasks getBuildTasks() {
		return buildTasks;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Fetch the Reports manager associated with this BuildStore. This object
	 * contains methods for generating summary reports from the Disco database.
	 *  
	 * @return A Reports manager object.
	 */
	public Reports getReports() {
		return reports;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Fetch the FileAttributes manager associated with this BuildStore. This object
	 * encapsulates knowledge of which attributes are attached to the paths in
	 * our FileNameSpaces object.
	 * 
	 * @return A FileAttributes manager object.
	 */
	public FileAttributes getFileAttributes() {
		return fileAttributes;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Fetch the Components manager associated with this BuildStore. This object
	 * encapsulates knowledge of the component names used in the BuildStore.
	 * 
	 * @return A Components manager object.
	 */
	public Components getComponents() {
		return components;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Specify whether database access should be fast (true) or safe (false). Fast
	 * access is considerably faster than safe access, but won't ensure that
	 * changes are written to the disk. Only use fast access for "large write" operations.
	 * 
	 * @param fast Set to true to enable fast access, or false for safe access.
	 */
	public void setFastAccessMode(boolean fast) {
		db.setFastAccessMode(fast);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Force the database to be empty. This is primarily for testing purposes.
	 */
	public void forceInitialize() {
		db.initDatabase();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Close the BuildStore, and release any resources associated with it. Attempting to
	 * access the BuildStore's content after it's closed will likely cause a
	 * FatalBuildStoreError.
	 */
	public void close() {
		db.close();
	}
	
	/*=====================================================================================*
	 * PACKAGE-LEVEL METHODS
	 *=====================================================================================*/
	
	/**
	 * Fetch a reference to this BuildStore's underlying database. This is a package-scope
	 * method to be used only by delegate classes (such as FileNameSpaces).
	 * 
	 * @return Reference to this BuildStore's underlying database.
	 */
	/* package */ BuildStoreDB getBuildStoreDB() {
		return db;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
