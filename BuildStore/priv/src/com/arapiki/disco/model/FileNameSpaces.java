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
	 * The PathID of the root of our name space.
	 * TODO: Fix this to support multiple name spaces.
	 */
	private int rootPathId;
	
	/**
	 * Various prepared statement for database access.
	 */
	PreparedStatement findChildPrepStmt = null;
	PreparedStatement insertChildPrepStmt = null;
	PreparedStatement findPathDetailsPrepStmt = null;
	PreparedStatement findpathIdFromParentPrepStmt = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new BuildStoreFileSpace object.
	 * @param db The BuildStoreDB object to use when accessing the underlying database.
	 */
	public FileNameSpaces(BuildStoreDB db) {
		this.db = db;
		rootPathId = 0; // TODO: fix this to handle multiple name spaces.
		
		/* initialize prepared database statements */
		findChildPrepStmt = db.prepareStatement("select id, pathType from files where parentId = ? and name = ?");
		insertChildPrepStmt = db.prepareStatement("insert into files values (null, ?, ?, ?)");
		findPathDetailsPrepStmt = db.prepareStatement("select parentId, pathType, name from files where id=?");
		findpathIdFromParentPrepStmt = db.prepareStatement(
				"select id from files where parentId = ? and name != \"/\" order by name");
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Returns the pathId of the root of a specified namespace. This is equivalent to "/" in a file system,
	 * although in this case we potentially have multiple namespaces, each with their own root.
	 * @return The namespace's root path ID.
	 */
	public int getRootPath(String spaceName) {
		return rootPathId;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Add a new file into the FileNameSpace.
	 * @param fullPathName The full path of the file.
	 * @return the ID of the newly added file, or -1 if the file couldn't be added within
	 * this part of the tree (such as when the parent itself is a file, not a directory).
	 */
	public int addFile(String spaceName, String fullPathName) {
		return addPath(spaceName, PathType.TYPE_FILE, fullPathName);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add a new directory into the FileNameSpace.
	 * @param fullPathName The full path of the directory.
	 * @return the ID of the newly added file, or -1 if the directory couldn't be added within
	 * this part of the tree (such as when the parent itself is a file, not a directory).
	 */
	public int addDirectory(String spaceName, String fullPathName) {
		return addPath(spaceName, PathType.TYPE_DIR, fullPathName);
	}
	
	/*-------------------------------------------------------------------------------------*/

	public int addSymlink(String spaceName, String fullPath) {
		
		// TODO: implement this
		return -1;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Given a parent's path's ID, add a new child path. If that path already exists, return
	 * the existing child path ID, rather than adding anything.
	 * @param parentId The ID of the parent path.
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
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Similar to addFile, but return null if the path doesn't exist, rather than automatically adding it.
	 * @param fullPath
	 * @return
	 */
	public int getPath(String spaceName, String fullPath) {
		String components[] = PathUtils.tokenizePath(fullPath);

		int parentID = getRootPath(spaceName);
		for (int i = 0; i < components.length; i++) {
			
			/* get the next child ID, if it's missing, then the full path is missing */
			int childID = getChildOfPath(parentID, components[i]);
			if (childID == -1) {
				return -1;
			}
			
			/* this component was found - move to the next */
			parentID = childID;
		}
		
		/* all path components exist, so return the last one */
		return parentID;
		
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	public String getPathName(int pathId) {

		// TODO: handle case where the pathId is invalid.
		// TODO: put root name, such as ${root}/a/b/c
		StringBuffer sb = new StringBuffer();
		getPathNameHelper(sb, pathId);
		return sb.toString();
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
			findpathIdFromParentPrepStmt.setInt(1, pathId);
			results = db.executePrepSelectIntegerColumn(findpathIdFromParentPrepStmt);
			
		} catch (SQLException e) {
			new FatalBuildStoreError("Error in SQL: " + e);
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

	// TODO: need an attrName table and an attr table.
	// TODO: void setAttr(int fileID, String attrName, String attrValue)
	// TODO: String getAttr(int fileID, String attrName)
	// TODO: String[] getAttrNames(int fileID) -> return all attr names (but not values).
	
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	private int addPath(String spaceName, PathType pathType, String fullPathName) {
		String components[] = PathUtils.tokenizePath(fullPathName);
		
		int parentId = getRootPath(spaceName);
		
		/* for each path name component, make sure it's added, then move to the next */
		int len = components.length - 1;
		for (int i = 0; i <= len; i++) {
			PathType thisType = (i == len) ? pathType : PathType.TYPE_DIR;
			int childId = addChildOfPath(parentId, thisType, components[i]);
			parentId = childId;
		}
		return parentId;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the given path's parent path ID, and the path's own name.
	 * @return An array of three Objects. The first is the parent's path ID (Integer), the
	 * second is true if this is a directory, else false and the
	 * third is the path's own name (String). Returns null if there's no matching record.
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
			new FatalBuildStoreError("SQL error", e);
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

	private void getPathNameHelper(StringBuffer sb, int pathId) {

		Object pathDetails[] = getPathDetails(pathId);
		int parentID = (Integer)pathDetails[0];
		String name = (String)pathDetails[2];
	
		if (!name.equals("/")){
			getPathNameHelper(sb, parentID);
			sb.append("/");
			sb.append(name);
		}
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
		try {
			findChildPrepStmt.setInt(1, parentId);
			findChildPrepStmt.setString(2, childName);
			ResultSet rs = db.executePrepSelectResultSet(findChildPrepStmt);

			/* if there's a result, return it. */
			if (rs.next()){
				result = new Object[] { Integer.valueOf(rs.getInt(1)), intToPathType(rs.getInt(2))};
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
	
	/*=====================================================================================*/
}
