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

package com.buildml.eclipse;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * A Dialog allowing the user to change the name of the currently selected editor tab.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class EditorTabNameChangeDialog extends TitleAreaDialog {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** The text box into which the name is entered. */
	private Text textBox = null;
	
	/**  
	 * The initial text box content, as well as the text saved when the "OK" button
	 * is pressed.
	 */
	private String enteredText = "";
		
	/*=====================================================================================*
	 * CONSTRUCTOR
	 *=====================================================================================*/
	
	/**
	 * Create a new EditorTabNameChangeDialog instance. This allows the user to modify
	 * the name of the current editor tab.
	 */
	public EditorTabNameChangeDialog() {
		super(new Shell());
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * @return The tab name, as entered by the user. This would normally be called to
	 * retrieve the text box's content, after the dialog box has been closed.
	 */
	public String getName() {
		return enteredText;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set the initial value of the text entry box, which is the current name of the editor
	 * tab. This is normally called before the dialog box is first opened.
	 * @param name The initial name to display in the text entry box.
	 */
	public void setName(String name) {
		enteredText = name;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		
		setTitle("Enter the name for this editor tab:");
		setHelpAvailable(false);
		
		/* create and format the top-level composite of this dialog */
		Composite composite = (Composite) super.createDialogArea(parent);

		/*
		 * Add a text entry box, that the user enters the tab name into.
		 * This box will start out displaying the tab's current name.
		 */
		textBox = new Text(composite, SWT.SINGLE | SWT.BORDER);
		textBox.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		textBox.setText(enteredText);
		textBox.selectAll();
		textBox.setFocus();

		/* un-disable the OK button, once some text is entered */
		textBox.addModifyListener(new ModifyListener() {	
			public void modifyText(ModifyEvent e) {
				getButton(OK).setEnabled(!textBox.getText().isEmpty());
			}
		});
		
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
		
		newShell.setText("Modify Editor Tab Name");
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
