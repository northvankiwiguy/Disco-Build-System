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

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.buildml.eclipse.utils.BmlTitleAreaDialog;
import com.buildml.eclipse.utils.errors.FatalError;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.model.ISlotTypes;
import com.buildml.model.ISlotTypes.SlotDetails;
import com.buildml.utils.string.BuildStoreUtils;

/**
 * A dialog allowing the user to add/modify a slot's definitions.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class SlotDefinitionDialog extends BmlTitleAreaDialog {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The IBuildStore */
	private IBuildStore buildStore;
	
	/** True if this Dialog is being used to edit a newly-created slot definition */
	private boolean createNew;
	
	/** The slot details to display/edit */
	private SlotDetails details;
	
	/** All the slots that are currently active for this package/action-type */
	private ArrayList<SlotDetails> allSlots;

	/** The Text Control for entering/editing the slot name */
	private Text slotNameEntry;
	
	/** The Text Control for entering/editing the default value */
	private Text defaultValueEntry;
	
	/** The Text Control for entering/editing the slot description */
	private Text descrEntry;
	
	/** The Combo box for selecting the type of the slot (e.g. Text, Integer, etc) */
	private Combo typeCombo;
	
	/** The Combo box for selecting the cardinality of the slot (e.g. optional, required) */
	private Combo cardCombo;
	
	/*=====================================================================================*
	 * CONSTRUCTOR
	 *=====================================================================================*/

	/**
	 * Create a new {@link SlotDefinitionDialog}
	 * 
	 * @param buildStore 	The IBuildStore that the slot is part of.
	 * @param createNew 	True if we're creating (and editing) a new slot.
	 * @param details 		The existing (or default) slot details to edit.
	 * @param allSlots 		All of the slots for this package/action-type.
	 */
	public SlotDefinitionDialog(IBuildStore buildStore, boolean createNew, 
									SlotDetails details, ArrayList<SlotDetails> allSlots) {
		super(new Shell(), 0.3, 0.5, 0.5, 0.5);

		this.buildStore = buildStore;
		this.createNew = createNew;
		this.details = details;
		this.allSlots = allSlots;
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
		
		/* the behaviour of some fields may vary, depending on slot's position */
		boolean isInputOutputSlot = (details.slotPos == ISlotTypes.SLOT_POS_INPUT) ||
				(details.slotPos == ISlotTypes.SLOT_POS_OUTPUT);
		
		/*
		 * Create the various form fields. All field validation and behaviour is
		 * encapsulated in these methods.
		 */
		createNameEntryField(panel);
		createSlotPosField(panel);
		createSlotTypeField(panel, isInputOutputSlot);
		createDefaultValueEntryField(panel, isInputOutputSlot);
		createCardField(panel, details.slotPos);
		createDescrField(panel);

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
		
		/* fetch the new slotName field */
		details.slotName = slotNameEntry.getText();
		
		/* fetch the new slotType field */
		if (typeCombo != null) {
			details.slotType = getSlotTypeFromName(typeCombo.getText());
		}
		
		/* fetch the defaultValue field */
		if (defaultValueEntry != null) {
			details.defaultValue = getDefaultValueAsObject(defaultValueEntry.getText());
		} else {
			details.defaultValue = null;
		}
			
		/* fetch the slotCard field */
		details.slotCard = getSlotCardFromName(cardCombo.getText());
		
		/* fetch the slotDescr field */
		details.slotDescr = descrEntry.getText();
		
		/* now close the dialog box */
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
	 * This method is the central location for validating field values, updating the
	 * error message, and enabling the OK button.
	 */
	private void updateOkAndErrorMessage() {
		
		/* assume the OK button should be enabled, and assume there's no error message */
		String errorMsg = null;
		
		/* validate the defaultValue field */
		if (defaultValueEntry != null) {
			if (!validateDefaultValue(defaultValueEntry.getText())) {
				errorMsg = "Default value is not valid for this slot type.";
			}
		}
		
		/* validate the slotName field */
		String name = slotNameEntry.getText();
		boolean valid = BuildStoreUtils.isValidSlotName(name);
		boolean slotNameInUse = isSlotNameAlreadyUsed(name);
		if (!valid) {
			errorMsg = "Slot name is invalid (too short, or invalid characters)";
		} else if (slotNameInUse) {
			errorMsg = "Name is already in use by another slot";
		}
		
		/* finally, if any of the above tests reported a problem, set the error message and disable OK */
		setErrorMessage(errorMsg);
		getButton(OK).setEnabled(errorMsg == null);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create the "slot name" entry field. For new slots, we encourage the user to change
	 * the default name immediately. Any changes to the slot name will be evaluated to
	 * ensure that the name is valid, and it's not already in use by another slot.
	 * 
	 * @param panel The parent Composite to add the field to.
	 */
	private void createNameEntryField(Composite panel) {
		new Label(panel, SWT.None).setText("Name:");
		slotNameEntry = new Text(panel, SWT.None);
		slotNameEntry.setLayoutData(new GridData(SWT.FILL, SWT.None, true, false));
		slotNameEntry.setText(details.slotName);
		if (createNew) {
			slotNameEntry.selectAll();
		}
		
		/* on every change to the slotName, report on whether the name is good */
		slotNameEntry.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				updateOkAndErrorMessage();
			}
		});
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Create the "slot position" field. This can never be edited, so it's done with static labels.
	 * 
	 * @param panel The parent Composite to add the field to.
	 */
	private void createSlotPosField(Composite panel) {
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		
		new Label(panel, SWT.None).setText("Position:");

		/* determine the owner (action or package) */
		String ownerString;
		if (details.ownerType == ISlotTypes.SLOT_OWNER_PACKAGE) {
			String pkgName = pkgMgr.getName(details.ownerId);
			ownerString = "\"" + pkgName + "\" Package";
		}
		else if (details.ownerType == ISlotTypes.SLOT_OWNER_ACTION) {
			ownerString = "Action - ";
		}
		else {
			ownerString = "<not defined>";
		}
		
		/* determine the slot position */
		String slotPosName = getSlotPosName(details.slotPos);

		/* display an owner/position string */
		new Label(panel, SWT.None).setText(ownerString + " - " + slotPosName + " Slot");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create the "slot type" field. This can only be edited for new slots that haven't
	 * yet been added to the BuildStore, otherwise we use labels.
	 * 
	 * @param panel 			The parent Composite to add the field to.
	 * @param isInputOutputSlot True if this is an input/output slot, else false.
	 */
	private void createSlotTypeField(Composite panel, boolean isInputOutputSlot) {
		
		new Label(panel, SWT.None).setText("Type:");
		
		/* for newly-created parameter/local slots, the user can choose the type */
		if ((details.slotId == -1) && !isInputOutputSlot) {
			typeCombo = new Combo(panel, SWT.READ_ONLY);
			typeCombo.add(getSlotTypeName(ISlotTypes.SLOT_TYPE_TEXT));
			typeCombo.add(getSlotTypeName(ISlotTypes.SLOT_TYPE_INTEGER));
			typeCombo.add(getSlotTypeName(ISlotTypes.SLOT_TYPE_BOOLEAN));
			typeCombo.add(getSlotTypeName(ISlotTypes.SLOT_TYPE_FILE));
			typeCombo.add(getSlotTypeName(ISlotTypes.SLOT_TYPE_DIRECTORY));
			int index = getSlotTypeIndex(details.slotType);
			if (index != -1) {
				typeCombo.select(index);
			}
			
			/*
			 * Update the "default value" field based on a change in the selection.
			 */
			typeCombo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					int selectedType = getSlotTypeFromName(typeCombo.getText());
					switch(selectedType) {
					case ISlotTypes.SLOT_TYPE_BOOLEAN:
						defaultValueEntry.setText("false");
						defaultValueEntry.setEnabled(true);
						break;
					case ISlotTypes.SLOT_TYPE_TEXT:
						defaultValueEntry.setText("");
						defaultValueEntry.setEnabled(true);
						break;
					case ISlotTypes.SLOT_TYPE_INTEGER:
						defaultValueEntry.setText("0");
						defaultValueEntry.setEnabled(true);
						break;
					default:
						defaultValueEntry.setText("");
						defaultValueEntry.setEnabled(false);
						break;
					}
				}
			});
		} 
		
		/* else, it's fixed and we can't change it */
		else {
			String slotTypeName = getSlotTypeName(details.slotType);
			new Label(panel, SWT.None).setText(slotTypeName);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Create the "default value" field. This is only relevant for text, integer and
	 * boolean values, otherwise it's greyed-out.
	 * 
	 * @param panel 			The parent Composite to add the field to.
	 * @param isInputOutputSlot True if this is an input/output slot, else false.
	 */
	private void createDefaultValueEntryField(Composite panel, boolean isInputOutputSlot) {
		if (!isInputOutputSlot) {
			new Label(panel, SWT.None).setText("Default Value:");
			defaultValueEntry = new Text(panel, SWT.None);
			defaultValueEntry.setLayoutData(new GridData(SWT.FILL, SWT.None, true, false));
			defaultValueEntry.setText((details.defaultValue == null) ? "" : details.defaultValue.toString());
			
			/* validate the text, to make sure it's valid for the current slotType */
			defaultValueEntry.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					updateOkAndErrorMessage();
				}
			});
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create the "cardinality" field. For input/output, this can be optional/required/multi. 
	 * For parameters and locals, only optional/required make sense.
	 * 
	 * @param panel 	The parent Composite to add the field to.
	 * @param slotPos	The position of this slot (e.g. SLOT_POS_INPUT).	
	 */
	private void createCardField(Composite panel, int slotPos) {
	
		new Label(panel, SWT.None).setText("Cardinality:");
		cardCombo = new Combo(panel, SWT.READ_ONLY);
		cardCombo.add("Optional");
		cardCombo.add("Required");
		if (slotPos == ISlotTypes.SLOT_POS_INPUT) {
			cardCombo.add("Multi-Slot");
		}
		
		/* set the combo box to reflect the slot's current state */
		int slotCard = details.slotCard;
		cardCombo.select((slotCard == ISlotTypes.SLOT_CARD_OPTIONAL) ? 0 :
							(slotCard == ISlotTypes.SLOT_CARD_REQUIRED) ? 1 : 2);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Create the "description" field.
	 * 
 	 * @param panel The parent Composite to add the field to.
	 */
	private void createDescrField(Composite panel) {
		new Label(panel, SWT.NONE).setText("Description:");
		descrEntry = new Text(panel, SWT.MULTI | SWT.BORDER);
		descrEntry.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		descrEntry.setText(details.slotDescr);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a proposed slot name, check if it's already used for any other slots
	 * in this package/action-type.
	 * 
	 * @param name	The proposed name.
	 * @return True if the name is already used, else false.
	 */
	private boolean isSlotNameAlreadyUsed(String name) {
		
		/* it's OK if the name is set back to what it was when we started editing */
		if (name.equals(details.slotName)) {
			return false;
		}
		
		/* otherwise check all of the existing slots to make sure we're not duplicating slot names */
		for (SlotDetails slot : allSlots) {
			if (name.equals(slot.slotName)) {
				return true;
			}
		}
		return false;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a slotPos number, return the corresponding String name.
	 * 
	 * @param slotPos The slotPos number.
	 * @return The corresponding String name.
	 */
	private String getSlotPosName(int slotPos) {
		return (slotPos == ISlotTypes.SLOT_POS_INPUT) ? "Input" :
			(slotPos == ISlotTypes.SLOT_POS_LOCAL) ? "Local" :
				(slotPos == ISlotTypes.SLOT_POS_PARAMETER) ? "Parameter" :
					(slotPos == ISlotTypes.SLOT_POS_OUTPUT) ? "Output" : "<invalid>";
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Given a slotType number, return the corresponding String name.
	 * 
	 * @param slotType The slotType number.
	 * @return The corresponding String name.
	 */
	private String getSlotTypeName(int slotType) {
		switch (slotType) {
		case ISlotTypes.SLOT_TYPE_BOOLEAN:
			return "Boolean";
		case ISlotTypes.SLOT_TYPE_DIRECTORY:
			return "Directory";
		case ISlotTypes.SLOT_TYPE_FILE:
			return "File";
		case ISlotTypes.SLOT_TYPE_FILEGROUP:
			return "FileGroup";
		case ISlotTypes.SLOT_TYPE_INTEGER:
			return "Integer";
		case ISlotTypes.SLOT_TYPE_TEXT:
			return "Text";
		default:
			return "<invalid>";
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a slotType number, return the corresponding index within the combo box.
	 * 
	 * @param slotType The slotType number.
	 * @return The corresponding index in the combo box, or -1 if not shown in combo box.
	 */
	private int getSlotTypeIndex(int slotType) {
		switch (slotType) {
		case ISlotTypes.SLOT_TYPE_TEXT:
			return 0;
		case ISlotTypes.SLOT_TYPE_INTEGER:
			return 1;
		case ISlotTypes.SLOT_TYPE_BOOLEAN:
			return 2;
		case ISlotTypes.SLOT_TYPE_FILE:
			return 3;
		case ISlotTypes.SLOT_TYPE_DIRECTORY:
			return 4;
		default:
			return -1;
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a slot type name, return the corresponding ISlotTypes value. A valid type name
	 * must be provided.
	 * 
	 * @param typeName	Textual name of the slot type (e.g "Integer").
	 * @return The corresponding ISlotTypes value.
	 */
	private int getSlotTypeFromName(String typeName) {
		
		if (typeName.equals("Boolean")) {
			return ISlotTypes.SLOT_TYPE_BOOLEAN;
		} else if (typeName.equals("Directory")) {
			return ISlotTypes.SLOT_TYPE_DIRECTORY;
		} else if (typeName.equals("File")) {
			return ISlotTypes.SLOT_TYPE_FILE;
		} else if (typeName.equals("FileGroup")) {
			return ISlotTypes.SLOT_TYPE_FILEGROUP;
		} else if (typeName.equals("Integer")) {
			return ISlotTypes.SLOT_TYPE_INTEGER;
		} else if (typeName.equals("Text")) {
			return ISlotTypes.SLOT_TYPE_TEXT;
		}
		throw new FatalError("Invalid typeName");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given the name of a slot cardinality, return the corresponding ISlotTypes constant.
	 * @param name The name of the cardinality.
	 * @return The corresponding ISlotTypes constant.
	 */
	private int getSlotCardFromName(String name) {

		if (name.equals("Optional")) {
			return ISlotTypes.SLOT_CARD_OPTIONAL;
		} else if (name.equals("Required")) {
			return ISlotTypes.SLOT_CARD_REQUIRED;
		} else if (name.equals("Multi-Slot")) {
			return ISlotTypes.SLOT_CARD_MULTI;
		}
		throw new FatalError("Invalid name");
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Based on the currently selected "slotType", determine whether the default value
	 * field currently holds a valid value.
	 * 
	 * @param value	The string that's currently in the "default value" field.
	 * @return True if the default field is valid, else false.
	 */
	private boolean validateDefaultValue(String value) {
		
		int slotType = getSlotTypeFromName(typeCombo.getText());
		switch(slotType) {
		case ISlotTypes.SLOT_TYPE_BOOLEAN:
			return value.equals("true") || value.equals("false");
		
		case ISlotTypes.SLOT_TYPE_INTEGER:
			try {
				Integer.valueOf(value);
				return true;
			} catch (NumberFormatException ex) {
				return false;
			}
			
		case ISlotTypes.SLOT_TYPE_TEXT:
			return true;
			
		default:
			return true;
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Convert the default value from a String to an Object of the relevant type.
	 * 
	 * @param stringValue	The default value as a String.
	 * @return The default value as an Object.
	 */
	private Object getDefaultValueAsObject(String stringValue) {
		
		int slotType = getSlotTypeFromName(typeCombo.getText());
		switch(slotType) {
		case ISlotTypes.SLOT_TYPE_BOOLEAN:
			if (stringValue.equals("true")) {
				return Boolean.TRUE;
			} else {
				return Boolean.FALSE;
			}
		
		case ISlotTypes.SLOT_TYPE_INTEGER:
			return Integer.valueOf(stringValue);
			
		case ISlotTypes.SLOT_TYPE_TEXT:
			return stringValue;
			
		default:
			return null;
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

}
