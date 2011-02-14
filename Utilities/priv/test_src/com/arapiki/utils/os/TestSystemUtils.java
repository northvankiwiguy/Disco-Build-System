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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestSystemUtils {

	/*
	 * A temporary file that contains a simple executable program
	 */
	private static File ourTempExe;
	
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
		ourTempExe = File.createTempFile("tempExecutable", null, null);
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
		Runtime.getRuntime().exec("chmod 755 " + ourTempExe);
	}

	/**
	 * This method is called once (and only once) when all test cases have completed.
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		/* remove our short perl script */
		ourTempExe.delete();
	}

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
		
	}

}
