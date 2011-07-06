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
import com.arapiki.disco.model.Components;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.FileSet;
import com.arapiki.utils.errors.ErrorCode;

/**
 * A helper class for DiscoMain. This class handles the disco commands that manipulate
 * various attributes of the BuildStore. These methods should only be called by methods in DiscoMain.
 * @author "Peter Smith <psmith@arapiki.com>"
 */
/* package */ class DiscoAttributes {

	/*-------------------------------------------------------------------------------------*/

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

	/*-------------------------------------------------------------------------------------*/

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
		int rc;

		int pathId = fns.getPath(pathName);

		/* is the path valid, if so, set the root */
		if (pathId != ErrorCode.BAD_PATH) {
			/* 
			 * There are two approaches here - if the root exists already, move it.
			 */
			if (fns.getRootPath(rootName) != ErrorCode.NOT_FOUND){
				rc = fns.moveRootToPath(rootName, pathId);
			}
		
			/* else, create a new root */
			else {
				rc = fns.addNewRoot(rootName, pathId);
			}
		} 
		
		/* the path we're trying to set the root at is invalid */
		else {
			rc = ErrorCode.BAD_PATH;
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

	/*-------------------------------------------------------------------------------------*/

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

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Show the components that are defined within the BuildStore
	 * @param buildStore The BuildStore to query.
	 * @param cmdArgs Unused for now
	 */
	public static void showComp(BuildStore buildStore, String[] cmdArgs) {
		Components cmpts = buildStore.getComponents();
		
		String compNames[] = cmpts.getComponents();
		for (int i = 0; i < compNames.length; i++) {
			System.out.println(compNames[i]);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add a new component to this BuildStore. The component is only described by a String name,
	 * with no other values required.
	 * @param buildStore The BuildStore to modify.
	 * @param cmdArgs The name of a component (cmdArg[1]) to add.
	 */
	public static void addComp(BuildStore buildStore, String[] cmdArgs) {
		Components cmpts = buildStore.getComponents();
		
		String compName = cmdArgs[1];
		int compId = cmpts.addComponent(compName);
		
		/* was the syntax of the name valid? */
		if (compId == ErrorCode.INVALID_NAME){
			System.err.println("Error: Invalid component name " + compName);
			System.exit(1);
		}
		
		/* was the name already defined in the buildstore? */
		if (compId == ErrorCode.ALREADY_USED){
			System.err.println("Error: Component " + compName + " is already defined.");
			System.exit(1);
		}
		
		/* all is good */
		System.out.println("New component " + compName + " added.");		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Remove a component from the BuildStore.
	 * @param buildStore The BuildStore to modify.
	 * @param cmdArgs The name of a component (cmdArg[1]) to delete.
	 */
	public static void rmComp(BuildStore buildStore, String[] cmdArgs) {
		Components cmpts = buildStore.getComponents();
		
		String compName = cmdArgs[1];
		int result = cmpts.removeComponent(compName);
		if (result == ErrorCode.CANT_REMOVE) {
			System.err.println("Error: Component " + compName + " can't be deleted while it still contains files or tasks.");
			System.exit(1);
		}
		if (result == ErrorCode.NOT_FOUND) {
			System.err.println("Error: Component " + compName + " is not defined.");
			System.exit(1);
		}
		
		/* else, all is good */
		System.out.println("Component " + compName + " removed.");		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @param buildStore
	 * @param cmdArgs
	 */
	public static void setFileComp(BuildStore buildStore, String[] cmdArgs) {
		FileNameSpaces fns = buildStore.getFileNameSpaces();
		Components cmpts = buildStore.getComponents();

		/* 
		 * The component can be of the form: "comp" or "comp/section". If section
		 * isn't specified, "private" will be used.
		 */
		String compName = cmdArgs[1];
		int compAndSectionIds[] = CliUtils.parseComponentAndSection(cmpts, compName, true);
		int compId = compAndSectionIds[0];
		int sectId = compAndSectionIds[1];

		/* now visit each file in the FileSet and set it's component/section */
		FileSet filesToSet = CliUtils.getCmdLineFileSet(fns, cmdArgs, 2);
		buildStore.setFastAccessMode(true);
		for (int file : filesToSet) {
			cmpts.setFileComponent(file, compId, sectId);
		}
		buildStore.setFastAccessMode(false);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @param buildStore
	 * @param cmdArgs
	 */
	public static void setTaskComp(BuildStore buildStore, String[] cmdArgs) {
		String compName = cmdArgs[1];
		
	}

	/*-------------------------------------------------------------------------------------*/
}
