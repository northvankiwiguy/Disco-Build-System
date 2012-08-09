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
 * Provides an Eclipse wizard for importing an ElectricAccelerator annotation file
 * into a user-specified BuildStore.
 * 
 * @author Peter Smith <psmith@arapiki.com>.
 */
public class ImportEaAnno extends Wizard implements IImportWizard {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** The one-and-only wizard page */
	private ImportEaAnnoPage mainPage;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new ImportEaAnno object, which provides a UI wizard to allow the user
	 * to import an ElectricAccelerator file into the BuildStore.
	 */
	public ImportEaAnno() {
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
		String inputXmlFilePath = mainPage.getInputPath();

		System.out.println("Importing from " + inputXmlFilePath + " into " + outputBmlFilePath);
        return true;
	}
	 
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Initialize our wizards, adding a new UI page for the user to fill out the fields. 
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import from an ElectricAccelerator Annotation File.");
		setNeedsProgressMonitor(true);
		mainPage = new ImportEaAnnoPage("Import ElectricAccelerator Annotation File.");
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
