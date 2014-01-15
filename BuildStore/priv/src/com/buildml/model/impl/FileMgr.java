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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IActionMgr;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.IActionTypeMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileAttributeMgr;
import com.buildml.model.IFileGroupMgrListener;
import com.buildml.model.IFileIncludeMgr;
import com.buildml.model.IFileMgr;
import com.buildml.model.IFileMgrListener;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMemberMgr.PackageDesc;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageRootMgr;
import com.buildml.model.ISlotTypes.SlotDetails;
import com.buildml.model.types.PathNameCache;
import com.buildml.model.types.PathNameCache.PathNameCacheValue;
import com.buildml.utils.errors.ErrorCode;
import com.buildml.utils.string.PathUtils;

/**
 * A manager class (that supports the BuildStore class) that manages all BuildStore
 * information about paths (files, directories, etc), as well as path roots.
 * <p>
 * There should be exactly one FileMgr object per BuildStore object. Use the
 * BuildStore's getFileMgr() method to obtain that one instance.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class FileMgr implements IFileMgr {
	
	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/
	
	/** The BuildStore object that "owns" this FileMgr object. */
	private IBuildStore buildStore;
	
	/**
	 * Our database manager object, used to access the database content. This is provided 
	 * to us when the FileMgr is first instantiated.
	 */
	private BuildStoreDB db = null;
	
	/**
	 * A cache for recording the most recently accessed file name mappings. This
	 * helps to speed up file access.
	 */
	PathNameCache fileNameCache;
	
	/**
	 * Other BuildStore managers we need to communicate with
	 */
	private IActionMgr actionMgr;
	private IActionTypeMgr actionTypeMgr;
	private IFileAttributeMgr fileAttrMgr;
	private IFileIncludeMgr fileIncludeMgr;
	
	/** The slotID for the "Directory" slot */
	private int dirSlotId;
	
	/**
	 * Various prepared statement for database access.
	 */
	private PreparedStatement 
		findChildPrepStmt = null,
		insertChildPrepStmt = null,
		findPathDetailsPrepStmt = null,
		findPathIdFromParentPrepStmt = null,
		trashPathPrepStmt = null,
		pathIsTrashPrepStmt = null,
		insertPackageMemberPrepStmt = null;
	
	/** The event listeners who are registered to learn about path changes */
	List<IFileMgrListener> listeners = new ArrayList<IFileMgrListener>();
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new FileMgr object.
	 * 
	 * @param buildStore The BuildStore that "owns" this FileMgr manager object.
	 */
	public FileMgr(BuildStore buildStore) {
		this.buildStore = buildStore;
		this.db = buildStore.getBuildStoreDB();
		
		/* initialize prepared database statements */
		findChildPrepStmt = db.prepareStatement("select id, pathType from files where parentId = ? and name = ? " +
												"and trashed = 0");
		insertChildPrepStmt = db.prepareStatement("insert into files values (null, ?, 0, ?, ?)");
		findPathDetailsPrepStmt = db.prepareStatement(
				"select parentId, pathType, files.name from files where files.id = ?");
		findPathIdFromParentPrepStmt = db.prepareStatement(
				"select id from files where parentId = ? and trashed = 0 and name != \"/\" order by name");
		trashPathPrepStmt = db.prepareStatement("update files set trashed = ? where id = ?");
		pathIsTrashPrepStmt = db.prepareStatement("select trashed from files where id = ?");
		insertPackageMemberPrepStmt = db.prepareStatement("insert into packageMembers values (?, ?, ?, ?, -1, -1)");
		
		/* 
		 * Create an empty cache to record the most-recently accessed file name mapping, to save us from
		 * querying the database all the time.
		 */
		fileNameCache = new PathNameCache(40960);
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#addFile(java.lang.String)
	 */
	@Override
	public int addFile(String fullPathName) {
		return addPath(PathType.TYPE_FILE, fullPathName);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#addDirectory(java.lang.String)
	 */
	@Override
	public int addDirectory(String fullPathName) {
		return addPath(PathType.TYPE_DIR, fullPathName);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#addSymlink(java.lang.String)
	 */
	@Override
	public int addSymlink(String fullPath) {
		
		// TODO: implement this
		return ErrorCode.BAD_VALUE;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#addChildOfPath(int, com.buildml.model.IFileMgr.PathType, java.lang.String)
	 */
	@Override
	public int addChildOfPath(int parentId, PathType pathType, String childName) {
		
		/*
		 * Validate that the parent path exists, and that it's TYPE_DIR.
		 */
		if (getPathType(parentId) != PathType.TYPE_DIR){
			return ErrorCode.NOT_A_DIRECTORY;
		}
		
		/* delegate to our helper function, which does the rest of the work */
		return addChildOfPathHelper(parentId, pathType, childName);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#getPath(java.lang.String)
	 */
	@Override
	public int getPath(String fullPathName) {
		
		/* parse the path name and separate it into root and path components */
		String rootAndPath[] = getRootAndPath(fullPathName);
		String rootName = rootAndPath[0];
		String pathName = rootAndPath[1];

		/* split the path into components, separated by / */
		String components[] = PathUtils.tokenizePath(pathName);

		IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
		int parentID = pkgRootMgr.getRootPath(rootName);
		for (int i = 0; i < components.length; i++) {
			
			/* get the next child ID, if it's missing, then the full path is missing */
			int childID = getChildOfPath(parentID, components[i]);
			if (childID == ErrorCode.NOT_FOUND) {
				return ErrorCode.BAD_PATH;
			}
			
			/* this component was found - move to the next */
			parentID = childID;
		}
		
		/* all path components exist, so return the last one */
		return parentID;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#getPathName(int, int)
	 */
	@Override
	public String getPathName(int pathId, int pkgId) {
		
		/* delegate to the common method */
		return getPathNameCommon(pathId, true, pkgId);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#getPathName(int, boolean)
	 */
	@Override
	public String getPathName(int pathId, boolean showRoots) {
		
		/* determine the path's current package */
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		IPackageMemberMgr pkgMemberMgr = buildStore.getPackageMemberMgr();
		
		/* automatically determine the path's package - we should default to <import> */
		PackageDesc pathPackage = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE, pathId);
		int pkgId = pkgMgr.getImportPackage();
		if (pathPackage != null) {
			pkgId = pathPackage.pkgId;
		}
		
		/* delegate to the common method */
		return getPathNameCommon(pathId, showRoots, pkgId);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#getPathName(int)
	 */
	@Override
	public String getPathName(int pathId) {
		return getPathName(pathId, false);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#getNativePath(int)
	 */
	@Override
	public String getNativePathName(int pathId) {

		/* get the path, relative to its root */
		String pathWithRoot = getPathName(pathId, true);
		int slashIndex = pathWithRoot.indexOf('/');
		if (slashIndex == -1){
			return null;
		}
		String rootName = pathWithRoot.substring(1, slashIndex);
		
		/* get the native path of the root (possibly with overrides) */
		IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
		String nativeRootPath = pkgRootMgr.getRootNative(rootName);
		if (nativeRootPath == null) {
			return null;
		}
		
		/* return the concatenation of the two */
		return nativeRootPath + pathWithRoot.substring(slashIndex);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#getBaseName(int)
	 */
	@Override
	public String getBaseName(int pathId) {
		
		Object pathDetails[] = getPathDetails(pathId);
		if (pathDetails == null) {
			return null;
		}
		return (String)pathDetails[2];
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#getParentPath(int)
	 */
	@Override
	public int getParentPath(int pathId) {
		
		Object pathDetails[] = getPathDetails(pathId);
		if (pathDetails == null) {
			return ErrorCode.NOT_FOUND;
		}
		return (Integer)pathDetails[0];
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#getPathType(int)
	 */
	@Override
	public PathType getPathType(int pathId) {		
		Object pathDetails[] = getPathDetails(pathId);
		if (pathDetails == null) {
			return PathType.TYPE_INVALID;
		}
		return (PathType)pathDetails[1];
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#getChildOfPath(int, java.lang.String)
	 */
	@Override
	public int getChildOfPath(int parentId, String childName)
	{
		Object [] result = getChildOfPathWithType(parentId, childName);
		if (result == null) {
			return ErrorCode.NOT_FOUND;
		} else {
			return (Integer)result[0];
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#getChildPaths(int)
	 */
	@Override
	public Integer[] getChildPaths(int pathId) {
		
		/*
		 * Fetch the records from the file table where the parentID is
		 * set to 'pathId'. The records will be returned in alphabetical
		 * order.
		 * 
		 * The corner case (built into the prepared statement
		 * query) is that / (the root) is always a child of itself, although
		 * we don't want to report it. The query already excludes /.
		 */
		Integer results[] = null;
		try {
			findPathIdFromParentPrepStmt.setInt(1, pathId);
			results = db.executePrepSelectIntegerColumn(findPathIdFromParentPrepStmt);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}	
		return results;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#removePath(int)
	 */
	@Override
	public int movePathToTrash(int pathId)
	{	
		/* check that this path doesn't have children */
		if (getChildPaths(pathId).length != 0) {
			return ErrorCode.CANT_REMOVE;
		}

		/* check that it's not used as the directory for any shell actions */
		Integer dirResult[] = actionMgr.getActionsWhereSlotEquals(dirSlotId, pathId);
		if ((dirResult == null) || (dirResult.length != 0)) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* check that it's not accessed by any actions */
		if (actionMgr.getActionsThatAccess(pathId, 
					OperationType.OP_UNSPECIFIED).length != 0) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* check that it's not associated with a root */
		IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
		if (pkgRootMgr.getRootsAtPath(pathId).length != 0) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* check that it's not included by another file */
		if (fileIncludeMgr.getFilesThatInclude(pathId).length != 0) {
			return ErrorCode.CANT_REMOVE;
		}

		/* check that it's not including another file */
		if (fileIncludeMgr.getFilesIncludedBy(pathId).length != 0) {
			return ErrorCode.CANT_REMOVE;
		}

		/* 
		 * All checks have passed, so remove from the file name cache, in 
		 * case it's there.
		 */
		fileNameCache.remove(getParentPath(pathId), getBaseName(pathId));
		
		/* now remove the entry from the "files" table (marking it as trashed) */
		try {
			trashPathPrepStmt.setInt(1, 1);
			trashPathPrepStmt.setInt(2, pathId);
			db.executePrepUpdate(trashPathPrepStmt);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}
		
		/* notify listeners */
		notifyListeners(pathId, IFileMgrListener.PATH_REMOVED);
		
		/* success, the path has been removed */
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#revivePath(int)
	 */
	@Override
	public int revivePathFromTrash(int pathId) {
				
		/* untrash the file record */
		try {
			trashPathPrepStmt.setInt(1, 0);
			trashPathPrepStmt.setInt(2, pathId);
			db.executePrepUpdate(trashPathPrepStmt);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}
		
		/* notify listeners */
		notifyListeners(pathId, IFileMgrListener.NEW_PATH);
		
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#isPathTrashed(int)
	 */
	@Override
	public boolean isPathTrashed(int pathId) {
		Integer results[] = null;
		
		try {
			pathIsTrashPrepStmt.setInt(1, pathId);
			results = db.executePrepSelectIntegerColumn(pathIsTrashPrepStmt);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}
		
		/* file isn't even known - let's assume it's trashed */
		if (results.length != 1) {
			return true;
		}
		
		/* if "trashed" field is 1, then the path is trashed */
		return results[0] == 1;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#isAncestorOf(int, int)
	 */
	@Override
	public boolean isAncestorOf(int directoryId, int pathId) {

		/* iterate upwards from pathId to @root, looking for directoryId */
		while (true) {
			int parentId = getParentPath(pathId);
			if (parentId == directoryId) {
				return true;
			}

			/* did we reach the @root without finding the ancestor? */
			if (parentId == pathId) {
				return false;
			}
			
			pathId = parentId;
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#getBuildStore()
	 */
	@Override
	public IBuildStore getBuildStore() {
		return buildStore;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#addListener(com.buildml.model.IFileMgrListener)
	 */
	@Override
	public void addListener(IFileMgrListener listener) {
		listeners.add(listener);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#removeListener(com.buildml.model.IFileMgrListener)
	 */
	@Override
	public void removeListener(IFileMgrListener listener) {
		listeners.remove(listener);		
	}
	
	/*=====================================================================================*
	 * PACKAGE METHODS
	 *=====================================================================================*/	

	/**
	 * Extra initialization that can only happen all other managers are initialized.
	 */
	/* package */ void initPass2() {
		/* 
		 * We need to refer to all these helper objects, to see if they
		 * use the path we're trying to delete.
		 */
		actionMgr = buildStore.getActionMgr();
		actionTypeMgr = buildStore.getActionTypeMgr();
		fileAttrMgr = buildStore.getFileAttributeMgr();
		fileIncludeMgr = buildStore.getFileIncludeMgr();
		
		/* fetch the slot ID for "Directory" */
		SlotDetails slotDetails = 
				actionTypeMgr.getSlotByName(ActionTypeMgr.BUILTIN_SHELL_COMMAND_ID, "Directory");
		dirSlotId = slotDetails.slotId;
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * Notify any registered listeners about our change in state.
	 * @param pathId   		The path that has changed.
	 * @param how     		The way in which the path changed (see {@link IFileMgrListener}).
	 */
	private void notifyListeners(int pathId, int how) {
		
		/* 
		 * Make a copy of the listeners list, otherwise a registered listener can't remove
		 * itself from the list within the pathChangeNotification() method.
		 */
		IFileMgrListener listenerCopy[] = listeners.toArray(new IFileMgrListener[listeners.size()]);
		for (int i = 0; i < listenerCopy.length; i++) {
			listenerCopy[i].pathChangeNotification(pathId, how);			
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper method for addDirectory, addFile and addSymlink. Adds a new path, of the
	 * specified type (pathType) to the database. If the path already exists in
	 * the FileMgr object, return the ID of the existing path rather than adding
	 * a new entry.
	 * 
	 * @param pathType The type of the path to be added (TYPE_DIR, TYPE_FILE, TYPE_SYMLINK).
	 * @param fullPathName The full absolute path name to be added.
	 * @return The new (or existing) path's ID, or ErrorCode.BAD_PATH if for some reason
	 * the path isn't valid.
	 */
	private int addPath(PathType pathType, String fullPathName) {
		
		/* parse the path name and separate it into root and path components */
		String rootAndPath[] = getRootAndPath(fullPathName);
		String rootName = rootAndPath[0];
		String pathName = rootAndPath[1];
		
		/* path's must be absolute (relative to the root of this name space */
		if (!PathUtils.isAbsolutePath(pathName)) {
			return ErrorCode.BAD_PATH;
		}
		
		/* convert the absolute path in /-separated path components */
		String components[] = PathUtils.tokenizePath(pathName);
		
		IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
		int parentId = pkgRootMgr.getRootPath(rootName);
		if (parentId == ErrorCode.NOT_FOUND) {
			return ErrorCode.BAD_PATH;
		}
		
		/* for each path name component, make sure it's added, then move to the next */
		int len = components.length - 1;
		for (int i = 0; i <= len; i++) {
			PathType thisType = (i == len) ? pathType : PathType.TYPE_DIR;
			int childId = addChildOfPathHelper(parentId, thisType, components[i]);
			parentId = childId;
			
			/* 
			 * If the path we just added didn't have the correct type, that's a problem.
			 * For example, if we thought we added a directory, but it was already added
			 * as a file, return ErrorCode.BAD_PATH;
			 */
			if (childId < 0) {
				return ErrorCode.BAD_PATH;
			}
			
		}
		
		/* notify all listeners */
		notifyListeners(parentId, IFileMgrListener.NEW_PATH);
		
		return parentId;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the given path's parent path ID, and the path's own name.
	 * 
	 * @param pathId The ID of the path to query.
	 * @return An array of three Objects:
	 * <ul>
	 *    <li>The first is the parent's path ID (Integer)</li>
	 *    <li>The second is true if this is a directory, else false.</li>
	 *    <li>The third is the path's own name (String).</li>
	 * </ul>
	 * Return null if there's no matching record.
	 */
	private Object[] getPathDetails(int pathId) {
		Object result[] = new Object[3];
		
		try {
			findPathDetailsPrepStmt.setInt(1, pathId);
			ResultSet rs = db.executePrepSelectResultSet(findPathDetailsPrepStmt);
			if (rs.next()){
				result[0] = rs.getInt(1);
				result[1] = intToPathType(rs.getInt(2));
				result[2] = rs.getString(3);
				rs.close();
			} else {
				
				/* error - there was no record, so the pathId must be invalid */
				return null;
			}
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("SQL error", e);
		}
				
		return result;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper function for translating from an ordinal integer to a PathType. This is the
	 * opposite of PathType.ordinal().
	 * 
	 * @param pathTypeNum The ordinal value of a PathType value.
	 * @return The corresponding PathType value.
	 * @throws FatalBuildStoreError if the ordinal value is out of range.
	 */
	private PathType intToPathType(int pathTypeNum)
			throws FatalBuildStoreError {
		switch (pathTypeNum) {
			case 0: return PathType.TYPE_INVALID;
			case 1: return PathType.TYPE_DIR;
			case 2: return PathType.TYPE_FILE;
			case 3: return PathType.TYPE_SYMLINK;
			default:
				throw new FatalBuildStoreError("Invalid value found in pathType field: " + pathTypeNum);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * A common method used by all getPathName() variants.
	 * 
	 * @param pathId	ID of the path to display.
	 * @param showRoots	True if we should show roots, else False.
	 * @param pkgId		ID of the package that this file belongs to (or should be considered as part of).
	 * @return The returned path name, possibly with roots.
	 */
	private String getPathNameCommon(int pathId, boolean showRoots, int pkgId) {

		/* is pathId a valid path? */
		if (getPathType(pathId) == PathType.TYPE_INVALID) {
			return null;
		}
		
		/* we accumulate the path string in a string builder */
		StringBuilder sb = new StringBuilder();
		
		/* if we're at the root, simply return /, else recurse */
		if (pathId == 0) {
			if (showRoots) {
				sb.append("@root");
			} else {
				sb.append('/');
			}
		} else {
			
			/* determine which package this file is in */
			IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
			int workspaceRootPathId = pkgRootMgr.getWorkspaceRoot();
			int pkgRootPathId = 0;
			String pkgRootName = null;
			
			/* error case: package not available - disable showRoots */
			if (workspaceRootPathId == ErrorCode.NOT_FOUND) {
				showRoots = false;
			}
			
			/* else, determine path of package root */
			else {
				pkgRootPathId = pkgRootMgr.getPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT);
				pkgRootName = pkgRootMgr.getPackageRootName(pkgId, IPackageRootMgr.SOURCE_ROOT);
			}
			
			getPathNameHelper(sb, pathId, showRoots, 
								workspaceRootPathId, pkgRootPathId, pkgRootName);
		}
		return sb.toString();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * A helper method for getPathName(). This method is called recursively as we traverse from
	 * the path ID in question, right up to the root path. The recursive step moves up the
	 * path hierarchy until either / is reached or one of the path's is a "root". At that point,
	 * the recursion unwinds back to the start as it appends the path names to the result string.
	 * 
	 * @param sb The StringBuffer we'll append path component names onto (as we recurse).
	 * @param pathId The ID of the path we're currently looking at (whose name we'll append to sb).
	 * @param showRoots True if we should return a file system root (e.g. "@root") in the path name.
	 * @param workspaceRootPathId Path ID of the "@workspace" root.
	 * @param pkgRootPathId PathID of the root for this file's package.
	 * @param pkgRootName Name of this file's package.
	 */
	private void getPathNameHelper(StringBuilder sb, int pathId, boolean showRoots, 
									int workspaceRootPathId, int pkgRootPathId, 
									String pkgRootName) {

		/*
		 * Get the details of this path, including it's parent ID, its name and whether
		 * or not it's a root.
		 */
		Object pathDetails[] = getPathDetails(pathId);
		int parentId = (Integer)pathDetails[0];
		String name = (String)pathDetails[2];
	
		/* 
		 * If we're showing root names, and we've reached one, display it. This can
		 * be @root, @workspace, or the path's own package root.
		 */
		if (showRoots) {
			if (pathId == 0) {
				sb.append("@root");
				return;
			} else if (pathId == workspaceRootPathId) {
				sb.append("@workspace");
				return;
			} else if (pathId == pkgRootPathId) {
				sb.append('@');
				sb.append(pkgRootName);
				return;
			}
		}
		
		/*
		 * If we're not showing roots, we terminate recursion at the / path.
		 */
		else if (!showRoots && name.equals("/")){
			return;
		}
		
		/* 
		 * Now the recursion has terminated and we start moving back along the sequence
		 * of paths until we reach the original path again. At each step, we'll append
		 * the path component onto the full result string.
		 */
		getPathNameHelper(sb, parentId, showRoots, 
				          workspaceRootPathId, pkgRootPathId, pkgRootName);
		sb.append("/");
		sb.append(name);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * A helper function for fetching the named child of a specified path, along with the
	 * path type of that child.
	 * 
	 * @param parentId The parent path's ID.
	 * @param childName The name of the child path to search for within this parent.
	 * @return A Object[2] array, where Object[0] is a Integer containing the path ID, and
	 *			Object[1] is a PathType object for the child.
	 */
	private Object[] getChildOfPathWithType(int parentId, String childName) {
		Object result[];
		
		/*
		 * Start by looking in the in-memory cache to see if it's there.
		 */
		PathNameCacheValue cacheValue = fileNameCache.get(parentId, childName);
		if (cacheValue != null) {
			return new Object[] { cacheValue.getChildPathId(), intToPathType(cacheValue.getChildType())};
		}
		// TODO: what happens if the mapping changes?
		
		/*
		 * Not in cache, try the database
		 */
		try {
			findChildPrepStmt.setInt(1, parentId);
			findChildPrepStmt.setString(2, childName);
			ResultSet rs = db.executePrepSelectResultSet(findChildPrepStmt);

			/* if there's a result, return it and add it to the cache for faster access next time */
			if (rs.next()){
				int childId = rs.getInt(1);
				int childType = rs.getInt(2);
				result = new Object[] { Integer.valueOf(childId), intToPathType(childType)};
				fileNameCache.put(parentId, childName, childId, childType);
			} 
			
			/* else, no result = no child */
			else {
				result = null;
			}
			
			rs.close();
			return result;
					
		} catch (SQLException e) {
			throw new FatalBuildStoreError("SQL problem in prepared statement", e);
		}
	}	
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a path name of the format "@root/absolute/path/name", split out the root and
	 * path components. If no root component is provided, default to "root". Note: minimal 
	 * error checking is done on the root and path names, so they should be validated by this
	 * method's caller.
	 * 
	 * @param fullPathName The full path string, possibly starting with a root name.
	 * @return A String[2] array, where element 0 is the root name, and element 1 is the 
	 * path name.
	 */
	private String[] getRootAndPath(String fullPathName) {
		
		/* the default values, in case no @root is provided */
		String rootName = "root";
		String pathName = fullPathName;
		
		/* 
		 * See if the path name starts with @, if so, 
		 * split the full path into "root" and "path name" components.
		 * The root part of the name will end when a "/" is seen. If
		 * there's no "/", then the whole fullPathName string is the 
		 * root name.
		 */
		if (fullPathName.startsWith("@")){
			int slashIndex = fullPathName.indexOf('/');
			if (slashIndex != -1){
				rootName = fullPathName.substring(1, slashIndex);
				pathName = fullPathName.substring(slashIndex);
			} else {
				rootName = fullPathName.substring(1);
				pathName = "/";
			}
		}

		String resultPair[] = new String[2];
		resultPair[0] = rootName;
		resultPair[1] = pathName;
		return resultPair;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * This is a helper function that does most of the work of the addChildOfPath() method,
	 * which is a public method. This helper function exists solely because we don't always
	 * want the extra error checking provided by addChildOfPath(), so sometimes we'll
	 * call this method directly.
	 * 
	 * @param parentId The ID of the parent path.
	 * @param pathType The type of the path to be added (directory, file, etc).
	 * @param childName The name of the child path to add.
	 * @return The ID of the child path, or ErrorCode.ONLY_ONE_ALLOWED if the path already 
	 * exists, but was of the wrong type.
	 */
	private int addChildOfPathHelper(int parentId, PathType pathType, String childName) {
		
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		int lastRowId;
		
		/*
		 * Search for the path ID and path type for a child of "parentId" that has
		 * the name "childName". This is similar to the getChildOfPath() operation,
		 * but we also fetch the path's type.
		 */
		Object childPathAndType[] = getChildOfPathWithType(parentId, childName);

		/* If child isn't yet present, we need to add it */
		if (childPathAndType == null) {
			/*
			 *  TODO: fix the race condition here - there's a small chance that somebody
			 *  else has already added it. Not thread safe.
			 */
			try {
				insertChildPrepStmt.setInt(1, parentId);
				insertChildPrepStmt.setInt(2, pathType.ordinal());
				insertChildPrepStmt.setString(3, childName);
				db.executePrepUpdate(insertChildPrepStmt);
				
				lastRowId = db.getLastRowID();
				if (lastRowId >= MAX_FILES) {
					throw new FatalBuildStoreError("Exceeded maximum file number: " + MAX_FILES);
				}
				
				/* insert the default package membership values */
				insertPackageMemberPrepStmt.setInt(1, IPackageMemberMgr.TYPE_FILE);
				insertPackageMemberPrepStmt.setInt(2, lastRowId);
				insertPackageMemberPrepStmt.setInt(3, pkgMgr.getImportPackage());
				insertPackageMemberPrepStmt.setInt(4, IPackageMemberMgr.SCOPE_NONE);
				if (db.executePrepUpdate(insertPackageMemberPrepStmt) != 1) {
					throw new FatalBuildStoreError("Unable to insert new record into packageMembers table");
				}
				
			} catch (SQLException e) {
				throw new FatalBuildStoreError("Unable to execute SQL statement", e);
			} catch (FatalBuildStoreError e) {
				/* 
				 * This is likely to happen if there's already a database row with the same name
				 * (which means there's a path with the same name that was trashed).
				 */
				return ErrorCode.BAD_PATH;
			}
			
			return lastRowId;
		}
		
		/* else, it exists, but we need to make sure it's the correct type of path */
		else if ((PathType)childPathAndType[1] != pathType) {
			return ErrorCode.ONLY_ONE_ALLOWED;
		}
		
		/* else, return the existing child ID */
		else {
			return (Integer)childPathAndType[0];
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
