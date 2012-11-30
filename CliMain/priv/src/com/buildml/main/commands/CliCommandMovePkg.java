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
 * BuildML CLI Command class that implements the "move-pkg" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandMovePkg implements ICliCommand {
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/move-pkg.txt");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "move-pkg";
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
		return "[ <pkg-name> | <folder-name> ] <dest-folder-name>";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Move a package (or folder) into a new parent folder.";
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
				"You must provide a package or folder name, and a destination folder.");

		IPackageMgr pkgMgr = buildStore.getPackageMgr();

		String pkgName = args[0];
		String folderName = args[1];
		
		/* check validity of names */		
		int pkgId = pkgMgr.getId(pkgName);
		if (pkgId == ErrorCode.NOT_FOUND) {
			CliUtils.reportErrorAndExit("Package/folder " + pkgName + " is not defined.");			
		}
		int folderId = pkgMgr.getId(folderName);
		if (folderId == ErrorCode.NOT_FOUND) {
			CliUtils.reportErrorAndExit("Destination folder " + folderName + " is not defined.");			
		}
		
		/* attempt the move, possibly getting an error message back */
		int result = pkgMgr.setParent(pkgId, folderId);
		if (result == ErrorCode.NOT_A_DIRECTORY) {
			CliUtils.reportErrorAndExit("The name " + folderName + " does not refer to a folder.");
		} else if (result == ErrorCode.BAD_PATH) {
			CliUtils.reportErrorAndExit("The package " + pkgName + " can't be moved into the folder " +
					folderName + ".");
		}

		/* else, all is good */
		System.out.println("Package " + pkgName + " moved.");		
	}

	/*-------------------------------------------------------------------------------------*/
}
