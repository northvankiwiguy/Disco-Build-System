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

package com.buildml.eclipse.utils.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.buildml.eclipse.utils.BmlTitleAreaDialog;
import com.buildml.model.ISlotTypes;
import com.buildml.model.ISlotTypes.SlotDetails;

/**
 * A dialog allowing the user to add/modify a slot's definitions.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class SlotDefinitionDialog extends BmlTitleAreaDialog {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** True if this Dialog is being used to edit a newly-created slot definition */
	private boolean createNew;
	
	/** The slot details to display/edit */
	private SlotDetails details;

	/** The Text Control for entering/editing the slot name */
	private Text slotNameEntry;
	
	/*=====================================================================================*
	 * CONSTRUCTOR
	 *=====================================================================================*/

	/**
	 * Create a new {@link SlotDefinitionDialog}
	 * 
	 * @param createNew True if we're creating (and editing) a new slot.
	 * @param details The existing (or default) slot details to edit.
	 */
	public SlotDefinitionDialog(boolean createNew, SlotDetails details) {
		super(new Shell(), 0.3, 0.5, 0.5, 0.5);

		this.createNew = createNew;
		this.details = details;
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Return the slot's details. This should only be called after the "OK" button has been
	 * pressed.
	 * 
	 * @return The slot's details.
	 */
	public SlotDetails getSlotDetails() {
		return details;
	}
	
	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/


	/* (non-Javadoc)
	 * @see com.buildml.eclipse.utils.BmlTitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		
		/* 
		 * Create the main panel widget for the body of the dialog box.
		 * There are 2 columns: 1) The labels, 2) The field entries.
		 */
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout();
		layout.marginHeight = 10;
		layout.marginWidth = 20;
		layout.verticalSpacing = 10;
		layout.numColumns = 2;
		panel.setLayout(layout);
		
		/*
		 * Title - spans two columns.
		 */
		Label titleLabel = new Label(panel, SWT.None);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		titleLabel.setLayoutData(gd);
		titleLabel.setText("Specify the name and type information for this slot:\n");
		
		/*
		 * Row - Slot Position.
		 */
		new Label(panel, SWT.None).setText("Position:");
		int slotPos = details.slotPos;
		String slotPosName = (slotPos == ISlotTypes.SLOT_POS_INPUT) ? "Input" :
							  (slotPos == ISlotTypes.SLOT_POS_LOCAL) ? "Local" :
							  (slotPos == ISlotTypes.SLOT_POS_PARAMETER) ? "Parameter" :
							  (slotPos == ISlotTypes.SLOT_POS_OUTPUT) ? "Output" : "<invalid>";
		new Label(panel, SWT.None).setText(slotPosName + " Slot");
		
		/*
		 * Row - Slot Type
		 */
		new Label(panel, SWT.None).setText("Type:");
		new Label(panel, SWT.None).setText("File Group");
		
		/*
		 * Row - Slot name.
		 */
		new Label(panel, SWT.None).setText("Name:");
		slotNameEntry = new Text(panel, SWT.None);
		slotNameEntry.setLayoutData(new GridData(SWT.FILL, SWT.None, true, false));
		slotNameEntry.setText(details.slotName);
		
		/* for "create new" dialogs, we encourage the user to change the default name */
		if (createNew) {
			slotNameEntry.selectAll();
		}
		
		/* on every change to the slotName, report on whether the name is good */
		slotNameEntry.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String name = slotNameEntry.getText();
				boolean valid = isValidSlotName(name);
				setErrorMessage(valid ? null : "Slot name is invalid (too short, or invalid characters)");						
				getButton(OK).setEnabled(valid);
			}
		});
		
		return parent;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/*
	 * The "OK" button has been pressed - fetch the field value out of the Control (which
	 * will soon be destroyed) and store them for later retrieval by the getSlotDetails()
	 * method.
	 */
	@Override
	protected void okPressed() {
		details.slotName = slotNameEntry.getText();
		super.okPressed();
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * Ensure that the OK button is initially grey-out when we're creating a new slot.
	 * This encourages the user to replace the default name.
	 */
	@Override
	protected Control createButtonBar(Composite parent) {
		Control buttons = super.createButtonBar(parent);
		if (createNew) {
			getButton(OK).setEnabled(false);
		}
		return buttons;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* 
	 * (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		
		newShell.setText("Edit Slot Details");
	}	

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Determine whether the specified slot name is valid. This is used to determine whether
	 * a given name is legal if it was to be added to a BuildStore.
	 * 
	 * @param text  The slot's name
	 * @return True if the name is legal, else false.
	 */
	protected boolean isValidSlotName(String text) {
		if (text.length() < 3) {
			return false;
		}
		// TODO: reuse the existing isSlotNameValid() method.
		return true;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
