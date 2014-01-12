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
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.types.ActionSet;

/**
 * BuildML CLI Command class that implements the "set-action-pkg" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandSetActionPkg implements ICliCommand {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/set-action-pkg.txt");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "set-action-pkg";
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
		return "<pkg-name> <action-spec>:...";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Add a set of actions into a package.";
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
	public void invoke(IBuildStore buildStore, String buildStorePath, String[] args) {

		CliUtils.validateArgs(getName(), args, 2, 2, 
				"You must specify a package name and a colon-separated list of action-specs.");
		
		IPackageMemberMgr pkgMemberMgr = buildStore.getPackageMemberMgr();
		IActionMgr actionMgr = buildStore.getActionMgr();

		/* 
		 * The package can be of the form: "pkg". There is no scope value allowed
		 * for actions.
		 */
		String pkgName = args[0];
		int pkgAndScopeIds[] = CliUtils.parsePackageAndScope(buildStore, pkgName, false);
		int pkgId = pkgAndScopeIds[0];
		
		/* compute the ActionSet from the user-supplied list of action-specs */
		String actionSpecs = args[1];
		ActionSet actionsToSet = CliUtils.getCmdLineActionSet(actionMgr, actionSpecs);
		
		/* now visit each action in the ActionSet and set its package */
		boolean prevState = buildStore.setFastAccessMode(true);
		for (int actionId : actionsToSet) {
			pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_ACTION, actionId, pkgId);
		}
		buildStore.setFastAccessMode(prevState);	
	}

	/*-------------------------------------------------------------------------------------*/
}
