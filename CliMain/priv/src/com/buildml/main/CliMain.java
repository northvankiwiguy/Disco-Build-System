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

package com.buildml.main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.cli.*;

import com.buildml.main.commands.*;
import com.buildml.model.BuildStore;
import com.buildml.model.errors.BuildStoreVersionException;
import com.buildml.utils.print.PrintUtils;
import com.buildml.utils.string.StringArray;
import com.buildml.utils.version.Version;

/**
 * This is the main entry point for the "bml" command line program. All other parts
 * of the code (with the exception of the Eclipse plug-in) are invoked via this point.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public final class CliMain {	
	
	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/
	
	/** The file name to use for the BuildStore database (defaults to "buildstore"). */
	private String buildStoreFileName = "buildstore";	
	
	/** Set if the user selected the -h option. */
	private boolean optionHelp = false;
	
	/** Set if the user selected the -v option. */
	private boolean optionVersion = false;
	
	/** The global command line options, as managed by the Apache Commons CLI library. */
	private Options globalOpts = null;
	
	/**
	 * All CLI command are "plugged into" the CliMain class, from where they can then
	 * be invoked. The CommandGroup class is used to encapsulate logical groups of commands.
	 * For example, one group could be all the commands that display FileSet listings.
	 */
	private class CommandGroup {
		
		/** The title of the command group (e.g. "Commands for displaying task information") */
		String groupHeading;
		
		/** The commands that fall within this group */
		ICliCommand commands[];
	}
	
	/** The list of command groups that are registered with CliMain. */
	private ArrayList<CommandGroup> commandGroups = null;
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * Create a new CliMain instance. This should only be done once, from the standard
	 * Java main() function.
	 */
	private CliMain() {
		/* empty */
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Process the global command line arguments, using the Apache Commons CLI library.
	 * Global options are defined as being those arguments that appear before the sub-command
	 * name. They are distinct from "command options" that are specific to each sub-command.
	 * 
	 * @param args The standard command line array from the main() method.
	 * @return The remaining command line arguments (with global options excluded), with the first
	 * being the command name.
	 */
	private String [] processGlobalOptions(String[] args) {
		
		/* create a new Apache Commons CLI parser, using Posix style arguments */
		CommandLineParser parser = new PosixParser();
		
		/* define the bml command's arguments */
		globalOpts = new Options();

		/* add the -f / --buildstore-file option */
		Option fOpt = new Option("f", "buildstore-file", true, "Name of buildstore database to query/edit");
		fOpt.setArgName("file-name");
		globalOpts.addOption(fOpt);
		
		/* add the -h / --help option */
		Option hOpt = new Option("h", "help", false, "Show this help information");
		globalOpts.addOption(hOpt);
		
		/* add the -v / --version option */
		Option vOpt = new Option("v", "version", false, "Show version information");
		globalOpts.addOption(vOpt);
		
		/* how many columns of output should we show (default is 80) */
		Option widthOpt = new Option("w", "width", true, "Number of output columns in reports (default is " +
				CliUtils.getColumnWidth() + ")");
		globalOpts.addOption(widthOpt);

		/*
		 * Initiate the parsing process - also, report on any options that require
		 * an argument but didn't receive one. We only want to parse arguments
		 * up until the first non-argument (that doesn't start with -).
		 */
		CommandLine line = null;
	    try {
	    	line = parser.parse(globalOpts, args, true);

		} catch (ParseException e) {
			displayHelpAndExit(e.getMessage());
		}
		
		/*
		 * Validate all the options and their argument values.
		 */
		if (line.hasOption('f')){
			buildStoreFileName = line.getOptionValue('f');
		}
		
		if (line.hasOption('h')) {
			optionHelp = true;
		}
		
		if (line.hasOption('v')) {
			optionVersion = true;
		}
		
		String argWidth = line.getOptionValue("width");
		if (argWidth != null) {
			try {
				int newWidth = Integer.valueOf(argWidth);
				CliUtils.setColumnWidth(newWidth);
			} catch (NumberFormatException ex) {
				CliUtils.reportErrorAndExit("Invalid argument to --width: " + argWidth);
			}
		}
		
		/* 
		 * Return the array of arguments that come after the global option. This
		 * includes the sub-command name, any sub-command options, and the sub-command's
		 * arguments.
		 */
		return line.getArgs();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Display a set of options (as defined by the Options class). This methods is used
	 * in displaying the help text
	 * @param opts A set of command line options, as defined by the Options class.
	 */
	@SuppressWarnings("unchecked")
	private void displayOptions(Options opts) {
		
		/* obtain a list of all the options */
		Collection<Option> optList = opts.getOptions();
		
		/* if there are no options for this command ... */
		if (optList.size() == 0){
			System.err.println("    No options available.");	
		} 
		
		/* 
		 * Else, we have options to display. Show them in a nicely tabulated
		 * format, with the short option name (e.g. -p) and the long option name
		 * (--show-pkgs) first, followed by a text description of the option.
		 */
		else {
			for (Iterator<Option> iterator = optList.iterator(); iterator.hasNext();) {
				Option thisOpt = iterator.next();
				String shortOpt = thisOpt.getOpt();
				String longOpt = thisOpt.getLongOpt();
				String line = "    ";
				if (shortOpt != null) {
					line += "-" + shortOpt;
				} else {
					line += "  ";
				}
				if (shortOpt != null && longOpt != null) {
					line += " | ";
				} else {
					line += "   ";
				}
				if (longOpt != null) {
					line += "--" + thisOpt.getLongOpt();
				}
				if (thisOpt.hasArg()) {
					line += " <" + thisOpt.getArgName() + ">";
				}
				formattedDisplayLine(line, thisOpt.getDescription());
			}
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Display a formatted line in the help output. This is a helper method used for lining 
	 * up the columns in the help text.
	 * @param leftCol The text in the left column
	 * @param rightCol The text in the right column
	 */
	private void formattedDisplayLine(String leftCol, String rightCol) {
		System.err.print(leftCol);
		PrintUtils.indent(System.err, 40 - leftCol.length());
		System.err.println(rightCol);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Display the main help text for the "bml" command. The includes the global command line
	 * options and the list of sub-commands. Note: this method never returns, instead the whole program
	 * is aborted. Optionally, a text message will be displayed.
	 * @param message A special purpose string error message.
	 */
	private void displayHelpAndExit(String message) {
		System.err.println("\nUsage: bml [ global-options ] command [ command-options ] arg, ...");
		System.err.println("\nOptions for all commands:");
		
		/* display the global command options */
		displayOptions(globalOpts);

		/* display a summary of all the sub-commands (in their respective groups) */
		for (CommandGroup group : commandGroups) {
			System.err.println("\n" + group.groupHeading + ":");
			for (int i = 0; i < group.commands.length; i++) {
				ICliCommand cmd = group.commands[i];
				formattedDisplayLine("    " + cmd.getName(), cmd.getShortDescription());
			}
		}
		
		System.err.println("\nFor more help, use bml -h <command-name>\n");
		
		/* if the caller supplied a message, display it */
		CliUtils.reportErrorAndExit(message);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Display detailed help text about a specific CLI command. The output from this
	 * command may be hundreds of lines long, depending on the length of the command-specific
	 * help message. Note that this method aborts the whole program, and never returns.
	 * 
	 * @param cmd The CLI command to display detailed information about.
	 */
	private void displayDetailedHelpAndExit(ICliCommand cmd) {
		
		System.err.println("\nSynopsis:\n\n    " + cmd.getName() + " - " + cmd.getShortDescription());
		System.err.println("\nSyntax:\n\n    " + cmd.getName() + " " + cmd.getParameterDescription());
	
		System.err.println("\nOptions:\n");
		displayOptions(cmd.getOptions());

		System.err.println("\nDescription:\n");
		String longHelp = cmd.getLongDescription();
		if (longHelp != null) {
			/* indent by 4, we'll manage the wrapping ourselves */
			PrintUtils.indentAndWrap(System.err, longHelp, 4, 1000);
		} else {
			System.err.println("    No detailed help available for this command.");
		}
		System.err.println();

		/* exit, with no particular error message */
		CliUtils.reportErrorAndExit(null);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Register a group of CLI commands. This is a helper method for registerCommands().
	 * @param heading The title to be printed at the start of this group of commands.
	 * @param commandList An array of commands to be added into this group.
	 */
	private void registerCommandGroup(String heading,
			ICliCommand[] commandList) {

		/* 
		 * A command group is essentially a structure with a title/heading an array of
		 * ICliCommand entries.
		 */
		CommandGroup newGrp = new CommandGroup();
		newGrp.groupHeading = heading;
		newGrp.commands = commandList;
		
		commandGroups.add(newGrp);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Register all the CliCommand* classes so that their commands can be executed. We want
	 * our help page to be meaningful, so we add the commands in groups. Any new CLI commands
	 * should be added here (and only here).
	 */
	private void registerCommands() {

		/* we'll record all the command groups in a list */
		commandGroups = new ArrayList<CommandGroup>();
		
		registerCommandGroup("Commands for scanning builds and build trees",
			new ICliCommand[] {
				new CliCommandScanTree(),
				new CliCommandScanEaAnno(),
				new CliCommandScanBuild()
			});
		
		registerCommandGroup("Commands for displaying file/path information",
			new ICliCommand[] {
					new CliCommandShowFiles(),
					new CliCommandShowUnusedFiles(),
					new CliCommandShowWriteOnlyFiles(),
					new CliCommandShowPopularFiles(),
					new CliCommandShowDerivedFiles(),
					new CliCommandShowInputFiles(),
					new CliCommandShowFilesUsedBy()
			});

		registerCommandGroup("Commands for displaying task information",
			new ICliCommand[] {
				new CliCommandShowTasks(),
				new CliCommandShowTasksThatUse()
			});
		
		registerCommandGroup("Commands for managing file system roots",
			new ICliCommand[] {
				new CliCommandShowRoot(),
				new CliCommandAddRoot(),
				new CliCommandRemoveRoot()
			});

		registerCommandGroup("Commands for managing packages",
			new ICliCommand[] {
				new CliCommandShowPkg(),
				new CliCommandAddPkg(),
				new CliCommandRemovePkg(),
				new CliCommandSetFilePkg(),
				new CliCommandSetTaskPkg()
			});
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a CLI command name, find the associated ICliCommand object that describes the
	 * command.
	 * @param cmdName The name of the CLI command, as entered by the user on the command line.
	 * @return The associated ICliCommand object, or null if the command wasn't registered.
	 */
	private ICliCommand findCommand(String cmdName) {

		/* 
		 * Given the small number of commands, we can do a linear search
		 * through the command groups and commands.
		 */
		for (CommandGroup group : commandGroups) {
			for (ICliCommand cmd : group.commands) {
				if (cmdName.equals(cmd.getName())) {
					return cmd;
				}
			}
		}
		
		/* command not found, return null */
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
		
	/**
	 * This is the main entry point for the bml command. This method parses the global
	 * command line arguments, determines which sub-command is being invoked, parses that
	 * command's options, then invokes the command. 
	 * 
	 * @param args Standard Java command line arguments - passed to us by the 
	 * "bml" shell script.
	 */
	private void invokeCommand(String[] args) {
		
		/* register all the sub-command classes */
		registerCommands();
		
		/* Process global command line options */
		String cmdArgs[] = processGlobalOptions(args);
		
		/* 
		 * If the user types "bml -h" with no other arguments, show the general help page.
		 * Also, if the user doesn't provide a sub-command name, show the same help page,
		 * but also with an error message.
		 */
		if (cmdArgs.length == 0) {
			if (optionVersion) {
				System.out.println(Version.getVersion());
				CliUtils.reportErrorAndExit(null);
			}
			if (optionHelp) {
				displayHelpAndExit(null);
			} else {
				displayHelpAndExit("Missing command - please specify an operation to perform.");
			}
		}
		
		/* what's the command's name? If it begins with '-', this means we have unparsed options! */
		String cmdName = cmdArgs[0];
		if (cmdName.startsWith("-")) {
			CliUtils.reportErrorAndExit("Unrecognized global option " + cmdName);
		}
		cmdArgs = StringArray.shiftLeft(cmdArgs);
		
		/* find the appropriate command object (if it exists) */
		ICliCommand cmd = findCommand(cmdName);
		if (cmd == null) {
			CliUtils.reportErrorAndExit("Unrecognized command - \"" + cmdName + "\"");
		}
		
		/* Does the user want help with this command? */
		if (optionHelp) {
			displayDetailedHelpAndExit(cmd);
		}
		
		/*
		 * Open the build store file, or create a new file.
		 */
		BuildStore buildStore = null;
		try {
			buildStore = new BuildStore(buildStoreFileName);
		} catch (FileNotFoundException ex) {
			CliUtils.reportErrorAndExit(ex.getMessage());
		} catch (IOException ex) {
			CliUtils.reportErrorAndExit(ex.getMessage());
		} catch (BuildStoreVersionException ex) {
			CliUtils.reportErrorAndExit(ex.getMessage());				
		}

		/*
		 * Fetch the command's command line options (Options object) which
		 * is then used to parse the user-provided arguments.
		 */
		CommandLineParser parser = new PosixParser();
		CommandLine cmdLine = null;
		Options cmdOpts = cmd.getOptions();
		try {
			cmdLine = parser.parse(cmdOpts, cmdArgs, true);
		} catch (ParseException e) {
			CliUtils.reportErrorAndExit(e.getMessage());
		}
		cmd.processOptions(buildStore, cmdLine);

		/*
		 * Check for unprocessed command options. That is, if the first
		 * non-option argument starts with '-', then it's actually an
		 * unprocessed option.
		 */
		String remainingArgs[] = cmdLine.getArgs();
		if (remainingArgs.length > 0) {
			String firstArg = remainingArgs[0];
			if (firstArg.startsWith("-")) {
				CliUtils.reportErrorAndExit("Unrecognized option " + firstArg);
			}
		}

		/*
		 * Now, invoke the command. If the invoke() method wants to, it may completely
		 * exit from the program. This is the typical case when an error is reported
		 * via the CliUtils.reportErrorAndExit() method.
		 */
		cmd.invoke(buildStore, remainingArgs);

		/* release resources */
		buildStore.close();

	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * The standard Java main() function, which does its work by delegating to invokeCommand().
	 * We also use this opportunity to catch and report any stray Exceptions/Errors.
	 * 
	 * @param args The standard command line argument array.
	 */
	public static void main(String[] args) {
		
		/*
		 * We wrap everything in a global "try", to catch any uncaught Exception
		 * exceptions that might be thrown. This is a catch all for all errors and
		 * exceptions that don't get caught anywhere else. We'll do our best to 
		 * display a meaningful error message.
		 */
		try {
			new CliMain().invokeCommand(args);
			
		} catch (Throwable e) {
			System.err.println("\n============================================================\n");
			System.err.println("Error: Unexpected software problem, which was probably an internal");
			System.err.println("programming error, rather than something you did wrong. Please cut");
			System.err.println("and paste the following error and email it to bugs@buildml.com,");
			System.err.println("along with a description of what you were doing at the time.");
			System.err.println("\n============================================================\n");
			System.err.println(Version.getVersion());
			System.err.println("Java version is " + System.getProperty("java.vendor" ) + " " +
					System.getProperty("java.version"));
			System.err.println("Operating system version is " + System.getProperty("os.name") +
					" " + System.getProperty("os.version"));			
			e.printStackTrace(System.err);
			System.err.println("\n============================================================");
			System.exit(1);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/	
}

