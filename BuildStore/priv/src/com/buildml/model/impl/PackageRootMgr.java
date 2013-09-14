/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.model.impl;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageMgrListener;
import com.buildml.model.IPackageRootMgr;
import com.buildml.model.IFileMgr.PathType;
import com.buildml.model.types.FileSet;
import com.buildml.utils.errors.ErrorCode;

/**
 * A {@link PackageRootMgr} object manages the mapping o
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class PackageRootMgr implements IPackageRootMgr {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The IBuildStore object that owns this manager */
	private IBuildStore buildStore;
	
	/** The associated FileMgr */
	private IFileMgr fileMgr;

	/** The associated PackageMgr */
	private IPackageMgr pkgMgr;

	/** The associated PackageMemberMgr */
	private IPackageMemberMgr pkgMemberMgr;

	/**
	 * Our database manager object, used to access the database content. This is provided 
	 * to us when the PackageRootMgr object is first instantiated.
	 */
	private BuildStoreDB db = null;
	
	/**
	 * We cache the pathID of the workspace root, to save accessing the database too often.
	 */
	private int cachedWorkspaceRootId = -1;
	
	/**
	 * The cached version of the native workspace root.
	 */
	private String cachedWorkspaceRootNative = null;
	
	/**
	 * The map of root names to native paths. The format of the key is "<pkgId>_<rootType>".
     * For example, "34_1".
	 */
	private HashMap<String, String> nativeRootMap = null;
	
	/**
	 * Various prepared statements for database access.
	 */
	private PreparedStatement 
		getWorkspaceDistancePrepStmt = null,
		setWorkspaceDistancePrepStmt = null,
		findRootPathIdPrepStmt = null,
		insertRootPrepStmt = null,
		updateRootPathPrepStmt = null,
		findRootNamesPrepStmt = null,
		findRootNamesAtPathPrepStmt = null,
		deleteFileRootPrepStmt = null;
	
	/** The event listeners who are registered to learn about package changes */
	List<IPackageMgrListener> listeners = new ArrayList<IPackageMgrListener>();

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new PackageRootMgr.
	 * 
	 * @param buildStore The BuildStore that this PackageRootMgr object belongs to.
	 */
	public PackageRootMgr(BuildStore buildStore) {
		this.buildStore = buildStore;
		this.fileMgr = buildStore.getFileMgr();
		this.pkgMgr = buildStore.getPackageMgr();
		this.pkgMemberMgr = buildStore.getPackageMemberMgr();
		this.db = buildStore.getBuildStoreDB();
		
		/* initialize map of root -> native path mappings */
		nativeRootMap = new HashMap<String, String>();
		
		/* initialize prepared database statements */
		getWorkspaceDistancePrepStmt = 
				db.prepareStatement("select distance from workspace");
		setWorkspaceDistancePrepStmt = 
				db.prepareStatement("update workspace set distance = ?");
		findRootPathIdPrepStmt = 
				db.prepareStatement("select fileId from fileRoots where name = ?");
		insertRootPrepStmt = 
				db.prepareStatement("insert into fileRoots values (?, ?)");
		updateRootPathPrepStmt = 
				db.prepareStatement("update fileRoots set fileId = ? where name = ?");
		findRootNamesPrepStmt = 
				db.prepareStatement("select name from fileRoots order by name");
		findRootNamesAtPathPrepStmt = 
				db.prepareStatement("select name from fileRoots where fileId = ? order by name");
		deleteFileRootPrepStmt = 
				db.prepareStatement("delete from fileRoots where name = ?");
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#setWorkspaceRoot(int)
	 */
	@Override
	public int setWorkspaceRoot(int workspacePathId) {
		
		/* 
		 * Verify that all existing package roots are at the same level, or below the
		 * new proposed workspace root.
		 */
		String [] roots = getRoots();
		for (int i = 0; i < roots.length; i++) {
			String rootName = roots[i];
			if (!rootName.equals("root") && !rootName.equals("workspace")) {
				int rootPathId = getRootPath(rootName);
				if ((rootPathId != workspacePathId) && 
					 !fileMgr.isAncestorOf(workspacePathId, rootPathId)) {
					return ErrorCode.OUT_OF_RANGE;
				}
			}
		}
		
		int rc = addRoot("workspace", workspacePathId);
		if (rc != ErrorCode.OK) {
			return rc;
		}
		
		/* cache the value for performance reasons */
		cachedWorkspaceRootId = workspacePathId;
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#getWorkspaceRoot()
	 */
	@Override
	public int getWorkspaceRoot() {

		/* If we don't have a cached copy, query the database */
		if (cachedWorkspaceRootId == -1) {
			Integer results[] = null;
			try {
				findRootPathIdPrepStmt.setString(1, "workspace");
				results = db.executePrepSelectIntegerColumn(findRootPathIdPrepStmt);

				/* is there exactly one root registered? */
				if (results.length == 1) {
					cachedWorkspaceRootId = results[0];
				}
				
				/* else, we didn't find the root */
				else {
					return ErrorCode.NOT_FOUND;
				}

			} catch (SQLException e) {
				new FatalBuildStoreError("Error in SQL: " + e);
			}

		}
		return cachedWorkspaceRootId;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#setWorkspaceRootNative(java.lang.String)
	 */
	@Override
	public int setWorkspaceRootNative(String nativePath) {
		
		/* check that this in a valid native directory */
		File dirFile = new File(nativePath);
		if (!dirFile.isDirectory()) {
			return ErrorCode.NOT_A_DIRECTORY;
		}

		/* this path is only stored locally - not persisted in the database */
		cachedWorkspaceRootNative = dirFile.toString();
		return ErrorCode.OK;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#getWorkspaceRootNative()
	 */
	@Override
	public String getWorkspaceRootNative() {
		
		/* 
		 * If there's no cached native workspace root (nobody has done a setWorkspaceRootNative()
		 * or a setBuildMLDepth()), compute it from the persisted "depth"
		 */
		if (cachedWorkspaceRootNative == null) {
			Integer results[] = db.executePrepSelectIntegerColumn(getWorkspaceDistancePrepStmt);
			if (results.length == 1) {
				setBuildMLFileDepth(results[0]);
			} else {
				throw new FatalBuildStoreError(
						"Unable to determine build.bml file depth in workspace");
			}
		}
		return cachedWorkspaceRootNative;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#setRelativeWorkspace(int)
	 */
	@Override
	public int setBuildMLFileDepth(int depth) {
		
		if (depth < 0) {
			return ErrorCode.BAD_PATH;
		}
		
		/* determine the path to our BuildML file, and move upwards "depth" levels. */
		String dbFileName = db.getDatabaseFileName();
		File dbFile = new File(dbFileName);
		for (int i = 0; i <= depth; i++) {
			dbFile = dbFile.getParentFile();
			if (dbFile == null) {
				return ErrorCode.BAD_PATH;
			}
		}

		/* persistent the depth value into the database */
		try {
			setWorkspaceDistancePrepStmt.setInt(1, depth);
			db.executePrepUpdate(setWorkspaceDistancePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* 
		 * Refresh the cache with a new copy of the native workspace root. Note that
		 * often the suffix /. is added to the file name (probably because of the getParentFile()
		 * operation above. We strip this off, since it's unattractive.
		 */
		cachedWorkspaceRootNative = dbFile.toString();
		if (cachedWorkspaceRootNative.endsWith("/.")) {
			cachedWorkspaceRootNative = 
					cachedWorkspaceRootNative.substring(0, cachedWorkspaceRootNative.length() - 2);
		}
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#setPackageRoot(int, int, java.lang.String)
	 */
	@Override
	public int setPackageRoot(int packageId, int type, int rootPathId) {

		/* check that pathId is valid (not trashed) */
		if (fileMgr.isPathTrashed(rootPathId)) {
			return ErrorCode.BAD_PATH;
		}
		
		/* check that pathId is a directory */
		if (fileMgr.getPathType(rootPathId) != PathType.TYPE_DIR) {
			return ErrorCode.NOT_A_DIRECTORY;
		}

		/* check that pathId is below @workspace */
		int workspaceRootId = getWorkspaceRoot();
		if (workspaceRootId == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}
		if ((workspaceRootId != rootPathId) &&
			(!fileMgr.isAncestorOf(workspaceRootId, rootPathId))) {
			return ErrorCode.OUT_OF_RANGE;
		}
		
		/* we can't modify the <import> package */
		if (packageId == pkgMgr.getImportPackage()) {
			return ErrorCode.NOT_FOUND;
		}

		/* test that all paths in the package are below the new proposed package root */
		FileSet memberPaths = pkgMemberMgr.getFilesInPackage(packageId);
		for (int memberPathId : memberPaths) {
			if ((memberPathId != rootPathId) && 
				(!fileMgr.isAncestorOf(rootPathId, memberPathId))) {
				return ErrorCode.OUT_OF_RANGE;
			}
		}
		
		/* derive the root name, e.g <package>_src or <package>_gen */
		String rootName = getPackageRootName(packageId, type);
		if (rootName == null) {
			return ErrorCode.NOT_FOUND;
		}
		
		/* fetch the existing root - we need to know if it changes before we notify */
		int existingRootPathId = getPackageRoot(packageId, type);
		int rc = addRoot(rootName, rootPathId);
		
		/* if the root actually changed, notify listeners */
		if (existingRootPathId != rootPathId) {
			notifyListeners(packageId, IPackageMgrListener.CHANGED_ROOTS);
		}
		return rc;
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#getPackageRoot(int, int)
	 */
	@Override
	public int getPackageRoot(int packageId, int type)
	{
		String rootName = getPackageRootName(packageId, type);
		if (rootName == null) {
			return ErrorCode.NOT_FOUND;
		}
		return getRootPath(rootName);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#removePackageRoot(int, int)
	 */
	@Override
	public int removePackageRoot(int packageId, int type) {
		
		String rootName = getPackageRootName(packageId, type);
		if (rootName == null) {
			return ErrorCode.NOT_FOUND;
		}
		
		try {
			deleteFileRootPrepStmt.setString(1, rootName);
			int rowCount = db.executePrepUpdate(deleteFileRootPrepStmt);
			if (rowCount == 0) {
				return ErrorCode.NOT_FOUND;
			}
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		return ErrorCode.OK;
	}

	
	/*-------------------------------------------------------------------------------------*/

	/*
	 * (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#getPackageRootName(int, int)
	 */
	@Override
	public String getPackageRootName(int packageId, int type) {

		/* Not supported for folders, invalid packages or <import> */
		if (pkgMgr.isFolder(packageId) || !pkgMgr.isValid(packageId) ||
				(packageId == pkgMgr.getImportPackage())) {
			return null;
		}
		
		/* get the package name, and append _src or _gen */
		String packageName = pkgMgr.getName(packageId);
		if (packageName == null) {
			return null;
		}
		if (type == IPackageRootMgr.SOURCE_ROOT) {
			return packageName + "_src";
		} else if (type == IPackageRootMgr.GENERATED_ROOT) {
			return packageName + "_gen";
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a root name, return the associated path ID.
	 * 
	 * @param rootName Name of the root to search for. 
	 * @return The pathId associated with the root, or ErrorCode.NOT_FOUND if the root name
	 *         is invalid.
	 */
	public int getRootPath(String rootName) {

		try {
			findRootPathIdPrepStmt.setString(1, rootName);
			Integer results[] = db.executePrepSelectIntegerColumn(findRootPathIdPrepStmt);

			/* is there exactly one root registered? */
			if (results.length == 1) {
				return results[0];
			}
		} catch (SQLException e) {
			new FatalBuildStoreError("Error in SQL: " + e);
		}
		return ErrorCode.NOT_FOUND;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#overridePackageRoot(int, int, java.lang.String)
	 */
	@Override
	public int setPackageRootNative(int packageId, int type, String path) {
		
		/* validate the package ID */
		String pkgName = pkgMgr.getName(packageId);
		if (pkgName == null){
			return ErrorCode.NOT_FOUND;
		}
		
		/* validate the package type */
		if ((type != IPackageRootMgr.SOURCE_ROOT) &&
				(type != IPackageRootMgr.GENERATED_ROOT)) {
			return ErrorCode.BAD_VALUE;
		}
		
		/* paths must be absolute */
		if (!path.startsWith("/")) {
			return ErrorCode.BAD_PATH;
		}
		
		/* validate that paths refer to a real native directory */
		File nativePath = new File(path);
		if (!nativePath.exists() || !nativePath.isDirectory()) {
			return ErrorCode.BAD_PATH;
		}
		
		nativeRootMap.put(packageId + "_" + type, path);
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#clearPackageRoot(int, int)
	 */
	@Override
	public int clearPackageRootNative(int packageId, int type) {

		/* validate the package ID */
		String pkgName = pkgMgr.getName(packageId);
		if (pkgName == null){
			return ErrorCode.NOT_FOUND;
		}
		
		/* validate the package type */
		if ((type != IPackageRootMgr.SOURCE_ROOT) &&
				(type != IPackageRootMgr.GENERATED_ROOT)) {
			return ErrorCode.BAD_VALUE;
		}

		/* remove the native path mapping (which may or may not already exist) */
		nativeRootMap.remove(packageId + "_" + type);
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#getPackageRoot(int, int)
	 */
	@Override
	public String getPackageRootNative(int packageId, int type) {

		/* is there a native path override? */
		String override = nativeRootMap.get(packageId + "_" + type);
		if (override != null) {
			return override;
		}

		/* 
		 * No override - compute relative path from workspace to root, and append to
		 * the native path of the workspace.
		 */
		int wsRootId = getWorkspaceRoot();
		int rootId = getPackageRoot(packageId, type);
		if (rootId == ErrorCode.NOT_FOUND) {
			return null;
		}
		String wsRootPath = fileMgr.getPathName(wsRootId);
		String rootPath = fileMgr.getPathName(rootId);
		String relativePath = rootPath.substring(wsRootPath.length(), rootPath.length());
		return getWorkspaceRootNative() + relativePath;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#getPackageRoot(java.lang.String)
	 */
	@Override
	public String getRootNative(String rootName) {

		if (rootName.equals("root")) {
			return "/";
		}
		
		else if (rootName.equals("workspace")) {
			return getWorkspaceRootNative();
		}
		
		else {
			
			/* determine type of root, based on suffix */
			int packageType;
			if (rootName.endsWith("_src")) {
				packageType = IPackageRootMgr.SOURCE_ROOT;
			} else if (rootName.endsWith("_gen")) {
				packageType = IPackageRootMgr.GENERATED_ROOT;				
			} else {
				return null;
			}
			
			/* find package ID number */
			String packageName = rootName.substring(0, rootName.length() - 4);
			int pkgId = pkgMgr.getId(packageName);
			if (pkgId == ErrorCode.NOT_FOUND) {
				return null;
			}			
			return getPackageRootNative(pkgId, packageType);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#getRoots()
	 */
	@Override
	public String[] getRoots() {
		return db.executePrepSelectStringColumn(findRootNamesPrepStmt);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#getRootAtPath(int)
	 */
	@Override
	public String[] getRootsAtPath(int pathId) {

		/* fetch all records at this path */
		try {
			findRootNamesAtPathPrepStmt.setInt(1, pathId);
			return db.executePrepSelectStringColumn(findRootNamesAtPathPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
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
	 * @param rootName  Name of the root to be added (updated).
	 * @param pathId	The ID of the path to attach the root to.
	 * @return ErrorCode.OK on success, ErrorCode.BAD_PATH if the pathId is invalid,
	 *         ErrorCode.NOT_A_DIRECTORY if the pathId doesn't reference a directory.
	 */
	private int addRoot(String rootName, int pathId) {
		
		/* the path must not be trashed */
		if (fileMgr.isPathTrashed(pathId)) {
			return ErrorCode.BAD_PATH;
		}
		
		/* this path must be valid, and refer to a directory, not a file */
		PathType pathType = fileMgr.getPathType(pathId);
		if (pathType == PathType.TYPE_INVALID) {
			return ErrorCode.BAD_PATH;
		}
		if (pathType != PathType.TYPE_DIR){
			return ErrorCode.NOT_A_DIRECTORY;
		}
		
		/* start by trying to update the existing "workspace" record */
		try {
			updateRootPathPrepStmt.setInt(1, pathId);
			updateRootPathPrepStmt.setString(2, rootName);
			if (db.executePrepUpdate(updateRootPathPrepStmt) == 0) {
				
				/* doesn't exist yet, insert it */
				insertRootPrepStmt.setString(1, rootName);
				insertRootPrepStmt.setInt(2, pathId);
				db.executePrepUpdate(insertRootPrepStmt);				
			}
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return ErrorCode.OK;
	}
	
	/*-------------------------------------------------------------------------------------*/

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
		IPackageMgrListener listenerCopy[] = 
				listeners.toArray(new IPackageMgrListener[listeners.size()]);
		for (int i = 0; i < listenerCopy.length; i++) {
			listenerCopy[i].packageChangeNotification(pkgId, how);			
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
}
