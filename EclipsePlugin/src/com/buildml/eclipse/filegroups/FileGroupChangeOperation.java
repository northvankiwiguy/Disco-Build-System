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

package com.buildml.eclipse.filegroups;

import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.utils.BmlAbstractOperation;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.errors.FatalError;
import com.buildml.model.IFileGroupMgr;
import com.buildml.model.IPackageMemberMgr;

/**
 * An undo/redo operation for any change that is made to a FileGroup. This object records
 * the change in the undo/redo stack, allowing changes to fileGroupMgr to be made and unmade.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class FileGroupChangeOperation extends BmlAbstractOperation {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/**
	 * Bitmap of all the parts of the fileGroup that have changed, and will need to be changed
	 * back on an undo/redo operation.
	 */
	private final static int CHANGED_PACKAGE    	= 1;
	private final static int CHANGED_LOCATION   	= 2;
	private final static int CHANGED_MEMBERSHIP 	= 4;
	
	/** The ID of the fileGroup being changed */
	private int fileGroupId;
	
	/** The type of this file group (e.g. SOURCE_GROUP, GENERATED_GROUP, etc) */
	private int fileGroupType;
	
	/** The fields of this operation that have changed - see above bitmap */
	private int changedFields = 0;
	
	/** if CHANGED_PACKAGE set, what is the original package ID? */
	private int oldPackage;

	/** if CHANGED_PACKAGE set, what is the new package ID? */
	private int newPackage;
	
	/** if CHANGED_LOCATION, what is the old (x, y) location */
	private int oldX, oldY;
	
	/** if CHANGED_LOCATION, what is the new (x, y) location */
	private int newX, newY;
	
	/** if CHANGED_MEMBERSHIP, what are the old members? */
	private List<Integer> oldMembers;

	/** if CHANGED_MEMBERSHIP, what are the new members? */
	private List<Integer> newMembers;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new FileGroupChangeOperation object, representing a single entry on the
	 * undo/redo stack.
	 * 
	 * @param label       The text label to appear in the "Edit" menu, next to "Undo" or "Redo".
	 * @param fileGroupId The fileGroupMgr ID of the fileGroup being changed.
	 */
	public FileGroupChangeOperation(String label, int fileGroupId) {
		super(label);
		
		IFileGroupMgr fileGroupMgr = buildStore.getFileGroupMgr();
		
		this.fileGroupId = fileGroupId;
		this.fileGroupType = fileGroupMgr.getGroupType(fileGroupId);
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Records the fact that the file groups's package has changed. If there is no change in the
	 * packageId, this method does nothing.
	 * 
	 * @param prevPackageId The group's current package ID.
	 * @param nextPackageId The group's future package ID.
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
	 * Records the fact that the file groups's pictogram (icon) has been moved to a new location
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
	 * Record the fact that the file groups's membership (file IDs) has changed.
	 * @param oldMembers The old members of this file group.
	 * @param newMembers The new members of this file group.
	 */
	public void recordMembershipChange(List<Integer> oldMembers, List<Integer> newMembers) {
		if (!oldMembers.equals(newMembers)){
			changedFields |= CHANGED_MEMBERSHIP;
			this.oldMembers = oldMembers;
			this.newMembers = newMembers;
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Records the operation in the undo/redo stack, but only if there's an actual change
	 * in the file group.
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
		
		/* if the action's package needs to change... */
		if ((changedFields & CHANGED_PACKAGE) != 0) {
			pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE_GROUP, fileGroupId, oldPackage);
		}

		/* if the action's location needs to change... */
		if ((changedFields & CHANGED_LOCATION) != 0){
			pkgMemberMgr.setMemberLocation(IPackageMemberMgr.TYPE_FILE_GROUP, fileGroupId, oldX, oldY);
		}

		if ((changedFields & CHANGED_MEMBERSHIP) != 0){
			setFileGroupMembers(fileGroupId, oldMembers);
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

		/* if the file group's package needs to change... */
		if ((changedFields & CHANGED_PACKAGE) != 0) {
			pkgMemberMgr.setPackageOfMember(IPackageMemberMgr.TYPE_FILE_GROUP, fileGroupId, newPackage);
		}

		/* if the file group's location needs to change... */
		if ((changedFields & CHANGED_LOCATION) != 0){
			pkgMemberMgr.setMemberLocation(IPackageMemberMgr.TYPE_FILE_GROUP, fileGroupId, newX, newY);
		}
		
		/* if the file group's membership needs to change... */
		if ((changedFields & CHANGED_MEMBERSHIP) != 0){
			setFileGroupMembers(fileGroupId, newMembers);
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

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/	

	/**
	 * Set the membership of a file group to the contents of the specified ArrayList.
	 * @param fileGroupId 	The ID of the file group to set membership of.
	 * @param members		The ArrayList of members to populate the file group with.
	 */
	private void setFileGroupMembers(int fileGroupId, List<Integer> members) {
		IFileGroupMgr fileGroupMgr = buildStore.getFileGroupMgr();
		
		if ((fileGroupType != IFileGroupMgr.SOURCE_GROUP) && 
				(fileGroupType != IFileGroupMgr.MERGE_GROUP)) {
			throw new FatalError("Unhandled file group type: " + fileGroupType);
		}
				
		if (fileGroupType == IFileGroupMgr.SOURCE_GROUP) {
			fileGroupMgr.setPathIds(fileGroupId, members.toArray(new Integer[0]));
		} else {
			fileGroupMgr.setSubGroups(fileGroupId, members.toArray(new Integer[0]));			
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
