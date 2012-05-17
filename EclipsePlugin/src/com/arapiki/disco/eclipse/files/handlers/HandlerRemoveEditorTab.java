package com.arapiki.disco.eclipse.files.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import com.arapiki.disco.eclipse.DiscoMainEditor;
import com.arapiki.disco.eclipse.utils.AlertDialog;

/**
 * A Command Handler for removing an existing editor tab.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class HandlerRemoveEditorTab extends AbstractHandler {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		/* fetch the active editor, and its BuildStore, and the active sub-editor. */
		DiscoMainEditor mainEditor = (DiscoMainEditor)HandlerUtil.getActiveEditor(event);

		/* if allowed, remove the current page */
		int activeTab = mainEditor.getActivePage();
		if (mainEditor.isPageRemovable(activeTab)) {
			mainEditor.removePage(activeTab);
		}
		
		else {
			AlertDialog.displayErrorDialog("Unable to delete this editor tab.", 
					"The default editor tabs can't be removed.");
		}

		return null;
	}

	/*-------------------------------------------------------------------------------------*/
}
