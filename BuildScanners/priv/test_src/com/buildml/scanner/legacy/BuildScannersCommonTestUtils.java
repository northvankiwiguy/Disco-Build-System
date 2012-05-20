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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.buildml.model.BuildStore;
import com.buildml.model.CommonTestUtils;
import com.buildml.scanner.legacy.LegacyBuildScanner;
import com.buildml.utils.os.ShellResult;
import com.buildml.utils.os.SystemUtils;

/**
 * Reusable test method for validating code in com.buildml.scanner.legacy.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class BuildScannersCommonTestUtils {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Given the source code to a small C-language program, compile that program to produce
	 * an executable binary.
	 * @param tmpDir The directory into which the program will be stored.
	 * @param programName The file name of the program to be created.
	 * @param programSource The C-language source code for the program.
	 * @return The path name of the executable program.
	 */
	public static String compileProgram(File tmpDir, String programName, String programSource) {
		/*
		 * Write our program into a .c file (called "prog.c")
		 */
		PrintStream out = null;
		try {
			out = new PrintStream(new FileOutputStream(tmpDir + "/" + programName + ".c"));
		} catch (FileNotFoundException e1) {
			fail("Unable to write program content to a file");
		}
		out.println(programSource);
		out.close();

		/*
		 * Compile the program, using the default C compiler
		 */
		try {
			ShellResult sr = SystemUtils.executeShellCmd("cc -o " + tmpDir + "/" + 
					programName + " " + tmpDir + "/" + programName + ".c", "");
			if (sr.getReturnCode() != 0) {
				throw new Exception("Compile error: " + sr.getStderr());
			}
		} catch (Exception ex) {
			fail("Unable to compile program: " + ex.getMessage());
		}
		
		return tmpDir + "/" + programName;
	}
	
    /*-------------------------------------------------------------------------------------*/
	
	/**
	 * Given a program, contained entirely within a string, compile and execute
	 * the program, run it through CFS, and generate a BuildStore. All the temporary
	 * files and generated files will be stored in the directory specified by tmpDir.
	 * @param tmpDir The directory into which temporary files should be placed.
	 * @param program The entire body of the C program to be compiled/executed/scanned.
	 * @param args The command line arguments for the program
	 * @return The BuildStore created by scanning the program
	 * @throws Exception Something bad happened
	 */
	public static BuildStore parseLegacyProgram(File tmpDir, String program, String args[]) throws Exception {
		
		/* our return value */
		BuildStore bs = null;

		String exeProgram = compileProgram(tmpDir, "prog", program);

		/*
		 * Invoke the legacy build scanner to create a trace the file
		 */
		LegacyBuildScanner lbs = new LegacyBuildScanner();
		lbs.setTraceFile(tmpDir + "/cfs.trace");

		/* set this to 1 or 2 for more debug information */
		String debugLevelString = System.getenv("CFS_DEBUG");
		int debugLevel = 0;
		if (debugLevelString != null) {
			try {
				debugLevel = Integer.valueOf(debugLevelString);
			} catch (NumberFormatException ex) {
				fail("Invalid value for CFS_DEBUG environment variable.");
			}
		}
		lbs.setDebugLevel(debugLevel);
		
		/* invoke the newly compiled "prog" executable, and trace it with cfs */
		try {
			StringBuffer argString = new StringBuffer();
			if (args != null) {
				for (String arg : args) {
					argString.append(arg);
					argString.append(' ');
				}
			}
			lbs.traceShellCommand(new String[] { exeProgram + " " + argString.toString()});
		} catch (Exception ex) {
			fail("Unable to trace shell command: " + ex.getMessage());
		}

		/* create an empty BuildStore for the tracer to populate */
		bs = CommonTestUtils.getEmptyBuildStore(tmpDir);
		lbs.setBuildStore(bs);

		/* trace the file, while displaying debug output */
		lbs.parseTraceFile();	

		return bs;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
