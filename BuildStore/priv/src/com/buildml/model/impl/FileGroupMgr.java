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
		removeGroupPrepStmt = null;
		
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
		
		/* initialize prepared database statements */
		insertNewGroupPrepStmt = db.prepareStatement("insert into fileGroups values (null, ?, ?)");
		findGroupTypePrepStmt = db.prepareStatement("select type from fileGroups where id = ?");
		findGroupPkgPrepStmt = db.prepareStatement("select pkgId from fileGroups where id = ?");
		updateGroupPkgPrepStmt = db.prepareStatement("update fileGroups set pkgId = ? where id = ?");
		removeGroupPrepStmt = db.prepareStatement("delete from fileGroups where id = ?");
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
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#addSourcePath(int, int, int)
	 */
	@Override
	public int addSourcePath(int groupId, int pathId, int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#getSourcePathAt(int, int)
	 */
	@Override
	public int getSourcePathAt(int groupId, int index) {
		// TODO Auto-generated method stub
		return 0;
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
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#removeEntry(int, int)
	 */
	@Override
	public int removeEntry(int groupId, int index) {
		// TODO Auto-generated method stub
		return 0;
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
		
		// TODO: calculate the size correctly.
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#getExpandedGroupFiles(int)
	 */
	@Override
	public String[] getExpandedGroupFiles(int groupId) {
		// TODO Auto-generated method stub
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#getBuildStore()
	 */
	@Override
	public IBuildStore getBuildStore() {
		return buildStore;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
