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
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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

	/** Maximum size of a dialog box is 1/MAX_DIALOG_RATIO of the screen height/width */
	private static final int MAX_DIALOG_RATIO = 2;
	
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
		
		/* set the title image to say "BuildML" */
		Image iconImage = EclipsePartUtils.getImage("images/buildml_logo_dialog.gif");
		if (iconImage != null) {
			setTitleImage(iconImage);
		}
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
		
		/* set the dialog's title, or default to "Alert" */
		setHelpAvailable(false);
		if (title == null) {
			title = "Alert";
		}
		setMessage(title, severity);
				
		/* create and format the top-level composite of this dialog */
		Composite composite = (Composite) super.createDialogArea(parent);
		composite.setLayout(new GridLayout());
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		composite.setLayoutData(gd);
		
		ScrolledComposite sc = new ScrolledComposite(composite, SWT.V_SCROLL | SWT.H_SCROLL);
		sc.setLayout(new GridLayout());
		sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		/* create a label for the message, and centre it */
		if (message != null) {
			Label label = new Label(sc, SWT.WRAP);
			label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			label.setText(message);
			
			sc.setContent(label);
			label.setSize(label.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		}
		return parent;
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
	
	/**
	 * When the dialog box is created (based on the size of all the child widgets - particularly
	 * the label), make sure that it never takes up too much of the screen, otherwise it loses
	 * the feeling of being a Dialog.
	 */
	@Override
	protected Point getInitialSize() {
		
		Point initialSize = super.getInitialSize();
		int screenWidth = EclipsePartUtils.getScreenWidth();
		int screenHeight = EclipsePartUtils.getScreenHeight();
		
		if (initialSize.x > (screenWidth / MAX_DIALOG_RATIO)) {
			initialSize.x = screenWidth / MAX_DIALOG_RATIO;
		}
		if (initialSize.y > (screenHeight / MAX_DIALOG_RATIO)) {
			initialSize.y = screenHeight / MAX_DIALOG_RATIO;
		}
		return initialSize;
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
