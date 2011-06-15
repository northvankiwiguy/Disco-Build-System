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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.arapiki.disco.model.BuildTasks.OperationType;
import com.arapiki.disco.model.FileNameSpaces.PathType;
import com.arapiki.utils.types.IntegerTreeSet;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class Reports {
	
	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/
	
	/**
	 * Our database manager object, used to access the database content. This is provided 
	 * to us when the Reports object is first instantiated.
	 */
	private BuildStoreDB db = null;
	
	/**
	 * The FileNameSpaces object we're reporting on. This is provided to use when the
	 * Reports object is first instantiated.
	 */
	private FileNameSpaces fns = null;
	
	/**
	 * Various prepared statement for database access.
	 */
	private PreparedStatement 
		selectFileAccessCountPrepStmt = null,
		selectFileIncludesCountPrepStmt = null,
		selectFilesNotUsedPrepStmt = null,
		selectFilesWithMatchingNamePrepStmt = null,
		selectDerivedFilesPrepStmt = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * @param db
	 */
	public Reports(BuildStore bs) {
		this.db = bs.getBuildStoreDB();
		this.fns = bs.getFileNameSpaces();
		
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
		
		selectFilesWithMatchingNamePrepStmt = db.prepareStatement(
				"select files.id from files where name like ? and pathType = " + PathType.TYPE_FILE.ordinal());

		selectDerivedFilesPrepStmt = db.prepareStatement(
				"select distinct fileId from buildTaskFiles where taskId in " +
				"(select taskId from buildTaskFiles where fileId = ? and " +
				"operation = " + OperationType.OP_READ.ordinal() + ") " + 
				"and operation = " + OperationType.OP_WRITE.ordinal());

		//
		// This is what I've used - it seems to work at scale.
		// selectFilesWrittenButNotUsedPrepStmt = db.prepareStatement(
		//		    "select writeFileId from (select distinct fileId as writeFileId from " +
		//		    "buildTaskFiles where operation = " + OperationType.OP_WRITE.ordinal() + ") " +
		//		    "left join (select distinct fileId as readFileId from buildTaskFiles " +
		//		    "where operation = " + OperationType.OP_READ.ordinal() + ") on writeFileId = readFileId " +
		//		      "where readFileId is null");
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Provides an ordered list of the most commonly accessed files across the whole build store.
	 * For each record, returns the number of unique tasks that accessed the file.
	 * @return A list of FileRecord, sorted by most commonly accessed file first.
	 */
	public FileRecord[] reportMostCommonlyAccessedFiles() {
		
		ArrayList<FileRecord> results = new ArrayList<FileRecord>();
		try {
			selectFileAccessCountPrepStmt.setInt(1, PathType.TYPE_FILE.ordinal());
			ResultSet rs = db.executePrepSelectResultSet(selectFileAccessCountPrepStmt);

			while (rs.next()) {
				FileRecord record = new FileRecord(rs.getInt(1));
				record.setCount(rs.getInt(2));
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
				FileRecord record = new FileRecord(rs.getInt(1));
				record.setCount(rs.getInt(2));
				results.add(record);
			}
			rs.close();
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results.toArray(new FileRecord[0]);
	}
	
	/*-------------------------------------------------------------------------------------*/

	public FileSet reportFilesNeverAccessed() {
		
		FileSet results = new FileSet(fns);
		try {
			ResultSet rs = db.executePrepSelectResultSet(selectFilesNotUsedPrepStmt);

			while (rs.next()) {
				FileRecord record = new FileRecord(rs.getInt(1));
				results.add(record);
			}
			rs.close();
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the set of files (not directories) that match the user-specified file name
	 * @param fileArg The name of the file(s) to match.
	 * @return The FileSet of matching file names.
	 */
	public FileSet reportFilesThatMatchName(String fileArg) {
		
		FileSet results = new FileSet(fns);
		try {
			selectFilesWithMatchingNamePrepStmt.setString(1, fileArg);
			ResultSet rs = db.executePrepSelectResultSet(selectFilesWithMatchingNamePrepStmt);

			while (rs.next()) {
				FileRecord record = new FileRecord(rs.getInt(1));
				results.add(record);
			}
			rs.close();
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given the input FileSet (sourceFileSet), return a new FileSet containing all the 
	 * paths that are derived from the paths listed in sourceFileSet. A file (A) is derived 
	 * from file (B) if there's a task (or sequence of paths) that reads B and writes to A.
	 * @param sourceFileSet The FileSet of all files we should consider as input to the tasks.
	 * @param reportIndirect Should we also report on indirectly derived files (multiple tasks
	 * between file A and file B?
	 * @return The FileSet of derived paths.
	 */
	public FileSet reportDerivedFiles(FileSet sourceFileSet, boolean reportIndirect) {
		
		/* 
		 * Create a new empty FileSet for tracking all the results. Each time we
		 * iterate through the loop, we merge the results into this FileSet
		 */
		FileSet results = new FileSet(fns);
		
		/* the first set of files to analyze is the original source FileSet */
		FileSet nextFileSet = sourceFileSet;
		
		/* 
		 * This variable is used to track the progress of finding new results.
		 * If it doesn't change from one iteration to the next, we stop the iterations.
		 */
		int lastNumberOfResults = 0;

		/* 
		 * Start the iterations. If "reportIndirect" is false, we'll only execute
		 * the loop once. Each time we go around the loop, we take the derived files
		 * from the previous round and treat them as the new set of input files.
		 */
		boolean done = false;
		do {
			
			/* empty FileSet to collect this round's set of results */
			FileSet thisRoundOfResults = new FileSet(fns);

			/* iterate through each of the files in this round's FileSet */
			for (int fileId : nextFileSet) {
				
				/* determine the directly derived files for this file, and add them to the result set */
				try {
					selectDerivedFilesPrepStmt.setInt(1, fileId);
					ResultSet rs = db.executePrepSelectResultSet(selectDerivedFilesPrepStmt);

					while (rs.next()) {
						FileRecord record = new FileRecord(rs.getInt(1));
						thisRoundOfResults.add(record);
					}
					rs.close();
			
				} catch (SQLException e) {
					throw new FatalBuildStoreError("Unable to execute SQL statement", e);
				}
			}
			
			/* 
			 * Prepare to repeat the process by using the results from this iteration as
			 * the input to the next iteration.
			 */
			nextFileSet = thisRoundOfResults;
		
			/*
			 * Merge this cycle's results into our final result set
			 */
			results.mergeFileSet(thisRoundOfResults);
			
			/* are we done? Did we find any new results in this iteration? */
			int thisNumberOfResults = results.size();
			done = (thisNumberOfResults == lastNumberOfResults);
			lastNumberOfResults = thisNumberOfResults;
			
		} while (reportIndirect && !done);
		
		/* return the combined set of results */
		return results;
	}
	
	/*=====================================================================================*/
}
