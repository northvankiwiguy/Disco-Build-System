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
	 * Remove a sub-package from the system (and from the parent package it's a member of).
	 * 
	 * @param subPkgId  ID of the sub-package to remove from the system.
	 * @return ErrorCode.OK on success, or ErrorCode.NOT_FOUND if subPkgId is invalid.
	 */
	public int removeSubPackage(int subPkgId);
	
	/**
	 * Return the type of the sub-package. That is, which package (via IPackageMgr) is
	 * this sub-package an instance of.
	 * 
	 * @param subPkgId ID of the sub-package to return the type of.
	 * @return The package type ID, or ErrorCode.NOT_FOUND if subPkgId is invalid.
	 */
	public int getSubPackageType(int subPkgId);
}

