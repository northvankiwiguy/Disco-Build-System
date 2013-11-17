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
import com.buildml.eclipse.actions.ActionChangeOperation;
import com.buildml.eclipse.bobj.UIFileGroup;
import com.buildml.eclipse.packages.PackageDiagramEditor;
import com.buildml.eclipse.packages.layout.LayoutAlgorithm;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.BmlMultiOperation;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageRootMgr;
import com.buildml.model.IPackageMemberMgr.MemberLocation;
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

		/* locate all the necessary things in our environment */
		IBuildStore buildStore = EclipsePartUtils.getActiveBuildStore();
		PackageDiagramEditor pde = EclipsePartUtils.getActivePackageDiagramEditor();
		List<Object> selectedObjects = GraphitiUtils.getSelection();
		if ((buildStore == null) || (pde == null) || (selectedObjects == null)) {
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
		
		/* add the creation of the new action to the undo/redo stack */
		BmlMultiOperation multiOp = new BmlMultiOperation("New Action");
		ActionChangeOperation newActionOp = new ActionChangeOperation("", actionId);
		newActionOp.recordNewAction();
		multiOp.add(newActionOp);
		
		/*
		 * If the user selected a UIFileGroup, attach that file group to the new action's "Input" slot.
		 */
		if (selectedObjects.size() == 1) {
			
			/* insert selected file group into action's "input" slot */
			int fileGroupId = ((UIFileGroup)(selectedObjects.get(0))).getId();
			int slotId = actionMgr.getSlotByName(actionId, "Input");
			if ((slotId == ErrorCode.NOT_FOUND) || 
				(actionMgr.setSlotValue(actionId, slotId, fileGroupId) != ErrorCode.OK)) {
					AlertDialog.displayErrorDialog("Can't add action", 
							"Unable to attach new action to selected file group");
			}
			
			/* successfully added */
			else {
				/* record this slot change in our undo/redo stack */ 
				ActionChangeOperation newSlotOp = new ActionChangeOperation("", actionId);
				newSlotOp.recordSlotChange(slotId, null, fileGroupId);
				multiOp.add(newSlotOp);
				
				/* 
				 * Finally, position the action to right of UIFileGroup. This doesn't need to be in
				 * undo/redo history because the action didn't exist until now.
				 */
				MemberLocation fgLocation = pkgMemberMgr.getMemberLocation(
													IPackageMemberMgr.TYPE_FILE_GROUP, fileGroupId);
				if (fgLocation != null) {
					int x = fgLocation.x;
					int y = fgLocation.y;
					LayoutAlgorithm layoutAlgorithm = pde.getLayoutAlgorithm();
					x += layoutAlgorithm.getSizeOfPictogram(IPackageMemberMgr.TYPE_FILE_GROUP).getWidth();
					x += layoutAlgorithm.getXPadding();
					pkgMemberMgr.setMemberLocation(IPackageMemberMgr.TYPE_ACTION, actionId, x, y);
				}	
			}
		}

		multiOp.recordOnly();
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * This handler is only enabled when 0 or 1 UIActions are selected. In the case of 0
	 * UIActions selected, we'll actually see that the background Diagram is selected.
	 */
	@Override
	public boolean isEnabled() {

		/* fetch the list of selected business objects (actions, file groups, etc) */
		List<Object> selectedObjects = GraphitiUtils.getSelection();
		if (selectedObjects == null) {
			return false;
		}
		
		/* there must be exactly 0 or 1 things selected */
		int size = selectedObjects.size();
		if (size > 1) {
			return false;
		}
		
		/* if there's one thing, it must be a UIFileGroup */
		if (size == 1) {
			return selectedObjects.get(0) instanceof UIFileGroup;
		}
		
		return true;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
