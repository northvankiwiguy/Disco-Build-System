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

package com.buildml.utils.files;

/**
 * An abstract class for creating callback objects that listen to a 
 * ProgressFileInputStream object. When attached to a ProgressFileInputStream
 * object, the progress() method will be invoked on a periodic basis to 
 * indicate how much of the file has been read.
 */
public abstract class ProgressFileInputStreamListener {
	
	/**
	 * Called by the associated FileInputStream object to update us on the
	 * progress of reading through the file.
	 * 
	 * @param current The current position of the stream's file pointer.
	 * @param total The total number of bytes in the file.
	 * @param percentage The percentage completion (0% to 100%).
	 */
	public abstract void progress(long current, long total, int percentage);
	
	/** Called when the file has been completely read. */
	public abstract void done();
}
