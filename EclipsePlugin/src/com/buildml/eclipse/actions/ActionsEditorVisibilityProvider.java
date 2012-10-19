/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
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

import com.buildml.eclipse.utils.IVisibilityProvider;
import com.buildml.model.types.ActionRecord;
import com.buildml.model.types.ActionSet;

/**
 * An adapter class to allow a ActionSet to be used as the visibility provider for
 * a VisibilityTreeViewer control.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class ActionsEditorVisibilityProvider implements IVisibilityProvider {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/**
	 * The ActionSet containing the visible/non-visible state of each path. If the path
	 * is in this set, it's visible, else it's non-visible.
	 */
	private ActionSet primaryFilterSet;
	
	/**
	 * A secondary filterSet which can be applied in conjunction with the first. Note that
	 * this filter set is provided as-is and won't be modified by the setVisibility() method.
	 * It's use is to further filter out elements after the primary filter set (filterSet)
	 * has been applied.
	 */
	private ActionSet secondaryFilterSet;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new ActionsEditorVisibilityProvider, which can be used in conjunction with
	 * a VisibilityTreeViewer to determine which elements are visible, versus which are
	 * non-visible (or greyed out).
	 * 
	 * @param filterSet The ActionSet specifying the visibility state of each path.
	 */
	public ActionsEditorVisibilityProvider(ActionSet filterSet)
	{
		this.primaryFilterSet = filterSet;
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.utils.IVisibilityProvider#isVisible(java.lang.Object)
	 */
	@Override
	public boolean isVisible(Object element) {
		if (element instanceof ActionRecord) {
			ActionRecord actionRecord = (ActionRecord)element;
			int actionId = actionRecord.getId();
			
			/* is this file a member of both primaryFilterSet and secondaryFilterSet. */
			return (primaryFilterSet.isMember(actionId) && 
					((secondaryFilterSet == null) || secondaryFilterSet.isMember(actionId)));
		}
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.utils.IVisibilityProvider#setVisibility(java.lang.Object, boolean)
	 */
	@Override
	public void setVisibility(Object element, boolean visible) {
		if (element instanceof ActionRecord) {
			ActionRecord actionRecord = (ActionRecord)element;
			if (visible) {
				primaryFilterSet.addSubTree(actionRecord.getId());
			} else {
				primaryFilterSet.removeSubTree(actionRecord.getId());
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set the primary FilterSet that determines which tree elements we want to
	 * display.
	 * @param filter The new primary filter set.
	 */
	public void setPrimaryFilterSet(ActionSet filter) {
		primaryFilterSet = filter;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The primary filter set.
	 */
	public ActionSet getPrimaryFilterSet() {
		return primaryFilterSet;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set the secondary FileSet that determine which packages we want to
	 * display.
	 * @param filter The new secondary filter set.
	 */
	public void setSecondaryFilterSet(ActionSet filter) {
		secondaryFilterSet = filter;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The secondary filter set.
	 */
	public ActionSet getSecondaryFilterSet() {
		return secondaryFilterSet;
	}
	
	/*-------------------------------------------------------------------------------------*/

}
