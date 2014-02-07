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

import com.buildml.model.IActionTypeMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.model.ISlotTypes;
import com.buildml.model.ISlotTypes.SlotDetails;

/**
 * An undo/redo operation for any change that is made to a slot. This object records the
 * details of the changes necessary to the IBuildStore so they can be undone or redone later.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class SlotUndoOp implements IUndoOp {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/**
	 * What type of event does this operation represent?
	 */
	private final static int NEW_SLOT   	 = 1;
	private final static int CHANGE_SLOT     = 2;
	private final static int REMOVE_SLOT	 = 3;
		
	/** The code for this operation */
	private int opCode = 0;

	/** The type of owner (ISlotTypes.SLOT_OWNER_ACTION/SLOT_OWNER_PACKAGE) */
	private int ownerType;

	/** Our IPackageMgr for operating on */
	private IPackageMgr pkgMgr;
	
	/** Our IActionTypeMgr for operating on */
	private IActionTypeMgr actionTypeMgr;

	/** For NEW_SLOT and REMOVE_SLOT, what slotId */
	private int slotId;
	
	/** For CHANGE_SLOT, the new slot details */
	private SlotDetails newDetails;

	/** For CHANGE_SLOT, the existing slot details */
	private SlotDetails oldDetails;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link SlotUndoOp} object, representing a single entry on the
	 * undo/redo stack.
	 * 
	 * @param buildStore  The IBuildStore we're performing the operation on.
	 * @param ownerType   One of SLOT_OWNER_ACTION or SLOT_OWNER_PACKAGE. 
	 */
	public SlotUndoOp(IBuildStore buildStore, int ownerType) {
		this.ownerType = ownerType;
		this.pkgMgr = buildStore.getPackageMgr();
		this.actionTypeMgr = buildStore.getActionTypeMgr();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Record the creation of a new slot.
	 * 
	 * @param slotId ID of the newly-created slot.
	 */
	public void recordNewSlot(int slotId) {
		opCode = NEW_SLOT;
		this.slotId = slotId;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Record the removal of a slot.
	 * 
	 * @param slotId ID of the slot to remove.
	 */
	public void recordRemoveSlot(int slotId) {
		opCode = REMOVE_SLOT;
		this.slotId = slotId;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Record the change of a slot.
	 * 
	 * @param oldDetails The changed slot's old details.
	 * @param newDetails The changed slot's new details.
	 */
	public void recordChangeSlot(SlotDetails oldDetails, SlotDetails newDetails) {
		opCode = CHANGE_SLOT;
		this.oldDetails = oldDetails;
		this.newDetails = newDetails;
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

		/* undo the "new slot" operation by trashing it */
		case NEW_SLOT:
			if (ownerType == ISlotTypes.SLOT_OWNER_PACKAGE) {
				pkgMgr.trashSlot(slotId);
			} else if (ownerType == ISlotTypes.SLOT_OWNER_ACTION) {
				actionTypeMgr.trashSlot(slotId);
			}
			break;

		/* undo the "change slot" operation */
		case CHANGE_SLOT:
			if (ownerType == ISlotTypes.SLOT_OWNER_PACKAGE) {
				pkgMgr.changeSlot(oldDetails);
			} else if (ownerType == ISlotTypes.SLOT_OWNER_ACTION) {
				actionTypeMgr.changeSlot(oldDetails);
			}
			break;

		/* undo the "remove slot" operation by reviving it */
		case REMOVE_SLOT:
			if (ownerType == ISlotTypes.SLOT_OWNER_PACKAGE) {
				pkgMgr.reviveSlot(slotId);
			} else if (ownerType == ISlotTypes.SLOT_OWNER_ACTION) {
				actionTypeMgr.reviveSlot(slotId);
			}
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
		
		/* redo the "new slot" operation by reviving it */
		case NEW_SLOT:
			if (ownerType == ISlotTypes.SLOT_OWNER_PACKAGE) {
				pkgMgr.reviveSlot(slotId);
			} else if (ownerType == ISlotTypes.SLOT_OWNER_ACTION) {
				actionTypeMgr.reviveSlot(slotId);
			}
			break;

		/* redo the "change slot" operation */
		case CHANGE_SLOT:
			if (ownerType == ISlotTypes.SLOT_OWNER_PACKAGE) {
				pkgMgr.changeSlot(newDetails);
			} else if (ownerType == ISlotTypes.SLOT_OWNER_ACTION) {
				actionTypeMgr.changeSlot(newDetails);
			}
			break;

		/* redo the "remove slot" operation by trashing it */
		case REMOVE_SLOT:
			if (ownerType == ISlotTypes.SLOT_OWNER_PACKAGE) {
				pkgMgr.trashSlot(slotId);
			} else if (ownerType == ISlotTypes.SLOT_OWNER_ACTION) {
				actionTypeMgr.trashSlot(slotId);
			}
			break;

		default:
			return false;
		}
		
		/* yes, a change happened */
		return true;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
