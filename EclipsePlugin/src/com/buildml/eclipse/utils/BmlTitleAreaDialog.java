/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.utils;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * A sub-class of TitleAreaDialog that provides a BuildML look and feel for
 * dialog boxes.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class BmlTitleAreaDialog extends TitleAreaDialog {

	/*=====================================================================================*
	 * CONSTRUCTOR
	 *=====================================================================================*/
	
	/**
	 * Create a new BmlTitleAreaDialog, with a BuildML look and feel.
	 * @param parentShell The SWT shell that owns this dialog.
	 */
	public BmlTitleAreaDialog(Shell parentShell) {
		super(parentShell);
		
		/* set the title image to say "BuildML" */
		Image iconImage = EclipsePartUtils.getImage("images/buildml_logo_dialog.gif");
		if (iconImage != null) {
			setTitleImage(iconImage);
		}
	}	
 
	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = new Composite(
				(Composite)super.createDialogArea(parent), SWT.None);
		
		/*
		 * The default composite doesn't contain margins. We start by adding some
		 * padding on the left and right of the dialog's composite.
		 */
		GridLayout gridLayout = new GridLayout(3, false);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(gridLayout);
		
		/* left filler */
		Label leftMargin = new Label(composite, SWT.NONE);
		leftMargin.setLayoutData(new GridData(5, 0));
		
		/* main body of dialog */
		Composite mainBody = new Composite(composite, SWT.FILL);
		mainBody.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		mainBody.setLayout(new GridLayout());
		
		/* right filler */
		Label rightMargin = new Label(composite, SWT.NONE);
		rightMargin.setLayoutData(new GridData(5, 0));
		
		/* our child class puts their content in mainBody */
		return mainBody;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
