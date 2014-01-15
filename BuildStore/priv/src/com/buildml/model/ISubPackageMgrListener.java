/*******************************************************************************
 * Copyright (c) 2014 Arapiki Solutions Inc.
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
 * within the SubPackageMgr.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface ISubPackageMgrListener {
	
	/** one of the sub-package's slots has changed */
	public static final int CHANGED_SLOT = 1;
	
	/** one of the sub-packages has been trashed (or revived) */
	public static final int TRASHED_SUB_PACKAGE = 2;

	/**
	 * Called to notify the listener that the specified sub-package has changed.
	 * 
	 * @param subPkgId  The SubPackageMgr ID of the sub-package that has changed.
	 * @param how       An indication of how the sub-package has changed (see above).
	 * @param changeId  Which thing has changed (CHANGED_SLOT).
	 */
	public void subPackageChangeNotification(int subPkgId, int how, int changeId);
}
