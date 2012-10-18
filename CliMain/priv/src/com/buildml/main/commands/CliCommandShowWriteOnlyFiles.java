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
import com.buildml.model.impl.BuildStore;
import com.buildml.model.impl.Packages;
import com.buildml.model.impl.FileNameSpaces;
import com.buildml.model.impl.Reports;
import com.buildml.model.types.FileSet;

/**
 * BuildML CLI Command class that implements the "show-write-only-files" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandShowWriteOnlyFiles extends CliCommandShowFiles {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.main.commands.CliCommandShowFiles#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/show-write-only-files.txt");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.commands.CliCommandShowFiles#getName()
	 */
	@Override
	public String getName() {
		return "show-write-only-files";
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
		return "Report on files that are written to, but never read from.";	
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
		Packages pkgMgr = buildStore.getPackages();

		/* get list of write-only files, and add their parent paths */
		FileSet writeOnlyFileSet = reports.reportWriteOnlyFiles();
		writeOnlyFileSet.populateWithParents();

		/* pretty print the results */
		CliUtils.printFileSet(System.out, fns, pkgMgr, writeOnlyFileSet, filterFileSet, optionShowRoots, optionShowPkgs);
	}

	/*-------------------------------------------------------------------------------------*/
}
