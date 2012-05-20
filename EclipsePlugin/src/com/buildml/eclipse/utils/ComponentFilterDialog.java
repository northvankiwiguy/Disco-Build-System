/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.utils;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.buildml.eclipse.utils.errors.FatalError;
import com.buildml.model.BuildStore;
import com.buildml.model.Components;
import com.buildml.model.types.ComponentSet;

/**
 * A Dialog allowing the user to select (with checkboxes) a subset of all the
 * BuildStore's components. This class is used in the Eclipse plugin, wherever
 * it's necessary to select a combination of components.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class ComponentFilterDialog extends TitleAreaDialog {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/**
	 * The input ComponentSet that this Dialog will display. This remains unmodified since
	 * we create a clone of this object before modifying it.
	 */
	private ComponentSet compSet;
	
	/**
	 * We need one widget for each line of the table.
	 */
	private CompSelectWidget lineWidgets[];
	
	
	/*=====================================================================================*
	 * NESTED CLASS - CompSelectWidget
	 *=====================================================================================*/
	
	/**
	 * A nested class representing a single line in the ComponentFilterDialog window.
	 * Each CompSelectWidget contains the components name, and a checkbox for each of
	 * the scopes.
	 * 
	 * @author "Peter Smith <psmith@arapiki.com>"
	 */
	private class CompSelectWidget extends Composite {

		/*---------------------------------------------------------------------------------*/

		/** Each scope name (except for None) has its own checkbox widget. */
		private Button checkBoxes[] = null;
		
		/** This ID of the component that this widget represents. */
		int compId;
		
		/*---------------------------------------------------------------------------------*/

		/**
		 * Create a new instance of the CompSelectWidth class.
		 * @param parent The ComponentFilterDialog composite that this widget is pat of.
		 * @param compName The name of the component to be displayed.
		 */
		public CompSelectWidget(
				Composite parent, 
				String compName)
		{	
			super(parent, 0);
			
			final Components compMgr = compSet.getBuildStore().getComponents();
			
			/* get the scope name, and prepare for a checkbox for each */
			checkBoxes = new Button[Components.SCOPE_MAX];
			
			/* we'll use the component's internal ID when accessing the component Set */
			compId = compMgr.getComponentId(compName);
			
			/* make sure this widget is stretched to the full width of the shell */
			this.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
			
			/* 
			 * Each line will contain a section for the component name,
			 * and a section for each of the scopes (excluding the None scope).
			 */
			GridLayout layout = new GridLayout();
			layout.numColumns = 1 + Components.SCOPE_MAX;
			layout.marginHeight = 0;
			setLayout(layout);
			
			/* The component name takes as much of the left side as possible */
			Label label1 = new Label(this, 0);
			label1.setText(compName);
			label1.setLayoutData(new GridData(SWT.LEFT, SWT.NONE, true, false));

			/*
			 * On the right side, create a new checkbox button for each scope
			 * (except for "None"). We take note of each widget (in the checkBoxes)
			 * array so we can set/refresh it later.
			 */
			for (int i = 0; i < Components.SCOPE_MAX; i++) {
				
				/* the scope check boxes are on the right side */
				final Button newButton = new Button(this, SWT.CHECK);
				newButton.setText(compMgr.getScopeName(i + 1));
				newButton.setLayoutData(new GridData());

				/*
				 * An an event listener that updates the current state of
				 * the ComponentSet whenever a checkbox is selected.
				 */
				newButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						Button button = (Button)(e.getSource());
						String buttonText = button.getText();
						int scopeId = compMgr.getScopeId(buttonText);
						if (button.getSelection()) {
							compSet.add(compId, scopeId);
						} else {
							compSet.remove(compId, scopeId);	
						}
					}
				});
				checkBoxes[i] = newButton;
			}
			
			/* set all the checkboxes to their initial values. */
			refresh();
		}

		/*---------------------------------------------------------------------------------*/

		/**
		 * Refresh this widget with update content from the compSet.
		 */
		public void refresh()
		{
			for (int i = 0; i != Components.SCOPE_MAX; i++) {
				Button thisCheck = checkBoxes[i];
				thisCheck.setSelection(compSet.isMember(compId, i + 1));
			}
		}
		
		/*---------------------------------------------------------------------------------*/		
	}
	
	/*=====================================================================================*
	 * CONSTRUCTOR
	 *=====================================================================================*/
	
	/**
	 * Create a new ComponentFilterDialog object, with the specified set of components
	 * shown as being selected.
	 * @param initialComponents The components that will initially be selected. 
	 */
	public ComponentFilterDialog(ComponentSet initialComponents) {
		super(new Shell());
		
		/* 
		 * Make a copy of the ComponentSet, so we can mess with it as much as we like
		 * without disturbing the original copy (if the user hits "Cancel", we want the
		 * original to be untouched).
		 */
		try {
			compSet = (ComponentSet)initialComponents.clone();
		} catch (CloneNotSupportedException e) {
			throw new FatalError("Cloning not supported on ComponentSet objects");
		}
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * @return The ComponentSet representing the current combination
	 * of components/scopes that are selected in the dialog.
	 */
	public ComponentSet getComponentSet() {
		return compSet;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		
		final Components compMgr = compSet.getBuildStore().getComponents();

		setTitle("Select the components you wish to view:");
		setHelpAvailable(false);

		/* create and format the top-level composite of this dialog */
		Composite composite = (Composite) super.createDialogArea(parent);
		
		/* Add a box containing all the components, and their selectable state */ 
		Composite listComposite = new Composite(composite, SWT.BORDER | SWT.V_SCROLL);
		listComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		listComposite.setLayout(layout);
		
		String componentNames[] = compMgr.getComponents();
		lineWidgets = new CompSelectWidget[componentNames.length];
		for (int i = 0; i != componentNames.length; i++) {
			String compName = componentNames[i];
			lineWidgets[i] = new CompSelectWidget(listComposite, compName);
		}
		return composite;
	}

	/*-------------------------------------------------------------------------------------*/

	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		/* add the standard buttons */
		super.createButtonsForButtonBar(parent);
		
		createButton(parent, IDialogConstants.SELECT_ALL_ID, 
				"Select All", false);		
		createButton(parent, IDialogConstants.DESELECT_ALL_ID, 
				"Deselect All", false);		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* 
	 * (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		
		/* centre the dialog in the middle of the screen */
		Rectangle parentBounds = Display.getCurrent().getBounds();
		newShell.setBounds(
				parentBounds.width / 2 - 250, parentBounds.height / 2 - 200, 500, 400);
		newShell.setText("Select Components to View");
	}

	/*-------------------------------------------------------------------------------------*/

	/* 
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	@Override
	protected void buttonPressed(int buttonId) {
		/* handle standard buttons */
		super.buttonPressed(buttonId);
		
		/*
		 * Handle the "Select all" or "Deselect All" buttons.
		 */
		if ((buttonId == IDialogConstants.SELECT_ALL_ID) ||
				(buttonId == IDialogConstants.DESELECT_ALL_ID)) {
			
			/* 
			 * Create a new (replacement) ComponentSet with no content, but
			 * a default of false (deselect all) or true (select all).
			 */
			BuildStore bs = compSet.getBuildStore();
			compSet = new ComponentSet(bs);
			if (buttonId == IDialogConstants.SELECT_ALL_ID) {
				compSet.setDefault(true);
			}
			
			/*
			 * Refresh all the widgets, using this new ComponentSet.
			 */
			for (int i = 0; i < lineWidgets.length; i++) {
				lineWidgets[i].refresh();
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
