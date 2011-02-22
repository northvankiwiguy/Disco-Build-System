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
	/* package */ static void showFiles(BuildStore buildStore, String cmdArgs[]) {

		FileNameSpaces fns = buildStore.getFileNameSpaces();
		int rootPath;
		
		/* 
		 * If the user provided a starting point on the traversal, use that, else
		 * use the tree's top root.
		 */
		if (cmdArgs.length == 2) {
			String rootPathName = cmdArgs[1];
			rootPath = fns.getPath(rootPathName);
			if (rootPath == -1) {
				System.err.println("Error: Invalid path " + rootPathName);
				System.exit(1);
			}
		} else {
			rootPath = fns.getRootPath("root");
		} 
		
		showFilesHelper(fns, rootPath, 0);
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
	private static void showFilesHelper(FileNameSpaces fns, int pathId, int indent) {
		
		System.out.println(fns.getPathName(pathId, true));
		Integer children[] = fns.getChildPaths(pathId);
		for (int i = 0; i < children.length; i++) {
			showFilesHelper(fns, children[i], indent + 2);
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
