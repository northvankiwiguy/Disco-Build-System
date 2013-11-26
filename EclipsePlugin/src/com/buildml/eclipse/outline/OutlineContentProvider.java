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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;

import com.buildml.eclipse.bobj.UIInteger;
import com.buildml.eclipse.bobj.UIPackage;
import com.buildml.eclipse.bobj.UIPackageFolder;
import com.buildml.model.IPackageMgr;

/**
 * A content provider for the OutlinePage class, which uses an SWT TreeViewer to
 * display a BuildML package hierarchy.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class OutlineContentProvider extends ArrayContentProvider
		implements ITreeContentProvider {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The IPackageMgr that we're providing content from */
	private IPackageMgr pkgMgr = null;
	
	/** True if we should display the &lt;import&gt; package, else false */
	private boolean showImportPkg;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new OutlineContentProvider. There should be one of these objects for
	 * each OutlinePage object.
	 * 
	 * @param pkgMgr The IPackageMgr that this object will provide content from.
	 * @param showImportPkg True if we should display the &lt;import&gt; package, else false
	 */
	public OutlineContentProvider(IPackageMgr pkgMgr, boolean showImportPkg) {
		this.pkgMgr = pkgMgr;
		this.showImportPkg = showImportPkg;
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
			
			/* we might need to skip over this one, depending on value of showImportPkg */
			int importPkgId = pkgMgr.getImportPackage();
			
			/* convert the children to UIPackage or UIPackageFolder objects */
			List<UIInteger> uiChildren = new ArrayList<UIInteger>();
			for (int i = 0; i != childrenLength; i++) {
				int thisPkgId = children[i];
				if (showImportPkg || (thisPkgId != importPkgId)) {
					if (pkgMgr.isFolder(thisPkgId)) {
						uiChildren.add(new UIPackageFolder(thisPkgId));
					} else {
						uiChildren.add(new UIPackage(thisPkgId));					
					}
				}
			}
			return uiChildren.toArray();
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
