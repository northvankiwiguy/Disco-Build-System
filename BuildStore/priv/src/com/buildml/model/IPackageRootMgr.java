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
	public static final int GENERATED_ROOT = 1;
	
	/**
	 * Set the location of the workspace root. This is done by specifying the
	 * number of parent directories between the location of the current 
	 * build.bml file and the workspace root.
	 * 
	 * That is, if the build.bml file is in /a/b/c/d/build.bml, and the
	 * workspace root is at /a/b/, pass a value of 2 to this method. 
	 * 
	 * This setting is persistent (stored in the database) and allows all files
	 * in the BuildStore to be stored as relative paths, without any absolute 
	 * (machine-specific) paths involved.
	 *
	 * @param distance  The distance between the workspace and the current 
	 * 					build.bml file.
	 * @return ErrorCode.OK on success or ErrorCode.BAD_PATH if the distance
	 *         is invalid (too large or too small).
	 */
	public int setRelativeWorkspace(int distance);

	/**
	 * Return the distance to the current workspace root, as previously set
	 * by a call to setRelativeWorkspace(). If no call has yet been made
	 * to setRelativeWorkspace, the directory containing the build.bml
	 * file will be given (value of 0).
	 * 
	 * @return The distance between build.bml and the workspace root.
	 */
	public int getRelativeWorkspace();

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
	 * @return ErrorCode.OK on success, ErrorCode.NOT_FOUND if the packageId is not
	 *         valid, ErrorCode.BAD_VALUE if type is invalid, or ErrorCode.BAD_PATH
	 *         if the path is not enclosed within the workspace root.
	 */
	public int setPackageRoot(int packageId, int type, String pathId);
	
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
	public int setRootMapping(int packageId, int type, String path);
	
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
	public int clearRootMapping(int packageId, int type);
	
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
	public String getRootNativePath(int packageId, int type);

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
	public String getRootNativePath(String rootName);

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
	 * If this path has an associated root attached to it, return the root name. If there's
	 * no root, return null. There can be at most one root associated with this path.
	 * 
	 * @param pathId The ID of the path we're querying.
	 * @return The name of the root, or null if there's no root attached.
	 */
	public String getRootAtPath(int pathId);
}
