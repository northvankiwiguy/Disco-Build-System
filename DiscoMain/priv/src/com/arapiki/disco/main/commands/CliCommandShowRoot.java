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
import com.arapiki.utils.errors.ErrorCode;

/**
 * Disco CLI Command class that implements the "show-root" command. See the 
 * getLongDescription() method for details of this command's features.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandShowRoot implements ICliCommand {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		// TODO Add a description
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "show-root";
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
		return "[ <root-name> ]";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Show the file system path referred to by this root";
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
		
		CliUtils.validateArgs(getName(), args, 0, 1, "Only one root name can be specified");

		FileNameSpaces fns = buildStore.getFileNameSpaces();

		/* no arguments == display all roots */
		if (args.length == 0) {
			String [] roots = fns.getRoots();
			for (int i = 0; i < roots.length; i++) {
				String rootName = roots[i];
				String associatedPath = fns.getPathName(fns.getRootPath(rootName));
				System.out.println(rootName + " " + associatedPath);
			}
		}

		/* else, one arg == show the path for this specific root */
		else {
			String rootName = args[0];
			int rootId = fns.getRootPath(rootName);
			if (rootId == ErrorCode.NOT_FOUND) {
				CliUtils.reportErrorAndExit("Root name not found - " + rootName);
			}
			String associatedPath = fns.getPathName(rootId);
			System.out.println(associatedPath);
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
