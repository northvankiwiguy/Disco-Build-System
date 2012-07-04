/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.utils.EclipsePartUtils;

/**
 * Command Handler for moving to the previous editor tab.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class HandlerPreviousEditorTab extends AbstractHandler {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		MainEditor mainEditor = EclipsePartUtils.getActiveMainEditor();
		if (mainEditor != null) {
			mainEditor.previousPage();
		}
		return null;
	}

	/*-------------------------------------------------------------------------------------*/
}
