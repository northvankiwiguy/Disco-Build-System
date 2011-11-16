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

import com.arapiki.disco.main.CliUtils;
import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.Components;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.Reports;
import com.arapiki.disco.model.types.FileRecord;

/**
 * Disco CLI Command class that implements the "show-popular-files" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandShowPopularFiles extends CliCommandShowFiles {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowFiles#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/show-popular-files.txt");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowFiles#getName()
	 */
	@Override
	public String getName() {
		return "show-popular-files";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowFiles#getParameterDescription()
	 */
	@Override
	public String getParameterDescription() {
		return "";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowFiles#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Report on files that are accessed the most often.";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.commands.CliCommandShowFiles#invoke(com.arapiki.disco.model.BuildStore, java.lang.String[])
	 */
	@Override
	public void invoke(BuildStore buildStore, String[] args) {

		CliUtils.validateArgs(getName(), args, 0, 0, "No arguments expected.");

		FileNameSpaces fns = buildStore.getFileNameSpaces();
		Reports reports = buildStore.getReports();
		Components cmpts = buildStore.getComponents();

		/* fetch the list of most popular files */
		FileRecord results[] = reports.reportMostCommonlyAccessedFiles();

		/* pretty print the results - only show files if they're in the filter set */
		for (FileRecord fileRecord : results) {
			int id = fileRecord.getId();
			if ((filterFileSet == null) || (filterFileSet.isMember(id))){
				int count = fileRecord.getCount();
				String pathName = fns.getPathName(id, optionShowRoots);
				
				/* should we show component names? */
				if (optionShowComps) {
					Integer cmptSectIds[] = cmpts.getFileComponent(id);
					String cmptName = cmpts.getComponentName(cmptSectIds[0]);
					String sectName = cmpts.getSectionName(cmptSectIds[1]);
					System.out.println(count + "\t" + pathName + "  (" + cmptName + "/" + sectName + ")");
				}
				
				/* no, just the file names without components */
				else {
					System.out.println(count + "\t" + pathName);
				}
			}
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
}
