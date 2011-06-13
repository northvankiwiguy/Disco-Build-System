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

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.cli.*;

import com.arapiki.disco.model.BuildStore;
import com.arapiki.utils.print.PrintUtils;

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
	
	/* should we show roots when displaying reports? */
	private static boolean optionShowRoots = false;
	
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

		/* add the -r / --show-roots option */
		Option rOpt = new Option("r", "show-roots", false, "Show file name space roots when displaying report output");
		opts.addOption(rOpt);

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
		optionShowRoots = line.hasOption('r');
		
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
	 * Display a formatted line in the help output. Used for lining up the columns in the help text.
	 * @param leftCol The text in the left column
	 * @param rightCol The text in the right column
	 */
	private static void formattedDisplayLine(String leftCol, String rightCol) {
		System.err.print(leftCol);
		PrintUtils.indent(System.err, 40 - leftCol.length());
		System.err.println(rightCol);
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
			String line = "    -" + thisOpt.getOpt() + " | --" + thisOpt.getLongOpt();
			if (thisOpt.hasArg()) {
				line += " <" + thisOpt.getArgName() + ">";
			}
			formattedDisplayLine(line, thisOpt.getDescription());
		}

		System.err.println("\nGeneral commands:");
		formattedDisplayLine("    help", "Provides this help information");
		
		System.err.println("\nScanning commands:");
		formattedDisplayLine("    scan-tree <directories>", "Scan one or more file system directory and record file names");
		formattedDisplayLine("    scan-ea-anno <anno-file>", "Scan an Electric Accelerator annotation file");
		
		System.err.println("\nFile reporting commands:");
		formattedDisplayLine("    show-files", "List all files recorded in the build store.");
		formattedDisplayLine("    show-unused-files", "Report on files that are never used by the build system.");
		formattedDisplayLine("    show-most-used-files", "Report on files the build system accessed the most.");
		
		System.err.println("\nTask reporting commands:");
		formattedDisplayLine("    show-tasks", "List all tasks recorded in the build store.");
		
		System.err.println("\nFile System commands:");
		formattedDisplayLine("    show-root [<root-name>]", "Show the file system path referred to by this root. Without");
		formattedDisplayLine("", "arguments, list all available roots.");
		formattedDisplayLine("    set-root <root> <path>", "Set the <root> to refer to the <path>");
		formattedDisplayLine("    rm-root <root>", "Remove the <root> so it no long references a path");

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
		else if (cmdName.equals("scan-ea-anno")){
			validateArgs(cmdArgs, 1, 1, "scan-ea-anno requires one Electric Accelerator annotation file to be specified.");
			DiscoScans.scanElectricAnno(buildStore, cmdArgs);
		}
		
		/*
		 * File reporting commands
		 */
		else if (cmdName.equals("show-files")){
			validateArgs(cmdArgs, 0, ARGS_INFINITE, "show-files [ <top-path> ]");
			DiscoReports.showFiles(buildStore, optionShowRoots, cmdArgs);
		}
		else if (cmdName.equals("show-unused-files")) {
			validateArgs(cmdArgs, 0, ARGS_INFINITE, "show-unused-files [ <top-path> ]");
			DiscoReports.showUnusedFiles(buildStore, optionShowRoots, cmdArgs);			
		}
		
		/*
		 * Task reporting commands
		 */
		else if (cmdName.equals("show-tasks")) {
			validateArgs(cmdArgs, 0, ARGS_INFINITE, "show-tasks");
			DiscoReports.showTasks(buildStore, cmdArgs);			
		}
		
		/*
		 * Commands for showing/manipulating attributes of the BuildStore
		 */
		else if (cmdName.equals("show-root")){
			validateArgs(cmdArgs, 0, 1, "show-root [ <root> ]");
			DiscoAttributes.showRoot(buildStore, cmdArgs);
		}
		
		else if (cmdName.equals("set-root")){
			validateArgs(cmdArgs, 2, 2, "set-root <root> <path>");
			DiscoAttributes.setRoot(buildStore, cmdArgs);
		}

		else if (cmdName.equals("rm-root")){
			validateArgs(cmdArgs, 1, 1, "rm-root <root>");
			DiscoAttributes.rmRoot(buildStore, cmdArgs);
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
			BuildStore buildStore = null;
			try {
				 buildStore = new BuildStore(buildStoreFileName);
			} catch (FileNotFoundException ex) {
				System.err.println(ex.getMessage());
				System.exit(1);
			}
			/*
			 * Now, invoke a command.
			 */
			invokeCommandOnBuildStore(buildStore, cmdArgs[0], cmdArgs);
			
		} catch (Exception e) {
			System.err.println("Error: Unexpected software problem. Please report the following error:\n");
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	/*-------------------------------------------------------------------------------------*/
}

