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
import java.util.ArrayList;
import java.util.Iterator;

import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IActionMgr;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.IFileMgr;
import com.buildml.model.IFileMgr.PathType;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IReportMgr;
import com.buildml.model.types.FileRecord;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.PackageSet;
import com.buildml.model.types.TaskSet;
import com.buildml.utils.errors.ErrorCode;

/**
 * A manager class (that supports the BuildStore class) that handles reporting of
 * information from the BuildStore. These reports are able to access the database
 * directly, rather than using the standard BuildStore APIs.
 * <p>
 * There should be exactly one ReportMgr object per BuildStore object. Use the
 * BuildStore's getReportMgr() method to obtain that one instance.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
/* package private */ class ReportMgr implements IReportMgr {
	
	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/
	
	/**
	 * Our database manager object, used to access the database content. This is provided 
	 * to us when the Reports object is first instantiated.
	 */
	private BuildStoreDB db = null;
	
	/**
	 * The FileMgr object we're reporting on. This is provided to us when the
	 * Reports object is first instantiated.
	 */
	private IFileMgr fileMgr = null;
	
	/**
	 * The ActionMgr object we're reporting on. This is provided to us when the
	 * Reports object is first instantiated.
	 */
	private IActionMgr actionMgr = null;
	
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
		selectAllFilesPrepStmt = null,
		selectAllActionsPrepStmt = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new Reports manager object, which performs a lot of the reporting work
	 * on behalf of the BuildStore.
	 * 
	 * @param buildStore The BuildStore than owns this Reports object.
	 */
	public ReportMgr(BuildStore buildStore) {
		this.db = buildStore.getBuildStoreDB();
		this.fileMgr = buildStore.getFileMgr();
		this.actionMgr = buildStore.getActionMgr();
		
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
		selectAllActionsPrepStmt = db.prepareStatement("select taskId from buildTasks");
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportMostCommonlyAccessedFiles()
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportMostCommonIncludersOfFile(int)
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportFilesNeverAccessed()
	 */
	@Override
	public FileSet reportFilesNeverAccessed() {
		
		FileSet results = new FileSet(fileMgr);
		try {
			ResultSet rs = db.executePrepSelectResultSet(selectFilesNotUsedPrepStmt);

			while (rs.next()) {
				results.add(rs.getInt(1));
			}
			rs.close();
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportFilesThatMatchName(java.lang.String)
	 */
	@Override
	public FileSet reportFilesThatMatchName(String fileArg) {
		
		/* map any occurrences of * into %, since that's what SQL requires */
		if (fileArg != null) {
			fileArg = fileArg.replace('*', '%');
		}
		
		FileSet results = new FileSet(fileMgr);
		try {
			selectFilesWithMatchingNamePrepStmt.setString(1, fileArg);
			ResultSet rs = db.executePrepSelectResultSet(selectFilesWithMatchingNamePrepStmt);

			while (rs.next()) {
				results.add(rs.getInt(1));
			}
			rs.close();
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportTasksThatMatchName(java.lang.String)
	 */
	@Override
	public TaskSet reportTasksThatMatchName(String pattern) {
		
		Integer results[];
		try {
			selectTasksWithMatchingNamePrepStmt.setString(1, "%" + pattern + "%");
			results = db.executePrepSelectIntegerColumn(selectTasksWithMatchingNamePrepStmt);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return new TaskSet(actionMgr, results);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportDerivedFiles(com.buildml.model.types.FileSet, boolean)
	 */
	@Override
	public FileSet reportDerivedFiles(FileSet sourceFileSet, boolean reportIndirect) {
		return reportDerivedFilesHelper(sourceFileSet, reportIndirect, selectDerivedFilesPrepStmt);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportInputFiles(com.buildml.model.types.FileSet, boolean)
	 */
	@Override
	public FileSet reportInputFiles(FileSet targetFileSet, boolean reportIndirect) {
		return reportDerivedFilesHelper(targetFileSet, reportIndirect, selectInputFilesPrepStmt);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportTasksThatAccessFiles(com.buildml.model.types.FileSet, com.buildml.model.impl.BuildTasks.OperationType)
	 */
	@Override
	public TaskSet reportTasksThatAccessFiles(FileSet fileSet,
			OperationType opType) {
		
		/* create an empty result TaskSet */
		TaskSet results = new TaskSet(actionMgr);
		
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
						results.add(rs.getInt(1));
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

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportFilesAccessedByTasks(com.buildml.model.types.TaskSet, com.buildml.model.impl.BuildTasks.OperationType)
	 */
	@Override
	public FileSet reportFilesAccessedByTasks(TaskSet taskSet, OperationType opType) {
		
		/* create an empty result FileSet */
		FileSet results = new FileSet(fileMgr);
		
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
						results.add(rs.getInt(1));
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

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportWriteOnlyFiles()
	 */
	@Override
	public FileSet reportWriteOnlyFiles() {
		
		FileSet results = new FileSet(fileMgr);
		try {
			ResultSet rs = db.executePrepSelectResultSet(selectWriteOnlyFilesPrepStmt);

			while (rs.next()) {
				results.add(rs.getInt(1));
			}
			rs.close();
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportAllFiles()
	 */
	@Override
	public FileSet reportAllFiles() {
		FileSet results = new FileSet(fileMgr);
		try {
			ResultSet rs = db.executePrepSelectResultSet(selectAllFilesPrepStmt);

			while (rs.next()) {
				results.add(rs.getInt(1));
			}
			rs.close();
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportAllActions()
	 */
	@Override
	public TaskSet reportAllActions() {
		TaskSet results = new TaskSet(actionMgr);
		try {
			ResultSet rs = db.executePrepSelectResultSet(selectAllActionsPrepStmt);

			while (rs.next()) {
				results.add(rs.getInt(1));
			}
			rs.close();
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportFilesFromPackageSet(com.buildml.model.types.PackageSet)
	 */
	@Override
	public FileSet reportFilesFromPackageSet(PackageSet pkgSet) {
		FileSet results = new FileSet(fileMgr);
		IPackageMgr pkgMgr = pkgSet.getBuildStore().getPackageMgr();
		
		/*
		 * Form the (complex) query string, which considers each package/scope individually.
		 */
		StringBuffer sb = new StringBuffer(256);
		sb.append("select id from files where ");
		int memberCount = 0;
		
		String pkgList[] = pkgMgr.getPackages();
		for (String pkgName : pkgList) {
			int pkgId = pkgMgr.getPackageId(pkgName);
			if (pkgId != ErrorCode.NOT_FOUND) {
				
				/* is this package in the set? */
				boolean hasPrivate = pkgSet.isMember(pkgId, IPackageMgr.SCOPE_PRIVATE);
				boolean hasPublic = pkgSet.isMember(pkgId, IPackageMgr.SCOPE_PUBLIC);
		
				/* do we need a "or" between neighboring tests? */
				if (hasPrivate || hasPublic) {
					memberCount++;
					if (memberCount > 1) {
						sb.append(" or ");
					}
				}
				
				/* form the condition for comparing the file's packages/scope */
				if (hasPrivate && hasPublic) {
					sb.append("(pkgId == " + pkgId + ")");
				} else if (hasPrivate) {
					sb.append("((pkgId == " + pkgId + 
								") and (pkgScopeId == " + IPackageMgr.SCOPE_PRIVATE + "))");
				} else if (hasPublic) {
					sb.append("((pkgId == " + pkgId + 
								") and (pkgScopeId == " + IPackageMgr.SCOPE_PUBLIC + "))");
				}
				
			}
		}
		
		/* if the package set was empty, so to is the result set */
		if (memberCount == 0) {
			return results;
		}
		
		ResultSet rs = db.executeSelectResultSet(sb.toString());
		try {
			while (rs.next()) {
				results.add(rs.getInt(1));
			}
			rs.close();
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results;		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IReportMgr#reportActionsFromPackageSet(com.buildml.model.types.PackageSet)
	 */
	@Override
	public TaskSet reportActionsFromPackageSet(PackageSet pkgSet) {
		TaskSet results = new TaskSet(actionMgr);
		IPackageMgr pkgMgr = pkgSet.getBuildStore().getPackageMgr();
		
		/*
		 * Form the (complex) query string.
		 */
		StringBuffer sb = new StringBuffer(256);
		sb.append("select taskId from buildTasks where ");
		int memberCount = 0;
		
		String pkgList[] = pkgMgr.getPackages();
		for (String pkgName : pkgList) {
			int pkgId = pkgMgr.getPackageId(pkgName);
			if (pkgId != ErrorCode.NOT_FOUND) {
				
				/* is this package in the set? */
				boolean isMember = pkgSet.isMember(pkgId, IPackageMgr.SCOPE_PUBLIC);
		
				/* do we need a "or" between neighboring tests? */
				if (isMember) {
					memberCount++;
					if (memberCount > 1) {
						sb.append(" or ");
					}
					
					/* form the condition for comparing the action's package. */
					sb.append("(pkgId == " + pkgId + ")");
				}
			}
		}
		
		/* if the package set was empty, so too is the result set */
		if (memberCount == 0) {
			return results;
		}
		
		ResultSet rs = db.executeSelectResultSet(sb.toString());
		try {
			while (rs.next()) {
				results.add(rs.getInt(1));
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
		FileSet results = new FileSet(fileMgr);
		
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
			FileSet thisRoundOfResults = new FileSet(fileMgr);

			/* iterate through each of the files in this round's FileSet */
			for (int fileId : nextFileSet) {

				/* determine the direct input/output files for this file, and add them to the result set */
				try {
					sqlStatement.setInt(1, fileId);
					ResultSet rs = db.executePrepSelectResultSet(sqlStatement);

					while (rs.next()) {
						thisRoundOfResults.add(rs.getInt(1));
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
