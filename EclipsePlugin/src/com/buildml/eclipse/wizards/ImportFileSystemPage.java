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

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import com.buildml.eclipse.utils.fieldeditors.WorkspaceDirSelectFieldEditor;

/**
 * An Eclipse import wizard page, for importing a file system hierarchy
 * into a BuildStore. Most of the dialog functionality is in the ImportToBuildStorePage
 * class.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class ImportFileSystemPage extends ImportToBuildStorePage implements IPropertyChangeListener {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** Flag indicating whether the current "import directory" content is valid */
	private boolean isValid = false;
	
	/** Textual instructions, to be shown near the top of the wizard page. */
	private static String instructions = "The workspace directory will be scanned, with " +
			"the files and directories being inserted into the selected " +
			"BuildML (.bml) file.";

	/** The directory browser widget */
	private WorkspaceDirSelectFieldEditor directoryFieldEditor;

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
		setDescription("Select the source directory and the destination BuildML file.");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the path of the directory hierarchy that the user has selected. This method 
	 * should be called once the "finish" button has been pressed.
	 * @return The input directory's full path.
	 */
	public String getDirectoryPath()
	{
		return directoryFieldEditor.getAbsoluteDirectoryPath();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Called whenever the user enters text into the "import directory" field. This method
	 * updates the status after determining whether the directory path is valid.
	 */
	public void propertyChange(PropertyChangeEvent event) {

		String message = null;

		/* determine if the current field entry refers to a directory */
		String dirName = directoryFieldEditor.getStringValue();
		IResource container = directoryFieldEditor.getResource();

		if (dirName.length() == 0) {
			message = "Import project/folder must be specified.";
		} else if ((container == null) || 
				((!dirName.equals("/")) && 
				(container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0)) {
			message = "Import project/folder doesn't exist.";
		}
		
		setErrorMessage(message);
		isValid = (message == null);
		
		/* re-evaluate whether the "finish" button should be highlighted */
		contentChanged();
	}
	
	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/

	/**
	 * Create our input fields, which consist entirely of the directory hierarchy's path.
	 * @param parent The group box we're adding the fields into.
	 */
	protected void createInputFields(Composite parent) {

		Composite container = new Composite(parent, SWT.NULL);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		container.setLayoutData(gd);
		directoryFieldEditor = 
				new WorkspaceDirSelectFieldEditor("Directory", "Import Directory: ", container);
		directoryFieldEditor.setPropertyChangeListener(this);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether the input field contains valid content. 
	 * @return true if the fields are valid, else false.
	 */
	@Override
	protected boolean isInputValid() {
		return isValid;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
