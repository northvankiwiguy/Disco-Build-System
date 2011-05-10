/*******************************************************************************
 * Copyright (c) 2011 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.arapiki.utils.errors;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class ErrorCode {

	/**
	 * The following error codes can be used by any application that requires them.
	 */
	public static final int OK 				= 0;	/* no error */
	public static final int NOT_FOUND			= -1;	/* the item was not found */
	public static final int ALREADY_USED 		= -2;	/* the name was already in use */
	public static final int INVALID_NAME		= -3;	/* the name is incorrectly formatted */
	public static final int ONLY_ONE_ALLOWED 	= -4;	/* the item can only be added once */
	public static final int CANT_REMOVE		= -5;	/* this item can't be removed */
	public static final int NOT_A_DIRECTORY	= -6;	/* a directory name or ID was expected */
	public static final int BAD_PATH 			= -7;	/* an invalid path name or ID was provided */
	public static final int BAD_VALUE 		= -8;	/* an invalid value was provided */

}
