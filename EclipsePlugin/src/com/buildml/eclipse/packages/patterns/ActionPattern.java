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
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.mm.algorithms.Ellipse;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.algorithms.styles.Orientation;
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
import com.buildml.eclipse.packages.PackageDiagramEditor;
import com.buildml.model.IActionMgr;
import com.buildml.model.IActionTypeMgr;
import com.buildml.model.IBuildStore;

/**
 * A Graphiti pattern for managing the "Action" graphical element in a BuildML diagram.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class ActionPattern extends AbstractPattern implements IPattern {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The PackageDiagramEditor we're part of */
	private PackageDiagramEditor editor;
	
	/** The IBuildStore that this diagram represents */
	private IBuildStore buildStore;
	
	/** The IActionMgr owned by this BuildStore */
	private IActionMgr actionMgr;

	/** The IActionTypeMgr owned by this BuildStore */
	private IActionTypeMgr actionTypeMgr;

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
	 * Return the name of this element.
	 */
	@Override
	public String getCreateName() {
		return "Action";
	}

	/*-------------------------------------------------------------------------------------*/

	/* 
	 * We do not want "Action" to appear in the palette.
	 */
	@Override
	public boolean isPaletteApplicable() {
		return false;
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
		
		/* determine our editor and BuildStore */
		editor = (PackageDiagramEditor)getDiagramEditor();
		buildStore = editor.getBuildStore();
		actionMgr = buildStore.getActionMgr();
		actionTypeMgr = buildStore.getActionTypeMgr();
		
		/* 
		 * What are we adding, and where are we adding it?
		 */
		UIAction addedAction = (UIAction) context.getNewObject();
		int actionId = addedAction.getId();

		Diagram targetDiagram = (Diagram) context.getTargetContainer();
		
		int actionTypeId = actionMgr.getActionType(actionId);
		String actionTypeName = actionTypeMgr.getName(actionTypeId);

		/*
		 * How many ellipses will be shown? This illustrate whether it's
		 * a regular action, or a multi-action.
		 */
		int numEllipses = 1;
		
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
		 * Create the required number of ellipse(s) within the overall shape. When
		 * multiple ovals are drawn, we need to position them (and the inner text)
		 * carefully.
		 */
		int xOverlap = 3;
		int yOverlap = 4;
		int xAdjust = (numEllipses - 1) * xOverlap;
		int yAdjust = (numEllipses - 1) * yOverlap;
		for (int i = 0; i != numEllipses; i++) {
			
			Ellipse ellipse = gaService.createEllipse(invisibleRectangle);
			ellipse.setForeground(manageColor(FOREGROUND_COLOUR));
			ellipse.setBackground(manageColor(BACKGROUND_COLOUR));
			ellipse.setLineWidth(2);
			gaService.setLocationAndSize(ellipse, i * xOverlap, i * yOverlap, 
										 ACTION_WIDTH - xAdjust, ACTION_HEIGHT - yAdjust);
		}
		
		/*
		 * Provide a quick title for the command to be displayed within the oval. This
		 * takes "Shell:" and appends the shell command's base name.
		 * TODO: replace this with more generic code, but only after we support more than
		 * shell commands.
		 */
		String actionCommand = actionMgr.getCommand(actionId);
		String actionCommandSplit[] = actionCommand.split(" ", 2);
		String actionCommandBase = actionCommandSplit[0];
		int slashIndex = actionCommandBase.lastIndexOf('/');
		if (slashIndex != -1){
			actionCommandBase = actionCommandBase.substring(slashIndex + 1);
		}
		
		/* draw the action type's name inside the oval(s) */
		Text actionTypeNameText = gaService.createPlainText(invisibleRectangle, 
				"Shell: " + actionCommandBase);
		actionTypeNameText.setFilled(false);
		actionTypeNameText.setForeground(manageColor(TEXT_FOREGROUND));
		actionTypeNameText.setHorizontalAlignment(Orientation.ALIGNMENT_CENTER);
		gaService.setLocationAndSize(actionTypeNameText, xAdjust, yAdjust,
										ACTION_WIDTH - xAdjust, ACTION_HEIGHT - yAdjust);
		
		/* add a chopbox anchor to the shape */
		peCreateService.createChopboxAnchor(containerShape);
		
		
		/* create a link between the shape and the business object, and display it. */
		link(containerShape, addedAction);
		layoutPictogramElement(containerShape);
		return containerShape;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * No, we can't create new actions via the diagram editor.
	 */
	@Override
	public boolean canCreate(ICreateContext context) {
        return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether a business object can be added to the diagram.
	 */
	public Object[] create(ICreateContext context) {
		
		// TODO: is this method necessary?
		
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

	/*
	 * The UIAction business object may have changed. Update the pictogram from the model.
	 */
	@Override
	public boolean update(IUpdateContext context) {
		editor.refresh();
		return super.update(context);
	}
}
