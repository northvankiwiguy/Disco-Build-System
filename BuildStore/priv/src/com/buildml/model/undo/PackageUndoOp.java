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

package com.buildml.model.undo;

import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageRootMgr;

/**
 * An undo/redo operation for any change that is made to a package. This object records the
 * details of the changes necessary to the IBuildStore so they can be undone or redone later.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class PackageUndoOp implements IUndoOp {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/**
	 * Bitmap of all the parts of the package that have changed, and will need to be changed
	 * back on an undo/redo operation.
	 */
	private final static int NEW_PACKAGE    	= 1;
	private final static int NEW_FOLDER	    	= 2;
	private final static int REMOVE_PACKAGE   	= 4;
	private final static int REMOVE_FOLDER   	= 8;
	private final static int RENAME		    	= 16;
	private final static int MOVE    			= 32;
	private final static int CHANGE_ROOTS    	= 64;
	
	/** The fields of this operation that have changed - see above bitmap */
	private int changedFields = 0;
	
	/** The ID of the package being changed */
	private int pkgId;
	
	/** For RENAME, REMOVE_PACKAGE and REMOVE_FOLDER, what is the old name */
	private String oldName;
	
	/** For RENAME, NEW_PACKAGE and NEW_FOLDER, what is the new name */
	private String newName;
	
	/** For MOVE, the old parent ID */
	private int oldParentId;
	
	/** For MOVE, NEW_PACKAGE, NEW_FOLDER, REMOVE_PACKAGE, REMOVE_FOLDER, the new parent ID */
	private int newParentId;
	
	/** For CHANGE_ROOTS, the source path root, before the change */
	private int oldSrcRootPathId;
	
	/** For CHANGE_ROOTS, The generated path root, before the change */
	private int oldGenRootPathId;
	
	/** For CHANGE_ROOTS, the source path root, after the change */
	private int newSrcRootPathId;
	
	/** For CHANGE_ROOTS, the generated path root, after the change */
	private int newGenRootPathId;

	/** Our IPackageMgr for operating on */
	private IPackageMgr pkgMgr;

	/** Our IPackageRootMgr for operating on */
	private IPackageRootMgr pkgRootMgr;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link PackageUndoOp} object, representing a single entry on the
	 * undo/redo stack.
	 * 
	 * @param buildStore  The IBuildStore we're performing the operation on.
	 * @param pkgId       The packageMgr ID of the package being changed.
	 */
	public PackageUndoOp(IBuildStore buildStore, int pkgId) {
		this.pkgId = pkgId;
		this.pkgMgr = buildStore.getPackageMgr();
		this.pkgRootMgr = buildStore.getPackageRootMgr();
		this.changedFields = 0;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Record the creation of a new package.
	 * 
	 * @param newName		The name of the package.
	 * @param parentId		The parent folder's ID.
	 */
	public void recordNewPackage(String newName, int parentId) {
		changedFields |= NEW_PACKAGE;
		this.newName = newName;
		this.newParentId = parentId;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Record the creation of a new folder.
	 * 
	 * @param newName	The name of the folder.
	 * @param parentId	The parent folder's ID.
	 */
	public void recordNewFolder(String newName, int parentId) {
		changedFields |= NEW_FOLDER;
		this.newName = newName;
		this.newParentId = parentId;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Record the removal of this package.
	 * 
	 * @param oldName 		The package's current name.
	 * @param oldParentId 	The package's current parent ID.
	 */
	public void recordRemovePackage(String oldName, int oldParentId) {
		changedFields |= REMOVE_PACKAGE;
		this.oldName = oldName;
		this.oldParentId = oldParentId;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Record the removal of this folder.
	 * 
	 * @param oldName 		The folder's current name.
	 * @param oldParentId 	The folder's current parent ID.
	 */
	public void recordRemoveFolder(String oldName, int oldParentId) {
		changedFields |= REMOVE_FOLDER;
		this.oldName = oldName;
		this.oldParentId = oldParentId;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Record the package/folder's name change.
	 * 
	 * @param oldName	The current name for the package/folder.
	 * @param newName	The new name for the package/folder.
	 */
	public void recordRename(String oldName, String newName) {
		if (oldName.equals(newName)) {
			return;
		}
		changedFields |= RENAME;
		this.newName = newName;
		this.oldName = oldName;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Record the package/folder's new parent.
	 * 
	 * @param oldParentId	The package/folder's old parent ID.
	 * @param newParentId	The package/folder's new parent ID.
	 */
	public void recordMove(int oldParentId, int newParentId) {
		if (oldParentId == newParentId) {
			return;
		}
		changedFields |= MOVE;
		this.oldParentId = oldParentId;
		this.newParentId = newParentId;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Record a change in the package's roots.
	 *
	 * @param oldSrcRootPathId		The existing source root path ID.
	 * @param oldGenRootPathId		The existing generated root path ID.
	 * @param newSrcRootPathId		The new source root path ID.
	 * @param newGenRootPathId		The new generated root path ID.
	 */
	public void recordRootChange(
			int oldSrcRootPathId, int oldGenRootPathId,
			int newSrcRootPathId, int newGenRootPathId) {

		if ((oldSrcRootPathId == newSrcRootPathId) && (oldGenRootPathId == newGenRootPathId)) {
			return;
		}
		changedFields |= CHANGE_ROOTS;
		this.oldSrcRootPathId = oldSrcRootPathId;
		this.oldGenRootPathId = oldGenRootPathId;
		this.newSrcRootPathId = newSrcRootPathId;
		this.newGenRootPathId = newGenRootPathId;
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.model.undo.IUndoOp#undo()
	 */
	@Override
	public boolean undo() {
		
		/* undo the NEW_PACKAGE command */
		if ((changedFields & NEW_PACKAGE) != 0) {
			pkgMgr.remove(pkgId);
		}

		/* undo the NEW_FOLDER command */
		if ((changedFields & NEW_FOLDER) != 0) {
			pkgMgr.remove(pkgId);
		}

		/* undo the RENAME command */
		if ((changedFields & RENAME) != 0) {	
			pkgMgr.setName(pkgId, oldName);
		}
		
		/* undo the REMOVE_PACKAGE command */
		if ((changedFields & REMOVE_PACKAGE) != 0) {
			pkgId = pkgMgr.addPackage(oldName);				
			if (pkgId >= 0) {
				pkgMgr.setParent(pkgId, oldParentId);
			}
		}

		/* undo the REMOVE_FOLDER command */
		if ((changedFields & REMOVE_FOLDER) != 0) {	
			pkgId = pkgMgr.addFolder(oldName);				
			if (pkgId >= 0) {
				pkgMgr.setParent(pkgId, oldParentId);
			}
		}

		/* undo the MOVE command */
		if ((changedFields & MOVE) != 0) {	
			pkgMgr.setParent(pkgId, oldParentId);
		}

		/* undo the CHANGE_ROOTS command */
		if ((changedFields & CHANGE_ROOTS) != 0) {	
			pkgRootMgr.setPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT, oldSrcRootPathId);
			pkgRootMgr.setPackageRoot(pkgId, IPackageRootMgr.GENERATED_ROOT, oldGenRootPathId);			
		}

		return (changedFields != 0);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.undo.IUndoOp#redo()
	 */
	@Override
	public boolean redo() {

		/* do/redo the NEW_PACKAGE command */
		if ((changedFields & NEW_PACKAGE) != 0) {
			pkgId = pkgMgr.addPackage(newName);
			if (pkgId >= 0) {
				pkgMgr.setParent(pkgId, newParentId);
			}
		}

		/* do/redo the NEW_FOLDER command */
		if ((changedFields & NEW_FOLDER) != 0) {
			pkgId = pkgMgr.addFolder(newName);
			if (pkgId >= 0) {
				pkgMgr.setParent(pkgId, newParentId);
			}
		}

		/* do/redo the RENAME command */
		if ((changedFields & RENAME) != 0) {	
			pkgMgr.setName(pkgId, newName);
		}
		
		/* do/redo the REMOVE_PACKAGE command */
		if ((changedFields & REMOVE_PACKAGE) != 0) {	
			pkgMgr.remove(pkgId);
		}

		/* do/redo the REMOVE_FOLDER command */
		if ((changedFields & REMOVE_FOLDER) != 0) {	
			pkgMgr.remove(pkgId);
		}

		/* do/redo the MOVE command */
		if ((changedFields & MOVE) != 0) {	
			pkgMgr.setParent(pkgId, newParentId);
		}

		/* do/redo the CHANGE_ROOTS command */
		if ((changedFields & CHANGE_ROOTS) != 0) {	
			pkgRootMgr.setPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT, newSrcRootPathId);
			pkgRootMgr.setPackageRoot(pkgId, IPackageRootMgr.GENERATED_ROOT, newGenRootPathId);			
		}

		return (changedFields != 0);
	}
	
	/*-------------------------------------------------------------------------------------*/
}
