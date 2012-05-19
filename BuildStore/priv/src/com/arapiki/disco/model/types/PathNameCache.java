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

import com.buildml.utils.types.LRULinkedHashMap;

/**
 * A PathNameCache is used as means of caching the most commonly used content
 * in the "files" database table, rather than accessing the database every time
 * there's a need to get information. In particular, this cache stores the following
 * relationship:
 * <p>
 *   &lt;parentPathId, childPathName&gt; maps to &lt;childPathId, childPathType&gt;
 * <p>
 * It's an extremely common operation to look up a child's path ID, given the
 * parent's ID and the child's name, so this has to be optimized.
 * <p>
 * An LRU algorithm is used so that only the most recent N mappings are kept (to
 * avoid endless growth of the cache).
 *
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class PathNameCache {
	
	/*=====================================================================================*
	 * FIELDS
	 *=====================================================================================*/
	
	/** 
	 * The customized HashMap that will automatically remove the least recently accessed
	 * item once the cache fills up.
	 */
	private LRULinkedHashMap<PathNameCacheKey, PathNameCacheValue> map;
	
	/*=====================================================================================*
	 * NESTED CLASSES
	 *=====================================================================================*/

	/**
	 * This nested class is used as the "key" in the PathNameCache. It simply contains
	 * the parent's path ID (int) and the child's path name (String). Given that it's used
	 * as the key in a hash table, we need custom-written equals() and hashCode() methods.
	 */
	public class PathNameCacheKey {
		
		/** The parent's path ID. */
		private int parentPathId;
		
		/** The child's path name. */
		private String childPathName;
		
		/**
		 * Create a new PathNameCacheKey object.
		 * 
		 * @param parentPathId The parent's path ID.
		 * @param childPathName The child's path name.
		 */
		public PathNameCacheKey(int parentPathId, String childPathName) {
			this.parentPathId = parentPathId;
			this.childPathName = childPathName;
		}

		/**
		 * Return the parent's path ID.
		 * @return The parent's path ID.
		 */
		public int getParentPathId() {
			return parentPathId;
		}

		/**
		 * Return the child's path name.
		 * @return The child's path name.
		 */
		public String getChildPathName() {
			return childPathName;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof PathNameCacheKey)) {
				return false;
			}
			PathNameCacheKey keyObj = (PathNameCacheKey)obj;
			return (parentPathId == keyObj.parentPathId) && (childPathName.equals(keyObj.childPathName));
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return parentPathId + childPathName.hashCode();
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * This nested class is used as the "value" of the cache. The value
	 * is a pair consisting of <childPathId, childType>, where childType is
	 * the ordinal value of a FileNameSpaces.PathType value. 
	 */
	public class PathNameCacheValue {
		
		/** The child's path ID. */
		private int childPathId;
		
		/** The child's type (directory, file, etc). */
		private int childType;

		/**
		 * Create a new PathNameCacheValue object.
		 * 
		 * @param childPathId The child's path ID.
		 * @param childType The child's type (directory, file, etc).
		 */
		public PathNameCacheValue(int childPathId, int childType) {
			this.childPathId = childPathId;
			this.childType = childType;
		}

		/**
		 * Return the child's path ID.
		 * @return The child's path ID.
		 */
		public int getChildPathId() {
			return childPathId;
		}

		/**
		 * Return the child's type.
		 * @return The child's type.
		 */
		public int getChildType() {
			return childType;
		}
	}

	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Constructor for the PathNameCache class.
	 * 
	 * @param maxSize the maximum number of items to hold in the cache.
	 */
	public PathNameCache(int maxSize) {
		map = new LRULinkedHashMap<PathNameCacheKey, PathNameCacheValue>(maxSize);
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Clear the cache, removing all stored items and reseting the size to 0.
	 */
	public void clear() {
		map.clear();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a parent Path ID number and the child's path name look up the value in
	 * the cache.
	 * 
	 * @param parentPathId The parent's path (directory) ID number.
	 * @param childPathName The name of the path within the parent's directory
	 * @return a PathNameCacheValue object containing the cache mapping, or null if
	 * the mapping isn't in the cache.
	 */
	public PathNameCacheValue get(int parentPathId, String childPathName) {
		PathNameCacheKey key = new PathNameCacheKey(parentPathId, childPathName);
		return map.get(key);		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add a new mapping to the cache.
	 * 
	 * @param parentPathId The parent's path ID (actually, a directory ID).
	 * @param childPathName The name of the path within that parent's directory.
	 * @param childPathId The child's path ID to be used as the target of the mapping.
	 * @param childType What type is the child (file, directory, etc), to be used as
	 * the target of the mapping.
	 */
	public void put(int parentPathId, String childPathName, int childPathId, int childType) {
		PathNameCacheKey key = new PathNameCacheKey(parentPathId, childPathName);
		PathNameCacheValue value = new PathNameCacheValue(childPathId, childType);
		map.put(key, value);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Remove an existing mapping from the cache.
	 * 
	 * @param parentPathId The parent's path (directory) ID number.
	 * @param childPathName The name of the path within the parent's directory
	 */
	public void remove(int parentPathId, String childPathName) {
		PathNameCacheKey key = new PathNameCacheKey(parentPathId, childPathName);
		map.remove(key);		
	}
	
	/*-------------------------------------------------------------------------------------*/
}
