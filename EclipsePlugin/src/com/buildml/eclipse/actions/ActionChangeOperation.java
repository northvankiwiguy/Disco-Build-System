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

package com.buildml.eclipse.actions;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.utils.BmlAbstractOperation;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.model.IActionMgr;
import com.buildml.model.IPackageMemberMgr;

/**
 * An undo/redo operation for any change that is made to an Action. This object records
 * the change in the undo/redo stack, allowing changes to actionMgr to be made and unmade.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class ActionChangeOperation extends BmlAbstractOperation {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/**
	 * Bitmap of all the parts of the action that have changed, and will need to be changed
	 * back on an undo/redo operation.
	 */
	private final static int CHANGED_PACKAGE  = 1;
	private final static int CHANGED_COMMAND  = 2;
	private final static int CHANGED_LOCATION = 4;
	
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
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new ActionChangeOperation object, representing a single entry on the
	 * undo/redo stack.
	 * 
	 * @param label The text label to appear in the "Edit" menu, next to "Undo" or "Redo".
	 * @param actionId The actionMgr ID of the action being changed.
	 */
	public ActionChangeOperation(String label, int actionId) {
		super(label);
		
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
	 * Records the operation in the undo/redo stack, but only if there's an actual change
	 * in the action.
	 */
	@Override
	public void recordAndInvoke() {
		if (changedFields != 0) {
			super.recordAndInvoke();
		}
	}
	
	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.utils.BmlAbstractOperation#undo()
	 */
	@Override
	protected IStatus undo() throws ExecutionException {
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

		/* if there's a change, mark the editor as dirty */
		if (changedFields != 0) {
			MainEditor editor = EclipsePartUtils.getActiveMainEditor();
			if (editor != null) {
				editor.markDirty();
			}
		}
		return Status.OK_STATUS;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.utils.BmlAbstractOperation#redo()
	 */
	@Override
	protected IStatus redo() throws ExecutionException {
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
		
		/* if there's a change, mark the editor as dirty */
		if (changedFields != 0) {
			MainEditor editor = EclipsePartUtils.getActiveMainEditor();
			if (editor != null) {
				editor.markDirty();
			}
		}
		return Status.OK_STATUS;
	}

	/*-------------------------------------------------------------------------------------*/
}
