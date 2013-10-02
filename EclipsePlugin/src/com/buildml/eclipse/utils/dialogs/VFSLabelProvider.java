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

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.buildml.eclipse.bobj.UIDirectory;
import com.buildml.eclipse.bobj.UIFile;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageRootMgr;

/**
 * Standard ILabelProvider, providing images/text for labelling the content of the
 * VFSTreeSelectionDialog.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
/* package */ class VFSLabelProvider implements ILabelProvider {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The manager objects for this BuildStore */
	private IFileMgr fileMgr;

	/** The package root manager */
	private IPackageRootMgr pkgRootMgr;
	
	/** Images for a folder */
	Image folderImage;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Construct a new VFSLabelProvider object.
	 * 
	 * @param buildStore The BuildStore object we're graphically representing.
	 */
	public VFSLabelProvider(IBuildStore buildStore) {

		this.fileMgr = buildStore.getFileMgr();
		this.pkgRootMgr = buildStore.getPackageRootMgr();
	
		ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
		folderImage = sharedImages.getImage(ISharedImages.IMG_OBJ_FOLDER);
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	public void addListener(ILabelProviderListener listener) { /* empty */ }
	public void dispose() { /* empty */ }
	public boolean isLabelProperty(Object element, String property) { return false; }
	public void removeListener(ILabelProviderListener listener) { /* empty */ }

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
		
		/* folders always have the folder icon */
		if (element instanceof UIDirectory) {
			return folderImage;
		}
		
		/* files have an icon that illustrates their type */
		else if (element instanceof UIFile) {
			UIFile file = (UIFile)element;			
			String name = fileMgr.getBaseName(file.getId());
			return EclipsePartUtils.getImageFromFileType(name);
		}
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		
		/* for directories, show the directory name, and possibly some file system roots */
		if (element instanceof UIDirectory) {
			UIDirectory node = (UIDirectory)element;
			int pathId = node.getId();
			
			String baseName = fileMgr.getBaseName(pathId);
			String roots[] = pkgRootMgr.getRootsAtPath(pathId);
			
			/*
			 * Does this element has attached roots? If so, display them in a list.
			 */
			if (roots.length != 0)
			{
				StringBuilder sb = new StringBuilder();
				sb.append(baseName);
				sb.append(" (");
				for (int i = 0; i < roots.length; i++) {
					if (i != 0) {
						sb.append(' ');
					}
					sb.append('@');
					sb.append(roots[i]);
				}
				sb.append(')');
				return sb.toString();
			}
			
			/*
			 * No, just display the element's base file name.
			 */
			else {
				return baseName;
			}
			
		}
		
		/* for files, just show the file's base name */
		else if (element instanceof UIFile) {
			UIFile node = (UIFile)element;
			int pathId = node.getId();
			
			String baseName = fileMgr.getBaseName(pathId);
			if (baseName != null) {
				return baseName;
			}
		}
		
		return "<Invalid>";
	}
	
	/*-------------------------------------------------------------------------------------*/
}
