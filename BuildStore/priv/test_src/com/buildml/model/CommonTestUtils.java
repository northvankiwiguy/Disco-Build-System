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

package com.buildml.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.buildml.model.FatalBuildStoreError;
import com.buildml.utils.types.IntegerTreeSet;

/**
 * Common methods for testing BuildStore and related classes.
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CommonTestUtils {

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create a new empty BuildStore, with an empty database. For
	 * testing purposes only.
	 * @param saveRequired True if this database must be explicitly saved.
	 * @return The empty BuildStore database
	 * @throws FileNotFoundException If the database file can't be opened
	 * @throws IOException An I/O problem occurred while opening the database.
	 */
	public static IBuildStore getEmptyBuildStore(boolean saveRequired) 
			throws FileNotFoundException, IOException {
		return getEmptyBuildStore(new File("/tmp"), saveRequired);
	}	

	/*-------------------------------------------------------------------------------------*/

	/*
	 * Similar to getEmptyBuildStore(boolean), but default to false for saveRequired.
	 */
	@SuppressWarnings("javadoc")
	public static IBuildStore getEmptyBuildStore() 
			throws FileNotFoundException, IOException {
		return getEmptyBuildStore(new File("/tmp"), false);
	}	

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create a new empty BuildStore in the user-specified directory. For testing
	 * purposes only.
	 * @param tmpDir The directory in which to place the BuildStore file.
	 * @param saveRequired True if this database must be explicitly saved.
	 * @return The empty BuildStore database.
	 * @throws FileNotFoundException If the database file can't be opened.
	 * @throws IOException An I/O problem occurred while opening the database.
	 */
	public static IBuildStore getEmptyBuildStore(File tmpDir, boolean saveRequired) 
			throws FileNotFoundException, IOException {
		IBuildStore bs;
		try {
			File bsFile = new File(tmpDir, "testBuildStore.bml");
			bsFile.delete();
			bs = BuildStoreFactory.createBuildStore(bsFile.toString(), saveRequired);
		} catch (BuildStoreVersionException e) {
			/* we can't handle schema version problems - make it a fatal error */
			throw new FatalBuildStoreError(e.getMessage());
		}
		
		return bs;
	}	
	
	/*-------------------------------------------------------------------------------------*/

	/*
	 * Similar to getEmptyBuildStore(File, boolean), but default to false for saveRequired.
	 */
	@SuppressWarnings("javadoc")
	public static IBuildStore getEmptyBuildStore(File tmpDir) 
			throws FileNotFoundException, IOException {
		return getEmptyBuildStore(tmpDir, false);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether two arrays contain the same elements, even if those elements
	 * aren't in the same order in both arrays. The equals() method is used to determine
	 * if elements are the same.
	 * @param arr1 The first array
	 * @param arr2 The second array
	 * @return True if the arrays contain the same elements, else false.
	 */
	public static boolean sortedArraysEqual(Object[] arr1, Object[] arr2) {
		
		/* if one is null, then both must be null */
		if (arr1 == null) {
			return (arr2 == null);
		}
		
		/* types must be the same */
		if (arr1.getClass() != arr2.getClass()){
			return false;
		}

		/* lengths must be the same */
		if (arr1.length != arr2.length) {
			return false;
		}
		
		/* now sort the elements and compare them */
		Arrays.sort(arr1);
		Arrays.sort(arr2);
		return Arrays.equals(arr1, arr2);		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Compare the content of a FileSet with an array of Integer values.
	 * @param actual The FileSet to compare
	 * @param expected The array of integers to compare
	 * @return True if the FileSet content is the same as the array content, else False
	 */
	public static boolean treeSetEqual(IntegerTreeSet actual, Integer[] expected) {
		
		/* first, translate the FileSet into an array of Integer */
		ArrayList<Integer> fsInts = new ArrayList<Integer>();
		for (Integer fsInt : actual) {
			fsInts.add(fsInt);
		}
		
		/* now compare the actual and expected values */
		return sortedArraysEqual(fsInts.toArray(new Integer[0]), expected);
	}

	/*-------------------------------------------------------------------------------------*/
}
