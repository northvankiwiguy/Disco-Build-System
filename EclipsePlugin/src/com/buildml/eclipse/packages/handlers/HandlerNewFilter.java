/*******************************************************************************
 * Copyright (c) 2013 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.packages.handlers;

import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import com.buildml.eclipse.bobj.UIFileActionConnection;
import com.buildml.eclipse.bobj.UIMergeFileGroupConnection;
import com.buildml.eclipse.packages.PackageDiagramEditor;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.model.IBuildStore;

/**
 * An Eclipse UI Handler for managing the "New Filter" UI command.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class HandlerNewFilter extends AbstractHandler {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IBuildStore buildStore = EclipsePartUtils.getActiveBuildStore();
		PackageDiagramEditor pde = EclipsePartUtils.getActivePackageDiagramEditor();
		if ((buildStore == null) || (pde == null)) {
			return null;
		}
		
		System.out.println("Adding filter");
		return null;
	}

	/*-------------------------------------------------------------------------------------*/
	
	/*
	 * This handler is enabled when exactly one connection arrow is enabled (and no other
	 * diagram elements are selected.
	 */
	@Override
	public boolean isEnabled() {

		/* only a single element may be selected */
		List<Object> selectedObjects = GraphitiUtils.getSelection();
		if (selectedObjects.size() != 1) {
			return false;
		}

		/* check whether this is a connection we know about */
		Object bo = selectedObjects.get(0);
		if (!(bo instanceof UIFileActionConnection) && !(bo instanceof UIMergeFileGroupConnection)) {
			return false;
		}
		
		// TODO: check whether this connection already has a filter.

		return true;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
}
