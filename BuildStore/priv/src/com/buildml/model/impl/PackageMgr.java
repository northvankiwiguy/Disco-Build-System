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

package com.buildml.model.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMgr;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.TaskSet;
import com.buildml.utils.errors.ErrorCode;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 */
/* package private */ class PackageMgr implements IPackageMgr {

	/*=====================================================================================*
	 * FIELDS
	 *=====================================================================================*/
	
	/** The BuildStore object that "owns" this Packages object. */
	private IBuildStore buildStore;
	
	/**
	 * Our database manager object, used to access the database content. This is provided 
	 * to us when the Packages object is first instantiated.
	 */
	private BuildStoreDB db = null;
	
	/** The FileMgr object that manages the files in our packages. */
	private IFileMgr fileMgr = null;
	
	/** The ActionMgr object that manages the actions in our packages. */
	private IActionMgr actionMgr = null;
	
	/**
	 * The names of the scopes within a package. These are statically defined and
	 * can't be modified by the user.
	 */
	private static String scopeNames[] = new String[] {"None", "Private", "Public"};
	
	/**
	 * Various prepared statements for database access.
	 */
	private PreparedStatement 
		addPackagePrepStmt = null,
		findPackageByNamePrepStmt = null,
		findPackageByIdPrepStmt = null,
		findAllPackagesPrepStmt = null,
		removePackageByNamePrepStmt = null,
		updateFilePackagePrepStmt = null,
		findFilePackagePrepStmt = null,
		findFilesInPackage1PrepStmt = null,
		findFilesInPackage2PrepStmt = null,
		findFilesOutsidePackage1PrepStmt = null,
		findFilesOutsidePackage2PrepStmt = null,		
		updateTaskPackagePrepStmt = null,
		findTaskPackagePrepStmt = null,
		findTasksInPackagePrepStmt = null,
		findTasksOutsidePackagePrepStmt = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new Packages object, which represents the file/task packages that
	 * are part of the BuildStore.
	 * 
	 * @param buildStore The BuildStore that this Packages object belongs to.
	 */
	public PackageMgr(BuildStore buildStore) {
		this.buildStore = buildStore;
		this.db = buildStore.getBuildStoreDB();
		this.fileMgr = buildStore.getFileMgr();
		this.actionMgr = buildStore.getActionMgr();
		
		/* initialize prepared database statements */
		addPackagePrepStmt = db.prepareStatement("insert into packages values (null, ?)");
		findPackageByNamePrepStmt = db.prepareStatement("select id from packages where name = ?");
		findPackageByIdPrepStmt = db.prepareStatement("select name from packages where id = ?");
		findAllPackagesPrepStmt = db.prepareStatement(
				"select name from packages order by name collate nocase");
		removePackageByNamePrepStmt = db.prepareStatement("delete from packages where name = ?");
		updateFilePackagePrepStmt = db.prepareStatement("update files set pkgId = ?, pkgScopeId = ? " +
				"where id = ?");
		findFilePackagePrepStmt = db.prepareStatement("select pkgId, pkgScopeId from files " +
				"where id = ?");
		findFilesInPackage1PrepStmt = db.prepareStatement("select id from files where pkgId = ?");
		findFilesInPackage2PrepStmt = db.prepareStatement("select id from files where " +
				"pkgId = ? and pkgScopeId = ?");
		findFilesOutsidePackage1PrepStmt = db.prepareStatement("select id from files where pkgId != ?");
		findFilesOutsidePackage2PrepStmt = db.prepareStatement("select id from files where " +
				"not (pkgId = ? and pkgScopeId = ?)");
		updateTaskPackagePrepStmt = db.prepareStatement("update buildTasks set pkgId = ? " +
				"where taskId = ?");
		findTaskPackagePrepStmt = db.prepareStatement("select pkgId from buildTasks where taskId = ?");
		findTasksInPackagePrepStmt = db.prepareStatement("select taskId from buildTasks where pkgId = ?");
		findTasksOutsidePackagePrepStmt = db.prepareStatement("select taskId from buildTasks " +
				"where pkgId != ? and taskId != 0");
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IPackageMgr#addPackage(java.lang.String)
	 */
	@Override
	public int addPackage(String packageName) {
		
		/* validate the new package's name */
		if (!isValidName(packageName)){
			return ErrorCode.INVALID_NAME;
		}
		
		/* check that the package doesn't already exist in the database */
		if (getPackageId(packageName) != ErrorCode.NOT_FOUND){
			return ErrorCode.ALREADY_USED;
		}
		
		/* insert the package into our table */
		try {
			addPackagePrepStmt.setString(1, packageName);
			db.executePrepUpdate(addPackagePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* return the new package's ID number */
		return db.getLastRowID();
	};

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IPackageMgr#getPackageName(int)
	 */
	@Override
	public String getPackageName(int packageId) {
		
		/* find the package in our table */
		String results[] = null;
		try {
			findPackageByIdPrepStmt.setInt(1, packageId);
			results = db.executePrepSelectStringColumn(findPackageByIdPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* no result == no package with this Id */
		if (results.length == 0) {
			return null;
		} 
		
		/* one result == we have the correct name */
		else if (results.length == 1) {
			return results[0];
		}
		
		/* multiple results is an error */
		else {
			throw new FatalBuildStoreError("Multiple entries found in packages table, for ID " + 
					packageId);
		}	
	};

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IPackageMgr#getPackageId(java.lang.String)
	 */
	@Override
	public int getPackageId(String packageName) {
		
		/* find the package into our table */
		Integer results[] = null;
		try {
			findPackageByNamePrepStmt.setString(1, packageName);
			results = db.executePrepSelectIntegerColumn(findPackageByNamePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* no result == no package by this name */
		if (results.length == 0) {
			return ErrorCode.NOT_FOUND;
		} 
		
		/* one result == we have the correct ID */
		else if (results.length == 1) {
			return results[0];
		}
		
		/* multiple results is an error */
		else {
			throw new FatalBuildStoreError("Multiple entries found in packages table, for name " + 
					packageName);
		}		
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IPackageMgr#removePackage(java.lang.String)
	 */
	@Override
	public int removePackage(String packageName) {
		
		/* check that the package already exists */
		int pkgId = getPackageId(packageName);
		if (pkgId == ErrorCode.NOT_FOUND){
			return ErrorCode.NOT_FOUND;
		}
		
		/* we can't remove the "None" package */
		if (packageName.equals("None")) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* determine if this package is used by any files */
		FileSet filesInPackage = getFilesInPackage(pkgId);
		if (filesInPackage.size() != 0) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* determine if this package is used by any tasks */
		TaskSet tasksInPackage = getTasksInPackage(pkgId);
		if (tasksInPackage.size() != 0) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* remove from the database */
		try {
			removePackageByNamePrepStmt.setString(1, packageName);
			db.executePrepUpdate(removePackageByNamePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return ErrorCode.OK;
	};
	
	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IPackageMgr#getPackages()
	 */
	@Override
	public String[] getPackages() {
		
		/* find all the package into our table */
		return db.executePrepSelectStringColumn(findAllPackagesPrepStmt);
	};	
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IPackageMgr#getScopeName(int)
	 */
	@Override
	public String getScopeName(int id) {
		
		/* the names are a static mapping, so no need for database look-ups */
		switch (id) {
		case SCOPE_NONE:
			return "None";
		case SCOPE_PRIVATE:
			return "Private";
		case SCOPE_PUBLIC:
			return "Public";
		default:
			return null;
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IPackageMgr#getScopeId(java.lang.String)
	 */
	@Override
	public int getScopeId(String name) {
		
		/* the mapping is static, so no need for a database look up */
		if (name.equalsIgnoreCase("None")) {
			return SCOPE_NONE;
		}
		if (name.equalsIgnoreCase("priv") || name.equalsIgnoreCase("private")) {
			return SCOPE_PRIVATE;
		}
		if (name.equalsIgnoreCase("pub") || name.equalsIgnoreCase("public")) {
			return SCOPE_PUBLIC;
		}
		return ErrorCode.NOT_FOUND;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IPackageMgr#parsePkgSpec(java.lang.String)
	 */
	@Override
	public Integer[] parsePkgSpec(String pkgSpec) {

		/* parse the pkgSpec to separate it into "pkg" and "scope" portions */
		String pkgName = pkgSpec;
		String scopeName = null;

		/* check if there's a '/' in the string, to separate "package" from "scope" */
		int slashIndex = pkgSpec.indexOf('/');
		if (slashIndex != -1) {
			pkgName = pkgSpec.substring(0, slashIndex);
			scopeName = pkgSpec.substring(slashIndex + 1);
		} 

		/* 
		 * Convert the package's name into it's internal ID. If there's an error,
		 * we simply pass it back to our own caller.
		 */
		int pkgId = getPackageId(pkgName);

		/* if the user provided a /scope portion, convert that to an ID too */
		int scopeId = 0;
		if (scopeName != null) {
			scopeId = getScopeId(scopeName);
		}
		
		return new Integer[] {pkgId, scopeId};
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IPackageMgr#setFilePackage(int, int, int)
	 */
	@Override
	public int setFilePackage(int fileId, int pkgId, int pkgScopeId) {
		
		try {
			updateFilePackagePrepStmt.setInt(1, pkgId);
			updateFilePackagePrepStmt.setInt(2, pkgScopeId);
			updateFilePackagePrepStmt.setInt(3, fileId);
			int rowCount = db.executePrepUpdate(updateFilePackagePrepStmt);
			if (rowCount == 0) {
				return ErrorCode.NOT_FOUND;
			}
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return ErrorCode.OK;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IPackageMgr#getFilePackage(int)
	 */
	@Override
	public Integer[] getFilePackage(int fileId) {
		
		Integer result[] = new Integer[2];
		
		try {
			findFilePackagePrepStmt.setInt(1, fileId);
			ResultSet rs = db.executePrepSelectResultSet(findFilePackagePrepStmt);
			if (rs.next()){
				result[0] = rs.getInt(1);
				result[1] = rs.getInt(2);
				rs.close();
			} else {
				/* error - there was no record, so the fileId must be invalid */
				return null;
			}
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("SQL error", e);
		}
				
		return result;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IPackageMgr#getFilesInPackage(int)
	 */
	@Override
	public FileSet getFilesInPackage(int pkgId) {
		Integer results[] = null;
		try {
			findFilesInPackage1PrepStmt.setInt(1, pkgId);
			results = db.executePrepSelectIntegerColumn(findFilesInPackage1PrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* convert to a FileSet */
		return new FileSet(fileMgr, results);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IPackageMgr#getFilesInPackage(int, int)
	 */
	@Override
	public FileSet getFilesInPackage(int pkgId, int pkgScopeId) {
		
		Integer results[] = null;
		try {
			findFilesInPackage2PrepStmt.setInt(1, pkgId);
			findFilesInPackage2PrepStmt.setInt(2, pkgScopeId);
			results = db.executePrepSelectIntegerColumn(findFilesInPackage2PrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* convert to a FileSet */
		return new FileSet(fileMgr, results);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IPackageMgr#getFilesInPackage(java.lang.String)
	 */
	@Override
	public FileSet getFilesInPackage(String pkgSpec) {

		Integer pkgSpecParts[] = parsePkgSpec(pkgSpec);
		
		int pkgId = pkgSpecParts[0];
		int scopeId = pkgSpecParts[1];
		
		/* the ID must not be invalid, else that's an error */
		if ((pkgId == ErrorCode.NOT_FOUND) || (scopeId == ErrorCode.NOT_FOUND)) {
			return null;
		}
		
		/* 
		 * If the scope ID isn't specified by the user, then scopeId == 0 (the
		 * ID of the "None" scope). This indicates we should look for all paths
		 * in the package, regardless of the scope.
		 */
		if (scopeId != 0) {
			return getFilesInPackage(pkgId, scopeId);
		} else {
			return getFilesInPackage(pkgId);			
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IPackageMgr#getFilesOutsidePackage(int)
	 */
	@Override
	public FileSet getFilesOutsidePackage(int pkgId) {
		Integer results[] = null;
		try {
			findFilesOutsidePackage1PrepStmt.setInt(1, pkgId);
			results = db.executePrepSelectIntegerColumn(findFilesOutsidePackage1PrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* convert to a FileSet */
		return new FileSet(fileMgr, results);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IPackageMgr#getFilesOutsidePackage(int, int)
	 */
	@Override
	public FileSet getFilesOutsidePackage(int pkgId, int pkgScopeId) {
		Integer results[] = null;
		try {
			findFilesOutsidePackage2PrepStmt.setInt(1, pkgId);
			findFilesOutsidePackage2PrepStmt.setInt(2, pkgScopeId);
			results = db.executePrepSelectIntegerColumn(findFilesOutsidePackage2PrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* convert to a FileSet */
		return new FileSet(fileMgr, results);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IPackageMgr#getFilesOutsidePackage(java.lang.String)
	 */
	@Override
	public FileSet getFilesOutsidePackage(String pkgSpec) {
		
		Integer pkgSpecParts[] = parsePkgSpec(pkgSpec);
		int pkgId = pkgSpecParts[0];
		int scopeId = pkgSpecParts[1];
		
		/* the ID must not be invalid, else that's an error */
		if ((pkgId == ErrorCode.NOT_FOUND) || (scopeId == ErrorCode.NOT_FOUND)) {
			return null;
		}
		
		/* 
		 * The scope ID is optional, since it still allows us to
		 * get the package's files. Note that scopeId == 0 implies
		 * that the user didn't specify a /scope value.
		 */
		if (scopeId != 0) {
			return getFilesOutsidePackage(pkgId, scopeId);
		} else {
			return getFilesOutsidePackage(pkgId);			
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IPackageMgr#setTaskPackage(int, int)
	 */
	@Override
	public int setTaskPackage(int taskId, int pkgId) {
		
		try {
			updateTaskPackagePrepStmt.setInt(1, pkgId);
			updateTaskPackagePrepStmt.setInt(2, taskId);
			int rowCount = db.executePrepUpdate(updateTaskPackagePrepStmt);
			if (rowCount == 0) {
				return ErrorCode.NOT_FOUND;
			}
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return ErrorCode.OK;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IPackageMgr#getTaskPackage(int)
	 */
	@Override
	public int getTaskPackage(int taskId) {
		
		Integer results[];
		try {
			findTaskPackagePrepStmt.setInt(1, taskId);
			results = db.executePrepSelectIntegerColumn(findTaskPackagePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* no result == no package by this name */
		if (results.length == 0) {
			return ErrorCode.NOT_FOUND;
		} 
		
		/* 
		 * One result == we have the correct ID (note: it's not possible to have
		 * multiple results, since taskId is a unique key
		 */
		return results[0];
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IPackageMgr#getTasksInPackage(int)
	 */
	@Override
	public TaskSet getTasksInPackage(int pkgId) {
		Integer results[] = null;
		try {
			findTasksInPackagePrepStmt.setInt(1, pkgId);
			results = db.executePrepSelectIntegerColumn(findTasksInPackagePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		return new TaskSet(actionMgr, results);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IPackageMgr#getTasksInPackage(java.lang.String)
	 */
	@Override
	public TaskSet getTasksInPackage(String pkgSpec) {
		
		/* translate the package's name to its ID */
		int pkgId = getPackageId(pkgSpec);
		if (pkgId == ErrorCode.NOT_FOUND){
			return null;
		}
		
		return getTasksInPackage(pkgId);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IPackageMgr#getTasksOutsidePackage(int)
	 */
	@Override
	public TaskSet getTasksOutsidePackage(int pkgId) {
		Integer results[] = null;
		try {
			findTasksOutsidePackagePrepStmt.setInt(1, pkgId);
			results = db.executePrepSelectIntegerColumn(findTasksOutsidePackagePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		return new TaskSet(actionMgr, results);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IPackageMgr#getTasksOutsidePackage(java.lang.String)
	 */
	@Override
	public TaskSet getTasksOutsidePackage(String pkgSpec) {
		
		/* translate the package's name to its ID */
		int pkgId = getPackageId(pkgSpec);
		if (pkgId == ErrorCode.NOT_FOUND){
			return null;
		}
		
		return getTasksOutsidePackage(pkgId);
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * Validate a package's name. Valid characters are digits, letters (upper or lower case),
	 * '-' and '_'. No other characters are permitted. Package names must contain at least
	 * three characters and start with a letter.
	 * 
	 * @param packageName The package name to be validated.
	 * @return True if the name is valid, else false.
	 */
	private boolean isValidName(String packageName) {
		
		if (packageName == null) {
			return false;
		}
		int length = packageName.length();
		if (length < 3) {
			return false;
		}
		
		int i = 0;
		while (i != length) {
			char ch = packageName.charAt(i);
			
			/* first character must be a letter */
			if (i == 0) {
				if (!(Character.isLetter(ch))) {
					return false;
				}	
			} 
			
			/* following characters are letter, digit, _ or - */
			else {
				if (!(Character.isLetterOrDigit(ch) ||
						(ch == '_') || (ch == '-'))){
					return false;
				}
			}
			i++;
		}
		
		return true;
	}
	
	/*-------------------------------------------------------------------------------------*/
}