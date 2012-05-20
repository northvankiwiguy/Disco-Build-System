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

package com.buildml.eclipse.files;

import com.buildml.eclipse.utils.IVisibilityProvider;
import com.buildml.model.types.FileRecord;
import com.buildml.model.types.FileSet;

/**
 * An adapter class to allow a FileSet to be used as the visibility provider for
 * a VisibilityTreeViewer control.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class FilesEditorVisibilityProvider implements IVisibilityProvider {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/**
	 * The FileSet containing the visible/non-visible state of each path. If the path
	 * is in this set, it's visible, else it's non-visible.
	 */
	private FileSet primaryFilterSet;
	
	/**
	 * A secondary filterSet which can be applied in conjunction with the first. Note that
	 * this filter set is provided as-is and won't be modified by the setVisibility() method.
	 * It's use is to further filter out elements after the primary filter set (filterSet)
	 * has been applied.
	 */
	private FileSet secondaryFilterSet;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new FilesEditorVisibilityProvider, which can be used in conjunction with
	 * a VisibilityTreeViewer to determine which elements are visible, versus which are
	 * non-visible (or greyed out).
	 * 
	 * @param filterSet The FileSet specifying the visibility state of each path.
	 */
	public FilesEditorVisibilityProvider(FileSet filterSet)
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
		if (element instanceof FileRecord) {
			FileRecord fr = (FileRecord)element;
			int fileId = fr.getId();
			
			/* is this file a member of both primaryFilterSet and secondaryFilterSet. */
			return (primaryFilterSet.isMember(fileId) && 
					((secondaryFilterSet == null) || secondaryFilterSet.isMember(fileId)));
		}
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.utils.IVisibilityProvider#setVisibility(java.lang.Object, boolean)
	 */
	@Override
	public void setVisibility(Object element, boolean visible) {
		if (element instanceof FileRecord) {
			FileRecord fr = (FileRecord)element;
			if (visible) {
				primaryFilterSet.addSubTree(fr.getId());
			} else {
				primaryFilterSet.removeSubTree(fr.getId());
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set the primary FilterSet that determines which packages/scopes we want to
	 * display.
	 * @param filter The new primary filter set.
	 */
	public void setPrimaryFilterSet(FileSet filter) {
		primaryFilterSet = filter;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The primary filter set.
	 */
	public FileSet getPrimaryFilterSet() {
		return primaryFilterSet;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set the secondary FileSet that determine which packages/scopes we want to
	 * display.
	 * @param filter The new secondary filter set.
	 */
	public void setSecondaryFilterSet(FileSet filter) {
		secondaryFilterSet = filter;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The secondary filter set.
	 */
	public FileSet getSecondaryFilterSet() {
		return secondaryFilterSet;
	}
	
	/*-------------------------------------------------------------------------------------*/

}
