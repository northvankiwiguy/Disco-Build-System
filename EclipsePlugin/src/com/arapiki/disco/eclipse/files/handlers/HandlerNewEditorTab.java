package com.arapiki.disco.eclipse.files.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.EditorPart;

import com.arapiki.disco.eclipse.DiscoMainEditor;
import com.arapiki.disco.eclipse.files.DiscoFilesEditor;
import com.arapiki.disco.eclipse.tasks.DiscoTasksEditor;
import com.arapiki.disco.eclipse.utils.AlertDialog;
import com.arapiki.disco.eclipse.utils.errors.FatalDiscoError;
import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.Reports;
import com.arapiki.disco.model.types.ComponentSet;
import com.arapiki.disco.model.types.FileSet;

/**
 * Command handler for adding a new tab in the current editor by duplicating the
 * currently visible tab.
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
		DiscoMainEditor mainEditor = (DiscoMainEditor)HandlerUtil.getActiveEditor(event);
		BuildStore buildStore = mainEditor.getBuildStore();
		Reports reports = buildStore.getReports();
		IEditorPart currentEditor = mainEditor.getActiveSubEditor();
		EditorPart newEditor = null;
		
		/* 
		 * Determine exactly which (sub)command should be executed. If this is null, then the user
		 * would have pressed the top-level toolbar button, which doesn't have a parameter attached.
		 */
		String subCmd = event.getParameter("com.arapiki.disco.eclipse.commandParameters.newEditorTab");
		if (subCmd == null) {
			subCmd = "duplicate";
		}
		
		/*
		 * Option 1: Duplicate the current editor tab.
		 */
		if (subCmd.equals("duplicate")) {
			if (currentEditor instanceof DiscoFilesEditor) {
				DiscoFilesEditor existingEditor = (DiscoFilesEditor)currentEditor;
				DiscoFilesEditor newFilesEditor = 
					new DiscoFilesEditor(buildStore, existingEditor.getTitle() + " (copy)");
				newFilesEditor.setOptions(existingEditor.getOptions());
				try {
					newFilesEditor.setFilterComponentSet(
							(ComponentSet)(existingEditor.getFilterComponentSet().clone()));
					newFilesEditor.setVisibilityFilterSet(
							(FileSet)(existingEditor.getVisibilityFilterSet().clone()));
				} catch (CloneNotSupportedException e) {
					throw new FatalDiscoError("Unable to duplicate a DiscoFilesEditor");
				}
				newEditor = newFilesEditor;
			}		
			else if (currentEditor instanceof DiscoTasksEditor) {
				// TODO: support DiscoTasksEditor.
			}
		}
		
		/*
		 * Option 2: New files editor, showing all files.
		 */
		else if (subCmd.equals("allFiles")) {
			DiscoFilesEditor newFilesEditor = new DiscoFilesEditor(buildStore, "All Files");
			newFilesEditor.setVisibilityFilterSet(reports.reportAllFiles());
			newEditor = newFilesEditor;
		}

		/*
		 * Option 3: New files editor, not showing any files.
		 */
		else if (subCmd.equals("emptyFiles")) {
			DiscoFilesEditor newFilesEditor = new DiscoFilesEditor(buildStore, "Empty");
			newFilesEditor.setVisibilityFilterSet(new FileSet(buildStore.getFileNameSpaces()));
			newEditor = newFilesEditor;
		}

		/*
		 * Option 4: New files editor, showing unused files.
		 */
		else if (subCmd.equals("unusedFiles")) {
			DiscoFilesEditor newFilesEditor = new DiscoFilesEditor(buildStore, "Unused Files");
			FileSet unusedFileSet = reports.reportFilesNeverAccessed();
			unusedFileSet.populateWithParents();
			newFilesEditor.setVisibilityFilterSet(unusedFileSet);
			newEditor = newFilesEditor;
		}

		/*
		 * Option 5: New files editor, showing write-only files.
		 */
		else if (subCmd.equals("writeOnlyFiles")) {
			DiscoFilesEditor newFilesEditor = new DiscoFilesEditor(buildStore, "Write-only Files");
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
