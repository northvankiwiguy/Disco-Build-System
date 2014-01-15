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

package com.buildml.model.undo;

import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.ISubPackageMgr;

/**
 * An undo/redo operation for any change that is made to a sub-package. This object records the
 * details of the changes necessary to the IBuildStore so they can be undone or redone later.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class SubPackageUndoOp implements IUndoOp {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/**
	 * What type of event does this operation represent?
	 */
	private final static int NEW_SUB_PACKAGE    = 1;
	private final static int MOVED_TO_TRASH     = 2;
	private final static int CHANGED_LOCATION   = 3;
		
	/** The code for this operation */
	private int opCode = 0;
	
	/** The ID of the sub-package being changed */
	private int subPkgId;

	/** Our IPackageMemberMgr for operating on */
	private IPackageMemberMgr pkgMemberMgr;

	/** Our ISubPackageMgr for operating on */
	private ISubPackageMgr subPkgMgr;
	
	/** if CHANGED_LOCATION, what is the old (x, y) location */
	private int oldX, oldY;
	
	/** if CHANGED_LOCATION, what is the new (x, y) location */
	private int newX, newY;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link SubPackageUndoOp} object, representing a single entry on the
	 * undo/redo stack.
	 * 
	 * @param buildStore  The IBuildStore we're performing the operation on.
	 * @param subPkgId    The sub-package ID of the sub-package being changed.
	 */
	public SubPackageUndoOp(IBuildStore buildStore, int subPkgId) {
		this.subPkgId = subPkgId;
		this.subPkgMgr = buildStore.getSubPackageMgr();
		this.pkgMemberMgr = buildStore.getPackageMemberMgr();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Record the creation of a new sub-package.
	 */
	public void recordNewSubPackage() {
		opCode = NEW_SUB_PACKAGE;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Record the removal of a sub-package.
	 */
	public void recordRemoveSubPackage() {
		opCode = MOVED_TO_TRASH;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Records the fact that the sub-package's pictogram (icon) has been moved to a new location
	 * on the diagram.
	 * @param oldX The existing x location
	 * @param oldY The existing y location
	 * @param newX The new x location (must be >= 0)
	 * @param newY The new y location (must be >= 0)
	 */
	public void recordLocationChange(int oldX, int oldY, int newX, int newY) {
		if ((oldX != newX) || (oldY != newY)) {
			opCode = CHANGED_LOCATION;
			this.oldX = oldX;
			this.oldY = oldY;
			this.newX = newX;
			this.newY = newY;
		}		
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.model.undo.IUndoOp#undo()
	 */
	@Override
	public boolean undo() {
		
		switch (opCode) {

		/* undo the "new package" operation by trashing it */
		case NEW_SUB_PACKAGE:
			subPkgMgr.moveSubPackageToTrash(subPkgId);
			break;
			
		/* undo the "remove package" operation by reviving it */
		case MOVED_TO_TRASH:
			subPkgMgr.reviveSubPackageFromTrash(subPkgId);
			break;
			
		/* if the file group's location needs to change... */
		case CHANGED_LOCATION:
			pkgMemberMgr.setMemberLocation(IPackageMemberMgr.TYPE_SUB_PACKAGE, subPkgId, oldX, oldY);
			break;
			
		default:
			return false;
		}
		
		/* yes, a change happened */
		return true;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.undo.IUndoOp#redo()
	 */
	@Override
	public boolean redo() {

		switch (opCode) {

		/* redo the "new package" operation by reviving it */
		case NEW_SUB_PACKAGE:
			subPkgMgr.reviveSubPackageFromTrash(subPkgId);
			break;
			
		/* redo the "remove package" operation by trashing it */
		case MOVED_TO_TRASH:
			subPkgMgr.moveSubPackageToTrash(subPkgId);
			break;
			
		/* if the file group's location needs to change... */
		case CHANGED_LOCATION:
			pkgMemberMgr.setMemberLocation(IPackageMemberMgr.TYPE_SUB_PACKAGE, subPkgId, newX, newY);
			break;
			
		default:
			return false;
		}
		
		/* yes, a change happened */
		return true;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
