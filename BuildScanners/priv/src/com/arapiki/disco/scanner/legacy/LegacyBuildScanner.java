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

package com.arapiki.disco.scanner.legacy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.scanner.FatalBuildScannerError;
import com.arapiki.utils.os.ShellResult;
import com.arapiki.utils.os.SystemUtils;

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
	private BuildStore buildStore = null;
	
	/**
	 * The PrintStream we should send debug output to. If set to null, the debug output 
	 * will be disabled.
	 */
	private PrintStream debugStream = null;
	
	/**
	 * The debug verbosity level for showing the progress of a trace. 0 = none, 1 = some,
	 * 2 = extended debug. Note that unless debugStream is also set, there will be no
	 * debug output at all.
	 */
	private int debugLevel = 0;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new LegacyBuildScanner object. By default, there is no BuildStore attached
	 * to this scanner (use setBuildStore() to set one), and the default trace file name will
	 * be used (use setTraceFile() to change it).
	 */
	public LegacyBuildScanner() {
		
		/* set the default trace file name */
		setTraceFile(null);
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
	 * Set the BuildStore object that the scanner should add the build process to.
	 * 
	 * @param buildStore The BuildStore to collect information in, or null to not collect
	 * information.
	 */
	public void setBuildStore(BuildStore buildStore) {
		this.buildStore = buildStore;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the BuildStore object that this scanner has been asked to add the build process to.
	 * 
	 * @return The BuildStore we'll write the trace file's data into.
	 */
	public BuildStore getBuildStore() {
		return buildStore;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Set the PrintStream object on which debugging information will be displayed.
	 * 
	 * @param debugStream The debugStream, or null to not display debug information.
	 */
	public void setDebugStream(PrintStream debugStream) {
		this.debugStream = debugStream;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the current PrintStream used for displaying debug information.
	 *
	 * @return The current PrintStream used for displaying debug information.
	 */
	public PrintStream getDebugStream() {
		return debugStream;
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
	 * @throws InterruptedException The scan operation was interrupted before it completed fully.
	 * @throws IOException The build command was not found, or failed to execute for some reason.
	 */
	public void traceShellCommand(String args[]) throws IOException, InterruptedException {
		
		/* locate the "cfs" executable program (in $DISCO_HOME/bin) */
		String discoHome = System.getenv("DISCO_HOME");
		if (discoHome == null) {
			throw new IOException("Unable to locate cfs tool. DISCO_HOME environment variable not set.");
		}
		
		/* 
		 * Create a single command line string, by joining all the arguments. If
		 * the user specified --trace-file, we also pass that to the cfs command.
		 */
		StringBuffer sb = new StringBuffer();
		sb.append(discoHome);
		sb.append("/bin/cfs ");
		
		/* pass the trace file name (which will default to "cfs.trace" otherwise) */
		sb.append("-o ");
		sb.append(traceFilePathName);
		sb.append(" ");
		
		/* pass debug flags */
		sb.append("-d ");
		sb.append(getDebugLevel());
		sb.append(" ");
		
		/* now the command's arguments */
		for (int i = 0; i < args.length; i++) {
			sb.append(args[i]);
			sb.append(' ');
		}
		
		/* 
		 * Execute the command, echoing the output/error to our console (but don't capture it
		 * in a buffer since we won't be looking at it.
		 */
		String commandLine = sb.toString();
		ShellResult result = SystemUtils.executeShellCmd(commandLine, "", true, false);
		if (result.getReturnCode() != 0) {
			throw new IOException("Failed to execute shell command: " + commandLine);
		}
		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Parse the content of an existing trace file, as is generated by traceShellCommand(). 
	 * If the caller had previously invoked setBuildStore() with a BuildStore object, this 
	 * method will add the parsed information to that BuildStore. If the user had invoked 
	 * setDebugStream(), debug information will be dumped to that stream. In normal operation,
	 * users would call setBuildStore(), but probably won't call setDebugStream().
	 */
	public void parseTraceFile() {
		/*
		 * We now have a cfs.trace file in the current directory. We should parse this file
		 * and read the content into our BuildStore.
		 */
		TraceFileScanner scanner = null;
		
		try {
			scanner = new TraceFileScanner(traceFilePathName, 
					getBuildStore(), getDebugStream(), getDebugLevel());
			scanner.parse();
			scanner.close();
			
		} catch (FileNotFoundException e) {
			throw new FatalBuildScannerError("Trace file not found: " + traceFilePathName);
			
		} catch (IOException e) {
			throw new FatalBuildScannerError("Can't parse trace file: " + traceFilePathName);
		}		
	}
	
	/*-------------------------------------------------------------------------------------*/

}
