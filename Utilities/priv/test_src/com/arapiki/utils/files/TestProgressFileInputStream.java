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

package com.arapiki.utils.files;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;

import org.junit.Test;

/**
 * Test cases for the ProgressFileInputStream class. Note that we don't spend a lot
 * of time testing failure cases, since the return values and Exceptions are passed
 * on directly from the based FileInputStream class.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestProgressFileInputStream {

	/** The most recently reported file position, total length, and percentage */
	private long currentPos;
	private long length;
	private int percentComplete;
	
	/** indicates when the file read is complete */
	private boolean done = false;
	
	/**
	 * Test the ProgressFileInputStream methods. There are three different
	 * read() methods that we've overidden, so we test each of them.
	 * @throws Exception
	 */
	@Test
	public void testProgress() throws Exception {
		
		/* Create a temporary file and put content into it */
		File tempFile = File.createTempFile("progressFile", null);
		FileWriter fWrite = new FileWriter(tempFile);
		fWrite.write("This is the string that will be written to a file\n");
		fWrite.write("This is the second line that I'll read\n");
		fWrite.close();
		
		/*
		 * Create a listener that does nothing but save the values so we can examine
		 * them later.
		 */
		ProgressFileInputStreamListener listener = new ProgressFileInputStreamListener() {
			@Override
			public void progress(long current, long total, int percentage) {
				currentPos = current;
				length = total;
				percentComplete = percentage;
			}

			@Override
			public void done() {
				done = true;
			}
		};
		
		/* create a new object-under-test. This will report progress every two seconds */
		ProgressFileInputStream in = new ProgressFileInputStream(tempFile.toString(), listener, 2);
		done = false;
		
		/* wait for the first reporting cycle */
		Thread.sleep(3000);
		assertEquals(0, currentPos);
		assertEquals(89, length);
		assertEquals(0, percentComplete);
		
		/* read a single byte, then wait for the second cycle */
		in.read();
		Thread.sleep(2000);
		assertEquals(1, currentPos);
		assertEquals(89, length);
		assertEquals(1, percentComplete);
		
		/* read 10 bytes */
		byte tenBytes[] = new byte[10];
		in.read(tenBytes);
		Thread.sleep(2000);
		assertEquals(11, currentPos);
		assertEquals(89, length);
		assertEquals(12, percentComplete);

		/* read 5 more bytes, into the center of the array */
		in.read(tenBytes, 3, 5);
		Thread.sleep(2000);
		assertEquals(16, currentPos);
		assertEquals(89, length);
		assertEquals(17, percentComplete);
		
		/* close the file. We should be at 100% now */
		in.close();
		Thread.sleep(2000);
		assertEquals(89, currentPos);
		assertEquals(89, length);
		assertEquals(100, percentComplete);
		assertTrue(done);
		
		/* clean up */
		tempFile.delete();
	}
}
