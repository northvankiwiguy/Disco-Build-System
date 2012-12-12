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

package com.buildml.model.impl;

import java.util.ArrayList;
import java.util.List;

import com.buildml.model.IBuildStore;
import com.buildml.model.IFileGroupMgr;
import com.buildml.utils.errors.ErrorCode;

/**
 * This is a very simple (non-persistent) implementation of IFileGroupMgr. A lot more
 * work is required to make this fully-featured.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class FileGroupMgr implements IFileGroupMgr {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/** The BuildStore that delegates work to this FileGroupMgr */
	private IBuildStore buildStore;
	
	/** The array of file groups. */
	private List<List<Integer>> fileGroups;
	
	/** The sequence number of the next group ID to allocate */
	private int nextGroupIndex;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new FileGroupMgr.
	 * 
	 * @param buildStore The BuildStore that delegates work to this FileGroupMgr.
	 */
	public FileGroupMgr(IBuildStore buildStore) {
		this.buildStore = buildStore;
		
		/*
		 * Create the new top-level file group. An entry in this list refers to a second
		 * level List<Integer> which contains the entries in the group.
		 */
		fileGroups = new ArrayList<List<Integer>>();
		
		nextGroupIndex = 0;
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#newGroup()
	 */
	@Override
	public int newGroup() {
		List<Integer> newGroupEntries = new ArrayList<Integer>(); 
				
		fileGroups.add(newGroupEntries);
		return nextGroupIndex++;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#removeGroup(int)
	 */
	@Override
	public int removeGroup(int groupId) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#getGroupSize(int)
	 */
	@Override
	public int getGroupSize(int groupId) {
		return nextGroupIndex;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#getExpandedGroupFiles(int)
	 */
	@Override
	public Integer[] getExpandedGroupFiles(int groupId) {
		List<Integer> groupEntries = fileGroups.get(groupId);
		return groupEntries.toArray(new Integer[groupEntries.size()]);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#addPath(int, int)
	 */
	@Override
	public int addPath(int groupId, int pathId) {
		List<Integer> groupEntries = fileGroups.get(groupId);
		groupEntries.add(Integer.valueOf(pathId));
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#addPath(int, int, int)
	 */
	@Override
	public int addPath(int groupId, int index, int pathId) {
		List<Integer> groupEntries = fileGroups.get(groupId);
		groupEntries.add(index, Integer.valueOf(pathId));
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IFileGroupMgr#removeEntry(int, int)
	 */
	@Override
	public int removeEntry(int groupId, int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.IFileMgr#getBuildStore()
	 */
	@Override
	public IBuildStore getBuildStore() {
		return buildStore;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
