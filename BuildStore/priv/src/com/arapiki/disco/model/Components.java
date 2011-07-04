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
		removeComponentByNamePrepStmt = null;
	
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
		if (getComponentId(componentName) == ErrorCode.NOT_FOUND){
			return ErrorCode.NOT_FOUND;
		}
		
		/* we can't remove the "None" component */
		if (componentName.equals("None")) {
			return ErrorCode.CANT_REMOVE;
		}
		
		// TODO: return ErrorCode.CANT_REMOVE if this component is used anywhere.
		
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
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * Validate a component's name. Valid characters are digits, letters (upper or lower case),
	 * '-' and '_'. No other characters are permitted. Component names must contain at least
	 * three characters.
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
			if (!(Character.isLetterOrDigit(ch) ||
					(ch == '_') || (ch == '-'))){
				return false;
			}
			i++;
		}
		
		return true;
	}

	/*-------------------------------------------------------------------------------------*/
}