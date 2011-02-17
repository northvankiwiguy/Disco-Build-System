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
	 * 
	 */
	public void scanForFiles(final String spaceName, String fileSystemPath) {
		
		/* 
		 * Get the path's base name, since we want to remove it from any
		 * paths we add to a BuildStore. Note that if baseName is null, that's
		 * a valid case where there's no prefix (parent path) to remove.
		 */
		String baseName = new File(fileSystemPath).getParent();
		if (baseName != null) {
			baseName += "/";
		}
		
		/* these need to be final so the callback class (see later) can access them */
		final String prefixToRemove = baseName;
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
					 * When a file is located, remove our path prefix from its file name
					 * (if there is a prefix), then add it to the BuildStore.
					 */
					@Override
					public void callback(File thisPath) {
						String relativeName = thisPath.toString();
						if (prefixToRemove != null) {
							if (relativeName.startsWith(prefixToRemove)){
								relativeName = relativeName.substring(prefixToRemove.length());
							} else {
								throw new FatalBuildTreeScannerError("File name " + relativeName + 
										" is not within prefix " + prefixToRemove);
							}
						}
						if (fns.addFile(spaceName, "/" + relativeName) == -1){
							throw new FatalBuildTreeScannerError("Adding file name /" + relativeName +
									" to BuildStore returned an error."); 
						}
					}
				});
		
		/* now commit everything */
		bs.setFastAccessMode(false);
	}

	/*=====================================================================================*/
}
