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

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.graphiti.ui.editor.DefaultPersistencyBehavior;
import org.eclipse.graphiti.ui.editor.DefaultUpdateBehavior;
import org.eclipse.graphiti.ui.editor.DiagramBehavior;

/**
 * A standard Graphiti class for implementing the behaviour of {@link PackageDiagramEditor}.
 * There is a 1:1 relationship between our diagram editor and this behaviour class.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class PackageDiagramBehaviour extends DiagramBehavior {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The DiagramEditor that we're providing behaviour for */
	private PackageDiagramEditor editor = null;
	
	/** The delegate object that Graphiti queries to see if our underlying model has changed */
	private PackageEditorUpdateBehavior updateBehavior = null;
	
	/** The delegate object that Graphiti queries to manage the persistency of data */
	private EditorPersistencyBehavior persistencyBehaviour = null;
	
	/** Provider for our context menu */
	private PackageDiagramEditorContextMenuProvider contextMenuProvider = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link PackageDiagramBehaviour} object, providing behavioural information
	 * for a {@link PackageDiagramEditor}.
	 * 
	 * @param diagramEditor The diagram editor we're providing behaviour for.
	 */
	public PackageDiagramBehaviour(PackageDiagramEditor diagramEditor) {
		super(diagramEditor);
		editor = diagramEditor;
	}

	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/

	/**
	 * Override the default persistency behaviour of this editor. This allows us to
	 * generate the view by demand-loading from the database, rather than loading a
	 * resource (file) from disk.
	 */
	@Override
	protected DefaultPersistencyBehavior createPersistencyBehavior() {
		if (persistencyBehaviour == null) {
			persistencyBehaviour = new EditorPersistencyBehavior(this);
		}
		return persistencyBehaviour;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.ui.editor.DiagramBehavior#getPersistencyBehavior()
	 */
	@Override
	protected DefaultPersistencyBehavior getPersistencyBehavior() {
		return persistencyBehaviour;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Override the default update behaviour for this editor. This allows us to update
	 * the diagram (pictogram) when the underlying model changes.
	 */
	@Override
	protected DefaultUpdateBehavior createUpdateBehavior() {
		if (updateBehavior == null){
			updateBehavior = new PackageEditorUpdateBehavior(this);
		}
		return updateBehavior;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.ui.editor.DiagramBehavior#getUpdateBehavior()
	 */
	@Override
	public DefaultUpdateBehavior getUpdateBehavior() {
		return updateBehavior;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.ui.editor.DiagramEditor#createContextMenuProvider()
	 */
	@Override
	protected ContextMenuProvider createContextMenuProvider() {
		
		if (contextMenuProvider == null) {
			contextMenuProvider = new PackageDiagramEditorContextMenuProvider(
				editor.getGraphicalViewer(), editor.getActionRegistry(), getConfigurationProvider());
		}
		return contextMenuProvider;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * @return the contextMenuProvider
	 */
	public PackageDiagramEditorContextMenuProvider getContextMenuProvider() {
		return contextMenuProvider;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Override the default update behaviour by not selecting the same pictograms after
	 * an update is complete. This seems like it would be a nice behaviour, but has the
	 * side-effect of scrolling the Diagram in awkward ways.
	 */
	@Override
	public void selectBufferedPictogramElements() {
		/* nothing */
	}

	/*-------------------------------------------------------------------------------------*/

}
