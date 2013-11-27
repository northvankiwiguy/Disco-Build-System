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
import org.eclipse.ui.handlers.IHandlerService;

import com.buildml.eclipse.bobj.UIConnection;
import com.buildml.eclipse.bobj.UIFileActionConnection;
import com.buildml.eclipse.bobj.UIMergeFileGroupConnection;
import com.buildml.eclipse.packages.PackageDiagramEditor;
import com.buildml.eclipse.packages.properties.ConnectionPropertyPage;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.eclipse.utils.errors.FatalError;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileGroupMgr;
import com.buildml.model.undo.ActionUndoOp;
import com.buildml.model.undo.FileGroupUndoOp;
import com.buildml.model.undo.MultiUndoOp;

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
		UIConnection bo = (UIConnection)selectedObjects.get(0);
		
		/* all our steps to create a filter must be recorded in the undo/redo stack */
		MultiUndoOp multiOp = new MultiUndoOp();
		
		/*
		 * Handle addition of new filter between a file group and an INPUT slot for
		 * an action (we can't have filters after action OUTPUTs).
		 */
		int filterGroupId = -1;
		if (bo instanceof UIFileActionConnection) {
			UIFileActionConnection connection = (UIFileActionConnection)bo;
			int actionId = connection.getActionId();
			int slotId = connection.getSlotId();
			int fileGroupId = connection.getFileGroupId();
			
			filterGroupId = fileGroupMgr.newFilterGroup(pkgId, fileGroupId);
			if (filterGroupId < 0) {
				AlertDialog.displayErrorDialog("Can't create filter", 
						"Unable to create new filter on this connection");
				return null;
			}
			
			/* modify the action's slot so it now refers to our new filter group */
			ActionUndoOp actionOp = new ActionUndoOp(buildStore, actionId);
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
			filterGroupId = fileGroupMgr.newFilterGroup(pkgId, sourceFileGroupId);
			if (filterGroupId < 0) {
				return filterGroupId;
			}
			if (filterGroupId < 0) {
				AlertDialog.displayErrorDialog("Can't create filter", 
						"Unable to create new filter on this connection");
				return null;
			}
			
			/* modify the members of the merge group so that "index" entry now refers to the filter */
			FileGroupUndoOp fileGroupOp = new FileGroupUndoOp(buildStore, targetFileGroupId);
			Integer currentMembers[] = fileGroupMgr.getSubGroups(targetFileGroupId);
			Integer newMembers[] = currentMembers.clone();
			newMembers[index] = filterGroupId;
			fileGroupOp.recordMembershipChange(Arrays.asList(currentMembers), Arrays.asList(newMembers));
			multiOp.add(fileGroupOp);
		}
		
		/*
		 * Now that we've created an empty filter, pop up the properties page to allow the user
		 * to edit the filter patterns. We do this by storing our multiOp in the UIConnection object, which
		 * is then used by the properties dialog (see ConnectionPropertyPage).
		 */
		bo.setFilterGroupId(filterGroupId);
		bo.setUndoRedoOperation(multiOp);
		
    	/* Open the standard "properties" dialog for UIConnection */
    	String commandId = "org.eclipse.ui.file.properties";
    	IHandlerService handlerService = (IHandlerService) 
    			EclipsePartUtils.getService(IHandlerService.class);
    	try {
			handlerService.executeCommand(commandId, null);
		} catch (Exception e) {
			throw new FatalError("Unable to open Properties Dialog.");
		}
    	
    	/* 
    	 * At this point, the operation has been invoked (by ConnectionPropertyPage.performOK), so detach
    	 * it from the UIConnection. Note that if "cancel" was pressed, we also need to remove the
    	 * filter.
    	 */
    	if (bo.getUndoRedoOperation() == null) {
    		bo.removeFilter();
    	}
    	bo.setUndoRedoOperation(null);
    	
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
	
	/*-------------------------------------------------------------------------------------*/
}
