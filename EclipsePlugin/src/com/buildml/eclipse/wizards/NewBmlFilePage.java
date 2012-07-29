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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

/**
 * Wizard page to support the "New->BuildML->BuildML File" option. Allows the user
 * to enter the file name, and the parent project/folder. This is invoked solely by
 * the NewBmlFile class.
 */
public class NewBmlFilePage extends WizardPage {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/**
	 * The name of the containing (parent) project or folder.
	 */
	private Text containerText;

	/**
	 * The name of the new file to be created.
	 */
	private Text fileText;

	/**
	 * The resource (project/folder) that was selected when the "new" operation was invoked.
	 */
	private ISelection selection;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new NewBmlFilePage object, used to create an empty BuildML 
	 * build store.
	 * 
	 * @param selection
	 */
	public NewBmlFilePage(ISelection selection) {
		super("wizardPage");
		setTitle("New BuildML File");
		setDescription("Create a new BuildML File (*.bml).");
		this.selection = selection;
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * @see WizardPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		Label label = new Label(container, SWT.NULL);
		label.setText("&Parent Folder:");

		containerText = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		containerText.setLayoutData(gd);
		containerText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		Button button = new Button(container, SWT.PUSH);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});
		label = new Label(container, SWT.NULL);
		label.setText("&File Name:");

		fileText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fileText.setLayoutData(gd);
		fileText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		initialize();
		dialogChanged();
		setControl(container);
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Tests if the current workbench selection is a suitable container (project/folder) 
	 * to use.
	 */
	private void initialize() {
		if (selection != null && selection.isEmpty() == false
				&& selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() > 1) {
				return;
			}
			Object obj = ssel.getFirstElement();
			if (obj instanceof IResource) {
				IContainer container;
				if (obj instanceof IContainer)
					container = (IContainer) obj;
				else
					container = ((IResource) obj).getParent();
				containerText.setText(container.getFullPath().toString());
			}
		}
		fileText.setText("build.bml");
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Uses the standard container selection dialog to choose the new value for
	 * the "parent" field.
	 */
	private void handleBrowse() {
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(
				getShell(), ResourcesPlugin.getWorkspace().getRoot(), false,
				"Select new project/folder into which the file will be created.");
		if (dialog.open() == ContainerSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				containerText.setText(((Path) result[0]).toString());
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Ensures that both text fields are set.
	 */
	private void dialogChanged() {
		IResource container = ResourcesPlugin.getWorkspace().getRoot()
				.findMember(new Path(getContainerName()));
		String fileName = getFileName();

		if (getContainerName().length() == 0) {
			updateStatus("Parent project/folder must be specified.");
			return;
		}
		if (container == null
				|| (container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0) {
			updateStatus("Parent project/folder doesn't exist.");
			return;
		}
		if (!container.isAccessible()) {
			updateStatus("Project isn't writable.");
			return;
		}
		if (fileName.length() == 0) {
			updateStatus("File name must be specified.");
			return;
		}
		if (fileName.replace('\\', '/').indexOf('/', 1) > 0) {
			updateStatus("File name isn't valid.");
			return;
		}
		int dotLoc = fileName.lastIndexOf('.');
		if (dotLoc != -1) {
			String ext = fileName.substring(dotLoc + 1);
			if (ext.equalsIgnoreCase("bml") == false) {
				updateStatus("File extension must be \"bml\".");
				return;
			}
		} else {
			updateStatus("File extension must be \"bml\".");
			return;
		}
		updateStatus(null);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Display a status message in the dialog box, to reflect the current state of the
	 * input.
	 * @param message The message to be displayed.
	 */
	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The parent project/folder name.
	 */
	public String getContainerName() {
		return containerText.getText();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The new file's name.
	 */
	public String getFileName() {
		return fileText.getText();
	}
	
	/*-------------------------------------------------------------------------------------*/
}