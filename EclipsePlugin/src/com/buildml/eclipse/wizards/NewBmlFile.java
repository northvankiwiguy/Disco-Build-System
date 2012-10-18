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

import java.io.FileNotFoundException;
import java.io.IOException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.model.BuildStore;
import com.buildml.model.errors.BuildStoreVersionException;

/**
 * This class provides Eclipse wizard functionality for creating a new empty BuildML
 * file.
 */
public class NewBmlFile extends Wizard implements INewWizard {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/**
	 * The one-and-only UI wizard page that'll request the user to enter the new file name,
	 * and the file's parent container.
	 */
	private NewBmlFilePage page;
	
	/**
	 * The resource that was selected in the Eclipse UI, when the "new" operation was
	 * invoked. If valid, this is used as the parent project/folder for the new file.
	 */
	private ISelection selection;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new NewBmlFile wizard, which is invoke with the user selects
	 * the "New->BuildML->BuildML File" option.
	 */
	public NewBmlFile() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Add the single wizard page to this wizard.
	 */
	@Override
	public void addPages() {
		page = new NewBmlFilePage(selection);
		addPage(page);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * We will accept the selection in the workbench to see if we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * This method is called when 'Finish' button is pressed in the wizard. We will create
	 * an operation and run it using wizard as execution context.
	 */
	public boolean performFinish() {
		
		/* 
		 * Fetch the "parent container" (project/folder) and the new file name that the
		 * user entered in the wizard page.
		 */
		final String containerName = page.getContainerName();
		final String fileName = page.getFileName();
		
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(containerName));
		if ((resource == null) || !resource.exists() || !(resource instanceof IContainer)) {
			AlertDialog.displayErrorDialog("Error", 
					"Parent project/folder \"" + containerName + "\" does not exist.");
		}
		
		/* check that the file doesn't already exist */
		IContainer container = (IContainer) resource;
		final IFile file = container.getFile(new Path(fileName));
		if (file.exists()) {
			AlertDialog.displayErrorDialog("Error", 
					"The BuildML file " + fileName + " already exists.");			
		}
		
		/* try to create the file, as a new BuildStore database. */
		IPath location = file.getLocation();
		if (location != null) {
			try {
				new BuildStore(location.toOSString());
			} catch (FileNotFoundException e) {
				AlertDialog.displayErrorDialog("Error", 
						"A problem occurred while creating the new file: " + fileName);
			} catch (IOException e) {
					AlertDialog.displayErrorDialog("Error", 
							"A I/O error occurred while creating the new file: " + fileName);
			} catch (BuildStoreVersionException e) {
				AlertDialog.displayErrorDialog("Error", 
						"BuildML file has an incompatible version: " + fileName);
			}			
		}

		/* refresh the container, so the new file appears in the package explorer */
		try {
			container.refreshLocal(1, null);
		} catch (CoreException e1) {
			/* empty */
		}

		/*
		 * Open the new file in an appropriate editor.
		 */
		getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page =
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					IDE.openEditor(page, file, true);
				} catch (PartInitException e) {
				}
			}
		});
		
		return true;
	}

	/*-------------------------------------------------------------------------------------*/
}