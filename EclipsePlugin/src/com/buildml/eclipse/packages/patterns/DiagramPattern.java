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

package com.buildml.eclipse.packages.patterns;

import org.eclipse.emf.common.command.CommandStack;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.context.impl.AddContext;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.pattern.AbstractPattern;
import org.eclipse.graphiti.pattern.IPattern;

import com.buildml.eclipse.bobj.UIAction;
import com.buildml.eclipse.packages.PackageDiagramEditor;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.model.types.ActionSet;

/**
 * A Graphiti pattern for managing the top-level "Diagram" graphical element in a
 * BuildML diagram.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class DiagramPattern extends AbstractPattern implements IPattern {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The PackageDiagramEditor we're part of */
	private PackageDiagramEditor editor;
	
	/** The IBuildStore that this diagram represents */
	private IBuildStore buildStore;
	
	/** The IPackageMgr in this BuildStore */
	private IPackageMgr pkgMgr;
	
	/** The IActionMgr in this BuildStore */
	private IActionMgr actionMgr;	
	
	/** The pkgMgr ID of the package we're displaying */
	private int pkgId;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link DiagramPattern} object.
	 */
	public DiagramPattern() {
		super(null);
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * It should not be possible to add a "Diagram" from the palette.
	 */
	@Override
	public String getCreateName() {
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.pattern.AbstractPattern#isMainBusinessObjectApplicable(java.lang.Object)
	 */
	@Override
	public boolean isMainBusinessObjectApplicable(Object mainBusinessObject) {
		return mainBusinessObject instanceof Diagram;
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.graphiti.pattern.AbstractPattern#isPatternControlled(
	 * 								org.eclipse.graphiti.mm.pictograms.PictogramElement)
	 */
	@Override
	protected boolean isPatternControlled(PictogramElement pictogramElement) {
		Object domainObject = getBusinessObjectForPictogramElement(pictogramElement);
		return isMainBusinessObjectApplicable(domainObject);
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.graphiti.pattern.AbstractPattern#isPatternRoot(
	 * 								org.eclipse.graphiti.mm.pictograms.PictogramElement)
	 */
	@Override
	protected boolean isPatternRoot(PictogramElement pictogramElement) {
		Object domainObject = getBusinessObjectForPictogramElement(pictogramElement);
		return isMainBusinessObjectApplicable(domainObject);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * We can only update Diagram objects.
	 */
	@Override
	public boolean canUpdate(IUpdateContext context) {
		return (context.getPictogramElement() instanceof Diagram);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Update the Diagram from the model. This will create all of the FileGroups and Actions
	 * that should appear on the canvas. This method will only be called when the
	 * PackageEditorUpdateBehavior.isResourceChanged() method returns true.
	 */
	@Override
	public boolean update(final IUpdateContext context) {
				
		/* determine our editor and BuildStore */
		editor = (PackageDiagramEditor)getDiagramEditor();
		buildStore = editor.getBuildStore();
		pkgMgr = buildStore.getPackageMgr();
		actionMgr = buildStore.getActionMgr();
		pkgId = editor.getPackageId();
		
		final ContainerShape container = (ContainerShape)context.getPictogramElement();
        TransactionalEditingDomain editingDomain = getDiagramEditor().getEditingDomain();

        /*
         * Add each of the Diagram elements. This must be done as part of a transaction.
         */
		CommandStack commandStack = editingDomain.getCommandStack();
		commandStack.execute(new RecordingCommand(editingDomain) {
			@Override
			protected void doExecute() {
				
				/* fetch the complete list of actions in this package */
				ActionSet actions = pkgMgr.getActionsInPackage(pkgId);
				int row = 0, column = 0;
				for (int actionId : actions) {
					UIAction newAction = new UIAction(actionId);

					/*
					 * Determine the location for this pictogram. If the database
					 * has a valid location, use it. If not, perform a very simple
					 * placement algorithm.
					 */
					Integer location[] = actionMgr.getLocation(newAction.getId());
					int x = column * 150, y = row * 50;
					
					if ((location != null) && (location[0] >= 0) && (location[1] >= 0)) {
						x = location[0];
						y = location[1];
					}
					
					/*
					 * TODO: Fix this layout algorithm. For now it simply lays out
					 * actions in rows and columns.
					 */
					container.eResource().getContents().add(newAction);
					AddContext context2 = new AddContext();
					context2.setNewObject(newAction);
					context2.setLocation(x, y);
					context2.setTargetContainer(container);
					getFeatureProvider().addIfPossible(context2);
					column++;
					if (column == 5) {
						column = 0;
						row++;
					}			
				}
			}
			
		});
		
		return true;
	}

	/*-------------------------------------------------------------------------------------*/
}
