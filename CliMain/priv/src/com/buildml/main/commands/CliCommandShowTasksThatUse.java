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

package com.buildml.main.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.buildml.main.CliUtils;
import com.buildml.model.IActionMgr;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IReportMgr;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.TaskSet;

/**
 * BuildML CLI Command class that implements the "show-tasks-that-use" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandShowTasksThatUse extends CliCommandShowTasks {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** Set if we should only show files that are "read" - set by the --read option. */
	protected static boolean optionRead = false;
	
	/** Set if we should only show files that are "written" - set by the --write option. */
	protected static boolean optionWrite = false;

	/** Set if we should only show files that are "modified" - set by the --modify option. */
	protected static boolean optionModify = false;
	
	/** Set if we should only show files that are "deleted" - set by the --delete option. */
	protected static boolean optionDelete = false;
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.main.commands.CliCommandShowTasks#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/show-tasks-that-use.txt");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.commands.CliCommandShowTasks#getName()
	 */
	@Override
	public String getName() {
		return "show-tasks-that-use";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.commands.CliCommandShowTasks#getOptions()
	 */
	@Override
	public Options getOptions() {
		
		/* use all the options from show-tasks */
		Options opts = super.getOptions();
		
		/* add the --read option */
		Option readOpt = new Option("r", "read", false, "Only show tasks that read the specified files.");
		opts.addOption(readOpt);

		/* add the --write option */
		Option writeOpt = new Option("w", "write", false, "Only show tasks that write the specified files.");
		opts.addOption(writeOpt);	
		
		/* add the --modify option */
		Option modifyOpt = new Option("m", "modify", false, "Only show tasks that modify the specified files.");
		opts.addOption(modifyOpt);
		
		/* add the --delete option */
		Option deleteOpt = new Option("d", "delete", false, "Only show tasks that delete the specified files.");
		opts.addOption(deleteOpt);
		
		return opts;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.commands.CliCommandShowTasks#getParameterDescription()
	 */
	@Override
	public String getParameterDescription() {
		return "<path-spec>:...";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.commands.CliCommandShowTasks#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "List all tasks that access (read, write, modify or delete) the specified files.";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.commands.CliCommandShowTasks#processOptions(org.apache.commons.cli.CommandLine)
	 */
	@Override
	public void processOptions(IBuildStore buildStore, CommandLine cmdLine) {
		
		/* process the standard show-tasks options */
		super.processOptions(buildStore, cmdLine);
		
		/* now the specific options for show-files-used-by */
		optionRead = cmdLine.hasOption("read");
		optionWrite = cmdLine.hasOption("write");
		optionModify = cmdLine.hasOption("modify");
		optionDelete = cmdLine.hasOption("delete");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.commands.CliCommandShowTasks#invoke(com.buildml.model.BuildStore, java.lang.String[])
	 */
	@Override
	public void invoke(IBuildStore buildStore, String[] args) {

		CliUtils.validateArgs(getName(), args, 1, 1, "A colon-separated list of path-specs must be provided.");

		IFileMgr fileMgr = buildStore.getFileMgr();
		IActionMgr actionMgr = buildStore.getActionMgr();
		IReportMgr reportMgr = buildStore.getReportMgr();
		IPackageMgr pkgMgr = buildStore.getPackageMgr();

		/* are we searching for reads, writes, or both? */
		OperationType opType = CliUtils.getOperationType(optionRead, optionWrite, optionModify, optionDelete);		

		/* fetch the FileSet of paths from the user's command line */
		String fileSpecs = args[0];
		FileSet fileSet = CliUtils.getCmdLineFileSet(fileMgr, fileSpecs);

		/* find all tasks that access (read, write or both) these files */
		TaskSet taskSet = reportMgr.reportTasksThatAccessFiles(fileSet, opType);
		taskSet.populateWithParents();

		/* display the resulting set of tasks */
		CliUtils.printTaskSet(System.out, actionMgr, fileMgr, pkgMgr, taskSet, filterTaskSet, outputFormat, optionShowPkgs);
	}

	/*-------------------------------------------------------------------------------------*/
}
