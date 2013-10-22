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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
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
public class AlertDialog extends BmlTitleAreaDialog {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The title of the dialog box */
	private String title;
	
	/** The detailed message to display */
	private String message;
	
	/** The severity level */
	private int severity;
	
	/** Should the dialog box provide a "cancel" choice? */
	private boolean allowCancel;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new AlertDialog. This method is private, so can only be invoked by
	 * the static class methods (displayInfoDialog, displayErrorDialog, etc).
	 * @param title The title to display in the dialog box.
	 * @param message The detailed message to display in the dialog box.
	 * @param severity The severity level (e.g. IMessageProvider.ERROR).
	 * @param allowCancel Should the dialog provide a "cancel" option?
	 */
	private AlertDialog(String title, String message, int severity, boolean allowCancel) {
		super(Display.getDefault().getActiveShell());
		this.title = title;
		this.message = message;
		this.severity = severity;
		this.allowCancel = allowCancel;
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Display an informational dialog box in the context of the current shell. Both
	 * a general message and an explanatory reason will be display. Given that there's
	 * only an "OK" button, there's no return code needed.
	 * @param title The general informational message to be displayed.
	 * @param message The detailed reason for the event.
	 */
	public static void displayInfoDialog(String title, String message) {
		openDialog(title, message, IMessageProvider.INFORMATION, false);
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
		openDialog(title, message, IMessageProvider.WARNING, false);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Display an error dialog box in the context of the current shell. Both
	 * a general message and an explanatory reason will be display. Given that there's
	 * only an "OK" button, there's no return code needed.
	 * @param title The general error message to be displayed.
	 * @param message The detailed reason for the event.
	 */
	public static void displayErrorDialog(final String title, final String message) {
		openDialog(title, message, IMessageProvider.ERROR, false);
	}
	
	/*-------------------------------------------------------------------------------------*/

    /**
     * Display an question dialog box in the context of the current shell. The user will
     * be expected to click on OK or CANCEL to answer the question.
     * @param message The question to be displayed.
     * @return The answer to the question (IDialogConstants.OK_ID or IDialogConstants.CANCEL_ID).
     */
	public static int displayOKCancelDialog(final String message) {
		return openDialog("Question...", message, IMessageProvider.INFORMATION, true);
	}
	
	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {

		/* estimate how large the error dialog should be */
		Rectangle parentBounds = Display.getCurrent().getBounds();
		int dialogWidth = parentBounds.width / 4;
		
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
			Label label = new Label(composite, SWT.WRAP);
			GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
			gridData.horizontalAlignment = SWT.CENTER;
			gridData.verticalAlignment = SWT.CENTER;
			gridData.widthHint = dialogWidth;
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
		 * Ignore the CANCEL button (if not needed), but display the OK button.
		 */
		if (!allowCancel && (id == IDialogConstants.CANCEL_ID)) {
			return null;
		} else {
			return super.createButton(parent, id, label, defaultButton);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#getInitialSize()
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(EclipsePartUtils.getScreenWidth() / 4, EclipsePartUtils.getScreenHeight() / 5);
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * Create and open a dialog box, using the UI thread. This allows non-UI code to 
	 * raise an error.
	 * 
	 * @param title The title to display in the dialog box.
	 * @param message The detailed message to display in the dialog box.
	 * @param severity The severity level (e.g. IMessageProvider.ERROR).
	 * @param allowCancel Should the dialog provide a "cancel" option?
	 * @return The dialog status (typically IDialogConstants.OK_ID or
	 * 				IDialogConstants.CANCEL_ID).
	 */
	private static int openDialog(
			final String title, final String message, final int severity,
			final boolean allowCancel) {
		
		/* for returning the dialog code (OK/Cancel/etc) from the UI thread */
		final Integer retCode[] = new Integer[1];

		/* Open the dialog in the UI thread, blocking until the user presses "OK" */
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				AlertDialog dialog = new AlertDialog(title, message, severity, allowCancel);
				dialog.setBlockOnOpen(true);
				dialog.open();
				
				/* return response back to our calling thread */
				retCode[0] = dialog.getReturnCode();
			}
		});
		
		/* status code? (OK, Cancel, etc) */
		return retCode[0];
	}
	
	/*-------------------------------------------------------------------------------------*/

}
