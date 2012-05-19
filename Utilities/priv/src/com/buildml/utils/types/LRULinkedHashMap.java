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

import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * This utility class is essentially the same as a LinkedHashMap, although when
 * the number of elements reaches a predefined maximum, the LRU (least recently used)
 * element will be removed. This keeps the most important items in the cache, 
 * without allowing the cache to grow too large.
 * <p.
 * Note: in this case, LRU really means "Least Recently Accessed", as opposed to "Least
 * Recently Added".
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 * @param <K> The map's key type.
 * @param <V> The map's value type.
 */
@SuppressWarnings("serial")
public class LRULinkedHashMap<K, V> extends LinkedHashMap<K, V> {

	/** The maximum number of items permitted in the cache. */
	private int maxSize;
	
	/**
	 * Create a new LRULinkedHashMap class.
	 * @param maxSize The maximum number of elements to allow before removing the LRU
	 * element.
	 */
	public LRULinkedHashMap(int maxSize) {
		super(maxSize, 0.75f, true);
		this.maxSize = maxSize;
	}

	/**
	 * This method is called by the LinkedHashMap implementation to determine whether
	 * the LRU element should be removed.
	 * @see java.util.LinkedHashMap#removeEldestEntry(java.util.Map.Entry)
	 */
	@Override
	protected boolean removeEldestEntry(Entry<K, V> eldest) {
		return (size() >= maxSize);
	}
	
}