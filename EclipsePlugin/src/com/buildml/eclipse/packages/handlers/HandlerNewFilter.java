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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.graphiti.ui.internal.parts.IConnectionEditPart;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.buildml.eclipse.bobj.UIFileActionConnection;
import com.buildml.eclipse.bobj.UIMergeFileGroupConnection;
import com.buildml.eclipse.packages.PackageDiagramEditor;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageRootMgr;

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
		if (buildStore == null) {
			return null;
		}
		PackageDiagramEditor pde = EclipsePartUtils.getActivePackageDiagramEditor();
		if (pde == null) {
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
		IStructuredSelection selection = EclipsePartUtils.getSelection();
		if (selection.size() != 1) {
			return false;
		}
		
		/* If this is a connection object, fetch the underlying business object */
		Object element = selection.getFirstElement();
		if (!(element instanceof IConnectionEditPart)) {
			return false;
		}
		Object bo = GraphitiUtils.getBusinessObject(
				((IConnectionEditPart)element).getPictogramElement());

		/* check whether this is a connection we know about */
		if (!(bo instanceof UIFileActionConnection) && !(bo instanceof UIMergeFileGroupConnection)) {
			return false;
		}
		
		// TODO: check whether this connection already has a filter.

		return true;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
}
