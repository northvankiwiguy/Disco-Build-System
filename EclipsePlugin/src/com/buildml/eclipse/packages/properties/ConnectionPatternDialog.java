/*******************************************************************************
 * Copyright (c) 2013 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.packages.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.buildml.eclipse.utils.BmlTitleAreaDialog;

/**
 * A Dialog class for allowing the user to add/edit a filter pattern.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class ConnectionPatternDialog extends BmlTitleAreaDialog {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** True if we're creating a new pattern - used for display appropriate text labels */
	private boolean createNew;
	
	/** 
	 * The pattern that we're editing. This can be set before the dialog is created, or
	 * queried after pressing OK.
	 */
	private String pattern;

	/** The radio Button widget for selecting an "include" pattern */
	private Button includeButton;

	/** The radio Button widget for selecting an "exclude" pattern */
	private Button excludeButton;

	/** The Text widget the user types into */
	private Text textBox;
	
	/** Informational instructions for entering pattern */
	private static final String MESSAGE = 
			"<body style=\"font-size:small;\">" +
			"<p>Please enter a pattern for matching against input files.</p>" +
			"<h4>Syntax:</h4>" +
			"<table style=\"font-size:small;\">" +
			"<tr><td>?</td><td>Matches a single character.</td></tr>" +
			"<tr><td>*</td><td>Matches multiple characters, but doesn't cross directory boundaries (separated by /).</td></tr>" +
			"<tr><td>**</td><td>Matches multiple characters, possibly crossing directory boundaries.</td></tr>" +
			"</table>" +
			"<h4>Examples:</h4>" +
			"<table style=\"font-size:small;\">" +
			"<tr><td>**/*.c</td><td>Matches all C source files.</td></tr>" +
			"<tr><td>@zlib/**/*.c</td><td>Matches all C source files in the zlib package.</td></tr>" +
			"<tr><td>**/images/*.jpg</td><td>Matches all JPEG files in any \"images\" subdirectory.</td></tr>" +
			"<tr><td>**/work.?</td><td>Matches any file with basename of \"work\".</td></tr>" +
			"</table></body>";
	
	/** list of characters that can not be entered into a pattern */
	private static final char BAD_CHARS[] = { 
		'\n', '\r', '\t', '\0', '\f', '`', '\'', '"', '\\', '<', '>', '|', ':', '[', ']', '!'};
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new ConnectionPatternDialog box.
	 * @param createNew True if we're creating a new pattern, or false if we're editing an
	 * 				    existing pattern.
	 */
	public ConnectionPatternDialog(boolean createNew) {
		super(new Shell(), 0.3, 0.5, 0.5, 0.5);
		this.createNew = createNew;
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Set the pattern that will be displayed (initially) by the dialog box. This is used
	 * when we're editing an existing pattern.
	 * 
	 * @param pattern  The pattern to edit, including the prefix of "ia:" or "ea:" etc.
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The pattern that was entered by the user. This should only be queried after
	 * the OK button has been pressed.
	 */
	String getPattern() {
		return pattern;
	}
	
	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		
		setTitle("Enter Filter Pattern:");
		setHelpAvailable(false);
		
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout());
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.verticalIndent = 20;
		gd.horizontalIndent = 10;
		container.setLayoutData(gd);
		
		/* Create a multi-line label that contains instructions */
		Browser label = new Browser(container, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		label.setText(MESSAGE);

		/* split the pattern into the ia/ea part, and the pattern itself */
		String prefix, regex;
		if (createNew) {
			prefix = "ia";
			regex = "enter pattern";
		} else {
			String parts[] = pattern.split(":");
			if (parts.length != 2) {
				return null;
			}
			prefix = parts[0];
			regex = parts[1];
		}
		
		/* create the text box that the pattern is entered into, making it the focus */
		textBox = new Text(container, SWT.BORDER);
		textBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		textBox.setText(regex);
		textBox.selectAll();
		textBox.setFocus();
		
		/* we'll carefully monitor the user's input, to ensure the pattern is valid */
		textBox.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validateText();
			}
		});
				
		/* create the "include" and "exclude" radio buttons */
		Composite buttonBar = new Composite(container, SWT.NONE);
		buttonBar.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false));
		buttonBar.setLayout(new GridLayout(2, false));
		includeButton = new Button(buttonBar, SWT.RADIO);
		includeButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		includeButton.setText("Include Pattern");
		excludeButton = new Button(buttonBar, SWT.RADIO);
		excludeButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		excludeButton.setText("Exclude Pattern");
		if (prefix.equals("ia")) {
			includeButton.setSelection(true);
		} else {
			excludeButton.setSelection(true);
		}
		
		return container;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {

		/* the user has finished editing, save the text box value for querying by our caller */
		String regex = textBox.getText(); 
		String prefix;
		if (includeButton.getSelection()) {
			prefix = "ia";
		} else {
			prefix = "ea";
		}
		pattern = prefix + ":" + regex;
		
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
		
		/* The dialog's title depends on if we did "add" or "edit" */
		if (createNew) {
			newShell.setText("Add New Filter Pattern");
		} else {
			newShell.setText("Modify Existing Filter Pattern");
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TrayDialog#createButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createButtonBar(Composite parent) {
		Control buttonBar = super.createButtonBar(parent);
		
		/* set the OK button status appropriately */
		validateText();
		return buttonBar;
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Set the enabled state of the "OK" button, based on the content of the text box. Only
	 * valid characters (that could appear in file names), as well as * and ? will be allowed.
	 */
	private void validateText() {
		
		boolean enabled = true;
		
		/* check the minimum length */
		String text = textBox.getText();
		if (text.length() < 2) {
			enabled = false;
		}
		
		/* else scan through the string for illegal characters */
		else {
			for (int i = 0; i != text.length(); i++) {
				char ch = text.charAt(i);
				for (int j = 0; j != BAD_CHARS.length; j++) {
					if (ch == BAD_CHARS[j]) {
						enabled = false;
					}
				}
			}
			if (enabled) {
				setErrorMessage(null);
			} else {
				setErrorMessage("Invalid Character in Pattern");
			}
		}
		
		getButton(OK).setEnabled(enabled);
	}
	
	/*-------------------------------------------------------------------------------------*/
}
