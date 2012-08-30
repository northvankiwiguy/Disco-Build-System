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

package com.buildml.eclipse.wizards;

import java.io.File;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

/**
 * An Eclipse import wizard page, for importing a file system hierarchy
 * into a BuildStore. Most of the dialog functionality is in the ImportToBuildStorePage
 * class.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class ImportFileSystemPage extends ImportToBuildStorePage {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** The UI field for entering the directory hierarchy */
	protected DirectoryFieldEditor inputFile;

	/** The textual path of the user-selected directory path */
	private String inputPath;
	
	/**
	 * Textual instructions, to be shown near the top of the wizard page.
	 */
	private static String instructions = "The file system directory will be scanned, with " +
			" the files and directories being inserted into the selected " +
			"BuildML (.bml) file.";

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new ImportFileSystemPage, which serves as the wizard dialog page for importing
	 * a directory hierarchy into a BuildML .bml file.
	 * @param pageName Title of the dialog box.
	 * @param selection Resource(s) selected when "import" was invoked.
	 */
	public ImportFileSystemPage(String pageName, ISelection selection) {
		super(pageName, instructions, selection);
		setDescription("Select the source directory hierarchy and the destination BuildML file.");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the path of the directory hierarchy that the user has selected. This method 
	 * should be called once the "finish" button has been pressed.
	 * @return The input directory's full path.
	 */
	public String getDirectoryPath()
	{
		return inputPath;
	}
	
	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/

	/**
	 * Create our input fields, which consist entirely of the directory hierarchy's path.
	 * @param parent The group box we're adding the fields into.
	 */
	protected void createInputFields(Composite parent) {
		Composite inputFileComposite = new Composite(parent, SWT.NONE);
		inputFileComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		inputFile = new DirectoryFieldEditor("inputDirectory", "Select Directory Hierarchy: ", 
										inputFileComposite);
		addTextValidator(inputFile.getTextControl(inputFileComposite));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether the input field contains valid content. The only check we do is
	 * whether the directory exists.
	 * 
	 * @return true if the fields are valid, else false.
	 */
	@Override
	protected boolean isInputValid() {
		inputPath = inputFile.getStringValue();
		return new File(inputPath).isDirectory();
	}
	
	/*-------------------------------------------------------------------------------------*/
}
