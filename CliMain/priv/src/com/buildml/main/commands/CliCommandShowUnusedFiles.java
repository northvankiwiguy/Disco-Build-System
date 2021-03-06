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

import com.buildml.main.CliUtils;
import com.buildml.model.IBuildStore;
import com.buildml.model.IReportMgr;
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
	public void invoke(IBuildStore buildStore, String buildStorePath, String[] args) {

		CliUtils.validateArgs(getName(), args, 0, 0, "No arguments expected.");

		IReportMgr reportMgr = buildStore.getReportMgr();

		/* get list of unused files, and add their parent paths */
		FileSet unusedFileSet = reportMgr.reportFilesNeverAccessed();
		unusedFileSet.populateWithParents();

		/* pretty print the results */
		CliUtils.printFileSet(System.out, buildStore, unusedFileSet, filterFileSet, optionShowRoots, optionShowPkgs);
	}
	
	/*-------------------------------------------------------------------------------------*/
}
