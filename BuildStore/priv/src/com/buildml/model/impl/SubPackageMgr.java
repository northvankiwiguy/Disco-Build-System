/*******************************************************************************
 * Copyright (c) 2014 Arapiki Solutions Inc.
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMgr;
import com.buildml.model.ISlotTypes;
import com.buildml.model.ISubPackageMgr;
import com.buildml.model.ISubPackageMgrListener;
import com.buildml.model.ISlotTypes.SlotDetails;
import com.buildml.utils.errors.ErrorCode;

/**
 * A manager class (that supports the BuildStore class) responsible for managing all 
 * BuildStore information pertaining to sub-packages.
 * <p>
 * There should be exactly one SubPackageMgr object per BuildStore object. Use the
 * BuildStore's getSubPackageMgr() method to obtain that one instance.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
/* package private */ class SubPackageMgr implements ISubPackageMgr {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** The BuildStore object that owns all the manager objects. */
	private IBuildStore buildStore = null;
	
	/**
	 * Our database manager object, used to access the database content. This is provided 
	 * to us when the Packages object is first instantiated.
	 */
	private BuildStoreDB db = null;
	
	/** The IPackageMgr object that manages our package types. */
	private IPackageMgr pkgMgr = null;
	
	/** The IPackageMemberMgr object that manages our package types. */
	private IPackageMemberMgr pkgMemberMgr = null;	
	
	/** The SlotMgr object that manages the slots in our packages. */
	private SlotMgr slotMgr = null;	

	/**
	 * Various prepared statements for database access.
	 */
	private PreparedStatement 
		addSubPackagePrepStmt = null,
		insertPackageMemberPrepStmt = null,
		findSubPackageTypePrepStmt = null,
		trashOrReviveSubPackagePrepStmt = null,
		findSubPackagesOfTypePrepStmt = null,
		isValidOrTrashedPrepStmt = null;
	
	/** The event listeners who are registered to learn about sub-package changes */
	private List<ISubPackageMgrListener> listeners = new ArrayList<ISubPackageMgrListener>();
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new Packages object, which represents the file/action packages that
	 * are part of the BuildStore.
	 * 
	 * @param buildStore The BuildStore that this Packages object belongs to.
	 */
	public SubPackageMgr(BuildStore buildStore) {
		this.buildStore = buildStore;
		this.db = buildStore.getBuildStoreDB();
		this.pkgMgr = buildStore.getPackageMgr();
		this.slotMgr = buildStore.getSlotMgr();
		
		/* initialize prepared database statements */
		addSubPackagePrepStmt = db.prepareStatement(
				"insert into subPackages values (null, ?, 0)");
		insertPackageMemberPrepStmt = db.prepareStatement(
				"insert into packageMembers values (?, ?, ?, ?, -1, -1)");
		trashOrReviveSubPackagePrepStmt = db.prepareStatement(
				"update subPackages set trashed = ? where subPkgId = ? and trashed = ?");
		findSubPackageTypePrepStmt = db.prepareStatement(
				"select pkgTypeId from subPackages where subPkgId = ? and trashed = 0");
		findSubPackagesOfTypePrepStmt = db.prepareStatement(
				"select distinct packageMembers.pkgId from packageMembers, subPackages" +
				" where packageMembers.memberType = " + IPackageMemberMgr.TYPE_SUB_PACKAGE + 
				" and packageMembers.memberId = subPackages.subPkgId" +
				" and subPackages.pkgTypeId = ?" +
				" and subPackages.trashed = 0");
		isValidOrTrashedPrepStmt = db.prepareStatement(
				"select trashed from subPackages where subPkgId = ?");
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.model.ISubPackageMgr#newSubPackage(int, int)
	 */
	@Override
	public int newSubPackage(int parentPkgId, int pkgTypeId) {

		/* validate parent package - must be a valid package (not a folder) */
		if (!(pkgMgr.isValid(parentPkgId)) || pkgMgr.isFolder(parentPkgId)) {
			return ErrorCode.BAD_VALUE;
		}
				
		/* validate package type - must be a valid package (not a folder) */
		if (!(pkgMgr.isValid(pkgTypeId)) || pkgMgr.isFolder(pkgTypeId)) {
			return ErrorCode.NOT_FOUND;
		}

		/* we can't create a sub-package of type "Main" or "<import>" */
		if ((pkgTypeId == pkgMgr.getMainPackage()) || (pkgTypeId == pkgMgr.getImportPackage())) {
			return ErrorCode.NOT_FOUND;
		}

		/* validate that no cycles are created */
		if (isAncestorOf(pkgTypeId,  parentPkgId)) {
			return ErrorCode.LOOP_DETECTED;
		}
		
		/* insert the sub-package information into the database */
		int lastRowId;
		try {
			addSubPackagePrepStmt.setInt(1, pkgTypeId);
			db.executePrepUpdate(addSubPackagePrepStmt);
			
			lastRowId = db.getLastRowID();			
			
			/* 
			 * insert the default package membership values (set parent of -1 so we
			 * can change it later on an trigger a notification).
			 */
			insertPackageMemberPrepStmt.setInt(1, IPackageMemberMgr.TYPE_SUB_PACKAGE);
			insertPackageMemberPrepStmt.setInt(2, lastRowId);
			insertPackageMemberPrepStmt.setInt(3, -1);
			insertPackageMemberPrepStmt.setInt(4, IPackageMemberMgr.SCOPE_NONE);
			if (db.executePrepUpdate(insertPackageMemberPrepStmt) != 1) {
				throw new FatalBuildStoreError("Unable to insert new record into packageMembers table");
			}

			/* 
			 * This additional call is required to trigger package membership change notifications.
			 * Without this, our package diagrams won't be refreshed properly.
			 */
			pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_SUB_PACKAGE, lastRowId, parentPkgId);

		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* return the new package's ID number */
		return lastRowId;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.ISubPackageMgr#getSubPackageType(int)
	 */
	@Override
	public int getSubPackageType(int subPkgId) {
		
		int pkgTypeId = ErrorCode.NOT_FOUND;
		
		try {
			findSubPackageTypePrepStmt.setInt(1, subPkgId);
			Integer results[] = db.executePrepSelectIntegerColumn(findSubPackageTypePrepStmt);
			if (results.length == 1){
				pkgTypeId = results[0];
			}
	
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return pkgTypeId;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.ISubPackageMgr#isSubPackageValid(int)
	 */
	@Override
	public boolean isSubPackageValid(int subPkgId) {

		/*
		 * A sub-package is valid if there's a single record for it. We don't actually
		 * care if it's trashed or not - it's still valid.
		 */
		try {
			isValidOrTrashedPrepStmt.setInt(1, subPkgId);
			Integer rows[] = db.executePrepSelectIntegerColumn(isValidOrTrashedPrepStmt);
			return (rows.length == 1);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.ISubPackageMgr#moveSubPackageToTrash(int)
	 */
	@Override
	public int moveSubPackageToTrash(int subPkgId) {
		return trashReviveCommon(subPkgId, 1);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.ISubPackageMgr#reviveSubPackageFromTrash(int)
	 */
	@Override
	public int reviveSubPackageFromTrash(int subPkgId) {
		return trashReviveCommon(subPkgId, 0);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.ISubPackageMgr#isSubPackageTrashed(int)
	 */
	@Override
	public boolean isSubPackageTrashed(int subPkgId) {
		
		/*
		 * A sub-package is trashed if the "trashed" field is set (or if for some reason
		 * there's no record for it).
		 */
		try {
			isValidOrTrashedPrepStmt.setInt(1, subPkgId);
			Integer rows[] = db.executePrepSelectIntegerColumn(isValidOrTrashedPrepStmt);
			return (rows.length != 1) || (rows[0] == 1);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.ISubPackageMgr#getSlotByName(int, java.lang.String)
	 */
	@Override
	public int getSlotByName(int subPkgId, String slotName) {
		
		int pkgId = getSubPackageType(subPkgId);
		if (pkgId == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}
		SlotDetails details = pkgMgr.getSlotByName(pkgId, slotName);
		if (details == null) {
			return ErrorCode.NOT_FOUND;
		}
		
		return details.slotId;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.ISubPackageMgr#setSlotValue(int, int, java.lang.Object)
	 */
	@Override
	public int setSlotValue(int subPkgId, int slotId, Object value) {

		/* validate subPkgId */
		if (!isSubPackageValid(subPkgId)) {
			return ErrorCode.NOT_FOUND;
		}
				
		/* what is the current slot value? */
		Object oldValue = slotMgr.getSlotValue(ISlotTypes.SLOT_OWNER_PACKAGE, subPkgId, slotId);
		
		/* if no change in value, do nothing */
		if (((oldValue == null) && (value == null)) || 
			((oldValue != null) && (oldValue.equals(value)))) {
			return ErrorCode.OK;
		}
		
		/* delegate all slot assignments to SlotMgr */
		int status = slotMgr.setSlotValue(ISlotTypes.SLOT_OWNER_PACKAGE, subPkgId, slotId, value);
		
		/* notify listeners about the change */
		notifyListeners(subPkgId, ISubPackageMgrListener.CHANGED_SLOT, slotId);
		return status;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.ISubPackageMgr#getSlotValue(int, int)
	 */
	@Override
	public Object getSlotValue(int subPkgId, int slotId) {
		
		/* validate subPkgId */
		if (!isSubPackageValid(subPkgId)) {
			return null;
		}

		/* delegate all slot assignments to SlotMgr */
		return slotMgr.getSlotValue(ISlotTypes.SLOT_OWNER_PACKAGE, subPkgId, slotId);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.ISubPackageMgr#isSlotSet(int, int)
	 */
	@Override
	public boolean isSlotSet(int subPkgId, int slotId) {
		return slotMgr.isSlotSet(ISlotTypes.SLOT_OWNER_PACKAGE, subPkgId, slotId);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.ISubPackageMgr#clearSlotValue(int, int)
	 */
	@Override
	public void clearSlotValue(int subPkgId, int slotId) {
		slotMgr.clearSlotValue(ISlotTypes.SLOT_OWNER_PACKAGE, subPkgId, slotId);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.ISubPackageMgr#addListener(com.buildml.model.ISubPackageMgrListener)
	 */
	@Override
	public void addListener(ISubPackageMgrListener listener) {
		listeners.add(listener);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.ISubPackageMgr#removeListener(com.buildml.model.ISubPackageMgrListener)
	 */
	@Override
	public void removeListener(ISubPackageMgrListener listener) {
		listeners.remove(listener);
	};
	
	/*=====================================================================================*
	 * PACKAGE-PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * A second phase of initialization, needed when managers are fully initialized
	 * in the necessary order.
	 */
	public void initPass2() {
		this.pkgMemberMgr = buildStore.getPackageMemberMgr();
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * Common code, shared between moveSubPackageToTrash() and reviveSubPackageFromTrash().
	 * 
	 * @param subPkgId		ID of the sub-package to trash/revive.
	 * @param toTrashState	The state to move it to (1 = trash, 0 = revive).
	 * @return ErrorCode.OK on success, or ErrorCode.NOT_FOUND if subPkgId has no record.
	 */
	private int trashReviveCommon(int subPkgId, int toTrashState) {

		try {
			trashOrReviveSubPackagePrepStmt.setInt(1, toTrashState);
			trashOrReviveSubPackagePrepStmt.setInt(2, subPkgId);
			trashOrReviveSubPackagePrepStmt.setInt(3, 1 - toTrashState);

			int rowCount = db.executePrepUpdate(trashOrReviveSubPackagePrepStmt);
			if (rowCount != 1) {
				return ErrorCode.NOT_FOUND;
			}

		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}

		/* notify listeners of this event */
		notifyListeners(subPkgId, ISubPackageMgrListener.TRASHED_SUB_PACKAGE, 0);
		
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/** 
	 * Determine whether pkgA contains pkgB as a sub-package (or sub-sub-package etc).
	 * 
	 * @param pkgAId The package that is potentially an ancestor of pkgB.
	 * @param pkgBId The package that is potentially contained within pkgA.
	 * @return True if pkgA contains pkgB, else false.
	 */
	private boolean isAncestorOf(int pkgAId, int pkgBId) {

		/* 
		 * If the package IDs are the same, then there's definitely 
		 * an ancestor relationship.
		 */
		if (pkgAId == pkgBId) {
			return true;
		}
		
		/*
		 * Work upward from pkgBId until we reach packageMain.
		 */
		int pkgMain = pkgMgr.getMainPackage();
		
		/*
		 * Step 1: Find the packages that contain sub-packages of type "pkgBId".
		 */
		Integer containingPackages[];
		try {
			findSubPackagesOfTypePrepStmt.setInt(1, pkgBId);
			containingPackages = db.executePrepSelectIntegerColumn(findSubPackagesOfTypePrepStmt);
	
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}

		/*
		 * Step 2: If the containingPackages contains pkgAId, return true.
		 */
		for (int i = 0; i < containingPackages.length; i++) {
			if (containingPackages[i] == pkgAId) {
				return true;
			}
		}
		
		/*
		 * Step 3: Call ourselves recursively for each of the containing packages.
		 */
		for (int i = 0; i < containingPackages.length; i++) {
			int thisParent = containingPackages[i];
			if ((thisParent != pkgMain) && (isAncestorOf(pkgAId, containingPackages[i]))) {
				return true;
			}
		}
		
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Notify any registered listeners about our change in state.
	 * 
	 * @param subPkgId  The sub-package that has changed.
	 * @param how       The way in which the sub-package changed (see {@link ISubPackageMgrListener}).
	 * @param changeId  Which thing has changed (CHANGED_SLOT)
	 */
	private void notifyListeners(int subPkgId, int how, int changeId) {
		
		/* 
		 * Make a copy of the listeners list, otherwise a registered listener can't remove
		 * itself from the list within the subPackageChangeNotification() method.
		 */
		ISubPackageMgrListener listenerCopy[] = 
				listeners.toArray(new ISubPackageMgrListener[listeners.size()]);
		for (int i = 0; i < listenerCopy.length; i++) {
			listenerCopy[i].subPackageChangeNotification(subPkgId, how, changeId);			
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

}