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
public interface IPackageMemberMgrListener {

	/** the package membership has changed in some way */
	public static final int CHANGED_MEMBERSHIP = 1;
	
	/** a member's location has changed  */
	public static final int CHANGED_LOCATION = 2;
	
	/**
	 * Called to notify the listener that the specified package has changed.
	 * 
	 * @param pkgId 		The PackageMgr ID of the package that has changed.
	 * @param how   		An indication of how the package has changed (see above).
	 * @param memberType	The type of the member that has changed (e.g. TYPE_ACTION)
	 * @param memberId		The ID of the member (e.g. actionId or fileGroupId).
	 */
	public void packageMemberChangeNotification(int pkgId, int how, int memberType, int memberId);
}
