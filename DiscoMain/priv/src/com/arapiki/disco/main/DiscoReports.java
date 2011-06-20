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
import com.arapiki.disco.model.BuildTasks;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.FileSet;
import com.arapiki.disco.model.Reports;
import com.arapiki.disco.model.TaskSet;
import com.arapiki.disco.model.BuildTasks.OperationType;
import com.arapiki.disco.model.FileNameSpaces.PathType;
import com.arapiki.utils.errors.ErrorCode;
import com.arapiki.utils.print.PrintUtils;

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
		FileSet filterFileSet = getCmdLineFileSet(fns, cmdArgs);
		
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
		FileSet filterFileSet = getCmdLineFileSet(fns, cmdArgs);

		/* get list of unused files, and add their parent paths */
		FileSet unusedFileSet = reports.reportFilesNeverAccessed();
		unusedFileSet.populateWithParents();
		
		/* pretty print the results */
		printFileSet(System.out, fns, unusedFileSet, filterFileSet, showRoots);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Display a report of the files that are directly (or indirectly) derived from the set
	 * of input files (provided on the command line). The concept of "derived" means that there's
	 * some task that takes the specific file(s) as input and writes to some other file. A
	 * file is "directly" derived if the same task reads the input and writes the output. 
	 * A file is "indirectly" derived if a chain of tasks exists between the input and
	 * output files.
	 * For example, foo.o is directly derived from foo.c, and foo.exe is indirectly derived
	 * through the chain foo.c -> foo.o -> foo.exe.
	 * @param buildStore The BuildStore to query.
	 * @param optionShowRoots True if file system roots (e.g. "root:") should be shown
	 * @param showAll Set to true if indirectly derived files should also be shown.
	 * @param cmdArgs The user-supplied list of files/directories to be displayed. Only unused
	 * files that match this filter will be displayed. Note that cmdArgs[0] is the
	 * name of the command (show-files) are will be ignored.
	 */
	public static void showDerivedFiles(BuildStore buildStore,
			boolean showRoots, boolean showAll, String[] cmdArgs) {
		
		FileNameSpaces fns = buildStore.getFileNameSpaces();
		Reports reports = buildStore.getReports();

		/* fetch the list of files that are the source of the derivation */
		FileSet sourceFileSet = getCmdLineFileSet(fns, cmdArgs);

		/* get list of derived files, and add their parent paths */
		FileSet derivedFileSet = reports.reportDerivedFiles(sourceFileSet, showAll);
		derivedFileSet.populateWithParents();
		
		/* pretty print the results - no filtering used here */
		printFileSet(System.out, fns, derivedFileSet, null, showRoots);	
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Show a number of tasks from the BuildStore. By default, all tasks will be shown in
	 * a tree hierarchy. If user-supplied filter parameters are provided, the set of tasks
	 * will be filtered accordingly.
	 * @param buildStore The BuildStore to query
	 * @param cmdArgs The user-supplied list of tasks to be displayed. Only tasks 
	 * that match this filter will be displayed. Note that cmdArgs[0] is the
	 * name of the command (show-tasks) are will be ignored.
	 */
	public static void showTasks(BuildStore buildStore, String[] cmdArgs) {
		BuildTasks bts = buildStore.getBuildTasks();
		FileNameSpaces fns = buildStore.getFileNameSpaces();
		
		/* compute a TaskSet to display, or null if no arguments are provided */
		TaskSet ts = getCmdLineTaskSet(bts, cmdArgs);
		
		/* display the selected task set */
		printTaskSet(System.out, bts, fns, ts, null);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Display all build tasks that access (read, write or use) any of the user-specified
	 * files.
	 * @param buildStore The BuildStore to query
	 * @param optionRead Only show tasks that read the files
	 * @param optionWrite Only show tasks that read the files
	 * @param cmdArgs The user-supplied list of files/directories to query. 
	 * Note that cmdArgs[0] is the name of the command (show-files) are will be ignored.
	 */
	public static void showTasksThatAccess(BuildStore buildStore,
			boolean optionRead, boolean optionWrite, String[] cmdArgs) {
		
		/* can't have both --read and --write at the same time */
		if (optionRead && optionWrite) {
			System.err.println("Error: can't specify both --read and --write in the same command.");
			System.exit(-1);
		}
		
		OperationType opType = OperationType.OP_UNSPECIFIED;
		if (optionRead) {
			opType = OperationType.OP_READ;
		} else if (optionWrite) {
			opType = OperationType.OP_WRITE;			
		}
		
		FileNameSpaces fns = buildStore.getFileNameSpaces();
		BuildTasks bts = buildStore.getBuildTasks();
		Reports reports = buildStore.getReports();

		/* fetch the FileSet of paths from the user's command line */
		FileSet fileSet = getCmdLineFileSet(fns, cmdArgs);
		
		/* find all tasks that access (read, write or both) these files */
		TaskSet taskSet = reports.reportTasksThatAccessFiles(fileSet, opType);
		taskSet.populateWithParents();
		
		/* display the resulting set of tasks */
		printTaskSet(System.out, bts, fns, taskSet, null);
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
		
		/* call the helper function to display each of our children */
		Integer children[] = fns.getChildPaths(topRoot);
		for (int i = 0; i < children.length; i++) {
			printFileSetHelper(outStream, sb, fns, children[i], 
					resultFileSet, filterFileSet, showRoots);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * TODO: Comment this properly
	 */
	/* package */ static void printTaskSet(
			PrintStream outStream, BuildTasks bts, FileNameSpaces fns,
			TaskSet resultTaskSet, TaskSet filterTaskSet) {
		
		/* 
		 * We always start at the top root, even though we may only display a subset
		 * of the paths underneath that root.
		 */
		int topRoot = bts.getRootTask("");

		/* call the helper function to display each of our children */
		Integer children[] = bts.getChildren(topRoot);
		for (int i = 0; i < children.length; i++) {
			printTaskSetHelper(outStream, bts, fns, children[i], 
					resultTaskSet, filterTaskSet, 1);
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

		/* should this path be displayed? */
		if (!shouldBeDisplayed(thisPathId, resultFileSet, filterFileSet)){
			return;
		}	
		
		/* get this path's list of children */
		Integer children[] = fns.getChildPaths(thisPathId);

		/* we'll use this to record the current path's name */
		String baseName;
		
		/*
		 * There are two cases to handle:
		 * 	1) Roots should be displayed, and this path is a root.
		 *  2) Roots shouldn't be displayed, OR this isn't a root.
		 */
		String rootName = null;
		if (showRoots) {
			rootName = fns.getRootAtPath(thisPathId);
		}
		
		/* what is this path? A directory or something else? */
		boolean isDirectory = (fns.getPathType(thisPathId) == PathType.TYPE_DIR);
		boolean isNonEmptyDirectory = isDirectory && (children.length != 0);
		
		/* this path isn't a root, or we're not interested in displaying roots */
		if (rootName == null) {
			
			/* get the name of this path */
			baseName = fns.getBaseName(thisPathId);
		
			/* 
			 * Display this path, prefixed by the absolute pathSoFar. Don't show non-empty
			 * directories, since they'll be displayed when the file they contain is
			 * displayed.
			 */
			if (!isNonEmptyDirectory) {
				outStream.print(pathSoFar);		
				outStream.println(baseName);
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
			
			/* display information about this root. */
			outStream.println(pathSoFar + " (" + fns.getPathName(thisPathId) + ")");
			
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
	private static FileSet getCmdLineFileSet(FileNameSpaces fns, String[] cmdArgs) {
		
		/* if no arguments are provided (except the command name), return null to represent "all files" */
		if (cmdArgs.length <= 1) {
			return null;
		}

		/* skip over the first argument, which is the command name */
		String inputPaths[] = new String[cmdArgs.length - 1];
		System.arraycopy(cmdArgs, 1, inputPaths, 0, cmdArgs.length - 1);
		
		FileSet result = new FileSet(fns);
		if (result.populateWithPaths(inputPaths) != ErrorCode.OK) {
			System.err.println("Error: Invalid path filter provided.");
			System.exit(1);
		}
		
		/* this result set will be traversed, so populate the parents too */
		result.populateWithParents();
		
		return result;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a user-supplied set of command line arguments, parse those arguments and create
	 * a suitable TaskSet. If no arguments are provided, the null TaskSet is returned (indicating
	 * that all files should be shown).
	 * @param bts The BuildTasks object to query.
	 * @param cmdArgs The command line arguments that specify the TaskSet to show. Note that cmdArgs[0]
	 * is ignored since it's the disco command to be executed, rather than a TaskSet filter.
	 */
	private static TaskSet getCmdLineTaskSet(BuildTasks bts, String[] cmdArgs) {
		
		/* if no arguments are provided (except the command name), return null to represent "all tasks" */
		if (cmdArgs.length <= 1) {
			return null;
		}

		/* skip over the first argument, which is the command name */
		String inputTasks[] = new String[cmdArgs.length - 1];
		System.arraycopy(cmdArgs, 1, inputTasks, 0, cmdArgs.length - 1);
		
		TaskSet result = new TaskSet(bts);
		if (result.populateWithTasks(inputTasks) != ErrorCode.OK) {
			System.err.println("Error: Invalid task filter provided.");
			System.exit(1);
		}
		
		/* this result set will be traversed, so populate the parents too */
		result.populateWithParents();
		
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
	
	/**
	 * 
	 * TODO: comment this properly.
	 * @param outStream
	 * @param bts
	 * @param integer
	 * @param resultTaskSet
	 * @param filterTaskSet
	 * @param i
	 */
	private static void printTaskSetHelper(PrintStream outStream,
			BuildTasks bts, FileNameSpaces fns, int taskId, TaskSet resultTaskSet,
			TaskSet filterTaskSet, int indentLevel) {

		/* 
		 * Display the current task, at the appropriate indentation level. The format is:
		 * 
		 * - Task 1 (/home/psmith/t/cvs-1.11.23)
         *     if test ! -f config.h; then rm -f stamp-h1; emake  stamp-h1; else :; 
         * -- Task 2 (/home/psmith/t/cvs-1.11.23)
         *      failcom='exit 1'; for f in x $MAKEFLAGS; do case $f in *=* | --[!k]*);; \
         *
         * Where Task 1 is the parent of Task 2.
         */
		// TODO: consider this logic.
		if (! ((resultTaskSet == null) || (resultTaskSet.isMember(taskId)))) {
			return;
		}

		/* fetch the task's command string (if there is one) */
		String command = bts.getCommand(taskId);
		if (command == null) {
			command = "<unknown command>";
		}
		
		/* fetch the name of the directory the task was executed in */
		int taskDirId = bts.getDirectory(taskId);
		String taskDirName = fns.getPathName(taskDirId);
		
		/* display the correct number of "-" characters */
		for (int i = 0; i != indentLevel; i++) {
			outStream.append('-');
		}
		outStream.println(" Task " + taskId + " (" + taskDirName + ")");
		
		/* display the task's command string. Each line must be indented appropriately */
		/* TODO: make the wrap value settable */
		PrintUtils.indentAndWrap(outStream, command, indentLevel + 3, 100);
		outStream.println();
		
		/* recursively call ourselves to display each of our children */
		Integer children[] = bts.getChildren(taskId);
		for (int i = 0; i < children.length; i++) {
			printTaskSetHelper(outStream, bts, fns, children[i], 
					resultTaskSet, filterTaskSet, indentLevel + 1);
		}
		
	}

	/*-------------------------------------------------------------------------------------*/
}
