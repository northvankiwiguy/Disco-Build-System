/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.model.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.buildml.model.BuildStoreVersionException;
import com.buildml.model.FatalBuildStoreError;

/**
 * Static class for upgrading a pre-existing database file to the latest schema. Whenever
 * the schema (in BuildStoreDB) is changed, the appropriate upgrade statements should also
 * be added here.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class UpgradeDB {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * If necessary, perform the upgrade of the database to the current software's schema version.
	 * Downgrading is not possible.
	 * 
	 * @param databaseFileName Name of the database file to open.
	 * @throws BuildStoreVersionException We couldn't upgrade from the database file's schema.
	 * @throws FileNotFoundException The database file doesn't exist.
	 */
	public static void upgrade(String databaseFileName)
			throws BuildStoreVersionException, FileNotFoundException {
		
		Connection dbConn = null;
		Statement stat = null;
		ResultSet rs = null;
		int dbVersion = 0;

		/* check that the file actually exists */
		if (!new File(databaseFileName).exists()) {
			throw new FileNotFoundException("Database file: " + databaseFileName + " not found.");
		}
		
		/* Load the sqlite JDBC connector */
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			throw new FatalBuildStoreError("Unable to access the SQLite driver", e);
		}

		/* open the database */
		try {
			dbConn = DriverManager.getConnection("jdbc:sqlite:" + databaseFileName);
		} catch (SQLException e) {
			throw new BuildStoreVersionException("The database couldn't be opened. " + e.getMessage());
		}

		/* fetch the version number of the database file. */
		try {
			stat = dbConn.createStatement();
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Unable to create a SQL statement", e);
		}
		try {
			rs = stat.executeQuery("select version from schemaVersion");
			if (rs.next()) {
				dbVersion = rs.getInt("version");
			}
			rs.close();
			stat.close();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			dbVersion = 0;
		}

		/* check the version number for "update-ness" */
		if (dbVersion == 0) {
			throw new BuildStoreVersionException("The database doesn't contain a version number.");
		}
		if (dbVersion > BuildStoreDB.SCHEMA_VERSION) {
			throw new BuildStoreVersionException(
					"This database schema is too new to be supported by this software.");
		}
		
		/* perform the upgrade */
		if (dbVersion < BuildStoreDB.SCHEMA_VERSION) {
			performUpgradeSteps(dbVersion, dbConn);
		}

		/* else, the version numbers are consistent - nothing to do */
		try {
			dbConn.close();
		} catch (SQLException e) {
			throw new BuildStoreVersionException("Upgrade of database failed for some reason.");
		}
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Perform the actual database upgrade. This is essentially a big list of upgrade
	 * steps, allowing upgrade from any old version to the current version.
	 * 
	 * @param dbVersion Current version of the database schema.
	 * @param dbConn Connection to the database.
	 */
	private static void performUpgradeSteps(int dbVersion, Connection dbConn) {
		
		/* fetch the version number of the database file. */
		try {
			Statement stat = dbConn.createStatement();
			
			/* upgrade from 400 to 401 */
			if (dbVersion == 400) {
				stat.executeUpdate("create table fileGroups (id integer primary key, pkgId integer, " +
						"type integer)");
			}

			/* finish by setting the new version number */
			stat.executeUpdate("update schemaVersion set version=" + BuildStoreDB.SCHEMA_VERSION);
		
		} catch (SQLException e) {
			throw new FatalBuildStoreError("Error while upgrading database: " + e.getMessage());
		}		
	}
	
	/*-------------------------------------------------------------------------------------*/
}
