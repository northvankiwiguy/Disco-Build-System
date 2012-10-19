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

package com.buildml.model.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IFileIncludeMgr;

/**
 * A manager class (that supports the BuildStore class) that manages all BuildStore
 * information on the relationship between paths. That is, if one file in the build
 * system "includes" another file, the relationship is recorded by this class.
 * <p>
 * There should be exactly one FileIncludes object per BuildStore object. Use the
 * BuildStore's getFileIncludes() method to obtain that one instance.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
/* package private */ class FileIncludeMgr implements IFileIncludeMgr {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/**
	 * Our database manager object, used to access the database content. This is provided 
	 * to us when the FileNameSpaces object is first instantiated.
	 */
	private BuildStoreDB db = null;
	
	/**
	 * Various prepared statements for database access.
	 */
	private PreparedStatement 
		insertFileIncludesPrepStmt = null,
		updateFileIncludesPrepStmt = null,
		deleteFilesIncludedByPrepStmt = null,
		selectUsageFromFileIncludesPrepStmt = null,
		selectTotalUsageFromFileIncludesPrepStmt = null,
		selectFile1FromFileIncludesPrepStmt = null,
		selectFile2FromFileIncludesPrepStmt = null;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new FileIncludes manager object.
	 * 
	 * @param buildStore The BuildStore object that owns this FileIncludes object.
	 */
	public FileIncludeMgr(BuildStore buildStore) {
		this.db = buildStore.getBuildStoreDB();
		
		/* create prepared database statements */
		insertFileIncludesPrepStmt = db.prepareStatement("insert into fileIncludes values (?, ?, 1)");
		updateFileIncludesPrepStmt = db.prepareStatement("update fileIncludes set usage = usage + 1 " +
				"where fileId1 = ? and fileId2 = ?");
		deleteFilesIncludedByPrepStmt = db.prepareStatement("delete from fileIncludes where fileId1 = ?");
		selectUsageFromFileIncludesPrepStmt = db.prepareStatement(
				"select usage from fileIncludes where fileId1 = ? and fileId2 = ?");
		selectTotalUsageFromFileIncludesPrepStmt = db.prepareStatement(
				"select sum(usage) from fileIncludes where fileId2 = ?");
		selectFile1FromFileIncludesPrepStmt = db.prepareStatement(
				"select fileId1 from fileIncludes where fileId2 = ?");
		selectFile2FromFileIncludesPrepStmt = db.prepareStatement(
				"select fileId2 from fileIncludes where fileId1 = ?");
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IFileIncludeMgr#addFileIncludes(int, int)
	 */
	@Override
	public void addFileIncludes(int file1, int file2) {
		
		try {
			updateFileIncludesPrepStmt.setInt(1, file1);
			updateFileIncludesPrepStmt.setInt(2, file2);
			int rowCount = db.executePrepUpdate(updateFileIncludesPrepStmt);
		
			if (rowCount == 0) {
				insertFileIncludesPrepStmt.setInt(1, file1);
				insertFileIncludesPrepStmt.setInt(2, file2);
				db.executePrepUpdate(insertFileIncludesPrepStmt);
			}
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IFileIncludeMgr#getFileIncludesCount(int, int)
	 */
	@Override
	public int getFileIncludesCount(int file1, int file2) {
	
		Integer results[];
		try {
			selectUsageFromFileIncludesPrepStmt.setInt(1, file1);
			selectUsageFromFileIncludesPrepStmt.setInt(2, file2);
			results = db.executePrepSelectIntegerColumn(selectUsageFromFileIncludesPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}

		/* if there's no entry at all, return a 0 usage count */
		if (results.length == 0) {
			return 0;
		}
		
		/* if there's one entry, that's good - just return that number. */
		if (results.length == 1) {
			return results[0];
		}
		
		/* else, problem - too many records */
		throw new FatalBuildStoreError("Too many records in includes table for " + file1 + "/" + file2);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IFileIncludeMgr#getTotalFileIncludedCount(int)
	 */
	@Override
	public int getTotalFileIncludedCount(int file) {
		ResultSet rs;
		int usageCount = 0;
		try {
			selectTotalUsageFromFileIncludesPrepStmt.setInt(1, file);
			rs = db.executePrepSelectResultSet(selectTotalUsageFromFileIncludesPrepStmt);
			
			/* if there's no entry at all, return a 0 usage count */			
			if (rs.next()) {
				usageCount = rs.getInt(1);				
			}	
			rs.close();
				
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		return usageCount;		
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IFileIncludeMgr#getFilesThatInclude(int)
	 */
	@Override
	public Integer[] getFilesThatInclude(int fileId) {
		
		Integer results[];
		try {
			selectFile1FromFileIncludesPrepStmt.setInt(1, fileId);
			results = db.executePrepSelectIntegerColumn(selectFile1FromFileIncludesPrepStmt);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}

		return results;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IFileIncludeMgr#getFilesIncludedBy(int)
	 */
	@Override
	public Integer[] getFilesIncludedBy(int fileId) {
		
		Integer results[];
		try {
			selectFile2FromFileIncludesPrepStmt.setInt(1, fileId);
			results = db.executePrepSelectIntegerColumn(selectFile2FromFileIncludesPrepStmt);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}

		return results;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IFileIncludeMgr#deleteFilesIncludedBy(int)
	 */
	@Override
	public void deleteFilesIncludedBy(int pathId) {
		
		try {
			deleteFilesIncludedByPrepStmt.setInt(1, pathId);
			db.executePrepUpdate(deleteFilesIncludedByPrepStmt);
		
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
	}
	
	/*=====================================================================================*/	
}
