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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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

	/**
	 * Create a new BuildStoreDB object.
	 * @param databaseName The name of the database to create. For SQLite databases,
	 * this is the path to the database file.
	 */
	/* package private */ 
	BuildStoreDB(String databaseName) {
		
		/* make sure that the sqlite JDBC connector is available */
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			throw new FatalBuildStoreError("Unable to access the SQLite driver", e);
		}
		
		/* 
		 * Open/create the database. The sqlite database will be created as
		 * a local disk file with a .db extension.
		 */
	    try {
			dbConn = DriverManager.getConnection("jdbc:sqlite:" + databaseName + ".db");

			
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to connect to SQLite database: " + 
					databaseName + ".db", e);
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
			rs = stat.executeQuery("select version from schema_version");
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

		try {
			Statement stat = dbConn.createStatement();
			
			/*
			 * Create the "schema_version" table, and insert the current schema version. We can
			 * use this field to detect older versions of the database.
			 */
			stat.executeUpdate("drop table if exists schema_version");
			stat.executeUpdate("create table schema_version ( version integer )");
			stat.executeUpdate("insert into schema_version values ( " + SCHEMA_VERSION + ")");

			/*
			 * Create the "files" table.
			 */
			stat.executeUpdate("drop table if exists files");
			stat.executeUpdate(
				"create table files ( " +
					"id integer primary key," +
					"name text not null" +
				")");
			stat.close();
						
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to initialize database schema", e);
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
	 */
	/* package private */
	void executeUpdate(String sql) {
		Statement stmt;
		try {
			stmt = dbConn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error execute SQL: " + sql, e);
		}
	}
	
}
