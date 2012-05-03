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
import com.arapiki.disco.model.BuildTasks;
import com.arapiki.disco.model.Components;
import com.arapiki.disco.model.types.TaskSet;

/**
 * Disco CLI Command class that implements the "set-task-comp" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandSetTaskComp implements ICliCommand {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/set-task-comp.txt");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "set-task-comp";
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
		return "<comp-name> <task-spec>:...";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.disco.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Add a set of tasks into a component.";
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

		CliUtils.validateArgs(getName(), args, 2, 2, "You must specify a component name and a colon-separated list of task-specs.");
		
		Components cmpts = buildStore.getComponents();
		BuildTasks bts = buildStore.getBuildTasks();

		/* 
		 * The component can be of the form: "comp". There is no section allowed
		 * for tasks.
		 */
		String compName = args[0];
		int compAndSectionIds[] = CliUtils.parseComponentAndScope(cmpts, compName, false);
		int compId = compAndSectionIds[0];
		
		/* compute the TaskSet from the user-supplied list of task-specs */
		String taskSpecs = args[1];
		TaskSet tasksToSet = CliUtils.getCmdLineTaskSet(bts, taskSpecs);
		
		/* now visit each task in the TaskSet and set its component */
		buildStore.setFastAccessMode(true);
		for (int task : tasksToSet) {
			cmpts.setTaskComponent(task, compId);
		}
		buildStore.setFastAccessMode(false);	
	}

	/*-------------------------------------------------------------------------------------*/
}
