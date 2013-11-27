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

import com.buildml.model.IActionMgr;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.IFileMgr;
import com.buildml.model.impl.FileMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMemberMgr;

/**
 * An undo/redo operation for any change that is made to an Action. This object records the
 * details of the changes necessary to the IBuildStore so they can be undone or redone later.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class ActionUndoOp implements IUndoOp {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/**
	 * Bitmap of all the parts of the action that have changed, and will need to be changed
	 * back on an undo/redo operation.
	 */
	private final static int CHANGED_PACKAGE      = 1;
	private final static int CHANGED_COMMAND      = 2;
	private final static int CHANGED_LOCATION     = 4;
	private final static int CHANGED_SLOT         = 8;
	private final static int REMOVED_SLOT         = 16;
	private final static int MOVED_TO_TRASH       = 32;
	private final static int NEW_ACTION           = 64;
	private final static int CHANGED_PARENT       = 128;
	private final static int ADD_PATH_ACCESS      = 256;
	private final static int REMOVE_PATH_ACCESS   = 512;
	
	/** The IBuildStore we're operating on */
	private IBuildStore buildStore;

	/** The ID of the action being changed */
	private int actionId;
	
	/** The fields of this operation that have changed - see above bitmap */
	private int changedFields = 0;
	
	/** if CHANGED_PACKAGE set, what is the original package ID? */
	private int oldPackage;

	/** if CHANGED_PACKAGE set, what is the new package ID? */
	private int newPackage;
	
	/** if CHANGED_COMMAND set, what is the original command string? */
	private String oldCommand;

	/** if CHANGED_COMMAND set, what is the new command string? */
	private String newCommand;
	
	/** if CHANGED_LOCATION, what is the old (x, y) location */
	private int oldX, oldY;
	
	/** if CHANGED_LOCATION, what is the new (x, y) location */
	private int newX, newY;
	
	/** if CHANGED_SLOT | REMOVED_SLOT, what is the ID of the slot that's changing */
	private int slotId;
	
	/** if CHANGED_SLOT | REMOVED_SLOT, what is the old value in the slot */
	private Object oldSlotValue;

	/** if CHANGED_SLOT, what is the new value in the slot */
	private Object newSlotValue;
	
	/** if CHANGED_PARENT, the old parent ID */
	private int oldParentId;
	
	/** if CHANGED_PARENT, the new parent ID */
	private int newParentId;
	
	/** if ADD_PATH_ACCESS | REMOVE_PATH_ACCESS what is the sequence number? */
	private int accessSeqno;
	
	/** If ADD_PATH_ACCESS | REMOVE_PATH_ACCESS what is the ID of the path being accessed? */
	private int accessPathId;
	
	/** If ADD_PATH_ACCESS | REMOVE_PATH_ACCESS what is the type of access? */
	private OperationType accessOpType;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link ActionUndoOp} object, representing a single entry on the
	 * undo/redo stack.
	 * 
     * @param buildStore The IBuildStore we're operating on.
   	 * @param actionId   The actionMgr ID of the action being changed.
	 */
	public ActionUndoOp(IBuildStore buildStore, int actionId) {	
		this.buildStore = buildStore;
		this.actionId = actionId;
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Records the fact that the action's package has changed. If there is no change in the
	 * packageId, this method does nothing.
	 * 
	 * @param prevPackageId The action's current package ID.
	 * @param nextPackageId The action's future package ID.
	 */
	public void recordPackageChange(int prevPackageId, int nextPackageId) {
		if (prevPackageId != nextPackageId) {
			changedFields |= CHANGED_PACKAGE;
			oldPackage = prevPackageId;
			newPackage = nextPackageId;
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Records the fact that the action's shell command has changed. If there is no change,
	 * this method does nothing.
	 * 
	 * @param oldCommandString The previous command string for this action.
	 * @param newCommandString The new command string for this action.
	 */
	public void recordCommandChange(String oldCommandString, String newCommandString) {
		if (!oldCommandString.equals(newCommandString)){
			changedFields |= CHANGED_COMMAND;
			oldCommand = oldCommandString;
			newCommand = newCommandString;
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Records the fact that the action's pictogram (icon) has been moved to a new location
	 * on the diagram.
	 * @param oldX The existing x location
	 * @param oldY The existing y location
	 * @param newX The new x location (must be >= 0)
	 * @param newY The new y location (must be >= 0)
	 */
	public void recordLocationChange(int oldX, int oldY, int newX, int newY) {
		if ((oldX != newX) || (oldY != newY)) {
			changedFields |= CHANGED_LOCATION;
			this.oldX = oldX;
			this.oldY = oldY;
			this.newX = newX;
			this.newY = newY;
		}		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Records the fact that a file group has been inserted into an action's slot.
	 * 
	 * @param slotId   The slot being modified.
	 * @param oldValue The current value in the slot.
	 * @param newValue The new value in the slot.
	 */
	public void recordSlotChange(int slotId, Object oldValue, Object newValue) {
		if (((oldValue == null) && (newValue != null)) ||
			(!oldValue.equals(newValue))) {
			changedFields |= CHANGED_SLOT;
			this.slotId = slotId;
			this.oldSlotValue = oldValue;
			this.newSlotValue = newValue;
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Records the fact that a slot value has been deleted (will revert to default value).
	 * 
	 * @param slotId   The slot being deleted.
	 * @param oldValue The current value in the slot.
	 */
	public void recordSlotRemove(int slotId, Object oldValue) {
		changedFields |= REMOVED_SLOT;
		this.slotId = slotId;
		this.oldSlotValue = oldValue;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Records the fact that this action has been deleted
	 */
	public void recordMoveToTrash() {
		changedFields |= MOVED_TO_TRASH;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Records the fact that this action has been deleted
	 */
	public void recordNewAction() {
		changedFields |= NEW_ACTION;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Records a change to an action's parent.
	 * @param oldParentId	The action's current parent ID.
	 * @param newParentId	The action's new parent ID.
	 */
	public void recordParentChange(int oldParentId, int newParentId) {
		if (oldParentId != newParentId) {
			changedFields |= CHANGED_PARENT;
			this.oldParentId = oldParentId;
			this.newParentId = newParentId;
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Record the removal of a file access for this action.
	 * 
	 * @param seqno		The sequence number of this access (to retain ordering).
	 * @param pathId	The path being accessed.
	 * @param opType	The type of access (OP_READ, OP_WRITE, etc).
	 */
	public void recordRemovePathAccess(int seqno, int pathId, OperationType opType) {
		changedFields |= REMOVE_PATH_ACCESS;
		this.accessSeqno = seqno;
		this.accessPathId = pathId;
		this.accessOpType = opType;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Record the addition of a file access for this action.
	 * 
	 * @param seqno		The sequence number of this access (to retain ordering).
	 * @param pathId	The path being accessed.
	 * @param opType	The type of access (OP_READ, OP_WRITE, etc).
	 */
	public void recordAddPathAccess(int seqno, int pathId, OperationType opType) {
		changedFields |= ADD_PATH_ACCESS;
		this.accessSeqno = seqno;
		this.accessPathId = pathId;
		this.accessOpType = opType;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.utils.BmlAbstractOperation#undo()
	 */
	@Override
	public boolean undo() {
		IPackageMemberMgr pkgMemberMgr = buildStore.getPackageMemberMgr();
		IActionMgr actionMgr = buildStore.getActionMgr();
		
		/* if the action's package needs to change... */
		if ((changedFields & CHANGED_PACKAGE) != 0) {
			pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_ACTION, actionId, oldPackage);
		}

		/* if the action's command needs to change... */
		if ((changedFields & CHANGED_COMMAND) != 0) {
			actionMgr.setCommand(actionId, oldCommand);			
		}

		/* if the action's location needs to change... */
		if ((changedFields & CHANGED_LOCATION) != 0){
			pkgMemberMgr.setMemberLocation(IPackageMemberMgr.TYPE_ACTION, actionId, oldX, oldY);
		}
		
		/* if one of the action's slots needs to change... */
		if ((changedFields & (CHANGED_SLOT | REMOVED_SLOT)) != 0){
			actionMgr.setSlotValue(actionId, slotId, oldSlotValue);
		}
		
		/* if the action has been moved to the trash... */
		if ((changedFields & MOVED_TO_TRASH) != 0) {
			actionMgr.reviveActionFromTrash(actionId);
		}
		
		/* if the action has been newly created */
		if ((changedFields & NEW_ACTION) != 0) {
			actionMgr.moveActionToTrash(actionId);
		}
		
		/* if the action's parent has changed */
		if ((changedFields & CHANGED_PARENT) != 0) {
			actionMgr.setParent(actionId, oldParentId);
		}
		
		/* if this action is now accessing a new path */
		if ((changedFields & ADD_PATH_ACCESS) != 0) {
			actionMgr.removeFileAccess(actionId, accessPathId);
		}

		/* if this action is no longer accessing a path */
		if ((changedFields & REMOVE_PATH_ACCESS) != 0) {
			actionMgr.addSequencedFileAccess(accessSeqno, actionId, accessPathId, accessOpType);
		}
		
		return (changedFields != 0);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.utils.BmlAbstractOperation#redo()
	 */
	@Override
	public boolean redo() {
		IPackageMemberMgr pkgMemberMgr = buildStore.getPackageMemberMgr();
		IActionMgr actionMgr = buildStore.getActionMgr();

		/* if the action's package needs to change... */
		if ((changedFields & CHANGED_PACKAGE) != 0) {
			pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_ACTION, actionId, newPackage);
		}
		
		/* if the action's command needs to change... */
		if ((changedFields & CHANGED_COMMAND) != 0) {
			actionMgr.setCommand(actionId, newCommand);
		}

		/* if the action's location needs to change... */
		if ((changedFields & CHANGED_LOCATION) != 0){
			pkgMemberMgr.setMemberLocation(IPackageMemberMgr.TYPE_ACTION, actionId, newX, newY);
		}
		
		/* if one of the action's slots needs to change... */
		if ((changedFields & CHANGED_SLOT) != 0){
			actionMgr.setSlotValue(actionId, slotId, newSlotValue);
		}
		
		/* if one of the action's slots needs to be deleted... */
		if ((changedFields & REMOVED_SLOT) != 0){
			actionMgr.clearSlotValue(actionId, slotId);
		}
		
		/* if the action has been moved to the trash... */
		if ((changedFields & MOVED_TO_TRASH) != 0) {
			actionMgr.moveActionToTrash(actionId);
		}
		
		/* if the action has been newly created */
		if ((changedFields & NEW_ACTION) != 0) {
			actionMgr.reviveActionFromTrash(actionId);
		}
		
		/* if the action's parent has changed */
		if ((changedFields & CHANGED_PARENT) != 0) {
			actionMgr.setParent(actionId, newParentId);
		}

		/* if this action is now accessing a new path */
		if ((changedFields & ADD_PATH_ACCESS) != 0) {
			actionMgr.addSequencedFileAccess(accessSeqno, actionId, accessPathId, accessOpType);
		}

		/* if this action is no longer accessing a new path */
		if ((changedFields & REMOVE_PATH_ACCESS) != 0) {
			actionMgr.removeFileAccess(actionId, accessPathId);
		}
		
		return (changedFields != 0);
	}

	/*-------------------------------------------------------------------------------------*/
}
