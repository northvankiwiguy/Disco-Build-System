package com.buildml.eclipse.files.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.SubEditor;
import com.buildml.eclipse.files.FilesEditor;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.errors.FatalError;
import com.buildml.model.BuildStore;
import com.buildml.model.Reports;
import com.buildml.model.types.FileSet;

/**
 * Command Handler for opening a new editor tab and showing the list of files that
 * are derived from the currently selected files.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class HandlerShowDerivedFiles extends AbstractHandler {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		/* 
		 * Determine whether the user wants to show all derived files, or just directly
		 * derived files.
		 */
		boolean optAll = false;
		boolean optDerived = false;
		String cmdName = "com.buildml.eclipse.commandParameters.derivedType";
		String opType = event.getParameter(cmdName);
		if (opType.equals("directDerived")) {
			optAll = false;
			optDerived = true;
		} else if (opType.equals("allDerived")) {
			optAll = true;
			optDerived = true;
		} else if (opType.equals("directInput")) {
			optAll = false;
			optDerived = false;
		} else if (opType.equals("allInput")) {
			optAll = true;
			optDerived = false;
		} else {
			throw new FatalError("Unable to handle command: " + cmdName);
		}
		
		/* fetch the FileRecord nodes that are selected in the currently active editor. */
		TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);
		
		/* fetch the active editor, its BuildStore, and the active sub-editor */
		MainEditor mainEditor = (MainEditor)HandlerUtil.getActiveEditor(event);
		BuildStore buildStore = mainEditor.getBuildStore();
		SubEditor subEditor = mainEditor.getActiveSubEditor();
		FilesEditor existingEditor = (FilesEditor)subEditor;
		
		/* build a FileSet of all the selected files */
		FileSet selectedFiles = EclipsePartUtils.getFileSetFromSelection(buildStore, selection);

		/* get the set of all derived files */
		Reports reports = buildStore.getReports();
		FileSet resultFiles;
		if (optDerived) {
			resultFiles = reports.reportDerivedFiles(selectedFiles, optAll);
		} else {
			resultFiles = reports.reportInputFiles(selectedFiles, optAll);			
		}
		
		/* if the result set is empty, don't open an editor, but instead open a dialog */
		if (resultFiles.size() == 0) {
			AlertDialog.displayInfoDialog("No results", 
					"There are no " + (optDerived ? "derived" : "input") + " files to display.");
			return null;
		}
		
		resultFiles.populateWithParents();
		
		/* create a new editor that will display the resulting set */
		FilesEditor newEditor = 
			new FilesEditor(buildStore, "Derived files");
		newEditor.setOptions(existingEditor.getOptions());
		newEditor.setVisibilityFilterSet(resultFiles);
		
		/* add the new editor as a new tab */
		mainEditor.newPage(newEditor);
		mainEditor.setActiveEditor(newEditor);
		
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

}
