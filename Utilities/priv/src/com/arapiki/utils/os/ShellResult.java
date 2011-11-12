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

package com.arapiki.utils.os;

/**
 * This class is used as a return value from the executeShellCmd() method. Upon return,
 * each of these fields contains the output from the process. The size of the String's in
 * this class will depend on the amount of output provided by the shell command.
 */
public class ShellResult {
	
	/** The process's standard output */
	private String stdout;
	
	/** The process's standard error */
	private String stderr;
	
	/** The process's return code */
	private int returnCode;
	
	/**
	 * Create a new ShellResult object. This object reflects the output and return code
	 * from a shell command invocation.
	 * @param stdout The text captured from the command's standard output
	 * @param stderr The text captured from the command's standard error
	 * @param returnCode The command's return code
	 */
	public ShellResult(String stdout, String stderr, int returnCode) {
		this.stdout = stdout;
		this.stderr = stderr;
		this.returnCode = returnCode;
	}
	
	/**
	 * @return The command's standard output
	 */
	public String getStdout() {
		return stdout;
	}
	
	/**
	 * @return The command's standard error
	 */
	public String getStderr() {
		return stderr;
	}
	
	/**
	 * @return The command's return code
	 */
	public int getReturnCode() {
		return returnCode;
	}

}
