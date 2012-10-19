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

package com.buildml.model.impl;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.buildml.model.IPackageMgr;
import com.buildml.model.IReportMgr;
import com.buildml.model.errors.BuildStoreVersionException;
import com.buildml.utils.version.Version;


/**
 * A BuildStore object is the main class that implements a BuildML build system. By creating
 * a new BuildStore object, we create all the necessary data structures and databases
 * to store an entire BuildML build.
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
	private IReportMgr reportMgr;
	
	/** The FileAttributes object we'll delegate work to. */
	private FileAttributes fileAttributes;
	
	/** The Packages manager object we'll delegate work to. */
	private IPackageMgr packages;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Open or create a new BuildStore database. If the database already
	 * exists, open it for updating. If there's no database by this name,
	 * create a fresh database.
	 * 
	 * @param buildStoreName Name of the database to open or create.
	 * @param saveRequired True if BuildStore must explicitly be "saved" before
	 *        it's closed (otherwise the changes will be discarded).
	 * @throws FileNotFoundException The database file can't be found, or isn't writable.
	 * @throws IOException An I/O problem occurred while opening the database file.
	 * @throws BuildStoreVersionException The database schema of an existing database is the wrong version.
	 */
	public BuildStore(String buildStoreName, boolean saveRequired)
			throws FileNotFoundException, IOException, BuildStoreVersionException {
		
		/* create a new DB manager to handle all the SQL connection issues */
		db = new BuildStoreDB(buildStoreName, saveRequired);
		
		/* if necessary, initialize the database with required tables */
		int buildStoreVersion = getBuildStoreVersion();
		if (buildStoreVersion == -1){
			/* changes must be committed promptly */
			db.setFastAccessMode(false); 
			db.initDatabase();
		}
		
		/* 
		 * Else, it's an existing database. Validate that the schema is the
		 * correct version.
		 */
		else {
			int actualVersion = Version.getVersionNumberAsInt();
			if (buildStoreVersion != actualVersion){
				throw new BuildStoreVersionException(
						"Database \"" + buildStoreName + "\" has schema version " + buildStoreVersion +
						". Expected version " + actualVersion + ".");
			}
		}
		
		/* create a new FileNameSpaces object to manage our list of files */
		fileSpaces = new FileNameSpaces(this);
		
		/* create a new FileIncludes object to manage the relationship between files */
		fileIncludes = new FileIncludes(this);

		/* create a new BuildTasks object to manage the relationship between files */
		buildTasks = new BuildTasks(this);
		
		/* create a new ReportMgr object to provide reporting methods */
		reportMgr = new ReportMgr(this);
		
		/* create a new FileAttributes object to manage the attributes on files */
		fileAttributes = new FileAttributes(this, fileSpaces);
		
		/* create a new Packages object */
		packages = new PackageMgr(this);		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Open or create a new BuildStore database. If the database already
	 * exists, open it for updating. If there's no database by this name,
	 * create a fresh database. Changes to this BuildStore will automatically
	 * be saved to the original BuildML file (no "save" operation required).
	 * 
	 * @param buildStoreName Name of the database to open or create.
	 * @throws FileNotFoundException The database file can't be found, or isn't writable.
	 * @throws IOException An I/O problem occurred while opening the database file.
	 * @throws BuildStoreVersionException The database schema of an existing database is the 
	 *         wrong version.
	 */
	public BuildStore(String buildStoreName)
			throws FileNotFoundException, IOException, BuildStoreVersionException {
		this(buildStoreName, false);
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Return the BuildML database's schema version.
	 * 
	 * @return The schema version as an integer, or -1 if there's no schema in place.
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
	 * contains methods for generating summary reports from the BuildML database.
	 *  
	 * @return A Reports manager object.
	 */
	public IReportMgr getReportMgr() {
		return reportMgr;
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
	 * Fetch the Packages manager associated with this BuildStore. This object
	 * encapsulates knowledge of the package names used in the BuildStore.
	 * 
	 * @return A Packages manager object.
	 */
	public IPackageMgr getPackageMgr() {
		return packages;
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
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Save this BuildStore to disk, ensuring that any temporary (unsaved) changes are
	 * persisted. This method only has an effect if the database was created with
	 * saveRequired == true.
	 * @throws IOException Unable to write database to disk.
	 */
	public void save() throws IOException
	{
		db.save();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Save this BuildStore to disk, using the caller-specified file name. This method only
	 * has an effect if the database was created with saveRequired == true.
	 * @param fileToSave The file to write the database content into. This file now becomes
	 * the default for future save() operations.
	 * @throws IOException Unable to write database to disk.
	 */
	public void saveAs(String fileToSave) throws IOException
	{
		db.saveAs(fileToSave);
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
