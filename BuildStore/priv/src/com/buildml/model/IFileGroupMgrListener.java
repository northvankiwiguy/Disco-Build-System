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
 * within the FileGroupMgr.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface IFileGroupMgrListener {

	/** the file group membership has changed in some way */
	public static final int CHANGED_MEMBERSHIP = 1;

	/**
	 * Called to notify the listener that the specified file group has changed.
	 * 
	 * @param fileGroupId 	The FileGroupMgr ID of the file group that has changed.
	 * @param how   		An indication of how the file group has changed (see above).
	 */
	public void fileGroupChangeNotification(int fileGroupId, int how);
}
