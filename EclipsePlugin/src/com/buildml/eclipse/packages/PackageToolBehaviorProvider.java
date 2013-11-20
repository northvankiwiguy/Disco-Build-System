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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.context.IDoubleClickContext;
import org.eclipse.graphiti.features.context.IPictogramElementContext;
import org.eclipse.graphiti.features.custom.ICustomFeature;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.tb.DefaultToolBehaviorProvider;
import org.eclipse.graphiti.tb.IContextButtonPadData;

import com.buildml.eclipse.bobj.UIAction;
import com.buildml.eclipse.bobj.UIFileActionConnection;
import com.buildml.eclipse.bobj.UIFileGroup;
import com.buildml.eclipse.bobj.UIMergeFileGroupConnection;
import com.buildml.eclipse.packages.features.PackageDiagramDoubleClickFeature;
import com.buildml.model.IActionMgr;
import com.buildml.model.IActionTypeMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileGroupMgr;
import com.buildml.model.ISlotTypes.SlotDetails;
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

	/** The IActionTypeMgr associated with this BuildStore */
	private IActionTypeMgr actionTypeMgr;

	/** The IFileGroupMgr associated with this BuildStore */
	private IFileGroupMgr fileGroupMgr;

	/** The maximum number of characters wide that a tooltip should be */
	private final int toolTipWrapWidth = 120;
	
	/** The maximum number of file group members we should show in the tool-tip */
	private final int toolTipMaxFileGroupLines = 20;
	
	/** The custom graphiti feature for handling double-clicks */
 	private ICustomFeature doubleClickFeature = null;
	
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
    	actionTypeMgr = buildStore.getActionTypeMgr();
    	fileGroupMgr = buildStore.getFileGroupMgr();
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
        
        /*
         * For file groups, show the content of the file group.
         */
        else if (bo instanceof UIFileGroup) {
        	UIFileGroup fileGroup = (UIFileGroup)bo;
        	int fileGroupId = fileGroup.getId();
        	
        	StringBuffer sb = new StringBuffer();
        	displayFileGroupMembers(fileGroupId, sb);
        	return sb.toString();
        }
        
        /*
         * For UIFileActionConnection, we can be going into, or going out of an action.
         */
        else if (bo instanceof UIFileActionConnection) {
        	UIFileActionConnection connection = (UIFileActionConnection)bo;
        	
    	    SlotDetails details = actionTypeMgr.getSlotByID(connection.getSlotId());
    	    if (details == null) {
    	    	return null;
    	    }
    	    
    	    /* for output connections, just show the slot we come out of */
        	if (connection.getDirection() == UIFileActionConnection.OUTPUT_FROM_ACTION) {
        		return "\n Output from slot \"" + details.slotName + "\" \n";
        	}
        	
        	/* for input connections, show the slot and the (optional) filter group content */
        	else {
        		String str = "\n Input to slot \"" + details.slotName + "\" \n";
        		if (connection.hasFilter()) {
	        		// TODO: show output of filter file set.
        		}
        		return str;
        	}
        }
        
        /*
         * for UIMergeFileGroupConnection, show the sub group's 1-based position inside the
         * merge group. If there's a filter on the connection, also show the output of
         * the filter group. 
         */
        else if (bo instanceof UIMergeFileGroupConnection) {
        	UIMergeFileGroupConnection connection = (UIMergeFileGroupConnection)bo;
        	int subGroupId = connection.getSourceFileGroupId();
        	int mergeGroupId = connection.getTargetFileGroupId();
        	
        	/* find the index (or indicies) that the sub group appears in the merge group */
        	Integer subGroups[] = fileGroupMgr.getSubGroups(mergeGroupId);
        	if (subGroups == null) {
        		return null;
        	}
        	List<Integer> indexList = new ArrayList<Integer>();
        	for (int i = 0; i < subGroups.length; i++) {
				if (subGroups[i] == subGroupId) {
					indexList.add(i + 1);
				}
			}
			
        	StringBuffer sb = new StringBuffer();
        	sb.append("\n File group merged into position");
        	if (indexList.size() > 1) {
        		sb.append('s');
        	}
        	int size = indexList.size();
        	for (int i = 0; i < size; i++) {
        		if (i == 0) {
        			sb.append(' ');
        		}
        		else if (i == (size - 1)) {
        			sb.append(" and ");
        		}
        		else {
        			sb.append(", ");
        		}
    			sb.append(indexList.get(i));
			}
        	sb.append(" \n");
        	return sb.toString();
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

    /**
     * Handle double-clicking on a UI icon.
     */
    @Override
    public ICustomFeature getDoubleClickFeature(IDoubleClickContext context) {
    	
    	/* if we don't already have a custom feature for double-clicking, create one */
    	if (doubleClickFeature == null) {
    		doubleClickFeature = 
    				new PackageDiagramDoubleClickFeature(getFeatureProvider(), buildStore);
    	}

    	/* can our custom feature handle this event? */
    	if (doubleClickFeature.canExecute(context)) {
    		return doubleClickFeature;
    	}
    	
    	/* else, default to standard handler */
        return super.getDoubleClickFeature(context);    	
    }
    
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Helper function for appending the list of a file group's members onto a StringBuffer.
	 * 
	 * @param fileGroupId	ID of the file group to display the content of.
	 * @param stringBuffer	The StringBuffer to append the member names to.
	 */
	private void displayFileGroupMembers(int fileGroupId, StringBuffer stringBuffer) {
		String files[] = fileGroupMgr.getExpandedGroupFiles(fileGroupId);
		
		int fileGroupType = fileGroupMgr.getGroupType(fileGroupId);
		String typeName = (fileGroupType == IFileGroupMgr.SOURCE_GROUP) ? "Source" :
							((fileGroupType == IFileGroupMgr.GENERATED_GROUP) ? "Generated" : 
								((fileGroupType == IFileGroupMgr.MERGE_GROUP) ? "Merge" : "Filter"));
		
		stringBuffer.append("\n ");
		stringBuffer.append(typeName);
		stringBuffer.append(" File Group: \n");
		
		int halfMaxLines = toolTipMaxFileGroupLines / 2;
		
		for (int i = 0; i < files.length; i++) {
			if ((i < halfMaxLines) || (i >= (files.length - halfMaxLines))) {
				stringBuffer.append(' ');
				stringBuffer.append(files[i]);
				stringBuffer.append(" \n");
			}
			
			else if (i == halfMaxLines) {
				stringBuffer.append("    ...\n");
			}
		}
	}
	
    /*-------------------------------------------------------------------------------------*/
}
