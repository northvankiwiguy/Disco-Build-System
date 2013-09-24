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

package com.buildml.eclipse.files;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.buildml.eclipse.Activator;
import com.buildml.eclipse.EditorOptions;
import com.buildml.eclipse.bobj.UIDirectory;
import com.buildml.eclipse.bobj.UIFile;
import com.buildml.eclipse.bobj.UIInteger;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IFileMgr.PathType;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMemberMgr.PackageDesc;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageRootMgr;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class FilesEditorLabelProvider implements ITableLabelProvider {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The FilesEditor that we provide content for */
	private FilesEditor editor = null;
	
	/** The manager objects for this BuildStore */
	private IFileMgr fileMgr;
	private IPackageMgr pkgMgr;
	private IPackageMemberMgr pkgMemberMgr;
	private IPackageRootMgr pkgRootMgr;
	
	/** The ID of the top-root for our FileMgr object */
	private int topRootId;
	
	/** Images we'll use when displaying a tree of files */
	Image 
		folderImage,
		symlinkImage;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Construct a new FilesEditorLabelProvider object, which provides text and image
	 * labels for the FilesEditor class.
	 * @param editor The editor that we're providing text/images for.
	 * @param buildStore The BuildStore object we're graphically representing.
	 */
	public FilesEditorLabelProvider(FilesEditor editor, IBuildStore buildStore) {

		this.editor = editor;
		this.fileMgr = buildStore.getFileMgr();
		this.pkgMgr = buildStore.getPackageMgr();
		this.pkgMemberMgr = buildStore.getPackageMemberMgr();
		this.pkgRootMgr = buildStore.getPackageRootMgr();
	
		ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
		folderImage = sharedImages.getImage(ISharedImages.IMG_OBJ_FOLDER);
		symlinkImage = null; // TODO: create an image for symlinks
		
		/* determine the top-root of this FileMgr object */
		topRootId = pkgRootMgr.getRootPath("root");
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * @param element
	 * @return An Image for the specified column.
	 */
	public Image getColumnImage(Object element, int columnIndex) {
	
		switch (columnIndex) {
		
		 /* select an image for the tree column */
		case 0:
			
			/* we only care about UIInteger types */
			if ((element instanceof UIDirectory) || (element instanceof UIFile)) {
				UIInteger uiInt = (UIInteger)element;
				PathType pathType = fileMgr.getPathType(uiInt.getId());

				switch (pathType) {
				case TYPE_INVALID:
					return null;
				case TYPE_DIR:
					return folderImage;
				case TYPE_FILE:
					/*
					 * Fetch the image that's normally associated with the file type
					 * (based on its file suffix). Rather than creating a large number
					 * of images, we cache them in this plugin's image registry.
					 */
					IEditorRegistry editorImageRegistry = PlatformUI.getWorkbench().getEditorRegistry();
					String name = fileMgr.getBaseName(uiInt.getId());
					ImageDescriptor imageDescr = editorImageRegistry.getImageDescriptor(name);

					/* can we get this image from the plugin's cache? */
					ImageRegistry pluginImageRegistry = Activator.getDefault().getImageRegistry();
					Image iconImage = pluginImageRegistry.get(imageDescr.toString());
					if (iconImage == null) {
						iconImage = imageDescr.createImage();
						pluginImageRegistry.put(imageDescr.toString(), iconImage);
					}
					return iconImage;

				case TYPE_SYMLINK:
					return symlinkImage;
				}
			}

		/* there is no image for the package column or the scope column. */
		case 1:
		case 2:
			return null;
		}
		
		/* by default, show nothing */
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @param element
	 * @return The text that will be displayed in the specified column.
	 */
	public String getColumnText(Object element, int columnIndex) {

		if ((element instanceof UIFile) || (element instanceof UIDirectory)) {
			UIInteger uiInt = (UIInteger)element;
			int pathId = uiInt.getId();

			switch (columnIndex) {

			/* select the text for the file tree column */
			case 0:
				return getCoalescedText(pathId);
				
			/* select text for the package column */
			case 1:
				PackageDesc pkgInfo = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.MEMBER_TYPE_FILE, pathId);
				if (pkgInfo == null) {
					break;	/* return "invalid" */
				}
				if (pkgInfo.pkgId == 0) {
					return "";
				}
				String pkgName = pkgMgr.getName(pkgInfo.pkgId);
				if (pkgName == null) {
					break; /* return "invalid" */
				}
				return pkgName;
				
			/* select text for the visibility column */
			case 2:
				pkgInfo = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.MEMBER_TYPE_FILE, pathId);
				if (pkgInfo == null) {
					break;	/* return "invalid" */
				}
				if (pkgInfo.pkgId == 0) {
					return "";
				}
				String scopeName = pkgMemberMgr.getScopeName(pkgInfo.pkgScopeId);
				if (scopeName == null) {
					break; /* return "invalid" */
				}
				return scopeName;
				
			}
		}

		return "<invalid>";
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @param pathId The ID of the top path.
	 * @return The coalesced path name, containing one or more path components.
	 * 
	 */
	public String getCoalescedText(int pathId) {	
		StringBuffer sb = new StringBuffer();
		Integer children[];
		String roots[] = null;
		boolean showRoots = editor.isOptionSet(EditorOptions.OPT_SHOW_ROOTS);

		/*
		 * If we're at the top of the tree, we'll want to put "/"
		 * at the start of the name.
		 */
		if (!showRoots && fileMgr.getParentPath(pathId) == topRootId) {
			sb.append('/');
		}
		
		/* should we try to coalesce the path? */
		if (editor.isOptionSet(EditorOptions.OPT_COALESCE_DIRS)) {
			
			/* 
			 * loop, until we find a directory containing multiple entries, a
			 * file, or a package root.
			 */
			while (true) {
				String pathName = fileMgr.getBaseName(pathId);
				sb.append(pathName);

				children = fileMgr.getChildPaths(pathId);

				/* are there roots attached here? If so, stop coalescing */
				if (showRoots) {
					roots = pkgRootMgr.getRootsAtPath(pathId);
					if (roots.length != 0) {
						break;
					}
				}

				/* if there are no children or multiple children, we're done */
				if (children.length != 1) {
					break;
				}

				/* or if the single child is not a directory, we're done */
				int childId = children[0];
				if (fileMgr.getPathType(childId) != PathType.TYPE_DIR) {
					break;
				}

				/* else, we'll coalesce the next path too */
				sb.append('/');
				pathId = childId;
			}
		}

		/* don't coalesce the path */
		else {
			sb.append(fileMgr.getBaseName(pathId));
			if (showRoots) {
				roots = pkgRootMgr.getRootsAtPath(pathId);
			}
		}
		
		/* if there were roots at the end of the path chain, display them */
		if ((roots != null) && (roots.length != 0)) {
			sb.append(" (");
			for (int i = 0; i < roots.length; i++) {
				if (i != 0) {
					sb.append(' ');
				}
				sb.append('@');
				sb.append(roots[i]);
			}
			sb.append(')');
		}
		
		return sb.toString();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener) {
		/* empty */
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		/* empty */
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
	 */
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener) {
		/* empty */
	}
	
	/*-------------------------------------------------------------------------------------*/
}
