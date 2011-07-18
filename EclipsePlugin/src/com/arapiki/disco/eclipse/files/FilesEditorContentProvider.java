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

import java.util.ArrayList;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;

import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.FileRecord;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class FilesEditorContentProvider extends ArrayContentProvider
	implements ITreeContentProvider {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The FileNameSpaces object we should query for file information */
	private FileNameSpaces fns = null;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link FilesEditorContentProvider} that translates the information
	 * in the BuildStore into something that a TreeViewer can understand.
	 */
	public FilesEditorContentProvider(FileNameSpaces fns) {
	
		/* save away the FileNameSpaces, so we can query it for file information */
		this.fns = fns;
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		
		/* We only care about FileRecord elements */
		if (parentElement instanceof FileRecord) {
			
			/* Fetch the children IDs from the BuildStore */
			FileRecord fr = (FileRecord)parentElement;
			Integer childIds[] = fns.getChildPaths(fr.getId());
			
			/* Convert this into a FileRecord[] */
			// TODO: do this in a better way.
			ArrayList<FileRecord> array = new ArrayList<FileRecord>();
			for (int i = 0; i < childIds.length; i++) {
				array.add(new FileRecord(childIds[i]));
			}
			return array.toArray();
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {
		System.out.println("Called getParent - not yet implemented");
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		
		/* we only care about FileRecord element types */
		if (element instanceof FileRecord) {
			FileRecord fr = (FileRecord)element;
			
			/* query the BuildStore to see if there are any children */
			Integer childIds[] = fns.getChildPaths(fr.getId());
			if (childIds.length > 0) {
				return true;
			}
		}
		
		/* else, we have no children */
		return false;
	}

	/*-------------------------------------------------------------------------------------*/
}
