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

package com.buildml.scanner.legacy;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.buildml.model.IActionMgr;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IFileMgr.PathType;
import com.buildml.utils.os.SystemUtils;

/**
 * Basic testing that the LegacyBuildScanner can produce a valid
 * BuildStore. There are many test cases, split over multiple
 * test case files, with this file testing C Functions that
 * manipulate directories.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestCFuncDir {
	
	/* variables used in many test cases */
	private IBuildStore bs = null;
	private IActionMgr actionMgr = null;
	private IFileMgr fileMgr = null;
	private int rootAction;
	private int action;
	private Integer fileAccesses[], fileReads[], fileWrites[], fileModifies[], fileDeletes[];
	
	/** temporary directory into which test cases can store files */
	private File tmpDir;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Called before each test case starts. Creates a temporary directory in which the
	 * test case can store temporary files.
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		tmpDir = SystemUtils.createTempDir();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Called after each test case ends. Removes the temporary directory and its content.
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		SystemUtils.deleteDirectory(tmpDir);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given the source code of a small C program, compile the program and scan it into a 
	 * BuildStore. We then retrieve the one (and only) action that was registered in the 
	 * BuildStore, along with the lists of files that were accessed (accessed, read, written,
	 * and deleted).
	 * @param programCode The body of the small C program to be compiled.
	 * @param args The command line arguments to pass to the small C program.
	 * @throws Exception Something went wrong when compiling/running the program.
	 */
	private void traceOneProgram(String programCode, String args[]) throws Exception {
		
		/* compile, run, and trace the program */
		bs = BuildScannersCommonTestUtils.parseLegacyProgram(tmpDir, programCode, args);
		
		/* fetch references to sub objects */
		actionMgr = bs.getActionMgr();
		fileMgr = bs.getFileMgr();
		
		/* find the root action */
		rootAction = actionMgr.getRootAction("root");
		
		/* there should only be one child action */
		Integer childActions[] = actionMgr.getChildren(rootAction);
		assertEquals(1, childActions.length);
		
		/* this is the action ID of the one action */
		action = childActions[0];

		/* fetch the file access arrays */
		fileAccesses = actionMgr.getFilesAccessed(action, OperationType.OP_UNSPECIFIED);
		fileReads = actionMgr.getFilesAccessed(action, OperationType.OP_READ);
		fileWrites = actionMgr.getFilesAccessed(action, OperationType.OP_WRITE);
		fileModifies = actionMgr.getFilesAccessed(action, OperationType.OP_MODIFIED);
		fileDeletes = actionMgr.getFilesAccessed(action, OperationType.OP_DELETE);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the chdir() C function.
	 * @throws Exception
	 */
	@Test
	public void testChdir() throws Exception {
		
		/*
		 * Chdir to a path that exists.
		 */
		traceOneProgram(
				"#include <unistd.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" +
				"  system(\"true\");" +
				"}", null);
		
		/* test that the child action ("true") executed in tmpDir */
		Integer childActions[] = actionMgr.getChildren(action);
		assertEquals(1, childActions.length);
		int dirId = (Integer) actionMgr.getSlotValue(childActions[0], IActionMgr.DIRECTORY_SLOT_ID);
		assertEquals(tmpDir.toString(), fileMgr.getPathName(dirId));
		
		/*
		 * Chdir to a path that doesn't exist.
		 */
		traceOneProgram(
				"#include <unistd.h>\n" +
				"int main() {" +
				"  chdir(\"/\");" +
				"  chdir(\"" + tmpDir + "/invalid\");" +
				"  system(\"true\");" +
				"}", null);
		
		/* test that the child action ("true") executes in /, rather than tmpdir/invalid */
		childActions = actionMgr.getChildren(action);
		assertEquals(1, childActions.length);
		dirId = (Integer) actionMgr.getSlotValue(childActions[0], IActionMgr.DIRECTORY_SLOT_ID);
		assertEquals("/", fileMgr.getPathName(dirId));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the fchdir() C function.
	 * @throws Exception
	 */
	@Test
	public void testFchdir() throws Exception {
		/*
		 * fchdir to a path that exists.
		 */
		traceOneProgram(
				"#include <fcntl.h>\n" +
				"#include <unistd.h>\n" +
				"int main() {" +
				"  chdir(\"/\");" +
				"  int fd = open(\"" + tmpDir +"\", O_RDONLY);" +
				"  fchdir(fd);" +
				"  system(\"true\");" +
				"}", null);
		
		/* test that the child action ("true") executed in tmpDir */
		Integer childActions[] = actionMgr.getChildren(action);
		assertEquals(1, childActions.length);
		int dirId = (Integer) actionMgr.getSlotValue(childActions[0], IActionMgr.DIRECTORY_SLOT_ID);
		assertEquals(tmpDir.toString(), fileMgr.getPathName(dirId));
		
		/*
		 * fchdir to a bad file descriptor
		 */
		traceOneProgram(
				"#include <fcntl.h>\n" +
				"#include <unistd.h>\n" +
				"int main() {" +
				"  chdir(\"/\");" +
				"  fchdir(-1);" +
				"  system(\"true\");" +
				"}", null);
		
		/* test that the child action ("true") executes in / */
		childActions = actionMgr.getChildren(action);
		assertEquals(1, childActions.length);
		dirId = (Integer) actionMgr.getSlotValue(childActions[0], IActionMgr.DIRECTORY_SLOT_ID);
		assertEquals("/", fileMgr.getPathName(dirId));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the mkdir() C function.
	 * @throws Exception
	 */
	@Test
	public void testMkdir() throws Exception {
		
		/*
		 * Make a valid directory, and check that the top-level action
		 * is credited with making it.
		 */
		traceOneProgram(
				"#include <unistd.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" +
				"  mkdir(\"newDir\", 0755);" +
				"  return 0;" +
				"}", null);
		
		assertEquals(1, fileAccesses.length);
		assertEquals(1, fileWrites.length);
		String dirName = fileMgr.getPathName(fileAccesses[0]);
		assertEquals(tmpDir + "/newDir", dirName);
		assertEquals(PathType.TYPE_DIR, fileMgr.getPathType(fileAccesses[0]));
		
		/*
		 * Fail to make a directory - should not be logged.
		 */
		traceOneProgram(
				"#include <unistd.h>\n" +
				"int main() {" +
				"  mkdir(\"/invalid/dir\", 0755);" +
				"  return 0;" +
				"}", null);
		
		assertEquals(0, fileAccesses.length);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the mkdirat() C function.
	 * @throws Exception
	 */
	@Test
	public void testMkdirat() throws Exception {
		
		/*
		 * Make a valid directory, and check that the top-level action
		 * is credited with making it.
		 */
		traceOneProgram(
				"#include <fcntl.h>\n" +
				"#include <unistd.h>\n" +
				"int main() {" +
				"  int dirfd = open(\"" + tmpDir + "\", O_RDONLY);" +
				"  mkdirat(dirfd, \"anotherDir\", 0755);" +
				"  return 0;" +
				"}", null);
		
		assertEquals(2, fileAccesses.length);
		assertEquals(1, fileReads.length);
		assertEquals(1, fileWrites.length);
		String dirName = fileMgr.getPathName(fileWrites[0]);
		assertEquals(tmpDir + "/anotherDir", dirName);
		assertEquals(PathType.TYPE_DIR, fileMgr.getPathType(fileWrites[0]));

		/*
		 * Failing to make a directory will result in no accesses.
		 */
		traceOneProgram(
				"#include <fcntl.h>\n" +
				"#include <unistd.h>\n" +
				"int main() {" +
				"  int dirfd = open(\"" + tmpDir + "\", O_RDONLY);" +
				"  mkdirat(dirfd, \"/invalid/dir\", 0755);" +
				"  return 0;" +
				"}", null);
		
		assertEquals(1, fileAccesses.length);
		assertEquals(1, fileReads.length);
		assertEquals(0, fileWrites.length);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the rmdir() C function.
	 * @throws Exception
	 */
	@Test
	public void testRmdir() throws Exception {
		
		/*
		 * Delete a valid directory, and check that the top-level action
		 * is credited with removing it.
		 */
		assertTrue(new File(tmpDir, "newDir").mkdirs());
		traceOneProgram(
				"#include <unistd.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" +
				"  rmdir(\"newDir\");" +
				"  return 0;" +
				"}", null);
		
		assertEquals(1, fileAccesses.length);
		assertEquals(1, fileDeletes.length);
		String dirName = fileMgr.getPathName(fileDeletes[0]);
		assertEquals(tmpDir + "/newDir", dirName);
		assertEquals(PathType.TYPE_DIR, fileMgr.getPathType(fileDeletes[0]));
		
		/*
		 * Fail to make a directory - should not be logged.
		 */
		traceOneProgram(
				"#include <unistd.h>\n" +
				"int main() {" +
				"  rmdir(\"/invalid/dir\");" +
				"  return 0;" +
				"}", null);
		
		assertEquals(0, fileAccesses.length);
	}

	/*-------------------------------------------------------------------------------------*/
}


