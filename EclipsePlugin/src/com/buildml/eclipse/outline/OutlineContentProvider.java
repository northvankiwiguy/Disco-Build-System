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

package com.buildml.eclipse.outline;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;

import com.buildml.eclipse.utils.UIInteger;
import com.buildml.model.IPackageMgr;

/**
 * A content provider for the OutlinePage class, which uses an SWT TreeViewer to
 * display a BuildML package hierarchy.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
/* package */ class OutlineContentProvider extends ArrayContentProvider
		implements ITreeContentProvider {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The IPackageMgr that we're providing content from */
	private IPackageMgr pkgMgr = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new OutlineContentProvider. There should be one of these objects for
	 * each OutlinePage object.
	 * 
	 * @param pkgMgr The IPackageMgr that this object will provide content from.
	 */
	/* package */ OutlineContentProvider(IPackageMgr pkgMgr) {
		this.pkgMgr = pkgMgr;
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {

		/* we can only get the children of a folder */
		if (parentElement instanceof UIPackageFolder) {
			
			/* fetch the list of child package IDs from the database */
			int pkgId = ((UIPackageFolder) parentElement).getId();
			Integer children[] = pkgMgr.getFolderChildren(pkgId);
			int childrenLength = children.length;
			
			/* convert the children to UIPackage or UIPackageFolder objects */
			UIInteger uiChildren[] = new UIInteger[childrenLength];
			for (int i = 0; i != childrenLength; i++) {
				if (pkgMgr.isFolder(children[i])) {
					uiChildren[i] = new UIPackageFolder(children[i]);
				} else {
					uiChildren[i] = new UIPackage(children[i]);					
				}
			}
			return uiChildren;
		}
		
		/* non-folders don't have children */
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {
		
		/* Not implemented for now, since it doesn't appear to be useful */
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
	
		/* Only folders can have children */
		if (element instanceof UIInteger) {
			int pkgId = ((UIInteger) element).getId();
			return pkgMgr.getFolderChildren(pkgId).length != 0;
		}
		return false;
	}

	/*-------------------------------------------------------------------------------------*/
}
