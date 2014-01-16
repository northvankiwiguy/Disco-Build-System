/*******************************************************************************
 * Copyright (c) 2014 Arapiki Solutions Inc.
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

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.bobj.UISubPackage;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.model.IBuildStore;
import com.buildml.model.ISubPackageMgr;
import com.buildml.utils.errors.ErrorCode;

/**
 * Eclipse command handler for the "Open Sub-Package" command. Allows the user to drill-down
 * into a selected UISubPackage object.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class HandlerOpenSubPackage extends AbstractHandler {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Execute the handler - open the sub-package's diagram.
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		/* determine which sub-package is selected */
		List<Object> selectedObjects = GraphitiUtils.getSelection();
		UISubPackage subPkg = (UISubPackage) selectedObjects.get(0);
		int subPkgId = subPkg.getId();
		
		/* now compute the package that this sub-package is a type of */
		IBuildStore buildStore = EclipsePartUtils.getActiveBuildStore();
		if (buildStore == null) {
			return null;
		}
		ISubPackageMgr subPkgMgr = buildStore.getSubPackageMgr();
		int pkgId = subPkgMgr.getSubPackageType(subPkgId);
		if (pkgId == ErrorCode.NOT_FOUND) {
			return null;
		}

		/* ask the main editor to open this sub-package's diagram */
		MainEditor mainEditor = EclipsePartUtils.getActiveMainEditor();
		if (mainEditor != null) {
			mainEditor.openPackageDiagram(pkgId);
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * This handler is only relevant if a UISubPackage has been selected.
	 */
	@Override
	public boolean isEnabled() {
		
		List<Object> selectedObjects = GraphitiUtils.getSelection();
		return ((selectedObjects.size() == 1) && (selectedObjects.get(0) instanceof UISubPackage));
	}
	
	/*-------------------------------------------------------------------------------------*/
}
