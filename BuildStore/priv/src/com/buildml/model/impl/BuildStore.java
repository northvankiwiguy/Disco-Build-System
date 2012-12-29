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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.buildml.model.BuildStoreVersionException;
import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileAttributeMgr;
import com.buildml.model.IFileGroupMgr;
import com.buildml.model.IFileIncludeMgr;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageRootMgr;
import com.buildml.model.IReportMgr;
import com.buildml.utils.errors.ErrorCode;


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

	/** The FileGroupMgr manager object we'll delegate work to. */
	private IFileGroupMgr fileGroupMgr;

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
	
	/** The PackageRootMgr object we'll delegate work to */
	private IPackageRootMgr pkgRootMgr;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Open or create a new BuildStore database. Clients should not invoke this
	 * constructor directly. Instead, use BuildStoreFactory.
	 * 
	 * @param buildStoreName Name of the database to open or create.
	 * @param saveRequired True if BuildStore must explicitly be "saved" before
	 *        it's closed (otherwise the changes will be discarded).
	 * @param createIfNeeded True if the database should be initialized if it's not already.
	 * @throws FileNotFoundException The database file can't be found, or isn't writable.
	 * @throws IOException An I/O problem occurred while opening the database file.
	 * @throws BuildStoreVersionException The database schema of an existing database is the wrong version.
	 */
	public BuildStore(String buildStoreName, boolean saveRequired, boolean createIfNeeded)
			throws FileNotFoundException, IOException, BuildStoreVersionException {
		
		boolean freshDatabase = false;
		
		/* create a new DB manager to handle all the SQL connection issues */
		db = new BuildStoreDB(buildStoreName, saveRequired);
		
		/* if necessary, initialize the database with required tables */
		int buildStoreVersion = getBuildStoreVersion();
		if (buildStoreVersion == -1){
			if (createIfNeeded) {
				/* changes must be committed promptly */
				db.setFastAccessMode(false); 
				db.initDatabase();
				freshDatabase = true;
			} else {
				throw new BuildStoreVersionException("File \"" + buildStoreName + "\" does not " +
						"appear to be a valid BuildML database file.");
			}
		}
		
		/* 
		 * Else, it's an existing database. Validate that the schema is the
		 * correct version.
		 */
		else {
			if (buildStoreVersion < BuildStoreDB.SCHEMA_VERSION){
				throw new BuildStoreVersionException(
						"Database \"" + buildStoreName + "\" has an older schema version (" 
								+ buildStoreVersion + "). Please run \"bmladmin upgrade\" to upgrade " +
								"the database content.");
			}
			if (buildStoreVersion > BuildStoreDB.SCHEMA_VERSION){
				throw new BuildStoreVersionException(
						"Database \"" + buildStoreName + "\" has an unrecognized schema version (" + 
								buildStoreVersion + "). It can only be read by a newer version of BuildML.");
			}
		}

		/*
		 * Create a bunch of manager objects that we'll delegate work to.
		 */
		fileMgr = new FileMgr(this);
		fileGroupMgr = new FileGroupMgr(this);
		fileIncludeMgr = new FileIncludeMgr(this);
		actionMgr = new ActionMgr(this);
		reportMgr = new ReportMgr(this);
		fileAttrMgr = new FileAttributeMgr(this, fileMgr);
		packages = new PackageMgr(this);
		pkgRootMgr = new PackageRootMgr(this);
		
		/*
		 * When the database is first created, it won't have the "workspace" root set.
		 * By default, we set it to the parent directory of the build.bml file.
		 */
		if (freshDatabase) {
			File dbFile = new File(db.getDatabaseFileName());
			File workspaceDir = dbFile.getParentFile();
			if (workspaceDir == null) {
				throw new FatalBuildStoreError("Unable to determine initial \"workspace\" directory.");
			}
			int workspacePathId = fileMgr.addDirectory(workspaceDir.getPath());
			if (workspacePathId == ErrorCode.BAD_PATH) {
				throw new FatalBuildStoreError("Unable to add initial \"workspace\" directory.");				
			}
			pkgRootMgr.setWorkspaceRoot(workspacePathId);
			pkgRootMgr.setBuildMLFileDepth(0);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Open or create a new BuildStore database. Clients should not invoke this
	 * constructor directly. Instead, use BuildStoreFactory.
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
		this(buildStoreName, saveRequired, true);
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
		this(buildStoreName, false, true);
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
	 * @see com.buildml.model.IBuildStore#getFileMgr()
	 */
	@Override
	public IFileMgr getFileMgr() {
		return fileMgr;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IBuildStore#getFileGroupMgr()
	 */
	@Override
	public IFileGroupMgr getFileGroupMgr() {
		return fileGroupMgr;
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
	 * @see com.buildml.model.IBuildStore#getPackageRootMgr()
	 */
	@Override
	public IPackageRootMgr getPackageRootMgr() {
		return pkgRootMgr;
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
		/* if we're changing the saveAs path, we might need to change the workspace depth */
		String workspaceRoot = pkgRootMgr.getWorkspaceRootNative();
		if (!fileToSave.startsWith(workspaceRoot + "/")) {
			throw new IOException("Can not save file outside of workspace");
		}
		int newDepth = 0;
		File parentFile = new File(fileToSave).getParentFile();
		while (!(parentFile.toString().equals(workspaceRoot))) {
			parentFile = parentFile.getParentFile();
			newDepth++;
		}
		pkgRootMgr.setBuildMLFileDepth(newDepth);
		
		/* proceed to save the database in the new location */
		db.saveAs(fileToSave);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#emptyTrash()
	 */
	public void emptyTrash() {
		db.emptyTrash();
	}	
	
	/*=====================================================================================*
	 * PACKAGE-LEVEL METHODS
	 *=====================================================================================*/
	
	/**
	 * Fetch a reference to this BuildStore's underlying database. This is a package-scope
	 * method to be used only by delegate classes (such as FileMgr).
	 * 
	 * @return Reference to this BuildStore's underlying database.
	 */
	/* package */ BuildStoreDB getBuildStoreDB() {
		return db;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
