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

package com.buildml.model.types;

import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IReportMgr;
import com.buildml.utils.errors.ErrorCode;
import com.buildml.utils.types.IntegerTreeSet;

/**
 * Implements an unordered set of action IDs. This is used in numerous places
 * where a collection of actions must be grouped together into a single unit.
 * 
 * The ActionSet data type is different from a regular set, since the parent/child
 * relationship between entries is maintained.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class ActionSet extends IntegerTreeSet {
	
	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/**
	 * The ActionMgr object that contains the actions referenced in this ActionSet.
	 */
	private IActionMgr actionMgr;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Creates a new ActionSet and initializes it to being empty.
	 * 
	 * @param actionMgr The ActionMgr object that owns the actions in this set.
	 */	
	public ActionSet(IActionMgr actionMgr) {
		
		/* most of the functionality is provided by the IntegerTreeSet class */
		super();
		
		/* except we also need to record our ActionMgr object */
		this.actionMgr = actionMgr;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Creates a new ActionSet and initializes it from an array of integer values.
	 * 
	 * @param actionMgr The ActionMgr object that owns the actions in this set.
	 * @param initValues The initial values to be added to the ActionSet.
	 */
	public ActionSet(IActionMgr actionMgr, Integer[] initValues) {

		/* most of the functionality is provided by the IntegerTreeSet class */
		super(initValues);
		
		/* except we also need to record our ActionMgr object */
		this.actionMgr = actionMgr;
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Given a action's ID, return the ID of the action's parent.
	 * 
	 *  @param id The ID of the action whose parent we're interested in.
	 *  @return The ID of the action's parent.
	 */
	@Override
	public int getParent(int id) {
		int parent = actionMgr.getParent(id);
		
		/* if we've reached the root, our parent is ourselves */
		if (parent == ErrorCode.NOT_FOUND) {
			return id;
		}
		
		return parent;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.utils.types.IntegerTreeSet#isValid(int)
	 */
	@Override
	public boolean isValid(int id) {
		return actionMgr.isActionValid(id) && !actionMgr.isActionTrashed(id);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a action's ID, return the array of children of the action.
	 * @param id The ID of the action whose children we wish to determine.
	 */
	@Override
	public Integer[] getChildren(int id) {
		return actionMgr.getChildren(id);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Merge the content of a second ActionSet into this ActionSet.
	 * 
	 * @param second The second ActionSet.
	 */
	public void mergeSet(ActionSet second) {
		
		/* ensure the ActionMgr is the same for both ActionSets */
		if (actionMgr != second.actionMgr) {
			return;
		}
		super.mergeSet(second);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given the String-formatted specification of a set of actions, populate this ActionSet
	 * with those action values. A action specification has the format: {[-]actionNum[/[depth]]}
	 * That is, we use the following syntax rules:
	 *  <ol>
	 *   <li>A action specification can have zero or more entries.</li>
	 *   <li>Each entry contains a mandatory action number, which will be added to the ActionSet
	 *     (or removed - see later).</li>
	 *   <li>The action number may be followed by [/depth] to indicate that all actions in the sub tree,
	 *     starting at the specified action and moving down the action tree "depth" level, should
	 *     be added (or removed).</li>
	 *   <li>If 'depth' is omitted (only the '/' is provided), all actions is the subtree are added
	 *     (regardless of their depth).</li>
	 *   <li>If the action number is prefixed by '-', the actions are removed from the ActionSet, rather
	 *     than being added.</li>
	 *   <li>The special syntax "%pkg/foo" means all actions in the package "foo".</li>
	 *   <li>The special syntax "%not-pkg/foo" means all actions outside the package "foo".</li>
	 *  </ol>
	 *  
	 * @param actionSpecs An array of command line arguments that specify which actions (or sub-trees
	 * of actions) should be added (or removed) from the action tree.
	 * @return ErrorCode.OK on success, or Error.BAD_VALUE if one of the action specifications
	 * is badly formed.
	 */
	public int populateWithActions(String actionSpecs[]) {
	
		IBuildStore bs = actionMgr.getBuildStore();
		IPackageMgr pkgMgr = bs.getPackageMgr();
		IPackageMemberMgr pkgMemberMgr = bs.getPackageMemberMgr();
		
		/* 
		 * Process each action spec in turn. They're mostly independent, although
		 * removing actions from the ActionSet requires that you've already added a larger
		 * set of actions from which actions can be subtracted from. The order is 
		 * therefore important.
		 */
		for (String actionSpec : actionSpecs) {
	
			/* only non-empty action specs are allowed */
			int tsLen = actionSpec.length();
			if (tsLen < 1) {
				return ErrorCode.BAD_VALUE;
			}
			
			/* check for commands that start with %, and end with / */
			if (actionSpec.startsWith("%")){
				
				/* 
				 * Figure out what the "name" is. It must be terminated by a '/',
				 * which is then followed by the command's argument(s).
				 */
				int slashIndex = actionSpec.indexOf('/');
				if (slashIndex == -1) { /* there must be a / */
					return ErrorCode.BAD_VALUE;
				}
				String commandName = actionSpec.substring(1, slashIndex);
				String commandArgs = actionSpec.substring(slashIndex + 1);
				
				if (commandName.equals("p") || commandName.equals("pkg")){
					ActionSet pkgActionSet = pkgMemberMgr.getActionsInPackage(commandArgs);
					if (pkgActionSet == null) {
						return ErrorCode.BAD_VALUE;
					}
					mergeSet(pkgActionSet);
				}
				else if (commandName.equals("np") || (commandName.equals("not-pkg"))){
					ActionSet pkgActionSet = pkgMemberMgr.getActionsOutsidePackage(commandArgs);
					if (pkgActionSet == null) {
						return ErrorCode.BAD_VALUE;
					}
					mergeSet(pkgActionSet);
				}
				
				/* try to match the action's command name */
				else if (commandName.equals("m") || commandName.equals("match")){
					IReportMgr reports = bs.getReportMgr();
					
					/* substitute * with %, and un-escape \\: to be : */
					commandArgs = commandArgs.replace('*', '%');
					commandArgs = commandArgs.replaceAll("\\\\:", ":");
					ActionSet matchingActionSet = reports.reportActionsThatMatchName(commandArgs);
					mergeSet(matchingActionSet);
				}
				
				/* else, the command isn't recognized */
				else {
					return ErrorCode.BAD_VALUE;
				}
			}
			
			else {
				/* 
				 * Parse the string. It'll be in the format: [-]NNNN[/[DD]]
				 * Does it start with an optional '-'? 
				 */
				int actionNumPos = 0;			    /* by default, action number is at start of string */
				boolean isAdditiveSpec = true;		/* by default, we're adding (not removing) actions */
				if (actionSpec.charAt(actionNumPos) == '-'){
					actionNumPos++;
					isAdditiveSpec = false;
				}
			
				/* is there a '/' character that separates the action number from the depth? */
				int slashIndex = actionSpec.indexOf('/', actionNumPos);

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
							depth = Integer.valueOf(actionSpec.substring(slashIndex + 1, tsLen));
						} catch (NumberFormatException ex) {
							return ErrorCode.BAD_VALUE;
						}
					}	
				} else {
					slashIndex = tsLen;
				}

				/* what is the action number? It's between 'actionNumPos' and 'slashIndex' */
				int actionNum;
				try {
					actionNum = Integer.valueOf(actionSpec.substring(actionNumPos, slashIndex));
				} catch (NumberFormatException ex) {
					return ErrorCode.BAD_VALUE;
				}

				/* populate this ActionSet, based on the actionNum and depth the user provided */
				populateWithActionsHelper(actionNum, depth, isAdditiveSpec);
			}
		}

		return ErrorCode.OK;
	}

	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/

	/**
	 * @return the maximum number of actions that can be represented in this ActionSet
	 * (numbered 0 to getMaxIdNumber() - 1).
	 */
	protected int getMaxIdNumber()
	{
		return IActionMgr.MAX_ACTIONS;
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * This is a helper method, to be used only by populateWithActions().
	 * 
	 * @param actionNum The action number to be added to the ActionSet.
	 * @param depth The number of tree levels to add. Use 1 to indicate that only this
	 * action should be added, 2 to indicate that this action and it's immediate children be
	 * added etc. The value -1 is used to indicate that all levels should be added.
	 * @param toBeAdded True if we should add the actions to the ActionSet, else false to
	 * remove them.
	 */
	private void populateWithActionsHelper(int actionNum, int depth, boolean toBeAdded) {
		
		/* we always add/remove the action itself */
		if (toBeAdded) {
			add(Integer.valueOf(actionNum));
		} else {
			remove(actionNum);
		}
		
		/* 
		 * And perhaps add/remove the children, if they're within the depth range, 
		 * or if there's no depth range specified (defaults to -1) 
		 */
		if ((depth > 1) || (depth == -1)) {
			Integer children [] = actionMgr.getChildren(actionNum);
			for (int i = 0; i < children.length; i++) {
				populateWithActionsHelper(children[i], (depth == -1) ? -1 : depth - 1, toBeAdded);
			}
		}
		
	}
	
	/*-------------------------------------------------------------------------------------*/
}
