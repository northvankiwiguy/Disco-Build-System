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

import com.buildml.eclipse.packages.PackageDiagramEditor;
import com.buildml.eclipse.packages.layout.LayoutAlgorithm;
import com.buildml.eclipse.utils.BmlMultiOperation;
import com.buildml.eclipse.utils.EclipsePartUtils;

/**
 * An Eclipse UI Handler for managing the "Auto Layout" UI command.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class HandlerAutoLayout extends AbstractHandler {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		/* determine which editor we're currently looking at */
		PackageDiagramEditor pde = EclipsePartUtils.getActivePackageDiagramEditor();
		if (pde == null) {
			return null;
		}
		
		/* invoke the layout algorithm for this editor's package */
		LayoutAlgorithm layoutAlgorithm = pde.getLayoutAlgorithm();
		BmlMultiOperation multiOp = new BmlMultiOperation("Auto Layout");
		layoutAlgorithm.autoLayoutPackage(multiOp, pde.getPackageId());
		multiOp.recordAndInvoke();
		return null;
	}

	/*-------------------------------------------------------------------------------------*/
}
