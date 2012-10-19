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
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.model.impl.FileNameSpaces;
import com.buildml.model.types.FileSet;

/**
 * BuildML CLI Command class that implements the "show-files" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandShowFiles implements ICliCommand {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** Set if we should show roots when displaying reports. */
	protected boolean optionShowRoots = false;
	
	/** Set if we should show package membership when displaying reports. */
	protected boolean optionShowPkgs = false;
	
	/** The FileSet used to filter our results (if -f/--filter is used). */
	protected FileSet filterFileSet = null;
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/show-files.txt");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "show-files";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getOptions()
	 */
	@Override
	public Options getOptions() {
		
		Options opts = new Options();

		/* add the --show-roots option */
		Option showRootsOpt = new Option("r", "show-roots", false, "Show path roots when displaying report output.");
		opts.addOption(showRootsOpt);

		/* add the --show-pkgs option */
		Option showPkgsOpt = new Option("p", "show-pkgs", false, "Show package of each path in report output.");
		opts.addOption(showPkgsOpt);

		/* add the -f/--filter option */
		Option filterOpt = new Option("f", "filter", true, "Path-specs used to filter the output.");
		filterOpt.setArgName("path-spec:...");
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
		return "List all files in the build system.";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#processOptions(java.lang.String[])
	 */
	@Override
	public void processOptions(IBuildStore buildStore, CommandLine cmdLine) {
		optionShowRoots = cmdLine.hasOption("show-roots");
		optionShowPkgs = cmdLine.hasOption("show-pkgs");
		
		/* fetch the subset of files we should filter-in */
	
		FileNameSpaces fns = buildStore.getFileNameSpaces();
		String filterInString = cmdLine.getOptionValue("f");
		if (filterInString != null) {
			filterFileSet = CliUtils.getCmdLineFileSet(fns, filterInString);
			if (filterFileSet != null) {
				filterFileSet.populateWithParents();
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#invoke(org.apache.commons.cli.CommandLine)
	 */
	@Override
	public void invoke(IBuildStore buildStore, String [] args) {
		
		CliUtils.validateArgs(getName(), args, 0, 0, "No arguments expected.");

		FileNameSpaces fns = buildStore.getFileNameSpaces();
		IPackageMgr pkgMgr = buildStore.getPackageMgr();

		/* 
		 * There were no search "results", so we'll show everything (except those
		 * that are filtered-out by filterFileSet).
		 */
		FileSet resultFileSet = null;

		/* pretty print the results */
		CliUtils.printFileSet(System.out, fns, pkgMgr, resultFileSet, filterFileSet, optionShowRoots, optionShowPkgs);
	}

	/*-------------------------------------------------------------------------------------*/
}
