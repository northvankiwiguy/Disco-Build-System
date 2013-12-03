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
 * within the FileMgr.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface IFileMgrListener {

	/** a new path (file or directory) has been added */
	public static final int NEW_PATH			= 1;

	/** a path (file or directory) has been removed */
	public static final int PATH_REMOVED	    = 2;
	
	/**
	 * Called to notify the listener that the specified file group has changed.
	 * 
	 * @param pathId 	The FileMgr ID of the path that has changed.
	 * @param how   	An indication of how the file has changed (see above).
	 */
	public void pathChangeNotification(int pathId, int how);
}
