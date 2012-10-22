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
import com.buildml.eclipse.utils.ConversionUtils;
import com.buildml.model.IFileMgr;
import com.buildml.model.IFileMgr.PathType;
import com.buildml.model.types.FileRecord;
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
		
		/* We only care about FileRecord elements */
		if (parentElement instanceof FileRecord) {
			
			Integer childIds[];
			FileRecord fr = (FileRecord)parentElement;
			int pathId = fr.getId();
			
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
			
			/* Convert our child list from an Integer[] to a FileRecord[] */
			return ConversionUtils.convertIntArrToFileRecordArr(fileMgr, childIds);
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {

		if (element instanceof FileRecord) {
			
			/* query the BuildStore for this element's parent */
			FileRecord frElement = (FileRecord)element;
			int parentId = fileMgr.getParentPath(frElement.getId());

			/* base case - parent of / is null */
			if (parentId == frElement.getId()) {
				return null;
			}
						
			/* if there's an error, inform the caller that we can't find the parent */
			if (parentId < 0) {
				return null;
			}
			
			/* construct a new FileRecord (which must be a directory) */
			return new UIFileRecordDir(parentId);
		}
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
			Integer childIds[] = fileMgr.getChildPaths(fr.getId());
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
	 * @return The list of FileRecord elements
	 */
	public FileRecord[] getRootElements() {
		
		int topRootId = fileMgr.getRootPath("root");
		if (editor.isOptionSet(EditorOptions.OPT_SHOW_ROOTS))
		{
			String rootNames[] = fileMgr.getRoots();
			FileRecord fileRecords[] = new FileRecord[rootNames.length];
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
				
				/* create either a UIFileRecordFile, or a UIFileRecordDir object */
				fileRecords[i] = ConversionUtils.createFileRecordWithType(fileMgr, id);
			}
			return fileRecords;
		}
		
		/* else, the directories at the / level are the top-level elements */
		else {
			Integer childIds[] = fileMgr.getChildPaths(topRootId);
			return ConversionUtils.convertIntArrToFileRecordArr(fileMgr, childIds);
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
