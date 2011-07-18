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

package com.arapiki.disco.eclipse.files;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.FileRecord;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class FilesEditorLabelProvider extends LabelProvider {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The FileNameSpaces object we'll use for querying file information from the BuildStore */
	private FileNameSpaces fns = null;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	public FilesEditorLabelProvider(FileNameSpaces fns) {
		this.fns = fns;
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {

		/* for FileRecords, we return the path's base name */
		if (element instanceof FileRecord) {
			FileRecord fr = (FileRecord)element;
			int pathId = fr.getId();
			String pathName = fns.getBaseName(pathId);
			return pathName;
		}
		return "<invalid>";
	}
	
	/*-------------------------------------------------------------------------------------*/
}
