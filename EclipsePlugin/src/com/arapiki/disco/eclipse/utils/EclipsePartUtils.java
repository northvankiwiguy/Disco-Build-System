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

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Shell;

import com.arapiki.disco.eclipse.Activator;
import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.types.FileRecord;
import com.arapiki.disco.model.types.FileSet;

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
				fs.add((FileRecord) item);
			}
		}
		return fs;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @param parent
	 * @param message
	 */
	public static void displayErrorDialog(Shell parent, String message) {
		Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, message);
		ErrorDialog.openError(parent, "Disco Error", "An error occurred and Disco is " +
				"unable to complete the current operation.", status);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @param parent
	 * @param message
	 */
	public static void displayFatalErrorDialog(Shell parent, String message) {
		Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, message);
		ErrorDialog.openError(parent, "Fatal Disco Error", "A fatal error occurred " +
				"and Disco is unable to continue. The program will now exit.", status);
	}

	/*-------------------------------------------------------------------------------------*/
}
