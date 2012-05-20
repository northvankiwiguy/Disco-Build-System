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
import com.buildml.model.CommonTestUtils;
import com.buildml.model.FileNameSpaces;
import com.buildml.model.BuildTasks.OperationType;
import com.buildml.model.FileNameSpaces.PathType;
import com.buildml.utils.os.SystemUtils;

/**
 * Basic testing that the LegacyBuildScanner can produce a valid
 * BuildStore. There are many test cases, split over multiple
 * test case files, with this file testing C Functions that
 * perform an link/unlink/rename()-like operation.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestCFuncLink {

	/* variables used in many test cases */
	private BuildStore bs = null;
	private BuildTasks bts = null;
	private FileNameSpaces fns = null;
	private int rootTask;
	private int task;
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
		fileAccesses = bts.getFilesAccessed(task, OperationType.OP_UNSPECIFIED);
		fileReads = bts.getFilesAccessed(task, OperationType.OP_READ);
		fileWrites = bts.getFilesAccessed(task, OperationType.OP_WRITE);
		fileModifies = bts.getFilesAccessed(task, OperationType.OP_MODIFIED);
		fileDeletes = bts.getFilesAccessed(task, OperationType.OP_DELETE);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the link() C function.
	 * @throws Exception
	 */
	@Test
	public void testLink() throws Exception {
		
		/*
		 * create a hard link to a valid file
		 */
		assertTrue(new File(tmpDir, "oldFile1").createNewFile());
		traceOneProgram(
				"#include <unistd.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" +
				"  link(\"oldFile1\", \"linkFile1\");" +
				"}", null);
		assertEquals(2, fileAccesses.length);
		assertEquals(1, fileReads.length);
		assertEquals(1, fileWrites.length);
		assertEquals(tmpDir + "/oldFile1", fns.getPathName(fileReads[0]));
		assertEquals(tmpDir + "/linkFile1", fns.getPathName(fileWrites[0]));
		
		/*
		 * Creation of a hard link to a non-existent file should not be logged.
		 */
		traceOneProgram(
				"#include <unistd.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" +
				"  link(\"badFile\", \"linkFile1a\");" +
				"}", null);
		assertEquals(0, fileAccesses.length);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the linkat() C function.
	 * @throws Exception
	 */
	@Test
	public void testLinkat() throws Exception {
		
		/*
		 * create a hard link to a valid file
		 */
		assertTrue(new File(tmpDir, "oldFile2").createNewFile());
		traceOneProgram(
				"#include <unistd.h>\n" +
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  int dirfd = open(\"" + tmpDir + "\", O_RDONLY);" +
				"  linkat(dirfd, \"oldFile2\", dirfd, \"linkFile2\", 0);" +
				"}", null);
		assertEquals(3, fileAccesses.length);		/* include the dirfd open */
		assertEquals(1, fileWrites.length);
		assertEquals(tmpDir + "/linkFile2", fns.getPathName(fileWrites[0]));
		assertTrue(CommonTestUtils.sortedArraysEqual(fileReads, 
				new Integer[] { fns.getPath(tmpDir + "/oldFile2"),
								fns.getPath(tmpDir.toString()) }));
		
		/*
		 * Creation of a hard link to a non-existent file should not be logged.
		 */
		traceOneProgram(
				"#include <unistd.h>\n" +
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  int dirfd = open(\"" + tmpDir + "\", O_RDONLY);" +
				"  linkat(dirfd, \"badFile2\", dirfd, \"linkFile2a\", 0);" +
				"}", null);
		assertEquals(1, fileAccesses.length);  /* only the dirfd open is read */
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the remove() C function.
	 * @throws Exception
	 */
	@Test
	public void testRemove() throws Exception {
		
		/*
		 * Remove a file that exists.
		 */
		assertTrue(new File(tmpDir, "fileToDelete").createNewFile());
		traceOneProgram(
				"#include <unistd.h>\n" +
				"#include <fcntl.h>\n" +
				"int main() {" +
				"    remove(\"" + tmpDir + "/fileToDelete\");" +
				"}", null);
		assertEquals(1, fileAccesses.length);
		assertEquals(1, fileDeletes.length);
		assertEquals(tmpDir + "/fileToDelete", fns.getPathName(fileDeletes[0]));
		assertEquals(PathType.TYPE_FILE, fns.getPathType(fileDeletes[0]));

		/*
		 * Remove a directory that exists
		 */
		assertTrue(new File(tmpDir, "dirToDelete").mkdirs());
		traceOneProgram(
				"#include <unistd.h>\n" +
				"#include <fcntl.h>\n" +
				"int main() {" +
				"    remove(\"" + tmpDir + "/dirToDelete\");" +
				"}", null);
		assertEquals(1, fileAccesses.length);
		assertEquals(1, fileDeletes.length);
		assertEquals(tmpDir + "/dirToDelete", fns.getPathName(fileDeletes[0]));
		assertEquals(PathType.TYPE_DIR, fns.getPathType(fileDeletes[0]));

		/*
		 * Remove a file that doesn't exist
		 */
		traceOneProgram(
				"#include <unistd.h>\n" +
				"#include <fcntl.h>\n" +
				"int main() {" +
				"    remove(\"" + tmpDir + "/invalidFile\");" +
				"}", null);
		assertEquals(0, fileAccesses.length);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the rename() C function.
	 * @throws Exception
	 */
	@Test
	public void testRename() throws Exception {
		
		/*
		 * Rename a file that exists.
		 */
		assertTrue(new File(tmpDir, "fileToRename").createNewFile());
		traceOneProgram(
				"#include <stdio.h>\n" +
				"int main() {" +
				"    rename(\"" + tmpDir + "/fileToRename\", \"" + tmpDir + "/newName\");" +
				"}", null);
		assertEquals(2, fileAccesses.length);
		assertEquals(1, fileDeletes.length);
		assertEquals(1, fileWrites.length);		
		assertEquals(tmpDir + "/fileToRename", fns.getPathName(fileDeletes[0]));
		assertEquals(PathType.TYPE_FILE, fns.getPathType(fileDeletes[0]));
		assertEquals(tmpDir + "/newName", fns.getPathName(fileWrites[0]));
		assertEquals(PathType.TYPE_FILE, fns.getPathType(fileWrites[0]));

		/*
		 * Rename a directory that exists
		 */
		assertTrue(new File(tmpDir, "dirToRename").mkdirs());
		traceOneProgram(
				"#include <stdio.h>\n" +
				"int main() {" +
				"    rename(\"" + tmpDir + "/dirToRename\", \"" + tmpDir + "/newDirName\");" +
				"}", null);
		assertEquals(2, fileAccesses.length);
		assertEquals(1, fileDeletes.length);
		assertEquals(1, fileWrites.length);	
		assertEquals(tmpDir + "/dirToRename", fns.getPathName(fileDeletes[0]));
		assertEquals(PathType.TYPE_DIR, fns.getPathType(fileDeletes[0]));
		assertEquals(tmpDir + "/newDirName", fns.getPathName(fileWrites[0]));
		assertEquals(PathType.TYPE_DIR, fns.getPathType(fileWrites[0]));
		
		/*
		 * Rename a file that doesn't exist
		 */
		traceOneProgram(
				"#include <stdio.h>\n" +
				"int main() {" +
				"    rename(\"" + tmpDir + "/badFile\", \"" + tmpDir + "/newBadFile\");" +
				"}", null);
		assertEquals(0, fileAccesses.length);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the renameat() C function.
	 * @throws Exception
	 */
	@Test
	public void testRenameat() throws Exception {
		
		/*
		 * Rename a file that exists.
		 */
		assertTrue(new File(tmpDir, "fileToRename").createNewFile());
		traceOneProgram(
				"#include <stdio.h>\n" +
				"#include <fcntl.h>\n" +
				"int main() {" +
				"    int dirfd = open(\"" + tmpDir + "\", O_RDONLY);" +
				"    renameat(dirfd, \"fileToRename\", dirfd, \"newName\");" +
				"}", null);
		assertEquals(3, fileAccesses.length);
		assertEquals(1, fileReads.length);			 /* opening the dirfd */
		assertEquals(1, fileDeletes.length);
		assertEquals(1, fileWrites.length);		
		assertEquals(tmpDir + "/fileToRename", fns.getPathName(fileDeletes[0]));
		assertEquals(PathType.TYPE_FILE, fns.getPathType(fileDeletes[0]));
		assertEquals(tmpDir + "/newName", fns.getPathName(fileWrites[0]));
		assertEquals(PathType.TYPE_FILE, fns.getPathType(fileWrites[0]));

		/*
		 * Rename a directory that exists
		 */
		assertTrue(new File(tmpDir, "dirToRename").mkdirs());
		traceOneProgram(
				"#include <stdio.h>\n" +
				"#include <fcntl.h>\n" +
				"int main() {" +
				"    int dirfd = open(\"" + tmpDir + "\", O_RDONLY);" +
				"    renameat(dirfd, \"dirToRename\", dirfd, \"newDirName\");" +
				"}", null);
		assertEquals(3, fileAccesses.length);
		assertEquals(1, fileReads.length);			/* opening the dirfd */
		assertEquals(1, fileDeletes.length);
		assertEquals(1, fileWrites.length);	
		assertEquals(tmpDir + "/dirToRename", fns.getPathName(fileDeletes[0]));
		assertEquals(PathType.TYPE_DIR, fns.getPathType(fileDeletes[0]));
		assertEquals(tmpDir + "/newDirName", fns.getPathName(fileWrites[0]));
		assertEquals(PathType.TYPE_DIR, fns.getPathType(fileWrites[0]));
		
		/*
		 * Rename a file that doesn't exist
		 */
		traceOneProgram(
				"#include <stdio.h>\n" +
				"#include <fcntl.h>\n" +
				"int main() {" +
				"    int dirfd = open(\"" + tmpDir + "\", O_RDONLY);" +
				"    renameat(dirfd, \"badFile\", dirfd, \"newBadName\");" +
				"}", null);
		assertEquals(1, fileAccesses.length); /* the dirfd open */
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the symlink() C function.
	 * @throws Exception
	 */
	@Test
	public void testSymlink() throws Exception {
		
		/*
		 * create a symbolic link to a valid file.
		 */
		assertTrue(new File(tmpDir, "oldFile1").createNewFile());
		traceOneProgram(
				"#include <unistd.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" +
				"  symlink(\"oldFile1\", \"linkFile1\");" +
				"}", null);
		assertEquals(2, fileAccesses.length);
		assertEquals(1, fileReads.length);
		assertEquals(1, fileWrites.length);
		assertEquals(tmpDir + "/oldFile1", fns.getPathName(fileReads[0]));
		assertEquals(PathType.TYPE_FILE, fns.getPathType(fileReads[0]));
		assertEquals(tmpDir + "/linkFile1", fns.getPathName(fileWrites[0]));
		assertEquals(PathType.TYPE_FILE, fns.getPathType(fileWrites[0]));

		/*
		 * create a symbolic link to a valid directory.
		 */
		assertTrue(new File(tmpDir, "oldDir1").mkdirs());
		traceOneProgram(
				"#include <unistd.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" +
				"  symlink(\"oldDir1\", \"linkDir1\");" +
				"}", null);
		assertEquals(2, fileAccesses.length);
		assertEquals(1, fileReads.length);
		assertEquals(1, fileWrites.length);
		assertEquals(tmpDir + "/oldDir1", fns.getPathName(fileReads[0]));
		assertEquals(PathType.TYPE_DIR, fns.getPathType(fileReads[0]));
		assertEquals(tmpDir + "/linkDir1", fns.getPathName(fileWrites[0]));
		assertEquals(PathType.TYPE_DIR, fns.getPathType(fileWrites[0]));
		
		/*
		 * Creation of a symlink to a non-existent file should still work
		 * (as opposed to hard link, which would fail).
		 */
		traceOneProgram(
				"#include <unistd.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" +
				"  symlink(\"badFile\", \"linkFile1a\");" +
				"}", null);
		assertEquals(2, fileAccesses.length);
		assertEquals(1, fileReads.length);
		assertEquals(1, fileWrites.length);
		assertEquals(tmpDir + "/badFile", fns.getPathName(fileReads[0]));
		assertEquals(tmpDir + "/linkFile1a", fns.getPathName(fileWrites[0]));
		
		/*
		 * create a symbolic link to a valid file, using a relative path. Make
		 * sure that the absolute path of the target file is relative to the
		 * source file's directory, not to the current directory.
		 */
		assertTrue(new File(tmpDir, "subdir1/subdir2").mkdirs());
		traceOneProgram(
				"#include <unistd.h>\n" +
				"int main() {" +
				"  chdir(\"" + tmpDir + "\");" +
				"  symlink(\"../../oldFile1\", \"subdir1/subdir2/linkFile1\");" +
				"}", null);
		assertEquals(2, fileAccesses.length);
		assertEquals(1, fileReads.length);
		assertEquals(1, fileWrites.length);
		assertEquals(tmpDir + "/oldFile1", fns.getPathName(fileReads[0]));
		assertEquals(PathType.TYPE_FILE, fns.getPathType(fileReads[0]));
		assertEquals(tmpDir + "/subdir1/subdir2/linkFile1", fns.getPathName(fileWrites[0]));
		assertEquals(PathType.TYPE_FILE, fns.getPathType(fileWrites[0]));
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the symlinkat() C function.
	 * @throws Exception
	 */
	@Test
	public void testSymlinkat() throws Exception {
		
		/*
		 * create a symbolic link to a valid file.
		 */
		assertTrue(new File(tmpDir, "oldFile2").createNewFile());
		traceOneProgram(
				"#include <unistd.h>\n" +
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  int dirfd = open(\"" + tmpDir + "\", O_RDONLY);" +
				"  symlinkat(\"" + tmpDir + "/oldFile2\", dirfd, \"linkFile2\");" +
				"}", null);
		assertEquals(3, fileAccesses.length);		/* include the dirfd open */
		assertEquals(1, fileWrites.length);
		assertEquals(tmpDir + "/linkFile2", fns.getPathName(fileWrites[0]));
		assertEquals(PathType.TYPE_FILE, fns.getPathType(fileWrites[0]));
		assertTrue(CommonTestUtils.sortedArraysEqual(fileReads, 
				new Integer[] { fns.getPath(tmpDir + "/oldFile2"),
								fns.getPath(tmpDir.toString()) }));

		/*
		 * create a symbolic link to a valid directory
		 */
		assertTrue(new File(tmpDir, "oldDir2").mkdirs());
		traceOneProgram(
				"#include <unistd.h>\n" +
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  int dirfd = open(\"" + tmpDir + "\", O_RDONLY);" +
				"  symlinkat(\"" + tmpDir + "/oldDir2\", dirfd, \"linkDir2\");" +
				"}", null);
		assertEquals(3, fileAccesses.length);		/* include the dirfd open */
		assertEquals(1, fileWrites.length);
		assertEquals(tmpDir + "/linkDir2", fns.getPathName(fileWrites[0]));
		assertEquals(PathType.TYPE_DIR, fns.getPathType(fileWrites[0]));
		assertTrue(CommonTestUtils.sortedArraysEqual(fileReads, 
				new Integer[] { fns.getPath(tmpDir + "/oldDir2"),
								fns.getPath(tmpDir.toString()) }));

		/*
		 * Creation of a symbolic link to a non-existent file should still work.
		 */
		traceOneProgram(
				"#include <unistd.h>\n" +
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  int dirfd = open(\"" + tmpDir + "\", O_RDONLY);" +
				"  symlinkat(\"" + tmpDir + "/badFile2\", dirfd, \"linkFile2a\");" +
				"}", null);
		assertEquals(3, fileAccesses.length);		/* include the dirfd open */
		assertEquals(1, fileWrites.length);
		assertEquals(tmpDir + "/linkFile2a", fns.getPathName(fileWrites[0]));
		assertTrue(CommonTestUtils.sortedArraysEqual(fileReads, 
				new Integer[] { fns.getPath(tmpDir + "/badFile2"),
								fns.getPath(tmpDir.toString()) }));
		
		/*
		 * create a symbolic link to a valid file where the target file is
		 * relative to the source file's directory, rather than the current
		 * directory.
		 */
		assertTrue(new File(tmpDir, "subdir1/subdir2").mkdirs());
		traceOneProgram(
				"#include <unistd.h>\n" +
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  int dirfd = open(\"" + tmpDir + "\", O_RDONLY);" +
				"  symlinkat(\"../../oldFile2\", dirfd, \"subdir1/subdir2/linkFile2\");" +
				"}", null);
		assertEquals(3, fileAccesses.length);		/* include the dirfd open */
		assertEquals(1, fileWrites.length);
		assertEquals(tmpDir + "/subdir1/subdir2/linkFile2", fns.getPathName(fileWrites[0]));
		assertEquals(PathType.TYPE_FILE, fns.getPathType(fileWrites[0]));
		assertTrue(CommonTestUtils.sortedArraysEqual(fileReads, 
				new Integer[] { fns.getPath(tmpDir + "/oldFile2"),
								fns.getPath(tmpDir.toString()) }));

	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the unlink() C function.
	 * @throws Exception
	 */
	@Test
	public void testUnlink() throws Exception {
		
		/*
		 * Unlink a valid file.
		 */
		assertTrue(new File(tmpDir, "fileToDelete").createNewFile());
		traceOneProgram(
				"#include <unistd.h>\n" +
				"int main() {" +
				"  unlink(\"" + tmpDir + "/fileToDelete\");" +
				"}", null);
		assertEquals(1, fileAccesses.length);
		assertEquals(1, fileDeletes.length);
		assertEquals(tmpDir + "/fileToDelete", fns.getPathName(fileDeletes[0]));
		
		/*
		 * Unlink a file that doesn't exist.
		 */
		traceOneProgram(
				"#include <unistd.h>\n" +
				"int main() {" +
				"  unlink(\"" + tmpDir + "/invalidFile\");" +
				"}", null);
		assertEquals(0, fileAccesses.length);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Test the unlinkat() C function.
	 * @throws Exception
	 */
	@Test
	public void testUnlinkat() throws Exception {
		
		/*
		 * Unlinkat a valid file.
		 */
		assertTrue(new File(tmpDir, "fileToDelete").createNewFile());
		traceOneProgram(
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  int dirfd = open(\"" + tmpDir + "\", O_RDONLY);" +
				"  unlinkat(dirfd, \"fileToDelete\", 0);" +
				"}", null);
		assertEquals(2, fileAccesses.length);
		assertEquals(1, fileDeletes.length);
		assertEquals(tmpDir + "/fileToDelete", fns.getPathName(fileDeletes[0]));
		assertEquals(PathType.TYPE_FILE, fns.getPathType(fileDeletes[0]));
		
		/*
		 * Unlinkat a valid directory (using the AT_REMOVEDIR flag).
		 */
		assertTrue(new File(tmpDir, "dirToDelete").mkdirs());
		traceOneProgram(
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  int dirfd = open(\"" + tmpDir + "\", O_RDONLY);" +
				"  unlinkat(dirfd, \"dirToDelete\", AT_REMOVEDIR);" +
				"}", null);
		assertEquals(2, fileAccesses.length);
		assertEquals(1, fileDeletes.length);
		assertEquals(tmpDir + "/dirToDelete", fns.getPathName(fileDeletes[0]));
		assertEquals(PathType.TYPE_DIR, fns.getPathType(fileDeletes[0]));
		
		/*
		 * Unlink a file that doesn't exist.
		 */
		traceOneProgram(
				"#include <fcntl.h>\n" +
				"int main() {" +
				"  int dirfd = open(\"" + tmpDir + "\", O_RDONLY);" +
				"  unlinkat(dirfd, \"invalidFile\", 0);" +
				"}", null);
		assertEquals(1, fileAccesses.length); /* only the dirfd open */
	}

	/*-------------------------------------------------------------------------------------*/

}
