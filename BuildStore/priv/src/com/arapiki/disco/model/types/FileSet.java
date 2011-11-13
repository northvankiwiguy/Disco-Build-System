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

package com.arapiki.disco.model.types;

import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.Components;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.Reports;
import com.arapiki.utils.errors.ErrorCode;
import com.arapiki.utils.types.IntegerTreeSet;

/**
 * Implements an unordered set of FileRecord objects. This is used in numerous places
 * where a set of files must be passed as a single data value.
 * 
 * The FileSet data type is different from a regular set, since the parent/child
 * relationship between entries is maintained.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class FileSet extends IntegerTreeSet<FileRecord>  {
	
	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/**
	 * The FileNameSpaces manager object containing the files referenced in this FileSet.
	 */
	private FileNameSpaces fns;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Creates a new FileSet and initializes it to empty.
	 * 
	 * @param fns The FileNameSpaces manager object that owns the files in the FileSet.
	 */	
	public FileSet(FileNameSpaces fns) {
		
		/* most of the functionality is provided by the IntegerTreeSet class */
		super();
		
		/* except we also need to record our FileNameSpaces object */
		this.fns = fns;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Creates a new FileSet and initializes it from an array of path ID values.
	 * 
	 * @param fns The FileNameSpaces manager object that owns the files in the FileSet.
	 * @param paths The IDs of the paths to be added to the FileSet.
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
	 * Given zero or more text-based path name specifications, populate the FileSet with
	 * the relevant files. 
	 * 
	 * @param pathArgs Each pathArg String can be one of the following:
	 *  <ol>
	 *    <li>An absolute path name (starting with /), either a directory name or a file name. If the
	 *       path is a directory, all files and directories below that point in the tree are added.</li>
	 *    <li>A path name starting with a root: - the same rules apply as for #1.</li>
	 *    <li>A single file name, with one or more wildcard (*) characters. All files that match
     *       the name are added, no matter what their directory.</li>
     *    <li>A component spec, starting with %comp, or the complement of a component, starting 
     *       with %not-comp.</li>
     *  </ol>
	 * @return ErrorCode.OK on success, or ErrorCode.BAD_PATH if an invalid path name was
	 * provided.
	 */
	public int populateWithPaths(String [] pathArgs) {
		
		BuildStore bs = fns.getBuildStore();
		Components cmpts = bs.getComponents();
		Reports reports = bs.getReports();

		/* for each path provided as input... */
		for (int i = 0; i < pathArgs.length; i++) {
			
			String thisPath = pathArgs[i];
		
			/*
			 * First, check for "commands" that have the syntax: "%<name>/".
			 */
			if (thisPath.startsWith("%")){
				
				/* 
				 * Figure out what the "name" is. It must be terminated by a '/',
				 * which is then followed by the command's argument(s).
				 */
				int slashIndex = thisPath.indexOf('/');
				if (slashIndex == -1) { /* there must be a / */
					return ErrorCode.BAD_PATH;
				}
				String commandName = thisPath.substring(1, slashIndex);
				String commandArgs = thisPath.substring(slashIndex + 1);
				
				if (commandName.equals("c") || commandName.equals("comp")){
					FileSet results = cmpts.getFilesInComponent(commandArgs);
					if (results == null) {
						return ErrorCode.BAD_PATH;
					}
					mergeSet(results);
				}
				else if (commandName.equals("nc") || (commandName.equals("not-comp"))){
					FileSet results = cmpts.getFilesOutsideComponent(commandArgs);
					if (results == null) {
						return ErrorCode.BAD_PATH;
					}
					mergeSet(results);
				}
				
				/* else, it's a bad command name */
				else {
					return ErrorCode.BAD_PATH;
				}
				
			}
			
			/* 
			 * Next check case where a single file name (possibly with wildcards) is used.
			 * This implies there are no '/' or '@' characters in the path.
			 */
			else if ((thisPath.indexOf('/') == -1) && (thisPath.indexOf('@') == -1)){
				
				/* map any occurrences of * into %, since that's what SQL requires */
				String regExp = thisPath.replace('*', '%');
				
				/* run a report to get all files that match this regexp */
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
	 * Merge the content of a second FileSet into this FileSet.
	 * 
	 * @param second The second FileSet.
	 */
	public void mergeSet(FileSet second) {
		
		/* ensure the FileNameSpaces are the same for both FileSets */
		if (fns != second.fns) {
			return;
		}
		super.mergeSet(second);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a path ID, return the ID of that path's parent.
	 * 
	 * @param id The ID of the path whose parent we wish to determine.
	 * @return The ID of the path's parent.
	 */
	@Override
	public int getParent(int id) {
		return fns.getParentPath(id);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create a new FileRecord object, which can be used as an entry in the FileSet.
	 * 
	 * @param id The ID to assign to the new object.
	 * @return A new FileRecord object.
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
	 * 
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
