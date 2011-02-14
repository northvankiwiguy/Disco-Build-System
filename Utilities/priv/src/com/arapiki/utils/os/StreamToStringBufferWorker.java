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

package com.arapiki.utils.os;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Special purpose Thread class for capturing a stream's output and storing it into
 * a StringBuffer. This type of Thread is necessary to handle multiplexing of multiple
 * streams (not supported by basic java.io.* calls).
 */
public class StreamToStringBufferWorker extends Thread {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/* the Reader we'll used to read the stream on a line-by-line basis. */
	private BufferedReader reader;
	
	/* the StringBuffer we'll write into */
	private StringBuffer sb;
	
	/* 
	 * If we encounter an exception in this Thread, we'll save it and return it to
	 * whoever calls the getString() method.
	 */
	private IOException savedException = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new StreamToStringBufferWorker object.
	 * @param str The InputStream to read from.
	 */
	public StreamToStringBufferWorker(InputStream str) {
		reader = new BufferedReader(new InputStreamReader(str));
		sb = new StringBuffer();
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * This is the entry point for the Thread. When run() terminates, we've either reached
	 * the end of the stream, or have encountered an IOException.
	 */
	public void run() {
		
		String line;
		try {
			while ((line = reader.readLine()) != null){
				sb.append(line);
				sb.append('\n');
			}
		} catch (IOException e) {
			savedException = e;
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Fetch the stream's content in String form.
	 * @return The String content
	 * @throws IOException If this thread encountered an IOException during the reading process.
	 */
	public String getString() throws IOException {
		
		/* did an IOException happen during reading? If so, report it now */
		if (savedException != null) {
			throw savedException;
		}
		return sb.toString();
	}

	/*-------------------------------------------------------------------------------------*/
}
