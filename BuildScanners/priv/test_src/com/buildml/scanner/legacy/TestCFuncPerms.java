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

import static org.junit.Assert.*;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.buildml.model.BuildStore;
import com.buildml.model.BuildTasks;
import com.buildml.model.FileNameSpaces;
import com.buildml.model.BuildTasks.OperationType;
import com.buildml.utils.os.SystemUtils;

/**
 * Basic testing that the LegacyBuildScanner can produce a valid
 * BuildStore. There are many test cases, split over multiple
 * test case files, with this file testing C Functions that
 * manipulate or access file permissions.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestCFuncPerms {
	
	/* variables used in many test cases */
	private BuildStore bs = null;
	private BuildTasks bts = null;
	private FileNameSpaces fns = null;
	private int rootTask;
	private int task;
	private Integer fileModifies[];
	
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
	 * BuildStore. We then retrieve the one (and only) task that was registered in the 
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
		bts = bs.getBuildTasks();
		fns = bs.getFileNameSpaces();
		
		/* find the root task */
		rootTask = bts.getRootTask("root");
		
		/* there should only be one child task */
		Integer childTasks[] = bts.getChildren(rootTask);
		assertEquals(1, childTasks.length);
		
		/* this is the task ID of the one task */
		task = childTasks[0];

		/* fetch the file access arrays */
		fileModifies = bts.getFilesAccessed(task, OperationType.OP_MODIFIED);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the access() C function.
	 * @throws Exception
	 */
	@Test
	public void testAccess() throws Exception {
		/* 
		 * Nothing to test, for now. This can be implemented when the access() function
		 * is modified to support package-boundary checking.
		 */
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the chmod() C function.
	 * @throws Exception
	 */
	@Test
	public void testChmod() throws Exception {
		
		/*
		 * chmod() a file that exists.
		 */
		assertTrue(new File(tmpDir, "chmod-file").createNewFile());
		traceOneProgram(
				"#include <sys/stat.h>\n" +
				"int main() {" +
				"  chmod(\"" + tmpDir + "/chmod-file\", 0644);" +
				"  return 0;" +
				"}", null);
		assertEquals(1, fileModifies.length);
		assertEquals(fns.getPath(tmpDir + "/chmod-file"), fileModifies[0].intValue());
		
		/*
		 * chmod() a file that doesn't exist - nothing should be logged.
		 */
		traceOneProgram(
				"#include <sys/stat.h>\n" +
				"int main() {" +
				"  chmod(\"" + tmpDir + "/no-file\", 0644);" +
				"  return 0;" +
				"}", null);
		assertEquals(0, fileModifies.length);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the chown() C function.
	 * @throws Exception
	 */
	@Test
	public void testChown() throws Exception {
		
		/*
		 * chmod() a file that exists (note that it's legal to change a file's
		 * uid and gid to getuid() and getgid() respectively. This doesn't
		 * require root elevation, so we can do it in a unit test.
		 */
		assertTrue(new File(tmpDir, "chown-file").createNewFile());
		traceOneProgram(
				"#include <sys/stat.h>\n" +
				"int main() {" +
				"  chown(\"" + tmpDir + "/chown-file\", getuid(), getgid());" +
				"  return 0;" +
				"}", null);
		assertEquals(1, fileModifies.length);
		assertEquals(fns.getPath(tmpDir + "/chown-file"), fileModifies[0].intValue());
		
		/*
		 * chmod() a file that doesn't exist - nothing should be logged.
		 */
		traceOneProgram(
				"#include <sys/stat.h>\n" +
				"int main() {" +
				"  chown(\"" + tmpDir + "/no-file\", getuid(), getgid());" +
				"  return 0;" +
				"}", null);
		assertEquals(0, fileModifies.length);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the eaccess() C function.
	 * @throws Exception
	 */
	@Test
	public void testEaccess() throws Exception {
		/* 
		 * Nothing to test, for now. This can be implemented when the eaccess() function
		 * is modified to support package-boundary checking.
		 */
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the euidaccess() C function.
	 * @throws Exception
	 */
	@Test
	public void testEuidaccess() throws Exception {
		/* 
		 * Nothing to test, for now. This can be implemented when the euidaccess() function
		 * is modified to support package-boundary checking.
		 */
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the faccessat() C function.
	 * @throws Exception
	 */
	@Test
	public void testFaccessat() throws Exception {
		/* 
		 * Nothing to test, for now. This can be implemented when the faccessat() function
		 * is modified to support package-boundary checking.
		 */
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the fchmod() C function.
	 * @throws Exception
	 */
	@Test
	public void testFchmod() throws Exception {
		
		/*
		 * fchmod() a file that exists.
		 */
		assertTrue(new File(tmpDir, "fchmod-file").createNewFile());
		traceOneProgram(
				"#include <sys/stat.h>\n" +
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" +
				"  int fd = open(\"fchmod-file\", O_RDONLY);" +
				"  fchmod(fd, 0644);" +
				"  return 0;" +
				"}", null);
		assertEquals(1, fileModifies.length);
		assertEquals(fns.getPath(tmpDir + "/fchmod-file"), fileModifies[0].intValue());
		
		/*
		 * fchmod() with an invalid file descriptor.
		 */
		traceOneProgram(
				"#include <sys/stat.h>\n" +
				"int main() {" +
				"  fchmod(-1, 0644);" +
				"  return 0;" +
				"}", null);
		assertEquals(0, fileModifies.length);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the fchmodat() C function.
	 * @throws Exception
	 */
	@Test
	public void testFchmodat() throws Exception {
		
		/*
		 * fchmodat() a file that exists.
		 */
		assertTrue(new File(tmpDir, "fchmodat-file").createNewFile());
		traceOneProgram(
				"#include <sys/stat.h>\n" +
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  int fd = open(\"" + tmpDir + "\", O_RDONLY);" +
				"  fchmodat(fd, \"fchmodat-file\", 0644, 0);" +
				"  return 0;" +
				"}", null);
		assertEquals(1, fileModifies.length);
		assertEquals(fns.getPath(tmpDir + "/fchmodat-file"), fileModifies[0].intValue());
		
		/*
		 * fchmodat() with an invalid file.
		 */
		traceOneProgram(
				"#include <sys/stat.h>\n" +
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  int fd = open(\"" + tmpDir + "\", O_RDONLY);" +
				"  fchmodat(fd, \"bad-file\", 0644, 0);" +
				"  return 0;" +
				"}", null);
		assertEquals(0, fileModifies.length);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the fchown() C function.
	 * @throws Exception
	 */
	@Test
	public void testFchown() throws Exception {
		
		/*
		 * fchown() a file that exists (note that it's legal to change a file's
		 * uid and gid to getuid() and getgid() respectively. This doesn't
		 * require root elevation, so we can do it in a unit test.
		 */
		assertTrue(new File(tmpDir, "fchown-file").createNewFile());
		traceOneProgram(
				"#include <sys/stat.h>\n" +
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  int fd = open(\"" + tmpDir + "/fchown-file\", O_RDONLY);" +
				"  fchown(fd, getuid(), getgid());" +
				"  return 0;" +
				"}", null);
		assertEquals(1, fileModifies.length);
		assertEquals(fns.getPath(tmpDir + "/fchown-file"), fileModifies[0].intValue());
		
		/*
		 * chmod() a file that doesn't exist - nothing should be logged.
		 */
		traceOneProgram(
				"#include <sys/stat.h>\n" +
				"int main() {" +
				"  fchown(-1, getuid(), getgid());" +
				"  return 0;" +
				"}", null);
		assertEquals(0, fileModifies.length);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the fchownat() C function.
	 * @throws Exception
	 */
	@Test
	public void testFchownat() throws Exception {
		
		/*
		 * fchownat() a file that exists.
		 */
		assertTrue(new File(tmpDir, "fchownat-file").createNewFile());
		traceOneProgram(
				"#include <sys/stat.h>\n" +
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  int dirfd = open(\"" + tmpDir + "\", O_RDONLY);" +
				"  fchownat(dirfd, \"fchownat-file\", getuid(), getgid(), 0);" +
				"  return 0;" +
				"}", null);
		assertEquals(1, fileModifies.length);
		assertEquals(fns.getPath(tmpDir + "/fchownat-file"), fileModifies[0].intValue());
		
		/*
		 * fchownat() with an invalid file.
		 */
		traceOneProgram(
				"#include <sys/stat.h>\n" +
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  int dirfd = open(\"" + tmpDir + "\", O_RDONLY);" +
				"  fchownat(dirfd, \"bad-file\", getuid(), getgid(), 0);" +
				"  return 0;" +
				"}", null);
		assertEquals(0, fileModifies.length);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the lchown() C function.
	 * @throws Exception
	 */
	@Test
	public void testLchown() throws Exception {
		/*
		 * Not implemented for now, since symlinks aren't handled very well in BuildML.
		 */
	}

	/*-------------------------------------------------------------------------------------*/
	
}
