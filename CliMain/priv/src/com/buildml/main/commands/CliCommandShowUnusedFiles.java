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

import com.buildml.main.CliUtils;
import com.buildml.model.BuildStore;
import com.buildml.model.Components;
import com.buildml.model.FileNameSpaces;
import com.buildml.model.Reports;
import com.buildml.model.types.FileSet;

/**
 * BuildML CLI Command class that implements the "show-unused-files" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandShowUnusedFiles extends CliCommandShowFiles {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.main.commands.CliCommandShowFiles#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/show-unused-files.txt");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.commands.CliCommandShowFiles#getName()
	 */
	@Override
	public String getName() {
		return "show-unused-files";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.commands.CliCommandShowFiles#getParameterDescription()
	 */
	@Override
	public String getParameterDescription() {
		return "";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.commands.CliCommandShowFiles#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Report on files that are never used by the build system.";
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.commands.CliCommandShowFiles#invoke(com.buildml.model.BuildStore, java.lang.String[])
	 */
	@Override
	public void invoke(BuildStore buildStore, String[] args) {

		CliUtils.validateArgs(getName(), args, 0, 0, "No arguments expected.");

		FileNameSpaces fns = buildStore.getFileNameSpaces();
		Reports reports = buildStore.getReports();
		Components cmpts = buildStore.getComponents();

		/* get list of unused files, and add their parent paths */
		FileSet unusedFileSet = reports.reportFilesNeverAccessed();
		unusedFileSet.populateWithParents();

		/* pretty print the results */
		CliUtils.printFileSet(System.out, fns, cmpts, unusedFileSet, filterFileSet, optionShowRoots, optionShowComps);
	}
	
	/*-------------------------------------------------------------------------------------*/
}