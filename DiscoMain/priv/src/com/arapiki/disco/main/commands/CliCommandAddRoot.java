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
import org.apache.commons.cli.Options;

import com.arapiki.disco.main.CliUtils;
import com.arapiki.disco.main.ICliCommand;
import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.FileNameSpaces;
import com.buildml.utils.errors.ErrorCode;

/**
 * Disco CLI Command class that implements the "add-root" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandAddRoot implements ICliCommand {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/add-root.txt");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "add-root";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getOptions()
	 */
	@Override
	public Options getOptions() {
		return new Options();
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getParameterDescription()
	 */
	@Override
	public String getParameterDescription() {
		return "<root-name> <path>";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Set the root to refer to the specified path.";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#processOptions(org.apache.commons.cli.CommandLine)
	 */
	@Override
	public void processOptions(BuildStore buildStore, CommandLine cmdLine) {
		/* no options */
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#invoke(com.arapiki.disco.model.BuildStore, java.lang.String[])
	 */
	@Override
	public void invoke(BuildStore buildStore, String[] args) {

		CliUtils.validateArgs(getName(), args, 2, 2, "You must specify both a root name and a single path.");

		FileNameSpaces fns = buildStore.getFileNameSpaces();
		String rootName = args[0];
		String pathName = args[1];
		int rc;

		int pathId = fns.getPath(pathName);

		/* is the path valid, if so, set the root */
		if (pathId != ErrorCode.BAD_PATH) {
			/* 
			 * There are two approaches here - if the root exists already, move it.
			 */
			if (fns.getRootPath(rootName) != ErrorCode.NOT_FOUND){
				rc = fns.moveRootToPath(rootName, pathId);
			}

			/* else, create a new root */
			else {
				rc = fns.addNewRoot(rootName, pathId);
			}
		} 

		/* the path we're trying to set the root at is invalid */
		else {
			rc = ErrorCode.BAD_PATH;
		}

		/* Do we need to report an error? */
		if (rc != ErrorCode.OK) {

			String msg = null;

			switch (rc) {
			case ErrorCode.NOT_A_DIRECTORY:
				msg = pathName + " is not a directory.";
				break;

			case ErrorCode.INVALID_NAME:
				msg = rootName + " is not a valid root name.";
				break;

			case ErrorCode.ONLY_ONE_ALLOWED:
				msg = pathName + " already has a root associated with it.";
				break;

			case ErrorCode.BAD_PATH:
				msg = pathName + " is an invalid path.";
				break;
			}

			CliUtils.reportErrorAndExit("Unable to set root. " + msg);
		}

	}

	/*-------------------------------------------------------------------------------------*/
}
