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
import com.arapiki.disco.model.BuildTasks;
import com.arapiki.disco.model.Components;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.FileRecord;
import com.arapiki.disco.model.FileSet;
import com.arapiki.disco.model.Reports;
import com.arapiki.disco.model.TaskSet;
import com.arapiki.disco.model.BuildTasks.OperationType;

/**
 *  A helper class for DiscoMain. This class handles the disco commands that report things
 * (trees, builds, etc). These methods should only be called by methods in DiscoMain.
 * @author "Peter Smith <psmith@arapiki.com>"
 */
/* package */ class DiscoReports {

	/*=====================================================================================*
	 * Package-level Methods
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
			boolean showRoots, boolean showComps, String cmdArgs[]) {

		FileNameSpaces fns = buildStore.getFileNameSpaces();
		Components cmpts = buildStore.getComponents();
		
		/* fetch the subset of files we should filter through */
		FileSet filterFileSet = CliUtils.getCmdLineFileSet(fns, cmdArgs, 1);
		if (filterFileSet != null) {
			filterFileSet.populateWithParents();
		}
		
		/* 
		 * There were no search "results", so we'll show everything (except those
		 * that are filtered-out by filterFileSet. 
		 */
		FileSet resultFileSet = null;
		
		/* pretty print the results */
		CliUtils.printFileSet(System.out, fns, cmpts, resultFileSet, filterFileSet, showRoots, showComps);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Display a report of all the files that are used (or read, or written) by a user-specified
	 * set of tasks. 
	 * @param buildStore The BuildStore to query.
	 * @param showRoots Should we display path roots? (--show-roots option)
	 * @param optionRead Should we only show files that were read by these tasks
	 * @param optionWrite Should we only show files that were written by these tasks
	 * @param cmdArgs The user-supplied command argument, providing the list of tasks to query
	 */
	/* package */ static void showFilesUsedBy(BuildStore buildStore,
			boolean showRoots, boolean showComps, 
			boolean optionRead, boolean optionWrite,
			String[] cmdArgs) {
		
		/* are we searching for reads, writes, or both? */
		OperationType opType = getOperationType(optionRead, optionWrite);
		
		BuildTasks bts = buildStore.getBuildTasks();
		Reports reports = buildStore.getReports();
		FileNameSpaces fns = buildStore.getFileNameSpaces();
		Components cmpts = buildStore.getComponents();
		
		/* fetch the list of tasks we're querying */
		TaskSet ts = CliUtils.getCmdLineTaskSet(bts, cmdArgs, 1);
		if (ts == null) {
			System.err.println("Error: no tasks were selected.");
			System.exit(-1);
		}
		ts.populateWithParents();
		
		/* run the report */
		FileSet accessedFiles = reports.reportFilesAccessedByTasks(ts, opType);
		accessedFiles.populateWithParents();
		
		/* pretty print the results */
		CliUtils.printFileSet(System.out, fns, cmpts, accessedFiles, null, showRoots, showComps);
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
			boolean showRoots, boolean showComps, String cmdArgs[]) {

		FileNameSpaces fns = buildStore.getFileNameSpaces();
		Reports reports = buildStore.getReports();
		Components cmpts = buildStore.getComponents();

		/* fetch the file/directory filter so we know which result files to display */
		FileSet filterFileSet = CliUtils.getCmdLineFileSet(fns, cmdArgs, 1);
		if (filterFileSet != null) {
			filterFileSet.populateWithParents();
		}

		/* get list of unused files, and add their parent paths */
		FileSet unusedFileSet = reports.reportFilesNeverAccessed();
		unusedFileSet.populateWithParents();
		
		/* pretty print the results */
		CliUtils.printFileSet(System.out, fns, cmpts, unusedFileSet, filterFileSet, showRoots, showComps);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Provide a list of all files in the BuildStore that are written-to by a task, but
	 * there are no other tasks that read from this file. This implies that the file
	 * is "final", such as an executable program or release package, rather than an
	 * intermediate file (such as an object file).
	 * @param buildStore The BuildStore to query.
	 * @param showRoots True if file system roots (e.g. "root:") should be shown
	 * @param cmdArgs The user-supplied list of files/directories to be displayed. Only unused
	 * files that match this filter will be displayed. Note that cmdArgs[0] is the
	 * name of the command (show-files) are will be ignored.
	 */
	/* package */ static void showWriteOnlyFiles(BuildStore buildStore, 
			boolean showRoots, boolean showComps, String cmdArgs[]) {

		FileNameSpaces fns = buildStore.getFileNameSpaces();
		Reports reports = buildStore.getReports();
		Components cmpts = buildStore.getComponents();

		/* fetch the file/directory filter so we know which result files to display */
		FileSet filterFileSet = CliUtils.getCmdLineFileSet(fns, cmdArgs, 1);
		if (filterFileSet != null) {
			filterFileSet.populateWithParents();
		}

		/* get list of write-only files, and add their parent paths */
		FileSet writeOnlyFileSet = reports.reportWriteOnlyFiles();
		writeOnlyFileSet.populateWithParents();
		
		/* pretty print the results */
		CliUtils.printFileSet(System.out, fns, cmpts, writeOnlyFileSet, filterFileSet, showRoots, showComps);
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
	/* package */ static void showDerivedFiles(BuildStore buildStore,
			boolean showRoots, boolean showComps, boolean showAll, String[] cmdArgs) {
		
		FileNameSpaces fns = buildStore.getFileNameSpaces();
		Reports reports = buildStore.getReports();
		Components cmpts = buildStore.getComponents();

		/* fetch the list of files that are the source of the derivation */
		FileSet sourceFileSet = CliUtils.getCmdLineFileSet(fns, cmdArgs, 1);
		if (sourceFileSet != null) {
			sourceFileSet.populateWithParents();
		}

		/* get list of derived files, and add their parent paths */
		FileSet derivedFileSet = reports.reportDerivedFiles(sourceFileSet, showAll);
		derivedFileSet.populateWithParents();
		
		/* pretty print the results - no filtering used here */
		CliUtils.printFileSet(System.out, fns, cmpts, derivedFileSet, null, showRoots, showComps);	
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Display a list of files, ordered by the number of tasks make use of that file.
	 * The result set can be filtered based on the input file set.
	 * @param buildStore The BuildStore to query.
	 * @param showRoots True if file system roots (e.g. "root:") should be shown
	 * @param cmdArgs The user-supplied list of files/directories to use as a filter. Only
	 * files that match this filter will be displayed. Note that cmdArgs[0] is the
	 * name of the command (show-files) are will be ignored.
	 */
	/* package */ static void showPopularFiles(BuildStore buildStore,
			boolean showRoots, boolean showComps, String[] cmdArgs) {
		
		FileNameSpaces fns = buildStore.getFileNameSpaces();
		Reports reports = buildStore.getReports();

		/* fetch the set of files we're interested in learning about */
		FileSet filterFileSet = CliUtils.getCmdLineFileSet(fns, cmdArgs, 1);
		if (filterFileSet != null) {
			filterFileSet.populateWithParents();
		}

		/* fetch the list of most popular files */
		FileRecord results[] = reports.reportMostCommonlyAccessedFiles();
		
		/* pretty print the results - only show files if they're in the filter set */
		for (FileRecord fileRecord : results) {
			int id = fileRecord.getId();
			if ((filterFileSet == null) || (filterFileSet.isMember(id))){
				int count = fileRecord.getCount();
				String pathName = fns.getPathName(id, showRoots);
				System.out.println(count + "\t" + pathName);
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Show a number of tasks from the BuildStore. By default, all tasks will be shown in
	 * a tree hierarchy. If user-supplied filter parameters are provided, the set of tasks
	 * will be filtered accordingly.
	 * @param buildStore The BuildStore to query
	 * @param longOutput Should command strings be shown in full?
	 * @param cmdArgs The user-supplied list of tasks to be displayed. Only tasks 
	 * that match this filter will be displayed. Note that cmdArgs[0] is the
	 * name of the command (show-tasks) are will be ignored.
	 */
	/* package */ static void showTasks(BuildStore buildStore, 
			CliUtils.DisplayWidth outputFormat, boolean showComps, String[] cmdArgs) {
		
		BuildTasks bts = buildStore.getBuildTasks();
		FileNameSpaces fns = buildStore.getFileNameSpaces();		
		Components cmpts = buildStore.getComponents();
		
		/* compute a TaskSet to display, or null if no arguments are provided */
		TaskSet ts = CliUtils.getCmdLineTaskSet(bts, cmdArgs, 1);
		if (ts != null) {
			ts.populateWithParents();
		}
		
		/* 
		 * Display the selected task set.
		 */
		CliUtils.printTaskSet(System.out, bts, fns, cmpts, ts, null, outputFormat, showComps);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Display all build tasks that access (read, write or use) any of the user-specified
	 * files.
	 * @param buildStore The BuildStore to query
	 * @param optionRead Only show tasks that read the files
	 * @param optionWrite Only show tasks that read the files
	 * @param longOutput Should command strings be shown in full?
	 * @param cmdArgs The user-supplied list of files/directories to query. 
	 * Note that cmdArgs[0] is the name of the command (show-files) are will be ignored.
	 */
	/* package */ static void showTasksThatAccess(BuildStore buildStore,
			boolean optionRead, boolean optionWrite, 
			CliUtils.DisplayWidth outputFormat, boolean showComps, String[] cmdArgs) {
		
		FileNameSpaces fns = buildStore.getFileNameSpaces();
		BuildTasks bts = buildStore.getBuildTasks();
		Reports reports = buildStore.getReports();
		Components cmpts = buildStore.getComponents();

		/* are we searching for reads, writes, or both? */
		OperationType opType = getOperationType(optionRead, optionWrite);		

		/* fetch the FileSet of paths from the user's command line */
		FileSet fileSet = CliUtils.getCmdLineFileSet(fns, cmdArgs, 1);
		if (fileSet != null) {
			fileSet.populateWithParents();
		}
		
		/* find all tasks that access (read, write or both) these files */
		TaskSet taskSet = reports.reportTasksThatAccessFiles(fileSet, opType);
		taskSet.populateWithParents();
		
		/* display the resulting set of tasks */
		CliUtils.printTaskSet(System.out, bts, fns, cmpts, taskSet, null, outputFormat, showComps);
	}
	
	/*=====================================================================================*
	 * Private Methods
	 *=====================================================================================*/

	/**
	 * Given the possible use of the --read and --write command line flags, return an
	 * OperationType value that can be used for querying the database
	 * @param optionRead Whether the user provided the --read flag
	 * @param optionWrite Whether the user provided the --write flag
	 * @return Either OP_UNSPECIFIED (search for either), OP_READ, or OP_WRITE
	 */
	private static OperationType getOperationType(boolean optionRead,
			boolean optionWrite) {
		
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
		return opType;
	}

	/*-------------------------------------------------------------------------------------*/
}
