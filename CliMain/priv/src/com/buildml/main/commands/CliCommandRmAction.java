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
import com.buildml.refactor.CanNotRefactorException;
import com.buildml.refactor.IImportRefactorer;
import com.buildml.refactor.imports.ImportRefactorer;

/**
 * BuildML CLI Command class that implements the "rm-action" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandRmAction implements ICliCommand {
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/rm-action.txt");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "rm-action";
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
		return "<action>";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Remove an action, promoting child actions up one level.";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#processOptions(org.apache.commons.cli.CommandLine)
	 */
	@Override
	public void processOptions(IBuildStore buildStore, CommandLine cmdLine) {
		/* empty */
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#invoke(com.buildml.model.BuildStore, java.lang.String[])
	 */
	@Override
	public void invoke(IBuildStore buildStore, String buildStorePath, String[] args) {

		CliUtils.validateArgs(getName(), args, 1, 1, "You must specify a single action ID number.");

		int actionId = -1;
		try {
			actionId = Integer.parseInt(args[0]);
		} catch (NumberFormatException ex) {
			CliUtils.reportErrorAndExit("Invalid action ID: " + args[0]);
		}
		
		IImportRefactorer refactor = new ImportRefactorer(buildStore);
		try {
			refactor.deleteAction(actionId);
			
		/* 
		 * Handle the array of possible error cases.
		 */
		} catch (CanNotRefactorException e) {
				
			switch (e.getCauseCode()) {
			
			case ACTION_IS_TRASHED:
			case INVALID_ACTION:
				CliUtils.reportErrorAndExit("Invalid action ID. Action may have been deleted.");
				break;
			
			case ACTION_IN_USE:
				CliUtils.reportErrorAndExit("Action can't be deleted as it generates a file that is " +
						"still in active use.");
				break;
			
			default:
				CliUtils.reportErrorAndExit("Unrecognized error: " + e.getCauseCode());
				break;
			}
		} 
	}

	/*-------------------------------------------------------------------------------------*/
}
