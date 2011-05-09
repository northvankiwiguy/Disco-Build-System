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
import java.io.FileNotFoundException;
import java.io.IOException;

import org.xml.sax.SAXException;

import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.scanner.FatalBuildScannerError;
import com.arapiki.disco.scanner.buildtree.FatalBuildTreeScannerError;
import com.arapiki.disco.scanner.buildtree.FileSystemScanner;
import com.arapiki.disco.scanner.electricanno.ElectricAnnoScanner;

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
			String dirNameAbs;
			try {
				dirNameAbs = new File(dirName).getCanonicalPath();
			} catch (IOException e) {
				throw new FatalBuildTreeScannerError("Can't determine absolute path of " + dirName);
			}
			System.out.println("Scanning " + dirNameAbs);
			fss.scanForFiles("root", dirNameAbs);
		}		
	}

	/**
	 * Parse an ElectricAccelerator annotation file and store the data in the specified 
	 * BuildStore file.
	 * @param buildStore
	 * @param cmdArgs
	 */
	public static void scanElectricAnno(BuildStore buildStore, String[] cmdArgs) {
		String fileName = cmdArgs[1];
		
		ElectricAnnoScanner eas = new ElectricAnnoScanner(buildStore);
		try {
			eas.parse(fileName);

		} catch (FileNotFoundException e) {
			System.err.println("Error: ElectricAccelerator annotation file " + fileName + " not found.");
			System.exit(1);
			
		} catch (IOException e) {
			System.err.println("Error: I/O error while reading ElectricAccelerator annotation file " + fileName);
			System.exit(1);

		} catch (SAXException e) {
			System.err.println("Error: Unexpected syntax in ElectricAccelerator annotation file " + fileName);
			System.exit(1);
		} catch (FatalBuildScannerError e) {
			System.err.println("Error: Logic problem while scanning ElectricAccelerator annotation file " + fileName);
			System.err.println(e.getMessage());
			System.exit(1);
		}
		
		
	}
	
	/*-------------------------------------------------------------------------------------*/
}
