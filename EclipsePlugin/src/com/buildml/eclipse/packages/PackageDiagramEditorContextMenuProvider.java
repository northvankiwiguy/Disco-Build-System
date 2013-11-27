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

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.ui.editor.DiagramEditorContextMenuProvider;
import org.eclipse.graphiti.ui.platform.IConfigurationProvider;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.actions.ActionFactory;

/**
 * A provider class for generating the content of a PackageDiagram's context menu.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class PackageDiagramEditorContextMenuProvider extends
		DiagramEditorContextMenuProvider {

	/** the ZoomManager for this PackageDiagram */
	private ZoomManager zoomManager;
	
	/** Action for zooming in */
	private ZoomInAction zoomInAction;
	
	/** Action for zooming out */
	private ZoomOutAction zoomOutAction;

	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link PackageDiagramEditorContextMenuProvider} object.
	 * @param viewer
	 * @param registry
	 * @param configurationProvider
	 */
	public PackageDiagramEditorContextMenuProvider(EditPartViewer viewer,
			ActionRegistry registry, IConfigurationProvider configurationProvider) {
		super(viewer, registry, configurationProvider);		
		
		ScalableFreeformRootEditPart rootEditPart = (ScalableFreeformRootEditPart) viewer.getRootEditPart();
		zoomManager = rootEditPart.getZoomManager();
		zoomInAction = new ZoomInAction(zoomManager);
		zoomOutAction = new ZoomOutAction(zoomManager);
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Generate the content of the context menu. Note that additional menu items (from
	 * other plugins) will also be added to the menu.
	 * 
	 * @param manager the menu manager
	 */
	@Override
	public void buildContextMenu(IMenuManager manager) {
		
		GEFActionConstants.addStandardActionGroups(manager);
		addActionToMenuIfAvailable(manager, ActionFactory.DELETE.getId(), GEFActionConstants.GROUP_EDIT);
		addActionToMenu(manager, zoomInAction.getId(), GEFActionConstants.GROUP_VIEW);
		addActionToMenu(manager, zoomOutAction.getId(), GEFActionConstants.GROUP_VIEW);
	}
	
	/*-------------------------------------------------------------------------------------*/
}
