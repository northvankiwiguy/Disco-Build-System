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

import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.ICreateContext;
import org.eclipse.graphiti.features.context.IResizeShapeContext;
import org.eclipse.graphiti.mm.algorithms.Ellipse;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.pattern.AbstractPattern;
import org.eclipse.graphiti.pattern.IPattern;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;
import org.eclipse.graphiti.util.ColorConstant;
import org.eclipse.graphiti.util.IColorConstant;

import com.buildml.eclipse.bobj.UIAction;

/**
 * A Graphiti pattern for managing the "Action" graphical element in a BuildML diagram.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class ActionPattern extends AbstractPattern implements IPattern {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/*
	 * Various colour constants used in displaying this element.
	 */
	private static final IColorConstant TEXT_FOREGROUND = IColorConstant.BLACK;
	private static final IColorConstant FOREGROUND_COLOUR = new ColorConstant(98, 131, 167);
	private static final IColorConstant BACKGROUND_COLOUR = new ColorConstant(187, 218, 247);

	/*
	 * Size of this element (in pixels).
	 */
	private static final int ACTION_WIDTH = 150;
	private static final int ACTION_HEIGHT = 50;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link ActionPattern} object.
	 */
	public ActionPattern() {
		super(null);
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Return the name of this element, as will appears in the Diagram's palette.
	 */
	@Override
	public String getCreateName() {
		return "Action";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.pattern.AbstractPattern#isMainBusinessObjectApplicable(java.lang.Object)
	 */
	@Override
	public boolean isMainBusinessObjectApplicable(Object mainBusinessObject) {
		return mainBusinessObject instanceof UIAction;
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.graphiti.pattern.AbstractPattern#isPatternControlled(
	 * 							org.eclipse.graphiti.mm.pictograms.PictogramElement)
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
	 * 							org.eclipse.graphiti.mm.pictograms.PictogramElement)
	 */
	@Override
	protected boolean isPatternRoot(PictogramElement pictogramElement) {
		Object domainObject = getBusinessObjectForPictogramElement(pictogramElement);
		return isMainBusinessObjectApplicable(domainObject);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether a specific business object can be added to the diagram.
	 */
	@Override
	public boolean canAdd(IAddContext context) {
		
		/* yes, a UIAction can be added to a Diagram */
		if (context.getNewObject() instanceof UIAction) {
			if (context.getTargetContainer() instanceof Diagram) {
				return true;
			}
		}
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create the visual representation of a UIAction on the parent Diagram.
	 */
	@Override
	public PictogramElement add(IAddContext context) {
				
		/* 
		 * What are we adding, and where are we adding it?
		 */
		UIAction addedAction = (UIAction) context.getNewObject();
		Diagram targetDiagram = (Diagram) context.getTargetContainer();

		/*
		 * How many ellipses will be shown? This illustrate whether it's
		 * a regular action, or a multi-action.
		 */
		int numEllipses = 3;
		
		IPeCreateService peCreateService = Graphiti.getPeCreateService();
		IGaService gaService = Graphiti.getGaService();
		ContainerShape containerShape =
				peCreateService.createContainerShape(targetDiagram, true);

		/*
		 * Create an invisible outer rectangle. The smaller ellipse(s) will be placed
		 * (or stacked) inside this.
		 */
		Rectangle invisibleRectangle =
				gaService.createInvisibleRectangle(containerShape);
		gaService.setLocationAndSize(invisibleRectangle,
				context.getX(), context.getY(), ACTION_WIDTH, ACTION_HEIGHT);

		/*
		 * Create the required number of ellipse(s) within the overall shape.
		 */
		for (int i = 0; i != numEllipses; i++) {
			
			Ellipse ellipse = gaService.createEllipse(invisibleRectangle);
			ellipse.setForeground(manageColor(FOREGROUND_COLOUR));
			ellipse.setBackground(manageColor(BACKGROUND_COLOUR));
			ellipse.setLineWidth(2);
			gaService.setLocationAndSize(ellipse, i * 3, i * 4,
											ACTION_WIDTH - (numEllipses - 1) * 5, 
											ACTION_HEIGHT - (numEllipses - 1) * 5);
		}

		/* add a chopbox anchor to the shape */
		peCreateService.createChopboxAnchor(containerShape);

		/* create a link between the shape and the business object, and display it. */
		link(containerShape, addedAction);
		layoutPictogramElement(containerShape);
		return containerShape;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.pattern.AbstractPattern#canCreate(org.eclipse.graphiti.features.context.ICreateContext)
	 */
	@Override
	public boolean canCreate(ICreateContext context) {
        return context.getTargetContainer() instanceof Diagram;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether a business object can be added to the diagram.
	 */
	public Object[] create(ICreateContext context) {
		
		/* create new UIAction object */
		UIAction newAction = new UIAction(0);

		/* Add model element to same resource as the parent Diagram */
		getDiagram().eResource().getContents().add(newAction);

		/* do the add procedure */
		addGraphicalRepresentation(context, newAction);

		/* return newly created business object(s) */
		return new Object[] { newAction };
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * The user isn't allowed to resize the object.
	 */
	@Override
	public boolean canResizeShape(IResizeShapeContext context) {
		return false;
	}

	/*-------------------------------------------------------------------------------------*/
}
