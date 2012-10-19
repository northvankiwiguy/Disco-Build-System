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
import com.buildml.model.IPackageMgr;
import com.buildml.model.impl.BuildStore;
import com.buildml.model.impl.FileNameSpaces;
import com.buildml.model.types.FileSet;

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
	public void processOptions(BuildStore buildStore, CommandLine cmdLine) {
		/* no options */
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#invoke(com.buildml.model.BuildStore, java.lang.String[])
	 */
	@Override
	public void invoke(BuildStore buildStore, String[] args) {

		CliUtils.validateArgs(getName(), args, 2, 2, "You must specify a package name and a path-spec.");

		FileNameSpaces fns = buildStore.getFileNameSpaces();
		IPackageMgr pkgMgr = buildStore.getPackageMgr();

		/* 
		 * The package can be of the form: "pkg" or "pkg/scope". If scope
		 * isn't specified, "private" will be used.
		 */
		String pkgName = args[0];
		int pkgAndSectionIds[] = CliUtils.parsePackageAndScope(pkgMgr, pkgName, true);
		int pktId = pkgAndSectionIds[0];
		int scopeId = pkgAndSectionIds[1];

		/* find out which files the user wants to set */
		String fileSpec = args[1];
		FileSet filesToSet = CliUtils.getCmdLineFileSet(fns, fileSpec);

		/* now visit each file in the FileSet and set it's package/scope */
		buildStore.setFastAccessMode(true);
		for (int file : filesToSet) {
			pkgMgr.setFilePackage(file, pktId, scopeId);
		}
		buildStore.setFastAccessMode(false);
	}

	/*-------------------------------------------------------------------------------------*/
}
