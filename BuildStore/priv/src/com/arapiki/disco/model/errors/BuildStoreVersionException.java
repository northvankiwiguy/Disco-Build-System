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

package com.arapiki.disco.model.errors;

/**
 * Exception indicating that an existing BuildStore database that
 * we're trying to open has the wrong schema version. The database
 * will need to be regenerated or upgraded in order to be used by
 * this version of Disco.
 *  
 * @author "Peter Smith <psmith@arapiki.com>"
 */
@SuppressWarnings("serial")
public class BuildStoreVersionException extends Exception {

	/**
	 * Create a new BuildStoreVersionException, with a string
	 * message to explain the problem in more detail.
	 * 
	 * @param message A message explaining the problem in more detail.
	 */
	public BuildStoreVersionException(String message) {
		super(message);
	}
}
