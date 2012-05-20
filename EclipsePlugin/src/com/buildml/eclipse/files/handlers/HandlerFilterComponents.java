package com.buildml.eclipse.files.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.files.FilesEditor;
import com.buildml.eclipse.utils.ComponentFilterDialog;
import com.buildml.model.types.ComponentSet;

/**
 * Handler for when the user selects the "filterComponent" command, which triggers
 * a pop-up dialog to be shown. This dialog permits the user to select the subset
 * of components they wish to view in the editor.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class HandlerFilterComponents extends AbstractHandler {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Handle execution of the filterComponents command. 
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		/* figure out which editor is currently active */
		MainEditor editor = (MainEditor) HandlerUtil.getActiveEditor(event);
		IEditorPart editorPart = editor.getActiveSubEditor();
		
		/* 
		 * For the file editor, start a "ComponentFilterDialog" and if the user
		 * presses "OK", update the editor's filter ComponentSet with the 
		 * newly selected set.
		 */
		if (editorPart instanceof FilesEditor) {
			FilesEditor filesEditor = (FilesEditor)editorPart;
			ComponentSet compFilterSet = filesEditor.getFilterComponentSet();
			
			ComponentFilterDialog compSelDialog = 
				new ComponentFilterDialog(compFilterSet);
			compSelDialog.open();
			
			int retCode = compSelDialog.getReturnCode();
			if (retCode == Dialog.OK) {
				filesEditor.setFilterComponentSet(compSelDialog.getComponentSet());

			} else if (retCode == Dialog.CANCEL) {
				/* do nothing - the Dialog was canceled */
				
			} else {
				System.out.println("Unknown return code");
			}

		}
		return null;
	}

	/*-------------------------------------------------------------------------------------*/
}
