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

package com.arapiki.disco.model.types;

import java.util.HashMap;

import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.Components;

/**
 * Represents a set of components. Each component (and it's various scopes) can
 * be either be a member of this set, or not a member. Objects of this class are
 * typically used for recording which components should be displayed/filtered.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class ComponentSet {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/**
	 * The Components object containing the components/scopes that this set can have as
	 * members.
	 */
	private Components cmpts = null;
	
	/**
	 * The default membership state for components/scopes that have not explicitly been
	 * added to, or removed from this set.
	 */
	private boolean defaultState = false;
	
	/**
	 * The map that records the membership state. The "key" is the component's ID, and the
	 * "value" is a bitmap of scopes that are set (that is, bit 0 = None, bit 1 = Private, etc).
	 * If a component is not explicitly stored in this map, it's membership status defaults
	 * to "defaultState".
	 */
	private HashMap<Integer, Integer> membershipMap;
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Create a new ComponentSet object. This records a subset of all possible components
	 * from the specified buildStore. Objects of this type are typically used for selecting
	 * or filtering a list of components.
	 * 
	 * @param buildStore The BuildStore that contains the components.
	 */
	public ComponentSet(BuildStore buildStore) {
		
		cmpts = buildStore.getComponents();
		
		/* Create a mapping of component IDs to a scope-ID bitmap */
		membershipMap = new HashMap<Integer, Integer>();
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Fetch the complete list of components that may appear in this set.
	 * @return An alphabetically ordered list of component names.
	 */
	public String[] getComponents() {
		return cmpts.getComponents();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a component's name, return its ID number.
	 * 
	 * @param compName The component's name.
	 * @return The component's ID number, ErrorCode.NOT_FOUND if there's no component
	 * with this name.
	 */
	public int getComponentId(String compName) {
		return cmpts.getComponentId(compName);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Fetch the list of scopes that can be represented in this ComponentSet.
	 * 
	 * @return An alphabetically ordered list of scope names.
	 */
	public String[] getScopes() {
		return cmpts.getSections();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a scope name, return its ID number. 
	 * 
	 * @param scopeName The scope's name (e.g. "Private")
	 * @return The scope's ID number, or ErrorCode.NOT_FOUND if the name isn't valid.
	 */
	public int getScopeId(String scopeName) {
		return cmpts.getSectionId(scopeName);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * When a component has not explicitly been added or removed from this set then
	 * a default state (true or false) is used to determine whether the member should
	 * be considered as a member. This is useful for when a set should contain "everything
	 * except for these components".
	 * 
	 * @param defaultState The default state of components/scopes that are not explicitly
	 * added or removed.
	 */
	public void setDefault(boolean defaultState) {
		this.defaultState = defaultState;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add the specified component into the set. Given that a scope ID isn't specified,
	 * all scopes will be added.
	 * 
	 * @param compId The ID of the component to be added.
	 */
	public void add(int compId) {
		
		/* 
		 * By adding -1 as the key, we're adding all scopes to the bitmap. This "put"
		 * may overwrite an existing value for this key.
		 */
		membershipMap.put(Integer.valueOf(compId), Integer.valueOf(-1));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add the specified component/scope into this set.
	 * @param compId The component's ID.
	 * @param scopeId The scope's ID.
	 */
	public void add(int compId, int scopeId) {

		Integer compIdInteger = Integer.valueOf(compId);
		int existingBitMap = 0;
		
		/* fetch the existing bitmap value (if it exists) */
		Object value = membershipMap.get(compIdInteger);
		if (value != null) {
			existingBitMap = (Integer)value;
		}
		
		/* merge in the new scopeId bit */
		existingBitMap |= (1 << scopeId);
		
		/* write it back to the map */
		membershipMap.put(compIdInteger, Integer.valueOf(existingBitMap));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Remove the specified component from the set.
	 * @param compId The component's ID.
	 */
	public void remove(int compId) {
		
		/*
		 * Set the whole entry to be 0, which removes all scopes. Note that this isn't
		 * the same as removing the map entry, since removing a component from a set
		 * that has setDefault(true) will cause the component to explicitly not be in
		 * the set, as opposed to being in the set by default.
		 */
		membershipMap.put(Integer.valueOf(compId), Integer.valueOf(0));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Remove the specified component/scope from this set.
	 * @param compId The component's ID.
	 * @param scopeId The scope's ID.
	 */
	public void remove(int compId, int scopeId) {

		Integer compIdInteger = Integer.valueOf(compId);
		int existingBitMap = 0;
		
		/* fetch the existing bitmap value (if it exists) */
		Object value = membershipMap.get(compIdInteger);
		if (value != null) {
			existingBitMap = (Integer)value;
			
			/* merge in the new scopeId bit */
			existingBitMap &= ~(1 << scopeId);
		}
		
		/* write it back to the map */
		membershipMap.put(compIdInteger, Integer.valueOf(existingBitMap));		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether the specified component is a member of this set. Given that the
	 * scope ID is not specified, a component is a member if any of its scopes are members.
	 * @param compId The component's ID.
	 * @return true if the component is a member, else false.
	 */
	public boolean isMember(int compId) {
		
		Object value = membershipMap.get(Integer.valueOf(compId));
		if (value == null) {
			/* component not register, return default setting */
			return defaultState;
		}
		
		/* 
		 * For the 1-parameter isMember function, we simply look at whether there are
		 * any scopes registered for this component.
		 */
		int scopeMap = (Integer)value;
		return (scopeMap != 0);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether the specified component/scope combination is a member of this set.
	 * @param compId The component's ID.
	 * @param scopeId The scope's ID.
	 * @return true if the component/scope is a member, else false.
	 */
	public boolean isMember(int compId, int scopeId) {
		
		Object value = membershipMap.get(Integer.valueOf(compId));
		if (value == null) {
			/* component not register, return default setting */
			return defaultState;
		}
		
		/* look within the bit-map to see if the scope's bit is set */
		int scopeMap = (Integer)value;
		return ((scopeMap & (1 << scopeId)) != 0);	
	}

	/*-------------------------------------------------------------------------------------*/
}
