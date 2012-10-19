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
import org.apache.commons.cli.Options;

import com.buildml.main.CliUtils;
import com.buildml.main.ICliCommand;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.utils.errors.ErrorCode;

/**
 * BuildML CLI Command class that implements the "rm-root" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandRemoveRoot implements ICliCommand {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/rm-root.txt");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "rm-root";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getOptions()
	 */
	@Override
	public Options getOptions() {
		return new Options();
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getParameterDescription()
	 */
	@Override
	public String getParameterDescription() {
		return "<root-name>";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Remove the root so it no long references a path.";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#processOptions(org.apache.commons.cli.CommandLine)
	 */
	@Override
	public void processOptions(IBuildStore buildStore, CommandLine cmdLine) {
		/* no options */
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#invoke(com.buildml.model.BuildStore, java.lang.String[])
	 */
	@Override
	public void invoke(IBuildStore buildStore, String[] args) {

		CliUtils.validateArgs(getName(), args, 1, 1, "You must specify a root name.");
		
		IFileMgr fileMgr = buildStore.getFileMgr();
		String rootName = args[0];
		
		int rc = fileMgr.deleteRoot(rootName);
		
		if (rc != ErrorCode.OK) {
			String msg = null;
			
			switch (rc) {
			case ErrorCode.NOT_FOUND:
				msg = rootName + " doesn't exist.";
				break;
				
			case ErrorCode.CANT_REMOVE:
				msg = rootName + " can't be removed.";
				break;
			}
			
			CliUtils.reportErrorAndExit("Error removing root. " + msg);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
}
