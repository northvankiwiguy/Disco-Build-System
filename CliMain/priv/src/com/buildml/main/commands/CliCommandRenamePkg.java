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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import com.buildml.main.CliUtils;
import com.buildml.main.ICliCommand;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.utils.errors.ErrorCode;

/**
 * BuildML CLI Command class that implements the "rename-pkg" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandRenamePkg implements ICliCommand {
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/rename-pkg.txt");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "rename-pkg";
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
		return "<old-name> <new-name>";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Rename a package (or folder).";
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

		CliUtils.validateArgs(getName(), args, 2, 2,
				"You must provide both old and new package/folder names.");

		IPackageMgr pkgMgr = buildStore.getPackageMgr();

		String oldName = args[0];
		String newName = args[1];
		
		/* check validity of old name */		
		int oldId = pkgMgr.getId(oldName);
		if (oldId == ErrorCode.NOT_FOUND) {
			CliUtils.reportErrorAndExit("Package/folder " + oldName + " is not defined.");			
		}
		
		/* attempt the rename, possibly getting an error message back */
		int result = pkgMgr.setName(oldId, newName);
		if (result == ErrorCode.ALREADY_USED) {
			CliUtils.reportErrorAndExit("The name " + newName + " is already in use.");
		} else if (result == ErrorCode.INVALID_NAME) {
			CliUtils.reportErrorAndExit("The name " + newName + " is not a valid name.");
		}

		/* else, all is good */
		System.out.println("Package " + oldName + " renamed to " + newName + ".");		
	}

	/*-------------------------------------------------------------------------------------*/
}
