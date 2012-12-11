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

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import com.buildml.eclipse.bobj.UIInteger;
import com.buildml.model.IFileMgr;
import com.buildml.model.IFileMgr.PathType;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class FilesEditorViewerSorter extends ViewerSorter {

	/** The FilesEditor that we provide content for */
	private FilesEditor editor = null;
	
	/** The FileMgr object we should query for file information */
	private IFileMgr fileMgr = null;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create a new FilesEditorViewerSorter, which is designed for sorting elements
	 * within the FilesEditor page.
	 * @param editor The FilesEditor we're sorting information for.
	 * @param fileMgr The FileMgr object the information is derived from.
	 */
	public FilesEditorViewerSorter(FilesEditor editor, IFileMgr fileMgr) {
		
		this.editor = editor;
		this.fileMgr = fileMgr;
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, 
	 * 					java.lang.Object, java.lang.Object)
	 */
	public int compare(Viewer viewer, Object r1, Object r2) {
		
		/* Sort first by type (directories before files/symlinks), and then by name */
		UIInteger uiInt1 = (UIInteger)r1;
		UIInteger uiInt2 = (UIInteger)r2;
		PathType pt1 = fileMgr.getPathType(uiInt1.getId());
		PathType pt2 = fileMgr.getPathType(uiInt2.getId());
		
		/* if r1 is a directory, and r2 isn't, then r1 comes first */
		if ((pt1 == PathType.TYPE_DIR) && (pt2 != PathType.TYPE_DIR)) {
			return -1;
		}
		
		/* if r2 is a directory, and r1 isn't, then r2 comes first */
		if ((pt2 == PathType.TYPE_DIR) && (pt1 != PathType.TYPE_DIR)) {
			return 1;
		}
		
		/* else, compare their names */
		String name1 = fileMgr.getBaseName(uiInt1.getId());
		String name2 = fileMgr.getBaseName(uiInt2.getId());
		return name1.compareToIgnoreCase(name2);
	}
	
	/*-------------------------------------------------------------------------------------*/

}
