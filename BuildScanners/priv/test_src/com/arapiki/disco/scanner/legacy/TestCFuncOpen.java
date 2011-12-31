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


import static org.junit.Assert.*;

import org.junit.Test;

import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.BuildTasks;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.BuildTasks.OperationType;
import com.arapiki.utils.errors.ErrorCode;

/**
 * Basic testing that the LegacyBuildScanner can produce a valid
 * BuildStore. There are many test cases, split over multiple
 * test case files, with this file testing C Functions that
 * perform an open()-like operation.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestCFuncOpen {

	/* variables used in many test cases */
	private BuildStore bs = null;
	private BuildTasks bts = null;
	private FileNameSpaces fns = null;
	private int rootTask;
	private int task;
	private Integer fileAccesses[], fileReads[], fileWrites[];

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Given the code of a small C program, compile the program and scan it into a BuildStore.
	 * We then retrieve the one (and only) task that was registered in the BuildStore, along
	 * with the lists of files that were accessed (accessed, read and written).
	 * @param programCode The body of the small C program to be compiled.
	 * @param args The command line arguments to pass to the small C program.
	 * @throws Exception Something went wrong when compiling/running the program.
	 */
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
	
	/**
	 * Test the creat() C function.
	 * @throws Exception
	 */
	@Test
	public void testCreat() throws Exception {
		//int creat(const char *path, mode_t mode)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the creat64() C function.
	 * @throws Exception
	 */
	@Test
	public void testCreat64() throws Exception {
		//int creat64(const char *path, mode_t mode)
		fail("Not implemented.");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the dlopen() C function.
	 * @throws Exception
	 */
	@Test
	public void testDlopen() throws Exception {
		//void *dlopen(const char *file, int mode)
		fail("Not implemented.");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the fopen() C function.
	 * @throws Exception
	 */
	@Test
	public void testFopen() throws Exception {

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

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the fopen64() C function.
	 * @throws Exception
	 */
	@Test
	public void testFopen64() throws Exception {
		//FILE *fopen64(const char *filename, const char *mode)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the freopen() C function.
	 * @throws Exception
	 */
	@Test
	public void testFreopen() throws Exception {
		//FILE * freopen(const char *path, const char *mode, FILE *stream)
		fail("Not implemented.");
	}
	

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the freopen64() C function.
	 * @throws Exception
	 */
	@Test
	public void testFreopen64() throws Exception {
		//FILE * freopen64(const char *path, const char *mode, FILE *stream)
		fail("Not implemented.");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the ftok() C function.
	 * @throws Exception
	 */
	@Test
	public void testFtok() throws Exception {
		//key_t ftok(const char *pathname, int proj_id)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the open() C function.
	 * @throws Exception
	 */
	@Test
	public void testOpen() throws Exception {

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

	/**
	 * Test the open64() C function.
	 * @throws Exception
	 */
	@Test
	public void testOpen64() throws Exception {
		//int open64(const char *filename, int flags, ...)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the openat() C function.
	 * @throws Exception
	 */
	@Test
	public void testOpenat() throws Exception {
		//int openat(int dirfd, const char *pathname, int flags, ...)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the openat64() C function.
	 * @throws Exception
	 */
	@Test
	public void testOpenat64() throws Exception {
		//int openat64(int dirfd, const char *pathname, int flags, ...)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the truncate() C function.
	 * @throws Exception
	 */
	@Test
	public void testTruncate() throws Exception {
		//int truncate(const char *filename, off_t length)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the truncate64() C function.
	 * @throws Exception
	 */
	@Test
	public void testTruncate64() throws Exception {
		//int truncate64(const char *filename, off64_t length)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/

}
