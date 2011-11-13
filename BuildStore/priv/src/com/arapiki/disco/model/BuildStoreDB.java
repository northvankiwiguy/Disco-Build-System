/*******************************************************************************
 * Copyright (c) 2010 Arapiki Solutions Inc.
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

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * A helper class to manage and simplify all the database access performed
 * by the BuildStore class and it's managers. This class, and all the methods
 * are package-private, meaning that the end user shouldn't be aware of this
 * class.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
class BuildStoreDB  {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/**
	 * Our connection to the BuildStore's SQL database.
	 */
	private Connection dbConn = null;

	/**
	 * The current schema version (for newly created BuildStore objects).
	 * If the database we're reading has a newer schema, we can't handle it. If
	 * it has an older schema, we need to upgrade it.
	 */
	private static final int SCHEMA_VERSION = 1;


	/** Prepared Statements to make database access faster. */
	private PreparedStatement lastRowIDPrepStmt = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new BuildStoreDB object.
	 * 
	 * @param databaseName The name of the database to create. For SQLite databases,
	 * this is the path to the database file.
	 * @throws FileNotFoundException The database file can't be found, or isn't writable.
	 */
	/* package private */ 
	BuildStoreDB(String databaseName) throws FileNotFoundException {
		
		/* make sure that the sqlite JDBC connector is available */
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			throw new FatalBuildStoreError("Unable to access the SQLite driver", e);
		}
		
		/*
		 * Ensure that the database name ends with .disco (if it doesn't already)
		 */
		if (!databaseName.endsWith(".disco")) {
			databaseName += ".disco";
		}
		
		/* 
		 * Open/create the database. The sqlite database will be created as
		 * a local disk file with a .disco extension.
		 */
	    try {
			dbConn = DriverManager.getConnection("jdbc:sqlite:" + databaseName);
			
		} catch (SQLException e) {
			/* provide a meaningful error message if the file simply can't be opened */
			if (e.getMessage().contains("Permission denied")) {
				throw new FileNotFoundException("Error: Unable to open database file: " + databaseName);
			}
			
			/* else provide a more generic message */
			throw new FatalBuildStoreError("Unable to access to SQLite database: " + 
					databaseName + "\n" + e.getMessage());
		}
		
		/* prepare some statements */
		lastRowIDPrepStmt = prepareStatement("select last_insert_rowid()");
		
		/* performance tuning for the database */
		long maxMemory = Runtime.getRuntime().maxMemory();
		if (maxMemory != Long.MAX_VALUE){
			/* 
			 * Set the SQLITE in-memory page cache to 30% of available memory. 
			 * This makes the database really fast, although does tend to take up memory.
			 */
			int cache_size = (int)(maxMemory / 1024 * 0.3);
			if (cache_size < 2000) {
				cache_size = 2000;
			}
			executeUpdate("pragma cache_size=" + cache_size);
		}
	}
	
	/*=====================================================================================*
	 * PACKAGE METHODS
	 *=====================================================================================*/
	
	/**
	 * Retrieve the schema version of this database.
	 * 
	 * @return The schema version, or 0 if the schema isn't yet initialized.
	 */
	/* package private */
	int getBuildStoreVersion() {
		int version = 0;
		Statement stat = null;
		ResultSet rs = null;
		
		/* make sure the database connection is still open */
		checkDatabase();
		
		/* 
		 * Create a new statement. If this fails, it's really bad and
		 * we can't use the database at all.
		 */
		try {
			stat = dbConn.createStatement();
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to create a SQL statement", e);
		}
		
		/* 
		 * Query the schema_version table to retrieve the version number. If this
		 * fails, it's just because we haven't initialized the schema. Return 0.
		 */
		try {
			rs = stat.executeQuery("select version from schemaVersion");
			if (rs.next()) {
				version = rs.getInt("version");
			}			
		} catch (SQLException e) {
			/* there's no schema in place, return 0 */
			version = 0;
		}
		
		/* close everything - if this fails, things are really bad */
		try {
			if (stat != null) {
				stat.close();
				if (rs != null) {
					rs.close();					
				}
			}			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to close SQL statement", e);
		}
		
		/* return the schema version */
	    return version;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Initialize the database by adding an entirely new schema. Remove any
	 * fields that already exist.
	 */
	/* package private */
	void initDatabase() {
		
		/* make sure the database connection is still open */
		checkDatabase();

		dropDatabase();
		try {
			Statement stat = dbConn.createStatement();
			
			/*
			 * Create the "schema_version" table, and insert the current schema version. We can
			 * use this field to detect older versions of the database.
			 */
			stat.executeUpdate("create table schemaVersion ( version integer )");
			stat.executeUpdate("insert into schemaVersion values ( " + SCHEMA_VERSION + ")");

			/* Create the "files" table. */
			stat.executeUpdate("create table files ( id integer primary key, parentId integer, " +
							   "pathType integer, compId integer, compSectionId integer, name text not null)");
			stat.executeUpdate("insert into files values (0, 0, 1, 0, 0, \"/\")");
			stat.executeUpdate("create unique index filesIdx on files (parentId, name)");
			
			/* Create the "fileIncludes" table */
			stat.executeUpdate("create table fileIncludes ( fileId1 integer, fileId2 integer, usage integer)");
			stat.executeUpdate("create unique index buildFileIncludesIdx1 on fileIncludes (fileId1, fileId2)");
			stat.executeUpdate("create index buildFileIncludesIdx2 on fileIncludes (fileId1)");
			stat.executeUpdate("create index buildFileIncludesIdx3 on fileIncludes (fileId2)");
			
			/* Create the "buildTasks" table. */
			stat.executeUpdate("create table buildTasks ( taskId integer primary key, parentTaskId integer, " +
							   "taskDirId integer, compId integer, command text)");
			stat.executeUpdate("insert into buildTasks values (0, 0, 0, 0, null)");
			stat.executeUpdate("create index buildTasksIdx on buildTasks (parentTaskId)");
			
			/* Create the "buildTaskFiles" tables. */
			stat.executeUpdate("create table buildTaskFiles ( taskId integer, fileId integer, operation integer)");			
			stat.executeUpdate("create index buildTaskFilesIdx1 on buildTaskfiles (fileId)");
			stat.executeUpdate("create unique index buildTaskFilesIdx2 on buildTaskfiles (taskId, fileId)");
			stat.executeUpdate("create index buildTaskFilesIdx3 on buildTaskfiles (fileId, operation)");
			
			/* Create the "fileRoots" table */
			stat.executeUpdate("create table fileRoots (name text primary key, fileId integer)");	
			stat.executeUpdate("insert into fileRoots values (\"root\", 0)");
			
			/* Create the fileAttrsName table */
			stat.executeUpdate("create table fileAttrsName (id integer primary key, name text)");
			
			
			/* Create the fileAttrs table */
			stat.executeUpdate("create table fileAttrs (pathId integer, attrId integer, value text)");
			stat.executeUpdate("create index fileAttrsIdx1 on fileAttrs (pathId)");
			stat.executeUpdate("create unique index fileAttrsIdx2 on fileAttrs (pathId, attrId)");
			
			/* Create the components table */
			stat.executeUpdate("create table components (id integer primary key, name text)");
			stat.executeUpdate("insert into components values (0, 'None')");
			
			stat.close();
						
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to initialize database schema", e);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Delete the content of the database, normally in preparation to recreate it with
	 * a fresh schema.
	 */
	/* package private */ 
	void dropDatabase() {
				
		/* make sure the database connection is still open */
		checkDatabase();

		try {
			Statement stat = dbConn.createStatement();
			stat.executeUpdate("drop table if exists schemaVersion");
			stat.executeUpdate("drop table if exists files");
			stat.executeUpdate("drop table if exists fileIncludes");
			stat.executeUpdate("drop table if exists buildTasks");
			stat.executeUpdate("drop table if exists buildTaskFiles");
			stat.executeUpdate("drop table if exists fileRoots");
			stat.executeUpdate("drop table if exists fileAttrsName");
			stat.executeUpdate("drop table if exists fileAttrs");
			stat.executeUpdate("drop table if exists components");
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to drop database schema", e);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Specify whether database access should be fast (true) or safe (false). Fast
	 * access is considerably faster than safe access, but won't ensure that
	 * changes are written to the disk. Only use fast access for "large write" operations.
	 * 
	 * @param fast Set to true to enable fast access, or false for safe access.
	 */
	/* package private */	
	void setFastAccessMode(boolean fast){
				
		/* make sure the database connection is still open */
		checkDatabase();

		try {
			dbConn.setAutoCommit(!fast);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to setFastAccessMode", e);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Execute a (non-prepared) SQL statement of any update-style command. That is,
	 * there can't be any results returned from this command.
	 * 
	 * @param sql The SQL command to executed.
	 * @return The number of rows changed (or 0 if the command in question didn't change rows).
	 */
	/* package private */
	int executeUpdate(String sql) {
				
		/* make sure the database connection is still open */
		checkDatabase();

		Statement stmt;
		int rowCount = 0;
		try {
			stmt = dbConn.createStatement();
			rowCount = stmt.executeUpdate(sql);
			stmt.close();
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error executing SQL: " + sql, e);
		}
		return rowCount;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Execute a prepared SQL update-style statement, and return the number of rows that 
	 * were updated. This should only be used for SQL commands that update the database,
	 * since it can't return the result of a query.
	 * 
	 * @param stmt The prepared SQL statement to execute
	 * @return The number of rows updated after executing the statement
	 */
	/* package private */
	int executePrepUpdate(PreparedStatement stmt) {
				
		/* make sure the database connection is still open */
		checkDatabase();

		int rowCount = 0;
		try {
			rowCount = stmt.executeUpdate();
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error executing SQL: ", e);
		}
		return rowCount;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Execute a (non-prepared) SQL statement, returning a String array of the results
	 * (one array entry per returned row). This method is simply a helper to make this
	 * common operation easier to use.
	 * @param sql The SQL command to be executed
	 * @return An array of Strings, one per returned row.
	 */
	/* package private */
	String[] executeSelectColumn(String sql) {		
		
		/* make sure the database connection is still open */
		checkDatabase();

		Statement stmt;
		ArrayList<String> result;
		try {
			stmt = dbConn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			result = new ArrayList<String>();
			while (rs.next()){
				result.add(rs.getString(1));
			}
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error executing SQL: " + sql, e);
		}
		
		return result.toArray(new String[0]);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Execute a prepared database statement that performs a query on a String column. 
	 * The query should return a single column of results, and the values will be returned as 
	 * strings. If multiple columns are queried, only the first will be returned.
	 * 
	 * @param stmt The prepared statement to be executed.
	 * @return Returns a (possibly empty) array of results. 
	 */
	/* package private */
	String[] executePrepSelectStringColumn(PreparedStatement stmt) {
		
		/* make sure the database connection is still open */
		checkDatabase();
		
		ArrayList<String> result;
		try {
			ResultSet rs = stmt.executeQuery();
			result = new ArrayList<String>();
			while (rs.next()){
				result.add(rs.getString(1));
			}
			rs.close();
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error executing SQL:", e);
		}
		
		return result.toArray(new String[0]);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Execute a prepared database statement that returns Integer values, such as a select
	 * on a numeric column. The query should only return a single column of Integer results.
	 * If multiple columns are queried, only the first will be returned.
	 * 
	 * @param stmt The prepared statement to be executed.
	 * @return Returns a (possibly empty) array of results. 
	 */
	/* package private */
	Integer[] executePrepSelectIntegerColumn(PreparedStatement stmt) {
				
		/* make sure the database connection is still open */
		checkDatabase();

		ArrayList<Integer> result;
		try {
			ResultSet rs = stmt.executeQuery();
			result = new ArrayList<Integer>();
			while (rs.next()){
				result.add(rs.getInt(1));
			}
			rs.close();
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error executing SQL:", e);
		}
		
		return result.toArray(new Integer[0]);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Execute a database query using a prepared statement, and return the full ResultSet 
	 * object. This is purely a convenience function to make it easier to access the
	 * database and catch Exceptions.
	 * 
	 * @param stmt The prepared SQL statement to be executed.
	 * @return The ResultSet from the database query.
	 */
	/* package private */
	ResultSet executePrepSelectResultSet(PreparedStatement stmt) {
				
		/* make sure the database connection is still open */
		checkDatabase();

		ResultSet rs;
		try {
			rs = stmt.executeQuery();
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error executing SQL:", e);
		}
		
		return rs;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Returns the integer value of the last auto-increment row ID. This is used to
	 * fetch the last primary key inserted into a table, when the user specified "null"
	 * to have the database automatically select a suitable unique primary key.
	 * 
	 * @return The last inserted primary key.
	 */
	/* package private */
	int getLastRowID() {
		
		/* make sure the database connection is still open */
		checkDatabase();
		
		Integer lastRowID[] = executePrepSelectIntegerColumn(lastRowIDPrepStmt);
		return lastRowID[0];
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create a prepared statement from an SQL command string.
	 * 
	 * @param sql The SQL command to be prepared.
	 * @return The prepared database statement for executing the SQL command at a later time.
	 */
	/* package private */
	PreparedStatement prepareStatement(String sql) {
		
		/* make sure the database connection is still open */
		checkDatabase();
		
		try {
			return dbConn.prepareStatement(sql);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to prepare sql statement: " + sql, e);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Close a database connection, releasing all resources. From this point on,
	 * none of the methods in the class may be used (they'll simply throw an
	 * exception).
	 */
	/* package private */
	void close() {
				
		/* make sure the database connection is still open */
		checkDatabase();

		/* make sure all changes are committed */
		setFastAccessMode(false);
		try {
			dbConn.close();
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to close database connection: " + e);
		}
		
		/* 
		 * Make the connection variable unusable - this will result in exceptions being
		 * thrown if somebody try to use it.
		 */
		dbConn = null;
	}
		
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * Check that our database connection is open, else throw an exception. This stops
	 * us from getting weird SQL errors when somebody tries to use the database after
	 * it's closed.
	 */
	private void checkDatabase() {
		if (dbConn == null) {
			throw new FatalBuildStoreError("BuildStore has been closed.");
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
}
