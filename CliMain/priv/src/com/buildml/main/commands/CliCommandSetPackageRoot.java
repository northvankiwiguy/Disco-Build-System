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
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageRootMgr;
import com.buildml.utils.errors.ErrorCode;

/**
 * BuildML CLI Command class that implements the "set-pkg-root" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandSetPackageRoot implements ICliCommand {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/set-pkg-root.txt");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "set-pkg-root";
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
		return "<pkg-root-name> <path>";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Set a package's 'src' or 'gen' root to the specified path.";
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
				"You must specify a root name (ending with 'src' or 'gen'), then a path.");

		IFileMgr fileMgr = buildStore.getFileMgr();
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
		
		String pkgRootName = args[0];
		String pathName = args[1];
		int rc;
				
		/* Validate the type of root, src or gen */
		int rootType = 0;
		if (pkgRootName.endsWith("_src")) {
			rootType = IPackageRootMgr.SOURCE_ROOT;
		} else if (pkgRootName.endsWith("_gen")) {
			rootType = IPackageRootMgr.GENERATED_ROOT;
		} else {
			CliUtils.reportErrorAndExit("Invalid root name: " + pkgRootName + 
										". Must end with either 'src' or 'gen'.");
		}		
		
		/* Validate the package name. */
		String pkgName = pkgRootName.substring(0, pkgRootName.length() - 4);
		int pkgId = pkgMgr.getId(pkgName);
		if (pkgId == ErrorCode.NOT_FOUND) {
			CliUtils.reportErrorAndExit("Invalid package name: " + pkgName + ".");
		}

		/* validate the path */
		int pathId = rc = fileMgr.getPath(pathName);
		if (pathId != ErrorCode.BAD_PATH) {
			rc = pkgRootMgr.setPackageRoot(pkgId, rootType, pathId);
		} 
		
		/* Do we need to report an error? */
		if (rc != ErrorCode.OK) {

			String msg = null;

			switch (rc) {
			case ErrorCode.NOT_A_DIRECTORY:
				msg = pathName + " is not a directory.";
				break;
			case ErrorCode.BAD_PATH:
				msg = pathName + " is an invalid path.";
				break;
			case ErrorCode.NOT_FOUND:
				msg = "Invalid package name or package type.";
				break;
			case ErrorCode.OUT_OF_RANGE:
				msg = "The proposed package root is either above the workspace root, " +
			          "or does not emcompass all the package's files.";
				break;
			}
			CliUtils.reportErrorAndExit("Unable to set root. " + msg);
		}

		System.out.println("Package root: " + pkgRootName + " set to " + 
							fileMgr.getPathName(pathId) + ".");		
	}

	/*-------------------------------------------------------------------------------------*/
}
