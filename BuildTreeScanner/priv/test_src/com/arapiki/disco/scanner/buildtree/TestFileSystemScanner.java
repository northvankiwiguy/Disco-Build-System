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

package com.arapiki.disco.scanner.buildtree;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.Reports;
import com.arapiki.disco.model.TestCommon;
import com.arapiki.disco.model.Reports.FileRecord;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestFileSystemScanner {

	private BuildStore bs;
	private FileNameSpaces fns;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		bs = TestCommon.getEmptyBuildStore();
		fns = bs.getFileNameSpaces();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	@Test
	public void testScanForFiles() throws Exception {
		FileSystemScanner fss = new FileSystemScanner(bs);
		
		fss.scanForFiles("root", "/home/psmith/work");
		
		/* display the list of files that were never accessed */
		Reports reports = bs.getReports();
		FileRecord results[] = reports.reportFilesNeverAccessed();
		for (int i = 0; i < results.length; i++) {
			String pathName = fns.getPathName(results[i].pathId);
			System.out.println(pathName);
		}
		System.out.println("Found " + results.length + " files");
		
	}

	/*-------------------------------------------------------------------------------------*/
}
