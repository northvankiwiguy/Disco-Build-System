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

package com.buildml.utils.types;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.buildml.utils.errors.ErrorCode;
import com.buildml.utils.errors.FatalError;

/**
 * This is an abstract class for implementing a tree-like set of integer keys, and their
 * respective values. That is, the elements in the set are arranged in a tree structure
 * (with parents and children), although an element may or may not exist in the set. The
 * main use of this data structure is to select sub-sets of an overall tree structure.
 * <p>
 * Each entry is keyed by an integer, and is either in the set or not in the set.
 * <p>
 * This class is sub-classed by FileSet and ActionSet, both of which are Integer-based sets.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public abstract class IntegerTreeSet implements Iterable<Integer>, Cloneable {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/*
	 * In order to store a bitmap of entries, we use a bucket-like system that's similar
	 * to how Unix file systems work. That is, we keep a top-level "bucket array" where
	 * each entry points to a second-level bitmap. If one of the bucket array's pointers
	 * is null, then that portion of the set is considered to be completely empty. This
	 * permits the allocation of a small amount of memory for small sets, and a larger amount
	 * for larger sets. It also allows the creation of a sparse bit-map, which is necessary
	 * to represents sets resulting from a database query (where the query returns a small
	 * number of sparsely distributed results).
	 */
	
	/** 
	 * The number of bits represented in each bucket (must be a multiple of 8). Also, 
	 * getMaxIdNumber() must be evenly divisible by BUCKET_SIZE.
	 */
	private static final int BUCKET_SIZE = 2048;
	
	/** The initial number of buckets that a newly-created set will start with. */
	private static final int INITIAL_NUM_BUCKETS = 1;
	
	/**
	 * When the bucket array needs to grow, we'll increase it by this many new buckets.
	 * Note that each time we grow the bucket array, we also increase this value. Therefore,
	 * our first increase will add 1 new bucket, the second increase will add 2, the third
	 * increase will add 3 new buckets, etc. Therefore, for small sets we'll only add
	 * small increments, but for large sets we'll pre-allocate a larger number of new
	 * buckets.
	 */
	private int currentBucketIncrease = 1;

	/**
	 * Each entry in the bucket array has the following content. It tracks the set
	 * membership for BUCKET_SI
	 */
	private class IntegerTreeSetBucket {
		
		/** the number of bits set in this bucket */
		public int size;
		
		/** the actual bit map of set members (numbered 0 -> BUCKET_SIZE - 1) */
		public byte content[];
		
		/** create a new bucket, with a newly allocated bitmap */
		public IntegerTreeSetBucket() {
			size = 0;
			content = new byte[BUCKET_SIZE];
		}
	}
	
	/** 
	 * The top-level array of buckets. This start out having INITIAL_NUM_BUCKETS members,
	 * and can grow to a maximum of (getMaxIdNumber() / BUCKET_SIZE) entries.
	 */
	private IntegerTreeSetBucket bucketArray[] = null;

	/**
	 * The current size of bucketArray[].
	 */
	private int currentBucketArraySize;
	
	/**
	 * The current number of members in this set.
	 */
	private int totalMembers;
	
	/*=====================================================================================*
	 * NESTED CLASS - IntegerTreeSetIterator
	 *=====================================================================================*/

	/**
	 * An Iterator for traversing the content of an IntegerTreeSet.
	 */
	private class IntegerTreeSetIterator implements Iterator<Integer> {

		/**
		 * The most recent element of the TreeSet that we reported to the user.
		 */
		private int currentId = -1;
		
		/**
		 * Have we identified a "next" element to report when the user calls next()?
		 */
		private boolean nextAvailable = false;
		
		/*---------------------------------------------------------------------------------*/

		/**
		 * Search through the set to find the next element that's set. 
		 */
		private void searchForNext() {
			int id = currentId + 1;
			
			while (true) {
				
				int idBucket = (id / BUCKET_SIZE);

				/* reached the end of whole bucketArray? */
				if (idBucket == currentBucketArraySize) {
					currentId = -1;
					return;
				}
			
				/* current bucket is empty, skip to the start of the next */
				IntegerTreeSetBucket bucket = bucketArray[idBucket];
				if (bucket == null) {
					id = (id + BUCKET_SIZE) & ~(BUCKET_SIZE - 1);
				}
				
				/*
				 * We know there's something in this bucket. Look forward
				 * (beyond the current point), in case we find it (we may not,
				 * if we already reported it).
				 */
				else {
					int bucketOffset = (id & (BUCKET_SIZE - 1)) >> 3;
					for (int i = bucketOffset; i != (BUCKET_SIZE >> 3); i++){
				
						/* current byte is empty, skip to the start of the next */
						if (bucket.content[i] == 0) {
							id = (id + 8) & ~7;
						}
						else
						{							
							/* check the individual bits in the byte */
							int bucketBit = id & 7;
							byte bitMap = bucket.content[i]; 
							for (int j = bucketBit; j != 8; j++){
								if ((bitMap & (1 << bucketBit)) != 0) {
									currentId = id;
									return;
								}
								else {
									bucketBit++;
									id++;
								}		
							}
						}
					}
				}
			}
		}
		
		/*---------------------------------------------------------------------------------*/

		@Override
		public boolean hasNext() {
			if (!nextAvailable){
				searchForNext();
				nextAvailable = true;
			}
			return (currentId != -1);
		}

		/*---------------------------------------------------------------------------------*/

		@Override
		public Integer next() {
			if (!hasNext()) {
				throw new NoSuchElementException(); 
			}
			nextAvailable = false;
			return currentId;
		}

		/*---------------------------------------------------------------------------------*/

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		/*---------------------------------------------------------------------------------*/
		
	};
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new IntegerTreeSet object.
	 */
	public IntegerTreeSet() {
		
		/* create an initial bucket array, with nothing in it */
		currentBucketArraySize = INITIAL_NUM_BUCKETS;
		bucketArray = new IntegerTreeSetBucket[currentBucketArraySize];
		totalMembers = 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create a new IntegerTreeSet and initialize it from an array of integer values.
	 * 
	 * @param initValues The initial values to be added to the set.
	 */
	public IntegerTreeSet(Integer[] initValues) {
		this();
		for (int i = 0; i < initValues.length; i++) {
			add(initValues[i]);
		}
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Add a new member into the IntegerTreeSet.
	 * 
	 * @param id The ID to be added to the set. If the ID already exists in the set, the
	 * result set will be unchanged.
	 */
	public void add(int id) {

		/*
		 * There are several cases to consider here:
		 *   1) Is the 'id' within the range of 0 -> getMaxIdNumber()? If not, throw
		 *      an exception. This is a programming error.
		 *   2) Is the 'id' within the range of 0 -> (currentBucketArraySize * BUCKET_SIZE) - 1?
		 *      If not, we'll need to grow the bucketArray to allow for this new element.
		 *   3) Is the 'id' within a bucket that currently exists, or is the bucketArray
		 *      pointer currently null?
		 *   4) If the bucket already exists, is the bit already set?
		 */
		
		/* case 1 - ID completely out of range - programming error */
		if ((id < 0) || (id >= getMaxIdNumber())) {
			throw new FatalError(
					"New entry to set: " + id + " is beyond maximum allowed value: " + 
					getMaxIdNumber());
		}
		
		/* case 2 - grow the bucket array to contain the new ID. */
		int idBucket = (id / BUCKET_SIZE);
		if (idBucket >= currentBucketArraySize) {
			growBucketArray(idBucket);
		}
		
		/* case 3 - if the bucket for this 'id' is null */
		if (bucketArray[idBucket] == null) {
			bucketArray[idBucket] = new IntegerTreeSetBucket();
		}
		
		/* case 4 - is the corresponding bit already set? */
		IntegerTreeSetBucket bucket = bucketArray[idBucket];
		int bucketOffset = (id & (BUCKET_SIZE - 1)) >> 3;
		int bucketBit = (id & 0x7);
		if ((bucket.content[bucketOffset] & (1 << bucketBit)) != 0) {
			/* bit already set - do nothing */
		}
		
		/* set the bit, and update the bucket's size */
		else {
			bucket.content[bucketOffset] |= (1 << bucketBit);
			bucket.size++;
			totalMembers++;
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test whether a particular element is in the set.
	 * 
	 * @param id The ID of the element we're testing for.
	 * @return True or False to indicate the members presence.
	 */
	public boolean isMember(int id) {

		/*
		 * Cases to consider:
		 *   1) Is the 'id' beyond the end of the bucketArray? Return false.
		 *   2) Is the 'id' in a bucket that has a null pointer? Return false.
		 *   3) Is the 'id' present in the bucket's bitmap?
		 */
		
		/* case 1 */
		if ((id < 0) || (id >= (currentBucketArraySize * BUCKET_SIZE))) {
			return false;
		}
		
		/* case 2 */
		int idBucket = (id / BUCKET_SIZE);
		if (bucketArray[idBucket] == null) {
			return false;
		}
		
		IntegerTreeSetBucket bucket = bucketArray[idBucket];
		int bucketOffset = (id & (BUCKET_SIZE - 1)) >> 3;
		int bucketBit = (id & 0x7);
		return (bucket.content[bucketOffset] & (1 << bucketBit)) != 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Remove the specified element from the set. If the record isn't
	 * in the set, the set is left unchanged.
	 * 
	 * @param id The ID of the element we're removing.
	 */
	public void remove(int id) {
		
		/*
		 * Cases to consider:
		 *   1) Is the 'id' beyond the end of the bucketArray? Do nothing.
		 *   2) Is the 'id' in a bucket that has a null pointer? Do nothing.
		 *   3) Is the 'id' bit in the bucket's bit map set? If not, do nothing.
		 *   4) Remove the bit from the set, decrement the size, and possibly
		 *      remove the bucket (setting it to null in the array) if size is now 0.
		 */
		
		/* case 1 */
		if ((id < 0) || (id >= (currentBucketArraySize * BUCKET_SIZE))) {
			return;
		}
		
		/* case 2 */
		int idBucket = (id / BUCKET_SIZE);
		if (bucketArray[idBucket] == null) {
			return;
		}
		
		/* case 3 */
		IntegerTreeSetBucket bucket = bucketArray[idBucket];
		int bucketOffset = (id & (BUCKET_SIZE -1 )) >> 3;
		int bucketBit = (id & 0x7);
		if ((bucket.content[bucketOffset] & (1 << bucketBit)) == 0) {
			return;
		}
		
		/* case 4 - remove the element, decrement the size */
		bucket.content[bucketOffset] &= ~(1 << bucketBit);
		bucket.size--;
		totalMembers--;
		if (bucket.size == 0) {
			bucketArray[idBucket] = null;
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the number of members in the set.
	 * 
	 * @return the number of members in the set.
	 */
	public int size() {		
		return totalMembers;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * An iterator for traversing the keys in the set.
	 * @return An iterator for traversing the keys in the set. The order the keys are visited 
	 * is not specified.
	 */
	@Override
	public Iterator<Integer> iterator() {
		return new IntegerTreeSetIterator();
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Implement the standard Object clone() method for IntegerTreeSet, but perform a deep
	 * copy, rather than a shallow copy.
	 */
	public Object clone() throws CloneNotSupportedException {
		
		/* retrieve the new object */
		IntegerTreeSet newSet = (IntegerTreeSet)super.clone();
		
		/* clone the top-level fields */
		newSet.currentBucketArraySize = this.currentBucketArraySize;
		newSet.currentBucketIncrease = this.currentBucketIncrease;
		newSet.totalMembers = this.totalMembers;
		newSet.bucketArray = new IntegerTreeSetBucket[newSet.currentBucketArraySize];
		
		/* clone each element of the bucket array */
		for (int i = 0; i != newSet.currentBucketArraySize; i++) {
			IntegerTreeSetBucket oldBucket = this.bucketArray[i]; 
			if (oldBucket != null) {
				IntegerTreeSetBucket newBucket = new IntegerTreeSetBucket();
				newSet.bucketArray[i] = newBucket;
				newBucket.size = oldBucket.size;
				newBucket.content = oldBucket.content.clone();
			}
		}
		return newSet;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Abstract method for fetching the parent of a particular element. This method
	 * must be overridden by sub-classes that actually know what the parent-child relations
	 * should be.
	 * 
	 * @param id The ID of the element we want to find the parent of.
	 * @return The ID of this element's parent, or ErrorCode.NOT_FOUND.
	 */
	public abstract int getParent(int id);

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Abstract method for determining whether a particular element is valid (exists in
	 * the database and isn't trashed).
	 * 
	 * @param id The ID of the element to determine the valid status of.
	 * @return True if the ID is valid, else false.
	 */
	public abstract boolean isValid(int id);
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Abstract method for fetching the array of children of a particular element. This
	 * method must be overridden by sub-classes that actually know what the parent-child
	 * relations should be.
	 * @param id The ID of the element we want to find the children of.
	 * @return An Integer[] of IDs of this element's children. If there are no children,
	 * 			return Integer[0].
	 */
	public abstract Integer[] getChildren(int id);
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * For all the IDs already present in the set, ensure that each of it's
	 * parent IDs are also in the set. This is useful for when displaying the
	 * report in the full tree hierarchy, in which case we must also know which parent
	 * elements are to be shown. For example, in the case of FileSet, if "/a/b/c.c" is in
	 * the set, then "/a/b", "/a" and "/" will also be added.
	 */
	public void populateWithParents() {
	
		/*
		 * Fetch the list of IDs already in the IntegerTreeSet. We'll be modifying
		 * ourselves, so we first need to make a copy of ourselves, as a stable
		 * base for repetition.
		 */
		IntegerTreeSet copy;
		try {
			copy = (IntegerTreeSet)this.clone();
		} catch (CloneNotSupportedException e) {
			throw new FatalError("clone() not support for IntegerTreeSet.");
		}
		
		/*
		 * For each ID, add all of its parent Is, all the way up to the root. However,
		 * if any of this ID's ancestors have already been added, there's no need to
		 * add it again.
		 */
		for (Integer elementId : copy) {
			
			if (!isValid(elementId)) {
				continue;
			}
			
			int parentId;
			while (true) {
				/* 
				 * Get the parent of this ID - note that the parent of the root element
				 * must be itself (id.getParent() == id), which terminates the loop.
				 */
				parentId = getParent(elementId);
			
				/* if the parent wasn't already added, insert a new IntegerTreeRecord */
				if (!isMember(parentId)){
					add(Integer.valueOf(parentId));
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
	 * Given a second set, mask off any files from this set that 
	 * don't appear in the second set. This is essentially a bitwise "and".
	 * 
	 * @param mask The second set that acts as a mask value.
	 */
	public void maskSet(IntegerTreeSet mask) {
		// TODO: implement this if ever needed
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a second set, remove any files from this set that appear in the second set.
	 * 
	 * @param second The second set containing the values to be removed.
	 */
	public void extractSet(IntegerTreeSet second) {
		
		/* for each element in the second IntegerTreeSet */
		for (Iterator<Integer> iterator = second.iterator(); iterator.hasNext();) {
			Integer elementId = (Integer) iterator.next();
			
			/* if it's currently in "this" IntegerTreeSet, remove it */
			if (isMember(elementId) && second.isMember(elementId)) {
				remove(elementId);
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a second set, merge all the files from that second set into this set. This is
	 * essentially a bitwise "or". If a particular path is already present in "this" set,
	 * we won't override it with the IntegerTreeRecord from "second" (this fact is only interesting if
	 * you care about the content of the IntegerTreeRecord).
	 * 
	 * @param second The second set to merge into this set
	 */
	public void mergeSet(IntegerTreeSet second) {
			
		/* for each element in the second IntegerTreeSet */
		for (Iterator<Integer> iterator = second.iterator(); iterator.hasNext();) {
			Integer elementId = (Integer) iterator.next();
			
			/* if it's not already in "this" IntegerTreeSet, add it */
			if (!isMember(elementId) && second.isMember(elementId)) {
				add(elementId);
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add all the elements in the sub-tree which is rooted at "id". If necessary, the parents
	 * of "id" will also be added so that all newly added elements are reachable from
	 * the root.
	 * 
	 * @param id The tree element whose sub-tree should be added to the set.
	 */
	public void addSubTree(int id) {
		
		/* first, add this path and all it's descendants. This is done recursively */
		addSubTreeHelper(id);
		
		/* now progress upwards, ensuring that all parents are added too */
		while (true) {
			int parentId = getParent(id);
			
			/* stop on error, or if we hit the root (/) */
			if ((parentId == ErrorCode.NOT_FOUND) || (parentId == id)) {
				break;
			}
			add(parentId);
			id = parentId;
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Remove all the element in the sub-tree which is rooted at "id".
	 * @param id The tree element whose sub-tree should be removed from the set.
	 */
	public void removeSubTree(int id) {
		remove(id);
		Integer children[] = getChildren(id);
		for (int i = 0; i < children.length; i++) {
			removeSubTree(children[i]);
		}
	}
	
	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/
	
	/**
	 * Returns the maximum allowable ID number for this set. Each child class must
	 * implement this method to return the appropriate maximum for their purpose.
	 * 
	 * @return The maximum allowable ID number for this set.
	 */
	protected abstract int getMaxIdNumber();
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * A helper method for addSubTree() that traverses downwards through the children
	 * of the given path.
	 * @param id The path whose sub-tree should be added to the set.
	 */
	private void addSubTreeHelper(int id) {
		add(id);
		Integer children [] = getChildren(id);
		for (int i = 0; i < children.length; i++) {
			addSubTreeHelper(children[i]);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Grow the current bucket array so that it's large enough to contain the specified
	 * ID number. In fact, we may grow by more than the minimum necessary size, to make
	 * sure we have room for growth. We already know that 'id' is less than our absolute
	 * maximum value, so that doesn't need to be checked twice.
	 * 
	 * @param idBucket The new bucket number that must be accommodated in the bucket array.
	 */
	private void growBucketArray(int idBucket) {
		
		int newBucketArraySize = idBucket + currentBucketIncrease;
		
		/* but never grow larger than the maximum bucket size allows */
		if (newBucketArraySize > (getMaxIdNumber() / BUCKET_SIZE)) {
			newBucketArraySize = getMaxIdNumber() / BUCKET_SIZE;
		}
		
		/* allocate a new bucketArray, copy over the old content, then discard the old array */
		IntegerTreeSetBucket newArray[] = new IntegerTreeSetBucket[newBucketArraySize];
		System.arraycopy(bucketArray, 0, newArray, 0, currentBucketArraySize);
		bucketArray = newArray;
		currentBucketArraySize = newBucketArraySize;
		
		/* the more often we grow the array, the larger we should grow each time */
		currentBucketIncrease++;
	}

	/*-------------------------------------------------------------------------------------*/
}