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

import com.arapiki.disco.model.types.FileSet;
import com.arapiki.disco.model.types.TaskSet;
import com.arapiki.utils.errors.ErrorCode;

/**
 * A manager class (that supports the BuildStore class) that manages all BuildStore
 * information relating to component definitions. That is, it keeps information on
 * which files and tasks belong in each component.
 * <p>
 * A component's name is simply a text identifier that maps to underlying numeric ID.
 * These IDs are then associated with files and tasks as a means of grouping them
 * together into logical units.
 * <p>
 * In the case of files, a component is also associated with a scope within that
 * component, such as "private" and "public". That is, if a file is associated
 * with component "foo", the file will either belong to the foo/private scope, or the
 * foo/public scope.
 * <p>
 * Note: tasks can only belong to the component as a whole, rather than belonging to
 * an individual scope within that component. 
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class Components {

	/*=====================================================================================*
	 * FIELDS
	 *=====================================================================================*/
	
	/**
	 * Numeric constants for each of the scopes.
	 */
	public static final int SCOPE_NONE 		= 0;
	public static final int SCOPE_PRIVATE 	= 1;
	public static final int SCOPE_PUBLIC 	= 2;
	public static final int SCOPE_MAX		= 2;
	
	/** The BuildStore object that "owns" this Components object. */
	private BuildStore buildStore;
	
	/**
	 * Our database manager object, used to access the database content. This is provided 
	 * to us when the Components object is first instantiated.
	 */
	private BuildStoreDB db = null;
	
	/** The FileNameSpaces object that manages the files in our components. */
	private FileNameSpaces fns = null;
	
	/** The BuildTasks object that manages the tasks in our components. */
	private BuildTasks bts = null;
	
	/**
	 * The names of the scopes within a component. These are statically defined and
	 * can't be modified by the user.
	 */
	private static String scopeNames[] = new String[] {"None", "Private", "Public"};
	
	/**
	 * Various prepared statements for database access.
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
		findFilesOutsideComponent1PrepStmt = null,
		findFilesOutsideComponent2PrepStmt = null,		
		updateTaskComponentPrepStmt = null,
		findTaskComponentPrepStmt = null,
		findTasksInComponentPrepStmt = null,
		findTasksOutsideComponentPrepStmt = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new Components object, which represents the file/task components that
	 * are part of the BuildStore.
	 * 
	 * @param buildStore The BuildStore that this Component object belongs to.
	 */
	public Components(BuildStore buildStore) {
		this.buildStore = buildStore;
		this.db = buildStore.getBuildStoreDB();
		this.fns = buildStore.getFileNameSpaces();
		this.bts = buildStore.getBuildTasks();
		
		/* initialize prepared database statements */
		addComponentPrepStmt = db.prepareStatement("insert into components values (null, ?)");
		findComponentByNamePrepStmt = db.prepareStatement("select id from components where name = ?");
		findComponentByIdPrepStmt = db.prepareStatement("select name from components where id = ?");
		findAllComponentsPrepStmt = db.prepareStatement(
				"select name from components order by name collate nocase");
		removeComponentByNamePrepStmt = db.prepareStatement("delete from components where name = ?");
		updateFileComponentPrepStmt = db.prepareStatement("update files set compId = ?, compScopeId = ? " +
				"where id = ?");
		findFileComponentPrepStmt = db.prepareStatement("select compId, compScopeId from files " +
				"where id = ?");
		findFilesInComponent1PrepStmt = db.prepareStatement("select id from files where compId = ?");
		findFilesInComponent2PrepStmt = db.prepareStatement("select id from files where " +
				"compId = ? and compScopeId = ?");
		findFilesOutsideComponent1PrepStmt = db.prepareStatement("select id from files where compId != ?");
		findFilesOutsideComponent2PrepStmt = db.prepareStatement("select id from files where " +
				"not (compId = ? and compScopeId = ?)");
		updateTaskComponentPrepStmt = db.prepareStatement("update buildTasks set compId = ? " +
				"where taskId = ?");
		findTaskComponentPrepStmt = db.prepareStatement("select compId from buildTasks where taskId = ?");
		findTasksInComponentPrepStmt = db.prepareStatement("select taskId from buildTasks where compId = ?");
		findTasksOutsideComponentPrepStmt = db.prepareStatement("select taskId from buildTasks " +
				"where compId != ? and taskId != 0");
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Add a new component to the BuildStore.
	 * 
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
	 * 
	 * @param componentId The component's ID number.
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
	 * Given a component's name, return its ID number.
	 * 
	 * @param componentName The component's name.
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
	 * 
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
		FileSet filesInComponent = getFilesInComponent(compId);
		if (filesInComponent.size() != 0) {
			return ErrorCode.CANT_REMOVE;
		}
		
		/* determine if this component is used by any tasks */
		TaskSet tasksInComponent = getTasksInComponent(compId);
		if (tasksInComponent.size() != 0) {
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
	 * Return an alphabetically sorted array of all the components. The case (upper versus
	 * lower) is ignored when sorting the results.
	 * 
	 * @return A non-empty array of component names (will always contain the "None" component).
	 */
	public String[] getComponents() {
		
		/* find all the component into our table */
		return db.executePrepSelectStringColumn(findAllComponentsPrepStmt);
	};	
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a scope's ID number, return the corresponding scope name.
	 * 
	 * @param id The scope's ID number.
	 * @return The scope's name, or null if the ID number is invalid.
	 */
	public String getScopeName(int id) {
		
		/* the names are a static mapping, so no need for database look-ups */
		switch (id) {
		case SCOPE_NONE:
			return "None";
		case SCOPE_PRIVATE:
			return "Private";
		case SCOPE_PUBLIC:
			return "Public";
		default:
			return null;
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a scope's name, return its ID number. There can be many names for the same
	 * scope, so there isn't a 1:1 mapping of names to IDs. For example, both "private" and
	 * "priv" will return the same ID number.
	 * 
	 * @param name The scope's name.
	 * @return The scope's ID number, or ErrorCode.NOT_FOUND if the scope name isn't valid.
	 */
	public int getScopeId(String name) {
		
		/* the mapping is static, so no need for a database look up */
		if (name.equalsIgnoreCase("None")) {
			return SCOPE_NONE;
		}
		if (name.equalsIgnoreCase("priv") || name.equalsIgnoreCase("private")) {
			return SCOPE_PRIVATE;
		}
		if (name.equalsIgnoreCase("pub") || name.equalsIgnoreCase("public")) {
			return SCOPE_PUBLIC;
		}
		return ErrorCode.NOT_FOUND;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Parse a component specification string, and return the ID of the component and
	 * (optionally) the ID of the scope within that component. The syntax of the component
	 * spec must be of the form:
	 *  <ol>
	 * 	  <li>&lt;comp-name&gt;</li>
	 * 	  <li>&lt;comp-name&gt;/&lt;scope-name&gt;</li>
	 *  </ol>
	 * That is, the scope name is optional.
	 * 
	 * @param compSpec The component specification string.
	 * @return An Integer[2] array, where [0] is the component's ID and [1] is the scope
	 * ID. If either portion of the compSpec was invalid (not a registered component or scope),
	 * the ID for that portion will be ErrorCode.NOT_FOUND. If there was no scope name
	 * specified, the scope ID will be 0, which represents the "None" scope.
	 */
	public Integer[] parseCompSpec(String compSpec) {

		/* parse the compSpec to separate it into "comp" and "scope" portions */
		String compName = compSpec;
		String scopeName = null;

		/* check if there's a '/' in the string, to separate "component" from "scope" */
		int slashIndex = compSpec.indexOf('/');
		if (slashIndex != -1) {
			compName = compSpec.substring(0, slashIndex);
			scopeName = compSpec.substring(slashIndex + 1);
		} 

		/* 
		 * Convert the component's name into it's internal ID. If there's an error,
		 * we simply pass it back to our own caller.
		 */
		int compId = getComponentId(compName);

		/* if the user provided a /scope portion, convert that to an ID too */
		int scopeId = 0;
		if (scopeName != null) {
			scopeId = getScopeId(scopeName);
		}
		
		return new Integer[] {compId, scopeId};
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set the component/scope associated with this path.
	 * 
	 * @param fileId The ID of the file whose component will be set.
	 * @param compId The ID of the component to be associated with this file.
	 * @param compScopeId The ID of the component's scope.
	 * @return ErrorCode.OK on success, or ErrorCode.NOT_FOUND if this file doesn't exist
	 */
	public int setFileComponent(int fileId, int compId, int compScopeId) {
		
		try {
			updateFileComponentPrepStmt.setInt(1, compId);
			updateFileComponentPrepStmt.setInt(2, compScopeId);
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
	 * Get the component/scope associated with this path.
	 * 
	 * @param fileId The ID of the path whose component we're interested in
	 * @return A Integer[2] array where [0] is the component ID, and [1] is the scope ID,
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
	 * Return the set of files that are within the specified component (any scope).
	 * 
	 * @param compId The ID of the component we're examining.
	 * @return The set of files that reside inside that component.
	 */
	public FileSet getFilesInComponent(int compId) {
		Integer results[] = null;
		try {
			findFilesInComponent1PrepStmt.setInt(1, compId);
			results = db.executePrepSelectIntegerColumn(findFilesInComponent1PrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* convert to a FileSet */
		return new FileSet(fns, results);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the set of files that are within the specified component/scope.
	 * 
	 * @param compId The ID of the component we're examining.
	 * @param compScopeId The scope within the component we're interested in.
	 * @return The set of files that reside inside that component/scope.
	 */
	public FileSet getFilesInComponent(int compId, int compScopeId) {
		
		Integer results[] = null;
		try {
			findFilesInComponent2PrepStmt.setInt(1, compId);
			findFilesInComponent2PrepStmt.setInt(2, compScopeId);
			results = db.executePrepSelectIntegerColumn(findFilesInComponent2PrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* convert to a FileSet */
		return new FileSet(fns, results);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the set of files that are within the specified component/scope, given a
	 * string specification of the component/scope.
	 * 
	 * @param compSpec The component name, or the component/scope name.
	 * @return The FileSet of all files in this component or component/scope, or null if
	 * there's a an error in parsing the comp/spec name.
	 */
	public FileSet getFilesInComponent(String compSpec) {

		Integer compSpecParts[] = parseCompSpec(compSpec);
		
		int compId = compSpecParts[0];
		int scopeId = compSpecParts[1];
		
		/* the ID must not be invalid, else that's an error */
		if ((compId == ErrorCode.NOT_FOUND) || (scopeId == ErrorCode.NOT_FOUND)) {
			return null;
		}
		
		/* 
		 * If the scope ID isn't specified by the user, then scopeId == 0 (the
		 * ID of the "None" scope). This indicates we should look for all paths
		 * in the component, regardless of the scope.
		 */
		if (scopeId != 0) {
			return getFilesInComponent(compId, scopeId);
		} else {
			return getFilesInComponent(compId);			
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the set of files that are outside the specified component.
	 * 
	 * @param compId The ID of the component we're examining.
	 * @return The set of files that reside outside that component.
	 */
	public FileSet getFilesOutsideComponent(int compId) {
		Integer results[] = null;
		try {
			findFilesOutsideComponent1PrepStmt.setInt(1, compId);
			results = db.executePrepSelectIntegerColumn(findFilesOutsideComponent1PrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* convert to a FileSet */
		return new FileSet(fns, results);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the set of files that are outside the specified component/scope.
	 * 
	 * @param compId The ID of the component we're examining.
	 * @param compScopeId The ID of the scope within the component we're interested in.
	 * @return The set of files that reside outside that component/scope.
	 */
	public FileSet getFilesOutsideComponent(int compId, int compScopeId) {
		Integer results[] = null;
		try {
			findFilesOutsideComponent2PrepStmt.setInt(1, compId);
			findFilesOutsideComponent2PrepStmt.setInt(2, compScopeId);
			results = db.executePrepSelectIntegerColumn(findFilesOutsideComponent2PrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		
		/* convert to a FileSet */
		return new FileSet(fns, results);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Returns the set of files that fall outside of the component boundaries, using a string
	 * to specify the component/scope.
	 * 
	 * @param compSpec The component name, or the component/scope name.
	 * @return The FileSet of all files outside of this component or component/scope, or null if
	 * there's a an error in parsing the comp/spec name.
	 */
	public FileSet getFilesOutsideComponent(String compSpec) {
		
		Integer compSpecParts[] = parseCompSpec(compSpec);
		int compId = compSpecParts[0];
		int scopeId = compSpecParts[1];
		
		/* the ID must not be invalid, else that's an error */
		if ((compId == ErrorCode.NOT_FOUND) || (scopeId == ErrorCode.NOT_FOUND)) {
			return null;
		}
		
		/* 
		 * The scope ID is optional, since it still allows us to
		 * get the component's files. Note that scopeId == 0 implies
		 * that the user didn't specify a /scope value.
		 */
		if (scopeId != 0) {
			return getFilesOutsideComponent(compId, scopeId);
		} else {
			return getFilesOutsideComponent(compId);			
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set the component associated with this task.
	 * 
	 * @param taskId The ID of the task whose component will be set.
	 * @param compId The ID of the component to be associated with this task.
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
	 * Get the component associated with this task.
	 * 
	 * @param taskId The ID of the task whose component we're interested in.
	 * @return The task's component, or ErrorCode.NOT_FOUND if the task doesn't exist.
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
	 * Return the set of tasks that are within the specified component.
	 * 
	 * @param compId The component we're examining.
	 * @return The set of tasks that reside inside that component.
	 */
	public TaskSet getTasksInComponent(int compId) {
		Integer results[] = null;
		try {
			findTasksInComponentPrepStmt.setInt(1, compId);
			results = db.executePrepSelectIntegerColumn(findTasksInComponentPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		return new TaskSet(bts, results);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the set of tasks that are within the specified component.
	 * 
	 * @param compSpec The name of the component to query.
	 * @return The set of tasks that reside inside that component, null if the
	 * component name is invalid.
	 */
	public TaskSet getTasksInComponent(String compSpec) {
		
		/* translate the component's name to its ID */
		int compId = getComponentId(compSpec);
		if (compId == ErrorCode.NOT_FOUND){
			return null;
		}
		
		return getTasksInComponent(compId);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the set of tasks that are outside the specified component.
	 * 
	 * @param compId The component we're examining.
	 * @return The set of tasks that reside outside that component.
	 */
	public TaskSet getTasksOutsideComponent(int compId) {
		Integer results[] = null;
		try {
			findTasksOutsideComponentPrepStmt.setInt(1, compId);
			results = db.executePrepSelectIntegerColumn(findTasksOutsideComponentPrepStmt);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to execute SQL statement", e);
		}
		return new TaskSet(bts, results);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the set of tasks that are outside the specified component.
	 * 
	 * @param compSpec The name of the component to query.
	 * @return An array of tasks that reside outside that component, null if the
	 * component name is invalid.
	 */
	public TaskSet getTasksOutsideComponent(String compSpec) {
		
		/* translate the component's name to its ID */
		int compId = getComponentId(compSpec);
		if (compId == ErrorCode.NOT_FOUND){
			return null;
		}
		
		return getTasksOutsideComponent(compId);
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * Validate a component's name. Valid characters are digits, letters (upper or lower case),
	 * '-' and '_'. No other characters are permitted. Component names must contain at least
	 * three characters and start with a letter.
	 * 
	 * @param componentName The component name to be validated.
	 * @return True if the name is valid, else false.
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