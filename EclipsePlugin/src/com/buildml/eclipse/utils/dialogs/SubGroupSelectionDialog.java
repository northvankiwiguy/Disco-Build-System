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

package com.buildml.eclipse.utils.dialogs;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import com.buildml.eclipse.utils.BmlTitleAreaDialog;

/**
 * A dialog allowing the user to select equal-sized subgroups of a list of items.
 * This class can be used for any list of textual items, where there's a need to
 * break those items into equal-sized subgroups.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class SubGroupSelectionDialog extends BmlTitleAreaDialog {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** The items to be grouped */
	private String[] items;

	/** All the valid sub group sizes */
	private ArrayList<Integer> validSizes;

	/** The chosen subgroup size */
	private int chosenSize = 0;
	
	/*=====================================================================================*
	 * CONSTRUCTOR
	 *=====================================================================================*/

	/**
	 * Create a new {@link SubGroupSelectionDialog}
	 * 
	 * @param items		The list of String items to be grouped.
	 */
	public SubGroupSelectionDialog(String [] items) {
		super(new Shell(), 0.3, 0.5, 0.5, 0.5);
			
		this.items = items;
		
		/* compute the possible sub-group sizes */
		this.validSizes = computeDivisors(items.length);
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Return the chosen subgroup size. This method should only be called after the "OK"
	 * button has been pressed.
	 * 
	 * @return The selected subgroup size, or 0 if no size was selected.
	 */
	public int getGroupSize() {
		return chosenSize;
	}
	
	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.utils.BmlTitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
				
		/* Create the main panel widget for the body of the dialog box */
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout();
		layout.marginHeight = 10;
		layout.marginWidth = 20;
		layout.verticalSpacing = 10;
		layout.numColumns = 1;
		panel.setLayout(layout);
		
		/*
		 * Is the size of the input list appropriate? That is, are there any valid sub-group
		 * sizes that we can merge them into?
		 */
		if (validSizes.size() == 0) {
			setErrorMessage("Invalid number of items selected - can't divide into groups");
			Label error = new Label(panel, SWT.WRAP);
			error.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
			error.setText("To divide the selected items into smaller groups, " +
					"you must select an equally-divisable number of items. For example, " +
					"a multiple of 2, a multiple of 3, etc.");
			return parent;
		}
				
		/*
		 * Informative message
		 */
		new Label(panel, SWT.WRAP).setText("Please select the number of items to appear " +
				"in each sub-group:");
		
		/* 
		 * display the valid sizes in a pull-down list, and select the first size choice
		 * as the default (often this is "2").
		 */
		final Combo sizeSelector = new Combo(panel, SWT.READ_ONLY);
		for (int subGroupSize : validSizes) {
			sizeSelector.add("Group Size: " + subGroupSize);
		}
		sizeSelector.select(0);
		chosenSize = validSizes.get(0);
		
		/* 
		 * Display all the strings in a list box, with spaces between each sub-group
		 */
		final List itemList = new List(panel, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		itemList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		fillListBox(itemList, items, chosenSize);
		
		/*
		 * If the user selects a different group size, redraw the content of the 
		 */
		sizeSelector.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				chosenSize = validSizes.get(sizeSelector.getSelectionIndex());
				fillListBox(itemList, items, chosenSize);
			}
		});
		
		return parent;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* 
	 * (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		
		newShell.setText("Choose the Sub-Group Size");
	}	

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Given the size of the input group, compute all the possible sub-group sizes that
	 * evenly divide into the totalSize. For example, if totalSize is 10, then the valid
	 * sub-group sizes are [2, 5].
	 *  
	 * @param totalSize The size of the whole input group.
	 * @return An array of valid sub-group sizes.
	 */
	private ArrayList<Integer> computeDivisors(int totalSize) {
		
		ArrayList<Integer> results = new ArrayList<Integer>();
		for (int i = 2; i <= totalSize / 2; i++) {
			if ((totalSize % i) == 0) {
				results.add(i);
			}
		}
		return results;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Populate the List widget with the items to be divided into sub-groups. Based on the
	 * specified sub-group size, insert a divider line.
	 * 
	 * @param itemList		List widget to display the items within.
	 * @param items			The items to be displayed.
	 * @param subGroupSize	The size of each sub-group (for displaying divider lines).
	 */
	private void fillListBox(List itemList, String[] items, Integer subGroupSize) {

		itemList.removeAll();
		for (int i = 0; i < items.length; i++) {
			if ((i != 0) && (i % subGroupSize == 0)) {
				itemList.add("");
			}
			itemList.add(items[i]);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
}
