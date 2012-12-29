/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import com.buildml.config.PerTreeConfigFile;
import com.buildml.model.BuildStoreFactory;
import com.buildml.model.BuildStoreVersionException;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.utils.errors.ErrorCode;
import com.buildml.utils.print.PrintUtils;
import com.buildml.utils.string.StringArray;

/**
 * Main entry point for the "bml" command, used to invoke a build operation.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class BMLMain {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** name of the build.bml file to search for */
	private static final String DATABASE_FILE_NAME = "build.bml";
	
	/** name of the per-tree configuration file to search for (or create) */
	private static final String CONFIG_FILE_NAME = ".bmlconf";

	/** The BuildStore used when performing the build */
	private IBuildStore buildStore = null;
	
	/** The per-tree configuration file - contains aliases and root paths, etc. */
	private PerTreeConfigFile configFile = null;
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Starting point for execution.
	 * @param args The standard command line array.
	 */
	public static void main(String[] args) {
		new BMLMain().invokeCommand(args);
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * Display an error message, then completely exit from the program.
	 * @param message The error message to be displayed (possibly null).
	 */
	private void showUsageAndExit(String message) {
		if (message != null) {
			System.err.println(message);
		}
		System.err.println();
		System.err.println("Usage: bml <alias> | { <pkg-name> ... } - Build the specified packages.");
		System.err.println("       bml -h                           - Show this help page.");
		System.err.println("       bml -l                           - List available packages and aliases.");
		System.err.println("       bml -r                           - Show file system root path mappings.");
		System.err.println("       bml -s <root> <path>             - Set a root path mapping.");
		System.err.println("       bml -d <root>                    - Delete a root path mapping.");
		System.err.println("       bml -a <alias> <pkg-name> ...    - Define a package alias.");
		System.err.println("       bml -u <alias>                   - Undefine a package alias.");
		System.err.println("\nIn addition, the following global options can be used in combination with");
		System.err.println("the options shown above:\n");
		System.err.println("       -f <bml-file>                    - Specify path to .bml file");
		System.err.println();
		System.exit(-1);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Display an error message to the stderr, and exit the program.
	 * 
	 * @param message The fatal error message to display.
	 */
	private void fatal(String message) {
		System.err.println(message);
		System.exit(-1);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Invoke the "bml" with the specified arguments.
	 * 
	 * @param args The command line argument string.
	 */
	private void invokeCommand(String[] args) {

		int argPos = 0;

		/* open the BuildStore and the per-tree configuration */
		argPos = openDatabases(args, argPos);
		
		/* now parse the remaining arguments */
		if (args.length > argPos) {
			String option = args[argPos];
			
			/* if the option starts with '-', it's likely to be a flag */
			if (option.startsWith("-")) {
				if (option.length() == 2) {
					argPos++;

					switch (option.charAt(1)) {

					case 'h':
						showUsageAndExit(null);
						break;

					case 'l':
						listPackages(args, argPos);
						break;

					case 'a':
						doAddAlias(args, argPos);
						break;

					case 'u':
						doUnAlias(args, argPos);
						break;

					default:
						showUsageAndExit("Invalid option: " + option);
						break;
					}
				} else {
					showUsageAndExit("Invalid option: " + option);
				}
			}
			
			/* else, invoke a build with the user-supplied package names */
			else {
				doBuild(args, argPos);
			}
		}
		
		/* else perform "default" build */
		else {
			doBuild(new String[] { "default" }, 0);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Open the BuildStore database, and the per-tree configuration file. This method
	 * reports errors (and terminates the program) if there are any errors.
	 * 
	 * @param args		The command line arguments.
	 * @param firstArg  The index of the first argument to be used.
	 * @return The index into args of the next arguments to be processed.
	 */
	private int openDatabases(String[] args, int firstArg) {

		File pathToDatabase = null;
		File pathToConfiguration = null;

		/* if the -f option is given that's the location of the build.bml file */
		if ((args.length > 0) && args[firstArg].equals("-f")) {
			if (args.length == 1) {
				showUsageAndExit("The -f option requires a path name to the build.bml file");
			} else {
				pathToDatabase = new File(args[firstArg + 1]).getAbsoluteFile();
				if (!pathToDatabase.exists()) {
					fatal("File \"" + args[firstArg + 1] + "\" does not exist.");
				}
				firstArg += 2;
			}
		} 

		/* else, search for the build.bml file in this directory or its ancestors */
		else {
			pathToDatabase = searchForBMLFile();
			if (pathToDatabase == null) {
				fatal("No build.bml file could be found in this directory, or a parent directory.");
			}
		}
		
		/* attempt to open the build.bml file */
		try {
			buildStore = BuildStoreFactory.openBuildStoreReadOnly(pathToDatabase.toString());
		} catch (FileNotFoundException e) {
			fatal("File \"" + pathToDatabase + "\" can't be opened as a BuildML database.");
		} catch (IOException e) {
			fatal("File \"" + pathToDatabase + "\" has I/O problems.");
		} catch (BuildStoreVersionException e) {
			fatal(e.getMessage());
		}
		
		/* now we have an open BuildStore, let's open the configuration file */
		File configDir = pathToDatabase.getParentFile();
		if (configDir == null) {
			fatal("Unable to determine directory to contain " + CONFIG_FILE_NAME);
		}
		pathToConfiguration = new File(configDir, CONFIG_FILE_NAME);
		try {
			configFile = new PerTreeConfigFile(buildStore, pathToConfiguration);
		} catch (IOException e) {
			fatal("Unable to open configuration file: " + pathToConfiguration + 
					". " + e.getMessage());
		}
		
		/* all is OK... return the position of the next command line argument */
		return firstArg;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The file system path of the nearest enclosing build.bml file, or null if
	 * none could be found.
	 */
	private File searchForBMLFile() {
		
		/* loop upwards from CWD to /, until we find a directory containing build.bml */
		File thisDir = new File(".").getAbsoluteFile();
		do {
			File bmlFile = new File(thisDir, DATABASE_FILE_NAME);
			if (bmlFile.exists()) {
				return bmlFile;
			}
			thisDir = thisDir.getParentFile();
		} while (thisDir != null);
		
		/* reach top of file system without finding the file */
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Perform the -l (list packages) command.
	 * 
	 * @param args		The command line arguments.
	 * @param firstArg  The index of the first argument to be used.
	 */
	private void listPackages(String args[], int firstArg) {
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		String packages[] = pkgMgr.getPackages();
		
		if (args.length != firstArg) {
			showUsageAndExit("Excessive arguments after -l option.");
		}
		
		System.out.println("\nValid package names:\n");
		
		/* don't print the <import> package, since we can't build that */
		int importPkgId = pkgMgr.getImportPackage();
		String importPkgName = pkgMgr.getName(importPkgId);
		
		/* print all the remaining package names */
		for (int i = 0; i < packages.length; i++) {
			String thisName = packages[i];
			if (!thisName.equals(importPkgName)) {
				System.out.println("  " + thisName);
			}
		}

		String aliases[] = configFile.getAliases();
		if (aliases.length == 0) {
			System.out.println("\nNo aliases defined.");
		}
		
		else {			
			System.out.println("\nAliases defined:\n");

			int maxLength = StringArray.maxStringLength(aliases) + 3;
			for (int i = 0; i < aliases.length; i++) {
				System.out.print("  " + aliases[i]);
				PrintUtils.indent(System.out, maxLength - aliases[i].length());
				String pkgNames[] = configFile.getAlias(aliases[i]);
				for (int j = 0; j < pkgNames.length; j++) {
					System.out.print(pkgNames[j] + " ");
				}
				System.out.println();
			}
			
		}
		
		System.out.println("\nTo build a package, use: \"bml <pkg-name>\" or \"bml <alias>\"\n");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Invoke a build operation by building the packages that are specified on the command line.
	 * Any alias names that are mentioned will first be expanded to the corresponding package
	 * names.
	 * 
	 * @param args		The command line arguments.
	 * @param firstArg  The index of the first argument to be used.
	 */
	private void doBuild(String[] args, int firstArg) {
		
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		
		/* 
		 * For each argument, determine if it's an alias or a package name.
		 * Aliases are first expanded to their definitions.
		 */
		SortedSet<String> pkgSet = new TreeSet<String>();
		for (int i = firstArg; i < args.length; i++) {
			
			/* expand aliases - expPkgs is a list of one or more package names */
			String[] expPkgs = configFile.getAlias(args[i]);
			if (expPkgs == null) {
				expPkgs = new String[] { args[i] };
			}
			
			/* validate package names */
			for (int j = 0; j < expPkgs.length; j++) {
				int pkgId = pkgMgr.getId(expPkgs[j]);
				if ((pkgId == ErrorCode.NOT_FOUND) || pkgMgr.isFolder(pkgId)) {
					fatal("Invalid package or alias name: \"" + expPkgs[j] + 
							"\". Use \"bml -l\" to see valid choices.");
				}
			}
			
			/* 
			 * Add packages names to our set of packages to build. Using a set
			 * will eliminate duplicates.
			 */
			for (int j = 0; j < expPkgs.length; j++) {
				pkgSet.add(expPkgs[j]);
			}
		}
		
		/*
		 * Invoke the build (for now, display the packages).
		 */
		for (Iterator<String> iterator = pkgSet.iterator(); iterator.hasNext();) {
			String pkgName = (String) iterator.next();
			System.out.println("Building: " + pkgName);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Perform the -a (alias) command.
	 * 
	 * @param args		The command line arguments.
	 * @param firstArg  The index of the first argument to be used.
	 */
	private void doAddAlias(String[] args, int firstArg) {

		if (args.length < firstArg + 2) {
			showUsageAndExit("Insufficient arguments to -a option.");
		}
		String aliasName = args[firstArg];
		String packages[] = Arrays.copyOfRange(args, firstArg + 1, args.length);
		
		/* perform the add operation, handling errors as necessary */
		int rc = configFile.addAlias(aliasName, packages);
		if (rc == ErrorCode.INVALID_NAME) {
			fatal("Invalid alias name: " + aliasName);
		}
		if (rc == ErrorCode.BAD_VALUE) {
			fatal("One or more of the package names is invalid.");
		}

		/* success - save the configuration */
		try {
			configFile.save();
		} catch (IOException e) {
			fatal("Problem saving configuration: " + e.getMessage());
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Perform the -u (unalias) command.
	 * 
	 * @param args		The command line arguments.
	 * @param firstArg  The index of the first argument to be used.
	 */
	private void doUnAlias(String[] args, int firstArg) {
		
		if (args.length != firstArg + 1) {
			showUsageAndExit("Incorrect number of arguments for -u option.");
		}
		
		/* perform the remove operation */
		String aliasName = args[firstArg];
		if (configFile.removeAlias(aliasName) == ErrorCode.NOT_FOUND) {
			fatal("Alias not defined: " + aliasName);
		}

		/* success - save the configuration */
		try {
			configFile.save();
		} catch (IOException e) {
			fatal("Problem saving configuration: " + e.getMessage());
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
}
