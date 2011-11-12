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
 * Standard return codes for any Disco function that needs them. Error return values are negative so they can be returned from
 * functions that return positive integers as "success" values.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class ErrorCode {

	/*
	 * The following error codes can be used by any application that requires them.
	 */
	
	/** No error. */
	public static final int OK 					= 0;
	
	/** The item was not found. */
	public static final int NOT_FOUND			= -1;
	
	/** The name was already in use. */
	public static final int ALREADY_USED 		= -2;
	
	/** The name is incorrectly formatted. */
	public static final int INVALID_NAME		= -3;
	
	/** The item can only be added once. */
	public static final int ONLY_ONE_ALLOWED 	= -4;
	
	/** This item can't be removed. */
	public static final int CANT_REMOVE			= -5;
	
	/** A directory name or ID was expected. */
	public static final int NOT_A_DIRECTORY		= -6;
	
	/** An invalid path name or ID was provided. */
	public static final int BAD_PATH 			= -7;
	
	/** An invalid value was provided. */
	public static final int BAD_VALUE 			= -8;

	/**
	 * This class can't be instantiated.
	 */
	private ErrorCode() {
		/* empty */
	}
}

