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

package com.arapiki.disco.main;

import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.FileNameSpaces;

/**
 *  A helper class for DiscoMain. This class handles the disco commands that report things
 * (trees, builds, etc). These methods should only be called by methods in DiscoMain.
 * @author "Peter Smith <psmith@arapiki.com>"
 */
/* package */ class DiscoReports {

	/*=====================================================================================*
	 * PACKAGE METHODS
	 *=====================================================================================*/

	/**
	 * Provide a list of all files in the BuildStore.
	 * @param buildStore The BuildStore to query.
	 */
	/* package */ static void reportAllFiles(BuildStore buildStore) {

		FileNameSpaces fns = buildStore.getFileNameSpaces();
		int rootPath = fns.getRootPath("root");
		
		StringBuffer pathSoFar = new StringBuffer();
		pathSoFar.append("${root}");
		reportAllFilesHelper(fns, pathSoFar, rootPath, 0);
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * Helper function for reportAllFiles that recurses across the whole FileNameSpaces tree.
	 * @param fns The FileNameSpaces to traverse
	 * @param pathSoFar Contains the file's path (will be added to by the recursion).
	 * @param pathId The start point of the recursion
	 * @param indent The number of spaces to indent each line
	 */
	private static void reportAllFilesHelper(FileNameSpaces fns, StringBuffer pathSoFar,
			int pathId, int indent) {

		/* Print out this path's name, including the necessary indent level */
		String cmptName = fns.getBaseName(pathId);
		
		// TODO: make indentation a configuration option
		//PrintUtils.indent(System.out, indent);
		System.out.print(pathSoFar);
		System.out.println(cmptName);
		
		/* now consider the children, if there are any */
		Integer children[] = fns.getChildPaths(pathId);
		if (children.length != 0) {
			int sbLen = pathSoFar.length();
			pathSoFar.append(cmptName);
			if (!cmptName.equals("/")){
				pathSoFar.append('/');
			}
			for (int i = 0; i < children.length; i++) {
				reportAllFilesHelper(fns, pathSoFar, children[i], indent + 2);
			}
			pathSoFar.setLength(sbLen);
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
