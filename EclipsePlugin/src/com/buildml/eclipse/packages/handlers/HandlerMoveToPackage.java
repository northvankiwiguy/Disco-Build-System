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
import org.eclipse.core.runtime.Status;
import org.eclipse.graphiti.ui.platform.GraphitiConnectionEditPart;
import org.eclipse.graphiti.ui.platform.GraphitiShapeEditPart;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.bobj.UIAction;
import com.buildml.eclipse.bobj.UIDirectory;
import com.buildml.eclipse.bobj.UIFile;
import com.buildml.eclipse.bobj.UIFileGroup;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMemberMgr.MemberDesc;
import com.buildml.refactor.CanNotRefactorException;
import com.buildml.refactor.IImportRefactorer;

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
		
		List<MemberDesc> members = getSelectedObjects();

		MoveToPackageDialog dialog = new MoveToPackageDialog(buildStore);
		int status = dialog.open();
		if (status == MoveToPackageDialog.OK) {
			int pkgId = dialog.getPackageId();

			/* ask the refactorer to perform the redo */
			MainEditor editor = EclipsePartUtils.getActiveMainEditor();
			if (editor != null) {
				IImportRefactorer refactorer = editor.getImportRefactorer();
				try {
					refactorer.moveMembersToPackage(pkgId, members);
				} catch (CanNotRefactorException e) {
					// TODO: provide better error messages.
					AlertDialog.displayErrorDialog("Can't Move", "Error");
				}
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
		List<MemberDesc> objectList = getSelectedObjects();
		return (objectList != null);
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * @return A list of valid business objects (UIAction, UIFileGroup, etc) that have been
	 * selected by the user. Returns null if any non-valid objects were selected. Note that
	 * connection arrows are silently ignored, rather than flagged as invalid.
	 */
	private List<MemberDesc> getSelectedObjects() {
		
		/* get the list of all things that are selected */
		IStructuredSelection selection = EclipsePartUtils.getSelection();
		
		/* we'll return a list of valid business objects */
		List<MemberDesc> result = new ArrayList<MemberDesc>();
		
		Iterator<Object> iter = selection.iterator();
		while (iter.hasNext()) {
			Object obj = iter.next();
			
			/* Graphiti shapes */
			if (obj instanceof GraphitiShapeEditPart) {
				GraphitiShapeEditPart shape = (GraphitiShapeEditPart)obj;
				Object bo = GraphitiUtils.getBusinessObject(shape.getPictogramElement());
				if (bo instanceof UIAction) {
					result.add(new MemberDesc(IPackageMemberMgr.TYPE_ACTION, ((UIAction)bo).getId(), 0, 0));
				} else if (bo instanceof UIFileGroup) {
					result.add(new MemberDesc(IPackageMemberMgr.TYPE_FILE_GROUP, ((UIFileGroup)bo).getId(), 0, 0));
				}
			}
			
			/* silently ignore connections */
			else if (obj instanceof GraphitiConnectionEditPart) {
				/* silently do nothing - not an error */
			}
			
			/* Other objects, selectable from TreeViewers (rather than from Graphiti diagrams) */
			else if (obj instanceof UIAction){
				result.add(new MemberDesc(IPackageMemberMgr.TYPE_ACTION, ((UIAction)obj).getId(), 0, 0));
			}
			else if (obj instanceof UIFile) {
				result.add(new MemberDesc(IPackageMemberMgr.TYPE_FILE, ((UIFile)obj).getId(), 0, 0));
			}
			else if (obj instanceof UIDirectory) {
				// TODO: expand this into files?
				result.add(new MemberDesc(IPackageMemberMgr.TYPE_FILE, ((UIFile)obj).getId(), 0, 0));
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
