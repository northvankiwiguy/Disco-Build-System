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
 * by the BuildStore class and it's helpers. This class, and all the methods
 * are package-private, meaning that the end user shouldn't be aware of this
 * class.
 * @author "Peter Smith <psmith@arapiki.com>"
 */
class BuildStoreDB  {

	/**
	 * Our connection to the BuildStore's SQL database.
	 */
	private Connection dbConn = null;

	/**
	 * What is our current schema version (for newly created BuildStore objects)?
	 * If the database we're reading has a newer schema, we can't handle it. If
	 * it has an older schema, we need to upgrade it.
	 */
	private static final int SCHEMA_VERSION = 1;


	/* Prepared Statements to make database access faster */
	private PreparedStatement lastRowIDPrepStmt = null;
	
	/**
	 * Create a new BuildStoreDB object.
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
	
	/**
	 * Retrieve the schema version of this database.
	 * @return The schema version, or 0 if the schema isn't yet initialized.
	 */
	/* package private */
	int getBuildStoreVersion() {
		int version = 0;
		Statement stat = null;
		ResultSet rs = null;
		
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
	
	/**
	 * Initialize the database by adding an entirely new schema. Remove any
	 * fields that already exist.
	 */
	/* package private */
	void initDatabase() {

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
							   "pathType integer, name text not null)");
			stat.executeUpdate("insert into files values (0, 0, 1, \"/\")");
			stat.executeUpdate("create unique index filesIdx on files (parentId, name)");
			
			/* Create the "fileIncludes" table */
			stat.executeUpdate("create table fileIncludes ( fileId1 integer, fileId2 integer, usage integer)");
			stat.executeUpdate("create unique index buildFileIncludesIdx1 on fileIncludes (fileId1, fileId2)");
			stat.executeUpdate("create index buildFileIncludesIdx2 on fileIncludes (fileId1)");
			stat.executeUpdate("create index buildFileIncludesIdx3 on fileIncludes (fileId2)");
			
			/* Create the "buildTasks" table. */
			stat.executeUpdate("create table buildTasks ( taskId integer primary key, parentTaskId integer, " +
							   "taskDirId integer, command text)");
			stat.executeUpdate("insert into buildTasks values (0, 0, 0, null)");
			// TODO: do we need an index here?
			
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
			
			stat.close();
						
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to initialize database schema", e);
		}
	}
	
	/**
	 * 
	 */
	public void dropDatabase() {
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
			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to drop database schema", e);
		}
	}

	/**
	 * Specify whether database access should be fast (true) or safe (false). Fast
	 * access is considerably faster than safe access, but won't ensure that
	 * changes are written to the disk. Only use fast access for "large write" operations.
	 * @param fast Set to true to enable fast access, or false for safe access.
	 */
	/* package private */	
	void setFastAccessMode(boolean fast){
		try {
			dbConn.setAutoCommit(!fast);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to setFastAccessMode", e);
		}
	}

	/**
	 * A helper method to perform any type of "update" command in SQL. That is,
	 * there can't be any results returned from this command.
	 * @param sql The SQL command to executed.
	 * @return The number of rows changed (or 0 if the command in question didn't change rows).
	 */
	/* package private */
	int executeUpdate(String sql) {
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

	/**
	 * @param insertChildPrepStmt
	 */
	public int executePrepUpdate(PreparedStatement stmt) {
		int rowCount = 0;
		try {
			rowCount = stmt.executeUpdate();
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error executing SQL: ", e);
		}
		return rowCount;
	}
	
	/**
	 * @param cmd
	 * @return
	 */
	public String[] executeSelectColumn(String sql) {
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

	/**
	 * Execute a prepared database statement that returns a value
	 * (such as a select statement). The query should only return
	 * a single column of results (if multiple columns are queried,
	 * only the first will be returned).
	 * @param stmt The prepared statement to be executed.
	 * @return Returns a (possibly empty) array of results. 
	 */
	public String[] executePrepSelectStringColumn(PreparedStatement stmt) {
		
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
	
	/**
	 * Execute a prepared database statement that returns a value
	 * (such as a select statement). The query should only return
	 * a single column of results (if multiple columns are queried,
	 * only the first will be returned).
	 * @param stmt The prepared statement to be executed.
	 * @return Returns a (possibly empty) array of results. 
	 */
	public Integer[] executePrepSelectIntegerColumn(PreparedStatement stmt) {
		
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
	
	/**
	 * Execute a database query using a prepared statement, and
	 * return the full ResultSet object.
	 * @param cmd
	 * @return
	 */
	public ResultSet executePrepSelectResultSet(PreparedStatement stmt) {
		ResultSet rs;
		try {
			rs = stmt.executeQuery();
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error executing SQL:", e);
		}
		
		return rs;
	}



	/**
	 * Returns the integer value of the last auto-increment row ID. This is used to
	 * fetch the last primary key inserted into a table, when the user provided "null"
	 * to have the database automatically select a suitable unique primary key.
	 * @return The last inserted primary key.
	 */
	public int getLastRowID() {
		Integer lastRowID[] = executePrepSelectIntegerColumn(lastRowIDPrepStmt);
		return lastRowID[0];
	}

	/**
	 * Create a prepared statement. Some other class will use this later on. We're
	 * just creating it for them, since we own the database connection.
	 * @param string
	 * @return
	 */
	public PreparedStatement prepareStatement(String sql) {
		try {
			return dbConn.prepareStatement(sql);
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to prepare sql statement: " + sql, e);
		}
	}


	
}
