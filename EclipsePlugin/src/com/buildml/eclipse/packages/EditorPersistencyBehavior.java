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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.command.CommandStack;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IPeCreateService;
import org.eclipse.graphiti.ui.editor.DefaultPersistencyBehavior;
import org.eclipse.graphiti.ui.editor.DiagramBehavior;

import com.buildml.eclipse.utils.ConversionUtils;
import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.utils.errors.ErrorCode;

/**
 * A customized version of {@link DefaultPersistencyBehavior} that uses our own 
 * method of loading/saving from the database, rather than via a standard resource. 
 *
 * @author Peter Smith <psmith@arapiki.com>
 */
/* package */ class EditorPersistencyBehavior extends DefaultPersistencyBehavior {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The PackageDiagramEditor we're owned by */
	private PackageDiagramEditor diagramEditor;
	
	/** The IBuildStore that the editor represents */
	private IBuildStore buildStore;
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Create a new {@link EditorPersistencyBehavior} object.
	 * 
	 * @param diagramBehaviour The behaviour class for this diagram.
	 */
	public EditorPersistencyBehavior(DiagramBehavior diagramBehaviour) {
		super(diagramBehaviour);

		this.diagramEditor = (PackageDiagramEditor)(diagramBehaviour.getDiagramContainer());
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.ui.editor.DefaultPersistencyBehavior#loadDiagram(org.eclipse.emf.common.util.URI)
	 */
	@Override
	public Diagram loadDiagram(URI uri) {
		
		/* Parse the URI to determine which package we're managing */
		int pkgId = ConversionUtils.extractPkgIdFromURI(uri);
		if (pkgId == ErrorCode.NOT_FOUND) {
			throw new FatalBuildStoreError("Invalid package ID in URI: " + uri);
		}
		buildStore = this.diagramEditor.getBuildStore();
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		String pkgName = pkgMgr.getName(pkgId);

		/*
		 * Create a new Graphiti diagram, and associate it with the package type provider
		 * (as defined in plugin.xml).
		 */
		IPeCreateService peCreateService = Graphiti.getPeCreateService();
		final Diagram diagram = peCreateService.createDiagram("com.buildml.eclipse.diagram.package", 
															  "Package: " + pkgName, true);
		final Resource resource = diagramEditor.getDiagramBehavior().getResourceSet().createResource(uri);
		
		/*
		 * Add the new "Diagram" object. This is the only thing we "load" since all other information
		 * is demand-loaded from the database when it's needed. Note that since we're modifying
		 * the model here, we need to do this as part of an EditingDomain transaction.
		 */
		CommandStack commandStack = diagramEditor.getEditingDomain().getCommandStack();
		commandStack.execute(new RecordingCommand(diagramEditor.getEditingDomain()) {
			@Override
			protected void doExecute() {
				resource.getContents().add(diagram);
			}
		});
		return diagram;
	}

	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.ui.editor.DefaultPersistencyBehavior#saveDiagram(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void saveDiagram(IProgressMonitor monitor) {
		/* not implemented */
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.ui.editor.DefaultPersistencyBehavior#isDirty()
	 */
	@Override
	public boolean isDirty() {
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

}
