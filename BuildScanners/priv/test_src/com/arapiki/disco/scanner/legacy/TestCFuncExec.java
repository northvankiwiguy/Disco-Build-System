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


import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Basic testing that the LegacyBuildScanner can produce a valid
 * BuildStore. There are many test cases, split over multiple
 * test case files, with this file testing C Functions that
 * perform an exec()-like operation.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class TestCFuncExec {

	/*-------------------------------------------------------------------------------------*/
	
	@Test
	public void testNewProgram() throws Exception {

//		/* execute the program, and get the BuildStore, etc into our instance members */
//		traceOneProgram("int main() { }", null);
//
//		/* 
//		 * The command line should end with /prog (note that our test harness executes it
//		 * as /tmp/cfsXXXXXXXXX/prog), so we ignore the first part.
//		 */
//		String commandName = bts.getCommand(task);
//		assertTrue(commandName.endsWith("/prog"));
//		
//		/* this task has no file accesses */
//		assertEquals(0, fileAccesses.length);
		fail("Not implemented.");
	}
	
	/*-------------------------------------------------------------------------------------*/

	
	@Test
	public void testNewProgramWithArgs() throws Exception
	{
//
//		/* execute the program, and get the BuildStore, etc into our instance members */
//		traceOneProgram("int main() { }", new String[] {"-c", "aardvark", "22", "elephant"});
//
//		/* 
//		 * The command line should end with /prog (note that our test harness executes it
//		 * as /tmp/cfsXXXXXXXXX/prog), so we ignore the first part.
//		 */
//		String commandName = bts.getCommand(task);
//		assertTrue(commandName.endsWith("/prog -c aardvark 22 elephant"));
		fail("Not implemented.");

	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the execl() C function.
	 * @throws Exception
	 */
	@Test
	public void testExecl() throws Exception {
		//int execl(const char *path, const char *arg0, ...)
		fail("Not implemented.");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the execle() C function.
	 * @throws Exception
	 */
	@Test
	public void testExecle() throws Exception {
		//int execle(const char *path, const char *arg0, ... /*, 0, char *const envp[] */)
		fail("Not implemented.");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the execlp() C function.
	 * @throws Exception
	 */
	@Test
	public void testExeclp() throws Exception {
		//int execlp(const char *file, const char *arg0, ...)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the execv() C function.
	 * @throws Exception
	 */
	@Test
	public void testExecv() throws Exception {
		//int execv(const char *path, char *const argv[])
		fail("Not implemented.");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the execve() C function.
	 * @throws Exception
	 */
	@Test
	public void testExecve() throws Exception {
		//int execve(const char *filename, char *const argv[], char *const envp[])
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the execvp() C function.
	 * @throws Exception
	 */
	@Test
	public void testExecvp() throws Exception {
		//int execvp(const char *file, char *const argv[])
		fail("Not implemented.");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the execvpe() C function.
	 * @throws Exception
	 */
	@Test
	public void testExecvpe() throws Exception {
		//int execvpe(const char *file, char *const argv[], char *const envp[])
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the exit() C function.
	 * @throws Exception
	 */
	@Test
	public void testExit() throws Exception {
		//void exit(int status)
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the _exit() C function.
	 * @throws Exception
	 */
	@Test
	public void test_exit() throws Exception {
		//void _exit(int status)
		fail("Not implemented.");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the _Exit() C function.
	 * @throws Exception
	 */
	@Test
	public void test_Exit() throws Exception {
		//void _Exit(int status)
		fail("Not implemented.");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the fexecve() C function.
	 * @throws Exception
	 */
	@Test
	public void testFexecve() throws Exception {
		//int fexecve(int fd, char *const argv[], char *const envp[])
		fail("Not implemented.");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the fork() C function.
	 * @throws Exception
	 */
	@Test
	public void testFork() throws Exception {
		//pid_t fork(void)
		fail("Not implemented.");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the popen() C function.
	 * @throws Exception
	 */
	@Test
	public void testPopen() throws Exception {
		//FILE *popen(const char *command, const char *mode)
		fail("Not implemented.");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the posix_spawn() C function.
	 * @throws Exception
	 */
	@Test
	public void testPosix_spawn() throws Exception {
		//int posix_spawn(pid_t *pid, const char *path,
		//	       const posix_spawn_file_actions_t *file_actions,
		//	       const posix_spawnattr_t *attrp,
		//	       char *const argv[], char *const envp[])
		fail("Not implemented.");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the posix_spawnp() C function.
	 * @throws Exception
	 */
	@Test
	public void testPosix_spawnp() throws Exception {
		//int posix_spawnp(pid_t *pid, const char *file,
		//	       const posix_spawn_file_actions_t *file_actions,
		//	       const posix_spawnattr_t *attrp,
		//	       char *const argv[], char * const envp[])
		fail("Not implemented.");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the system() C function.
	 * @throws Exception
	 */
	@Test
	public void testSystem() throws Exception {
		//int system(const char *command)
		fail("Not implemented.");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test the vfork() C function.
	 * @throws Exception
	 */
	@Test
	public void testVfork() throws Exception {
		//pid_t vfork(void)
		fail("Not implemented.");
	}
	
	/*-------------------------------------------------------------------------------------*/
}

