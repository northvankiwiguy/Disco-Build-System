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

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.fieldeditors.WorkspaceFileSelectFieldEditor;

/**
 * An abstract parent class for any Eclipse import wizard dialog pages that
 * take some type of input, and write content to a BuildML (.bml) BuildStore
 * file. Each subclass is expected to manage it's own "input source" fields,
 * but this class will manage the "output destination" fields (the name of
 * the BuildML .bml file).
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public abstract class ImportToBuildStorePage extends WizardPage {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** The field used to select the output file */
	private WorkspaceFileSelectFieldEditor outputFile;
	
	/** The textual name/path of the output file */
	private String outputPath;

	/** Instruction text, to be displayed near the top of the wizard page */
	private String instructions;
	
	/** The combo box from which existing (open) .bml files can be selected */
	private Combo bmlFileComboBox;
	
	/**
	 * The resources that were selected in the Eclipse UI, when the "import" operation was
	 * invoked. If valid, this is used as the .bml file to import into.
	 */
	private ISelection selection;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new ImportToBuildStorePage. Only subclasses may be instantiated.
	 * 
	 * @param pageName The title of this wizard page.
	 * @param instructions The textual instructions to be displayed near the top of the
	 * 			page. These guide the user on how to fill out the fields.
	 * @param selection Resource(s) selected when "import" was invoked.
	 */
	protected ImportToBuildStorePage(String pageName, String instructions, 
									 ISelection selection) {
		super(pageName);
		setTitle(pageName);
		this.instructions = instructions;
		this.selection = selection;
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Create the widgets for this wizard's dialog box. All import dialogs require the
	 * user to specify an "output" BuildML file (or select the name of a currently open
	 * editor that contains such a file). However, each subclass must provide its own set of
	 * input fields, depending on the goal of the import process (by overriding the 
	 * createInputFields() method).
	 * 
	 * @param parent The containing parent widget.
	 */
	@Override
	public void createControl(Composite parent) {
		
		/* The top-level composite has three columns */
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout compositeLayout = new GridLayout(3, false);
		compositeLayout.marginWidth = 20;
		compositeLayout.marginHeight = 20;
		compositeLayout.verticalSpacing = 20;
		composite.setLayout(compositeLayout);
		
		/* page is not complete until the fields are valid */
		setPageComplete(false);

		/*
		 * Show an introductory piece of text, providing instructions. This
		 * label also forces the wizard box to be 33% of the screen width.
		 */
		Label heading = new Label(composite, SWT.WRAP);
		GridData headingData = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
		headingData.widthHint = Display.getCurrent().getClientArea().width / 3;
		heading.setLayoutData(headingData);
		heading.setText(instructions);

		/*
		 * Provide a file input box, each subclass must provide its own set of input files,
		 * depending on the purpose of the wizard. The createInputFields() method does
		 * all of that work.
		 */
		Group inputGroup = new Group(composite, SWT.NONE);
		inputGroup.setText("Import Source:");
		inputGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
		GridLayout inputGroupLayout = new GridLayout();
		inputGroupLayout.marginHeight = 10;
		inputGroupLayout.marginWidth = 10;
		inputGroup.setLayout(inputGroupLayout);
		createInputFields(inputGroup);

		/*
		 * Select the destination BuildML file. Either by selecting an open editor from a combo box,
		 * or by browsing for the .bml file.
		 */
		Group outputGroup = new Group(composite, SWT.NONE);
		outputGroup.setText("Import Destination:");
		outputGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
		GridLayout outputGroupLayout = new GridLayout(2, false);
		outputGroupLayout.marginHeight = 10;
		outputGroupLayout.marginWidth = 10;
		outputGroup.setLayout(outputGroupLayout);

		/* Display list of open editors (if there are some) */
		final MainEditor openEditors[] = EclipsePartUtils.getOpenBmlEditors();
		boolean openEditorsExist = false;
		if ((openEditors != null) && (openEditors.length != 0)) {
			openEditorsExist = true;
			new Label(outputGroup, SWT.NONE).setText("Choose Open BuildML file:");
			bmlFileComboBox = new Combo(outputGroup, SWT.READ_ONLY | SWT.DROP_DOWN);
			bmlFileComboBox.add("");
			for (int i = 0; i < openEditors.length; i++) {
				String option = openEditors[i].getFile().toString();
				bmlFileComboBox.add(EclipsePartUtils.absoluteToWorkspaceRelativePath(option));
			}
			bmlFileComboBox.setLayoutData(
					new GridData(SWT.FILL, SWT.CENTER, true, false));
			
			/* 
			 * When the user selects from the combo box, update the text field with the
			 * workspace-relative path of the build.bml file.
			 */
			bmlFileComboBox.addModifyListener(new ModifyListener() {				
				@Override
				public void modifyText(ModifyEvent e) {
					int index = bmlFileComboBox.getSelectionIndex();
					if (index > 0) {
						String absPath = openEditors[index - 1].getFile().toString();
						String relPath = EclipsePartUtils.absoluteToWorkspaceRelativePath(absPath);
						if (relPath == null) {
							relPath = "";
						}
						outputFile.setStringValue(relPath);
					}
				}
			});
		}

		/* Or select the BuildML file via a file browser */
		Composite outputFileComposite = new Composite(outputGroup, SWT.NONE);
		outputFileComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		outputFile = new WorkspaceFileSelectFieldEditor("outputFile", 
				(openEditorsExist ? "or " : "") + "Browse BuildML Files: ", outputFileComposite,
				new String[] { "*.bml" });
		outputFile.getTextControl(outputFileComposite).addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				contentChanged();
			}
		});

		/*
		 * If there was a .bml file selected when the "import" operation was initiated, select
		 * that file as the default .bml file.
		 */
		if (selection != null && !selection.isEmpty() && 
				selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() == 1) {
				Object obj = ssel.getFirstElement();
				if (obj instanceof IFile) {
					IFile file = (IFile)obj;
					String relativePath = file.getFullPath().toString();
					if (relativePath.endsWith(".bml")) {
						outputFile.setStringValue(relativePath);
						if (bmlFileComboBox != null) {
							bmlFileComboBox.setText(relativePath);
						}
					}
				}
			}
		}

		/* done */
		setControl(composite);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the absolute path of the output file (with .bml extension) that the user has
	 * selected. This method should be called once the "finish" button has been pressed.
	 * @return The output file's full path.
	 */
	public String getOutputPath()
	{
		return EclipsePartUtils.workspaceRelativeToAbsolutePath(outputPath);
	}

	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/

	/**
	 * Each subclass should implement this method, to create the widgets that appear in
	 * the "input source" box.
	 * 
	 * @param parent The "input source" group box (a composite that all widgets should be
	 * 				added within).
	 */
	protected abstract void createInputFields(Composite parent);

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Each subclass should implement this method, to determine whether the input fields
	 * (which are supplied and managed by the subclass) contain valid content, and we
	 * should therefore enable the "Finish" button.
	 * 
	 * @return true if the input fields are valid, else false.
	 */
	protected abstract boolean isInputValid();
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * This method should be called after any input field is modified (including those
	 * added by sub-classes). The goal is to determine whether the current combination of
	 * input fields is valid.
	 */
	protected void contentChanged() {
		outputPath = ImportToBuildStorePage.this.outputFile.getStringValue();
		
		ImportToBuildStorePage.this.setPageComplete(
				outputPath.endsWith(".bml") &&
				new File(EclipsePartUtils.workspaceRelativeToAbsolutePath(outputPath)).isFile() &&
				isInputValid());	
	}

	/*-------------------------------------------------------------------------------------*/
}
