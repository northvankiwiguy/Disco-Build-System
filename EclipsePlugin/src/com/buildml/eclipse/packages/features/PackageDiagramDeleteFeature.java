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

import org.eclipse.graphiti.features.IDeleteFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.IDeleteContext;
import org.eclipse.graphiti.pattern.IPattern;

import com.buildml.eclipse.actions.ActionChangeOperation;
import com.buildml.eclipse.bobj.UIFileActionConnection;
import com.buildml.eclipse.packages.DiagramFeatureProvider;
import com.buildml.eclipse.utils.GraphitiUtils;

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
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new feature provider for handling our delete operations within Graphiti.
	 * @param diagramFeatureProvider The feature provider that owns the patterns.
	 */
	public PackageDiagramDeleteFeature(DiagramFeatureProvider diagramFeatureProvider) {
		this.featureProvider = diagramFeatureProvider;
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
				deleteConnection(deleteContext);
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

	/*-------------------------------------------------------------------------------------*/

	/**
	 * A special-purpose method for deleting a connection arrow.
	 * @param context The Graphiti context of the delete operation.
	 */
	private void deleteConnection(IDeleteContext context) {
		Object bo = GraphitiUtils.getBusinessObject(context.getPictogramElement());
		if (!(bo instanceof UIFileActionConnection)) {
			return;
		}
		UIFileActionConnection connection = (UIFileActionConnection)bo;
		
		/* create an undo/redo operation to set the slot value back to null */
		ActionChangeOperation op = new ActionChangeOperation("Delete Connection", connection.getActionId());
		op.recordSlotChange(connection.getSlotId(), connection.getFileGroupId(), null);
		op.recordAndInvoke();
	}

	/*-------------------------------------------------------------------------------------*/
}
