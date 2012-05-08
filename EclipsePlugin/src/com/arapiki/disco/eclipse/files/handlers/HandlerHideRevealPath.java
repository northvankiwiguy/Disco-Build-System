package com.arapiki.disco.eclipse.files.handlers;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.arapiki.disco.eclipse.DiscoMainEditor;
import com.arapiki.disco.eclipse.files.DiscoFilesEditor;
import com.arapiki.disco.eclipse.utils.errors.FatalDiscoError;
import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.types.FileRecord;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class HandlerHideRevealPath extends AbstractHandler {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
				
		/*
		 * figure out whether to reveal or hide the element
		 */
		boolean reveal = false;
		String cmdName = "com.arapiki.disco.eclipse.commandParameters.hideRevealType";
		String opType = event.getParameter(cmdName);
		if (opType.equals("hide")) {
			reveal = false;
		} else if (opType.equals("reveal")) {
			reveal = true;
		} else {
			throw new FatalDiscoError("Unable to handle command: " + cmdName);
		}

		/* fetch the active editor, and its BuildStore */
		DiscoMainEditor editor = (DiscoMainEditor)HandlerUtil.getActiveEditor(event);
		DiscoFilesEditor filesEditor = (DiscoFilesEditor)editor.getActiveSubEditor();
		BuildStore buildStore = editor.getBuildStore();

		/* fetch the FileRecord nodes that were selected */
		TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);

		/* 
		 * Iterate through all the selected elements, and perform the operation on each.
		 */
		Iterator<?> iter = selection.iterator();
		while (iter.hasNext()) {
			Object item = iter.next();
			if (item instanceof FileRecord) {
				FileRecord fr = (FileRecord)item;
				filesEditor.setPathVisibilityState(fr, reveal);
			}
		}
		
		return null;
	}

	/*-------------------------------------------------------------------------------------*/
}
