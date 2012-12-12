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

	/** We should only update the Diagram from the model once */
	private boolean alreadyUpdated = false;
	
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
	 * A Diagram can be updated from the model only once.
	 */
	@Override
	public boolean canUpdate(IUpdateContext context) {
		return !alreadyUpdated;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Update the Diagram from the model. This will create all of the FileGroups and Actions
	 * that should appear on the canvas.
	 */
	@Override
	public boolean update(final IUpdateContext context) {
		
		final ContainerShape container = (ContainerShape)context.getPictogramElement();
        TransactionalEditingDomain editingDomain = getDiagramEditor().getEditingDomain();

        /*
         * Add each of the Diagram elements. This must be done as part of a transaction.
         */
		CommandStack commandStack = editingDomain.getCommandStack();
		commandStack.execute(new RecordingCommand(editingDomain) {
			@Override
			protected void doExecute() {
				UIAction newAction = new UIAction(0);
				container.eResource().getContents().add(newAction);
				AddContext context2 = new AddContext();
				context2.setNewObject(newAction);
				context2.setLocation(50, 50);
				context2.setTargetContainer(container);
				getFeatureProvider().addIfPossible(context2);
			}
			
		});
		
		alreadyUpdated = true;
		return true;
	}
	
	/*-------------------------------------------------------------------------------------*/

}
