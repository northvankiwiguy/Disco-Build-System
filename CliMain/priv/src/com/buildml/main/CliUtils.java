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

package com.buildml.main;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.apache.commons.io.IOUtils;

import com.buildml.model.impl.BuildTasks;
import com.buildml.model.impl.Packages;
import com.buildml.model.impl.FileNameSpaces;
import com.buildml.model.impl.BuildTasks.OperationType;
import com.buildml.model.impl.FileNameSpaces.PathType;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.TaskSet;
import com.buildml.utils.errors.ErrorCode;
import com.buildml.utils.print.PrintUtils;

/**
 * A collection of utility methods that can be used by any CLI Command code. This
 * includes error reporting, command argument parsing, printing a FileSet and printing
 * a TaskSet. These methods are all static, so no object is required for them to be
 * invoked.
 * 
 * Note: A number of these methods are used for command-line validation, and could 
 * potentially abort the program without returning. They should therefore only be
 * used for command-line-based applications.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliUtils {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** Enumeration for specifying how a task's command string should be displayed. */
	public enum DisplayWidth { 
		
		/** 
		 * As much as possible of the task's command line should be displayed on one line 
		 * (truncate the remainder of the line if it's too long).
		 */
		ONE_LINE, 
		
		/** 
		 * If the command line is too long, wrap it onto multiple lines, using our custom-set
		 * line width when splitting lines. Try to be intelligent about breaking lines at spaces,
		 * rather than in the middle of words.
		 */
		WRAPPED, 
		
		/** 
		 * Don't do any command-line wrapping, and just let the terminal wrap the line 
		 * if it's too long. The whole string will be shown, across multiple lines.
		 */
		NOT_WRAPPED 
	}
	
	/** 
	 * The number of columns (characters) per output line. This is used when wrapping text.
	 */
	private static int columnWidth = 80;
	
	/** 
	 * When validating command line arguments, this value is used to represent an unlimited
	 * number of arguments.
	 */
	public static final int ARGS_INFINITE = -1;
	
	/*=====================================================================================*
	 * Public methods
	 *=====================================================================================*/

	/**
	 * Given a user-specified string (from the command line), parse the specification into
	 * a FileSet data structure containing all the relevant files. The string specification
	 * is a colon-separated list of:
	 *    <ol>
	 * 	  <li> An absolute path name (starting with /), either a directory name or a file name. 
	 *       If the path is a directory, add all files and directories below that point in the tree.</li>
	 *    <li>A path name starting with a "root:" - the same rules apply as for #1.</li>
	 *    <li>A single file name, with one or more wildcard (*) characters. All files that match
     *       the name are added, no matter what their directory.</li>
     *    <li>A package spec, starting with %pkg, or the complement of a package, starting 
     *       with %not-pkg.</li>
     *    </ol>
     *    
	 * @param fns The FileNameSpaces object that manages the files.
	 * @param pathSpecs A String of ":"-separated path specs (files, directories, or regular expressions).
	 * @return A FileSet containing all the files that were selected by the command-line arguments.
	 */
	public static FileSet getCmdLineFileSet(FileNameSpaces fns, String pathSpecs) {
	
		String pathSpecList[] = pathSpecs.split(":");
		
		/* else populate a new FileSet */
		FileSet result = new FileSet(fns);
		if (result.populateWithPaths(pathSpecList) != ErrorCode.OK) {
			CliUtils.reportErrorAndExit("Invalid path filter provided");
		}

		return result;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a user-supplied set of command line arguments, parse those arguments and create
	 * a suitable TaskSet containing all the relevant tasks that match the specification. The
	 * specification string is a colon-separated list of:
	 * <ol>
	 * 	 <li>A specific task number, which will be added to the TaskSet.</li>
	 *   <li>The task number followed by [/depth] to indicate that all tasks in the sub tree,
	 *      starting at the specified task and moving down the task tree "depth" level, should
	 *      be added.</li>
	 *   <li>If 'depth' is omitted (only the '/' is provided), all tasks is the subtree are added
	 *      (regardless of their depth).</li>
	 *   <li>If the task number is prefixed by '-', the tasks are removed from the TaskSet, rather
	 *      than being added. The "/depth" and "/" suffix can be used to remove subtasks as well.
	 *   <li>The special syntax "%pkg/foo" means all tasks in the package "foo".</li>
	 *   <li>The special syntax "%not-pkg/foo" means all tasks outside the package "foo".</li>
	 * </ol>
	 * @param bts The BuildTasks manager object to query.
	 * @param taskSpecs The command line argument providing the task specification string.
	 * @return The TaskSet, as described by the input task specification.
	 */
	public static TaskSet getCmdLineTaskSet(BuildTasks bts, String taskSpecs) {
		
		String taskSpecList[] = taskSpecs.split(":");

		TaskSet result = new TaskSet(bts);
		if (result.populateWithTasks(taskSpecList) != ErrorCode.OK) {
			System.err.println("Error: Invalid task filter provided.");
			System.exit(1);
		}
		
		return result;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a FileSet, display the files in that set in a pretty-printed format. This is 
	 * used primarily for displaying the result of reports.
	 * 
	 * @param outStream The PrintStream on which the output should be displayed.
	 * @param fns The FileNameSpaces manager object containing the files to be listed.
	 * @param pkgMgr The Packages manager object containing the package information.
	 * @param resultFileSet The set of files to be displayed (if null, show them all).
	 * @param filterFileSet If not-null, used to filter which paths from resultFileSet should be
	 *         displayed (set to null to display everything).
	 * @param showRoots Indicates whether path roots should be displayed (true), or whether 
	 *         absolute paths should be used (false).
	 * @param showPkgs Indicates whether the package names should be displayed.S
	 */
	public static void printFileSet(
			PrintStream outStream, FileNameSpaces fns, Packages pkgMgr, FileSet resultFileSet,
			FileSet filterFileSet, boolean showRoots, boolean showPkgs) {
		
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
		 * (it's '/' or '@root').
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
			printFileSetHelper(outStream, sb, fns, pkgMgr, children[i], 
					resultFileSet, filterFileSet, showRoots, showPkgs);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a TaskSet, display the tasks in that set in a pretty-printed format. To ensure
	 * that all tasks are displayed, you should first call TaskSet.populateWithParents().
	 * 
	 * @param outStream The PrintStream on which to display the output.
	 * @param bts The BuildTasks manager object containing the task information.
	 * @param fns The FileNameSpaces manager object containing file name information.
	 * @param pkgMgr The Packages manager object containing package information.
	 * @param resultTaskSet The set of tasks to be displayed (the results of some previous query).
	 * @param filterTaskSet The set of tasks to actually be displayed (for post-filtering the query results).
	 * @param outputFormat Mode for formatting the command strings.
	 * @param showPkgs Set to true if the package names should be shown.
	 */
	public static void printTaskSet(
			PrintStream outStream, BuildTasks bts, FileNameSpaces fns, Packages pkgMgr,
			TaskSet resultTaskSet, TaskSet filterTaskSet, DisplayWidth outputFormat,
			boolean showPkgs) {
		
		/* 
		 * We always start at the top root, even though we may only display a subset
		 * of the paths underneath that root.
		 */
		int topRoot = bts.getRootTask("");
	
		/* call the helper function to display each of our children */
		Integer children[] = bts.getChildren(topRoot);
		for (int i = 0; i < children.length; i++) {
			printTaskSetHelper(outStream, bts, fns, pkgMgr, children[i], 
					resultTaskSet, filterTaskSet, outputFormat, showPkgs, 1);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the number of columns (characters) of output to be printed per report line.
	 * @return The number of columns.
	 */
	public static int getColumnWidth() {
		return columnWidth;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set the number of columns (characters) of output to be printed per report line. The
	 * minimum width is 40 characters. Any attempt to set a narrower width will revert to 40.
	 * @param newWidth The new column width to set.
	 */
	public static void setColumnWidth(int newWidth) {
		if (newWidth < 40) {
			newWidth = 40;
		}
		columnWidth = newWidth;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Parse the user-supplied package/scope string, and return the corresponding
	 * package ID and scope ID. The string should be in the format "package" or 
	 * "package/scope". If "scope" is not provided (and scopeAllowed is true),
	 * "private" is assumed. If the input is invalid, display a meaningful error message
	 * and exit the program.
	 * 
	 * This method may abort the whole program (never returning) if the input string
	 * is invalid.
	 * 
	 * @param pkgMgr The Packages manager object containing the package information.
	 * @param pkgString The user-supplied input string (could be anything).
	 * @param scopeAllowed True if the input string is allowed to provide a scope name. 
	 * @return An array of two integers. The first is the package's ID number,
	 * and the second is the scope's ID number.
	 */
	public static int[] parsePackageAndScope(
			Packages pkgMgr,
			String pkgString, 
			boolean scopeAllowed) {
	
		String pkgName = null;
		String scopeName = null;
		
		/* check if there's a '/' in the string, to separate "package" from "scope" */
		int slashIndex = pkgString.indexOf('/');
		if (slashIndex != -1) {
			pkgName = pkgString.substring(0, slashIndex);
			scopeName = pkgString.substring(slashIndex + 1);
			if (!scopeAllowed) {
				CliUtils.reportErrorAndExit("Invalid syntax - '/" + scopeName + "' not allowed.");
			}	
		} 
		
		/* else, there's no /, assume 'private' for the scope */
		else {
			pkgName = pkgString;
			scopeName = "private";
		}

		/* compute the IDs */
		int pkgId = pkgMgr.getPackageId(pkgName);
		int scopeId = pkgMgr.getScopeId(scopeName);
		
		if (pkgId == ErrorCode.NOT_FOUND) {
			CliUtils.reportErrorAndExit("Unknown package: " + pkgName);
		}
		if (scopeId == ErrorCode.NOT_FOUND) {
			CliUtils.reportErrorAndExit("Unknown scope name: " + scopeName);
		}
		
		return new int[]{ pkgId, scopeId };
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Validation function to ensure that the number of arguments provided to a command
	 * is in range between minArgs and maxArgs.
	 * 
	 * This method may abort the whole program (never returning) if the number of input
	 * arguments is invalid.
	 * 
	 * @param cmdName The name of the command being executed.
	 * @param cmdArgs The array of input arguments.
	 * @param minArgs The minimum number of arguments required (0 or higher).
	 * @param maxArgs The maximum number of arguments required (0 or higher - possibly ARGS_INFINITE).
	 * @param message An error message to display if an invalid number of arguments is included.
	 */
	public static void validateArgs(String cmdName, String[] cmdArgs, int minArgs, int maxArgs,
			String message) {
		
		int actualArgs = cmdArgs.length;
	
		/* too few arguments? */
		if (actualArgs < minArgs) {
			reportErrorAndExit("Too few arguments to " + cmdName + " - " + message);
		}
		
		/* too many arguments? */
		else if ((maxArgs != ARGS_INFINITE) && (actualArgs > maxArgs)){
			reportErrorAndExit("Too many arguments to " + cmdName + " - " + message);
		}		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Display an error message in a standard format, then exit the program with a non-zero
	 * error code. This method call never returns.
	 * 
	 * @param message The message to be display. If null, just exit without displaying.
	 */
	public static void reportErrorAndExit(String message) {
		if (message != null){
			System.err.println("Error: " + message);
			System.err.println("       Use bml -h for more help.");
		}
		System.exit(1);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given that a command-line user may have specified the --read and --write command line 
	 * options, return the appropriate OperationType value that can be used for querying the database.
	 * 
	 * @param optionRead Set if the user provided the --read flag.
	 * @param optionWrite Set if the user provided the --write flag.
	 * @param optionModify Set if the user provided the --modify flag.
	 * @param optionDelete Set if the user provided the --delete flag.
	 * @return Either OP_UNSPECIFIED (search for either), OP_READ, or OP_WRITE
	 */
	public static OperationType getOperationType(boolean optionRead,
			boolean optionWrite, boolean optionModify, boolean optionDelete) {
		
		OperationType opType = OperationType.OP_UNSPECIFIED;
		int optionsProvided = 0;
		if (optionRead) {
			opType = OperationType.OP_READ;
			optionsProvided++;
		}
		if (optionWrite) {
			opType = OperationType.OP_WRITE;
			optionsProvided++;
		}
		if (optionModify) {
			opType = OperationType.OP_MODIFIED;
			optionsProvided++;
		}
		if (optionDelete) {
			opType = OperationType.OP_DELETE;
			optionsProvided++;
		}
		
		/* can't have more than one option provided at one time. */
		if (optionsProvided > 1) {
			System.err.println("Error: can't specify more than one of --read, --write, --modify or --delete.");
			System.exit(-1);
		}
		
		return opType;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Generate a localized help message, for use when displaying online help.
	 * The provided message string will most likely contain a single line with
	 * an "#include" directive, which has the effect of pulling in another text
	 * file containing the main body of the message. This text file may also pull
	 * in other text files for inclusion.
	 * 
	 * When searching for an included text file, the "messages/&lt;lang%gt;/" directory
	 * is searched, where %lt;lang%gt; is a language specifier, such as "en".
	 * 
	 * @param message Message to display, which most likely contains a #include directive.
	 * @return The full message string, which may be hundreds of lines long.
	 */
	public static String genLocalizedMessage(String message) {
		
		/* 
		 * We use recursion to pull all the messages (and possibly nested files)
		 * into the final string.
		 */
		StringBuffer sb = new StringBuffer();
		genLocalizedMessageHelper("en", message, sb);	
		return sb.toString();
	}
	
	/*=====================================================================================*
	 * Private methods
	 *=====================================================================================*/

	/**
	 * The CliUtils class can not be instantiated. Use the static methods only.
	 */
	private CliUtils() {
		/* empty */
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Helper method for displaying a path and all it's children, called exclusively by
	 * printFileSet().
	 * 
	 * @param outStream The PrintStream on which to display paths.
	 * @param pathSoFar This path's parent path as a string, complete with trailing "/".
	 * @param fns The FileNameSpaces manager object in which these paths belong.
	 * @param pkgMgr The Packages manager object that contains the package information.
	 * @param thisPathId The path to display (assuming it's in the filesToShow FileSet).
	 * @param resultFileSet The set of files to be displayed (if null, show them all).
	 * @param filterFileSet If not-null, used to filter which paths from resultFileSet
	 * 		   should be displayed (set to null to display everything).
	 * @param showRoots Whether to show path roots (true) or absolute paths (false).
	 * @param showPkgs Whether to show the package names.
	 */
	private static void printFileSetHelper(
			PrintStream outStream, StringBuffer pathSoFar, FileNameSpaces fns, Packages pkgMgr, int thisPathId,
			FileSet resultFileSet, FileSet filterFileSet, boolean showRoots, boolean showPkgs) {

		/* a StringBuffer for forming the package name */
		StringBuffer pkgString = new StringBuffer();
		
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
		
		/* 
		 * If we've been asked to display file packages, prepare the string to be printed.
		 */
		if (showPkgs) {

			/* to start, empty the StringBuffer */
			pkgString.delete(0, pkgString.length());
			
			/* fetch the file's package and scope */
			Integer pkgAndScopeId[] = pkgMgr.getFilePackage(thisPathId);
			if (pkgAndScopeId == null) {
				pkgString.append("Invalid file");
			} 
			
			/* if valid, fetch the human-readable names */
			else {
				int pkgId = pkgAndScopeId[0];
				int scopeId = pkgAndScopeId[1];

				String pkgName = pkgMgr.getPackageName(pkgId);
				String scopeName = pkgMgr.getScopeName(scopeId);
			
				/* if we can't fetch the text name of the package or scope... */
				if (pkgName == null || scopeName == null) {
					pkgString.append("Invalid package");
				}
			
				/* else, both names are valid, append them to the string */
				else {
					pkgString.append("  (");
					pkgString.append(pkgName);
					pkgString.append('/');
					pkgString.append(scopeName);
					pkgString.append(")");
				}
			}
		}
		
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
				outStream.print(baseName);
				
				/* show packages, if requested */
				if (showPkgs) {
					outStream.println(pkgString);
				} else {
					outStream.println();
				}
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
			pathSoFar.append('@');
			pathSoFar.append(rootName);
			
			/* display information about this root. */
			outStream.print(pathSoFar + " (" + fns.getPathName(thisPathId) + ") ");
			
			/* show packages, if requested */
			if (showPkgs) {
				outStream.println(pkgString);
			} else {
				outStream.println();
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
				printFileSetHelper(outStream, pathSoFar, fns, pkgMgr, children[i], 
						resultFileSet, filterFileSet, showRoots, showPkgs);
			}
			
			/* remove our base name from the pathSoFar, so our caller sees the correct value again */
			pathSoFar.setLength(pathSoFarLen);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * A helper method, called exclusively by printTaskSet(). This method calls itself recursively
	 * as it traverses the TaskSet's tree structure.
	 * 
	 * @param outStream The PrintStream on which to display the output.
	 * @param bts The BuildTasks manager object containing the task information.
	 * @param fns The FileNameSpaces manager object containing file name information.
	 * @param pkgMgr The Packages manager object containing the package information.
	 * @param taskId The ID of the task we're currently displaying (at this level of recursion).
	 * @param resultTaskSet The full set of tasks to be displayed (the result of some previous query).
	 * @param filterTaskSet The set of tasks to actually be displayed (for post-filtering the query results).
	 * @param outputFormat The way in which the tasks should be formatted.
	 * @param showPkgs Set to true if we should display package names.
	 * @param indentLevel The number of spaces to indent this task by (at this recursion level).
	 */
	private static void printTaskSetHelper(PrintStream outStream,
			BuildTasks bts, FileNameSpaces fns, Packages pkgMgr, 
			int taskId, TaskSet resultTaskSet,
			TaskSet filterTaskSet, DisplayWidth outputFormat, boolean showPkgs,
			int indentLevel) {
	
		/* 
		 * Display the current task, at the appropriate indentation level. The format is:
		 * 
		 * - Task 1 (/home/psmith/t/cvs-1.11.23)
	     *     if test ! -f config.h; then rm -f stamp-h1; emake  stamp-h1; else :;
	     *     
	     * -- Task 2 (/home/psmith/t/cvs-1.11.23)
	     *      failcom='exit 1'; for f in x $MAKEFLAGS; do case $f in *=* | --[!k]*);; \
	     *
	     * Where Task 1 is the parent of Task 2.
	     */
		
		/* is this task in the TaskSet to be printed? If not, terminate recursion */
		if (! (((resultTaskSet == null) || (resultTaskSet.isMember(taskId))) &&
			((filterTaskSet == null) || (filterTaskSet.isMember(taskId))))) {
			return;
		}	
	
		/* 
		 * Fetch the task's command string (if there is one). It can either be
		 * in short format (on a single line), or a full string (possibly multiple lines)
		 */
		String command;
		if (outputFormat == DisplayWidth.ONE_LINE) {
			command = bts.getCommandSummary(taskId, getColumnWidth() - indentLevel - 3);
		} else {
			command = bts.getCommand(taskId);
		}
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
		outStream.print(" Task " + taskId + " (" + taskDirName);
		
		/* if requested, display the task's package name */
		if (showPkgs) {
			int pkgId = pkgMgr.getTaskPackage(taskId);
			if (pkgId == ErrorCode.NOT_FOUND) {
				outStream.print(" - Invalid task");
			} else {
				String pkgName = pkgMgr.getPackageName(pkgId);
				if (pkgName == null) {
					outStream.print(" - Invalid package");
				} else {
					outStream.print(" - " + pkgName);					
				}
			}
		}
		outStream.println(")");
		
		/* display the task's command string. Each line must be indented appropriately */
		if (outputFormat != DisplayWidth.NOT_WRAPPED) {
			PrintUtils.indentAndWrap(outStream, command, indentLevel + 3, getColumnWidth());
			outStream.println();
		} else {
			outStream.println(command);
		}
		
		/* recursively call ourselves to display each of our children */
		Integer children[] = bts.getChildren(taskId);
		for (int i = 0; i < children.length; i++) {
			printTaskSetHelper(outStream, bts, fns, pkgMgr, children[i], 
					resultTaskSet, filterTaskSet, outputFormat, showPkgs, indentLevel + 1);
		}
		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether this path is in the set of paths to be displayed. That is, it's
	 * in the resultFileSet as well as being part of filterFileSet.
	 * 
	 * @param thisPathId The ID of the path we might want to display.
	 * @param resultFileSet The set of paths in the result set.
	 * @param filterFileSet The set of paths in the filter set.
	 * @return Whether or not the path should be displayed.
	 */
	private static boolean shouldBeDisplayed(int thisPathId, 
			FileSet resultFileSet, FileSet filterFileSet) {
		
		return ((resultFileSet == null) || (resultFileSet.isMember(thisPathId))) &&
				((filterFileSet == null) || (filterFileSet.isMember(thisPathId)));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * A helper function for genLocalizedMessage, used for recursion.
	 * 
	 * @param lang The language to localize into (e.g. "en" or "fr").
	 * @param message The message to be displayed (possibly including #include).
	 * @param sb The StringBuffer we'll use to build up the final string.
	 */
	private static void genLocalizedMessageHelper(
			String lang,
			String message,
			StringBuffer sb) {
		
		/* tokenize the string, and handle each line separately */
		String [] lines = message.split("\n");
		for (int i = 0; i < lines.length; i++) {
			String thisLine = lines[i];
			
			/* 
			 * If this line contains an #include directive, read the file content, then
			 * call ourselves recursively to process the content.
			 */
			if (thisLine.matches("#include .*")){
				String includeLine[] = thisLine.split(" ");
				
				/* try to open the file (resource) as a stream */
				String fileName = "messages/" + lang + "/" + includeLine[1];
				InputStream inStream = ClassLoader.getSystemResourceAsStream(fileName);
				if (inStream == null) {
					sb.append("<missing include: " + fileName + ">\n");	
				} 
				
				/* read the stream into a string, then recursively process it */
				else {
					try {
						String content = IOUtils.toString(inStream, "UTF-8");
						genLocalizedMessageHelper(lang, content, sb);

					} catch (IOException e1) {
						sb.append("<invalid include: " + fileName + ">\n");							
					}
					
					try {
						inStream.close();
					} catch (IOException e) {
						/* nothing */
					}
				}
			}
			
			/* no #include directive, so just include the line verbatim */
			else {
				sb.append(lines[i]);
				sb.append('\n');
			}
		}
		
	}

	/*-------------------------------------------------------------------------------------*/
}

