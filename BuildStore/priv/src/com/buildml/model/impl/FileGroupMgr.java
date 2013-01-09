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
		shiftUpSourcePathsAtPrepStmt = null,
		insertSourcePathAtPrepStmt = null,
		findSourceGroupSizePrepStmt = null,
		findSourcePathAtPrepStmt = null,
		findSourceGroupMembersPrepStmt = null,
		removeSourceGroupPathPrepStmt = null,
		shiftDownSourcePathsAtPrepStmt = null;
		
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
		shiftUpSourcePathsAtPrepStmt = db.prepareStatement(
				"update fileGroupSourcePaths set pos = pos + 1 where groupId = ? and pos >= ?");
		insertSourcePathAtPrepStmt = db.prepareStatement(
				"insert into fileGroupSourcePaths values (?, ?, ?)");
		findSourceGroupSizePrepStmt = db.prepareStatement(
				"select count(*) from fileGroupSourcePaths where groupId = ?");
		findSourcePathAtPrepStmt = db.prepareStatement(
				"select pathId from fileGroupSourcePaths where groupId = ? and pos = ?");
		findSourceGroupMembersPrepStmt = db.prepareStatement(
				"select pathId from fileGroupSourcePaths where groupId = ? order by pos");
		removeSourceGroupPathPrepStmt = db.prepareStatement(
				"delete from fileGroupSourcePaths where groupId = ? and pos = ?");
		shiftDownSourcePathsAtPrepStmt = db.prepareStatement(
				"update fileGroupSourcePaths set pos = pos - 1 where groupId = ? and pos >= ?");
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
	public int addSourcePath(int groupId, int pathId) {
		return addSourcePath(groupId, pathId, -1);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#addSourcePath(int, int, int)
	 */
	@Override
	public int addSourcePath(int groupId, int pathId, int index) {
		
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
		
		addSourcePathHelper(groupId, pathId, index, initialSize);
		return index;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#getSourcePathAt(int, int)
	 */
	@Override
	public int getSourcePathAt(int groupId, int index) {
		
		int size = getGroupSize(groupId);
		if (size == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}

		if ((index < 0) || (index >= size)) {
			return ErrorCode.OUT_OF_RANGE;
		}
		
		return getSourcePathAtHelper(groupId, index);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#addGeneratedPath(int, java.lang.String, boolean)
	 */
	@Override
	public int addGeneratedPath(int groupId, String path, boolean isPermanent) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#addGeneratedPath(int, java.lang.String, boolean, int)
	 */
	@Override
	public int addGeneratedPath(int groupId, String path, boolean isPermanent,
			int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#getGeneratedPathAt(int, int)
	 */
	@Override
	public String getGeneratedPathAt(int groupId, int index) {
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
		int pathId = getSourcePathAtHelper(groupId, fromIndex);
		
		/* delete the old entry */
		removeSourcePathEntry(groupId, fromIndex);
		
		/* insert the same path at the new location */
		addSourcePathHelper(groupId, pathId, toIndex, size);
		
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
		removeSourcePathEntry(groupId, index);
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
			findSourceGroupSizePrepStmt.setInt(1, groupId);
			results = db.executePrepSelectIntegerColumn(findSourceGroupSizePrepStmt);
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
	 * Similar to getSourcePathAt(), but without any error checking.
	 * 
	 * @param groupId	The ID of the group to query.
	 * @param index		The index of the path within the group.
	 * @return			The ID of the path at the specified index in the group.
	 */
	private int getSourcePathAtHelper(int groupId, int index) {
		Integer results[] = null;
		try {
			findSourcePathAtPrepStmt.setInt(1, groupId);
			findSourcePathAtPrepStmt.setInt(2, index);
			results = db.executePrepSelectIntegerColumn(findSourcePathAtPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}

		/* this shouldn't happen (groups should be complete), but just in case... */
		if (results.length == 0) {
			return ErrorCode.NOT_FOUND;
		}

		return results[0];
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Similar to removeEntry, but only for source file groups, and no error checking is done.
	 * 
	 * @param groupId	The ID of the group to remove the entry from.
	 * @param index		The index of the entry to remove.
	 */
	private void removeSourcePathEntry(int groupId, int index) {
		
		/* first, delete the specified entry */
		try {
			removeSourceGroupPathPrepStmt.setInt(1, groupId);
			removeSourceGroupPathPrepStmt.setInt(2, index);
			db.executePrepUpdate(removeSourceGroupPathPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}
		
		/* if necessary, move the higher-numbers entries down one index number */
		try {
			shiftDownSourcePathsAtPrepStmt.setInt(1, groupId);
			shiftDownSourcePathsAtPrepStmt.setInt(2, index);
			db.executePrepUpdate(shiftDownSourcePathsAtPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Similar to addSourcePath, but without any error checking.
	 * 
	 * @param groupId		The ID of the group we're modifying.
	 * @param pathId		The path ID to be added.
	 * @param index			The index at which the path will be added.
	 * @param size			The size of the group.	
	 */
	private void addSourcePathHelper(int groupId, int pathId, int index, int size) {
		
		/* if necessary, move existing entries up one index level */
		if (index < size) {
			try {
				shiftUpSourcePathsAtPrepStmt.setInt(1, groupId);
				shiftUpSourcePathsAtPrepStmt.setInt(2, index);
				db.executePrepUpdate(shiftUpSourcePathsAtPrepStmt);
			} catch (SQLException e) {
				throw new FatalBuildStoreError("Error in SQL: " + e);
			}
		}
		
		/* now insert the new record at the required index */
		try {
			insertSourcePathAtPrepStmt.setInt(1, groupId);
			insertSourcePathAtPrepStmt.setInt(2, pathId);
			insertSourcePathAtPrepStmt.setInt(3, index);
			db.executePrepUpdate(insertSourcePathAtPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}
	}

	/*-------------------------------------------------------------------------------------*/

}
