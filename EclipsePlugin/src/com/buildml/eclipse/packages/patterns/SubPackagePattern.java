/*******************************************************************************
 * Copyright (c) 2014 Arapiki Solutions Inc.
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
import org.eclipse.graphiti.features.context.IDeleteContext;
import org.eclipse.graphiti.features.context.IMoveShapeContext;
import org.eclipse.graphiti.features.context.IResizeShapeContext;
import org.eclipse.graphiti.mm.algorithms.Polygon;
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

import com.buildml.eclipse.bobj.UIPackage;
import com.buildml.eclipse.bobj.UISubPackage;
import com.buildml.eclipse.packages.PackageDiagramEditor;
import com.buildml.eclipse.packages.layout.PictogramSize;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.eclipse.utils.UndoOpAdapter;
import com.buildml.eclipse.utils.errors.FatalError;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMgr;
import com.buildml.model.ISubPackageMgr;
import com.buildml.model.IPackageMemberMgr.MemberLocation;
import com.buildml.model.undo.SubPackageUndoOp;
import com.buildml.utils.errors.ErrorCode;

/**
 * A Graphiti pattern for managing the "SubPackage" graphical element in a BuildML diagram.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class SubPackagePattern extends AbstractPattern implements IPattern {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** The IBuildStore that this diagram represents */
	private IBuildStore buildStore;
	
	/** The managers owned by this BuildStore */
	private IPackageMemberMgr pkgMemberMgr;
	private ISubPackageMgr subPkgMgr;
	private IPackageMgr pkgMgr;
	
	/*
	 * Various colour constants used in displaying this element.
	 */
	private static final IColorConstant TEXT_FOREGROUND = IColorConstant.BLACK;
	private static final IColorConstant LINE_COLOUR = new ColorConstant(150, 186, 150);
	private static final IColorConstant FILL_COLOUR = new ColorConstant(200, 255, 200);

	/*
	 * Size of this element (in pixels).
	 */
	private static final int SUB_PACKAGE_WIDTH = 80;
	private static final int SUB_PACKAGE_HEIGHT = 60;
	private static final int SUB_PACKAGE_TAB_WIDTH = 20;
	private static final int SUB_PACKAGE_TAB_HEIGHT = 12;
	
	/** Font type for labels */
	private static final String LABEL_FONT = "courier";
	
	/** Font size */
	private static final int LABEL_FONT_SIZE = 9;
	
	/**
	 * The geometric coordinates for drawing a sub-package pictogram.
	 */
	int coords[] = new int[] { 
			0, 0,
			SUB_PACKAGE_TAB_WIDTH, 0,
			SUB_PACKAGE_TAB_WIDTH, SUB_PACKAGE_TAB_HEIGHT,
			SUB_PACKAGE_WIDTH - 1, SUB_PACKAGE_TAB_HEIGHT,
			SUB_PACKAGE_WIDTH - 1, SUB_PACKAGE_HEIGHT - 1,
			0, SUB_PACKAGE_HEIGHT - 1
	};
	
	/** The (static) maximum size of a file group pictogram, in pixels */
	private static PictogramSize SUB_PACKAGE_MAX_SIZE = 
			new PictogramSize(SUB_PACKAGE_WIDTH, SUB_PACKAGE_HEIGHT);
	
	/*=====================================================================================*
	 * STATIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Return the (width, height) in pixel of the sub-package pictogram. This is used
	 * for laying-out the package members.
	 * @return The (width, height) in pixels.
	 */
	public static PictogramSize getSize() {
		return SUB_PACKAGE_MAX_SIZE;
	}
		
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link SubPackagePattern} object.
	 */
	public SubPackagePattern() {
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
		return "Sub Package";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.pattern.AbstractPattern#isMainBusinessObjectApplicable(java.lang.Object)
	 */
	@Override
	public boolean isMainBusinessObjectApplicable(Object mainBusinessObject) {
		return (mainBusinessObject instanceof UIPackage) || (mainBusinessObject instanceof UISubPackage);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether a specific business object can be added to the diagram.
	 */
	@Override
	public boolean canAdd(IAddContext context) {
		
		Object newObject = context.getNewObject();
		return (newObject instanceof UIPackage) || (newObject instanceof UISubPackage);	
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create the visual representation of a UIFileGroup on the parent Diagram.
	 */
	@Override
	public PictogramElement add(IAddContext context) {

		PackageDiagramEditor editor = (PackageDiagramEditor)(getDiagramBehavior().getDiagramContainer());
		buildStore = editor.getBuildStore();
		pkgMemberMgr = buildStore.getPackageMemberMgr();
		pkgMgr = buildStore.getPackageMgr();
		subPkgMgr = buildStore.getSubPackageMgr();

		/*
		 * Case handled:
		 *   1) UIPackage dropped on a Diagram - we create a new SubPackage within this package.
		 *   2) UISubPackage - just display it on the diagram. It already exists in the database.
		 */
		Object addedObject = context.getNewObject();
		Object targetObject = context.getTargetContainer();
		if (!(targetObject instanceof Diagram)) {
			return null;
		}
		int x = context.getX();
		int y = context.getY();
		
		/* 
		 * Case #1 - add/drag an existing UIPackage onto the Diagram. We must add
		 * a new sub-package onto this diagram (i.e. stored in the database).
		 */ 
		if ((addedObject instanceof UIPackage) && (targetObject instanceof Diagram)) {
			
			
			UIPackage uiPkgType = (UIPackage)addedObject;
			int pkgTypeId = uiPkgType.getId();
			int subPkgId = subPkgMgr.newSubPackage(editor.getPackageId(), pkgTypeId);
			if (subPkgId < 0) {

				/* can we create a package of the requested type? */
				if (subPkgId == ErrorCode.NOT_FOUND) {
					AlertDialog.displayErrorDialog("Can't Add Sub-Package", 
							"You may not add a sub-package of this type.");					
				}
				
				/* cycle detection - would a loop be created? */
				else if (subPkgId == ErrorCode.LOOP_DETECTED) {
					AlertDialog.displayErrorDialog("Can't Add Sub-Package", 
						"You may not add a sub-package of this type, otherwise a loop will be created " +
							"in the package hierarchy.");
				}
				
				/* all other errors */
				else {
					AlertDialog.displayErrorDialog("Can't Add Sub-Package", 
							"An unexpected error occurred when attempting to add a new sub-package.");					
				}
				return null;
			}
			
			/* set the x and y coordinates correctly */
			pkgMemberMgr.setMemberLocation(IPackageMemberMgr.TYPE_SUB_PACKAGE, subPkgId, x, y);
			
			/* create an undo/redo operation to track this change */
			SubPackageUndoOp undoOp = new SubPackageUndoOp(buildStore, subPkgId);
			undoOp.recordNewSubPackage();
			new UndoOpAdapter("New Sub-Package", undoOp).record();
			return null;
		}
		
		else if (addedObject instanceof UISubPackage) {
			return renderPictogram((Diagram)targetObject, (UISubPackage)addedObject, x, y);						
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
		return context.getTargetContainer() instanceof Diagram;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.pattern.AbstractPattern#moveShape(org.eclipse.graphiti.features.context.IMoveShapeContext)
	 */
	@Override
	public void moveShape(IMoveShapeContext context) {
		super.moveShape(context);

		/*
		 * Fetch the x, y and subPkgId. Note that all error checking was done by canMoveShape().
		 */
		int x = context.getX();
		int y = context.getY();
		UISubPackage subPkg = 
				(UISubPackage)GraphitiUtils.getBusinessObject(context.getPictogramElement());
		int subPkgId = subPkg.getId();
		
		/*
		 * Are we moving a UISubPackage around the Diagram?
		 */
		Object targetObject = context.getTargetContainer();
		if (targetObject instanceof Diagram) {
		
			/* determine the UISubPackage's old location */
			MemberLocation oldXY = pkgMemberMgr.getMemberLocation(IPackageMemberMgr.TYPE_SUB_PACKAGE,
																	subPkgId);
			if (oldXY == null){
				/* default, in the case of an error */
				oldXY = new MemberLocation();
				oldXY.x = 0;
				oldXY.y = 0;
			}
		
			/* create an undo/redo operation that will invoke the underlying database changes */
			SubPackageUndoOp op = new SubPackageUndoOp(buildStore, subPkgId);
			op.recordLocationChange(oldXY.x, oldXY.y, x, y);
			new UndoOpAdapter("Move Sub-Package", op).invoke();
		}		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Yes, we can delete UISubPackage members.
	 */
	@Override
	public boolean canDelete(IDeleteContext context) {
		return true;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Invoked when the user initiates a "delete" operation on a UISubPackage.
	 */
	@Override
	public void delete(IDeleteContext context) {
		
		/* determine the business object that related to the pictogram being deleted */
		PictogramLink pl = context.getPictogramElement().getLink();
		UISubPackage subPkg = (UISubPackage)(pl.getBusinessObjects().get(0));
		int subPkgId = subPkg.getId();
		
		SubPackageUndoOp op = new SubPackageUndoOp(buildStore, subPkgId);
		op.recordRemoveSubPackage();
		
		/* invoke all changes in one step... */
		new UndoOpAdapter("Delete Sub-Package", op).invoke();
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Render a pictogram, representing a UISubPackage, onto a Graphiti Diagram.
	 * 
	 * @param targetDiagram The Diagram to add the pictogram to.
	 * @param subPkg The UIPackage to show a pictogram for.
	 * @param x The X location within the Diagram.
	 * @param y The Y location within the Diagram.
	 * @return The ContainerShape representing the UISubPackage pictogram.
	 */
	private PictogramElement renderPictogram(Diagram targetDiagram, UISubPackage subPkg,
												int x, int y) {
		
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
		gaService.setLocationAndSize(invisibleRectangle, x, y, SUB_PACKAGE_WIDTH, SUB_PACKAGE_HEIGHT);

		
		/* now the front page, with the corner bent */
		Polygon box = gaService.createPolygon(invisibleRectangle, coords);
		box.setForeground(manageColor(LINE_COLOUR));
		box.setBackground(manageColor(FILL_COLOUR));
		box.setLineWidth(2);
		
		/* Display the type of the package in the center of the box */
		int pkgTypeId = subPkgMgr.getSubPackageType(subPkg.getId());
		if (pkgTypeId < 0) {
			throw new FatalError("Invalid package ID");
		}
		String pkgTypeName = pkgMgr.getName(pkgTypeId);
		if (pkgTypeName != null) {
			Text pkgType = gaService.createText(getDiagram(), invisibleRectangle, 
				pkgTypeName, LABEL_FONT, LABEL_FONT_SIZE);
			pkgType.setFilled(false);
			pkgType.setForeground(manageColor(TEXT_FOREGROUND));
			pkgType.setHorizontalAlignment(Orientation.ALIGNMENT_CENTER);
			gaService.setLocationAndSize(pkgType, 
				LABEL_FONT_SIZE, LABEL_FONT_SIZE, 
				SUB_PACKAGE_WIDTH - (2 * LABEL_FONT_SIZE), SUB_PACKAGE_HEIGHT - (2 * LABEL_FONT_SIZE));
		}
		
		/* create a link between the shape and the business object, and display it. */
		link(containerShape, subPkg);
		layoutPictogramElement(containerShape);
		return containerShape;
	}
	
	/*-------------------------------------------------------------------------------------*/	
}
