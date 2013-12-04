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
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.transaction.RecordingCommand;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.context.impl.AddConnectionContext;
import org.eclipse.graphiti.features.context.impl.AddContext;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.pattern.AbstractPattern;
import org.eclipse.graphiti.pattern.IPattern;

import com.buildml.eclipse.bobj.UIAction;
import com.buildml.eclipse.bobj.UIFileActionConnection;
import com.buildml.eclipse.bobj.UIFileGroup;
import com.buildml.eclipse.bobj.UIInteger;
import com.buildml.eclipse.bobj.UIMergeFileGroupConnection;
import com.buildml.eclipse.packages.PackageDiagramEditor;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.eclipse.utils.errors.FatalError;
import com.buildml.model.IActionMgr;
import com.buildml.model.IActionTypeMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileGroupMgr;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMemberMgr.MemberDesc;
import com.buildml.model.ISlotTypes;
import com.buildml.model.ISlotTypes.SlotDetails;

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
	
	/** The IPackageMemberMgr in this BuildStore */
	private IPackageMemberMgr pkgMemberMgr;
	
	/** The IActionMgr in this BuildStore */
	private IActionMgr actionMgr;
	
	/** The IActionTypeMgr in this BuildStore */
	private IActionTypeMgr actionTypeMgr;
	
	/** The IFileGroupMgr in this BuildStore */
	private IFileGroupMgr fileGroupMgr;
	
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
		editor = (PackageDiagramEditor)(getDiagramBehavior().getDiagramContainer());
		buildStore = editor.getBuildStore();
		pkgMemberMgr = buildStore.getPackageMemberMgr();
		actionMgr = buildStore.getActionMgr();
		actionTypeMgr = buildStore.getActionTypeMgr();
		fileGroupMgr = buildStore.getFileGroupMgr();
		pkgId = editor.getPackageId();
		
		final ContainerShape container = (ContainerShape)context.getPictogramElement();
        TransactionalEditingDomain editingDomain = editor.getEditingDomain();

        /*
         * Add each of the Diagram elements. This must be done as part of a transaction.
         */
		CommandStack commandStack = editingDomain.getCommandStack();
		commandStack.execute(new RecordingCommand(editingDomain) {
			@Override
			protected void doExecute() {
				
				/* fetch the complete list of members in the package */
				MemberDesc members[] = pkgMemberMgr.getMembersInPackage(
						pkgId, IPackageMemberMgr.SCOPE_NONE, IPackageMemberMgr.TYPE_ANY);
				int row = 0, column = 0;
				for (MemberDesc member : members) {
					
					/* create a business object to represent this member */
					UIInteger memberBo = null;
					switch (member.memberType) {
					case IPackageMemberMgr.TYPE_ACTION:
						memberBo = new UIAction(member.memberId);
						break;
					case IPackageMemberMgr.TYPE_FILE_GROUP:
						int fileGroupType = fileGroupMgr.getGroupType(member.memberId);
						if (fileGroupType != IFileGroupMgr.FILTER_GROUP) {
							memberBo = new UIFileGroup(member.memberId);
						}
						break;
					case IPackageMemberMgr.TYPE_FILE:
						/* ignore */
						break;
					default:
						throw new FatalError("Unrecognized package member type: " + member.memberType);
					}
					
					/*
					 * Add the business object (UIFileGroup, UIAction, etc) to the diagram.
					 */
					if (memberBo != null) {
						
						/*
						 * Determine the location for this pictogram. If the database
						 * has a valid location, use it. If not, perform a very simple
						 * placement algorithm.
						 */
						if (member.x == -1) {
							member.x = column * 150;
							member.y = row * 50;
							column++;
							if (column == 5) {
								column = 0;
								row++;
							}
						}
						
						container.eResource().getContents().add(memberBo);
						AddContext context2 = new AddContext();
						context2.setNewObject(memberBo);
						context2.setLocation(member.x, member.y);
						context2.setTargetContainer(container);
						getFeatureProvider().addIfPossible(context2);
					}
				}
				
				/*
				 * Now draw connections between various file groups, and actions.
				 */
				for (MemberDesc member : members) {
					
					/* for package members that are actions... */
					if (member.memberType == IPackageMemberMgr.TYPE_ACTION) {
						addFileActionConnections(member);
					}
					
					/* for merge file group, draw the input arrows */
					else if (member.memberType == IPackageMemberMgr.TYPE_FILE_GROUP) {
						int fileGroupType = fileGroupMgr.getGroupType(member.memberId);
						if (fileGroupType == IFileGroupMgr.MERGE_GROUP) {
							addMergeFileGroupConnections(member);
						}
					}
				}
			}
				
		});
		
		return true;
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * For each of an action's slots that have a file group connected to them, draw a connection
	 * line, and possibly a filter. At this point we're not actually drawing them, but we're
	 * adding their business objects (UIAction, UIFileActionConnection, etc) to the diagram
	 * to be drawn later by Graphiti.
	 * 
	 * @param member The UIAction's details.
	 */
	private void addFileActionConnections(MemberDesc member) {

		/* get the complete list of input slots for this action - whether set, or not set */
		int actionId = member.memberId;
		int actionTypeId = actionMgr.getActionType(actionId);
		if (actionTypeId < 0) {
			return;
		}
		SlotDetails slots[] = actionTypeMgr.getSlots(actionTypeId, ISlotTypes.SLOT_POS_ANY);
		if (slots == null) {
			return;
		}
		
		/*
		 * For each slot within the action, see if there's a FileGroup in the slot.
		 */
		for (int i = 0; i < slots.length; i++) {
			int slotId = slots[i].slotId;
			int slotPos = slots[i].slotPos;
			
			/* connections can only come from input or output slots... */
			if ((slotPos == ISlotTypes.SLOT_POS_INPUT) || (slotPos == ISlotTypes.SLOT_POS_OUTPUT)){

				Object slotValue = actionMgr.getSlotValue(actionId, slotId);

				/* yes, this slot has a file group in it */
				if ((slotValue != null) && (slotValue instanceof Integer)) {
					Integer fileGroupId = (Integer)slotValue;
					int connectionDirection = (slotPos == ISlotTypes.SLOT_POS_INPUT) ?
							UIFileActionConnection.INPUT_TO_ACTION :
							UIFileActionConnection.OUTPUT_FROM_ACTION;

					/* 
					 * if this is INPUT_TO_ACTION, and fileGroupId is a filter, register the
					 * filter in the connection object, and instead draw the line to the
					 * filter's input. 
					 */
					int filterGroupId = -1;
					if (connectionDirection == UIFileActionConnection.INPUT_TO_ACTION) {
						int fileGroupType = fileGroupMgr.getGroupType(fileGroupId);
						if (fileGroupType == IFileGroupMgr.FILTER_GROUP) {
							filterGroupId = fileGroupId;
							fileGroupId = fileGroupMgr.getPredId(fileGroupId);
						}
					}
					
					/* create the new business object */
					UIFileActionConnection newConnection = new UIFileActionConnection(
							fileGroupId, actionId, slotId, connectionDirection);
					if (filterGroupId != -1) {
						newConnection.setFilterGroupId(filterGroupId);
					}
					getDiagram().eResource().getContents().add(newConnection);

					/*
					 * Now we need to figure out which pictograms this connection goes between. We
					 * know the fileGroupId and actionId, but need to search for the pictogram.
					 * Search for the UIFileGroup and UIAction end points, and retrieve their anchors
					 */
					Anchor fileGroupAnchor = getAnchorFor(IPackageMemberMgr.TYPE_FILE_GROUP, fileGroupId, connectionDirection);
					Anchor actionAnchor = getAnchorFor(IPackageMemberMgr.TYPE_ACTION, actionId, connectionDirection);

					/* We found both anchors, so now draw the connection between them */
					if ((fileGroupAnchor != null) && (actionAnchor != null)) {
						AddConnectionContext addContext = new AddConnectionContext(fileGroupAnchor, actionAnchor);
						addContext.setNewObject(newConnection);
						getFeatureProvider().addIfPossible(addContext);
					}
				}
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * For each member of the merge file group, draw an arrow from the right side of the 
	 * sub file group into the left side of the merge.
	 * @param member Details of the merge file group.
	 */
	protected void addMergeFileGroupConnections(MemberDesc member) {
		
		/* the merge group's anchor point */
		Anchor targetAnchor = getAnchorFor(IPackageMemberMgr.TYPE_FILE_GROUP, member.memberId, 
				UIMergeFileGroupConnection.INPUT_TO_MERGE_GROUP);
		if (targetAnchor == null) {
			return;
		}
		
		/* for each of the input sub groups... */
		int groupSize = fileGroupMgr.getGroupSize(member.memberId);
		for (int i = 0; i != groupSize; i++) {
			int subGroupId = fileGroupMgr.getSubGroup(member.memberId, i);

			/* 
			 * If the subGroupId is a filter group, record the filter in the 
			 * connection, but actually draw the line to the filter's predecessor.
			 */
			int filterGroupId = -1;
			int subGroupType = fileGroupMgr.getGroupType(subGroupId);
			if (subGroupType == IFileGroupMgr.FILTER_GROUP) {
				filterGroupId = subGroupId;
				subGroupId = fileGroupMgr.getPredId(subGroupId);
			}
			
			/* find the sub group's anchor */
			Anchor sourceAnchor = getAnchorFor(IPackageMemberMgr.TYPE_FILE_GROUP, subGroupId, 
												UIMergeFileGroupConnection.OUTPUT_FROM_SUB_GROUP);

			/* create a new business object (to be displayed) */
			UIMergeFileGroupConnection newConnection = new UIMergeFileGroupConnection(subGroupId, member.memberId, i);
			if (filterGroupId != -1) {
				newConnection.setFilterGroupId(filterGroupId);
			}
			getDiagram().eResource().getContents().add(newConnection);
		
			/* We found the sub group's anchor, so now draw the connection */
			if (sourceAnchor != null) {
				AddConnectionContext addContext = new AddConnectionContext(sourceAnchor, targetAnchor);
				addContext.setNewObject(newConnection);
				getFeatureProvider().addIfPossible(addContext);
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * For a particular package member, return the relevant Graphiti anchor.
	 * 
	 * @param memberType	The type of the member (TYPE_ACTION, TYPE_FILE_GROUP, etc).
	 * @param memberId		The ID of the member (fileGroupId, or actionId, etc).
	 * @param connectionId	The ID of the anchor (0, 1, etc.).
	 * @return The Graphiti anchor associated with this package member, or null if the member can't
	 * be found.
	 */
	private Anchor getAnchorFor(int memberType, Integer memberId, int connectionId) {

		/* visit each shape in the current diagram, looking for the package member */
		EList<Shape> shapes = getDiagram().getChildren();
		for (Shape shape : shapes) {
			Object bo = GraphitiUtils.getBusinessObject(shape);
			if ((memberType == IPackageMemberMgr.TYPE_FILE_GROUP) && (bo instanceof UIFileGroup)) {
				if (((UIFileGroup)bo).getId() == memberId) {
					return shape.getAnchors().get(connectionId);
				}
			} 
			
			else if ((memberType == IPackageMemberMgr.TYPE_ACTION) && (bo instanceof UIAction)) {
				if (((UIAction)bo).getId() == memberId) {
					return shape.getAnchors().get(connectionId);
				}
			}
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

}
