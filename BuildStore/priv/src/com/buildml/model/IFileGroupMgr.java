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
 * The interface conformed-to by any FileGroupMgr object, which represents a
 * subset of the functionality managed by a BuildStore object. A FileGroupMgr
 * deals with groupings of files, along with file patterns and other grouping
 * mechanisms.
 * <p>
 * There should be exactly one FileGroupMgr object per BuildStore object. Use the
 * BuildStore's getFileGroupMgr() method to obtain that one instance.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface IFileGroupMgr {

	/**
	 * Create a new file group, into which paths/patterns/etc can be added.
	 * 
	 * @return The unique ID number of this new group.
	 */
	int newGroup();
	
	/**
	 * Delete an existing file group. After deletion, no more operations
	 * can be performed on this group.
	 * 
	 * @param  groupId The ID of the group to be removed. 
	 * @return         ErrorCode.OK on success, ErrorCode.CANT_REMOVE if the
	 *                 group still contains entries, or ErrorCode.NOT_FOUND
	 *                 if the group ID is invalid.
	 */
	int removeGroup(int groupId);
	
	/**
	 * Return the number of entries in this file group.
	 * 
	 * @param groupId The ID of the file group to query.
	 * @return        The number of elements in this group, or 
	 *                ErrorCode.NOT_FOUND if the group ID is invalid.
	 */
	int getGroupSize(int groupId);
	
	/**
	 * Return the complete ordered list of files in this file group. Any
	 * patterns in the file group will be expanded into actual files.
	 * 
	 * @param groupId The ID of the file group to query.
	 * @return        An array of path IDs, containing the ordered list of
	 *                paths that the group expands into.
	 */
	Integer[] getExpandedGroupFiles(int groupId);
	
	/**
	 * Append the specified path to the end of the file group.
	 * 
	 * @param groupId The ID of the group to append to.
	 * @param pathId  The ID of the path to append.
	 * @return        The index of the new entry (within the file group), or 
	 *                ErrorCode.NOT_FOUND if the group ID is invalid.
	 */
	int addPath(int groupId, int pathId);
	
	/**
	 * Insert a path at the specified location within the file group.
	 * All the following entries in the file group will be moved along
	 * one position.
	 * 
	 * @param groupId The ID of the group to append to.
	 * @param index   Index within the file group at which the new path
	 *                will be inserted.
	 * @param pathId  The ID of the path to append.
	 * @return        The index of the new entry (within the file group), or 
	 *                ErrorCode.NOT_FOUND if the group ID is invalid.
	 */
	int addPath(int groupId, int index, int pathId);
	
	/**
	 * Remove an entry from within a file group.
	 * 
	 * @param groupId The ID of the group from which to remove an entry.
	 * @param index   The index (within the group) of the entry to remove.
	 * @return        ErrorCode.OK on success, ErrorCode.NOT_FOUND if the group
	 *                ID is not valid, or ErrorCode.BAD_VALUE if the index
	 *                is out of range.
	 */
	int removeEntry(int groupId, int index);

	/**
	 * @return The BuildStore that delegates to this FileGroupMgr.
	 */
	IBuildStore getBuildStore();
}
