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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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
	
	/** A constant representing the radio button state: "select only matching items." */
	public static final int SELECT_ONLY_MATCHING_ITEMS = 0;

	/** A constant representing the radio button state: "add matching items to existing." */
	public static final int ADD_MATCHING_ITEMS = 1;

	/** A constant representing the radio button state: "remove matching items from existing." */
	public static final int REMOVE_MATCHING_ITEMS = 2;

	/** A constant representing the radio button state: "select all items" */
	public static final int SELECT_ALL_ITEMS = 3;

	/** A constant representing the radio button state: "deselect all items" */
	public static final int DESELECT_ALL_ITEMS = 4;

	/** The text box into which the regular expression is entered. */
	private Text textBox = null;
	
	/**  The text, saved at the point the "OK" button is pressed */
	private String enteredText = null;
	
	/** The radio button state, saved at the point the "OK" button is pressed */
	private int radioButtonState = SELECT_ONLY_MATCHING_ITEMS;
	
	/** The string name of the type of item being selected (e.g. "files" or "actions"). */
	private String itemType;
	
	/** The version of itemType with the first letter capitalized. (e.g. "Files" or "Actions") */
	private String upperItemType;

	/*=====================================================================================*
	 * CONSTRUCTOR
	 *=====================================================================================*/
	
	/**
	 * Create a new NameFilterDialog instance. This allows the user to enter a regular
	 * expression to filter the name of something.
	 * @param itemType The name of the item being selected (e.g. "files" or "actions").
	 */
	public NameFilterDialog(String itemType) {
		super(new Shell());
		
		this.itemType = itemType;
		this.upperItemType = itemType.substring(0, 1).toUpperCase() + 
								itemType.substring(1);
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
		
		setTitle("Enter a pattern to select " + itemType + " (using * to match multiple characters):");
		setHelpAvailable(false);
		
		/* create and format the top-level composite of this dialog */
		Composite composite = new Composite(
				(Composite)super.createDialogArea(parent), SWT.None);
		composite.setLayout(new GridLayout());
		GridData compositeGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(compositeGridData);

		/*
		 * Add all the radio buttons, for the various ways to select data.
		 */
		Button firstButton = addRadioOption(composite, true, SELECT_ONLY_MATCHING_ITEMS,
						"Select only the " + itemType + " that match this pattern.");
		addRadioOption(composite, true, ADD_MATCHING_ITEMS, 
						"Add matching " + itemType + " to the existing selection.");
		addRadioOption(composite, true, REMOVE_MATCHING_ITEMS,
						"Remove matching " + itemType + " from the existing selection.");
		addRadioOption(composite, false, SELECT_ALL_ITEMS,
						"Add all " + itemType + " to the selection.");
		addRadioOption(composite, false, DESELECT_ALL_ITEMS,
						"Remove all " + itemType + " from the selection.");
		
		/* Add a text entry box, that the user enters the expression into */
		textBox = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridData gridData = new GridData(SWT.FILL, SWT.NONE, true, false);
		gridData.verticalIndent = 10;
		textBox.setLayoutData(gridData);

		/* fix the vertical indent for the first item in our list of radio buttons */
		firstButton.setSelection(true);
		gridData = (GridData)firstButton.getLayoutData();
		gridData.verticalIndent = 10;

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
		
		newShell.setText("Select " + upperItemType + " to Display in Editor.");
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Select", true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		
		/* disable the OK button by default, until some text is entered */
		getButton(OK).setEnabled(false);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create a new radio-style button, with the associated text label. When the radio
	 * button is selected, that specific option will be set as the value to be returned
	 * by getAddRemoveChoice().
	 * 
	 * @param composite The parent widget we're adding the radio button to.
	 * @param requiresTextBox True if the text box is required for this option, else false.
	 * @param selectedButtonIndex The value to be returned by getAddRemoveChoice().
	 * @param label The textual label to display next to the radio button.
	 * @return The newly created Button widget.
	 */
	private Button addRadioOption(Composite composite, final boolean requiresTextBox, 
								  final int selectedButtonIndex, String label) {
		final Button newButton = new Button(composite, SWT.RADIO);
		newButton.setText(label);
		GridData gridData = new GridData();
		newButton.setLayoutData(gridData);
		
		/* 
		 * Add a listener for this radio button. Depending on the radio button chosen,
		 * we may (or may not) want to enable the text box entry.
		 */
		newButton.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				/* was the radio button recently selected? */
				if (newButton.getSelection()) {
					radioButtonState = selectedButtonIndex;
					
					/* does this option require a (non-empty) text box field? */
					if (requiresTextBox) {
						textBox.setEnabled(true);
						getButton(OK).setEnabled(!textBox.getText().isEmpty());
					}
					
					/* or is no text box input required? */
					else {
						textBox.setEnabled(false);
						getButton(OK).setEnabled(true);
					}
				}
			}
		});
		
		return newButton;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
