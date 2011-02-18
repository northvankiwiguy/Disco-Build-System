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
import com.arapiki.utils.os.FileSystemTraverseCallback;
import com.arapiki.utils.os.SystemUtils;

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
	 * Scan a real file system tree (on our local disk, not in the BuildStore), and add the
	 * paths into the BuildStore. It's important to note that the entire file path will be
	 * added into the BuildStore. For example, adding "/usr/include/sys" will result in all
	 * paths starting with the /usr/include/sys prefix. However, adding "sys" will result 
	 * in everything starting with just "/sys".
	 */
	public void scanForFiles(final String spaceName, String fileSystemPath) {
		
		/* these need to be final so the callback class (see later) can access them */
		final FileNameSpaces fns = bs.getFileNameSpaces();
		
		/* make the database really fast (turn off auto-commit ). */
		bs.setFastAccessMode(true);
		
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
						if (fns.addFile(spaceName, "/" + pathName) == -1){
							throw new FatalBuildTreeScannerError("Adding file name /" + pathName +
									" to BuildStore returned an error."); 
						}
					}
				});
		
		/* now commit everything */
		bs.setFastAccessMode(false);
	}

	/*=====================================================================================*/
}
