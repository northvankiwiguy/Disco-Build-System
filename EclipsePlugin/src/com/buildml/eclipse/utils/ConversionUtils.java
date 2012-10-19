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

package com.buildml.eclipse.utils;

import com.buildml.eclipse.actions.UIActionRecord;
import com.buildml.eclipse.files.UIFileRecordDir;
import com.buildml.eclipse.files.UIFileRecordFile;
import com.buildml.model.IActionMgr;
import com.buildml.model.IFileMgr;
import com.buildml.model.types.FileRecord;

/**
 * A collection of static methods implementing type conversions, as required
 * by the Eclipse GUI.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class ConversionUtils {
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Given a path ID number, return a suitable subclass of FileRecord that reflects
	 * the type of the path. For example, return a UIFileRecordFile() if the pathId refers
	 * to a file, or a UIFileRecordDir() if it refers to a directory.
	 * @param fileMgr The FileMgr object that this path belongs to
	 * @param pathId The ID of the path we need to create a FileRecord for.
	 * @return The new object, whose class is a sub-class of FileRecord.
	 */
	public static FileRecord createFileRecordWithType(IFileMgr fileMgr, int pathId)
	{
		switch (fileMgr.getPathType(pathId)) {
		case TYPE_DIR:
			return new UIFileRecordDir(pathId);
		case TYPE_FILE:
			return new UIFileRecordFile(pathId);
		case TYPE_INVALID:
		case TYPE_SYMLINK:
			// TODO: handle this error somehow
		}	
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given an Integer[], return a corresponding FileRecord[]
	 * @param fileMgr The FileMgr object that this path belongs to.
	 * @param intArr The Integer[] to be converted.
	 * @return The equivalent FileRecord[].
	 */
	public static FileRecord[] convertIntArrToFileRecordArr(
			IFileMgr fileMgr,
			Integer[] intArr)
	{
		FileRecord result[] = new FileRecord[intArr.length];
		for (int i = 0; i < intArr.length; i++) {
			result[i] = createFileRecordWithType(fileMgr, intArr[i]);
		}
		return result;		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given an Integer[], return a corresponding ActionRecord[]
	 * @param actionMgr The ActionMgr object that these actions belong to.
	 * @param intArr The Integer[] to be converted.
	 * @return The equivalent UIActionRecord[].
	 */
	public static UIActionRecord[] convertIntArrToActionRecordArr(
			IActionMgr actionMgr, Integer[] intArr) {
		
		UIActionRecord result[] = new UIActionRecord[intArr.length];
		for (int i = 0; i < intArr.length; i++) {
			result[i] = new UIActionRecord(intArr[i]);
		}
		return result;
	}
	
	/*-------------------------------------------------------------------------------------*/
}

