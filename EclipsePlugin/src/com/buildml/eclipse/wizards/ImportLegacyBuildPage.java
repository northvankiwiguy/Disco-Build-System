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

import org.eclipse.core.resources.IResource;import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import com.buildml.eclipse.utils.fieldeditors.WorkspaceDirSelectFieldEditor;

/**
 * An Eclipse import wizard page, for importing a legacy build process into a BuildStore. 
 * Most of the dialog functionality is in the ImportToBuildStorePage class.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class ImportLegacyBuildPage extends ImportToBuildStorePage implements IPropertyChangeListener {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The widget for selecting the directory the build command should be executed in */
	protected DirectoryFieldEditor inputDirectory;

	/** The Text widget for entering the legacy shell command */
	private Text inputCommand;
	
	/** The textual shell command that the user has entered */
	private String inputCommandString;
	
	/** Flag indicating whether the current "import directory" content is valid */
	private boolean isDirectoryValid = false;
		
	/** The directory browser widget */
	private WorkspaceDirSelectFieldEditor directoryFieldEditor;
	
	/** Textual instructions, to be shown near the top of the wizard page. */
	private static String instructions = "The legacy build process will be executed and " +
			"parsed, with the files and actions being inserted into the selected " +
			"BuildML (.bml) file.";

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new ImportLegacyBuildPage, which serves as the wizard dialog page for importing
	 * a legacy build process into a BuildML .bml file.
	 * @param pageName Title of the dialog box.
	 * @param selection Resource(s) selected when "import" was invoked.
	 */
	public ImportLegacyBuildPage(String pageName, ISelection selection) {
		super(pageName, instructions, selection);
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
		return directoryFieldEditor.getAbsoluteDirectoryPath();
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

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Called whenever the user enters text into the "execution directory" field. This method
	 * updates the status after determining whether the directory path is valid.
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		
		String message = null;
		String dirName = directoryFieldEditor.getAbsoluteDirectoryPath();
		IResource container = directoryFieldEditor.getResource();

		/* determine if it's a valid directory */
		if (dirName.length() == 0) {
			message = "Import project/folder must be specified.";
		} else if ((container == null) || 
				((!dirName.equals("/")) && 
				(container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0)) {
			message = "Import project/folder doesn't exist.";
		}
		
		setErrorMessage(message);
		isDirectoryValid = (message == null);
		
		/* re-evaluate whether the "finish" button should be highlighted */
		contentChanged();
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
		Composite container = new Composite(parent, SWT.NULL);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		container.setLayoutData(gd);
		directoryFieldEditor = 
				new WorkspaceDirSelectFieldEditor("Directory", "Execution Directory: ", container);
		directoryFieldEditor.setPropertyChangeListener(this);
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
		inputCommandString = inputCommand.getText();
		return (!inputCommandString.isEmpty()) && isDirectoryValid;
	}

	/*-------------------------------------------------------------------------------------*/
}
