/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.packages;

import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.pattern.DefaultFeatureProviderWithPatterns;

import com.buildml.eclipse.packages.patterns.ActionPattern;
import com.buildml.eclipse.packages.patterns.DiagramPattern;
import com.buildml.eclipse.packages.patterns.FileGroupPattern;

/**
 * An object that supports DiagramTypeProvider in providing configuration information
 * for the PackageDiagramEditor.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
/* package */ class DiagramFeatureProvider extends DefaultFeatureProviderWithPatterns {

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new DiagramFeatureProvider object.
	 * 
	 * @param dtp The DiagramTypeProvider that we're linked to.
	 */
	public DiagramFeatureProvider(IDiagramTypeProvider dtp) {
		super(dtp);
		
		/* add a "pattern" for each type of graphical shape that can appear in the diagram */
		addPattern(new FileGroupPattern());
		addPattern(new ActionPattern());
		addPattern(new DiagramPattern());
	}

	/*-------------------------------------------------------------------------------------*/
}
