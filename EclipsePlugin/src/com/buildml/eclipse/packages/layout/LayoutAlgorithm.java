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

package com.buildml.eclipse.packages.layout;

import com.buildml.eclipse.actions.ActionChangeOperation;
import com.buildml.eclipse.filegroups.FileGroupChangeOperation;
import com.buildml.eclipse.packages.patterns.ActionPattern;
import com.buildml.eclipse.packages.patterns.FileGroupPattern;
import com.buildml.eclipse.utils.BmlMultiOperation;
import com.buildml.eclipse.utils.errors.FatalError;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMemberMgr.MemberDesc;
import com.buildml.model.IPackageMemberMgr.MemberLocation;

/**
 * A support object for managing the layout of members on a BuildML package diagram.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class LayoutAlgorithm {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** The minimum X coordinate for a position on this diagram */
	private static final int MIN_X = 0;
	
	/** The maximum X coordinate for a position on this diagram */
	private static final int MAX_X = 3000;
	
	/** For esthetic reasons, we leave this many pixels between pictograms */
	private static final int X_PADDING = 50;
	
	/** The IBuildStore that contains the package information */
	private IBuildStore buildStore;
	
	/** The IPackageMemberMgr that contains our package membership information */
	private IPackageMemberMgr pkgMemberMgr;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link LayoutAlgorithm} object.
	 * @param buildStore The IBuildStore that contains the package information. 
	 */
	public LayoutAlgorithm(IBuildStore buildStore) {
		this.buildStore = buildStore;
		this.pkgMemberMgr = buildStore.getPackageMemberMgr();
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * For the specified package member, determine the range of X coordinates that this member's
	 * pictogram can be moved to. This range will depend on the X coordinates of the neighbouring
	 * package members.
	 * 
	 * @param memberType    One of TYPE_ACTION, TYPE_FILE_GROUP, etc.
	 * @param memberId 	    The file group that is being moved.
	 * @return The left and right X coordinate bounds.
	 */
	public LeftRightBounds getMemberMovementBounds(int memberType, int memberId) {
		
		IPackageMemberMgr pkgMemberMgr = buildStore.getPackageMemberMgr();
		int leftX = MIN_X, rightX = MAX_X;
				
		/* determine the list of neighbours on the right side of this file group */
		MemberDesc[] rightNeighbours = pkgMemberMgr.getNeighboursOf(
				memberType, memberId, IPackageMemberMgr.NEIGHBOUR_RIGHT);
		for (int i = 0; i < rightNeighbours.length; i++) {
			if (rightNeighbours[i].x < rightX) {
				rightX = rightNeighbours[i].x;
			}
		}
		
		/* adjust the right margin to account for the width of the file group pictogram itself */
		rightX = rightX - getSizeOfPictogram(memberType).getWidth();

		/* 
		 * Now check the left neighbours, although be sure to account for the
		 * width of each left neighbour.
		 */
		MemberDesc[] leftNeighbours = pkgMemberMgr.getNeighboursOf(
				memberType, memberId, IPackageMemberMgr.NEIGHBOUR_LEFT);
		for (int i = 0; i < leftNeighbours.length; i++) {
			if (leftNeighbours[i].x > leftX) {
				leftX = leftNeighbours[i].x;
				leftX += getSizeOfPictogram(leftNeighbours[i].memberType).getWidth();
			}
		}
		
		/* just to be sure, make sure the left bound isn't now greater than the right bound */
		if (leftX > rightX) {
			rightX = leftX;
		}

		return new LeftRightBounds(leftX, rightX);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the (width, height) in pixel of the specified type of member.
	 * @param memberType
	 * @return The size (width, height) in pixels of the specified pictogram.
	 */
	public PictogramSize getSizeOfPictogram(int memberType) {
		if (memberType == IPackageMemberMgr.TYPE_ACTION) {
			return ActionPattern.getSize();
		} else if (memberType == IPackageMemberMgr.TYPE_FILE_GROUP) {
			return FileGroupPattern.getSize();
		}
		throw new FatalError("Unhandled pictogram type: " + memberType);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Given a package member (e.g. file group or action), ensure that all of its right-side
	 * neighbours are moved (if necessary) to higher x-coordinates. The avoid the appearance
	 * of backward-facing arrows.
	 * 
	 * @param multiOp		The multi-undo/redo operation to append "move" operations to.
	 * @param memberType	The type of member that we're starting the bumping from.
	 * @param memberId		The ID of the member (actionId, fileGroupId).
	 * @param moveNow		True if we should do the move operations now, or false to let the
	 * 						undo/redo operation handle it later.
	 */
	public void bumpPictogramsRight(BmlMultiOperation multiOp,
			int memberType, int memberId, boolean moveNow) {

		/* 
		 * Determine this member's current x coordinate (of it's right edge). This is
		 * necessary to start the recursion process.
		 */
		MemberLocation location = pkgMemberMgr.getMemberLocation(memberType, memberId);
		if (location == null) {
			return;
		}
		
		/* our recursive helper does the rest... */
		bumpPictogramsRightHelper(multiOp, memberType, memberId, moveNow, location.x, location.y);
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * A helper method for bumpPictogramsRight.
	 * 
	 * @param multiOp		The multi-undo/redo operation to append "move" operations to.
	 * @param memberType	The type of member that we're starting the bumping from.
	 * @param memberId		The ID of the member (actionId, fileGroupId).
	 * @param moveNow		True if we should do the move operations now, or false to let the
	 * 						undo/redo operation handle it later.
	 * @param x				The current member's x-coordinate.
	 * @param y				y-coordinate (not used for now)
	 */
	private void bumpPictogramsRightHelper(BmlMultiOperation multiOp,
			int memberType, int memberId, boolean moveNow, int x, int y) {

		/* allow for the width of the pictogram, and some extra padding */
		int rightEdgeX = x + getSizeOfPictogram(memberType).getWidth() + X_PADDING;
				
		/* determine this member's right-side neigbours */
		MemberDesc neighbours[] = pkgMemberMgr.getNeighboursOf(
				memberType, memberId, IPackageMemberMgr.NEIGHBOUR_RIGHT);
		
		/* 
		 * For each neighbour, see if it's x-coordinate is left of us, and therefore needs
		 * to be bumped right.
		 */
		for (int i = 0; i < neighbours.length; i++) {
			MemberDesc thisNeighbour = neighbours[i];
			if (thisNeighbour.x < rightEdgeX) {
				/* bump */
				if (moveNow) {
					/* silently ignore errors - in worst case, the member just won't move */
					pkgMemberMgr.setMemberLocation(thisNeighbour.memberType, thisNeighbour.memberId, 
													rightEdgeX, thisNeighbour.y); 
				}
				
				/* add this move to the undo/redo history */
				addMemberMoveToHistory(
						multiOp, thisNeighbour.memberType, thisNeighbour.memberId, 
						thisNeighbour.x, thisNeighbour.y, rightEdgeX, thisNeighbour.y);
				
				
				/* now recursively traverse all of this neighbour's right-side neighbours */
				bumpPictogramsRightHelper(
						multiOp, thisNeighbour.memberType, thisNeighbour.memberId, moveNow,
						rightEdgeX, thisNeighbour.y);
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create the relevant undo/redo operation to indicate this member's change in location.
	 * 
	 * @param multiOp		The multi-undo/redo operation to append to.
	 * @param memberType	The type of this member (IPackageMemberMgr.TYPE_FILE_GROUP, etc).
	 * @param memberId		The member's ID number
	 * @param oldX			The member's old X coordinate.
	 * @param oldY			The member's old Y coordinate.
	 * @param newX			The member's new X coordinate.
	 * @param newY			The member's old Y coordinate.
	 */
	private void addMemberMoveToHistory(BmlMultiOperation multiOp, int memberType, int memberId, 
										int oldX,int oldY, int newX, int newY) {
		
		if (memberType == IPackageMemberMgr.TYPE_ACTION) {
			ActionChangeOperation op = new ActionChangeOperation("", memberId);
			op.recordLocationChange(oldX, oldY, newX, newY);
			multiOp.add(op);
			
		} else if (memberType == IPackageMemberMgr.TYPE_FILE_GROUP) {
			FileGroupChangeOperation op = new FileGroupChangeOperation("", memberId);
			op.recordLocationChange(oldX, oldY, newX, newY);
			multiOp.add(op);
			
		} else {
			throw new FatalError("Invalid memberType");
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
