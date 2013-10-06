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

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.ICreateContext;
import org.eclipse.graphiti.features.context.IDeleteContext;
import org.eclipse.graphiti.features.context.IMoveShapeContext;
import org.eclipse.graphiti.features.context.IResizeShapeContext;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.mm.algorithms.Ellipse;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.algorithms.styles.Orientation;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.PictogramLink;
import org.eclipse.graphiti.pattern.AbstractPattern;
import org.eclipse.graphiti.pattern.IPattern;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;
import org.eclipse.graphiti.util.ColorConstant;
import org.eclipse.graphiti.util.IColorConstant;

import com.buildml.eclipse.actions.ActionChangeOperation;
import com.buildml.eclipse.bobj.UIAction;
import com.buildml.eclipse.packages.PackageDiagramEditor;
import com.buildml.model.IActionMgr;
import com.buildml.model.IActionTypeMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMemberMgr.MemberLocation;

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
	
	/** The managers owned by this BuildStore */
	private IActionMgr actionMgr;
	private IActionTypeMgr actionTypeMgr;
	private IPackageMemberMgr pkgMemberMgr;

	/*
	 * Various colour constants used in displaying this element.
	 */
	private static final IColorConstant TEXT_FOREGROUND = IColorConstant.BLACK;
	private static final IColorConstant LINE_COLOUR = new ColorConstant(10, 10, 10);
	private static final IColorConstant FILL_COLOUR = new ColorConstant(220, 220, 190);

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
		pkgMemberMgr = buildStore.getPackageMemberMgr();
		
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
			ellipse.setForeground(manageColor(LINE_COLOUR));
			ellipse.setBackground(manageColor(FILL_COLOUR));
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

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.pattern.AbstractPattern#canMoveShape(org.eclipse.graphiti.features.context.IMoveShapeContext)
	 */
	@Override
	public boolean canMoveShape(IMoveShapeContext context) {
		
		/* 
		 * Validate where the UIAction is moving to. We can't move UIActions 
		 * off the left/top of the window.
		 */
		int x = context.getX();
		int y = context.getY();		
		if ((x < 0) || (y < 0)) {
			return false;
		}
		
		/* check that we've moved a single UIAction object */
		PictogramElement pe = context.getPictogramElement();
		PictogramLink pl = pe.getLink();
		EList<EObject> bos = pl.getBusinessObjects();
		if (bos.size() != 1) {
			return false;
		}
		
		/* 
		 * Finally, check that this is a UIAction (although we probably wouldn't have
		 * got here otherwise.
		 */
		Object bo = bos.get(0);
		return (bo instanceof UIAction);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.pattern.AbstractPattern#moveShape(org.eclipse.graphiti.features.context.IMoveShapeContext)
	 */
	@Override
	public void moveShape(IMoveShapeContext context) {
		super.moveShape(context);

		/*
		 * Fetch the x, y and actionId. Note that all error checking was done by canMoveShape().
		 */
		int x = context.getX();
		int y = context.getY();
		PictogramLink pl = context.getPictogramElement().getLink();
		UIAction action = (UIAction)(pl.getBusinessObjects().get(0));
		int actionId = action.getId();
		
		/* determine the UIAction's old location */
		MemberLocation oldXY = pkgMemberMgr.getMemberLocation(IPackageMemberMgr.TYPE_ACTION, actionId);
		if (oldXY == null){
			/* default, in the case of an error */
			oldXY = new MemberLocation();
			oldXY.x = 0;
			oldXY.y = 0;
		}
		
		/* create an undo/redo operation that will invoke the underlying database changes */
		ActionChangeOperation op = new ActionChangeOperation("move action", actionId);
		op.recordLocationChange(oldXY.x, oldXY.y, x, y);
		op.recordAndInvoke();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.pattern.AbstractPattern#canDelete(org.eclipse.graphiti.features.context.IDeleteContext)
	 */
	@Override
	public boolean canDelete(IDeleteContext context) {
		/* we can't delete actions */
		return false;
	}
	
	/*-------------------------------------------------------------------------------------*/

}
