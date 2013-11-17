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
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.graphiti.ui.internal.parts.IContainerShapeEditPart;
import org.eclipse.jface.viewers.IStructuredSelection;
import com.buildml.eclipse.bobj.UIFileGroup;
import com.buildml.eclipse.packages.PackageDiagramEditor;
import com.buildml.eclipse.packages.patterns.FileGroupPattern;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.model.IBuildStore;

/**
 * An Eclipse UI Handler for managing the "New Action" UI command.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class HandlerNewMergeGroup extends AbstractHandler {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@SuppressWarnings("restriction")
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IBuildStore buildStore = EclipsePartUtils.getActiveBuildStore();
		PackageDiagramEditor pde = EclipsePartUtils.getActivePackageDiagramEditor();
		List<Object> selectedObjects = GraphitiUtils.getSelection();
		if ((buildStore == null) || (pde == null) || (selectedObjects == null)) {
			return null;
		}

		/*
		 * Convert the selected UIFileGroups into a list of fileGroup IDs - we know there's 
		 * at least one UIFileGroup selected, and that all selected elements are UIFileGroups.
		 * We can skip a bunch of error checking because we know what to expect (isEnabled() already
		 * validated for us).
		 */
		List<Integer> subGroupIds = new ArrayList<Integer>(selectedObjects.size());
		for (Iterator<Object> iterator = selectedObjects.iterator(); iterator.hasNext();) {
			UIFileGroup fileGroup = (UIFileGroup)iterator.next();
			subGroupIds.add(fileGroup.getId());
		}

		/* we defer the actual work of creating the merge file group to the FileGroupPattern */
		IStructuredSelection selection = EclipsePartUtils.getSelection();
		IContainerShapeEditPart shapePart = (IContainerShapeEditPart) selection.getFirstElement();
		FileGroupPattern fgp = (FileGroupPattern)GraphitiUtils.getPattern(shapePart.getPictogramElement());
		fgp.createMergeFileGroup(pde.getPackageId(), subGroupIds);
		
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * This handler is only enabled when one or more file groups are selected (and no other
	 * diagram elements are selected).
	 */
	@Override
	public boolean isEnabled() {
		List<Object> selectedObjects = GraphitiUtils.getSelection();
		if (selectedObjects == null) {
			return false;
		}

		/* count the number of UIFileGroups - exit completely if other object types are selected */
		int fileGroupsSelected = 0;
		for (Iterator<Object> iterator = selectedObjects.iterator(); iterator.hasNext();) {
			Object object = (Object) iterator.next();
			
			if (object instanceof UIFileGroup) {
				fileGroupsSelected++;
			} else {
				return false;
			}
		}
	
		/* there must be at least one */
		return fileGroupsSelected >= 1;
	}

	/*-------------------------------------------------------------------------------------*/
}
