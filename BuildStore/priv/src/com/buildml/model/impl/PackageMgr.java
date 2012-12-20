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
import java.util.ArrayList;
import java.util.List;

import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageMgrListener;
import com.buildml.model.IPackageRootMgr;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.ActionSet;
import com.buildml.utils.errors.ErrorCode;

/**
 * A manager class (that supports the BuildStore class) responsible for managing all 
 * BuildStore information pertaining to packages.
 * <p>
 * There should be exactly one PackageMgr object per BuildStore object. Use the
 * BuildStore's getPackageMgr() method to obtain that one instance.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
/* package private */ class PackageMgr implements IPackageMgr {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** ID number for the <import> package */
	private final static int IMPORT_PACKAGE_ID = 0;
	
	/** ID number for the "Root" folder */
	private final static int ROOT_FOLDER_ID = 1;
	
	/** The BuildStore that owns this package manager */
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
	 * Various prepared statements for database access.
	 */
	private PreparedStatement 
		addPackagePrepStmt = null,
		findPackageByNamePrepStmt = null,
		findPackageByIdPrepStmt = null,
		findPackageTypePrepStmt = null,
		findPackageParentPrepStmt = null,
		updatePackageParentPrepStmt = null,
		updatePackageNamePrepStmt = null,
		findAllPackagesPrepStmt = null,
		findChildPackagesPrepStmt = null,
		removePackageByIdPrepStmt = null,
		updateFilePackagePrepStmt = null,
		findFilePackagePrepStmt = null,
		findFilesInPackage1PrepStmt = null,
		findFilesInPackage2PrepStmt = null,
		findFilesOutsidePackage1PrepStmt = null,
		findFilesOutsidePackage2PrepStmt = null,		
		updateActionPackagePrepStmt = null,
		findActionPackagePrepStmt = null,
		findActionsInPackagePrepStmt = null,
		findActionsOutsidePackagePrepStmt = null;
	
	/** The event listeners who are registered to learn about package changes */
	List<IPackageMgrListener> listeners = new ArrayList<IPackageMgrListener>();
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new Packages object, which represents the file/action packages that
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
		addPackagePrepStmt = db.prepareStatement("insert into packages values (null, ?, " + 
												 ROOT_FOLDER_ID + ", ?)");
		findPackageByNamePrepStmt = db.prepareStatement("select id from packages where name = ?");
		findPackageByIdPrepStmt = db.prepareStatement("select name from packages where id = ?");
		findPackageTypePrepStmt = db.prepareStatement("select isFolder from packages where id = ?");
		findPackageParentPrepStmt = db.prepareStatement("select parent from packages where id = ?");
		updatePackageParentPrepStmt = db.prepareStatement("update packages set parent = ? where id = ?");
		updatePackageNamePrepStmt = db.prepareStatement("update packages set name = ? where id = ?");
		findAllPackagesPrepStmt = db.prepareStatement(
				"select name from packages where isFolder = 0 order by name collate nocase");
		findChildPackagesPrepStmt = db.prepareStatement(
				"select id from packages where parent = ? and id != " + ROOT_FOLDER_ID + 
				" order by isFolder desc, name collate nocase");
		removePackageByIdPrepStmt = db.prepareStatement("delete from packages where id = ?");
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
		updateActionPackagePrepStmt = db.prepareStatement("update buildActions set pkgId = ? " +
				"where actionId = ?");
		findActionPackagePrepStmt = db.prepareStatement("select pkgId from buildActions where actionId = ?");
		findActionsInPackagePrepStmt = db.prepareStatement("select actionId from buildActions where pkgId = ?");
		findActionsOutsidePackagePrepStmt = db.prepareStatement("select actionId from buildActions " +
				"where pkgId != ? and actionId != 0");
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#getRootFolder()
	 */
	@Override
	public int getRootFolder() {
		return ROOT_FOLDER_ID;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#getImportPackage()
	 */
	@Override
	public int getImportPackage() {
		return IMPORT_PACKAGE_ID;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#addPackage(java.lang.String)
	 */
	@Override
	public int addPackage(String packageName) {
		
		/* add the new package */
		int pkgId = addPackageOrFolderHelper(packageName, false);
		if (pkgId < 0) {
			return pkgId;
		}
		
		/* set the package's roots to the same level as the workspace root */
		IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
		int workspaceRootId = pkgRootMgr.getWorkspaceRoot();
		if (workspaceRootId == ErrorCode.NOT_FOUND) {
			throw new FatalBuildStoreError(
					"Workspace root must be set before addPackage is called");
		}
		int rc = pkgRootMgr.setPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT, workspaceRootId);
		if (rc != ErrorCode.OK) {
			return rc;
		}
		rc = pkgRootMgr.setPackageRoot(pkgId, IPackageRootMgr.GENERATED_ROOT, workspaceRootId);
		if (rc != ErrorCode.OK) {
			return rc;
		}
		return pkgId;
	};

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#addFolder(java.lang.String)
	 */
	@Override
	public int addFolder(String folderName) {
		return addPackageOrFolderHelper(folderName, true);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#getPackageName(int)
	 */
	@Override
	public String getName(int folderOrPackageId) {
		
		/* find the package in our table */
		String results[] = null;
		try {
			findPackageByIdPrepStmt.setInt(1, folderOrPackageId);
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
					folderOrPackageId);
		}	
	};

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#setName(int, java.lang.String)
	 */
	@Override
	public int setName(int folderOrPackageId, String newName) {
		
		/* if there's no change in name, there's nothing to update */
		if (newName.equals(getName(folderOrPackageId))) {
			return ErrorCode.OK;
		}
		
		/* check that the package/folder doesn't already exist in the database */
		if (getId(newName) != ErrorCode.NOT_FOUND){
			return ErrorCode.ALREADY_USED;
		}

		/* validate the new package/folder's name */
		if (!isValidName(newName)){
			return ErrorCode.INVALID_NAME;
		}

		/* 
		 * If this is a package (not a folder), we also must rename the associated
		 * roots. This is done by deleting the old roots and adding new roots.
		 */
		IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
		boolean isFolder = isFolder(folderOrPackageId); 
		int srcRootPath = 0;
		int genRootPath = 0;
		if (!isFolder) {
			srcRootPath = pkgRootMgr.getPackageRoot(folderOrPackageId, IPackageRootMgr.SOURCE_ROOT);
			genRootPath = pkgRootMgr.getPackageRoot(folderOrPackageId, IPackageRootMgr.GENERATED_ROOT);
			pkgRootMgr.removePackageRoot(folderOrPackageId, IPackageRootMgr.SOURCE_ROOT);
			pkgRootMgr.removePackageRoot(folderOrPackageId, IPackageRootMgr.GENERATED_ROOT);
		}
		
		/* update the database */
		try {
			updatePackageNamePrepStmt.setString(1, newName);
			updatePackageNamePrepStmt.setInt(2, folderOrPackageId);
			int rowCount = db.executePrepUpdate(updatePackageNamePrepStmt);
			if (rowCount == 0) {
				return ErrorCode.NOT_FOUND;
			}
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* now, add the new package roots at the same location as the old */
		if (!isFolder){
			pkgRootMgr.setPackageRoot(folderOrPackageId, IPackageRootMgr.SOURCE_ROOT, srcRootPath);
			pkgRootMgr.setPackageRoot(folderOrPackageId, IPackageRootMgr.GENERATED_ROOT, genRootPath);
		}
		
		/* notify anybody who cares that our name has changed */
		notifyListeners(folderOrPackageId, IPackageMgrListener.CHANGED_NAME);
		return ErrorCode.OK;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#getPackageId(java.lang.String)
	 */
	@Override
	public int getId(String folderOrPackageName) {
		
		/* find the package into our table */
		Integer results[] = null;
		try {
			findPackageByNamePrepStmt.setString(1, folderOrPackageName);
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
					folderOrPackageName);
		}		
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#removePackage(java.lang.String)
	 */
	@Override
	public int remove(int folderOrPackageId) {
		
		/* we can't remove the "<import>" package or the "Root" folder */
		if ((folderOrPackageId == ROOT_FOLDER_ID) || (folderOrPackageId == IMPORT_PACKAGE_ID)) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* determine if this package is used by any files */
		FileSet filesInPackage = getFilesInPackage(folderOrPackageId);
		if (filesInPackage.size() != 0) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* determine if this package is used by any actions */
		ActionSet actionsInPackage = getActionsInPackage(folderOrPackageId);
		if (actionsInPackage.size() != 0) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* determine if the folder has any children */
		if (getFolderChildren(folderOrPackageId).length != 0) {
			return ErrorCode.CANT_REMOVE;
		}

		/* remove any associated package roots */
		if (!isFolder(folderOrPackageId)) {
			IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
			int rc = pkgRootMgr.removePackageRoot(folderOrPackageId, IPackageRootMgr.SOURCE_ROOT);
			if (rc != ErrorCode.OK) {
				return rc;
			}
			rc = pkgRootMgr.removePackageRoot(folderOrPackageId, IPackageRootMgr.GENERATED_ROOT);
			if (rc != ErrorCode.OK) {
				return rc;
			}
		}
		
		/* remove from the database */
		int removedCount = 0;
		try {
			removePackageByIdPrepStmt.setInt(1, folderOrPackageId);
			removedCount = db.executePrepUpdate(removePackageByIdPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		if (removedCount == 0) {
			return ErrorCode.NOT_FOUND;
		}
		
		return ErrorCode.OK;
	};
	
	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#getPackages()
	 */
	@Override
	public String[] getPackages() {
		
		/* find all the package into our table */
		return db.executePrepSelectStringColumn(findAllPackagesPrepStmt);
	};	
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#getFolderChildren(int)
	 */
	@Override
	public Integer[] getFolderChildren(int folderId) {

		try {
			findChildPackagesPrepStmt.setInt(1, folderId);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		return db.executePrepSelectIntegerColumn(findChildPackagesPrepStmt);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#getParent(int)
	 */
	@Override
	public int getParent(int folderOrPackageId) {

		Integer results[] = null;
		try {
			findPackageParentPrepStmt.setInt(1, folderOrPackageId);
			results = db.executePrepSelectIntegerColumn(findPackageParentPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* If folderOrPackageId is invalid (or duplicated), assume not a folder */
		if (results.length != 1) {
			return ErrorCode.NOT_FOUND;
		}
		
		return results[0];
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#setParent(int, int)
	 */
	@Override
	public int setParent(int folderOrPackageId, int parentId) {
		
		/* validate that the parent and child are valid */
		if (!isValid(parentId) || !isValid(folderOrPackageId)) {
			return ErrorCode.BAD_VALUE;
		}
		
		/* the parent must be a folder */
		if (!isFolder(parentId)) {
			return ErrorCode.NOT_A_DIRECTORY;
		}
		
		/* we can't move the root folder or the <import> package */
		if ((folderOrPackageId == ROOT_FOLDER_ID) || (folderOrPackageId == IMPORT_PACKAGE_ID)) {
			return ErrorCode.BAD_PATH;
		}
				
		/* if the child is a folder, make sure it doesn't create a cycle */
		if (isFolder(folderOrPackageId)) {
			
			/* 
			 * Starting with the new (proposed) parent, work upward to see if the child is already
			 * a parent.
			 */
			int ancestor = parentId;
			while (true) {
				/* a cycle has been detected */
				if (ancestor == folderOrPackageId) {
					return ErrorCode.BAD_PATH;
				}
								
				/* did we reach the root, without a cycle? */
				int nextAncestor = getParent(ancestor);
				if (nextAncestor == ancestor) {
					break;
				}
				ancestor = nextAncestor;
			}
		}
		
		/* update the database entry */
		try {
			updatePackageParentPrepStmt.setInt(1, parentId);
			updatePackageParentPrepStmt.setInt(2, folderOrPackageId);
			db.executePrepUpdate(updatePackageParentPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return ErrorCode.OK;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#isFolder(int)
	 */
	@Override
	public boolean isFolder(int folderOrPackageId) {

		Integer results[] = null;
		try {
			findPackageTypePrepStmt.setInt(1, folderOrPackageId);
			results = db.executePrepSelectIntegerColumn(findPackageTypePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* If folderOrPackageId is invalid (or duplicated), assume not a folder */
		if (results.length != 1) {
			return false;
		}
		
		return (results[0] == 1);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/*
	 * (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#isValid(int)
	 */
	@Override
	public boolean isValid(int folderOrPackageId) {
		/* the ID is valid if we can find its parent */
		return getParent(folderOrPackageId) != ErrorCode.NOT_FOUND;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#getScopeName(int)
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
	 * @see com.buildml.model.IPackageMgr#getScopeId(java.lang.String)
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
	 * @see com.buildml.model.IPackageMgr#parsePkgSpec(java.lang.String)
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
		int pkgId = getId(pkgName);

		/* if the user provided a /scope portion, convert that to an ID too */
		int scopeId = 0;
		if (scopeName != null) {
			scopeId = getScopeId(scopeName);
		}
		
		return new Integer[] {pkgId, scopeId};
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#setFilePackage(int, int, int)
	 */
	@Override
	public int setFilePackage(int fileId, int pkgId, int pkgScopeId) {
		
		/* we can't assign files into folders (only into packages) */
		if (isFolder(pkgId)) {
			return ErrorCode.BAD_VALUE;
		}
		
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
	 * @see com.buildml.model.IPackageMgr#getFilePackage(int)
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
	 * @see com.buildml.model.IPackageMgr#getFilesInPackage(int)
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
	 * @see com.buildml.model.IPackageMgr#getFilesInPackage(int, int)
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
	 * @see com.buildml.model.IPackageMgr#getFilesInPackage(java.lang.String)
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
	 * @see com.buildml.model.IPackageMgr#getFilesOutsidePackage(int)
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
	 * @see com.buildml.model.IPackageMgr#getFilesOutsidePackage(int, int)
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
	 * @see com.buildml.model.IPackageMgr#getFilesOutsidePackage(java.lang.String)
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
	 * @see com.buildml.model.IPackageMgr#setActionPackage(int, int)
	 */
	@Override
	public int setActionPackage(int actionId, int pkgId) {
		
		/* we can't assign actions into folders (only into packages) */
		if (isFolder(pkgId)) {
			return ErrorCode.BAD_VALUE;
		}

		try {
			updateActionPackagePrepStmt.setInt(1, pkgId);
			updateActionPackagePrepStmt.setInt(2, actionId);
			int rowCount = db.executePrepUpdate(updateActionPackagePrepStmt);
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
	 * @see com.buildml.model.IPackageMgr#getActionPackage(int)
	 */
	@Override
	public int getActionPackage(int actionId) {
		
		Integer results[];
		try {
			findActionPackagePrepStmt.setInt(1, actionId);
			results = db.executePrepSelectIntegerColumn(findActionPackagePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* no result == no package by this name */
		if (results.length == 0) {
			return ErrorCode.NOT_FOUND;
		} 
		
		/* 
		 * One result == we have the correct ID (note: it's not possible to have
		 * multiple results, since actionId is a unique key
		 */
		return results[0];
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#getActionsInPackage(int)
	 */
	@Override
	public ActionSet getActionsInPackage(int pkgId) {
		Integer results[] = null;
		try {
			findActionsInPackagePrepStmt.setInt(1, pkgId);
			results = db.executePrepSelectIntegerColumn(findActionsInPackagePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		return new ActionSet(actionMgr, results);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#getActionsInPackage(java.lang.String)
	 */
	@Override
	public ActionSet getActionsInPackage(String pkgSpec) {
		
		/* translate the package's name to its ID */
		int pkgId = getId(pkgSpec);
		if (pkgId == ErrorCode.NOT_FOUND){
			return null;
		}
		
		return getActionsInPackage(pkgId);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#getActionsOutsidePackage(int)
	 */
	@Override
	public ActionSet getActionsOutsidePackage(int pkgId) {
		Integer results[] = null;
		try {
			findActionsOutsidePackagePrepStmt.setInt(1, pkgId);
			results = db.executePrepSelectIntegerColumn(findActionsOutsidePackagePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		return new ActionSet(actionMgr, results);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#getActionsOutsidePackage(java.lang.String)
	 */
	@Override
	public ActionSet getActionsOutsidePackage(String pkgSpec) {
		
		/* translate the package's name to its ID */
		int pkgId = getId(pkgSpec);
		if (pkgId == ErrorCode.NOT_FOUND){
			return null;
		}
		
		return getActionsOutsidePackage(pkgId);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#addListener(com.buildml.model.IPackageMgrListener)
	 */
	@Override
	public void addListener(IPackageMgrListener listener) {
		listeners.add(listener);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#removeListener(com.buildml.model.IPackageMgrListener)
	 */
	@Override
	public void removeListener(IPackageMgrListener listener) {
		listeners.remove(listener);
	};
	
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
	
	/**
	 * Helper method for adding a new package or folder to the database. This is called by
	 * addPackage() or addFolder(), both which share the same name space.
	 * 
	 * @param name		Name of the new package or folder.
	 * @param isFolder	True if a folder should be created, else false for a package.
	 * @return			On success, return the package/folder's numeric ID. On failure,
	 *                  return:
	 *                     ErrorCode.ALREADY_USED - The name is already in use.
	 *                     ErrorCode.INVALID_NAME - The name doesn't conform to naming standards.
	 */
	private int addPackageOrFolderHelper(String name, boolean isFolder) {

		/* check that the package/folder doesn't already exist in the database */
		if (getId(name) != ErrorCode.NOT_FOUND){
			return ErrorCode.ALREADY_USED;
		}

		/* validate the new package/folder's name */
		if (!isValidName(name)){
			return ErrorCode.INVALID_NAME;
		}
				
		/* insert the package into our table */
		try {
			addPackagePrepStmt.setInt(1, isFolder ? 1 : 0);
			addPackagePrepStmt.setString(2, name);
			db.executePrepUpdate(addPackagePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* return the new package's ID number */
		return db.getLastRowID();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Notify any registered listeners about our change in state.
	 * @param pkgId   The package that has changed.
	 * @param how     The way in which the package changed (see {@link IPackageMgrListener}).
	 */
	private void notifyListeners(int pkgId, int how) {
		for (IPackageMgrListener listener : listeners) {
			listener.packageChangeNotification(pkgId, how);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
}