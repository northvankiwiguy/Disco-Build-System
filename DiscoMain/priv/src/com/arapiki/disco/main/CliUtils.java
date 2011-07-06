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

import com.arapiki.disco.model.BuildTasks;
import com.arapiki.disco.model.Components;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.FileSet;
import com.arapiki.disco.model.TaskSet;
import com.arapiki.disco.model.FileNameSpaces.PathType;
import com.arapiki.utils.errors.ErrorCode;
import com.arapiki.utils.print.PrintUtils;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class CliUtils {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** how are tasks commands displayed? */
	public enum DisplayWidth { ONE_LINE, WRAPPED, NOT_WRAPPED }
	
	/** the number of columns (characters) per output line */
	private static int columnWidth = 80;
	
	/*=====================================================================================*
	 * Package-level methods
	 *=====================================================================================*/

	/**
	 * Given zero or more command line arguments, create a FileSet that stores all the files
	 * mention in those command-line arguments
	 * @param cmdArgs A String[] of command line arguments (files, directories, or regular expressions).
	 * Note that cmdArgs[0] is the command name (e.g. show-files), and should therefore be ignored.
	 * @return A FileSet containing all the files that were selected by the command-line arguments.
	 */
	/* package */ static FileSet getCmdLineFileSet(FileNameSpaces fns, String[] cmdArgs, int firstArgPos) {
		
		/* if no arguments are provided (except the command name), return null to represent "all files" */
		if (cmdArgs.length <= firstArgPos) {
			return null;
		}
	
		/* skip over the first argument, which is the command name */
		String inputPaths[] = new String[cmdArgs.length - firstArgPos];
		System.arraycopy(cmdArgs, firstArgPos, inputPaths, 0, cmdArgs.length - firstArgPos);
		
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
	/* package */ static TaskSet getCmdLineTaskSet(BuildTasks bts, String[] cmdArgs) {
		
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
			PrintStream outStream, FileNameSpaces fns, Components cmpts, FileSet resultFileSet,
			FileSet filterFileSet, boolean showRoots, boolean showComps) {
		
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
			printFileSetHelper(outStream, sb, fns, cmpts, children[i], 
					resultFileSet, filterFileSet, showRoots, showComps);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a TaskSet, display the tasks in that set in a pretty-printed format. To ensure
	 * that all tasks are displayed, you should first call TaskSet.populateWithParents().
	 * @param outStream The PrintStream to display the output on
	 * @param bts The BuildTasks object containing the task information
	 * @param fns The FileNameSpaces object containing file name information
	 * @param resultTaskSet The set of tasks to be displayed
	 * @param filterTaskSet Currently unused
	 * @param longOutput Set to true if the full command strings should be displayed.
	 */
	/* package */ static void printTaskSet(
			PrintStream outStream, BuildTasks bts, FileNameSpaces fns,
			TaskSet resultTaskSet, TaskSet filterTaskSet, DisplayWidth outputFormat) {
		
		/* 
		 * We always start at the top root, even though we may only display a subset
		 * of the paths underneath that root.
		 */
		int topRoot = bts.getRootTask("");
	
		/* call the helper function to display each of our children */
		Integer children[] = bts.getChildren(topRoot);
		for (int i = 0; i < children.length; i++) {
			printTaskSetHelper(outStream, bts, fns, children[i], 
					resultTaskSet, filterTaskSet, outputFormat, 1);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the number of columns (characters) of output to be printed per report line.
	 * @return The number of columns 
	 */
	/* package */ static int getColumnWidth() {
		return columnWidth;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set the number of columns (characters) of output to be printed per report line. The
	 * minimum width is 40 characters. Any attempt to set a narrower width will revert to 40.
	 * @param newWidth The new column width to set
	 */
	/* package */ static void setColumnWidth(int newWidth) {
		if (newWidth < 40) {
			newWidth = 40;
		}
		columnWidth = newWidth;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Parses the user-supplied component/section string, and returns the corresponding
	 * component ID and section ID. The string should be in the format "component" or 
	 * "component/section". If "section" is not provided (and sectionAllowed is true),
	 * "private" is assumed. If the input is invalid, display a meaningful error message
	 * and exit the program.
	 * @param compString The user-supplied input string (could be anything)
	 * @param sectionAllowed Is the user allowed to provide a section? 
	 */
	/* package */ static int[] parseComponentAndSection(
			Components cmpts,
			String compString, 
			boolean sectionAllowed) {
	
		String cmptName = null;
		String sectName = null;
		
		/* check if there's a '/' in the string, to separate "component" from "section" */
		int slashIndex = compString.indexOf('/');
		if (slashIndex != -1) {
			cmptName = compString.substring(0, slashIndex);
			sectName = compString.substring(slashIndex + 1);
			if (!sectionAllowed) {
				System.err.println("Error: Invalid syntax - '/" + sectName + "' not allowed.");
				System.exit(1);
			}	
		} 
		
		/* else, there's no /, assume 'private' for the section */
		else {
			cmptName = compString;
			sectName = "private";
		}

		/* compute the IDs */
		int cmptId = cmpts.getComponentId(cmptName);
		int sectId = cmpts.getSectionId(sectName);
		
		if (cmptId == ErrorCode.NOT_FOUND) {
			System.err.println("Error: Unknown component: " + cmptName);
			System.exit(1);
		}
		if (sectId == ErrorCode.NOT_FOUND) {
			System.err.println("Error: Unknown section name: " + sectName);
			System.exit(1);
		}
		
		return new int[]{ cmptId, sectId };
	}

	/*=====================================================================================*
	 * Private methods
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
			PrintStream outStream, StringBuffer pathSoFar, FileNameSpaces fns, Components cmpts, int thisPathId,
			FileSet resultFileSet, FileSet filterFileSet, boolean showRoots, boolean showComps) {

		/* a StringBuffer for forming the component name */
		StringBuffer compString = new StringBuffer();
		
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
		 * If we've been asked to display file components, prepare the string to be printed.
		 */
		if (showComps) {

			/* to start, empty the StringBuffer */
			compString.delete(0, compString.length());
			
			/* fetch the file's component and section */
			Integer compAndSectionId[] = cmpts.getFileComponent(thisPathId);
			if (compAndSectionId == null) {
				compString.append("Invalid file");
			} 
			
			/* if valid, fetch the human-readable names */
			else {
				int compId = compAndSectionId[0];
				int sectId = compAndSectionId[1];

				String compName = cmpts.getComponentName(compId);
				String sectName = cmpts.getSectionName(sectId);
			
				/* if we can't fetch the text name of the component or section... */
				if (compName == null || sectName == null) {
					compString.append("Invalid component");
				}
			
				/* else, both names are valid, append them to the string */
				else {
					compString.append("  (");
					compString.append(compName);
					compString.append('/');
					compString.append(sectName);
					compString.append(")");
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
				
				/* show components, if requested */
				if (showComps) {
					outStream.println(compString);
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
			pathSoFar.append(rootName);
			pathSoFar.append(':');
			
			/* display information about this root. */
			outStream.print(pathSoFar + " (" + fns.getPathName(thisPathId) + ") ");
			
			/* show components, if requested */
			if (showComps) {
				outStream.println(compString);
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
				printFileSetHelper(outStream, pathSoFar, fns, cmpts, children[i], 
						resultFileSet, filterFileSet, showRoots, showComps);
			}
			
			/* remove our base name from the pathSoFar, so our caller sees the correct value again */
			pathSoFar.setLength(pathSoFarLen);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * A helper method, used exclusively by printTaskSet. This method calls itself recursively
	 * as it traverses the TaskSet's tree structure.
	 * @param outStream The PrintStream to display the output on
	 * @param bts The BuildTasks object containing the task information
	 * @param fns The FileNameSpaces object containing file name information
	 * @param resultTaskSet The set of tasks to be displayed
	 * @param filterTaskSet Currently unused
	 * @param longOutput Set to true if the full command strings should be displayed.
	 * @param indentLevel The number of spaces to indent this task by.
	 */
	private static void printTaskSetHelper(PrintStream outStream,
			BuildTasks bts, FileNameSpaces fns, int taskId, TaskSet resultTaskSet,
			TaskSet filterTaskSet, DisplayWidth outputFormat, int indentLevel) {
	
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
		if (! ((resultTaskSet == null) || (resultTaskSet.isMember(taskId)))) {
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
		outStream.println(" Task " + taskId + " (" + taskDirName + ")");
		
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
			printTaskSetHelper(outStream, bts, fns, children[i], 
					resultTaskSet, filterTaskSet, outputFormat, indentLevel + 1);
		}
		
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

