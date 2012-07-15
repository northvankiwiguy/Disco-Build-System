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
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;

import com.buildml.eclipse.Activator;
import com.buildml.eclipse.SubEditor;
import com.buildml.model.BuildTasks;

/**
 * Label provider for the first column of the TreeViewer which is the main viewer for the
 * ActionsEditor class.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class ActionsEditorLabelCol1Provider extends ColumnLabelProvider implements ILabelProvider {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The Action Manager object we'll use for querying information from the BuildStore */
	private BuildTasks actionMgr;
	
	/** Image representing the "action" icon. */
	private Image actionImage;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Construct a new ActionsEditorLabelCol1Provider object, which provides text and image
	 * labels for the FilesEditor class.
	 * @param editor The editor that we're providing text/images for.
	 * @param actionMgr The FileNameSpaces object we're graphically representing.
	 */
	public ActionsEditorLabelCol1Provider(SubEditor editor, BuildTasks actionMgr) {

		this.actionMgr = actionMgr;
		
		/* all entries in the first column have an icon - precache it now */
		ImageDescriptor descr = Activator.getImageDescriptor("images/action_icon.gif");
		actionImage = descr.createImage();
		
		actionMgr.getRootTask("root");
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Get the image for the specified tree element.
	 * @param element The element for which an image is requested.
	 * @return An Image for the column.
	 */
	public Image getImage(Object element) {
		return actionImage;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Get the column text for the specified tree element.
	 * @param element The tree element for which a label is requested.
	 * @return The text that will be displayed. This will be limited in size so that
	 * it appears on a single line of the tree.
	 */
	public String getText(Object element) {
		
		if (element instanceof UIActionRecord) {
			UIActionRecord actionRecord = (UIActionRecord)element;
			int actionId = actionRecord.getId();
			return " " + actionMgr.getCommandSummary(actionId, 200);
		}
		return " <invalid>";
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Get the tool-tip text for the specified tree element.
	 * @param element The tree element for which a label is requested.
	 * @return The text that will be displayed. This includes the full text of the action
	 * command, which might be very long, and might wrap over multiple lines.
	 */
	@Override
	public String getToolTipText(Object element) {
		if (element instanceof UIActionRecord) {
			UIActionRecord actionRecord = (UIActionRecord)element;
			int actionId = actionRecord.getId();

			return actionMgr.getCommand(actionId);
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
