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

package com.buildml.model;

/**
 * Any class that implements this interface may listen to changes that occur
 * within the ActionMgr.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface IActionMgrListener {
	
	/** one of the action's slots has changed */
	public static final int CHANGED_SLOT = 1;
	
	/** one of the actions has been trashed (or revived) */
	public static final int TRASHED_ACTION = 2;

	/**
	 * Called to notify the listener that the specified action has changed.
	 * 
	 * @param actionId  The ActionMgr ID of the action that has changed.
	 * @param how       An indication of how the action has changed (see above).
	 * @param changeId  Which thing has changed (CHANGED_SLOT).
	 */
	public void actionChangeNotification(int actionId, int how, int changeId);
}
