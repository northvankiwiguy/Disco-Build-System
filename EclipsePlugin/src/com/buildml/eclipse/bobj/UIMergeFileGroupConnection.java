/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.bobj;

/**
 * A "business object" representing a connection between a UIFileGroup and a merge UIFileGroup.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class UIMergeFileGroupConnection extends UIConnection {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/** The connection is outward from the sub file group */
	public static final int OUTPUT_FROM_SUB_GROUP = 0;
	
	/** The connection is into the merge file group's input */
	public static final int INPUT_TO_MERGE_GROUP = 1;

	/** The ID of the file group at one end of the connection */
	private int sourceFileGroupId;

	/** The ID of the merge file group at one end of the connection */
	private int targetFileGroupId;
	
	/** The 0-based index, within the merge group, of this connection */
	private int index;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link UIMergeFileGroupConnection}, representing a connection between a UIFileGroup
	 * and a merge file group
	 * @param sourceFileGroupId	 ID of the sub file group. 
	 * @param targetFileGroupId  ID of the merge file group that the sub file group is joined into.
	 * @param index              0-based index within the merge group, for this connection.
	 * 
	 */
	public UIMergeFileGroupConnection(int sourceFileGroupId, int targetFileGroupId, int index) {
		this.sourceFileGroupId = sourceFileGroupId;
		this.targetFileGroupId = targetFileGroupId;
		this.index = index;
	}

	/*-------------------------------------------------------------------------------------*/	

	/**
	 * @return the sourceFileGroupId
	 */
	public int getSourceFileGroupId() {
		return sourceFileGroupId;
	}

	/*-------------------------------------------------------------------------------------*/	

	/**
	 * @return the targetFileGroupId
	 */
	public int getTargetFileGroupId() {
		return targetFileGroupId;
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/*-------------------------------------------------------------------------------------*/	
}
