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
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.buildml.main.CliUtils;
import com.buildml.main.ICliCommand;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.utils.errors.ErrorCode;

/**
 * BuildML CLI Command class that implements the "add-pkg" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandAddPkg implements ICliCommand {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** Set if we should show create a folder (not just a package) */
	protected static boolean optionAddFolder = false;

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/add-pkg.txt");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "add-pkg";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getOptions()
	 */
	@Override
	public Options getOptions() {
		
		Options opts = new Options();

		/* add the --folder option */
		Option addFolderOpt = new Option("f", "folder", false, "Create a folder.");
		opts.addOption(addFolderOpt);
		
		return opts;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getParameterDescription()
	 */
	@Override
	public String getParameterDescription() {
		return "[ <pkg-name> | <folder-name> ]";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Add a new (empty) package, or a new folder.";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#processOptions(org.apache.commons.cli.CommandLine)
	 */
	@Override
	public void processOptions(IBuildStore buildStore, CommandLine cmdLine) {
		optionAddFolder = cmdLine.hasOption("folder");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#invoke(com.buildml.model.BuildStore, java.lang.String[])
	 */
	@Override
	public void invoke(IBuildStore buildStore, String buildStorePath, String[] args) {

		String objName = optionAddFolder ? "folder" : "package";
		
		CliUtils.validateArgs(getName(), args, 1, 1, "You must provide a " + objName + " name.");

		IPackageMgr pkgMgr = buildStore.getPackageMgr();

		String pkgName = args[0];
		int pkgId;
		
		if (optionAddFolder) {
			pkgId = pkgMgr.addFolder(pkgName);
		} else {
			pkgId = pkgMgr.addPackage(pkgName);			
		}
		
		/* was the syntax of the name valid? */
		if (pkgId == ErrorCode.INVALID_NAME){
			CliUtils.reportErrorAndExit("Invalid " + objName + " name " + pkgName + ".");
		}

		/* was the name already defined in the buildstore? */
		if (pkgId == ErrorCode.ALREADY_USED){
			CliUtils.reportErrorAndExit("The " + objName + " " + pkgName + " is already defined.");
		}

		/* all is good */
		System.out.println("New " + objName + " " + pkgName + " added.");		
	}

	/*-------------------------------------------------------------------------------------*/
}
