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

import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageRootMgr;
import com.buildml.model.IFileMgr.PathType;
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
		this.db = buildStore.getBuildStoreDB();
		
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
	public int setWorkspaceRoot(int pathId) {
		// TODO: make sure that it's above all other roots (except for "root") and the bml file.
		// TODO: return ErrorCode.OUT_OF_RANGE in this case.

		int rc = addRoot("workspace", pathId);
		if (rc != ErrorCode.OK) {
			return rc;
		}
		
		/* cache the value for performance reasons */
		cachedWorkspaceRootId = pathId;
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
		
		/* refresh the cache with a new copy of the native workspace root */
		cachedWorkspaceRootNative = dbFile.toString();
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#setPackageRoot(int, int, java.lang.String)
	 */
	@Override
	public int setPackageRoot(int packageId, int type, int pathId) {

		// TODO: check that pathId is below "workspace". If not, return ErrorCode.OUT_OF_RANGE.
		
		/* derive the root name, e.g <package>_src or <package>_gen */
		String rootName = getPackageRootName(packageId, type);
		if (rootName == null) {
			return ErrorCode.NOT_FOUND;
		}
		
		return addRoot(rootName, pathId);
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
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#clearPackageRoot(int, int)
	 */
	@Override
	public int clearPackageRootNative(int packageId, int type) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#getPackageRoot(int, int)
	 */
	@Override
	public String getPackageRootNative(int packageId, int type) {
		// TODO: if there's an override of this root.
		// TODO:    if override is absolute, return that path.
		// TODO:    if override is relative, append to workspace root.
		// TODO: else:
		// TODO     compute relative path from workspace to root.
		// TODO:    append to native path of workspace.
		return "none";
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
			// TODO: compute packageId/type and call getRootNativePath(int, int).
		}
		return "none";	
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
}
