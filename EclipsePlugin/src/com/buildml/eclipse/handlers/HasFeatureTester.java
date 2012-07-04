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

package com.buildml.eclipse.handlers;

import org.eclipse.core.expressions.PropertyTester;
import com.buildml.eclipse.utils.EclipsePartUtils;

/**
 * An Eclipse PropertyTester, for testing whether the current sub editor
 * supports a specified feature. This class is used by adding a "test"
 * expression in the enabledWhen or visibleWhen clauses in plugin.xml
 *
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class HasFeatureTester extends PropertyTester {
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Test the "hasFeature" property of the currently active sub editor to see if it
	 * supports the specified argument.
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		
		if (property.equals("hasFeature")) {
			if ((args.length == 1) && (args[0] instanceof String)) {
				String feature = (String)args[0];
				return EclipsePartUtils.activeSubEditorHasFeature(feature);
			}
		}
		return false;
	}

	/*-------------------------------------------------------------------------------------*/
}
