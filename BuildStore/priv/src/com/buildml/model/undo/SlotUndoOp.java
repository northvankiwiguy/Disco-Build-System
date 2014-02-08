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

import com.buildml.model.IActionMgr;
import com.buildml.model.IActionTypeMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.model.ISlotTypes;
import com.buildml.model.ISubPackageMgr;
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
	private final static int NEW_SLOT   	     = 1;
	private final static int CHANGE_SLOT         = 2;
	private final static int REMOVE_SLOT	     = 3;
	private final static int CHANGE_SLOT_VALUE	 = 4;
		
	/** The code for this operation */
	private int opCode = 0;

	/** The type of owner (ISlotTypes.SLOT_OWNER_ACTION/SLOT_OWNER_PACKAGE) */
	private int ownerType;

	/** Our IPackageMgr for operating on */
	private IPackageMgr pkgMgr;
	
	/** Our IActionTypeMgr for operating on */
	private IActionTypeMgr actionTypeMgr;
	
	/** Our ISubPackageMgr for operating on */
	private ISubPackageMgr subPkgMgr;
	
	/** Our IActionMgr for operating on */
	private IActionMgr actionMgr;

	/** For NEW_SLOT, REMOVE_SLOT and CHANGE_SLOT_VALUE, what slotId */
	private int slotId;
	
	/** For CHANGE_SLOT, the new slot details */
	private SlotDetails newDetails;

	/** For CHANGE_SLOT, the existing slot details */
	private SlotDetails oldDetails;
	
	/** For CHANGE_SLOT_VALUE, the action or sub-package ID */
	private int ownerId;
	
	/** For CHANGE_SLOT_VALUE, true if there was an old value (false if it defaulted) */
	private boolean oldExisted;
	
	/** For CHANGE_SLOT_VALUE, true if there is a new value (false if it should default) */
	private boolean newExists;
	
	/** For CHANGE_SLOT_VALUE, the old slot value (if oldExisted is true) */
	private Object oldValue;
	
	/** For CHANGE_SLOT_VALUE, the new slot value (if newExisted is true) */
	private Object newValue;
	
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
		this.subPkgMgr = buildStore.getSubPackageMgr();
		this.actionMgr = buildStore.getActionMgr();
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
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Record a change in a slot's value.
	 * 
	 * @param ownerId 		The actionId or subPkgId that owns the slot.
	 * @param slotId 		The ID of the slot to be changed.
	 * @param oldExisted	True if the slot previously had a value (false if it defaulted).
	 * @param oldValue		If oldExisted was true, the previous slot value.
	 * @param newExists		True if the slot should now have a value (false if it should default).
	 * @param newValue		If newExisted is true, the new slot value.
	 */
	public void recordChangeSlotValue(int ownerId, int slotId, 
										boolean oldExisted, Object oldValue, 
										boolean newExists, Object newValue) {
		
		opCode = CHANGE_SLOT_VALUE;
		this.ownerId = ownerId;
		this.slotId = slotId;
		this.oldExisted = oldExisted;
		this.oldValue = oldValue;
		this.newExists = newExists;
		this.newValue = newValue;
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
			
		case CHANGE_SLOT_VALUE:
			if (ownerType == ISlotTypes.SLOT_OWNER_PACKAGE) {
				if (oldExisted) {
					subPkgMgr.setSlotValue(ownerId, slotId, oldValue);
				} else {
					subPkgMgr.clearSlotValue(ownerId, slotId);
				}
			} else {
				if (oldExisted) {
					actionMgr.setSlotValue(ownerId, slotId, oldValue);
				} else {
					actionMgr.clearSlotValue(ownerId, slotId);
				}
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
			
		/* redo the "change slot value" operation */
		case CHANGE_SLOT_VALUE:
			if (ownerType == ISlotTypes.SLOT_OWNER_PACKAGE) {
				if (newExists) {
					subPkgMgr.setSlotValue(ownerId, slotId, newValue);
				} else {
					subPkgMgr.clearSlotValue(ownerId, slotId);
				}
			} else {
				if (newExists) {
					actionMgr.setSlotValue(ownerId, slotId, newValue);
				} else {
					actionMgr.clearSlotValue(ownerId, slotId);
				}
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
