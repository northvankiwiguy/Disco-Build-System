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
	 * @param bts The BuildTasks object that contains the tasks in this set
	 */	
	public TaskSet(BuildTasks bts) {
		
		/* most of the functionality is provided by the IntegerTreeSet class */
		super();
		
		/* except we also need to record our BuildTasks object */
		this.bts = bts;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Constructor - creates a new TaskSet and initializes it from an array of integer values.
	 * @param bts The BuildTasks object that contains the tasks in this set
	 * @param initValues The initial values to be added to the task set
	 */
	public TaskSet(BuildTasks bts, Integer[] initValues) {

		/* most of the functionality is provided by the IntegerTreeSet class */
		super();
		
		/* except we also need to record our BuildTasks object */
		this.bts = bts;
		
		/* copy the initial task values from the array into the TaskSet */
		for (int i = 0; i < initValues.length; i++) {
			add(new TaskRecord(initValues[i]));
		}
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

	/**
	 * Given the String-formatted specification of a set of tasks, populate this TaskSet
	 * with those task values. A task specification has the format: {[-]taskNum[/[depth]]}
	 * That is, we use the following syntax rules:
	 *   - A task specification can have zero or more entries
	 *   - Each entry contains a mandatory task number, which will be added to the TaskSet
	 *     (or removed - see later).
	 *   - The task number may be followed by [/depth] to indicate that all tasks in the sub tree,
	 *     starting at the specified task and moving down the task tree "depth" level, should
	 *     be added (or removed)
	 *   - If 'depth' is omitted (only the '/' is provided), all tasks is the subtree are added
	 *     (regardless of their depth).
	 *   - If the task number is prefixed by '-', the tasks are removed from the TaskSet, rather
	 *     than being added.
	 * @param taskSpecs[] An array of command line arguments that specify which tasks (or sub-trees
	 * of tasks) should be added (or removed) from the task tree.
	 * @return ErrorCode.OK on success, or Error.BAD_VALUE if one of the task specifications
	 * is badly formed.
	 */
	public int populateWithTasks(String taskSpecs[]) {
	
		/* 
		 * Process each task spec in turn. They're mostly independent, although
		 * removing tasks from the TaskSet requires that you've already added a larger
		 * set of tasks from which tasks can be subtracted from. The order is 
		 * therefore important.
		 */
		for (String taskSpec : taskSpecs) {
	
			/* only non-empty task specs are allowed */
			int tsLen = taskSpec.length();
			if (tsLen < 1) {
				return ErrorCode.BAD_VALUE;
			}
			
			/* 
			 * Parse the string. It'll be in the format: [-]NNNN[/[DD]]
			 * Does it start with an optional '-'? 
			 */
			int taskNumPos = 0;					/* by default, task number is at start of string */
			boolean isAdditiveSpec = true;		/* by default, we're adding (not removing) tasks */
			if (taskSpec.charAt(taskNumPos) == '-'){
				taskNumPos++;
				isAdditiveSpec = false;
			}
	
			/* is there a '/' character that separates the task number from the depth? */
			int slashIndex = taskSpec.indexOf('/', taskNumPos);
	
			/* yes, there's a /, so we care about the depth (otherwise we'd default to depth = 1 */
			int depth = 1;
			if (slashIndex != -1) {
				
				/* if there's no number after the '/', the depth is -1 (infinite) */
				if (slashIndex + 1 == tsLen) {
					depth = -1;
				} 
				
				/* else, the number after the / is the depth */
				else {
					try {
						depth = Integer.valueOf(taskSpec.substring(slashIndex + 1, tsLen));
					} catch (NumberFormatException ex) {
						return ErrorCode.BAD_VALUE;
					}
				}	
			} else {
				slashIndex = tsLen;
			}
			
			/* what is the task number? It's between 'taskNumPos' and 'slashIndex' */
			int taskNum;
			try {
				taskNum = Integer.valueOf(taskSpec.substring(taskNumPos, slashIndex));
			} catch (NumberFormatException ex) {
				return ErrorCode.BAD_VALUE;
			}
			
			/* populate this TaskSet, based on the taskNum and depth the user provided */
			populateWithTasksHelper(taskNum, depth, isAdditiveSpec);
		}
		
		return ErrorCode.OK;
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * This is a helper method, to be used only by populateWithTasks.
	 * @param taskNum The task number to be added to the TaskSet.
	 * @param depth The number of tree levels to add. Use 1 to indicate that only this
	 * task should be added, 2 to indicate that this task and it's immediate children be
	 * added etc. The value -1 is used to indicate that all levels should be added.
	 * @param toBeAdded True if we should add the tasks to the TaskSet, else false to
	 * remove them.
	 */
	private void populateWithTasksHelper(int taskNum, int depth, boolean toBeAdded) {
		
		/* we always add/remove the task itself */
		if (toBeAdded) {
			add(new TaskRecord(taskNum));
		} else {
			remove(taskNum);
		}
		
		/* 
		 * And perhaps add/remove the children, if they're within the depth range, 
		 * or if there's no depth range specified (defaults to -1) 
		 */
		if ((depth > 1) || (depth == -1)) {
			Integer children [] = bts.getChildren(taskNum);
			for (int i = 0; i < children.length; i++) {
				populateWithTasksHelper(children[i], (depth == -1) ? -1 : depth - 1, toBeAdded);
			}
		}
		
	}
	
	/*-------------------------------------------------------------------------------------*/

}
