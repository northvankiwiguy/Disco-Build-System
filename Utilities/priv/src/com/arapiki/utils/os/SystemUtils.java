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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Various static methods for accessing the operating system features.
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class SystemUtils {
	
	/*=====================================================================================*
	 * FIELDS/TYPE
	 *=====================================================================================*/
	
	/**
	 * These flags are used as a bit map to inform traverseFileSystem which type of
	 * files/directories/symlinks we're interested in knowing about.
	 */
	public static final int REPORT_FILES = 1;
	public static final int REPORT_DIRECTORIES = 2;
	public static final int REPORT_SYMLINKS = 4;
	
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

	/**
	 * 
	 */
	public static void traverseFileSystem(String rootPath, int pathsToReport, 
			FileSystemTraverseCallback callbackObj) {

		File rootFile = new File(rootPath);
		traverseFileSystemHelper(rootFile, null, null, pathsToReport, callbackObj);
	}

	/**
	 * 
	 * @param rootPath
	 * @param matchFilePattern
	 * @param ignoreDirPattern
	 * @param pathsToReport
	 * @param callbackObj
	 */
	public static void traverseFileSystem(String rootPath, String matchFilePattern, String ignoreDirPattern, 
			int pathsToReport, FileSystemTraverseCallback callbackObj) {

		File rootFile = new File(rootPath);
		Pattern mfPattern = null;
		if (matchFilePattern != null) {
			mfPattern = Pattern.compile(matchFilePattern);
		}
		Pattern idPattern = null;
		if (ignoreDirPattern != null) {
			idPattern = Pattern.compile(ignoreDirPattern);
		}
		
		traverseFileSystemHelper(rootFile, mfPattern, idPattern, pathsToReport, callbackObj);
	}

	/**
	 * 
	 * @param thisPath
	 * @param pathsToReport
	 * @param callbackObj
	 */
	private static void traverseFileSystemHelper(File thisPath, Pattern mfPattern, Pattern idPattern,
			int pathsToReport, FileSystemTraverseCallback callbackObj) {
		
		/* if the file doesn't actually exist on the local file system, there's nothing to do */
		if (!thisPath.exists()) {
			return;
		}
	
		String fileName = thisPath.getName();
		
		/* if the file is actually a directory, recursively visit each of the entries */
		if (thisPath.isDirectory()) {

			/* check whether the caller wanted to exclude this directory from the traversal */
			Matcher m = null;
			if (idPattern != null) {
				m = idPattern.matcher(fileName);
			}
			if ((m == null) || (!m.matches())) {
				
				/* if the user requested it, report this directory to them */
				if ((pathsToReport & REPORT_DIRECTORIES) != 0) {
					callbackObj.callback(thisPath);
				}	

				/* traverse the children */
				File children [] = thisPath.listFiles();
				if (children == null) {
					return;
				}
				for (int i = 0; i < children.length; i++) {
					traverseFileSystemHelper(children[i], mfPattern, idPattern, pathsToReport, callbackObj);
				}
			}
		}
	
		/* else if it's a file, and the user is interested in hearing about it */
		else if (thisPath.isFile()) {
			
			if ((pathsToReport & REPORT_FILES) != 0) {
				
				/* if a pattern was provided, only report names that match */
				Matcher m = null;
				if (mfPattern != null) {
					m = mfPattern.matcher(fileName);
				}
				if ((m == null) || (m.matches())){
					callbackObj.callback(thisPath);
				}
			}
		}
	
		/* else, it's not a file or directory - throw an error, for now */
		else {
			throw new Error("Found a path that isn't a file or directory: " + thisPath.toString());
		}
	}
}