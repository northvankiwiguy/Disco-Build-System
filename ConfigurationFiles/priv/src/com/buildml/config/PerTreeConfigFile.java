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

package com.buildml.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.utils.errors.ErrorCode;

/**
 * Objects of this class manage the per-tree configuration data that's associated with
 * each checked-out source tree. Alongside the standard build.bml file, there's a
 * ".bmlconfig" file that holds per-tree aliases, native root paths, etc. All reading,
 * writing and management of this configuration file is handled by this class.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class PerTreeConfigFile {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** version of the XML file schema */
	public static final int SCHEMA_VERSION = 1;
	
	/** The native path to the configuration file */
	private File configFile = null;
	
	/** A mapping from alias names to the list of packages the alias represents */
	private HashMap<String, String[]> aliasMap;

	/** The BuildStore that this config file augments */
	private IBuildStore buildStore;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new {@link PerTreeConfigFile}.
	 * 
	 * @param buildStore 	The IBuildStore that this configuration is associated with.
	 * @param configFile 	The path on the native file system to the configuration file.
	 *                      This file will be read from disk when the object is instantiated,
	 *                      and written back to disk when a save() operation is invoked.
	 * @throws IOException  An error occurred while opening/parsing the file.
	 * 
	 */
	public PerTreeConfigFile(IBuildStore buildStore, File configFile) throws IOException {
		
		this.buildStore = buildStore;
		this.configFile = configFile;
		
		/* create empty data structures - to be populated from the file, or programmatically */
		aliasMap = new HashMap<String, String[]>();
		
		/* parse the content of the file into memory, if it exists, else create it. */
		if (configFile.exists()) {
			try {
				load(configFile);
			} catch (SAXException e) {
				/* translate SAXException into IOException */
				throw new IOException(e.getMessage());
			}
		} else {
			save();
		}
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Write the content of the configuration to the disk file. The whole set of in-memory
	 * data structures are written to the file, overwriting any previous content in the file.
	 * 
	 * @throws IOException A problem occurred while writing to the file.
	 */
	public void save() throws IOException {
		
		PrintWriter out = new PrintWriter(new FileWriter(configFile));
		out.println("<bmlconfig version=\"" + SCHEMA_VERSION + "\">");
		
		for (Iterator<String> iter = aliasMap.keySet().iterator(); iter.hasNext();) {
			String aliasName = (String) iter.next();
			out.println(" <alias name=\"" + aliasName + "\">");
			String pkgs[] = getAlias(aliasName);
			for (int i = 0; i < pkgs.length; i++) {
				out.println("  <package name=\"" + pkgs[i] + "\"/>");
			}
			out.println(" </alias>");
		}
		out.println("</bmlconfig>");
		out.close();		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add or update a build alias in the configuration. An alias can be used as a short-cut
	 * for specifying a list of packages to be built.
	 * 
	 * @param aliasName	The alias name.
	 * @param packages	An array of package names to be built when the alias is built.
	 * @return ErrorCode.OK on success, ErrorCode.INVALID_NAME if the alias name is not
	 * legal, ErrorCode.BAD_VALUE if one of the package names is invalid.
	 */
	public int addAlias(String aliasName, String packages[]) {
		
		/* valid the alias's name */
		if (!isValidAliasName(aliasName)) {
			return ErrorCode.INVALID_NAME;
		}
		
		/* valid the list of packages */
		if ((packages == null) || (packages.length == 0)) {
			return ErrorCode.BAD_VALUE;
		}
		for (int i = 0; i < packages.length; i++) {
			if (!isValidPackage(packages[i])) {
				return ErrorCode.BAD_VALUE;
			}
		}
		
		/* make a copy of the input array, and make sure it's sorted */
		String sortedPackages[] = Arrays.copyOf(packages, packages.length);
		Arrays.sort(sortedPackages);
		
		aliasMap.put(aliasName, sortedPackages);
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Remove an existing build alias from the configuration.
	 * 
	 * @param aliasName The name of the alias to be removed.
	 * @return ErrorCode.OK on success, or ErrorCode.NOT_FOUND if the alias is not defined.
	 */
	public int removeAlias(String aliasName) {

		Object previous = aliasMap.remove(aliasName);
		if (previous == null) {
			return ErrorCode.NOT_FOUND;
		}
		
		return ErrorCode.OK;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the array of packages that are associated with the specified alias.
	 * 
	 * @param aliasName Name of the alias to expand.
	 * @return An array of package names to be built, or null if the alias is undefined.
	 */
	public String[] getAlias(String aliasName) {
		return (String[])aliasMap.get(aliasName);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The configure file's list of build aliases, in alphabetic order.
	 */
	public String[] getAliases() {
		
		Set<String> keySet = aliasMap.keySet();
		String result[] = keySet.toArray(new String[keySet.size()]);
		Arrays.sort(result);
		return result;
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Read the content of the configuration file into in-memory data structures.
	 * 
	 * @param configFile		The native file containing the configuration data (in XML).
	 * @throws SAXException 	An error occurred while parsing the XML.
	 * @throws IOException 		An error occurred while opening/reading the file.
	 */
	private void load(File configFile) throws SAXException, IOException {
		
		/* Open the file for input, and report progress every two seconds */
		FileInputStream in = new FileInputStream(configFile);

		/*
		 * Create a new XMLReader to parse this file, then set the ContentHandler
		 * to our own SAX handler class.
		 */
		XMLReader parser = XMLReaderFactory.createXMLReader();
		ContentHandler contentHandler = new PerTreeConfigSAXHandler(this);
		parser.setContentHandler(contentHandler);

		try {
			parser.parse(new InputSource(in));
		} finally {
			in.close();
		}
	}
		
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Determine whether a user-supplied alias name is valid.
	 * 
	 * @param aliasName The alias name.
	 * @return True if the name is valid, else false.
	 */
	private boolean isValidAliasName(String aliasName) {
		
		if ((aliasName == null) || (aliasName.length() == 0)) {
			return false;
		}
		for (int i = 0; i < aliasName.length(); i++) {
			char ch = aliasName.charAt(i);
			if ((ch != '_') && (!Character.isAlphabetic(ch))) {
				return false;
			}
		}
		
		return true;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Determine whether the specified package is valid (in the BuildStore).
	 * 
	 * @param pkgName The package's name.
	 * @return True if the package is valid, else false.
	 */
	private boolean isValidPackage(String pkgName) {
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		return pkgMgr.getId(pkgName) != ErrorCode.NOT_FOUND;
	}

	/*-------------------------------------------------------------------------------------*/
}
