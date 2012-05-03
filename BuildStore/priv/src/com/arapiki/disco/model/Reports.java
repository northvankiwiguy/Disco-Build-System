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
import java.util.Iterator;

import com.arapiki.disco.model.BuildTasks.OperationType;
import com.arapiki.disco.model.FileNameSpaces.PathType;
import com.arapiki.disco.model.types.ComponentSet;
import com.arapiki.disco.model.types.FileRecord;
import com.arapiki.disco.model.types.FileSet;
import com.arapiki.disco.model.types.TaskRecord;
import com.arapiki.disco.model.types.TaskSet;
import com.arapiki.utils.errors.ErrorCode;

/**
 * A manager class (that supports the BuildStore class) that handles reporting of
 * information from the BuildStore. These reports are able to access the database
 * directly, rather than using the standard BuildStore APIs.
 * <p>
 * There should be exactly one Reports object per BuildStore object. Use the
 * BuildStore's getReports() method to obtain that one instance.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
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
	 * The FileNameSpaces object we're reporting on. This is provided to us when the
	 * Reports object is first instantiated.
	 */
	private FileNameSpaces fns = null;
	
	/**
	 * The BuildTasks object we're reporting on. This is provided to us when the
	 * Reports object is first instantiated.
	 */
	private BuildTasks bts = null;
	
	/**
	 * Various prepared statement for database access.
	 */
	private PreparedStatement 
		selectFileAccessCountPrepStmt = null,
		selectFileIncludesCountPrepStmt = null,
		selectFilesNotUsedPrepStmt = null,
		selectFilesWithMatchingNamePrepStmt = null,
		selectTasksWithMatchingNamePrepStmt = null,
		selectDerivedFilesPrepStmt = null,
		selectInputFilesPrepStmt = null,
		selectTasksAccessingFilesPrepStmt = null,
		selectTasksAccessingFilesAnyPrepStmt = null,
		selectFilesAccessedByTaskPrepStmt = null,
		selectFilesAccessedByTaskAnyPrepStmt = null,
		selectWriteOnlyFilesPrepStmt = null,
		selectAllFilesPrepStmt = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new Reports manager object, which performs a lot of the reporting work
	 * on behalf of the BuildStore.
	 * 
	 * @param buildStore The BuildStore than owns this Reports object.
	 */
	public Reports(BuildStore buildStore) {
		this.db = buildStore.getBuildStoreDB();
		this.fns = buildStore.getFileNameSpaces();
		this.bts = buildStore.getBuildTasks();
		
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

		selectTasksWithMatchingNamePrepStmt = db.prepareStatement(
				"select taskId from buildTasks where command like ?");

		selectDerivedFilesPrepStmt = db.prepareStatement(
				"select distinct fileId from buildTaskFiles where taskId in " +
				"(select taskId from buildTaskFiles where fileId = ? and " +
				"operation = " + OperationType.OP_READ.ordinal() + ") " + 
				"and operation = " + OperationType.OP_WRITE.ordinal());
		
		selectInputFilesPrepStmt = db.prepareStatement(
				"select distinct fileId from buildTaskFiles where taskId in " +
				"(select taskId from buildTaskFiles where fileId = ? and " +
				"operation = " + OperationType.OP_WRITE.ordinal() + ") " + 
				"and operation = " + OperationType.OP_READ.ordinal());
		
		selectTasksAccessingFilesPrepStmt = db.prepareStatement(
				"select taskId from buildTaskFiles where fileId = ? and operation = ?");
		
		selectTasksAccessingFilesAnyPrepStmt = db.prepareStatement(
				"select taskId from buildTaskFiles where fileId = ?");
		
		selectFilesAccessedByTaskPrepStmt = db.prepareStatement(
				"select fileId from buildTaskFiles where taskId = ? and operation = ?");
		
		selectFilesAccessedByTaskAnyPrepStmt = db.prepareStatement(
				"select fileId from buildTaskFiles where taskId = ?");
		
		selectWriteOnlyFilesPrepStmt = db.prepareStatement(
				    "select writeFileId from (select distinct fileId as writeFileId from " +
				    "buildTaskFiles where operation = " + OperationType.OP_WRITE.ordinal() + ") " +
				    "left join (select distinct fileId as readFileId from buildTaskFiles " +
				    "where operation = " + OperationType.OP_READ.ordinal() + ") on writeFileId = readFileId " +
				      "where readFileId is null");
		
		selectAllFilesPrepStmt = db.prepareStatement("select id from files");
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Provides an ordered array of the most commonly accessed files across the whole build store.
	 * For each record in the result set, return the number of unique tasks that access the file.
	 * 
	 * @return An array of FileRecord, sorted with the most commonly accessed file first.
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

	/**
	 * Generate a report to show which files are the most common includers of the specified
	 * file. This provides information on where the specified file is used the most often.
	 * 
	 * @param includedFile ID of the file that is being included.
	 * @return An ordered array of FileRecord objects, containing the output of the report.
	 */
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

	/**
	 * Return a FileSet of all files in the BuildStore that aren't accessed by any tasks.
	 * This helps identify which source files are never used.
	 * 
	 * @return The set of files that are never accessed.
	 */
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
	 * Return the set of files (not directories) that match the user-specified file name.
	 * 
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
	 * Return the set of tasks whose command string matches the user-specified pattern.
	 * 
	 * @param pattern The user-specified pattern to match.
	 * @return The TaskSet of matching file names.
	 */
	public TaskSet reportTasksThatMatchName(String pattern) {
		
		Integer results[];
		try {
			selectTasksWithMatchingNamePrepStmt.setString(1, "%" + pattern + "%");
			results = db.executePrepSelectIntegerColumn(selectTasksWithMatchingNamePrepStmt);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return new TaskSet(bts, results);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given the input FileSet (sourceFileSet), return a new FileSet containing all the 
	 * paths that are derived from the paths listed in sourceFileSet. A file (A) is derived 
	 * from file (B) if there's a task (or sequence of paths) that reads B and writes to A.
	 * 
	 * @param sourceFileSet The FileSet of all files we should consider as input to the tasks.
	 * @param reportIndirect Set if we should also report on indirectly derived files
	 * (with multiple tasks between file A and file B).
	 * @return The FileSet of derived paths.
	 */
	public FileSet reportDerivedFiles(FileSet sourceFileSet, boolean reportIndirect) {
		return reportDerivedFilesHelper(sourceFileSet, reportIndirect, selectDerivedFilesPrepStmt);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given the input FileSet (targetFileSet), return a new FileSet containing all the 
	 * paths that are used as input when generating the paths listed in targetFileSet. 
	 * A file (A) is input for file (B) if there's a task (or sequence of paths) that reads A 
	 * and writes to B. This is the opposite of reportDerivedFiles().
	 * 
	 * @param targetFileSet The FileSet of all files that are the target of tasks.
	 * @param reportIndirect Set if we should also report on indirectly derived files
	 * (multiple tasks between file A and file B).
	 * @return The FileSet of input paths.
	 */
	public FileSet reportInputFiles(FileSet targetFileSet, boolean reportIndirect) {
		return reportDerivedFilesHelper(targetFileSet, reportIndirect, selectInputFilesPrepStmt);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Given a FileSet, return a TaskSet containing all the build tasks that access this
	 * collection files. The opType specifies whether we want tasks that read these files,
	 * write to these files, or either read or write.
	 * 
	 * @param fileSet The set of input files.
	 * @param opType Either OP_READ, OP_WRITE or OP_UNSPECIFIED.
	 * @return a TaskSet of all tasks that access these files in the mode specified.
	 */
	public TaskSet reportTasksThatAccessFiles(FileSet fileSet,
			OperationType opType) {
		
		/* create an empty result TaskSet */
		TaskSet results = new TaskSet(bts);
		
		/* for each file in the FileSet */
		for (Iterator<Integer> iterator = fileSet.iterator(); iterator.hasNext();) {		
			int fileId = (Integer) iterator.next();
			
			/* find the tasks that access this file */
			try {
				ResultSet rs;
				
				/* the case where we care about the operation type */
				if (opType != OperationType.OP_UNSPECIFIED) {
					selectTasksAccessingFilesPrepStmt.setInt(1, fileId);
					selectTasksAccessingFilesPrepStmt.setInt(2, opType.ordinal());
					rs = db.executePrepSelectResultSet(selectTasksAccessingFilesPrepStmt);
				} 
				
				/* the case where we don't care */
				else {
					selectTasksAccessingFilesAnyPrepStmt.setInt(1, fileId);
					rs = db.executePrepSelectResultSet(selectTasksAccessingFilesAnyPrepStmt);	
				}
				
				/* add the results into our TaskSet */
				while (rs.next()) {
					int taskId = rs.getInt(1);
				
					/* only add the result if it's not in the set */
					if (!results.isMember(taskId)){
						TaskRecord record = new TaskRecord(rs.getInt(1));
						results.add(record);
					}
				}
				rs.close();
			
			} catch (SQLException e) {
				throw new FatalBuildStoreError("Unable to execute SQL statement", e);
			}
		}
		
		return results;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a TaskSet, return a FileSet containing all the files that are accessed by this
	 * collection of tasks. The opType specifies whether we want files that are read by
	 * these tasks, written by these tasks, or either read or written. 
	 * 
	 * @param taskSet The set of input tasks
	 * @param opType Either OP_READ, OP_WRITE or OP_UNSPECIFIED.
	 * @return a FileSet of all files that are accessed by these tasks, in the mode specified.
	 */
	public FileSet reportFilesAccessedByTasks(TaskSet taskSet, OperationType opType) {
		
		/* create an empty result FileSet */
		FileSet results = new FileSet(fns);
		
		/* for each task in the TaskSet */
		for (Iterator<Integer> iterator = taskSet.iterator(); iterator.hasNext();) {		
			int taskId = (Integer) iterator.next();
			
			/* find the tasks that access this file */
			try {
				ResultSet rs;
				
				/* the case where we care about the operation type */
				if (opType != OperationType.OP_UNSPECIFIED) {
					selectFilesAccessedByTaskPrepStmt.setInt(1, taskId);
					selectFilesAccessedByTaskPrepStmt.setInt(2, opType.ordinal());
					rs = db.executePrepSelectResultSet(selectFilesAccessedByTaskPrepStmt);
				} 
				
				/* the case where we don't care */
				else {
					selectFilesAccessedByTaskAnyPrepStmt.setInt(1, taskId);
					rs = db.executePrepSelectResultSet(selectFilesAccessedByTaskAnyPrepStmt);	
				}
				
				/* add the results into our FileSet */
				while (rs.next()) {
					int fileId = rs.getInt(1);
				
					/* only add the result if it's not in the set */
					if (!results.isMember(fileId)){
						FileRecord record = new FileRecord(rs.getInt(1));
						results.add(record);
					}
				}
				rs.close();
			
			} catch (SQLException e) {
				throw new FatalBuildStoreError("Unable to execute SQL statement", e);
			}
		}
		
		return results;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the set of files that are written to by a build task, but aren't ever
	 * read by a different task. This generally implies that the file is a "final"
	 * file (such as an executable program, or release package), rather than an intermediate
	 * file (such as an object file).
	 * 
	 * @return The FileSet of write-only files.
	 */
	public FileSet reportWriteOnlyFiles() {
		
		FileSet results = new FileSet(fns);
		try {
			ResultSet rs = db.executePrepSelectResultSet(selectWriteOnlyFilesPrepStmt);

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
	 * Return the complete set of files in the BuildStore. The allows us to generate
	 * a FileSet containing all known files.
	 * @return The complete set of files in the BuildStore.
	 */
	public FileSet reportAllFiles() {
		FileSet results = new FileSet(fns);
		try {
			ResultSet rs = db.executePrepSelectResultSet(selectAllFilesPrepStmt);

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
	 * Given a ComponentSet, return the complete FileSet of all files that belong to components
	 * that are members of the set.
	 * 
	 * @param compSet The ComponentSet that selects the components to be included.
	 * @return The FileSet of files that are within the selected components.
	 */
	public FileSet reportFilesFromComponentSet(ComponentSet compSet) {
		FileSet results = new FileSet(fns);
		Components compMgr = compSet.getBuildStore().getComponents();
		
		/*
		 * Form the (complex) query string, which considers each component/scope individually.
		 */
		StringBuffer sb = new StringBuffer(256);
		sb.append("select id from files where ");
		int memberCount = 0;
		
		String compList[] = compMgr.getComponents();
		for (String compName : compList) {
			int compId = compMgr.getComponentId(compName);
			if (compId != ErrorCode.NOT_FOUND) {
				
				/* is this component in the set? */
				boolean hasPrivate = compSet.isMember(compId, Components.SCOPE_PRIVATE);
				boolean hasPublic = compSet.isMember(compId, Components.SCOPE_PUBLIC);
		
				/* do we need a "or" between neighboring tests? */
				if (hasPrivate || hasPublic) {
					memberCount++;
					if (memberCount > 1) {
						sb.append(" or ");
					}
				}
				
				/* form the condition for comparing the file's components/scope */
				if (hasPrivate && hasPublic) {
					sb.append("(compId == " + compId + ")");
				} else if (hasPrivate) {
					sb.append("((compId == " + compId + 
								") and (compScopeId == " + Components.SCOPE_PRIVATE + "))");
				} else if (hasPublic) {
					sb.append("((compId == " + compId + 
								") and (compScopeId == " + Components.SCOPE_PUBLIC + "))");
				}
				
			}
		}
		
		/* if the component set was empty, so to is the result set */
		if (memberCount == 0) {
			return results;
		}
		
		ResultSet rs = db.executeSelectResultSet(sb.toString());
		try {
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
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * A helper method for reportDerivedFiles and reportInputFiles that both use the same
	 * algorithm, but a slightly different SQL query.
	 * 
	 * @param startFileSet The set of files that we're deriving from, or that are used as
	 * the target of the derivation.
	 * @param reportIndirect True if we should do multiple iterations of derivation.
	 * @param sqlStatement The prepared SQL statement to find derived/input files.
	 * @return The result FileSet.
	 */
	private FileSet reportDerivedFilesHelper(FileSet startFileSet, boolean reportIndirect,
			PreparedStatement sqlStatement) {
		
		/* 
		 * Create a new empty FileSet for tracking all the results. Each time we
		 * iterate through the loop, we merge the results into this FileSet
		 */
		FileSet results = new FileSet(fns);
		
		/* the first set of files to analyze */
		FileSet nextFileSet = startFileSet;
		
		/* 
		 * This variable is used to track the progress of finding new results.
		 * If it doesn't change from one iteration to the next, we stop the iterations.
		 */
		int lastNumberOfResults = 0;

		/* 
		 * Start the iterations. If "reportIndirect" is false, we'll only execute
		 * the loop once. Each time we go around the loop, we take the files
		 * from the previous round and treat them as the new set of start files.
		 */
		boolean done = false;
		do {
			
			/* empty FileSet to collect this round's set of results */
			FileSet thisRoundOfResults = new FileSet(fns);

			/* iterate through each of the files in this round's FileSet */
			for (int fileId : nextFileSet) {

				/* determine the direct input/output files for this file, and add them to the result set */
				try {
					sqlStatement.setInt(1, fileId);
					ResultSet rs = db.executePrepSelectResultSet(sqlStatement);

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
			results.mergeSet(thisRoundOfResults);
			
			/* are we done? Did we find any new results in this iteration? */
			int thisNumberOfResults = results.size();
			done = (thisNumberOfResults == lastNumberOfResults);
			lastNumberOfResults = thisNumberOfResults;
			
		} while (reportIndirect && !done);
		
		/* return the combined set of results */
		return results;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
