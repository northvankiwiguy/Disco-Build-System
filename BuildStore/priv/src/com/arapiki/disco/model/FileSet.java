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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * Implements an unordered set of FileRecord objects. This is used as the return
 * value from any method in the Reports class where the order of the returned values
 * is irrelevant (otherwise a FileRecord[] is used).
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class FileSet implements Iterable<Integer> {
	
	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/**
	 * This object's internal data structure. This could easily be changed in
	 * the future to provide better scalability.
	 */
	private Hashtable<Integer, FileRecord> content;
	
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
		this.fns = fns;
		content = new Hashtable<Integer, FileRecord>();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Constructor - creates a new FileSet and initializes it from an array of pathId values
	 */
	public FileSet(FileNameSpaces fns, Integer paths[]) {
		this(fns);
		
		/* copy the paths from the array into a FileSet */
		for (int i = 0; i < paths.length; i++) {
			FileRecord fr = new FileRecord();
			fr.pathId = paths[i];
			add(fr);
		}
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * For all the path IDs already present in the FileSet, ensure that each of it's
	 * parent paths are also in the FileSet. This is useful for when displaying the
	 * report in the full tree hierarchy, in which case we must also know which parent
	 * files are to be shown. For example, if "/a/b/c.c" is in the list, then "/a/b", "/a"
	 * and "/" will also be added.
	 */
	public void populateWithParents() {

		/* fetch the list of path IDs already in the FileSet */
		Enumeration<Integer> keys = content.keys();
	
		/*
		 * For each path, add all of its parent paths, all the way up to "/". However,
		 * if any of this path's ancestors have already been added, there's no need to
		 * add it again.
		 */
		while (keys.hasMoreElements()) {
			int pathId = keys.nextElement();
			
			int parentId;
			while (true) {
				/* 
				 * Get the parent of this path - note that the parent of "/" is also "/",
				 * so the looping stops when the second "/" is found.
				 */
				parentId = fns.getParentPath(pathId);
			
				/* if the parent wasn't already added, insert a dummy FileRecord */
				if (!isMember(parentId)){
					FileRecord fr = new FileRecord();
					fr.pathId = parentId;
					add(fr);
					pathId = parentId;
				} 
				
				/* else, quit the loop */
				else {
					break;
				}
			};
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Add a new FileRecord to the FileSet.
	 * @param fileRecord The file record to add. The pathId field will be used as the index key and
	 * 			must therefore be unique.
	 */
	public void add(FileRecord fileRecord) {
		
		/* Simply store this record in the hash table */
		content.put(Integer.valueOf(fileRecord.pathId), fileRecord);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Fetch a FileRecord from the FileSet, using the pathId as the unique key.
	 * @param pathId The pathId of the FileRecord to retrieve.
	 * @return The FileRecord, or null if there's no corresponding record.
	 */
	public FileRecord get(int pathId) {
		
		/* Simply fetch the data from the hash table */
		return content.get(Integer.valueOf(pathId));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test whether a particular FileRecord is in the FileSet.
	 * @param pathId The pathId of the FileRecord we're searching for.
	 * @return True or False to indicate the FileRecord's presence.
	 */
	public boolean isMember(int pathId) {
		
		/* Ask the hash table if the key exists */
		return content.containsKey(Integer.valueOf(pathId));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Remove the specified FileRecord from the FileSet. If the record isn't
	 * in the FileSet, the FileSet is left unchanged.
	 * @param pathId The pathId of the FileRecord we're removing.
	 */
	public void remove(int pathId) {
		
		/* Attempt to remove the record from the hash table */
		content.remove(Integer.valueOf(pathId));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the number of FileRecord objects in the FileSet.
	 * @return the number of FileRecord objects.
	 */
	public int size() {
		
		/* Simply ask the underlying hash table */
		return content.size();
	}

	/*-------------------------------------------------------------------------------------*/

	/* 
	 * Provide an iterator to traverse the elements in the FileSet. The usual rules for iterators
	 * apply.
	 */
	@Override
	public Iterator<Integer> iterator() {
		return content.keySet().iterator();
	}

	/*-------------------------------------------------------------------------------------*/
}
