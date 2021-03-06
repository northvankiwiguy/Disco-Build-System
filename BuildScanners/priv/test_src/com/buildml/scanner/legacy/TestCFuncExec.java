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

import com.buildml.model.CommonTestUtils;
import com.buildml.model.IActionMgr;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.utils.errors.ErrorCode;
import com.buildml.utils.os.SystemUtils;
import com.buildml.utils.string.PathUtils;

/**
 * Basic testing that the LegacyBuildScanner can produce a valid
 * BuildStore. There are many test cases, split over multiple
 * test case files, with this file testing C Functions that
 * perform an exec()-like operation.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestCFuncExec {

	/* variables used in many test cases */
	private IBuildStore bs = null;
	private IActionMgr actionMgr = null;
	private IFileMgr fileMgr = null;
	private int shellActionId;
	
	/** temporary directory into which test cases can store files */
	private File tmpDir;

	/*=====================================================================================*
	 * Helper methods
	 *=====================================================================================*/

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
	 * A helper method for tracing a program that uses exec()-like system calls to 
	 * invoke a sub-process. The sub-process (hard-coded in this function) checks
	 * environment variables and command line arguments to determine whether the
	 * exec() call operated correctly.
	 * 
	 * @param extraLines The lines of code that perform the exec()-like function.
	 * @param needFork Should we fork() before running the exec()-like function?
	 * @throws Exception
	 */
	private void traceExecProgram(String extraLines, boolean needFork) throws Exception {
		
		/*
		 * This is the source code for the child program. It validates the environment
		 * variables and command line arguments, returning 123 on success, or some
		 * other number if the variables/arguments are wrong.
		 * Also, if successful create /tmp/flag-file3 as a way for our unit tests to
		 * double-check that the process executed.
		 */
		String childSource = 
			"#include <stdio.h>\n" +
			"#include <stdlib.h>\n" +
			"int main(int argc, char *argv[]) {" +
			"char *value = getenv(\"MY_VAR_1\");" +
			"if ((value == NULL) || (strcmp(value, \"value1\") != 0)){ return 3; }" +
			"value = getenv(\"SECOND_VARIABLE\");" +
			"if ((value == NULL) || (strcmp(value, \"13579\") != 0)){ return 4; }" +
			"if (argc != 4){ return 5; }" +
			"if (strcmp(argv[1], \"arg1\") != 0){ return 6; }" +
			"if (strcmp(argv[2], \"arg2\") != 0){ return 7; }" +
			"if (strcmp(argv[3], \"arg3\") != 0){ return 8; }" +
			"creat(\"/tmp/flag-file3\", 0666);"+
			"return 123;}";
		
		/* compile this hard-coded child program */
		String childPath = BuildScannersCommonTestUtils.compileProgram(tmpDir, "child", childSource);

		/* Substitute the child's path into the extraLines that the caller provided */
		extraLines = extraLines.replaceAll("INSERT_PATH", childPath);
		
		/*
		 * The parent process sets up the environment variables and command line arguments to be
		 * passed to the child process. Also, write to /tmp/flag-file1 and /tmp/flag-file2 as 
		 * a means of indicating to our test cases that progress is being made.
		 */
		String parentSource = 
			"#include <fcntl.h>\n" +
			"#include <stdlib.h>\n" +
			"#include <unistd.h>\n" +
			"#include <sys/wait.h>\n" +
			"extern char **environ;" +
			"char *tmp_argv[] = {\"child\", \"arg1\", \"arg2\", \"arg3\", 0};"+
			"int main() {" +
			"  int status;" +
			"  setenv(\"MY_VAR_1\", \"value1\", 1);" +
			"  setenv(\"SECOND_VARIABLE\", \"13579\", 1);" +
			"  creat(\"/tmp/flag-file1\", 0666);";
		
		/*
		 * If the exec()-like function expects to be run within the child process,
		 * we must fork() first. For functions like posix_spawn(), we don't need
		 * to fork().
		 */
		if (needFork) {
			parentSource += 
				"  switch (fork()) {" +
				"    case 0:" +
				"      " + extraLines +
				"      exit(1);" +
				"    case -1:" +
				"      exit(2);" +
				"    default:" +
				"      wait(&status);" +
				"      if (WEXITSTATUS(status) == 123) { creat(\"/tmp/flag-file2\", 0666); }" +
				"  }" +
				"  return 0;" +
		    	"}";
		} else {
			parentSource +=
				"  " + extraLines +
				"  return 0;" +
				"};";
		}

		/*
		 * Run the parent program, and trace the parent and child behaviour into a BuildStore.
		 */
		bs = BuildScannersCommonTestUtils.parseLegacyProgram(tmpDir, parentSource, null);

		/* fetch references to sub objects */
		actionMgr = bs.getActionMgr();
		fileMgr = bs.getFileMgr();
		
		/* find the root action */
		int rootAction = actionMgr.getRootAction("root");
		
		/* 
		 * There should only be one top-level action (parent), and it should contain a 
		 * single second-level action (child). The second-level action has no children.
		 */
		Integer actions[] = actionMgr.getChildren(rootAction);
		assertEquals(1, actions.length);
		int parentActionId = actions[0].intValue();
		actions = actionMgr.getChildren(parentActionId);
		assertEquals(1, actions.length);
		int childActionId = actions[0].intValue();
		actions = actionMgr.getChildren(childActionId);
		assertEquals(0, actions.length);

		/* 
		 * Now validate the file access patterns. The parent action wrote to /tmp/flag-file1
		 * and /tmp/flag-file2. 
		 */		
		Integer[] fileWrites = actionMgr.getFilesAccessed(parentActionId, OperationType.OP_WRITE);
		assertEquals(2, fileWrites.length);
		int flag1Id = fileMgr.getPath("/tmp/flag-file1");
		int flag2Id = fileMgr.getPath("/tmp/flag-file2");
		assertNotSame(ErrorCode.BAD_PATH, flag1Id);
		assertNotSame(ErrorCode.BAD_PATH, flag2Id);
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] {flag1Id, flag2Id}, fileWrites));
		
		/* And the child action wrote to /tmp/flag-file3 */
		fileWrites = actionMgr.getFilesAccessed(childActionId, OperationType.OP_WRITE);
		assertEquals(1, fileWrites.length);
		int flag3Id = fileMgr.getPath("/tmp/flag-file3");
		assertNotSame(ErrorCode.BAD_PATH, flag3Id);
		assertTrue(CommonTestUtils.sortedArraysEqual(new Integer[] {flag3Id}, fileWrites));
		
		/*
		 * Validate that the actions executed in the expected directory.
		 */
		int currentDirId = 
			fileMgr.getPath(PathUtils.normalizeAbsolutePath(new File(".").getAbsolutePath()));
		int parentDirId = (Integer) actionMgr.getSlotValue(parentActionId, IActionMgr.DIRECTORY_SLOT_ID);
		assertEquals(currentDirId, parentDirId);
		int childDirId = (Integer) actionMgr.getSlotValue(childActionId, IActionMgr.DIRECTORY_SLOT_ID);
		assertEquals(currentDirId, childDirId);
		
		/*
		 * Validate that the child command's arguments are correct. That is, the absolute
		 * path name to the child, followed by the expected arguments. 
		 */
		String cmdLine = (String) actionMgr.getSlotValue(childActionId, IActionMgr.COMMAND_SLOT_ID);
		assertEquals(childPath + " arg1 arg2 arg3", cmdLine);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * A helper method for tracing the behaviour of system()-like functions. This is
	 * similar to traceExecProgram(), but a lot less plumbing is required.
	 * @param extraLines The command to be passed into the system() call.
	 * @throws Exception 
	 */
	private void traceSystemProgram(String extraLines) throws Exception {

		/* the source for the program is very simple, and we ignore return code */
		String source = 
			"int main() {" +
			"  system(\"" + extraLines + "\");" + 
			"  return 0;" +
			"}";
	
		/* trace the program's behaviour into a BuildStore */
		bs = BuildScannersCommonTestUtils.parseLegacyProgram(tmpDir, source, null);
		fileMgr = bs.getFileMgr();
		actionMgr = bs.getActionMgr();
		
		/* validate the top-level action (the process that invokes "system"). */
		Integer [] actions = actionMgr.getChildren(actionMgr.getRootAction("root"));
		assertEquals(1, actions.length);
		int systemActionId = actions[0].intValue();
		
		/* the system action has a single child (the shell action). */
		actions = actionMgr.getChildren(systemActionId);
		assertEquals(1, actions.length);
		shellActionId = actions[0].intValue();
	}
	
	/*=====================================================================================*
	 * Test cases for C-library functions.
	 *=====================================================================================*/

	/**
	 * Test the execl() C function.
	 * @throws Exception
	 */
	@Test
	public void testExecl() throws Exception {
		traceExecProgram(
				"execl(\"INSERT_PATH\", \"child\", \"arg1\", \"arg2\", \"arg3\", 0);",
				true);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the execl() C function with the LD_PRELOAD environment variable removed. It
	 * should be reintroduced before the child process is invoked.
	 * @throws Exception
	 */
	@Test
	public void testExeclRemovePreload() throws Exception {		
		traceExecProgram(
				"unsetenv(\"LD_PRELOAD\");" +
				"execl(\"INSERT_PATH\", \"child\", \"arg1\", \"arg2\", \"arg3\", 0);",
				true);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the execl() C function with the LD_PRELOAD environment variable already set to
	 * some other value. A warning message should be provided.
	 * @throws Exception
	 */
	@Test
	public void testExeclOverridePreload() throws Exception {		
		traceExecProgram(
				"setenv(\"LD_PRELOAD\", \"/usr/lib/libc.so\", 1);" +
				"execl(\"INSERT_PATH\", \"child\", \"arg1\", \"arg2\", \"arg3\", 0);",
				true);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the execl() C function with the CFS_ID environment variable removed. It should
	 * be reintroduced before the child process is invoked.
	 * @throws Exception
	 */
	@Test
	public void testExeclRemoveCfsId() throws Exception {
		traceExecProgram(
				"unsetenv(\"CFS_ID\");" +
				"execl(\"INSERT_PATH\", \"child\", \"arg1\", \"arg2\", \"arg3\", 0);",
				true);	
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the execl() C function with the CFS_PARENT_ID environment variable removed.
	 * It should be reintroduced before the child process is invoked.
	 * @throws Exception
	 */
	@Test
	public void testExeclRemoveCfsParentId() throws Exception {
		
		traceExecProgram(
				"unsetenv(\"CFS_PARENT_ID\");" +
				"execl(\"INSERT_PATH\", \"child\", \"arg1\", \"arg2\", \"arg3\", 0);",
				true);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the execle() C function.
	 * @throws Exception
	 */
	@Test
	public void testExecle() throws Exception {
		traceExecProgram(
			"execle(\"INSERT_PATH\", \"child\", \"arg1\", \"arg2\", \"arg3\", 0, environ);",
			true);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the execle() C function, with a couple of empty strings as arguments. Note that
	 * we don't care about the return value, but if empty arguments aren't accepted, this
	 * code will crash.
	 * @throws Exception
	 */
	@Test
	public void testExecleWithNullArgs() throws Exception {

		String source = 
			"#include <unistd.h>\n" +
			"extern char **environ;" +
			"int main() {" +
			"   execle(\"true\", \"true\", \"arg1\", \"\", \"arg3\", \"\", 0, environ);" +
			"   return 0;" +
			"}";

		bs = BuildScannersCommonTestUtils.parseLegacyProgram(tmpDir, source, null);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the execlp() C function.
	 * @throws Exception
	 */
	@Test
	public void testExeclp() throws Exception {
		traceExecProgram(
			"execlp(\"INSERT_PATH\", \"child\", \"arg1\", \"arg2\", \"arg3\", 0);",
			true);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the execv() C function.
	 * @throws Exception
	 */
	@Test
	public void testExecv() throws Exception {
		traceExecProgram("execv(\"INSERT_PATH\", tmp_argv);", true);		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the execve() C function.
	 * @throws Exception
	 */
	@Test
	public void testExecve() throws Exception {
		traceExecProgram("execve(\"INSERT_PATH\", tmp_argv, environ);", true);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the execvp() C function.
	 * @throws Exception
	 */
	@Test
	public void testExecvp() throws Exception {
		traceExecProgram("execvp(\"INSERT_PATH\", tmp_argv);", true);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the execvpe() C function.
	 * @throws Exception
	 */
	@Test
	public void testExecvpe() throws Exception {
		traceExecProgram("execvpe(\"INSERT_PATH\", tmp_argv, environ);", true);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the fexecve() C function.
	 * @throws Exception
	 */
	@Test
	public void testFexecve() throws Exception {
		traceExecProgram(
				"{ " +
				"  int fd = open(\"INSERT_PATH\", O_RDONLY);" +
				"  fexecve(fd, tmp_argv, environ);" +
				"}", true);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Common code for testing the fork() and vfork() functions.
	 * @param func The function to test ("fork" or "vfork").
	 * @throws Exception
	 */
	public void testForkCmn(String func) throws Exception {
		
		String source = 
			"int main() {" +
			"  if (" + func + "() == 0) {" +
			"    creat(\"/tmp/flag-file1\", 0666);" +
			"  } else {" +
			"    creat(\"/tmp/flag-file2\", 0666);" +
			"    int status;" +
			"    wait(&status);" +
			"  }" +
			"  return 0;" +
			"}";
		
		bs = BuildScannersCommonTestUtils.parseLegacyProgram(tmpDir, source, null);

		fileMgr = bs.getFileMgr();
		actionMgr = bs.getActionMgr();
		
		/* validate the top-level action (the process that invokes fork()). */
		Integer [] actions = actionMgr.getChildren(actionMgr.getRootAction("root"));
		assertEquals(1, actions.length);
		int forkActionId = actions[0].intValue();
		
		/* check that this action accessed both flag-file1 and flag-file2 */
		int file1Id = fileMgr.getPath("/tmp/flag-file1");
		int file2Id = fileMgr.getPath("/tmp/flag-file2");
		Integer fileWrites[] = actionMgr.getFilesAccessed(forkActionId, OperationType.OP_WRITE);
		assertTrue(CommonTestUtils.sortedArraysEqual(fileWrites, new Integer[] {file1Id, file2Id}));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the fork() C function.
	 * @throws Exception
	 */
	@Test
	public void testFork() throws Exception {
		testForkCmn("fork");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the popen() C function.
	 * @throws Exception
	 */
	@Test
	public void testPopen() throws Exception {

		String source = 
			"#include <stdio.h>\n" +
			"int main() {" +
			"  FILE *f = popen(\"echo Hello World\", \"r\");" + 
			"  fclose(f);" +
			"  return 0;" +
			"}";
	
		bs = BuildScannersCommonTestUtils.parseLegacyProgram(tmpDir, source, null);
		fileMgr = bs.getFileMgr();
		actionMgr = bs.getActionMgr();
		
		/* validate the top-level action (the process that invokes "popen"). */
		Integer [] actions = actionMgr.getChildren(actionMgr.getRootAction("root"));
		assertEquals(1, actions.length);
		int popenActionId = actions[0].intValue();
		
		/* the popen action has a single child (the shell action). */
		actions = actionMgr.getChildren(popenActionId);
		assertEquals(1, actions.length);
		shellActionId = actions[0].intValue();
		
		/* the shell action has no child ("echo" is built-in to the shell */
		actions = actionMgr.getChildren(shellActionId);
		assertEquals(0, actions.length);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the posix_spawn() C function.
	 * @throws Exception
	 */
	@Test
	public void testPosix_spawn() throws Exception {
		traceExecProgram(
				"{ " +
				"  pid_t pid;" + 
				"  int status;" +
				"  posix_spawn(&pid, \"INSERT_PATH\", NULL, NULL, tmp_argv, environ);" +
				"  wait(&status);" +
				"  if (WEXITSTATUS(status) == 123) { creat(\"/tmp/flag-file2\", 0666); }" +
				"}", false);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the posix_spawnp() C function.
	 * @throws Exception
	 */
	@Test
	public void testPosix_spawnp() throws Exception {
		traceExecProgram(
				"{ " +
				"  pid_t pid;" + 
				"  int status;" +
				"  posix_spawnp(&pid, \"INSERT_PATH\", NULL, NULL, tmp_argv, environ);" +
				"  wait(&status);" +
				"  if (WEXITSTATUS(status) == 123) { creat(\"/tmp/flag-file2\", 0666); }" +
				"}", false);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the system() C function with a single shell command (without redirection or pipes).
	 * This creates a top-level action which calls system() to create a shell action, which exec()'s
	 * itself (without forking) to run the underlying command action. That gives three actions in total.
	 * @throws Exception
	 */
	@Test
	public void testSystem() throws Exception {
		
		/* The command to be tested... */
		traceSystemProgram("cp /etc/passwd /tmp/flag-file4");
	
		/* the shell action has a single child (the underlying command). */
		Integer actions[] = actionMgr.getChildren(shellActionId);
		assertEquals(1, actions.length);
		int cmdActionId = actions[0].intValue();
		
		/* the command action has no children */
		actions = actionMgr.getChildren(cmdActionId);
		assertEquals(0, actions.length);
		
		/* check that /etc/passwd and /tmp/flag-file4 have been accessed. */
		assertNotSame(ErrorCode.BAD_PATH, fileMgr.getPath("/etc/passwd"));
		assertNotSame(ErrorCode.BAD_PATH, fileMgr.getPath("/tmp/flag-file4"));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the system() C function, with standard output being redirected to a file.
	 * @throws Exception 
	 */
	@Test
	public void testSystemWithRedirect() throws Exception {

		/* The command to be tested... */
		traceSystemProgram("cat /etc/passwd >/tmp/flag-file4");

		/* the shell action has a single child (the underlying command). */
		Integer actions[] = actionMgr.getChildren(shellActionId);
		assertEquals(1, actions.length);
		int cmdActionId = actions[0].intValue();
		
		/* the command action has no children */
		actions = actionMgr.getChildren(cmdActionId);
		assertEquals(0, actions.length);
		
		/* check that /etc/passwd and /tmp/flag-file4 have been accessed. */
		assertNotSame(ErrorCode.BAD_PATH, fileMgr.getPath("/etc/passwd"));
		assertNotSame(ErrorCode.BAD_PATH, fileMgr.getPath("/tmp/flag-file4"));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the system() C function, with two commands connected by a pipe.
	 * @throws Exception 
	 */
	@Test
	public void testSystemWithPipe() throws Exception {
		
		/* The command to be tested... */
		traceSystemProgram("cat /etc/passwd | wc -l > /dev/null");

		/* the shell action has two children (cat and wc). */
		Integer actions[] = actionMgr.getChildren(shellActionId);
		assertEquals(2, actions.length);
		
		/* neither of these actions have children */
		assertEquals(0, actionMgr.getChildren(actions[0].intValue()).length);
		assertEquals(0, actionMgr.getChildren(actions[1].intValue()).length);

		/* check that /etc/passwd has been accessed. */
		assertNotSame(ErrorCode.BAD_PATH, fileMgr.getPath("/etc/passwd"));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the system() C function, with two commands connected with &&
	 * @throws Exception 
	 */
	@Test
	public void testSystemWithAndAnd() throws Exception {
		
		/* The command to be tested... */
		traceSystemProgram("cat /etc/passwd > /dev/null && cat /etc/group >/dev/null");

		/* the shell action has two children (cat and cat). */
		Integer actions[] = actionMgr.getChildren(shellActionId);
		assertEquals(2, actions.length);
		
		/* neither of these actions have children */
		assertEquals(0, actionMgr.getChildren(actions[0].intValue()).length);
		assertEquals(0, actionMgr.getChildren(actions[1].intValue()).length);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the vfork() C function.
	 * @throws Exception
	 */
	@Test
	public void testVfork() throws Exception {
		testForkCmn("vfork");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test an exec call that fails. No child process should be registered.
	 * @throws Exception
	 */
	@Test
	public void testExecFailure() throws Exception {

		String source = 
			"int main() {" +
			"	execl(\"/bad-path\", \"bad-path\", 0);" +
			"   return 0;" +
			"}";
		
		IBuildStore bs = BuildScannersCommonTestUtils.parseLegacyProgram(tmpDir, source, null);
		IActionMgr actionMgr = bs.getActionMgr();
		
		/* there should be one top-level action (the one we just ran). */
		int rootAction = actionMgr.getRootAction("root");
		Integer children[] = actionMgr.getChildren(rootAction);
		assertEquals(1, children.length);
		
		/* there should be no second level actions */
		int parentAction = children[0].intValue();
		children = actionMgr.getChildren(parentAction);
		assertEquals(0, children.length);
	}
	
	/*-------------------------------------------------------------------------------------*/
}

