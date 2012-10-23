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

package com.buildml.eclipse.utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * A Dialog allowing the user to select the name of a path root to be added.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class RootNameDialog extends BmlTitleAreaDialog {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The text box into which the root name is entered. */
	private Text textBox = null;
	
	/**  The text, saved at the point the "OK" button is pressed */
	private String enteredText = null;

	/*=====================================================================================*
	 * CONSTRUCTOR
	 *=====================================================================================*/
	
	/**
	 * Create a new RootNameDialog instance. 
	 */
	public RootNameDialog() {
		super(new Shell());		
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * @return The root name, as entered by the user.
	 */
	public String getRootName() {
		return enteredText;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		
		setTitle("Enter the name of path root to associated with the selected directory:");
		setHelpAvailable(false);
		
		/* create and format the top-level composite of this dialog */
		Composite composite = new Composite(
				(Composite)super.createDialogArea(parent), SWT.None);
		composite.setLayout(new GridLayout());
		GridData compositeGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(compositeGridData);

		/* Add a text entry box, that the user enters the expression into */
		textBox = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridData gridData = new GridData(SWT.FILL, SWT.NONE, true, false);
		gridData.verticalIndent = 10;
		textBox.setLayoutData(gridData);

		/* un-disable the OK button, once some text is entered */
		textBox.addModifyListener(new ModifyListener() {	
			public void modifyText(ModifyEvent e) {
				getButton(OK).setEnabled(!textBox.getText().isEmpty());
			}
		});
		
		textBox.setFocus();
		return composite;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		
		/* save the state of the dialog, so our creator can access it later */
		enteredText = textBox.getText();
		
		/* now dispose the window */
		super.okPressed();
	}

	/*-------------------------------------------------------------------------------------*/

	/* 
	 * (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		
		newShell.setText("Select Path Root Name");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		
		/* disable the OK button by default, until some text is entered */
		getButton(OK).setEnabled(false);
	}
	
	/*-------------------------------------------------------------------------------------*/
}
