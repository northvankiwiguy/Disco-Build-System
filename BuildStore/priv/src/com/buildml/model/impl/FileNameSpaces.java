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

import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IFileIncludeMgr;
import com.buildml.model.impl.BuildTasks.OperationType;
import com.buildml.model.types.PathNameCache;
import com.buildml.model.types.PathNameCache.PathNameCacheValue;
import com.buildml.utils.errors.ErrorCode;
import com.buildml.utils.string.PathUtils;

/**
 * A manager class (that supports the BuildStore class) that manages all BuildStore
 * information about paths (files, directories, etc), as well as path roots.
 * <p>
 * There should be exactly one FileNameSpaces object per BuildStore object. Use the
 * BuildStore's getFileNameSpaces() method to obtain that one instance.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class FileNameSpaces {
	
	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/**
	 * The maximum number of files that this FileNamesSpaces object can handle.
	 */
	public static final int MAX_FILES = 16777216;
	
	/**
	 * Path types - paths can be directories, plain files, or symlinks.
	 */
	public enum PathType { 
		/** The path has an invalid type. */
		TYPE_INVALID, 
		
		/** The path refers to a directory. */
		TYPE_DIR, 
		
		/** The path refers to a file. */
		TYPE_FILE, 
		
		/** The path refers to a symlink. */
		TYPE_SYMLINK
	};
	
	/** The BuildStore object that "owns" this FileNameSpaces object. */
	private BuildStore buildStore;
	
	/**
	 * Our database manager object, used to access the database content. This is provided 
	 * to us when the FileNameSpaces is first instantiated.
	 */
	private BuildStoreDB db = null;
	
	/**
	 * A cache for recording the most recently accessed file name mappings. This
	 * helps to speed up file access.
	 */
	PathNameCache fileNameCache;
	
	/**
	 * Various prepared statement for database access.
	 */
	private PreparedStatement 
		findChildPrepStmt = null,
		insertChildPrepStmt = null,
		findPathDetailsPrepStmt = null,
		findPathIdFromParentPrepStmt = null,
		deletePathPrepStmt = null,
		insertRootPrepStmt = null,
		findRootPathIdPrepStmt = null,
		findRootNamesPrepStmt = null,
		findRootNamePrepStmt = null,
		updateRootPathPrepStmt = null,
		deleteFileRootPrepStmt = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new FileNameSpaces object.
	 * 
	 * @param buildStore The BuildStore that "owns" this FileNameSpaces manager object.
	 */
	public FileNameSpaces(BuildStore buildStore) {
		this.buildStore = buildStore;
		this.db = buildStore.getBuildStoreDB();
		
		/* initialize prepared database statements */
		findChildPrepStmt = db.prepareStatement("select id, pathType from files where parentId = ? and name = ?");
		insertChildPrepStmt = db.prepareStatement("insert into files values (null, ?, ?, 0, 0, ?)");
		findPathDetailsPrepStmt = db.prepareStatement("select parentId, pathType, files.name, fileRoots.name " +
				" from files left join fileRoots on files.id = fileRoots.fileId where files.id = ?");
		findPathIdFromParentPrepStmt = db.prepareStatement(
				"select id from files where parentId = ? and name != \"/\" order by name");
		deletePathPrepStmt = db.prepareStatement("delete from files where id = ?");
		insertRootPrepStmt = db.prepareStatement("insert into fileRoots values (?, ?)");	
		findRootPathIdPrepStmt = db.prepareStatement("select fileId from fileRoots where name = ?");
		findRootNamesPrepStmt = db.prepareStatement("select name from fileRoots order by name");
		findRootNamePrepStmt = db.prepareStatement("select name from fileRoots where fileId = ?");
		updateRootPathPrepStmt = db.prepareStatement("update fileRoots set fileId = ? where name = ?");
		deleteFileRootPrepStmt = db.prepareStatement("delete from fileRoots where name = ?");
		
		/* 
		 * Create an empty cache to record the most-recently accessed file name mapping, to save us from
		 * querying the database all the time.
		 */
		fileNameCache = new PathNameCache(40960);
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Retrieve the ID of the path that's currently associated with this path root.
	 * 
	 * @param rootName The name of the root.
	 * @return The namespace's root path ID, or ErrorCode.NOT_FOUND if it's not defined.
	 */
	public int getRootPath(String rootName) {
		Integer results[] = null;
		
		/* by default, assume it won't be found */
		int pathId = ErrorCode.NOT_FOUND;
		
		try {
			findRootPathIdPrepStmt.setString(1, rootName);
			results = db.executePrepSelectIntegerColumn(findRootPathIdPrepStmt);
			
			/* is there exactly one root registered? */
			if (results.length == 1) {
				pathId = results[0];
			}
			
			/* were there multiple roots with this name? Throw an error */
			else if (results.length > 1) {
				throw new FatalBuildStoreError("More than one root found with name: " + rootName);
			}			
			
		} catch (SQLException e) {
			new FatalBuildStoreError("Error in SQL: " + e);
		}	
		return pathId;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add a new root, to be associated with a specified path ID (which must refer to an
	 * existing directory).
	 * 
	 * @param rootName The name of the new root to be created.
	 * @param pathId The ID of the path the root should be attached to.
	 * @return 
	 * <ul>
	 *   <li>ErrorCode.OK on success.</li>
	 *   <li>ErrorCode.ONLY_ONE_ALLOWED if there's already a root associated with this path ID.</li>
	 *   <li>ErrorCode.ALREADY_USED if the root name is already in use.</li>
	 *   <li>ErrorCode.INVALID_NAME if the new proposed name is invalid.</li>
	 *   <li>ErrorCode.NOT_A_DIRECTORY if the path doesn't refer to a valid directory.</li>
	 * </ul>
	 */
	public int addNewRoot(String rootName, int pathId) {
		
		/* the root name can't already be in use */
		if (getRootPath(rootName) != ErrorCode.NOT_FOUND) {
			return ErrorCode.ALREADY_USED;
		}
		
		/* names can't contain @, :, or spaces, since this are considered root name separators */
		if (rootName.contains("@") || rootName.contains(":") || rootName.contains(" ")){
			return ErrorCode.INVALID_NAME;
		}
		
		/* this path must be valid, and refer to a directory, not a file */
		PathType pathType = getPathType(pathId);
		if (pathType == PathType.TYPE_INVALID) {
			return ErrorCode.BAD_PATH;
		}
		if (pathType != PathType.TYPE_DIR){
			return ErrorCode.NOT_A_DIRECTORY;
		}
		
		/* there can't already be a root at this path */
		if (getRootAtPath(pathId) != null) {
			return ErrorCode.ONLY_ONE_ALLOWED;
		}
		
		/* insert the root into our table */
		try {
			insertRootPrepStmt.setString(1, rootName);
			insertRootPrepStmt.setInt(2, pathId);
			db.executePrepUpdate(insertRootPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return ErrorCode.OK;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Return an array of all root names that are currently valid. The list is returned
	 * in alphabetical order.
	 * 
	 * @return A String array of root names.
	 */
	public String [] getRoots() {
		return db.executePrepSelectStringColumn(findRootNamesPrepStmt);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Move an existing root to be associated with a new path.
	 * 
	 * @param rootName Name of the root to be moved.
	 * @param pathId The ID of the new path to attach the root to.
	 * @return
	 * <ul>
	 *    <li>ErrorCode.OK if the move completed successfully.</li>
	 *    <li>ErrorCode.NOT_FOUND if the root doesn't exist.</li>
	 *    <li>ErrorCode.NOT_A_DIRECTORY if the new pathID doesn't refer to a directory.</li>
	 *    <li>ErrorCode.ONLY_ONE_ALLOWED if the target pathId already has a root.</li>
	 *    <li>ErrorCode.BAD_PATH an invalid path ID was provided.</li>
	 * </ul>
	 */
	public int moveRootToPath(String rootName, int pathId) {
	
		/* if the root doesn't already exists, that's an error */
		if (getRootPath(rootName) == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}
		
		/* is the new path valid? */
		PathType pathType = getPathType(pathId);
		if (pathType == PathType.TYPE_INVALID) {
			return ErrorCode.BAD_PATH;
		}
		
		/* is it a directory? */
		if (pathType != PathType.TYPE_DIR) {
			return ErrorCode.NOT_A_DIRECTORY;
		}
		
		/* does the pathId already have a root associated with it? */
		if (getRootAtPath(pathId) != null){
			return ErrorCode.ONLY_ONE_ALLOWED;
		}
		
		/* now, update the fileRoots table to refer to this new path */
		try {
			updateRootPathPrepStmt.setInt(1, pathId);
			updateRootPathPrepStmt.setString(2, rootName);
			db.executePrepUpdate(updateRootPathPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return ErrorCode.OK;
	}
		
	/*-------------------------------------------------------------------------------------*/

	/**
	 * If this path has an associated root attached to it, return the root name. If there's
	 * no root, return null. There can be at most one root associated with this path.
	 * 
	 * @param pathId The ID of the path we're querying.
	 * @return The name of the root, or null if there's no root attached.
	 */
	public String getRootAtPath(int pathId) {
		
		/* query the table to see if there's a record for this path ID */
		String results[] = null;
		try {
			findRootNamePrepStmt.setInt(1, pathId);
			results = db.executePrepSelectStringColumn(findRootNamePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* no result == no root at this path */
		if (results.length == 0) {
			return null;
		} 
		
		/* one result == we have the correct result */
		else if (results.length == 1) {
			return results[0];
		}
		
		/* multiple results is an error */
		else {
			throw new FatalBuildStoreError("Multiple entries found in fileRoots table, for path ID " + pathId);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the name of the root that encloses this path. There may be multiple roots
	 * above this path, but return the root that's closest to the path. The "root" root
	 * will always be the default if there's no closer root.
	 * 
	 * @param pathId The ID of the path to be queried.
	 * @return The name of the root that encloses the path.
	 */
	public String getEnclosingRoot(int pathId) {
		
		do {
			/* if the current path has a root, use it */
			String thisRoot = getRootAtPath(pathId);
			if (thisRoot != null) {
				return thisRoot;
			}
			/* else, move up to the parent */
			pathId = getParentPath(pathId);
		} while (pathId != ErrorCode.NOT_FOUND);
			
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	
	/**
	 * Delete the specified root from the database. The very top root ("root") can't be removed.
	 * 
	 * @param rootName The name of the root to be deleted.
	 * @return
	 *  <ul>
	 *    <li>ErrorCode.OK on success.</li>
	 *    <li>ErrorCode.NOT_FOUND if the root doesn't exist.</li>
	 *    <li>ErrorCode.CANT_REMOVE the "root" root can't be removed.</li>
	 *  </ul>
	 */
	public int deleteRoot(String rootName) {
		
		if (rootName.equals("root")) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* try to delete it from the database, but if no records where removed, that's an error */
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

	
	/**
	 * Add a new file to the database.
	 * 
	 * @param fullPathName The full path of the file.
	 * @return The ID of the newly added file, or ErrorCode.BAD_PATH if the file couldn't 
	 * be added within this part of the tree (such as when the parent itself is a 
	 * file, not a directory).
	 */
	public int addFile(String fullPathName) {
		return addPath(PathType.TYPE_FILE, fullPathName);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add a new directory to the database.
	 * 
	 * @param fullPathName The full path of the directory.
	 * @return The ID of the newly added file, or ErrorCode.BAD_PATH if the directory 
	 * couldn't be added within this part of the tree (such as when the parent itself 
	 * is a file, not a directory).
	 */
	public int addDirectory(String fullPathName) {
		return addPath(PathType.TYPE_DIR, fullPathName);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add a new symlink into the database.
	 * 
	 * @param fullPath The full path of the symlink.
	 * @return The ID of the newly added symlink, or ErrorCode.BAD_PATH if the symlink 
	 * couldn't be added within this part of the tree (such as when the parent itself 
	 * is a file, not a directory).
	 */
	public int addSymlink(String fullPath) {
		
		// TODO: implement this
		return ErrorCode.BAD_VALUE;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Given a parent's path ID, add a new child path directly within that parent. If the
	 * new path already exists, return the existing child path ID, rather than adding a
	 * new entry.
	 * 
	 * @param parentId The ID of the parent path.
	 * @param pathType The type of the path to be added (directory, file, etc)
	 * @param childName The name of the child path.
	 * @return
	 *   <ul>
	 *      <li>On success, the ID of the child path</li>
	 *      <li>ErrorCode.NOT_A_DIRECTORY if the parent isn't a directory.</li>
	 *      <li>ErrorCode.ONLY_ONE_ALLOWED if the child already exists but has the 
	 *                wrong path type (such as file instead of directory).</li>
	 *   </ul>
	 */
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
	
	/**
	 * Similar to addFile, but return an error if the path doesn't exist, rather than
	 * automatically adding it.
	 * 
	 * @param fullPathName The full path of the file.
	 * @return The path's ID, or ErrorCode.BAD_PATH if the path isn't defined.
	 */
	public int getPath(String fullPathName) {
		
		/* parse the path name and separate it into root and path components */
		String rootAndPath[] = getRootAndPath(fullPathName);
		String rootName = rootAndPath[0];
		String pathName = rootAndPath[1];

		/* split the path into components, separated by / */
		String components[] = PathUtils.tokenizePath(pathName);

		int parentID = getRootPath(rootName);
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
	
	/**
	 * Given a path ID, return a String containing the full path name, possibly
	 * including root names.
	 * 
	 * @param pathId The ID of the path to display as a String.
	 * @param showRoots True if we should show applicable file system roots in the
	 * string, else show the absolute path.
	 * @return The String representation of the path, in the form /a/b/c/..., possibly
	 * containing a file system root (e.g. @root/a/b/c/...)
	 */
	public String getPathName(int pathId, boolean showRoots) {

		// TODO: handle case where the pathId is invalid.
		StringBuffer sb = new StringBuffer();
		
		/* if we're at the root, simply return /, else recurse */
		if (pathId == 0) {
			if (showRoots) {
				sb.append("@root");
			} else {
				sb.append('/');
			}
		} else {
			getPathNameHelper(sb, pathId, showRoots);
		}
		return sb.toString();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a path ID, return a String containing the full path name.
	 * 
	 * @param pathId The ID of the path to display as a String.
	 * @return The String representation of the path, in the form /a/b/c/...
	 */
	public String getPathName(int pathId) {
		return getPathName(pathId, false);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Fetch the base name of this path. For example, if the path represents "a/b/c/d", then 
	 * return "d". If the pathId is invalid, return null.
	 * 
	 * @param pathId The ID of the path for which we want the base name.
	 * @return The path's base name as a String, or null if the pathId is invalid.
	 */
	public String getBaseName(int pathId) {
		
		Object pathDetails[] = getPathDetails(pathId);
		if (pathDetails == null) {
			return null;
		}
		return (String)pathDetails[2];
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Fetch the ID of the parent of this path. For example, if the path represents "a/b/c/d", then 
	 * return the ID of "a/b/c". If the pathId is invalid, return null. As a special case, the
	 * parent of "/" is itself "/".
	 * 
	 * @param pathId The ID of the path to determine the parent of.
	 * @return The path's parent ID, or ErrorCode.NOT_FOUND if the pathId is invalid.
	 */
	public int getParentPath(int pathId) {
		
		Object pathDetails[] = getPathDetails(pathId);
		if (pathDetails == null) {
			return ErrorCode.NOT_FOUND;
		}
		return (Integer)pathDetails[0];
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the type of the specified path. This will be one of the values in the PathType
	 * enum, such as TYPE_DIR, TYPE_FILE, or TYPE_SYMLINK.
	 * 
	 * @param pathId ID of the path to query.
	 * @return The type of this path.
	 */
	public PathType getPathType(int pathId) {		
		Object pathDetails[] = getPathDetails(pathId);
		if (pathDetails == null) {
			return PathType.TYPE_INVALID;
		}
		return (PathType)pathDetails[1];
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * For a given parent path, search for the specified child path. Return the child's path ID, 
	 * or ErrorCode.NOT_FOUND if the child isn't present. This method call only makes sense
	 * when the parent is a directory, or a symlink that points to a directory.
	 * 
	 * @param parentId The parent path under which the child should be found.
	 * @param childName The child's base name.
	 * @return The child's path ID, or ErrorCode.NOT_FOUND if it doesn't exist.
	 */
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

	/**
	 * For the specified parent path, fetch an array of all children of that parent.
	 * 
	 * @param pathId The ID of the parent path.
	 * @return An array of child path ID numbers.
	 */
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

	/**
	 * Remove a specific path from the build store. This operation can be only be performed
	 * on files and directories that are unused. That is, directories must be empty, and 
	 * files/directories must not be reference by any tasks (or any other such objects).
	 * 
	 * @param pathId The ID of the path to be removed.
	 * 
	 * @return ErrorCode.OK on successful removal, or ErrorCode.CANT_REMOVE if the
	 * path is still used in some way.
	 */
	public int removePath(int pathId)
	{
		/* 
		 * We need to refer to all these helper objects, to see if they
		 * use the path we're trying to delete.
		 */
		BuildTasks buildTasks = buildStore.getBuildTasks();
		FileAttributes fileAttrs = buildStore.getFileAttributes();
		IFileIncludeMgr fileIncludeMgr = buildStore.getFileIncludeMgr();
		
		/* check that this path doesn't have children */
		if (getChildPaths(pathId).length != 0) {
			return ErrorCode.CANT_REMOVE;
		}

		/* check that it's not used as the directory for any tasks */
		if (buildTasks.getTasksInDirectory(pathId).length != 0) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* check that it's not accessed by any tasks */
		if (buildTasks.getTasksThatAccess(pathId, 
					OperationType.OP_UNSPECIFIED).length != 0) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* check that it's not associated with a root */
		if (getRootAtPath(pathId) != null) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* check that it's not included by another file */
		if (fileIncludeMgr.getFilesThatInclude(pathId).length != 0) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* 
		 * All checks have passed, so remove from the file name cache, in 
		 * case it's there.
		 */
		fileNameCache.remove(getParentPath(pathId), getBaseName(pathId));
		
		/* now remove the entry from the "files" table */
		try {
			deletePathPrepStmt.setInt(1, pathId);
			db.executePrepUpdate(deletePathPrepStmt);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}	
		
		/* remove any attributes that are associated with this file */
		fileAttrs.deleteAllAttrOnPath(pathId);
		
		/* remove any file-includes where this file does the including */
		fileIncludeMgr.deleteFilesIncludedBy(pathId);
		
		/* success, the path has been removed */
		return ErrorCode.OK;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Returns a reference to this FileNameSpace's BuildStore object. 
	 * @return A reference to this FileNameSpace's BuildStore object.
	 */
	public BuildStore getBuildStore() {
		return buildStore;
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Helper method for addDirectory, addFile and addSymlink. Adds a new path, of the
	 * specified type (pathType) to the database. If the path already exists in
	 * the FileNameSpaces object, return the ID of the existing path rather than adding
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
		
		int parentId = getRootPath(rootName);
		
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
		return parentId;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the given path's parent path ID, and the path's own name.
	 * 
	 * @param pathId The ID of the path to query.
	 * @return An array of four Objects:
	 * <ul>
	 *    <li>The first is the parent's path ID (Integer)</li>
	 *    <li>The second is true if this is a directory, else false.</li>
	 *    <li>The third is the path's own name (String).</li>
	 *    <li>The fourth is the name of the attached root (if any).</li>
	 * </ul>
	 * Return null if there's no matching record.
	 */
	private Object[] getPathDetails(int pathId) {
		Object result[] = new Object[4];
		
		try {
			findPathDetailsPrepStmt.setInt(1, pathId);
			ResultSet rs = db.executePrepSelectResultSet(findPathDetailsPrepStmt);
			if (rs.next()){
				result[0] = rs.getInt(1);
				result[1] = intToPathType(rs.getInt(2));
				result[2] = rs.getString(3);
				result[3] = rs.getString(4);
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
	 * A helper method for getPathName(). This method is called recursively as we traverse from
	 * the path ID in question, right up to the root path. The recursive step moves up the
	 * path hierarchy until either / is reached or one of the path's is a "root". At that point,
	 * the recursion unwinds back to the start as it appends the path names to the result string.
	 * 
	 * @param sb The StringBuffer we'll append path component names onto (as we recurse).
	 * @param pathId The ID of the path we're currently looking at (whose name we'll append to sb).
	 * @param showRoots True if we should return a file system root (e.g. "@root") in the path name.
	 */
	private void getPathNameHelper(StringBuffer sb, int pathId, boolean showRoots) {

		/*
		 * Get the details of this path, including it's parent ID, its name and whether
		 * or not it's a root.
		 */
		Object pathDetails[] = getPathDetails(pathId);
		int parentId = (Integer)pathDetails[0];
		String name = (String)pathDetails[2];
		String rootName = (String)pathDetails[3];
	
		/* 
		 * If we're showing root names, and we've reached one, display it and terminate recursion
		 */
		if (showRoots && (rootName != null)) {
			sb.append('@');
			sb.append(rootName);
			return;
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
		getPathNameHelper(sb, parentId, showRoots);
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
			} catch (SQLException e) {
				throw new FatalBuildStoreError("Unable to execute SQL statement", e);
			}

			int lastRowId = db.getLastRowID();
			if (lastRowId >= MAX_FILES) {
				throw new FatalBuildStoreError("Exceeded maximum file number: " + MAX_FILES);
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
