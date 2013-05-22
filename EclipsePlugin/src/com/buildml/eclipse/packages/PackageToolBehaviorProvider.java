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

package com.buildml.eclipse.packages;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.context.IPictogramElementContext;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.tb.DefaultToolBehaviorProvider;
import org.eclipse.graphiti.tb.IContextButtonPadData;

import com.buildml.eclipse.bobj.UIAction;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.utils.print.PrintUtils;

/**
 * A "provider" class that implements diagram-viewing/editing behaviour for the BuildML
 * "package diagram". This includes such things as context menus, double-click behaviour
 * and tool tips.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class PackageToolBehaviorProvider extends DefaultToolBehaviorProvider {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The BuildStore associated with this diagram */
	private IBuildStore buildStore;
	
	/** The IActionMgr associated with this BuildStore */
	private IActionMgr actionMgr;
	
	/** The maximum number of characters wide that a tooltip should be */
	private final int toolTipWrapWidth = 120;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new PackageToolBehaviorProvider.
	 * @param dtp The diagram type provider that owns this provider.
	 */
    public PackageToolBehaviorProvider(IDiagramTypeProvider dtp) {
        super(dtp);
        
        /* figure out the IBuildStore associated with this editor */
        PackageDiagramEditor pde = (PackageDiagramEditor)dtp.getDiagramEditor();
        buildStore = pde.getBuildStore();
    	actionMgr = buildStore.getActionMgr();
    }
    
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

    /**
     * For a specific business object on the diagram, compute the tool-tip that the user
     * will see if they hover their mouse for long enough. This is only computed once for
     * each business object. The method used to determine the tool-tip is dependent on
     * object type. 
     */
    @Override
    public String getToolTip(GraphicsAlgorithm ga) {

    	/* Determine the business object that we're computing the tool-tip for */
    	PictogramElement pe = ga.getPictogramElement();
        Object bo = getFeatureProvider().getBusinessObjectForPictogramElement(pe);
        
        /*
         * For actions (UIAction), fetch the command string from the BuildStore.
         */
        if (bo instanceof UIAction) {
        	UIAction action = (UIAction)bo;
        	int actionId = action.getId();
        	String actionString = actionMgr.getCommand(actionId);

        	/* format the command string nicely, wrapping the command if it's long */
			if (actionString != null) {
				ByteArrayOutputStream outStream = new ByteArrayOutputStream();
				PrintStream printStream = new PrintStream(outStream);
				PrintUtils.indentAndWrap(printStream, actionString, 0, toolTipWrapWidth);
				String toolTip = outStream.toString();
				printStream.close();
				return toolTip;
			}
        }
        
        /* else, return the default tooltip */
    	return super.getToolTip(ga);
    }

    /*-------------------------------------------------------------------------------------*/

    /**
     * We don't want to show the fly-in palette, since the user can't use it anyway.
     */
    @Override
    public boolean isShowFlyoutPalette() {
    	return false;
    }

    /*-------------------------------------------------------------------------------------*/

    /**
     * Determine which context buttons are displayed around the UI icons.
     */
    @Override
    public IContextButtonPadData getContextButtonPad(
    		IPictogramElementContext context) {
    	
    	IContextButtonPadData data = super.getContextButtonPad(context);
        PictogramElement pe = context.getPictogramElement();
     
        /* for now, disable all context buttons */
        setGenericContextButtons(data, pe, 0);
        
        return data;
    }
    
    /*-------------------------------------------------------------------------------------*/
}
