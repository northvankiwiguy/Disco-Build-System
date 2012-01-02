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

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.BuildTasks;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.BuildTasks.OperationType;
import com.arapiki.disco.model.FileNameSpaces.PathType;
import com.arapiki.utils.errors.ErrorCode;
import com.arapiki.utils.os.SystemUtils;

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
	private Integer fileAccesses[], fileReads[], fileWrites[], fileModifies[];
	
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
	 * BuildStore, along with the lists of files that were accessed (accessed, read and written).
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
		fileAccesses = bts.getFilesAccessed(task, OperationType.OP_UNSPECIFIED);
		fileReads = bts.getFilesAccessed(task, OperationType.OP_READ);
		fileWrites = bts.getFilesAccessed(task, OperationType.OP_WRITE);
		fileModifies = bts.getFilesAccessed(task, OperationType.OP_MODIFIED);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Common method for testing creat() and creat64().
	 * @param func Which function to test ("creat" or "creat64").
	 * @throws Exception
	 */
	public void testCreatCmn(String func) throws Exception {
			
		/*
		 * Create a file using an absolute path.
		 */
		traceOneProgram(
				"#include <stdio.h>\n" +
				"int main() {" +
				"  int fd = " + func + "(\"" + tmpDir + "/test-file1\", 0644);" +
				"}", null);
		assertEquals(0, fileReads.length);
		assertEquals(1, fileWrites.length);
		int fileId = fns.getPath(tmpDir + "/test-file1");
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileWrites[0].intValue());
	
		/*
		 * Create a file using a relative path.
		 */
		traceOneProgram(
				"#include <stdio.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" +
				"  int fd = " + func + "(\"test-file2\", 0644);" +
				"}", null);
		assertEquals(0, fileReads.length);
		assertEquals(1, fileWrites.length);
		fileId = fns.getPath(tmpDir + "/test-file2");
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileWrites[0].intValue());		
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the creat() C function.
	 * @throws Exception
	 */
	@Test
	public void testCreat() throws Exception {
		testCreatCmn("creat");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the creat64() C function.
	 * @throws Exception
	 */
	@Test
	public void testCreat64() throws Exception {
		testCreatCmn("creat64");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Common code for testing fopen() and fopen64().
	 * @param func The function to invoke ("fopen" or "fopen64").
	 * @throws Exception
	 */
	public void testFopenCmn(String func) throws Exception {

		/*
		 * Open a file for read, using its absolute path name.
		 */
		traceOneProgram(
				"#include <stdio.h>\n" +
				"int main() {" +
				"  FILE *file = " + func + "(\"/etc/passwd\", \"r\");" +
				"}", null);
		assertEquals(1, fileReads.length);
		assertEquals(0, fileWrites.length);
		int fileId = fns.getPath("/etc/passwd");
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileReads[0].intValue());
		assertEquals(PathType.TYPE_FILE, fns.getPathType(fileReads[0].intValue()));
		
		/*
		 * Open a missing file, in read mode - no read should be logged.
		 */
		traceOneProgram(
				"#include <stdio.h>\n" +
				"int main() {" +
				"  FILE *file = " + func + "(\"/etc/not-passwd\", \"r\");" +
				"}", null);
		assertEquals(0, fileReads.length);
		assertEquals(0, fileWrites.length);
		
		/* 
		 * Open a relative path, in read mode.
		 */
		assertTrue(new File(tmpDir, "test-file1").createNewFile());
		traceOneProgram(
				"#include <stdio.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" + 
				"  FILE *file = " + func + "(\"test-file1\", \"r\");" +
				"}", null);
		assertEquals(1, fileReads.length);
		assertEquals(0, fileWrites.length);
		fileId = fns.getPath(tmpDir + "/test-file1");
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileReads[0].intValue());
		
		/*
		 * Open a relative path, in write mode.
		 */
		traceOneProgram(
				"#include <stdio.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" + 
				"  FILE *file = " + func + "(\"test-file2\", \"w\");" +
				"}", null);
		assertEquals(0, fileReads.length);
		assertEquals(1, fileWrites.length);
		fileId = fns.getPath(tmpDir + "/test-file2");
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileWrites[0].intValue());
		
		/* 
		 * Try opening files with a range of valid open modes.
		 */
		assertTrue(new File(tmpDir, "test-file-r").createNewFile());
		assertTrue(new File(tmpDir, "test-file-rb").createNewFile());
		assertTrue(new File(tmpDir, "test-file-r+").createNewFile());
		assertTrue(new File(tmpDir, "test-file-rb+").createNewFile());
		assertTrue(new File(tmpDir, "test-file-r+b").createNewFile());
		traceOneProgram(
				"#include <stdio.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" + 
				"  FILE *file = " + func + "(\"test-file-r\", \"r\");" +
				"  file = " + func + "(\"test-file-rb\", \"rb\");" +
				"  file = " + func + "(\"test-file-w\", \"w\");" +
				"  file = " + func + "(\"test-file-wb\", \"wb\");" +
				"  file = " + func + "(\"test-file-a\", \"a\");" +
				"  file = " + func + "(\"test-file-ab\", \"ab\");" +
				"  file = " + func + "(\"test-file-r+\", \"r+\");" +
				"  file = " + func + "(\"test-file-rb+\", \"rb+\");" +
				"  file = " + func + "(\"test-file-r+b\", \"r+b\");" +
				"  file = " + func + "(\"test-file-w+\", \"w+\");" +
				"  file = " + func + "(\"test-file-wb+\", \"w+b\");" +
				"  file = " + func + "(\"test-file-a+\", \"a+\");" +
				"  file = " + func + "(\"test-file-ab+\", \"ab+\");" +
				"  file = " + func + "(\"test-file-a+b\", \"a+b\");" +
				"}", null);
		assertEquals(2, fileReads.length);
		assertEquals(9, fileWrites.length);
		assertEquals(3, fileModifies.length);
		
		/*
		 * Test opening a directory.
		 */
		traceOneProgram(
				"#include <stdio.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" + 
				"  FILE *file = " + func + "(\".\", \"r\");" +
				"}", null);
		assertEquals(1, fileReads.length);
		assertEquals(0, fileWrites.length);
		assertEquals(PathType.TYPE_DIR, fns.getPathType(fileReads[0].intValue()));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the fopen() C function.
	 * @throws Exception
	 */
	@Test
	public void testFopen() throws Exception {
		testFopenCmn("fopen");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the fopen64() C function.
	 * @throws Exception
	 */
	@Test
	public void testFopen64() throws Exception {
		testFopenCmn("fopen64");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Common test code for the freopen() and freopen64() functions.
	 * @param func The function to use ("freopen" or "freopen64").
	 * @throws Exception
	 */
	public void testFreopenCmn(String func) throws Exception {

		String fileName1 = tmpDir + "/file1";
		String fileName2 = tmpDir + "/file2";
		assertTrue(new File(fileName1).createNewFile());
		assertTrue(new File(fileName2).createNewFile());
		
		/* 
		 * Open a temporary file in read mode, then reopen in write mode,
		 * using the same file name.
	     */
		traceOneProgram(
				"#include <stdio.h>\n" +
				"int main() {" +
				"  FILE *file = fopen(\"" + fileName1 + "\", \"r\");" + 
				"  file = " + func + "(\"" + fileName1 + "\", \"w\", file);" +
				"}", null);
		assertEquals(0, fileReads.length);
		assertEquals(0, fileWrites.length);
		assertEquals(1, fileModifies.length);
		int fileId = fns.getPath(fileName1);
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileModifies[0].intValue());

		/* 
		 * Open a temporary file in read mode, then reopen in write mode,
		 * using the same file name, but using a relative path.
	     */
		traceOneProgram(
				"#include <stdio.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" + 
				"  FILE *file = fopen(\"" + fileName1 + "\", \"r\");" + 
				"  file = " + func + "(\"file1\", \"w\", file);" +
				"}", null);
		assertEquals(0, fileReads.length);
		assertEquals(0, fileWrites.length);
		assertEquals(1, fileModifies.length);
		fileId = fns.getPath(fileName1);
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileModifies[0].intValue());
		
		/* 
		 * Open a temporary file in read mode, then reopen in write mode
		 * using a different file name.
	     */
		traceOneProgram(
				"#include <stdio.h>\n" +
				"int main() {" +
				"  FILE *file = fopen(\"" + fileName1 + "\", \"r\");" + 
				"  file = " + func + "(\"" + fileName2 + "\", \"w\", file);" +
				"}", null);
		assertEquals(1, fileReads.length);
		assertEquals(1, fileWrites.length);
		assertEquals(0, fileModifies.length);
		fileId = fns.getPath(fileName1);
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileReads[0].intValue());
		fileId = fns.getPath(fileName2);
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileWrites[0].intValue());

		/* 
		 * Open a temporary file in read mode, then reopen in write mode,
		 * but using NULL for the file name (to reopen the same file).
		 * TODO: this case not yet implemented properly (see interposer
		 * function for an explanation). This test case validates
		 * the INCORRECT behaviour and should be fixed.
		 */
		traceOneProgram(
				"#include <stdio.h>\n" +
				"int main() {" +
				"  FILE *file = fopen(\"" + fileName1 + "\", \"r\");" + 
				"  file = " + func + "(NULL, \"w\", file);" +
				"}", null);
		assertEquals(1, fileReads.length);
		assertEquals(0, fileWrites.length);
		assertEquals(0, fileModifies.length);		
		
		/*
		 * Open a temporary file in write mode, then reopen in read mode,
		 * using the same file name.
		 */
		traceOneProgram(
				"#include <stdio.h>\n" +
				"int main() {" +
				"  FILE *file = fopen(\"" + fileName1 + "\", \"w\");" + 
				"  file = " + func + "(\"" + fileName1 + "\", \"r\", file);" +
				"}", null);
		assertEquals(0, fileReads.length);
		assertEquals(1, fileWrites.length);
		assertEquals(0, fileModifies.length);
		fileId = fns.getPath(fileName1);
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileWrites[0].intValue());

		/*
		 * Open a temporary file in write mode, then reopen a different
		 * file which is missing.
		 */
		traceOneProgram(
				"#include <stdio.h>\n" +
				"int main() {" +
				"  FILE *file = fopen(\"" + fileName1 + "\", \"w\");" + 
				"  file = " + func + "(\"missing-file\", \"r\", file);" +
				"}", null);
		assertEquals(0, fileReads.length);
		assertEquals(1, fileWrites.length);
		assertEquals(0, fileModifies.length);
		fileId = fns.getPath(fileName1);
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileWrites[0].intValue());
	}
	

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the freopen() C function.
	 * @throws Exception
	 */
	@Test
	public void testFreopen() throws Exception {
		testFreopenCmn("freopen");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the freopen64() C function.
	 * @throws Exception
	 */
	@Test
	public void testFreopen64() throws Exception {
		testFreopenCmn("freopen64");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the ftok() C function.
	 * @throws Exception
	 */
	@Test
	public void testFtok() throws Exception {
		
		/*
		 * Execute ftok() on a legitimate file.
		 */
		traceOneProgram(
				"#include <sys/ipc.h>\n" +
				"int main() {" +
				"  key_t key = ftok(\"/etc/passwd\", 10);" +
				"}", null);
		assertEquals(1, fileReads.length);
		int fileId = fns.getPath("/etc/passwd");
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileReads[0].intValue());
		assertEquals(PathType.TYPE_FILE, fns.getPathType(fileReads[0].intValue()));

		/*
		 * Execute ftok() on a missing file.
		 */
		traceOneProgram(
				"#include <sys/ipc.h>\n" +
				"int main() {" +
				"  key_t key = ftok(\"/missing-file\", 10);" +
				"}", null);
		assertEquals(0, fileReads.length);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Common code for testing the open() and open64() functions.
	 * @param func The function to invoke ("open" or "open64").
	 * @throws Exception
	 */
	public void testOpenCmn(String func) throws Exception {

		/*
		 * Open a file with an absolute path, in read-mode.
		 */
		traceOneProgram(
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  int fd = " + func + "(\"/etc/passwd\", O_RDONLY);" +
				"}", null);
		
		assertEquals(1, fileReads.length);
		assertEquals(0, fileWrites.length);
		int fileId = fns.getPath("/etc/passwd");
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileReads[0].intValue());
		assertEquals(PathType.TYPE_FILE, fns.getPathType(fileReads[0].intValue()));
		
		/*
		 * Open a file with a relative path, in read-mode.
		 */
		assertTrue(new File(tmpDir, "test-file1").createNewFile());
		traceOneProgram(
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" +
				"  int fd = " + func + "(\"test-file1\", O_RDONLY);" +
				"}", null);
		
		assertEquals(1, fileReads.length);
		assertEquals(0, fileWrites.length);
		fileId = fns.getPath(tmpDir + "/test-file1");
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileReads[0].intValue());	
		
		/*
		 * Open a file with a relative path, in write-mode.
		 */
		traceOneProgram(
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" +
				"  int fd = " + func + "(\"test-file2\", O_CREAT | O_WRONLY);" +
				"}", null);
		assertEquals(0, fileReads.length);
		assertEquals(1, fileWrites.length);
		fileId = fns.getPath(tmpDir + "/test-file2");
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileWrites[0].intValue());	
		
		/*
		 * Open a file with a relative path, in modify-mode.
		 */
		assertTrue(new File(tmpDir, "test-file3").createNewFile());
		traceOneProgram(
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" +
				"  int fd = " + func + "(\"test-file3\", O_RDWR);" +
				"}", null);
		assertEquals(0, fileReads.length);
		assertEquals(0, fileWrites.length);
		assertEquals(1, fileModifies.length);
		fileId = fns.getPath(tmpDir + "/test-file3");
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileModifies[0].intValue());	

		/*
		 * Open a file that doesn't exist.
		 */
		traceOneProgram(
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" +
				"  int fd = open(\"test-file-missing\", O_RDWR);" +
				"}", null);
		assertEquals(0, fileReads.length);
		assertEquals(0, fileWrites.length);
		assertEquals(0, fileModifies.length);
		
		/*
		 * Open a directory.
		 */
		traceOneProgram(
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" +
				"  int fd = open(\".\", O_RDONLY);" +
				"}", null);
		assertEquals(1, fileReads.length);
		assertEquals(0, fileWrites.length);
		assertEquals(0, fileModifies.length);
		assertEquals(PathType.TYPE_DIR, fns.getPathType(fileReads[0].intValue()));		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the open() C function.
	 * @throws Exception
	 */
	@Test
	public void testOpen() throws Exception {
		testOpenCmn("open");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the open64() C function.
	 * @throws Exception
	 */
	@Test
	public void testOpen64() throws Exception {
		testOpenCmn("open64");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Command method for testing openat() and openat64().
	 * @param func The function to test ("openat" or "openat64").
	 * @throws Exception
	 */
	public void testOpenatCmn(String func) throws Exception {

		/*
		 * Test openat() with a path that's relative to a valid directory fd.
		 */
		traceOneProgram(
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" + 
				"  mkdir(\"sub-dir\", 0755);" + 
				"  int dirfd = open(\"sub-dir\", O_RDONLY);" +
				"  int fd = " + func + "(dirfd, \"test-file1\", O_CREAT|O_WRONLY, 0666);" +
				"  close(dirfd);" + 
				"  close(fd);" + 
				"}", null);
		assertEquals(1, fileReads.length);		/* the sub-dir directory is read */
		assertEquals(1, fileWrites.length);
		int fileId = fns.getPath(tmpDir + "/sub-dir/test-file1");
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileWrites[0].intValue());
		
		/*
		 * Test openat() with an absolute path name (a dirfd is provided, but shouldn't be used).
		 */
		traceOneProgram(
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" + 
				"  mkdir(\"sub-dir\", 0755);" + 
				"  mkdir(\"sub-dir2\", 0755);" + 
				"  int dirfd = open(\"sub-dir\", O_RDONLY);" +
				"  int fd = " + func + "(dirfd, \"" + tmpDir + "/sub-dir2/test-file2\"," +
						"O_CREAT|O_WRONLY, 0666);" +
				"}", null);
		assertEquals(1, fileReads.length);		/* the sub-dir directory is read */
		assertEquals(1, fileWrites.length);
		fileId = fns.getPath(tmpDir + "/sub-dir2/test-file2");
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileWrites[0].intValue());
		
		/*
		 * Test openat() with a path relative to the current directory.
		 */
		traceOneProgram(
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" + 
				"  mkdir(\"sub-dir\", 0755);" + 
				"  int fd = " + func + "(AT_FDCWD, \"sub-dir/test-file3\"," +
						"O_CREAT|O_WRONLY, 0666);" +
				"}", null);
		assertEquals(0, fileReads.length);
		assertEquals(1, fileWrites.length);
		fileId = fns.getPath(tmpDir + "/sub-dir/test-file3");
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileWrites[0].intValue());
		
		/*
		 * Test openat() with an invalid dirfd number - should fail.
		 */
		traceOneProgram(
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" + 
				"  mkdir(\"sub-dir\");" + 
				"  int fd = " + func + "(100, \"sub-dir/test-file4\"," +
						"O_CREAT|O_WRONLY, 0666);" +
				"}", null);
		assertEquals(0, fileReads.length);
		assertEquals(0, fileWrites.length);
		
		/*
		 * Test openat() with a file that doesn't exist - should fail. 
		 */
		traceOneProgram(
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" + 
				"  mkdir(\"sub-dir\");" + 
				"  int dirfd = open(\"sub-dir\", O_RDONLY);" +
				"  int fd = " + func + "(dirfd, \"sub-dir/test-file-missing\"," +
						"O_RDONLY, 0666);" +
				"}", null);
		assertEquals(1, fileReads.length);		/* the sub-dir directory is read */
		assertEquals(0, fileWrites.length);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the openat() C function.
	 * @throws Exception
	 */
	@Test
	public void testOpenat() throws Exception {
		testOpenatCmn("openat");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the openat64() C function.
	 * @throws Exception
	 */
	@Test
	public void testOpenat64() throws Exception {
		testOpenatCmn("openat64");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Common code for testing truncate() and truncate64().
	 * @param func Function to use ("truncate" or "truncate64").
	 * @throws Exception
	 */
	public void testTruncateCmn(String func) throws Exception {
				
		/*
		 * Truncate a file using an absolute path. Note that we
		 * need an explicit declaration of truncate64(), since it
		 * expects a 64-bit offset value.
		 */
		assertTrue(new File(tmpDir, "test-file1").createNewFile());
		traceOneProgram(
				"#include <stdio.h>\n" +
				"#include <unistd.h>\n" +
				"extern int truncate64(const char *, __off64_t);" +
				"int main() {" +
				"  int status = " + func + "(\"" + tmpDir + "/test-file1\", 10);" +
				"}", null);
		assertEquals(0, fileReads.length);
		assertEquals(0, fileWrites.length);
		assertEquals(1, fileModifies.length);
		int fileId = fns.getPath(tmpDir + "/test-file1");
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileModifies[0].intValue());
	
		/*
		 * Truncate a file using a relative path.
		 */
		assertTrue(new File(tmpDir, "test-file2").createNewFile());
		traceOneProgram(
				"#include <stdio.h>\n" +
				"#include <unistd.h>\n" +
				"extern int truncate64(const char *, __off64_t);" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" +
				"  int status = " + func + "(\"test-file2\", 10);" +
				"}", null);
		assertEquals(0, fileReads.length);
		assertEquals(0, fileWrites.length);
		assertEquals(1, fileModifies.length);
		fileId = fns.getPath(tmpDir + "/test-file2");
		assertNotSame(ErrorCode.NOT_FOUND, fileId);
		assertEquals(fileId, fileModifies[0].intValue());
		
		/*
		 * Truncate a non-existent file
		 */
		traceOneProgram(
				"#include <stdio.h>\n" +
				"#include <unistd.h>\n" +
				"extern int truncate64(const char *, __off64_t);" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" +
				"  int status = " + func + "(\"test-file-missing\", 10);" +
				"}", null);
		assertEquals(0, fileReads.length);
		assertEquals(0, fileWrites.length);
		assertEquals(0, fileModifies.length);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the truncate() C function.
	 * @throws Exception
	 */
	@Test
	public void testTruncate() throws Exception {
		testTruncateCmn("truncate");
	}
	
	/*-------------------------------------------------------------------------------------*/

	
	/**
	 * Test the truncate64() C function.
	 * @throws Exception
	 */
	@Test
	public void testTruncate64() throws Exception {
		testTruncateCmn("truncate64");
	}

	/*-------------------------------------------------------------------------------------*/

}
