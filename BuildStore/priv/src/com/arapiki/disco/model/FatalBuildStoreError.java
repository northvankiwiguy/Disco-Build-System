/*******************************************************************************
 * Copyright (c) 2010 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.arapiki.disco.model;

/**
 * A Java Error that can be thrown from any part of the BuildStore mechanism
 * to indicate that a fatal error occurred. The receiver of the FatalBuildStoreError
 * must simply catch and display the error. There isn't intended to be any way to
 * recover from this type of error.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
@SuppressWarnings("serial")
public class FatalBuildStoreError extends Error {
	
	/**
	 * Create a new FataBuildStoreError, with a string message and an embedded
	 * Exception.
	 * 
	 * @param message A string message to indicate the cause of the failure.
	 * @param e An embedded Exception that caused the failure.
	 */
	public FatalBuildStoreError(String message, Throwable e) {
		super(message, e);
	}
	
	/**
	 * Create a new FataBuildStoreError, with a string message.
	 * @param message A string message to indicate the cause of the failure.
	 */
	public FatalBuildStoreError(String message) {
		super(message);
	}

}
