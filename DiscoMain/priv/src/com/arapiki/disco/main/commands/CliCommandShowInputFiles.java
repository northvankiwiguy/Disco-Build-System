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
import com.arapiki.disco.model.Components;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.FileSet;
import com.arapiki.disco.model.Reports;

/**
 * Disco CLI Command class that implements the "show-input-files" command. See the 
 * getLongDescription() method for details of this command's features.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandShowInputFiles extends CliCommandShowFiles {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/* should we all show the indirect files? */
	protected static boolean optionAll = false;
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowFiles#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/show-input-files.txt");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowFiles#getName()
	 */
	@Override
	public String getName() {
		return "show-input-files";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowFiles#getOptions()
	 */
	@Override
	public Options getOptions() {
		
		/* start with the standard show-files options */
		Options opts = super.getOptions();
		
		/* add the --all option */
		Option allOpt = new Option("a", "all", false, "Also show indirect file inputs");
		opts.addOption(allOpt);
		
		return opts;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowFiles#getParameterDescription()
	 */
	@Override
	public String getParameterDescription() {
		return "<path-spec>:...";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowFiles#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "List the files that are used as input to the specified file(s)";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowFiles#processOptions(org.apache.commons.cli.CommandLine)
	 */
	@Override
	public void processOptions(BuildStore buildStore, CommandLine cmdLine) {

		/* Handle the default show-files options first */
		super.processOptions(buildStore, cmdLine);
		
		/* we also support the -a/--all flags */
		optionAll = cmdLine.hasOption("all");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowFiles#invoke(com.arapiki.disco.model.BuildStore, java.lang.String[])
	 */
	@Override
	public void invoke(BuildStore buildStore, String[] args) {

		CliUtils.validateArgs(getName(), args, 1, 1, "One or more colon-separated path-specs must be provided");

		FileNameSpaces fns = buildStore.getFileNameSpaces();
		Reports reports = buildStore.getReports();
		Components cmpts = buildStore.getComponents();

		/* fetch the list of files that are the target of the derivation */
		FileSet sourceFileSet = CliUtils.getCmdLineFileSet(fns, args[0]);
		if (sourceFileSet != null) {
			sourceFileSet.populateWithParents();
		}

		/* get list of input files, and add their parent paths */
		FileSet inputFileSet = reports.reportInputFiles(sourceFileSet, optionAll);
		inputFileSet.populateWithParents();

		/* pretty print the results - no filtering used here */
		CliUtils.printFileSet(System.out, fns, cmpts, inputFileSet, filterFileSet, optionShowRoots, optionShowComps);	
	}

	/*-------------------------------------------------------------------------------------*/
}
