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

import com.arapiki.utils.errors.ErrorCode;
import com.arapiki.utils.types.IntegerTreeSet;

/**
 * Implements an unordered set of TaskRecord objects. This is used as the return
 * value from any method in the Reports class where the order of the returned values
 * is irrelevant.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TaskSet extends IntegerTreeSet<TaskRecord>{
	
	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/**
	 * The BuildTasks object that contains the tasks referenced in this TaskSet
	 * object.
	 */
	private BuildTasks bts;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Constructor - creates a new TaskSet and initializes it to being empty.
	 */	
	public TaskSet(BuildTasks bts) {
		
		/* most of the functionality is provided by the IntegerTreeSet class */
		super();
		
		/* except we also need to record our BuildTasks object */
		this.bts = bts;
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.arapiki.utils.types.IntegerTreeSet#getParent(int)
	 */
	@Override
	public int getParent(int id) {
		int parent = bts.getParent(id);
		
		/* if we've reached the root, our parent is ourselves */
		if (parent == ErrorCode.NOT_FOUND) {
			return id;
		}
		
		return parent;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.arapiki.utils.types.IntegerTreeSet#newRecord(int)
	 */
	@Override
	public TaskRecord newRecord(int id) {
		return new TaskRecord(id);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * (non-Javadoc)
	 * @see com.arapiki.utils.types.IntegerTreeSet#mergeSet()
	 */
	public void mergeSet(TaskSet second) {
		
		/* ensure the BuildTasks are the same for both TaskSets */
		if (bts != second.bts) {
			return;
		}
		super.mergeSet(second);
	}
	
	/*-------------------------------------------------------------------------------------*/
}
