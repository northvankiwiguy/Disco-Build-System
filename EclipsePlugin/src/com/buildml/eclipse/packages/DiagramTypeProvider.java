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

import org.eclipse.graphiti.dt.AbstractDiagramTypeProvider;

/**
 * A "Diagram Type Provider" that provides customization information for the
 * PackageDiagramEditor class. When the Graphiti framework wants to learn about this
 * editor and how it behaves, it starts here.
 *
 * @author Peter Smith <psmith@arapiki.com>
 */
public class DiagramTypeProvider extends AbstractDiagramTypeProvider {

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new DiagramTypeProvider. There should be one of these per 
	 * PackageDiagramEditor.
	 */
	public DiagramTypeProvider() {
		
		/* 
		 * Set the Diagram Feature Provider, which informs the framework about the
		 * Diagram's features.
		 */
		setFeatureProvider(new DiagramFeatureProvider(this));
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Request that the corresponding DiagramEditor be updated (from the model) when the
	 * editor starts up.
	 */
	@Override
	public boolean isAutoUpdateAtStartup() {
		return true;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
