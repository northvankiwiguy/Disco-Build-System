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
import com.buildml.eclipse.packages.PackageDiagramEditor;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.eclipse.utils.errors.FatalError;
import com.buildml.model.IActionMgr;
import com.buildml.model.IActionTypeMgr;
import com.buildml.model.IBuildStore;
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
		editor = (PackageDiagramEditor)getDiagramEditor();
		buildStore = editor.getBuildStore();
		pkgMemberMgr = buildStore.getPackageMemberMgr();
		actionMgr = buildStore.getActionMgr();
		actionTypeMgr = buildStore.getActionTypeMgr();
		pkgId = editor.getPackageId();
		
		final ContainerShape container = (ContainerShape)context.getPictogramElement();
        TransactionalEditingDomain editingDomain = getDiagramEditor().getEditingDomain();

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
					
					/* create a business object to represent this member */
					UIInteger memberBo;
					switch (member.memberType) {
					case IPackageMemberMgr.TYPE_ACTION:
						memberBo = new UIAction(member.memberId);
						break;
					case IPackageMemberMgr.TYPE_FILE_GROUP:
						memberBo = new UIFileGroup(member.memberId);
						break;
					default:
						throw new FatalError("Unrecognized package member type: " + member.memberType);
					}
					
					/*
					 * TODO: Fix this layout algorithm. For now it simply lays out
					 * actions in rows and columns.
					 */
					container.eResource().getContents().add(memberBo);
					AddContext context2 = new AddContext();
					context2.setNewObject(memberBo);
					context2.setLocation(member.x, member.y);
					context2.setTargetContainer(container);
					getFeatureProvider().addIfPossible(context2);
				}
				
				/*
				 * Now draw connections between file groups and actions.
				 */
				for (MemberDesc member : members) {
					
					/* for package members that are actions... */
					if (member.memberType == IPackageMemberMgr.TYPE_ACTION) {
						addFileActionConnections(member);
					}
				}
			}
				
		});
		
		return true;
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * For each of an action's slots that have a file group connected to them, draw a connection
	 * line.
	 * @param member The UIAction's details.
	 */
	private void addFileActionConnections(MemberDesc member) {

		/* get the complete list of input slots for this action - whether set, or not set */
		int actionId = member.memberId;
		int actionTypeId = actionMgr.getActionType(actionId);
		if (actionTypeId < 0) {
			return;
		}
		SlotDetails slots[] = actionTypeMgr.getSlots(actionTypeId, ISlotTypes.SLOT_POS_INPUT);
		if (slots == null) {
			return;
		}
		
		/*
		 * For each slot within the action, see if there's a FileGroup in the slot.
		 */
		for (int i = 0; i < slots.length; i++) {
			int slotId = slots[i].slotId;
			Object slotValue = actionMgr.getSlotValue(actionId, slotId);
			
			/* yes, this slot has a file group in it */
			if ((slotValue != null) && (slotValue instanceof Integer)) {
				Integer fileGroupId = (Integer)slotValue;
				
				/* create the new business object */
				UIFileActionConnection newConnection = new UIFileActionConnection(
						fileGroupId, actionId, slotId, UIFileActionConnection.INPUT_TO_ACTION);
				
				/*
				 * Now we need to figure out which pictograms this connection goes between. We
				 * know the fileGroupId and actionId, but need to search for the pictogram.
				 * Search for the UIFileGroup and UIAction end points, and retrieve their anchors
				 */
				Anchor fileGroupAnchor = null, actionAnchor = null;
				EList<Shape> shapes = getDiagram().getChildren();
				for (Shape shape : shapes) {
					Object bo = GraphitiUtils.getBusinessObject(shape);
					if (bo instanceof UIFileGroup) {
						if (((UIFileGroup)bo).getId() == fileGroupId) {
							fileGroupAnchor = shape.getAnchors().get(0);
						}
					} else if (bo instanceof UIAction) {
						if (((UIAction)bo).getId() == actionId) {
							actionAnchor = shape.getAnchors().get(0);
						}
					}
				}
				
				/* We found both anchors, so now draw the connection between them */
				if ((fileGroupAnchor != null) && (actionAnchor != null)) {
					AddConnectionContext addContext = new AddConnectionContext(fileGroupAnchor, actionAnchor);
					addContext.setNewObject(newConnection);
					getFeatureProvider().addIfPossible(addContext);
				}
			}
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

}
