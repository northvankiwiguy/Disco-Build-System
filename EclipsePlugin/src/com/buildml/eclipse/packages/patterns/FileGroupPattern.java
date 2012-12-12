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

import com.buildml.eclipse.bobj.UIFileGroup;

/**
 * A Graphiti pattern for managing the "FileGroup" graphical element in a BuildML diagram.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class FileGroupPattern extends AbstractPattern implements IPattern {

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
	private static final int FILE_GROUP_WIDTH = 50;
	private static final int FILE_GROUP_HEIGHT = 50;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link FileGroupPattern} object.
	 */
	public FileGroupPattern() {
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
		return "File Group";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.pattern.AbstractPattern#isMainBusinessObjectApplicable(java.lang.Object)
	 */
	@Override
	public boolean isMainBusinessObjectApplicable(Object mainBusinessObject) {
		return mainBusinessObject instanceof UIFileGroup;
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.graphiti.pattern.AbstractPattern#isPatternControlled(
	 * 					org.eclipse.graphiti.mm.pictograms.PictogramElement)
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
	 * 					org.eclipse.graphiti.mm.pictograms.PictogramElement)
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
		
		/* A UIFileGroup can be added to a Diagram */
		if (context.getNewObject() instanceof UIFileGroup) {
			if (context.getTargetContainer() instanceof Diagram) {
				return true;
			}
		}
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create the visual representation of a UIFileGroup on the parent Diagram.
	 */
	@Override
	public PictogramElement add(IAddContext context) {
		
		/* 
		 * What are we adding, and where are we adding it?
		 */
		UIFileGroup addedFileGroup = (UIFileGroup)context.getNewObject();
		Diagram targetDiagram = (Diagram)context.getTargetContainer();
		
		/*
		 * How many boxes will be shown? This helps us distinguish between file groups
		 * containing a single file, versus multiple files.
		 */
		int numBoxes = 3;
		
		IPeCreateService peCreateService = Graphiti.getPeCreateService();
		IGaService gaService = Graphiti.getGaService();
		ContainerShape containerShape =
				peCreateService.createContainerShape(targetDiagram, true);

		/*
		 * Create an invisible outer rectangle. The smaller file boxes will be placed
		 * (or stacked) inside this.
		 */
		Rectangle invisibleRectangle =
				gaService.createInvisibleRectangle(containerShape);
		gaService.setLocationAndSize(invisibleRectangle,
				context.getX(), context.getY(), FILE_GROUP_WIDTH, FILE_GROUP_HEIGHT);

		/*
		 * Create the required number of (visible) boxes within the outer shape.
		 */
		for (int i = 0; i != numBoxes; i++) {
			
			Rectangle box = gaService.createRectangle(invisibleRectangle);
			box.setForeground(manageColor(FOREGROUND_COLOUR));
			box.setBackground(manageColor(BACKGROUND_COLOUR));
			box.setLineWidth(2);
			gaService.setLocationAndSize(box, i * 3, i * 4,
				FILE_GROUP_WIDTH - (numBoxes - 1) * 5, 
				FILE_GROUP_HEIGHT - (numBoxes - 1) * 5);
		}

		/* add a chopbox anchor to the shape */
		peCreateService.createChopboxAnchor(containerShape);

		/* create a link between the shape and the business object, and display it. */
		link(containerShape, addedFileGroup);
		layoutPictogramElement(containerShape);
		return containerShape;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether a business object can be added to the diagram.
	 */
	@Override
	public boolean canCreate(ICreateContext context) {
		
		/* yes, everything from the palette can be added */
        return context.getTargetContainer() instanceof Diagram;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create a new UIFileGroup icon, which would have been selected via the Diagram's palette.
	 */
	public Object[] create(ICreateContext context) {
		
		/* create new UIFileGroup */
		UIFileGroup newFileGroup = new UIFileGroup(0);

		/* Add model element to the same resource as the diagram */
		getDiagram().eResource().getContents().add(newFileGroup);

		/* do the add procedure */
		addGraphicalRepresentation(context, newFileGroup);

		/* return newly created business object(s) */
		return new Object[] { newFileGroup };
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
