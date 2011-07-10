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
import com.arapiki.disco.main.ICliCommand;
import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.Components;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.FileSet;

/**
 * Disco CLI Command class that implements the "show-files" command. See the 
 * getLongDescription() method for details of this command's features.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandShowFiles implements ICliCommand {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/* should we show roots when displaying reports? */
	protected boolean optionShowRoots = false;
	
	/* should we show component membership when displaying reports? */
	protected boolean optionShowComps = false;
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		// TODO Add a description
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "show-files";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getOptions()
	 */
	@Override
	public Options getOptions() {
		
		Options opts = new Options();

		/* add the --show-roots option */
		Option showRootsOpt = new Option("r", "show-roots", false, "Show file name space roots when displaying report output");
		opts.addOption(showRootsOpt);

		/* add the --show-comps option */
		Option showCompsOpt = new Option("c", "show-comps", false, "Show component of each file in report output");
		opts.addOption(showCompsOpt);

		return opts;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getParameterDescription()
	 */
	@Override
	public String getParameterDescription() {
		return "[ <path>, ... ]";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "List all files in the build system";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#processOptions(java.lang.String[])
	 */
	@Override
	public void processOptions(CommandLine cmdLine) {
		optionShowRoots = cmdLine.hasOption("show-roots");
		optionShowComps = cmdLine.hasOption("show-comps");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#invoke(org.apache.commons.cli.CommandLine)
	 */
	@Override
	public void invoke(BuildStore buildStore, String [] args) {
				
		FileNameSpaces fns = buildStore.getFileNameSpaces();
		Components cmpts = buildStore.getComponents();

		/* fetch the subset of files we should filter through */
		FileSet filterFileSet = CliUtils.getCmdLineFileSet(fns, args);
		if (filterFileSet != null) {
			filterFileSet.populateWithParents();
		}

		/* 
		 * There were no search "results", so we'll show everything (except those
		 * that are filtered-out by filterFileSet. 
		 */
		FileSet resultFileSet = null;

		/* pretty print the results */
		CliUtils.printFileSet(System.out, fns, cmpts, resultFileSet, filterFileSet, optionShowRoots, optionShowComps);
	}

	/*-------------------------------------------------------------------------------------*/
}
