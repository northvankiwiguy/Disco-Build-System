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
import com.arapiki.utils.errors.ErrorCode;

/**
 * A helper class for DiscoMain. This class handles the disco commands that manipulate
 * various attributes of the BuildStore. These methods should only be called by methods in DiscoMain.
 * @author "Peter Smith <psmith@arapiki.com>"
 */
/* package */ class DiscoAttributes {

	/**
	 * Show the FileNameSpaces "roots" in this BuildStore. If a root name is provided,
	 * only show the details for that specific root.
	 * @param buildStore The BuildStore to query.
	 * @param cmdArgs The (optional) name of a root to show details of.
	 */
	/* package */ static void showRoot(BuildStore buildStore, String[] cmdArgs) {
		FileNameSpaces fns = buildStore.getFileNameSpaces();
		
		/* no arguments == display all roots */
		if (cmdArgs.length == 1) {
			String [] roots = fns.getRoots();
			for (int i = 0; i < roots.length; i++) {
				String rootName = roots[i];
				String associatedPath = fns.getPathName(fns.getRootPath(rootName));
				System.out.println(rootName + " " + associatedPath);
			}
		}
		
		/* else, one arg == show the path for this specific root */
		else {
			String rootName = cmdArgs[1];
			String associatedPath = fns.getPathName(fns.getRootPath(rootName));
			System.out.println(associatedPath);			
		}
	}

	/**
	 * Assign a root to a specific path within the FileNameSpaces. Simply
	 * calls the addNewRoot() or moveRootToPath() methods, but adds a lot of 
	 * error checking and user-friendliness.
	 * @param buildStore The BuildStore to query.
	 * @param cmdArgs The name of a root (cmdArg[1]) and file system path (cmdArgs[2]).
	 */
	/* package */ static void setRoot(BuildStore buildStore, String[] cmdArgs) {
		FileNameSpaces fns = buildStore.getFileNameSpaces();
		String rootName = cmdArgs[1];
		String pathName = cmdArgs[2];

		int pathId = fns.getPath(pathName);

		/* 
		 * There are two approaches here - if the root exists already, move it.
		 */
		int rc;
		if (fns.getRootPath(rootName) != ErrorCode.NOT_FOUND){
			rc = fns.moveRootToPath(rootName, pathId);
		}
		
		/* else, create a new root */
		else {
			rc = fns.addNewRoot(rootName, pathId);
		}
		
		/* Do we need to report an error? */
		if (rc != ErrorCode.OK) {
			
			String msg = null;
			
			switch (rc) {
			case ErrorCode.NOT_A_DIRECTORY:
				msg = pathName + " is not a directory";
				break;
				
			case ErrorCode.INVALID_NAME:
				msg = rootName + " is not a valid root name";
				break;

			case ErrorCode.ONLY_ONE_ALLOWED:
				msg = pathName + " already has a root associated with it";
				break;
				
			case ErrorCode.BAD_PATH:
				msg = pathName + " is an invalid path";
				break;
			}
			
			System.out.println("Error setting root. " + msg);
			System.exit(1);
		}
		
	}

	/**
	 * Remove a root from the FileNameSpaces. Wraps the deleteRoot() with a lot
	 * of error checking.
	 * @param buildStore The BuildStore to query.
	 * @param cmdArgs The name of a root (cmdArg[1]) to delete.
	 */
	/* package */ static void rmRoot(BuildStore buildStore, String[] cmdArgs) {
		
		FileNameSpaces fns = buildStore.getFileNameSpaces();
		String rootName = cmdArgs[1];
		
		int rc = fns.deleteRoot(rootName);
		
		if (rc != ErrorCode.OK) {
			String msg = null;
			
			switch (rc) {
			case ErrorCode.NOT_FOUND:
				msg = rootName + " doesn't exist";
				break;
				
			case ErrorCode.CANT_REMOVE:
				msg = rootName + " can't be removed";
				break;
			}
			
			System.out.println("Error removing root. " + msg);
			System.exit(1);
		}
	}

}