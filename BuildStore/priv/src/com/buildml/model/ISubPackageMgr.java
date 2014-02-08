/*******************************************************************************
 * Copyright (c) 2014 Arapiki Solutions Inc.
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

/**
 * The interface conformed-to by any SubPackageMgr object, which represents a
 * subset of the functionality managed by a BuildStore object. A SubPackageMgr
 * records which sub-packages (or "package instances") are defined in the system,
 * and what type of package they represent. That is, a sub-package is an instance
 * of a package that itself appears within a package diagram.
 * <p>
 * There should be exactly one SubPackageMgr object per BuildStore object. Use
 * the BuildStore's getSubPackageMgr() method to obtain that one instance.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface ISubPackageMgr {

	/**
	 * Create a new sub-package (which is an instance of package type pkgTypeId)
	 * and place it as a child of the parent package (parentPkgId).
	 * 
	 * @param parentPkgId	ID of the parent package to add the sub-package into.
	 * @param pkgTypeId		ID of the package that this sub-package will be new instance of.
	 * 
	 * @return The newly-create sub-package ID, or one of the following errors:
	 *   ErrorCode.NOT_FOUND     - The pkgTypeId parameter is invalid.
	 *   ErrorCode.BAD_VALUE     - The parentPkgId parameter is invalid.
	 *   ErrorCode.LOOP_DETECTED - The pkgTypeId package is the same as, or is an ancestor of 
	 *                             parentPkgId, which is not allowed as it would create
	 *                             a cycle in the package hierarchy.                      
	 */
	public int newSubPackage(int parentPkgId, int pkgTypeId);
	
	/**
	 * Return the type of the sub-package. That is, which package (via IPackageMgr) is
	 * this sub-package an instance of.
	 * 
	 * @param subPkgId ID of the sub-package to return the type of.
	 * @return The package type ID, or ErrorCode.NOT_FOUND if subPkgId is invalid.
	 */
	public int getSubPackageType(int subPkgId);
	
	/**
	 * Indicates whether the specified sub-package ID refers to a valid sub-package
	 * (Note that trashed sub-packages will also be considered valid).
	 * 
	 * @param subPkgId	ID of the sub-package to query for. 
	 * @return True if the sub-package is valid, else false.
	 */
	public boolean isSubPackageValid(int subPkgId);
	
	/**
	 * Remove a specific sub-package from the build store. This operation can be only
	 * be performed on sub-packages that are unused. Trashing a sub-package only marks
	 * it as "deleted", but doesn't remove it from the database, so it can still be revived.
	 * 
	 * @param subPkgId The ID of the sub-package to be move to the trash.
	 * 
	 * @return ErrorCode.OK on successful removal, ErrorCode.NOT_FOUND if subPkgId is invalid
	 * or ErrorCode.CANT_REMOVE if the sub-package is still used in some way.
	 */
	public int moveSubPackageToTrash(int subPkgId);
	
	/**
	 * Revive a sub-package that had previously been deleted by the moveSubPackageToTrash() method.
     *
	 * @param subPkgId The ID of the sub-package to be revived.
	 * @return ErrorCode.OK on successful revival, or ErrorCode.CANT_REVIVE if
	 * for some reason the sub-package can't be revived.
	 */
	public int reviveSubPackageFromTrash(int subPkgId);
	
	/**
	 * Determine whether an sub-package is currently marked as "trash" (to be deleted
	 * when the BuildStore is next closed).
	 * 
	 * @param subPkgId The ID of the sub-package we are querying.
	 * @return true if the sub-package has been marked as trash, else false.
	 */
	public boolean isSubPackageTrashed(int subPkgId);
	
	/**
	 * For this specific sub-package, return the ID of the named slot. This is simply a short-cut
	 * approach, instead of getting the slotID from the IPackageMgr.
	 * 
	 * @param subPkgId		The sub-package that contains the slot.
	 * @param slotName		The name of the slot.
	 * @return The slot's ID, or ErrorCode.NOT_FOUND if the sub-package is invalid, or the slot
	 * name is not valid for this sub-package.
	 */
	public abstract int getSlotByName(int subPkgId, String slotName);
	
	/**
	 * For the specified sub-package, set the specified slot to the given value.
	 * 
	 * @param subPkgId 	The sub-package that the slot is attached to.
	 * @param slotId   	The slot that's connected to the action.
	 * @param value	   	The new value to be set (typically an Integer or String).
	 * @return ErrorCode.OK on success, ErrorCode.NOT_FOUND if subPkgId is invalid, slotId
	 *         is invalid, or slotId isn't attached to subPkgId, ErrorCode.BAD_VALUE if
	 *         the value can't be assigned to the specified slot.
	 */
	public abstract int setSlotValue(int subPkgId, int slotId, Object value);
	
	/**
	 * For the specified sub-package, retrieve the specified slot's value. If the value
	 * has not been explicitly set for this sub-package, the slot default value will be returned.
	 * 
	 * @param subPkgId	The sub-package that the slot is attached to.
	 * @param slotId	The slot that's connected to the sub-package.
	 * @return The slot's value (typically Integer or String), or null if subPkgId/slotId can't
	 * be mapped to a valid slot.
	 */
	public abstract Object getSlotValue(int subPkgId, int slotId);	
	
	/**
	 * Determine whether the specified slot currently holds a value.
	 * @param subPkgId	The sub-package that the slot is attached to.
	 * @param slotId	The slot that's connected to the sub-package.
	 * @return True if there's an explicit (non-default) value in this slot, else false.
	 *		   Also return false if subPkgId/slotId are invalid.
	 */
	public abstract boolean isSlotSet(int subPkgId, int slotId);
	
	/**
	 * Remove the value (if any) that has been inserted into this slot, therefore setting
	 * this slot to its default value. If subPkgId or slotId is invalid, silently do nothing.
	 * @param subPkgId	The sub-package that the slot is attached to.
	 * @param slotId	The slot that's connected to the sub-package.
	 */
	public abstract void clearSlotValue(int subPkgId, int slotId);
	
	/**
	 * Add the specified listener to the list of objects that are notified when
	 * a sub-package changes in some way.
	 * 
	 * @param listener The object to be added as a listener.
	 */
	public void addListener(ISubPackageMgrListener listener);

	/**
	 * Remove the specified listener from the list of objects to be notified when
	 * a sub-package changes in some way.
	 * 
	 * @param listener The object to be removed from the list of listeners.
	 */
	public void removeListener(ISubPackageMgrListener listener);
}

