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
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageRootMgr;
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

		if ((args.length > 0) && (args[0].equals("-h"))) {
			showUsageAndExit(null);			
		}
		
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

					case 'l':
						listPackages(args, argPos);
						break;

					case 'a':
						doAddAlias(args, argPos);
						break;

					case 'u':
						doUnAlias(args, argPos);
						break;
						
					case 'r':
						listRoots(args, argPos);
						break;

					case 's':
						setRoot(args, argPos);
						break;

					case 'd':
						removeRoot(args, argPos);
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
			buildStore = BuildStoreFactory.openBuildStore(pathToDatabase.toString());
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
		
		/* load some of the content from the configuration file into the BuildStore */
		loadMappingsIntoBuildStore(configFile, buildStore);
		
		/* all is OK... return the position of the next command line argument */
		return firstArg;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Load the package root mappings that are stored in the configuration file and set
	 * them into the BuildStore. The BuildStore doesn't persist these mappings (they're
	 * per-user settings), so they need to be explicitly loaded each time we run "bml".
	 * 
	 * @param configFile	The configuration file to load the mappings from.
	 * @param buildStore	The BuildStore to load them into.
	 */
	private void loadMappingsIntoBuildStore(PerTreeConfigFile configFile,
			IBuildStore buildStore) {

		IPackageMgr pkgMgr = buildStore.getPackageMgr();

		/*
		 * For each package (except for <import>), check if the config file contains a 
		 * mapping for each of the package roots. If so, copy the mapping to the BuildStore.
		 */
		String packages[] = pkgMgr.getPackages();
		for (int i = 0; i < packages.length; i++) {
			int pkgId = pkgMgr.getId(packages[i]);
			if (pkgId != pkgMgr.getImportPackage()) {
				loadOneMapping(packages[i] + "_src", pkgId, 
								IPackageRootMgr.SOURCE_ROOT, configFile, buildStore);
				loadOneMapping(packages[i] + "_gen", pkgId, 
								IPackageRootMgr.GENERATED_ROOT, configFile, buildStore);
			}
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper function for loading a single package root mapping from the configuration
	 * file into the BuildStore.
	 * 
	 * @param rootName		Name of the root to be mapped.
	 * @param pkgId			Package ID associated with this root.
	 * @param type			SOURCE_ROOT or GENERATED_ROOT.
	 * @param configFile	Configuration file to load from.
	 * @param buildStore	BuildStore to load into.
	 */
	private void loadOneMapping(String rootName, int pkgId, int type,
			PerTreeConfigFile configFile, IBuildStore buildStore) {

		IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();

		String nativePath = configFile.getNativeRootMapping(rootName);
		if (nativePath != null) {
			/* 
			 * Assuming that the config file object has already validated the content
			 * of the configuration file, there's no need to check for errors.
			 */
			pkgRootMgr.setPackageRootNative(pkgId, type, nativePath);
		}
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
							"\". Use \"bml -l\" to see valid choices, or \"bml -h\" for more help.");
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

	/**
	 * Perform the -r (list roots) command.
	 * 
	 * @param args		The command line arguments.
	 * @param firstArg  The index of the first argument to be used.
	 */
	private void listRoots(String[] args, int firstArg) {
	
		if (args.length != firstArg) {
			showUsageAndExit("No arguments expected for -r option.");
		}

		IFileMgr fileMgr = buildStore.getFileMgr();
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();

		/* Display details about the @workspace root */
		String workspaceVFSPath;
		int workspaceRootId = pkgRootMgr.getWorkspaceRoot();
		if (workspaceRootId == ErrorCode.NOT_FOUND) {
			workspaceVFSPath = "<invalid>";
		} else {
			workspaceVFSPath = fileMgr.getPathName(workspaceRootId);
		}
		System.out.println("@workspace root:");
		System.out.println("  VFS path    : " + workspaceVFSPath);
		System.out.println("  Native path : " + pkgRootMgr.getWorkspaceRootNative());
		System.out.println();
		
		/* 
		 * For each package, displaying the _src and _gen root details. For each root,
		 * we show the name, the VFS path, and the native path.
		 */
		String[] packageNames = pkgMgr.getPackages();
		for (int i = 0; i < packageNames.length; i++) {
			int pkgId = pkgMgr.getId(packageNames[i]);
			if (pkgId != pkgMgr.getImportPackage()) {
				listRootsHelper(pkgId, packageNames[i] + "_src", IPackageRootMgr.SOURCE_ROOT);
				listRootsHelper(pkgId, packageNames[i] + "_gen", IPackageRootMgr.GENERATED_ROOT);
				System.out.println();
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper method (for listRoots()) that displays the VFS/native paths for a single root.
	 * 
	 * @param pkgId		The ID of the package the root belongs to.
	 * @param rootName	The name of the root.
	 * @param type		The type of root (SOURCE_ROOT or GENERATED_ROOT).
	 */
	private void listRootsHelper(int pkgId, String rootName, int type) {
		
		IFileMgr fileMgr = buildStore.getFileMgr();
		IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
		
		String vfsPathName;
		int pathId = pkgRootMgr.getRootPath(rootName);
		if (pathId == ErrorCode.NOT_FOUND) {
			vfsPathName = "<invalid>";
		} else {
			vfsPathName = fileMgr.getPathName(pathId);
		}
		
		String nativePathName = pkgRootMgr.getPackageRootNative(pkgId, type);
		
		System.out.println("@" + rootName + " root:");
		System.out.println("  VFS path    : " + vfsPathName);
		System.out.println("  Native path : " + nativePathName);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Perform the -s (set root) command.
	 * 
	 * @param args		The command line arguments.
	 * @param firstArg  The index of the first argument to be used.
	 */
	private void setRoot(String[] args, int firstArg) {

		if (args.length != firstArg + 2) {
			showUsageAndExit("Incorrect number of arguments for -s option.");
		}
		
		String rootName = args[firstArg];
		String nativePath = args[firstArg + 1];
		
		/* 
		 * Set the mapping in the configuration file. It won't impact the BuildStore
		 * at all (for now), but the whole configuration will be read into the
		 * BuildStore when the "bml" script is next invoked.
		 */
		int rc = configFile.addNativeRootMapping(rootName, nativePath);
		if (rc == ErrorCode.NOT_FOUND) {
			fatal("Invalid root name: " + rootName);
		} else if (rc == ErrorCode.BAD_PATH) {
			fatal("Native path is not a valid directory: " + nativePath);
		}
		
		/* success - save the configuration */
		try {
			configFile.save();
		} catch (IOException e) {
			fatal("Problem saving configuration: " + e.getMessage());
		}
		System.out.println("Root \"" + rootName + "\" now refers to native path: " + nativePath);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Perform the -d (remove root) command.
	 * 
	 * @param args		The command line arguments.
	 * @param firstArg  The index of the first argument to be used.
	 */
	private void removeRoot(String[] args, int firstArg) {

		if (args.length != firstArg + 1) {
			showUsageAndExit("Incorrect number of arguments for -d option.");
		}
		String rootName = args[firstArg];
		
		/*
		 * Start by checking whether the mapping is already in place. This doesn't
		 * affect the result of this command, other than giving a meaningful error message.
		 */
		String oldMapping = configFile.getNativeRootMapping(rootName);
		
		/* 
		 * Make the change in the configuration file. When the BuildStore is next loaded (and
		 * the configuration file loaded into the BuildStore), the change will actually take place.
		 */
		int rc = configFile.clearNativeRootMapping(rootName);
		if (rc == ErrorCode.NOT_FOUND) {
			fatal("Invalid root name: " + rootName);
		}
		
		if (oldMapping == null) {
			fatal("Root is not currently mapped: " + rootName);
		}
		
		/* success - save the configuration */
		try {
			configFile.save();
		} catch (IOException e) {
			fatal("Problem saving configuration: " + e.getMessage());
		}
		System.out.println("Root \"" + rootName + "\" has been unmapped.");
	}

	/*-------------------------------------------------------------------------------------*/
}
