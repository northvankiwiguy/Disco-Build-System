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

package com.arapiki.disco.model;

import com.arapiki.utils.errors.ErrorCode;
import com.arapiki.utils.types.IntegerTreeSet;

/**
 * Implements an unordered set of FileRecord objects. This is used as the return
 * value from any method in the Reports class where the order of the returned values
 * is irrelevant (otherwise a FileRecord[] is used).
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class FileSet extends IntegerTreeSet<FileRecord>  {
	
	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/**
	 * The FileNameSpaces object that contains the files referenced in this FileSet
	 * object.
	 */
	private FileNameSpaces fns;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Constructor - creates a new FileSet and initializes it to being empty.
	 */	
	public FileSet(FileNameSpaces fns) {
		
		/* most of the functionality is provided by the IntegerTreeSet class */
		super();
		
		/* except we also need to record our FileNameSpaces object */
		this.fns = fns;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Constructor - creates a new FileSet and initializes it from an array of pathId values
	 */
	public FileSet(FileNameSpaces fns, Integer paths[]) {
		
		/* initialize the parent class */
		this(fns);
		
		/* copy the paths from the array into a FileSet */
		for (int i = 0; i < paths.length; i++) {
			FileRecord fr = new FileRecord(paths[i]);
			add(fr);
		}
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Given zero or more textual path names, populate the FileSet with the relevant files. Each
	 * pathArg String can be one of the following:
	 *    1) An absolute path name (starting with /), either a directory name or a file name. If the
	 *       path is a directory, add all files and directories below that point in the tree.
	 *    2) A path name starting with a root: - the same rules apply as for #1
	 *    3) A single file name, with one or more wildcard (*) characters. All files that match
     *       the name are added, no matter what their directory.
	 * 
	 *    @returns ErrorCode.OK on success, or ErrorCode.BAD_PATH if an invalid path name was
	 *    provided.
	 */
	public int populateWithPaths(String [] pathArgs) {
		
		/* for each path provided as input... */
		for (int i = 0; i < pathArgs.length; i++) {
			
			String thisPath = pathArgs[i];
			
			/* 
			 * First check case where a single file name (possibly with wildcards) is used.
			 * This implies there are no '/' or ':' characters in the path.
			 */
			if ((thisPath.indexOf('/') == -1) && (thisPath.indexOf(':') == -1)){
				
				/* map any occurrences of * into %, since that's what SQL requires */
				String regExp = thisPath.replace('*', '%');
				
				/* run a report to get all files that match this regexp */
				BuildStore bs = fns.getBuildStore();
				Reports reports = bs.getReports();
				FileSet results = reports.reportFilesThatMatchName(regExp);
				
				/* 
				 * Merge these results into this FileSet (we update the same file set
				 * for each user-supplied path).
				 */
				mergeSet(results);
			} 

			/* else add files/directories by name. Look up the path and add its children recursively */
			else {
				int pathId = fns.getPath(thisPath);
				if (pathId == ErrorCode.BAD_PATH) {
					return ErrorCode.BAD_PATH;
				}
			
				/* add this path to the FileSet, and all its children too */
				populateWithPathsHelper(pathId);
			}
		}
		
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * (non-Javadoc)
	 * @see com.arapiki.utils.types.IntegerTreeSet#mergeSet()
	 */
	public void mergeSet(FileSet second) {
		
		/* ensure the FileNameSpaces are the same for both FileSets */
		if (fns != second.fns) {
			return;
		}
		super.mergeSet(second);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.utils.types.IntegerTreeSet#getParent(int)
	 */
	@Override
	public int getParent(int id) {
		return fns.getParentPath(id);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.utils.types.IntegerTreeSet#newRecord(int)
	 */
	@Override
	public FileRecord newRecord(int id) {
		return new FileRecord(id);
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Helper function for populateWithPaths. Recursively add a path and its children to 
	 * this FileSet.
	 * @param pathId The ID of the path to be added.
	 */
	private void populateWithPathsHelper(int pathId) {
		
		/* add a new file record for this pathId, but only if it's not already in the FileSet */
		if (get(pathId) == null) {
			FileRecord fr = new FileRecord(pathId);
			add(fr);
		}
		
		/* now add all the children of this path */
		Integer children [] = fns.getChildPaths(pathId);
		for (int i = 0; i < children.length; i++) {
			populateWithPathsHelper(children[i]);
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
