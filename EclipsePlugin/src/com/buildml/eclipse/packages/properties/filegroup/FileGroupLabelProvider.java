/*******************************************************************************
 * Copyright (c) 2013 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.packages.properties.filegroup;

import org.eclipse.jface.viewers.LabelProvider;

import com.buildml.model.IBuildStore;
import com.buildml.model.IFileGroupMgr;
import com.buildml.model.IFileMgr;

/**
 * A "label provider" class that feeds the TreeViewer in {@link FileGroupContentPropertyPage}
 * with the text labels for members of file groups.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class FileGroupLabelProvider extends LabelProvider {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/
	
	/** The IFileMgr we retrieve label information from */
	private IFileMgr fileMgr;
	
	/** The type of this file group (e.g. SOURCE_GROUP, MERGE_GROUP, etc). */
	private int fileGroupType;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link FileGroupLabelProvider} object.
	 * 
	 * @param buildStore The IBuildStore to retrieve content information from.
	 * @param fileGroupType The type of this file group (e.g. SOURCE_GROUP, MERGE_GROUP, etc).
	 */
	public FileGroupLabelProvider(IBuildStore buildStore, int fileGroupType) {
		this.fileMgr = buildStore.getFileMgr();
		this.fileGroupType = fileGroupType;
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		
		/* 
		 * for source groups, the objects are path IDs. Return the full path name with
		 * path roots (e.g. @root/etc/passwd)
		 */
		if ((fileGroupType == IFileGroupMgr.SOURCE_GROUP) && (element instanceof Integer)) {
			Integer pathId = (Integer)element;
			return fileMgr.getPathName(pathId, true);
		}
		
		/* for file groups, we have Integer (top level), and String (second level) */
		else if (fileGroupType == IFileGroupMgr.MERGE_GROUP) {
			if (element instanceof Integer) {
				return "Sub Group ";
			}
			
			else if (element instanceof String) {
				return (String)element;
			}
		}
		
		return "";
	}
	
	/*-------------------------------------------------------------------------------------*/

}
