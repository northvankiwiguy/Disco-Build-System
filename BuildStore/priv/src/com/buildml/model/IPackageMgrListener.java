/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
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
 * within the PackageMgr.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface IPackageMgrListener {

	/** value of "how" if the package name has changed */
	public static final int CHANGED_NAME = 1;

	/** value of "how" if the package roots have changed */
	public static final int CHANGED_ROOTS = 2;

	/**
	 * Called to notify the listener that the specified package has changed.
	 * 
	 * @param pkgId The PackageMgr ID of the package that has changed.
	 * @param how   An indication of how the package has changed (see above).
	 */
	public void packageChangeNotification(int pkgId, int how);
}
