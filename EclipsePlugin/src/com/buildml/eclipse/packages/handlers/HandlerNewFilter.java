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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.buildml.eclipse.actions.ActionChangeOperation;
import com.buildml.eclipse.bobj.UIConnection;
import com.buildml.eclipse.bobj.UIFileActionConnection;
import com.buildml.eclipse.bobj.UIMergeFileGroupConnection;
import com.buildml.eclipse.filegroups.FileGroupChangeOperation;
import com.buildml.eclipse.packages.PackageDiagramEditor;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.BmlMultiOperation;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileGroupMgr;

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
		IFileGroupMgr fileGroupMgr = buildStore.getFileGroupMgr();
		IActionMgr actionMgr = buildStore.getActionMgr();
		PackageDiagramEditor pde = EclipsePartUtils.getActivePackageDiagramEditor();
		if ((buildStore == null) || (pde == null)) {
			return null;
		}
		int pkgId = pde.getPackageId();
		
		List<Object> selectedObjects = GraphitiUtils.getSelection();
		Object bo = (UIConnection)selectedObjects.get(0);
		
		/* all our steps to create a filter must be recorded in the undo/redo stack */
		BmlMultiOperation multiOp = new BmlMultiOperation("Add Filter");
		
		/*
		 * Handle addition of new filter between a file group and an INPUT slot for
		 * an action (we can't have filters after action OUTPUTs).
		 */
		if (bo instanceof UIFileActionConnection) {
			UIFileActionConnection connection = (UIFileActionConnection)bo;
			int actionId = connection.getActionId();
			int slotId = connection.getSlotId();
			int fileGroupId = connection.getFileGroupId();
			
			int filterGroupId = createNewFilter(fileGroupMgr, multiOp, fileGroupId, pkgId);
			if (filterGroupId < 0) {
				AlertDialog.displayErrorDialog("Can't create filter", 
						"Unable to create new filter on this connection");
				return null;
			}
			
			/* modify the action's slot so it now refers to our new filter group */
			ActionChangeOperation actionOp = new ActionChangeOperation("", actionId);
			int oldSlotId = (Integer)actionMgr.getSlotValue(actionId, slotId);
			actionOp.recordSlotChange(slotId, oldSlotId, filterGroupId);
			multiOp.add(actionOp);
		}
 
		else if (bo instanceof UIMergeFileGroupConnection) {
			UIMergeFileGroupConnection connection = (UIMergeFileGroupConnection)bo;
			int sourceFileGroupId = connection.getSourceFileGroupId();
			int targetFileGroupId = connection.getTargetFileGroupId();
			int index = connection.getIndex();
			
			/* create and populate the new filter */
			int filterGroupId = createNewFilter(fileGroupMgr, multiOp, sourceFileGroupId, pkgId);
			if (filterGroupId < 0) {
				AlertDialog.displayErrorDialog("Can't create filter", 
						"Unable to create new filter on this connection");
				return null;
			}
			
			/* modify the members of the merge group so that "index" entry now refers to the filter */
			FileGroupChangeOperation fileGroupOp = new FileGroupChangeOperation("", targetFileGroupId);
			Integer currentMembers[] = fileGroupMgr.getSubGroups(targetFileGroupId);
			Integer newMembers[] = currentMembers.clone();
			newMembers[index] = filterGroupId;
			fileGroupOp.recordMembershipChange(Arrays.asList(currentMembers), Arrays.asList(newMembers));
			multiOp.add(fileGroupOp);
		}
		
		/* invoke the change... */
		multiOp.recordAndInvoke();
		
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
		if (!(bo instanceof UIConnection)) {
			return false;
		}
		UIConnection connection = (UIConnection)bo;
		
		/* for UIFileActionConnection object, we can only add a filter when going into an action */
		if (bo instanceof UIFileActionConnection) {
			UIFileActionConnection fileActionConnection = (UIFileActionConnection)bo;
			if (fileActionConnection.getDirection() == UIFileActionConnection.OUTPUT_FROM_ACTION) {
				return false;
			}
		}
		
		/* finally check whether this connection already has a filter - we can only have one */
		return !(connection.hasFilter());
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Helper method for creating a new filter group, and populating it with an initial
	 * pattern.
	 * 
	 * @param fileGroupMgr	The IFileGroupMgr we create the filter in.
	 * @param multiOp		The BmlMultiOperation to add the changes to.
	 * @param fileGroupId	The file group to base this filter on.
	 * @param pkgId			The package to add the filter into.
	 * @return The new filter's ID.
	 */
	private int createNewFilter(IFileGroupMgr fileGroupMgr,
			BmlMultiOperation multiOp, int fileGroupId, int pkgId) {
			
		/* create the new filter group, populate it with the "ia:**" pattern */
		int filterGroupId = fileGroupMgr.newFilterGroup(pkgId, fileGroupId);
		if (filterGroupId < 0) {
			return filterGroupId;
		}
		
		/* add the ** pattern to the filter group. This must be recorded in the undo/redo stack */
		FileGroupChangeOperation filterOp = new FileGroupChangeOperation("", filterGroupId);
		List<String> newMembers = new ArrayList<String>();
		newMembers.add("ia:**");
		filterOp.recordMembershipChange(new ArrayList<String>(), newMembers);
		multiOp.add(filterOp);
		return filterGroupId;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
