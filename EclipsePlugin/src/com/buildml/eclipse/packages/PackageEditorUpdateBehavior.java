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

package com.buildml.eclipse.packages;

import org.eclipse.graphiti.ui.editor.DefaultUpdateBehavior;
import org.eclipse.graphiti.ui.editor.DiagramBehavior;
import org.eclipse.graphiti.ui.editor.DiagramEditor;

/**
 * A Graphiti "behaviour" class to control the update/refresh behaviour for 
 * PackageDiagramEditor. There's exactly one object of this class per PackageDiagramEditor
 * object. This object is consulted to determine whether the diagram's underlying model
 * has changed recently, and therefore the diagram needs to be updated with new pictogram
 * content.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class PackageEditorUpdateBehavior extends DefaultUpdateBehavior {

	/*=====================================================================================*
	 * FIELDS
	 *=====================================================================================*/

	/** Tracks whether the underlying data model has changed */
	private boolean modelHasChanged = false;
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Create a new PackageEditorUpdateBehavior instance, which controls the update/refresh
	 * behaviour for PackageDiagramEditor.
	 * 
	 * @param diagramBehaviour The behaviour class for this diagram.
	 */
	public PackageEditorUpdateBehavior(DiagramBehavior diagramBehaviour) {
		super(diagramBehaviour);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Called by PackageDiagramEditor to indicate that the underlying model has changed.
	 */
	public void markChanged() {
		modelHasChanged = true;
	}
	
	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/

	/**
	 * Called by the Graphiti framework to determine whether the underlying model for
	 * the PackageDiagramEditor has changed since the last update.
	 */
	@Override
	protected boolean isResourceChanged() {		
		/* 
		 * This will cause the pictograms to be updated from the domain model whenever
		 * the DiagramEditor is switched-to. Reset the flag each time we're asked.
		 */
		boolean result = modelHasChanged;
		modelHasChanged = false;
		return result;
	}

	/*-------------------------------------------------------------------------------------*/
	
	/*
	 * This method is called whenever the diagram changes, and the underlying model changes.
	 * The default behaviour is to ask the user what to do to resolve the conflict, but we
	 * just return true to always refresh from the model (which will reset the Diagram).
	 */
	@Override
	protected boolean handleDirtyConflict() {
		return true;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
