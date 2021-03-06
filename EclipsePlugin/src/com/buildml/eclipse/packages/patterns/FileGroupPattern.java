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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.ICreateContext;
import org.eclipse.graphiti.features.context.IDeleteContext;
import org.eclipse.graphiti.features.context.IMoveShapeContext;
import org.eclipse.graphiti.features.context.IResizeShapeContext;
import org.eclipse.graphiti.mm.algorithms.Polygon;
import org.eclipse.graphiti.mm.algorithms.Polyline;
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
import org.eclipse.swt.widgets.Display;

import com.buildml.eclipse.actions.dialogs.SlotSelectionDialog;
import com.buildml.eclipse.bobj.UIAction;
import com.buildml.eclipse.bobj.UIFileGroup;
import com.buildml.eclipse.packages.PackageDiagramEditor;
import com.buildml.eclipse.packages.layout.LayoutAlgorithm;
import com.buildml.eclipse.packages.layout.LeftRightBounds;
import com.buildml.eclipse.packages.layout.PictogramSize;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.eclipse.utils.UndoOpAdapter;
import com.buildml.eclipse.utils.errors.FatalError;
import com.buildml.model.IActionMgr;
import com.buildml.model.IActionTypeMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileGroupMgr;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.ISlotTypes;
import com.buildml.model.IPackageMemberMgr.MemberDesc;
import com.buildml.model.IPackageMemberMgr.MemberLocation;
import com.buildml.model.ISlotTypes.SlotDetails;
import com.buildml.model.undo.ActionUndoOp;
import com.buildml.model.undo.FileGroupUndoOp;
import com.buildml.model.undo.MultiUndoOp;
import com.buildml.utils.errors.ErrorCode;

/**
 * A Graphiti pattern for managing the "FileGroup" graphical element in a BuildML diagram.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class FileGroupPattern extends AbstractPattern implements IPattern {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The PackageDiagramEditor we're part of */
	private PackageDiagramEditor editor;
	
	/** The IBuildStore that this diagram represents */
	private IBuildStore buildStore;
	
	/** The managers owned by this BuildStore */
	private IPackageMemberMgr pkgMemberMgr;
	private IFileMgr fileMgr;
	private IFileGroupMgr fileGroupMgr;
	private IActionMgr actionMgr;
	private IActionTypeMgr actionTypeMgr;
	
	/** 
	 * If we're adding multiple files in one drag operation, which file group
	 * are we adding to?
	 */
	private UIFileGroup multiAddFileGroup = null;
	
	/**
	 * The initial set of members of this file group (possibly empty).
	 */
	private ArrayList<Integer> initialMembers = null;
	
	/**
	 * The final set of members of this file group (after a drag
	 * operation has completed).
	 */
	private ArrayList<Integer> currentMembers = null;
	
	/**
	 * The layout algorithm we use for positioning pictograms.
	 */
	private LayoutAlgorithm layoutAlgorithm = null;
	
	/*
	 * Various colour constants used in displaying this element.
	 */
	private static final IColorConstant TEXT_FOREGROUND = IColorConstant.BLACK;
	private static final IColorConstant LINE_COLOUR = new ColorConstant(141, 166, 190);
	private static final IColorConstant FILL_COLOUR = new ColorConstant(187, 218, 247);
	private static final IColorConstant FILL_CORNER_COLOUR = new ColorConstant(160, 190, 220);

	/*
	 * Size of this element (in pixels).
	 */
	private static final int FILE_GROUP_WIDTH = 50;
	private static final int FILE_GROUP_HEIGHT = 50;
	private static final int FILE_GROUP_CORNER_SIZE = 20;
	private static final int FILE_GROUP_OVERLAP = 3;
	
	/** Font type for labels */
	private static final String LABEL_FONT = "courier";
	
	/** Font size */
	private static final int LABEL_FONT_SIZE = 9;
	
	/** Gap between consecutive label lines */
	private static final int LABEL_FONT_GAP = 2;
	
	/** The maximum number of file names to show under an icon */
	private static final int MAX_LABELS_TO_SHOW = 4;
	
	/** Polygon coordinates for drawing the file's front page (the box with the bent corner) */
	private static final int OFF_X = FILE_GROUP_WIDTH / 2;
	
	int coordsPage1[] = new int[] { 
			OFF_X, 0, 
			OFF_X + FILE_GROUP_WIDTH - FILE_GROUP_CORNER_SIZE, 0,
			OFF_X + FILE_GROUP_WIDTH, FILE_GROUP_CORNER_SIZE,
			OFF_X + FILE_GROUP_WIDTH, FILE_GROUP_HEIGHT,
			OFF_X, FILE_GROUP_HEIGHT 
	};
	
	/** Polygon coordinates for drawing the bent corner */
	int coordsCorner[] = new int[] {
			OFF_X + FILE_GROUP_WIDTH - FILE_GROUP_CORNER_SIZE, 0,
			OFF_X + FILE_GROUP_WIDTH, FILE_GROUP_CORNER_SIZE,
			OFF_X + FILE_GROUP_WIDTH - FILE_GROUP_CORNER_SIZE, FILE_GROUP_CORNER_SIZE
	};
		
	/** Polygon coordinates for drawing the file group's second page (if one is drawn) */
	int coordsPage2[] = new int[] { 
			OFF_X + FILE_GROUP_OVERLAP, FILE_GROUP_OVERLAP, 
			OFF_X + FILE_GROUP_OVERLAP + FILE_GROUP_WIDTH, FILE_GROUP_OVERLAP,
			OFF_X + FILE_GROUP_OVERLAP + FILE_GROUP_WIDTH, FILE_GROUP_OVERLAP + FILE_GROUP_HEIGHT,
			OFF_X + FILE_GROUP_OVERLAP, FILE_GROUP_OVERLAP + FILE_GROUP_HEIGHT 
	};
	
	/**
	 * Coordinates for the three additional lines that are shown inside a merge group box.
	 */
	int coordsMergeLines[][] = new int[][] {
			{
				OFF_X + 5, FILE_GROUP_CORNER_SIZE,
				OFF_X + FILE_GROUP_WIDTH - 5, FILE_GROUP_CORNER_SIZE + 10,
			},
			{
				OFF_X + 5, FILE_GROUP_CORNER_SIZE + 13,
				OFF_X + FILE_GROUP_WIDTH - 5, FILE_GROUP_CORNER_SIZE + 13,
			},
			{
				OFF_X + 5, FILE_GROUP_CORNER_SIZE + 26,
				OFF_X + FILE_GROUP_WIDTH - 5, FILE_GROUP_CORNER_SIZE + 16,
			},
	};
	
	/** The (static) maximum size of a file group pictogram, in pixels */
	private static PictogramSize FILE_GROUP_MAX_SIZE = 
			new PictogramSize(2 * FILE_GROUP_WIDTH, 
							  FILE_GROUP_HEIGHT + FILE_GROUP_OVERLAP +
							  ((MAX_LABELS_TO_SHOW + 1) * (LABEL_FONT_SIZE + LABEL_FONT_GAP)));
	
	/*=====================================================================================*
	 * STATIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Return the (width, height) in pixel of the file group pictogram. This is used
	 * for laying-out the package members.
	 * @return The (width, height) in pixels.
	 */
	public static PictogramSize getSize() {
		return FILE_GROUP_MAX_SIZE;
	}
		
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
		return (mainBusinessObject instanceof UIFileGroup) || isFileClass(mainBusinessObject);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether a specific business object can be added to the diagram.
	 */
	@Override
	public boolean canAdd(IAddContext context) {
		
		Object newObject = context.getNewObject();
		
		/* 
		 * A UIFileGroup or an IFile can both be added to a Diagram (creating
		 * a new UIFileGroup, or can be added to an existing UIFileGroup.
		 */
		if ((newObject instanceof UIFileGroup) || isFileClass(newObject)) {
			
			/* what are we adding it to? */
			Object target = context.getTargetContainer();
			if (target instanceof Diagram) {
				return true;
			}
			Object bo = GraphitiUtils.getBusinessObject(target);
			if (bo != null) {
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

		editor = (PackageDiagramEditor)getDiagramEditor();
		buildStore = editor.getBuildStore();
		pkgMemberMgr = buildStore.getPackageMemberMgr();
		fileMgr = buildStore.getFileMgr();
		fileGroupMgr = buildStore.getFileGroupMgr();
		actionMgr = buildStore.getActionMgr();
		actionTypeMgr = buildStore.getActionTypeMgr();

		/*
		 * Case handled:
		 *   1) UIFileGroup added (programatically) or dragged onto Diagram - make it appear on Diagram.
		 *   2) IFile dragged onto Diagram - create a new UIFileGroup, make it appear.
		 *   3) IFile dragged onto UIFileGroup - add to existing UIFileGroup.
		 *   
		 * Possible future cases:
		 *   - UIFileGroup dragged onto UIFileGroup - merge the two.
		 */
		Object addedObject = context.getNewObject();
		Object targetObject = context.getTargetContainer();
		int x = context.getX();
		int y = context.getY();
		
		/* Case #1 - add/drag an existing UIFileGroup onto the Diagram */ 
		if ((addedObject instanceof UIFileGroup) && (targetObject instanceof Diagram)) {
			return renderPictogram((Diagram)targetObject, (UIFileGroup)addedObject, x, y);			
		}
		
		/* Case #2 - drag IFile onto Diagram */
		if ((targetObject instanceof Diagram) && isFileClass(addedObject)) {
			
			String pathName = getPathOf(addedObject);
			
			/* 
			 * If this is the first (of possibly many) files being added to this file
			 * group, we start by recording the "initial" content. Given that we're
			 * dragging directly onto the Diagram, there initial file group is empty.
			 * The "current" set of members is also empty, but we'll shortly be
			 * adding members to it.
			 */
			if (multiAddFileGroup == null) {
				initialMembers = new ArrayList<Integer>();
				currentMembers = new ArrayList<Integer>();
				
				/* 
				 * Create a brand new UIFileGroup in the database - initially empty,
				 * and won't be displayed.
				 */
				int pkgId = editor.getPackageId();
				final int fileGroupId = fileGroupMgr.newSourceGroup(pkgId);
				if (fileGroupId == ErrorCode.NOT_FOUND) {
					return null; /* invalid pkgId */
				}
				multiAddFileGroup = new UIFileGroup(fileGroupId);
				getDiagram().eResource().getContents().add(multiAddFileGroup);
				
				/*
				 * If the user is dragging multiple files onto the Diagram, then we need
				 * to do some clever work to make sure they all end up in the same file group.
				 * We end up seeing multiple add() calls from the graphiti framework, so record
				 * the UIFileGroup that we added the first file to, and reuse it for each
				 * successive call to add(). However, once the Eclipse UI is "idle" again, make
				 * sure we stop using that file group.
				 */
				Display.getCurrent().asyncExec(new Runnable() {
					@Override
					public void run() {
						multiAddFileGroup = null;
						FileGroupUndoOp op = new FileGroupUndoOp(buildStore, fileGroupId);
						op.recordMembershipChange(initialMembers, currentMembers);
						new UndoOpAdapter("Create New File Group", op).invoke();
					}
				});
					
				/* set the x and y coordinates correctly */
				pkgMemberMgr.setMemberLocation(IPackageMemberMgr.TYPE_FILE_GROUP, fileGroupId, x, y);		
			}
			
			/* 
			 * For all dragged files (not just the first), add the dragged file into our "currentMembers"
			 * array. Once our drag operation is complete, we'll add them all to the file group itself.
			 * Note: this might fail, but we'll silently ignore the error.
			 */
			addToFileGroup(pathName);
			
			/* 
			 * Nothing to display as a result of this call - it'll be displayed when we refresh
			 * the whole diagram.
			 */
			return null;
		}
		
		/* Case #3 - drag IFile (or many) onto existing UIFileGroup */
		if (isFileClass(addedObject)) {
			Object bo = GraphitiUtils.getBusinessObject(targetObject);
			if (bo instanceof UIFileGroup) {
				UIFileGroup fileGroup = (UIFileGroup)bo;
				final int fileGroupId = fileGroup.getId();
				
				/*
				 * We may have multiple files being added to an existing file group. If this is
				 * the first file, then we need to record the "initialMembers" of the group
				 * so that we can roll back after an "undo".
				 */
				if (multiAddFileGroup == null) {
					multiAddFileGroup = fileGroup;
					initialMembers = getFileGroupAsArrayList(fileGroupId);
					currentMembers = (ArrayList<Integer>) initialMembers.clone();
					
					/* once the add operation is complete... */
					Display.getCurrent().asyncExec(new Runnable() {
						@Override
						public void run() {
							multiAddFileGroup = null;
							FileGroupUndoOp op = new FileGroupUndoOp(buildStore, fileGroupId);
							op.recordMembershipChange(initialMembers, currentMembers);
							new UndoOpAdapter("Modify File Group", op).invoke();
						}
					});
				}
				
				/* for all invocations, not just the first, add the new path into currentMembers */
				addToFileGroup(getPathOf(addedObject));
				
				/* 
				 * Nothing to display as a result of this call - it'll be displayed when we refresh
				 * the whole diagram.
				 */
				return null;
			}
		}

		/* other cases are not handled */
		return null;
	}


	/*-------------------------------------------------------------------------------------*/

	/**
	 * We can't add FileGroups from the palette.
	 */
	@Override
	public boolean canCreate(ICreateContext context) {
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * The user isn't allowed to resize the object.
	 */
	@Override
	public boolean canResizeShape(IResizeShapeContext context) {
		return false;
	}

	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/

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

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.pattern.AbstractPattern#canMoveShape(org.eclipse.graphiti.features.context.IMoveShapeContext)
	 */
	@Override
	public boolean canMoveShape(IMoveShapeContext context) {

		/* what object is being moved? It must be a UIFileGroup */
		Object sourceBo = GraphitiUtils.getBusinessObject(context.getShape());
		if (!(sourceBo instanceof UIFileGroup)) {
			return false;
		}
		int fileGroupId = ((UIFileGroup)sourceBo).getId();
		
		/* 
		 * Validate where the UIFileGroup is moving to. We can't move UIFileGroups 
		 * off the left/top of the window, and they can't be moved left of their
		 * left neighbours, or right of their right neighbours.
		 */
		Object targetContainer = context.getTargetContainer();
		int x = context.getX();
		int y = context.getY();
		if (targetContainer instanceof Diagram) {
			
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
			LeftRightBounds bounds = layoutAlgorithm.getMemberMovementBounds(IPackageMemberMgr.TYPE_FILE_GROUP, fileGroupId);
			if ((x < bounds.leftBound) || (x > bounds.rightBound)) {
				return false;
			}
		}
		
		/* check that we've moved a single UIFileGroup object */
		PictogramElement pe = context.getPictogramElement();
		PictogramLink pl = pe.getLink();
		EList<EObject> bos = pl.getBusinessObjects();
		if (bos.size() != 1) {
			return false;
		}

		/* 
		 * Finally, what are we moving onto? Moving within the Diagram is allowed,
		 * and moving onto a UIAction is allowed.
		 */
		if (targetContainer instanceof Diagram) {
			return true;
		}
		Object targetBo = GraphitiUtils.getBusinessObject(targetContainer);
		if ((targetBo instanceof UIAction) || (targetBo instanceof UIFileGroup)) {
			return true;
		}
		
		/* all other moves are illegal */
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.pattern.AbstractPattern#moveShape(org.eclipse.graphiti.features.context.IMoveShapeContext)
	 */
	@Override
	public void moveShape(IMoveShapeContext context) {
		super.moveShape(context);

		/*
		 * Fetch the x, y and fileGroupId. Note that all error checking was done by canMoveShape().
		 */
		int x = context.getX();
		int y = context.getY();
		UIFileGroup fileGroup = 
				(UIFileGroup)GraphitiUtils.getBusinessObject(context.getPictogramElement());
		int fileGroupId = fileGroup.getId();
		
		/*
		 * Are we moving a UIFileGroup around the Diagram?
		 */
		Object targetObject = context.getTargetContainer();
		if (targetObject instanceof Diagram) {
		
			/* determine the UIFileGroups's old location */
			MemberLocation oldXY = pkgMemberMgr.getMemberLocation(IPackageMemberMgr.TYPE_FILE_GROUP,
																	fileGroupId);
			if (oldXY == null){
				/* default, in the case of an error */
				oldXY = new MemberLocation();
				oldXY.x = 0;
				oldXY.y = 0;
			}
		
			/* create an undo/redo operation that will invoke the underlying database changes */
			FileGroupUndoOp op = new FileGroupUndoOp(buildStore, fileGroupId);
			op.recordLocationChange(oldXY.x, oldXY.y, x, y);
			new UndoOpAdapter("Move File Group", op).invoke();
		}
		
		else {
			Object targetBo = GraphitiUtils.getBusinessObject(context.getTargetContainer());
			if (targetBo instanceof UIAction) {
				/* create a connection arrow between the file group and the action */
				int actionId = ((UIAction)targetBo).getId();
				moveOntoAction(fileGroupId, actionId);
			}
			
			else if (targetBo instanceof UIFileGroup) {
				
				/* create merge file groups */
				int targetFileGroupId = ((UIFileGroup)targetBo).getId();
				moveOntoFileGroup(fileGroupId, targetFileGroupId);
			}
		}
		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Yes, we can delete UIFileGroups.
	 */
	@Override
	public boolean canDelete(IDeleteContext context) {
		return true;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Invoked when the user initiates a "delete" operation on a UIFileGroup.
	 */
	@Override
	public void delete(IDeleteContext context) {
		
		/* determine the business object that related to the pictogram being deleted */
		PictogramLink pl = context.getPictogramElement().getLink();
		UIFileGroup fileGroup = (UIFileGroup)(pl.getBusinessObjects().get(0));
		int fileGroupId = fileGroup.getId();
		
		/* add the "delete" operation to our redo/undo stack */
		MultiUndoOp multiOp = new MultiUndoOp();
		
		/* first, delete all slots that refer to this file group */
		MemberDesc neighbours[] = pkgMemberMgr.getNeighboursOf(
				IPackageMemberMgr.TYPE_FILE_GROUP, fileGroupId, IPackageMemberMgr.NEIGHBOUR_ANY, false);
		for (int i = 0; i < neighbours.length; i++) {
			removeReferenceFromNeighbour(multiOp, fileGroupId, neighbours[i].memberType, neighbours[i].memberId);
		}
		
		/* now delete the file group itself */
		FileGroupUndoOp op = new FileGroupUndoOp(buildStore, fileGroupId);
		op.recordMembershipChange(getFileGroupAsArrayList(fileGroupId), new ArrayList<Integer>());
		multiOp.add(op);
		
		/* invoke all changes in one step... */
		new UndoOpAdapter("Delete File Group", multiOp).invoke();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create a new merge file group, based off one or more existing file groups. This method
	 * assumes that creation operation was invoked via a GUI operation, and therefore the
	 * change is added to the undo/redo stack.
	 * 
	 * @param pkgId 		The package that the file group will be placed in.
	 * @param subGroupIds	The list of sub file group IDs to add into the merge list.
	 */
	public void createMergeFileGroup(int pkgId, List<Integer> subGroupIds) {
		
		/* must have at least one member */
		if (subGroupIds.size() < 1) {
			return;
		}
		
		/*
		 * Create a new merge group. This will allocate an empty group, providing
		 * us with the group ID.
		 */
		int mergeFileGroupId = fileGroupMgr.newMergeGroup(pkgId);
		if (mergeFileGroupId == ErrorCode.NOT_FOUND) {
			AlertDialog.displayErrorDialog("Error Creating Merge Group",
					"Unable to create a new merge group in the database.");
			return;
		}
					
		/* 
		 * Position the new merge group at the y-axis mid point of all the
		 * existing sub groups, but to the right of the right-most sub group.
		 * First, find the "average" (y-axis) and "maximum" (x-axis) of all sub
		 * file groups.
		 */
		int totalY = 0, maxX = 0;
		for (Iterator<Integer> iterator = subGroupIds.iterator(); iterator.hasNext();) {
			int subGroupId = (Integer) iterator.next();
			
			MemberLocation subGroupLocation = pkgMemberMgr.getMemberLocation(
				IPackageMemberMgr.TYPE_FILE_GROUP, subGroupId);
			if (subGroupLocation == null) {
				AlertDialog.displayErrorDialog("Error Positioning Merge Group",
					"Unable to determine the current position of a source file group");
				return;
			}
			totalY += subGroupLocation.y;
			if (subGroupLocation.x > maxX) {
				maxX = subGroupLocation.x;
			}
		}
		int averageY = totalY / subGroupIds.size();
		
		/*
		 * Now position the new merge group to the right of the right-most sub group.
		 */
		maxX += layoutAlgorithm.getSizeOfPictogram(IPackageMemberMgr.TYPE_FILE_GROUP).getWidth();
		maxX += layoutAlgorithm.getXPadding();
		
		if (pkgMemberMgr.setMemberLocation(
				IPackageMemberMgr.TYPE_FILE_GROUP, mergeFileGroupId,
				maxX, averageY) != ErrorCode.OK) {
			AlertDialog.displayErrorDialog("Error Positioning Merge Group",
					"Unable to set position for new merge group");
			return;				
		}
		
		/*
		 * Finally, generate an undo/redo operation to add the members to the group.
		 * If this operation is "undoned" then the group will still exist in the
		 * database, but will have no members and therefore won't be shown.
		 */
		FileGroupUndoOp op = new FileGroupUndoOp(buildStore, mergeFileGroupId);		
		op.recordMembershipChange(new ArrayList<Integer>(), subGroupIds);
		new UndoOpAdapter("Create Merge Group", op).invoke();
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Given that a UIFileGroup is being deleted, we need to go through and first remove the
	 * connection to any neighbours who reference this file group.
	 * 
	 * @param multiOp		The undo/redo multi-operation to add our "delete connections" to.
	 * @param fileGroupId	The ID of the file group that has been removed.
	 * @param memberType	The type of neighbour (IPackageMemberMgr.TYPE_ACTION, etc).
	 * @param memberId		The ID of the neighbour (e.g. actionId).
	 */
	private void removeReferenceFromNeighbour(MultiUndoOp multiOp,
			int fileGroupId, int memberType, int memberId) {

		/*
		 * For actions...
		 */
		if (memberType == IPackageMemberMgr.TYPE_ACTION) {
			
			/* get the list of slots associated with this action */
			int actionTypeId = actionMgr.getActionType(memberId);
			if (actionTypeId == ErrorCode.NOT_FOUND) {
				return; 	/* invalid action type - ignore */
			}
			SlotDetails slots[] = actionTypeMgr.getSlots(actionTypeId, ISlotTypes.SLOT_POS_ANY);
			
			/* for each slot that's a file group, see if it references our file group */
			for (int i = 0; i < slots.length; i++) {
				if (slots[i].slotType == ISlotTypes.SLOT_TYPE_FILEGROUP) {
					int slotId = slots[i].slotId;
					Object slotValue = actionMgr.getSlotValue(memberId, slotId);
					if (slotValue instanceof Integer) {
						if (slotValue.equals(Integer.valueOf(fileGroupId))) {
							ActionUndoOp op = new ActionUndoOp(buildStore, memberId);
							op.recordSlotRemove(slotId, fileGroupId);
							multiOp.add(op);
						}
					}
				}
			}
		}
		
		/*
		 * Else if this UIFileGroup is somehow embedded in a neighbouring file group.
		 */
		else if (memberType == IPackageMemberMgr.TYPE_FILE_GROUP) {
			int fgType = fileGroupMgr.getGroupType(memberId);
			
			/* for merge groups, remove any/all references we have to the deleted group */
			if (fgType == IFileGroupMgr.MERGE_GROUP) {
				Integer members[] = fileGroupMgr.getSubGroups(memberId);
				for (int i = 0; i < members.length; i++) {
					if (members[i] == fileGroupId) {
						List<Integer> oldMembers = Arrays.asList(members);
						List<Integer> newMembers = new ArrayList<Integer>(Arrays.asList(members));
						while (newMembers.remove(Integer.valueOf(fileGroupId))) { /* remove delete group multiple times? */};
						FileGroupUndoOp op = new FileGroupUndoOp(buildStore, memberId);
						op.recordMembershipChange(oldMembers, newMembers);
						multiOp.add(op);
						break;
					}
				}
			}
			
			else if (fgType == IFileGroupMgr.FILTER_GROUP) {
				// TODO: implement this.
			}
		}
		
		/*
		 * For everything else...
		 */
		else {
			throw new FatalError("Unhandled memberType");
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Fetch the members of a file group (path IDs, or sub-group IDs) and return them as
	 * an ArrayList. This is used for recording history changes.
	 * 
	 * @param fileGroupId The ID of the file group to get the content of.
	 * @return An ArrayList<Integer> containing the members.
	 */
	private ArrayList<Integer> getFileGroupAsArrayList(int fileGroupId) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		int groupSize = fileGroupMgr.getGroupSize(fileGroupId);
		if (groupSize < 0) {
			return result;
		}
		int groupType = fileGroupMgr.getGroupType(fileGroupId);
		
		for (int i = 0; i != groupSize; i++) {
			int pathId;
			
			if (groupType == IFileGroupMgr.MERGE_GROUP) {
				pathId = fileGroupMgr.getSubGroup(fileGroupId, i);
			} else {
				pathId = fileGroupMgr.getPathId(fileGroupId, i);
			}
			if (pathId < 0) {
				return result;
			}
			result.add(pathId);
		}
		return result;
	}
	
	/*-------------------------------------------------------------------------------------*/


	/**
	 * Append a file path onto the end of the "currentMembers" array. After the "drag"
	 * operation is complete, we'll add all of these files into the file group.
	 * 
	 * @param fullPath The absolute path to add to the file group.
	 * @return ErrorCode.OK on success, or a relevant error code.
	 */
	private int addToFileGroup(String fullPath) {
		
		/* convert the path into an absolute path (not workspace-relative) */
		fullPath = EclipsePartUtils.workspaceRelativeToAbsolutePath(fullPath);

		/* now append the path to the array of members for this drag operation */
		int newPathId = fileMgr.addFile(fullPath);
		if (newPathId == ErrorCode.BAD_PATH) {
			return newPathId; /* couldn't add the path - return an error */
		}
		currentMembers.add(newPathId);

		/* all is good */
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add a new pictogram, representing a UIFileGroup, onto a Graphiti Diagram.
	 * 
	 * @param targetDiagram The Diagram to add the pictogram to.
	 * @param addedFileGroup The UIFileGroup to add a pictogram for.
	 * @param x The X location within the Diagram.
	 * @param y The Y location within the Diagram.
	 * @return The ContainerShape representing the FileGroup pictogram.
	 */
	private PictogramElement renderPictogram(Diagram targetDiagram, UIFileGroup addedFileGroup,
												int x, int y) {
		
		IFileGroupMgr fileGroupMgr = buildStore.getFileGroupMgr();
		int fileGroupId = addedFileGroup.getId();
		int fileGroupType = fileGroupMgr.getGroupType(fileGroupId);
		
		/*
		 * How many boxes will be shown? This helps us distinguish between file groups
		 * containing a single file, versus multiple files. With multiple files, we
		 * draw a second page that appears behind the front page.
		 */
		int groupSize = fileGroupMgr.getGroupSize(addedFileGroup.getId());
		if (groupSize <= 0) {
			return null; /* an empty file group (or an errored file group) shouldn't be shown */
		}
		
		/* 
		 * We can show a limited number of file names in the pictogram. If more than our
		 * maximum, display the last name as "...". For merge file groups, we don't show file
		 * names underneath the pictogram.
		 */
		int fileNamesToShow;
		if (fileGroupType == IFileGroupMgr.MERGE_GROUP) {
			fileNamesToShow = 0;
		} else {
			fileNamesToShow = (groupSize <= MAX_LABELS_TO_SHOW) ? groupSize : MAX_LABELS_TO_SHOW;
		}
		
		/* create a container that holds the pictogram */
		IPeCreateService peCreateService = Graphiti.getPeCreateService();
		IGaService gaService = Graphiti.getGaService();
		ContainerShape containerShape =
				peCreateService.createContainerShape(targetDiagram, true);
		
		/*
		 * Create an invisible outer rectangle. The smaller polygons and labels will be placed
		 * inside this. The width is always twice that of the polygon, to allow for long file
		 * names to be shown underneath the polygons. The height is carefully selected to allow
		 * for enough labels, as well as for the possible "second page" polygon.
		 */
		Rectangle invisibleRectangle =
				gaService.createInvisibleRectangle(containerShape);
		gaService.setLocationAndSize(invisibleRectangle, x, y, 
								2 * FILE_GROUP_WIDTH, 
								FILE_GROUP_HEIGHT + ((groupSize < 2) ? 0 : FILE_GROUP_OVERLAP) +
									((fileNamesToShow + 1) * (LABEL_FONT_SIZE + LABEL_FONT_GAP)));

		/*
		 * Create the visible file icon within the outer shape. First, consider whether
		 * a second page (with no corner bend) should be shown in the background.
		 */
		if (groupSize > 1) {
			Polygon box = gaService.createPolygon(invisibleRectangle, coordsPage2);
			box.setForeground(manageColor(LINE_COLOUR));
			box.setBackground(manageColor(FILL_COLOUR));
			box.setLineWidth(2);			
		}
		
		/* now the front page, with the corner bent */
		Polygon box = gaService.createPolygon(invisibleRectangle, coordsPage1);
		box.setForeground(manageColor(LINE_COLOUR));
		box.setBackground(manageColor(FILL_COLOUR));
		box.setLineWidth(2);
		Polygon boxCorner = gaService.createPolygon(invisibleRectangle, coordsCorner);
		boxCorner.setForeground(manageColor(LINE_COLOUR));
		boxCorner.setBackground(manageColor(FILL_CORNER_COLOUR));
		boxCorner.setLineWidth(2);
		
		/*
		 * For source/generated groups, display the file group's content (or at least part
		 * of it) under the file box. We can only show a limited number of file names. If
		 * there are too many to show, the last label must be "...".
		 */
		if (fileGroupType != IFileGroupMgr.MERGE_GROUP) {
			for (int i = 0; i != fileNamesToShow; i++) {

				String value;
				if ((i == (MAX_LABELS_TO_SHOW - 1)) && (groupSize > MAX_LABELS_TO_SHOW)) {
					value = "...";
				} else {
					/* fetch this particular file's base name */
					int pathId = fileGroupMgr.getPathId(fileGroupId, i);
					if (pathId < 0) {
						value = "";
					} else {
						value = fileMgr.getBaseName(pathId);
					}
				}

				/* draw the label underneath the main "page" polygon */
				Text fileNames = gaService.createText(getDiagram(), invisibleRectangle, 
						value, LABEL_FONT, LABEL_FONT_SIZE);
				fileNames.setFilled(false);
				fileNames.setForeground(manageColor(TEXT_FOREGROUND));
				fileNames.setHorizontalAlignment(Orientation.ALIGNMENT_CENTER);
				gaService.setLocationAndSize(fileNames, 
						0, FILE_GROUP_HEIGHT + ((1 + i) * (LABEL_FONT_SIZE + LABEL_FONT_GAP)), 
						FILE_GROUP_WIDTH * 2, (LABEL_FONT_SIZE + LABEL_FONT_GAP));
			}
		}

		/*
		 * For merge groups, we draw three extra lines inside the file group box as
		 * an indication to the user that it's a merge group.
		 */
		else {
			for (int i = 0; i < coordsMergeLines.length; i++) {				
				Polyline mergeLine = gaService.createPolyline(invisibleRectangle, coordsMergeLines[i]);
				mergeLine.setForeground(manageColor(LINE_COLOUR));
				mergeLine.setBackground(manageColor(FILL_COLOUR));
				mergeLine.setLineWidth(1);
			}
		}
		
		/* 
		 * Add anchors that reside to the immediate left/right of the file group box.
		 * There are for drawing connection arrows.
		 * 	UIFileActionConnection.INPUT_TO_ACTION (0) - right anchor
		 *  UIFileActionConnection.OUTPUT_FROM_ACTION (1) - left anchor
		 */
		FixPointAnchor rightAnchor = peCreateService.createFixPointAnchor(containerShape);
		rightAnchor.setLocation(
				gaService.createPoint(OFF_X + FILE_GROUP_WIDTH + (FILE_GROUP_OVERLAP * 2), FILE_GROUP_HEIGHT / 2));
		gaService.createInvisibleRectangle(rightAnchor);
		FixPointAnchor leftAnchor = peCreateService.createFixPointAnchor(containerShape);
		leftAnchor.setLocation(
				gaService.createPoint(OFF_X, FILE_GROUP_HEIGHT / 2));
		gaService.createInvisibleRectangle(leftAnchor);
	   
		/* create a link between the shape and the business object, and display it. */
		link(containerShape, addedFileGroup);
		layoutPictogramElement(containerShape);
		return containerShape;
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Determine whether the specified object is something in the Eclipse IDE that represents
	 * a "disk file" (but not a directory).
	 * 
	 * @param fileObject An object to be tested.
	 * @return true if this object represents some type of disk file in the Eclipse IDE.
	 */
	private boolean isFileClass(Object fileObject) {
		return getPathOf(fileObject) != null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given something that represents a file within the Eclipse IDE, return the full OS-specific
	 * path to that file.
	 * 
	 * @param fileObject The object that represents a file.
	 * @return The full OS-specific path to the file, or null if this is not a file.
	 */
	private String getPathOf(Object fileObject) {
		
		/* all IFile objects are files (not directories) */
		if (fileObject instanceof IFile) {
			IFile file = (IFile)fileObject;
			return file.getFullPath().toOSString();
		}
		
		/* 
		 * Anything that adapts to IResource might be a file, but we need to double check
		 * that it's not a directory.
		 */
		else if (fileObject instanceof IAdaptable) {
			Object iResource = ((IAdaptable)fileObject).getAdapter(IResource.class);
			if (iResource != null) {
				IPath location = ((IResource)iResource).getLocation();
				if ((location != null) && (location.toFile().isFile())) {
					return location.toOSString();
				}
			}
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * If necessary, bump the pictograms to the right. That is, if the fileGroup was just
	 * connected to an action's input, and that fileGroup is to the right (higher x-coord) 
	 * of the action, then bump the action (and all other downstream members) to the right, 
	 * so their x-coordinates will be higher than the fileGroup. If connected to the 
	 * action's output, but it's left of the action, bump the file group (and downstream
	 * members) to the right.
	 * 
	 * @param multiOp		The multi-undo/redo operation to append "move" actions to. 
	 * @param fileGroupId	The ID of the file group that just got connected.
	 * @param actionId		The ID of the action it was connected to.
	 * @param slotId		The slot (within the action) it was connected to.
	 * @param moveNow		True if this method should invoke the moves (false simply
	 *                      adds the details to operation).
	 */
	private void bumpPictogramsRight(MultiUndoOp multiOp, int fileGroupId,
			int actionId, int slotId, boolean moveNow) {

		/* determine whether the slot was INPUT or OUTPUT */
		SlotDetails details = actionTypeMgr.getSlotByID(slotId);
		if (details == null) {
			return;	/* shouldn't happen */
		}
		
		/* 
		 * Where do we start bumping from? The file group, or the action? It really
		 * depends on what type of slot it is.
		 */
		int memberType, memberId;
		if (details.slotPos == ISlotTypes.SLOT_POS_INPUT) {
			memberType = IPackageMemberMgr.TYPE_FILE_GROUP;
			memberId = fileGroupId;
		} else {
			memberType = IPackageMemberMgr.TYPE_ACTION;
			memberId = actionId;
		}
		
		/* start the bumping (recursively) */
		layoutAlgorithm.bumpPictogramsRight(multiOp, memberType, memberId, moveNow);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Helper method for moveShape() that handles the case where a file group is moved on
	 * top of an action, in which case a connection arrow must be added.
	 * 
	 * @param fileGroupId ID of the file group that was moved.
	 * @param actionId    ID of the action that the file group was moved onto.
	 */
	private void moveOntoAction(int fileGroupId, int actionId) {
		/*
		 * We're connecting a file group to an action's slot. First, pop up a dialog
		 * to ask the user which slot, then proceed to set it.
		 */
		SlotSelectionDialog dialog = new SlotSelectionDialog(buildStore, actionId, true, true);
		int status = dialog.open();
		if (status == SlotSelectionDialog.OK) {
			int slotId = dialog.getSlotId();
			
			Object currentValue = actionMgr.getSlotValue(actionId, slotId);
			
			/* make the change - checking for cycles in the diagram */
			int error = actionMgr.setSlotValue(actionId, slotId, fileGroupId);
			if (error == ErrorCode.LOOP_DETECTED) {
				AlertDialog.displayErrorDialog("Can't Connect File Group",
						"Adding this connection would cause a cycle in the package diagram.");
				return;
			}
			
			/* record an undo/redo operation that will actually change the slot value */
			MultiUndoOp multiOp = new MultiUndoOp();
			ActionUndoOp op = new ActionUndoOp(buildStore, actionId);
			op.recordSlotChange(slotId, currentValue, Integer.valueOf(fileGroupId));
			multiOp.add(op);
			
			/* if necessary, bump pictograms to the right */
			bumpPictogramsRight(multiOp, fileGroupId, actionId, slotId, true);
			
			/* record these operations for future undo/redo operations */
			new UndoOpAdapter("Connect File Group", multiOp).record();
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Helper method for moveShape() that handles the case where a file group is dragged
	 * on top of another file group. Either of the file groups can be SOURCE, GENERATED
	 * or MERGE groups. The following cases are handled:
	 *   1) SOURCE/GENERATED/MERGE dropped on SOURCE/GENERATED -> creates a new MERGE group that
	 *      they both feed into.
	 *   2) SOURCE/GENERATED/MERGE dropped on MERGE GROUP -> adds source group into MERGE GROUP.
	 * 
	 * @param sourceFileGroupId		ID of the file group that was dragged.
	 * @param targetFileGroupId ID of the file group it was dragged onto.
	 */
	private void moveOntoFileGroup(int sourceFileGroupId, int targetFileGroupId) {
		
		/* determine type of each file group (SOURCE, GENERATED, MERGE) */
		int sourceGroupType = fileGroupMgr.getGroupType(sourceFileGroupId);
		int targetGroupType = fileGroupMgr.getGroupType(targetFileGroupId);
		if ((sourceGroupType == ErrorCode.NOT_FOUND) || (targetGroupType == ErrorCode.NOT_FOUND)) {
			return; /* error - just ignore the move */
		}
		
		/*
		 * Handle case where we drag onto an existing MERGE file group. The
		 * source is simply added into the destination.
		 */
		if (targetGroupType == IFileGroupMgr.MERGE_GROUP) {
			
			ArrayList<Integer> currentMembers = getFileGroupAsArrayList(targetFileGroupId);
			int error = fileGroupMgr.addSubGroup(targetFileGroupId, sourceFileGroupId);
			if (error == ErrorCode.LOOP_DETECTED) {
				AlertDialog.displayErrorDialog("Can't Connect File Group",
						"Adding this connection would cause a cycle in the package diagram.");
			}
			else if (error < 0) {
				AlertDialog.displayErrorDialog("Error Adding to Merge Group",
						"For some reason, the file group could not be added to the merge group");
			}

			/*
			 * Now record the membership change in an operation, so we can undo/redo it later. This
			 * may also involve bumping pictograms to the right.
			 */
			MultiUndoOp multiOp = new MultiUndoOp();
			FileGroupUndoOp op = new FileGroupUndoOp(buildStore, targetFileGroupId);
			ArrayList<Integer> newMembers = getFileGroupAsArrayList(targetFileGroupId);
			op.recordMembershipChange(currentMembers, newMembers);
			multiOp.add(op);
			
			/* bump to the right (now), and record changes in undo/redo stack */
			layoutAlgorithm.bumpPictogramsRight(multiOp, IPackageMemberMgr.TYPE_FILE_GROUP, sourceFileGroupId, true);
			
			/* record, but don't execute now, since we've already added the members and bumped the pictograms */
			new UndoOpAdapter("Add to Merge Group", multiOp).record();
			
			return;
		}
		 
		/*
		 * Handle case where we dragged a SOURCE/GENERATED/MERGE group onto an existing SOURCE
		 * or GENERATED group. This results in the creation of a new MERGE group.
		 */
		else {
			List<Integer> subGroupIds = new ArrayList<Integer>();
			subGroupIds.add(sourceFileGroupId);
			subGroupIds.add(targetFileGroupId);
			createMergeFileGroup(editor.getPackageId(), subGroupIds);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/	
}
