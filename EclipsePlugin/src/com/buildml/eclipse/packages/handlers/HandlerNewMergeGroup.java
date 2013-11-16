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
		if (buildStore == null) {
			return null;
		}
		PackageDiagramEditor pde = EclipsePartUtils.getActivePackageDiagramEditor();
		if (pde == null) {
			return null;
		}

		/*
		 * Get the selection - we know there's at least one UIFileGroup selected,
		 * and that all selected elements are UIFileGroups. We can skip a bunch
		 * of error checking because we know what to expect (isEnabled() already
		 * validated for us).
		 */
		IStructuredSelection selection = EclipsePartUtils.getSelection();
		Iterator<Object> iter = selection.iterator();
		ArrayList<Integer> subGroupIds = new ArrayList<Integer>(selection.size());
		while (iter.hasNext()) {
			IContainerShapeEditPart element = (IContainerShapeEditPart) iter.next();
			UIFileGroup fileGroup = (UIFileGroup) GraphitiUtils.getBusinessObject(
					((IContainerShapeEditPart)element).getPictogramElement());
			subGroupIds.add(fileGroup.getId());
		}
		
		/* we defer the actual work of creating the merge file group to the FileGroupPattern */
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
		IStructuredSelection selection = EclipsePartUtils.getSelection();
		Iterator<Object> iter = selection.iterator();
		
		int fileGroupsSelected = 0;
		while (iter.hasNext()) {
			Object element = iter.next();
			
			/* Note the selected object is "internal" to Graphiti! (we must access an internal class) */
			if (!(element instanceof IContainerShapeEditPart)) {
				return false;
			}			
						
			/* if this selected item is not a UIFileGroup, we not enabled */
			Object bo = GraphitiUtils.getBusinessObject(
					((IContainerShapeEditPart)element).getPictogramElement());
			if (!(bo instanceof UIFileGroup)) {
				return false;
			}

			/* count the number of UIFileGroup objects we see */
			fileGroupsSelected++;
		}
	
		/* there must be at least one */
		return fileGroupsSelected >= 1;
	}

	/*-------------------------------------------------------------------------------------*/
}
