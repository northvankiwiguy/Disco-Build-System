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

package com.buildml.scanner.legacy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import com.buildml.model.IBuildStore;
import com.buildml.scanner.FatalBuildScannerError;
import com.buildml.utils.os.ShellResult;
import com.buildml.utils.os.SystemUtils;

/**
 * This class is the main entry point for scanning a legacy shell-command-based
 * build process, and creating a corresponding BuildStore. This is a wrapper for
 * the "cfs" command-line tool, and we also parse the "cfs.trace file" that cfs generates. 
 * The output from this class is a fully populated BuildStore.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class LegacyBuildScanner {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/
	
	/** The default trace file name, which is used if no other name is provided. */
	private static final String DEFAULT_TRACE_FILE_NAME = "cfs.trace";
	
	/** The trace file name the user has chosen. */
	private String traceFilePathName;
	
	/** 
	 * The BuildStore we should generate, or null if we should parse the trace file without
	 * creating a BuildStore.
	 */
	private IBuildStore buildStore = null;
	
	/**
	 * The debug verbosity level for showing the progress of a trace. 0 = none, 1 = some,
	 * 2 = extended debug. Note that unless debugStream is also set, there will be no
	 * debug output at all.
	 */
	private int debugLevel = 0;
	
	/** The default log file name, used if no name is chosen by the user. */
	private static final String DEFAULT_LOG_FILE_NAME = "cfs.log";
	
	/** The debug log file name the user has chosen. */
	private String logFileName;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new LegacyBuildScanner object. By default, there is no BuildStore attached
	 * to this scanner (use setBuildStore() to set one), and the default trace and log file 
	 * names will be used (use setTraceFile() and setLogFile() to change them).
	 */
	public LegacyBuildScanner() {
		
		/* set the default trace and log file names */
		setTraceFile(null);
		setLogFile(null);
		setBuildStore(null);
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Set this scanner's trace file name. An output file with this name will be generated
	 * when traceShellCommand() is called, and will be read when parseTraceFile() is called.
	 * Given the potentially enormous size of a trace file (> 1GB), it doesn't make sense
	 * to keep this information in memory, so an intermediate file is necessary.
	 * 
	 * @param traceFilePathName The name of the file to scan to/from. If null, set
	 * the path name back to the default.
	 */
	public void setTraceFile(String traceFilePathName) {

		/* if a file name is provided... */
		if (traceFilePathName != null) {
			this.traceFilePathName = traceFilePathName;
		} 
		
		/* else, null means revert to default name */
		else {
			this.traceFilePathName = DEFAULT_TRACE_FILE_NAME;
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Return the current trace file path name.
	 * @return The current trace file path name.
	 */
	public String getTraceFile() {
		return this.traceFilePathName;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set this scanner's log file name. This file will be freshly created, and used as
	 * the debug log file for all CFS operations (system calls, etc), as well as all output
	 * from the TraceFileScanner() class.
	 * 
	 * @param logFileName The name of the log file to write debug information to.
	 */
	public void setLogFile(String logFileName) {

		/* if a file name is provided... */
		if (logFileName != null) {
			this.logFileName = logFileName;
		} 
		
		/* else, null means revert to default name */
		else {
			this.logFileName = DEFAULT_LOG_FILE_NAME;
		}
		
		/* 
		 * Make sure it's an absolute path, otherwise when the program being traced does
		 * a chdir, it'll start writing the log file into that directory instead.
		 */
		this.logFileName = new File(this.logFileName).getAbsolutePath();
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Return the current debug log file name.
	 * @return The current debug log file name.
	 */
	public String getLogFile() {
		return this.logFileName;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set the BuildStore object that the scanner should add the build process to.
	 * 
	 * @param buildStore The BuildStore to collect information in, or null to not collect
	 * information.
	 */
	public void setBuildStore(IBuildStore buildStore) {
		this.buildStore = buildStore;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the BuildStore object that this scanner has been asked to add the build process to.
	 * 
	 * @return The BuildStore we'll write the trace file's data into.
	 */
	public IBuildStore getBuildStore() {
		return buildStore;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set the debug level of the scanner to control how much debug output is displayed.
	 * 
	 * @param level 0 (none), 1 (basic debug), 2 (extended debug). Any value > 2
	 * is consider to be the same as 2.
	 */
	public void setDebugLevel(int level) {
		
		/* validate the range, and restrict to meaningful values (without giving an error) */
		if (level < 0) {
			level = 0;
		} else if (level > 2) {
			level = 2;
		}
		
		debugLevel = level;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the current debug level (0, 1 or 2).
	 * 
	 * @return The current debug level (0, 1 or 2).
	 */
	public int getDebugLevel() {
		return debugLevel;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Invoke a shell command, trace the behavior of the command to see which sub-processes
	 * are created, and which files are accessed by those processes, then generate a trace file
	 * as output. Note: this method does not create a BuildStore, but generates the
	 * trace file that parseTraceFile() can use to populate a BuildStore.
	 * 
	 * @param args The shell command line arguments (as would normally be passed into a main()
	 * function).
	 * @param workingDir If not null, the directory in which to execute the command (if null,
	 * 			use the current directory).
	 * @param outStream The PrintStream on which the traced command's output should be displayed.
	 * @param useShell If true, pass the single (quoted) command line argument through a shell.
	 * @throws InterruptedException The scan operation was interrupted before it completed fully.
	 * @throws IOException The build command was not found, or failed to execute for some reason.
	 */
	public void traceShellCommand(String args[], File workingDir, PrintStream outStream, boolean useShell) 
			throws IOException, InterruptedException {
		
		/* locate the "cfs" executable program (in $BUILDML_HOME/bin) */
		String buildMlHome = System.getenv("BUILDML_HOME");
		if (buildMlHome == null) {
			buildMlHome = System.getProperty("BUILDML_HOME");
			if (buildMlHome == null) {
				throw new IOException(
						"Unable to locate cfs tool. BUILDML_HOME environment variable not set.");
			}
		}
		
		/* 
		 * Create an array of all the command line arguments. If the user 
		 * specified --trace-file, we also pass that to the cfs command.
		 */
		ArrayList<String> allArgs = new ArrayList<String>(args.length + 10);
		allArgs.add(buildMlHome + "/bin/cfs");
		
		/* pass the trace file name (which will default to "cfs.trace" otherwise) */
		allArgs.add("-o");
		allArgs.add(traceFilePathName);
		
		/* pass the log file name (which will default to "cfs.log" otherwise) */
		allArgs.add("-l");
		allArgs.add(logFileName);
				
		/* pass debug flags */
		allArgs.add("-d");
		allArgs.add(String.valueOf(getDebugLevel()));
		
		/* should the command argument be passed through a shell? */
		if (useShell) {
			allArgs.add("-c");
		}
		
		/* now the command's arguments */
		for (int i = 0; i < args.length; i++) {
			allArgs.add(args[i]);
		}
		
		/* 
		 * Execute the command, echoing the output/error to our console (but don't capture it
		 * in a buffer since we won't be looking at it.
		 */
		String allArgsArray[] = allArgs.toArray(new String[0]);
		ShellResult result = 
				SystemUtils.executeShellCmd(allArgsArray, "", outStream, false, workingDir);
		if (result.getReturnCode() != 0) {
			String errString = "";
			for (int i = 0; i < allArgsArray.length; i++) {
				errString += (allArgsArray[i] + " ");
			}
			throw new IOException("Failed to execute shell command: " + errString);
		}
		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Parse the content of an existing trace file, as is generated by traceShellCommand(). 
	 * If the caller had previously invoked setBuildStore() with a BuildStore object, this 
	 * method will add the parsed information to that BuildStore.
	 */
	public void parseTraceFile() {
		/*
		 * We now have a cfs.trace file in the current directory. We should parse this file
		 * and read the content into our BuildStore.
		 */
		TraceFileScanner scanner = null;
		
		/* 
		 * Open the log file for writing (we append, since we don't want to overwrite
		 * data that CFS stored in the file).
		 */
		PrintStream debugOut;
		try {
			debugOut = new PrintStream(new FileOutputStream(logFileName, true));
		} catch (FileNotFoundException e1) {
			throw new FatalBuildScannerError("Log file not found: " + logFileName);			
		}
		
		try {
			scanner = new TraceFileScanner(traceFilePathName, 
					getBuildStore(), debugOut, getDebugLevel());
			scanner.parse();
			scanner.close();
			
		} catch (FileNotFoundException e) {
			throw new FatalBuildScannerError("Trace file not found: " + traceFilePathName);
			
		} catch (IOException e) {
			throw new FatalBuildScannerError("Can't parse trace file: " + traceFilePathName);
		}
		
		/* close the debug log file */
		debugOut.close();
	}
	
	/*-------------------------------------------------------------------------------------*/

}
