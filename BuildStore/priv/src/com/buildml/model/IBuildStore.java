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

import java.io.IOException;

/**
 * The interface conformed-to by any BuildStore object which is the main object
 * providing an in-memory representation of a build.bml file.
 * 
 * Note that although BuildStore is the main entry point, most of the work is done by
 * its delegate classes, such as FileMgr, ActionMgr etc. These "Managers" each deal
 * with a specific part of the build system, providing business logic and database access
 * to implement features.
 * 
 * To open or create a BuildStore object, you should use the BuildStoreFactory class.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface IBuildStore {

	/**
	 * Return the BuildML database's schema version.
	 * 
	 * @return The schema version as an integer, or -1 if there's no schema in place.
	 */
	public abstract int getBuildStoreVersion();

	/**
	 * Fetch the FileMgr manager associated with this BuildStore. This object
	 * encapsulates all knowledge about files/directories within the build system.
	 * 
	 * @return A FileMgr manager object.
	 */
	public abstract IFileMgr getFileMgr();

	/**
	 * Fetch the FileGroupMgr manager associated with this BuildStore. This object
	 * encapsulates knowledge about groups of paths.
	 * 
	 * @return A FileGroupMgr manager object.
	 */
	public abstract IFileGroupMgr getFileGroupMgr();

	/**
	 * Fetch the FileIncludeMgr associated with this BuildStore. This object
	 * encapsulates knowledge of which source files textually include which other
	 * source files.
	 * 
	 * @return A FileIncludeMgr object.
	 */
	public abstract IFileIncludeMgr getFileIncludeMgr();

	/**
	 * Fetch the ActionMgr manager associated with this BuildStore. This object
	 * contains information about all build actions.
	 * 
	 * @return An ActionMgr manager object.
	 */
	public abstract IActionMgr getActionMgr();

	/**
	 * Fetch the Reports manager associated with this BuildStore. This object
	 * contains methods for generating summary reports from the BuildML database.
	 *  
	 * @return A Reports manager object.
	 */
	public abstract IReportMgr getReportMgr();

	/**
	 * Fetch the FileAttributeMgr manager associated with this BuildStore. This object
	 * encapsulates knowledge of which attributes are attached to the paths in
	 * our FileMgr object.
	 * 
	 * @return This BuildStore's FileAttributeMgr object.
	 */
	public abstract IFileAttributeMgr getFileAttributeMgr();

	/**
	 * Fetch the PackagesMgr associated with this BuildStore. This object
	 * encapsulates knowledge of the package names used in the BuildStore.
	 * 
	 * @return A Packages manager object.
	 */
	public abstract IPackageMgr getPackageMgr();

	/**
	 * Fetch the PackageRootMgr associated with this BuildStore. This object
	 * encapsulates knowledge of the package roots and their location.
	 * 
	 * @return A PackageRootMgr manager object.
	 */
	public abstract IPackageRootMgr getPackageRootMgr();

	/**
	 * Specify whether database access should be fast (true) or safe (false). Fast
	 * access is considerably faster than safe access, but won't ensure that
	 * changes are written to the disk. Only use fast access for long-running
	 * write operations that would otherwise be too slow in "safe" mode. Always
	 * be sure to disable fast-access to ensure that changes are eventually
	 * written to disk.
	 * 
	 * @param fast Set to true to enable fast access, or false for safe access.
	 */
	public abstract void setFastAccessMode(boolean fast);

	/**
	 * Close the BuildStore, and release any resources associated with it.
	 * Attempting to access the BuildStore's content after it's closed will
	 * cause a FatalBuildStoreError.
	 */
	public abstract void close();

	/**
	 * Save this BuildStore to disk, ensuring that any temporary (unsaved) changes are
	 * persisted. This method only has an effect if the database was created with
	 * saveRequired == true.
	 * @throws IOException Unable to write database to disk.
	 */
	public abstract void save() throws IOException;

	/**
	 * Save this BuildStore to disk, using the caller-specified file name. This method only
	 * has an effect if the database was created with saveRequired == true.
	 * @param fileToSave The file to write the database content into. This file now becomes
	 * the default for future save() operations.
	 * @throws IOException Unable to write database to disk.
	 */
	public abstract void saveAs(String fileToSave) throws IOException;

	/**
	 * Remove any trashed files or actions that may still be in the database. Use
	 * of this method will permanently delete files and actions, making it impossible
	 * to "undo" or "redo" historical changes.
	 */
	public void emptyTrash();
}
