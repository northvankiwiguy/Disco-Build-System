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

import java.io.PrintStream;

import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.FileSet;
import com.arapiki.disco.model.Reports;
import com.arapiki.disco.model.FileNameSpaces.PathType;
import com.arapiki.utils.print.PrintUtils;

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
	 * Provide a list of all files in the BuildStore. A argument can be provided to limit
	 * the result set:
	 *   1) A path name prefix - only show files within that path
	 *   2) A file name - show all files (regardless of path) matching that name.
	 * @param buildStore The BuildStore to query.
	 */
	/* package */ static void showFiles(BuildStore buildStore, 
			boolean showRoots, boolean useIndents, String cmdArgs[]) {

		FileNameSpaces fns = buildStore.getFileNameSpaces();
		
		/* by default (with no command line arg), we'll print all files from the root */
		int rootPath = -1;
		boolean showFromPrefix = true;
		String fileArg = null;
		
		/* in the case where a user-supplied path or file name is supplied, we override defaults */
		if (cmdArgs.length == 2) {
			fileArg = cmdArgs[1];

			/* 
			 * Detect whether the argument is a path prefix, or a single file name. A 
			 * path prefix will contain a /, whereas a file name won't.
			 */
			if (fileArg.indexOf('/') != -1){
				
				/* we've been given a path prefix, find the new root */
				showFromPrefix = true;
				rootPath = fns.getPath(fileArg);
				if (rootPath == -1) {
					System.err.println("Error: Invalid path " + fileArg);
					System.exit(1);
				}
			} 
			
			/* the argument was a single file name, not a path prefix */
			else {
				showFromPrefix = false;
			}
		}
		
		/* was a root path selected? If not, select the default */
		if (rootPath == -1) {
			rootPath = fns.getRootPath("root");
		}
		
		/* 
		 * So... should we print all files (within the prefix), or all files that 
		 * match the name?
		 */
		if (showFromPrefix) {
		
			/* 
			 * Go ahead and display the files - we want all files to be shown, so
			 * we provide null for the FileSet parameter.
			 */
			printPathListing(System.out, fns, rootPath, null, showRoots, useIndents);
		}
		
		/* the user provided a file name, print all paths that match that name */
		else {
			Reports reports = buildStore.getReports();
			FileSet matchingFiles = reports.reportFilesThatMatchName(fileArg);
			printPathListing(System.out, fns, rootPath, matchingFiles, showRoots, useIndents);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Provide a list of all unused files in the BuildStore. That is, files that aren't
	 * referenced by any build tasks.
	 * @param buildStore The BuildStore to query.
	 */
	/* package */ static void showUnusedFiles(BuildStore buildStore, 
			boolean showRoots, boolean useIndents, String cmdArgs[]) {

		FileNameSpaces fns = buildStore.getFileNameSpaces();
		Reports reports = buildStore.getReports();
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

		/* get list of unused files, including parent paths */
		FileSet unusedFileSet = reports.reportFilesNeverAccessed();
		unusedFileSet.populateWithParents();
		
		/* 
		 * Go ahead and display the files - we want all files to be shown, so
		 * we provide null for the FileSet parameter.
		 */
		printPathListing(System.out, fns, rootPath, unusedFileSet, showRoots, useIndents);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Generic function for displaying a list of files. This is used primarily for displaying
	 * the result of reports.
	 * 
	 * @param outStream The PrintStream on which the output should be displayed.
	 * @param fns The FileNameSpaces containing the files to be listed.
	 * @param topPath The top path of all the files to be displayed (can be used to limit which
	 * 		   paths are displayed).
	 * @param fileToShow If not-null, used to limited whether a path should be displayed (set to null to
	 *         display everything).
	 * @param showRoots Indicates whether path roots should be displayed (true), or whether absolute paths
	 * 		   should be used (false).
	 * @param useIndents Indicates whether listings should use indentation (true), or whether each line
	 * 		   should display the full path (false).
	 */
	/* package */ static void printPathListing(
			PrintStream outStream, FileNameSpaces fns, int topPath,
			FileSet filesToShow, boolean showRoots, boolean useIndents) {
		
		/*
		 * The case for indented printed.
		 */
		if (useIndents) {
			printPathListingIndentHelper(outStream, fns, topPath, filesToShow, showRoots, 0);
		} 
		
		/*
		 * The case for non-indented printing, requiring us to keep track of the parent path.
		 * Each recursive iteration only needs to append the current path's base name, rather
		 * than calling the expensive fns.getPathName() method for each path.
		 */
		else {
			
			/* what's our starting point for the traversal? (possibly a root in form "root:" */
			String rootPathName = fns.getPathName(topPath, showRoots);
		
			/* create a StringBuffer and put the top path's full name in it, complete with trailing '/' */
			StringBuffer sb = new StringBuffer();			
			sb.append(rootPathName);
			if (!rootPathName.equals("/")) {
				sb.append('/');
			}
			
			/* 
			 * Special case for displaying the top path, potentially when it's a "root", as opposed
			 * to an absolute path.
			 */
			if ((filesToShow == null) || (filesToShow.isMember(topPath))){
				if (!showRoots || fns.getRootAtPath(topPath) == null){
					outStream.println(rootPathName);
				} else {
					outStream.println(rootPathName + " (" + fns.getPathName(topPath, false) + ")");
				}
			}
			
			/* call the helper function to display each of our children */
			Integer children[] = fns.getChildPaths(topPath);
			for (int i = 0; i < children.length; i++) {
				printPathListingNonIndentHelper(outStream, sb, fns, children[i], filesToShow, showRoots);
			}
		}
	}
			

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Helper method for displaying a path and all it's children. This should only be called
	 * by printPathListing().
	 * 
	 * @param outStream The PrintStream to display the paths on
	 * @param pathSoFar This path's parent path as a string, complete with trailing "/"
	 * @param fns The FileNameSpaces object in which these paths belong
	 * @param thisPathId The path to display (assuming it's in the filesToShow FileSet).
	 * @param filesToShow A FileSet stating which paths to show (or null to show all of them)
	 * @param showRoots Whether to show path roots (true) or absolute paths (false)
	 */
	private static void printPathListingNonIndentHelper(
			PrintStream outStream, StringBuffer pathSoFar, FileNameSpaces fns, int thisPathId,
			FileSet filesToShow, boolean showRoots) {

		/* get this path's list of children */
		Integer children[] = fns.getChildPaths(thisPathId);

		/* we'll use this to record the current path's name */
		String baseName;
		
		/* is this path in the set of paths to be displayed? */
		boolean isInSet = (filesToShow == null) || (filesToShow.isMember(thisPathId));
		
		/*
		 * There are two cases to handle:
		 * 	1) Roots should be displayed, and this path is a root.
		 *  2) Roots shouldn't be displayed, OR this isn't a root.
		 */
		String rootName = null;
		if (showRoots) {
			rootName = fns.getRootAtPath(thisPathId);
		}
		
		/* this path isn't a root, or we're not interested in displaying roots */
		if (rootName == null) {
			
			/* get the name of this path */
			baseName = fns.getBaseName(thisPathId);
		
			/* if this is to be displayed, then display it */
			if (isInSet) {
				/* display this path, prefixed by the absolute pathSoFar */
				outStream.print(pathSoFar);		
				outStream.print(baseName);
		
				/* for all directories (empty or not), display a / after their name */
				if (fns.getPathType(thisPathId) == PathType.TYPE_DIR){
					outStream.print('/');
				}
				outStream.println();
			}
		}
			
		/* else, this is a root and we need to display it */
		else {
			
			/* 
			 * Start a new pathSoFar with the name of the root, ignoring the previous
			 * value of pathSoFar (which incidentally isn't lost - our caller still
			 * has a reference to it and will use it for displaying our sibling paths).
			 */
			pathSoFar = new StringBuffer();
			pathSoFar.append(rootName);
			pathSoFar.append(':');
			
			/* display information about this root */
			if (isInSet) {
				outStream.println(pathSoFar + " (" + fns.getPathName(thisPathId) + ")");
			}
			
			/* we don't display this path's name */
			baseName = "";
		}
			
		/* if there are children, call ourselves recursively to display them */
		if (children.length != 0) {
			
			/* append this path onto the pathSoFar, since it'll become the pathSoFar for each child */
			int pathSoFarLen = pathSoFar.length();
			pathSoFar.append(baseName);
			pathSoFar.append('/');
		
			/* display each of the children */
			for (int i = 0; i < children.length; i++) {
				printPathListingNonIndentHelper(outStream, pathSoFar, fns, children[i], filesToShow, showRoots);
			}
			
			/* remove our base name from the pathSoFar, so our caller sees the correct value again */
			pathSoFar.setLength(pathSoFarLen);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper method for displaying a path and all it's children. This should only be called
	 * by printPathListing().
	 * @param outStream The PrintStream to display the paths on
	 * @param fns The FileNameSpaces object in which these paths belong
	 * @param thisPathId The path to display (assuming it's in the filesToShow FileSet).
	 * @param filesToShow A FileSet stating which paths to show (or null to show all of them)
	 * @param showRoots Whether to show path roots (true) or absolute paths (false)
	 * @param indentLevel This path's indentation level (how many spaces to print before the name)
	 */
	private static void printPathListingIndentHelper(
			PrintStream outStream, FileNameSpaces fns, int thisPathId,
			FileSet filesToShow, boolean showRoots, int indentLevel) {

		/* 
		 * Get the base name (without directory) of this path. For "/", we display nothing 
		 * because a "/" will be added anyway. If this path has an attached root, we'll
		 * display that instead.
		 */
		String baseName = fns.getBaseName(thisPathId);
		if (baseName.equals("/")) {
			baseName = "";
		}
		boolean displaySlash = true;

		/* do we want to show roots? */
		if (showRoots) {
			String rootName = fns.getRootAtPath(thisPathId);
		
			/* if there's an attached root, display it */
			if (rootName != null) {
				baseName = rootName + ": (" + fns.getPathName(thisPathId) + ")";
				displaySlash = false;
			}
		}
		
		/* if this file is in the set of files to display */
		if ((filesToShow == null) || filesToShow.isMember(thisPathId)){
			/* print the base name, preceded by spaces to the desired indentation level */
			PrintUtils.indent(outStream, indentLevel);		
			outStream.print(baseName);
		
			/* for directories (empty or not), display a trailing "/" */
			if (displaySlash && (fns.getPathType(thisPathId) == PathType.TYPE_DIR)){
				outStream.print('/');
			}
			outStream.println();
		}
		
		/* recursively call ourselves for each child, with an increased indent level */
		Integer children[] = fns.getChildPaths(thisPathId);
		for (int i = 0; i < children.length; i++) {
			printPathListingIndentHelper(outStream, fns, children[i], filesToShow, showRoots, indentLevel + 2);
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
