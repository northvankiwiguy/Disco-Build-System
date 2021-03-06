/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/

package com.buildml.model;

import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.types.FileRecord;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.PackageSet;
import com.buildml.model.types.ActionSet;

/**
 * The interface conformed-to by any ReportMgr object, which represents a
 * subset of the functionality managed by a BuildStore object. An ReportMgr
 * object projects various "canned" reports (such as "write-only files" or
 * "unused files".
 * <p>
 * There should be exactly one ReportMgr object per BuildStore object. Use the
 * BuildStore's getReportMgr() method to obtain that one instance.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface IReportMgr {

	/**
	 * Provides an ordered array of the most commonly accessed files across the whole BuildStore.
	 * For each record in the result set, return the number of unique actions that access the file.
	 * 
	 * @return An array of FileRecord, sorted with the most commonly accessed file first.
	 */
	public abstract FileRecord[] reportMostCommonlyAccessedFiles();

	/**
	 * Generate a report to show which files are the most common includers of the specified
	 * file. This provides information on where the specified file is used the most often.
	 * 
	 * @param includedFile ID of the file that is being included.
	 * @return An ordered array of FileRecord objects, containing the output of the report.
	 */
	public abstract FileRecord[] reportMostCommonIncludersOfFile(
			int includedFile);

	/**
	 * Return a FileSet of all files in the BuildStore that aren't accessed by any actions.
	 * This helps identify which source files are never used.
	 * 
	 * @return The set of files that are never accessed.
	 */
	public abstract FileSet reportFilesNeverAccessed();

	/**
	 * Return the set of files (not directories) that match the user-specified file name.
	 * 
	 * @param fileArg The name of the file(s) to match (null is a valid value that matches
	 * 			nothing).
	 * @return The FileSet of matching file names.
	 */
	public abstract FileSet reportFilesThatMatchName(String fileArg);

	/**
	 * Return the set of actions whose command string matches the user-specified pattern.
	 * 
	 * @param pattern The user-specified pattern to match.
	 * @return The ActionSet of matching file names.
	 */
	public abstract ActionSet reportActionsThatMatchName(String pattern);

	/**
	 * Given the input FileSet (sourceFileSet), return a new FileSet containing all the 
	 * paths that are derived from the paths listed in sourceFileSet. A file (A) is derived 
	 * from file (B) if there's an action (or sequence of paths) that reads B and writes to A.
	 * 
	 * @param sourceFileSet The FileSet of all files we should consider as input to the actions.
	 * @param reportIndirect Set if we should also report on indirectly derived files
	 * (with multiple actions between file A and file B).
	 * @return The FileSet of derived paths.
	 */
	public abstract FileSet reportDerivedFiles(FileSet sourceFileSet,
			boolean reportIndirect);

	/**
	 * Given the input FileSet (targetFileSet), return a new FileSet containing all the 
	 * paths that are used as input when generating the paths listed in targetFileSet. 
	 * A file (A) is input for file (B) if there's an action (or sequence of paths) that reads A 
	 * and writes to B. This is the opposite of reportDerivedFiles().
	 * 
	 * @param targetFileSet The FileSet of all files that are the target of actions.
	 * @param reportIndirect Set if we should also report on indirectly derived files
	 * (multiple actions between file A and file B).
	 * @return The FileSet of input paths.
	 */
	public abstract FileSet reportInputFiles(FileSet targetFileSet,
			boolean reportIndirect);

	/**
	 * Given a FileSet, return a ActionSet containing all the actions that access this
	 * collection files. The opType specifies whether we want actions that read these files,
	 * write to these files, or either read or write.
	 * 
	 * @param fileSet The set of input files.
	 * @param opType Either OP_READ, OP_WRITE or OP_UNSPECIFIED.
	 * @return a ActionSet of all actions that access these files in the mode specified.
	 */
	public abstract ActionSet reportActionsThatAccessFiles(FileSet fileSet,
			OperationType opType);

	/**
	 * Given a FileSet, return all the actions that use one of these paths as their
	 * working directory.
	 * 
	 * @param directories The paths to query. These paths should represent directories,
	 * although non-directory paths will be silently ignored.
	 * @return The ActionSet of actions that use one or more of the input paths as their
	 * current working directory.
	 */
	public abstract ActionSet reportActionsInDirectory(FileSet directories);

	/**
	 * Given an ActionSet, return a FileSet containing all the files that are accessed by this
	 * collection of actions. The opType specifies whether we want files that are read by
	 * these actions, written by these actions, or either read or written. 
	 * 
	 * @param actionSet The set of input actions.
	 * @param opType Either OP_READ, OP_WRITE or OP_UNSPECIFIED.
	 * @return a FileSet of all files that are accessed by these actions, in the mode specified.
	 */
	public abstract FileSet reportFilesAccessedByActions(ActionSet actionSet,
			OperationType opType);

	/**
	 * Return the set of files that are written to by a build action, but aren't ever
	 * read by a different action. This generally implies that the file is a "final"
	 * file (such as an executable program, or release package), rather than an intermediate
	 * file (such as an object file).
	 * 
	 * @return The FileSet of write-only files.
	 */
	public abstract FileSet reportWriteOnlyFiles();

	/**
	 * Return the complete set of files in the BuildStore. The allows us to generate
	 * a FileSet containing all known files.
	 * @return The complete set of files in the BuildStore.
	 */
	public abstract FileSet reportAllFiles();

	/**
	 * Return the complete set of actions in the BuildStore. The allows us to generate
	 * an ActionSet containing all known actions.
	 * @return The complete set of actions in the BuildStore.
	 */
	public abstract ActionSet reportAllActions();

	/**
	 * Given a PackageSet, return the complete FileSet of all files that belong to packages
	 * that are members of the set.
	 * 
	 * @param pkgSet The PackageSet that selects the packages to be included.
	 * @return The FileSet of files that are within the selected packages.
	 */
	public abstract FileSet reportFilesFromPackageSet(PackageSet pkgSet);

	/**
	 * Given a PackageSet, return the complete ActionSet of all actions that belong to packages
	 * that are members of the set.
	 * 
	 * @param pkgSet The PackageSet that selects the packages to be included.
	 * @return The ActionSet of actions that are within the selected packages.
	 */
	public abstract ActionSet reportActionsFromPackageSet(PackageSet pkgSet);
}