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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageMgrListener;
import com.buildml.model.IPackageRootMgr;
import com.buildml.model.ISlotTypes;
import com.buildml.model.ISlotTypes.SlotDetails;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.ActionSet;
import com.buildml.utils.errors.ErrorCode;

/**
 * A manager class (that supports the BuildStore class) responsible for managing all 
 * BuildStore information pertaining to packages.
 * <p>
 * There should be exactly one PackageMgr object per BuildStore object. Use the
 * BuildStore's getPackageMgr() method to obtain that one instance.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
/* package private */ class PackageMgr implements IPackageMgr {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** ID number for the <import> package */
	private final static int IMPORT_PACKAGE_ID = 0;
	
	/** ID number for the "Root" folder */
	private final static int ROOT_FOLDER_ID = 1;
	
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
	
	/** The SlotMgr object that manages the slots in our packages. */
	private SlotMgr slotMgr = null;	

	/**
	 * Various prepared statements for database access.
	 */
	private PreparedStatement 
		addPackagePrepStmt = null,
		findPackageByNamePrepStmt = null,
		findPackageByIdPrepStmt = null,
		findPackageTypePrepStmt = null,
		findPackageParentPrepStmt = null,
		updatePackageParentPrepStmt = null,
		updatePackageNamePrepStmt = null,
		findAllPackagesPrepStmt = null,
		findChildPackagesPrepStmt = null,
		removePackageByIdPrepStmt = null;
	
	/** The event listeners who are registered to learn about package changes */
	List<IPackageMgrListener> listeners = new ArrayList<IPackageMgrListener>();
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new Packages object, which represents the file/action packages that
	 * are part of the BuildStore.
	 * 
	 * @param buildStore The BuildStore that this Packages object belongs to.
	 */
	public PackageMgr(BuildStore buildStore) {
		this.buildStore = buildStore;
		this.db = buildStore.getBuildStoreDB();
		this.fileMgr = buildStore.getFileMgr();
		this.actionMgr = buildStore.getActionMgr();
		this.slotMgr = buildStore.getSlotMgr();
		
		/* initialize prepared database statements */
		addPackagePrepStmt = db.prepareStatement("insert into packages values (null, ?, " + 
												 ROOT_FOLDER_ID + ", ?)");
		findPackageByNamePrepStmt = db.prepareStatement("select id from packages where name = ?");
		findPackageByIdPrepStmt = db.prepareStatement("select name from packages where id = ?");
		findPackageTypePrepStmt = db.prepareStatement("select isFolder from packages where id = ?");
		findPackageParentPrepStmt = db.prepareStatement("select parent from packages where id = ?");
		updatePackageParentPrepStmt = db.prepareStatement("update packages set parent = ? where id = ?");
		updatePackageNamePrepStmt = db.prepareStatement("update packages set name = ? where id = ?");
		findAllPackagesPrepStmt = db.prepareStatement(
				"select name from packages where isFolder = 0 order by name collate nocase");
		findChildPackagesPrepStmt = db.prepareStatement(
				"select id from packages where parent = ? and id != " + ROOT_FOLDER_ID + 
				" order by isFolder desc, name collate nocase");
		removePackageByIdPrepStmt = db.prepareStatement("delete from packages where id = ?");
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#getRootFolder()
	 */
	@Override
	public int getRootFolder() {
		return ROOT_FOLDER_ID;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#getImportPackage()
	 */
	@Override
	public int getImportPackage() {
		return IMPORT_PACKAGE_ID;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#addPackage(java.lang.String)
	 */
	@Override
	public int addPackage(String packageName) {
		
		/* add the new package */
		int pkgId = addPackageOrFolderHelper(packageName, false);
		if (pkgId < 0) {
			return pkgId;
		}
		
		/* set the package's roots to the same level as the workspace root */
		IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
		int workspaceRootId = pkgRootMgr.getWorkspaceRoot();
		if (workspaceRootId == ErrorCode.NOT_FOUND) {
			throw new FatalBuildStoreError(
					"Workspace root must be set before addPackage is called");
		}
		int rc = pkgRootMgr.setPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT, workspaceRootId);
		if (rc != ErrorCode.OK) {
			return rc;
		}
		rc = pkgRootMgr.setPackageRoot(pkgId, IPackageRootMgr.GENERATED_ROOT, workspaceRootId);
		if (rc != ErrorCode.OK) {
			return rc;
		}
		return pkgId;
	};

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#addFolder(java.lang.String)
	 */
	@Override
	public int addFolder(String folderName) {
		return addPackageOrFolderHelper(folderName, true);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#getPackageName(int)
	 */
	@Override
	public String getName(int folderOrPackageId) {
		
		/* find the package in our table */
		String results[] = null;
		try {
			findPackageByIdPrepStmt.setInt(1, folderOrPackageId);
			results = db.executePrepSelectStringColumn(findPackageByIdPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* no result == no package with this Id */
		if (results.length == 0) {
			return null;
		} 
		
		/* one result == we have the correct name */
		else if (results.length == 1) {
			return results[0];
		}
		
		/* multiple results is an error */
		else {
			throw new FatalBuildStoreError("Multiple entries found in packages table, for ID " + 
					folderOrPackageId);
		}	
	};

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#setName(int, java.lang.String)
	 */
	@Override
	public int setName(int folderOrPackageId, String newName) {
		
		/* if there's no change in name, there's nothing to update */
		if (newName.equals(getName(folderOrPackageId))) {
			return ErrorCode.OK;
		}
		
		/* check that the package/folder doesn't already exist in the database */
		if (getId(newName) != ErrorCode.NOT_FOUND){
			return ErrorCode.ALREADY_USED;
		}

		/* validate the new package/folder's name */
		if (!isValidName(newName)){
			return ErrorCode.INVALID_NAME;
		}

		/* 
		 * If this is a package (not a folder), we also must rename the associated
		 * roots. This is done by deleting the old roots and adding new roots.
		 */
		IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
		boolean isFolder = isFolder(folderOrPackageId); 
		int srcRootPath = 0;
		int genRootPath = 0;
		if (!isFolder) {
			srcRootPath = pkgRootMgr.getPackageRoot(folderOrPackageId, IPackageRootMgr.SOURCE_ROOT);
			genRootPath = pkgRootMgr.getPackageRoot(folderOrPackageId, IPackageRootMgr.GENERATED_ROOT);
			pkgRootMgr.removePackageRoot(folderOrPackageId, IPackageRootMgr.SOURCE_ROOT);
			pkgRootMgr.removePackageRoot(folderOrPackageId, IPackageRootMgr.GENERATED_ROOT);
		}
		
		/* update the database */
		try {
			updatePackageNamePrepStmt.setString(1, newName);
			updatePackageNamePrepStmt.setInt(2, folderOrPackageId);
			int rowCount = db.executePrepUpdate(updatePackageNamePrepStmt);
			if (rowCount == 0) {
				return ErrorCode.NOT_FOUND;
			}
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* now, add the new package roots at the same location as the old */
		if (!isFolder){
			pkgRootMgr.setPackageRoot(folderOrPackageId, IPackageRootMgr.SOURCE_ROOT, srcRootPath);
			pkgRootMgr.setPackageRoot(folderOrPackageId, IPackageRootMgr.GENERATED_ROOT, genRootPath);
		}
		
		/* notify anybody who cares that our name has changed */
		notifyListeners(folderOrPackageId, IPackageMgrListener.CHANGED_NAME);
		return ErrorCode.OK;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#getPackageId(java.lang.String)
	 */
	@Override
	public int getId(String folderOrPackageName) {
		
		/* find the package into our table */
		Integer results[] = null;
		try {
			findPackageByNamePrepStmt.setString(1, folderOrPackageName);
			results = db.executePrepSelectIntegerColumn(findPackageByNamePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* no result == no package by this name */
		if (results.length == 0) {
			return ErrorCode.NOT_FOUND;
		} 
		
		/* one result == we have the correct ID */
		else if (results.length == 1) {
			return results[0];
		}
		
		/* multiple results is an error */
		else {
			throw new FatalBuildStoreError("Multiple entries found in packages table, for name " + 
					folderOrPackageName);
		}		
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#removePackage(java.lang.String)
	 */
	@Override
	public int remove(int folderOrPackageId) {
		
		IPackageMemberMgr pkgMemberMgr = buildStore.getPackageMemberMgr();

		/* we can't remove the "<import>" package or the "Root" folder */
		if ((folderOrPackageId == ROOT_FOLDER_ID) || (folderOrPackageId == IMPORT_PACKAGE_ID)) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* determine if this package is used by any files */
		FileSet filesInPackage = pkgMemberMgr.getFilesInPackage(folderOrPackageId);
		if (filesInPackage.size() != 0) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* determine if this package is used by any actions */
		ActionSet actionsInPackage = pkgMemberMgr.getActionsInPackage(folderOrPackageId);
		if (actionsInPackage.size() != 0) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* determine if the folder has any children */
		if (getFolderChildren(folderOrPackageId).length != 0) {
			return ErrorCode.CANT_REMOVE;
		}

		/* remove any associated package roots */
		if (!isFolder(folderOrPackageId)) {
			IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
			int rc = pkgRootMgr.removePackageRoot(folderOrPackageId, IPackageRootMgr.SOURCE_ROOT);
			if (rc != ErrorCode.OK) {
				return rc;
			}
			rc = pkgRootMgr.removePackageRoot(folderOrPackageId, IPackageRootMgr.GENERATED_ROOT);
			if (rc != ErrorCode.OK) {
				return rc;
			}
		}
		
		/* remove from the database */
		int removedCount = 0;
		try {
			removePackageByIdPrepStmt.setInt(1, folderOrPackageId);
			removedCount = db.executePrepUpdate(removePackageByIdPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		if (removedCount == 0) {
			return ErrorCode.NOT_FOUND;
		}
		
		notifyListeners(folderOrPackageId, IPackageMgrListener.REMOVED_PACKAGE);
		return ErrorCode.OK;
	};
	
	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#getPackages()
	 */
	@Override
	public String[] getPackages() {
		
		/* find all the package into our table */
		return db.executePrepSelectStringColumn(findAllPackagesPrepStmt);
	};	
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#getFolderChildren(int)
	 */
	@Override
	public Integer[] getFolderChildren(int folderId) {

		try {
			findChildPackagesPrepStmt.setInt(1, folderId);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		return db.executePrepSelectIntegerColumn(findChildPackagesPrepStmt);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#getParent(int)
	 */
	@Override
	public int getParent(int folderOrPackageId) {

		Integer results[] = null;
		try {
			findPackageParentPrepStmt.setInt(1, folderOrPackageId);
			results = db.executePrepSelectIntegerColumn(findPackageParentPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* If folderOrPackageId is invalid (or duplicated), assume not a folder */
		if (results.length != 1) {
			return ErrorCode.NOT_FOUND;
		}
		
		return results[0];
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#setParent(int, int)
	 */
	@Override
	public int setParent(int folderOrPackageId, int parentId) {
		
		/* validate that the parent and child are valid */
		if (!isValid(parentId) || !isValid(folderOrPackageId)) {
			return ErrorCode.BAD_VALUE;
		}
		
		/* the parent must be a folder */
		if (!isFolder(parentId)) {
			return ErrorCode.NOT_A_DIRECTORY;
		}
		
		/* we can't move the root folder or the <import> package */
		if ((folderOrPackageId == ROOT_FOLDER_ID) || (folderOrPackageId == IMPORT_PACKAGE_ID)) {
			return ErrorCode.BAD_PATH;
		}
				
		/* if the child is a folder, make sure it doesn't create a cycle */
		if (isFolder(folderOrPackageId)) {
			
			/* 
			 * Starting with the new (proposed) parent, work upward to see if the child is already
			 * a parent.
			 */
			int ancestor = parentId;
			while (true) {
				/* a cycle has been detected */
				if (ancestor == folderOrPackageId) {
					return ErrorCode.BAD_PATH;
				}
								
				/* did we reach the root, without a cycle? */
				int nextAncestor = getParent(ancestor);
				if (nextAncestor == ancestor) {
					break;
				}
				ancestor = nextAncestor;
			}
		}
		
		/* update the database entry */
		try {
			updatePackageParentPrepStmt.setInt(1, parentId);
			updatePackageParentPrepStmt.setInt(2, folderOrPackageId);
			db.executePrepUpdate(updatePackageParentPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return ErrorCode.OK;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#isFolder(int)
	 */
	@Override
	public boolean isFolder(int folderOrPackageId) {

		Integer results[] = null;
		try {
			findPackageTypePrepStmt.setInt(1, folderOrPackageId);
			results = db.executePrepSelectIntegerColumn(findPackageTypePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* If folderOrPackageId is invalid (or duplicated), assume not a folder */
		if (results.length != 1) {
			return false;
		}
		
		return (results[0] == 1);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/*
	 * (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#isValid(int)
	 */
	@Override
	public boolean isValid(int folderOrPackageId) {
		/* the ID is valid if we can find its parent */
		return getParent(folderOrPackageId) != ErrorCode.NOT_FOUND;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionTypeMgr#newSlot(int, int, int, int, boolean, java.lang.Object, java.lang.String[])
	 */
	@Override
	public int newSlot(int typeId, String slotName, int slotType, int slotPos,
			int slotCard, Object defaultValue, String[] enumValues) {
		
		/* check for invalid typeId, since SlotMgr can't determine this */
		if (isFolder(typeId) || !isValid(typeId)) {
			return ErrorCode.NOT_FOUND;
		}
		
		/* we can't add input slots to a package */
		if (slotPos == ISlotTypes.SLOT_POS_INPUT) {
			return ErrorCode.INVALID_OP;
		}
		
		/* delegate the rest of the work to SlotMgr */
		return slotMgr.newSlot(SlotMgr.SLOT_OWNER_PACKAGE, typeId, slotName, slotType, slotPos,
								slotCard, defaultValue, enumValues);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#getSlots(int, int)
	 */
	@Override
	public SlotDetails[] getSlots(int pkgId, int slotPos) {
		
		/* validate all inputs */
		if (isFolder(pkgId) || !isValid(pkgId)) {
			return null;
		}
		if ((slotPos < ISlotTypes.SLOT_POS_ANY) || (slotPos > ISlotTypes.SLOT_POS_LOCAL)){
			return null;
		}
		
		/* delegate to SlotMgr */
		return slotMgr.getSlots(SlotMgr.SLOT_OWNER_PACKAGE, pkgId, slotPos);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#getSlotByID(int)
	 */
	@Override
	public SlotDetails getSlotByID(int slotId) {
		return slotMgr.getSlotByID(slotId);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#getSlotByName(int, java.lang.String)
	 */
	@Override
	public SlotDetails getSlotByName(int pkgId, String slotName) {

		/* all work can be delegated */
		return slotMgr.getSlotByName(SlotMgr.SLOT_OWNER_PACKAGE, pkgId, slotName);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#removeSlot(int, int)
	 */
	@Override
	public int removeSlot(int pkgId, int slotId) {
		
		/* all work can be delegated */
		return slotMgr.removeSlot(SlotMgr.SLOT_OWNER_PACKAGE, pkgId, slotId);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#addListener(com.buildml.model.IPackageMgrListener)
	 */
	@Override
	public void addListener(IPackageMgrListener listener) {
		listeners.add(listener);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageMgr#removeListener(com.buildml.model.IPackageMgrListener)
	 */
	@Override
	public void removeListener(IPackageMgrListener listener) {
		listeners.remove(listener);
	};
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * Validate a package's name. Valid characters are digits, letters (upper or lower case),
	 * '-' and '_'. No other characters are permitted. Package names must contain at least
	 * three characters and start with a letter.
	 * 
	 * @param packageName The package name to be validated.
	 * @return True if the name is valid, else false.
	 */
	private boolean isValidName(String packageName) {
		
		if (packageName == null) {
			return false;
		}
		int length = packageName.length();
		if (length < 3) {
			return false;
		}
		
		int i = 0;
		while (i != length) {
			char ch = packageName.charAt(i);
			
			/* first character must be a letter */
			if (i == 0) {
				if (!(Character.isLetter(ch))) {
					return false;
				}	
			} 
			
			/* following characters are letter, digit, _ or - */
			else {
				if (!(Character.isLetterOrDigit(ch) ||
						(ch == '_') || (ch == '-'))){
					return false;
				}
			}
			i++;
		}
		
		return true;
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Helper method for adding a new package or folder to the database. This is called by
	 * addPackage() or addFolder(), both which share the same name space.
	 * 
	 * @param name		Name of the new package or folder.
	 * @param isFolder	True if a folder should be created, else false for a package.
	 * @return			On success, return the package/folder's numeric ID. On failure,
	 *                  return:
	 *                     ErrorCode.ALREADY_USED - The name is already in use.
	 *                     ErrorCode.INVALID_NAME - The name doesn't conform to naming standards.
	 */
	private int addPackageOrFolderHelper(String name, boolean isFolder) {

		/* check that the package/folder doesn't already exist in the database */
		if (getId(name) != ErrorCode.NOT_FOUND){
			return ErrorCode.ALREADY_USED;
		}

		/* validate the new package/folder's name */
		if (!isValidName(name)){
			return ErrorCode.INVALID_NAME;
		}
				
		/* insert the package into our table */
		try {
			addPackagePrepStmt.setInt(1, isFolder ? 1 : 0);
			addPackagePrepStmt.setString(2, name);
			db.executePrepUpdate(addPackagePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* return the new package's ID number */
		int pkgId = db.getLastRowID();
		notifyListeners(pkgId, IPackageMgrListener.ADDED_PACKAGE);
		return pkgId;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Notify any registered listeners about our change in state.
	 * @param pkgId   The package that has changed.
	 * @param how     The way in which the package changed (see {@link IPackageMgrListener}).
	 */
	private void notifyListeners(int pkgId, int how) {
		
		/* 
		 * Make a copy of the listeners list, otherwise a registered listener can't remove
		 * itself from the list within the packageChangeNotification() method.
		 */
		IPackageMgrListener listenerCopy[] = 
				listeners.toArray(new IPackageMgrListener[listeners.size()]);
		for (int i = 0; i < listenerCopy.length; i++) {
			listenerCopy[i].packageChangeNotification(pkgId, how);			
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
}