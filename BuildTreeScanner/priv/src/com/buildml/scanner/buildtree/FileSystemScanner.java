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

package com.buildml.scanner.buildtree;

import java.io.File;

import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.utils.errors.ErrorCode;
import com.buildml.utils.os.FileSystemTraverseCallback;
import com.buildml.utils.os.SystemUtils;

/**
 * A class for scanning the content of a local file system, and inserting the file
 * information into a BuildStore.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class FileSystemScanner {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/**
	 * The BuildStore associated with this FileSystemScanner.
	 */
	private IBuildStore buildStore;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new FileSystemScanner object.
	 * 
	 * @param buildStore The BuildStore into which the scanned information should be added.
	 */
	public FileSystemScanner(IBuildStore buildStore) {
		this.buildStore = buildStore;
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Scan a real file system tree (which is on our Unix file system), and add the
	 * paths into the BuildStore. The user-specified root path is the top location of the
	 * scan. All paths beneath that root will be added to the BuildStore.
     * <p>
     * It's important to note that the absolute file path will be added into the BuildStore. 
     * For example, using a rootName of "/usr/include/sys" will result in all paths
     * in the BuildStore having the /usr/include/sys prefix. 
     * 
	 * @param rootName The BuildStore root under which the files will be added (e.g. "root").
	 * @param fileSystemPath The path on the real Unix file system where the scanning
	 * should start.
	 */
	public void scanForFiles(final String rootName, String fileSystemPath) {
		
		/* these need to be final so the callback class (see later) can access them */
		final IFileMgr fileMgr = buildStore.getFileMgr();
		
		/* make the database really fast (turn off auto-commit ). */
		buildStore.setFastAccessMode(true);
		
		/* now traverse the file system */
		SystemUtils.traverseFileSystem(fileSystemPath, 
				null, 
				"CVS|.git", 
				SystemUtils.REPORT_FILES, 
				new FileSystemTraverseCallback() {
					
					/**
					 * When a file is located, add it to the BuildStore.
					 */
					@Override
					public void callback(File thisPath) {
						String pathName = thisPath.toString();
						if (fileMgr.addFile("@" + rootName + "/" + pathName) == ErrorCode.BAD_PATH){
							throw new FatalBuildTreeScannerError("Adding file name /" + pathName +
									" to BuildStore returned an error."); 
						}
					}
				});
		
		/* now commit everything */
		buildStore.setFastAccessMode(false);
	}

	/*-------------------------------------------------------------------------------------*/
}
