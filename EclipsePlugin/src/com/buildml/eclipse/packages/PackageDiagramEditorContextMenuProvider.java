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
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.ui.editor.DiagramEditorContextMenuProvider;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.actions.ActionFactory;

/**
 * A provider class for generating the content of a PackageDiagram's context menu.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class PackageDiagramEditorContextMenuProvider extends
		DiagramEditorContextMenuProvider {

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link PackageDiagramEditorContextMenuProvider} object.
	 * @param viewer
	 * @param registry
	 * @param diagramTypeProvider
	 */
	public PackageDiagramEditorContextMenuProvider(EditPartViewer viewer,
			ActionRegistry registry, IDiagramTypeProvider diagramTypeProvider) {
		super(viewer, registry, diagramTypeProvider);
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
	}
	
	/*-------------------------------------------------------------------------------------*/
}
