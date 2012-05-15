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

package com.arapiki.disco.eclipse.utils;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * A Dialog allowing the user to select a regular expression to match a file/task name.
 * The user may also determine whether the matching files/tasks should be added to,
 * or extracted from the current set of items displayed.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class NameFilterDialog extends TitleAreaDialog {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** A constant representing the radio button state: "add" */
	public static final int ADD_ITEMS = 1;

	/** A constant representing the radio button state: "remove" */
	public static final int REMOVE_ITEMS = 2;
	
	/** The text box into which the regular expression is entered. */
	private Text textBox = null;
	
	/**  The text, saved at the point the "OK" button is pressed */
	private String enteredText = null;
	
	/** The radio button state, saved at the point the "OK" button is pressed */
	private int radioButtonState;
	
	/** The radio button for "adding the matching items to the view" */
	private Button addChoice;
	
	/** The radio button for "removing the matching items from the view" */
	private Button removeChoice;
		
	/*=====================================================================================*
	 * CONSTRUCTOR
	 *=====================================================================================*/
	
	/**
	 * Create a new NameFilterDialog instance. This allows the user to enter a regular
	 * expression that filter's the name of something. 
	 */
	public NameFilterDialog() {
		super(new Shell());
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * @return The regular expression, as entered by the user.
	 */
	public String getRegularExpression() {
		return enteredText;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the select that the user made in the "add" or "remove" radio buttons.
	 * @return NameFilterDialog.ADD_ITEMS if "add" was selected, else NameFilterDialog.REMOVE_ITEMS.
	 */
	public int getAddRemoveChoice() {
		return radioButtonState;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		
		setTitle("Enter a Regular Expression to Match Items in the View:");
		
		/* create and format the top-level composite of this dialog */
		Composite composite = (Composite) super.createDialogArea(parent);

		/* Add a text entry box, that the user enters the expression into */
		textBox = new Text(composite, SWT.SINGLE | SWT.BORDER);
		textBox.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

		/* Add the radio buttons for "Add matching files" and "Remove matching files" */
		addChoice = new Button(composite, SWT.RADIO);
		addChoice.setText("Add Matching Items to the View.");
		addChoice.setSelection(true);
		GridData gridData = new GridData();
		gridData.verticalIndent = 10;
		addChoice.setLayoutData(gridData);
		removeChoice = new Button(composite, SWT.RADIO);
		removeChoice.setText("Remove Matching Items from the View.");
		
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
		
		/* save the state of the radio buttons */
		if (addChoice.getSelection()) {
			radioButtonState = ADD_ITEMS;
		} else {
			radioButtonState = REMOVE_ITEMS;
		}
		
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
		
		newShell.setText("Select Items by Name");
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
