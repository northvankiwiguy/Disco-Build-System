/*******************************************************************************
 * Copyright (c) 2014 Arapiki Solutions Inc.
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import com.buildml.eclipse.utils.BmlPropertyPage;
import com.buildml.eclipse.utils.EclipsePartUtils;

/**
 * An Eclipse "property" page that allows viewing/editing of a packages properties.
 * 
 * Objects of this class are referenced in the plugin.xml file and are dynamically
 * created when the properties dialog is opened for a UIPackage object.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class PackagePropertyPage extends BmlPropertyPage {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/



	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new SubPackagePropertyPage() object.
	 */
	public PackagePropertyPage() {
		/* nothing */
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * The OK button has been pressed in the properties box. Save all the field values
	 * into the database. This is done via the undo/redo stack.
	 */
	@Override
	public boolean performOk() {
		
		return super.performOk();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * The cancel button was pressed.
	 */
	@Override
	public boolean performCancel() {

		return super.performCancel();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * The "restore default" button has been pressed, so return everything back to its
	 * original state.
	 */
	@Override
	protected void performDefaults() {
		
		super.performDefaults();
	}
	
	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/
	
	/**
	 * Create the widgets that appear within the properties dialog box.
	 */
	@Override
	protected Control createContents(Composite parent) {
		
		setTitle("Package Properties:");
		
		buildStore = EclipsePartUtils.getActiveBuildStore();
	
		
		/*
		 * We have three columns to display. The left column is the input file group (that
		 * we're filtering). The middle column contains the filter patterns that we're editing.
		 * The right column contains the output from the filter (a subset of the left column).
		 */
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		panel.setLayout(layout);
		
		return panel;
	}


	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	
	
	/*-------------------------------------------------------------------------------------*/

}
