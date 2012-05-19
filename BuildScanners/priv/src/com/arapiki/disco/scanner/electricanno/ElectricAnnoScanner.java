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

package com.arapiki.disco.scanner.electricanno;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.arapiki.disco.model.BuildStore;
import com.buildml.utils.files.ProgressFileInputStream;
import com.buildml.utils.files.ProgressFileInputStreamListener;

/**
 * Class for reading an Electric Accelerator annotation file, and populating a 
 * BuildStore database from the build data it contains.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class ElectricAnnoScanner {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/** The BuildStore object to which the annotation data will be added. */
	private BuildStore buildStore;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new ElectricAnnoScanner object, which will add data to the specified BuildStore.
	 * 
	 * @param buildStore The BuildStore to add the EC Annotation data into.
	 */
	public ElectricAnnoScanner(BuildStore buildStore) {
		this.buildStore = buildStore;
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Parse and store the content of the specified Electric Accelerator annotation file.
	 * 
	 * @param annoFileName The Electric Accelerator annotation file.
	 * @param listener The callback to receive progress reports.
	 * @throws IOException Something when wrong as the file was being read.
	 * @throws FileNotFoundException The annotation file couldn't be found.
	 * @throws SAXException The XML structure of this file is incorrect.
	 */
	public void parse(String annoFileName, ProgressFileInputStreamListener listener) 
		throws FileNotFoundException, IOException, SAXException {
				
		/* Open the file for input, and report progress every two seconds */
		FileInputStream in = new ProgressFileInputStream(annoFileName, listener, 2);

		/*
		 * Create a new XMLReader to parse this file, then set the ContentHandler
		 * to our own SAX handler class.
		 */
		XMLReader parser = XMLReaderFactory.createXMLReader();
		ContentHandler contentHandler = new ElectricAnnoSAXHandler(buildStore);
		parser.setContentHandler(contentHandler);
		
		/* 
		 * We want to ignore the *.dtd file mentioned in this *.xml file by returning
		 * the null String whenever a URL is resolved. The has the effect of allowing
		 * us to parse the *.xml file without validating the XML structure. We override
		 * the resolver's "resolveEntity" method with something that returns an empty file.
		 */
		parser.setEntityResolver(new EntityResolver() {
			@Override
			public InputSource resolveEntity(String publicId, String systemId)
					throws SAXException, IOException {
				return new InputSource(
						new ByteArrayInputStream("<?xml version='1.0' encoding='UTF-8'?>".getBytes()));
			}
		});

		/* 
		 * Read the *.xml file and process the content. This could take a long
		 * time, so put the database in fast mode.
		 */
		buildStore.setFastAccessMode(true);
		try {
			parser.parse(new InputSource(in));
		} finally {
			buildStore.setFastAccessMode(false);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

}
