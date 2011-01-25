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
import java.util.Hashtable;
import java.util.List;

import org.hamcrest.core.IsInstanceOf;

import com.arapiki.utils.string.PathUtils;


/**
 * A helper class to manage the file tree structures within a BuildStore.
 * The class encapsulates everything that's known about the content of the
 * file system during the build process (although nothing about the relationship
 * between files, or the commands used to create them).
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class FileNameSpace {
	
	/**
	 * Our database manager, used to access the database content. This is provided 
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
	private int rootPathID;
	
	/**
	 * Various prepared statement for database access.
	 */
	PreparedStatement findChildPrepStmt = null;
	PreparedStatement insertChildPrepStmt = null;
	PreparedStatement findNameAndParentPrepStmt = null;
	PreparedStatement findPathIdFromParentPrepStmt = null;
	
	/*-------------------------------------------------------------------------------------*/
	/**
	 * Create a new BuildStoreFileSpace object.
	 * @param db The BuildStoreDB object to use when accessing the underlying database.
	 */
	public FileNameSpace(BuildStoreDB db) {
		this.db = db;
		rootPathID = 0; // TODO: fix this to handle multiple name spaces.
		
		/* initialize prepared database statements */
		findChildPrepStmt = db.prepareStatement("select id from files where parentID = ? and name = ?");
		insertChildPrepStmt = db.prepareStatement("insert into files values (null, ?, ?)");
		findNameAndParentPrepStmt = db.prepareStatement("select parentID, name from files where id=?");
		findPathIdFromParentPrepStmt = db.prepareStatement(
				"select id from files where parentID = ? and name != \"/\" order by name");
	}
	
	/*-------------------------------------------------------------------------------------*/
	/**
	 * Returns the PathID of the root of a specified namespace. This is equivalent to "/" in a file system,
	 * although in this case we potentially have multiple namespaces, each with their own root.
	 * @return The namespace's root ID.
	 */
	public int getRootPath(String spaceName) {
		return rootPathID;
	}
	
	/*-------------------------------------------------------------------------------------*/
	/**
	 * Add a new file into the database.
	 * @param fullPath The full path of the file.
	 */
	public int addFullPath(String spaceName, String fullPath) {
		String components[] = PathUtils.tokenizePath(fullPath);
		
		int parentID = getRootPath(spaceName);
		
		/* for each path name component, make sure it's added, then move to the next */
		for (int i = 0; i < components.length; i++) {
			int childID = addChildUnderPath(parentID, components[i]);
			parentID = childID;
		}
		return parentID;
	}
	
	/*-------------------------------------------------------------------------------------*/
	/**
	 * Similar to addFullPath, but return null if the path doesn't exist, rather than automatically adding it.
	 * @param fullPath
	 * @return
	 */
	public int lookupFullPath(String spaceName, String fullPath) {
		String components[] = PathUtils.tokenizePath(fullPath);

		int parentID = getRootPath(spaceName);
		for (int i = 0; i < components.length; i++) {
			
			/* get the next child ID, if it's missing, then the full path is missing */
			int childID = getChildUnderPath(parentID, components[i]);
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
	/**
	 * For a given parent, search for the specific child path underneath
	 * that parent. Return the child's path ID, or -1 if the child
	 * isn't present.
	 * @param parentID The parent ID under which the child might exist.
	 * @param childName The child's path name.
	 * @return the child's path ID, or -1 if it doesn't exist.
	 */
	public int getChildUnderPath(int parentID, String childName)
	{
		String results[];
		try {
			findChildPrepStmt.setInt(1, parentID);
			findChildPrepStmt.setString(2, childName);
			results = db.executePrepSelectColumn(findChildPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("SQL problem in prepared statement", e);
		}
		
		/* if it doesn't exist */
		if (results.length == 0) {
			return -1;
		}
		
		/* if it does exist, return the child ID */
		else if (results.length == 1) {
			return Integer.valueOf(results[0]);
		}
		
		/* else, there are multiple results - this is an error */
		else {
			throw new FatalBuildStoreError("Database 'files' table has multiple entries for (" + 
					parentID + ", " + childName + ")");
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
	/**
	 * Given a parent's path's ID, add a new child path. If that path already exists, return
	 * the existing child path ID, rather than adding anything.
	 * @param parentID The ID of the parent path.
	 * @param childName The name of the child path.
	 * @return The idea of the child path.
	 */
	public int addChildUnderPath(int parentID, String childName) {
		
		int childID = getChildUnderPath(parentID, childName);
		
		/* If child isn't yet present, we need to add it */
		if (childID == -1) {
			/*
			 *  TODO: fix the race condition here - there's a small chance that somebody
			 *  else has already added it.
			 */
			
			try {
				insertChildPrepStmt.setInt(1, parentID);
				insertChildPrepStmt.setString(2, childName);
				db.executePrepUpdate(insertChildPrepStmt);
			} catch (SQLException e) {
				throw new FatalBuildStoreError("Unable to execute SQL statement", e);
			}

			return db.getLastRowID();
		}
		
		/* else, return the existing child ID */
		else {
			return childID;
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the given path's parent path ID, and the path's own name.
	 * @return An array of two Objects. The first is the parent's path ID (Integer), and the
	 * second is the path's own name (String). Returns null if there's no matching record.
	 */
	private Object[] getParentAndName(int pathID) {
		Object result[] = new Object[2];
		
		try {
			findNameAndParentPrepStmt.setInt(1, pathID);
			ResultSet rs = db.executePrepSelectResultSet(findNameAndParentPrepStmt);
			if (rs.next()){
				result[0] = rs.getInt(1);
				result[1] = rs.getString(2);
				rs.close();
			} else {
				return null;
			}
			
		} catch (SQLException e) {
			new FatalBuildStoreError("SQL error", e);
		}
				
		return result;
	}
	
	
	/*-------------------------------------------------------------------------------------*/

	private void getFullPathNameHelper(StringBuffer sb, int pathID) {

		Object parentAndName[] = getParentAndName(pathID);
		int parentID = (Integer)parentAndName[0];
		String name = (String)parentAndName[1];
	
		if (!name.equals("/")){
			getFullPathNameHelper(sb, parentID);
			sb.append("/");
			sb.append(name);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	public String getFullPathName(int pathID) {

		// TODO: handle case where the pathID is invalid.
		StringBuffer sb = new StringBuffer();
		getFullPathNameHelper(sb, pathID);
		return sb.toString();
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	public String getBasePathName(int pathID) {
		
		// TODO: handle case where the pathID is invalid.
		Object parentAndName[] = getParentAndName(pathID);
		return (String)parentAndName[1];
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	public int getParentPathID(int pathID) {
		
		// TODO: handle case where the pathID is invalid.
		Object parentAndName[] = getParentAndName(pathID);
		return (Integer)parentAndName[0];
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	public Integer[] getChildPathIDs(int pathID) {
		
		/*
		 * Fetch the records from the file table where the parentID is
		 * set to 'pathID'. The records will be returned in alphabetical
		 * order.
		 * 
		 * The corner case (built into the prepared statement
		 * query) is that / (the root) is always a child of itself, although
		 * we don't want to report it. The query already excludes /.
		 */
		String stringResults[] = null;
		try {
			findPathIdFromParentPrepStmt.setInt(1, pathID);
			stringResults = db.executePrepSelectColumn(findPathIdFromParentPrepStmt);
			
		} catch (SQLException e) {
			new FatalBuildStoreError("Error in SQL: " + e);
		}
		
		/* if there are no results, return an empty array */
		if (stringResults == null) {
			return new Integer[0];
		}
		
		Integer results[] = new Integer[stringResults.length];
		for (int i = 0; i < stringResults.length; i++) {
			results[i] = Integer.valueOf(stringResults[i]);
		}
		
		return results;
	}
}
