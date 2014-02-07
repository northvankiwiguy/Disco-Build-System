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

package com.buildml.model;

import com.buildml.model.ISlotTypes.SlotDetails;

/**
 * The interface conformed-to by any PackageMgr object, which represents a
 * subset of the functionality managed by a BuildStore object. A PackageMgr
 * deals with all information related to grouping files and actions
 * into packages.
 * <p>
 * There should be exactly one PackageMgr object per BuildStore object. Use
 * the BuildStore's getPackageMgr() method to obtain that one instance.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface IPackageMgr {

	/**
	 * @return The ID of the root folder. This is the special folder that always exists
	 * (can't be deleted). Newly created folders and packages will be inserted underneath
	 * this folder.
	 */
	public abstract int getRootFolder();
	
	/**
	 * @return The ID of the &lt;import%gt; package. This is the default package for
	 * newly imported files. 
	 */
	public abstract int getImportPackage();
	
	/**
	 * @return The ID of the "Main" package. This is the top level package diagram.
	 */
	public abstract int getMainPackage();

	/**
	 * Add a new package to the BuildStore. By default, packages are added immediately
	 * beneath the root folder.
	 * 
	 * @param packageName The name of the new package to be added.
	 * @return The package's ID if the addition was successful, ErrorCode.INVALID_NAME
	 * if the package's name isn't valid, or ErrorCode.ALREADY_USED if the package 
	 * name is already in use (packages and folders share a name space).
	 */
	public abstract int addPackage(String packageName);

	/**
	 * Add a new folder to the BuildStore. By default, folders are added immediately
	 * beneath the root folder.
	 * 
	 * @param folderName The name of the new folder to be added.
	 * @return The folder's ID if the addition was successful, ErrorCode.INVALID_NAME
	 * if the folder's name isn't valid, or ErrorCode.ALREADY_USED if the folder's 
	 * name is already in use (packages and folders share a name space).
	 */	
	public abstract int addFolder(String folderName);
	
	/**
	 * Given an ID number, return the package or folder's name.
	 * 
	 * @param folderOrPackageId The package or folder's ID number.
	 * @return The package or folder's name, or null if the ID is invalid.
	 */
	public abstract String getName(int folderOrPackageId);
	
	/**
	 * Set the name of the folder or package. The name must be unique across
	 * all folders/packages.
	 * 
	 * @param folderOrPackageId		ID of the package or folder to rename.
	 * @param newName				The new name for the package or folder.
	 * @return ErrorCode.OK on success, ErrorCode.ALREADY_USED if the name is already
	 * use by another package or folder, ErrorCode.INVALID_NAME if the name doesn't
	 * match naming standards, and ErrorCode.NOT_FOUND if the folderOrPackageId value
	 * doesn't reference a valid package/folder.
	 */
	public abstract int setName(int folderOrPackageId, String newName);
	
	/**
	 * Given a package or folder's name, return its ID number.
	 * 
	 * @param folderOrPackageName The package or folder name.
	 * @return The package's ID number, ErrorCode.NOT_FOUND if there's no package or
	 * folder with this name.
	 */
	public abstract int getId(String folderOrPackageName);

	/**
	 * Remove the specified package from the BuildStore. The package can only be removed
	 * if there are no files or actions associated with it. A folder can only be removed
	 * if it doesn't have any child packages or folders.
	 * 
	 * @param folderOrPackageId The ID of the folder or package to be removed.
	 * @return ErrorCode.OK if the package was successfully removed, ErrorCode.CANT_REMOVE
	 * if the package is still in use, and ErrorCode.NOT_FOUND if there's no package
	 * with this name.
	 */
	public abstract int remove(int folderOrPackageId);

	/**
	 * Return an alphabetically sorted array of all the packages. The case (upper versus
	 * lower) is ignored when sorting the results. This list does not include folder
	 * names.
	 * 
	 * @return A non-empty array of package names (will always contain the "<import>" package).
	 */
	public abstract String[] getPackages();
		
	/**
	 * Obtain a list of all child folders and packages that reside immediately beneath
	 * the specified folder ID.
	 * 
	 * @param folderId The parent folder ID we are querying.
	 * @return An array of the folder's children, with folders listed first, followed by
	 * packages. The folder and package lists are both sorted alphabetically.
	 */
	public abstract Integer[] getFolderChildren(int folderId);
	
	/**
	 * Obtain the ID of the folder that contains the specified package/folder. The
	 * parent of the root folder is itself.
	 * 
	 * @param folderOrPackageId  The package or folder to find the parent of.
	 * @return The folder ID of the specified package/folder, or ErrorCode.NOT_FOUND
	 * if the folderOrPackageId doesn't refer to a valid package or folder.
	 */
	public abstract int getParent(int folderOrPackageId);
	
	/**
	 * Move the specified package or folder into a new parent folder. When moving a folder,
	 * it is important that the new parent is not the same as, or a descendant of the 
	 * folder being moved (this would cause a cycle). 
	 * 
	 * @param folderOrPackageId The folder or package for which the parent is being set.
	 * @param parentId The parent folder into which the folder/package will be moved.
	 * @return ErrorCode.OK on success, or one of the following errors:
	 *         ErrorCode.BAD_VALUE - one or both of the IDs is not valid.
	 *         ErrorCode.NOT_A_DIRECTORY - the parentId does not refer to a folder.
	 *         ErrorCode.BAD_PATH - The destination for the package/folder is invalid,
	 *         possibly because a cycle would be created.
	 */
	public abstract int setParent(int folderOrPackageId, int parentId);
	
	/**
	 * Indicates whether the folder or package ID refers to a folder, or a package.
     *
	 * @param folderOrPackageId The ID to query.
	 * @return True if the ID relates to a folder, else false.
	 */
	public abstract boolean isFolder(int folderOrPackageId);
	
	/**
	 * Indicates whether the ID refers to a valid package or folder.
	 *
     * @param folderOrPackageId The ID to query.
	 * @return True if the ID refers to a valid package or folder.
	 */
	public boolean isValid(int folderOrPackageId);
	
	/**
	 * Add a new slot to this package.
	 * 
	 * @param typeId		The package to add the slot to.
	 * @param slotName		The name of the slot (must be unique within this package).
	 * @param slotDescr		A textual description of the slot (can be long and multi-line).
	 * @param slotType		The slot's type (SLOT_TYPE_FILEGROUP, etc).
	 * @param slotPos		The slot's position (SLOT_POS_OUTPUT, etc).
	 * @param cardinality	Either SLOT_CARD_OPTIONAL or SLOT_CARD_REQUIRED.
	 * @param defaultValue	A default value to be used when a SLOT_CARD_OPTIONAL slot is empty.
	 * @param enumValues	For SLOT_TYPE_ENUMERATION, an array of valid values.
	 * @return The newly-added slot ID, or:
	 * 			ErrorCode.NOT_FOUND if typeId is invalid, or is a folder.
	 * 			ErrorCode.INVALID_NAME if slotName is not a valid slot identifier.
	 * 			ErrorCode.ALREADY_USED if slotName is already in use (for this package).
	 * 			ErrorCode.INVALID_OP if slotType or slotPos are not valid/relevant, or
	 *                    if enumValues does not contain a valid enumeration.
	 * 			ErrorCode.BAD_VALUE if the default value is not valid for this type.
	 */
	public abstract int newSlot(int typeId, String slotName, String slotDescr, 
								int slotType, int slotPos, int cardinality, 
								Object defaultValue, String[] enumValues);

	/**
	 * Change the details of an existing slot. Although many of the slot's details can
	 * be changed, this is not true for the slotPos and slotType fields which must
	 * remain unmodified.
	 *
	 * @param details		The modified SlotDetails record.
	 * 
	 * @return ErrorCode.OK on success
	 * 		   ErrorCode.INVALID_NAME if details.slotName is not a valid slot identifier.
	 * 		   ErrorCode.ALREADY_USED if details.slotName is already in use (within this package).
	 * 		   ErrorCode.INVALID_OP if details.slotType or details.slotPos have changed.
	 *         ErrorCode.OUT_OF_RANGE if details.slotCard is invalid.
	 * 		   ErrorCode.BAD_VALUE if the default value is not valid for this slot's type.
	 * 		   ErrorCode.NOT_FOUND if details.slotId doesn't refer to an existing slotId.
	 */
	public int changeSlot(SlotDetails details);
	
	/**
	 * Return all the slots associated with a package.
	 * 
	 * @param pkgId		The ID of the package containing the slots.
	 * @param slotPos	The position (within the package) of the slot (SLOT_POS_OUTPUT, etc).
	 * @return An array of slot details, or null if typeId or slotPos is invalid, or typeId
	 *         relates to a folder.
	 */
	public abstract SlotDetails[] getSlots(int pkgId, int slotPos);

	/**
	 * Return a slot's detailed information.
	 * 
	 * @param slotId The slot to query.
	 * @return A SlotDetails structure containing the specified slot's details, or null if
	 * 		   slotId does not refer to a valid slot.
	 */
	public abstract SlotDetails getSlotByID(int slotId);
	
	/**
	 * For the specified package, return details of the named slot.
	 * 
	 * @param pkgId		The ID of the package containing the slot.
	 * @param slotName	The name of the slot (within the scope of the package).
	 * @return The slot details, or null if pkgId or slotName is invalid.
	 */
	public abstract SlotDetails getSlotByName(int pkgId, String slotName);
	
	/**
	 * Remove a slot from the package. The slot can only be removed if there are no
	 * package instances that define the slot value.
	 * 
	 * @param slotId	The ID of the slot to be removed.
	 * @return ErrorCode.OK on success,
	 * 		   ErrorCode.NOT_FOUND if slotId is invalid, or
	 * 		   ErrorCode.CANT_REMOVE if the slot is still in use by a package instance.
	 */
	public abstract int trashSlot(int slotId);
	
	/**
	 * Revive a slot that had previously been trashed.
	 * 
	 * @param slotId  The ID of the slot to be revived.
	 * @return ErrorCode.OK on success,
	 * 		   ErrorCode.NOT_FOUND if slotId is invalid, or
	 * 		   ErrorCode.CANT_REVIVE if this slotId isn't actually trashed.
	 */
	public abstract int reviveSlot(int slotId);
	
	/**
	 * Add the specified listener to the list of objects that are notified when
	 * a package changes in some way.
	 * 
	 * @param listener The object to be added as a listener.
	 */
	public void addListener(IPackageMgrListener listener);

	/**
	 * Remove the specified listener from the list of objects to be notified when
	 * a package changes in some way.
	 * 
	 * @param listener The object to be removed from the list of listeners.
	 */
	public void removeListener(IPackageMgrListener listener);
}