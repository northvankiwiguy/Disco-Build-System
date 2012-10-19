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
import com.buildml.main.ICliCommand;
import com.buildml.main.CliUtils.DisplayWidth;
import com.buildml.model.IPackageMgr;
import com.buildml.model.impl.BuildStore;
import com.buildml.model.impl.BuildTasks;
import com.buildml.model.impl.FileNameSpaces;
import com.buildml.model.types.TaskSet;

/**
 * BuildML CLI Command class that implements the "show-tasks" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandShowTasks implements ICliCommand {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** Set if we should show package membership when displaying reports. */
	protected static boolean optionShowPkgs = false;

	/** Set if we want short output. */
	protected static boolean optionShort = false;
	
	/** Set if we want long output. */
	protected static boolean optionLong = false;

	/** The output format of the report (ONE_LINE, WRAPPED, NOT_WRAPPED). */
	protected DisplayWidth outputFormat = DisplayWidth.WRAPPED;
	
	/** The TaskSet used to filter our results (if -f/--filter is used). */
	protected TaskSet filterTaskSet = null;
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/show-tasks.txt");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "show-tasks";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getOptions()
	 */
	@Override
	public Options getOptions() {
		
		Options opts = new Options();

		/* add the --show-pkgs option */
		Option showPkgsOpt = new Option("p", "show-pkgs", false, "Show the package of each task.");
		opts.addOption(showPkgsOpt);
		
		/* add the -s/--short option */
		Option shortOpt = new Option("s", "short", false, "Provide abbreviated output.");
		opts.addOption(shortOpt);

		/* add the -l/--long option */
		Option longOpt = new Option("l", "long", false, "Provide detailed/long output.");
		opts.addOption(longOpt);
		
		/* add the -f/--filter option */
		Option filterOpt = new Option("f", "filter", true, "Task-specs used to filter the output.");
		filterOpt.setArgName("task-spec:...");
		opts.addOption(filterOpt);
		
		return opts;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getParameterDescription()
	 */
	@Override
	public String getParameterDescription() {
		return "";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "List all tasks in the build system.";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#processOptions(org.apache.commons.cli.CommandLine)
	 */
	@Override
	public void processOptions(BuildStore buildStore, CommandLine cmdLine) {
		optionShort = cmdLine.hasOption("short");
		optionLong = cmdLine.hasOption("long");
		optionShowPkgs = cmdLine.hasOption("show-pkgs");
		
		/* do we want short or long command output? We can't have both */
		if (optionShort && optionLong) {
			CliUtils.reportErrorAndExit("Can't select --short and --long in the same command.");
		}
		
		outputFormat = optionShort ? DisplayWidth.ONE_LINE :
							optionLong ? DisplayWidth.NOT_WRAPPED : DisplayWidth.WRAPPED;
		
		/* fetch the subset of tasks we should filter-in */
		BuildTasks bts = buildStore.getBuildTasks();
		String filterInString = cmdLine.getOptionValue("f");
		if (filterInString != null) {
			filterTaskSet = CliUtils.getCmdLineTaskSet(bts, filterInString);
			if (filterTaskSet != null) {
				filterTaskSet.populateWithParents();
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#invoke(com.buildml.model.BuildStore, java.lang.String[])
	 */
	@Override
	public void invoke(BuildStore buildStore, String[] args) {

		CliUtils.validateArgs(getName(), args, 0, 0, "No arguments expected.");
		
		BuildTasks bts = buildStore.getBuildTasks();
		FileNameSpaces fns = buildStore.getFileNameSpaces();		
		IPackageMgr pkgMgr = buildStore.getPackageMgr();

		/* 
		 * Display the selected task set.
		 */
		CliUtils.printTaskSet(System.out, bts, fns, pkgMgr, null, filterTaskSet, outputFormat, optionShowPkgs);
	}

	/*-------------------------------------------------------------------------------------*/
}

