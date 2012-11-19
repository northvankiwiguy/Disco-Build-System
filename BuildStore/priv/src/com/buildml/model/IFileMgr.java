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
 * The interface conformed-to by any FileMgr object, which represents a
 * subset of the functionality managed by a BuildStore object. A FileMgr
 * deals with all information related to BuildML files (and directories).
 * <p>
 * There should be exactly one FileMgr object per BuildStore object. Use the
 * BuildStore's getFileMgr() method to obtain that one instance.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface IFileMgr {
	
	/**
	 * Path types - paths can be directories, plain files, or symlinks.
	 */
	public enum PathType { 
		/** The path has an invalid type. */
		TYPE_INVALID, 
		
		/** The path refers to a directory. */
		TYPE_DIR, 
		
		/** The path refers to a file. */
		TYPE_FILE, 
		
		/** The path refers to a symlink. */
		TYPE_SYMLINK
	};
	
	/**
	 * The maximum number of files that this FileMgr object can handle.
	 */
	public static final int MAX_FILES = 16777216;

	/**
	 * Retrieve the ID of the path that's currently associated with this path root.
	 * 
	 * @param rootName The name of the root.
	 * @return The namespace's root path ID, or ErrorCode.NOT_FOUND if it's not defined.
	 */
	public abstract int getRootPath(String rootName);

	/**
	 * Add a new root, to be associated with a specified path ID (which must refer to an
	 * existing directory).
	 * 
	 * @param rootName The name of the new root to be created.
	 * @param pathId The ID of the path the root should be attached to.
	 * @return 
	 * <ul>
	 *   <li>ErrorCode.OK on success.</li>
	 *   <li>ErrorCode.ONLY_ONE_ALLOWED if there's already a root associated with this path ID.</li>
	 *   <li>ErrorCode.ALREADY_USED if the root name is already in use.</li>
	 *   <li>ErrorCode.INVALID_NAME if the new proposed name is invalid.</li>
	 *   <li>ErrorCode.NOT_A_DIRECTORY if the path doesn't refer to a valid directory.</li>
	 * </ul>
	 */
	public abstract int addNewRoot(String rootName, int pathId);

	/**
	 * Return an array of all root names that are currently valid. The list is returned
	 * in alphabetical order.
	 * 
	 * @return A String array of root names.
	 */
	public abstract String[] getRoots();

	/**
	 * Move an existing root to be associated with a new path.
	 * 
	 * @param rootName Name of the root to be moved.
	 * @param pathId The ID of the new path to attach the root to.
	 * @return
	 * <ul>
	 *    <li>ErrorCode.OK if the move completed successfully.</li>
	 *    <li>ErrorCode.NOT_FOUND if the root doesn't exist.</li>
	 *    <li>ErrorCode.NOT_A_DIRECTORY if the new pathID doesn't refer to a directory.</li>
	 *    <li>ErrorCode.ONLY_ONE_ALLOWED if the target pathId already has a root.</li>
	 *    <li>ErrorCode.BAD_PATH an invalid path ID was provided.</li>
	 * </ul>
	 */
	public abstract int moveRootToPath(String rootName, int pathId);

	/**
	 * If this path has an associated root attached to it, return the root name. If there's
	 * no root, return null. There can be at most one root associated with this path.
	 * 
	 * @param pathId The ID of the path we're querying.
	 * @return The name of the root, or null if there's no root attached.
	 */
	public abstract String getRootAtPath(int pathId);

	/**
	 * Return the name of the root that encloses this path. There may be multiple roots
	 * above this path, but return the root that's closest to the path. The "root" root
	 * will always be the default if there's no closer root.
	 * 
	 * @param pathId The ID of the path to be queried.
	 * @return The name of the root that encloses the path.
	 */
	public abstract String getEnclosingRoot(int pathId);

	/**
	 * Delete the specified root from the database. The very top root ("root") can't be removed.
	 * 
	 * @param rootName The name of the root to be deleted.
	 * @return
	 *  <ul>
	 *    <li>ErrorCode.OK on success.</li>
	 *    <li>ErrorCode.NOT_FOUND if the root doesn't exist.</li>
	 *    <li>ErrorCode.CANT_REMOVE the "root" root can't be removed.</li>
	 *  </ul>
	 */
	public abstract int deleteRoot(String rootName);

	/**
	 * Add a new file to the database.
	 * 
	 * @param fullPathName The full path of the file.
	 * @return The ID of the newly added file, or ErrorCode.BAD_PATH if the file couldn't 
	 * be added within this part of the tree (such as when the parent itself is a 
	 * file, not a directory).
	 */
	public abstract int addFile(String fullPathName);

	/**
	 * Add a new directory to the database.
	 * 
	 * @param fullPathName The full path of the directory.
	 * @return The ID of the newly added file, or ErrorCode.BAD_PATH if the directory 
	 * couldn't be added within this part of the tree (such as when the parent itself 
	 * is a file, not a directory).
	 */
	public abstract int addDirectory(String fullPathName);

	/**
	 * Add a new symlink into the database.
	 * 
	 * @param fullPath The full path of the symlink.
	 * @return The ID of the newly added symlink, or ErrorCode.BAD_PATH if the symlink 
	 * couldn't be added within this part of the tree (such as when the parent itself 
	 * is a file, not a directory).
	 */
	public abstract int addSymlink(String fullPath);

	/**
	 * Given a parent's path ID, add a new child path directly within that parent. If the
	 * new path already exists, return the existing child path ID, rather than adding a
	 * new entry.
	 * 
	 * @param parentId The ID of the parent path.
	 * @param pathType The type of the path to be added (directory, file, etc)
	 * @param childName The name of the child path.
	 * @return
	 *   <ul>
	 *      <li>On success, the ID of the child path</li>
	 *      <li>ErrorCode.NOT_A_DIRECTORY if the parent isn't a directory.</li>
	 *      <li>ErrorCode.ONLY_ONE_ALLOWED if the child already exists but has the 
	 *                wrong path type (such as file instead of directory).</li>
	 *   </ul>
	 */
	public abstract int addChildOfPath(int parentId, PathType pathType,
			String childName);

	/**
	 * Similar to addFile, but return an error if the path doesn't exist, rather than
	 * automatically adding it.
	 * 
	 * @param fullPathName The full path of the file.
	 * @return The path's ID, or ErrorCode.BAD_PATH if the path isn't defined.
	 */
	public abstract int getPath(String fullPathName);

	/**
	 * Given a path ID, return a String containing the full path name, possibly
	 * including root names.
	 * 
	 * @param pathId The ID of the path to display as a String.
	 * @param showRoots True if we should show applicable file system roots in the
	 * string, else show the absolute path.
	 * @return The String representation of the path, in the form /a/b/c/..., possibly
	 * containing a file system root (e.g. @root/a/b/c/...)
	 */
	public abstract String getPathName(int pathId, boolean showRoots);

	/**
	 * Given a path ID, return a String containing the full path name.
	 * 
	 * @param pathId The ID of the path to display as a String.
	 * @return The String representation of the path, in the form /a/b/c/...
	 */
	public abstract String getPathName(int pathId);

	/**
	 * Fetch the base name of this path. For example, if the path represents "a/b/c/d", then 
	 * return "d". If the pathId is invalid, return null.
	 * 
	 * @param pathId The ID of the path for which we want the base name.
	 * @return The path's base name as a String, or null if the pathId is invalid.
	 */
	public abstract String getBaseName(int pathId);

	/**
	 * Fetch the ID of the parent of this path. For example, if the path represents "a/b/c/d", then 
	 * return the ID of "a/b/c". If the pathId is invalid, return null. As a special case, the
	 * parent of "/" is itself "/".
	 * 
	 * @param pathId The ID of the path to determine the parent of.
	 * @return The path's parent ID, or ErrorCode.NOT_FOUND if the pathId is invalid.
	 */
	public abstract int getParentPath(int pathId);

	/**
	 * Return the type of the specified path. This will be one of the values in the PathType
	 * enum, such as TYPE_DIR, TYPE_FILE, or TYPE_SYMLINK.
	 * 
	 * @param pathId ID of the path to query.
	 * @return The type of this path.
	 */
	public abstract PathType getPathType(int pathId);

	/**
	 * For a given parent path, search for the specified child path. Return the child's path ID, 
	 * or ErrorCode.NOT_FOUND if the child isn't present. This method call only makes sense
	 * when the parent is a directory, or a symlink that points to a directory.
	 * 
	 * @param parentId The parent path under which the child should be found.
	 * @param childName The child's base name.
	 * @return The child's path ID, or ErrorCode.NOT_FOUND if it doesn't exist.
	 */
	public abstract int getChildOfPath(int parentId, String childName);

	/**
	 * For the specified parent path, fetch an array of all children of that parent.
	 * 
	 * @param pathId The ID of the parent path.
	 * @return An array of child path ID numbers.
	 */
	public abstract Integer[] getChildPaths(int pathId);

	/**
	 * Remove a specific path from the build store. This operation can be only be performed
	 * on files and directories that are unused. That is, directories must be empty, and 
	 * files/directories must not be reference by any actions (or any other such objects).
	 * A trashed path can later be revived by calling the revivePathFromTrash() method.
	 * 
	 * @param pathId The ID of the path to be move to the trash.
	 * 
	 * @return ErrorCode.OK on successful removal, or ErrorCode.CANT_REMOVE if the
	 * path is still used in some way.
	 */
	public abstract int movePathToTrash(int pathId);

	/**
	 * Revive a path that had previously been deleted by the movePathToTrash() method.
	 * @param pathId The ID of the path to be revived.
	 * @return ErrorCode.OK on successful revival, or ErrorCode.CANT_REVIVE if
	 * for some reason the path can't be revived.
	 */
	public abstract int revivePathFromTrash(int pathId);
	
	/**
	 * Determine whether a path is currently marked as "trash" (to be deleted
	 * when the BuildStore is next closed).
	 * 
	 * @param pathId The ID of the path we are querying.
	 * @return true if the path has been marked as trash, else false.
	 */
	public abstract boolean isPathTrashed(int pathId);
	
	/**
	 * Returns a reference to this FileMgr's BuildStore object. 
	 * @return A reference to this FileMgr's BuildStore object.
	 */
	public abstract IBuildStore getBuildStore();

}