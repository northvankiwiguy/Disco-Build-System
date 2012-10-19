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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.progress.UIJob;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.model.IBuildStore;
import com.buildml.scanner.buildtree.FileSystemScanner;

/**
 * Provides an Eclipse wizard for importing a directory hierarchy into a BuildML file.
 * 
 * @author Peter Smith <psmith@arapiki.com>.
 */
public class ImportFileSystem extends Wizard implements IImportWizard {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** The one-and-only wizard page */
	private ImportFileSystemPage mainPage;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new ImportFileSystem object, which provides a UI wizard to allow the user
	 * to import a directory hierarchy into the BuildStore.
	 */
	public ImportFileSystem() {
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
		IBuildStore buildStore = null;
		
		/* This is the BuildStore we'll update */
		final String outputBmlFilePath = mainPage.getOutputPath();
		
		/* This is the root of the directory hierarchy we'll traverse */
		final String inputDirectoryPath = mainPage.getDirectoryPath();

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
		buildStore = EclipsePartUtils.getNewBuildStore(outputBmlFilePath);
		if (buildStore == null) {
			return false;
		}
		
		/*
		 * Start the background job that does the import. Once it's finished,
		 * the editor will be (re)opened so the user can see the content.
		 */
		final IBuildStore importBuildStore = buildStore;
		final FileSystemScanner fss = new FileSystemScanner(importBuildStore);
		Job importJob = new Job("Import Files and Directories") {
			@Override
			public IStatus run(final IProgressMonitor monitor) {
				monitor.beginTask("Importing Files and Directories...",  
									IProgressMonitor.UNKNOWN);
				
				/* perform the import */
				fss.scanForFiles("root", inputDirectoryPath);
				
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
		setWindowTitle("Import a File System Directory Hierarchy.");
		setNeedsProgressMonitor(true);
		mainPage = new ImportFileSystemPage("Import Directory Hierarchy.", selection);
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
