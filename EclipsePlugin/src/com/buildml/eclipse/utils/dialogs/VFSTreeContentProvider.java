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
import com.buildml.eclipse.bobj.UIFile;
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
	
	/** do we show files as well as directories? */
	private boolean showFiles = true;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link VFSTreeContentProvider} that translates the information
	 * in the BuildStore into something that a TreeViewer can understand.
	 * 
	 * @param buildStore The IBuildStore containing the VFS to be displayed.
	 * @param showFiles true if we should show files as well as directories.
	 */
	public VFSTreeContentProvider(IBuildStore buildStore, boolean showFiles) {
		this.fileMgr = buildStore.getFileMgr();
		this.showFiles = showFiles;
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
			return getChildPaths(uiInt.getId());
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {

		if (element instanceof UIInteger) {
			
			/* query the BuildStore for this element's parent */
			UIInteger uiInt = (UIInteger)element;
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
			UIInteger childIds[] = getChildPaths(uiInt.getId());
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
	 * @return An array of UIInteger objects, representing the child directories.
	 */
	private UIInteger[] getChildPaths(int dirId) {
		
		/* fetch all children of this directory */
		Integer childPaths[] = fileMgr.getChildPaths(dirId);
		
		/* for those children that are directories, add them to result array */
		List<UIInteger> childDirs = new ArrayList<UIInteger>();
		for (int pathId : childPaths) {
			PathType pathType = fileMgr.getPathType(pathId);
			if (pathType == PathType.TYPE_DIR) {
				childDirs.add(new UIDirectory(pathId));
			}
			else if (showFiles && (pathType == PathType.TYPE_FILE)) {
				childDirs.add(new UIFile(pathId));
			}
		}
		return childDirs.toArray(new UIInteger[childDirs.size()]);
	}
	
	/*-------------------------------------------------------------------------------------*/
}
