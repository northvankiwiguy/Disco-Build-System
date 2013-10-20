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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import com.buildml.eclipse.utils.BmlTitleAreaDialog;
import com.buildml.eclipse.utils.errors.FatalError;
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
	
	/** The list box widget containing the "input" slot names */
	private List inputSlotListBox;

	/** The list box widget containing the "output" slot names */
	private List outputSlotListBox;

	/** All the input slot values */
	private SlotDetails inputSlots[];
	
	/** All the output slot values */
	private SlotDetails outputSlots[];
	
	/** Should the "input" slots be listed? */
	private boolean showInputSlots;
	
	/** Should the "output" slots be listed? */
	private boolean showOutputSlots;
	
	/*=====================================================================================*
	 * CONSTRUCTOR
	 *=====================================================================================*/
	
	/**
	 * Create a new SlotSelectionDialog, used to allow the user to select one of the action's
	 * slots.
	 * @param buildStore The IBuildStore that contains the actions.
	 * @param actionId The ID of the action that owns the slots.
	 * @param showInputs Should the dialog show the "input" slots?
	 * @param showOutputs Should the dialog show the "output" slots?
	 */
	public SlotSelectionDialog(IBuildStore buildStore, int actionId, 
				boolean showInputs, boolean showOutputs) {
		super(new Shell());
			
		this.actionMgr = buildStore.getActionMgr();
		this.actionTypeMgr = buildStore.getActionTypeMgr();
		this.actionId = actionId;
		this.showInputSlots = showInputs;
		this.showOutputSlots = showOutputs;
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
		
		TabFolder tabFolder = new TabFolder(panel, SWT.NONE);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		/*
		 * Prepare the list of "input" slots, if requested by the Dialog creator.
		 */
		if (showInputSlots) {
			
			/* fetch the list of slots for this action type */
			inputSlots = actionTypeMgr.getSlots(actionTypeId, ISlotTypes.SLOT_POS_INPUT);
			if (inputSlots == null) {
				setErrorMessage("Unable to retrieve \"input\" slot information for this action");
				return parent;
			}
			
			/* add them to the listbox */
			inputSlotListBox = addSlotsToListBox(tabFolder, "Input Slots", inputSlots);
		}
		
		/*
		 * Prepare the list of "output" slots, if requested by the Dialog creator.
		 */
		if (showOutputSlots) {
			
			/* fetch the list of slots for this action type */
			outputSlots = actionTypeMgr.getSlots(actionTypeId, ISlotTypes.SLOT_POS_OUTPUT);
			if (outputSlots == null) {
				setErrorMessage("Unable to retrieve \"output\" slot information for this action");
				return parent;
			}

			/* add them to the listbox */
			outputSlotListBox = addSlotsToListBox(tabFolder, "Output Slots", outputSlots);
		}
		
		return parent;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {

		/* 
		 * Determine which slot was highlighted - check the input list first, then the
		 * output list. At least one of the lists must have a selected item, otherwise
		 * "OK" would have been greyed out.
		 */
		int selectedIndex = -1;

		/* an input slot was selected? */
		if (showInputSlots) {
			selectedIndex = inputSlotListBox.getSelectionIndex();
			if (selectedIndex != -1) {
				this.slotId = inputSlots[selectedIndex].slotId;
				this.slotName = inputSlots[selectedIndex].slotName;			
			}
		}

		if (showOutputSlots && (selectedIndex == -1)) {
			/* an output slot was selected? */
			selectedIndex = outputSlotListBox.getSelectionIndex();
			this.slotId = outputSlots[selectedIndex].slotId;
			this.slotName = outputSlots[selectedIndex].slotName;			
		}

		if (selectedIndex == -1) {
			/* oops - button shouldn't have been enabled */
			getButton(IDialogConstants.OK_ID).setEnabled(false);
			return;
		}

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
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TrayDialog#createButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createButtonBar(Composite parent) {
		Control result = super.createButtonBar(parent);
		
		/* until something is selected, the OK button is greyed out */
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		
		
		return result;
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Given an action type and a slot "position" add all the slot names to the tab, allowing
	 * the slot names to be selected.
	 * 
	 * @param tabFolder		The SWT TabFolder to add the tab to.
	 * @param tabTitle      The text string to display at the top of the tab.
	 * @param slots			The list of slots to be displayed.
	 * @return The newly added List widget (containing the slot names), or null on error.
	 */
	private List addSlotsToListBox(TabFolder tabFolder, String tabTitle, SlotDetails slots[]) {
		
		/* create a new tab within the existing TabFolder */
		TabItem slotTab = new TabItem(tabFolder, SWT.NONE);
		slotTab.setText(tabTitle);

		/* Add the list of slot names to the list box */
		List listBox = new List(tabFolder, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		listBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		slotTab.setControl(listBox);
		for (int i = 0; i != slots.length; i++) {
			listBox.add(slots[i].slotName);
		}

		/* 
		 * Make sure that selecting an item in this list will deselect all items in
		 * the other list. The only makes sense if both lists are shown.
		 */
		listBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				/* deselect the other list box */
				if (showInputSlots && showOutputSlots) {
					if (e.getSource() == inputSlotListBox) {
						outputSlotListBox.deselectAll();
					} else {
						inputSlotListBox.deselectAll();
					}
				}
				
				/* it's now OK to press "OK" */
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			}
		});
		return listBox;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
