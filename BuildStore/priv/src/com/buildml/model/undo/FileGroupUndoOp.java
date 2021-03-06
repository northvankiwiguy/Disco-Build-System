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

import java.util.List;

import com.buildml.model.IBuildStore;
import com.buildml.model.IFileGroupMgr;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.utils.errors.FatalError;

/**
 * An undo/redo operation for any change that is made to a FileGroup. This object records the
 * details of the changes necessary to the IBuildStore so they can be undone or redone later.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class FileGroupUndoOp implements IUndoOp {

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
	
	/** The IBuildStore we're operating on */
	private IBuildStore buildStore;
	
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
	private List<?> oldMembers;

	/** if CHANGED_MEMBERSHIP, what are the new members? */
	private List<?> newMembers;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link FileGroupUndoOp} object, representing a single entry on the
	 * undo/redo stack.
	 * 
	 * @param buildStore  The IBuildStore we're performing the operation on.
	 * @param fileGroupId The fileGroupMgr ID of the fileGroup being changed.
	 */
	public FileGroupUndoOp(IBuildStore buildStore, int fileGroupId) {
		
		IFileGroupMgr fileGroupMgr = buildStore.getFileGroupMgr();
		
		this.buildStore = buildStore;
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
	public void recordMembershipChange(List<?> oldMembers, List<?> newMembers) {
		if (!oldMembers.equals(newMembers)){
			changedFields |= CHANGED_MEMBERSHIP;
			this.oldMembers = oldMembers;
			this.newMembers = newMembers;
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.undo.IUndoOp.undo()
	 */
	@Override
	public boolean undo() {
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
		
		return (changedFields != 0);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.undo.IUndoOp.redo()
	 */
	@Override
	public boolean redo() {
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
		return (changedFields != 0);
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/	

	/**
	 * Set the membership of a file group to the contents of the specified ArrayList.
	 * @param fileGroupId 	The ID of the file group to set membership of.
	 * @param members		The ArrayList of members to populate the file group with.
	 */
	private void setFileGroupMembers(int fileGroupId, List<?> members) {
		IFileGroupMgr fileGroupMgr = buildStore.getFileGroupMgr();
				
		if (fileGroupType == IFileGroupMgr.SOURCE_GROUP) {
			fileGroupMgr.setPathIds(fileGroupId, members.toArray(new Integer[0]));
		} else if (fileGroupType == IFileGroupMgr.MERGE_GROUP) {
			fileGroupMgr.setSubGroups(fileGroupId, members.toArray(new Integer[0]));
		} else if (fileGroupType == IFileGroupMgr.FILTER_GROUP) {
			fileGroupMgr.setPathStrings(fileGroupId, members.toArray(new String[0]));			
		} else {
			throw new FatalError("Unhandled file group type: " + fileGroupType);
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
