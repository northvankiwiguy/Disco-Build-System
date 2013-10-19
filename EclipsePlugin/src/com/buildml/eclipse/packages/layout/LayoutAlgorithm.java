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

import com.buildml.eclipse.packages.patterns.ActionPattern;
import com.buildml.eclipse.packages.patterns.FileGroupPattern;
import com.buildml.eclipse.utils.errors.FatalError;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMemberMgr.MemberDesc;

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
	
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/** The IBuildStore that contains the package information */
	private IBuildStore buildStore;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link LayoutAlgorithm} object.
	 * @param buildStore The IBuildStore that contains the package information. 
	 */
	public LayoutAlgorithm(IBuildStore buildStore) {
		this.buildStore = buildStore;
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

}
