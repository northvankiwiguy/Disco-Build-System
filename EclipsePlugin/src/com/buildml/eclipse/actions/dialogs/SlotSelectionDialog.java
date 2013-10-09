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

package com.buildml.eclipse.actions.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

import com.buildml.eclipse.utils.BmlTitleAreaDialog;
import com.buildml.model.IActionMgr;
import com.buildml.model.IActionTypeMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.ISlotTypes;
import com.buildml.model.ISlotTypes.SlotDetails;

/**
 * A dialog allowing the user to select of an action's many slots. This is typically used for
 * assigning file group (or other values) into slots.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class SlotSelectionDialog extends BmlTitleAreaDialog {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** The buildStore's action manager */
	private IActionMgr actionMgr;
	
	/** The buildStore's action type manager */
	private IActionTypeMgr actionTypeMgr;
	
	/** The ID of the action we're querying */
	private int actionId;
	
	/** The ID of the slot that has been selected by the user */
	private int slotId;
	
	/** The name of the slot that has been selected by the user */
	private String slotName;
	
	/** The list box widget containing the slot names */
	private List listBox;
	
	/** All the slot values */
	private SlotDetails slots[];
	
	/*=====================================================================================*
	 * CONSTRUCTOR
	 *=====================================================================================*/
	
	/**
	 * Create a new SlotSelectionDialog, used to allow the user to select one of the action's
	 * slots.
	 * @param buildStore The IBuildStore that contains the actions.
	 * @param actionId The ID of the action that owns the slots.
	 */
	public SlotSelectionDialog(IBuildStore buildStore, int actionId) {
		super(new Shell());
			
		this.actionMgr = buildStore.getActionMgr();
		this.actionTypeMgr = buildStore.getActionTypeMgr();
		this.actionId = actionId;
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Return the ID of the selected slot. This should only be called after the "OK"
	 * button is pressed.
	 * @return the slotId
	 */
	public int getSlotId() {
		return slotId;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the name of the selected slot. This should only be called after the "OK"
	 * button is pressed.
	 * @return the slotName
	 */
	public String getSlotName() {
		return slotName;
	}
	
	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.utils.BmlTitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		
		/* what type is this action, and what is the type's name? */
		int actionTypeId = actionMgr.getActionType(actionId);
		if (actionTypeId < 0) {
			setErrorMessage("Invalid action - can't display slots");
			return parent;
		}
		String actionTypeName = actionTypeMgr.getName(actionTypeId);
		if (actionTypeName == null) {
			setErrorMessage("Invalid type of action - can't display slots");
			return parent;
		}
		
		/* fetch the list of slots for this action type */
		slots = actionTypeMgr.getSlots(actionTypeId, ISlotTypes.SLOT_POS_INPUT);
		if (slots == null) {
			setErrorMessage("Unable to retrieve slot information for this action");
			return parent;
		}
				
		/* Create the main panel widget for the body of the dialog box */
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		GridLayout layout = new GridLayout();
		layout.marginHeight = 10;
		layout.marginWidth = 20;
		layout.verticalSpacing = 10;
		layout.numColumns = 1;
		panel.setLayout(layout);
		
		/* Explanatory text that appears above the list */
		Label label1 = new Label(panel, SWT.NONE);
		label1.setText("Action Type: " + actionTypeName);
		Label label2 = new Label(panel, SWT.NONE);
		label2.setText("Select the Slot into which the selected File Group will be connected:");
		
		/* Add the list of slot names to the SWT list */
		listBox = new List(panel, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		listBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		for (int i = 0; i != slots.length; i++) {
			listBox.add(slots[i].slotName);
		}
		
		return parent;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		
		/* determine which slot was highlighted - at least one must be */
		int selectedIndex = listBox.getSelectionIndex();
		
		/* save these values so that the call can query for what was selected */
		this.slotId = slots[selectedIndex].slotId;
		this.slotName = slots[selectedIndex].slotName;
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
		
		newShell.setText("Connect to which slot?");
	}	

	/*-------------------------------------------------------------------------------------*/

}
