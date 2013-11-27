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
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.undo.MultiUndoOp;
import com.buildml.refactor.CanNotRefactorException;
import com.buildml.refactor.IImportRefactorer;
import com.buildml.refactor.imports.ImportRefactorer;
import com.buildml.utils.errors.ErrorCode;

/**
 * BuildML CLI Command class that implements the "rm-file" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandRmFile implements ICliCommand {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** Set if we should force removal of associated actions. */
	private boolean optionForceRmAction = false;
	
	/** Set if we want to delete directory content recursively */
	private boolean optionRecursive = false;
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/rm-file.txt");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "rm-file";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getOptions()
	 */
	@Override
	public Options getOptions() {

		Options opts = new Options();

		/* add the --force-rm-actions option */
		Option deleteActionsOpt = new Option("f", "force-rm-action", false, 
				"Also delete the action generating this file.");
		opts.addOption(deleteActionsOpt);

		/* add the --recursive option */
		Option deleteRecursiveOpt = new Option("r", "recursive", false, 
				"Delete sub-tree recursively.");
		opts.addOption(deleteRecursiveOpt);

		return opts;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getParameterDescription()
	 */
	@Override
	public String getParameterDescription() {
		return "[-r] [-f] <path>";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Delete a file (and associated action) from the build system.";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#processOptions(org.apache.commons.cli.CommandLine)
	 */
	@Override
	public void processOptions(IBuildStore buildStore, CommandLine cmdLine) {
		optionForceRmAction = cmdLine.hasOption("force-rm-action");
		optionRecursive = cmdLine.hasOption("recursive");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#invoke(com.buildml.model.BuildStore, java.lang.String[])
	 */
	@Override
	public void invoke(IBuildStore buildStore, String buildStorePath, String[] args) {

		CliUtils.validateArgs(getName(), args, 1, 1, "You must specify a single path name.");

		IFileMgr fileMgr = buildStore.getFileMgr();
		IActionMgr actionMgr = buildStore.getActionMgr();
		String pathName = args[0];

		int pathId = fileMgr.getPath(pathName);

		/* is the path valid, if so, set the root */
		if (pathId != ErrorCode.BAD_PATH) {
			IImportRefactorer refactor = new ImportRefactorer(buildStore);
			try {
				MultiUndoOp multiOp = new MultiUndoOp();
				if (optionRecursive) {
					refactor.deletePathTree(multiOp, pathId, optionForceRmAction);
				} else {
					refactor.deletePath(multiOp, pathId, optionForceRmAction);
				}
				multiOp.redo();
				
			/* 
			 * Handle the array of possible error cases.
			 */
			} catch (CanNotRefactorException e) {
				
				String msg = "Failed to delete file: " + pathName + ". ";
				
				switch (e.getCauseCode()) {
				
				/* The path is currently used as input to one or more actions */
				case PATH_IN_USE:
					msg += "The path is in use by the following actions:\n";
					msg = appendActions(actionMgr, msg, e.getCauseIDs());
					break;
					
				/* The path is still being generated by an action */
				case PATH_IS_GENERATED:
					msg += "This path is generated by the following action (or actions):\n";
					msg = appendActions(actionMgr, msg, e.getCauseIDs());
					msg += "\nUse the -f flag to force deletion of this action.\n";
					break;
					
				/* The action is still in use because a generated file is needed */
				case ACTION_IN_USE:
					msg += "The action that generates this file also generates additional files " +
							"that are in use by other actions. The files are:\n";
					msg = appendPaths(fileMgr, msg, e.getCauseIDs());
					break;
					
				/* The action is not atomic, so this operation can not proceed */
				case ACTION_NOT_ATOMIC:
					msg += "The action that generates this file is not atomic. Can't delete it:\n";
					msg = appendActions(actionMgr, msg, e.getCauseIDs());
					break;
					
					/** Couldn't delete a directory, since it's not empty */
				case DIRECTORY_NOT_EMPTY:
					msg += "This directory can't be deleted because it's not empty.";
					break;
					
				default:
					msg += "Unrecognized error: " + e.getCauseCode();
					break;
				}

				CliUtils.reportErrorAndExit(msg);
			}
			
		} 

		/* the path we're trying to delete is invalid */
		else {
			CliUtils.reportErrorAndExit("Invalid path: " + pathName + ".");
		}
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * Given a list of action IDs, append the command output of each action onto the
	 * existing error message. This is a helper message for display errors.
	 * 
	 * @param actionMgr The ActionMgr that owns the actions.
	 * @param msg The error message so far (we'll append to this).
	 * @param actions An array of action IDs.
	 * @return The original "msg" parameter, which the action summaries appended to it.
	 */
	private String appendActions(IActionMgr actionMgr, String msg, Integer[] actions) {
		for (int actionId : actions) {
			msg += "\n" + actionMgr.getCommand(actionId) + "\n";
		}
		return msg;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a list of path IDs, append each path name onto the existing error message. 
	 * This is a helper message for display errors.
	 * 
	 * @param fileMgr The FileMgr that owns the paths.
	 * @param msg The error message so far (we'll append to this).
	 * @param paths An array of path IDs.
	 * @return The original "msg" parameter, which the path names appended to it.
	 */
	private String appendPaths(IFileMgr fileMgr, String msg, Integer[] paths) {
		for (int pathId : paths) {
			msg += "\n   " + fileMgr.getPathName(pathId) + "\n";
		}
		return msg;
	}

	/*-------------------------------------------------------------------------------------*/
}
