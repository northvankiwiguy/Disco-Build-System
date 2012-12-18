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

package com.buildml.model.impl;

import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageRootMgr;

/**
 * A {@link PackageRootMgr} object manages the mapping o
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class PackageRootMgr implements IPackageRootMgr {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The IBuildStore object that owns this manager */
	private IBuildStore buildStore;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new PackageRootMgr.
	 * 
	 * @param buildStore The BuildStore that this PackageRootMgr object belongs to.
	 */
	public PackageRootMgr(IBuildStore buildStore) {
		this.buildStore = buildStore;
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#setRelativeWorkspace(int)
	 */
	@Override
	public int setRelativeWorkspace(int distance) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#getRelativeWorkspace()
	 */
	@Override
	public int getRelativeWorkspace() {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#setPackageRoot(int, int, java.lang.String)
	 */
	@Override
	public int setPackageRoot(int packageId, int type, String pathId) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#overridePackageRoot(int, int, java.lang.String)
	 */
	@Override
	public int setRootMapping(int packageId, int type, String path) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#clearPackageRoot(int, int)
	 */
	@Override
	public int clearRootMapping(int packageId, int type) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#getPackageRoot(int, int)
	 */
	@Override
	public String getRootNativePath(int packageId, int type) {
		// TODO Auto-generated method stub
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#getPackageRoot(java.lang.String)
	 */
	@Override
	public String getRootNativePath(String rootName) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#getRoots()
	 */
	@Override
	public String[] getRoots() {
		// TODO Auto-generated method stub
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IPackageRootMgr#getRootAtPath(int)
	 */
	@Override
	public String getRootAtPath(int pathId) {
		// TODO Auto-generated method stub
		return null;
	}

	/*-------------------------------------------------------------------------------------*/
}
