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

package com.buildml.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.EditorPart;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.SubEditor;
import com.buildml.eclipse.actions.ActionsEditor;
import com.buildml.eclipse.files.FilesEditor;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.errors.FatalError;
import com.buildml.model.impl.BuildStore;
import com.buildml.model.impl.Reports;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.PackageSet;
import com.buildml.model.types.TaskSet;

/**
 * Command handler for adding a new tab in the current BuildML editor. There area variety
 * of ways that a new tab can be created, and this class handles them all.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class HandlerNewEditorTab extends AbstractHandler {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		/* fetch the active editor, and its BuildStore, and the active sub-editor. */
		MainEditor mainEditor = (MainEditor)HandlerUtil.getActiveEditor(event);
		BuildStore buildStore = mainEditor.getBuildStore();
		Reports reports = buildStore.getReports();
		SubEditor currentEditor = mainEditor.getActiveSubEditor();
		SubEditor newEditor = null;
		
		/* 
		 * Determine exactly which (sub)command should be executed. If this is null, then the user
		 * would have pressed the top-level toolbar button, which doesn't have a parameter attached,
		 * so we'll assume they want "duplicate".
		 */
		String subCmd = event.getParameter("com.buildml.eclipse.commandParameters.newEditorTab");
		if (subCmd == null) {
			subCmd = "duplicate";
		}
		
		/*
		 * Option 1: Duplicate the current editor tab. This is handled differently, depending
		 * on what type is sub-editor is currently visible.
		 */
		if (subCmd.equals("duplicate")) {
			
			/* determine the new name for the new tab */
			int page = mainEditor.getActivePage();
			String newName = "";
			if (page != -1) {
				newName = mainEditor.getPageName(page);
				if (!newName.endsWith("(copy)")) {
					newName = newName + " (copy)";
				}
			}
			
			/* is it a FilesEditor tab? */
			if (currentEditor instanceof FilesEditor) {
				FilesEditor existingEditor = (FilesEditor)currentEditor;
				FilesEditor newFilesEditor = 
					new FilesEditor(buildStore, newName);
				newFilesEditor.setOptions(existingEditor.getOptions());
				try {
					newFilesEditor.setFilterPackageSet(
							(PackageSet)(existingEditor.getFilterPackageSet().clone()));
					newFilesEditor.setVisibilityFilterSet(
							(FileSet)(existingEditor.getVisibilityFilterSet().clone()));
				} catch (CloneNotSupportedException e) {
					throw new FatalError("Unable to duplicate a FilesEditor");
				}
				newEditor = newFilesEditor;
			}
			
			/* or is it an ActionsEditor tab? */
			else if (currentEditor instanceof ActionsEditor) {
				ActionsEditor existingEditor = (ActionsEditor)currentEditor;
				ActionsEditor newActionsEditor = 
					new ActionsEditor(buildStore, newName);
				newActionsEditor.setOptions(existingEditor.getOptions());
				try {
					newActionsEditor.setFilterPackageSet(
							(PackageSet)(existingEditor.getFilterPackageSet().clone()));
					newActionsEditor.setVisibilityFilterSet(
							(TaskSet)(existingEditor.getVisibilityFilterSet().clone()));
				} catch (CloneNotSupportedException e) {
					throw new FatalError("Unable to duplicate a ActionEditor");
				}
				newEditor = newActionsEditor;	
			}
		}
		
		/*
		 * Option 2: New files editor, showing all files.
		 */
		else if (subCmd.equals("allFiles")) {
			FilesEditor newFilesEditor = new FilesEditor(buildStore, "All Files");
			newFilesEditor.setVisibilityFilterSet(reports.reportAllFiles());
			newEditor = newFilesEditor;
		}

		/*
		 * Option 3: New files editor, not showing any files.
		 */
		else if (subCmd.equals("emptyFiles")) {
			FilesEditor newFilesEditor = new FilesEditor(buildStore, "Empty");
			newFilesEditor.setVisibilityFilterSet(new FileSet(buildStore.getFileNameSpaces()));
			newEditor = newFilesEditor;
		}

		/*
		 * Option 4: New files editor, showing unused files.
		 */
		else if (subCmd.equals("unusedFiles")) {
			FilesEditor newFilesEditor = new FilesEditor(buildStore, "Unused Files");
			FileSet unusedFileSet = reports.reportFilesNeverAccessed();
			unusedFileSet.populateWithParents();
			newFilesEditor.setVisibilityFilterSet(unusedFileSet);
			newEditor = newFilesEditor;
		}

		/*
		 * Option 5: New files editor, showing write-only files.
		 */
		else if (subCmd.equals("writeOnlyFiles")) {
			FilesEditor newFilesEditor = new FilesEditor(buildStore, "Write-only Files");
			FileSet writeOnlyFileSet = reports.reportWriteOnlyFiles();
			writeOnlyFileSet.populateWithParents();
			newFilesEditor.setVisibilityFilterSet(writeOnlyFileSet);
			newEditor = newFilesEditor;
		}

		/*
		 * Option 6: Most popular files.
		 */
		else if (subCmd.equals("mostPopularFiles")) {
			AlertDialog.displayErrorDialog("Not Implemented", "This feature is not yet implemented.");
		}
		
		/*
		 * Finish by adding the new editor to the main editor, as a new tab.
		 */
		if (newEditor != null) {
			mainEditor.newPage(newEditor);
			mainEditor.setActiveEditor(newEditor);
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
