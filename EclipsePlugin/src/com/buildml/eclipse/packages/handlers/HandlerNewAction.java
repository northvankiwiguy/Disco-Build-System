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

import com.buildml.eclipse.actions.ActionChangeOperation;
import com.buildml.eclipse.packages.PackageDiagramEditor;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageRootMgr;
import com.buildml.utils.errors.ErrorCode;

/**
 * An Eclipse UI Handler for managing the "New Action" UI command.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class HandlerNewAction extends AbstractHandler {

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
		IActionMgr actionMgr = buildStore.getActionMgr();
		IPackageMemberMgr pkgMemberMgr = buildStore.getPackageMemberMgr();
		IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
		
		/*
		 * For now, add a single new shell command action. The action will be created
		 * in the current diagram editor's package, with the package's source root
		 * used as the action's "directory"
		 */
		int pkgId = pde.getPackageId();
		int srcRootPathId = pkgRootMgr.getPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT);
		if (srcRootPathId == ErrorCode.NOT_FOUND) {
			AlertDialog.displayErrorDialog("Can't add action", "Invalid package ID number.");
			return null;
		}
		int parentActionId = actionMgr.getRootAction("root");
		int actionId = actionMgr.addShellCommandAction(parentActionId, srcRootPathId, "#empty");
		if (actionId < 0) {
			AlertDialog.displayErrorDialog("Can't add action", "Unable to add new shell command action.");
			return null;
		}
		pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_ACTION, actionId, pkgId);
		
		/* add this to the undo/redo stack */
		ActionChangeOperation op = new ActionChangeOperation("New Action", actionId);
		op.recordNewAction();
		op.recordOnly();
		return null;
	}

	/*-------------------------------------------------------------------------------------*/
}
