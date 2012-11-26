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
import java.util.List;

import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.utils.errors.ErrorCode;
import com.buildml.utils.string.ShellCommandUtils;

/**
 * A manager class (that supports the BuildStore class) responsible for managing all 
 * BuildStore information pertaining to actions.
 * <p>
 * There should be exactly one ActionMgr object per BuildStore object. Use the
 * BuildStore's getActionMgr() method to obtain that one instance.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class ActionMgr implements IActionMgr {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/
	
	/**
	 * Our database manager object, used to access the database content. This is provided 
	 * to us when the ActionMgr object is first instantiated.
	 */
	private BuildStoreDB db = null;
	
	/** The BuildStore object that owns this ActionMgr object. */
	private IBuildStore buildStore = null;
	
	/** The FileMgr object associated with this ActionMgr */
	private IFileMgr fileMgr = null;
	
	/** Various prepared statement for database access. */
	private PreparedStatement 
		insertActionPrepStmt = null,
		findCommandPrepStmt = null,
		findParentPrepStmt = null,
		findDirectoryPrepStmt = null,
		findChildrenPrepStmt = null,
		insertActionFilesPrepStmt = null,
		removeActionFilesPrepStmt = null,
		removeFileAccessesPrepStmt = null,
		findFileAccessBySeqnoPrepStmt = null,
		updateActionFilesPrepStmt = null,
		findOperationInActionFilesPrepStmt = null,
		findFilesInActionFilesPrepStmt = null,
		findActionsInDirectoryPrepStmt = null,
		findFilesByOperationInActionFilesPrepStmt = null,
		findActionsByFileInActionFilesPrepStmt = null,
		findActionsByFileAndOperationInActionFilesPrepStmt = null,
		trashActionPrepStmt = null,
		actionIsTrashPrepStmt = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new ActionMgr object. This object encapsulates information for all the build
	 * actions in the system.
	 * 
	 * @param buildStore The BuildStore object that "owns" this ActionMgr manager
	 */
	public ActionMgr(BuildStore buildStore) {
		this.buildStore = buildStore;
		this.db = buildStore.getBuildStoreDB();
		this.fileMgr = buildStore.getFileMgr();

		/* create prepared database statements */
		insertActionPrepStmt = db.prepareStatement("insert into buildActions values (null, ?, 0, ?, 0, ?)");
		findCommandPrepStmt = db.prepareStatement("select command from buildActions where actionId = ?");
		findParentPrepStmt = db.prepareStatement("select parentActionId from buildActions where actionId = ?");
		findDirectoryPrepStmt = db.prepareStatement("select actionDirId from buildActions where actionId = ?");
		findActionsInDirectoryPrepStmt =
			db.prepareStatement("select actionId from buildActions where (actionDirId = ?) and (trashed = 0)");
		findChildrenPrepStmt = db.prepareStatement("select actionId from buildActions where parentActionId = ?" +
				" and (parentActionId != actionId) and (trashed = 0)");
		insertActionFilesPrepStmt = db.prepareStatement("insert into actionFiles values (?, ?, ?, ?)");
		removeActionFilesPrepStmt = 
			db.prepareStatement("delete from actionFiles where actionId = ? and fileId = ?");
		findFileAccessBySeqnoPrepStmt =
			db.prepareStatement("select seqno, actionId, fileId, operation from actionFiles where seqno = ?");
		updateActionFilesPrepStmt = 
			db.prepareStatement("update actionFiles set operation = ? where actionId = ? and fileId = ?");
		findOperationInActionFilesPrepStmt = 
			db.prepareStatement("select operation from actionFiles where actionId = ? and fileId = ?");
		findFilesInActionFilesPrepStmt =
			db.prepareStatement("select fileId from actionFiles where actionId = ?");
		findFilesByOperationInActionFilesPrepStmt =
			db.prepareStatement("select fileId from actionFiles where actionId = ? and operation = ?");
		findActionsByFileInActionFilesPrepStmt =
			db.prepareStatement("select actionId from actionFiles where fileId = ?");		
		findActionsByFileAndOperationInActionFilesPrepStmt =
			db.prepareStatement("select actionId from actionFiles where fileId = ? and operation = ?");
		trashActionPrepStmt =
			db.prepareStatement("update buildActions set trashed = ? where actionId = ?");
		actionIsTrashPrepStmt =
			db.prepareStatement("select trashed from buildActions where actionId = ?");
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#addAction(int, int, java.lang.String)
	 */
	@Override
	public int addAction(int parentActionId, int actionDirId, String command) {
		
		try {
			insertActionPrepStmt.setInt(1, parentActionId);
			insertActionPrepStmt.setInt(2, actionDirId);
			insertActionPrepStmt.setString(3, command);
			db.executePrepUpdate(insertActionPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}

		int lastRowId = db.getLastRowID();
		if (lastRowId >= MAX_ACTIONS) {
			throw new FatalBuildStoreError("Exceeded maximum action number: " + MAX_ACTIONS);
		}
		return lastRowId;
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * A two-dimensional mapping table for tracking the state of each file access. If a file
	 * is accessed multiple times by a single action, the state of that access can also change. 
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

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#addFileAccess(int, int, com.buildml.model.IActionMgr.OperationType)
	 */
	@Override
	public void addFileAccess(int actionId, int fileId, OperationType newOperation) {
		
		addFileAccessCommon(-1, actionId, fileId, newOperation);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/*
	 * (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#addSequencedFileAccess(int, int, int, com.buildml.model.IActionMgr.OperationType)
	 */
	public int addSequencedFileAccess(int seqno, int actionId, 
			int fileId,	OperationType newOperation) {
		
		/* check if there's already a file access with the required sequence number */
		if (seqno != -1) {
			Integer intResults[] = null;
			try {
				findFileAccessBySeqnoPrepStmt.setInt(1, seqno);
				intResults = db.executePrepSelectIntegerColumn(findFileAccessBySeqnoPrepStmt);

			} catch (SQLException e) {
				throw new FatalBuildStoreError("Unable to execute SQL statement", e);
			}
			if (intResults.length != 0) {
				return ErrorCode.ONLY_ONE_ALLOWED;
			}
		}
		
		/* proceed to add the access, possibly merging it with existing actions */
		addFileAccessCommon(seqno, actionId, fileId, newOperation);
		return ErrorCode.OK;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#getFilesAccessed(int, com.buildml.model.IActionMgr.OperationType)
	 */
	@Override
	public Integer [] getFilesAccessed(int actionId, OperationType operation) {
				
		List<Integer> results;
		
		try {
			ResultSet rs;
			
			/* if we want all operation (OP_UNSPECIFIED), don't query the operation field */
			if (operation == OperationType.OP_UNSPECIFIED) {
				findFilesInActionFilesPrepStmt.setInt(1, actionId);
				rs = db.executePrepSelectResultSet(findFilesInActionFilesPrepStmt);
			} 
			
			/* else, we need to limit the results, based on the operation */
			else {
				findFilesByOperationInActionFilesPrepStmt.setInt(1, actionId);
				findFilesByOperationInActionFilesPrepStmt.setInt(2, operation.ordinal());
				rs = db.executePrepSelectResultSet(findFilesByOperationInActionFilesPrepStmt);				
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

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#getSequencedFileAccesses(int[])
	 */
	@Override
	public FileAccess[] getSequencedFileAccesses(Integer[] actionIds) {
				
		List<FileAccess> results = new ArrayList<FileAccess>();
		try {
			ResultSet rs;

            /* Form a comma-separated list of action IDs */
			StringBuilder actSb = new StringBuilder();
			int length = actionIds.length;
			for (int i = 0; i < length; i++) {
				actSb.append(actionIds[i]);
				if (i != length - 1) {
					actSb.append(", ");
				}
			}
			String actionsString = actSb.toString();
			
			/* query for all actionFile entries for any of the specified actions */
			String stmt = "select seqno, actionId, fileId, operation from actionFiles " +
			              "where actionId in (" + actionsString + ") order by seqno;";	
			rs = db.executeSelectResultSet(stmt);				
			
			/* read the results into an array */
			results = new ArrayList<FileAccess>();
			while (rs.next()) {
				FileAccess access = new FileAccess();
				access.seqno = rs.getInt(1);
				access.actionId = rs.getInt(2);
				access.pathId = rs.getInt(3);
				access.opType = intToOperationType(rs.getInt(4));
				results.add(access);
			}
			rs.close();
		
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* convert from List to FileAccess[] */
		return results.toArray(new FileAccess[results.size()]);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#getActionsThatAccess(int, com.buildml.model.IActionMgr.OperationType)
	 */
	@Override
	public Integer [] getActionsThatAccess(int fileId, OperationType operation) {
		
		List<Integer> results;

		try {
			ResultSet rs;
			
			/* if we want all operation (OP_UNSPECIFIED), don't query the operation field */
			if (operation == OperationType.OP_UNSPECIFIED) {
				findActionsByFileInActionFilesPrepStmt.setInt(1, fileId);
				rs = db.executePrepSelectResultSet(findActionsByFileInActionFilesPrepStmt);
			} 
			
			/* else, we need to limit the results, based on the operation */
			else {
				findActionsByFileAndOperationInActionFilesPrepStmt.setInt(1, fileId);
				findActionsByFileAndOperationInActionFilesPrepStmt.setInt(2, operation.ordinal());
				rs = db.executePrepSelectResultSet(findActionsByFileAndOperationInActionFilesPrepStmt);				
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

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#removeAccessesToPath(int)
	 */
	@Override
	public void removeFileAccess(int actionId, int pathId) {
		try {
			removeActionFilesPrepStmt.setInt(1, actionId);
			removeActionFilesPrepStmt.setInt(2, pathId);
			db.executePrepUpdate(removeActionFilesPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}		
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#getActionsInDirectory(int)
	 */
	@Override
	public Integer[] getActionsInDirectory(int pathId) {
		
		Integer intResults[] = null;
		try {
			findActionsInDirectoryPrepStmt.setInt(1, pathId);
			intResults = db.executePrepSelectIntegerColumn(findActionsInDirectoryPrepStmt);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		return intResults;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#getCommand(int)
	 */
	@Override
	public String getCommand(int actionId) {
		String [] stringResults = null;
		try {
			findCommandPrepStmt.setInt(1, actionId);
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
			throw new FatalBuildStoreError("Multiple results find in buildActions table for actionId = " + actionId);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#getCommandSummary(int, int)
	 */
	@Override
	public String getCommandSummary(int actionId, int width) {
		
		/* 
		 * For now, we treat all commands as being the same, and simply return the
		 * first 'width' characters from the action's command string.
		 */
		String command = ShellCommandUtils.joinCommandLine(getCommand(actionId));
		
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


	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#getParent(int)
	 */
	@Override
	public int getParent(int actionId) {
		
		/* query the database, based on the action Id */
		Integer [] intResults = null;
		try {
			findParentPrepStmt.setInt(1, actionId);
			intResults = db.executePrepSelectIntegerColumn(findParentPrepStmt);
		} catch (SQLException e) {
			new FatalBuildStoreError("Error in SQL: " + e);
		}
		
		/* if there were no results, it's because actionId is invalid. Return an error */
		if (intResults.length == 0) {
			return ErrorCode.BAD_VALUE;
		}
		
		/* if there was one result, return it */
		else if (intResults.length == 1) {
			
			/* the single result is the parent, unless this action's parent is itself! */
			int parentActionId = intResults[0];
			if (parentActionId == actionId) {
				/* the current action is at the root */
				return ErrorCode.NOT_FOUND;
			}
			return parentActionId;
			
		}
		
		/* else, multiple results is a bad thing */
		else {
			throw new FatalBuildStoreError("Multiple results find in buildActions table for actionId = " + actionId);
		}	
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#getDirectory(int)
	 */
	@Override
	public int getDirectory(int actionId) {
		Integer [] intResults = null;
		try {
			findDirectoryPrepStmt.setInt(1, actionId);
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
			throw new FatalBuildStoreError("Multiple results find in buildActions table for actionId = " + actionId);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#getChildren(int)
	 */
	@Override
	public Integer [] getChildren(int actionId) {
		
		Integer [] intResults = null;
		try {
			findChildrenPrepStmt.setInt(1, actionId);
			intResults = db.executePrepSelectIntegerColumn(findChildrenPrepStmt);
		} catch (SQLException e) {
			new FatalBuildStoreError("Error in SQL: " + e);
		}

		return intResults;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#getRootAction(java.lang.String)
	 */
	@Override
	public int getRootAction(String rootName) {
		
		// TODO: return something other than 0. Currently the default action is created
		// implicitly, rather than explicitly
		return 0;
	}
	
	/*-------------------------------------------------------------------------------------*/

	
	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#moveActionToTrash(int)
	 */
	@Override
	public int moveActionToTrash(int actionId) {
		
		/* check that the action is not the root action */
		if (getParent(actionId) == ErrorCode.NOT_FOUND) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* check that the action has no children */
		Integer children[] = getChildren(actionId);
		if (children.length != 0) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* check that the action is not referenced by a file */
		Integer filesAccess[] = getFilesAccessed(actionId, OperationType.OP_UNSPECIFIED);
		if (filesAccess.length != 0) {
			return ErrorCode.CANT_REMOVE;
		}

		/* mark action as trashed */		
		try {
			trashActionPrepStmt.setInt(1, 1);
			trashActionPrepStmt.setInt(2, actionId);
			db.executePrepUpdate(trashActionPrepStmt);
		} catch (SQLException e) {
			new FatalBuildStoreError("Error in SQL: " + e);
		}
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#reviveActionFromTrash(int)
	 */
	@Override
	public int reviveActionFromTrash(int actionId) {
		
		/* first, check that the parent action is not trashed */
		int parentId = getParent(actionId);
		if ((parentId == ErrorCode.NOT_FOUND) || (parentId == ErrorCode.BAD_VALUE)) {
			return ErrorCode.CANT_REVIVE;
		}
		if (isActionTrashed(parentId)) {
			return ErrorCode.CANT_REVIVE;
		}
		
		/* mark action as not trashed */		
		try {
			trashActionPrepStmt.setInt(1, 0);
			trashActionPrepStmt.setInt(2, actionId);
			db.executePrepUpdate(trashActionPrepStmt);
		} catch (SQLException e) {
			new FatalBuildStoreError("Error in SQL: " + e);
		}
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#isActionTrashed(int)
	 */
	@Override
	public boolean isActionTrashed(int actionId) {
		Integer results[] = null;
		
		try {
			actionIsTrashPrepStmt.setInt(1, actionId);
			results = db.executePrepSelectIntegerColumn(actionIsTrashPrepStmt);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error in SQL: " + e);
		}
		
		/* action isn't even known - let's assume it's trashed */
		if (results.length != 1) {
			return true;
		}
		
		/* if "trashed" field is 1, then the action is trashed */
		return results[0] == 1;
	}

	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#getBuildStore()
	 */
	@Override
	public IBuildStore getBuildStore() {
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

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * A helper method for addFileAccess() and addSequencedFileAccess().
	 * 
	 * @param seqno The sequence number to use for the new file-access, or -1 if we should use
	 * 		        the next available number.
	 * @param actionId The action that performs the access.
	 * @param fileId The file that is accessed.
	 * @param newOperation The operation type of the access.
	 */
	private void addFileAccessCommon(int seqno, int actionId, 
			int fileId, OperationType newOperation) {
		
		/* 
		 * We don't want to add the same record twice, but we might want to merge the two
		 * operations together. That is, if a action reads a file, then writes a file, we want
		 * to mark it as OP_MODIFIED.
		 */
		Integer intResults[] = null;
		try {
			findOperationInActionFilesPrepStmt.setInt(1, actionId);
			findOperationInActionFilesPrepStmt.setInt(2, fileId);
			intResults = db.executePrepSelectIntegerColumn(findOperationInActionFilesPrepStmt);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/*
		 * If there was no existing record, we'll insert a fresh record.
		 */
		if (intResults.length == 0) {
			try {
				if (seqno == -1) {
					insertActionFilesPrepStmt.setNull(1, java.sql.Types.INTEGER);
				} else {
					insertActionFilesPrepStmt.setInt(1, seqno);
				}
				insertActionFilesPrepStmt.setInt(2, actionId);
				insertActionFilesPrepStmt.setInt(3, fileId);
				insertActionFilesPrepStmt.setInt(4, newOperation.ordinal());
				db.executePrepUpdate(insertActionFilesPrepStmt);
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
			 * by this action.
			 */
			if ((existingOp == OperationType.OP_WRITE) && (combinedOp == OperationType.OP_DELETE)) {
				
				/* remove all file accesses that we previously added */
				removeFileAccess(actionId, fileId);

				/*
				 * Attempt to remove the file from the FileMgr. This will fail if the
				 * same path is already used by some other action, but that's acceptable. We
				 * only want to remove paths that were used exclusively by this action.
				 */
				fileMgr.movePathToTrash(fileId);
			}
			
			/*
			 * else, the normal case is to replace the old state with the new state.
			 */
			else {
				try {
					updateActionFilesPrepStmt.setInt(1, combinedOp.ordinal());
					updateActionFilesPrepStmt.setInt(2, actionId);
					updateActionFilesPrepStmt.setInt(3, fileId);
					db.executePrepUpdate(updateActionFilesPrepStmt);
				} catch (SQLException e) {
					throw new FatalBuildStoreError("Unable to execute SQL statement", e);
				}
			}
		}
		
		/* else, there's an error - can't have multiple entries */
		else {
			throw new FatalBuildStoreError("Multiple results find in actionFiles table for actionId = " 
					+ actionId + " and fileId = " + fileId);
		}
	}
}
