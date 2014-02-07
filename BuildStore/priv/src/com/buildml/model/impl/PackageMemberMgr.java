/*******************************************************************************
 * Copyright (c) 2011 Arapiki Solutions Inc.
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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileGroupMgr;
import com.buildml.model.IFileMgr;
import com.buildml.model.IFileMgr.PathType;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMemberMgrListener;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageRootMgr;
import com.buildml.model.ISlotTypes;
import com.buildml.model.ISubPackageMgr;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.ActionSet;
import com.buildml.utils.errors.ErrorCode;

/**
 * A manager class (that supports the BuildStore class) responsible for managing all 
 * BuildStore information pertaining to the membership of packages.
 * <p>
 * There should be exactly one PackageMemberMgr object per BuildStore object. Use the
 * BuildStore's getPackageMemberMgr() method to obtain that one instance.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
/* package private */ class PackageMemberMgr implements IPackageMemberMgr {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The BuildStore that owns this package manager */
	private IBuildStore buildStore;
	
	/**
	 * Our database manager object, used to access the database content. This is provided 
	 * to us when the Packages object is first instantiated.
	 */
	private BuildStoreDB db = null;
	
	/** The FileMgr object that manages the files in our packages. */
	private IFileMgr fileMgr = null;
	
	/** The ActionMgr object that manages the actions in our packages. */
	private IActionMgr actionMgr = null;
	
	/** The PackageMgr object that manages the list of packages. */
	private IPackageMgr pkgMgr = null;
	
	/** The SubPackageMgr object that manages the list of packages. */
	private ISubPackageMgr subPkgMgr = null;

	/** The FileGroupMgr object that manages the file groups in our package. */
	private IFileGroupMgr fileGroupMgr = null;	
	
	/**
	 * Various prepared statements for database access.
	 */
	private PreparedStatement 
		findMemberPackagePrepStmt = null,
		updatePackagePrepStmt = null,
		findFilesInPackage1PrepStmt = null,
		findFilesInPackage2PrepStmt = null,
		findFilesOutsidePackage1PrepStmt = null,
		findFilesOutsidePackage2PrepStmt = null,		
		findActionPackagePrepStmt = null,
		findActionsInPackagePrepStmt = null,
		findActionsOutsidePackagePrepStmt = null,
		findLocationPrepStmt = null,
		updateLocationPrepStmt = null,
		findActionNeighbourPrepStmt = null,
		findFileGroupNeighbourPrepStmt = null,
		findMergeFileGroupMembersPrepStmt = null,
		findMergeFileGroupsContainingFileGroup = null,
		findFiltersContainingFileGroup = null;
	
	/** The event listeners who are registered to learn about package membership changes */
	List<IPackageMemberMgrListener> listeners = new ArrayList<IPackageMemberMgrListener>();
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new Packages object, which represents the file/action packages that
	 * are part of the BuildStore.
	 * 
	 * @param buildStore The BuildStore that this Packages object belongs to.
	 */
	public PackageMemberMgr(BuildStore buildStore) {
		this.buildStore = buildStore;
		this.db = buildStore.getBuildStoreDB();
		this.fileMgr = buildStore.getFileMgr();
		this.actionMgr = buildStore.getActionMgr();
		this.pkgMgr = buildStore.getPackageMgr();
		this.fileGroupMgr = buildStore.getFileGroupMgr();
		this.subPkgMgr = buildStore.getSubPackageMgr();
		
		findMemberPackagePrepStmt = db.prepareStatement(
				"select pkgId, scopeId from packageMembers where memberType = ? and memberId = ?");
		updatePackagePrepStmt = 
				db.prepareStatement("update packageMembers set pkgId = ?, scopeId = ? " +
									"where memberType = ? and memberId = ?");				
		findFilesInPackage1PrepStmt = db.prepareStatement(
				"select memberId from packageMembers where pkgId = ? and memberType = " + TYPE_FILE);
		findFilesInPackage2PrepStmt = db.prepareStatement(
				"select memberId from packageMembers where pkgId = ? and memberType = " + 
						TYPE_FILE + " and scopeId = ?");
		findFilesOutsidePackage1PrepStmt = db.prepareStatement(
				"select memberId from packageMembers where pkgId != ? and memberType = " + TYPE_FILE);
		findFilesOutsidePackage2PrepStmt = db.prepareStatement(
				"select memberId from packageMembers where memberType = " + TYPE_FILE +
				" and not (pkgId = ? and scopeId = ?)");
		findActionPackagePrepStmt = db.prepareStatement("select pkgId from packageMembers where memberId = ?" +
				" and memberType = " + TYPE_ACTION);
		findActionsInPackagePrepStmt = db.prepareStatement(
				"select memberId from packageMembers where pkgId = ? and memberType = " + TYPE_ACTION);
		findActionsOutsidePackagePrepStmt = db.prepareStatement("select memberId from packageMembers " +
				"where pkgId != ? and memberId != 0 and memberType = " + TYPE_ACTION);
		findLocationPrepStmt = db.prepareStatement(
				"select x, y from packageMembers where memberType = ? and memberId = ?");
		updateLocationPrepStmt = db.prepareStatement(
				"update packageMembers set x = ?, y = ? where memberType = ? and memberId = ?");
		findActionNeighbourPrepStmt = db.prepareStatement(
				"select value, x, y from slotValues, slotTypes, packageMembers where " +
						"slotValues.ownerType = " + ISlotTypes.SLOT_OWNER_ACTION + " " + 
						"and slotValues.ownerId = ? " +
						"and slotValues.slotId = slotTypes.slotId " +
						"and slotTypes.slotPos = ? " +
						"and packageMembers.memberType = " + IPackageMemberMgr.TYPE_FILE_GROUP + " " +
						"and packageMembers.memberId = value");
		findFileGroupNeighbourPrepStmt = db.prepareStatement(
				"select slotValues.ownerId, x, y from slotValues, slotTypes, packageMembers where " +
						"slotValues.ownerType = " + ISlotTypes.SLOT_OWNER_ACTION + " " +
						"and slotValues.value = ? " +
						"and slotValues.slotId = slotTypes.slotId " +
						"and (slotTypes.slotPos = ? or slotTypes.slotPos = ?) " +
						"and packageMembers.memberType = " + IPackageMemberMgr.TYPE_ACTION + " " +
						"and packageMembers.memberId = slotValues.ownerId");
		findMergeFileGroupMembersPrepStmt = db.prepareStatement(
				"select distinct pathId, x, y from fileGroupPaths, packageMembers where groupId = ? " +
						"and packageMembers.memberType = " + IPackageMemberMgr.TYPE_FILE_GROUP + " " +
						"and packageMembers.memberId = fileGroupPaths.pathId");
		findMergeFileGroupsContainingFileGroup = db.prepareStatement(
				"select distinct groupId, x, y from fileGroups, fileGroupPaths, packageMembers " +
						"where packageMembers.memberType = " + IPackageMemberMgr.TYPE_FILE_GROUP + " " +
						"and packageMembers.memberId = fileGroupPaths.groupId " +
						"and fileGroups.id = fileGroupPaths.groupId " +
						"and fileGroups.type = " + IFileGroupMgr.MERGE_GROUP + " " +
						"and fileGroupPaths.pathId = ?");
		findFiltersContainingFileGroup = db.prepareStatement(
				"select id from fileGroups where predId = ?");
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#getScopeName(int)
	 */
	@Override
	public String getScopeName(int id) {
		
		/* the names are a static mapping, so no need for database look-ups */
		switch (id) {
		case SCOPE_NONE:
			return "None";
		case SCOPE_PRIVATE:
			return "Private";
		case SCOPE_PUBLIC:
			return "Public";
		default:
			return null;
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#getScopeId(java.lang.String)
	 */
	@Override
	public int getScopeId(String name) {
		
		/* the mapping is static, so no need for a database look up */
		if (name.equalsIgnoreCase("None")) {
			return SCOPE_NONE;
		}
		if (name.equalsIgnoreCase("priv") || name.equalsIgnoreCase("private")) {
			return SCOPE_PRIVATE;
		}
		if (name.equalsIgnoreCase("pub") || name.equalsIgnoreCase("public")) {
			return SCOPE_PUBLIC;
		}
		return ErrorCode.NOT_FOUND;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#parsePkgSpec(java.lang.String)
	 */
	@Override
	public PackageDesc parsePkgSpec(String pkgSpec) {

		/* parse the pkgSpec to separate it into "pkg" and "scope" portions */
		String pkgName = pkgSpec;
		String scopeName = null;

		/* check if there's a '/' in the string, to separate "package" from "scope" */
		int slashIndex = pkgSpec.indexOf('/');
		if (slashIndex != -1) {
			pkgName = pkgSpec.substring(0, slashIndex);
			scopeName = pkgSpec.substring(slashIndex + 1);
		} 

		/* 
		 * Convert the package's name into it's internal ID. If there's an error,
		 * we simply pass it back to our own caller.
		 */
		int pkgId = pkgMgr.getId(pkgName);

		/* if the user provided a /scope portion, convert that to an ID too */
		int scopeId = 0;
		if (scopeName != null) {
			scopeId = getScopeId(scopeName);
		}
		
		PackageDesc result = new PackageDesc();
		result.pkgId = pkgId;
		result.pkgScopeId = scopeId;
		return result;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#setPackageOfMember(int, int, int, int)
	 */
	@Override
	public int setPackageOfMember(int memberType, int memberId, int pkgId, int pkgScopeId) {

		/* we can't assign files into folders (only into packages) */
		if (!pkgMgr.isValid(pkgId) || pkgMgr.isFolder(pkgId)) {
			return ErrorCode.BAD_VALUE;
		}
		
		/* TODO: check pkgScopeId is a valid scope - else return BAD_VALUE */

		/* determine which package the member is currently in - if no change, we're done */
		PackageDesc oldPkg = getPackageOfMember(memberType, memberId);
		if (oldPkg == null) {
			return ErrorCode.NOT_FOUND;
		}
		if ((oldPkg.pkgId == pkgId) && (oldPkg.pkgScopeId == pkgScopeId)) {
			return ErrorCode.OK;
		}	
				
		/*
		 * Perform MEMBER_TYPE_FILE-specific validation checks.
		 */
		if (memberType == TYPE_FILE) {
			/* the path must be valid and not-trashed */
			if ((fileMgr.getPathType(memberId) == PathType.TYPE_INVALID) ||
					(fileMgr.isPathTrashed(memberId))) {
				return ErrorCode.NOT_FOUND;
			}	
			
			/* 
			 * Check that the path falls under the package's root (except for <import> which
			 * doesn't have root restrictions).
			 */
			if (pkgId != pkgMgr.getImportPackage()) {
				IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
				int pkgRootPathId = pkgRootMgr.getPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT);
				if (pkgRootPathId == ErrorCode.NOT_FOUND) {
					return ErrorCode.NOT_FOUND;
				}
				if ((pkgRootPathId != memberId) && !fileMgr.isAncestorOf(pkgRootPathId, memberId)) {
					return ErrorCode.OUT_OF_RANGE;
				}
			}
		}
			
		/* update the database table with the new pkgId/pkgScopeId */
		try {
			updatePackagePrepStmt.setInt(1, pkgId);
			updatePackagePrepStmt.setInt(2, pkgScopeId);
			updatePackagePrepStmt.setInt(3, memberType);
			updatePackagePrepStmt.setInt(4, memberId);
			int rowCount = db.executePrepUpdate(updatePackagePrepStmt);
			if (rowCount == 0) {
				return ErrorCode.NOT_FOUND;
			}
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}		
		

		/* 
		 * Notify listeners about the change in package content.
		 */
		notifyListeners(oldPkg.pkgId, IPackageMemberMgrListener.CHANGED_MEMBERSHIP, memberType, memberId);
		if (oldPkg.pkgId != pkgId) {
			notifyListeners(pkgId, IPackageMemberMgrListener.CHANGED_MEMBERSHIP, memberType, memberId);
		}
		
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#setPackageOfMember(int, int, int)
	 */
	@Override
	public int setPackageOfMember(int memberType, int memberId, int pkgId) {
		return setPackageOfMember(memberType, memberId, pkgId, IPackageMemberMgr.SCOPE_NONE);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#getPackageOfMember(int, int)
	 */
	@Override
	public PackageDesc getPackageOfMember(int memberType, int memberId) {
		
		try {
			findMemberPackagePrepStmt.setInt(1, memberType);
			findMemberPackagePrepStmt.setInt(2, memberId);
			ResultSet rs = db.executePrepSelectResultSet(findMemberPackagePrepStmt);
			if (!rs.next()){
				return null;
			}
			PackageDesc result = new PackageDesc();
			result.pkgId = rs.getInt(1);
			result.pkgScopeId = rs.getInt(2);
			rs.close();
			return result;
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#getMembersInPackage(int, int, int)
	 */
	@Override
	public MemberDesc[] getMembersInPackage(int pkgId, int pkgScopeId,
			int memberTypeFilter) {
		
		List<MemberDesc> members;
		
		/*
		 * There are multiple cases to handle in this query, depending on whether pkgScopeId
		 * is defined (versus being SCOPE_NONE) and whether memberTypeFilter is defined
		 * (versus being MEMBER_TYPE_ANY).
		 */
		String query = "select memberType, memberId, x, y from packageMembers where pkgId = " + pkgId;
		if (pkgScopeId != SCOPE_NONE) {
			query += " and scopeId = " + pkgScopeId;
		}
		if (memberTypeFilter != TYPE_ANY) {
			query += " and memberType = " + memberTypeFilter;
		}
		
		/* query the database to find the relevant package members */
		try {
			ResultSet rs = db.executeSelectResultSet(query);
			if (!rs.next()){
				return new MemberDesc[0];
			}
			
			/* copy results into a MemberDesc[] */
			members = new ArrayList<IPackageMemberMgr.MemberDesc>();
			do {
				MemberDesc newMember = new MemberDesc(
						rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4));
				
				/* don't show trashed actions */
				boolean isTrashed = false;
				if ((newMember.memberType == IPackageMemberMgr.TYPE_ACTION) &&
						actionMgr.isActionTrashed(newMember.memberId)) {
					isTrashed = true;
				}
				else if ((newMember.memberType == IPackageMemberMgr.TYPE_SUB_PACKAGE) &&
						subPkgMgr.isSubPackageTrashed(newMember.memberId)) {
					isTrashed = true;
				}
				
				if (!isTrashed){
					members.add(newMember);
				}
			} while (rs.next());
			
			rs.close();
			return members.toArray(new MemberDesc[members.size()]);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#setMemberLocation(int, int, int, int)
	 */
	@Override
	public int setMemberLocation(int memberType, int memberId, int x, int y) {

		/* If the actionId is invalid, or the (x, y) is unchanged, do nothing */
		MemberLocation currentLocation = getMemberLocation(memberType, memberId);
		if (currentLocation == null) {
			return ErrorCode.BAD_VALUE;
		}
		if ((x == currentLocation.x) && (y == currentLocation.y)) {
			return ErrorCode.OK;
		}

		/* a database change is required */
		try {
			updateLocationPrepStmt.setInt(1, x);
			updateLocationPrepStmt.setInt(2, y);
			updateLocationPrepStmt.setInt(3, memberType);
			updateLocationPrepStmt.setInt(4, memberId);
			db.executePrepUpdate(updateLocationPrepStmt);
			
		} catch (SQLException e) {
			new FatalBuildStoreError("Error in SQL: " + e);
		}

		PackageDesc pkg = getPackageOfMember(memberType, memberId);
		if (pkg != null) {
			notifyListeners(pkg.pkgId, IPackageMemberMgrListener.CHANGED_LOCATION, memberType, memberId);
		}
		
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#getMemberLocation(int, int)
	 */
	@Override
	public MemberLocation getMemberLocation(int memberType, int memberId) {
		ResultSet rs;
		MemberLocation result = null;
			
		try {
			findLocationPrepStmt.setInt(1, memberType);
			findLocationPrepStmt.setInt(2, memberId);
			rs = db.executePrepSelectResultSet(findLocationPrepStmt);			
			
			/* if memberType/memberID is valid, fetch the x and y fields */
			if (rs.next()) {
				result = new MemberLocation();
				result.x = rs.getInt(1);
				result.y = rs.getInt(2);
			}
			rs.close();
				
		} catch (SQLException e) {
			new FatalBuildStoreError("Error in SQL: " + e);
		}
		return result;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#getNeighboursOf(int, int, int)
	 */
	@Override
	public MemberDesc[] getNeighboursOf(
			int memberType, int memberId, int direction, boolean showFilters) {
		
		/* validate direction */
		boolean lookLeft = false;
		boolean lookRight = false;
		if (direction == IPackageMemberMgr.NEIGHBOUR_LEFT) {
			lookLeft = true;
		} else if (direction == IPackageMemberMgr.NEIGHBOUR_RIGHT) {
			lookRight = true;
		} else if (direction == IPackageMemberMgr.NEIGHBOUR_ANY) {
			lookLeft = lookRight = true;
		} else {
			return null;
		}
		
		/* start with an empty list - we'll add to this as we go along */
		List<MemberDesc> neighbours = new ArrayList<MemberDesc>();

		/* is this member a file group? */
		if (memberType == IPackageMemberMgr.TYPE_FILE_GROUP) {
			int groupType = fileGroupMgr.getGroupType(memberId);
			if (groupType == ErrorCode.NOT_FOUND) {
				return null;
			}
			if (lookLeft) {
				getNeighboursOfFileGroupLeft(neighbours, memberId, groupType, showFilters);
			}
			if (lookRight) {
				getNeighboursOfFileGroupRight(neighbours, memberId, groupType, showFilters);
			}
		}
		
		/* is this member an action? */
		else if (memberType == IPackageMemberMgr.TYPE_ACTION) {
			if (!actionMgr.isActionValid(memberId)) {
				return null;
			}
			if (lookLeft) {
				getNeighboursOfActionLeft(neighbours, memberId, showFilters);
			}
			if (lookRight) {
				getNeighboursOfActionRight(neighbours, memberId);
			}		
		}
		
		/* we don't yet support other types of package member */ 
		else {
			return null;
		}

		/* 
		 * Convert the accumulated list of neighbours into an array, but first remove
		 * duplicates. This is an O(n*n) algorithm, but with a small number of
		 * neighbours it should be quick.
		 */
		List<MemberDesc> resultsNoDups = new ArrayList<MemberDesc>();
		for (Iterator<MemberDesc> iterator = neighbours.iterator(); iterator.hasNext();) {
			MemberDesc newMember = (MemberDesc) iterator.next();
			
			/* search to see if this neighbour is already in our "resultsNoDups" list */
			boolean alreadyEntered = false;
			for (Iterator<MemberDesc> iterator2 = resultsNoDups.iterator(); iterator2.hasNext();) {
				MemberDesc existingMember = (MemberDesc) iterator2.next();
				if ((newMember.memberType == existingMember.memberType) &&
						(newMember.memberId == existingMember.memberId)) {
					alreadyEntered = true;
					break;
				}
			}
			if (!alreadyEntered) {
				resultsNoDups.add(newMember);
			}
		}
		
		return resultsNoDups.toArray(new MemberDesc[resultsNoDups.size()]);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#getFilesInPackage(int)
	 */
	@Override
	public FileSet getFilesInPackage(int pkgId) {
		Integer results[] = null;
		try {
			findFilesInPackage1PrepStmt.setInt(1, pkgId);
			results = db.executePrepSelectIntegerColumn(findFilesInPackage1PrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* convert to a FileSet */
		return new FileSet(fileMgr, results);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#getFilesInPackage(int, int)
	 */
	@Override
	public FileSet getFilesInPackage(int pkgId, int pkgScopeId) {
		
		Integer results[] = null;
		try {
			findFilesInPackage2PrepStmt.setInt(1, pkgId);
			findFilesInPackage2PrepStmt.setInt(2, pkgScopeId);
			results = db.executePrepSelectIntegerColumn(findFilesInPackage2PrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* convert to a FileSet */
		return new FileSet(fileMgr, results);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#getFilesInPackage(java.lang.String)
	 */
	@Override
	public FileSet getFilesInPackage(String pkgSpec) {

		PackageDesc spec = parsePkgSpec(pkgSpec);
				
		/* the ID must not be invalid, else that's an error */
		if ((spec.pkgId == ErrorCode.NOT_FOUND) || (spec.pkgScopeId == ErrorCode.NOT_FOUND)) {
			return null;
		}
		
		/* 
		 * If the scope ID isn't specified by the user, then scopeId == 0 (the
		 * ID of the "None" scope). This indicates we should look for all paths
		 * in the package, regardless of the scope.
		 */
		if (spec.pkgScopeId != 0) {
			return getFilesInPackage(spec.pkgId, spec.pkgScopeId);
		} else {
			return getFilesInPackage(spec.pkgId);			
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#getFilesOutsidePackage(int)
	 */
	@Override
	public FileSet getFilesOutsidePackage(int pkgId) {
		Integer results[] = null;
		try {
			findFilesOutsidePackage1PrepStmt.setInt(1, pkgId);
			results = db.executePrepSelectIntegerColumn(findFilesOutsidePackage1PrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* convert to a FileSet */
		return new FileSet(fileMgr, results);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#getFilesOutsidePackage(int, int)
	 */
	@Override
	public FileSet getFilesOutsidePackage(int pkgId, int pkgScopeId) {
		Integer results[] = null;
		try {
			findFilesOutsidePackage2PrepStmt.setInt(1, pkgId);
			findFilesOutsidePackage2PrepStmt.setInt(2, pkgScopeId);
			results = db.executePrepSelectIntegerColumn(findFilesOutsidePackage2PrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* convert to a FileSet */
		return new FileSet(fileMgr, results);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#getFilesOutsidePackage(java.lang.String)
	 */
	@Override
	public FileSet getFilesOutsidePackage(String pkgSpec) {
		
		PackageDesc spec = parsePkgSpec(pkgSpec);
		
		/* the ID must not be invalid, else that's an error */
		if ((spec.pkgId == ErrorCode.NOT_FOUND) || (spec.pkgScopeId == ErrorCode.NOT_FOUND)) {
			return null;
		}
		
		/* 
		 * The scope ID is optional, since it still allows us to
		 * get the package's files. Note that scopeId == 0 implies
		 * that the user didn't specify a /scope value.
		 */
		if (spec.pkgScopeId != 0) {
			return getFilesOutsidePackage(spec.pkgId, spec.pkgScopeId);
		} else {
			return getFilesOutsidePackage(spec.pkgId);			
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#getActionsInPackage(int)
	 */
	@Override
	public ActionSet getActionsInPackage(int pkgId) {
		Integer results[] = null;
		try {
			findActionsInPackagePrepStmt.setInt(1, pkgId);
			results = db.executePrepSelectIntegerColumn(findActionsInPackagePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		return new ActionSet(actionMgr, results);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#getActionsInPackage(java.lang.String)
	 */
	@Override
	public ActionSet getActionsInPackage(String pkgSpec) {
		
		/* translate the package's name to its ID */
		int pkgId = pkgMgr.getId(pkgSpec);
		if (pkgId == ErrorCode.NOT_FOUND){
			return null;
		}
		
		return getActionsInPackage(pkgId);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#getActionsOutsidePackage(int)
	 */
	@Override
	public ActionSet getActionsOutsidePackage(int pkgId) {
		Integer results[] = null;
		try {
			findActionsOutsidePackagePrepStmt.setInt(1, pkgId);
			results = db.executePrepSelectIntegerColumn(findActionsOutsidePackagePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		return new ActionSet(actionMgr, results);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#getActionsOutsidePackage(java.lang.String)
	 */
	@Override
	public ActionSet getActionsOutsidePackage(String pkgSpec) {
		
		/* translate the package's name to its ID */
		int pkgId = pkgMgr.getId(pkgSpec);
		if (pkgId == ErrorCode.NOT_FOUND){
			return null;
		}
		
		return getActionsOutsidePackage(pkgId);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#addListener(com.buildml.model.IPackageMemberMgrListener)
	 */
	@Override
	public void addListener(IPackageMemberMgrListener listener) {
		listeners.add(listener);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#removeListener(com.buildml.model.IPackageMemberMgrListener)
	 */
	@Override
	public void removeListener(IPackageMemberMgrListener listener) {
		listeners.remove(listener);
	};
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * Notify any registered listeners about our change in state.
	 * @param pkgId   		The package that has changed.
	 * @param how     		The way in which the package changed (see {@link IPackageMemberMgrListener}).
	 * @param memberType	The type of the member that has changed (e.g. TYPE_ACTION)
	 * @param memberId		The ID of the member (e.g. actionId or fileGroupId).
	 */
	private void notifyListeners(int pkgId, int how, int memberType, int memberId) {
		
		/* 
		 * Make a copy of the listeners list, otherwise a registered listener can't remove
		 * itself from the list within the packageChangeNotification() method.
		 */
		IPackageMemberMgrListener listenerCopy[] = 
				listeners.toArray(new IPackageMemberMgrListener[listeners.size()]);
		for (int i = 0; i < listenerCopy.length; i++) {
			listenerCopy[i].packageMemberChangeNotification(pkgId, how, memberType, memberId);			
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Helper function for GetNeighboursOf() to compute the left-side neighbours of an action.
	 * 
	 * @param neighbours	The List<MemberDesc> to add newly-found neighbours into.
	 * @param actionId		The ID of the action whose neighbours we are looking for.
	 * @param showFilters	True if we should return neighbouring filter file groups, or
	 *                      false if filter groups should be skipped over.
	 */
	private void getNeighboursOfActionLeft(List<MemberDesc> neighbours, int actionId, boolean showFilters)
	{
		try {
			/* 
			 * Query the database for all file groups that are mentioned in our INPUT slots.
			 */
			findActionNeighbourPrepStmt.setInt(1, actionId);
			findActionNeighbourPrepStmt.setInt(2, ISlotTypes.SLOT_POS_INPUT);
			ResultSet rs = db.executePrepSelectResultSet(findActionNeighbourPrepStmt);
			while (rs.next()) {
				MemberDesc member = new MemberDesc(IPackageMemberMgr.TYPE_FILE_GROUP,
						Integer.valueOf(rs.getString(1)), rs.getInt(2), rs.getInt(3));
				
				/* skip over filters, if necessary */
				if (!showFilters && 
				    (fileGroupMgr.getGroupType(member.memberId) == IFileGroupMgr.FILTER_GROUP)) {
					member = getPredecessorOfFilter(member.memberId);
				} 
				
				/* else add non-filter groups to our neighbour list */
				if (member != null) {
					neighbours.add(member);					
				}
			}
			rs.close();


		} catch (SQLException e) {
			BuildStoreDB.throwSqlException(e);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper function for GetNeighboursOf() to compute the right-side neighbours of an action.
	 * 
	 * @param neighbours	The List<MemberDesc> to add newly-found neighbours into.
	 * @param actionId		The ID of the action whose neighbours we are looking for.
	 */
	private void getNeighboursOfActionRight(List<MemberDesc> neighbours, int actionId)
	{
		try {
			/* 
			 * Query the database for all file groups mentioned in our OUTPUT slots.
			 */
			findActionNeighbourPrepStmt.setInt(1, actionId);
			findActionNeighbourPrepStmt.setInt(2, ISlotTypes.SLOT_POS_OUTPUT);
			ResultSet rs = db.executePrepSelectResultSet(findActionNeighbourPrepStmt);
			while (rs.next()) {
				MemberDesc member = new MemberDesc(IPackageMemberMgr.TYPE_FILE_GROUP,
						Integer.valueOf(rs.getString(1)), rs.getInt(2), rs.getInt(3));
				neighbours.add(member);
			}
			rs.close();

		} catch (SQLException e) {
			BuildStoreDB.throwSqlException(e);
		}
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Helper function for GetNeighboursOf() to compute the left-side neighbours of a file group.
	 * 
	 * @param neighbours	The List<MemberDesc> to add newly-found neighbours into.
	 * @param fileGroupId	The ID of the file group whose neighbours we are looking for.
	 * @param fileGroupType The type of this file group (e.g. MERGE_GROUP, etc).
	 * @param showFilters	True if we should return neighbouring filter file groups, or
	 *                      false if filter groups should be skipped over.
	 */
	private void getNeighboursOfFileGroupLeft(List<MemberDesc> neighbours, int fileGroupId, 
			int fileGroupType, boolean showFilters)
	{
		try {

			/*
			 * To find left neighbours that are actions... search for output slots in this 
			 * package's actions that refer to our file group.
=			 */
			findFileGroupNeighbourPrepStmt.setInt(1, fileGroupId);
			findFileGroupNeighbourPrepStmt.setInt(2, ISlotTypes.SLOT_POS_OUTPUT);
			ResultSet rs = db.executePrepSelectResultSet(findFileGroupNeighbourPrepStmt);
			while (rs.next()) {
				MemberDesc member = new MemberDesc(IPackageMemberMgr.TYPE_ACTION,
						Integer.valueOf(rs.getString(1)), rs.getInt(2), rs.getInt(3));
				neighbours.add(member);
			}
			rs.close();

			/*
			 * If this is a merge group, get all the members of this group (they will all
			 * be file groups, since merge groups can only contain file groups).
			 */
			if (fileGroupType == IFileGroupMgr.MERGE_GROUP) {

				findMergeFileGroupMembersPrepStmt.setInt(1, fileGroupId);
				rs = db.executePrepSelectResultSet(findMergeFileGroupMembersPrepStmt);
				while (rs.next()) {
					MemberDesc member = new MemberDesc(IPackageMemberMgr.TYPE_FILE_GROUP,
							rs.getInt(1), rs.getInt(2), rs.getInt(3));
					
					/* skip over filters, if necessary */
					if (!showFilters && 
					    (fileGroupMgr.getGroupType(member.memberId) == IFileGroupMgr.FILTER_GROUP)) {
						member = getPredecessorOfFilter(member.memberId);
					}
					if (member != null) {
						neighbours.add(member);
					}
				}
				rs.close();
			}

			/*
			 * If this file group is itself a filter file group, the left neighbour is always the
			 * predecessor.
			 */
			if (fileGroupType == IFileGroupMgr.FILTER_GROUP) {
				MemberDesc member = getPredecessorOfFilter(fileGroupId);
				if (member != null) {
					neighbours.add(member);
				}
			}

		} catch (SQLException e) {
			BuildStoreDB.throwSqlException(e);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper function for GetNeighboursOf() to compute the right-side neighbours of a file group.
	 * 
	 * @param neighbours	The List<MemberDesc> to add newly-found neighbours into.
	 * @param fileGroupId	The ID of the file group whose neighbours we are looking for.
	 * @param fileGroupType The type of this file group (e.g. MERGE_GROUP, etc).
	 * @param showFilters	True if we should return neighbouring filter file groups, or
	 *                      false if filter groups should be skipped over.
	 */
	private void getNeighboursOfFileGroupRight(List<MemberDesc> neighbours, int fileGroupId, 
												int fileGroupType, boolean showFilters)
	{
		try {
			/*
			 * To find right neighbours that are actions... search for input slots in this 
			 * package's actions that refer to our file group.
=			 */
			findFileGroupNeighbourPrepStmt.setInt(1, fileGroupId);
			findFileGroupNeighbourPrepStmt.setInt(2, ISlotTypes.SLOT_POS_INPUT);
			ResultSet rs = db.executePrepSelectResultSet(findFileGroupNeighbourPrepStmt);
			while (rs.next()) {
				MemberDesc member = new MemberDesc(IPackageMemberMgr.TYPE_ACTION,
						Integer.valueOf(rs.getString(1)), rs.getInt(2), rs.getInt(3));
				neighbours.add(member);
			}
			rs.close();

			/*
			 * To find a right neighbour that's a merge group... look for
			 * all occurrences of our file group ID in all merge groups.
			 */
			findMergeFileGroupsContainingFileGroup.setInt(1, fileGroupId);
			rs = db.executePrepSelectResultSet(findMergeFileGroupsContainingFileGroup);
			while (rs.next()) {
				MemberDesc member = new MemberDesc(IPackageMemberMgr.TYPE_FILE_GROUP,
						rs.getInt(1), rs.getInt(2), rs.getInt(3));
				neighbours.add(member);
			}
			rs.close();					

			/*
			 * When our right neighbour is a filter group... if we're a non-filter group, 
			 * search for any filter file groups that this file group is a predecessor of. 
			 * However, if showFilters is false, then we must instead return the right
			 * neighbour of the filter, rather than the filter itself.
			 */
			if (fileGroupType != IFileGroupMgr.FILTER_GROUP) {
			
				/* get a list of all the filters */
				List<MemberDesc> filters = new ArrayList<MemberDesc>();
				findFiltersContainingFileGroup.setInt(1, fileGroupId);
				rs = db.executePrepSelectResultSet(findFiltersContainingFileGroup);
				while (rs.next()) {
					MemberDesc member = new MemberDesc(IPackageMemberMgr.TYPE_FILE_GROUP,
							rs.getInt(1), -1, -1);	/* filter groups are never positioned */
					filters.add(member);
				}
				rs.close();	

				/* if showFilters is true, we do want to see the filters returned */ 
				if (showFilters) {
					neighbours.addAll(filters);						
				}

				/* if we don't want to showFilters, return the right neighbours of each filter */
				else {
					for (Iterator<MemberDesc> iterator = filters.iterator(); iterator.hasNext();) {
						MemberDesc memberDesc = (MemberDesc) iterator.next();
						MemberDesc[] filterNeighbours = getNeighboursOf(
								IPackageMemberMgr.TYPE_FILE_GROUP, memberDesc.memberId, 
								IPackageMemberMgr.NEIGHBOUR_RIGHT, false);
						neighbours.addAll(Arrays.asList(filterNeighbours));
					}
				}
			}

		} catch (SQLException e) {
			BuildStoreDB.throwSqlException(e);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Helper method for returning the left-neighbour of a filter file group.
	 * @param fileGroupId ID of the filter file group.
	 * @return The corresponding MemberDesc of the predecessor, or null on error.
	 */
	private MemberDesc getPredecessorOfFilter(int fileGroupId) {

		int predId = fileGroupMgr.getPredId(fileGroupId);
		if (predId >= 0) {
			MemberLocation location = getMemberLocation(
					IPackageMemberMgr.TYPE_FILE_GROUP, predId);
			if (location != null) {
				MemberDesc member = new MemberDesc(IPackageMemberMgr.TYPE_FILE_GROUP,
						predId, location.x, location.y);
				return member;
			}
		}
		return null;
	}

	/*-------------------------------------------------------------------------------------*/
}