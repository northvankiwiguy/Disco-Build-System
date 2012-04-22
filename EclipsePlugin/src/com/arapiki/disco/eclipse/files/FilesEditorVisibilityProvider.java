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

package com.arapiki.disco.eclipse.files;

import com.arapiki.disco.eclipse.utils.IVisibilityProvider;
import com.arapiki.disco.model.types.FileRecord;
import com.arapiki.disco.model.types.FileSet;

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
	private FileSet filterSet;

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
		this.filterSet = filterSet;
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.eclipse.utils.IVisibilityProvider#isVisible(java.lang.Object)
	 */
	@Override
	public boolean isVisible(Object element) {
		if (element instanceof FileRecord) {
			FileRecord fr = (FileRecord)element;
			return filterSet.isMember(fr.getId());
		}
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.eclipse.utils.IVisibilityProvider#setVisibility(java.lang.Object, boolean)
	 */
	@Override
	public void setVisibility(Object element, boolean visible) {
		if (element instanceof FileRecord) {
			FileRecord fr = (FileRecord)element;
			if (visible) {
				filterSet.addSubTree(fr.getId());
			} else {
				filterSet.removeSubTree(fr.getId());
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
