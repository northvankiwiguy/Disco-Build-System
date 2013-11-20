/*******************************************************************************
 * Copyright (c) 2013 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.packages.features;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.graphiti.features.IDeleteFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.IDeleteContext;
import org.eclipse.graphiti.pattern.IPattern;

import com.buildml.eclipse.actions.ActionChangeOperation;
import com.buildml.eclipse.bobj.UIFileActionConnection;
import com.buildml.eclipse.bobj.UIMergeFileGroupConnection;
import com.buildml.eclipse.filegroups.FileGroupChangeOperation;
import com.buildml.eclipse.packages.DiagramFeatureProvider;
import com.buildml.eclipse.packages.PackageDiagramEditor;
import com.buildml.eclipse.utils.BmlMultiOperation;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileGroupMgr;

/**
 * A customized "DeleteFeature" that calls upon the appropriate Pattern to do the work
 * of deleting a selected pictogram and its underlying business object.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class PackageDiagramDeleteFeature implements IDeleteFeature {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The feature provider that owns the patterns we use */
	private DiagramFeatureProvider featureProvider;
	
	/** The IBuildStore we operate on */
	private IBuildStore buildStore;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new feature provider for handling our delete operations within Graphiti.
	 * @param diagramFeatureProvider The feature provider that owns the patterns.
	 */
	public PackageDiagramDeleteFeature(DiagramFeatureProvider diagramFeatureProvider) {
		this.featureProvider = diagramFeatureProvider;
		
		PackageDiagramEditor pde = 
				(PackageDiagramEditor)featureProvider.getDiagramTypeProvider().getDiagramEditor();
		buildStore = pde.getBuildStore();
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.features.IFeature#isAvailable(org.eclipse.graphiti.features.context.IContext)
	 */
	@Override
	public boolean isAvailable(IContext context) {
		return true;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.features.IFeature#canExecute(org.eclipse.graphiti.features.context.IContext)
	 */
	@Override
	public boolean canExecute(IContext context) {
		return true;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Proceed to execute the delete operation. Only the pattern can do this.
	 */
	@Override
	public void execute(IContext context) {
		
		/* identify the pattern to be used */
		IDeleteContext deleteContext = (IDeleteContext)context;
		IPattern pattern = featureProvider.getPatternForPictogramElement(deleteContext.getPictogramElement());
		if (pattern != null) {			
			pattern.delete(deleteContext);
		}
		
		/* connections don't have patterns, so we need a special method to delete them */
		else {
			Object bo = GraphitiUtils.getBusinessObject(deleteContext.getPictogramElement());
			if (bo instanceof UIFileActionConnection) {
				deleteFileActionConnection(deleteContext);
			}
			else if (bo instanceof UIMergeFileGroupConnection) {
				deleteMergeFileGroupConnection(deleteContext);
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * We don't use Graphiti's undo/redo stack.
	 */
	@Override
	public boolean canUndo(IContext context) {
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * We don't want to appear in Graphiti's undo/redo stack. We'll maintain our own.
	 */
	@Override
	public boolean hasDoneChanges() {
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.IName#getName()
	 */
	@Override
	public String getName() {
		return "PackageDiagramDeleteFeature";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.IDescription#getDescription()
	 */
	@Override
	public String getDescription() {
		return "BuildML Delete Feature";
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.features.IFeatureProviderHolder#getFeatureProvider()
	 */
	@Override
	public IFeatureProvider getFeatureProvider() {
		return featureProvider;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine if we can delete the type of business object that's selected. Only the
	 * pattern will know this.
	 */
	@Override
	public boolean canDelete(IDeleteContext context) {
		
		/* identify the pattern to be used */
		IPattern pattern = featureProvider.getPatternForPictogramElement(context.getPictogramElement());
		if (pattern != null) {
			return pattern.canDelete(context);
		}

		Object bo = GraphitiUtils.getBusinessObject(context.getPictogramElement());
		if (bo instanceof UIFileActionConnection) {
			return true;
		} else if (bo instanceof UIMergeFileGroupConnection) {
			return true;
		}
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.graphiti.func.IDelete#preDelete(org.eclipse.graphiti.features.context.IDeleteContext)
	 */
	@Override
	public void preDelete(IDeleteContext context) {
		/* unused */
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.func.IDelete#delete(org.eclipse.graphiti.features.context.IDeleteContext)
	 */
	@Override
	public void delete(IDeleteContext context) {
		/* unused */
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.func.IDelete#postDelete(org.eclipse.graphiti.features.context.IDeleteContext)
	 */
	@Override
	public void postDelete(IDeleteContext context) {
		/* unused */
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * A special-purpose method for deleting a fileGroup-action connection arrow. There are
	 * two possible scenarios. If the connection has a filter, the delete operation will remove
	 * the filter only. If it doesn't have a filter, the whole connection will be removed.
	 * Therefore, the user might need to press "delete" twice to completely remove the
	 * connection.
	 * 
	 * @param context The Graphiti context of the delete operation.
	 */
	private void deleteFileActionConnection(IDeleteContext context) {
		Object bo = GraphitiUtils.getBusinessObject(context.getPictogramElement());
		if (!(bo instanceof UIFileActionConnection)) {
			return;
		}
		UIFileActionConnection connection = (UIFileActionConnection)bo;
	
		/* If there's filter, delete it */
		if (connection.hasFilter()) {
			
			BmlMultiOperation multiOp = new BmlMultiOperation("Delete Filter");
			
			/* set the action's slot to refer to the filter's predecessor */
			ActionChangeOperation actionOp = new ActionChangeOperation("", connection.getActionId());
			actionOp.recordSlotChange(connection.getSlotId(), connection.getFilterGroupId(), 
									connection.getFileGroupId());
			multiOp.add(actionOp);
			
			/* set the filter group's content to empty, so it'll be garbage collected upon shutdown */
			emptyFilterGroup(multiOp, connection.getFilterGroupId());
			multiOp.recordAndInvoke();
		}
		
		/* no filter, so delete the connection */
		else {
			/* create an undo/redo operation to set the slot value back to null */
			ActionChangeOperation op = new ActionChangeOperation("Delete Connection", connection.getActionId());
			op.recordSlotChange(connection.getSlotId(), connection.getFileGroupId(), null);
			op.recordAndInvoke();
		}
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * A special-purpose method for deleting a merge file group connection arrow. There are
	 * two possible scenarios. If the connection has a filter, the delete operation will remove
	 * the filter only. If it doesn't have a filter, the whole connection will be removed.
	 * Therefore, the user might need to press "delete" twice to completely remove the
	 * connection.
	 * 
	 * @param context The Graphiti context of the delete operation.
	 */
	private void deleteMergeFileGroupConnection(IDeleteContext context) {
		
		/* locate the connection information */
		Object bo = GraphitiUtils.getBusinessObject(context.getPictogramElement());
		if (!(bo instanceof UIMergeFileGroupConnection)) {
			return;
		}
		IFileGroupMgr fileGroupMgr = buildStore.getFileGroupMgr();
		
		UIMergeFileGroupConnection connection = (UIMergeFileGroupConnection)bo;
		int mergeFileGroupId = connection.getTargetFileGroupId();
		int subFileGroupId = connection.getSourceFileGroupId();
		int index = connection.getIndex();
		
		/*
		 * If there's a filter, remove that only.
		 */
		if (connection.hasFilter()) {
			BmlMultiOperation multiOp = new BmlMultiOperation("Delete Filter");
			
			/* replace the merge group's "index" entry with the filter group's predecessor */
			FileGroupChangeOperation mergeGroupOp = new FileGroupChangeOperation("", mergeFileGroupId);
			Integer members[] = fileGroupMgr.getSubGroups(mergeFileGroupId);
			ArrayList<Integer> currentMembers = new ArrayList<Integer>(Arrays.asList(members));
			ArrayList<Integer> newMembers = new ArrayList<Integer>(currentMembers);
			newMembers.set(index, subFileGroupId);
			mergeGroupOp.recordMembershipChange(currentMembers, newMembers);
			mergeGroupOp.recordAndInvoke();
			
			multiOp.add(mergeGroupOp);
			
			/* set the filter group's content to empty, so it'll be garbage collected upon shutdown */
			emptyFilterGroup(multiOp, connection.getFilterGroupId());
			multiOp.recordAndInvoke();			
		}
		
		/*
		 * If there's no filter, remove the whole connection.
		 */
		else {
			/* our new membership will be the same as the current membership, with subFileGroupId removed */
			FileGroupChangeOperation op = new FileGroupChangeOperation("Delete Connection", mergeFileGroupId);
			Integer members[] = fileGroupMgr.getSubGroups(mergeFileGroupId);
			ArrayList<Integer> currentMembers = new ArrayList<Integer>(Arrays.asList(members));
			ArrayList<Integer> newMembers = new ArrayList<Integer>(currentMembers);
			newMembers.remove(index);		
			op.recordMembershipChange(currentMembers, newMembers);
			op.recordAndInvoke();
		}
	}
	
	/*-------------------------------------------------------------------------------------*/		

	/**
	 * Helper method for creating the undo/redo commands for emptying a filter file group.
	 * @param multiOp	The BmlMultiOperation to append the commands to.
	 * @param groupId	The filter group's ID.
	 */
	private void emptyFilterGroup(BmlMultiOperation multiOp, int groupId) {
		FileGroupChangeOperation fileGroupOp = new FileGroupChangeOperation("", groupId);
		IFileGroupMgr fileGroupMgr = buildStore.getFileGroupMgr();
		String currentMembers[] = fileGroupMgr.getPathStrings(groupId);
		if (currentMembers != null) {
			fileGroupOp.recordMembershipChange(Arrays.asList(currentMembers), new ArrayList<String>());
			multiOp.add(fileGroupOp);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/	
}
