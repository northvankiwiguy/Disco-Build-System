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

package com.buildml.model;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.buildml.model.impl.BuildStore;

/**
 * A factory for opening and creating new BuildML BuildStore objects. A BuildStore
 * object is the in-memory representation of a .bml file.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class BuildStoreFactory {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Open or create a new BuildStore database. If the database already
	 * exists, open it for updating. If there's no database by this name,
	 * create a fresh database.
	 * 
	 * @param buildStoreName Name of the database to open or create.
	 * @param saveRequired True if BuildStore must explicitly be "saved" before
	 *        it's closed (otherwise the changes will be discarded).
	 * @return The new BuildStore object.
	 * @throws FileNotFoundException The database file can't be found, or isn't writable.
	 * @throws IOException An I/O problem occurred while opening the database file.
	 * @throws BuildStoreVersionException The database schema of an existing database is 
	 *         the wrong version.
	 */
	public static IBuildStore openBuildStore(String buildStoreName, boolean saveRequired)
			throws FileNotFoundException, IOException, BuildStoreVersionException
	{
		/*
		 * For now, there is only one implementation of the IBuildStore interface,
		 * but in future we might have more options.
		 */
		return new BuildStore(buildStoreName, saveRequired);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Open or create a new BuildStore database. If the database already
	 * exists, open it for updating. If there's no database by this name,
	 * create a fresh database. Changes to this BuildStore will automatically
	 * be saved to the original BuildML file (no "save" operation required).
	 * 
	 * @param buildStoreName Name of the database to open or create.
	 * @return The new BuildStore object.
	 * @throws FileNotFoundException The database file can't be found, or isn't writable.
	 * @throws IOException An I/O problem occurred while opening the database file.
	 * @throws BuildStoreVersionException The database schema of an existing database is the 
	 *         wrong version.
	 */
	public static IBuildStore openBuildStore(String buildStoreName)
			throws FileNotFoundException, IOException, BuildStoreVersionException 
	{
		/*
		 * For now, there is only one implementation of the IBuildStore interface,
		 * but in future we might have more options.
		 */
		return new BuildStore(buildStoreName, false);
	}
	
	/*-------------------------------------------------------------------------------------*/
}
