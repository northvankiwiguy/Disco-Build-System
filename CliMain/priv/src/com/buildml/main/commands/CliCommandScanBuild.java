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

package com.buildml.main.commands;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.buildml.main.CliUtils;
import com.buildml.main.ICliCommand;
import com.buildml.model.BuildStore;
import com.buildml.scanner.legacy.LegacyBuildScanner;

/**
 * BuildML CLI Command class that implements the "scan-build" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CliCommandScanBuild implements ICliCommand {

	/** If not-null, the name of the trace file to write/read. */
	private String traceFileName = null;
	
	/** Set if the user specified --trace-only. */
	private boolean optionTraceOnly = false;
	
	/** Set if the user specified --read-trace. */
	private boolean optionReadTrace = false;
	
	/** Set if the user specified --trace-level=. */
	private int optionDebugLevel = 0;
	
	/** The name of the log file to send debug information to */
	private String logFileName = null;

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getLongDescription()
	 */
	@Override
	public String getLongDescription() {
		return CliUtils.genLocalizedMessage("#include commands/scan-build.txt");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getName()
	 */
	@Override
	public String getName() {
		return "scan-build";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getOptions()
	 */
	@Override
	public Options getOptions() {
		Options opts = new Options();

		/* add the --trace-file option */
		Option traceFileOpt = new Option("f", "trace-file", true, "Specify the name of the trace file to write/read.");
		traceFileOpt.setArgName("file-name");
		opts.addOption(traceFileOpt);
		
		/* add the --trace-only option */
		Option traceOnlyOpt = new Option("t", "trace-only", false, "Trace the shell command, but don't create a database.");
		opts.addOption(traceOnlyOpt);

		/* add the --read-trace option */
		Option readTraceOpt = new Option("r", "read-trace", false, "Read an existing trace file, creating a new database.");
		opts.addOption(readTraceOpt);

		/* add the --debug-level option */
		Option dumpTraceOpt = new Option("d", "debug-level", true, 
				"Debug level (0 = none, 1 = brief, 2 = detailed).");
		opts.addOption(dumpTraceOpt);
		
		/* add the --log-file option */
		Option logFileOpt = new Option("l", "log-file", true,  "File for capturing debug information (default: cfs.log).");
		logFileOpt.setArgName("file-name");
		opts.addOption(logFileOpt);
		
		return opts;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getParameterDescription()
	 */
	@Override
	public String getParameterDescription() {
		return "<build-command> <args> ...";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return "Scan a legacy shell-based build command.";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#processOptions(org.apache.commons.cli.CommandLine)
	 */
	@Override
	public void processOptions(BuildStore buildStore, CommandLine cmdLine) {
		
		/* fetch the option values */
		optionTraceOnly = cmdLine.hasOption("trace-only");
		optionReadTrace = cmdLine.hasOption("read-trace");
		traceFileName = cmdLine.getOptionValue("trace-file");
		logFileName = cmdLine.getOptionValue("log-file");
		
		/*
		 * We can't specify both --trace-only and --read-trace
		 */
		if (optionTraceOnly && optionReadTrace) {
			CliUtils.reportErrorAndExit("Options --trace-only and --read-trace can't be used together.");
		}
		
		if (cmdLine.hasOption("debug-level")) {
			String level = cmdLine.getOptionValue("debug-level");
			if (level.equals("0")) {
				optionDebugLevel = 0;		/* disable debugging - the default */
			} else if (level.equals("1")) {
				optionDebugLevel = 1;		/* basic debugging output */
			} else if (level.equals("2")) {
				optionDebugLevel = 2;		/* extended debugging output */
			} else {
				CliUtils.reportErrorAndExit("Invalid argument to --debug-level: " + level + ".");
			}
		}

	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.main.ICliCommand#invoke(com.buildml.model.BuildStore, java.lang.String[])
	 */
	@Override
	public void invoke(BuildStore buildStore, String[] args) {
		
		CliUtils.validateArgs(getName(), args, 1, CliUtils.ARGS_INFINITE, "A shell command and arguments must be specified.");

		/*
		 * Create a LegacyBuildScanner object, which can execute the shell command, generate a trace
		 * file, and create a BuildStore.
		 */
		LegacyBuildScanner lbs = new LegacyBuildScanner();
		
		/* set the trace file (if this is null, the default will be used */
		lbs.setTraceFile(traceFileName);
		
		/* set the log file (if this is null, the default will be used */
		lbs.setLogFile(logFileName);
		
		/* 
		 * Possibly dump the content of the trace file to the stdout. This depends on whether
		 * --debug-level was specified.
		 */
		if (optionDebugLevel != 0) {
			lbs.setDebugLevel(optionDebugLevel);
		}
		
		/*
		 * Should we populate a BuildStore, based on the content of the trace file? Do so, unless
		 * --trace-only was specified.
		 */
		if (!optionTraceOnly) {
			lbs.setBuildStore(buildStore);
		}
		
		/*
		 * Invoke the shell commands via "cfs", and collect the trace output. However, skip this
		 * step if the user selected --read-trace (which uses an existing trace file)
		 */
		if (!optionReadTrace) {
			try {
				/* invoke the shell commands, and construct the trace file */
				lbs.traceShellCommand(args, null, System.out);

			} catch (Exception e) {
				CliUtils.reportErrorAndExit("Unable to invoke shell command: " + e.getMessage());
			}
		}
		
		/* 
		 * Now create the BuildStore and/or display debug trace information (depending on 
		 * what the user has chosen to do). If they want to dump the trace file, or if they
		 * didn't select --trace-only, parse the trace file.
		 */
		if ((optionDebugLevel != 0) || !optionTraceOnly) {
			lbs.parseTraceFile();
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
}
