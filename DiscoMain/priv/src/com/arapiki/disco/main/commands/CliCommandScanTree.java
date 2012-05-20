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

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import com.arapiki.disco.main.CliUtils;
import com.arapiki.disco.main.ICliCommand;
import com.arapiki.disco.model.BuildStore;
import com.buildml.scanner.buildtree.FatalBuildTreeScannerError;
import com.buildml.scanner.buildtree.FileSystemScanner;

/**
 * Disco CLI Command class that implements the "scan-tree" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandScanTree implements ICliCommand {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/scan-tree.txt");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "scan-tree";
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
		return "<path> ...";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Scan one or more file system directories and record file names.";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#processOptions(org.apache.commons.cli.CommandLine)
	 */
	@Override
	public void processOptions(BuildStore buildStore, CommandLine cmdLine) {
		/* nothing to do */
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#invoke(com.arapiki.disco.model.BuildStore, java.lang.String[])
	 */
	@Override
	public void invoke(BuildStore buildStore, String[] args) {

		CliUtils.validateArgs(getName(), args, 1, CliUtils.ARGS_INFINITE, "one or more directory names required.");

		/* validate that the directories exist */
		for (int i = 0; i < args.length; i++) {

			File thisFile = new File(args[i]);
			if (!thisFile.exists()){
				CliUtils.reportErrorAndExit("Error: directory (or file) doesn't exist: " + args[i] + ".");
			}
		}

		/* all the directories/files exist, so let's scan them */
		FileSystemScanner fss = new FileSystemScanner(buildStore);

		/* for each directory (skipping the first one which is the command name) */
		for (int i = 0; i < args.length; i++) {
			String dirName = args[i];
			String dirNameAbs;
			try {
				dirNameAbs = new File(dirName).getCanonicalPath();
			} catch (IOException e) {
				throw new FatalBuildTreeScannerError("Can't determine absolute path of " + dirName + ".");
			}
			System.out.println("Scanning " + dirNameAbs);
			fss.scanForFiles("root", dirNameAbs);
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
