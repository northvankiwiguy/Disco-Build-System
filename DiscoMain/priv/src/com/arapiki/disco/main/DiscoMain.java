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

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.cli.*;

import com.arapiki.disco.model.BuildStore;

/**
 * This is the main entry point for the "disco" command line program. All other projects (with
 * the exception of the Eclipse plug-in are invoked from this point.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public final class DiscoMain {	
	
	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/
	
	/* which file to use for the BuildStore database */
	private static String buildStoreFileName = "buildstore";	
	
	/* when validating command line args - this value is considered infinite */
	private static final int ARGS_INFINITE = -1;
	
	/* The command line options, as used by the Apache Commons CLI library */
	private static Options opts = null;
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * Process the input command line arguments, using the Apache Commons CLI library.
	 * @param args The standard command line array from the main() method.
	 * @return
	 */
	private static String[] processCommandLineOptions(String[] args) {
		
		/* create a new Apache Commons CLI parser, using Posix style arguments */
		CommandLineParser parser = new PosixParser();
		
		/* define the disco command's arguments */
		opts = new Options();

		/* add the -f / --buildstore-file option */
		Option fOpt = new Option("f", "buildstore-file", true, "Name of buildstore to query/edit");
		fOpt.setArgName("file-name");
		opts.addOption(fOpt);
	
		/*
		 * Initiate the parsing process - also, report on any options that require
		 * an argument but didn't receive one.
		 */
		CommandLine line = null;
	    try {
	    	line = parser.parse(opts, args);

		} catch (ParseException e) {
			reportErrorAndExit(e.getMessage());
		}
		
		/*
		 * Validate all the options and their argument values.
		 */
		if (line.hasOption('f')){
			buildStoreFileName = line.getOptionValue('f');
		}
		
		/*
		 * Validate that at least one more argument (the command name) is provided.
		 */
		String remainingArgs[] = line.getArgs();
		if (remainingArgs.length == 0) {
			reportErrorAndExit("Missing command - please specify an operation to perform.");
		}
		
		/* display help? */
		if (remainingArgs[0].equals("help")){
			reportErrorAndExit("");
		}
		
		/* return the array of arguments including the command name */
		return remainingArgs;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Display a formatted error message to explain that the command line args
	 * were invalid. Note: this method never returns, instead the whole program
	 * is aborted.
	 * @param opts The expected command line options.
	 * @param message A special purpose string error message.
	 */
	@SuppressWarnings("unchecked")
	private static void reportErrorAndExit(String message) {
		System.err.println("\nUsage: disco [ options ] command { command-args }");
		System.err.println("\nOptions:");
		
		Collection<Option> optList = opts.getOptions();
		for (Iterator<Option> iterator = optList.iterator(); iterator.hasNext();) {
			Option thisOpt = iterator.next();
			System.err.println("  -" + thisOpt.getOpt() + " | --" + 
					thisOpt.getLongOpt() + " <" + thisOpt.getArgName() + ">\t" + thisOpt.getDescription());
		}

		System.err.println("\nGeneral commands:");
		System.err.println("\thelp                       Provides this help information.");
		
		System.err.println("\nScanning commands:");
		System.err.println("\tscan-tree <directories>    Scan one or more file system directory and record file names.");
		
		System.err.println("\nReporting commands:");
		System.err.println("\treport-all-files           List all files recorded in the build store.");
		System.err.println("\treport-unused-files        Report on files that are never used by the build system.");
		System.err.println("\treport-most-used           Report on files the build system accessed the most.");

		System.err.println("\nError: " + message);
		System.exit(1);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given the user supplied command name, and command arguments, invoke the necessary command.
	 * We must first validate the command name and arguments, possibily exiting with an error
	 * message.
	 * @param buildStore The BuildStore to operate on.
	 * @param string The command's string name (as typed by the end user).
	 * @param cmdArgs The arguments (if any) for this command.
	 * 
	 */
	private static void invokeCommandOnBuildStore(BuildStore buildStore,
			String cmdName, String[] cmdArgs) {
		
		/*
		 * Scanning commands
		 */
		if (cmdName.equals("scan-tree")){
			validateArgs(cmdArgs, 1, ARGS_INFINITE, "scan-tree requires one or more directory names to be specified.");
			DiscoScans.scanBuildTree(buildStore, cmdArgs);
		}
		
		/*
		 * Reporting commands
		 */
		else if (cmdName.equals("report-all-files")){
			validateArgs(cmdArgs, 0, 0, "report-all-files doesn't require any arguments");
			DiscoReports.reportAllFiles(buildStore);
		}
		
		/*
		 * Else, unrecognized command.
		 */
		else {
			reportErrorAndExit("Unrecognized command: " + cmdName);
		}
		
	}
	
	/*-------------------------------------------------------------------------------------*/
		
	/**
	 * Validate function to ensure that the number of arguments provided to a command
	 * is in range between minArgs and maxArgs.
	 * @param cmdArgs The actual array of arguments.
	 * @param minArgs The minimum number of arguments required (0 or higher)
	 * @param maxArgs The maximum number of arguments required (0 or higher - possibly ARGS_INFINITE)
	 * @param message An error message to provide if an invalid number of arguments is included.
	 */
	private static void validateArgs(String[] cmdArgs, int minArgs, int maxArgs,
			String message) {
		
		String cmdName = cmdArgs[0];
		int actualArgs = cmdArgs.length - 1;

		/* too few arguments? */
		if (actualArgs < minArgs) {
			reportErrorAndExit("Error: too few arguments to " + cmdName + "\n" + message);
		}
		
		/* too many arguments? */
		else if ((maxArgs != ARGS_INFINITE) && (actualArgs > maxArgs)){
			reportErrorAndExit("Error: too many arguments to " + cmdName + "\n" + message);
		}
		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * This is the main entry point for the disco command.
	 * @param args Standard Java command line arguments - passed to us by the 
	 * "disco" shell script.
	 */
	public static void main(String[] args) {
	
		/* 
		 * Process command line options. Once we return from this method, all options 
		 * have been parsed and validated. Argument values will have been placed
		 * into this class's fields. The return value is the list of non-option arguments
		 * (the command name is in cmdArgs[0]).
		 */
		String cmdArgs[] = processCommandLineOptions(args);
		
		try {
			/*
			 * Open the build store file, or create a new file.
			 */
			BuildStore buildStore = new BuildStore(buildStoreFileName);
		
			/*
			 * Now, invoke a command.
			 */
			invokeCommandOnBuildStore(buildStore, cmdArgs[0], cmdArgs);
			
		} catch (Exception e) {
			System.err.println("Error: Unexpected software problem. Please report the following error:\n" + e);
			System.exit(1);
		}
	}

	/*-------------------------------------------------------------------------------------*/
}

