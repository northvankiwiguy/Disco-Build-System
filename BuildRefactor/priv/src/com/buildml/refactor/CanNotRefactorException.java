/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.refactor;

import com.buildml.model.FatalBuildStoreError;

/**
 * An Exception class for reporting the reason why a refactoring operation could not
 * be completed. Use getCauseCode() to return the reason (see the enum Cause for detail)
 * and then getPathIds() or getActionIds() to obtain more detail on which paths or
 * actions caused the failure.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
@SuppressWarnings("serial")
public class CanNotRefactorException extends Exception {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** error code so that the caller can determine exactly what went wrong */
	public enum Cause {
		
		/** An invalid file number was provided */
		INVALID_PATH,
		
		/** The file is still in use by one or more actions */
		PATH_IN_USE,
		
		/** The file is still being generated by an action */
		PATH_IS_GENERATED,
		
		/** The action is still in use because a generated file is needed */
		ACTION_IN_USE,
		
		/** The action is not atomic, so this operation can not proceed */
		ACTION_NOT_ATOMIC,
		
		/** Couldn't delete a directory, since it's not empty */
		DIRECTORY_NOT_EMPTY
	}
	
	/** The cause of the exception */
	private Cause causeCode = null;
	
	/** pathId numbers that caused the failure */
	private Integer[] pathIds = null;
	
	/** actionId numbers that caused the failure */
	private Integer[] actionIds = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new CanNotRefactorException, with a specific cause (and set of actions or
	 * paths) for the failure of the refactoring operation.
	 *  
	 * @param causeCode Reason that the refactoring failed.
	 * @param args Optional arguments (file or action IDs) that provide more information.
	 */
	public CanNotRefactorException(Cause causeCode, Integer... args) {
		super();
		
		this.causeCode = causeCode;
		switch (causeCode) {
		
		/* one or more files were invalid */
		case INVALID_PATH:
			pathIds = args;
			break;
			
		/* the file is currently in use, or is still being generated */
		case PATH_IN_USE:
		case PATH_IS_GENERATED:
			actionIds = args;
			break;
		
		/* The action is in use by certain files */
		case ACTION_IN_USE:
			pathIds = args;
			break;
		
		/* The action is not atomic, so the operation can't complete */
		case ACTION_NOT_ATOMIC:
			actionIds = args;
			break;
			
		/* the directory is not empty */
		case DIRECTORY_NOT_EMPTY:
			pathIds = args;
			break;
			
		default:
			throw new FatalBuildStoreError("Invalid cause type: " + causeCode);
		}
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * @return The detailed cause of this exception.
	 */
	public Cause getCauseCode() {
		return causeCode;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The list of pathIds that caused the refactoring to fail. Or null, if this
	 * was not the cause.
	 */
	public Integer[] getPathIds() {
		return pathIds;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The list of actionIds that caused the refactoring to fail. Or null, if this
	 * was not the cause.
	 */
	public Integer[] getActionIds() {
		return actionIds;
	}
	
	/*-------------------------------------------------------------------------------------*/
}