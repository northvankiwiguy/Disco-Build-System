/*******************************************************************************
 * Copyright (c) 2011 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

import com.buildml.eclipse.SubEditor;
import com.buildml.eclipse.utils.EclipsePartUtils;

/**
 * A command handler for implementing the "Edit->Copy" menu item. This
 * allows the user to copy the selection into the system clipboard.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class HandlerCopyCommand extends AbstractHandler {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		/* we delegate the "copy" command to the active sub editor */
		SubEditor subEditor = EclipsePartUtils.getActiveSubEditor();
		if (subEditor != null) {
			ISelection selection = HandlerUtil.getCurrentSelection(event);
			
			/* only delegate if there's a non-empty selection */
			if (!selection.isEmpty()) {
				Clipboard clipboard = new Clipboard(Display.getCurrent());
				subEditor.doCopyCommand(clipboard, selection);
				clipboard.dispose();
			}
		}
		
		/* for now, return code is always null */
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
