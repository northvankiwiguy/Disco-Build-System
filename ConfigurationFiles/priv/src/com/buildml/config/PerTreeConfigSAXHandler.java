/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.config;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.buildml.utils.errors.ErrorCode;

/**
 * A SAX handler class for parsing a ".bmlconfig" file.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
/* package */ class PerTreeConfigSAXHandler extends DefaultHandler {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/
	
	/** The {@link PerTreeConfigFile} that this configuration applies to */
	private PerTreeConfigFile configFile = null;
	
	/** remember whether the starting &lt;bmlconfig&gt; tag has been seen */
	private boolean startSeen = false;
	
	/** The name of the &lt;alias&gt; we're currently processing */
	private String currentAlias = null;

	/** The list of &lt;package&gt; entries associated with the current &lt;alias&gt; */
	private ArrayList<String> currentAliasList;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Instantiate a new PerTreeConfigSAXHandler object, with a reference to the 
	 * {@link PerTreeConfigFile} to be populated.
	 * 
	 * @param configFile The {@link PerTreeConfigFile} to be populated.
	 */
	public PerTreeConfigSAXHandler(PerTreeConfigFile configFile) {
		this.configFile = configFile;
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Handle occurrences of an element start tag. This method is invoked by the SAX Parser
	 * whenever a new element start tag (e.g. &lt;alias&gt;) is identified. See the definition of
	 * import org.xml.sax.helpers.DefaultHandler for parameter details.
	 */
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {
	
		/* The first tag must be <bmlconfig> */
		if (!startSeen) {
			if (localName.equals("bmlconfig")) {
				startSeen = true;
			} else {
				throw new SAXException("Invalid file content. Must start with <bmlconfig>.");
			}
		}

		/* process the <alias> tag by recording the name and initializing data structures */
		else if (localName.equals("alias")) {
			currentAlias = atts.getValue("name");
			if (currentAlias == null) {
				throw new SAXException("Invalid file content. <alias> tag must have a 'name' attribute.");
			}
			currentAliasList = new ArrayList<String>();
		}
		
		/* process the <package> tag by adding the package name to our list. */
		else if (localName.equals("package")) {
			String pkgName = atts.getValue("name");
			if (currentAlias == null) {
				throw new SAXException("Invalid file content. <package> tag must have a 'name' attribute.");
			}
			currentAliasList.add(pkgName);
		}
		
		/* else, error */
		else {
			throw new SAXException("Invalid file content. Unrecognized tag: <" + localName + ">");
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Handle occurrences of an element end tag. This method is invoked by the SAX Parser
	 * whenever a new element end tag (e.g. &lt;/alias&gt;) is identified. See the definition of
	 * import org.xml.sax.helpers.DefaultHandler for parameter details.
	 */
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		
		/* if we see </alias>, proceed to add the current alias definition */
		if (localName.equals("alias")) {
			
			if (currentAlias == null) {
				throw new SAXException("Invalid file content. Unexpected </alias>");
			}
			String packages[] = currentAliasList.toArray(new String[currentAliasList.size()]);
			int rc = configFile.addAlias(currentAlias, packages);
			
			/* 
			 * Check for invalid name, but that's all. Don't worry about package names that 
			 * aren't defined, since we don't want stale aliases to cause the whole loading
			 * to fail.
			 */
			if (rc == ErrorCode.INVALID_NAME) {
				throw new SAXException("Invalid file content. Invalid alias name: " + currentAlias);
			}
			
			/* reset, in preparation for next time we seen <alias> */
			currentAlias = null;
			currentAliasList = null;
		}
		
		/* else, if we see </package>, do nothing */
		else if (localName.equals("package")) {
			/* nothing to do - all done when <alias> is seen */
		}
		
		/* else, seeing </bmlconfig> means we're at the end of the file */
		else if (localName.equals("bmlconfig")) {
			/* nothing to do */
		}
		
		/* other things are an error */
		else {
			throw new SAXException("Invalid file content. Unrecognized tag: </" + localName + ">");
		}		
	}
	
	/*-------------------------------------------------------------------------------------*/
}
