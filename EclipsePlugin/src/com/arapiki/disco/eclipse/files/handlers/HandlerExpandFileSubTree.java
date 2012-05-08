package com.arapiki.disco.eclipse.files.handlers;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import com.arapiki.disco.eclipse.DiscoMainEditor;
import com.arapiki.disco.eclipse.files.DiscoFilesEditor;
import com.arapiki.disco.eclipse.files.FileRecordDir;

/**
 * 
 * A command handler for implementing the "Expand subtree" menu item. This
 * allows the user to expand an entire sub-tree of a file tree viewer.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class HandlerExpandFileSubTree extends AbstractHandler {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * The user has selected some elements on a file viewer tree, and we should proceed
	 * to expand any of those elements that are of the FileRecordDir type.
	 * @param event The event information describing the selection. 
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		/* 
		 * Determine the details of the selection, as well as which editor the
		 * selected items are part of.
		 */
		TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);		
		DiscoMainEditor editor = (DiscoMainEditor)HandlerUtil.getActiveEditor(event);
		IEditorPart subEditor = editor.getActiveSubEditor();

		/* we only execute if the current editor is a DiscoFilesEditor */
		if (subEditor instanceof DiscoFilesEditor) {
			DiscoFilesEditor filesEditor = (DiscoFilesEditor)subEditor;
			
			/* for each selected path (which is a directory), ask the editor to expand it. */
			for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
				Object node = (Object) iter.next();
				if (node instanceof FileRecordDir) {
					filesEditor.expandSubtree((FileRecordDir)node);
				}
			}
		}
		
		/* for now, return code is always null */
		return null;
	}

	/*-------------------------------------------------------------------------------------*/
}
