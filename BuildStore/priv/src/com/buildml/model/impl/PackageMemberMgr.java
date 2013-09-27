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
import java.util.List;

import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IFileMgr.PathType;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMemberMgrListener;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageRootMgr;
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
		updateActionPackagePrepStmt = null,
		findActionPackagePrepStmt = null,
		findActionsInPackagePrepStmt = null,
		findActionsOutsidePackagePrepStmt = null;
	
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
		
		findMemberPackagePrepStmt = db.prepareStatement(
				"select pkgId, scopeId from packageMembers where memberType = ? and memberId = ?");
		updatePackagePrepStmt = 
				db.prepareStatement("update packageMembers set pkgId = ?, scopeId = ? " +
									"where memberType = ? and memberId = ?");				
		findFilesInPackage1PrepStmt = db.prepareStatement(
				"select memberId from packageMembers where pkgId = ? and memberType = " + MEMBER_TYPE_FILE);
		findFilesInPackage2PrepStmt = db.prepareStatement(
				"select memberId from packageMembers where pkgId = ? and memberType = " + 
						MEMBER_TYPE_FILE + " and scopeId = ?");
		findFilesOutsidePackage1PrepStmt = db.prepareStatement(
				"select memberId from packageMembers where pkgId != ? and memberType = " + MEMBER_TYPE_FILE);
		findFilesOutsidePackage2PrepStmt = db.prepareStatement(
				"select memberId from packageMembers where memberType = " + MEMBER_TYPE_FILE +
				" and not (pkgId = ? and scopeId = ?)");
		updateActionPackagePrepStmt = db.prepareStatement("update buildActions set pkgId = ? " +
				"where actionId = ?");
		findActionPackagePrepStmt = db.prepareStatement("select pkgId from packageMembers where memberId = ?" +
				" and memberType = " + MEMBER_TYPE_ACTION);
		findActionsInPackagePrepStmt = db.prepareStatement(
				"select memberId from packageMembers where pkgId = ? and memberType = " + MEMBER_TYPE_ACTION);
		findActionsOutsidePackagePrepStmt = db.prepareStatement("select memberId from packageMembers " +
				"where pkgId != ? and memberId != 0 and memberType = " + MEMBER_TYPE_ACTION);
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
		if (memberType == MEMBER_TYPE_FILE) {
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
		notifyListeners(oldPkg.pkgId, IPackageMemberMgrListener.CHANGED_MEMBERSHIP);
		if (oldPkg.pkgId != pkgId) {
			notifyListeners(pkgId, IPackageMemberMgrListener.CHANGED_MEMBERSHIP);
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
		if (memberTypeFilter != MEMBER_TYPE_ANY) {
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
				MemberDesc newMember = new MemberDesc();
				newMember.memberType = rs.getInt(1);
				newMember.memberId = rs.getInt(2);
				newMember.x = rs.getInt(3);
				newMember.y = rs.getInt(4);
				members.add(newMember);
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
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMemberMgr#getMemberLocation(int, int)
	 */
	@Override
	public MemberLocation getMemberLocation(int memberType, int memberId) {
		// TODO Auto-generated method stub
		return null;
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
	 * @param pkgId   The package that has changed.
	 * @param how     The way in which the package changed (see {@link IPackageMemberMgrListener}).
	 */
	private void notifyListeners(int pkgId, int how) {
		
		/* 
		 * Make a copy of the listeners list, otherwise a registered listener can't remove
		 * itself from the list within the packageChangeNotification() method.
		 */
		IPackageMemberMgrListener listenerCopy[] = 
				listeners.toArray(new IPackageMemberMgrListener[listeners.size()]);
		for (int i = 0; i < listenerCopy.length; i++) {
			listenerCopy[i].packageMemberChangeNotification(pkgId, how);			
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
}