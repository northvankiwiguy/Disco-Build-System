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

package com.arapiki.disco.model;

import static org.junit.Assert.assertEquals;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.arapiki.disco.model.BuildTasks.OperationType;
import com.arapiki.disco.model.FileNameSpaces.PathType;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class Reports {
	
	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/**
	 * This record is used when returning results from a report query. Each report
	 * fills out the relevant parts of this record (not necessarily all fields).
	 */
	public class FileRecord {
		public int pathId;	
		public int count;
		public int size;
	}
	
	/**
	 * Our database manager object, used to access the database content. This is provided 
	 * to us when the Reports object is first instantiated.
	 */
	private BuildStoreDB db = null;
	
	/**
	 * Various prepared statement for database access.
	 */
	private PreparedStatement 
		selectFileAccessCountPrepStmt = null,
		selectFileIncludesCountPrepStmt = null,
		selectFilesNotUsedPrepStmt = null;

	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * @param db
	 */
	public Reports(BuildStoreDB db) {
		this.db = db;
		
		selectFileAccessCountPrepStmt = db.prepareStatement(
				"select fileId, count(*) as usage from buildTaskFiles, files " +
					"where pathType=? and buildTaskFiles.fileId = files.id " +
					"group by fileId order by usage desc");
		
		selectFileIncludesCountPrepStmt = db.prepareStatement(
				"select fileId1, usage from fileIncludes where fileId2 = ? order by usage desc");
		
		selectFilesNotUsedPrepStmt = db.prepareStatement("" +
				"select files.id from files left join buildTaskFiles on files.id = buildTaskfiles.fileId" +
					" where files.pathType = " + PathType.TYPE_FILE.ordinal() + 
					" and buildTaskFiles.taskId is null");
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Provides a list of the most commonly accessed files across the whole build store.
	 * For each record, returns the number of unique tasks that accessed the file.
	 * @return A list of FileRecord, sorted by most commonly accessed file first.
	 */
	public FileRecord[] reportMostCommonlyAccessedFiles() {
		
		ArrayList<FileRecord> results = new ArrayList<FileRecord>();
		try {
			selectFileAccessCountPrepStmt.setInt(1, PathType.TYPE_FILE.ordinal());
			ResultSet rs = db.executePrepSelectResultSet(selectFileAccessCountPrepStmt);

			while (rs.next()) {
				FileRecord record = new FileRecord();
				record.pathId = rs.getInt(1);
				record.count = rs.getInt(2);
				results.add(record);
			}
			rs.close();
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results.toArray(new FileRecord[0]);
	}
	
	/*-------------------------------------------------------------------------------------*/

	public FileRecord[] reportMostCommonIncludersOfFile(int includedFile) {
		
		ArrayList<FileRecord> results = new ArrayList<FileRecord>();
		try {
			selectFileIncludesCountPrepStmt.setInt(1, includedFile);
			ResultSet rs = db.executePrepSelectResultSet(selectFileIncludesCountPrepStmt);

			while (rs.next()) {
				FileRecord record = new FileRecord();
				record.pathId = rs.getInt(1);
				record.count = rs.getInt(2);
				results.add(record);
			}
			rs.close();
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results.toArray(new FileRecord[0]);
	}
	
	/*-------------------------------------------------------------------------------------*/

	public FileRecord [] reportFilesNeverAccessed() {
		
		
		ArrayList<FileRecord> results = new ArrayList<FileRecord>();
		try {
			ResultSet rs = db.executePrepSelectResultSet(selectFilesNotUsedPrepStmt);

			while (rs.next()) {
				FileRecord record = new FileRecord();
				record.pathId = rs.getInt(1);
				results.add(record);
			}
			rs.close();
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results.toArray(new FileRecord[0]);
		
	}
	
	/*=====================================================================================*/
}
