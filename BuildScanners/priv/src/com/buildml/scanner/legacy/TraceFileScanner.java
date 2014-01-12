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

package com.buildml.scanner.legacy;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import com.buildml.model.IActionMgr;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IFileMgr.PathType;
import com.buildml.scanner.FatalBuildScannerError;
import com.buildml.utils.errors.ErrorCode;
import com.buildml.utils.string.ShellCommandUtils;

/**
 * This class parses the output from a CFS (capture file system)
 * trace file (by default, "cfs.trace") and creates a corresponding BuildStore.
 * Typically this class would only be instantiated by the LegacyBuildScanner class.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
/* package */ class TraceFileScanner {
	
	/*
	 * Important note: the content of this file must be kept in sync with the 
	 * interposer functions in CFS. If any changes are made to the data being
	 * stored in the trace buffer, the follow methods must also be updated.
	 */
	
	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/
	
	/* 
	 * Each entry in the trace buffer has a corresponding tag to state what
	 * operation is being traced. See trace_file_format.h (in CaptureFS)
	 * for details.
	 */
	
	/** The end of file has been reached. Note this tag isn't actually stored in the trace file. */
	private static final int TRACE_FILE_EOF = -1;
	
	/** CFS is registering the existence of a source file. */
	private static final int TRACE_FILE_REGISTER = 1;
	
	/** A file has been opened for write. */
	private static final int TRACE_FILE_WRITE = 2;
	
	/** A file has been opened for read. */
	private static final int TRACE_FILE_READ = 3;

	/** A file has been opened for update. */
	private static final int TRACE_FILE_MODIFY = 4;

	/** A file has been deleted. */
	private static final int TRACE_FILE_DELETE = 5;
	
	/** A file has been renamed. */
	private static final int TRACE_FILE_RENAME	= 6;
	
	/** A new symlink has been created. */
	private static final int TRACE_FILE_NEW_LINK = 7;
	
	/** A new program has been executed. */
	private static final int TRACE_FILE_NEW_PROGRAM = 8;

	/** A directory has been opened for write. */
	private static final int TRACE_DIR_WRITE = 9;
	
	/** A directory has been opened for read. */
	private static final int TRACE_DIR_READ = 10;

	/** A directory has been opened for update. */
	private static final int TRACE_DIR_MODIFY = 11;

	/** A directory has been deleted. */
	private static final int TRACE_DIR_DELETE = 12;

	/** 
	 * When reading data from the trace file, the amount of data we should read each time.
	 */
	private static final int readBufferMax = 65536;
	
	/** The input stream for reading the trace file. */
	private InputStream inputStream;
	
	/** Tracks the current position within the input stream */
	private int traceFilePos;
	
	/** The BuildStore we should add trace file information to (null = don't add to BuildStore). */
	private IBuildStore buildStore;
	
	/** The ActionMgr object contained within our BuildStore (null = don't add to BuildStore). */
	private IActionMgr actionMgr;
	
	/** The FileMgr object contained within our BuildStore (null = don't add to BuildStore). */
	private IFileMgr fileMgr;
	
	/** The PrintStream to write debug information to (null = don't write debug information). */
	private PrintStream debugStream;
	
	/** The amount of debug output to provide (0, 1 or 2). */
	private int debugLevel;
	
	/** 
	 * An in-memory buffer of bytes read from the trace file. This will
	 * be at most readBufferMax bytes in size.
	 */
	private byte[] readBuffer;
	
	/** The index of the next byte within readBuffer to be processed. */
	private int bufferOffset = 0;
	
	/** The number of bytes still to be processed from readBuffer. */
	private int bytesRemaining = 0;
	
	/** 
	 * Mapping between the process numbers that CFS (and the trace file) provides us with
	 * to the "action ID" numbers that BuildStore uses. As we encounter new processes, and
	 * add them to the BuildStore, we'll be allocated corresponding build action IDs.
	 */
	private HashMap<Integer, Integer> processToActionMap = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Instantiate a new TraceFileScanner object. The trace file is opened, ready
	 * to have trace data read from it.
	 * 
	 * @param fileName Name of the trace file to read.
	 * @param buildStore The BuildStore to add the trace file information to (possibly null).
	 * @param debugStream The PrintStream to write debug information to (possibly null).
	 * @param debugLevel The amount of debug information desired (0, 1 or 2).
	 * @throws IOException If opening the file fails.
	 */
	/* package */ TraceFileScanner(
			String fileName, 
			IBuildStore buildStore, 
			PrintStream debugStream,
			int debugLevel) throws IOException {
		
		/* save the BuildStore and PrintStream, so that other methods can use them */
		this.buildStore = buildStore;
		this.debugStream = debugStream;
		this.debugLevel = debugLevel;
		
		/* these objects are part of our BuildStore object */
		if (buildStore != null) {
			this.actionMgr = buildStore.getActionMgr();
			this.fileMgr = buildStore.getFileMgr();
		}
		
		/* 
		 * Create a map between CFS process numbers and BuildStore action numbers.
		 * Insert the parent/root ID numbers to signify the first process/action 
		 */
		processToActionMap = new HashMap<Integer, Integer>();
		processToActionMap.put(Integer.valueOf(0), actionMgr.getRootAction("root"));
		
		/* set up input stream, and variables for reading it */
		inputStream = new GZIPInputStream(new FileInputStream(fileName));
		readBuffer = new byte[readBufferMax];
		bufferOffset = 0;
		bytesRemaining = 0;
		traceFilePos = 0;
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Parse the whole trace file and process the data. As each tag is read, the values 
	 * associated with that tag are also fetched and processed. The appropriate methods
	 * are then called on the BuildStore (and it's managers) to reconstruct the build process.
	 * 
	 * @throws IOException If an I/O operation occurs while reading the file.
	 */
	public void parse() throws IOException {
		
		/* we'll do a lot of writing to the database, speed up access in favour of safety. */
		boolean prevState = buildStore.setFastAccessMode(true);
		
		String fileName = null;
		boolean eof = false;
		do {
			
			/* all records start with a tag, followed by a process number */
			int tagTraceFilePos = traceFilePos;
			int tag = getTag();
			int processNum = getInt();
			
			/* do something different for each tag */
			switch (tag) {
			case TRACE_FILE_EOF:
				eof = true;
				break;
				
			case TRACE_FILE_REGISTER:
				fileName = getString();
				debugln(1, "Registered file: " + fileName);
				break;
				
			case TRACE_FILE_WRITE:
			case TRACE_DIR_WRITE:
				fileName = getString();
				addPathAccess(fileName, processNum, OperationType.OP_WRITE,
						(tag == TRACE_FILE_WRITE) ? PathType.TYPE_FILE : PathType.TYPE_DIR);
				break;

			case TRACE_FILE_READ:
			case TRACE_DIR_READ:
				fileName = getString();
				addPathAccess(fileName, processNum, OperationType.OP_READ,
						(tag == TRACE_FILE_READ) ? PathType.TYPE_FILE : PathType.TYPE_DIR);
				break;
				
			case TRACE_FILE_MODIFY:
			case TRACE_DIR_MODIFY:
				fileName = getString();
				addPathAccess(fileName, processNum, OperationType.OP_MODIFIED,
						(tag == TRACE_FILE_MODIFY) ? PathType.TYPE_FILE : PathType.TYPE_DIR);
				break;
				
			case TRACE_FILE_DELETE:
			case TRACE_DIR_DELETE:
				fileName = getString();
				addPathAccess(fileName, processNum, OperationType.OP_DELETE,
						(tag == TRACE_FILE_DELETE) ? PathType.TYPE_FILE : PathType.TYPE_DIR);
				break;

			case TRACE_FILE_RENAME:
				break;

			case TRACE_FILE_NEW_LINK:
				break;

			case TRACE_FILE_NEW_PROGRAM:
				addBuildAction(processNum);
				break;
				
			default:
				throw new FatalBuildScannerError("Invalid tag in trace file: " + tag +
						" at trace file position " + tagTraceFilePos);
			}
				
		} while (!eof);
		
		buildStore.setFastAccessMode(prevState);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Close the trace file.
	 * 
	 * @throws IOException If closing the file fails.
	 */
	public void close() throws IOException {
		inputStream.close();	
	}
		
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Create a new build action in the BuildStore.
	 * 
	 * @param processNum The Unix process ID of the scanned process.
	 * @throws IOException A problem occurred while reading the trace file.
	 */
	private void addBuildAction(int processNum) throws IOException {
		
		/* which process spawned this new process? */
		int parentProcessNum = getInt();
		
		/* what was the current working directory for the process */
		String cwd = getString();
		
		debug(1, "New Process " + processNum + " (parent " + parentProcessNum + 
				", directory " + cwd + ") - ");
		
		/* fetch all the command line arguments */
		int argCount = getInt();
		StringBuffer commandArgs = new StringBuffer();
		boolean first = true;
		while (argCount-- != 0) {
			String arg = getString();
			if (first) {
				first = false; /* no space separator required */
			} else {
				commandArgs.append(' ');  /* put a space between arguments */
			}
			String quotedArg = ShellCommandUtils.shellEscapeString(arg);
			commandArgs.append(quotedArg);
		}
		String command = commandArgs.toString();
		
		debugln(1, command);
		debugln(2, "Environment");
		while (true) {
			String env = getString();
			if (env.isEmpty()){ break; }
			debugln(2, " - " + env);
		}
		
		/* Update the BuildStore */
		if (buildStore != null) {
			
			/* map the process number (from cfs) into the BuildStore's actionId */
			Integer parentActionId = getActionId(parentProcessNum);
		
			/* fetch the current working directory ID */
			int actionDirId = fileMgr.addDirectory(cwd);
			if (actionDirId == ErrorCode.BAD_PATH){
				throw new FatalBuildScannerError("Invalid current working directory: " + cwd);
			}
			
			/* add the new action to the build store */
			int newActionId = actionMgr.addShellCommandAction(parentActionId, actionDirId, command);
			
			/* associate CFS's process number with BuildStore's actionID */
			setActionId(processNum, newActionId);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add a new mapping from a CFS process number to a BuildStore actionID.
	 * 
	 * @param processNum The CFS process number.
	 * @param newActionId The corresponding BuildStore actionId.
	 * @throws FatalBuildScannerError If there's already a mapping for this process.
	 */
	private void setActionId(int processNum, int newActionId) {
		
		if (processToActionMap.get(Integer.valueOf(processNum)) != null){
			throw new FatalBuildScannerError("Process number " + processNum + 
					" appears to have been created twice.");
		}
		processToActionMap.put(Integer.valueOf(processNum), Integer.valueOf(newActionId));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a CFS process number, translate it into a BuildStore action number. 
	 * 
	 * @param processNum The CFS process number.
	 * @return The corresponding BuildStore action number.
	 * @throws FatalBuildScannerError Ff the process-to-action mapping is unknown.
	 */
	private int getActionId(int processNum) {
		Integer actionId = processToActionMap.get(Integer.valueOf(processNum));
		if (actionId == null){
			throw new FatalBuildScannerError("Process number " + processNum + 
					" does not have an assigned action number");
		}
		return actionId;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Record a file access in the BuildStore, based on a file access that was noted
	 * in the trace file.
	 * 
	 * @param fileName The name of the file that was accessed.
	 * @param processNum The Unix process ID of the process that did the accessing.
	 * @param direction The type of access (read, write).
	 * @param type Is this a file, directory or symlink.
	 */
	private void addPathAccess(String fileName, int processNum, OperationType direction, PathType type) {

		/* debug output */
		String opString;
		switch(direction) {
		case OP_READ:
			opString = "reading";
			break;
		case OP_WRITE:
			opString = "writing";
			break;
		case OP_MODIFIED:
			opString="modified";
			break;
		case OP_DELETE:
			opString="deleted";
			break;
		default:
			opString="unknown operation";
			break;
		}
		debugln(1, "Process " + processNum + " " + opString + " " +
				fileName + 
					" (" + ((type == PathType.TYPE_FILE) ? "File" :
						(type == PathType.TYPE_DIR) ? "Directory" : "Symlink") + ")");
		
		/* get the BuildStore actionId for the current process */
		int actionId = getActionId(processNum);
		
		/* get the BuildStore fileId */
		int fileId;
		if (type == PathType.TYPE_FILE) {
			fileId = fileMgr.addFile(fileName);
		} else if (type == PathType.TYPE_DIR) {
			fileId = fileMgr.addDirectory(fileName);
		} else {
			fileId = fileMgr.addSymlink(fileName);
		}
		if (fileId < 0){
			String msg = "Failed to add file: " + fileName + " to the BuildML database.";
			int existingPathId = fileMgr.getPath(fileName);
			if (existingPathId >= 0) {
				PathType existingType = fileMgr.getPathType(existingPathId);
				if (existingType != type) {
					msg += "\nThere is already a path with this name, but with a " +
				           "different type (file versus directory).\n";
					msg += "You must modify the legacy build system to fix this problem.";
				}
			}
			
			throw new FatalBuildScannerError(msg);
		}
		
		/* add the file access information to the build store */
		actionMgr.addFileAccess(actionId, fileId, direction);
		
		/* 
		 * If the file we are accessing has been trashed, that's because it was a temporary
		 * file that's no longer required. Because temporary file names can sometimes be
		 * reused, we need to permanently empty the trash, otherwise we can't re-add the
		 * same path.
		 */
		if (fileMgr.isPathTrashed(fileId)) {
			buildStore.emptyTrash();
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Read a single byte from the trace file, and return it as an
	 * integer.
	 * 
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
		
		/* track the position within the input stream (regardless of buffer location) */
		traceFilePos++;
		
		/* Java doesn't have unsigned bytes, so do the adjustment */
		if (val < 0) {
			val += 256;
		}
		return val;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Fetch a trace file tag from the trace file.
	 * 
	 * @return The next tag in the file (e.g. TRACE_FILE_READ).
	 * @throws IOException If something fails when reading the file.
	 */
	private int getTag() throws IOException {
		return getByte();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Fetch a NUL-terminated string from the trace file.
	 * 
	 * @return The string that was read from the trace file.
	 * @throws IOException If something fails when reading the file. For example,
	 * if the EOF is reached before a NUL character is seen.
	 */
	private String getString() throws IOException {
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
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Fetch a 4-byte little-endian integer from the trace file.
	 * 
	 * @return The integer that was read from the trace file.
	 * @throws IOException If something fails while reading the integer.
	 */
	private int getInt() throws IOException {
		
		/* TODO: optimize if this ends up being slow */
		int dig1 = getByte();
		int dig2 = getByte();
		int dig3 = getByte();
		int dig4 = getByte();
				
		/* numbers are stored in little-endian order */
		return (dig4 << 24) | (dig3 << 16) | (dig2 << 8) | dig1;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Like println(), but display the message to the debug stream (if defined). Only
	 * display the message if this message's debug level is less than or equal to the
	 * overall debug level setting.
	 * 
	 * @param level The debug level for this message.
	 * @param message The message to be displayed on the debug stream.
	 */
	private void debugln(int level, String message) {
		if ((debugStream != null) && (level <= debugLevel)) {
			debugStream.println(message);
		}		
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Like print(), but display the message to the debug stream (if defined).
	 * 
 	 * @param level The debug level for this message.
	 * @param message The message to be displayed on the debug stream.
	 */
	private void debug(int level, String message) {
		if ((debugStream != null) && (level <= debugLevel)) {
			debugStream.print(message);
		}		
	}

	/*-------------------------------------------------------------------------------------*/
}
