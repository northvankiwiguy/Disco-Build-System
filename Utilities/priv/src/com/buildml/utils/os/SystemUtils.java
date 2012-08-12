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

package com.buildml.utils.os;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.buildml.utils.errors.FatalError;

/**
 * Various static methods for accessing the operating system's features.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class SystemUtils {
	
	/*=====================================================================================*
	 * FIELDS/TYPE
	 *=====================================================================================*/
	
	/*
	 * These flags are used as a bit map to inform traverseFileSystem which type of
	 * files/directories/symlinks we're interested in knowing about.
	 */
	
	/** Flag to indicate we're interested in seeing files. */ 
	public static final int REPORT_FILES = 1;
	
	/** Flag to indicate we're interested in seeing directories. */
	public static final int REPORT_DIRECTORIES = 2;

	/** Flag to indicate we're interested in seeing symlinks. */
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
		 * Use the BUILDML_HOME environment variable to determine where our dynamically loadable
		 * libraries are stored.
		 */
		String buildMlHome = System.getenv("BUILDML_HOME");
		if (buildMlHome == null) {
			buildMlHome = System.getProperty("BUILDML_HOME");
			if (buildMlHome == null) {
				throw new FatalError("The BUILDML_HOME environment variable is not set.");
			}
		}

		/* load our JNI libraries */
		try {
			System.load(buildMlHome + "/lib/libnativeLib.so");
		} catch (UnsatisfiedLinkError ex) {
			throw new FatalError("Unable to load native methods: " + ex.getMessage(), ex);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test whether a local file is a symlink (as opposed to a regular file or directory).
	 * 
	 * @param fileName The file's name.
	 * @return True if the file is a symlink, else false.
	 * @exception FileNotFoundException If the file doesn't exist.
	 */
	public static native boolean isSymlink(String fileName)
		throws FileNotFoundException;
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Read the target of the specified symlink.
	 * 
	 * @param fileName The name of the symlink.
	 * @return The target of the symlink, or null if it's not a symlink, or if some other 
	 *          error occurred.
	 * @exception FileNotFoundException If the file doesn't exist.
	 */
	public static native String readSymlink(String fileName)
		throws FileNotFoundException;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create a new symlink on the local machine's file system.
	 * 
	 * @param fileName The name of the symlink that will be created.
	 * @param targetFileName The destination of the symlink.
	 * @return 0 if the symlink was created successfully, otherwise non-zero.
	 */
	public static native int createSymlink(String fileName, String targetFileName);
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Execute a shell command and capture the return code, standard output and standard error
	 * strings.
	 * 
	 * @param cmd The shell command to be executed.
	 * @param stdin The text to be passed to the command's standard input.
	 * @param echoStream If not null, the process's stdout and stderr should be echoed to this stream.
	 * @param saveToBuffer Set if the stdout and stderr should be saved in buffers.
	 * @param workingDir If not null, the working directory the command should be executed in.
	 * 
	 * @return The command's standard output, error and return code in the form of a ShellResult object. If
	 * saveToBuffer is false, then the output/error fields will be empty.
	 * 
	 * @throws IOException For some reason the shell command failed to execute.
	 * @throws InterruptedException The command was interrupted before it completed.
	 */
	public static ShellResult executeShellCmd(
			String cmd, String stdin, PrintStream echoStream, 
			boolean saveToBuffer, File workingDir) 
		throws IOException, InterruptedException {
		
		/* invoke the command as a sub process */
		Runtime run = Runtime.getRuntime();
		Process pr = run.exec(cmd, null, workingDir);
		
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
				saveToBuffer, echoStream);
		StreamToStringBufferWorker stdErrWorker = new StreamToStringBufferWorker(pr.getErrorStream(),
				saveToBuffer, echoStream);
		
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
	 * 
	 * @param cmd The command to be executed.
	 * @param stdin The text to be passed to the command's standard input.
	 * 
	 * @return The command's standard output, error and return code in the form of a ShellResult object.
	 * 
	 * @throws IOException For some reason the shell command failed to execute.
	 * @throws InterruptedException The command was interrupted before it completed.
	 */
	public static ShellResult executeShellCmd(String cmd, String stdin) 
		throws IOException, InterruptedException {
	
		return executeShellCmd(cmd, stdin, null, true, null);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Traverse a file system and invoke a callback method on each file system entry (file,
	 * directory or symlink).
	 * 
	 * @param rootPath The starting (top) path for the traversal. The traversal will start
	 * at this point, and traverse downwards through the file system sub-directories.
	 * @param pathsToReport The types of paths to report - a bitmap of REPORT_FILES, 
	 * REPORT_DIRECTORIES and REPORT_SYMLINKS.
	 * @param callbackObj The callback object to be invoked as each file system entry
	 * is encountered.
	 */
	public static void traverseFileSystem(String rootPath, int pathsToReport, 
			FileSystemTraverseCallback callbackObj) {

		File rootFile = new File(rootPath);
		traverseFileSystemHelper(rootFile, null, null, pathsToReport, callbackObj);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Traverse a file system and invoke a callback method on each file system entry (file,
	 * directory or symlink).
	 * 
	 * @param rootPath The starting (top) path for the traversal. The traversal will start
	 * at this point, and traverse downwards through the file system sub-directories.
	 * @param matchFilePattern A Regex pattern specifying the types of file name we're 
	 * interested in hearing about (use null to match everything).
	 * @param ignoreDirPattern A Regex pattern specifying the types of directory names we're
	 * interested in skipping over and not traversing (use null to not skip over anything).
	 * @param pathsToReport The types of paths to report - a bitmap of REPORT_FILES, 
	 * REPORT_DIRECTORIES and REPORT_SYMLINKS.
	 * @param callbackObj The callback object to be invoked as each file system entry
	 * is encountered.
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
	 * A recursive helper function for traverseFileSystem().
	 * 
	 * @param thisPath The current path being traversed.
	 * @param mfPattern The matching file pattern.
	 * @param idPattern The ignore directory pattern.
	 * @param pathsToReport A bitmap of which path types should be reported.
	 * @param callbackObj The object to "call back" when a matching path is found.
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

	/**
	 * Create a temporary directory on the file system.
	 * @return The newly-created directory.
	 * @throws IOException If the directory couldn't be created.
	 */
	public static File createTempDir() throws IOException {
		
		File tmpDir = File.createTempFile("tempDir", null);
		if (!tmpDir.delete() || !tmpDir.mkdir()) {
			throw new IOException("Couldn't make temporary directory: " + tmpDir);
		}
		return tmpDir;
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Delete a file system file or directory (and all the files and sub-directories it may
	 * contain).
	 * @param fileOrDir The file or directory to be deleted.
	 * @return True or false, to indicate whether the deletion was successful.
	 */
	public static boolean deleteDirectory(File fileOrDir) {
	    if (fileOrDir.isDirectory()) {
	        String[] children = fileOrDir.list();
	        for (int i=0; i < children.length; i++) {
	            if (!deleteDirectory(new File(fileOrDir, children[i]))){
	                return false;
	            }
	        }
	    }
	    return fileOrDir.delete();
	}
	
	/*-------------------------------------------------------------------------------------*/
	
}