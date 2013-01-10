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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileGroupMgr;
import com.buildml.model.IFileMgr;
import com.buildml.model.IFileMgr.PathType;
import com.buildml.model.IPackageMgr;
import com.buildml.utils.errors.ErrorCode;

/**
 * An implementation of {@link IFileGroupMgr}.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class FileGroupMgr implements IFileGroupMgr {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/** The BuildStore that delegates work to this FileGroupMgr */
	private IBuildStore buildStore;
	
	/** This BuildStore's file mgr */
	private IFileMgr fileMgr = null;
	
	/**
	 * Our database manager object, used to access the database content. This is provided 
	 * to us when the FileGroupMgr is first instantiated.
	 */
	private BuildStoreDB db = null;
	
	/**
	 * Various prepared statement for database access.
	 */
	private PreparedStatement 
		insertNewGroupPrepStmt = null, 
		findGroupTypePrepStmt = null,
		findGroupPkgPrepStmt = null,
		updateGroupPkgPrepStmt = null,
		removeGroupPrepStmt = null,
		shiftUpPathsPrepStmt = null,
		insertPathAtPrepStmt = null,
		findGroupSizePrepStmt = null,
		findPathsAtPrepStmt = null,
		findSourceGroupMembersPrepStmt = null,
		removePathPrepStmt = null,
		shiftDownPathsPrepStmt = null;
		
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new FileGroupMgr.
	 * 
	 * @param buildStore The BuildStore that delegates work to this FileGroupMgr.
	 */
	public FileGroupMgr(BuildStore buildStore) {
		this.buildStore = buildStore;
		this.db = buildStore.getBuildStoreDB();
		this.fileMgr = buildStore.getFileMgr();
		
		/* initialize prepared database statements */
		insertNewGroupPrepStmt = db.prepareStatement(
				"insert into fileGroups values (null, ?, ?)");
		findGroupTypePrepStmt = db.prepareStatement(
				"select type from fileGroups where id = ?");
		findGroupPkgPrepStmt = db.prepareStatement(
				"select pkgId from fileGroups where id = ?");
		updateGroupPkgPrepStmt = db.prepareStatement(
				"update fileGroups set pkgId = ? where id = ?");
		removeGroupPrepStmt = db.prepareStatement(
				"delete from fileGroups where id = ?");
		shiftUpPathsPrepStmt = db.prepareStatement(
				"update fileGroupPaths set pos = pos + 1 where groupId = ? and pos >= ?");
		insertPathAtPrepStmt = db.prepareStatement(
				"insert into fileGroupPaths values (?, ?, ?, ?)");
		findGroupSizePrepStmt = db.prepareStatement(
				"select count(*) from fileGroupPaths where groupId = ?");
		findPathsAtPrepStmt = db.prepareStatement(
				"select pathId, pathString from fileGroupPaths where groupId = ? and pos = ?");
		findSourceGroupMembersPrepStmt = db.prepareStatement(
				"select pathId from fileGroupPaths where groupId = ? order by pos");
		removePathPrepStmt = db.prepareStatement(
				"delete from fileGroupPaths where groupId = ? and pos = ?");
		shiftDownPathsPrepStmt = db.prepareStatement(
				"update fileGroupPaths set pos = pos - 1 where groupId = ? and pos >= ?");
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#newSourceGroup(int)
	 */
	@Override
	public int newSourceGroup(int pkgId) {
		return newGroup(pkgId, IFileGroupMgr.SOURCE_GROUP);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#newGeneratedGroup(int)
	 */
	@Override
	public int newGeneratedGroup(int pkgId) {
		return newGroup(pkgId, IFileGroupMgr.GENERATED_GROUP);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#newMergeGroup(int)
	 */
	@Override
	public int newMergeGroup(int pkgId) {
		return newGroup(pkgId, IFileGroupMgr.MERGE_GROUP);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#newGroup(int, int)
	 */
	@Override
	public int newGroup(int pkgId, int type) {
		
		/* validate inputs */
		if ((type < IFileGroupMgr.SOURCE_GROUP) ||
			(type > IFileGroupMgr.MERGE_GROUP)) {
			return ErrorCode.BAD_VALUE;
		}
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		if (!pkgMgr.isValid(pkgId)) {
			return ErrorCode.NOT_FOUND;
		}
		
		/* insert the new group into the database, returning the new group ID */
		try {
			insertNewGroupPrepStmt.setInt(1, pkgId);
			insertNewGroupPrepStmt.setInt(2, type);
			db.executePrepUpdate(insertNewGroupPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}
		
		return db.getLastRowID();
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#getGroupType(int)
	 */
	@Override
	public int getGroupType(int groupId) {
		
		/* fetch the type of this group */
		Integer results[] = null;
		try {
			findGroupTypePrepStmt.setInt(1, groupId);
			results = db.executePrepSelectIntegerColumn(findGroupTypePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}
		
		if (results.length != 0) {
			return results[0];
		}
		
		return ErrorCode.NOT_FOUND;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#removeGroup(int)
	 */
	@Override
	public int removeGroup(int groupId) {

		/* group must exist and be empty */
		int size = getGroupSize(groupId);
		if (size == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}
		if (size != 0) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* update the database */
		try {
			removeGroupPrepStmt.setInt(1, groupId);
			db.executePrepUpdate(removeGroupPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}		
		
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#getGroupPkg(int)
	 */
	@Override
	public int getGroupPkg(int groupId) {
		
		/* fetch the package for this group */
		Integer results[] = null;
		try {
			findGroupPkgPrepStmt.setInt(1, groupId);
			results = db.executePrepSelectIntegerColumn(findGroupPkgPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}
		
		if (results.length != 0) {
			return results[0];
		}
		
		return ErrorCode.NOT_FOUND;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#setGroupPkg(int, int)
	 */
	@Override
	public int setGroupPkg(int groupId, int pkgId) {

		/* validate pkgId */
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		if (!pkgMgr.isValid(pkgId)) {
			return ErrorCode.BAD_VALUE;
		}
		
		/* update the database */
		int rowCount = 0;
		try {
			updateGroupPkgPrepStmt.setInt(1, pkgId);
			updateGroupPkgPrepStmt.setInt(2, groupId);
			rowCount = db.executePrepUpdate(updateGroupPkgPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}

		/* validate groupId - if no update happened, assume bad groupId */
		if (rowCount == 0) {
			return ErrorCode.NOT_FOUND;
		}
		
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#addSourcePath(int, int)
	 */
	@Override
	public int addPathId(int groupId, int pathId) {
		return addPathId(groupId, pathId, -1);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#addSourcePath(int, int, int)
	 */
	@Override
	public int addPathId(int groupId, int pathId, int index) {
		
		/* by fetching the size, we check if the group is valid */
		int initialSize = getGroupSize(groupId);
		if (initialSize == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}
		
		/* only applicable for source file groups */
		if (getGroupType(groupId) != SOURCE_GROUP) {
			return ErrorCode.INVALID_OP;
		}
		
		/* validate that the pathId is valid */
		if (fileMgr.getPathType(pathId) == PathType.TYPE_INVALID) {
			return ErrorCode.BAD_VALUE;
		}
		
		if (index == -1) {
			index = initialSize;
		} else if ((index < 0) || (index > initialSize)) {
			return ErrorCode.OUT_OF_RANGE;
		}
		
		addEntryHelper(groupId, pathId, null, index, initialSize);
		return index;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#getSourcePathAt(int, int)
	 */
	@Override
	public int getPathId(int groupId, int index) {
		
		int size = getGroupSize(groupId);
		if (size == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}

		/* only applicable for source file groups */
		if (getGroupType(groupId) != SOURCE_GROUP) {
			return ErrorCode.INVALID_OP;
		}
		
		if ((index < 0) || (index >= size)) {
			return ErrorCode.OUT_OF_RANGE;
		}
		
		Object output[] = new Object[2];
		if (getPathsAtHelper(groupId, index, output) == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}
		return (Integer)output[0];
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#addGeneratedPath(int, java.lang.String, boolean)
	 */
	@Override
	public int addPathString(int groupId, String path, boolean isPermanent) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#addGeneratedPath(int, java.lang.String, boolean, int)
	 */
	@Override
	public int addPathString(int groupId, String path, boolean isPermanent,
			int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#getGeneratedPathAt(int, int)
	 */
	@Override
	public String getPathString(int groupId, int index) {
		// TODO Auto-generated method stub
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#addSubGroup(int, int)
	 */
	@Override
	public int addSubGroup(int groupId, int subGroupId) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#addSubGroup(int, int, int)
	 */
	@Override
	public int addSubGroup(int groupId, int subGroupId, int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#getSubGroupAt(int, int)
	 */
	@Override
	public int getSubGroup(int groupId, int index) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#moveEntry(int, int, int)
	 */
	@Override
	public int moveEntry(int groupId, int fromIndex, int toIndex) {

		/* validate inputs */
		int size = getGroupSize(groupId);
		if (size == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}
		if ((fromIndex < 0) || (fromIndex >= size) || (toIndex < 0) || (toIndex >= size)) {
			return ErrorCode.OUT_OF_RANGE;
		}
		
		/* moving to the same position is pointless */
		if (fromIndex == toIndex) {
			return ErrorCode.OK;
		}
		
		/* determine the ID of the path we're moving */
		Object output[] = new Object[2];
		if (getPathsAtHelper(groupId, fromIndex, output) == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}
		int pathId = (Integer)output[0];
		String pathString = (String)output[1];
		
		/* delete the old entry */
		removeEntryHelper(groupId, fromIndex);
		
		/* insert the same path at the new location */
		addEntryHelper(groupId, pathId, pathString, toIndex, size);
		
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#removeEntry(int, int)
	 */
	@Override
	public int removeEntry(int groupId, int index) {

		/* validate inputs */
		int size = getGroupSize(groupId);
		if (size == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}
		if ((index < 0) || (index >= size)) {
			return ErrorCode.OUT_OF_RANGE;
		}
		
		/* update the database */
		removeEntryHelper(groupId, index);
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#getGroupSize(int)
	 */
	@Override
	public int getGroupSize(int groupId) {
		
		int type = getGroupType(groupId);
		if (type == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}

		Integer results[] = null;
		try {
			findGroupSizePrepStmt.setInt(1, groupId);
			results = db.executePrepSelectIntegerColumn(findGroupSizePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}
	
		return results[0];
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#getExpandedGroupFiles(int)
	 */
	@Override
	public String[] getExpandedGroupFiles(int groupId) {

		int size = getGroupSize(groupId);
		if (size == ErrorCode.NOT_FOUND) {
			return null;
		}
	
		/* fetch the members from the database */
		Integer results[] = null;
		try {
			findSourceGroupMembersPrepStmt.setInt(1, groupId);
			results = db.executePrepSelectIntegerColumn(findSourceGroupMembersPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}
		
		/* convert from path IDs to path strings (in @root/path) format */
		String paths[] = new String[size];
		for (int i = 0; i < results.length; i++) {
			int pathId = results[i];
			paths[i] = fileMgr.getPathName(pathId, true);
		}
		return paths;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#getBuildStore()
	 */
	@Override
	public IBuildStore getBuildStore() {
		return buildStore;
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Fetch the pathId and pathString values for a specific (groupId/index) combination.
	 * No error checking is done, so the caller must have first validated all inputs.
	 * 
	 * @param groupId	The ID of the group to query.
	 * @param index		The index of the path within the group.
	 * @param output    An array of two elements, for returning the results. On return from
	 * 					this method (with a ErrorCode.OK return code), output[0] will 
	 * 					contain the Integer pathId, and output[1] will contain the String
	 * 					path string (possibly null). If the return code is not ErrorCode.OK,
	 * 					this array is untouched.
	 * 					
	 * @return			ErrorCode.OK on success, or ErrorCode.NOT_FOUND if the 
	 */
	private int getPathsAtHelper(int groupId, int index, Object output[]) {
		
		ResultSet rs = null;
		try {
			findPathsAtPrepStmt.setInt(1, groupId);
			findPathsAtPrepStmt.setInt(2, index);
			rs = db.executePrepSelectResultSet(findPathsAtPrepStmt);
			
			/* this shouldn't happen (groups should be complete), but just in case... */
			if (!rs.next()) {
				return ErrorCode.NOT_FOUND;
			}
			output[0] = rs.getInt(1);
			output[1] = rs.getString(2);
			return ErrorCode.OK;
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Removed a path entry from the specified group index. There is no error checking done
	 * by this method, so the caller must take care to validate inputs.
	 * 
	 * @param groupId	The ID of the group to remove the entry from.
	 * @param index		The index of the entry to remove.
	 */
	private void removeEntryHelper(int groupId, int index) {
		
		/* first, delete the specified entry */
		try {
			removePathPrepStmt.setInt(1, groupId);
			removePathPrepStmt.setInt(2, index);
			db.executePrepUpdate(removePathPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}
		
		/* if necessary, move the higher-numbers entries down one index number */
		try {
			shiftDownPathsPrepStmt.setInt(1, groupId);
			shiftDownPathsPrepStmt.setInt(2, index);
			db.executePrepUpdate(shiftDownPathsPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add a new path entry (with pathId and pathString), at the specified index. There
	 * is no error checking done by this method, so the caller must have already validated
	 * inputs.
	 * 
	 * @param groupId		The ID of the group we're modifying.
	 * @param pathId		The path ID to be added.
	 * @param pathString	The path string to be added (can be null).
	 * @param index			The index at which the path will be added.
	 * @param size			The size of the group.	
	 */
	private void addEntryHelper(int groupId, int pathId, String pathString, int index, int size) {
		
		/* if necessary, move existing entries up one index level */
		if (index < size) {
			try {
				shiftUpPathsPrepStmt.setInt(1, groupId);
				shiftUpPathsPrepStmt.setInt(2, index);
				db.executePrepUpdate(shiftUpPathsPrepStmt);
			} catch (SQLException e) {
				throw new FatalBuildStoreError("Error in SQL: " + e);
			}
		}
		
		/* now insert the new record at the required index */
		try {
			insertPathAtPrepStmt.setInt(1, groupId);
			insertPathAtPrepStmt.setInt(2, pathId);
			insertPathAtPrepStmt.setString(3, pathString);
			insertPathAtPrepStmt.setInt(4, index);
			db.executePrepUpdate(insertPathAtPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}
	}

	/*-------------------------------------------------------------------------------------*/

}
