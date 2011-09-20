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

package com.arapiki.disco.scanner.legacy;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.BuildTasks;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.BuildTasks.OperationType;
import com.arapiki.utils.errors.ErrorCode;

/**
 * Basic testing that the LegacyBuildScanner can produce a valid
 * BuildStore. There are many test cases, split over multiple
 * test case files.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestLegacyScans1 {

	/* variables used in many test cases */
	private BuildStore bs = null;
	private BuildTasks bts = null;
	private FileNameSpaces fns = null;
	private int rootTask;
	private int task;
	private Integer fileAccesses[], fileReads[], fileWrites[];

	/*-------------------------------------------------------------------------------------*/
	
	private void traceOneProgram(String programCode, String args[]) throws Exception {
		
		/* compile, run, and trace the program */
		bs = CommonTestUtils.parseLegacyProgram(programCode, args);
		
		/* fetch references to sub objects */
		bts = bs.getBuildTasks();
		fns = bs.getFileNameSpaces();
		
		/* find the root task */
		rootTask = bts.getRootTask("root");
		
		/* there should only be one child task */
		Integer childTasks[] = bts.getChildren(rootTask);
		assertEquals(1, childTasks.length);
		
		/* this is the task ID of the one task */
		task = childTasks[0];

		/* fetch the file access array */
		fileAccesses = bts.getFilesAccessed(task, OperationType.OP_UNSPECIFIED);
		fileReads = bts.getFilesAccessed(task, OperationType.OP_READ);
		fileWrites = bts.getFilesAccessed(task, OperationType.OP_WRITE);
	}
	
	/*-------------------------------------------------------------------------------------*/

	@Test
	public void testNewProgram() throws Exception {

		/* execute the program, and get the BuildStore, etc into our instance members */
		traceOneProgram("int main() { }", null);

		/* 
		 * The command line should end with /prog (note that our test harness executes it
		 * as /tmp/cfsXXXXXXXXX/prog), so we ignore the first part.
		 */
		String commandName = bts.getCommand(task);
		assertTrue(commandName.endsWith("/prog"));
		
		/* this task has no file accesses */
		assertEquals(0, fileAccesses.length);
	}
	
	/*-------------------------------------------------------------------------------------*/

	@Test
	public void testNewProgramWithArgs() throws Exception {

		/* execute the program, and get the BuildStore, etc into our instance members */
		traceOneProgram("int main() { }", new String[] {"-c", "aardvark", "22", "elephant"});

		/* 
		 * The command line should end with /prog (note that our test harness executes it
		 * as /tmp/cfsXXXXXXXXX/prog), so we ignore the first part.
		 */
		String commandName = bts.getCommand(task);
		assertTrue(commandName.endsWith("/prog -c aardvark 22 elephant"));		
	}
	
	/*-------------------------------------------------------------------------------------*/

	@Test
	public void testFileOpen() throws Exception {

		/* execute the program, and get the BuildStore, etc into our instance members */
		traceOneProgram(
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  int fd = open(\"/etc/passwd\", O_RDONLY);" +
				"}", null);
		
		assertEquals(1, fileReads.length);
		assertEquals(0, fileWrites.length);
		int fileId = fns.getPath("/etc/passwd");
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileReads[0].intValue());
	}
	
	/*-------------------------------------------------------------------------------------*/

	@Test
	public void testFileFopen() throws Exception {

		/* execute the program, and get the BuildStore, etc into our instance members */
		traceOneProgram(
				"#include <stdio.h>\n" +
				"int main() {" +
				"  FILE *file = fopen(\"/etc/passwd\", \"r\");" +
				"}", null);
		
		assertEquals(1, fileReads.length);
		assertEquals(0, fileWrites.length);
		int fileId = fns.getPath("/etc/passwd");
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileReads[0].intValue());
	}	
}
