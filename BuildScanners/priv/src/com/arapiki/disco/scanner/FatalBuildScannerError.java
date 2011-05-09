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

package com.arapiki.disco.scanner;

/**
 * This exception indicates that an error occurred while scanning a legacy
 * build and reading it into the BuildStore.
 * @author "Peter Smith <psmith@arapiki.com>"
 */
@SuppressWarnings("serial")
public class FatalBuildScannerError extends Error {

	/**
	 * Create a new ScannerError
	 * @param message A string message to indicate the cause of the failure.
	 * @param e An embedded Exception that caused the failure.
	 */
	public FatalBuildScannerError(String message, Throwable e) {
		super(message, e);
	}
	
	/**
	 * Create a new ScannerError
	 * @param message A string message to indicate the cause of the failure.
	 */
	public FatalBuildScannerError(String message) {
		super(message);
	}
}
