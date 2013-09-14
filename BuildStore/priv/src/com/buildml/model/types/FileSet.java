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

package com.buildml.model.types;

import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IReportMgr;
import com.buildml.utils.errors.ErrorCode;
import com.buildml.utils.types.IntegerTreeSet;

/**
 * Implements an unordered set of file IDs. This is used in numerous places
 * where a set of files must be passed as a single data value.
 * 
 * The FileSet data type is different from a regular set, since the parent/child
 * relationship between entries is maintained.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class FileSet extends IntegerTreeSet  {
	
	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/**
	 * The FileMgr manager object containing the files referenced in this FileSet.
	 */
	private IFileMgr fileMgr;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Creates a new FileSet and initializes it to empty.
	 * 
	 * @param fileMgr The FileMgr manager object that owns the files in the FileSet.
	 */	
	public FileSet(IFileMgr fileMgr) {
		
		/* most of the functionality is provided by the IntegerTreeSet class */
		super();
		
		/* except we also need to record our FileMgr object */
		this.fileMgr = fileMgr;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Creates a new FileSet and initializes it from an array of path ID values.
	 * 
	 * @param fileMgr The FileMgr manager object that owns the files in the FileSet.
	 * @param paths The IDs of the paths to be added to the FileSet.
	 */
	public FileSet(IFileMgr fileMgr, Integer paths[]) {
		super(paths);
		this.fileMgr = fileMgr;
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
	 *    <li>A path name starting with a \@root - the same rules apply as for #1.</li>
	 *    <li>A single file name, with one or more wildcard (*) characters. All files that match
     *       the name are added, no matter what their directory.</li>
     *    <li>A package spec, starting with %pkg, or the complement of a package, starting 
     *       with %not-pkg.</li>
     *  </ol>
	 * @return ErrorCode.OK on success, or ErrorCode.BAD_PATH if an invalid path name was
	 * provided.
	 */
	public int populateWithPaths(String [] pathArgs) {
		
		IBuildStore buildStore = fileMgr.getBuildStore();
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		IPackageMemberMgr pkgMemberMgr = buildStore.getPackageMemberMgr();
		IReportMgr reportMgr = buildStore.getReportMgr();

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
				
				if (commandName.equals("p") || commandName.equals("pkg")){
					FileSet results = pkgMemberMgr.getFilesInPackage(commandArgs);
					if (results == null) {
						return ErrorCode.BAD_PATH;
					}
					mergeSet(results);
				}
				else if (commandName.equals("np") || (commandName.equals("not-pkg"))){
					FileSet results = pkgMemberMgr.getFilesOutsidePackage(commandArgs);
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
				
				/* run a report to get all files that match this regexp */
				FileSet results = reportMgr.reportFilesThatMatchName(thisPath);
				
				/* 
				 * Merge these results into this FileSet (we update the same file set
				 * for each user-supplied path).
				 */
				mergeSet(results);
			} 

			/* else add files/directories by name. Look up the path and add its children recursively */
			else {
				int pathId = fileMgr.getPath(thisPath);
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
		
		/* ensure the FileMgrs are the same for both FileSets */
		if (fileMgr != second.fileMgr) {
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
		return fileMgr.getParentPath(id);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.utils.types.IntegerTreeSet#isValid(int)
	 */
	@Override
	public boolean isValid(int id) {
		return !fileMgr.isPathTrashed(id);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a path ID, return the array of children of the path.
	 * @param id The ID of the path whose children we wish to determine.
	 */
	@Override
	public Integer[] getChildren(int id) {
		return fileMgr.getChildPaths(id);
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
		
		add(pathId);
		
		/* now add all the children of this path */
		Integer children [] = fileMgr.getChildPaths(pathId);
		for (int i = 0; i < children.length; i++) {
			populateWithPathsHelper(children[i]);
		}
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * @return the maximum number of files that can be represented in this FileSet
	 * (numbered 0 to getMaxIdNumber() - 1).
	 */
	protected int getMaxIdNumber()
	{
		return IFileMgr.MAX_FILES;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
