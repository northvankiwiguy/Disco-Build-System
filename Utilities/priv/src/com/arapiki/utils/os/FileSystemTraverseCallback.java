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

package com.arapiki.utils.os;

import java.io.File;

/**
 * This abstract class is used as a template for creating callback methods. The
 * caller of the SystemUtils.traverseFileSystem() method must subclass this class and define
 * a callback function. This callback method is invoked whenever traverseFileSystem()
 * finds a matching path.
 */
public abstract class FileSystemTraverseCallback {

	/**
	 * The callback method that will be invoked as we traverse the file system. This
	 * method is abstract and must be overridden.
	 * @param thisPath The path of the current point of traversal
	 */
	public abstract void callback(File thisPath);
}
