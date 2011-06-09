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
import java.util.List;

import com.arapiki.utils.errors.ErrorCode;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class BuildTasks {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	public enum OperationType {
		OP_UNSPECIFIED,		/* an unspecified operation - used for searches */
							/*     when we don't care which operation */
		OP_READ,			/* a file was read */
		OP_WRITE,			/* a file was written */
		OP_MODIFIED,		/* a file was read and written by the same task */
		OP_DELETE			/* a file was deleted */
	}
	
	/**
	 * Our database manager object, used to access the database content. This is provided 
	 * to us when the FileNameSpaces object is first instantiated.
	 */
	private BuildStoreDB db = null;
	
	
	/**
	 * Various prepared statement for database access.
	 */
	private PreparedStatement 
		insertBuildTaskPrepStmt = null,
		findCommandPrepStmt = null,
		findParentPrepStmt = null,
		findChildrenPrepStmt = null,
		insertBuildTaskFilesPrepStmt = null,
		findOperationInBuildTaskFilesPrepStmt = null,
		findFilesInBuildTaskFilesPrepStmt = null,
		findFilesByOperationInBuildTaskFilesPrepStmt = null,
		findTasksByFileInBuildTaskFilesPrepStmt = null,
		findTasksByFileAndOperationInBuildTaskFilesPrepStmt = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new BuildTasks object. This object encapsulates information for all the build
	 * tasks in the system.
	 * @param db
	 */
	public BuildTasks(BuildStore bs) {
		this.db = bs.getBuildStoreDB();

		/* create prepared database statements */
		insertBuildTaskPrepStmt = db.prepareStatement("insert into buildTasks values (null, ?, ?)");
		findCommandPrepStmt = db.prepareStatement("select command from buildTasks where taskId = ?");
		findParentPrepStmt = db.prepareStatement("select parentTaskId from buildTasks where taskId = ?");
		findChildrenPrepStmt = db.prepareStatement("select taskId from buildTasks where parentTaskId = ?" +
				" and parentTaskId != taskId");
		insertBuildTaskFilesPrepStmt = db.prepareStatement("insert into buildTaskFiles values (?, ?, ?)");
		findOperationInBuildTaskFilesPrepStmt = 
			db.prepareStatement("select operation from buildTaskFiles where taskId = ? and fileId = ?");
		findFilesInBuildTaskFilesPrepStmt =
			db.prepareStatement("select fileId from buildTaskFiles where taskId = ?");
		findFilesByOperationInBuildTaskFilesPrepStmt =
			db.prepareStatement("select fileId from buildTaskFiles where taskId = ? and operation = ?");
		findTasksByFileInBuildTaskFilesPrepStmt =
			db.prepareStatement("select taskId from buildTaskFiles where fileId = ?");		
		findTasksByFileAndOperationInBuildTaskFilesPrepStmt =
			db.prepareStatement("select taskId from buildTaskFiles where fileId = ? and operation = ?");
		
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Add a new build task, and return the task ID number.
	 * @param command The shell command to be executed.
	 * @return The new task's ID.
	 */
	public int addBuildTask(int parentTaskId, String command) {
		
		try {
			insertBuildTaskPrepStmt.setInt(1, parentTaskId);
			insertBuildTaskPrepStmt.setString(2, command);
			db.executePrepUpdate(insertBuildTaskPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}

		return db.getLastRowID();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Record the fact that the specific build task access the specified file. Adding a relationship
	 * the second or successive time has no affect.
	 * @param buildTaskID The build task to add the file access to.
	 * @param fileNumber The file's ID number.
	 * @param operation
	 */
	public void addFileAccess(int buildTaskId, int fileNumber, OperationType operation) {
		
		/* 
		 * We don't want to add the same record twice, but we might want to merge the two
		 * operations together. That is, if a task reads a file, then writes a file, we want
		 * to mark it as OP_READWRITE.
		 */
		String stringResults[] = null;
		try {
			findOperationInBuildTaskFilesPrepStmt.setInt(1, buildTaskId);
			findOperationInBuildTaskFilesPrepStmt.setInt(2, fileNumber);
			stringResults = db.executePrepSelectStringColumn(findOperationInBuildTaskFilesPrepStmt);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/*
		 * If there was no existing record, we'll insert a fresh record.
		 */
		if (stringResults.length == 0) {
			try {
				insertBuildTaskFilesPrepStmt.setInt(1, buildTaskId);
				insertBuildTaskFilesPrepStmt.setInt(2, fileNumber);
				insertBuildTaskFilesPrepStmt.setInt(3, operation.ordinal());
				db.executePrepUpdate(insertBuildTaskFilesPrepStmt);
			} catch (SQLException e) {
				throw new FatalBuildStoreError("Unable to execute SQL statement", e);
			}
		}
		
		/*
		 * Else, if there's one record, see if the operation needs to be merged. The DFA
		 * for transitioning to a new state is as follows:
		 * 
		 *             New:    |  Read    Write   Modify  Delete
		 *             -----------------------------------------
		 *             Read    |  Read    Modify  Modify  Delete
		 *  Existing:  Write   |  Write   Write   Modify  Delete
		 *             Modify  |  Modify  Modify  Modify  Delete
		 *             Delete  |  Read    Write   Modify  Delete
		 */
		else if (stringResults.length == 1) {
			// TODO: make this work.
			//OperationType existingOp = intToOperationType(opTypeNum)
			
		}
		
		/* else, there's an error - can't have multiple entries */
		else {
			throw new FatalBuildStoreError("Multiple results find in buildTaskFiles table for taskId = " 
					+ buildTaskId + " and fileId = " + fileNumber);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Get the list of files that are accessed by this build task.
	 * @param taskID The build task to query.
	 * @param operation The type of operation we're interested in. 'r' reads only, 
	 * 			'w' writes only, 'e" either reads or writes.
	 * @return An array of file IDs.
	 */
	public Integer [] getFilesAccessed(int taskId, OperationType operation) {
		
		// TODO: think about what operation = OP_MODIFIED should search for.
		
		List<Integer> results;
		
		try {
			ResultSet rs;
			
			/* if we want all operation (OP_UNSPECIFIED), don't query the operation field */
			if (operation == OperationType.OP_UNSPECIFIED) {
				findFilesInBuildTaskFilesPrepStmt.setInt(1, taskId);
				rs = db.executePrepSelectResultSet(findFilesInBuildTaskFilesPrepStmt);
			} 
			
			/* else, we need to limit the results, based on the operation */
			else {
				findFilesByOperationInBuildTaskFilesPrepStmt.setInt(1, taskId);
				findFilesByOperationInBuildTaskFilesPrepStmt.setInt(2, operation.ordinal());
				rs = db.executePrepSelectResultSet(findFilesByOperationInBuildTaskFilesPrepStmt);				
			}
			
			/* read the results into an array */
			results = new ArrayList<Integer>();
			while (rs.next()) {
				results.add(rs.getInt(1));
			}
			rs.close();
		
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		return results.toArray(new Integer[0]);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Get the list of tasks that access a specific file.
	 * @param fileId The file we're interested in querying for.
	 * @param operation The operation that the tasks perform on this file. 'r' reads only, 'w' write only,
	 * 		'e' either reads or writes.
	 * @return An array of task IDs that access this file.
	 */
	public Integer [] getTasksThatAccess(int fileId, OperationType operation) {
		
		List<Integer> results;

		try {
			ResultSet rs;
			
			/* if we want all operation (OP_UNSPECIFIED), don't query the operation field */
			if (operation == OperationType.OP_UNSPECIFIED) {
				findTasksByFileInBuildTaskFilesPrepStmt.setInt(1, fileId);
				rs = db.executePrepSelectResultSet(findTasksByFileInBuildTaskFilesPrepStmt);
			} 
			
			/* else, we need to limit the results, based on the operation */
			else {
				findTasksByFileAndOperationInBuildTaskFilesPrepStmt.setInt(1, fileId);
				findTasksByFileAndOperationInBuildTaskFilesPrepStmt.setInt(2, operation.ordinal());
				rs = db.executePrepSelectResultSet(findTasksByFileAndOperationInBuildTaskFilesPrepStmt);				
			}
			
			/* read the results into an array */
			results = new ArrayList<Integer>();
			while (rs.next()) {
				results.add(rs.getInt(1));
			}
			rs.close();
		
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		return results.toArray(new Integer[0]);
		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Fetch the task's command line string.
	 * @param taskID The build task we're interested in.
	 * @return The build tasks command line string.
	 */
	public String getCommand(int taskId) {
		String [] stringResults = null;
		try {
			findCommandPrepStmt.setInt(1, taskId);
			stringResults = db.executePrepSelectStringColumn(findCommandPrepStmt);
			
		} catch (SQLException e) {
			new FatalBuildStoreError("Error in SQL: " + e);
		}
		
		/* if there were no results, return null */
		if (stringResults.length == 0) {
			return null;
		}
		
		/* if there was one result, return it */
		else if (stringResults.length == 1) {
			return stringResults[0];
		}
		
		/* else, multiple results is a bad thing */
		else {
			throw new FatalBuildStoreError("Multiple results find in buildTasks table for taskId = " + taskId);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given the ID of a task, return the task's parent task.
	 * @param taskId The task to return the parent of
	 * @return The ID of the task's parent, or NOT_FOUND if the task is at the root, or
	 * BAD_VALUE if the task ID is invalid.
	 */
	public int getParent(int taskId) {
		
		/* query the database, based on the task Id */
		Integer [] intResults = null;
		try {
			findParentPrepStmt.setInt(1, taskId);
			intResults = db.executePrepSelectIntegerColumn(findParentPrepStmt);
		} catch (SQLException e) {
			new FatalBuildStoreError("Error in SQL: " + e);
		}
		
		/* if there were no results, it's because taskId is invalid. Return an error */
		if (intResults.length == 0) {
			return ErrorCode.BAD_VALUE;
		}
		
		/* if there was one result, return it */
		else if (intResults.length == 1) {
			
			/* the single result is the parent, unless this task's parent is itself! */
			int parentTaskId = intResults[0];
			if (parentTaskId == taskId) {
				/* the current task is at the root */
				return ErrorCode.NOT_FOUND;
			}
			return parentTaskId;
			
		}
		
		/* else, multiple results is a bad thing */
		else {
			throw new FatalBuildStoreError("Multiple results find in buildTasks table for taskId = " + taskId);
		}
		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given the ID of task, return an array of the task's children (possibly empty).
	 * @param taskId The task that is the parent of the children to be returned.
	 * @return An array of child task Id (in no particular order). Or the empty array if there
	 * are no children.
	 */
	public Integer [] getChildren(int taskId) {
		
		Integer [] intResults = null;
		try {
			findChildrenPrepStmt.setInt(1, taskId);
			intResults = db.executePrepSelectIntegerColumn(findChildrenPrepStmt);
		} catch (SQLException e) {
			new FatalBuildStoreError("Error in SQL: " + e);
		}

		return intResults;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the build task Id of the task with the associated root name
	 * @param rootName The name of the root, which is attached to a task.
	 * @return The root task's ID.
	 */
	public int getRootTask(String rootName) {
		
		// TODO: return something other than 0. Currently the default task is created
		// implicitly, rather than explicitly
		return 0;
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * Helper function for translating from an ordinal integer to an OperationType. This is the
	 * opposite of OperationType.ordinal.
	 * @param the ordinal value of a OperationType value.
	 * @return the corresponding OperationType value.
	 * @throws FatalBuildStoreError if the ordinal value is out of range.
	 */
	private OperationType intToOperationType(int opTypeNum)
			throws FatalBuildStoreError {
		switch (opTypeNum) {
			case 0: return OperationType.OP_UNSPECIFIED;
			case 1: return OperationType.OP_READ;
			case 2: return OperationType.OP_WRITE;
			case 3: return OperationType.OP_MODIFIED;
			case 4: return OperationType.OP_DELETE;
			default:
				throw new FatalBuildStoreError("Invalid value found in operation field: " + opTypeNum);
		}
	}
	/*=====================================================================================*/

}
