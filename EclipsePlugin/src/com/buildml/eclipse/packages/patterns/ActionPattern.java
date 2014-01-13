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
import org.eclipse.graphiti.mm.pictograms.FixPointAnchor;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.PictogramLink;
import org.eclipse.graphiti.pattern.AbstractPattern;
import org.eclipse.graphiti.pattern.IPattern;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;
import org.eclipse.graphiti.util.ColorConstant;
import org.eclipse.graphiti.util.IColorConstant;

import com.buildml.eclipse.bobj.UIAction;
import com.buildml.eclipse.packages.PackageDiagramEditor;
import com.buildml.eclipse.packages.layout.LayoutAlgorithm;
import com.buildml.eclipse.packages.layout.LeftRightBounds;
import com.buildml.eclipse.packages.layout.PictogramSize;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.eclipse.utils.UndoOpAdapter;
import com.buildml.model.IActionMgr;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.IActionTypeMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.ISlotTypes;
import com.buildml.model.IPackageMemberMgr.MemberLocation;
import com.buildml.model.ISlotTypes.SlotDetails;
import com.buildml.model.undo.ActionUndoOp;
import com.buildml.model.undo.MultiUndoOp;
import com.buildml.utils.errors.ErrorCode;

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
	
	/**
	 * The layout algorithm we use for positioning pictograms.
	 */
	private LayoutAlgorithm layoutAlgorithm = null;

	/*
	 * Various colour constants used in displaying this element.
	 */
	private static final IColorConstant TEXT_FOREGROUND = IColorConstant.BLACK;
	private static final IColorConstant LINE_COLOUR = new ColorConstant(180, 180, 155);
	private static final IColorConstant FILL_COLOUR = new ColorConstant(220, 220, 190);

	/*
	 * Size of this element (in pixels).
	 */
	private static final int ACTION_WIDTH = 150;
	private static final int ACTION_HEIGHT = 50;
	
	/** The (static) maximum size of an action's pictogram, in pixels */
	private static PictogramSize ACTION_MAX_SIZE = 
			new PictogramSize(ACTION_WIDTH, ACTION_HEIGHT);
	
	/*=====================================================================================*
	 * STATIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Return the (width, height) in pixel of the file group pictogram. This is used
	 * for laying-out the package members.
	 * @return The (width, height) in pixels.
	 */
	public static PictogramSize getSize() {
		return ACTION_MAX_SIZE;
	}
	
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
		String actionCommandBase = getShellCommandSummary(actionCommand);
		
		/* draw the action type's name inside the oval(s) */
		Text actionTypeNameText = gaService.createPlainText(invisibleRectangle, 
				"Shell: " + actionCommandBase);
		actionTypeNameText.setFilled(false);
		actionTypeNameText.setForeground(manageColor(TEXT_FOREGROUND));
		actionTypeNameText.setHorizontalAlignment(Orientation.ALIGNMENT_CENTER);
		gaService.setLocationAndSize(actionTypeNameText, xAdjust, yAdjust,
										ACTION_WIDTH - xAdjust, ACTION_HEIGHT - yAdjust);
		
		/*
		 * Add a couple of anchors so we can draw connections to this shape.
		 * 	UIFileActionConnection.INPUT_TO_ACTION (0) - left anchor
		 *  UIFileActionConnection.OUTPUT_FROM_ACTION (1) - right anchor
		 */
		FixPointAnchor anchorLeft = peCreateService.createFixPointAnchor(containerShape);
		anchorLeft.setLocation(gaService.createPoint(-5, ACTION_HEIGHT / 2));
		gaService.createInvisibleRectangle(anchorLeft);
		FixPointAnchor anchorRight = peCreateService.createFixPointAnchor(containerShape);
		anchorRight.setLocation(gaService.createPoint(ACTION_WIDTH + 5, ACTION_HEIGHT / 2));
		gaService.createInvisibleRectangle(anchorRight);
		
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
		editor.getDiagramBehavior().refresh();
		return super.update(context);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.pattern.AbstractPattern#canMoveShape(org.eclipse.graphiti.features.context.IMoveShapeContext)
	 */
	@Override
	public boolean canMoveShape(IMoveShapeContext context) {
		
		/* what object is being moved? It must be a UIAction */
		Object sourceBo = GraphitiUtils.getBusinessObject(context.getShape());
		if (!(sourceBo instanceof UIAction)) {
			return false;
		}
		int actionId = ((UIAction)sourceBo).getId();
		
		/* 
		 * Validate where the UIAction is moving to. We can't move UIActions 
		 * off the left/top of the window, and they must be moved within the
		 * Diagram (not onto other members).
		 */
		Object targetContainer = context.getTargetContainer();
		if (!(targetContainer instanceof Diagram)) {
			return false;
		}
		int x = context.getX();
		int y = context.getY();		

		/* we can never move off the top of the canvas (Y-axis) */
		if (y < 0) {
			return false;
		}
		
		/*
		 * Determine the acceptable X-axis movement bounds for the object we're moving. This involves
		 * a database query, which will happen roughly 10-20 times for an average mouse drag.
		 */
		if (layoutAlgorithm == null) {
			layoutAlgorithm = ((PackageDiagramEditor)getDiagramEditor()).getLayoutAlgorithm();
		}
		LeftRightBounds bounds = layoutAlgorithm.getMemberMovementBounds(IPackageMemberMgr.TYPE_ACTION, actionId);
		if ((x < bounds.leftBound) || (x > bounds.rightBound)) {
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
		ActionUndoOp op = new ActionUndoOp(buildStore, actionId);
		op.recordLocationChange(oldXY.x, oldXY.y, x, y);
		new UndoOpAdapter("Move Action", op).invoke();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Yes, we can delete UIActions.
	 */
	@Override
	public boolean canDelete(IDeleteContext context) {
		return true;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Invoked when the user initiates a "delete" operation on a UIAction.
	 */
	@Override
	public void delete(IDeleteContext context) {
		
		/* determine the business object that related to the pictogram being deleted */
		UIAction action = (UIAction)(GraphitiUtils.getBusinessObject(context.getPictogramElement()));
		int actionId = action.getId();
		
		/*
		 * Sanity checks.
		 */
		Integer[] children = actionMgr.getChildren(actionId);
		if (children.length != 0) {
			AlertDialog.displayErrorDialog("Can't Delete", "This action still has children (see the <import> package)");
			return;
		}
		Integer filesAccessed[] = actionMgr.getFilesAccessed(actionId, OperationType.OP_UNSPECIFIED);
		if (filesAccessed.length != 0) {
			AlertDialog.displayErrorDialog("Can't Delete", 
					"This action can't be deleted, as it still references files in the <import> package");
			return;
		}
		
		/* add the "delete" operation to our redo/undo stack */
		MultiUndoOp multiOp = new MultiUndoOp();
		
		/* 
		 * first, delete all of this action's slots that refer to file groups.
		 */
		int actionTypeId = actionMgr.getActionType(actionId);
		if (actionTypeId == ErrorCode.NOT_FOUND) {
			return; 	/* invalid action type - ignore */
		}
		SlotDetails slots[] = actionTypeMgr.getSlots(actionTypeId, ISlotTypes.SLOT_POS_ANY);
		for (int i = 0; i < slots.length; i++) {
			if (slots[i].slotType == ISlotTypes.SLOT_TYPE_FILEGROUP) {
				int slotId = slots[i].slotId;
				Object slotValue = actionMgr.getSlotValue(actionId, slotId);
				if (slotValue instanceof Integer) {
					ActionUndoOp op = new ActionUndoOp(buildStore, actionId);
					op.recordSlotRemove(slotId, slotValue);
					multiOp.add(op);
				}
			}
		}
		
		/* now delete the action itself */
		ActionUndoOp op = new ActionUndoOp(buildStore, actionId);
		op.recordMoveToTrash();
		multiOp.add(op);
		
		/* invoke all changes in one step... */
		new UndoOpAdapter("Delete Action", multiOp).invoke();
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Given a full shell command string, generate a summary of that command that is suitable
	 * for display with an action's oval. This summary provides the command name (without
	 * arguments or absolute paths), and possibly combines multiple commands into a single
	 * summary. For example, "/usr/bin/gcc -c test.c" will be summarized as "gcc". Also,
	 * "gcc -c test.c\n/usr/bin/ld -o test test.o" is summarized as "gcc,ld".
	 * 
	 * @param actionCommand The original (full) action shell command.
	 * @return The summarized String.
	 */
	private String getShellCommandSummary(String actionCommand) {
	
		/* we'll accumulate the command summary in this StringBuilder */
		StringBuilder sb = new StringBuilder();
		
		/* break the action's full (possibly multi-line) command into multiple lines */
		String lines[] = actionCommand.split("\n", 0);
		
		/* for each command line in the action's shell command... */
		for (int i = 0; i < lines.length; i++) {

			/* extract the first part of the command line, up until the first space */
			String lineSplit[] = lines[i].split(" ", 2);
			if (lineSplit.length > 0) {
				String base = lineSplit[0];
				
				/* Fetch the last part of the command. For example, /usr/bin/gcc -> gcc */
				int slashIndex = base.lastIndexOf('/');
				if (slashIndex != -1){
					base = base.substring(slashIndex + 1);
				}
				
				/* append the base of this command to the output */
				if (i != 0) {
					sb.append(',');
				}
				sb.append(base);
			}
		}	
		return sb.toString();
	}
	
	/*-------------------------------------------------------------------------------------*/
}
