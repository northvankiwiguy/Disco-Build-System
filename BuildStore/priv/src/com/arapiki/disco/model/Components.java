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

import com.arapiki.utils.errors.ErrorCode;

/**
 * Class for recording the "component" names in the BuildStore database. A component's name
 * is simply a text identifier that maps to underlying numeric ID. These IDs are then 
 * associated with files and tasks. Each component is associated with a number of pre-defined
 * sections, such as "private" and "public". That is, if a file or task is associated with
 * component "foo", the file/task will either belong to the foo/private section, or the
 * foo/public section.
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class Components {

	/*=====================================================================================*
	 * FIELDS
	 *=====================================================================================*/
	
	/**
	 * The BuildStore object that "owns" this Components object.
	 */
	private BuildStore buildStore;
	
	/**
	 * Our database manager object, used to access the database content. This is provided 
	 * to us when the Components object is first instantiated.
	 */
	private BuildStoreDB db = null;
	
	/**
	 * The names of the sections within a component. These are statically defined and
	 * can't be modified by the user.
	 */
	private static String sectionNames[] = new String[] {"private", "public"};
	
	/**
	 * Various prepared statement for database access.
	 */
	private PreparedStatement 
		addComponentPrepStmt = null,
		findComponentByNamePrepStmt = null,
		findComponentByIdPrepStmt = null,
		findAllComponentsPrepStmt = null,
		removeComponentByNamePrepStmt = null,
		updateFileComponentPrepStmt = null,
		findFileComponentPrepStmt = null,
		findFilesInComponent1PrepStmt = null,
		findFilesInComponent2PrepStmt = null,
		updateTaskComponentPrepStmt = null,
		findTaskComponentPrepStmt = null,
		findTasksInComponentPrepStmt = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new Components object, which represents the file/task components that
	 * are part of the BuildStore.
	 * @param bs The BuildStore that this Component object belongs to.
	 */
	public Components(BuildStore bs) {
		this.buildStore = bs;
		this.db = buildStore.getBuildStoreDB();
		
		/* initialize prepared database statements */
		addComponentPrepStmt = db.prepareStatement("insert into components values (null, ?)");
		findComponentByNamePrepStmt = db.prepareStatement("select id from components where name = ?");
		findComponentByIdPrepStmt = db.prepareStatement("select name from components where id = ?");
		findAllComponentsPrepStmt = db.prepareStatement(
				"select name from components order by name collate nocase");
		removeComponentByNamePrepStmt = db.prepareStatement("delete from components where name = ?");
		updateFileComponentPrepStmt = db.prepareStatement("update files set compId = ?, compSectionId = ? " +
				"where id = ?");
		findFileComponentPrepStmt = db.prepareStatement("select compId, compSectionId from files " +
				"where id = ?");
		findFilesInComponent1PrepStmt = db.prepareStatement("select id from files where compId = ?");
		findFilesInComponent2PrepStmt = db.prepareStatement("select id from files where " +
				"compId = ? and compSectionId = ?");
		updateTaskComponentPrepStmt = db.prepareStatement("update buildTasks set compId = ? " +
				"where taskId = ?");
		findTaskComponentPrepStmt = db.prepareStatement("select compId from buildTasks where taskId = ?");
		findTasksInComponentPrepStmt = db.prepareStatement("select taskId from buildTasks where compId = ?");
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Add a new component to the BuildStore.
	 * @param componentName The name of the new component to be added.
	 * @return The component's ID if the addition was successful, ErrorCode.INVALID_NAME
	 * if the component's name isn't valid, or ErrorCode.ALREADY_USED if the component 
	 * name is already in the BuildStore.
	 */
	public int addComponent(String componentName) {
		
		/* validate the new component's name */
		if (!isValidName(componentName)){
			return ErrorCode.INVALID_NAME;
		}
		
		/* check that the component doesn't already exist in the database */
		if (getComponentId(componentName) != ErrorCode.NOT_FOUND){
			return ErrorCode.ALREADY_USED;
		}
		
		/* insert the component into our table */
		try {
			addComponentPrepStmt.setString(1, componentName);
			db.executePrepUpdate(addComponentPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* return the new component's ID number */
		return db.getLastRowID();
	};

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a component's ID number, return the component's name.
	 * @param componentId The component's ID number
	 * @return The component's name, or null if the component ID is invalid.
	 */
	public String getComponentName(int componentId) {
		
		/* find the component in our table */
		String results[] = null;
		try {
			findComponentByIdPrepStmt.setInt(1, componentId);
			results = db.executePrepSelectStringColumn(findComponentByIdPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* no result == no component with this Id */
		if (results.length == 0) {
			return null;
		} 
		
		/* one result == we have the correct name */
		else if (results.length == 1) {
			return results[0];
		}
		
		/* multiple results is an error */
		else {
			throw new FatalBuildStoreError("Multiple entries found in components table, for ID " + 
					componentId);
		}	
	};

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a component's name, return it's ID number.
	 * @param componentName The component's name
	 * @return The component's ID number, ErrorCode.NOT_FOUND if there's no component
	 * with this name.
	 */
	public int getComponentId(String componentName) {
		
		/* find the component into our table */
		Integer results[] = null;
		try {
			findComponentByNamePrepStmt.setString(1, componentName);
			results = db.executePrepSelectIntegerColumn(findComponentByNamePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* no result == no component by this name */
		if (results.length == 0) {
			return ErrorCode.NOT_FOUND;
		} 
		
		/* one result == we have the correct ID */
		else if (results.length == 1) {
			return results[0];
		}
		
		/* multiple results is an error */
		else {
			throw new FatalBuildStoreError("Multiple entries found in components table, for name " + 
					componentName);
		}		
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Remove the specified component from the BuildStore. The component can only be removed
	 * if there are no files or tasks associated with the component.
	 * @param componentName The name of the component to be removed.
	 * @return ErrorCode.OK if the component was successfully removed, ErrorCode.CANT_REMOVE
	 * if the component is still in use, and ErrorCode.NOT_FOUND if there's no component
	 * with this name.
	 */
	public int removeComponent(String componentName) {
		
		/* check that the component already exists */
		int compId = getComponentId(componentName);
		if (compId == ErrorCode.NOT_FOUND){
			return ErrorCode.NOT_FOUND;
		}
		
		/* we can't remove the "None" component */
		if (componentName.equals("None")) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* determine if this component is used by any files */
		Integer filesInComponent[] = getFilesInComponent(compId);
		if (filesInComponent.length != 0) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* determine if this component is used by any tasks */
		Integer tasksInComponent[] = getTasksInComponent(compId);
		if (tasksInComponent.length != 0) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* remove from the database */
		try {
			removeComponentByNamePrepStmt.setString(1, componentName);
			db.executePrepUpdate(removeComponentByNamePrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return ErrorCode.OK;
	};
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Return an alphabetically sorted list of all the components. The case (upper versus
	 * lower) is ignored when sorting the results.
	 * @return A non-empty array of component names (will always contain the "None" component).
	 */
	public String[] getComponents() {
		
		/* find all the component into our table */
		return db.executePrepSelectStringColumn(findAllComponentsPrepStmt);
	};	
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a section's ID number, return the corresponding section name.
	 * @param id The section's ID number
	 * @return The section's name, or null if the ID number is invalid.
	 */
	public String getSectionName(int id) {
		
		/* the names are a static mapping, so no need for database look-ups */
		switch (id) {
		case 0:
			return "private";
		case 1:
			return "public";
		default:
			return null;
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a section's name, return it's ID number. There can be many names for the same
	 * section, so there isn't a 1:1 mapping of names to IDs. For example, both "private" and
	 * "priv" will return the same ID number.
	 * @param name The section's name
	 * @return The section's ID number, or ErrorCode.NOT_FOUND if the section name isn't valid.
	 */
	public int getSectionId(String name) {
		
		/* the mapping is static, so no need for a database look up */
		if (name.equals("priv") || name.equals("private")) {
			return 0;
		}
		if (name.equals("pub") || name.equals("public")) {
			return 1;
		}
		return ErrorCode.NOT_FOUND;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return all the section names. If there are multiple names for the same section, only
	 * one of them is returned.
	 * @return A String array of the section names, in alphabetical order.
	 */
	public String[] getSections() {
		return sectionNames;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set the component/section associated with this file.
	 * @param fileId The ID of the file whose component will be set
	 * @param compId The ID of the component to be associated with this file
	 * @param compSectionId The ID of the component's section.
	 * @return ErrorCode.OK on success, or ErrorCode.NOT_FOUND if this file doesn't exist
	 */
	public int setFileComponent(int fileId, int compId, int compSectionId) {
		
		try {
			updateFileComponentPrepStmt.setInt(1, compId);
			updateFileComponentPrepStmt.setInt(2, compSectionId);
			updateFileComponentPrepStmt.setInt(3, fileId);
			int rowCount = db.executePrepUpdate(updateFileComponentPrepStmt);
			if (rowCount == 0) {
				return ErrorCode.NOT_FOUND;
			}
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return ErrorCode.OK;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Get the component/section associated with this file.
	 * @param fileId The ID of the file whose component we're interested in
	 * @return A Integer[2] array where [0] is the component ID, and [1] is the section ID,
	 * or null if the file doesn't exist.
	 */
	public Integer[] getFileComponent(int fileId) {
		
		Integer result[] = new Integer[2];
		
		try {
			findFileComponentPrepStmt.setInt(1, fileId);
			ResultSet rs = db.executePrepSelectResultSet(findFileComponentPrepStmt);
			if (rs.next()){
				result[0] = rs.getInt(1);
				result[1] = rs.getInt(2);
				rs.close();
			} else {
				/* error - there was no record, so the fileId must be invalid */
				return null;
			}
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("SQL error", e);
		}
				
		return result;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the list of files that are within the specified component.
	 * @param compId The component we're examining
	 * @return An array of files that reside inside that component.
	 */
	public Integer[] getFilesInComponent(int compId) {
		Integer results[] = null;
		try {
			findFilesInComponent1PrepStmt.setInt(1, compId);
			results = db.executePrepSelectIntegerColumn(findFilesInComponent1PrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		return results;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the list of files that are within the specified component/section.
	 * @param compId The component we're examining
	 * @param compSectionId The section within the component we're interested in
	 * @return An array of files that reside inside that component/section.
	 */
	public Integer[] getFilesInComponent(int compId, int compSectionId) {
		Integer results[] = null;
		try {
			findFilesInComponent2PrepStmt.setInt(1, compId);
			findFilesInComponent2PrepStmt.setInt(2, compSectionId);
			results = db.executePrepSelectIntegerColumn(findFilesInComponent2PrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		return results;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set the component/section associated with this task.
	 * @param taskId The ID of the task whose component will be set
	 * @param compId The ID of the component to be associated with this task
	 * @return ErrorCode.OK on success, or ErrorCode.NOT_FOUND if this task doesn't exist
	 */
	public int setTaskComponent(int taskId, int compId) {
		
		try {
			updateTaskComponentPrepStmt.setInt(1, compId);
			updateTaskComponentPrepStmt.setInt(2, taskId);
			int rowCount = db.executePrepUpdate(updateTaskComponentPrepStmt);
			if (rowCount == 0) {
				return ErrorCode.NOT_FOUND;
			}
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		return ErrorCode.OK;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Get the component/section associated with this task.
	 * @param taskId The ID of the task whose component we're interested in
	 * @return The task's component, or ErrorCode.NOT_FOUND if the task doesn't exist
	 */
	public int getTaskComponent(int taskId) {
		
		Integer results[];
		try {
			findTaskComponentPrepStmt.setInt(1, taskId);
			results = db.executePrepSelectIntegerColumn(findTaskComponentPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* no result == no component by this name */
		if (results.length == 0) {
			return ErrorCode.NOT_FOUND;
		} 
		
		/* 
		 * One result == we have the correct ID (note: it's not possible to have
		 * multiple results, since taskId is a unique key
		 */
		return results[0];
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the list of tasks that are within the specified component.
	 * @param compId The component we're examining
	 * @return An array of tasks that reside inside that component.
	 */
	public Integer[] getTasksInComponent(int compId) {
		Integer results[] = null;
		try {
			findTasksInComponentPrepStmt.setInt(1, compId);
			results = db.executePrepSelectIntegerColumn(findTasksInComponentPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		return results;
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * Validate a component's name. Valid characters are digits, letters (upper or lower case),
	 * '-' and '_'. No other characters are permitted. Component names must contain at least
	 * three characters and start with a letter.
	 * @param componentName The component name to be validated
	 * @return True if the name is valid, else false
	 */
	private boolean isValidName(String componentName) {
		
		if (componentName == null) {
			return false;
		}
		int length = componentName.length();
		if (length < 3) {
			return false;
		}
		
		int i = 0;
		while (i != length) {
			char ch = componentName.charAt(i);
			
			/* first character must be a letter */
			if (i == 0) {
				if (!(Character.isLetter(ch))) {
					return false;
				}	
			} 
			
			/* following characters are letter, digit, _ or - */
			else {
				if (!(Character.isLetterOrDigit(ch) ||
						(ch == '_') || (ch == '-'))){
					return false;
				}
			}
			i++;
		}
		
		return true;
	}

	/*-------------------------------------------------------------------------------------*/
}