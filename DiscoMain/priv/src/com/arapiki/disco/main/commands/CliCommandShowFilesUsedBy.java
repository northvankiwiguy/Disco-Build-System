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
import com.buildml.model.BuildStore;
import com.buildml.model.BuildTasks;
import com.buildml.model.Components;
import com.buildml.model.FileNameSpaces;
import com.buildml.model.Reports;
import com.buildml.model.BuildTasks.OperationType;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.TaskSet;

/**
 * Disco CLI Command class that implements the "show-files-used-by" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandShowFilesUsedBy extends CliCommandShowFiles {

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
	 * @see com.arapiki.disco.main.commands.CliCommandShowFiles#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/show-files-used-by.txt");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowFiles#getName()
	 */
	@Override
	public String getName() {
		return "show-files-used-by";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowFiles#getOptions()
	 */
	@Override
	public Options getOptions() {
		Options opts = super.getOptions();
		
		/* add the --read option */
		Option readOpt = new Option("r", "read", false, "Only show files that are read by these tasks.");
		opts.addOption(readOpt);

		/* add the --write option */
		Option writeOpt = new Option("w", "write", false, "Only show files that are written by these tasks.");
		opts.addOption(writeOpt);	
		
		/* add the --modify option */
		Option modifyOpt = new Option("m", "modify", false, "Only show files that are modified by these tasks.");
		opts.addOption(modifyOpt);
		
		/* add the --delete option */
		Option deleteOpt = new Option("d", "delete", false, "Only show files that are deleted by these tasks.");
		opts.addOption(deleteOpt);
		return opts;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowFiles#getParameterDescription()
	 */
	@Override
	public String getParameterDescription() {
		return "<task-spec>:...";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowFiles#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Report on files that are used by the specified tasks.";
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowFiles#processOptions(org.apache.commons.cli.CommandLine)
	 */
	@Override
	public void processOptions(BuildStore buildStore, CommandLine cmdLine) {
		
		/* first, process options for the show-files class */
		super.processOptions(buildStore, cmdLine);
		
		/* now the specific options for show-files-used-by */
		optionRead = cmdLine.hasOption("read");
		optionWrite = cmdLine.hasOption("write");
		optionModify = cmdLine.hasOption("modify");
		optionDelete = cmdLine.hasOption("delete");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowFiles#invoke(com.buildml.model.BuildStore, java.lang.String[])
	 */
	@Override
	public void invoke(BuildStore buildStore, String[] args) {
		
		CliUtils.validateArgs(getName(), args, 1, 1, "A colon-separated list of task-specs must be provided.");

		/* are we searching for reads, writes, or both? */
		OperationType opType = CliUtils.getOperationType(optionRead, optionWrite, optionModify, optionDelete);

		BuildTasks bts = buildStore.getBuildTasks();
		Reports reports = buildStore.getReports();
		FileNameSpaces fns = buildStore.getFileNameSpaces();
		Components cmpts = buildStore.getComponents();

		/* fetch the list of tasks we're querying */
		String taskSpecs = args[0];
		TaskSet ts = CliUtils.getCmdLineTaskSet(bts, taskSpecs);
		if (ts == null) {
			CliUtils.reportErrorAndExit("no tasks were selected.");
		}

		/* run the report */
		FileSet accessedFiles = reports.reportFilesAccessedByTasks(ts, opType);
		accessedFiles.populateWithParents();

		/* pretty print the results */
		CliUtils.printFileSet(System.out, fns, cmpts, accessedFiles, filterFileSet, optionShowRoots, optionShowComps);
	}

	/*-------------------------------------------------------------------------------------*/
}
