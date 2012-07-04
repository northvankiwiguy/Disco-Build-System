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

package com.buildml.eclipse.utils;

import java.util.Iterator;

import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.SubEditor;
import com.buildml.eclipse.files.FilesEditor;
import com.buildml.model.BuildStore;
import com.buildml.model.FileNameSpaces;
import com.buildml.model.types.FileRecord;
import com.buildml.model.types.FileSet;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class EclipsePartUtils {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Given an Eclipse command handler's selection, such as when a user selects a bunch of FileRecord
	 * nodes from a TreeViewer, convert the selection into a FileSet. Selected items that are not of type
	 * FileRecord are ignored.
	 * @param buildStore The BuildStore that stores the selected objects.
	 * @param selection The Eclipse command handler's selection.
	 * @return The equivalent FileSet.
	 */
	public static FileSet getFileSetFromSelection(BuildStore buildStore, TreeSelection selection) {
		
		FileNameSpaces fns = buildStore.getFileNameSpaces();
		FileSet fs = new FileSet(fns);
		Iterator<?> iter = selection.iterator();
		while (iter.hasNext()) {
			Object item = iter.next();
			if (item instanceof FileRecord) {
				fs.add(((FileRecord)item).getId());
			}
		}
		return fs;
	}


	/*-------------------------------------------------------------------------------------*/

	/**
	 * Returns the currently active MainEditor. If the current editor is not a
	 * MainEditor, return null;
	 * @return The currently active MainEditor instance, or null;
	 */
	public static MainEditor getActiveMainEditor() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			return null;
		}
		IWorkbenchPage page = window.getActivePage();
		if (page == null) {
			return null;
		}
		IEditorPart part = page.getActiveEditor();
		if (part == null) {
			return null;
		}
		if (!(part instanceof MainEditor)) {
			return null;
		}
		return (MainEditor)part;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Returns the currently active SubEditor.
	 * @return The currently active SubEditor instance, or null;
	 */
	public static SubEditor getActiveSubEditor() {
		MainEditor mainEditor = getActiveMainEditor();
		if (mainEditor == null) {
			return null;
		}
		return mainEditor.getActiveSubEditor();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Returns the currently active FilesEditor. If the current editor is not a
	 * FilesEditor, return null;
	 * @return The currently active FilesEditor instance, or null;
	 */
	public static FilesEditor getActiveFilesEditor() {
		SubEditor subEditor = EclipsePartUtils.getActiveSubEditor();
		if (subEditor instanceof FilesEditor) {
			return (FilesEditor)subEditor;
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Returns the BuildStore for the currently active editor.
	 * @return The currently active BuildStore instance, or null;
	 */
	public static BuildStore getActiveBuildStore() {
		MainEditor mainEditor = getActiveMainEditor();
		if (mainEditor == null) {
			return null;
		}
		return mainEditor.getBuildStore();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Returns true or false, to specify whether the currently active sub editor supports
	 * the specified feature.
	 * @param feature A textual name for an editor feature.
	 * @return true if the feature is supported, or false. If the active editor is invalid
	 * for some reason, also return false.
	 */
	public static boolean activeSubEditorHasFeature(String feature) {
		SubEditor subEditor = getActiveSubEditor();
		if (subEditor == null) {
			return false;
		}
		return subEditor.hasFeature(feature);
	}

	/*-------------------------------------------------------------------------------------*/
}
