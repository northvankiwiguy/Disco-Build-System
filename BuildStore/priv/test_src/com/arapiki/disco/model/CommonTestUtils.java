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

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;

import com.arapiki.utils.types.IntegerTreeSet;


/**
 * Common methods for testing BuildStore and related classes.
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class CommonTestUtils {

	/*-------------------------------------------------------------------------------------*/

	/** The name of the temporary database file */
	private static String tempDbFile = "/tmp/testBuildStore";
	
	/**
	 * Create a new empty BuildStore, with an empty database. For
	 * testing purposes only.
	 * @return The empty BuildStore database
	 * @throws FileNotFoundException If the database file can't be opened
	 */
	public static BuildStore getEmptyBuildStore() throws FileNotFoundException {
		BuildStore bs = new BuildStore(tempDbFile);
		
		// force the schema to be dropped and recreated.
		bs.forceInitialize();
		
		return bs;
	}	
	
	/*-------------------------------------------------------------------------------------*/

	/* 
	 * Determine whether two arrays contain the same elements, even if those elements
	 * aren't in the same order in both arrays.
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
	 * @param fileSet The FileSet to compare
	 * @param integers The array of integers to compare
	 * @return True if the FileSet content is the same as the array content, else False
	 */
	public static boolean treeSetEqual(IntegerTreeSet<?> actual, Integer[] expected) {
		
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
