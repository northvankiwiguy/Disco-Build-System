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
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.types.ActionSet;
import com.buildml.model.undo.MultiUndoOp;
import com.buildml.refactor.CanNotRefactorException;
import com.buildml.refactor.IImportRefactorer;
import com.buildml.refactor.imports.ImportRefactorer;

/**
 * BuildML CLI Command class that implements the "merge-actions" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandMergeActions implements ICliCommand {
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/merge-actions.txt");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "merge-actions";
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
		return "<action-set>";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Merge multiple shell actions into a single shell action.";
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

		CliUtils.validateArgs(getName(), args, 1, 1, "You must specify a set of actions.");

		IActionMgr actionMgr = buildStore.getActionMgr();
		ActionSet set = CliUtils.getCmdLineActionSet(actionMgr, args[0]);

		IImportRefactorer refactor = new ImportRefactorer(buildStore);
		try {
			MultiUndoOp multiOp = new MultiUndoOp();
			refactor.mergeActions(multiOp, set);
			multiOp.redo();
			
		/* 
		 * Handle the array of possible error cases.
		 */
		} catch (CanNotRefactorException e) {
				
			switch (e.getCauseCode()) {
			
			case ACTION_NOT_ATOMIC:
				CliUtils.reportErrorAndExit("One or more of the actions are not atomic.");
				break;
				
			default:
				CliUtils.reportErrorAndExit("Unrecognized error: " + e.getCauseCode());
				break;
			}
		} 
	}

	/*-------------------------------------------------------------------------------------*/
}
