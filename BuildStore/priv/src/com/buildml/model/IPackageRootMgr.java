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

/**
 * A BuildStore manager that encapsulates information about package roots,
 * and their mapping to the underlying native file system.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface IPackageRootMgr {

	/** Used to identify a package's "source" root */
	public static final int SOURCE_ROOT = 1;

	/** Used to identify a package's "generated" root */
	public static final int GENERATED_ROOT = 2;
	
	/**
	 * Specify the location of the workspace root, within the BuildML
	 * build tree. The workspace root must be further up the tree than
	 * any package roots, or the BuildML database file.
	 * 
	 * @param pathId The ID of the path (directory) to set as the workspace
	 *        root.
	 * @return ErrorCode.OK on success, ErrorCode.BAD_PATH if the
	 *         pathId is invalid, ErrorCode.NOT_A_DIRECTORY if it's not a 
	 *         directory, or ErrorCode.OUT_OF_RANGE if any package root, or 
	 *         the BuildML database file, would not be encompassed by the
	 *         new root.
	 */
	public int setWorkspaceRoot(int pathId);
	
	/**
	 * @return The ID of the path that is currently set as the workspace root
	 * or ErrorCode.NOT_FOUND if a root hasn't yet been set.
	 */
	public int getWorkspaceRoot();
	
	/**
	 * Set (override) the native path associated with the workspace root. When
	 * file are actually accessed on the file system, this will be done so using
	 * the nativePath-relative location, making it possible to move the files
	 * to a new location, yet still use the same BuildML database content.
	 * 
	 * @param nativePath The new setting for the 
	 * @return ErrorCode.OK on success, or ErrorCode.NOT_A_DIRECTORY if the
	 *         specified path is not a valid native directory.
	 */
	public int setWorkspaceRootNative(String nativePath);
	
	/**
	 * Return the native path of the workspace root. Unless overridden by a call
	 * to setWorkspaceRootNative, the path will be calculated as being "depth"
	 * levels above the location of the current BuildML database file. "depth"
	 * is defined by a call to setBuildMLFileDepth().
	 * 
	 * @return The native path of the workspace root.
	 */
	public String getWorkspaceRootNative();
		
	/**
	 * Specify how many directory levels exist between the workspace root
	 * and this BuildML database file. This "depth" value is persisted
	 * in the database and can be used to reverse-engineer the native 
	 * workspace root, based on the native path to the database file.
	 *
	 * @param depth  The distance between the workspace root and the current 
	 * 				 build.bml file.
	 * @return ErrorCode.OK on success, or ErrorCode.BAD_PATH if the depth
	 * 	       is invalid (the workspace would be above the file system root).
	 */
	public int setBuildMLFileDepth(int depth);

	/**
	 * Set the default (persistent) source or generated root for a particular package.
	 * The root path is specified as a pathId (from FileMgr) that is enclosed
	 * within the workspace root.
	 * 
	 * @param packageId  The ID of the package to be modified.
	 * @param type		 Either IPackageRootMgr.SOURCE_ROOT or 
	 *                   IPackageRootMgr.GENERATED_ROOT.
	 * @param pathId     The ID of the path to the package root. Must be within
	 *                   the enclosing workspace root.
	 * @return ErrorCode.OK on success, ErrorCode.BAD_PATH if the pathId is not
	 *         valid, ErrorCode.NOT_A_DIRECTORY is it's not a directory,
	 *         ErrorCode.NOT_FOUND if packageId or type is invalid, or
	 *         ErrorCode.OUT_OF_RANGE if the path is not enclosed within the
	 *         workspace root.
	 */
	public int setPackageRoot(int packageId, int type, int pathId);
	
	/**
	 * Fetch the pathId (from FileMgr) that's associated with the specified
	 * package root.
	 * 
	 * @param packageId  The ID of the package to be modified.
	 * @param type		 Either IPackageRootMgr.SOURCE_ROOT or 
	 *                   IPackageRootMgr.GENERATED_ROOT.
	 * @return The pathId, ErrorCode.NOT_FOUND if the packageId or type is invalid.
	 */
	public int getPackageRoot(int packageId, int type);
	
	/**
	 * Remove the specified package root.
	 * 
	 * @param packageId  The ID of the package to be removed.
	 * @param type		 Either IPackageRootMgr.SOURCE_ROOT or 
	 *                   IPackageRootMgr.GENERATED_ROOT.
	 * @return ErrorCode.OK on success or ErrorCode.NOT_FOUND if the packageId or 
	 *         type is invalid.
	 */
	public int removePackageRoot(int packageId, int type);
	
	/**
	 * Compute the name of a package root, based on the packageId and type (SOURCE_ROOT
	 * or GENERATED_ROOT). For example, the "libz" package will have two roots: "libz_src"
	 * and "libz_gen".
	 * 
	 * @param packageId The ID of the package.
	 * @param type		The root type (SOURCE_ROOT or GENERATED_ROOT).
	 * @return			The corresponding root name, or null if either input parameter is invalid
	 * 					(including if packageId relates to a folder, or the <import> package).
	 */
	public String getPackageRootName(int packageId, int type);
	
	/**
	 * Given a root name, return the associated path ID.
	 * 
	 * @param rootName Name of the root to search for. 
	 * @return The pathId associated with the root, or ErrorCode.NOT_FOUND if the root name
	 *         is invalid.
	 */
	public int getRootPath(String rootName);
	
	/**
	 * Set the temporary source or generated root for a particular package. This method
	 * is similar to setDefaultPackageRoot but is specified as a native file system
	 * path and is not persisted in the database. It is therefore used for overriding
	 * the default package root in a particular BuildStore instance (but not in the 
	 * build.bml file).
	 * 
	 * The path may either be absolute or relative to the workspace root. Given that
	 * this root is temporary (non-persistent), the use of absolute paths is expected
	 * to be quite common.
	 * 
	 * @param packageId  The ID of the package to be modified. 
	 * @param type		 Either IPackageRootMgr.SOURCE_ROOT or 
	 *                   IPackageRootMgr.GENERATED_ROOT.
	 * @param path       The path to the package root. If this is an absolute path,
	 *                   it maps to the absolute path on the native file system. If
	 *                   it's relative (not starting with /), it'll be relative to
	 *                   the workspace root.
	 * @return ErrorCode.OK on success, ErrorCode.NOT_FOUND if the packageId is not
	 *         valid, ErrorCode.BAD_VALUE if type is invalid, or ErrorCode.BAD_PATH
	 *         if the path does not exist on the native file system.
	 */
	public int setPackageRootNative(int packageId, int type, String path);
	
	/**
	 * Remove the previous root, as set by overridePackageRoot(). This only affects
	 * the temporary root setting and therefore does not remove the persistent
	 * value that was set by setPackageRoot().
	 * 
	 * @param packageId  The ID of the package to be modified. 
	 * @param type       Either IPackageRootMgr.SOURCE_ROOT or 
	 *                   IPackageRootMgr.GENERATED_ROOT.
	 * @return ErrorCode.OK on success, ErrorCode.NOT_FOUND if the packageId is not
	 *         valid, or ErrorCode.BAD_VALUE if type is invalid.
	 */
	public int clearPackageRootNative(int packageId, int type);
	
	/**
	 * Retrieve the source or generate root for the specified package, as a
	 * native path. 
	 * 
	 * This will return the value last set by the overridePackageRoot()
	 * method, although if no such root has been set in the current instance of the
	 * BuildStore, the persistent value from setPackageRoot() will instead be
	 * returned. Finally, if no root has been set, the current workspace root will be
	 * returned.
	 * 
	 * @param packageId  The ID of the package whose root should be retrieved.
	 * @param type       Either IPackageRootMgr.SOURCE_ROOT or 
	 *                   IPackageRootMgr.GENERATED_ROOT.
	 * @return The native path to the package's root.
	 */
	public String getPackageRootNative(int packageId, int type);

	/**
	 * Retrieve the specified root, as a native path. This will return the value last
	 * set by the overridePackageRoot() method, although if no such root has been set
	 * in the current instance of the BuildStore, the persistent value from
	 * setPackageRoot() will instead be returned. Finally, if no root has been set,
	 * the current workspace root will be returned.
	 * 
	 * In addition to package-specific roots, this method can also be used to retrieve
	 * the default "root" and "workspace" roots.
	 * 
	 * @param rootName The textual name of the root.
	 * @return The path to the package's root.
	 */
	public String getRootNative(String rootName);

	/**
	 * Return an array of all root names that are currently valid. The list is returned
	 * in alphabetical order. In addition to the standard "root" and "workspace" roots,
	 * there will also be two roots for each package. For example, for "pkgA", there
	 * will exist "pkgA_src" and "pkgA_gen" roots.
	 * 
	 * @return A String array of root names.
	 */
	public String[] getRoots();
		
	/**
	 * If this path has one or more roots attached to it, return the root names
	 * (in alphabetical order).
	 * 
	 * @param pathId The ID of the path we're querying.
	 * @return The names of the associated roots (possibly empty).
	 */
	public String[] getRootsAtPath(int pathId);
}
