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
import java.sql.SQLException;

import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IFileAttributeMgr;
import com.buildml.model.types.FileSet;
import com.buildml.utils.errors.ErrorCode;

/**
 * A manager class (that supports the BuildStore class) that manages all BuildStore
 * information on which attributes are attached to the build system's files. 
 * Each attribute must first be added to the system (by name), which provides a
 * unique ID number for the attribute. Attributes (and their int/string values)
 * can then be associated with paths.
 * <p>
 * There should be exactly one FileAttributes object per BuildStore object. Use the
 * BuildStore's getFileAttributes() method to obtain that one instance.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
/* package private */ class FileAttributeMgr implements IFileAttributeMgr {

	/**
	 * Our database manager object, used to access the database content. This is provided 
	 * to us when the FileAttributes object is first instantiated.
	 */
	private BuildStoreDB db;
	
	/**
	 * The FileNameSpaces object that these file attributes are associated with.
	 */
	private FileNameSpaces fileNameSpaces;
	
	/**
	 * Various prepared statements for database access.
	 */
	private PreparedStatement 
		selectIdFromNamePrepStmt = null,
		selectNameFromIdPrepStmt = null,
		insertFileAttrsNamePrepStmt = null,
		selectOrderedNamePrepStmt = null,
		deleteFileAttrsNamePrepStmt = null,
		countFileAttrUsagePrepStmt = null,
		insertFileAttrsPrepStmt = null,
		selectValueFromFileAttrsPrepStmt = null,
		updateFileAttrsPrepStmt = null,
		deleteFileAttrsPrepStmt = null,
		deleteAllFileAttrsPrepStmt = null,
		findAttrsOnPathPrepStmt = null,
		findPathsWithAttrPrepStmt = null, 
		findPathsWithAttrValuePrepStmt = null;
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create a new FileAttributes object.
	 * 
	 * @param buildStore The BuildStore object that owns this FileAttributes object.
	 * @param fns The FileNameSpaces object that these attributes are attached to
	 */
	public FileAttributeMgr(BuildStore buildStore, FileNameSpaces fns) {
		this.db = buildStore.getBuildStoreDB();
		this.fileNameSpaces = fns;
		
		/* Prepare our database statements */
		selectIdFromNamePrepStmt = db.prepareStatement("select id from fileAttrsName where name = ?");
		selectNameFromIdPrepStmt = db.prepareStatement("select name from fileAttrsName where id = ?");
		selectOrderedNamePrepStmt = db.prepareStatement("select name from fileAttrsName order by name");
		insertFileAttrsNamePrepStmt = db.prepareStatement("insert into fileAttrsName values (null, ?)");
		deleteFileAttrsNamePrepStmt = db.prepareStatement("delete from fileAttrsName where name = ?");
		countFileAttrUsagePrepStmt = db.prepareStatement("select count(*) from fileAttrs where attrId = ?");
		insertFileAttrsPrepStmt = db.prepareStatement("insert into fileAttrs values (?, ?, ?)");
		selectValueFromFileAttrsPrepStmt = db.prepareStatement("select value from fileAttrs where " +
				"pathId = ? and attrId = ?");
		updateFileAttrsPrepStmt = db.prepareStatement("update fileAttrs set value = ? "
				+ "where pathId = ? and attrId = ?");
		deleteFileAttrsPrepStmt = db.prepareStatement("delete from fileAttrs where " +
				"pathId = ? and attrId = ?");
		deleteAllFileAttrsPrepStmt = db.prepareStatement("delete from fileAttrs where pathId = ?");
		findAttrsOnPathPrepStmt = db.prepareStatement("select attrId from fileAttrs where pathId = ?");
		findPathsWithAttrPrepStmt = db.prepareStatement("select pathId from fileAttrs where attrId = ?");
		findPathsWithAttrValuePrepStmt = db.prepareStatement(
				"select pathId from fileAttrs where attrId = ? and value = ?");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IFileAttributeMgr#newAttrName(java.lang.String)
	 */
	@Override
	public int newAttrName(String attrName) {
		
		/* if this name is already used, return an error */
		int existingId = getAttrIdFromName(attrName);
		if (existingId != ErrorCode.NOT_FOUND) {
			return ErrorCode.ALREADY_USED;
		}
		
		/* else, add the name to the fileAttrsName table, and retrieve its unique ID */
		try {
			insertFileAttrsNamePrepStmt.setString(1, attrName);
			db.executePrepUpdate(insertFileAttrsNamePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return db.getLastRowID();
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IFileAttributeMgr#getAttrIdFromName(java.lang.String)
	 */
	@Override
	public int getAttrIdFromName(String attrName) {
		
		Integer results[];
		try {
			selectIdFromNamePrepStmt.setString(1, attrName);
			results = db.executePrepSelectIntegerColumn(selectIdFromNamePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}

		/* if there's no entry at all, return NOT_FOUND */
		if (results.length == 0) {
			return ErrorCode.NOT_FOUND;
		}
		
		/* if there's one entry, that's good - just return that number. */
		if (results.length == 1) {
			return results[0];
		}
		
		/* else, problem - too many records */
		throw new FatalBuildStoreError("Too many records in fileAttrsName table for " + attrName);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IFileAttributeMgr#getAttrNameFromId(int)
	 */
	@Override
	public String getAttrNameFromId(int attrId) {
		
		String results[];
		try {
			selectNameFromIdPrepStmt.setInt(1, attrId);
			results = db.executePrepSelectStringColumn(selectNameFromIdPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}

		/* if there's no entry at all, return NOT_FOUND */
		if (results.length == 0) {
			return null;
		}
		
		/* if there's one entry, that's good - just return that number. */
		if (results.length == 1) {
			return results[0];
		}
		
		/* else, problem - too many records */
		throw new FatalBuildStoreError("Too many records in fileAttrsName table for " + attrId);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IFileAttributeMgr#getAttrNames()
	 */
	@Override
	public String[] getAttrNames() {
		
		return db.executePrepSelectStringColumn(selectOrderedNamePrepStmt);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IFileAttributeMgr#deleteAttrName(java.lang.String)
	 */
	@Override
	public int deleteAttrName(String attrName) {
		
		/* attribute names can't be deleted if they're in use - check this first */
		int attrId = getAttrIdFromName(attrName);
		try {
			countFileAttrUsagePrepStmt.setInt(1, attrId);

			/* 
			 * A select count(*) should always return 1 result. This count will be non-0
			 * if the attribute name is "in use" in the fileAttrs table.
			 */
			Integer results[] = db.executePrepSelectIntegerColumn(countFileAttrUsagePrepStmt);
			if (results[0] != 0) {
				return ErrorCode.CANT_REMOVE;
			}
		} catch (SQLException ex) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", ex);
		}
		
		/* 
		 * Try to delete the attribute name record, but also take note of whether anything was deleted.
		 * If nothing was deleted, the name is considered "not found"
		 */
		try {
			deleteFileAttrsNamePrepStmt.setString(1, attrName);
			int rowCount = db.executePrepUpdate(deleteFileAttrsNamePrepStmt);
			if (rowCount == 0) {
				return ErrorCode.NOT_FOUND;
			}
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return ErrorCode.OK;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IFileAttributeMgr#setAttr(int, int, java.lang.String)
	 */
	@Override
	public void setAttr(int pathId, int attrId, String attrValue) {

		/* if attrValue is null, that's equivalent to deleting the record */
		if (attrValue == null) {
			deleteAttr(pathId, attrId);
			return;
		}
		
		/* 
		 * Try to update the record, but take note of whether it really was updated.
		 * If not, we'll need to insert a new record.
		 */
		try {
			updateFileAttrsPrepStmt.setString(1, attrValue);
			updateFileAttrsPrepStmt.setInt(2, pathId);
			updateFileAttrsPrepStmt.setInt(3, attrId);
			int rowCount = db.executePrepUpdate(updateFileAttrsPrepStmt);
			
			/* did the record exist? If not, insert it */
			if (rowCount == 0) {
				insertFileAttrsPrepStmt.setInt(1, pathId);
				insertFileAttrsPrepStmt.setInt(2, attrId);
				insertFileAttrsPrepStmt.setString(3, attrValue);
				db.executePrepUpdate(insertFileAttrsPrepStmt);
			}
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IFileAttributeMgr#setAttr(int, int, int)
	 */
	@Override
	public int setAttr(int pathId, int attrId, int attrValue) {
		
		if (attrValue < 0) {
			return ErrorCode.BAD_VALUE;
		}
		setAttr(pathId, attrId, Integer.toString(attrValue));
		return ErrorCode.OK;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IFileAttributeMgr#getAttrAsString(int, int)
	 */
	@Override
	public String getAttrAsString(int pathId, int attrId) {
		String results[];
		try {
			selectValueFromFileAttrsPrepStmt.setInt(1, pathId);
			selectValueFromFileAttrsPrepStmt.setInt(2, attrId);
			results = db.executePrepSelectStringColumn(selectValueFromFileAttrsPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}

		/* if there was no result, return null */
		if (results.length == 0) {
			return null;
		}
		
		/* ok, there's exactly one result */
		else if (results.length == 1) {
			return results[0];
		}
		
		/* else, problem - too many records */
		throw new FatalBuildStoreError("Too many records in fileAttrs table for " + 
				pathId + " / " + attrId);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IFileAttributeMgr#getAttrAsInteger(int, int)
	 */
	@Override
	public int getAttrAsInteger(int pathId, int attrId) {
		
		/* fetch the attribute's value as a String */
		String result = getAttrAsString(pathId, attrId);
		
		/* if it's not set, return NOT_FOUND */
		if (result == null) {
			return ErrorCode.NOT_FOUND;
		}
		
		/* if the string isn't in the format of an integer, return BAD_VALUE */
		int iValue;
		try {
			iValue = Integer.valueOf(result);
		} catch (NumberFormatException ex) {
			return ErrorCode.BAD_VALUE;
		}
		
		/* we can only have positive integers */
		if (iValue < 0) {
			return ErrorCode.BAD_VALUE;
		}
		
		/* return the attribute's value */
		return iValue;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IFileAttributeMgr#deleteAttr(int, int)
	 */
	@Override
	public void deleteAttr(int pathId, int attrId) {
		
		/* delete the record, whether it exists in the database or not */
		try {
			deleteFileAttrsPrepStmt.setInt(1, pathId);
			deleteFileAttrsPrepStmt.setInt(2, attrId);
			db.executePrepUpdate(deleteFileAttrsPrepStmt);
		
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
	}

	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IFileAttributeMgr#deleteAllAttrOnPath(int)
	 */
	@Override
	public void deleteAllAttrOnPath(int pathId) {
		
		/* delete all records for this pathId (there are possibly none) */
		try {
			deleteAllFileAttrsPrepStmt.setInt(1, pathId);
			db.executePrepUpdate(deleteAllFileAttrsPrepStmt);
		
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IFileAttributeMgr#getAttrsOnPath(int)
	 */
	@Override
	public Integer[] getAttrsOnPath(int pathId) {

		Integer results[];
		try {
			findAttrsOnPathPrepStmt.setInt(1, pathId);
			results = db.executePrepSelectIntegerColumn(findAttrsOnPathPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return results;
	}
		
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IFileAttributeMgr#getPathsWithAttr(int)
	 */
	@Override
	public FileSet getPathsWithAttr(int attrId) {

		Integer results[] = null;
		try {
			findPathsWithAttrPrepStmt.setInt(1, attrId);
			results = db.executePrepSelectIntegerColumn(findPathsWithAttrPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
				
		return new FileSet(fileNameSpaces, results);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IFileAttributeMgr#getPathsWithAttr(int, java.lang.String)
	 */
	@Override
	public FileSet getPathsWithAttr(int attrId, String value) {

		Integer results[] = null;
		try {
			findPathsWithAttrValuePrepStmt.setInt(1, attrId);
			findPathsWithAttrValuePrepStmt.setString(2, value);
			results = db.executePrepSelectIntegerColumn(findPathsWithAttrValuePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return new FileSet(fileNameSpaces, results);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.model.impl.IFileAttributeMgr#getPathsWithAttr(int, int)
	 */
	@Override
	public FileSet getPathsWithAttr(int attrId, int value) {
		return getPathsWithAttr(attrId, String.valueOf(value));
	}
	
	/*-------------------------------------------------------------------------------------*/

}
