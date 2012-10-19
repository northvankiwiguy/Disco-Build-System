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

import com.buildml.model.types.FileSet;

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
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface IFileAttributeMgr {

	/**
	 * Add a new attribute name to the list of attributes that can be associated
	 * with a path.
	 * 
	 * @param attrName The name of the new attribute.
	 * @return The new attribute's ID number, or ALREADY_USED if this attribute name is
	 * already in use.
	 */
	public abstract int newAttrName(String attrName);

	/**
	 * For a given attribute name, return the corresponding ID number.
	 * 
	 * @param attrName The attribute's name.
	 * @return The attribute's ID number, or NOT_FOUND if the attribute name isn't defined.
	 */
	public abstract int getAttrIdFromName(String attrName);

	/**
	 * For a given attribute ID, return the corresponding attribute name.
	 * 
	 * @param attrId The attribute's ID number.
	 * @return The attributes name, or null if the attribute name isn't defined.
	 */
	public abstract String getAttrNameFromId(int attrId);

	/**
	 * Return an array of all attribute names.
	 * 
	 * @return A String array of attribute names. The names will be returned in 
	 * alphabetical order.
	 */
	public abstract String[] getAttrNames();

	/**
	 * Remove the attribute's name from the list of attributes that can be associated
	 * with a path. An attribute can only be removed if it's no longer in use.
	 * 
	 * @param attrName The name of the attribute to be removed.
	 * @return OK on successful deletion, NOT_FOUND if the attribute name doesn't exist, or
	 * CANT_REMOVE if there are files still making use of this attribute.
	 */
	public abstract int deleteAttrName(String attrName);

	/**
	 * For the specific path (pathId), set the attribute (attrId) to the specified String
	 * value (attrValue). Note that for performance reasons, there's no error checking on
	 * the pathId and attrId values - Any integer values are acceptable, and could potentially
	 * overwrite the existing value of this attribute for this path.
	 * 
	 * @param pathId The path to attach the attribute to.
	 * @param attrId The attribute to be set
	 * @param attrValue The String value to set the attribute to.
	 */
	public abstract void setAttr(int pathId, int attrId, String attrValue);

	/**
	 * For the specific path (pathId), set the attribute (attrId) to the specified integer
	 * value (attrValue). Note that for performance reasons, there's no error checking on
	 * the pathId and attrId values - Any integer values are acceptable, and could potentially
	 * overwrite the existing value of this attribute for this path.
	 * 
	 * @param pathId The path to attach the attribute to.
	 * @param attrId The attribute to be set.
	 * @param attrValue The integer value to set the attribute to.
	 * @return OK if the attribute was added successfully, or BAD_VALUE if the integer
	 * is not valid (i.e. not >= 0).
	 */
	public abstract int setAttr(int pathId, int attrId, int attrValue);

	/**
	 * Fetch the specified attribute value from the specified path, returning it as a String.
	 * 
	 * @param pathId The path on which the attribute is attached.
	 * @param attrId The attribute whose value we want to fetch.
	 * @return The attributes String value, or null if the attribute isn't set on this path.
	 */
	public abstract String getAttrAsString(int pathId, int attrId);

	/**
	 * Fetch the specified attribute value from the specified path, returning it as an int.
	 * 
	 * @param pathId The path on which the attribute is attached.
	 * @param attrId The attribute whose value we want to fetch.
	 * @return The attributes positive integer value, BAD_VALUE if the attribute's
	 * value isn't a positive integer, or NOT_FOUND if the attribute wasn't set.
	 */
	public abstract int getAttrAsInteger(int pathId, int attrId);

	/**
	 * Remove the attribute (attrId) that's currently associated with the specified 
	 * path (pathId). For performance reasons, no error checking is done to validate
	 * the path or attribute values. This method succeeds regardless of whether the
	 * attribute is set or not.
	 * 
	 * @param pathId The path on which the attribute is attached.
	 * @param attrId The attribute to be removed.
	 */
	public abstract void deleteAttr(int pathId, int attrId);

	/**
	 * Remove all the attributes that are associated with the specified path.
	 * @param pathId The path to which the attributes are currently attached.
	 */
	public abstract void deleteAllAttrOnPath(int pathId);

	/**
	 * Given a path ID, return an array of attributes that are attached to that path.
	 * 
	 * @param pathId The ID of the path the attributes are attached to.
	 * @return An Integer [] array of all attributes attached to this path.
	 */
	public abstract Integer[] getAttrsOnPath(int pathId);

	/**
	 * Return the FileSet containing all paths that have the attribute set (to any value).
	 * 
	 * @param attrId The attribute to test for.
	 * @return The FileSet of all files that have this attribute set.
	 */
	public abstract FileSet getPathsWithAttr(int attrId);

	/**
	 * Return the FileSet of all paths that have the specified attribute set to the
	 * specified String value.
	 * 
	 * @param attrId The attribute to test for.
	 * @param value The value to compare against.
	 * @return The FileSet of all files that have this attribute set to the specified value.
	 */
	public abstract FileSet getPathsWithAttr(int attrId, String value);

	/**
	 * Return the FileSet of all paths that have the specified attribute set to the
	 * specified Integer value.
	 * 
	 * @param attrId The attribute to test for.
	 * @param value The value to compare against.
	 * @return The FileSet of all files that have this attribute set to the specified value.
	 */
	public abstract FileSet getPathsWithAttr(int attrId, int value);

}