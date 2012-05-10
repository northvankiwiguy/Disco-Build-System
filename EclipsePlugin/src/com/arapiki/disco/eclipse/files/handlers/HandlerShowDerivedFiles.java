package com.arapiki.disco.eclipse.files.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import com.arapiki.disco.eclipse.DiscoMainEditor;
import com.arapiki.disco.eclipse.files.DiscoFilesEditor;
import com.arapiki.disco.eclipse.utils.EclipsePartUtils;
import com.arapiki.disco.eclipse.utils.errors.FatalDiscoError;
import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.Reports;
import com.arapiki.disco.model.types.FileSet;

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
		String cmdName = "com.arapiki.disco.eclipse.commandParameters.derivedType";
		String opType = event.getParameter(cmdName);
		if (opType.equals("direct")) {
			optAll = false;
		} else if (opType.equals("all")) {
			optAll = true;
		} else {
			throw new FatalDiscoError("Unable to handle command: " + cmdName);
		}
		
		/* fetch the FileRecord nodes that are selected in the currently active editor. */
		TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);
		
		/* fetch the active editor, its BuildStore, and the active sub-editor */
		DiscoMainEditor mainEditor = (DiscoMainEditor)HandlerUtil.getActiveEditor(event);
		BuildStore buildStore = mainEditor.getBuildStore();
		IEditorPart subEditor = mainEditor.getActiveSubEditor();
		DiscoFilesEditor existingEditor = (DiscoFilesEditor)subEditor;
		
		/* build a FileSet of all the selected files */
		FileSet selectedFiles = EclipsePartUtils.getFileSetFromSelection(buildStore, selection);

		/* get the set of all derived files */
		Reports reports = buildStore.getReports();
		FileSet derivedFiles = reports.reportDerivedFiles(selectedFiles, optAll);
		derivedFiles.populateWithParents();
		
		/* create a new editor that will display the resulting set */
		DiscoFilesEditor newEditor = 
			new DiscoFilesEditor(buildStore, "Derived files");
		newEditor.setOptions(existingEditor.getOptions());
		newEditor.setVisibilityFilterSet(derivedFiles);
		
		/* add the new editor as a new tab */
		mainEditor.newTab(newEditor);
		mainEditor.setActiveEditor(newEditor);
		
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

}
