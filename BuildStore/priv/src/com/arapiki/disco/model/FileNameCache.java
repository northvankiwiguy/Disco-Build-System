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

import java.util.LinkedHashMap;

import com.arapiki.utils.types.LRULinkedHashMap;

/**
 * A FileNameCache is used as means of caching the most commonly used content
 * in the "files" database table, rather than accessing the database every time
 * there's a need to get information. In particular, this cache stores the following
 * relationship:
 * 
 *   <parentFileId, childFileName> -> <childFileId, childFileType>
 * 
 * An LRU algorithm is used so that only the most recent N mappings are kept (to
 * avoid endless growth of the cache).
 *
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class FileNameCache {
	
	/*=====================================================================================*
	 * FIELDS
	 *=====================================================================================*/
	
	/** 
	 * The customized HashMap that will automatically remove the least recently accessed
	 * item once the cache fills up.
	 */
	private LRULinkedHashMap<FileNameCacheKey, FileNameCacheValue> map;
	
	/*=====================================================================================*
	 * NESTED CLASSES
	 *=====================================================================================*/

	/**
	 * This nested class is used as the key value in the FileNameCache. It simply contains
	 * the parent's file ID (int) and the child's file name (String). Given that it's used
	 * as the key in a hash table, we need custom-written equals() and hashCode() methods.
	 */
	public class FileNameCacheKey {
		
		/** The parent's file ID */
		private int parentFileId;
		
		/** The child's file name */
		private String childFileName;
		
		/**
		 * Create a new FileNameCacheKey object
		 * @param parentFileId The parent's file ID
		 * @param childFileName The child's file name
		 */
		public FileNameCacheKey(int parentFileId, String childFileName) {
			this.parentFileId = parentFileId;
			this.childFileName = childFileName;
		}

		/**
		 * @return the parentFileId
		 */
		public int getParentFileId() {
			return parentFileId;
		}

		/**
		 * @return the childFileName
		 */
		public String getChildFileName() {
			return childFileName;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof FileNameCacheKey)) {
				return false;
			}
			FileNameCacheKey keyObj = (FileNameCacheKey)obj;
			return (parentFileId == keyObj.parentFileId) && (childFileName.equals(keyObj.childFileName));
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return parentFileId + childFileName.hashCode();
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * This nested class is used as the "value" of the cache. The value
	 * is a pair consisting of <childFileId, childType>, which childType is
	 * the ordinal value of a FileNameSpaces.PathType value. 
	 */
	public class FileNameCacheValue {
		
		/** The child's file ID */
		private int childFileId;
		
		/** The child's type (directory, file, etc). */
		private int childType;

		/**
		 * Create a new FileNameCacheValue object.
		 * @param childFileId The child's file ID
		 * @param childType The child's type (directory, file, etc)
		 */
		public FileNameCacheValue(int childFileId, int childType) {
			this.childFileId = childFileId;
			this.childType = childType;
		}

		/**
		 * @return the childFileId
		 */
		public int getChildFileId() {
			return childFileId;
		}

		/**
		 * @return the childType
		 */
		public int getChildType() {
			return childType;
		}
	}

	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Constructor for the FileNameCache class.
	 * @parap maxSize the maximum number of items to hold in the cache.
	 */
	public FileNameCache(int maxSize) {
		map = new LRULinkedHashMap<FileNameCacheKey, FileNameCacheValue>(maxSize);
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
	 * Given a parent File ID number and the child's file name look up the value in
	 * the cache.
	 * @param parentFileId The parent's file ID number (actually, a directory)
	 * @param childFileName The name of the file within the parent's directory
	 * @return a FileNameCacheValue object containing the cache mapping, or null if the mapping
	 * isn't in the cache.
	 */
	public FileNameCacheValue get(int parentFileId, String childFileName) {
		FileNameCacheKey key = new FileNameCacheKey(parentFileId, childFileName);
		return map.get(key);		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add a new mapping to the cache.
	 * @param parentFileId The parent's file ID (actually, a directory)
	 * @param The name of the file within that parent's directory
	 * @param childFileId The child's file ID to be used as the target of the mapping
	 * @param childType What type is the child (file, directory, etc), to be used as
	 * the target of the mapping.
	 */
	public void put(int parentFileId, String childFileName, int childFileId, int childType) {
		FileNameCacheKey key = new FileNameCacheKey(parentFileId, childFileName);
		FileNameCacheValue value = new FileNameCacheValue(childFileId, childType);
		map.put(key, value);
	}

	/*-------------------------------------------------------------------------------------*/
}
