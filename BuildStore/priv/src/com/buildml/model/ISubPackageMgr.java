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
	
}

