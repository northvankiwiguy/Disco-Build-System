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

package com.arapiki.disco.scanner.electricanno;

import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.BuildTasks;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.BuildTasks.OperationType;
import com.arapiki.disco.scanner.FatalBuildScannerError;
import com.arapiki.utils.string.PathUtils;

/**
 * A SAX Handler class for use when parsing an Electric Accelerator annotation file. The
 * XML elements in the file are parsed, with the data being added into the BuildStore. The
 * most relevant parts of the XML structure are:
 * 
 * <job type="...">
 * ...
 *   <op type="..." file="..." />
 * ...
 * </job>
 * 
 * For each job, we extract the details of each file access (read, write or create), along
 * with the file's name.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
/* package */ class ElectricAnnoSAXHandler extends DefaultHandler {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/*
	 * Are we parsing the content within a <job>...</job> pair of elements? This is used 
	 * for optimizing the decision making in our code.
	 */
	private boolean withinJob = false;
	
	/** the BuildTasks object associated with this BuildStore */
	private BuildTasks buildTasks;
	
	/** the FileNameSpaces object associated with this BuildStore */
	private FileNameSpaces fns;
	
	/** the set of files that have been read in the current <job> */
	private ArrayList<String> filesRead;
	
	/** the set of files that have been written in the current <job> */	
	private ArrayList<String> filesWritten;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Instantiate a new ElectricAnnoSAXHandler object, with a reference
	 * to the BuildStore to be populated.
	 * @param buildStore The BuildStore object to be populated
	 */
	public ElectricAnnoSAXHandler(BuildStore buildStore) {
		buildTasks = buildStore.getBuildTasks();
		fns = buildStore.getFileNameSpaces();
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Handle occurrences of an element start tag. This method is invoked by the SAX Parser
	 * whenever a new element start tag (e.g. <job>) is identified. See the definition of
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
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Handle occurrences of an element end tag. This method is invoked by the SAX Parser
	 * whenever a new element end tag (e.g. </job>) is identified. See the definition of
	 * import org.xml.sax.helpers.DefaultHandler for parameter details.
	 */
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		
		/* 
		 * If we've found a </job> element tag, so we've parsed all the information related
		 * to this job. To finish up, create a new build task and populate it with all the
		 * file access information we acquired.. 
		 */
		if (localName.equals("job")) {
			
			withinJob = false;
			
			/* we've seen all the details of this job, add it as a build task */
			/* TODO: add the actual shell command here */
			int newTaskId = buildTasks.addBuildTask("");

			/* add all the file reads to the build task */
			for (String file : filesRead) {
				int newFileId = fns.addFile(file);
				if (newFileId != -1) {
					buildTasks.addFileAccess(newTaskId, newFileId, OperationType.OP_READ);
				} else {
					throw new FatalBuildScannerError("Unable to register new file in database: " + file);
				}
			}

			/* add all the file writes to the build task */
			for (String file : filesWritten) {
				int newFileId = fns.addFile(file);
				if (newFileId != -1) {
				buildTasks.addFileAccess(newTaskId, newFileId, OperationType.OP_WRITE);
				} else {
					throw new FatalBuildScannerError("Unable to register new file in database: " + file);
				}
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Handle occurrences of raw character in an XML stream. That is, the text that appears
	 * between the start and end tags. FOr example, <job>this is the text</job>.
	 * See the definition of import org.xml.sax.helpers.DefaultHandler for parameter details.
	 */
	@Override
	public void characters(char[] ch, int start, int length) 
			throws SAXException {
		
		/* TODO: capture the job's shell command */
	}
	
	/*-------------------------------------------------------------------------------------*/
}
