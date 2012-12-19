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
import com.buildml.model.IActionMgr;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.IBuildStore;
import com.buildml.model.IReportMgr;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.ActionSet;

/**
 * BuildML CLI Command class that implements the "show-files-used-by" command.
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
	 * @see com.buildml.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/show-files-used-by.txt");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "show-files-used-by";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getOptions()
	 */
	@Override
	public Options getOptions() {
		Options opts = super.getOptions();
		
		/* add the --read option */
		Option readOpt = new Option("r", "read", false, "Only show files that are read by these actions.");
		opts.addOption(readOpt);

		/* add the --write option */
		Option writeOpt = new Option("w", "write", false, "Only show files that are written by these actions.");
		opts.addOption(writeOpt);	
		
		/* add the --modify option */
		Option modifyOpt = new Option("m", "modify", false, "Only show files that are modified by these actions.");
		opts.addOption(modifyOpt);
		
		/* add the --delete option */
		Option deleteOpt = new Option("d", "delete", false, "Only show files that are deleted by these actions.");
		opts.addOption(deleteOpt);
		return opts;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getParameterDescription()
	 */
	@Override
	public String getParameterDescription() {
		return "<action-spec>:...";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Report on files that are used by the specified actions.";
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#processOptions(org.apache.commons.cli.CommandLine)
	 */
	@Override
	public void processOptions(IBuildStore buildStore, CommandLine cmdLine) {
		
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
	 * @see com.buildml.main.ICliCommand#invoke(com.buildml.model.BuildStore, java.lang.String[])
	 */
	@Override
	public void invoke(IBuildStore buildStore, String[] args) {
		
		CliUtils.validateArgs(getName(), args, 1, 1, "A colon-separated list of action-specs must be provided.");

		/* are we searching for reads, writes, or both? */
		OperationType opType = CliUtils.getOperationType(optionRead, optionWrite, optionModify, optionDelete);

		IActionMgr actionMgr = buildStore.getActionMgr();
		IReportMgr reportMgr = buildStore.getReportMgr();

		/* fetch the list of actions we're querying */
		String actionSpecs = args[0];
		ActionSet ts = CliUtils.getCmdLineActionSet(actionMgr, actionSpecs);
		if (ts == null) {
			CliUtils.reportErrorAndExit("no actions were selected.");
		}

		/* run the report */
		FileSet accessedFiles = reportMgr.reportFilesAccessedByActions(ts, opType);
		accessedFiles.populateWithParents();

		/* pretty print the results */
		CliUtils.printFileSet(System.out, buildStore, accessedFiles, filterFileSet, optionShowRoots, optionShowPkgs);
	}

	/*-------------------------------------------------------------------------------------*/
}
