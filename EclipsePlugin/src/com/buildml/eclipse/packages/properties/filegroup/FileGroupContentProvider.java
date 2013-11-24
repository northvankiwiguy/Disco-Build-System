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

import com.buildml.eclipse.utils.errors.FatalError;
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
	
	/** The IFileGroupMgr we retrieve content information from */
	private IFileGroupMgr fileGroupMgr;
	
	/** The type of this file group (e.g. SOURCE_GROUP, MERGE_GROUP, etc). */
	private int fileGroupType;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new FileGroupContentProvider.
	 * 
	 * @param buildStore The IBuildStore to retrieve content information from.
	 * @param fileGroupType The type of this file group (e.g. SOURCE_GROUP, MERGE_GROUP, etc).
	 */
	public FileGroupContentProvider(IBuildStore buildStore, int fileGroupType) {
		this.fileGroupType = fileGroupType;
		this.fileGroupMgr = buildStore.getFileGroupMgr();
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
		
		/* for merge groups, the top-level sub-group ID have String paths as their children */
		else if (fileGroupType == IFileGroupMgr.MERGE_GROUP) {
			
			/* the parent is the ID of the sub file group */
			if (parentElement instanceof TreeMember) {
				TreeMember parent = (TreeMember)parentElement;
				
				/* fetch all the children of this subgroup, and convert to FileGroupContentMember[] */
				String files[] = fileGroupMgr.getExpandedGroupFiles(parent.id);
				TreeMember children[] = new TreeMember[files.length];
				for (int i = 0; i < files.length; i++) {
					children[i] = new TreeMember(1, parent.seq, parent.id, files[i]);
				}
				return children;
			}
			
			/* else, if the parent is a String, it's at the second level, with no kids */
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
		
		/* 
		 * For merge groups, the first level (subgroup parent - Integer) has children, but
		 * the actual paths (Strings) don't have children.
		 */
		else if (fileGroupType == IFileGroupMgr.MERGE_GROUP) {
			if (element instanceof TreeMember) {
				return ((TreeMember)element).level == 0;
			}
			return false;
		}
		
		/* other cases are currently not handled */
		throw new FatalError("Unhandled file group type: " + fileGroupType);
	}

	/*-------------------------------------------------------------------------------------*/
}
