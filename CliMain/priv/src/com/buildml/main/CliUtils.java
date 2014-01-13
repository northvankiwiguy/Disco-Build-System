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

package com.buildml.main;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.apache.commons.io.IOUtils;

import com.buildml.model.IActionMgr;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMemberMgr.PackageDesc;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageRootMgr;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.ActionSet;
import com.buildml.utils.errors.ErrorCode;
import com.buildml.utils.print.PrintUtils;
import com.buildml.utils.string.ShellCommandUtils;

/**
 * A collection of utility methods that can be used by any CLI Command code. This
 * includes error reporting, command argument parsing, printing a FileSet and printing
 * a ActionSet. These methods are all static, so no object is required for them to be
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

	/** Enumeration for specifying how a action's command string should be displayed. */
	public enum DisplayWidth { 
		
		/** 
		 * As much as possible of the action's command line should be displayed on one line 
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
	
	/**
	 * The number of characters to allow when printing the package details.
	 */
	public static final int PACKAGE_NAME_WIDTH = 25;
	
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
	 *    <li>A path name starting with a "@root" - the same rules apply as for #1.</li>
	 *    <li>A single file name, with one or more wildcard (*) characters. All files that match
     *       the name are added, no matter what their directory.</li>
     *    <li>A package spec, starting with %pkg, or the complement of a package, starting 
     *       with %not-pkg.</li>
     *    </ol>
     *    
	 * @param fileMgr The FileMgr object that manages the files.
	 * @param pathSpecs A String of ":"-separated path specs (files, directories, or regular expressions).
	 * @return A FileSet containing all the files that were selected by the command-line arguments.
	 */
	public static FileSet getCmdLineFileSet(IFileMgr fileMgr, String pathSpecs) {
	
		String pathSpecList[] = pathSpecs.split(":");
		
		/* else populate a new FileSet */
		FileSet result = new FileSet(fileMgr);
		if (result.populateWithPaths(pathSpecList) != ErrorCode.OK) {
			CliUtils.reportErrorAndExit("Invalid path filter provided");
		}

		return result;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a user-supplied set of command line arguments, parse those arguments and create
	 * a suitable ActionSet containing all the relevant actions that match the specification. The
	 * specification string is a colon-separated list of:
	 * <ol>
	 * 	 <li>A specific action number, which will be added to the ActionSet.</li>
	 *   <li>The action number followed by [/depth] to indicate that all actions in the sub tree,
	 *      starting at the specified action and moving down the action tree "depth" level, should
	 *      be added.</li>
	 *   <li>If 'depth' is omitted (only the '/' is provided), all actions is the subtree are added
	 *      (regardless of their depth).</li>
	 *   <li>If the action number is prefixed by '-', the actions are removed from the ActionSet, rather
	 *      than being added. The "/depth" and "/" suffix can be used to remove subactions as well.
	 *   <li>The special syntax "%pkg/foo" means all actions in the package "foo".</li>
	 *   <li>The special syntax "%not-pkg/foo" means all actions outside the package "foo".</li>
	 * </ol>
	 * @param actionMgr The ActionMgr object to query.
	 * @param actionSpecs The command line argument providing the action specification string.
	 * @return The ActionSet, as described by the input action specification.
	 */
	public static ActionSet getCmdLineActionSet(IActionMgr actionMgr, String actionSpecs) {
		
		String actionSpecList[] = actionSpecs.split(":");

		ActionSet result = new ActionSet(actionMgr);
		if (result.populateWithActions(actionSpecList) != ErrorCode.OK) {
			System.err.println("Error: Invalid action filter provided.");
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
	 * @param buildStore The BuildStore object containing the files to be listed.
	 * @param resultFileSet The set of files to be displayed (if null, show them all).
	 * @param filterFileSet If not-null, used to filter which paths from resultFileSet should be
	 *         displayed (set to null to display everything).
	 * @param showRoots Indicates whether path roots should be displayed.
	 * @param showPkgs Indicates whether the package names should be displayed.
	 */
	public static void printFileSet(
			PrintStream outStream, IBuildStore buildStore, FileSet resultFileSet,
			FileSet filterFileSet, boolean showRoots, boolean showPkgs) {
		
		IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
		
		/*
		 * This method uses recursion to traverse the VFS from the root to the leaves 
		 * of the tree. It maintains a StringBuilder with the path encountered so far. 
		 * That is, if the StringBuilder contains "/a/b/c" and the path "d" is encountered, 
		 * then append "/d" to the StringBuilder to get "/a/b/c/d". Once we've finished 
		 * traversing directory "d", we pop it off the StringBuilder and return to 
		 * "/a/b/c/". This allows us to do a depth-first traversal of the VFS tree 
		 * without doing more database access than we need to.
		 * 
		 * The resultFileSet and filterFileSet work together to determine which paths are
		 * to be displayed. resultFileSet contains all the files from the relevant database
		 * query. On the other hand, filterFileSet is the list of files that have been
		 * selected by the user's command line argument (e.g. selecting a subdirectory, or
		 * selecting files that match a pattern, such as *.c).
		 */		
		StringBuffer sb = new StringBuffer();					
		
		/* call the helper function to display each of our children */
		printFileSetHelper(outStream, sb, buildStore, pkgRootMgr.getRootPath("root"), 
				resultFileSet, filterFileSet, showRoots, showPkgs);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a ActionSet, display the actions in that set in a pretty-printed format. To ensure
	 * that all actions are displayed, you should first call ActionSet.populateWithParents().
	 * 
	 * @param outStream The PrintStream on which to display the output.
	 * @param buildStore The database containing the action information.
	 * @param resultActionSet The set of actions to be displayed (the results of some previous query).
	 * @param filterActionSet The set of actions to actually be displayed (for post-filtering the query results).
	 * @param outputFormat Mode for formatting the command strings.
	 * @param showPkgs Set to true if the package names should be shown.
	 */
	public static void printActionSet(
			PrintStream outStream, IBuildStore buildStore,
			ActionSet resultActionSet, ActionSet filterActionSet, DisplayWidth outputFormat,
			boolean showPkgs) {
		
		IActionMgr actionMgr = buildStore.getActionMgr();
		
		/* 
		 * We always start at the top root, even though we may only display a subset
		 * of the paths underneath that root.
		 */
		int topRoot = actionMgr.getRootAction("");
	
		/* call the helper function to display each of our children */
		Integer children[] = actionMgr.getChildren(topRoot);
		for (int i = 0; i < children.length; i++) {
			printActionSetHelper(outStream, buildStore, children[i], 
					resultActionSet, filterActionSet, outputFormat, showPkgs, 1);
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
	 * @param buildStore The IBuildStore containing the package information.
	 * @param pkgString The user-supplied input string (could be anything).
	 * @param scopeAllowed True if the input string is allowed to provide a scope name. 
	 * @return An array of two integers. The first is the package's ID number,
	 * and the second is the scope's ID number.
	 */
	public static int[] parsePackageAndScope(
			IBuildStore buildStore,
			String pkgString, 
			boolean scopeAllowed) {
	
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		IPackageMemberMgr pkgMemberMgr = buildStore.getPackageMemberMgr();
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
		int pkgId = pkgMgr.getId(pkgName);
		int scopeId = pkgMemberMgr.getScopeId(scopeName);
		
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
	 * @param buildStore The BuildStore in which these paths belong.
	 * @param thisPathId The path to display (assuming it's in the filesToShow FileSet).
	 * @param resultFileSet The set of files to be displayed (if null, show them all).
	 * @param filterFileSet If not-null, used to filter which paths from resultFileSet
	 * 		   should be displayed (set to null to display everything).
	 * @param showRoots Whether to show path roots.
	 * @param showPkgs Whether to show the package names.
	 */
	private static void printFileSetHelper(
			PrintStream outStream, StringBuffer pathSoFar, IBuildStore buildStore, 
			int thisPathId, FileSet resultFileSet, FileSet filterFileSet, boolean showRoots, 
			boolean showPkgs) {

		IFileMgr fileMgr = buildStore.getFileMgr();
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		IPackageMemberMgr pkgMemberMgr = buildStore.getPackageMemberMgr();
		IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
		
		/* StringBuilders for forming the package name and the root names */
		StringBuilder pkgString = null;
		StringBuilder rootString = null;

		/* should this path be displayed? */
		if (!shouldBeDisplayed(thisPathId, resultFileSet, filterFileSet)){
			return;
		}	

		/* fetch this path's name */
		String baseName = fileMgr.getBaseName(thisPathId);

		/* get this path's list of children */
		Integer children[] = fileMgr.getChildPaths(thisPathId);
		
		/*
		 * Figure out whether this path has attached roots.
		 */
		String rootNames[] = null;
		if (showRoots) {
			rootNames = pkgRootMgr.getRootsAtPath(thisPathId);
		}
		
		/* 
		 * If we've been asked to display file packages, prepare the string to be printed.
		 */
		if (showPkgs) {
			
			pkgString = new StringBuilder();
			
			/* fetch the file's package and scope */
			PackageDesc pkgAndScopeId = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_FILE, thisPathId);
			if (pkgAndScopeId == null) {
				pkgString.append("Invalid file");
			} 
			
			/* if valid, fetch the human-readable names */
			else {
				String pkgName = pkgMgr.getName(pkgAndScopeId.pkgId);
				String scopeName = pkgMemberMgr.getScopeName(pkgAndScopeId.pkgScopeId);
			
				/* if we can't fetch the text name of the package or scope... */
				if (pkgName == null || scopeName == null) {
					pkgString.append("Invalid package");
				}
			
				/* else, both names are valid, append them to the string */
				else {
					pkgString.append(pkgName);
					pkgString.append(" - ");
					pkgString.append(scopeName);
				}
			}
		}
		
		/* 
		 * Does this path have a root (and we were asked to show roots)?
		 * If so, prepare the string to be printed.
		 */
		if ((rootNames != null) && (rootNames.length > 0)) {
			rootString = new StringBuilder();
			
			/* display a root name, or comma-separated root names */
			rootString.append(" (");
			for (int i = 0; i < rootNames.length; i++) {
				if (i != 0) {
					rootString.append(' ');
				}
				rootString.append('@');
				rootString.append(rootNames[i]);
			}
			rootString.append(')');
		}
		
		/* show packages, if requested. Truncate to a fixed column width. */
		if (pkgString != null) {
			if (pkgString.length() > PACKAGE_NAME_WIDTH - 1) {
				pkgString.setLength(PACKAGE_NAME_WIDTH - 1);
			}
			outStream.print(pkgString);
			PrintUtils.indent(outStream, PACKAGE_NAME_WIDTH - pkgString.length());
		}
		
		/* Display this path, prefixed by the absolute pathSoFar */
		outStream.print(pathSoFar);		
		outStream.print(baseName);
		
		/* show roots, if requested */
		if (rootString != null) {
			outStream.print(rootString);
		}
		outStream.println();
		
		/* if there are children, call ourselves recursively to display them */
		if (children.length != 0) {
			
			/* append this path onto the pathSoFar, since it'll become the pathSoFar for each child */
			int pathSoFarLen = pathSoFar.length();
			pathSoFar.append(baseName);
			if (baseName.charAt(0) != '/') {
				pathSoFar.append('/');
			}
			
			/* display each of the children */
			for (int i = 0; i < children.length; i++) {
				printFileSetHelper(outStream, pathSoFar, buildStore, children[i], 
						resultFileSet, filterFileSet, showRoots, showPkgs);
			}
			
			/* remove our base name from the pathSoFar, so our caller sees the correct value again */
			pathSoFar.setLength(pathSoFarLen);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * A helper method, called exclusively by printActionSet(). This method calls itself recursively
	 * as it traverses the ActionSet's tree structure.
	 * 
	 * @param outStream The PrintStream on which to display the output.
	 * @param buildStore The database containing file, action and package information.
	 * @param actionId The ID of the action we're currently displaying (at this level of recursion).
	 * @param resultActionSet The full set of actions to be displayed (the result of some previous query).
	 * @param filterActionSet The set of actions to actually be displayed (for post-filtering the query results).
	 * @param outputFormat The way in which the actions should be formatted.
	 * @param showPkgs Set to true if we should display package names.
	 * @param indentLevel The number of spaces to indent this action by (at this recursion level).
	 */
	private static void printActionSetHelper(PrintStream outStream, IBuildStore buildStore, 
			int actionId, ActionSet resultActionSet, ActionSet filterActionSet, 
			DisplayWidth outputFormat, boolean showPkgs, int indentLevel) {
	
		IActionMgr actionMgr = buildStore.getActionMgr();
		IFileMgr fileMgr = buildStore.getFileMgr();
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		IPackageMemberMgr pkgMemberMgr = buildStore.getPackageMemberMgr();
		
		/* 
		 * Display the current action, at the appropriate indentation level. The format is:
		 * 
		 * - Action 1 (/home/psmith/t/cvs-1.11.23)
	     *     if test ! -f config.h; then rm -f stamp-h1; emake  stamp-h1; else :;
	     *     
	     * -- Action 2 (/home/psmith/t/cvs-1.11.23)
	     *      failcom='exit 1'; for f in x $MAKEFLAGS; do case $f in *=* | --[!k]*);; \
	     *
	     * Where Action 1 is the parent of Action 2.
	     */
		
		/* is this action in the ActionSet to be printed? If not, terminate recursion */
		if (! (((resultActionSet == null) || (resultActionSet.isMember(actionId))) &&
			((filterActionSet == null) || (filterActionSet.isMember(actionId))))) {
			return;
		}	
	
		/* 
		 * Fetch the action's command string (if there is one). It can either be
		 * in short format (on a single line), or a full string (possibly multiple lines)
		 */
		String command = (String) actionMgr.getSlotValue(actionId, IActionMgr.COMMAND_SLOT_ID);
		if (command == null) {
			command = "<unknown command>";
		}
		else if (outputFormat == DisplayWidth.ONE_LINE) {
			command = ShellCommandUtils.getCommandSummary(command, getColumnWidth() - indentLevel - 3);
		}
		
		/* fetch the name of the directory the action was executed in */
		int actionDirId = (Integer) actionMgr.getSlotValue(actionId, IActionMgr.DIRECTORY_SLOT_ID);
		String actionDirName = fileMgr.getPathName(actionDirId);
		
		/* display the correct number of "-" characters */
		for (int i = 0; i != indentLevel; i++) {
			outStream.append('-');
		}
		outStream.print(" Action " + actionId + " (" + actionDirName);
		
		/* if requested, display the action's package name */
		if (showPkgs) {
			PackageDesc pkg = pkgMemberMgr.getPackageOfMember(IPackageMemberMgr.TYPE_ACTION, actionId);
			if (pkg == null) {
				outStream.print(" - Invalid action");
			} else {
				String pkgName = pkgMgr.getName(pkg.pkgId);
				if (pkgName == null) {
					outStream.print(" - Invalid package");
				} else {
					outStream.print(" - " + pkgName);					
				}
			}
		}
		outStream.println(")");
		
		/* display the action's command string. Each line must be indented appropriately */
		if (outputFormat != DisplayWidth.NOT_WRAPPED) {
			PrintUtils.indentAndWrap(outStream, command, indentLevel + 3, getColumnWidth());
			outStream.println();
		} else {
			outStream.println(command);
		}
		
		/* recursively call ourselves to display each of our children */
		Integer children[] = actionMgr.getChildren(actionId);
		for (int i = 0; i < children.length; i++) {
			printActionSetHelper(outStream, buildStore, children[i], 
					resultActionSet, filterActionSet, outputFormat, showPkgs, indentLevel + 1);
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

