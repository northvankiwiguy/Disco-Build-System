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

import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

/**
 * An Eclipse import wizard page, for importing an ElectricAccelerator annotation file
 * into a BuildStore. Most of the dialog functionality is in the ImportToBuildStorePage
 * class.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class ImportEaAnnoPage extends ImportToBuildStorePage {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** The file name of the ElectricAccelerator annotation file */
	protected FileFieldEditor inputFile;

	/** The textual path of the user-selected input (.xml) file */
	private String inputPath;
	
	/**
	 * Textual instructions, to be shown near the top of the wizard page.
	 */
	private static String instructions = "The ElectricAccelerator annotation file (.xml) will " +
			"be parsed, with the files and actions being inserted into the selected " +
			"BuildML (.bml) file.";

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new ImportEaAnnoPage, which serves as the wizard dialog page for importing
	 * from an ElectricAccelerator annotation file, into a BuildML .bml file.
	 * @param pageName Title of the dialog box.
	 * @param selection Resource(s) selected when "import" was invoked.
	 */
	public ImportEaAnnoPage(String pageName, ISelection selection) {
		super(pageName, instructions, selection);
		setDescription("Select the source Annotation file and the destination BuildML file.");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the path of the input file (with .xml extension) that the user has
	 * selected. This method should be called once the "finish" button has been pressed.
	 * @return The input file's full path.
	 */
	public String getInputPath()
	{
		return inputPath;
	}
	
	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/

	/**
	 * Create our input fields, which consist entirely of the annotation file's name.
	 * @param parent The group box we're adding the fields into.
	 */
	protected void createInputFields(Composite parent) {
		Composite inputFileComposite = new Composite(parent, SWT.NONE);
		inputFileComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		inputFile = new FileFieldEditor("inputFile", "Select an Annotation File: ", 
										inputFileComposite);
		inputFile.setFileExtensions(new String[] { "*.xml" });
		inputFile.getTextControl(inputFileComposite).addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				contentChanged();
			}
		});
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether the input field contains valid content. The only check we do is
	 * whether the file exists.
	 * 
	 * @return true if the fields are valid, else false.
	 */
	@Override
	protected boolean isInputValid() {
		inputPath = inputFile.getStringValue();
		return inputPath.endsWith(".xml") && new File(inputPath).isFile();
	}
	
	/*-------------------------------------------------------------------------------------*/
}
