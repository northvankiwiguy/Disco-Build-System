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
import com.buildml.eclipse.bobj.UISubPackage;
import com.buildml.eclipse.packages.features.PackageDiagramDoubleClickFeature;
import com.buildml.model.IActionMgr;
import com.buildml.model.IActionTypeMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileGroupMgr;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMgr;
import com.buildml.model.ISlotTypes;
import com.buildml.model.ISlotTypes.SlotDetails;
import com.buildml.model.ISubPackageMgr;
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
	
	/** The IFileMgr associated with this BuildStore */
	private IFileMgr fileMgr;
	
	/** The ISubPackageMgr associated with this BuildStore */
	private ISubPackageMgr subPkgMgr;
	
	/** The IPackageMgr associated with this BuildStore */
	private IPackageMgr pkgMgr;

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
    	fileMgr = buildStore.getFileMgr();
    	subPkgMgr = buildStore.getSubPackageMgr();
    	pkgMgr = buildStore.getPackageMgr();
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
        	
        	/* fetch the action's command string, and directory path */
        	String actionString = (String) actionMgr.getSlotValue(actionId, IActionMgr.COMMAND_SLOT_ID);
        	Object dirSlotValue = actionMgr.getSlotValue(actionId, IActionMgr.DIRECTORY_SLOT_ID);
        	String dirString = null;
        	if (dirSlotValue != null) {
        		int dirId = (Integer)dirSlotValue;
        		dirString = fileMgr.getPathName(dirId);
        	}

        	/* format the command string nicely, wrapping the command if it's long */
			if (actionString != null) {
				ByteArrayOutputStream outStream = new ByteArrayOutputStream();
				PrintStream printStream = new PrintStream(outStream);
				if (dirString != null) {
					printStream.println("Directory:\n" + dirString);
				}
				printStream.println("\nShell command:");
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
        	
        	StringBuilder sb = new StringBuilder();
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
        		return "\n Output from action slot \"" + details.slotName + "\" \n";
        	}
        	
        	/* for input connections, show the slot and the (optional) filter group content */
        	else {
        		int filterGroupId = connection.getFilterGroupId();
            	StringBuilder sb = new StringBuilder();
            	sb.append("\n Input to action slot \"");
            	sb.append(details.slotName);
            	sb.append("\" \n");
        		if (connection.hasFilter()) {
        			displayFilterPatterns(filterGroupId, sb);
        			displayFileGroupMembers(filterGroupId, sb);
        		}
        		return sb.toString();
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
        	int myIndex = connection.getIndex() + 1;
        	
        	StringBuilder sb = new StringBuilder();

        	/* display this subgroup's various positions in the merge group */
        	displayMergeGroupIndicies(subGroupId, mergeGroupId, myIndex, sb);
        	
        	/* if there's a filter attached, so the patterns and resulting output */
    		if (connection.hasFilter()) {
    			int filterGroupId = connection.getFilterGroupId();
    			displayFilterPatterns(filterGroupId, sb);
    			displayFileGroupMembers(filterGroupId, sb);
    		}
        	return sb.toString();
        }
        
        /*
         * For UISubPackage, so the full name of the package type.
         */
        else if (bo instanceof UISubPackage) {
        	UISubPackage subPkg = (UISubPackage)bo;
        	int subPkgId = subPkg.getId();
        	
        	/* compute the sub-package's type */
        	int pkgTypeId = subPkgMgr.getSubPackageType(subPkgId);
        	if (pkgTypeId < 0) {
        		return null;
        	}
        	
        	/* display the type name (which is itself a package name) */
    		StringBuilder sb = new StringBuilder();
        	String pkgTypeName = pkgMgr.getName(pkgTypeId);
        	if (pkgTypeName != null) {
        		sb.append("\n Sub-Package: ");
        		sb.append(pkgTypeName);
        		sb.append(" \n\n");
        	}
        	
        	/* Display the parameter values for this sub-package */
        	SlotDetails slots[] = pkgMgr.getSlots(pkgTypeId, ISlotTypes.SLOT_POS_PARAMETER);
        	if (slots != null) {
        		for (SlotDetails details : slots) {
					Object value = subPkgMgr.getSlotValue(subPkgId, details.slotId);
					sb.append(" - ");
					sb.append(details.slotName);
					sb.append(": ");
					
					/* null values can't be displayed */
					if (value == null) {
						sb.append("<undefined>");
						
					/* directories/files should be expanded into their path */
					} else if ((details.slotType == ISlotTypes.SLOT_TYPE_DIRECTORY) ||
								(details.slotType == ISlotTypes.SLOT_TYPE_FILE)) {
						int pathId = (Integer)value;
						String pathName = fileMgr.getPathName(pathId);
						if (pathName == null) {
							sb.append("<invalid>");
						} else {
							sb.append(pathName);
						}
					} 
					
					/* other types can be displayed directly */
					else {
						sb.append(value.toString());
					}
					sb.append(" \n");
				}
        	}
        	
    		return sb.toString();
        }
        
        /* else, return the default tooltip */
    	return (String) super.getToolTip(ga);
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
	 * Helper function for appending the list of a file group's members onto a StringBuilder.
	 * 
	 * @param fileGroupId	ID of the file group to display the content of.
	 * @param stringBuffer	The StringBuilder to append the member names to.
	 */
	private void displayFileGroupMembers(int fileGroupId, StringBuilder stringBuffer) {
		String files[] = fileGroupMgr.getExpandedGroupFiles(fileGroupId);
		
		int fileGroupType = fileGroupMgr.getGroupType(fileGroupId);
		
		stringBuffer.append("\n ");
		if (fileGroupType == IFileGroupMgr.FILTER_GROUP) {
			stringBuffer.append("Files passing through filter: \n");
		} else {
			String typeName = (fileGroupType == IFileGroupMgr.SOURCE_GROUP) ? "Source" :
				((fileGroupType == IFileGroupMgr.GENERATED_GROUP) ? "Generated" : "Merge");
			stringBuffer.append(typeName);
			stringBuffer.append(" File Group: \n");
		}
		
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

	/**
	 * Helper function for displaying the patterns in a filter file group.
	 * @param filterGroupId		ID of the filter group to display the patterns from.
	 * @param sb				The StringBuilder to append output to.
	 */
	private void displayFilterPatterns(int filterGroupId, StringBuilder sb) {
		String filterStrings[] = fileGroupMgr.getPathStrings(filterGroupId);
		sb.append("\n Filter Patterns:\n");
		for (int i = 0; i < filterStrings.length; i++) {
			sb.append(" ");
			String pattern = filterStrings[i];
			String[] parts = pattern.split(":");
			if (parts.length != 2) {
				sb.append("Invalid:");
				sb.append(pattern);
			} else if (parts[0].equals("ia")) {
				sb.append("Include: ");
				sb.append(parts[1]);
			} else if (parts[0].equals("ea")) {
				sb.append("Exclude: ");
				sb.append(parts[1]);				
			} else {
				sb.append("Invalid Prefix:");
				sb.append(parts[0]);
			}
			sb.append(" \n");
		}

	}
	
    /*-------------------------------------------------------------------------------------*/
	
	/**
	 * Helper method for displaying a connection's index positions within a merge file group.
	 * Any particular sub group may appear in multiple places in the merge group, so we
	 * provide a meaningful list of all index positions.
	 * 
	 * @param subGroupId	The ID of the sub group at one end of the connection.
	 * @param mergeGroupId	The ID of the merge group at the other end.
	 * @param myIndex		The merge group's index for this connection.
	 * @param sb			The StringBuilder to append content onto.
	 */
	private void displayMergeGroupIndicies(
			int subGroupId, int mergeGroupId, int myIndex, StringBuilder sb) {
		
		/* 
		 * Find the index (or indicies) where the sub group appears in the merge group, skipping
		 * over filters if they exist.
		 */
		Integer subGroups[] = fileGroupMgr.getSubGroups(mergeGroupId);
		if (subGroups == null) {
			return;
		}
		List<Integer> indexList = new ArrayList<Integer>();
		for (int i = 0; i < subGroups.length; i++) {
			int thisSubGroupId = subGroups[i];
			
			/* skip over filters - important with multiple connections from the same sub group */
			int thisSubGroupType = fileGroupMgr.getGroupType(thisSubGroupId);
			if (thisSubGroupType == IFileGroupMgr.FILTER_GROUP) {
				thisSubGroupId = fileGroupMgr.getPredId(thisSubGroupId);
			}
			
			/* if our subgroup is in this index position, record */
			if (thisSubGroupId == subGroupId) {
				indexList.add(i + 1);
			}
		}
		
		/*
		 * Now pretty-print the message, providing a list of all indicies. Our connection's
		 * position is highlighted with "(this link)".
		 */
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
			int index = indexList.get(i); 
			sb.append(index);
			if ((size > 1) && (index == myIndex)) {
				sb.append(" (this link)");
			}
		}
		sb.append(" \n");
	}

    /*-------------------------------------------------------------------------------------*/
}
