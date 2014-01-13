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
import com.buildml.model.IActionMgrListener;
import com.buildml.model.IActionTypeMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMemberMgr.MemberDesc;
import com.buildml.model.IPackageMgr;
import com.buildml.model.ISlotTypes;
import com.buildml.model.ISlotTypes.SlotDetails;
import com.buildml.utils.errors.ErrorCode;

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
	
	/** The PackageMgr object associated with this ActionMgr */
	private IPackageMgr pkgMgr = null;	

	/** The PackageMemberMgr object associated with this ActionMgr */
	private IPackageMemberMgr pkgMemberMgr = null;	

	/** The SlotMgr object associated with this ActionMgr */
	private SlotMgr slotMgr = null;	
	
	/** Various prepared statement for database access. */
	private PreparedStatement 
		insertActionPrepStmt = null,
		insertPackageMemberPrepStmt = null,
		findParentPrepStmt = null,
		updateParentPrepStmt = null,
		findChildrenPrepStmt = null,
		insertActionFilesPrepStmt = null,
		removeActionFilesPrepStmt = null,
		findFileAccessBySeqnoPrepStmt = null,
		updateActionFilesPrepStmt = null,
		findOperationInActionFilesPrepStmt = null,
		findFilesInActionFilesPrepStmt = null,
		findFilesByOperationInActionFilesPrepStmt = null,
		findActionsByFileInActionFilesPrepStmt = null,
		findActionsByFileAndOperationInActionFilesPrepStmt = null,
		trashActionPrepStmt = null,
		actionIsTrashPrepStmt = null,
		findActionTypePrepStmt = null;
	
	/** The event listeners who are registered to learn about action changes */
	private List<IActionMgrListener> listeners = new ArrayList<IActionMgrListener>();
	
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
		this.slotMgr = buildStore.getSlotMgr();

		/* create prepared database statements */
		insertActionPrepStmt = db.prepareStatement("insert into buildActions values (null, 0, 0, ?)");
		insertPackageMemberPrepStmt = db.prepareStatement("insert into packageMembers values (?, ?, ?, ?, -1, -1)");
		findParentPrepStmt = db.prepareStatement("select parentActionId from buildActions where actionId = ?");
		updateParentPrepStmt = db.prepareStatement("update buildActions set parentActionId = ? where actionId = ?");
		findChildrenPrepStmt = db.prepareStatement("select actionId from buildActions where parentActionId = ?" +
				" and (parentActionId != actionId) and (trashed = 0) order by actionId");
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
		findActionTypePrepStmt =
			db.prepareStatement("select actionType from buildActions where actionId = ?");
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/	

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#addAction(int, int)
	 */
	@Override
	public int addAction(int actionTypeId) {
		
		this.pkgMgr = buildStore.getPackageMgr();
		IActionTypeMgr actionTypeMgr = buildStore.getActionTypeMgr();
		if (!actionTypeMgr.isValid(actionTypeId) || actionTypeMgr.isFolder(actionTypeId)) {
			return ErrorCode.NOT_FOUND;
		}

		int lastRowId;
		try {
			insertActionPrepStmt.setInt(1, actionTypeId);
			db.executePrepUpdate(insertActionPrepStmt);
		
			lastRowId = db.getLastRowID();
			if (lastRowId >= MAX_ACTIONS) {
				throw new FatalBuildStoreError("Exceeded maximum action number: " + MAX_ACTIONS);
			}

			/* insert the default package membership values */
			insertPackageMemberPrepStmt.setInt(1, IPackageMemberMgr.TYPE_ACTION);
			insertPackageMemberPrepStmt.setInt(2, lastRowId);
			insertPackageMemberPrepStmt.setInt(3, pkgMgr.getImportPackage());
			insertPackageMemberPrepStmt.setInt(4, IPackageMemberMgr.SCOPE_NONE);
			if (db.executePrepUpdate(insertPackageMemberPrepStmt) != 1) {
				throw new FatalBuildStoreError("Unable to insert new record into packageMembers table");
			}

		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		return lastRowId;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#addAction(int, int, java.lang.String)
	 */
	@Override
	public int addShellCommandAction(int parentActionId, int actionDirId, String command) {
		
		/* create a new action of type "Shell Command" */
		int newActionId = addAction(ActionTypeMgr.BUILTIN_SHELL_COMMAND_ID);
		if (newActionId < 0) {
			return newActionId;
		}

		/* set the action's parent */
		int rc = setParent(newActionId, parentActionId);
		if (rc != ErrorCode.OK) {
			return rc;
		}
		
		/* set the action's command string */
		rc = setSlotValue(newActionId, IActionMgr.COMMAND_SLOT_ID, command);
		if (rc != ErrorCode.OK) {
			return rc;
		}

		/* set the action's working directory */
		rc = setSlotValue(newActionId, IActionMgr.DIRECTORY_SLOT_ID, actionDirId);
		if (rc != ErrorCode.OK) {
			return rc;
		}

		return newActionId;
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
	 * @see com.buildml.model.IActionMgr#getActionType(int)
	 */
	@Override
	public int getActionType(int actionId) {
				
		Integer intResults[] = null;
		try {
			findActionTypePrepStmt.setInt(1, actionId);
			intResults = db.executePrepSelectIntegerColumn(findActionTypePrepStmt);
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		if (intResults.length == 0) {
			return ErrorCode.NOT_FOUND;
		}
		
		return intResults[0];
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
	 * @see com.buildml.model.IActionMgr#setParent(int, int)
	 */
	@Override
	public int setParent(int actionId, int newParentId) {
		
		/* we can't be our own parent */
		if (actionId == newParentId) {
			return ErrorCode.BAD_VALUE;
		}
		
		/* we can't change the parent of the root */
		if (actionId == getRootAction("root")) {
			return ErrorCode.BAD_VALUE;
		}
		
		/* 
		 * Check that we are not an ancestor of our new parent (this would cause
		 * a loop in the tree - not allowed). Loop upwards through the tree,
		 * until we reach the root (parent is NOT_FOUND).
		 */
		int ancestorId = newParentId;
		while (ancestorId != ErrorCode.NOT_FOUND) {
			ancestorId = getParent(ancestorId);

			/* is newParentId a bad action ID? */
			if (ancestorId == ErrorCode.BAD_VALUE) {
				return ErrorCode.BAD_VALUE;
			}
		
			/* else, is actionId in the ancestor chain of newParentId? */
			if (actionId == ancestorId) {
				return ErrorCode.BAD_VALUE;
			}
		}

		/* attempt to update the existing record for actionId */
		try {
			updateParentPrepStmt.setInt(1, newParentId);
			updateParentPrepStmt.setInt(2, actionId);
			if (db.executePrepUpdate(updateParentPrepStmt) != 1) {
				/* there was no record for actionId */
				return ErrorCode.BAD_VALUE;
			}
		} catch (SQLException e) {
			new FatalBuildStoreError("Error in SQL: " + e);
		}
		
		return ErrorCode.OK;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#getDirectory(int)
	 */
	@Override
	public int getDirectory(int actionId) {
		
		Object result = getSlotValue(actionId, IActionMgr.DIRECTORY_SLOT_ID);
		if (result == null) {
			return ErrorCode.NOT_FOUND;
		}
		
		return (Integer)result;
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
		
		/* notify listeners of the change */
		notifyListeners(actionId, IActionMgrListener.TRASHED_ACTION, 0);
		
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

		/* notify listeners of the change */
		notifyListeners(actionId, IActionMgrListener.TRASHED_ACTION, 0);

		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#isActionValid(int)
	 */
	@Override
	public boolean isActionValid(int actionId) {
		
		/* all valid actions have parents */
		return getParent(actionId) != ErrorCode.BAD_VALUE;
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
	 * @see com.buildml.model.IActionMgr#getSlotByName(int, java.lang.String)
	 */
	@Override
	public int getSlotByName(int actionId, String slotName) {

		IActionTypeMgr actionTypeMgr = buildStore.getActionTypeMgr();
		int actionTypeId = getActionType(actionId);
		if (actionTypeId == ErrorCode.NOT_FOUND) {
			return ErrorCode.NOT_FOUND;
		}
		SlotDetails details = actionTypeMgr.getSlotByName(actionTypeId, slotName);
		if (details == null) {
			return ErrorCode.NOT_FOUND;
		}
		
		return details.slotId;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#setSlotValue(int, int, java.lang.Object)
	 */
	@Override
	public int setSlotValue(int actionId, int slotId, Object value) {

		/* validate actionId */
		if (!isActionValid(actionId)) {
			return ErrorCode.NOT_FOUND;
		}
				
		/* what is the current slot value? */
		Object oldValue = slotMgr.getSlotValue(SlotMgr.SLOT_OWNER_ACTION, actionId, slotId);
		
		/* if no change in value, do nothing */
		if (((oldValue == null) && (value == null)) || 
			((oldValue != null) && (oldValue.equals(value)))) {
			return ErrorCode.OK;
		}
		
		/*
		 * If the slot is an input or output, we first need to check for cycles in the graph.
		 */
		SlotDetails details = slotMgr.getSlotByID(slotId);
		if (details == null) {
			return ErrorCode.NOT_FOUND;
		}
		if (((details.slotPos == ISlotTypes.SLOT_POS_INPUT) || 
			 (details.slotPos == ISlotTypes.SLOT_POS_OUTPUT)) &&
			(value instanceof Integer)) {

				/* yes, we're inserting a file group into an action. Check for cycles */
				int direction = 
						(details.slotPos == ISlotTypes.SLOT_POS_OUTPUT) ?
						    IPackageMemberMgr.NEIGHBOUR_LEFT : IPackageMemberMgr.NEIGHBOUR_RIGHT;
				if (checkForCycles(IPackageMemberMgr.TYPE_ACTION, actionId, (Integer)value, direction)) {
					return ErrorCode.LOOP_DETECTED;
				}
		}
		
		/* delegate all slot assignments to SlotMgr */
		int status = slotMgr.setSlotValue(SlotMgr.SLOT_OWNER_ACTION, actionId, slotId, value);
		
		/* notify listeners about the change */
		notifyListeners(actionId, IActionMgrListener.CHANGED_SLOT, slotId);
		return status;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#getSlotValue(int, int)
	 */
	@Override
	public Object getSlotValue(int actionId, int slotId) {
		
		/* validate actionId */
		if (!isActionValid(actionId)) {
			return null;
		}

		/* delegate all slot assignments to SlotMgr */
		return slotMgr.getSlotValue(SlotMgr.SLOT_OWNER_ACTION, actionId, slotId);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#isSlotSet(int, int)
	 */
	@Override
	public boolean isSlotSet(int actionId, int slotId) {
		return slotMgr.isSlotSet(SlotMgr.SLOT_OWNER_ACTION, actionId, slotId);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#clearSlotValue(int, int)
	 */
	@Override
	public void clearSlotValue(int actionId, int slotId) {
		slotMgr.clearSlotValue(SlotMgr.SLOT_OWNER_ACTION, actionId, slotId);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#getActionsWhereSlotIsLike(int, java.lang.String)
	 */
	@Override
	public Integer[] getActionsWhereSlotIsLike(int slotId, String match) {
		return slotMgr.getOwnersWhereSlotIsLike(SlotMgr.SLOT_OWNER_ACTION, slotId, match);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#getActionsWhereSlotEquals(int, java.lang.String)
	 */
	@Override
	public Integer[] getActionsWhereSlotEquals(int slotId, Object match) {
		return slotMgr.getOwnersWhereSlotEquals(SlotMgr.SLOT_OWNER_ACTION, slotId, match);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#getBuildStore()
	 */
	@Override
	public IBuildStore getBuildStore() {
		return buildStore;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#addListener(com.buildml.model.IActionMgrListener)
	 */
	@Override
	public void addListener(IActionMgrListener listener) {
		listeners.add(listener);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.IActionMgr#removeListener(com.buildml.model.IActionMgrListener)
	 */
	@Override
	public void removeListener(IActionMgrListener listener) {
		listeners.remove(listener);
	};
	
	/*=====================================================================================*
	 * PACKAGE METHODS
	 *=====================================================================================*/
	
	/**
	 * Extra initialization that can only happen all other managers are initialized.
	 */
	/* package */ void initPass2() {
		/* empty for now */
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

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Notify any registered listeners about our change in state.
	 * @param actionId  The action that has changed.
	 * @param how       The way in which the action changed (see {@link IActionMgrListener}).
	 * @param changeId  Which thing has changed (CHANGED_SLOT)
	 */
	private void notifyListeners(int actionId, int how, int changeId) {
		
		/* 
		 * Make a copy of the listeners list, otherwise a registered listener can't remove
		 * itself from the list within the actionChangeNotification() method.
		 */
		IActionMgrListener listenerCopy[] = 
				listeners.toArray(new IActionMgrListener[listeners.size()]);
		for (int i = 0; i < listenerCopy.length; i++) {
			listenerCopy[i].actionChangeNotification(actionId, how, changeId);			
		}
	}
	
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * If we're modifying an action's input or output slot, we could potentially be creating
	 * a cycle in the dependency graph. Before allowing this addition, check whether it
	 * would create a cycle. This is a recursive algorithm that searches to the left/right in search
	 * of the file group to be added. For example, if we're insert a file group into an action's
	 * output slot, search left (through the inputs) in search of that file group.
	 * 
	 * @param memberType  What are we currently looking at? (IPackageMemberMgr.TYPE_ACTION, etc).
	 * @param memberId	  The ID of the member we're currently looking at (initially the action
	 * 					  containing the slot to be changed).
	 * @param fileGroupId The ID of the file group that's being inserted into the slot.
	 * @param direction   The direction to search (IPackageMemberMgr.NEIGHBOUR_LEFT or
	 *                    IPackageMemberMgr.NEIGHBOUR_RIGHT).
	 * @return True if a cycle would be created, else false.
	 */
	private boolean checkForCycles(int memberType, int memberId, int fileGroupId, int direction) {

		pkgMemberMgr = buildStore.getPackageMemberMgr();

		/* get neighbours of this member */
		MemberDesc[] neighbours = pkgMemberMgr.getNeighboursOf(memberType, memberId, direction, false);
		for (int i = 0; i < neighbours.length; i++) {
			MemberDesc neighbour = neighbours[i];
			
			/* if we've hit the file group we're searching for - end the search */
			if ((neighbour.memberType == IPackageMemberMgr.TYPE_FILE_GROUP) &&
				(neighbour.memberId == fileGroupId)) {
				return true;
			}
			
			/* not found, recursively search our neighbours */
			if (checkForCycles(neighbour.memberType, neighbour.memberId, fileGroupId, direction)) {
				return true;
			}
			
			/* now loop to the next neighbour */
		}
		
		return false;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
