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
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * An Eclipse import wizard page, for importing a legacy build process into a BuildStore. 
 * Most of the dialog functionality is in the ImportToBuildStorePage class.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class ImportLegacyBuildPage extends ImportToBuildStorePage {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The widget for selecting the directory the build command should be executed in */
	protected DirectoryFieldEditor inputDirectory;

	/** The Text widget for entering the legacy shell command */
	private Text inputCommand;
	
	/** The textual shell command that the user has entered */
	private String inputCommandString;
	
	/** The textual path of the user-selected directory */
	private String inputPath;
		
	/**
	 * Textual instructions, to be shown near the top of the wizard page.
	 */
	private static String instructions = "The legacy build process will be executed and " +
			"parsed, with the files and actions being inserted into the selected " +
			"BuildML (.bml) file.";

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new ImportLegacyBuildPage, which serves as the wizard dialog page for importing
	 * a legacy build process into a BuildML .bml file.
	 * @param pageName
	 */
	public ImportLegacyBuildPage(String pageName) {
		super(pageName, instructions);
		setDescription("Select the necessary shell command(s), the directory in which " +
						"the command(s) should be executed, and the destination BuildML file.");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the textual path of the directory that the user has selected. This method 
	 * should be called once the "finish" button has been pressed.
	 * @return The input file's full path.
	 */
	public String getInputPath()
	{
		return inputPath;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the textual shell command that the user has entered. This method should be 
	 * called once the "finish" button has been pressed.
	 * @return The shell command's textual content.
	 */
	public String getInputCommand()
	{
		return inputCommandString;
	}
	
	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/

	/**
	 * Create our input fields, which consist of the shell commands to execute the legacy
	 * build, along with the directory in which those commands will be executed.
	 * @param parent The group box we're adding the fields into.
	 */
	protected void createInputFields(Composite parent) {
		
		/* first, the text entry box for the shell command (1/8th of the screen height) */
		new Label(parent, SWT.NONE).setText("Enter Shell Command(s) for Legacy Build:");
		inputCommand = new Text(parent, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		GridData inputCommandData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		inputCommandData.heightHint = Display.getCurrent().getClientArea().height / 8;
		inputCommand.setLayoutData(inputCommandData);
		addTextValidator(inputCommand);
		
		/* second, the directory in which the command should be executed */
		Composite inputDirectoryComposite = new Composite(parent, SWT.NONE);
		inputDirectoryComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		inputDirectory = new DirectoryFieldEditor("inputDirectory", 
				"Select Execution Directory: ", inputDirectoryComposite);
		addTextValidator(inputDirectory.getTextControl(inputDirectoryComposite));
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether the input field contains valid content. The only check we do is
	 * whether the directory exists and that the shell command has non-empty content.
	 * 
	 * @return true if the fields are valid, else false.
	 */
	@Override
	protected boolean isInputValid() {
		inputPath = inputDirectory.getStringValue();
		inputCommandString = inputCommand.getText();
		return (!inputCommandString.isEmpty()) && new File(inputPath).isDirectory();
	}
	
	/*-------------------------------------------------------------------------------------*/
}
