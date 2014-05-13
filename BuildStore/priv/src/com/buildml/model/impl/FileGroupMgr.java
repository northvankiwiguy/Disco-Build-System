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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileGroupMgr;
import com.buildml.model.IFileGroupMgrListener;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IFileMgr.PathType;
import com.buildml.model.IPackageMemberMgr.MemberDesc;
import com.buildml.model.IPackageMemberMgr.PackageDesc;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageRootMgr;
import com.buildml.utils.errors.ErrorCode;
import com.buildml.utils.regex.BmlRegex;
import com.buildml.utils.regex.RegexChain;

/**
 * An implementation of {@link IFileGroupMgr}.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class FileGroupMgr implements IFileGroupMgr {

	/*=====================================================================================*
	 * NESTED CLASSES
	 *=====================================================================================*/
	
	/**
	 * Represents a single transient member of a file group. A file group has zero or more
	 * entries of this type that are only stored in memory, and are never persisted to the
	 * database.
	 */
	private class TransientEntry {
				
		/** The String path that this entry represents */
		String pathString;
	}
	
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
	
	/** The PackageMemberMgr object associated with this FileGroupMgr */
	private IPackageMemberMgr pkgMemberMgr = null;	
	
	/**
	 * Various prepared statement for database access.
	 */
	private PreparedStatement 
		insertNewGroupPrepStmt = null, 
		findGroupTypePrepStmt = null,
		findGroupPredPrepStmt = null,
		removeGroupPrepStmt = null,
		shiftUpPathsPrepStmt = null,
		insertPathAtPrepStmt = null,
		findGroupSizePrepStmt = null,
		findPathsAtPrepStmt = null,
		findIntegerMembersPrepStmt = null,
		findStringMembersPrepStmt = null,
		findGroupMembersPrepStmt = null,
		removePathPrepStmt = null,
		removePathsPrepStmt = null,
		shiftDownPathsPrepStmt = null,
		insertPackageMemberPrepStmt = null,
		removePackageMemberPrepStmt = null,
		findSourceGroupsContainingPathPrepStmt = null;

	/**
	 * A mapping from group ID to the list of transient path entries.
	 */
	private HashMap<Integer, ArrayList<TransientEntry>> transientEntryMap = null;
	
	/** The event listeners who are registered to learn about file group changes */
	List<IFileGroupMgrListener> listeners = new ArrayList<IFileGroupMgrListener>();
	
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
		findGroupPredPrepStmt = db.prepareStatement(
				"select predId from fileGroups where id = ?");
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
		findIntegerMembersPrepStmt = db.prepareStatement(
				"select pathId from fileGroupPaths where groupId = ? order by pos");
		findStringMembersPrepStmt = db.prepareStatement(
				"select pathString from fileGroupPaths where groupId = ? order by pos");
		findGroupMembersPrepStmt = db.prepareStatement(
				"select pathId, pathString from fileGroupPaths where groupId = ? order by pos");
		removePathPrepStmt = db.prepareStatement(
				"delete from fileGroupPaths where groupId = ? and pos = ?");
		removePathsPrepStmt = db.prepareStatement(
				"delete from fileGroupPaths where groupId = ?");
		shiftDownPathsPrepStmt = db.prepareStatement(
				"update fileGroupPaths set pos = pos - 1 where groupId = ? and pos >= ?");
		insertPackageMemberPrepStmt = 
				db.prepareStatement("insert into packageMembers values (?, ?, ?, ?, -1, -1)");
		removePackageMemberPrepStmt =
				db.prepareStatement("delete from packageMembers where memberId = ? and memberType = " +
						IPackageMemberMgr.TYPE_FILE_GROUP);
		findSourceGroupsContainingPathPrepStmt = db.prepareStatement(
				"select distinct groupId from fileGroups, fileGroupPaths" +
						" where (fileGroups.id = fileGroupPaths.groupId)" +
						" and (fileGroups.type = " + IFileGroupMgr.SOURCE_GROUP + ")" +
						" and (pathId = ?)");
		
		/* initialize the mapping of group IDs to list of transient entries */
		transientEntryMap = new HashMap<Integer, ArrayList<TransientEntry>>();
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

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

		/* all group types provide us with an array of string paths */
		ArrayList<String> outputPaths = new ArrayList<String>();

		/* since merge groups can be recursive, we need a recursion helper */
		if (getExpandedGroupFilesHelper(groupId, outputPaths) != ErrorCode.OK) {
			return null;
		}
		
		/* final results as a String[] */
		return outputPaths.toArray(new String[0]);
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
		
		/* notify listeners about the change */
		notifyListeners(groupId, IFileGroupMgrListener.CHANGED_MEMBERSHIP);
		
		return ErrorCode.OK;
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
		
		/* update the database to remove it from fileGroups and packageMembers tables*/
		try {
			removeGroupPrepStmt.setInt(1, groupId);
			db.executePrepUpdate(removeGroupPrepStmt);
			
			removePackageMemberPrepStmt.setInt(1, groupId);
			db.executePrepUpdate(removePackageMemberPrepStmt);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}
		
		
		return ErrorCode.OK;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#newSourceGroup(int)
	 */
	@Override
	public int newSourceGroup(int pkgId) {
		return newGroup(pkgId, IFileGroupMgr.SOURCE_GROUP, -1);
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
	 * @see com.buildml.model.IFileGroupMgr#getPathIds(int)
	 */
	@Override
	public Integer[] getPathIds(int groupId) {
		
		/* only applicable for merge file groups */
		int groupType = getGroupType(groupId);
		if (groupType != SOURCE_GROUP) {
			return null;
		}
		
		/* defer work to our helper */
		return getIntegerMembersHelper(groupId);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#setPathIds(int, java.lang.Integer[])
	 */
	@Override
	public int setPathIds(int groupId, Integer[] members) {
		
		/* only applicable for source file groups */
		int groupType = getGroupType(groupId);
		if (groupType == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}
		if (groupType != SOURCE_GROUP) {
			return ErrorCode.INVALID_OP;
		}

		setMembersHelper(groupId, members);
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#getSourceGroupsContainingPath(int)
	 */
	@Override
	public Integer[] getSourceGroupsContainingPath(int pathId) {

		Integer results[] = null;
		try {
			findSourceGroupsContainingPathPrepStmt.setInt(1, pathId);
			results = db.executePrepSelectIntegerColumn(findSourceGroupsContainingPathPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}
		return results;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#newMergeGroup(int)
	 */
	@Override
	public int newMergeGroup(int pkgId) {
		return newGroup(pkgId, IFileGroupMgr.MERGE_GROUP, -1);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#addSubGroup(int, int)
	 */
	@Override
	public int addSubGroup(int groupId, int subGroupId) {
		return addSubGroup(groupId, subGroupId, -1);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#addSubGroup(int, int, int)
	 */
	@Override
	public int addSubGroup(int groupId, int subGroupId, int index) {
		
		/* by fetching the size, we check if the group is valid */
		int initialSize = getGroupSize(groupId);
		if (initialSize == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}
		
		/* only applicable for merge file groups */
		if (getGroupType(groupId) != MERGE_GROUP) {
			return ErrorCode.INVALID_OP;
		}
		
		/* validate that the sub group ID is valid */
		if (getGroupSize(subGroupId) == ErrorCode.NOT_FOUND) {
			return ErrorCode.BAD_VALUE;
		}
		
		if (index == -1) {
			index = initialSize;
		} else if ((index < 0) || (index > initialSize)) {
			return ErrorCode.OUT_OF_RANGE;
		}
		
		/*
		 * Check for possibility of cycles.
		 */
		if ((groupId == subGroupId) ||
				checkForCycles(IPackageMemberMgr.TYPE_FILE_GROUP, groupId, subGroupId)) {
			return ErrorCode.LOOP_DETECTED;
		}
		
		addEntryHelper(groupId, subGroupId, null, index, initialSize);
		return index;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#getSubGroupAt(int, int)
	 */
	@Override
	public int getSubGroup(int groupId, int index) {

		int size = getGroupSize(groupId);
		if (size == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}

		/* only applicable for merge file groups */
		if (getGroupType(groupId) != MERGE_GROUP) {
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
	 * @see com.buildml.model.IFileGroupMgr#getSubGroups(int)
	 */
	@Override
	public Integer[] getSubGroups(int groupId) {

		/* only applicable for merge file groups */
		int groupType = getGroupType(groupId);
		if (groupType != MERGE_GROUP) {
			return null;
		}
		
		/* delegate most of the work to a helper */
		return getIntegerMembersHelper(groupId);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#setSubGroups(int, java.lang.Integer[])
	 */
	@Override
	public int setSubGroups(int groupId, Integer[] members) {
		
		/* only applicable for merge file groups */
		int groupType = getGroupType(groupId);
		if (groupType == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}
		if (groupType != MERGE_GROUP) {
			return ErrorCode.INVALID_OP;
		}

		setMembersHelper(groupId, members);
		return ErrorCode.OK;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#newGeneratedGroup(int)
	 */
	@Override
	public int newGeneratedGroup(int pkgId) {
		return newGroup(pkgId, IFileGroupMgr.GENERATED_GROUP, -1);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#newFilterGroup(int, int)
	 */
	@Override
	public int newFilterGroup(int pkgId, int predGroupId) {

		/* check that pkgId refers to a valid package */
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		if (!pkgMgr.isValid(pkgId)) {
			return ErrorCode.NOT_FOUND;
		}
		
		/* validate the predGroupId exists and is already in the pkgId package */
		if (getGroupType(predGroupId) == ErrorCode.NOT_FOUND) {
			return ErrorCode.BAD_VALUE;
		}
		pkgMemberMgr = buildStore.getPackageMemberMgr();
		PackageDesc desc = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE_GROUP, predGroupId);
		if ((desc == null) || (desc.pkgId != pkgId)) {
			return ErrorCode.BAD_VALUE;
		}
		
		/* defer the rest of the work - it's common across group types */
		return newGroup(pkgId, IFileGroupMgr.FILTER_GROUP, predGroupId);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#getPredId(int)
	 */
	@Override
	public int getPredId(int groupId) {

		/* validate that groupId is valid group */
		int groupType = getGroupType(groupId);
		if (groupType == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}
		
		/* only applicable for filter file groups */
		if (groupType != FILTER_GROUP) {
			return ErrorCode.INVALID_OP;
		}
		
		Integer results[] = null;
		try {
			findGroupPredPrepStmt.setInt(1, groupId);
			results = db.executePrepSelectIntegerColumn(findGroupPredPrepStmt);
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
	 * @see com.buildml.model.IFileGroupMgr#addGeneratedPath(int, java.lang.String, boolean)
	 */
	@Override
	public int addPathString(int groupId, String path) {
		return addPathString(groupId, path, -1);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#addGeneratedPath(int, java.lang.String, boolean, int)
	 */
	@Override
	public int addPathString(int groupId, String path, int index) {
		
		/* by fetching the size, we check if the group is valid */
		int initialSize = getGroupSize(groupId);
		if (initialSize == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}
		
		/* only applicable for generated file groups */
		int groupType = getGroupType(groupId);
		if ((groupType != GENERATED_GROUP) && (groupType != FILTER_GROUP)) {
			return ErrorCode.INVALID_OP;
		}
		
		/* validate that the path string is valid */
		if ((groupType == GENERATED_GROUP) && (!isValidPathString(path))) {
			return ErrorCode.BAD_VALUE;
		}
		
		if (index == -1) {
			index = initialSize;
		} else if ((index < 0) || (index > initialSize)) {
			return ErrorCode.OUT_OF_RANGE;
		}
		
		addEntryHelper(groupId, 0, path, index, initialSize);
		return index;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#getGeneratedPathAt(int, int)
	 */
	@Override
	public String getPathString(int groupId, int index) {

		/* only applicable for generated and filter groups */
		int groupType = getGroupType(groupId);
		if ((groupType != GENERATED_GROUP) && (groupType != FILTER_GROUP)) {
			return null;
		}
		
		int size = getGroupSize(groupId);
		if ((index < 0) || (index >= size)) {
			return null;
		}
		
		Object output[] = new Object[2];
		if (getPathsAtHelper(groupId, index, output) == ErrorCode.NOT_FOUND) {
			return null;
		}
		return (String)output[1];
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#getPathStrings(int)
	 */
	@Override
	public String[] getPathStrings(int groupId) {
		
		/* only applicable for generated and filter groups */
		int groupType = getGroupType(groupId);
		if ((groupType != GENERATED_GROUP) && (groupType != FILTER_GROUP)) {
			return null;
		}
		
		/* Defer the work to our helper */
		return getStringMembersHelper(groupId);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#setPathStrings(int, java.lang.String[])
	 */
	@Override
	public int setPathStrings(int groupId, String[] members) {
		
		/* only applicable for generated and filter groups */
		int groupType = getGroupType(groupId);
		if (groupType == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}
		if ((groupType != GENERATED_GROUP) && (groupType != FILTER_GROUP)) {
			return ErrorCode.INVALID_OP;
		}

		if (members == null) {
			return ErrorCode.BAD_VALUE;
		}
		
		/* defer to our helper function */
		setMembersHelper(groupId, members);
		return ErrorCode.OK;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#addTransientPathString(int, java.lang.String)
	 */
	@Override
	public int addTransientPathString(int groupId, String path) {

		/* only generated groups are supported */
		int type = getGroupType(groupId);
		if (type == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}
		if (type != GENERATED_GROUP) {
			return ErrorCode.INVALID_OP;
		}
		
		/* validate that the path string is valid */
		if (!isValidPathString(path)) {
			return ErrorCode.BAD_VALUE;
		}
		
		Integer groupIdInt = Integer.valueOf(groupId);
		
		/* 
		 * Check if there are already transient paths for this group. If not, create
		 * a new entry for this group.
		 */
		ArrayList<TransientEntry> entries = transientEntryMap.get(groupIdInt);
		if (entries == null) {
			entries = new ArrayList<FileGroupMgr.TransientEntry>();
			transientEntryMap.put(groupIdInt, entries);
		}
				
		/* append the new path to the entry list */
		TransientEntry newEntry = new TransientEntry();
		newEntry.pathString = path;
		entries.add(newEntry);
		
		/* notify listeners about the change */
		notifyListeners(groupId, IFileGroupMgrListener.CHANGED_MEMBERSHIP);
		
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#clearTransientPathStrings(int)
	 */
	@Override
	public int clearTransientPathStrings(int groupId) {

		/* only generated groups are supported */
		int type = getGroupType(groupId);
		if (type == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}
		if (type != GENERATED_GROUP) {
			return ErrorCode.INVALID_OP;
		}
		transientEntryMap.remove(Integer.valueOf(groupId));	
		
		/* notify listeners about the change */
		notifyListeners(groupId, IFileGroupMgrListener.CHANGED_MEMBERSHIP);
		
		return ErrorCode.OK;
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

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#addListener(com.buildml.model.IFileGroupMgrListener)
	 */
	@Override
	public void addListener(IFileGroupMgrListener listener) {
		listeners.add(listener);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#removeListener(com.buildml.model.IFileGroupMgrListener)
	 */
	@Override
	public void removeListener(IFileGroupMgrListener listener) {
		listeners.remove(listener);
	};
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Helper method for creating a new group. This is only invoked by public methods
	 * such as newSourceGroup(), newMergeGroup(), etc.
	 * 
	 * @param pkgId   ID of the package to add the group to.
	 * @param type    The type of the new group (SOURCE, GENERATED, etc)
	 * @param optArg1 Some group types contain an additional argument.
	 * @return The new group's ID. 
	 */
	private int newGroup(int pkgId, int type, int optArg1) {
		
		int lastRowId;
		
		/* validate inputs */
		if ((type < IFileGroupMgr.SOURCE_GROUP) ||
			(type > IFileGroupMgr.FILTER_GROUP)) {
			return ErrorCode.BAD_VALUE;
		}
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		if (!pkgMgr.isValid(pkgId)) {
			return ErrorCode.NOT_FOUND;
		}
		
		/* insert the new group into the database, returning the new group ID */
		try {
			insertNewGroupPrepStmt.setInt(1, type);
			insertNewGroupPrepStmt.setInt(2, optArg1);
			db.executePrepUpdate(insertNewGroupPrepStmt);
			
			lastRowId = db.getLastRowID();
			
			/* insert the default package membership values */
			insertPackageMemberPrepStmt.setInt(1, IPackageMemberMgr.TYPE_FILE_GROUP);
			insertPackageMemberPrepStmt.setInt(2, lastRowId);
			insertPackageMemberPrepStmt.setInt(3, pkgId);
			insertPackageMemberPrepStmt.setInt(4, IPackageMemberMgr.SCOPE_NONE);
			if (db.executePrepUpdate(insertPackageMemberPrepStmt) != 1) {
				throw new FatalBuildStoreError("Unable to insert new record into packageMembers table");
			}
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}
		
		return lastRowId; 
	}
	
	/*-------------------------------------------------------------------------------------*/

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
			rs.close();
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
		
		/* notify listeners about the change */
		notifyListeners(groupId, IFileGroupMgrListener.CHANGED_MEMBERSHIP);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether a path string is valid (that is, does it start with a valid root
	 * name).
	 * 
	 * @param path	The path string to validate.
	 * @return True if the path is valid, else false.
	 */
	private boolean isValidPathString(String path) {
		
		/* syntax must be "@<root-name>/.*" where <root-name> must be at least 4 characters */
		if (!path.startsWith("@")) {
			return false;
		}
		int slashIndex = path.indexOf('/');
		if (slashIndex < "@root".length()) {
			return false;
		}
		String rootName = path.substring(1, slashIndex);
		
		IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
		return pkgRootMgr.getRootNative(rootName) != null;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Helper method for getExpandedGroupFiles(). This method handles the recursion that's
	 * possible when merge groups contain sub-groups. It's also possible that merge groups
	 * contain other merge groups.
	 * 
	 * @param groupId		The ID of the top-level group.
	 * @param outputPaths	An input/output list that we'll append output paths to.
	 * @return ErrorCode.OK on success, ErrorCode.NOT_FOUND if a sub group is invalid,
	 *         or ErrorCode.BAD_VALUE if it's a filter group with bad regular expressions.
	 */
	private int getExpandedGroupFilesHelper(int groupId, ArrayList<String> outputPaths) {
		
		/* must be source, generated or merge group */
		int type = getGroupType(groupId);		
		if (type == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}

		/* source and generate groups are simple - just expand their content */
		if ((type == SOURCE_GROUP) || (type == GENERATED_GROUP)) {

			/* fetch the individual members from the database */
			ResultSet rs = null;
			try {
				findGroupMembersPrepStmt.setInt(1, groupId);
				rs = db.executePrepSelectResultSet(findGroupMembersPrepStmt);
				while (rs.next()) {
					if (type == SOURCE_GROUP) {
						int pathId = rs.getInt(1);
						outputPaths.add(fileMgr.getPathName(pathId, true));
					} else {
						outputPaths.add(rs.getString(2));
					}
				}
				rs.close();
				
			} catch (SQLException e) {
				throw new FatalBuildStoreError("Error in SQL: " + e);
			}
		}
		
		/* merge groups involve using recursion to expand children */
		else if (type == MERGE_GROUP) {
			
			int size = getGroupSize(groupId);
			int index = 0;
			while (index != size) {
				int subGroupId = getSubGroup(groupId, index);
				int rc = getExpandedGroupFilesHelper(subGroupId, outputPaths);
				if (rc != ErrorCode.OK) {
					return rc;
				}
				index++;
			}
		}
		
		/* filter groups require us to filter out the files of our upstream file group */
		else if (type == FILTER_GROUP) {
			int predGroupId = getPredId(groupId);
			if (predGroupId < 0) {
				return predGroupId;
			}
			
			/* fetch the input files from the predecessor group - we'll filter from these */
			String inputPaths[] = getExpandedGroupFiles(predGroupId);
			
			/* our own file group contains the regex strings to filter with */
			ResultSet rs = null;
			ArrayList<String> regexs = new ArrayList<String>();
			try {
				findGroupMembersPrepStmt.setInt(1, groupId);
				rs = db.executePrepSelectResultSet(findGroupMembersPrepStmt);
				while (rs.next()) {
					regexs.add(rs.getString(2));
				}
				rs.close();
			} catch (SQLException e) {
				throw new FatalBuildStoreError("Error in SQL: " + e);
			}
			String regexArray[] = regexs.toArray(new String[regexs.size()]);
			
			/* convert the regex strings into a RegexChain (precompiled regexes) */
			RegexChain chain;
			try {
				chain = BmlRegex.compileRegexChain(regexArray);
			} catch (PatternSyntaxException ex) {
				return ErrorCode.BAD_VALUE;
			}
				
			/* finally, filter the inputPaths using our regexes */
			String filteredPaths[] = BmlRegex.filterRegexChain(inputPaths, chain);
			for (int i = 0; i < filteredPaths.length; i++) {
				outputPaths.add(filteredPaths[i]);
			}
		}
		
		/* else, it's an error - unsupported group type */
		else {
			throw new FatalBuildStoreError("Unsupported file group type: " + type);
		}
		
		/* if there are transient entries for this group, append them to the list */
		if (type == GENERATED_GROUP) {
			ArrayList<TransientEntry> entries = transientEntryMap.get(Integer.valueOf(groupId));
			if (entries != null) {
				for (Iterator<TransientEntry> iterator = entries.iterator(); iterator.hasNext();) {
					TransientEntry transientEntry = (TransientEntry) iterator.next();
					outputPaths.add(transientEntry.pathString);
				}
			}
		}
		
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper method for determining whether a merge file group contains (somewhere in the
	 * hierarchy) a second group.
	 * 
	 * @param parentGroupId		The ID of the file group that may contain the child group.
	 * @param childGroupId		The ID of the file group that may be contained by the parent group.
	 * @return True if the parent group contains the child group.
	 */
	private boolean groupContainsGroup(int parentGroupId, int childGroupId) {

		/* recursion termination */
		if (parentGroupId == childGroupId) {
			return true;
		}
		
		/* if parent is a merge group, traverse recursively */
		int parentType = getGroupType(parentGroupId);
		if (parentType != MERGE_GROUP) {
			return false;
		}
		int size = getGroupSize(parentGroupId);
		int index = 0;
		while (index != size) {
			int memberGroup = getSubGroup(parentGroupId, index);
			if (groupContainsGroup(memberGroup, childGroupId)) {
				return true;
			}
			index++;
		}
	
		return false;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Notify any registered listeners about our change in state.
	 * @param fileGroupId   The file group that has changed.
	 * @param how     		The way in which the file group changed (see {@link IFileGroupMgrListener}).
	 */
	private void notifyListeners(int fileGroupId, int how) {
		
		/* 
		 * Make a copy of the listeners list, otherwise a registered listener can't remove
		 * itself from the list within the fileGroupChangeNotification() method.
		 */
		IFileGroupMgrListener listenerCopy[] = 
				listeners.toArray(new IFileGroupMgrListener[listeners.size()]);
		for (int i = 0; i < listenerCopy.length; i++) {
			listenerCopy[i].fileGroupChangeNotification(fileGroupId, how);			
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * If we're modifying the membership of a merge file group,we could potentially be creating
	 * a cycle in the dependency graph. Before allowing this addition, check whether it
	 * would create a cycle. This is a recursive algorithm that searches "to the right" in search
	 * of the file group to be added. For example, if we're inserting fileGroup1 into fileGroup2
	 * (i.e. its being inserted to the left), search downstream (right) of fileGroup2 to see if
	 * we bump into fileGroup1.
	 * 
	 * @param memberType  What are we currently looking at? (IPackageMemberMgr.TYPE_ACTION, etc).
	 * @param memberId	  The ID of the member we're currently looking at (initially the merge
	 * 					  file group).
	 * @param fileGroupId The ID of the file group that's being inserted into the merge file group.
	 * @return True if a cycle would be created, else false.
	 */
	private boolean checkForCycles(int memberType, int memberId, int fileGroupId) {

		pkgMemberMgr = buildStore.getPackageMemberMgr();

		/* get neighbours of this member */
		MemberDesc[] neighbours = pkgMemberMgr.getNeighboursOf(
				memberType, memberId, IPackageMemberMgr.NEIGHBOUR_RIGHT, false);
		for (int i = 0; i < neighbours.length; i++) {
			MemberDesc neighbour = neighbours[i];
			
			/* if we've hit the file group we're searching for - end the search */
			if ((neighbour.memberType == IPackageMemberMgr.TYPE_FILE_GROUP) &&
				(neighbour.memberId == fileGroupId)) {
				return true;
			}
			
			/* not found, recursively search our neighbours */
			if (checkForCycles(neighbour.memberType, neighbour.memberId, fileGroupId)) {
				return true;
			}
			
			/* now loop to the next neighbour */
		}
		
		return false;
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Helper function (supporting getPathIds() and getSubGroups() to return the IDs of all
	 * members within a specific group.
     *
	 * @param groupId The ID of the group to query.
	 * @return A (possibly empty) array of group members.
	 */
	private Integer[] getIntegerMembersHelper(int groupId) {

		try {
			findIntegerMembersPrepStmt.setInt(1, groupId);
			return db.executePrepSelectIntegerColumn(findIntegerMembersPrepStmt);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Helper function (supporting getPathStrings()) to return the Strings within a file group.
     *
	 * @param groupId The ID of the group to query.
	 * @return A (possibly empty) array of group members.
	 */
	private String[] getStringMembersHelper(int groupId) {

		try {
			findStringMembersPrepStmt.setInt(1, groupId);
			return db.executePrepSelectStringColumn(findStringMembersPrepStmt);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Helper function (supporting setPathIds() and setSubGroups()) to set the members of
	 * a group. All existing members will first be removed.
	 * 
	 * @param groupId	The ID of the group (source or merge group that uses numeric member IDs).
	 * @param members	The members to set.
	 */
	private void setMembersHelper(int groupId, Object[] members) {

		/* lots of individual changes here - do them without committing */
		boolean prevState = db.setFastAccessMode(true);
		
		/* start by removing all existing members of this group */
		try {
			removePathsPrepStmt.setInt(1, groupId);
			db.executePrepUpdate(removePathsPrepStmt);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}
		
		/* add all the new members */
		for (int i = 0; i < members.length; i++) {
			try {
				insertPathAtPrepStmt.setInt(1, groupId);
				if (members[i] instanceof Integer) {
					insertPathAtPrepStmt.setInt(2, (Integer)members[i]);
					insertPathAtPrepStmt.setString(3, null);
				} else {
					insertPathAtPrepStmt.setInt(2, 0);
					insertPathAtPrepStmt.setString(3, (String)members[i]);					
				}
				insertPathAtPrepStmt.setInt(4, i);
				db.executePrepUpdate(insertPathAtPrepStmt);
			} catch (SQLException e) {
				throw new FatalBuildStoreError("Error in SQL: " + e);
			}
		}

		/* commit */
		db.setFastAccessMode(prevState);

		/* notify about the change */
		notifyListeners(groupId, IFileGroupMgrListener.CHANGED_MEMBERSHIP);
	}

	/*-------------------------------------------------------------------------------------*/
}
