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

package com.buildml.utils.os;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * Special purpose Thread class for capturing a stream's output and storing it into
 * a StringBuffer. Used primarily by the executeShellCmd() method.
 * 
 * This type of Thread is necessary to handle multiplexing of multiple
 * streams (not supported by basic java.io.* calls).
 */
public class StreamToStringBufferWorker extends Thread {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/** The Reader we'll used to read the stream on a line-by-line basis. */
	private BufferedReader reader;
	
	/** The StringBuffer we'll write into. */
	private StringBuffer sb;
	
	/** Set if we should echo the stdout/stderr of the sub-process to our own stdout/stderr. */
	private boolean echoToOutput;
	
	/** If echoToOutput is true, the stream we should echo to. */
	private PrintStream outStream;
	
	/** Set if we should save the stdout/stderr of the sub-process to buffers. */
	private boolean saveToBuffer;
	
	/**
	 * If we encounter an exception in this Thread, we'll save it and return it to
	 * whoever calls the getString() method.
	 */
	private IOException savedException = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new StreamToStringBufferWorker object.
	 * 
	 * @param str The InputStream to read from.
	 * @param saveToBuffer Set if the stdout and stderr should be saved in buffers.
	 * @param outStream if not null, the stream we should echo to.
	 */
	public StreamToStringBufferWorker(InputStream str, 
			boolean saveToBuffer, PrintStream outStream) {
		reader = new BufferedReader(new InputStreamReader(str));
		sb = new StringBuffer();
		this.saveToBuffer = saveToBuffer;
		this.outStream = outStream;
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
				if (saveToBuffer) {
					sb.append(line);
					sb.append('\n');
				}
				if (outStream != null) {
					outStream.println(line);
				}
			}
		} catch (IOException e) {
			savedException = e;
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Fetch the stream's content in String form.
	 * 
	 * @return The String content.
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
