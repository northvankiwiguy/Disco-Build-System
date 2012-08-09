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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

/**
 * Provides an Eclipse wizard for importing a legacy build process into a 
 * user-specified BuildStore.
 * 
 * @author Peter Smith <psmith@arapiki.com>.
 */
public class ImportLegacyBuild extends Wizard implements IImportWizard {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** The one-and-only wizard page */
	private ImportLegacyBuildPage mainPage;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new ImportLegacyBuild object, which provides a UI wizard to allow the user
	 * to import a legacy build into the BuildStore.
	 */
	public ImportLegacyBuild() {
		super();
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Perform the operation that happens when the user presses "Finish".
	 * @return true if the action was accepted, else false.
	 */	
	public boolean performFinish() {
		// TODO: finish this.
		String outputBmlFilePath = mainPage.getOutputPath();
		String inputDirectory = mainPage.getInputPath();
		String inputCommand = mainPage.getInputCommand();

		System.out.println("Running the command " + inputCommand + " in directory " +
				inputDirectory + " and importing into " + outputBmlFilePath);
        return true;
	}
	 
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Initialize our wizards, adding a new UI page for the user to fill out the fields. 
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import a Legacy Build Process.");
		setNeedsProgressMonitor(true);
		mainPage = new ImportLegacyBuildPage("Import Legacy Build Process.");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add the single wizard page to this wizard.
	 */
    public void addPages() {
        super.addPages(); 
        addPage(mainPage);        
    }

    /*-------------------------------------------------------------------------------------*/
}
