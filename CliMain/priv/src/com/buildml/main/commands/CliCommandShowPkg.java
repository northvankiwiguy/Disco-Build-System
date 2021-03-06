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

import java.io.PrintStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import com.buildml.main.CliUtils;
import com.buildml.main.ICliCommand;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.utils.print.PrintUtils;

/**
 * BuildML CLI Command class that implements the "show-pkg" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandShowPkg implements ICliCommand {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/show-pkg.txt");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "show-pkg";
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
		return "";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Show the packages defined in the build system.";
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

		CliUtils.validateArgs(getName(), args, 0, 0, "No arguments expected.");
	
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		
		int rootId = pkgMgr.getRootFolder();
		invokeHelper(System.out, pkgMgr, rootId, 0);
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * A helper function for displaying a package folder hierarchy. This method is called
	 * recursively to display the package tree.
	 * 
	 * @param out 	   The PrintStream on which the tree should be displayed.
	 * @param pkgMgr   The PackageMgr that owns the packages.
	 * @param thisId   The current package/folder ID (at this level of the tree).
	 * @param indent   The indent level.
	 */
	private void invokeHelper(PrintStream out, IPackageMgr pkgMgr, int thisId, int indent) {
		
		String name = pkgMgr.getName(thisId);
		boolean isFolder = pkgMgr.isFolder(thisId);
		
		PrintUtils.indent(out, indent);
		
		if (isFolder) {
			out.println(name + "/");
			
			Integer children[] = pkgMgr.getFolderChildren(thisId);
			for (int i = 0; i < children.length; i++) {
				invokeHelper(out, pkgMgr, children[i], indent + 2);
			}
			
		} else {
			out.println(name);
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
