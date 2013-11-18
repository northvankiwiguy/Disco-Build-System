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
 * The interface conformed-to by any PackageMemberMgr object, which represents a
 * subset of the functionality managed by a BuildStore object. A PackageMemberMgr
 * records which actions, file groups and sub packages belong inside each package.
 * <p>
 * There should be exactly one PackageMemberMgr object per BuildStore object. Use
 * the BuildStore's getPackageMemberMgr() method to obtain that one instance.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface IPackageMemberMgr {

	/**
	 * Numeric constants for type of package member.
	 */
	
	public static final int TYPE_ANY 			= 0;
	public static final int TYPE_FILE 			= 1;
	public static final int TYPE_FILE_GROUP 	= 2;
	public static final int TYPE_ACTION 		= 3;
	public static final int TYPE_SUB_PACKAGE 	= 4;
	
	/**
	 * Numeric constants for each of the scopes.
	 */
	public static final int SCOPE_NONE 			= 0;
	public static final int SCOPE_PRIVATE 		= 1;
	public static final int SCOPE_PUBLIC 		= 2;
	public static final int SCOPE_MAX 			= 2;

	/**
	 * Numeric constants describing the direction of neigbours
	 */
	public static final int NEIGHBOUR_ANY		= 0;
	public static final int NEIGHBOUR_LEFT		= 1;
	public static final int NEIGHBOUR_RIGHT		= 2;
	
	/**
	 * A helper class used to describe the package/scope that a member belongs to.
	 */
	public class PackageDesc {
		public int pkgId;
		public int pkgScopeId;
	}
	
	/**
	 * A helper class used to describe a package member's type, ID, and location.
	 */
	public class MemberDesc {
		public int memberType;
		public int memberId;
		public int x;
		public int y;
	}
	
	/**
	 * A helper class used to describe the location of a package's member.
	 */
	public class MemberLocation {
		public int x;
		public int y;
	}
	
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
	public abstract PackageDesc parsePkgSpec(String pkgSpec);

	/**
	 * Set the package/scope for a specific package member (file, file group, action, sub-package).
	 * 
	 * @param memberType 	The type of the member (MEMBER_TYPE_FILE, etc).
	 * @param memberId		The ID of the member (as defined in fileMgr, actionMgr, etc).
	 * @param pkgId			The package to add the member into.
	 * @param pkgScopeId	The scope (within the package) to add the member into. Only useful for MEMBER_TYPE_FILE.
	 * @return 	ErrorCode.OK on success, ErrorCode.NOT_FOUND if the member isn't defined, ErrorCode.BAD_VALUE
	 *          if the pkgId/pkgScopeId values are wrong, or ErrorCode.OUT_OF_RANGE if the member is not
	 *          within an appropriate range (such as within a package root).
	 */
	public abstract int setPackageOfMember(int memberType, int memberId, int pkgId, int pkgScopeId);
	
	/**
	 * Set the package/scope for a specific package member (file, file group, action, sub-package), using
	 * the scope of SCOPE_NONE.
	 * 
	 * @param memberType 	The type of the member (MEMBER_TYPE_FILE, etc).
	 * @param memberId		The ID of the member (as defined in fileMgr, actionMgr, etc).
	 * @param pkgId			The package to add the member into.
	 * @return 	ErrorCode.OK on success, ErrorCode.NOT_FOUND if the member isn't defined, ErrorCode.BAD_VALUE
	 *          if the pkgId/pkgScopeId values are wrong, or ErrorCode.OUT_OF_RANGE if the member is not
	 *          within an appropriate range (such as within a package root).
	 */
	public abstract int setPackageOfMember(int memberType, int memberId, int pkgId);
	
	/**
	 * Obtain the PackageDesc (package and scope) for the specified member. By default, members
	 * will be in the &lt;import&gt; package.
	 * 
	 * @param memberType 	The type of the member to query (MEMBER_TYPE_FILE, etc).
	 * @param memberId		The ID of the member (as defined in fileMgr, actionMgr, etc).
	 * @return The PackageDesc, or null if any error occurs.
	 */
	public abstract PackageDesc getPackageOfMember(int memberType, int memberId);
	
	/**
	 * Retrieve the list of members in a specific package.
	 * 
	 * @param pkgId				The package to query the members from.
	 * @param pkgScopeId		The scope to search within (SCOPE_NONE for all scopes).
	 * @param memberTypeFilter	The type of member to search for (TYPE_ANY to return all members).
	 * @return An (possibly empty) array of MemberDesc, describing the relevant package members.
	 */
	public abstract MemberDesc[] getMembersInPackage(int pkgId, int pkgScopeId, int memberTypeFilter);
	
	/**
	 * Set the (x, y) location of a package member on the package diagram.
	 * 
	 * @param memberType	The type of the member (MEMBER_TYPE_FILE, etc).
	 * @param memberId		The ID of the member (as defined in fileMgr, actionMgr, etc).
	 * @param x				The member's new X coordinate on the package diagram.
	 * @param y				The member's new Y coordinate on the package diagram.
	 * @return				ErrorCode.OK on success, ErrorCode.NOT_FOUND if the member is not found,
	 * 						or ErrorCode.OUT_OF_RANGE if the (x, y) coordinate is invalid.
	 */
	public abstract int setMemberLocation(int memberType, int memberId, int x, int y);
	
	/**
	 * Retrieve the package member's (x, y) location.
	 * @param memberType	The type of the member (MEMBER_TYPE_FILE, etc).
	 * @param memberId		The ID of the member (as defined in fileMgr, actionMgr, etc).
	 * @return				The member's (x, y) location, or null if memberType/memberId is invalid.
	 */
	public abstract MemberLocation getMemberLocation(int memberType, int memberId);

	/**
	 * Return an array of neighbours for the specified package member. A neighbour is defined
	 * as any package member that is connected to this member via a connection arrow. The order
	 * in which neighbours are returned is unspecified.
	 * 
	 * @param memberType	The type of this member (e.g. TYPE_FILE).
	 * @param memberId		The ID of this member.
	 * @param direction		NEIGHBOUR_LEFT, NEIGHBOUR_RIGHT or NEIGHBOUR_EITHER.
	 * @param showFilters	If true, return filter file groups, otherwise skip over them 
	 * @return A (possibly empty) array of MemberDesc, describing this member's connected
	 * neighbours. Returns null if any of the input parameters are invalid.
	 */
	public abstract MemberDesc[] getNeighboursOf(
			int memberType, int memberId, int direction, boolean showFilters);
	
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

	/**
	 * Add the specified listener to the list of objects that are notified when
	 * a package's membership changes in some way.
	 * 
	 * @param listener The object to be added as a listener.
	 */
	public void addListener(IPackageMemberMgrListener listener);

	/**
	 * Remove the specified listener from the list of objects to be notified when
	 * a package's membership changes in some way.
	 * 
	 * @param listener The object to be removed from the list of listeners.
	 */
	public void removeListener(IPackageMemberMgrListener listener);
}