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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.EclipsePartUtils;

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
		
		/* if allowed, remove the current page */
		MainEditor mainEditor = (MainEditor)HandlerUtil.getActiveEditor(event);
		if (EclipsePartUtils.activeSubEditorHasFeature("removable")) {
			int activeTab = mainEditor.getActivePage();
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
