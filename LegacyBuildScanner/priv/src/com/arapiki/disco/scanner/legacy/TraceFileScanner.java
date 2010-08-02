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

package com.arapiki.disco.scanner.legacy;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;

/**
 * The TraceFileScanner class parses the output from the cfs (component file system)
 * and creates a new BuildStore.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TraceFileScanner {
	
	/*
	 * Important note: the content of this file must be kept in sync with the 
	 * interposer functions in cfs. If any changes are made to the data being
	 * stored in the trace buffer, the follow methods must also be updated.
	 */
	
	/* 
	 * Each entry in the trace buffer has a corresponding tag to state what
	 * operation is being traced. See trace_file_format.h (in ComponentFS)
	 * for details.
	 */
	
	/** The end of file has been reached - note, this isn't actually stored in the trace file */
	private final int TRACE_FILE_EOF 			= -1;
	
	/** cfs is registering the existence of a source file */
	private final int TRACE_FILE_REGISTER 		= 1;
	
	/** a file write operation has taken place */
	private final int TRACE_FILE_WRITE 		= 2;
	
	/** a file read operation has taken place */
	private final int TRACE_FILE_READ 			= 3;
	
	/** a file has been removed */
	private final int TRACE_FILE_REMOVE 		= 4;
	
	/** a file has been renamed */
	private final int TRACE_FILE_RENAME		= 5;
	
	/** a new symlink has been created */
	private final int TRACE_FILE_NEW_LINK		= 6;
	
	/** a new program has been executed */
	private final int TRACE_FILE_NEW_PROGRAM 	= 7;
	
	/**
	 * When reading data from the trace file, how much data should we 
	 * read at a time.
	 */
	private final int readBufferMax = 65536;
	
	/** The input stream for reading the trace file */
	private InputStream inputStream;
	
	/** 
	 * an in-memory buffer of bytes read from the trace file. This will
	 * be at most readBufferMax bytes in size.
	 */
	private byte[] readBuffer;
	
	/** the index of the next byte within readBuffer to be processed */
	private int bufferOffset = 0;
	
	/** the number of bytes still to be processed from readBuffer */
	private int bytesRemaining = 0;

	
	/**
	 * Instantiate a new TraceFileScanner object. The trace file is opened, ready
	 * to have trace data read from it.
	 * @param fileName Name of the trace file.
	 * @throws IOException If opening the file fails.
	 */
	public TraceFileScanner(String fileName) throws IOException {
		inputStream = new GZIPInputStream(new FileInputStream(fileName));
		readBuffer = new byte[readBufferMax];
		bufferOffset = 0;
		bytesRemaining = 0;
	}
	
	/**
	 * Close the trace file.
	 * @throws IOException If closing the file fails.
	 */
	public void close() throws IOException {
		inputStream.close();	
	}
	
	/**
	 * Read a single byte from the trace file, and return it as an
	 * integer.
	 * @return The byte of data, or TRACE_FILE_EOF (-1) if there's no more data left.
	 * @throws IOException If anything abnormal happens when reading the data.
	 */
	private int getByte() throws IOException {
		
		/* if there's no data left in the in-memory buffer, read some more */
		if (bytesRemaining == 0){
			bytesRemaining = inputStream.read(readBuffer);
			bufferOffset = 0;
		}
		
		/* if there are no more bytes in the input stream, inform the caller */
		if (bytesRemaining == -1) {	
			return TRACE_FILE_EOF;
		}
		
		bytesRemaining--;
		int val = readBuffer[bufferOffset++];
		
		/* Java doesn't have unsigned bytes, so do the adjustment */
		if (val < 0) {
			val += 256;
		}
		return val;
	}
	
	/**
	 * Fetch a trace file tag from the trace file.
	 * @return The next tag in the file (e.g. TRACE_FILE_READ)
	 * @throws IOException If something fails when reading the file.
	 */
	public int getTag() throws IOException {
		return getByte();
	}
	
	/**
	 * Fetch a NUL-terminated string from the trace file.
	 * @return The string
	 * @throws IOException If something fails when reading the file. For example,
	 * if the EOF is reached before a NUL character is seen.
	 */
	public String getString() throws IOException {
		StringBuffer buf = new StringBuffer(256);

		while (true) {
			int val = getByte();
			
			/* a nul-byte is the end of the C-style string */
			if (val == 0) {
				break;
				
			/* but if we see an EOF in the middle of the string, error */
			} else if (val == TRACE_FILE_EOF) {
				throw new IOException("File appears to be truncated");
			}
			buf.append((char)val);
		} 
		return buf.toString();
	}
	
	/**
	 * Fetch a 4-byte little-endian integer from the trace file.
	 * @return The integer.
	 * @throws IOException If something fails while reading the integer.
	 */
	public int getInt() throws IOException {
		
		/* TODO: optimize if this ends up being slow */
		int dig1 = getByte();
		int dig2 = getByte();
		int dig3 = getByte();
		int dig4 = getByte();
				
		/* numbers are stored in little-endian order */
		return (dig4 << 24) | (dig3 << 16) | (dig2 << 8) | dig1;
	}
	
	/**
	 * Parse the whole trace file and process the data. As each tag is
	 * read, the values associated with that tag and also fetched and processed.
	 * @throws IOException If an I/O operation occurs while reading the file.
	 */
	public void parse() throws IOException {
		
		String fileName = null;
		boolean eof = false;
		do {
			
			/* all records start with a tag, followed by a process number */
			int tag = getTag();
			int process_num = getInt();
			
			/* do something different for each tag */
			switch (tag) {
			case TRACE_FILE_EOF:
				eof = true;
				break;
				
			case TRACE_FILE_REGISTER:
				fileName = getString();
				System.out.println("Registered file: " + fileName);
				break;
				
			case TRACE_FILE_WRITE:
				fileName = getString();
				System.out.println("Process " + process_num + " writing file: " + fileName);
				break;

			case TRACE_FILE_READ:
				fileName = getString();
				System.out.println("Process " + process_num + " reading file: " + fileName);
				break;

			case TRACE_FILE_REMOVE:
				break;

			case TRACE_FILE_RENAME:
				break;

			case TRACE_FILE_NEW_LINK:
				break;

			case TRACE_FILE_NEW_PROGRAM:
				int parent_process_num = getInt();
				System.out.print("New Process " + process_num + " (parent " + parent_process_num + ") -");
				
				while (true) {
					String arg = getString();
					if (arg.isEmpty()){
						break;
					}
					System.out.print(" " + arg);
				}
				System.out.println("");
				//System.out.println("Environment");
				while (true) {
					String env = getString();
					if (env.isEmpty()){
						break;
					}
					//System.out.println(" - " + env);
				}
				break;
				
			}
				
		} while (!eof);
	}
}
