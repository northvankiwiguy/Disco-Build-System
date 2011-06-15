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

package com.arapiki.utils.types;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * This is an abstract class for implementing a tree-like set of integer keys, and their
 * respective values. Each entry is keyed by an integer, and is either in the set or not in the set.
 * Each key is associated with a value, whose type must extend IntegerTreeRecord.
 *
 * This data structure is described as tree-like because the elements have a parent-child relationship.
 * 
 * This class is sub-classed by FileSet and TaskSet, both of which are Integer-based Sets, with an associated
 * key value (FileRecord and TaskRecord).
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public abstract class IntegerTreeSet<T extends IntegerTreeRecord> implements Iterable<Integer> {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/**
	 * This object's internal data structure. This could easily be changed in
	 * the future to provide better scalability.
	 */
	protected Hashtable<Integer, T> content;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new IntegerTreeSet object.
	 */
	public IntegerTreeSet() {
		
		/* create the underlying hash table */
		content = new Hashtable<Integer, T>();
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Add a new IntegerTreeRecord (or derived class) to the IntegerTreeSet.
	 * @param record The file record to add. The ID field will be used as the index key and
	 * 			must therefore be unique. If not unique, the existing record will be
	 * 			overwritten.
	 */
	public void add(T record) {
		
		/* Simply store this record in the hash table */
		content.put(Integer.valueOf(record.getId()), record);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Fetch a IntegerTreeRecord from the IntegerTreeSet, using the id as the unique key.
	 * @param id The ID of the IntegerTreeRecord to retrieve.
	 * @return The IntegerTreeRecord, or null if there's no corresponding record.
	 */
	public T get(int id) {
		
		/* Simply fetch the data from the hash table */
		return content.get(Integer.valueOf(id));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test whether a particular IntegerTreeRecord is in the IntegerTreeSet.
	 * @param id The ID of the IntegerTreeRecord we're searching for.
	 * @return True or False to indicate the IntegerTreeRecord's presence.
	 */
	public boolean isMember(int id) {
		
		/* Ask the hash table if the key exists */
		return content.containsKey(Integer.valueOf(id));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Remove the specified IntegerTreeRecord from the IntegerTreeSet. If the record isn't
	 * in the IntegerTreeSet, the IntegerTreeSet is left unchanged.
	 * @param id The ID of the IntegerTreeRecord we're removing.
	 */
	public void remove(int id) {
		
		/* Attempt to remove the record from the hash table */
		content.remove(Integer.valueOf(id));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the number of IntegerTreeRecord objects in the IntegerTreeSet.
	 * @return the number of IntegerTreeRecord objects.
	 */
	public int size() {
		
		/* Simply ask the underlying hash table */
		return content.size();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return an iterator for traversing the keys in the IntegerTreeSet. The
	 * order the keys are visited is not specified.
	 */
	@Override
	public Iterator<Integer> iterator() {
		return content.keySet().iterator();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Abstract method for fetching the parent of a particular element. This method
	 * must be overridden by sub-classes that actually know what the parent-child relations
	 * should be.
	 * @param id The ID of the element we want to find the parent of
	 * @return The ID of this element's parent.
	 */
	public abstract int getParent(int id);

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Abstract method for constructing a new IntegerTreeRecord, or derived class. This
	 * must be overridden by all subclasses who know how to create a new record.
	 * @param id The ID to insert into the new record
	 * @return A new record, with the ID field set appropriately.
	 */
	public abstract T newRecord(int id);

	/*-------------------------------------------------------------------------------------*/

	/**
	 * For all the IDs already present in the IntegerTreeSet, ensure that each of it's
	 * parent IDs are also in the IntegerTreeSet. This is useful for when displaying the
	 * report in the full tree hierarchy, in which case we must also know which parent
	 * elements are to be shown. For example, in the case of FileSet, if "/a/b/c.c" is in
	 * the set, then "/a/b", "/a" and "/" will also be added.
	 */
	public void populateWithParents() {
	
		/* fetch the list of IDs already in the IntegerTreeSet */
		Enumeration<Integer> keys = content.keys();
	
		/*
		 * For each ID, add all of its parent Is, all the way up to the root. However,
		 * if any of this ID's ancestors have already been added, there's no need to
		 * add it again.
		 */
		while (keys.hasMoreElements()) {
			int elementId = keys.nextElement();
			
			int parentId;
			while (true) {
				/* 
				 * Get the parent of this ID - note that the parent of the root element
				 * must be itself (id.getParent() == id), which terminates the loop.
				 */
				parentId = getParent(elementId);
			
				/* if the parent wasn't already added, insert a new IntegerTreeRecord */
				if (!isMember(parentId)){
					T record = newRecord(parentId);
					add(record);
					elementId = parentId;
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
	 * Given a second IntegerTreeSet, mask off any files from this IntegerTreeSet that 
	 * don't appear in the second IntegerTreeSet. This is essentially a bitwise "and".
	 * @param mask The second set that acts as a mask value.
	 */
	public void maskSet(IntegerTreeSet<T> mask) {
		// TODO: implement this if ever needed
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a second IntegerTreeSet, merge all the files from that second set into this set. This is
	 * essentially a bitwise "or". If a particular path is already present in "this" IntegerTreeSet,
	 * we won't override it with the IntegerTreeRecord from "second" (this fact is only interesting if
	 * you care about the content of the IntegerTreeRecord).
	 */
	public void mergeSet(IntegerTreeSet<T> second) {
			
		/* for each element in the second IntegerTreeSet */
		for (Iterator<Integer> iterator = second.iterator(); iterator.hasNext();) {
			Integer elementId = (Integer) iterator.next();
			
			/* if it's not already in "this" IntegerTreeSet, add it */
			if (get(elementId) == null) {
				T secondElement = second.get(elementId);
				add(secondElement);
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/
}