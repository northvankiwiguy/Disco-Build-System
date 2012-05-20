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
import com.buildml.model.BuildStore;
import com.buildml.model.Components;

/**
 * Disco CLI Command class that implements the "show-comp" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandShowComp implements ICliCommand {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/show-comp.txt");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "show-comp";
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
		return "";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Show the components defined in the build system.";
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
	 * @see com.arapiki.disco.main.ICliCommand#invoke(com.buildml.model.BuildStore, java.lang.String[])
	 */
	@Override
	public void invoke(BuildStore buildStore, String[] args) {

		CliUtils.validateArgs(getName(), args, 0, 0, "No arguments expected.");
	
		Components cmpts = buildStore.getComponents();
		
		String compNames[] = cmpts.getComponents();
		for (int i = 0; i < compNames.length; i++) {
			System.out.println(compNames[i]);
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
