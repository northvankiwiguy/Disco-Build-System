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

package com.buildml.main;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import com.buildml.model.impl.BuildStore;

/**
 * This interface must be implemented by any class that provides a BuildML CLI
 * command. The CliMain class uses these classes to identify each CLI command's
 * name, options, arguments, and human-readable descriptions. It also processes
 * command line options and invokes the command via this interface.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public interface ICliCommand {

	/**
	 * Fetch the name of this CLI command (such as "show-files" or "show-tasks").
	 * @return The command's name.
	 */
	public String getName();
	
	/**
	 * Fetch the command's parameter description, which is the syntax of the parameters that 
	 * the command accepts. For example, "&lt;pkg-name&gt; &lt;path&gt;, ...".
	 * @return The command's parameter description.
	 */
	public String getParameterDescription();
	
	/**
	 * Fetch the command's one-line description. For example, "Show the list of tasks".
	 * @return The command's one-line description.
	 */
	public String getShortDescription();
	
	/**
	 * Fetch the command's multi-line description. This is the full help text for this command
	 * which may contain many hundreds of lines of output.
	 * @return The command's full multi-line description.
	 */
	public String getLongDescription();
	
	/**
	 * Fetch this command's command line options.
	 * @return The command's command line options.
	 */
	public Options getOptions();
	
	/**
	 * Pre-process the user-supplied options, in preparation for invoking the command. 
	 * Each CliCommand* object should process these options in its own way and set its internal 
	 * state appropriately. This method will be called immediately before invoke() is called.
	 * @param buildStore The BuildStore we'll operate on.
	 * @param cmdLine The pre-parsed command line, which contains the command's options.
	 */
	public void processOptions(BuildStore buildStore, CommandLine cmdLine);
	
	/**
	 * Invoke this command, using the provided command line arguments. If an error occurs,
	 * this method is entitled to abort the whole program without returning. 
	 * It can be assumed that processOptions() has already been called to configure 
	 * the command's options.
	 * @param buildStore The BuildStore to perform the command on.
	 * @param args The command line arguments (normal "non-option" arguments only).
	 */
	public void invoke(BuildStore buildStore, String [] args);
	
}
