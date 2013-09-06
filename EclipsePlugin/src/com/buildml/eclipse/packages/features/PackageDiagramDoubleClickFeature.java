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

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.custom.AbstractCustomFeature;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.ui.handlers.IHandlerService;

import com.buildml.eclipse.bobj.UIAction;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.errors.FatalError;
import com.buildml.model.IBuildStore;

/**
 * A graphiti "custom feature" for handling the "double click" event. That is, when
 * the user double-clicks on one of the diagram's picture elements (icons), this code
 * is called into action.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class PackageDiagramDoubleClickFeature extends AbstractCustomFeature {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The IBuildStore that our associated diagram is visualizing */
	private IBuildStore buildStore;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Construct a new PackageDiagramDoubleClickFeature. There should only be one of these
	 * objects for each Graphiti diagram.
	 * 
	 * @param fp The feature provider for this Graphiti diagram.
	 * @param buildStore The IBuildStore that this diagram is visualizing.
	 */
	public PackageDiagramDoubleClickFeature(IFeatureProvider fp, IBuildStore buildStore) {
		super(fp);

		this.buildStore = buildStore;
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Determine whether the icon that the user double-clicked on is something that we
	 * want to handle (versus ignoring).
	 */
	@Override
	public boolean canExecute(ICustomContext context) {
		
		/* we only support double-clicking on a single icon */
		PictogramElement pe[] = context.getPictogramElements();
		if (pe.length != 1) {
			return false;
		}
        Object bo = getFeatureProvider().getBusinessObjectForPictogramElement(pe[0]);
		
        /* we support UIAction, but nothing else (yet) */
        return (bo instanceof UIAction);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * The double-click has happened, and we know it's on an icon that we handle. Perform
	 * the resulting action.
	 */
	@Override
	public void execute(ICustomContext context) {
		
		/* determine the business object of the icon that was clicked on */
		PictogramElement pe[] = context.getPictogramElements();
        Object bo = getFeatureProvider().getBusinessObjectForPictogramElement(pe[0]);
		
        /*
         * For UIAction, we open a property editor that allows the user to change the
         * action's properties. If anything changed (they pressed "OK"), we then update the
         * diagram to reflect any graphical changes.
         */
        if (bo instanceof UIAction) {

        	/* Open the standard "properties" dialog */
        	String commandId = "org.eclipse.ui.file.properties";
        	IHandlerService handlerService = (IHandlerService) 
        			EclipsePartUtils.getService(IHandlerService.class);
        	try {
				handlerService.executeCommand(commandId, null);
			} catch (Exception e) {
				throw new FatalError("Unable to open Properties Dialog.");
			}
        }
	}

	/*-------------------------------------------------------------------------------------*/

}
