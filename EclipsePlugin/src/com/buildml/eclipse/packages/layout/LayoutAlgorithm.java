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

import java.util.ArrayList;

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
	
	/** simple class for returning a member's row and col, within the auto-layout grid */
	private class GridLocation {
		int col;
		int row;
	}
	
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
				memberType, memberId, IPackageMemberMgr.NEIGHBOUR_RIGHT, false);
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
				memberType, memberId, IPackageMemberMgr.NEIGHBOUR_LEFT, false);
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
	 * @return The number of pixels in the X direction to pad between pictograms, to make
	 * them look nicely spread out.
	 */
	public int getXPadding() {
		return X_PADDING;
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
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Auto-layout the members of the specified package using an algorithm that places
	 * package members in an easy-to-understand format. That is, all members are placed
	 * neatly into columns on the package diagram, with the right-most column containing
	 * all of the terminal file groups (no actions depend on these groups). All connection
	 * arrows must flow from left to right, crossing other arrows as little as possible.
	 * 
	 * The result of calling this method is a set of undo/redo operations that will make
	 * the necessary "move" operations happen.
	 * 
	 * @param multiOp	The multi-undo/redo operation to append "move" operations to.
	 * @param pkgId		The ID of the package to auto-layout.
	 */
	public void autoLayoutPackage(BmlMultiOperation multiOp, int pkgId) {
		
		/* initialize the data structure we'll use for sorting package members */
		ArrayList<ArrayList<MemberDesc>> layoutGrid = new ArrayList<ArrayList<MemberDesc>>();
		
		/* 
		 * Start by obtaining the sub-set of this package's members that have no outputs.
		 * These members will go in the right-most column (column 0). The recursively place
		 * all of their left neighbours into columns 1, 2, 3, ...
		 */
		MemberDesc allMembers[] = pkgMemberMgr.getMembersInPackage(
				pkgId, IPackageMemberMgr.SCOPE_NONE, IPackageMemberMgr.TYPE_ANY);
		for (int i = 0; i < allMembers.length; i++) {
			MemberDesc member = allMembers[i];
			MemberDesc rightNeighbours[] = pkgMemberMgr.getNeighboursOf(
					member.memberType, member.memberId, IPackageMemberMgr.NEIGHBOUR_RIGHT, false);
			if (rightNeighbours.length == 0) {
				addWithNeighboursToAutoLayoutGrid(layoutGrid, member, 0);
			}
		}
	
		/* 
		 * All members are now in the layout grid, and are in the correct columns. Next, sort
		 * the members in each column into a logical order and assign (x, y) positions.
		 */
		assignAutoLayoutLocations(layoutGrid);
		
		/*
		 * Add the actual move operations to our multi-undo/redo operation. No changes
		 * have actually taken place, but they will if our caller invokes this operation.
		 */
		recordAutoLayoutOperations(multiOp, layoutGrid);		
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
				memberType, memberId, IPackageMemberMgr.NEIGHBOUR_RIGHT, false);
		
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

	/**
	 * Helper method for autoLayoutPackage() to recursively add package members into the
	 * layout grid. Neighbouring members are added to consecutive columns in the layout
	 * grid so that they're appearing in neighbouring columns on the diagram.
	 * 
	 * @param layoutGrid The layout grid that we're populating.
	 * @param member	 The member to be added.
	 * @param colNum	 The column within the grid to add the member to.
	 */
	private void addWithNeighboursToAutoLayoutGrid(
			ArrayList<ArrayList<MemberDesc>> layoutGrid, 
			MemberDesc member, int colNum) {
		
		/* 
		 * if this member is already in the layout grid, and its further to the right
		 * than our current column, remove it from it's existing location so that we
		 * can place it in its new location. This gives the effect of placing members
		 * in one column left of its left-most right-side neighbour, therefore avoiding
		 * arrows that point backwards.
		 */
		GridLocation loc = findMemberInAutoLayoutGrid(layoutGrid, member);
		if ((loc != null) && (loc.col < colNum)) {
			removeMemberFromAutoLayoutGrid(layoutGrid, loc.col, loc.row);
		}
		
		/* 
		 * If it's not already somewhere in the current column, add this member to the end of 
		 * this column. Take care to add the column if it doesn't yet exist.
		 */
		if ((loc == null) || (loc.col < colNum)) {
			if (colNum >= layoutGrid.size()) {
				layoutGrid.add(colNum, new ArrayList<MemberDesc>());
			}
			layoutGrid.get(colNum).add(member);
		}
		
		/* 
		 * Now, fetch all of this member's left neighbours, adding them to colNum + 1.
		 * Note that since colNum == 0 implies the right most column, we are adding
		 * neighbours in a left-ward direction.
		 */
		MemberDesc leftNeighbours[] = pkgMemberMgr.getNeighboursOf(
				member.memberType, member.memberId, IPackageMemberMgr.NEIGHBOUR_LEFT, false);
		if (leftNeighbours == null) {
			return;
		}
		for (int i = 0; i < leftNeighbours.length; i++) {
			addWithNeighboursToAutoLayoutGrid(layoutGrid, leftNeighbours[i], colNum + 1);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper method for autoLayoutPackage() that locates a specific member within the
	 * layout grid, returning its (col, row) position.
	 * 
	 * @param layoutGrid The layout grid containing all of the members.
	 * @param member The member to search for (only memberType and memberId are used for
	 * comparison).
	 * @return The (col, row) location where the member was found, else null if the member
	 * is not in the layout grid.
	 */
	private GridLocation findMemberInAutoLayoutGrid(
			ArrayList<ArrayList<MemberDesc>> layoutGrid, MemberDesc member) {
		
		/* for now, this is an O(n*n) algorithm, but that's OK since packages are kept small */
		int numCols = layoutGrid.size();
		
		/* for each column... */
		for (int col = 0; col != numCols; col++) {
			ArrayList<MemberDesc> column = layoutGrid.get(col);
			int numRows = column.size();
			
			/* for each row in that column... */
			for (int row = 0; row != numRows; row++) {
				MemberDesc cell = column.get(row);
				
				/* if this member matches what we're searching for, return (col, row) */
				if ((cell.memberId == member.memberId) && (cell.memberType == member.memberType)) {
					GridLocation location = new GridLocation();
					location.col = col;
					location.row = row;
					return location;
				}
			}
		}		
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper method for autoLayoutPackage() that removes a member from a specified (col, row)
	 * location within the grid. If the (col, row) parameters are out of range, no change is
	 * made to the layout grid.
	 * 
	 * @param layoutGrid The layout grid containing all of the members.
	 * @param col The column of the member to remove. 
	 * @param row The row of the member to remove.
	 */
	private void removeMemberFromAutoLayoutGrid(
			ArrayList<ArrayList<MemberDesc>> layoutGrid, 
			int col, int row) {
		
		if (col >= layoutGrid.size()) {
			return;
		}
		ArrayList<MemberDesc> column = layoutGrid.get(col);
		if (row >= column.size()) {
			return;
		}
		column.remove(row);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Helper method for autoLayoutPackage() that assigns (x, y) coordinates for each member. 
	 * This method encapsulates the "smarts" of the layout algorithm. The outcome is that each
	 * member has it's "x" and "y" fields adjusted accordingly.
	 * 
	 * @param layoutGrid The layout grid containing all of the members.
	 */
	private void assignAutoLayoutLocations(ArrayList<ArrayList<MemberDesc>> layoutGrid) {

		/*
		 * These initial calculations are done to ensure that all member types can fit into
		 * a fixed-sized column. "colWidth" is the width that will incorporate all of the
		 * members. We also need to center the members within that column, so "fileGroupOffset"
		 * is used as an offset for positioning file groups.
		 */
		int actionWidth = getSizeOfPictogram(IPackageMemberMgr.TYPE_ACTION).getWidth();
		int actionHeight = getSizeOfPictogram(IPackageMemberMgr.TYPE_ACTION).getHeight();
		int fileGroupWidth = getSizeOfPictogram(IPackageMemberMgr.TYPE_FILE_GROUP).getWidth();
		int fileGroupHeight = getSizeOfPictogram(IPackageMemberMgr.TYPE_FILE_GROUP).getHeight();
		int colWidth = (int)((actionWidth > fileGroupWidth ? actionWidth : fileGroupWidth) * 1.2);
		int rowHeight = (actionHeight > fileGroupHeight) ? actionHeight : fileGroupHeight;
		int fileGroupOffset = (actionWidth - fileGroupWidth) / 2;

		/*
		 * Now, we proceed to assign x-coordinates for each members, based on its column. For later
		 * on, we also remember which of the columns is the "tallest".
		 */
		int numCols = layoutGrid.size();
		if (numCols == 0) {
			return;
		}
		int tallestColumn = -1;
		int tallestColumnSize = -1; 
		for (int i = 0; i != numCols; i++) {
			
			/* determine the x coordinate of the column so that members are nicely lined up. */
			int colX = colWidth * (numCols - i - 1);
			ArrayList<MemberDesc> colMembers = layoutGrid.get(i);
			int numRows = colMembers.size();
			
			/* is this the column (or one of the columns) that has the most members in it? */
			if (numRows > tallestColumnSize) {
				tallestColumn = i;
				tallestColumnSize = numRows;
			}
			
			/* set all members in the column to the same x coordinate */
			for (int j = 0; j < numRows; j++) {
				MemberDesc member = colMembers.get(j);
				member.x = colX;
				
				/* center file groups */
				if (member.memberType == IPackageMemberMgr.TYPE_FILE_GROUP) {
					member.x += fileGroupOffset;
				}
			}
		}
				
		/*
		 * Now assign y coordinates. This is done by evenly spacing the members across
		 * each column. For example, if there are two members in a row, they're positioned
		 * at 1/3rd and 2/3rds of the way.
		 * TODO: we need a force-based algorithm for positioning members within a column.
		 */
		int diagramHeight = tallestColumnSize * rowHeight;

		for (int i = 0; i != numCols; i++) {	
			ArrayList<MemberDesc> colMembers = layoutGrid.get(i);
			int numRows = colMembers.size();
			
			/* evenly space out the members of the column */
			for (int j = 0; j < numRows; j++) {
				MemberDesc member = colMembers.get(j);
				member.y = (int) (((j + 1) / (double)(numRows + 1)) * diagramHeight);
			}
		}
		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper method for autoLayoutPackage() to appropriately position members within the
	 * layout grid, and set their (x, y) coordinates, we generate all the necessary "move" operations
	 * to actually move each member to its new location.
	 * 
	 * @param multiOp 		The multi-undo/redo operation to add all the move operations to.	
	 * @param layoutGrid 	The layout grid that contains all the members.
	 */
	private void recordAutoLayoutOperations(BmlMultiOperation multiOp, 
											ArrayList<ArrayList<MemberDesc>> layoutGrid) {
		
		/* for each column in the layout grid... */
		int numCols = layoutGrid.size();
		for (int i = 0; i != numCols; i++) {
			ArrayList<MemberDesc> colMembers = layoutGrid.get(i);
			
			/* for each row in each column... */
			for (int j = 0; j < colMembers.size(); j++) {
				MemberDesc member = colMembers.get(j);
				
				/* get the member's current (x, y) location */
				MemberLocation currentLoc = pkgMemberMgr.getMemberLocation(member.memberType, member.memberId);

				/* create an operation to move the member to it's new (x, y) location */
				if (currentLoc != null) {
					addMemberMoveToHistory(multiOp, member.memberType, member.memberId, 
												currentLoc.x, currentLoc.y, member.x, member.y);
				}
			}
		}
	}
	
	/*-------------------------------------------------------------------------------------*/	
}
