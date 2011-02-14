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

import java.io.IOException;
import java.io.PrintStream;

/**
 * Various static methods for accessing the operating system features.
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class SystemUtils {
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Execute a shell command and capture the return code and output.
	 * @param cmd The command to be executed.
	 * @param stdin The text to be passed to the command's standard input.
	 * @return The command's standard output, error and return code in the form of a ShellResult object.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static ShellResult executeShellCmd(String cmd, String stdin) 
		throws IOException, InterruptedException {
		
		/* invoke the command as a sub process */
		Runtime run = Runtime.getRuntime();
		Process pr = run.exec(cmd);
		
		/* send the child process it's stdin, followed by an EOF */
		PrintStream childStdin = new PrintStream(pr.getOutputStream());
		childStdin.print(stdin);
		childStdin.close();
		
		/*
		 * Now, to separate out the stdout and stderr, we need to create two worker threads. Each
		 * thread reads the child process's stream into a string buffer, then terminates when the
		 * EOF is reached.
		 */
		StreamToStringBufferWorker stdOutWorker = new StreamToStringBufferWorker(pr.getInputStream());
		StreamToStringBufferWorker stdErrWorker = new StreamToStringBufferWorker(pr.getErrorStream());
		
		/* Start the threads and wait for them both to terminate */
		Thread stdOutThread = new Thread(stdOutWorker);
		Thread stdErrThread = new Thread(stdErrWorker);
		stdOutThread.start();
		stdErrThread.start();
		stdOutThread.join();
		stdErrThread.join();
		
		/* Wait for the child process to terminate - it probably has by now, but we need to be careful */
		pr.waitFor();

		/* 
		 * Place the child process's stdout, stderr and return code in a ShellResult structure. Note
		 * that getString() can throw an exception if for some reason the corresponding worker thread
		 * encountered an exception. We'll simply throw it back to our parent.
		 */
		ShellResult res = new ShellResult(stdOutWorker.getString(), stdErrWorker.getString(), pr.exitValue());
		return res;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
