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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * A subclass of FileInputStream that allows us to monitor progress as we read through
 * a file. This is extremely useful for long-running applications that read/parse
 * very large files. It's important to give the end user some feedback on how much of
 * the file has been processed.
 * 
 * This class is intended to be used as a drop-in replacement for FileInputStream.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class ProgressFileInputStream extends FileInputStream {
	
	/*=====================================================================================*
	 * FIELDS/TYPE
	 *=====================================================================================*/
	
	/** The total length of the file we're reading. */
	private long fileLength;
	
	/** Our current position within the file. */
	private long filePos;
	
	/** The thread that'll keep the caller updated on the progress of reading the file. */
	private Thread progressReporter;
	
	/** Flag set to true when the file has been completely read. */
	private boolean fileDone = false;
	
	/*=====================================================================================*
	 * NESTED CLASSES
	 *=====================================================================================*/
	
	/**
	 * A Thread class that'll report the current progress of reading through
	 * a ProgressFileInputStream. This thread continues until it's terminated, which is usually
	 * when the file is closed.
	 */
	private class ProgressReporterThread extends Thread {

		/** The number of seconds between each successive progress report. */
		private int intervalInSeconds;
		
		/** The callback object to invoke. */
		private ProgressFileInputStreamListener listener;
		
		/**
		 * Create a new ProgressReporterThread object.
		 * @param listener The listener object that'll receive our progress updates.
		 * @param intervalInSeconds The number of seconds between consecutive progress reports.
		 */
		public ProgressReporterThread(ProgressFileInputStreamListener listener, 
				int intervalInSeconds) {
			this.intervalInSeconds = intervalInSeconds;
			this.listener = listener;
		}
		
		/*-------------------------------------------------------------------------------------*/

		/**
		 * The main loop of the listener thread. This method invokes the listener's progress()
		 * method on a periodic basis.
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			
			/* continuously report progress, until the file is closed */
			do {
				/* sleep for the desired interval */
				try {
					sleep(intervalInSeconds * 1000);
				} catch (InterruptedException e) {
					/* do nothing */
				}
				/* report */
				listener.progress(filePos, fileLength, (int)(100 * (float)filePos / (float)fileLength));
			} while (!fileDone);
			
			/* just to make sure we report 100% */
			listener.progress(fileLength, fileLength, 100);
			
			/* this will help the progress meter know when to stop showing results */
			listener.done();
		}
	}
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new ProgressFileInputStream object. This is identical to a FileInputStream object,
	 * except that a listener object can be attached to the stream in order to monitor progress.
	 * 
	 * @param name The name of the file to be read.
	 * @param listener The ProgressFileInputStreamListener object that will be notified of progress.
	 * @param intervalInSeconds The interval (in seconds) between consecutive progress reports.
	 * @throws FileNotFoundException If the file wasn't found.
	 */
	public ProgressFileInputStream(String name, ProgressFileInputStreamListener listener,
			int intervalInSeconds) throws FileNotFoundException {
	
		/* most of the work is delegated to the parent class */
		super(name);
		
		/* determine the total length of the file */
		File file = new File(name);
		fileLength = file.length();
		
		/* the position counter starts at 0 */
		filePos = 0;
		
		/* start our progress reporter thread */
		progressReporter = new ProgressReporterThread(listener, intervalInSeconds);
		progressReporter.start();
	}

	/*=====================================================================================*
	 * PUBLIC METHODS (all are overrides of standard FileInputStream methods).
	 *=====================================================================================*/
	
	/**
 	 * A wrapper around the standard read() method.
	 * @see java.io.FileInputStream#read()
	 */
	@Override
	public int read() throws IOException {
		int byteRead = super.read();
		if (byteRead != -1) {
			filePos++;
		}
		return byteRead;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * A wrapper around the standard read() method.
	 * @see java.io.FileInputStream#read(byte[], int, int)
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int lengthRead = super.read(b, off, len);
		if (lengthRead > 0) {
			filePos += lengthRead;
		}
		return lengthRead;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * A wrapper around the standard read() method.
	 * @see java.io.FileInputStream#read(byte[])
	 */
	@Override
	public int read(byte[] b) throws IOException {
		int lengthRead = super.read(b);
		if (lengthRead > 0) {
			filePos += lengthRead;
		}
		return lengthRead;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * A wrapper around the standard skip() method.
	 * @see java.io.FileInputStream#skip(long)
	 */
	@Override
	public long skip(long n) throws IOException {
		long bytesSkipped = super.skip(n);
		if (bytesSkipped > 0) {
			filePos += bytesSkipped;
		}
		return bytesSkipped;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * A wrapper around the standard close() method.
	 * @see java.io.FileInputStream#close()
	 */
	@Override
	public void close() throws IOException {
		fileDone = true;
		super.close();
	}

	/*-------------------------------------------------------------------------------------*/
}
