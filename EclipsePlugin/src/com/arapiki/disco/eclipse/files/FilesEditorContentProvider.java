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

import com.arapiki.disco.eclipse.utils.ConversionUtils;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.FileNameSpaces.PathType;
import com.arapiki.disco.model.types.FileRecord;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class FilesEditorContentProvider extends ArrayContentProvider
	implements ITreeContentProvider {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The DiscoFilesEditor that we provide content for */
	private DiscoFilesEditor editor = null;
	
	/** The FileNameSpaces object we should query for file information */
	private FileNameSpaces fns = null;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link FilesEditorContentProvider} that translates the information
	 * in the BuildStore into something that a TreeViewer can understand.
	 * @param editor The editor that this content provider is associated with.
	 * @param fns The FileNameSpaces object that we're displaying information from.
	 */
	public FilesEditorContentProvider(DiscoFilesEditor editor, FileNameSpaces fns) {

		this.editor = editor;
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
			
			Integer childIds[];
			FileRecord fr = (FileRecord)parentElement;
			int pathId = fr.getId();
			
			/* repeat until we've found the children (or grandchildren) we want */
			while (true) {
				
				/* Fetch the children IDs from the BuildStore */
				childIds = fns.getChildPaths(pathId);
			
				/*
				 * If directory coalescing is enabled, we want to skip
				 * over single-child directories where the child is itself
				 * a directory.
				 */
				if (editor.isOptionSet(DiscoFilesEditor.OPT_COALESCE_DIRS)) {
					
					/* if there isn't a single child, we don't care - exit */
					if (childIds.length != 1) {
						break;
					}
					
					/* if the single child isn't a directory - exit */
					pathId = childIds[0];
					if (fns.getPathType(pathId) != PathType.TYPE_DIR) {
						break;
					}
					
					/* else, we loop around to compute the child of this single child */
				}
				
				/* no coalescing required */
				else {
					break;
				}
			}
			
			/* Convert our child list from an Integer[] to a FileRecord[] */
			return ConversionUtils.convertIntArrToFileRecordArr(childIds);
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

	/**
	 * Return the top-level elements, which represent the starting point for displaying
	 * the tree. Depending on the DiscoFilesEditor's mode, we may have a different set
	 * of root elements.
	 * @return The list of FileRecord elements
	 */
	public FileRecord[] getRootElements() {
		
		int topRootId = fns.getRootPath("root");
		if (editor.isOptionSet(DiscoFilesEditor.OPT_SHOW_ROOTS))
		{
			Integer childIds[] = fns.getChildPaths(topRootId);
			return ConversionUtils.convertIntArrToFileRecordArr(childIds);
		}
		
		/* else, the directories at the / level are the top-level elements */
		else {
			return new FileRecord[] { new FileRecord(topRootId) };			
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
