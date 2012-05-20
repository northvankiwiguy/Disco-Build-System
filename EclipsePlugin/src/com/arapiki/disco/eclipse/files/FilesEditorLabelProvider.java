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

package com.arapiki.disco.eclipse.files;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.arapiki.disco.eclipse.Activator;
import com.buildml.model.Components;
import com.buildml.model.FileNameSpaces;
import com.buildml.model.FileNameSpaces.PathType;
import com.buildml.model.types.FileRecord;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class FilesEditorLabelProvider implements ITableLabelProvider {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The DiscoFilesEditor that we provide content for */
	private DiscoFilesEditor editor = null;
	
	/** The FileNameSpaces object we'll use for querying file information from the BuildStore */
	private FileNameSpaces fns;
	
	/** The Components object we'll use for querying path component information */
	private Components comps;
	
	/** The ID of the top-root for our FileNameSpaces object */
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
	 * labels for the DiscoFilesEditor class.
	 * @param editor The editor that we're providing text/images for.
	 * @param fns The FileNameSpaces object we're graphically representing.
	 * @param comps The Components object containing path component information.
	 */
	public FilesEditorLabelProvider(DiscoFilesEditor editor, FileNameSpaces fns,
					Components comps) {

		this.editor = editor;
		this.fns = fns;
		this.comps = comps;
	
		ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
		folderImage = sharedImages.getImage(ISharedImages.IMG_OBJ_FOLDER);
		symlinkImage = null; // TODO: create an image for symlinks
		
		/* determine the top-root of this FileNameSpaces object */
		topRootId = fns.getRootPath("root");
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
			
			/* we only care about FileRecord types */
			if (element instanceof FileRecord) {
				FileRecord fr = (FileRecord)element;
				PathType pathType = fns.getPathType(fr.getId());

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
					String name = fns.getBaseName(fr.getId());
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

		/* there is no image for the component column or the visibility column. */
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

		if (element instanceof FileRecord) {
			FileRecord fr = (FileRecord)element;
			int pathId = fr.getId();

			switch (columnIndex) {

			/* select the text for the file tree column */
			case 0:

				/* for FileRecords, we return the path's base name */

				/* case: show file path roots */
				if (editor.isOptionSet(DiscoFilesEditor.OPT_SHOW_ROOTS)) {
					String rootName = fns.getRootAtPath(pathId);
					if (rootName != null) {
						String fullPath = fns.getPathName(pathId);
						return "@" + rootName + " (" + fullPath + ")";
					}
				}

				/* case: show the directory, but coalesce child directories */
				if (editor.isOptionSet(DiscoFilesEditor.OPT_COALESCE_DIRS)) {
					return getCoalescedText(pathId);

					/* case: show the directory, with no coalescing */
				} else {
					String pathName;

					if (fns.getParentPath(pathId) == topRootId) {
						pathName = "/" + fns.getBaseName(pathId);
					} else {
						pathName = fns.getBaseName(pathId);					
					}
					return pathName;
				}

			/* select text for the component column */
			case 1:
				Integer compInfo[] = comps.getFileComponent(pathId);
				if (compInfo == null) {
					break;	/* return "invalid" */
				}
				if (compInfo[0] == 0) {
					return "";
				}
				String compName = comps.getComponentName(compInfo[0]);
				if (compName == null) {
					break; /* return "invalid" */
				}
				return compName;
				
			/* select text for the visibility column */
			case 2:
				compInfo = comps.getFileComponent(pathId);
				if (compInfo == null) {
					break;	/* return "invalid" */
				}
				if (compInfo[1] == 0) {
					return "";
				}
				String sectName = comps.getScopeName(compInfo[1]);
				if (sectName == null) {
					break; /* return "invalid" */
				}
				return sectName;
				
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
		boolean done = false;
		Integer children[];
		
		/*
		 * If we're at the top of the tree, we'll want to put "/"
		 * at the start of the name.
		 */
		if (fns.getParentPath(pathId) == topRootId) {
			sb.append('/');
		}
		
		/* loop, until we find a directory containing multiple entries */
		while (!done) {
			String pathName = fns.getBaseName(pathId);
			sb.append(pathName);
		
			children = fns.getChildPaths(pathId);
			
			/* if there are no children or multiple children, we're done */
			if (children.length != 1) {
				done = true;
			}
			
			else {
				/* or if the single child is not a directory, we're done */
				int childId = children[0];
				if (fns.getPathType(childId) != PathType.TYPE_DIR) {
					done = true;
				}
				
				/* else, we'll coalesce the next path too */
				else {
					sb.append('/');
					pathId = childId;
				}
			}
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
