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
import java.io.IOException;
import java.io.PrintStream;

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

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.model.impl.BuildStore;
import com.buildml.scanner.legacy.LegacyBuildScanner;
import com.buildml.utils.string.ShellCommandUtils;

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
		
		/* This is the BuildStore we'll update */
		final String outputBmlFilePath = mainPage.getOutputPath();
		
		/* This is the root of the directory hierarchy we'll traverse */
		final String inputDirectory = mainPage.getInputPath();
		
		/* This is the build command we'll execute */
		final String inputCommand = 
				ShellCommandUtils.joinCommandLine(mainPage.getInputCommand());
		
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
		final BuildStore importBuildStore = EclipsePartUtils.getNewBuildStore(outputBmlFilePath);
		if (importBuildStore == null) {
			return false;
		}
		
		/* Start the import process */
		Job importJob = new Job("Import a Legacy Build Process") {
			@Override
			public IStatus run(final IProgressMonitor monitor) {
				
				PrintStream outStream = EclipsePartUtils.getConsolePrintStream("BuildML Import");
				
				monitor.beginTask("Executing Legacy Build Process...",  
									IProgressMonitor.UNKNOWN);
				
				LegacyBuildScanner lbs = new LegacyBuildScanner();
				lbs.setBuildStore(importBuildStore);
				lbs.setTraceFile(new File(inputDirectory, "cfs.trace").toString());

				try {
					lbs.traceShellCommand(new String[] {inputCommand}, new File(inputDirectory), 
							outStream, true);

				} catch (Exception e) {
					AlertDialog.displayErrorDialog("Import Failed", "For some reason, the legacy build process " +
							"couldn't be imported. The .bml file has not be modified.");
					monitor.done();
					importBuildStore.close();
					return Status.CANCEL_STATUS;
				}
				
				monitor.beginTask("Importing Files and Actions into BuildML File...",  
						IProgressMonitor.UNKNOWN);

				/*
				 * Parse the trace file. This only reason this could fail is if something got lost/corrupted,
				 * rather than due to a user error. On failure, a FatalBuildStoreError will be thrown
				 * and then displayed by Eclipse.
				 */
				lbs.parseTraceFile();
								
				/* we're done - close up */
				monitor.done();
				try {
					importBuildStore.save();
				} catch (IOException e) {
					AlertDialog.displayErrorDialog("Import Failed", 
							"The build database could not be closed. " + e.getMessage());
				}
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
				return Status.OK_STATUS;
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
		setWindowTitle("Import a Legacy Build Process.");
		setNeedsProgressMonitor(true);
		mainPage = new ImportLegacyBuildPage("Import Legacy Build Process.", selection);
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
