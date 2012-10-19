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
import com.buildml.model.impl.BuildTasks;
import com.buildml.model.types.TaskSet;

/**
 * BuildML CLI Command class that implements the "set-task-pkg" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandSetTaskPkg implements ICliCommand {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/set-task-pkg.txt");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "set-task-pkg";
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
		return "<pkg-name> <task-spec>:...";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Add a set of tasks into a package.";
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

		CliUtils.validateArgs(getName(), args, 2, 2, "You must specify a package name and a colon-separated list of task-specs.");
		
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		BuildTasks bts = buildStore.getBuildTasks();

		/* 
		 * The package can be of the form: "pkg". There is no section allowed
		 * for tasks.
		 */
		String pkgName = args[0];
		int pkgAndScopeIds[] = CliUtils.parsePackageAndScope(pkgMgr, pkgName, false);
		int pkgId = pkgAndScopeIds[0];
		
		/* compute the TaskSet from the user-supplied list of task-specs */
		String taskSpecs = args[1];
		TaskSet tasksToSet = CliUtils.getCmdLineTaskSet(bts, taskSpecs);
		
		/* now visit each task in the TaskSet and set its package */
		buildStore.setFastAccessMode(true);
		for (int task : tasksToSet) {
			pkgMgr.setTaskPackage(task, pkgId);
		}
		buildStore.setFastAccessMode(false);	
	}

	/*-------------------------------------------------------------------------------------*/
}
