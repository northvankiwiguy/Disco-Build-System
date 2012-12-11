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

package com.buildml.eclipse.files;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;

import com.buildml.eclipse.EditorOptions;
import com.buildml.eclipse.bobj.UIDirectory;
import com.buildml.eclipse.bobj.UIFile;
import com.buildml.eclipse.bobj.UIInteger;
import com.buildml.eclipse.utils.ConversionUtils;
import com.buildml.model.IFileMgr;
import com.buildml.model.IFileMgr.PathType;
import com.buildml.utils.errors.ErrorCode;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class FilesEditorContentProvider extends ArrayContentProvider
	implements ITreeContentProvider {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The FilesEditor that we provide content for */
	private FilesEditor editor = null;
	
	/** The FileMgr object we should query for file information */
	private IFileMgr fileMgr = null;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link FilesEditorContentProvider} that translates the information
	 * in the BuildStore into something that a TreeViewer can understand.
	 * @param editor The editor that this content provider is associated with.
	 * @param fileMgr The FileMgr object that we're displaying information from.
	 */
	public FilesEditorContentProvider(FilesEditor editor, IFileMgr fileMgr) {

		this.editor = editor;
		this.fileMgr = fileMgr;
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		
		/* We only care about UIFile or UIDirectory elements */
		if ((parentElement instanceof UIFile) || (parentElement instanceof UIDirectory)) {
			
			Integer childIds[];
			UIInteger uiInt = (UIInteger)parentElement;
			int pathId = uiInt.getId();
			
			/* repeat until we've found the children (or grandchildren) we want */
			while (true) {
				
				/* Fetch the children IDs from the BuildStore */
				childIds = fileMgr.getChildPaths(pathId);
			
				/*
				 * If directory coalescing is enabled, we want to skip
				 * over single-child directories where the child is itself
				 * a directory.
				 */
				if (editor.isOptionSet(EditorOptions.OPT_COALESCE_DIRS)) {
					
					/* if there isn't a single child, we don't care - exit */
					if (childIds.length != 1) {
						break;
					}
					
					/* if the single child isn't a directory - exit */
					pathId = childIds[0];
					if (fileMgr.getPathType(pathId) != PathType.TYPE_DIR) {
						break;
					}
					
					/* else, we loop around to compute the child of this single child */
				}
				
				/* no coalescing required */
				else {
					break;
				}
			}
			
			/* Convert our child list from an Integer[] to a UIInteger[] */
			return ConversionUtils.convertIntArrToUIIntegerArr(fileMgr, childIds);
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {

		if ((element instanceof UIFile) || (element instanceof UIDirectory)) {
			
			/* query the BuildStore for this element's parent */
			UIInteger uiInt = (UIInteger)element;
			int parentId = fileMgr.getParentPath(uiInt.getId());

			/* base case - parent of / is null */
			if (parentId == uiInt.getId()) {
				return null;
			}
						
			/* if there's an error, inform the caller that we can't find the parent */
			if (parentId < 0) {
				return null;
			}
			
			/* construct a new UIDirectory (which must be a directory) */
			return new UIDirectory(parentId);
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		
		/* we only care about UIDirectory element types */
		if (element instanceof UIDirectory) {
			UIInteger uiInt = (UIInteger)element;
			
			/* query the BuildStore to see if there are any children */
			Integer childIds[] = fileMgr.getChildPaths(uiInt.getId());
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
	 * the tree. Depending on the FilesEditor's mode, we may have a different set
	 * of root elements.
	 * @return The list of UIInteger elements
	 */
	public UIInteger[] getRootElements() {
		
		int topRootId = fileMgr.getRootPath("root");
		if (editor.isOptionSet(EditorOptions.OPT_SHOW_ROOTS))
		{
			String rootNames[] = fileMgr.getRoots();
			UIInteger uiIntegers[] = new UIInteger[rootNames.length];
			for (int i = 0; i < rootNames.length; i++) {
				int id = fileMgr.getRootPath(rootNames[i]);
				
				/* 
				 * if the name is missing, it's an internal error, but just
				 * return "root:" instead.
				 * TODO: throw an internal error.
				 */
				if (id == ErrorCode.NOT_FOUND) {
					id = topRootId;
				}
				
				/* create either a UIFile, or a UIDirectory object */
				uiIntegers[i] = ConversionUtils.createUIIntegerWithType(fileMgr, id);
			}
			return uiIntegers;
		}
		
		/* else, the directories at the / level are the top-level elements */
		else {
			Integer childIds[] = fileMgr.getChildPaths(topRootId);
			return ConversionUtils.convertIntArrToUIIntegerArr(fileMgr, childIds);
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
