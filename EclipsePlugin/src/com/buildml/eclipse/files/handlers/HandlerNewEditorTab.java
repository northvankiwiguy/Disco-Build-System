package com.buildml.eclipse.files.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.EditorPart;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.files.FilesEditor;
import com.buildml.eclipse.tasks.TasksEditor;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.errors.FatalError;
import com.buildml.model.BuildStore;
import com.buildml.model.Reports;
import com.buildml.model.types.ComponentSet;
import com.buildml.model.types.FileSet;

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
		MainEditor mainEditor = (MainEditor)HandlerUtil.getActiveEditor(event);
		BuildStore buildStore = mainEditor.getBuildStore();
		Reports reports = buildStore.getReports();
		IEditorPart currentEditor = mainEditor.getActiveSubEditor();
		EditorPart newEditor = null;
		
		/* 
		 * Determine exactly which (sub)command should be executed. If this is null, then the user
		 * would have pressed the top-level toolbar button, which doesn't have a parameter attached.
		 */
		String subCmd = event.getParameter("com.buildml.eclipse.commandParameters.newEditorTab");
		if (subCmd == null) {
			subCmd = "duplicate";
		}
		
		/*
		 * Option 1: Duplicate the current editor tab.
		 */
		if (subCmd.equals("duplicate")) {
			if (currentEditor instanceof FilesEditor) {
				FilesEditor existingEditor = (FilesEditor)currentEditor;
				FilesEditor newFilesEditor = 
					new FilesEditor(buildStore, existingEditor.getTitle() + " (copy)");
				newFilesEditor.setOptions(existingEditor.getOptions());
				try {
					newFilesEditor.setFilterComponentSet(
							(ComponentSet)(existingEditor.getFilterComponentSet().clone()));
					newFilesEditor.setVisibilityFilterSet(
							(FileSet)(existingEditor.getVisibilityFilterSet().clone()));
				} catch (CloneNotSupportedException e) {
					throw new FatalError("Unable to duplicate a FilesEditor");
				}
				newEditor = newFilesEditor;
			}		
			else if (currentEditor instanceof TasksEditor) {
				// TODO: support TasksEditor.
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
