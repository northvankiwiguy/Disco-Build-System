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
import com.arapiki.utils.string.ShellCommandUtils;

/**
 * A manager class (that supports the BuildStore class) that manages all BuildStore
 * information pertaining to build tasks.
 * <p>
 * There should be exactly one BuildTasks object per BuildStore object. Use the
 * BuildStore's getBuildTasks() method to obtain that one instance.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class BuildTasks {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/** Data type for specifying the type of a file access that a task performs. */
	public enum OperationType {
		/** An unspecified operation for when we don't care which operation is performed. */
		OP_UNSPECIFIED,		
		
		/** The file was read by the task. */
		OP_READ,
		
		/** The file was written by the task. */
		OP_WRITE,
		
		/** The file was read and written by the same task. */
		OP_MODIFIED,
		
		/** The file was deleted by the task. */
		OP_DELETE
	}
	
	/**
	 * Our database manager object, used to access the database content. This is provided 
	 * to us when the BuildTasks object is first instantiated.
	 */
	private BuildStoreDB db = null;
	
	/** The BuildStore object that owns this BuildTasks object. */
	private BuildStore buildStore = null;
	
	/** The FileNameSpaces object associated with these BuildTasks */
	private FileNameSpaces fns = null;
	
	/** Various prepared statement for database access. */
	private PreparedStatement 
		insertBuildTaskPrepStmt = null,
		findCommandPrepStmt = null,
		findParentPrepStmt = null,
		findDirectoryPrepStmt = null,
		findChildrenPrepStmt = null,
		insertBuildTaskFilesPrepStmt = null,
		removeBuildTaskFilesPrepStmt = null,
		updateBuildTaskFilesPrepStmt = null,
		findOperationInBuildTaskFilesPrepStmt = null,
		findFilesInBuildTaskFilesPrepStmt = null,
		findTasksInDirectoryPrepStmt = null,
		findFilesByOperationInBuildTaskFilesPrepStmt = null,
		findTasksByFileInBuildTaskFilesPrepStmt = null,
		findTasksByFileAndOperationInBuildTaskFilesPrepStmt = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new BuildTasks object. This object encapsulates information for all the build
	 * tasks in the system.
	 * 
	 * @param buildStore The BuildStore object that "owns" this BuildTasks manager
	 */
	public BuildTasks(BuildStore buildStore) {
		this.buildStore = buildStore;
		this.db = buildStore.getBuildStoreDB();
		this.fns = buildStore.getFileNameSpaces();

		/* create prepared database statements */
		insertBuildTaskPrepStmt = db.prepareStatement("insert into buildTasks values (null, ?, ?, 0, ?)");
		findCommandPrepStmt = db.prepareStatement("select command from buildTasks where taskId = ?");
		findParentPrepStmt = db.prepareStatement("select parentTaskId from buildTasks where taskId = ?");
		findDirectoryPrepStmt = db.prepareStatement("select taskDirId from buildTasks where taskId = ?");
		findTasksInDirectoryPrepStmt =
			db.prepareStatement("select taskId from buildTasks where taskDirId = ?");
		findChildrenPrepStmt = db.prepareStatement("select taskId from buildTasks where parentTaskId = ?" +
				" and parentTaskId != taskId");
		insertBuildTaskFilesPrepStmt = db.prepareStatement("insert into buildTaskFiles values (?, ?, ?)");
		removeBuildTaskFilesPrepStmt = 
			db.prepareStatement("delete from buildTaskFiles where taskId = ? and fileId = ?");
		updateBuildTaskFilesPrepStmt = 
			db.prepareStatement("update buildTaskFiles set operation = ? where taskId = ? and fileId = ?");
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
	 * 
	 * @param parentTaskId The task ID of the new task's parent.
	 * @param taskDirId The ID of the path (a directory) in which this task was executed.
	 * @param command The shell command associated with this task.
	 * @return The new task's ID.
	 */
	public int addBuildTask(int parentTaskId, int taskDirId, String command) {
		
		try {
			insertBuildTaskPrepStmt.setInt(1, parentTaskId);
			insertBuildTaskPrepStmt.setInt(2, taskDirId);
			insertBuildTaskPrepStmt.setString(3, command);
			db.executePrepUpdate(insertBuildTaskPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}

		return db.getLastRowID();
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * A two-dimensional mapping table for tracking the state of each file access. If a file
	 * is accessed multiple times by a single task, the state of that access can also change. 
	 * Given an "existing" file-access state, and a "new" file-access state, this matrix tells
	 * us the state to transition to. For example, if "existing" is OP_READ and "new" is 
	 * OP_WRITE, then the combined state is OP_MODIFIED.
	 */
	private OperationType operationTypeMapping[][] = {
			{ /* For Existing == OP_UNSPECIFIED */
				OperationType.OP_UNSPECIFIED, 	/* New == OP_UNSPECIFIED */
				OperationType.OP_UNSPECIFIED, 	/* New == OP_READ */
				OperationType.OP_UNSPECIFIED, 	/* New == OP_WRITE */
				OperationType.OP_UNSPECIFIED, 	/* New == OP_MODIFIED */
				OperationType.OP_UNSPECIFIED  	/* New == OP_DELETE */
			}, 
			{ /* For Existing == OP_READ */
				OperationType.OP_UNSPECIFIED, 	/* New == OP_UNSPECIFIED */
				OperationType.OP_READ, 			/* New == OP_READ */
				OperationType.OP_MODIFIED, 		/* New == OP_WRITE */
				OperationType.OP_MODIFIED, 		/* New == OP_MODIFIED */
				OperationType.OP_DELETE	  		/* New == OP_DELETE */
			}, 
			{ /* For Existing == OP_WRITE */
				OperationType.OP_UNSPECIFIED, 	/* New == OP_UNSPECIFIED */
				OperationType.OP_WRITE, 		/* New == OP_READ */
				OperationType.OP_WRITE, 		/* New == OP_WRITE */
				OperationType.OP_WRITE, 		/* New == OP_MODIFIED */
				OperationType.OP_DELETE  		/* New == OP_DELETE */
			}, 
			{ /* For Existing == OP_MODIFIED */
				OperationType.OP_UNSPECIFIED, 	/* New == OP_UNSPECIFIED */
				OperationType.OP_MODIFIED, 		/* New == OP_READ */
				OperationType.OP_MODIFIED, 		/* New == OP_WRITE */
				OperationType.OP_MODIFIED, 		/* New == OP_MODIFIED */
				OperationType.OP_DELETE  		/* New == OP_DELETE */
			},
			{ /* For Existing == OP_DELETED */
				OperationType.OP_UNSPECIFIED, 	/* New == OP_UNSPECIFIED */
				OperationType.OP_READ, 			/* New == OP_READ */
				OperationType.OP_WRITE, 		/* New == OP_WRITE */
				OperationType.OP_MODIFIED, 		/* New == OP_MODIFIED */
				OperationType.OP_DELETE  		/* New == OP_DELETE */
			}
	};			

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Record the fact that the specific build task accessed the specified file. Adding
	 * the same relationship a second or successive time has no effect.
	 * 
	 * @param buildTaskId The ID of the build task that accessed the file.
	 * @param fileNumber The file's ID number.
	 * @param newOperation How the task accessed the file (read, write, delete, etc).
	 */
	public void addFileAccess(int buildTaskId, int fileNumber, OperationType newOperation) {
		
		/* 
		 * We don't want to add the same record twice, but we might want to merge the two
		 * operations together. That is, if a task reads a file, then writes a file, we want
		 * to mark it as OP_MODIFIED.
		 */
		Integer intResults[] = null;
		try {
			findOperationInBuildTaskFilesPrepStmt.setInt(1, buildTaskId);
			findOperationInBuildTaskFilesPrepStmt.setInt(2, fileNumber);
			intResults = db.executePrepSelectIntegerColumn(findOperationInBuildTaskFilesPrepStmt);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/*
		 * If there was no existing record, we'll insert a fresh record.
		 */
		if (intResults.length == 0) {
			try {
				insertBuildTaskFilesPrepStmt.setInt(1, buildTaskId);
				insertBuildTaskFilesPrepStmt.setInt(2, fileNumber);
				insertBuildTaskFilesPrepStmt.setInt(3, newOperation.ordinal());
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
		 *  Existing:  Write   |  Write   Write   Write   Temporary
		 *             Modify  |  Modify  Modify  Modify  Delete
		 *             Delete  |  Read    Write   Modify  Delete
		 *             
		 *  Remember:
		 *    - Read = the process has *only* ever read this file.
		 *    - Write = the process created this file (it didn't exist before).
		 *    - Modify = the process read and then wrote to this file.
		 *    - Delete = the process ended up by deleting this file.
		 */
		else if (intResults.length == 1) {
			OperationType existingOp = intToOperationType(intResults[0]);
			OperationType combinedOp = operationTypeMapping[existingOp.ordinal()][newOperation.ordinal()];
			
			/*
			 * Handle a special case of temporary files. That is, if the existingOp is WRITE,
			 * and the combinedOp is DELETE, then this file was both created and deleted
			 * by this task.
			 */
			if ((existingOp == OperationType.OP_WRITE) && (combinedOp == OperationType.OP_DELETE)) {
				try {
					removeBuildTaskFilesPrepStmt.setInt(1, buildTaskId);
					removeBuildTaskFilesPrepStmt.setInt(2, fileNumber);
					db.executePrepUpdate(removeBuildTaskFilesPrepStmt);
				} catch (SQLException e) {
					throw new FatalBuildStoreError("Unable to execute SQL statement", e);
				}

				/*
				 * Attempt to remove the file from the FileNameSpaces. This will fail if the
				 * same path is already used by some other task, but that's acceptable. We
				 * only want to remove paths that were used exclusively by this task.
				 */
				fns.removePath(fileNumber);
			}
			
			/*
			 * else, the normal case is to replace the old state with the new state.
			 */
			else {
				try {
					updateBuildTaskFilesPrepStmt.setInt(1, combinedOp.ordinal());
					updateBuildTaskFilesPrepStmt.setInt(2, buildTaskId);
					updateBuildTaskFilesPrepStmt.setInt(3, fileNumber);
					db.executePrepUpdate(updateBuildTaskFilesPrepStmt);
				} catch (SQLException e) {
					throw new FatalBuildStoreError("Unable to execute SQL statement", e);
				}
			}
		}
		
		/* else, there's an error - can't have multiple entries */
		else {
			throw new FatalBuildStoreError("Multiple results find in buildTaskFiles table for taskId = " 
					+ buildTaskId + " and fileId = " + fileNumber);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return an array of files that were accessed by this build task.
	 * 
	 * @param taskId The build task that accessed the files.
	 * @param operation The type of operation we're interested in (such as OP_READ,
	 *    OP_WRITE, or OP_UNSPECIFIED if you don't care).
	 * @return An array of file IDs.
	 */
	public Integer [] getFilesAccessed(int taskId, OperationType operation) {
				
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
	 * Return an array of tasks that accessed a specific file.
	 * 
	 * @param fileId The file we're interested in querying for.
	 * @param operation The operation that the tasks perform on this file (such as OP_READ,
	 *    OP_WRITE, or OP_UNSPECIFIED if you don't care).
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
	 * Return the list of all tasks that execute within the directory specified by pathId.
	 * @param pathId The directory in which the tasks must be executed.
	 * @return The list of tasks.
	 */
	public Integer[] getTasksInDirectory(int pathId) {
		
		Integer intResults[] = null;
		try {
			findTasksInDirectoryPrepStmt.setInt(1, pathId);
			intResults = db.executePrepSelectIntegerColumn(findTasksInDirectoryPrepStmt);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		return intResults;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Fetch the build task's command line string.
	 * 
	 * @param taskId The build task we're querying.
	 * @return The build task's command line string.
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
	 * Fetch a summary of this task's command. The summary string is designed to give a
	 * high-level overview of what the command does. The summary string for certain commands
	 * may contain the command name and most important parameters, whereas for other commands
	 * it may just be the first 'width' characters of the shell command.
	 * 
	 * @param taskId The ID of the task.
	 * @param width The maximum number of characters in the summary string.
	 * @return The summary string for this task's command.
	 */
	public String getCommandSummary(int taskId, int width) {
		
		/* 
		 * For now, we treat all commands as being the same, and simply return the
		 * first 'width' characters from the task's command string.
		 */
		String command = ShellCommandUtils.joinCommandLine(getCommand(taskId));
		
		/* for strings that are longer than 'width', truncate them and suffix them with "..." */
		boolean dotsNeeded = false;
		int stringLen = command.length();
		if (stringLen > width - 3) {
			stringLen = width - 3;
			dotsNeeded = true;
		}
		
		/* form the summary string, possibly with ... */
		StringBuffer sb = new StringBuffer(command.substring(0, stringLen));
		if (dotsNeeded){
			sb.append("...");
		}
		
		/* return the summary string */
		return sb.toString();
	}
	
	/*-------------------------------------------------------------------------------------*/


	/**
	 * Given the ID of a task, return the task's parent task.
	 * 
	 * @param taskId The task to return the parent of.
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
	 * Return the path ID of the directory in which this task was executed.
	 * 
	 * @param taskId The ID of the task.
	 * @return The path ID of the directory in which this task was executed.
	 */
	public int getDirectory(int taskId) {
		Integer [] intResults = null;
		try {
			findDirectoryPrepStmt.setInt(1, taskId);
			intResults = db.executePrepSelectIntegerColumn(findDirectoryPrepStmt);
			
		} catch (SQLException e) {
			new FatalBuildStoreError("Error in SQL: " + e);
		}
		
		/* if there were no results, return a not found error */
		if (intResults.length == 0) {
			return ErrorCode.NOT_FOUND;
		}
		
		/* if there was one result, return it */
		else if (intResults.length == 1) {
			return intResults[0];
		}
		
		/* else, multiple results is a bad thing */
		else {
			throw new FatalBuildStoreError("Multiple results find in buildTasks table for taskId = " + taskId);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given the ID of task, return an array of the task's children (possibly empty).
	 * 
	 * @param taskId The task that is the parent of the children to be returned.
	 * @return An array of child task IDs (in no particular order). Or the empty array if there
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
	 * Return the build task ID of the task with the associated root name.
	 * 
	 * @param rootName The name of the root, which is attached to a task.
	 * @return The root task's ID.
	 */
	public int getRootTask(String rootName) {
		
		// TODO: return something other than 0. Currently the default task is created
		// implicitly, rather than explicitly
		return 0;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the BuildStore object that owns this BuildTasks object.
	 *
	 * @return The BuildStore object that owns this BuildTasks object.
	 */
	public BuildStore getBuildStore() {
		return buildStore;
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * Helper function for translating from an ordinal integer to an OperationType. This is the
	 * opposite of OperationType.ordinal().
	 * 
	 * @param opTypeNum The ordinal value of a OperationType value.
	 * @return The corresponding OperationType value.
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
