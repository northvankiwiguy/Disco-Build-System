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

package com.buildml.scanner.electricanno;

import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.buildml.model.IActionMgr;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageRootMgr;
import com.buildml.scanner.FatalBuildScannerError;
import com.buildml.utils.errors.ErrorCode;
import com.buildml.utils.string.PathUtils;

/**
 * A SAX Handler class used when parsing an Electric Accelerator annotation file. The
 * XML elements in the file are parsed, and the associated data is added into the
 * BuildStore. The most relevant parts of the XML structure are:
 * <p>
 * &lt;job type="..."&gt;<br>
 * ...<br>
 *   &lt;op type="..." file="..." /&gt;<br>
 * ...<br>
 * &lt;/job&gt;<br>
 * <p>
 * For each job, we extract the details of each file access (read, write or create), along
 * with the file's name.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
/* package */ class ElectricAnnoSAXHandler extends DefaultHandler {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/** The command argv StringBuffer starts out this size. */
	private static final int INITIAL_COMMAND_SB_SIZE = 256;

	/**
	 * Set if we're parsing the content within a &lt;job&gt;...&lt;/job&gt; pair of elements. This is used 
	 * for optimizing the decision making in our code.
	 */
	private boolean withinJob = false;
	
	/** Set if we're parsing the content within a &lt;argv&gt;...&lt;/argv&gt; pair of elements. */
	private boolean withinArgv = false;
	
	/**
	 * The command line for this job. This is a StringBuffer, since command lines are often
	 * multiline and we append to them as we seen new &lt;argv&gt; tags.
	 */
	StringBuffer commandArgv;
	
	/** The ActionMgr object associated with this BuildStore. */
	private IActionMgr actionMgr;
	
	/** The FileMgr object associated with this BuildStore. */
	private IFileMgr fileMgr;

	/** The PackageRootMgr object associated with this BuildStore. */
	private IPackageRootMgr pkgRootMgr;

	/** The set of files that have been read in the current &lt;job&gt;. */
	private ArrayList<String> filesRead;
	
	/** The set of files that have been written in the current &lt;job&gt;. */	
	private ArrayList<String> filesWritten;
	
	/** The ID of the current action's parent action. */
	private int currentParentAction;
	
	/** The ID of the action we most recently processed. */
	private int mostRecentAction;
	
	/** Our stack of parent actions, recording our progress through the &lt;make&gt; &lt;/make&gt; tags. */
	private ArrayList<Integer> actionStack;
	
	/** The file system directory in which the current action was performed. */
	private int currentDirId;
	
	/** Maintain a stack of directories, pushing and popping as we encounter &lt;make&gt; and &lt;/make&gt;. */
	private ArrayList<Integer> directoryStack;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Instantiate a new ElectricAnnoSAXHandler object, with a reference
	 * to the BuildStore to be populated.
	 * 
	 * @param buildStore The BuildStore object to be populated.
	 */
	public ElectricAnnoSAXHandler(IBuildStore buildStore) {
		actionMgr = buildStore.getActionMgr();
		fileMgr = buildStore.getFileMgr();
		pkgRootMgr = buildStore.getPackageRootMgr();
		
		/* create a stack to remember the hierarchy of actions */
		actionStack = new ArrayList<Integer>();
		
		/* To start with, all actions we encounter are children of the root action */
		mostRecentAction = currentParentAction = actionMgr.getRootAction("");
		actionStack.add(currentParentAction);
		
		/* we'll also need to track our current directory */
		directoryStack = new ArrayList<Integer>();
		directoryStack.add(pkgRootMgr.getRootPath("root"));
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Handle occurrences of an element start tag. This method is invoked by the SAX Parser
	 * whenever a new element start tag (e.g. &lt;job&gt;) is identified. See the definition of
	 * import org.xml.sax.helpers.DefaultHandler for parameter details.
	 */
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {
		
		/*
		 * The <job> element contains the most interesting data, we'll set "withinJob" to true
		 * if we're within a <job> </job> pair.
		 */
		if (localName.equals("job")) {
			
			/* we only care about jobs of type "parse" or "rule". */
			String jobType = atts.getValue("type");
			if ((jobType != null) && (jobType.equals("parse") || jobType.equals("rule"))){
				withinJob = true;
				
				/* 
				 * Create an empty pair of sets for recording files that this job has read,
				 * or that this job has written.
				 */
				filesRead = new ArrayList<String>();
				filesWritten = new ArrayList<String>();
			}
		}
		
		/*
		 * else, the <make> tag tells us the nesting relationship between actions.
		 */
		else if (localName.equals("make")) {
			String cwd = atts.getValue("cwd");
			
			/* record the <make>'s current directory */
			currentDirId = fileMgr.addDirectory(cwd);
			if (currentDirId == ErrorCode.BAD_PATH) {
				throw new FatalBuildScannerError("Unable to register new directory in database: " + cwd);
			}
			directoryStack.add(currentDirId);
								
			/* push our existing state on a stack, effectively changing our current parent action */
			actionStack.add(Integer.valueOf(mostRecentAction));
			currentParentAction = mostRecentAction;
		}
		
		/*
		 * If we're within a <job> </job> pair, do some extra processing.
		 */
		if (withinJob) {
			
			/*
			 * We want to know which files are accesses. These are specified
			 * within the <op> element.
			 */
			if (localName.equals("op")) {
				
				String opType = atts.getValue("type");
				String file = atts.getValue("file");
				String isDir = atts.getValue("isdir");
				file = PathUtils.normalizeAbsolutePath(file);
				
				/* for files (not directories) that are read or written... */
				if ((opType != null) && (isDir == null)) {
					
					/* this file was read by the job */
					if (opType.equals("read")){
						filesRead.add(file);
					}
					
					/* this file was written by the job */
					else if (opType.equals("create")) {
						filesWritten.add(file);
					}
					
					/* this file was renamed by the job */
					else if (opType.equals("rename")) {
						String otherFile = atts.getValue("other");
						otherFile = PathUtils.normalizeAbsolutePath(otherFile);
						filesWritten.remove(otherFile);
						filesWritten.add(file);
						// TODO: does this work if otherFile was added multiple times?
					}
				}
			}
			
			/* else, handle command line arguments that appear within <argv> </argv> */
			else if (localName.equals("argv")) {
				withinArgv = true;
			}
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Handle occurrences of an element end tag. This method is invoked by the SAX Parser
	 * whenever a new element end tag (e.g. &lt;/job&gt;) is identified. See the definition of
	 * import org.xml.sax.helpers.DefaultHandler for parameter details.
	 */
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		
		/* 
		 * If we've found a </job> element tag, so we've parsed all the information related
		 * to this job. To finish up, create a new build action and populate it with all the
		 * file access information we acquired.. 
		 */
		if (withinJob && localName.equals("job")) {
			
			withinJob = false;
			
			/* 
			 * We've seen all the details of this job, and if it's an interesting
			 * job, then we'll add it as a build action. We currently define "interesting"
			 * as whether there's a command argv associated with it.
			 */
			if (commandArgv != null){
				
				String argvString = commandArgv.toString();
				commandArgv = null;	
				
				int newActionId = actionMgr.addAction(currentParentAction, currentDirId, argvString);
				
				/* record the ID of this action, since it might be the new parent action soon */
				mostRecentAction = newActionId;

				/* add all the file reads to the build action */
				for (String file : filesRead) {
					int newFileId = fileMgr.addFile(file);
					if (newFileId != ErrorCode.BAD_PATH) {
						actionMgr.addFileAccess(newActionId, newFileId, OperationType.OP_READ);
					} else {
						throw new FatalBuildScannerError("Unable to register new file in database: " + file);
					}
				}

				/* add all the file writes to the build action */
				for (String file : filesWritten) {
					int newFileId = fileMgr.addFile(file);
					if (newFileId != ErrorCode.BAD_PATH) {
						actionMgr.addFileAccess(newActionId, newFileId, OperationType.OP_WRITE);
					} else {
						throw new FatalBuildScannerError("Unable to register new file in database: " + file);
					}
				}
			}
		}
		
		/* have we seen a </argv>? */
		else if (withinArgv && localName.equals("argv")) {
			withinArgv = false;
			if (commandArgv != null) {
				commandArgv.append('\n');
			}
		}
		
		/* else, we're done with this nesting level of actions */
		else if (localName.equals("make")) {
			
			/* 
			 * Restore the current parent's ID and most recent action ID for the
			 * parent's <job>
			 */
			int actionStackSize = actionStack.size();
			if (actionStackSize == 0) {
				throw new FatalBuildScannerError("Too many </make> tags in annotation file");
			}
			currentParentAction = actionStack.get(actionStackSize - 2);
			mostRecentAction = actionStack.remove(actionStackSize - 1);
			
			/* restore the previous job's current directory */
			int dirStackSize = directoryStack.size();
			directoryStack.remove(dirStackSize - 1);
			currentDirId = directoryStack.get(dirStackSize - 2);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Handle occurrences of raw character in an XML stream. That is, the text that appears
	 * between the start and end tags. For example, &lt;job&gt;this is the text&lt;/job&gt;.
	 * See the definition of import org.xml.sax.helpers.DefaultHandler for parameter details.
	 */
	@Override
	public void characters(char[] ch, int start, int length) 
			throws SAXException {
		
		if (withinArgv) {
			if (commandArgv == null) {
				commandArgv = new StringBuffer(INITIAL_COMMAND_SB_SIZE);
			}
			commandArgv.append(ch, start, length);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
}
