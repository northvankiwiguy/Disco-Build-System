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

/**
 *  A helper class for DiscoMain. This class handles the disco commands that report things
 * (trees, builds, etc). These methods should only be called by methods in DiscoMain.
 * @author "Peter Smith <psmith@arapiki.com>"
 */
/* package */ class DiscoReports {

	/*=====================================================================================*
	 * PACKAGE-LEVEL METHODS
	 *=====================================================================================*/

	/**
	 * Provide a list of all files in the BuildStore. Path filters can be provided to limit
	 * the result set
	 * @param buildStore The BuildStore to query
	 * @param showRoots True if file system roots (e.g. "root:") should be shown
	 * @param cmdArgs The user-supplied list of files/directories to be displayed. Only
	 * files that match this filter will be displayed. Note that cmdArgs[0] is the
	 * name of the command (show-files) are will be ignored.
	 */
	/* package */ static void showFiles(BuildStore buildStore, 
			boolean showRoots, String cmdArgs[]) {

		FileNameSpaces fns = buildStore.getFileNameSpaces();
		
		/* fetch the subset of files we should filter through */
		FileSet filterFileSet = getFilterFileSet(fns, cmdArgs);
		
		/* 
		 * There were no search "results", so we'll show everything (except those
		 * that are filtered-out by filterFileSet. 
		 */
		FileSet resultFileSet = null;
		
		/* pretty print the results */
		printFileSet(System.out, fns, resultFileSet, filterFileSet, showRoots);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Provide a list of all unused files in the BuildStore. That is, files that aren't
	 * referenced by any build tasks.
	 * @param buildStore The BuildStore to query.
	 * @param showRoots True if file system roots (e.g. "root:") should be shown
	 * @param cmdArgs The user-supplied list of files/directories to be displayed. Only unused
	 * files that match this filter will be displayed. Note that cmdArgs[0] is the
	 * name of the command (show-files) are will be ignored.
	 */
	/* package */ static void showUnusedFiles(BuildStore buildStore, 
			boolean showRoots, String cmdArgs[]) {

		FileNameSpaces fns = buildStore.getFileNameSpaces();
		Reports reports = buildStore.getReports();

		/* fetch the file/directory filter so we know which result files to display */
		FileSet filterFileSet = getFilterFileSet(fns, cmdArgs);

		/* get list of unused files, and add their parent paths */
		FileSet unusedFileSet = reports.reportFilesNeverAccessed();
		unusedFileSet.populateWithParents();
		
		/* pretty print the results */
		printFileSet(System.out, fns, unusedFileSet, filterFileSet, showRoots);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Generic function for displaying a FileSet. This is used primarily for displaying
	 * the result of reports.
	 * 
	 * @param outStream The PrintStream on which the output should be displayed.
	 * @param fns The FileNameSpaces containing the files to be listed.
	 * @param resultFileSet The set of files to be displayed (if null, show them all)
	 * @param filterFileSet If not-null, used to filter which paths from resultFileSet should be
	 *         displayed (set to null to display everything).
	 * @param showRoots Indicates whether path roots should be displayed (true), or whether absolute paths
	 * 		   should be used (false).
	 */
	/* package */ static void printFileSet(
			PrintStream outStream, FileNameSpaces fns, FileSet resultFileSet,
			FileSet filterFileSet, boolean showRoots) {
		
		/*
		 * This method uses recursion to traverse the FileNameSpaces
 		 * from the root to the leaves of the tree. It maintains a StringBuffer with the 
		 * path encountered so far. That is, if the StringBuffer contains "/a/b/c" and the
		 * path "d" is encountered, then append "/d" to the StringBuffer to get "/a/b/c/d".
		 * Once we've finished traversing directory "d", we pop it off the StringBuffer
		 * and return to "/a/b/c/". This allows us to do a depth-first traversal of the
		 * FileNameSpaces tree without doing more database access than we need to.
		 * 
		 * The resultFileSet and filterFileSet work together to determine which paths are
		 * to be displayed. resultFileSet contains all the files from the relevant database
		 * query. On the other hand, filterFileSet is the list of files that have been
		 * selected by the user's command line argument (e.g. selecting a subdirectory, or
		 * selecting files that match a pattern, such as *.c).
		 */
		
		/* 
		 * We always start at the top root, even though we may only display a subset
		 * of the paths underneath that root. Also, figure out the root's name
		 * (it's '/' or 'root:').
		 */
		int topRoot = fns.getRootPath("root");
		String rootPathName = fns.getPathName(topRoot, showRoots);

		/*
		 * Create a StringBuffer that'll be used for tracking the path name. We'll
		 * expand and contract this StringBuffer as we progress through the directory
		 * listing. This saves us from recomputing the parent path each time we
		 * visit a new directory.
		 */
		StringBuffer sb = new StringBuffer();					
		sb.append(rootPathName);
		if (!rootPathName.equals("/")) {
			sb.append('/');
		}
			
		/* 
		 * Special case for displaying the top path, potentially when it's a "root", as opposed
		 * to an absolute path.
		 */
		if (shouldBeDisplayed(topRoot, resultFileSet, filterFileSet)){
			if (!showRoots || fns.getRootAtPath(topRoot) == null){
				outStream.println(rootPathName);
			} else {
				outStream.println(rootPathName + " (" + fns.getPathName(topRoot, false) + ")");
			}
		}
		
		/* call the helper function to display each of our children */
		Integer children[] = fns.getChildPaths(topRoot);
		for (int i = 0; i < children.length; i++) {
			printFileSetHelper(outStream, sb, fns, children[i], 
					resultFileSet, filterFileSet, showRoots);
		}
	}
			

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Helper method for displaying a path and all it's children. This should only be called
	 * by printFileSet().
	 * 
	 * @param outStream The PrintStream to display the paths on
	 * @param pathSoFar This path's parent path as a string, complete with trailing "/"
	 * @param fns The FileNameSpaces object in which these paths belong
	 * @param thisPathId The path to display (assuming it's in the filesToShow FileSet).
	 * @param resultFileSet The set of files to be displayed (if null, show them all)
	 * @param filterFileSet If not-null, used to filter which paths from resultFileSet
	 * 		   should be displayed (set to null to display everything).
	 * @param showRoots Whether to show path roots (true) or absolute paths (false)
	 */
	private static void printFileSetHelper(
			PrintStream outStream, StringBuffer pathSoFar, FileNameSpaces fns, int thisPathId,
			FileSet resultFileSet, FileSet filterFileSet, boolean showRoots) {

		/* 
		 * Optimize the tree walk - if thisPathId isn't in resultFileSet, there's no point
		 * in walking the tree any further down. This assumes that resultFileSet.populateWithParents()
		 * has been called. Note that filterFileSet can't be used in this optimization.
		 */
		if ((resultFileSet != null) && (!resultFileSet.isMember(thisPathId))){
			return;
		}	
		
		/* get this path's list of children */
		Integer children[] = fns.getChildPaths(thisPathId);

		/* we'll use this to record the current path's name */
		String baseName;
		
		/* should this path be displayed? */
		boolean isInSet = shouldBeDisplayed(thisPathId, resultFileSet, filterFileSet);	
		
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
				printFileSetHelper(outStream, pathSoFar, fns, children[i], 
						resultFileSet, filterFileSet, showRoots);
			}
			
			/* remove our base name from the pathSoFar, so our caller sees the correct value again */
			pathSoFar.setLength(pathSoFarLen);
		}
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Given zero or more command line arguments, create a FileSet that stores all the files
	 * mention in those command-line arguments
	 * @param cmdArgs A String[] of command line arguments (files, directories, or regular expressions).
	 * Note that cmdArgs[0] is the command name (e.g. show-files), and should therefore be ignored.
	 * @return A FileSet containing all the files that were selected by the command-line arguments.
	 */
	private static FileSet getFilterFileSet(FileNameSpaces fns, String[] cmdArgs) {
		
		/* if no arguments are provided (except the command name), return null to represent "all files" */
		if (cmdArgs.length <= 1) {
			return null;
		}

		/* skip over the first argument, which is the command name */
		String filterPaths[] = new String[cmdArgs.length - 1];
		System.arraycopy(cmdArgs, 1, filterPaths, 0, cmdArgs.length - 1);
		
		FileSet result = new FileSet(fns);
		result.populateWithPaths(filterPaths);
		
		return result;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Is this path in the set of paths to be displayed? That is, is it in the resultFileSet
	 * as well as being part of filterFileSet?
	 * @param thisPathId The ID of the path we might want to display
	 * @param resultFileSet The set of paths in the result set
	 * @param filterFileSet The set of paths in the filter set
	 */
	private static boolean shouldBeDisplayed(int thisPathId, 
			FileSet resultFileSet, FileSet filterFileSet) {
		
		return ((resultFileSet == null) || (resultFileSet.isMember(thisPathId))) &&
				((filterFileSet == null) || (filterFileSet.isMember(thisPathId)));
	}
	
	/*-------------------------------------------------------------------------------------*/
	
}
