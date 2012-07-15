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

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;

import com.buildml.eclipse.SubEditor;
import com.buildml.model.Packages;
import com.buildml.utils.errors.ErrorCode;

/**
 * Label provider for the "package" column of the TreeViewer which is the main viewer for the
 * ActionsEditor class.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class ActionsEditorLabelCol2Provider extends ColumnLabelProvider implements ILabelProvider {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** 
	 * The Packages Manager object we'll use for querying file information from 
	 * the BuildStore.
	 */
	private Packages pkgMgr;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Construct a new ActionsEditorLabelCol1Provider object, which provides text and image
	 * labels for the FilesEditor class.
	 * @param editor The editor that we're providing text/images for.
	 * @param pkgMgr The Packages object containing path component information.
	 */
	public ActionsEditorLabelCol2Provider(SubEditor editor, Packages pkgMgr) {
		this.pkgMgr = pkgMgr;
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Get the image for the specified tree element.
	 * @param element The element for which an image is requested.
	 * @return An Image for the specified column.
	 */
	public Image getImage(Object element) {
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Get the column text for the specified tree element.
	 * @param element The tree element for which a label (or two) is requested.
	 * @return The text that will be displayed in the column.
	 */
	public String getText(Object element) {
		
		if (element instanceof UIActionRecord) {
			UIActionRecord actionRecord = (UIActionRecord)element;
			int actionId = actionRecord.getId();
			int pkgId = pkgMgr.getTaskPackage(actionId);
			if (pkgId == ErrorCode.NOT_FOUND) {
				return "<invalid>";
			}
			if (pkgId == 0) {
				return "";
			}
			String pkgName = pkgMgr.getPackageName(pkgId);
			if (pkgName == null) {
				return "<invalid>";
			}
			return pkgName;
		}

		return "<invalid>";
	}
	
	/*-------------------------------------------------------------------------------------*/
}
