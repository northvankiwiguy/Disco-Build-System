/*******************************************************************************
 * Copyright (c) 2011 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.utils.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;

import com.buildml.eclipse.bobj.UIDirectory;
import com.buildml.eclipse.bobj.UIInteger;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IFileMgr.PathType;

/**
 * A standard ITreeContentProvider associated with the VFSTreeSelectionDialog class.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
/* package */ class VFSTreeContentProvider extends ArrayContentProvider
	implements ITreeContentProvider {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** The FileMgr object we should query for file information */
	private IFileMgr fileMgr = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link VFSTreeContentProvider} that translates the information
	 * in the BuildStore into something that a TreeViewer can understand.
	 * 
	 * @param buildStore The IBuildStore containing the VFS to be displayed.
	 */
	public VFSTreeContentProvider(IBuildStore buildStore) {
		this.fileMgr = buildStore.getFileMgr();
		buildStore.getPackageRootMgr();
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		
		/* We only care about UIDirectory elements (ignore files) */
		if (parentElement instanceof UIDirectory) {
			UIDirectory uiInt = (UIDirectory)parentElement;
			return getChildDirectories(uiInt.getId());
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {

		if (element instanceof UIDirectory) {
			
			/* query the BuildStore for this element's parent */
			UIDirectory uiInt = (UIDirectory)element;
			int parentId = fileMgr.getParentPath(uiInt.getId());

			/* base case - parent of / is null */
			if (parentId == uiInt.getId()) {
				return null;
			}
						
			/* if there's an error, inform the caller that we can't find the parent */
			if (parentId < 0) {
				return null;
			}
			
			/* construct a new UIDirectory (which must be a directory) */
			return new UIDirectory(parentId);
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {

		/* we only care about UIDirectory element types */
		if (element instanceof UIDirectory) {
			UIInteger uiInt = (UIInteger)element;
			
			/* query the BuildStore to see if there are any children */
			UIDirectory childIds[] = getChildDirectories(uiInt.getId());
			if (childIds.length > 0) {
				return true;
			}
		}
		
		/* else, we have no children */
		return false;
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Fetch the children of the specified FileMgr directory that themselves are directories.
	 * 
	 * @param dirId The path ID of the directory being queried.
	 * @return An array of UIDirectory objects, representing the child directories.
	 */
	private UIDirectory[] getChildDirectories(int dirId) {
		
		/* fetch all children of this directory */
		Integer childPaths[] = fileMgr.getChildPaths(dirId);
		
		/* for those children that are directories, add them to result array */
		List<UIDirectory> childDirs = new ArrayList<UIDirectory>();
		for (int pathId : childPaths) {
			if (fileMgr.getPathType(pathId) == PathType.TYPE_DIR) {
				childDirs.add(new UIDirectory(pathId));
			}
		}
		return childDirs.toArray(new UIDirectory[childDirs.size()]);
	}
	
	/*-------------------------------------------------------------------------------------*/
}
