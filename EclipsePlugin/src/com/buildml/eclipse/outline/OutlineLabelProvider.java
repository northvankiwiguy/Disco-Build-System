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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.buildml.eclipse.bobj.UIInteger;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.model.IPackageMgr;

/**
 * A label provider for the OutlinePage class, which uses an SWT TreeViewer to
 * display a BuildML package hierarchy.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class OutlineLabelProvider extends LabelProvider {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The IPackageMgr that we'll provide labels from */
	private IPackageMgr pkgMgr;
	
	/** The image used to display BuildML package folders */
	Image folderImage;

	/** The image used to display BuildML packages */
	Image packageImage;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new OutlineLabelProvider. There should be exactly one of these objects
	 * per OutlinePage.
	 * 
	 * @param pkgMgr The IPackageMgr from which to obtain label information.
	 */
	public OutlineLabelProvider(IPackageMgr pkgMgr) {
		this.pkgMgr = pkgMgr;
		
		/* pre-obtain images for packages and package folders */
		ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
		folderImage = sharedImages.getImage(ISharedImages.IMG_OBJ_FOLDER);
		packageImage = EclipsePartUtils.getImage("images/package_icon.gif");
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		
		/* For either UIPackage or UIPackageFolder, query IPackageMgr for the label */
		if (element instanceof UIInteger) {
			int pkgId = ((UIInteger) element).getId();
			String name = pkgMgr.getName(pkgId);
			if (name != null) {
				return name;
			}
		}
		
		/* this shouldn't happen, but just in case... */
		return "<invalid>";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
		
		/* For UIPackage and UIPackageFolder, select a suitable icon */
		if (element instanceof UIInteger) {
			int pkgId = ((UIInteger) element).getId();
			if (pkgMgr.isFolder(pkgId)) {
				return folderImage;
			} else {
				return packageImage;
			}
		}
		
		/* this shouldn't happen, but default to having no image */
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
