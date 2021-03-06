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

package com.buildml.main.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.buildml.main.CliUtils;
import com.buildml.main.ICliCommand;
import com.buildml.main.CliUtils.DisplayWidth;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.types.ActionSet;

/**
 * BuildML CLI Command class that implements the "show-actions" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandShowActions implements ICliCommand {

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
	
	/** The ActionSet used to filter our results (if -f/--filter is used). */
	protected ActionSet filterActionSet = null;
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/show-actions.txt");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "show-actions";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getOptions()
	 */
	@Override
	public Options getOptions() {
		
		Options opts = new Options();

		/* add the --show-pkgs option */
		Option showPkgsOpt = new Option("p", "show-pkgs", false, "Show the package of each action.");
		opts.addOption(showPkgsOpt);
		
		/* add the -s/--short option */
		Option shortOpt = new Option("s", "short", false, "Provide abbreviated output.");
		opts.addOption(shortOpt);

		/* add the -l/--long option */
		Option longOpt = new Option("l", "long", false, "Provide detailed/long output.");
		opts.addOption(longOpt);
		
		/* add the -f/--filter option */
		Option filterOpt = new Option("f", "filter", true, "Action-specs used to filter the output.");
		filterOpt.setArgName("action-spec:...");
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
		return "List all actions in the build system.";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#processOptions(org.apache.commons.cli.CommandLine)
	 */
	@Override
	public void processOptions(IBuildStore buildStore, CommandLine cmdLine) {
		optionShort = cmdLine.hasOption("short");
		optionLong = cmdLine.hasOption("long");
		optionShowPkgs = cmdLine.hasOption("show-pkgs");
		
		/* do we want short or long command output? We can't have both */
		if (optionShort && optionLong) {
			CliUtils.reportErrorAndExit("Can't select --short and --long in the same command.");
		}
		
		outputFormat = optionShort ? DisplayWidth.ONE_LINE :
							optionLong ? DisplayWidth.NOT_WRAPPED : DisplayWidth.WRAPPED;
		
		/* fetch the subset of actions we should filter-in */
		IActionMgr actionMgr = buildStore.getActionMgr();
		String filterInString = cmdLine.getOptionValue("f");
		if (filterInString != null) {
			filterActionSet = CliUtils.getCmdLineActionSet(actionMgr, filterInString);
			if (filterActionSet != null) {
				filterActionSet.populateWithParents();
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#invoke(com.buildml.model.BuildStore, java.lang.String[])
	 */
	@Override
	public void invoke(IBuildStore buildStore, String buildStorePath, String[] args) {

		CliUtils.validateArgs(getName(), args, 0, 0, "No arguments expected.");

		/* 
		 * Display the selected action set.
		 */
		CliUtils.printActionSet(System.out, buildStore, null, 
									filterActionSet, outputFormat, optionShowPkgs);
	}

	/*-------------------------------------------------------------------------------------*/
}

