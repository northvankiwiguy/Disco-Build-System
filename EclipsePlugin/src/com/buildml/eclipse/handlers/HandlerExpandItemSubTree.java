package com.buildml.eclipse.handlers;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.buildml.eclipse.ISubEditor;
import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.utils.EclipsePartUtils;

/**
 * A command handler for implementing the "Expand subtree" menu item. This
 * allows the user to expand an entire sub-tree of a file tree viewer.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class HandlerExpandItemSubTree extends AbstractHandler {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * The user has selected some elements on a TreeViewer, and we should proceed
	 * to expand those elements (although clearly, they'll only be expanded if they
	 * have children, and are not already expanded).
	 * @param event The event information describing the selection. 
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		/* 
		 * Determine the details of the selection, as well as which editor the
		 * selected items are part of.
		 */
		TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);		
		ISubEditor subEditor = EclipsePartUtils.getActiveSubEditor();
			
		/* for each selected item, ask the editor to expand it. */
		for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
			Object node = (Object) iter.next();
			subEditor.expandSubtree(node);
		}
		
		/* for now, return code is always null */
		return null;
	}

	/*-------------------------------------------------------------------------------------*/
}
