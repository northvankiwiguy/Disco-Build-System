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

package com.buildml.model;

/**
 * This manager provides support for recording and retrieving information
 * on which files access which other files.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface IFileIncludeMgr {

	/**
	 * Record the fact that file1 somehow includes file2.
	 * @param file1 The file that does the including.
	 * @param file2 The file that is included.
	 */
	public abstract void addFileIncludes(int file1, int file2);

	/**
	 * Given a pair of files, where file1 depends on file2 in some way, return the count of
	 * how many times this dependency relationship has been noted. That is, how many times
	 * was addFileIncludes() called with this pair of files.
	 * 
	 * @param file1 The file that does the including.
	 * @param file2 The file that is included.
	 * @return The number of times the dependency was noted.
	 */
	public abstract int getFileIncludesCount(int file1, int file2);

	/**
	 * Return the total number of times that a specific file is included, regardless of who
	 * includes it.
	 * 
	 * @param file The file in which we're interested.
	 * @return The total number of times the file is accessed by one or more other files.
	 */
	public abstract int getTotalFileIncludedCount(int file);

	/**
	 * Return an Integer array of all files that include the specified file.
	 * 
	 * @param fileId ID of the file that is being included
	 * @return An Integer array of all files that include the specified file.
	 */
	public abstract Integer[] getFilesThatInclude(int fileId);

	/**
	 * Return an Integer array of all files that are included by the specified file.
	 * 
	 * @param fileId ID of the file that does the including.
	 * @return An Integer array of all files that are included by the specified file
	 */
	public abstract Integer[] getFilesIncludedBy(int fileId);

	/**
	 * Delete any file-includes relationship where the specified file does the including.
	 * @param pathId The file that does the include.
	 */
	public abstract void deleteFilesIncludedBy(int pathId);

}