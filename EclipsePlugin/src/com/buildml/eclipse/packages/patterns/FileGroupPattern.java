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

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.ICreateContext;
import org.eclipse.graphiti.features.context.IMoveShapeContext;
import org.eclipse.graphiti.features.context.IResizeShapeContext;
import org.eclipse.graphiti.mm.algorithms.Polygon;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
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

import com.buildml.eclipse.bobj.UIFileGroup;
import com.buildml.eclipse.filegroups.FileGroupChangeOperation;
import com.buildml.eclipse.packages.PackageDiagramEditor;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileGroupMgr;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMemberMgr.MemberLocation;
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
	
	/*
	 * Various colour constants used in displaying this element.
	 */
	private static final IColorConstant TEXT_FOREGROUND = IColorConstant.BLACK;
	private static final IColorConstant LINE_COLOUR = new ColorConstant(98, 131, 167);
	private static final IColorConstant FILL_COLOUR = new ColorConstant(187, 218, 247);
	private static final IColorConstant FILL_CORNER_COLOUR = new ColorConstant(160, 190, 220);

	/*
	 * Size of this element (in pixels).
	 */
	private static final int FILE_GROUP_WIDTH = 50;
	private static final int FILE_GROUP_HEIGHT = 50;
	private static final int FILE_GROUP_CORNER_SIZE = 20;
	private static final int FILE_GROUP_OVERLAP = 3;
	
	/** Coordinates of the file's front page */
	int coordsPage1[] = new int[] { 
			0, 0, 
			FILE_GROUP_WIDTH - FILE_GROUP_CORNER_SIZE, 0,
			FILE_GROUP_WIDTH, FILE_GROUP_CORNER_SIZE,
			FILE_GROUP_WIDTH, FILE_GROUP_HEIGHT,
			0, FILE_GROUP_HEIGHT 
	};
	
	/** Coordinates of the bent corner */
	int coordsCorner[] = new int[] {
			FILE_GROUP_WIDTH - FILE_GROUP_CORNER_SIZE, 0,
			FILE_GROUP_WIDTH, FILE_GROUP_CORNER_SIZE,
			FILE_GROUP_WIDTH - FILE_GROUP_CORNER_SIZE, FILE_GROUP_CORNER_SIZE
	};
		
	/** Coordinates for the file group's second page (if one is drawn) */
	int coordsPage2[] = new int[] { 
			FILE_GROUP_OVERLAP, FILE_GROUP_OVERLAP, 
			FILE_GROUP_OVERLAP + FILE_GROUP_WIDTH, FILE_GROUP_OVERLAP,
			FILE_GROUP_OVERLAP + FILE_GROUP_WIDTH, FILE_GROUP_OVERLAP + FILE_GROUP_HEIGHT,
			FILE_GROUP_OVERLAP, FILE_GROUP_OVERLAP + FILE_GROUP_HEIGHT 
	};
	
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
		return (mainBusinessObject instanceof UIFileGroup) ||
				(mainBusinessObject instanceof IFile);
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
		if ((newObject instanceof UIFileGroup) || (newObject instanceof IFile)) {
			
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
		if ((addedObject instanceof IFile) && (targetObject instanceof Diagram)) {
			
			/* create a totally new file group the database, and add the file to it */
			IFile file = (IFile)addedObject;
			UIFileGroup newFileGroup = addToFileGroup(null, file.getFullPath().toOSString());
			if (newFileGroup == null) {
				return null;
			}
			/* set the x and y coordinates correctly */
			pkgMemberMgr.setMemberLocation(IPackageMemberMgr.TYPE_FILE_GROUP, newFileGroup.getId(), x, y);
			
			/* render the new file group */
			return renderPictogram((Diagram)targetObject, newFileGroup, x, y);			
		}
		
		/* Case #3 - drag IFile (or many) onto existing UIFileGroup */
		if (addedObject instanceof IFile) {
			ContainerShape existingPictogram = (ContainerShape)targetObject;
			Object bo = GraphitiUtils.getBusinessObject(targetObject);
			if (bo instanceof UIFileGroup) {
				IFile file = (IFile)addedObject;
				UIFileGroup fileGroup = (UIFileGroup)bo;
				fileGroup = addToFileGroup(fileGroup, file.getFullPath().toOSString());
				if (fileGroup == null) {
					return null;
				}
				Diagram diagram = (Diagram) (existingPictogram.getContainer());
				return renderPictogram(diagram, fileGroup, x, y);			
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
		
		/* 
		 * Validate where the UIFileGroup is moving to. We can't move UIFileGroups 
		 * off the left/top of the window.
		 */
		int x = context.getX();
		int y = context.getY();		
		if ((x < 0) || (y < 0)) {
			return false;
		}
		
		/* check that we've moved a single UIFileGroup object */
		PictogramElement pe = context.getPictogramElement();
		PictogramLink pl = pe.getLink();
		EList<EObject> bos = pl.getBusinessObjects();
		if (bos.size() != 1) {
			return false;
		}
		
		/* 
		 * Finally, check that this is a UIFileGroup (although we probably wouldn't have
		 * got here otherwise.
		 */
		Object bo = bos.get(0);
		return (bo instanceof UIFileGroup);
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
		PictogramLink pl = context.getPictogramElement().getLink();
		UIFileGroup fileGroup = (UIFileGroup)(pl.getBusinessObjects().get(0));
		int fileGroupId = fileGroup.getId();
		
		/* determine the UIFileGroups's old location */
		MemberLocation oldXY = pkgMemberMgr.getMemberLocation(IPackageMemberMgr.TYPE_FILE_GROUP, fileGroupId);
		if (oldXY == null){
			/* default, in the case of an error */
			oldXY = new MemberLocation();
			oldXY.x = 0;
			oldXY.y = 0;
		}
		
		/* create an undo/redo operation that will invoke the underlying database changes */
		FileGroupChangeOperation op = new FileGroupChangeOperation("move file group", fileGroupId);
		op.recordLocationChange(oldXY.x, oldXY.y, x, y);
		op.recordAndInvoke();
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Append a file path onto the end of a UIFileGroup. If the group doesn't yet exist
	 * (is null), create a new UIFileGroup.
	 * 
	 * @param fileGroup The UIFileGroup to append the path to, or null if no group yet exists.
	 * @param fullPath The absolute path to add to the file group.
	 * @return The file group, with the new file appended to the end.
	 */
	private UIFileGroup addToFileGroup(UIFileGroup fileGroup, String fullPath) {
		
		IFileGroupMgr fileGroupMgr = buildStore.getFileGroupMgr();
		IFileMgr fileMgr = buildStore.getFileMgr();
		
		/* convert the path into an absolute path (not workspace-relative) */
		fullPath = EclipsePartUtils.workspaceRelativeToAbsolutePath(fullPath);
		
		/* 
		 * If necessary, create a brand new UIFileGroup in the database.
		 */
		if (fileGroup == null) {
			int pkgId = editor.getPackageId();
			int fileGroupId = fileGroupMgr.newSourceGroup(pkgId);
			if (fileGroupId == ErrorCode.NOT_FOUND) {
				return null; /* invalid pkgId */
			}
			fileGroup = new UIFileGroup(fileGroupId);
			getDiagram().eResource().getContents().add(fileGroup);
		}

		/* now append the path to the fileGroup */
		int newPathId = fileMgr.addFile(fullPath);
		if (newPathId == ErrorCode.BAD_PATH) {
			return fileGroup; /* couldn't add the path - return the unchanged filegroup */
		}
		int index = fileGroupMgr.addPathId(fileGroup.getId(), newPathId);
		if (index < 0) {
			return fileGroup; /* couldn't add the path to the group - return unchanged filegroup */
		}

		/* all is good - return modified filegroup */
		return fileGroup;
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
		
		/*
		 * How many boxes will be shown? This helps us distinguish between file groups
		 * containing a single file, versus multiple files.
		 */
		int groupSize = fileGroupMgr.getGroupSize(addedFileGroup.getId());
		if (groupSize < 0) {
			return null; /* an error */
		}
		
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
		gaService.setLocationAndSize(invisibleRectangle, x, y, 
										FILE_GROUP_WIDTH + ((groupSize < 2) ? 0 : FILE_GROUP_OVERLAP), 
										FILE_GROUP_HEIGHT + ((groupSize < 2) ? 0 : FILE_GROUP_OVERLAP));

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

		/* add a chopbox anchor to the shape */
		peCreateService.createChopboxAnchor(containerShape);

		/* create a link between the shape and the business object, and display it. */
		link(containerShape, addedFileGroup);
		layoutPictogramElement(containerShape);
		return containerShape;
	}

	/*-------------------------------------------------------------------------------------*/
}
