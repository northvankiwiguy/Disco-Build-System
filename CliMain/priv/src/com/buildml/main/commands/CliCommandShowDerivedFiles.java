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
import com.buildml.model.IPackageMgr;
import com.buildml.model.IReportMgr;
import com.buildml.model.impl.BuildStore;
import com.buildml.model.impl.FileNameSpaces;
import com.buildml.model.types.FileSet;

/**
 * BuildML CLI Command class that implements the "show-derived-files" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandShowDerivedFiles extends CliCommandShowFiles {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** Set if we should show all the indirect files too. */
	protected static boolean optionAll = false;
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.main.commands.CliCommandShowFiles#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/show-derived-files.txt");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.commands.CliCommandShowFiles#getName()
	 */
	@Override
	public String getName() {
		return "show-derived-files";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.commands.CliCommandShowFiles#getOptions()
	 */
	@Override
	public Options getOptions() {
		
		/* start with the standard show-files options */
		Options opts = super.getOptions();
		
		/* add the --all option */
		Option allOpt = new Option("a", "all", false, "Also show indirectly derived files.");
		opts.addOption(allOpt);
		
		return opts;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.commands.CliCommandShowFiles#getParameterDescription()
	 */
	@Override
	public String getParameterDescription() {
		return "<path-spec>:...";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.commands.CliCommandShowFiles#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "List the files that are derived from the input files.";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.commands.CliCommandShowFiles#processOptions(org.apache.commons.cli.CommandLine)
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
	 * @see com.buildml.main.commands.CliCommandShowFiles#invoke(com.buildml.model.BuildStore, java.lang.String[])
	 */
	@Override
	public void invoke(BuildStore buildStore, String[] args) {

		CliUtils.validateArgs(getName(), args, 1, 1, "One or more colon-separated path-specs must be provided.");

		FileNameSpaces fns = buildStore.getFileNameSpaces();
		IReportMgr reportMgr = buildStore.getReportMgr();
		IPackageMgr pkgMgr = buildStore.getPackageMgr();

		/* fetch the list of files that are the source of the derivation */
		FileSet sourceFileSet = CliUtils.getCmdLineFileSet(fns, args[0]);
		if (sourceFileSet != null) {
			sourceFileSet.populateWithParents();
		}

		/* get list of derived files, and add their parent paths */
		FileSet derivedFileSet = reportMgr.reportDerivedFiles(sourceFileSet, optionAll);
		derivedFileSet.populateWithParents();

		/* pretty print the results - no filtering used here */
		CliUtils.printFileSet(System.out, fns, pkgMgr, derivedFileSet, filterFileSet, optionShowRoots, optionShowPkgs);	
	}

	/*-------------------------------------------------------------------------------------*/
}
