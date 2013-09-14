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
import com.buildml.model.IFileMgr.PathType;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMemberMgrListener;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageMgrListener;
import com.buildml.model.IPackageRootMgr;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.ActionSet;
import com.buildml.utils.errors.ErrorCode;

/**
 * A manager class (that supports the BuildStore class) responsible for managing all 
 * BuildStore information pertaining to the membership of packages.
 * <p>
 * There should be exactly one PackageMemberMgr object per BuildStore object. Use the
 * BuildStore's getPackageMemberMgr() method to obtain that one instance.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
/* package private */ class PackageMemberMgr implements IPackageMemberMgr {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

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
	
	/** The PackageMgr object that manages the list of packages. */
	private IPackageMgr pkgMgr = null;
	
	/**
	 * Various prepared statements for database access.
	 */
	private PreparedStatement 
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
	
	/** The event listeners who are registered to learn about package membership changes */
	List<IPackageMemberMgrListener> listeners = new ArrayList<IPackageMemberMgrListener>();
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new Packages object, which represents the file/action packages that
	 * are part of the BuildStore.
	 * 
	 * @param buildStore The BuildStore that this Packages object belongs to.
	 */
	public PackageMemberMgr(BuildStore buildStore) {
		this.buildStore = buildStore;
		this.db = buildStore.getBuildStoreDB();
		this.fileMgr = buildStore.getFileMgr();
		this.actionMgr = buildStore.getActionMgr();
		this.pkgMgr = buildStore.getPackageMgr();
		
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
		int pkgId = pkgMgr.getId(pkgName);

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
		if (pkgMgr.isFolder(pkgId)) {
			return ErrorCode.BAD_VALUE;
		}
		
		/* the path must be valid and not-trashed */
		if ((fileMgr.getPathType(fileId) == PathType.TYPE_INVALID) ||
			(fileMgr.isPathTrashed(fileId))) {
			return ErrorCode.NOT_FOUND;
		}
		
		/* 
		 * Check that the path falls under the package's root (except for <import> which
		 * doesn't have root restrictions).
		 */
		if (pkgId != pkgMgr.getImportPackage()) {
			IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
			int pkgRootPathId = pkgRootMgr.getPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT);
			if (pkgRootPathId == ErrorCode.NOT_FOUND) {
				return ErrorCode.NOT_FOUND;
			}
			if ((pkgRootPathId != fileId) && !fileMgr.isAncestorOf(pkgRootPathId, fileId)) {
				return ErrorCode.OUT_OF_RANGE;
			}
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
		if (pkgMgr.isFolder(pkgId)) {
			return ErrorCode.BAD_VALUE;
		}

		/* determine which package the action is currently in */
		int oldPkgId = getActionPackage(actionId);
		if (oldPkgId == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}
		
		/* if the package is unchanged, we're done */
		if (oldPkgId == pkgId) {
			return ErrorCode.OK;
		}
		
		/* update the database */
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
		
		/* 
		 * Notify listeners about the change in package content.
		 */
		notifyListeners(oldPkgId, IPackageMgrListener.CHANGED_MEMBERSHIP);
		notifyListeners(pkgId, IPackageMgrListener.CHANGED_MEMBERSHIP);

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
		int pkgId = pkgMgr.getId(pkgSpec);
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
		int pkgId = pkgMgr.getId(pkgSpec);
		if (pkgId == ErrorCode.NOT_FOUND){
			return null;
		}
		
		return getActionsOutsidePackage(pkgId);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#addListener(com.buildml.model.IPackageMemberMgrListener)
	 */
	@Override
	public void addListener(IPackageMemberMgrListener listener) {
		listeners.add(listener);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#removeListener(com.buildml.model.IPackageMemberMgrListener)
	 */
	@Override
	public void removeListener(IPackageMemberMgrListener listener) {
		listeners.remove(listener);
	};
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * Notify any registered listeners about our change in state.
	 * @param pkgId   The package that has changed.
	 * @param how     The way in which the package changed (see {@link IPackageMgrListener}).
	 */
	private void notifyListeners(int pkgId, int how) {
		
		/* 
		 * Make a copy of the listeners list, otherwise a registered listener can't remove
		 * itself from the list within the packageChangeNotification() method.
		 */
		IPackageMemberMgrListener listenerCopy[] = 
				listeners.toArray(new IPackageMemberMgrListener[listeners.size()]);
		for (int i = 0; i < listenerCopy.length; i++) {
			listenerCopy[i].packageMemberChangeNotification(pkgId, how);			
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
}