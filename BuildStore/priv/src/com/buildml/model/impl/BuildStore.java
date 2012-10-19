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

import com.buildml.model.BuildStoreVersionException;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileAttributeMgr;
import com.buildml.model.IFileIncludeMgr;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IReportMgr;
import com.buildml.utils.version.Version;


/**
 * A BuildStore object is the main class that implements a BuildML build system. By creating
 * a new BuildStore object, we create all the necessary data structures and databases
 * to store an entire BuildML build.
 * 
 * Note that although BuildStore is the main entry point, most of the work is done by
 * its delegate classes, such as FileMgr, ActionMgr etc. These "Managers" each deal
 * with a specific part of the build system, providing business logic and database access
 * to implement features.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class BuildStore implements IBuildStore {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** 
	 * The BuildStoreDB manager object that manages our connection to the underlying
	 * database.
	 */
	private BuildStoreDB db;
	
	/** The FileMgr manager object we'll delegate work to. */
	private IFileMgr fileMgr;

	/** The FileIncludeMgr object we'll delegate work to. */
	private IFileIncludeMgr fileIncludeMgr;

	/** The ActionMgr manager object we'll delegate work to. */
	private IActionMgr actionMgr;
	
	/** The Reports manager object we'll delegate work to. */
	private IReportMgr reportMgr;
	
	/** The FileAttributeMgr object we'll delegate work to. */
	private IFileAttributeMgr fileAttrMgr;
	
	/** The Packages manager object we'll delegate work to. */
	private IPackageMgr packages;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Open or create a new BuildStore database. Clients should not invoke this
	 * constructor directly. Instead, use BuildStoreFactory instead.
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
		
		/* create a new FileMgr object to manage our list of files */
		fileMgr = new FileMgr(this);
		
		/* create a new FileIncludeMgr object to manage the relationship between files */
		fileIncludeMgr = new FileIncludeMgr(this);

		/* create a new ActionMgr object to manage the relationship between files */
		actionMgr = new ActionMgr(this);
		
		/* create a new ReportMgr object to provide reporting methods */
		reportMgr = new ReportMgr(this);
		
		/* create a new FileAttributeMgr object to manage the attributes on files */
		fileAttrMgr = new FileAttributeMgr(this, fileMgr);
		
		/* create a new Packages object */
		packages = new PackageMgr(this);		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Open or create a new BuildStore database. Clients should not invoke this
	 * constructor directly. Instead, use BuildStoreFactory instead.
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
	
	/* (non-Javadoc)
	 * @see com.buildml.model.IBuildStore#getBuildStoreVersion()
	 */
	@Override
	public int getBuildStoreVersion() {
		return db.getBuildStoreVersion();
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IBuildStore#getFileNameSpaces()
	 */
	@Override
	public IFileMgr getFileMgr() {
		return fileMgr;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IBuildStore#getFileIncludeMgr()
	 */
	@Override
	public IFileIncludeMgr getFileIncludeMgr() {
		return fileIncludeMgr;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IBuildStore#getActionMgr()
	 */
	@Override
	public IActionMgr getActionMgr() {
		return actionMgr;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IBuildStore#getReportMgr()
	 */
	@Override
	public IReportMgr getReportMgr() {
		return reportMgr;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IBuildStore#getFileAttributeMgr()
	 */
	@Override
	public IFileAttributeMgr getFileAttributeMgr() {
		return fileAttrMgr;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IBuildStore#getPackageMgr()
	 */
	@Override
	public IPackageMgr getPackageMgr() {
		return packages;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IBuildStore#setFastAccessMode(boolean)
	 */
	@Override
	public void setFastAccessMode(boolean fast) {
		db.setFastAccessMode(fast);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IBuildStore#close()
	 */
	@Override
	public void close() {
		db.close();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IBuildStore#save()
	 */
	@Override
	public void save() throws IOException
	{
		db.save();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IBuildStore#saveAs(java.lang.String)
	 */
	@Override
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
