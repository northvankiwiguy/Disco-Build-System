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

package com.arapiki.disco.main;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import com.arapiki.disco.model.BuildStore;

/**
 * This interface must be implemented by any class that provides a disco CLI
 * command. The DiscoMain class uses these class to identify each CLI command's
 * name, options, arguments, and human-readable descriptions. It also processes
 * command line options and invokes the command via this interface.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public interface ICliCommand {

	/**
	 * Fetch the name of this CLI command (this is what the user actually types)
	 * @return The command's name
	 */
	public String getName();
	
	/**
	 * Fetch the command's parameter description, which is the syntax of the parameters that 
	 * the command accepts (e.g. "<comp-name> <path>, ...").
	 * @return The command's parameter description
	 */
	public String getParameterDescription();
	
	/**
	 * Fetch the command's one-line description. For example, "Show the list of tasks".
	 * @return The command's one-line description
	 */
	public String getShortDescription();
	
	/**
	 * Fetch the command's multi-line description. This is the full help text for this command.
	 * @return The command's full multi-line description.
	 */
	public String getLongDescription();
	
	/**
	 * Fetch this command's command line options.
	 * @return The command's command line options.
	 */
	public Options getOptions();
	
	/**
	 * DiscoMain invokes this method to process the user-supplied options. Each CliCommand*
	 * object should process these options in its own way and set its internal state
	 * appropriately. 
	 * @param cmdLine The already-processed command line.
	 */
	public void processOptions(CommandLine cmdLine);
	
	/**
	 * Invoke this command, using the provided command line arguments. If an error occurs,
	 * this method is entitled to exit the whole program.
	 * @param buildStore The BuildStore to perform the command on
	 * @param args The command line arguments (options and normal arguments)
	 */
	public void invoke(BuildStore buildStore, String [] args);
	
}
