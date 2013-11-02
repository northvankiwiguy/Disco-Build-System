/*******************************************************************************
 * Copyright (c) 2013 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.packages.properties.filegroup;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;

import com.buildml.model.IBuildStore;
import com.buildml.model.IFileGroupMgr;

/**
 * A "content provider" class that feeds the TreeViewer in {@link FileGroupContentPropertyPage}
 * with the content of a file group.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class FileGroupContentProvider extends ArrayContentProvider implements
		ITreeContentProvider {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/
	
	/** The IBuildStore we retrieve content information from */
	private IBuildStore buildStore;
	
	/** The file group we're viewing */
	private int fileGroupId;
	
	/** The type of this file group (e.g. SOURCE_GROUP, MERGE_GROUP, etc). */
	private int fileGroupType;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new FileGroupContentProvider.
	 * 
	 * @param buildStore The IBuildStore to retrieve content information from.
	 * @param fileGroupId The file group we're viewing.
	 * @param fileGroupType The type of this file group (e.g. SOURCE_GROUP, MERGE_GROUP, etc).
	 */
	public FileGroupContentProvider(IBuildStore buildStore, int fileGroupId, int fileGroupType) {
		this.buildStore = buildStore;
		this.fileGroupId = fileGroupId;
		this.fileGroupType = fileGroupType;
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		
		/* the elements within source groups are paths, and never have children */
		if (fileGroupType == IFileGroupMgr.SOURCE_GROUP) {
			return null;
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		
		/* elements within source groups are paths, and never have children */
		if (fileGroupType == IFileGroupMgr.SOURCE_GROUP) {
			return false;
		}
		return false;
	}

	/*-------------------------------------------------------------------------------------*/
}
