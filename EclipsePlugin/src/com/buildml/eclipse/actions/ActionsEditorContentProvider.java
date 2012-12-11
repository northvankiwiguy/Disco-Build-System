/*******************************************************************************
 * Copyright (c) 2011 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.actions;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;

import com.buildml.eclipse.ISubEditor;
import com.buildml.eclipse.bobj.UIAction;
import com.buildml.eclipse.utils.ConversionUtils;
import com.buildml.model.IActionMgr;

/**
 * Content Provider for the TreeViewer used as the main viewer in the ActionsEditor.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class ActionsEditorContentProvider extends ArrayContentProvider
	implements ITreeContentProvider {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The ActionsEditor that we provide content for */
	private ISubEditor editor = null;
	
	/** The Action Manager object we should query for file information */
	private IActionMgr actionMgr = null;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new ActionsEditorContentProvider that translates the information
	 * in the BuildStore into something that a TreeViewer can understand.
	 * @param editor The editor that this content provider is associated with.
	 * @param actionMgr The ActionMgr object that we're displaying information from.
	 */
	public ActionsEditorContentProvider(ISubEditor editor, IActionMgr actionMgr) {

		this.editor = editor;
		this.actionMgr = actionMgr;
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		
		/* We only care about UIAction elements */
		if (parentElement instanceof UIAction) {
			
			Integer childIds[];
			UIAction uiAction = (UIAction)parentElement;
			int actionId = uiAction.getId();
				
			/* Fetch the children IDs from the BuildStore */
			childIds = actionMgr.getChildren(actionId);
			
			/* Convert our child list from an Integer[] to a UIInteger[] */
			return ConversionUtils.convertIntArrToUIActionArr(actionMgr, childIds);
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {

		if (element instanceof UIAction) {
			
			/* query the BuildStore for this element's parent */
			UIAction actElement = (UIAction)element;
			int parentId = actionMgr.getParent(actElement.getId());

			/* base case - parent of top-level action is null */
			if (parentId == actElement.getId()) {
				return null;
			}
						
			/* if there's an error, inform the caller that we can't find the parent */
			if (parentId < 0) {
				return null;
			}
			
			/* construct a new UIAction object */
			return new UIAction(parentId);
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		
		/* we only care about UIAction element types */
		if (element instanceof UIAction) {
			UIAction act = (UIAction)element;
			
			/* query the BuildStore to see if there are any children */
			Integer childIds[] = actionMgr.getChildren(act.getId());
			if (childIds.length > 0) {
				return true;
			}
		}
		
		/* else, we have no children */
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the top-level elements, which represent the starting point for displaying
	 * the tree.
	 * @return The list of UIAction elements
	 */
	public UIAction[] getRootElements() {
		
		int topRootId = actionMgr.getRootAction("root");
		Integer childIds[] = actionMgr.getChildren(topRootId);
		return ConversionUtils.convertIntArrToUIActionArr(actionMgr, childIds);
	}

	/*-------------------------------------------------------------------------------------*/
}
