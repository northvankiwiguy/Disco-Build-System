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

package com.arapiki.disco.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.arapiki.disco.model.FileNameCache.FileNameCacheValue;
import com.arapiki.utils.errors.ErrorCode;
import com.arapiki.utils.string.PathUtils;

// TODO: comment and clean-up this file.

/**
 * A helper class to manage the file tree structures within a BuildStore.
 * The class encapsulates everything that's known about the content of the
 * file system during the build process (although nothing about the relationship
 * between files, or the commands used to create them).
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class FileNameSpaces {
	
	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/**
	 * Path types - paths can be directories, plain files, or symlinks.
	 */
	public enum PathType { TYPE_INVALID, TYPE_DIR, TYPE_FILE, TYPE_SYMLINK };
	
	/**
	 * The BuildStore object that "owns" this FileNameSpaces object.
	 */
	private BuildStore buildStore;
	
	/**
	 * Our database manager object, used to access the database content. This is provided 
	 * to us when the BuildStoreFileSpace is first instantiated.
	 */
	private BuildStoreDB db = null;
	
	/**
	 * A set of database tables may contain multiple namespaces. This name is used to
	 * differentiate one name space from another. All operations on this object will
	 * be done within this namespace.
	 */
	private String spaceName;
	
	/**
	 * A cache for recording the most recently accessed file name mappings.
	 */
	FileNameCache fileNameCache;
	
	/**
	 * Various prepared statement for database access.
	 */
	private PreparedStatement 
		findChildPrepStmt = null,
		insertChildPrepStmt = null,
		findPathDetailsPrepStmt = null,
		findPathIdFromParentPrepStmt = null,
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
	 * Create a new BuildStoreFileSpace object.
	 * @param db The BuildStoreDB object to use when accessing the underlying database.
	 */
	public FileNameSpaces(BuildStore bs) {
		this.buildStore = bs;
		this.db = bs.getBuildStoreDB();
		
		/* initialize prepared database statements */
		findChildPrepStmt = db.prepareStatement("select id, pathType from files where parentId = ? and name = ?");
		insertChildPrepStmt = db.prepareStatement("insert into files values (null, ?, ?, 0, 0, ?)");
		findPathDetailsPrepStmt = db.prepareStatement("select parentId, pathType, files.name, fileRoots.name " +
				" from files left join fileRoots on files.id = fileRoots.fileId where files.id = ?");
		findPathIdFromParentPrepStmt = db.prepareStatement(
				"select id from files where parentId = ? and name != \"/\" order by name");
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
		fileNameCache = new FileNameCache(40960);
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Retrieve the ID of the path that's currently associated with this root.
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
	 * Add a new root to be associated with a specified path ID, which must refer to an
	 * existing directory.
	 * Returns:
	 * 	 ErrorCode.OK on success
	 *   ErrorCode.ONLY_ONE_ALLOWED if there's already a root associated with this path ID,
	 *   ErrorCode.ALREADY_USED if the root name is already in use
	 *   ErrorCode.INVALID_NAME if the new proposed name is invalid.
	 *   ErrorCode.NOT_A_DIRECTORY if the path doesn't refer to a valid directory.
	 */
	public int addNewRoot(String rootName, int pathId) {
		
		/* the root name can't already be in use */
		if (getRootPath(rootName) != ErrorCode.NOT_FOUND) {
			return ErrorCode.ALREADY_USED;
		}
		
		/* names can't contain :, or spaces, since this are considered root name separators */
		if (rootName.contains(":") || rootName.contains(" ")){
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
	
	/*
	 * Return an array of all root names that are currently valid. The list is returned
	 * in alphabetical order.
	 */
	public String [] getRoots() {
		return db.executePrepSelectStringColumn(findRootNamesPrepStmt);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Move an existing root to refer to an existing pathId.
	 * Return codes:
	 *    ErrorCode.OK if the move completed successfully.
	 *    ErrorCode.NOT_FOUND if the root doesn't exist
	 *    ErrorCode.NOT_A_DIRECTORY if the new pathID doesn't refer to a directory
	 *    ErrorCode.ONLY_ONE_ALLOWED if the target pathId already has a root.
	 *    ErrorCode.BAD_PATH an invalid path ID was provided
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
	 * Return the name of the root that is encloses this path. There may be multiple roots
	 * above this path, but return the root that's closest to the path. The "root" root
	 * will always be the default if there's no closer root.
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
		} while (pathId != -1);
			
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	
	/**
	 * Delete the specified root from the database. The very top root ("root") can't be removed.
	 * Return:
	 *     ErrorCode.OK on success
	 *     ErrorCode.NOT_FOUND if the root doesn't exist.
	 *     ErrorCode.CANT_REMOVE the "root" root can't be removed.
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
	 * Add a new file into the FileNameSpace.
	 * @param fullPathName The full path of the file.
	 * @return the ID of the newly added file, or ErrorCode.BAD_PATH if the file couldn't 
	 * be added within this part of the tree (such as when the parent itself is a 
	 * file, not a directory).
	 */
	public int addFile(String fullPathName) {
		return addPath(PathType.TYPE_FILE, fullPathName);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add a new directory into the FileNameSpace.
	 * @param fullPathName The full path of the directory.
	 * @return the ID of the newly added file, or ErrorCode.BAD_PATH if the directory 
	 * couldn't be added within this part of the tree (such as when the parent itself 
	 * is a file, not a directory).
	 */
	public int addDirectory(String fullPathName) {
		return addPath(PathType.TYPE_DIR, fullPathName);
	}
	
	/*-------------------------------------------------------------------------------------*/

	public int addSymlink(String fullPath) {
		
		// TODO: implement this
		return -1;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Given a parent's path's ID, add a new child path. If that path already exists, return
	 * the existing child path ID, rather than adding anything.
	 * @param parentId The ID of the parent path.
	 * @param pathType The type of the path to be added (directory, file, etc)
	 * @param childName The name of the child path.
	 * @return The ID of the child path, or -1 if the path couldn't be added.
	 */
	public int addChildOfPath(int parentId, PathType pathType, String childName) {
		
		/*
		 * Validate that the parent path exists, and that it's TYPE_DIR.
		 */
		if (getPathType(parentId) != PathType.TYPE_DIR){
			return -1;
		}
		
		/* delegate to our helper function, which does the rest of the work */
		return addChildOfPathHelper(parentId, pathType, childName);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Similar to addFile, but return an error if the path doesn't exist, rather than automatically adding it.
	 * @param fullPath
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
			if (childID == -1) {
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
	 * Given a path Id, return a String containing the full path name.
	 * @param pathId The ID of the path to display as a String
	 * @param showRoots True if we should show applicable file system roots in the string, else
	 * show the absolute path.
	 * @return The String representation of the path, in the form /a/b/c/..., possibly
	 * containing a file system root (e.g. root:/a/b/c/...)
	 */
	public String getPathName(int pathId, boolean showRoots) {

		// TODO: handle case where the pathId is invalid.
		StringBuffer sb = new StringBuffer();
		
		/* if we're at the root, simply return /, else recurse */
		if (pathId == 0) {
			if (showRoots) {
				sb.append("root:");
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
	 * Given a path Id, return a String containing the full path name.
	 * @param pathId The ID of the path to display as a String
	 * 
	 * @return The String representation of the path, in the form /a/b/c/...
	 */
	public String getPathName(int pathId) {
		return getPathName(pathId, false);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Fetch the base name of this path. For example, if the path represents "a/b/c/d", then 
	 * return "d". If the pathId is invalid, return null.
	 * @param The pathId of the path to determine the base name of.
	 * @return The path's base name, or null if the pathId is invalid.
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
	 * parent of "/" is itself "/"
	 * @param The pathId of the path to determine the parent of.
	 * @return The path's parent ID, or -1 if the pathId is invalid.
	 */
	public int getParentPath(int pathId) {
		
		Object pathDetails[] = getPathDetails(pathId);
		if (pathDetails == null) {
			return -1;
		}
		return (Integer)pathDetails[0];
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the type of the specified path. This will be one of the values in the PathType
	 * enum, such as TYPE_DIR, TYPE_FILE, or TYPE_SYMLINK.
	 * @param the path ID to query.
	 * @return the type of this path.
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
	 * For a given path, search for the specified child path. Return the child's path ID, 
	 * or -1 if the child isn't present. This method call only makes sense when the parent
	 * is a directory, or a symlink that points to a directory.
	 * @param parentId The parent path under which the child should be found.
	 * @param childName The child's base name.
	 * @return the child's path ID, or -1 if it doesn't exist.
	 */
	public int getChildOfPath(int parentId, String childName)
	{
		Object [] result = getChildOfPathWithType(parentId, childName);
		if (result == null) {
			return -1;
		} else {
			return (Integer)result[0];
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * For a specific parent path, fetch a list of all children of that parent.
	 * @param The ID of the parent path.
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
	 * A variation on the one-argument getChildpathIds method that filters paths based on
	 * whether attributes are set.
	 * TODO: Figure this out better. How should it work for directories?
	 */
	public Integer[] getChildPaths(int pathId, String attrName, String attrValue) {
		return null;
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
	 * specified type (pathType) to the FileNameSpaces. If the path already exists in
	 * the FileNameSpaces object, return the ID of the existing path.
	 * @param pathType The type of the path to be added (TYPE_DIR, TYPE_FILE, TYPE_SYMLINK)
	 * @param fullPathName The full absolute path name to be added
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
			if (childId == -1) {
				return ErrorCode.BAD_PATH;
			}
			
		}
		return parentId;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the given path's parent path ID, and the path's own name.
	 * @return An array of three Objects. The first is the parent's path ID (Integer), the
	 * second is true if this is a directory, else false, the
	 * third is the path's own name (String), and the fourth is the name of the attached
	 * root (if any). Returns null if there's no matching record.
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
	 * opposite of PathType.ordinal.
	 * @param the ordinal value of a PathType value.
	 * @return the corresponding PathType value.
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
	 * A helper method for getPathName. This method is called recursively as we traverse from
	 * the path Id in question, right up to the root path. The recursive step moves up the
	 * path hierarchy until either / is reached or one of the path's is a "root". At that point,
	 * the recursion unwinds back to the start as it appends the path names to the result string.
	 * @param sb The StringBuffer we'll append path component names onto (as we recurse)
	 * @param pathId The ID of the path we're currently looking at (whose name we'll append to sb)
	 * @param showRoots True if we should return a file system root (e.g. "root:") in the path name.
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
			sb.append(rootName);
			sb.append(':');
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
	 * @param the parent path's ID.
	 * @param the name of the child path to search for within this parent.
	 * @return A Object[2] array, where Object[0] is a Integer containing the path ID, and
	 *			Object[1] is a PathType object for the child.
	 */
	private Object[] getChildOfPathWithType(int parentId, String childName) {
		Object result[];
		
		/*
		 * Start by looking in the in-memory cache to see if it's there.
		 */
		FileNameCacheValue cacheValue = fileNameCache.get(parentId, childName);
		if (cacheValue != null) {
			return new Object[] { cacheValue.getChildFileId(), intToPathType(cacheValue.getChildType())};
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
	 * Given a path name of the format "root:/absolute/path/name", split out the root and
	 * path components. If not root component is provided, default to "root". Note: minimal 
	 * error checking is done on the root and path names, so they should be validated by this
	 * method's caller.
	 * @param fullPathName
	 * @return A String[2] array, where element 0 is the root name, and element 1 is the 
	 * path name.
	 */
	private String[] getRootAndPath(String fullPathName) {
		
		/* the default values, in case no root: is provided */
		String rootName = "root";
		String pathName = fullPathName;
		
		/* 
		 * See if there's a colon, if so, split the full path into
		 * "root" and "path name" components.
		 */
		int colonIndex = fullPathName.indexOf(':');
		if (colonIndex != -1){
			rootName = fullPathName.substring(0, colonIndex);
			pathName = fullPathName.substring(colonIndex + 1);
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
	 * @param parentId The ID of the parent path
	 * @param pathType The type of the path to be added (directory, file, etc)
	 * @param childName The name of the child path to add
	 * @return The ID of the child path, or -1 if the path already exists, but was of the wrong type.
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

			return db.getLastRowID();
		}
		
		/* else, it exists, but we need to make sure it's the correct type of path */
		else if ((PathType)childPathAndType[1] != pathType) {
			return -1;
		}
		
		/* else, return the existing child ID */
		else {
			return (Integer)childPathAndType[0];
		}
	}
	
	/*=====================================================================================*/
}
