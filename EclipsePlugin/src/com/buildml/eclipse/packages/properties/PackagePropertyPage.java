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

import java.util.ArrayList;
import java.util.Arrays;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;

import com.buildml.eclipse.bobj.UIPackage;
import com.buildml.eclipse.utils.BmlPropertyPage;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.eclipse.utils.dialogs.SlotDefinitionDialog;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.model.ISlotTypes;
import com.buildml.model.ISlotTypes.SlotDetails;

/**
 * An Eclipse "property" page that allows viewing/editing of a packages slot information.
 * 
 * Objects of this class are referenced in the plugin.xml file and are dynamically
 * created when the properties dialog is opened for a UIPackage object.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class PackagePropertyPage extends BmlPropertyPage {

	/*=====================================================================================*
	 * NESTED CLASSES
	 *=====================================================================================*/

	/**
	 * A structure for holding information about a List and it's three Buttons
	 */
	private class ListBoxControls {
		List list;
		Button newButton;
		Button editButton;
		Button deleteButton;
	}
	
	/*=====================================================================================*
	 * CONSTANTS
	 *=====================================================================================*/

	/** Value for a List box that contains no slots */
	private static final String NO_SLOTS = "--- none defined ---";
		
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** Our IBuildStore */
	private IBuildStore buildStore;

	/** The BuildStore's package manager */
	private IPackageMgr pkgMgr;

	/** ID of the package that these slots belong to */
	private int pkgId;
	
	/** The original slot information, before any changes are made */
	private ArrayList<SlotDetails> originalSlots;
	
	/** The slot information as changes are being made */
	private ArrayList<SlotDetails> currentSlots;
	
	/** The Controls showing the output slots */
	private ListBoxControls outputSlotControls;
	
	/** The Controls showing the parameters slots */	
	private ListBoxControls parameterSlotControls;
	
	/** The Controls showing the local slots */
	private ListBoxControls localSlotControls;	
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new SubPackagePropertyPage() object.
	 */
	public PackagePropertyPage() {
		buildStore = EclipsePartUtils.getActiveBuildStore();
		pkgMgr = buildStore.getPackageMgr();
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
		
		// TODO: compare originalSlots and currentSlots and generate undo/redo operations
		// to make all the changes.
		
		return super.performOk();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * The "restore default" button has been pressed, so return everything back to its
	 * original state.
	 */
	@Override
	protected void performDefaults() {
		
		/* restore the currentSlots to the values in originalSlots */
		currentSlots = duplicateSlotList(originalSlots);
		
		/* refresh all the list boxes */
		populateSlotList(outputSlotControls, ISlotTypes.SLOT_POS_OUTPUT);
		populateSlotList(parameterSlotControls, ISlotTypes.SLOT_POS_PARAMETER);
		populateSlotList(localSlotControls, ISlotTypes.SLOT_POS_LOCAL);
		
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
				
		/* 
		 * Determine the numeric ID of the package. We have the following choices
		 * for getElement():
		 *   1) We're passed a UIPackage, which is what we want.
		 *   2) We're passed something that adapts to UIPackage.
		 *   3) We're passed a Graphiti EditPart that converts to a UIPackage.
		 * Otherwise give up completely and display an error.
		 */
		UIPackage pkg = null;
		if (getElement() instanceof UIPackage) {
			pkg = (UIPackage) getElement();
		} else if (getElement() instanceof IAdaptable) {
			IAdaptable adapter = (IAdaptable)getElement();
			pkg = (UIPackage) adapter.getAdapter(UIPackage.class);	
		}
		if (pkg == null) {
			pkg = (UIPackage) GraphitiUtils.getBusinessObjectFromElement(getElement(), UIPackage.class);
		}
		
		/* couldn't identify the UIPackage - give an error */
		if (pkg == null) {
			setErrorMessage("Selected element is not a recognized package");
			return null;
		}

		/*
		 * Fetch the slot information, and make a copy of it so we can restore it 
		 * later if necessary. We'll use this information as our "database", rather
		 * than actually modifying the buildstore. This should only happen when
		 * the final "OK" button is pressed.
		 */
		originalSlots = new ArrayList<SlotDetails>();
		SlotDetails details[] = pkgMgr.getSlots(pkgId, ISlotTypes.SLOT_POS_ANY);
		for (int i = 0; i < details.length; i++) {
			originalSlots.add(details[i]);
		}
		currentSlots = duplicateSlotList(originalSlots);
		
		/*
		 * We're good, and we have a UIPackage, so display the package information.
		 */
		pkgId = pkg.getId();
		String pkgName = pkgMgr.getName(pkgId);
		setTitle("Package Properties: " + pkgName);
		
		/* 
		 * Create a panel in which all sub-widgets are added. The first (of 2)
		 * columns will content the "list" of slots defined for this package. 
		 * The second (of 2) columns contain buttons for performing actions
		 * on those slots.
		 */
		Composite panel = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		panel.setLayout(layout);
		
		/* display the lists of slots (output, parameter and local) */
		outputSlotControls = createSlotList(panel, "Output Slots:", ISlotTypes.SLOT_POS_OUTPUT);
		parameterSlotControls = createSlotList(panel, "Parameter Slots:", ISlotTypes.SLOT_POS_PARAMETER);
		localSlotControls = createSlotList(panel, "Local Slots:", ISlotTypes.SLOT_POS_LOCAL);

		return panel;
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * Make a deep copy of the slots list. We use this to create a "backup" of the slot
	 * information in case we "restore defaults".
	 * 
	 * @param originalSlots The original list to be copied.
	 * @return A duplicate of the original list.
	 */
	private ArrayList<SlotDetails> duplicateSlotList(ArrayList<SlotDetails> originalSlots) {
		
		ArrayList<SlotDetails> newList = new ArrayList<SlotDetails>();
		for (SlotDetails slot : originalSlots) {
			newList.add(new SlotDetails(slot.slotId, slot.slotName, slot.slotType, 
					slot.slotPos, slot.slotCard, slot.defaultValue, slot.enumValues));
		}
		return newList;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create a list box and appropriate buttons, to allow a specific slot type to be
	 * manipulated.
	 * 
	 * @param panel		The Composite that the list box (and buttons) should be placed in.
	 * @param title		The textual title to be shown above the list box.
	 * @param slotPos	The type of slot to manipulate (e.g. ISlotType.SLOT_POS_OUTPUT).
	 * 
	 * @return The List controls (list and buttons).
	 */
	private ListBoxControls createSlotList(Composite panel, String title, final int slotPos) {
		
		final ListBoxControls controls = new ListBoxControls();
		
		/*
		 * The title for this section.
		 */
		Label titleLabel = new Label(panel, SWT.None);
		titleLabel.setText(title);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		titleLabel.setLayoutData(gd);
		
		/*
		 * The first column - the list of slots in the package.
		 */
		controls.list = new List(panel, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		controls.list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		/*
		 * The second column - buttons that we can press to modify the slots.
		 */
		Composite buttonPanel = new Composite(panel, SWT.NONE);
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		RowLayout buttonPanelLayout = new RowLayout(SWT.VERTICAL);
		buttonPanelLayout.fill = true;
		buttonPanelLayout.marginLeft = buttonPanelLayout.marginRight = 10;
		buttonPanelLayout.spacing = 10;
		buttonPanel.setLayout(buttonPanelLayout);
				
		/* new button - for adding new slots to this package */
		controls.newButton = new Button(buttonPanel, SWT.NONE);
		controls.newButton.setText("New Slot");
		controls.newButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				performNewOperation(controls, slotPos);
			}
		});
		
		/* edit button - for changing a slot's information */
		controls.editButton = new Button(buttonPanel, SWT.NONE);
		controls.editButton.setText("Edit");
		controls.editButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				performEditOperation(controls, slotPos);
			}
		});
		
		/* delete button - for removing a slot from this package */
		controls.deleteButton = new Button(buttonPanel, SWT.NONE);
		controls.deleteButton.setText("Delete");
		controls.deleteButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				performDeleteOperation(controls, slotPos);
			}
		});
		
		/*
		 * When items in the list box are selected/deselected, we need to enable/disable
		 * the buttons accordingly. By default, only the "new" button is enabled.
		 */
		controls.newButton.setEnabled(true);
		controls.editButton.setEnabled(false);
		controls.deleteButton.setEnabled(false);
		controls.list.addSelectionListener(new SelectionAdapter() {
			
			/* select the name evaluates the button-enabled statuses */
			@Override
			public void widgetSelected(SelectionEvent e) {
				String selectedNames[] = controls.list.getSelection();
				if ((selectedNames.length == 1) && (selectedNames[0].equals(NO_SLOTS))) {
					return;
				}
				controls.editButton.setEnabled(true);
				controls.deleteButton.setEnabled(true);
			}

			/* double-click performs "edit" */
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				performEditOperation(controls, slotPos);
			}
		});
		
		/* populate the List control with all the slots */
		populateSlotList(controls, slotPos);
		
		return controls;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Populate the specified List Control with the names of the slots (from the "pkgId"
	 * package).
	 * 
	 * @param controls	The List Controls to populate. 
	 * @param slotPos 	The type of slot (e.g. ISlotType.SLOT_POS_OUTPUT).
	 */
	private void populateSlotList(ListBoxControls controls, int slotPos) {
		
		/* filter which of the slots we want to see (based on slotPos) */
		ArrayList<String> names = new ArrayList<String>();
		for (SlotDetails slot: currentSlots) {
			if (slot.slotPos == slotPos) {
				names.add(slot.slotName);
			}
		}
		String sortedNames[] = names.toArray(new String[0]);
		Arrays.sort(sortedNames);
		
		/* 
		 * Now add the alphabetically sorted names to the list. For empty lists,
		 * show the NO_SLOTS string.
		 */
		controls.list.removeAll();
		controls.editButton.setEnabled(false);
		controls.deleteButton.setEnabled(false);
		
		/*
		 * Set the button enabled status, as necessary.
		 */
		if (sortedNames.length == 0) {
			controls.list.add(NO_SLOTS);
		}
		else {
			for (int i = 0; i < sortedNames.length; i++) {
				controls.list.add(sortedNames[i]);
			}
		}
	}	

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the index of the currently-selected slot within the currentSlots list, or
	 * -1 if nothing is selected (or if the NO_SLOTS line is selected).
	 * 
	 * @param slotList	The List box to retrieve the selection from.
	 * @return Index of the selected slot within the currentSlots list (or -1 if not found).
	 */
	private int getSelectedSlotDetails(List slotList) {
		
		/* What is the selected text from the list box? */
		String selectedNames[] = slotList.getSelection();
		if (selectedNames.length != 1) {
			return -1;
		}
		String slotName = selectedNames[0];
		if (slotName.equals(NO_SLOTS)) {
			return -1;
		}
		
		/* compute the index of this text from the list */
		int i = 0;
		for (SlotDetails details : currentSlots) {
			if (details.slotName.equals(slotName)) {
				return i;
			}
			i++;
		}
		return i;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * The user has pressed the "Delete" button, so proceed to remove the slot.
	 * 
	 * @param controls 	The Controls that contain the list of slots.
	 * @param slotPos 	The position of the slot (e.g. SLOT_POS_OUTPUT).
	 */
	private void performDeleteOperation(ListBoxControls controls, int slotPos) {
		
		/* fetch the slot name */
		int index = getSelectedSlotDetails(controls.list);
		if (index != -1) {
			currentSlots.remove(index);
			populateSlotList(controls, slotPos);
			controls.list.deselectAll();
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * The user has pressed the "Edit" button, so proceed to edit the slot details.
	 * 
	 * @param controls 	The Controls that contain the list of slots.
	 * @param slotPos 	The position of the slot (e.g. SLOT_POS_OUTPUT).
	 */
	private void performEditOperation(ListBoxControls controls, int slotPos) {

		/* get the details for the currently selected slot */
		int index = getSelectedSlotDetails(controls.list);
		if (index == -1) {
			return;
		}
		SlotDetails selectedDetails = currentSlots.get(index);
		
		/* create a dialog so the user can edit these defaults */
		SlotDefinitionDialog dialog = new SlotDefinitionDialog(false, selectedDetails, currentSlots);
		int status = dialog.open();
		
		/* on OK, insert the new slot details back into our active set of slots */
		if (status == SlotDefinitionDialog.OK) {
			SlotDetails editedDetails = dialog.getSlotDetails();
			currentSlots.set(index, editedDetails);
			populateSlotList(controls, slotPos);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * The user has pressed the "New Slot" button, so open a dialog that allows them to
	 * define the new slot.
	 * 
	 * @param controls 	The Controls that display the list of slots.
	 * @param slotPos 	The position of the slot (e.g. SLOT_POS_OUTPUT).
	 */
	private void performNewOperation(ListBoxControls controls, int slotPos) {
		
		/* create suitable default slot details */
		SlotDetails defaults = new SlotDetails(
				-1, "Enter a new slot name", ISlotTypes.SLOT_TYPE_FILEGROUP, slotPos,
				ISlotTypes.SLOT_CARD_REQUIRED, null, null);
		
		/* create a dialog so the user can edit these defaults */
		SlotDefinitionDialog dialog = new SlotDefinitionDialog(true, defaults, currentSlots);
		int status = dialog.open();
		
		/* on OK, add the new slot details to our active set of slots */
		if (status == SlotDefinitionDialog.OK) {
			SlotDetails newDetails = dialog.getSlotDetails();
			currentSlots.add(newDetails);
			populateSlotList(controls, slotPos);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

}
