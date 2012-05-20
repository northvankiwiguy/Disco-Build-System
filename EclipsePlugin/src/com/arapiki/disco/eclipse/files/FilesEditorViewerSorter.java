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

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import com.buildml.model.FileNameSpaces;
import com.buildml.model.FileNameSpaces.PathType;
import com.buildml.model.types.FileRecord;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class FilesEditorViewerSorter extends ViewerSorter {

	/** The DiscoFilesEditor that we provide content for */
	private DiscoFilesEditor editor = null;
	
	/** The FileNameSpaces object we should query for file information */
	private FileNameSpaces fns = null;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create a new FilesEditorViewerSorter, which is designed for sorting elements
	 * within the DiscoFilesEditor page.
	 * @param editor The DiscoFilesEditor we're sorting information for.
	 * @param fns The FileNameSpaces object the information is derived from.
	 */
	public FilesEditorViewerSorter(DiscoFilesEditor editor, FileNameSpaces fns) {
		
		this.editor = editor;
		this.fns = fns;
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, 
	 * 					java.lang.Object, java.lang.Object)
	 */
	public int compare(Viewer viewer, Object r1, Object r2) {
		
		/* Sort first by type (directories before files/symlinks), and then by name */
		FileRecord fr1 = (FileRecord)r1;
		FileRecord fr2 = (FileRecord)r2;
		PathType pt1 = fns.getPathType(fr1.getId());
		PathType pt2 = fns.getPathType(fr2.getId());
		
		/* if r1 is a directory, and r2 isn't, then r1 comes first */
		if ((pt1 == PathType.TYPE_DIR) && (pt2 != PathType.TYPE_DIR)) {
			return -1;
		}
		
		/* if r2 is a directory, and r1 isn't, then r2 comes first */
		if ((pt2 == PathType.TYPE_DIR) && (pt1 != PathType.TYPE_DIR)) {
			return 1;
		}
		
		/* else, compare their names */
		String name1 = fns.getBaseName(fr1.getId());
		String name2 = fns.getBaseName(fr2.getId());
		return name1.compareToIgnoreCase(name2);
	}
	
	/*-------------------------------------------------------------------------------------*/

}
