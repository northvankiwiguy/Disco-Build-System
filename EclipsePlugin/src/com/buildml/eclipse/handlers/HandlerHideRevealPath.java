package com.buildml.eclipse.handlers;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.SubEditor;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.errors.FatalError;

/**
 * Command handler for the "hide selected items" and "reveal selected items" menu options.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class HandlerHideRevealPath extends AbstractHandler {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
				
		/* Rigure out whether to reveal or hide the element */
		boolean revealState = false;
		String cmdName = "com.buildml.eclipse.commandParameters.hideRevealType";
		String opType = event.getParameter(cmdName);
		if (opType == null) {
			return null;
		}
		if (opType.equals("hide")) {
			revealState = false;
		} else if (opType.equals("reveal")) {
			revealState = true;
		} else {
			throw new FatalError("Unable to handle command: " + cmdName);
		}

		/* fetch the active editor, and its BuildStore */
		SubEditor subEditor = EclipsePartUtils.getActiveSubEditor();
		
		/* fetch the items that were selected */
		TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);

		/* 
		 * Iterate through all the selected elements, and perform the operation on each.
		 */
		Iterator<?> iter = selection.iterator();
		while (iter.hasNext()) {
			Object item = iter.next();
			subEditor.setItemVisibilityState(item, revealState);
		}
		
		return null;
	}

	/*-------------------------------------------------------------------------------------*/
}
