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
import java.io.FileNotFoundException;
import java.io.IOException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.progress.UIJob;
import org.xml.sax.SAXException;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.model.BuildStore;
import com.buildml.scanner.FatalBuildScannerError;
import com.buildml.scanner.electricanno.ElectricAnnoScanner;
import com.buildml.utils.files.ProgressFileInputStreamListener;

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
		
		/* we're importing content into this .bml file */
		final String outputBmlFilePath = mainPage.getOutputPath();
		
		/* here's the Annotation file we're reading from */
		final String inputXmlFilePath = mainPage.getInputPath();
		
		/* 
		 * If the BuildStore is currently open in an editor, force it to save/close so
		 * we don't need to worry about concurrency problems.
		 */
		MainEditor bmlEditor = EclipsePartUtils.getOpenBmlEditor(new File(outputBmlFilePath));
		if (bmlEditor != null) {
			IWorkbenchPage page = EclipsePartUtils.getActiveWorkbenchPage();
			if (page != null) {
				page.closeEditor(bmlEditor, true);
			}
		}
		
		/* Freshly open the BuildStore (but just not in an editor) */
		BuildStore buildStore = EclipsePartUtils.getNewBuildStore(outputBmlFilePath);
		if (buildStore == null) {
			return false;
		}
		
		/*
		 * Start the background job that does the import. Once it's finished,
		 * the editor will be (re)opened so the user can see the content.
		 */
		final BuildStore importBuildStore = buildStore;
		Job importJob = new Job("Import ElectricAccelerator Annotation File") {
			
			/* the background job starts here... */
			@Override
			public IStatus run(final IProgressMonitor monitor) {
				IStatus status = Status.OK_STATUS;
				ElectricAnnoScanner eas = new ElectricAnnoScanner(importBuildStore);
				try {
					
					/* 
					 * Create a listener that will monitor/display our progress in parsing the file.
					 * This listener reports the percentage of work completed so far (as a percentage
					 * of the size of file it's reading). On the first call to "progress()", we
					 * start our progress monitor, then we call worked() on that monitor whenever
					 * the percentage increases.
					 */
					ProgressFileInputStreamListener listener = new ProgressFileInputStreamListener() {
						
						/** first time we've been called? */
						private boolean firstReport = true;
						
						/** the percentage complete, last time we were called */
						private int lastPercentage = 0;
						
						/**
						 * Progress has been made by the ElectricAnnoScanner.
						 */
						@Override
						public void progress(long current, long total, int percentage) {
							if (firstReport) {
								monitor.beginTask("Importing Annotation File...", 0);
								firstReport = false;
							}
							if (percentage != lastPercentage) {
								monitor.worked(percentage - lastPercentage);
								lastPercentage = percentage;
							}
						}
						
						/** the import is completely done */
						@Override
						public void done() {
							/* empty */
						}
					};

					/*
					 * Start the parse - the listener will update us as we go.
					 */
					eas.parse(inputXmlFilePath, listener);

				} catch (FileNotFoundException e) {
					AlertDialog.displayErrorDialog("Error During Import", 
							"ElectricAccelerator annotation file " + inputXmlFilePath + " not found.");
					status = Status.CANCEL_STATUS;
					
				} catch (IOException e) {
					AlertDialog.displayErrorDialog("Error During Import", 
							"I/O error while reading ElectricAccelerator annotation file " + 
										inputXmlFilePath + ".");
					status = Status.CANCEL_STATUS;
					
				} catch (SAXException e) {
					AlertDialog.displayErrorDialog("Error During Import", 
							"Unexpected syntax in ElectricAccelerator annotation file " + 
									inputXmlFilePath + ".");
					status = Status.CANCEL_STATUS;
					
				} catch (FatalBuildScannerError e) {
					AlertDialog.displayErrorDialog("Error During Import", 
							"Logic problem while scanning ElectricAccelerator annotation file " + 
									inputXmlFilePath + "\n" + e.getMessage());
					status = Status.CANCEL_STATUS;
				}
				
				/* we're done - close the BuildStore */
				monitor.done();
				importBuildStore.close();
								
				/* 
				 * Open the BuildStore in an editor (this must be done
				 * in the UI thread) so that the user can see what was just
				 * imported.
				 */
				UIJob openEditorJob = new UIJob("Open Editor") {
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						EclipsePartUtils.openNewEditor(outputBmlFilePath);						
						return Status.OK_STATUS;
					}
				};
				openEditorJob.schedule();
				return status;
			}
		};
		
		/* start up the progress monitor service so that it monitors the job */
		importJob.setUser(true);
		importJob.schedule();
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
