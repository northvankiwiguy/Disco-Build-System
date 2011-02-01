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

package com.arapiki.disco.scanner.buildtree;

import java.io.File;

import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.FileNameSpaces;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class FileSystemScanner {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/**
	 * The BuildStore associated with this FileSystemScanner.
	 */
	private BuildStore bs;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new FileSystemScanner object.
	 */
	public FileSystemScanner(BuildStore buildStore) {
		this.bs = buildStore;
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * 
	 */
	void scanForFiles(String rootName, String fileSystemPath) {
		File startingFile = new File(fileSystemPath);
		bs.setFastAccessMode(true);
		scanForFilesHelper(bs.getFileNameSpaces(), rootName, startingFile, "/");
		bs.setFastAccessMode(false);
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * 
	 */
	private void scanForFilesHelper(FileNameSpaces fns, String rootName, File thisFile, String relativePath) {
		
		/* if the file doesn't actually exist, there's nothing to do */
		if (!thisFile.exists()) {
			return;
		}
		
		String fileName = thisFile.getName();
		/* if this file is something we should ignore (based on it's name), skip over it */
		if (ignoreFile(fileName)){
			return;
		}
			
		/* if the file is actually a directory, recursively visit each of the entries */
		if (thisFile.isDirectory()) {
			File children [] = thisFile.listFiles();
			if (children == null) {
				return;
			}
			String subDirName = relativePath + fileName + "/";
			for (int i = 0; i < children.length; i++) {
				scanForFilesHelper(fns, rootName, children[i], subDirName);
			}
		}
		
		/* else if it's a file, register it in the BuildStore */
		else if (thisFile.isFile()) {
			fns.addFile(rootName, relativePath + fileName);
		}
		
		/* else, it's not a file or directory - throw an error, for now */
		else {
			throw new Error("Found a path that isn't a file or directory: " + relativePath + fileName);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @param name
	 * @return
	 */
	private boolean ignoreFile(String name) {
		return (name.equals("CVS") || name.equals(".git") ||
					name.equals(".gitignore") || name.endsWith("~"));
	}

	/*=====================================================================================*/
}
