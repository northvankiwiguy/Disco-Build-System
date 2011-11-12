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

package com.arapiki.utils.os;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.arapiki.utils.os.FileSystemTraverseCallback;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestSystemUtils {

	/** A temporary file that contains a simple executable program */
	private static File ourTempExe;
	
	/**
	 * A temporary directory that we'll fill up with interesting files and directories.
	 * This is used for testing the traverseFileSystem() method.
	 */
	private static File ourTempDir;
	
	/**
	 * This method is called before any of test methods are called, but
	 * it's only called once per test suite.
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
		/*
		 * Create a short perl script for test purposes. This code looks cryptic,
		 * but serves to exercise the stdin/stdout/stderr streams. This script takes
		 * two arguments:
		 *    1) The number of letters to write on the stdout, and equally, the number of
		 *       digits to write on the stderr.
		 *    2) The exit code to terminate with.
		 * This script starts by echoing it's stdin to its stdout, then displays N letters/digits
		 * on the stdout/stderr respectively.
		 */
		ourTempExe = File.createTempFile("tempExecutable", null);
		PrintStream tmpStream = new PrintStream(ourTempExe);
		tmpStream.println("#!/usr/bin/perl");
		tmpStream.println("while (<STDIN>) { print $_; };");
		tmpStream.println("my $count = $ARGV[0];");	
		tmpStream.println("my $rc = $ARGV[1];");	
		tmpStream.println("for (my $i = 0; $i != $count; $i++) {");
		tmpStream.println("  my $ch = 65 + ($i % 26);");
		tmpStream.println("  my $num = ($i % 10);");
		tmpStream.println("  print STDOUT chr($ch);");
		tmpStream.println("  print STDERR $num;");
		tmpStream.println("}");
		tmpStream.println("exit($rc);");
		tmpStream.close();
		
		/* set execute permission - only works on Unix */
		ourTempExe.setExecutable(true);
		
		/*
		 * Next, create a temporary directory full of files and subdirectories, for the purpose of
		 * testing traverseFileSystem(). Note, we create a temporary directory name by first creating
		 * a temporary file, then stealing (reusing) the same name for a directory.
		 */	
		ourTempDir = File.createTempFile("tempDir", null);
		ourTempDir.delete();
		ourTempDir.mkdir();
		
		/* create some subdirectories */
		File dirA = new File(ourTempDir.getPath() + "/dirA");
		File dirB = new File(ourTempDir.getPath() + "/dirA/nested/dirB");
		File dirC = new File(ourTempDir.getPath() + "/dirC");
		dirA.mkdirs();
		dirB.mkdirs();
		dirC.mkdirs();
		
		new File(ourTempDir + "/topLevelFile").createNewFile();
		new File(dirA + "/fileInDirA").createNewFile();
		new File(dirB + "/fileInDirB").createNewFile();
		new File(dirB + "/anotherFileInDirB").createNewFile();
		new File(dirB + "/aThirdFileInDirB").createNewFile();
		new File(dirB + "/onelastFileInDirB").createNewFile();
	}

	/**
	 * This method is called once (and only once) when all test cases have completed.
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		/* remove our short perl script */
		ourTempExe.delete();
		
		/* remove our temporary directory */
		Runtime.getRuntime().exec("rm -r " + ourTempDir);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for {@link com.arapiki.utils.os.SystemUtils#executeShellCmd(java.lang.String, java.lang.String)}.
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	@Test
	public void testExecuteShellCmd() throws IOException, InterruptedException {

		ShellResult sr;
		
		/* Execute an invalid command - should return an IOException */
		try {
			sr = SystemUtils.executeShellCmd("/blah", "Hello World\n");
			fail("Failed to throw IOException when executing invalid command");
		} catch (IOException ex) {
			/* passed */
		}
		
		/* request a specific error code - our perl script always does exit() with it's second argument */
		sr = SystemUtils.executeShellCmd(ourTempExe + " 0 23", "\n");
		assertEquals(sr.getReturnCode(), 23);
		
		/* Simply echo back our stdin - the stdout should be identical to the stdin we provided. */
		sr = SystemUtils.executeShellCmd(ourTempExe + " 0 0", "Hello World\n");
		assertEquals(sr.getReturnCode(), 0);
		assertEquals("Hello World\n", sr.getStdout());
		assertEquals("", sr.getStderr());
	
		/* Same, but with multiple lines of text. */
		sr = SystemUtils.executeShellCmd(ourTempExe + " 0 0", "Hello World\nHow are you?\n");
		assertEquals(sr.getReturnCode(), 0);
		assertEquals("Hello World\nHow are you?\n", sr.getStdout());
		assertEquals("", sr.getStderr());

		/* 
		 * Now get the program to generate some of its own output - that is, 5 letters on stdout 
		 * and 5 digits on stderr.
		 */
		sr = SystemUtils.executeShellCmd(ourTempExe + " 5 0", "Hi\n");
		assertEquals(sr.getReturnCode(), 0);
		assertEquals("Hi\nABCDE\n", sr.getStdout());
		assertEquals("01234\n", sr.getStderr());
		
		/* 
		 * Now try a really really big case, where the stdout and stderr will certainly be intermingled.
		 */
		int count = 250000;
		sr = SystemUtils.executeShellCmd(ourTempExe + " " + count, "");
		assertEquals(sr.getReturnCode(), 0);
		
		/* first, check the lengths */
		String stdOut = sr.getStdout();
		String stdErr = sr.getStderr();
		assertEquals(count + 1, stdOut.length());  /* include the trailing \n */
		assertEquals(count + 1, stdErr.length());
		
		/* now check each individual character of what was returned */
		for (int i = 0; i != count; i++) {
			char ch = stdOut.charAt(i);
			int num = (int)stdErr.charAt(i) - '0';
			assertEquals('A' + (i % 26), ch);
			assertEquals(i % 10, num);
		}
		
		/*
		 * Test the non-buffering variant. Even though there's output, we shouldn't get any
		 * of it.
		 */
		sr = SystemUtils.executeShellCmd(ourTempExe + " 0 0", "Hello World\n", false, false);
		assertEquals(sr.getReturnCode(), 0);
		assertEquals("", sr.getStdout());
		assertEquals("", sr.getStderr());
		
		/*
		 * Note: we can't automatically test the echoToOutput option, unless we had a way to
		 * observe our own standard output.
		 */
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test class for capturing file system traversal callbacks. As we encounter names,
	 * gather them in a list, ready to be returned for later analysis.
	 */
	private class TestCallback extends FileSystemTraverseCallback {
		
		/** For accumulating path names as we encounter them */
		private ArrayList<String> names = new ArrayList<String>();
		
		@Override
		public void callback(File thisPath) {
			names.add(thisPath.toString());
		}
		
		/**
		 * @return The names we've captured.
		 */
		public String [] getNames() {
			String result[] = names.toArray(new String[0]);
			names = new ArrayList<String>();
			return result;
		}
	};
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Test method for traverseFileSystem
	 * @throws Exception
	 */
	@Test
	public void testTraverseFileSystem() throws Exception {

		/*
		 * Create an object of type FileSystemTraverseCallback that'll be called for each
		 * file system path that's reported. Calling getNames() returns an array of Strings
		 * containing the file names.
		 */
		TestCallback callbackCollector = new TestCallback();
		
		/* Test a non-existent directory */
		SystemUtils.traverseFileSystem("/non-existent", SystemUtils.REPORT_DIRECTORIES | SystemUtils.REPORT_FILES,
				callbackCollector);
		String names[] = callbackCollector.getNames();
		assertEquals(0, names.length);
		
		/* report all directories in our test hierarchy */
		SystemUtils.traverseFileSystem(ourTempDir.toString(), SystemUtils.REPORT_DIRECTORIES, callbackCollector);
		names = callbackCollector.getNames();
		assertEquals(5, names.length);
		assertSortedPathArraysEqual(ourTempDir.toString(), names, 
				new String[] {"", "/dirA", "/dirA/nested", "/dirA/nested/dirB", "/dirC"});
		
		/* report all files in our test hierarchy */
		SystemUtils.traverseFileSystem(ourTempDir.toString(), SystemUtils.REPORT_FILES, callbackCollector);
		names = callbackCollector.getNames();
		assertSortedPathArraysEqual(ourTempDir.toString(), names, 
				new String[] {"/dirA/fileInDirA", "/dirA/nested/dirB/aThirdFileInDirB", 
							  "/dirA/nested/dirB/anotherFileInDirB", "/dirA/nested/dirB/fileInDirB", 
							  "/dirA/nested/dirB/onelastFileInDirB", "/topLevelFile"});

		/* report all files and directories in our test hierarchy */
		SystemUtils.traverseFileSystem(ourTempDir.toString(), SystemUtils.REPORT_FILES | SystemUtils.REPORT_DIRECTORIES, 
				callbackCollector);
		names = callbackCollector.getNames();
		assertSortedPathArraysEqual(ourTempDir.toString(), names, 
				new String[] {"", "/dirA", "/dirA/fileInDirA", "/dirA/nested", "/dirA/nested/dirB",
							  "/dirA/nested/dirB/aThirdFileInDirB", 
							  "/dirA/nested/dirB/anotherFileInDirB", "/dirA/nested/dirB/fileInDirB", 
							  "/dirA/nested/dirB/onelastFileInDirB", "/dirC", "/topLevelFile"});

		/* filter out the "nested" directory */
		SystemUtils.traverseFileSystem(ourTempDir.toString(), 
				null,
				"nested",
				SystemUtils.REPORT_FILES | SystemUtils.REPORT_DIRECTORIES, 
				callbackCollector);
		names = callbackCollector.getNames();
		assertSortedPathArraysEqual(ourTempDir.toString(), names, 
				new String[] {"", "/dirA", "/dirA/fileInDirA", "/dirC", "/topLevelFile"});

		/* filter out the "nested" and "dirC" directories */
		SystemUtils.traverseFileSystem(ourTempDir.toString(), 
				null,
				"nested|dirC",
				SystemUtils.REPORT_FILES | SystemUtils.REPORT_DIRECTORIES, 
				callbackCollector);
		names = callbackCollector.getNames();
		assertSortedPathArraysEqual(ourTempDir.toString(), names, 
				new String[] {"", "/dirA", "/dirA/fileInDirA", "/topLevelFile"});

		/* only return the files that have "In" in their name */
		SystemUtils.traverseFileSystem(ourTempDir.toString(),
				".*In.*",
				null,
				SystemUtils.REPORT_FILES, 
				callbackCollector);
		names = callbackCollector.getNames();
		assertSortedPathArraysEqual(ourTempDir.toString(), names, 
				new String[] {"/dirA/fileInDirA", "/dirA/nested/dirB/aThirdFileInDirB", 
							  "/dirA/nested/dirB/anotherFileInDirB", "/dirA/nested/dirB/fileInDirB", 
							  "/dirA/nested/dirB/onelastFileInDirB"});
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Helper method to determine whether two arrays of path names contain the same elements, 
	 * even if those elements aren't in the same order in both arrays. Also, remove a prefix
	 * from each path in arr1 before comparing. 
	 * @param arr1prefix The String prefix to remove
	 * @param arr1 The first array of Strings
	 * @param arr2 The second array of Strings
	 */
	public static void assertSortedPathArraysEqual(String arr1prefix, String[] arr1, String[] arr2) {
		
		/* if one is null, then both must be null */
		if (arr1 == null) {
			if (arr2 != null) {
				fail("One array is null, but the other isn't.");
			} else {
				/* everything is good */
				return;
			}
		}
		
		/* lengths must be the same */
		if (arr1.length != arr2.length) {
			fail("Arrays have different length: " + arr1.length + " versus " + arr2.length);
		}
		
		/* now sort the elements and compare them */
		Arrays.sort(arr1);
		Arrays.sort(arr2);
		
		for (int i = 0; i < arr1.length; i++) {
			String str1 = arr1[i];
			if (!str1.startsWith(arr1prefix)) {
				fail("Array element " + str1 + " doesn't start with " + arr1prefix);
			}
			str1 = str1.substring(arr1prefix.length());
			String str2 = arr2[i];
			//System.out.println("Comparing " + str1 + " and " + str2);
			if (!str1.equals(str2)) {
				fail("Mismatched array elements: " + str1 + " and " + str2);
			}
		}		
	}

	/*-------------------------------------------------------------------------------------*/

}
