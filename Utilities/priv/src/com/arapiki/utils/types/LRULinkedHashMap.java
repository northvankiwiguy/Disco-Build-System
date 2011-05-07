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

import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * This utility class is essentially the same as a LinkedHashMap, although when
 * the number of elements reaches a predefined maximum, the LRU (least recently used)
 * element will be removed. This essentially keeps the most important items in
 * the cache, without allowing the cache to grow too large.
 * 
 * Note: in this case, LRU means "Least Recently Accessed", as opposed to "Least
 * Recently Added".
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 * @param <K> The map's key type
 * @param <V> The map's value type
 */
public class LRULinkedHashMap<K, V> extends LinkedHashMap<K, V> {

	/* the maximum number of items permitted in the cache */
	private int maxSize;
	
	/**
	 * Create a new LRULinkedHashMap class.
	 * @param maxSize
	 */
	public LRULinkedHashMap(int maxSize) {
		super(maxSize, 0.75f, true);
		this.maxSize = maxSize;
	}

	/* (non-Javadoc)
	 * @see java.util.LinkedHashMap#removeEldestEntry(java.util.Map.Entry)
	 */
	@Override
	protected boolean removeEldestEntry(Entry<K, V> eldest) {
		return (size() >= maxSize);
	}
	
}