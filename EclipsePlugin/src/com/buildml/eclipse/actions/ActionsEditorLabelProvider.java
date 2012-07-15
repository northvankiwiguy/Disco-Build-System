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

package com.buildml.eclipse.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;

import com.buildml.eclipse.Activator;
import com.buildml.eclipse.SubEditor;
import com.buildml.model.BuildTasks;
import com.buildml.model.Packages;
import com.buildml.utils.errors.ErrorCode;

/**
 * Label provider for the TreeViewer which is the main viewer for the
 * ActionsEditor class.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class ActionsEditorLabelProvider implements ITableLabelProvider {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The ActionsEditor that we provide content for */
	private SubEditor editor = null;
	
	/** The Action Manager object we'll use for querying file information from the BuildStore */
	private BuildTasks actionMgr;
	
	/** The Packages object we'll use for querying action package information */
	private Packages pkgMgr;
	
	/** The ID of the top-root for our Action Manager object */
	private int topRootId;
	
	/** Image representing the "action" icon. */
	private Image actionImage;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Construct a new ActionsEditorLabelProvider object, which provides text and image
	 * labels for the FilesEditor class.
	 * @param editor The editor that we're providing text/images for.
	 * @param actionMgr The FileNameSpaces object we're graphically representing.
	 * @param pkgMgr The Packages object containing path component information.
	 */
	public ActionsEditorLabelProvider(SubEditor editor, BuildTasks actionMgr,
					Packages pkgMgr) {

		this.editor = editor;
		this.actionMgr = actionMgr;
		this.pkgMgr = pkgMgr;
		
		ImageDescriptor descr = Activator.getImageDescriptor("images/action_icon.gif");
		actionImage = descr.createImage();
		
		/* determine the top-root action */
		topRootId = actionMgr.getRootTask("root");
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Get the image for the specified tree element.
	 * @param element The element for which an image is requested.
	 * @return An Image for the specified column.
	 */
	public Image getColumnImage(Object element, int columnIndex) {
		return actionImage;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Get the column text for the specified tree element.
	 * @param element The tree element for which a label (or two) is requested.
	 * @param columnIndex The column index within the tree, for which a label is requested.
	 * @return The text that will be displayed in the specified column.
	 */
	public String getColumnText(Object element, int columnIndex) {

		if (element instanceof UIActionRecord) {
			UIActionRecord actionRecord = (UIActionRecord)element;
			int actionId = actionRecord.getId();

			switch (columnIndex) {

			/* select the text for the Action "command" column */
			case 0:
				if (actionRecord.isExpandedText()) {
					return " " + actionMgr.getCommand(actionId);
				} 
				else {
					return " " + actionMgr.getCommandSummary(actionId, 200);
				}
					
			/* select text for the package column */
			case 1:
				int pkgId = pkgMgr.getTaskPackage(actionId);
				if (pkgId == ErrorCode.NOT_FOUND) {
					break;	/* return "invalid" */
				}
				if (pkgId == 0) {
					return "";
				}
				String pkgName = pkgMgr.getPackageName(pkgId);
				if (pkgName == null) {
					break; /* return "invalid" */
				}
				return pkgName;
			}
		}

		return " <invalid>";
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
