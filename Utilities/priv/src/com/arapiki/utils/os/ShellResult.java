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
	
	private String stdout;					/* the process's standard output */
	private String stderr;					/* the process's standard error */
	private int returnCode;				/* the process's return code */
	
	/*
	 * Create a new ShellResult object.
	 */
	public ShellResult(String stdout, String stderr, int returnCode) {
		this.stdout = stdout;
		this.stderr = stderr;
		this.returnCode = returnCode;
	}
	
	/**
	 * @return the stdout
	 */
	public String getStdout() {
		return stdout;
	}
	
	/**
	 * @return the stderr
	 */
	public String getStderr() {
		return stderr;
	}
	
	/**
	 * @return the returnCode
	 */
	public int getReturnCode() {
		return returnCode;
	}

}
