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
		
	/** The code for this operation */
	private int opCode = 0;
	
	/** The ID of the sub-package being changed */
	private int subPkgId;

	/** Our ISubPackageMgr for operating on */
	private ISubPackageMgr subPkgMgr;

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
			
		default:
			return false;
		}
		
		/* yes, a change happened */
		return true;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
