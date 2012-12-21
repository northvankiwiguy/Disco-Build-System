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
import com.buildml.model.types.FileSet;
import com.buildml.utils.errors.ErrorCode;

/**
 * BuildML CLI Command class that implements the "set-file-pkg" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandSetFilePkg implements ICliCommand {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/set-file-pkg.txt");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "set-file-pkg";
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
		return "<pkg-name> <path-spec>:...";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Add a set of files into a package.";
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
						"You must specify a package name and a path-spec.");

		IFileMgr fileMgr = buildStore.getFileMgr();
		IPackageMgr pkgMgr = buildStore.getPackageMgr();

		/* 
		 * The package can be of the form: "pkg" or "pkg/scope". If scope
		 * isn't specified, "private" will be used.
		 */
		String pkgName = args[0];
		int pkgAndScopeIds[] = CliUtils.parsePackageAndScope(pkgMgr, pkgName, true);
		int pkgId = pkgAndScopeIds[0];
		int scopeId = pkgAndScopeIds[1];

		/* find out which files the user wants to set */
		String fileSpec = args[1];
		FileSet filesToSet = CliUtils.getCmdLineFileSet(fileMgr, fileSpec);

		/* now visit each file in the FileSet and set it's package/scope */
		boolean errorOccurred = false;
		buildStore.setFastAccessMode(true);
		for (int file : filesToSet) {
			int rc = pkgMgr.setFilePackage(file, pkgId, scopeId);
			if (rc == ErrorCode.OUT_OF_RANGE) {
				System.err.println("Unable to move file " + fileMgr.getPathName(file) + 
									" into package " + pkgName + ". It is not within the package root.");
				errorOccurred = true;
			}
		}
		buildStore.setFastAccessMode(false);

		if (errorOccurred) {
			CliUtils.reportErrorAndExit("One or more errors occurred while changing packages.");
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
