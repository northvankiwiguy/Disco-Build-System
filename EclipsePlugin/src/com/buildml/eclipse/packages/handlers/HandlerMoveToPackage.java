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
import org.eclipse.graphiti.ui.platform.GraphitiConnectionEditPart;
import org.eclipse.graphiti.ui.platform.GraphitiShapeEditPart;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.buildml.eclipse.bobj.UIAction;
import com.buildml.eclipse.bobj.UIDirectory;
import com.buildml.eclipse.bobj.UIFile;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.model.IBuildStore;

/**
 * An Eclipse UI Handler for managing the "Move to Package" UI command.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class HandlerMoveToPackage extends AbstractHandler {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Execute the "Move to Package" command.
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IBuildStore buildStore = EclipsePartUtils.getActiveBuildStore();
		
		List<Object> objectList = getSelectedObjects();

		MoveToPackageDialog dialog = new MoveToPackageDialog(buildStore);
		int status = dialog.open();
		if (status == MoveToPackageDialog.OK) {
			int pkgId = dialog.getPackageId();
			
			// TODO: proceed to refactor...
			System.out.println("selected package " + pkgId);
			for (Iterator iterator = objectList.iterator(); iterator.hasNext();) {
				Object object = (Object) iterator.next();
				System.out.println("Object: " + object);
			}
		}
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether this handler is enabled. To do so, all the selected elements must
	 * be valid/recognized business objects, such as UIAction, UIFile, etc.
	 */
	@Override
	public boolean isEnabled() {
		
		/* 
		 * Get the list of business objects that are selected, return false if an unhandled
		 * object is selected.
		 */
		List<Object> objectList = getSelectedObjects();
		return (objectList != null);
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * @return A list of valid business objects (UIAction, UIFileGroup, etc) that have been
	 * selected by the user. Returns null if any non-valid objects were selected. Note that
	 * connection arrows are silently ignored, rather than flagged as invalid.
	 */
	private List<Object> getSelectedObjects() {
		
		/* get the list of all things that are selected */
		IStructuredSelection selection = EclipsePartUtils.getSelection();
		
		/* we'll return a list of valid business objects */
		List<Object> result = new ArrayList<Object>();
		
		Iterator<Object> iter = selection.iterator();
		while (iter.hasNext()) {
			Object obj = iter.next();
			
			/* Graphiti shapes */
			if (obj instanceof GraphitiShapeEditPart) {
				GraphitiShapeEditPart shape = (GraphitiShapeEditPart)obj;
				Object bo = GraphitiUtils.getBusinessObject(shape.getPictogramElement());
				if (bo != null) {
					result.add(bo);
				}
			}
			
			/* silently ignore connections */
			else if (obj instanceof GraphitiConnectionEditPart) {
				/* silently do nothing - not an error */
			}
			
			/* Other objects, selectable from TreeViewers (rather than from Graphiti diagrams) */
			else if ((obj instanceof UIAction) || (obj instanceof UIFile) || (obj instanceof UIDirectory)) {
				result.add(obj);
			}
			
			/* else, anything else is invalid */
			else {
				return null;
			}
		}
		
		/* finally, there must be at least one valid thing selected */
		if (result.size() > 0) {
			return result;
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
