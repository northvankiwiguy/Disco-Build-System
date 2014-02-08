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

package com.buildml.eclipse.utils.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.buildml.eclipse.bobj.UISubPackage;
import com.buildml.eclipse.utils.BmlPropertyPage;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.eclipse.utils.UndoOpAdapter;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.model.ISlotTypes;
import com.buildml.model.ISlotTypes.SlotDetails;
import com.buildml.model.undo.MultiUndoOp;
import com.buildml.model.undo.SlotUndoOp;
import com.buildml.model.ISubPackageMgr;

/**
 * An Eclipse "property" page that allows viewing/editing of a sub-packages properties.
 * 
 * Objects of this class are referenced in the plugin.xml file and are dynamically
 * created when the properties dialog is opened for a UISubPackage object.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class SlotValuePropertyPage extends BmlPropertyPage {

	/*=====================================================================================*
	 * NESTED CLASSES
	 *=====================================================================================*/
	
	/**
	 * A nested class to be used by create*Editor methods to provide functionality for:
	 *  1) Validating each field's current value (as entered by the user into a TextBox)
	 *  2) Scheduling the necessary undo/redo work when "OK" is pressed.
	 *  3) Restoring the field's default values.
	 */
	private abstract class Validator {
		/**
		 * @return null if the field's value is valid, else an error message.
		 */
		abstract String getValidationError();
		
		/**
		 * Schedule the necessary undo/redo work if this field's value has been user-modified.
		 * @param multiOp	The MultiOp to add the changes to.
		 */
		abstract void scheduleChange(MultiUndoOp multiOp);
		
		/**
		 * Ask each field to restore itself to its default values.
		 */
		abstract void restoreDefaults();
	}
	
	/*=====================================================================================*
	 * CONSTANTS
	 *=====================================================================================*/
	
	/** The number of lines that should appear when we're editing a text-typed slot */
	public static final int LINES_FOR_TEXT_BOX = 5;
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** Manager that holds package details */
	private IPackageMgr pkgMgr;

	/** Manager that holds sub-package details */
	private ISubPackageMgr subPkgMgr;
	
	/** The validators that check the content of each entry field */
	private List<Validator> validators;
	
	/** The ID of the sub-package we're editing */
	private int subPkgId;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new SubPackagePropertyPage() object.
	 */
	public SlotValuePropertyPage() {
		validators = new ArrayList<SlotValuePropertyPage.Validator>();
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * The OK button has been pressed in the properties box. Save all the field values
	 * into the database. This is done via the undo/redo stack. Each create*Editor method
	 * provides a Validator object that schedules the necessary undo/redo operations.
	 */
	@Override
	public boolean performOk() {
		
		MultiUndoOp multiOp = new MultiUndoOp();
		for (Validator validator : validators) {
			validator.scheduleChange(multiOp);
		}
		if (multiOp.size() > 0) {
			new UndoOpAdapter("Edit Slots", multiOp).invoke();
		}
		
		return super.performOk();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * The "restore default" button has been pressed, so return everything back to its
	 * original state.
	 */
	@Override
	protected void performDefaults() {
		for (Validator validator : validators) {
			validator.restoreDefaults();
		}
		super.performDefaults();
	}
	
	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/
	
	/**
	 * Create the widgets that appear within the properties dialog box. For this property
	 * page, we list all of the slots, along with their associated values.
	 */
	@Override
	protected Control createContents(Composite parent) {

		IBuildStore buildStore = EclipsePartUtils.getActiveBuildStore();
		pkgMgr = buildStore.getPackageMgr();
		subPkgMgr = buildStore.getSubPackageMgr();
				
		/* 
		 * Determine which "thing" (UISubPackage or UIAction) we're looking at, then
		 * compute the corresponding pkgId or actionTypeId that this "thing" is an
		 * instance of.
		 */
		UISubPackage subPkg = (UISubPackage)
				GraphitiUtils.getBusinessObjectFromElement(getElement(), UISubPackage.class);
		if (subPkg == null) {
			return null;
		}
		subPkgId = subPkg.getId();
		int pkgId = subPkgMgr.getSubPackageType(subPkgId);
		if (pkgId < 0) {
			return null;
		}
		
		/* fetch the "parameter" slots, and sort them alphabetically */
		SlotDetails[] paramSlots = pkgMgr.getSlots(pkgId, ISlotTypes.SLOT_POS_PARAMETER);
		Arrays.sort(paramSlots,new Comparator<SlotDetails>() {
			@Override
			public int compare(SlotDetails arg0, SlotDetails arg1) {
				return arg0.slotName.compareTo(arg1.slotName);
			}
		});
		
		/* prepare the top-level Composite in which everything else is placed */
		setTitle("Sub-Package Properties:");
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		panel.setLayout(layout);
		
		/*
		 * For each slot, display a group box, the slot's name and description,
		 * and then an appropriate set of controls to allow editing of the
		 * field's value.
		 */
		for (int i = 0; i < paramSlots.length; i++) {
			
			SlotDetails details = paramSlots[i];
			
			/* add group-box around each slot's information */
			Group group = new Group(panel, SWT.BORDER);
			group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			group.setLayout(new GridLayout());
			
			/* display the slot title - centered and bolded */
			Label title = new Label(group, SWT.CENTER);
			title.setLayoutData(new GridData(SWT.CENTER, SWT.None, true, false));
			title.setText(details.slotName);
			title.setFont(JFaceResources.getFontRegistry().getBold(""));
			
			/* display the description - wrapped */
			Label descr = new Label(group, SWT.WRAP);
			GridData gd = new GridData(GridData.FILL_BOTH);
			gd.widthHint = EclipsePartUtils.getScreenWidth() / 3;
			descr.setLayoutData(gd);
			descr.setText(details.slotDescr);
			
			/* fetch the slot's current value */
			boolean isSlotSet = subPkgMgr.isSlotSet(subPkgId, details.slotId);
			Object slotValue = subPkgMgr.getSlotValue(subPkgId, details.slotId);
			
			/*
			 * For each slot type, provide the ability to edit the current value.
			 */
			switch (details.slotType) {
			
			/* Integer-typed slots */
			case ISlotTypes.SLOT_TYPE_INTEGER:
				createIntegerEditor(group, details, isSlotSet, (Integer)details.defaultValue, (Integer)slotValue);
				break;
				
			/* Text-typed slots */
			case ISlotTypes.SLOT_TYPE_TEXT:
				createTextEditor(group, details, isSlotSet, (String)details.defaultValue, (String)slotValue);
				break;
			
			/* Boolean-typed slots */
			case ISlotTypes.SLOT_TYPE_BOOLEAN:
				createBooleanEditor(group, details, isSlotSet, (Boolean)details.defaultValue, (Boolean)slotValue);
				break;
				
			/* File-typed slots */
			case ISlotTypes.SLOT_TYPE_FILE:
				createFileEditor(group, details, isSlotSet, (Integer)details.defaultValue, (Integer)slotValue);
				break;

			/* Directory-typed slots */
			case ISlotTypes.SLOT_TYPE_DIRECTORY:
				createDirectoryEditor(group, details, isSlotSet, (Integer)details.defaultValue, (Integer)slotValue);
				break;
			
			default:
				/* do nothing */
			}
		}
		
		return panel;
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Create the Controls necessary for editing a Text-typed slot.
	 * 
	 * @param group			The parent group panel to add Control into.
	 * @param details		The slot's details.
	 * @param isSlotSet 	True if the slot currently has a value set, or false if the default
	 * 						value would be used.
	 * @param defaultValue 	The default value for the slot.
	 * @param slotValue 	The slot's current value (or null if !isSlotSet).
	 */
	private void createTextEditor(Composite group, final SlotDetails details, 
								  final boolean isSlotSet, final String defaultValue, 
								  final String slotValue) {

		/* 
		 * First, show the textBox, which should extend over multiple lines.
		 */
		final Text textBox = new Text(group, SWT.BORDER | SWT.MULTI);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.heightHint = EclipsePartUtils.getFontHeight(textBox) * LINES_FOR_TEXT_BOX;
		textBox.setLayoutData(gd);
		
		/* 
		 * Add the "Use Default" check-button. If it's checked, then we showed
		 * the slot's default value in the textBox and un-enabled the textBox.
		 */
		final Button checkButton = addDefaultCheckbox(group, isSlotSet);
		final SelectionListener select = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean useDefault = checkButton.getSelection();
				textBox.setEnabled(!useDefault);
				if (useDefault) {
					textBox.setText(defaultValue);
				} else {
					textBox.setText(isSlotSet ? slotValue : "");
				}
			}};
		checkButton.addSelectionListener(select);
		select.widgetSelected(null);
		
		/*
		 * Register a validator for this field.
		 */
		registerField(new Validator() {
			
			/* any String value is OK - never any errors given */
			@Override
			String getValidationError() {
				return null;
			}
			
			/* If this field has changed, schedule the undo/redo operation */
			@Override
			void scheduleChange(MultiUndoOp multiOp) {
				boolean newExists = !checkButton.getSelection();
				String newValue = newExists ? textBox.getText() : slotValue;				
				if ((newExists != isSlotSet) || !(slotValue.equals(newValue))) {
					SlotUndoOp op = new SlotUndoOp(buildStore, ISlotTypes.SLOT_OWNER_PACKAGE);
					op.recordChangeSlotValue(subPkgId, details.slotId, isSlotSet, slotValue, newExists, newValue);
					multiOp.add(op);
				}
			}
			
			/* restore default values */
			@Override
			void restoreDefaults() {
				checkButton.setSelection(!isSlotSet);
				select.widgetSelected(null);
			}
		});
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create the Controls necessary for editing an Integer-typed slot.
	 * 
	 * @param group			The parent group panel to add Control into.
	 * @param details		The slot's details.
	 * @param isSlotSet 	True if the slot currently has a value set, or false if the default
	 * 						value would be used.
 	 * @param defaultValue 	The default value for the slot.
	 * @param slotValue 	The slot's current value (or null if !isSlotSet).
	 */
	private void createIntegerEditor(Group group, final SlotDetails details, 
									 final boolean isSlotSet, final Integer defaultValue, 
									 final Integer slotValue) {
		/* 
		 * First, show the textBox.
		 */
		final Text integerBox = new Text(group, SWT.BORDER);
		integerBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		/* 
		 * Add the "Use Default" check-button. If it's checked, then we showed
		 * the slot's default value in the textBox and un-enabled the textBox.
		 */
		final Button checkButton = addDefaultCheckbox(group, isSlotSet);
		final SelectionListener select = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean useDefault = checkButton.getSelection();
				integerBox.setEnabled(!useDefault);
				if (useDefault) {
					integerBox.setText(defaultValue.toString());
				} else {
					integerBox.setText(isSlotSet ? slotValue.toString() : "0");
				}
			}};
		checkButton.addSelectionListener(select);
		select.widgetSelected(null);
		
		/*
		 * Register a validator for this field.
		 */
		registerField(new Validator() {
			
			/* Validate the text box, and return an error message */
			@Override
			String getValidationError() {
				String s = integerBox.getText();
				for (int i = 0; i != s.length(); i++) {
					if (!Character.isDigit(s.charAt(i))) {
						return details.slotName + ": Non-numeric characters not allowed.";
					}
				}
				return null;
			}
			
			/* If our fields changed, schedule the necessary undo/redo operation to happen */
			@Override
			void scheduleChange(MultiUndoOp multiOp) {
				boolean newExists = !checkButton.getSelection();
				Integer newValue = newExists ? Integer.valueOf(integerBox.getText()) : slotValue;				
				if ((newExists != isSlotSet) || !(slotValue.equals(newValue))) {
					SlotUndoOp op = new SlotUndoOp(buildStore, ISlotTypes.SLOT_OWNER_PACKAGE);
					op.recordChangeSlotValue(subPkgId, details.slotId, isSlotSet, slotValue, newExists, newValue);
					multiOp.add(op);
				}
			}
			
			/* restore default values */
			@Override
			void restoreDefaults() {
				checkButton.setSelection(!isSlotSet);
				select.widgetSelected(null);
			}
		});
		
		/*
		 * Monitor edits to the integerBox, to make sure it always contains a valid
		 * integer value.
		 */
		integerBox.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validateFields();
			}
		});
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create the Controls necessary for editing a Boolean-typed slot.
	 * 
	 * @param group			The parent group panel to add Control into.
	 * @param details		The slot's details.
	 * @param isSlotSet 	True if the slot currently has a value set, or false if the default
	 * 						value would be used.
	 * @param defaultValue 	The default value for the slot.
	 * @param slotValue 	The slot's current value (or null if !isSlotSet).
	 */
	private void createBooleanEditor(Group group, final SlotDetails details, 
									 final boolean isSlotSet, final Boolean defaultValue, 
									 final Boolean slotValue) {
		
		/* Show a pair of radio buttons - "true" and "false" */
		Composite choices = new Composite(group, SWT.NONE);
		choices.setLayoutData(new GridData(SWT.FILL, SWT.None, true, false));
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = layout.verticalSpacing = 0;
		choices.setLayout(layout);
		final Button trueButton = new Button(choices, SWT.RADIO);
		trueButton.setText("true");
		final Button falseButton = new Button(choices, SWT.RADIO);
		falseButton.setText("false");

		/* 
		 * Add the "Use Default" check-button. If it's checked, then we showed
		 * the slot's default value in the textBox and un-enabled the textBox.
		 */
		final Button checkButton = addDefaultCheckbox(group, isSlotSet);
		final SelectionListener select = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean value = slotValue;
				boolean useDefault = checkButton.getSelection();
				if (useDefault) {
					value = defaultValue;
				}
				trueButton.setEnabled(!useDefault);
				falseButton.setEnabled(!useDefault);
				trueButton.setSelection(value);
				falseButton.setSelection(!value);
			}};
		checkButton.addSelectionListener(select);
		select.widgetSelected(null);
		
		/*
		 * Register a validator for this field.
		 */
		registerField(new Validator() {
			
			/* any Boolean value is OK - never any errors given */
			@Override
			String getValidationError() {
				return null;
			}
			
			/* If this field has changed, schedule the undo/redo operation */
			@Override
			void scheduleChange(MultiUndoOp multiOp) {
				boolean newExists = !checkButton.getSelection();
				Boolean newValue = newExists ? Boolean.valueOf(trueButton.getSelection()) : slotValue;				
				if ((newExists != isSlotSet) || !(slotValue.equals(newValue))) {
					SlotUndoOp op = new SlotUndoOp(buildStore, ISlotTypes.SLOT_OWNER_PACKAGE);
					op.recordChangeSlotValue(subPkgId, details.slotId, isSlotSet, slotValue, newExists, newValue);
					multiOp.add(op);
				}
			}
			
			/* restore default values */
			@Override
			void restoreDefaults() {
				checkButton.setSelection(!isSlotSet);
				select.widgetSelected(null);
			}
		});
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create the Controls necessary for editing a File-typed slot.
	 * 
	 * @param group			The parent group panel to add Control into.
	 * @param details		The slot's details.
	 * @param isSlotSet 	True if the slot currently has a value set, or false if the default
	 * 						value would be used.
	 * @param defaultValue 	The default value for the slot.
	 * @param slotValue 	The slot's current value (or null if !isSlotSet).
	 */
	private void createFileEditor(Group group, SlotDetails details, 
									boolean isSlotSet, Object defaultValue, final Object slotValue) {
		// TODO: implement this
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create the Controls necessary for editing an Directory-typed slot.
	 * 
	 * @param group			The parent group panel to add Control into.
	 * @param details		The slot's details.
	 * @param isSlotSet 	True if the slot currently has a value set, or false if the default
	 * 						value would be used.
	 * @param defaultValue 	The default value for the slot.
	 * @param slotValue 	The slot's current value (or null if !isSlotSet).
	 */
	private void createDirectoryEditor(Group group, SlotDetails details, 
										boolean isSlotSet, Object defaultValue, final Object slotValue) {
		// TODO: implement this
		
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Helper method for displaying a "Use Default Value" checkbox.
	 * 
	 * @param composite	The Composite to add the checkbox to.
	 * @param isSlotSet	True if the slot currentl has an explicit value (i.e. does not default)
	 * @return The Button control, that can be queried for its status.
	 */
	private Button addDefaultCheckbox(Composite composite, boolean isSlotSet) {
		
		/* Create the checkbox */
		Button checkButton = new Button(composite, SWT.CHECK);
		checkButton.setText("Use Default Value");
		checkButton.setSelection(!isSlotSet);
		return checkButton;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Validate all of the fields that are being displayed. Whenever a control is modified,
	 * this method is called to validate all fields (not just the field that was modified).
	 * The result is that if any fields contain invalid values, an error message will be
	 * displayed.
	 */
	private void validateFields() {
		for (Validator validator : validators) {
			String error = validator.getValidationError();
			if (error != null) {
				setMessage(error, ERROR);
				setValid(false);
				return;
			}
		}
		setMessage(null);
		setValid(true);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Register a field validator method. Each field editor must register an object that
	 * provides the ability to validate the field, as well as to schedule the field's value
	 * change as an undo/redo operation, and perhaps other things.
	 * 
	 * @param validator	A Validator object for the field editor.
	 */
	private void registerField(Validator validator) {
		validators.add(validator);
	}
	
	/*-------------------------------------------------------------------------------------*/
}
