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

import java.io.File;

import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.scanner.buildtree.FileSystemScanner;

/**
 * A helper class for DiscoMain. This class handles the disco commands that scan things
 * (trees, builds, etc). These methods should only be called by methods in DiscoMain.
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
/* package */ class DiscoScans {

	/*=====================================================================================*
	 * PACKAGE METHODS
	 *=====================================================================================*/
	
	/**
	 * Given an array of command line arguments, each being the name of a directory,
	 * scan and record the files found in those directories
	 * @param cmdArgs The array of directories (although cmdArgs[0] is actually the command
	 * 		name which should be skipped).
	 */
	/* package */ static void scanBuildTree(BuildStore buildStore, String[] cmdArgs) {
		
		/* validate that the directories exist */
		for (int i = 1; i < cmdArgs.length; i++) {
			
			File thisFile = new File(cmdArgs[i]);
			if (!thisFile.exists()){
				System.err.println("Error: directory (or file) doesn't exist: " + cmdArgs[i]);
				System.exit(1);
			}
		}

		/* all the directories/files exist, so let's scan them */
		FileSystemScanner fss = new FileSystemScanner(buildStore);

		/* for each directory (skipping the first one which is the command name) */
		for (int i = 1; i < cmdArgs.length; i++) {
			String dirName = cmdArgs[i];
			System.out.println("Scanning " + dirName);
			fss.scanForFiles("root", dirName);
		}		
	}
	
	/*-------------------------------------------------------------------------------------*/
}
