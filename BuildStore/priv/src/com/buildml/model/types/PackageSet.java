/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.model.types;

import java.util.HashMap;

import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;

/**
 * Represents a set of packages. Each package (and it's various scopes) can
 * be either be a member of this set, or not a member. Objects of this class are
 * typically used for recording which packages should be displayed/filtered.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class PackageSet implements Cloneable {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/**
	 * The BuildStore this PackageSet belongs to.
	 */
	private IBuildStore buildStore;
	
	/**
	 * The Packages object containing the packages/scopes that this set can have as
	 * members.
	 */
	private IPackageMgr pkgMgr = null;
	
	/**
	 * The default membership state for packages/scopes that have not explicitly been
	 * added to, or removed from this set.
	 */
	private boolean defaultState = false;
	
	/**
	 * The map that records the membership state. The "key" is the package's ID, and the
	 * "value" is a bitmap of scopes that are set (that is, bit 0 = None, bit 1 = Private, etc).
	 * If a package is not explicitly stored in this map, it's membership status defaults
	 * to "defaultState".
	 */
	private HashMap<Integer, Integer> membershipMap;
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Create a new PackageSet object. This records a subset of all possible packages
	 * from the specified buildStore. Objects of this type are typically used for selecting
	 * or filtering a list of packages.
	 * 
	 * @param buildStore The BuildStore that contains the packages.
	 */
	public PackageSet(IBuildStore buildStore) {
		
		this.buildStore = buildStore;
		pkgMgr = buildStore.getPackageMgr();
		
		/* Create a mapping of package IDs to a scope-ID bitmap */
		membershipMap = new HashMap<Integer, Integer>();
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * @return The BuildStore associated with this PackageSet.
	 */
	public IBuildStore getBuildStore() {
		return buildStore;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * When a package has not explicitly been added or removed from this set then
	 * a default state (true or false) is used to determine whether the member should
	 * be considered as a member. This is useful for when a set should contain "everything
	 * except for these packages".
	 * 
	 * @param defaultState The default state of packages/scopes that are not explicitly
	 * added or removed.
	 */
	public void setDefault(boolean defaultState) {
		this.defaultState = defaultState;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add the specified package into the set. Given that a scope ID isn't specified,
	 * all scopes will be added.
	 * 
	 * @param pkgId The ID of the package to be added.
	 */
	public void add(int pkgId) {
		
		/* 
		 * By adding -1 as the key, we're adding all scopes to the bitmap. This "put"
		 * may overwrite an existing value for this key.
		 */
		membershipMap.put(Integer.valueOf(pkgId), Integer.valueOf(-1));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add the specified package/scope into this set.
	 * @param pkgId The package's ID.
	 * @param scopeId The scope's ID.
	 */
	public void add(int pkgId, int scopeId) {

		Integer pkgIdInteger = Integer.valueOf(pkgId);
		int existingBitMap = 0;
		
		/* fetch the existing bitmap value (if it exists) */
		Object value = membershipMap.get(pkgIdInteger);
		if (value != null) {
			existingBitMap = (Integer)value;
		}
		
		/* merge in the new scopeId bit */
		existingBitMap |= (1 << scopeId);
		
		/* write it back to the map */
		membershipMap.put(pkgIdInteger, Integer.valueOf(existingBitMap));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Remove the specified package from the set.
	 * @param pkgId The package's ID.
	 */
	public void remove(int pkgId) {
		
		/*
		 * Set the whole entry to be 0, which removes all scopes. Note that this isn't
		 * the same as removing the map entry, since removing a package from a set
		 * that has setDefault(true) will cause the package to explicitly not be in
		 * the set, as opposed to being in the set by default.
		 */
		membershipMap.put(Integer.valueOf(pkgId), Integer.valueOf(0));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Remove the specified package/scope from this set.
	 * @param pkgId The package's ID.
	 * @param scopeId The scope's ID.
	 */
	public void remove(int pkgId, int scopeId) {

		Integer pkgIdInteger = Integer.valueOf(pkgId);
		int existingBitMap = 0;
		
		/* fetch the existing bitmap value (if it exists) */
		Object value = membershipMap.get(pkgIdInteger);
		if (value != null) {
			existingBitMap = (Integer)value;
		}
		
		/* 
		 * if it doesn't exist, figure out its default state. defaultState of true 
		 * means that all scopes are set (-1 in the bitmap).
		 */
		else {
			existingBitMap = defaultState ? -1 : 0;
		}
		
		/* merge in the new scopeId bit */
		existingBitMap &= ~(1 << scopeId);
		
		/* write it back to the map */
		membershipMap.put(pkgIdInteger, Integer.valueOf(existingBitMap));		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether the specified package is a member of this set. Given that the
	 * scope ID is not specified, a package is a member if any of its scopes are members.
	 * @param pkgId The package's ID.
	 * @return true if the package is a member, else false.
	 */
	public boolean isMember(int pkgId) {
		
		Object value = membershipMap.get(Integer.valueOf(pkgId));
		if (value == null) {
			/* package not register, return default setting */
			return defaultState;
		}
		
		/* 
		 * For the 1-parameter isMember function, we simply look at whether there are
		 * any scopes registered for this package.
		 */
		int scopeMap = (Integer)value;
		return (scopeMap != 0);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether the specified package/scope combination is a member of this set.
	 * @param pkgId The package's ID.
	 * @param scopeId The scope's ID.
	 * @return true if the package/scope is a member, else false.
	 */
	public boolean isMember(int pkgId, int scopeId) {
		
		Object value = membershipMap.get(Integer.valueOf(pkgId));
		if (value == null) {
			/* package not register, return default setting */
			return defaultState;
		}
		
		/* look within the bit-map to see if the scope's bit is set */
		int scopeMap = (Integer)value;
		return ((scopeMap & (1 << scopeId)) != 0);	
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Implement the standard Object clone() method for PackageSet, but perform a deep
	 * copy, rather than a shallow copy.
	 */
	@SuppressWarnings("unchecked")
	public Object clone() throws CloneNotSupportedException {
		PackageSet newCs = (PackageSet)super.clone();
		newCs.buildStore = this.buildStore;
		newCs.defaultState = this.defaultState;
		newCs.membershipMap = (HashMap<Integer, Integer>)this.membershipMap.clone();
		return newCs;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
