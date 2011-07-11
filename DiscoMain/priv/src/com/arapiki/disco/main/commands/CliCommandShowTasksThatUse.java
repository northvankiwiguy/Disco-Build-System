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

package com.arapiki.disco.main.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.arapiki.disco.main.CliUtils;
import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.BuildTasks;
import com.arapiki.disco.model.Components;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.FileSet;
import com.arapiki.disco.model.Reports;
import com.arapiki.disco.model.TaskSet;
import com.arapiki.disco.model.BuildTasks.OperationType;

/**
 * Disco CLI Command class that implements the "show-tasks-that-use" command. See the 
 * getLongDescription() method for details of this command's features.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandShowTasksThatUse extends CliCommandShowTasks {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/* should we only show files that are "read" - set by the --read option */
	protected static boolean optionRead = false;
	
	/* should we only show files that are "written" - set by the --write option */
	protected static boolean optionWrite = false;

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowTasks#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		// TODO Add a description
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowTasks#getName()
	 */
	@Override
	public String getName() {
		return "show-tasks-that-use";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowTasks#getOptions()
	 */
	@Override
	public Options getOptions() {
		
		/* use all the options from show-tasks */
		Options opts = super.getOptions();
		
		/* add the --read option */
		Option readOpt = new Option("r", "read", false, "Only show tasks that read the specified files");
		opts.addOption(readOpt);

		/* add the --write option */
		Option writeOpt = new Option("w", "write", false, "Only show tasks that write the specified files");
		opts.addOption(writeOpt);	
		
		return opts;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowTasks#getParameterDescription()
	 */
	@Override
	public String getParameterDescription() {
		return "<path-spec>:...";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowTasks#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "List all tasks that use (read and/or write) the specified file(s)";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowTasks#processOptions(org.apache.commons.cli.CommandLine)
	 */
	@Override
	public void processOptions(BuildStore buildStore, CommandLine cmdLine) {
		
		/* process the standard show-tasks options */
		super.processOptions(buildStore, cmdLine);
		
		/* now the specific options for show-files-used-by */
		optionRead = cmdLine.hasOption("read");
		optionWrite = cmdLine.hasOption("write");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowTasks#invoke(com.arapiki.disco.model.BuildStore, java.lang.String[])
	 */
	@Override
	public void invoke(BuildStore buildStore, String[] args) {

		CliUtils.validateArgs(getName(), args, 1, 1, "A colon-separated list of path-specs must be provided");

		FileNameSpaces fns = buildStore.getFileNameSpaces();
		BuildTasks bts = buildStore.getBuildTasks();
		Reports reports = buildStore.getReports();
		Components cmpts = buildStore.getComponents();

		/* are we searching for reads, writes, or both? */
		OperationType opType = CliUtils.getOperationType(optionRead, optionWrite);		

		/* fetch the FileSet of paths from the user's command line */
		String fileSpecs = args[0];
		FileSet fileSet = CliUtils.getCmdLineFileSet(fns, fileSpecs);
		if (fileSet != null) {
			fileSet.populateWithParents();
		}

		/* find all tasks that access (read, write or both) these files */
		TaskSet taskSet = reports.reportTasksThatAccessFiles(fileSet, opType);
		taskSet.populateWithParents();

		/* display the resulting set of tasks */
		CliUtils.printTaskSet(System.out, bts, fns, cmpts, taskSet, null, outputFormat, optionShowComps);
	}

	/*-------------------------------------------------------------------------------------*/
}
