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

import com.buildml.model.types.FileSet;
import com.buildml.model.types.ActionSet;

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
	 * Numeric constants for each of the scopes.
	 */
	public static final int SCOPE_NONE = 0;
	public static final int SCOPE_PRIVATE = 1;
	public static final int SCOPE_PUBLIC = 2;
	public static final int SCOPE_MAX = 2;

	/**
	 * Add a new package to the BuildStore.
	 * 
	 * @param packageName The name of the new package to be added.
	 * @return The package's ID if the addition was successful, ErrorCode.INVALID_NAME
	 * if the package's name isn't valid, or ErrorCode.ALREADY_USED if the package 
	 * name is already in the BuildStore.
	 */
	public abstract int addPackage(String packageName);

	/**
	 * Given a package's ID number, return the package's name.
	 * 
	 * @param packageId The package's ID number.
	 * @return The package's name, or null if the package ID is invalid.
	 */
	public abstract String getPackageName(int packageId);

	/**
	 * Given a package's name, return its ID number.
	 * 
	 * @param packageName The package's name.
	 * @return The package's ID number, ErrorCode.NOT_FOUND if there's no package
	 * with this name.
	 */
	public abstract int getPackageId(String packageName);

	/**
	 * Remove the specified package from the BuildStore. The package can only be removed
	 * if there are no files or actions associated with the package.
	 * 
	 * @param packageName The name of the package to be removed.
	 * @return ErrorCode.OK if the package was successfully removed, ErrorCode.CANT_REMOVE
	 * if the package is still in use, and ErrorCode.NOT_FOUND if there's no package
	 * with this name.
	 */
	public abstract int removePackage(String packageName);

	/**
	 * Return an alphabetically sorted array of all the packages. The case (upper versus
	 * lower) is ignored when sorting the results.
	 * 
	 * @return A non-empty array of package names (will always contain the "None" package).
	 */
	public abstract String[] getPackages();

	/**
	 * Given a scope's ID number, return the corresponding scope name.
	 * 
	 * @param id The scope's ID number.
	 * @return The scope's name, or null if the ID number is invalid.
	 */
	public abstract String getScopeName(int id);

	/**
	 * Given a scope's name, return its ID number. There can be many names for the same
	 * scope, so there isn't a 1:1 mapping of names to IDs. For example, both "private" and
	 * "priv" will return the same ID number.
	 * 
	 * @param name The scope's name.
	 * @return The scope's ID number, or ErrorCode.NOT_FOUND if the scope name isn't valid.
	 */
	public abstract int getScopeId(String name);

	/**
	 * Parse a package specification string, and return the ID of the package and
	 * (optionally) the ID of the scope within that package. The syntax of the package
	 * spec must be of the form:
	 *  <ol>
	 * 	  <li>&lt;pkg-name&gt;</li>
	 * 	  <li>&lt;pkg-name&gt;/&lt;scope-name&gt;</li>
	 *  </ol>
	 * That is, the scope name is optional.
	 * 
	 * @param pkgSpec The package specification string.
	 * @return An Integer[2] array, where [0] is the package's ID and [1] is the scope
	 * ID. If either portion of the pkgSpec was invalid (not a registered package or scope),
	 * the ID for that portion will be ErrorCode.NOT_FOUND. If there was no scope name
	 * specified, the scope ID will be 0, which represents the "None" scope.
	 */
	public abstract Integer[] parsePkgSpec(String pkgSpec);

	/**
	 * Set the package/scope associated with this path.
	 * 
	 * @param fileId The ID of the file whose package will be set.
	 * @param pkgId The ID of the package to be associated with this file.
	 * @param pkgScopeId The ID of the package's scope.
	 * @return ErrorCode.OK on success, or ErrorCode.NOT_FOUND if this file doesn't exist
	 */
	public abstract int setFilePackage(int fileId, int pkgId, int pkgScopeId);

	/**
	 * Get the package/scope associated with this path.
	 * 
	 * @param fileId The ID of the path whose package we wish to know.
	 * @return A Integer[2] array where [0] is the package ID, and [1] is the scope ID,
	 * or null if the file doesn't exist.
	 */
	public abstract Integer[] getFilePackage(int fileId);

	/**
	 * Return the set of files that are within the specified package (any scope).
	 * 
	 * @param pkgId The ID of the package we're examining.
	 * @return The set of files that reside inside that package.
	 */
	public abstract FileSet getFilesInPackage(int pkgId);

	/**
	 * Return the set of files that are within the specified package/scope.
	 * 
	 * @param pkgId The ID of the package we're examining.
	 * @param pkgScopeId The scope within the package we're interested in.
	 * @return The set of files that reside inside that package/scope.
	 */
	public abstract FileSet getFilesInPackage(int pkgId, int pkgScopeId);

	/**
	 * Return the set of files that are within the specified package/scope, given a
	 * string specification of the package/scope.
	 * 
	 * @param pkgSpec The package name, or the package/scope name.
	 * @return The FileSet of all files in this package or package/scope, or null if
	 * there's a an error in parsing the pkg/spec name.
	 */
	public abstract FileSet getFilesInPackage(String pkgSpec);

	/**
	 * Return the set of files that are outside the specified package.
	 * 
	 * @param pkgId The ID of the package we're examining.
	 * @return The set of files that reside outside that package.
	 */
	public abstract FileSet getFilesOutsidePackage(int pkgId);

	/**
	 * Return the set of files that are outside the specified package/scope.
	 * 
	 * @param pkgId The ID of the package we're examining.
	 * @param pkgScopeId The ID of the scope within the package we're interested in.
	 * @return The set of files that reside outside that package/scope.
	 */
	public abstract FileSet getFilesOutsidePackage(int pkgId, int pkgScopeId);

	/**
	 * Returns the set of files that fall outside of the package boundaries, using a string
	 * to specify the package/scope.
	 * 
	 * @param pkgSpec The package name, or the package/scope name.
	 * @return The FileSet of all files outside of this package or package/scope, or null if
	 * there's a an error in parsing the pkg/spec name.
	 */
	public abstract FileSet getFilesOutsidePackage(String pkgSpec);

	/**
	 * Set the package associated with this action.
	 * 
	 * @param actionId The ID of the action whose package will be set.
	 * @param pkgId The ID of the package to be associated with this action.
	 * @return ErrorCode.OK on success, or ErrorCode.NOT_FOUND if this action doesn't exist
	 */
	public abstract int setActionPackage(int actionId, int pkgId);

	/**
	 * Get the package associated with this action.
	 * 
	 * @param actionId The ID of the action whose package we're interested in.
	 * @return The action's package, or ErrorCode.NOT_FOUND if the action doesn't exist.
	 */
	public abstract int getActionPackage(int actionId);

	/**
	 * Return the set of actions that are within the specified package.
	 * 
	 * @param pkgId The package we're examining.
	 * @return The set of actions that reside inside that package.
	 */
	public abstract ActionSet getActionsInPackage(int pkgId);

	/**
	 * Return the set of actions that are within the specified package.
	 * 
	 * @param pkgSpec The name of the package to query.
	 * @return The set of actions that reside inside that package, null if the
	 * package name is invalid.
	 */
	public abstract ActionSet getActionsInPackage(String pkgSpec);

	/**
	 * Return the set of actions that are outside the specified package.
	 * 
	 * @param pkgId The package we're examining.
	 * @return The set of actions that reside outside that package.
	 */
	public abstract ActionSet getActionsOutsidePackage(int pkgId);

	/**
	 * Return the set of actions that are outside the specified package.
	 * 
	 * @param pkgSpec The name of the package to query.
	 * @return An array of actions that reside outside that package, null if the
	 * package name is invalid.
	 */
	public abstract ActionSet getActionsOutsidePackage(String pkgSpec);

}