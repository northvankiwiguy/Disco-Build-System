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

package com.arapiki.disco.eclipse.utils;

import java.util.Iterator;

import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.arapiki.disco.eclipse.DiscoMainEditor;
import com.arapiki.disco.eclipse.files.DiscoFilesEditor;
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
	 * Returns the currently active DiscoMainEditor. If the current editor is not a
	 * DiscoMainEditor, return null;
	 * @return The currently active DiscoMainEditor instance, or null;
	 */
	public static DiscoMainEditor getActiveDiscoMainEditor() {
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
		if (!(part instanceof DiscoMainEditor)) {
			return null;
		}
		return (DiscoMainEditor)part;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Returns the currently active DiscoFilesEditor. If the current editor is not a
	 * DiscoFilesEditor, return null;
	 * @return The currently active DiscoFilesEditor instance, or null;
	 */
	public static DiscoFilesEditor getActiveDiscoFilesEditor() {
		DiscoMainEditor mainEditor = getActiveDiscoMainEditor();
		if (mainEditor == null) {
			return null;
		}
		IEditorPart subEditor = mainEditor.getActiveSubEditor();
		if (subEditor instanceof DiscoFilesEditor) {
			return (DiscoFilesEditor)subEditor;
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Returns the BuildStore for the currently active editor.
	 * @return The currently active BuildStore instance, or null;
	 */
	public static BuildStore getActiveBuildStore() {
		DiscoMainEditor mainEditor = getActiveDiscoMainEditor();
		if (mainEditor == null) {
			return null;
		}
		return mainEditor.getBuildStore();
	}

	/*-------------------------------------------------------------------------------------*/
}
