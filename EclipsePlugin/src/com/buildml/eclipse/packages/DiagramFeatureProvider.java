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
import org.eclipse.graphiti.features.IAddFeature;
import org.eclipse.graphiti.features.IDeleteFeature;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.IDeleteContext;
import org.eclipse.graphiti.pattern.DefaultFeatureProviderWithPatterns;

import com.buildml.eclipse.bobj.UIFileActionConnection;
import com.buildml.eclipse.packages.features.AddFileActionConnectionFeature;
import com.buildml.eclipse.packages.features.PackageDiagramDeleteFeature;
import com.buildml.eclipse.packages.patterns.ActionPattern;
import com.buildml.eclipse.packages.patterns.DiagramPattern;
import com.buildml.eclipse.packages.patterns.FileGroupPattern;

/**
 * An object that supports DiagramTypeProvider in providing configuration information
 * for the PackageDiagramEditor.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class DiagramFeatureProvider extends DefaultFeatureProviderWithPatterns {

	/** The one and only "delete" feature provider */
	private PackageDiagramDeleteFeature deleteFeature = null;
	
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

	/**
	 * Return a feature for adding new "things". By default we default to using whatever
	 * Patterns are defined, but we might also add features for types that don't have Patterns.
	 */
	@Override
	public IAddFeature getAddFeature(IAddContext context) {
	    if (context.getNewObject() instanceof UIFileActionConnection) {
	        return new AddFileActionConnectionFeature(this);
	    }
	    return super.getAddFeature(context);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return a feature for handling delete operations. The default feature doesn't behave
	 * as we need it to, so we write our own simple version.
	 */
	@Override
	public IDeleteFeature getDeleteFeature(IDeleteContext context) {
		
		/* we only need a single feature for all delete operations */
		if (deleteFeature == null) {
			deleteFeature = new PackageDiagramDeleteFeature(this);
		}
		return deleteFeature;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
