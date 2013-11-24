/*******************************************************************************
 * Copyright (c) 2013 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.packages.properties.filegroup;

/**
 * A TreeMember is a business object representing a node in the tree of file group members.
 * This is a fairly complex structure, but it's important to uniquely identify each tree
 * member, especially since members (files, sub groups) may appear multiple times - they
 * each need to be individually selectable and can't have the same underlying business object.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
/* package */ class TreeMember {
	
	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/** The nesting depth of this member (0 = top level) */
	int level;
	
	/** The position within the list (0 = first entry) */
	int seq;
	
	/** 
	 * The underlying database ID of the member (level == 0) or parent's seq (level == 1).
	 */
	int id;
	
	/** The textual string associated with this member (level == 1) */
	String text;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new TreeMember object.
	 * 
	 * @param level		The level of this member within the tree (0 = top level) 
	 * @param seq		The position within the tree: 0, 1, 2, 3, 4, etc.
	 * @param id		The underlying database ID (for the group, or for the file).
	 * @param text		The text String associated with this TreeMember.
	 */
	public TreeMember(int level, int seq, int id, String text) {
		this.level = level;
		this.seq = seq;
		this.id = id;
		this.text = text;
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * The standard equals implementation for this class.
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TreeMember)) {
			return false;
		}
		TreeMember other = (TreeMember)obj;
		if ((this.level != other.level) || (this.seq != other.seq) || (this.id != other.id)) {
			return false;
		}
		if ((this.text == null) && (other.text == null)) {
			return true;
		}
		return this.text.equals(other.text);
	}
	
	/*-------------------------------------------------------------------------------------*/
}
