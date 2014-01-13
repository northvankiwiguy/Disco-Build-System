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

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.xml.type.internal.RegEx;

import com.buildml.eclipse.bobj.UIAction;
import com.buildml.eclipse.bobj.UIDirectory;
import com.buildml.eclipse.bobj.UIFile;
import com.buildml.eclipse.bobj.UIInteger;
import com.buildml.model.IActionMgr;
import com.buildml.model.IFileMgr;
import com.buildml.utils.errors.ErrorCode;

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
	 * Given a path ID number, return a suitable subclass of UIInteger that reflects
	 * the type of the path. For example, return a UIFile() if the pathId refers
	 * to a file, or a UIDirectory() if it refers to a directory.
	 * @param fileMgr The FileMgr object that this path belongs to
	 * @param pathId The ID of the path we need to create a UIInteger for.
	 * @return The new object, whose class is a sub-class of UIInteger.
	 */
	public static UIInteger createUIIntegerWithType(IFileMgr fileMgr, int pathId)
	{
		switch (fileMgr.getPathType(pathId)) {
		case TYPE_DIR:
			return new UIDirectory(pathId);
		case TYPE_FILE:
			return new UIFile(pathId);
		case TYPE_INVALID:
		case TYPE_SYMLINK:
			// TODO: handle this error somehow
		}	
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given an Integer[], return a corresponding UIInteger[]
	 * @param fileMgr The FileMgr object that this path belongs to.
	 * @param intArr The Integer[] to be converted.
	 * @return The equivalent UIInteger[].
	 */
	public static UIInteger[] convertIntArrToUIIntegerArr(
			IFileMgr fileMgr,
			Integer[] intArr)
	{
		UIInteger result[] = new UIInteger[intArr.length];
		for (int i = 0; i < intArr.length; i++) {
			result[i] = createUIIntegerWithType(fileMgr, intArr[i]);
		}
		return result;		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given an Integer[], return a corresponding UIAction[]
	 * @param actionMgr The ActionMgr object that these actions belong to.
	 * @param intArr The Integer[] to be converted.
	 * @return The equivalent UIAction[].
	 */
	public static UIAction[] convertIntArrToUIActionArr(
			IActionMgr actionMgr, Integer[] intArr) {
		
		UIAction result[] = new UIAction[intArr.length];
		for (int i = 0; i < intArr.length; i++) {
			result[i] = new UIAction(intArr[i]);
		}
		return result;
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Given an array of action ID numbers, return a pretty-printed string that can be
	 * used in dialog boxes.
	 * 
	 * @param actionMgr The ActionMgr that owns the actions.
	 * @param actionIds The array of actions to be displayed.
	 * @return A formatted string showing the command summary of the actions.
	 */
	public static String getActionsAsText(IActionMgr actionMgr, Integer[] actionIds) {
		StringBuffer sb = new StringBuffer();
		for (int actionId : actionIds) {
			sb.append(actionMgr.getSlotValue(actionId, IActionMgr.COMMAND_SLOT_ID));
			sb.append("\n\n");
		}
		return sb.toString();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given an array of path ID numbers, return a pretty-printed string that can be
	 * used in dialog boxes.
	 * 
	 * @param fileMgr The FileMgr that owns the paths.
	 * @param pathIds The array of paths to be displayed.
	 * @return A formatted string showing the absolute paths.
	 */
	public static String getPathsAsText(IFileMgr fileMgr, Integer[] pathIds) {
		StringBuffer sb = new StringBuffer();
		for (int pathId : pathIds) {
			sb.append(fileMgr.getPathName(pathId));
			sb.append('\n');
		}
		return sb.toString();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a URI of the form "buildml:pkgId#/0", return the pkgId portion as an integer.
	 * 
	 * @param uri The BuildML URI.
	 * @return The package ID as an integer, or NOT_FOUND if the URI format is invalid.
	 */
	public static int extractPkgIdFromURI(URI uri) {
		String str = uri.toString();
		
		/* check the prefix and suffix of the URI string */
		if (!str.startsWith("buildml:") || !str.endsWith("#/0")) {
			return ErrorCode.NOT_FOUND;
		}
		int endIndex = str.lastIndexOf('#');
		String subStr = str.substring("buildml:".length(), endIndex);

		/* Parse out and return the pkgId portion*/
		int pkgId;
		try {
			pkgId = Integer.valueOf(subStr);
		} catch (NumberFormatException ex) {
			return ErrorCode.NOT_FOUND;
		}
		return pkgId;
	}
	
	/*-------------------------------------------------------------------------------------*/
}

