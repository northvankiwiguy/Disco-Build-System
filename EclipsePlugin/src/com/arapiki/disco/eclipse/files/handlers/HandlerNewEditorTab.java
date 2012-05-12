package com.arapiki.disco.eclipse.files.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import com.arapiki.disco.eclipse.DiscoMainEditor;
import com.arapiki.disco.eclipse.files.DiscoFilesEditor;
import com.arapiki.disco.eclipse.tasks.DiscoTasksEditor;
import com.arapiki.disco.eclipse.utils.errors.FatalDiscoError;
import com.arapiki.disco.model.BuildStore;
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
		IEditorPart subEditor = mainEditor.getActiveSubEditor();
		
		/*
		 * Create a new DiscoFilesEditor?
		 */
		if (subEditor instanceof DiscoFilesEditor) {
			DiscoFilesEditor existingEditor = (DiscoFilesEditor)subEditor;
			
			/* 
			 * Create a new DiscoFilesEditor, and populate it with the same state as the
			 * existing editor.
			 */
			DiscoFilesEditor newEditor = 
				new DiscoFilesEditor(buildStore, existingEditor.getTitle() + "(new)");
			newEditor.setOptions(existingEditor.getOptions());
			try {
				newEditor.setFilterComponentSet(
						(ComponentSet)(existingEditor.getFilterComponentSet().clone()));
				newEditor.setVisibilityFilterSet(
						(FileSet)(existingEditor.getVisibilityFilterSet().clone()));
			} catch (CloneNotSupportedException e) {
				throw new FatalDiscoError("Unable to duplicate a DiscoFilesEditor");
			}
			
			/* add the new editor as a new tab */
			mainEditor.newPage(newEditor);
			mainEditor.setActiveEditor(newEditor);
		}
		
		/*
		 * Create a new DiscoTasksEditor?
		 */
		else if (subEditor instanceof DiscoTasksEditor) {
			// TODO: support DiscoTasksEditor.
		}
		
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
