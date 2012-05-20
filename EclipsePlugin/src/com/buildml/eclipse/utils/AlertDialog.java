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
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

/**
 * A general purpose dialog box for reporting information, warnings and errors to the 
 * end user.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class AlertDialog extends TitleAreaDialog {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The title of the dialog box */
	private String title;
	
	/** The detailed message to display */
	private String message;
	
	/** The severity level */
	private int severity;
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Create a new AlertDialog. This method is private, so can only be invoked by
	 * the static class methods (displayInfoDialog, displayErrorDialog, etc).
	 * @param title The title to display in the dialog box.
	 * @param message The detailed message to display in the dialog box.
	 * @param severity The severity level (e.g. IMessageProvider.ERROR).
	 */
	private AlertDialog(String title, String message, int severity) {
		super(Display.getDefault().getActiveShell());
		this.title = title;
		this.message = message;
		this.severity = severity;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {

		/* set the dialog's title, or default to "Alert" */
		setHelpAvailable(false);
		if (title == null) {
			title = "Alert";
		}
		setMessage(title, severity);
				
		/* create and format the top-level composite of this dialog */
		Composite composite = (Composite) super.createDialogArea(parent);
		
		/* create a label for the message, and centre it */
		if (message != null) {
			Label label = new Label(composite, 0);
			GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
			gridData.horizontalAlignment = SWT.CENTER;
			gridData.verticalAlignment = SWT.CENTER;
			label.setLayoutData(gridData);
			label.setText(message);
		}
		return composite;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButton(org.eclipse.swt.widgets.Composite, 
	 * 					int, java.lang.String, boolean)
	 */
	@Override
	protected Button createButton(Composite parent, int id, String label,
			boolean defaultButton) {
		
		/*
		 * Ignore the CANCEL button, but display the OK button.
		 */
		if (id == IDialogConstants.CANCEL_ID) {
			return null;
		} else {
			return super.createButton(parent, id, label, defaultButton);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Display an informational dialog box in the context of the current shell. Both
	 * a general message and an explanatory reason will be display. Given that there's
	 * only an "OK" button, there's no return code needed.
	 * @param title The general informational message to be displayed.
	 * @param message The detailed reason for the event.
	 */
	public static void displayInfoDialog(String title, String message) {
		AlertDialog dialog = new AlertDialog(title, message, IMessageProvider.INFORMATION);
		dialog.open();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Display an warning dialog box in the context of the current shell. Both
	 * a general message and an explanatory reason will be display. Given that there's
	 * only an "OK" button, there's no return code needed.
	 * @param title The general informational message to be displayed.
	 * @param message The detailed reason for the event.
	 */
	public static void displayWarningDialog(String title, String message) {
		AlertDialog dialog = new AlertDialog(title, message, IMessageProvider.WARNING);
		dialog.open();
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Display an error dialog box in the context of the current shell. Both
	 * a general message and an explanatory reason will be display. Given that there's
	 * only an "OK" button, there's no return code needed.
	 * @param title The general error message to be displayed.
	 * @param message The detailed reason for the event.
	 */
	public static void displayErrorDialog(String title, String message) {
		AlertDialog dialog = new AlertDialog(title, message, IMessageProvider.ERROR);
		dialog.open();
	}
	
	/*-------------------------------------------------------------------------------------*/

}
