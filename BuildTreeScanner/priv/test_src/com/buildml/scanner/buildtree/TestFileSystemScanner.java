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

package com.buildml.scanner.buildtree;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.buildml.model.impl.BuildStore;
import com.buildml.model.CommonTestUtils;
import com.buildml.model.impl.FileNameSpaces;
import com.buildml.model.impl.Reports;
import com.buildml.model.types.FileSet;
import com.buildml.scanner.buildtree.FileSystemScanner;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class TestFileSystemScanner {

	/** Our test BuildStore object */
	private BuildStore bs;

	/** Our test FileNameSpaces object */
	private FileNameSpaces fns;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		bs = CommonTestUtils.getEmptyBuildStore();
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
	
	/**
	 * TODO: This isn't a unit test. Needs to be fixed.
	 */
	@Test
	public void testScanForFiles() {
		FileSystemScanner fss = new FileSystemScanner(bs);
		
		fss.scanForFiles("root", "/home/psmith/work");
		
		/* display the list of files that were never accessed */
		Reports reports = bs.getReports();
		FileSet results = reports.reportFilesNeverAccessed();
		for (Integer pathId : results) {			
			/*String pathName = */fns.getPathName(pathId);
			//System.out.println(pathName);
		}
		//System.out.println("Found " + results.size() + " files");
		
	}

	/*-------------------------------------------------------------------------------------*/
}
