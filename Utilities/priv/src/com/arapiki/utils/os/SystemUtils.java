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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.arapiki.utils.errors.FatalError;

/**
 * Various static methods for accessing the operating system's features.
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
	 * Static block - called when the class is first loaded. This will load any native
	 * libraries that we need.
	 */
	static {
		/* 
		 * Use the DISCO_HOME environment variable to determine where our dynamically loadable
		 * libraries are stored.
		 */
		String discoHome = System.getenv("DISCO_HOME");
		if (discoHome == null) {
			throw new FatalError("The DISCO_HOME environment variable is not set.");
		}

		/* load our JNI libraries */
		try {
			System.load(discoHome + "/lib/libnativeLib.so");
		} catch (UnsatisfiedLinkError ex) {
			throw new FatalError("Unable to load native methods: " + ex.getMessage(), ex);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test whether a local file is a symlink (as opposed to a regular file or directory)
	 * @param The file's name.
	 * @return True if the file is a symlink, else false.
	 * @exception FileNotFoundException If the file doesn't exist
	 */
	public static native boolean isSymlink(String fileName)
		throws FileNotFoundException;
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Read the target of the specified symlink.
	 * @param fileName The name of the symlink
	 * @return The target of the symlink, or null if it's not a symlink, or if some other 
	 *          error occurred.
	 * @exception FileNotFoundException If the file doesn't exist
	 */
	public static native String readSymlink(String fileName)
		throws FileNotFoundException;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create a new symlink on the local machine's file system.
	 * @param fileName The name of the symlink that will be created.
	 * @param targetFileName The destination of the symlink.
	 * @return 0 if the symlink was created successfully, otherwise non-zero.
	 */
	public static native int createSymlink(String fileName, String targetFileName);
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Execute a shell command and capture the return code and output.
	 * @param cmd The command to be executed.
	 * @param stdin The text to be passed to the command's standard input.
	 * @param echoToOutput Should the stdout and stderr be echoed to our own process's output channels?
	 * @param saveToBuffer Should the stdout and stderr be saved in buffers?
	 * @return The command's standard output, error and return code in the form of a ShellResult object. If
	 * saveToBuffer is false, then the output/error fields will be empty.
	 * @throws IOException For some reason the shell command failed to execute
	 * @throws InterruptedException The command was interrupted before it completed
	 */
	public static ShellResult executeShellCmd(String cmd, String stdin, boolean echoToOutput, boolean saveToBuffer) 
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
		StreamToStringBufferWorker stdOutWorker = new StreamToStringBufferWorker(pr.getInputStream(),
				echoToOutput, saveToBuffer, System.out);
		StreamToStringBufferWorker stdErrWorker = new StreamToStringBufferWorker(pr.getErrorStream(),
				echoToOutput, saveToBuffer, System.err);
		
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
	 * A variant of executeShellCmd that defaults to saving output/error to a buffer, but not echoing it
	 * @param cmd The command to be executed.
	 * @param stdin The text to be passed to the command's standard input.
	 * @return The command's standard output, error and return code in the form of a ShellResult object.
	 * @throws IOException For some reason the shell command failed to execute
	 * @throws InterruptedException The command was interrupted before it completed
	 */
	public static ShellResult executeShellCmd(String cmd, String stdin) 
		throws IOException, InterruptedException {
	
		return executeShellCmd(cmd, stdin, false, true);
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
	
	/*-------------------------------------------------------------------------------------*/


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

	/*-------------------------------------------------------------------------------------*/

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
	
	/*-------------------------------------------------------------------------------------*/
}