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

import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.utils.errors.ErrorCode;

/**
 * An undo/redo operation for any change that is made to a path. This object records the
 * details of the changes necessary to the IBuildStore so they can be undone or redone later.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class FileUndoOp implements IUndoOp {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/**
	 * Bitmap of all the parts of the path that have changed, and will need to be changed
	 * back on an undo/redo operation.
	 */
	private final static int REMOVE_PATH    	= 1;
	private final static int CHANGE_PACKAGE		= 2;
	
	/** The IBuildStore we're operating on */
	private IBuildStore buildStore;
	
	/** The fields of this operation that have changed - see above bitmap */
	private int changedFields = 0;
	
	/** The ID of the path being changed */
	private int pathId;
	
	/** For CHANGE_PACKAGE, what is the current package ID */
	private int oldPkgId;
	
	/** For CHANGE_PACKAGE, what is the new package ID */
	private int newPkgId;

	/** For CHANGE_PACKAGE, what is the current scope ID */
	private int oldScopeId;
	
	/** For CHANGE_PACKAGE, what is the new scope ID */
	private int newScopeId;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link FileUndoOp} object, representing a single entry on the
	 * undo/redo stack.
	 * 
	 * @param buildStore  The IBuildStore we're performing the operation on.
	 * @param pathId      The fileMgr ID of the path being changed.
	 */
	public FileUndoOp(IBuildStore buildStore, int pathId) {		
		this.buildStore = buildStore;
		this.pathId = pathId;
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Record the fact that the path is being trashed.
	 */
	public void recordRemovePath() {
		changedFields |= REMOVE_PATH;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Record the fact that a path has moved into a different package.
	 * 
	 * @param oldPkgId		ID of the path's current package.
	 * @param oldScopeId	ID of the path's current scope.
	 * @param newPkgId		ID of the path's new package.
	 * @param newScopeId	ID of the path's new scope.
	 */
	public void recordChangePackage(int oldPkgId, int oldScopeId, int newPkgId, int newScopeId) {
		if ((oldPkgId == newPkgId) && (oldScopeId == newScopeId)) {
			return;
		}
		changedFields |= CHANGE_PACKAGE;
		this.oldPkgId = oldPkgId;
		this.newPkgId = newPkgId;
		this.oldScopeId = oldScopeId;
		this.newScopeId = newScopeId;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.undo.IUndoOp.undo()
	 */
	@Override
	public boolean undo() {
		IFileMgr fileMgr = buildStore.getFileMgr();
		IPackageMemberMgr pkgMemberMgr = buildStore.getPackageMemberMgr();
		
		/* undo removal of a path */
		if ((changedFields & REMOVE_PATH) != 0) {
			if (fileMgr.revivePathFromTrash(pathId) != ErrorCode.OK) {
				throw new FatalBuildStoreError(
						"Unable to revive file from trash: " + fileMgr.getPathName(pathId));
			}
		}

		/* undo the change of package/scope */
		if ((changedFields & CHANGE_PACKAGE) != 0) {
			pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE, pathId, oldPkgId, oldScopeId);
		}
		
		return (changedFields != 0);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.undo.IUndoOp.redo()
	 */
	@Override
	public boolean redo() {
		IFileMgr fileMgr = buildStore.getFileMgr();
		IPackageMemberMgr pkgMemberMgr = buildStore.getPackageMemberMgr();

		/* do/redo removal of a path */
		if ((changedFields & REMOVE_PATH) != 0) {
			if (fileMgr.movePathToTrash(pathId) != ErrorCode.OK) {
				throw new FatalBuildStoreError(
						"Unable to move file to trash: " + fileMgr.getPathName(pathId));
			}
		}

		/* do/redo the change of package/scope */
		if ((changedFields & CHANGE_PACKAGE) != 0) {
			pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE, pathId, newPkgId, newScopeId);
		}

		/* if there's a change, mark the editor as dirty */
		return (changedFields != 0);
	}

	/*-------------------------------------------------------------------------------------*/
}
