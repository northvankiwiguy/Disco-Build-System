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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;

import com.buildml.eclipse.Activator;
import com.buildml.eclipse.SubEditor;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.impl.BuildTasks;
import com.buildml.utils.print.PrintUtils;

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
	
	/** The BuildStore that we're viewing. */
	private IBuildStore buildStore;
	
	/** The FileMgr associated with the BuildStore. */
	private IFileMgr fileMgr;
	
	/** Image representing the "action" icon. */
	private Image actionImage;
	
	/** The maximum number of characters wide that a tooletip should be */
	private final int toolTipWrapWidth = 120;
	
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
		this.buildStore = actionMgr.getBuildStore();
		this.fileMgr = this.buildStore.getFileMgr();
		
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

			/* We'll accumulate the full text of the tooltip in a StringBuffer */
			StringBuffer fullText = new StringBuffer();
			
			/* 
			 * First, output the command's current working directory, taking care to
			 * wrap the path if it's too long.
			 */
			int cmdDirId = actionMgr.getDirectory(actionId);
			String dirString = fileMgr.getPathName(cmdDirId);
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			PrintStream printStream = new PrintStream(outStream);
			PrintUtils.indentAndWrap(printStream, dirString, 0, toolTipWrapWidth);
			fullText.append("Directory:\n");
			fullText.append(outStream.toString());
			fullText.append("\n");
			
			/* Compute the action's command string, taking care to wrap it where necessary. */
			String cmdText = actionMgr.getCommand(actionId);
			outStream.reset();
			PrintUtils.indentAndWrap(printStream, cmdText, 0, toolTipWrapWidth);
			fullText.append("Shell command:\n");
			fullText.append(outStream.toString());
			
			/* we're done - return the full string */
			return fullText.toString();
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
