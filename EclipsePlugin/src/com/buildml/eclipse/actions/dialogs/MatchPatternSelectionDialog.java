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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.buildml.eclipse.utils.BmlTitleAreaDialog;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.types.ActionSet;

/**
 * A dialog allowing the user to select a pattern that will be used to provide a list
 * of actions that match the pattern. The user must then select which of the matching
 * actions will be used in an operation (such as delete, or make atomic).
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class MatchPatternSelectionDialog extends BmlTitleAreaDialog {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** The buildStore's action manager */
	private IActionMgr actionMgr;
	
	/** The pattern (regular expression) that the user wants to match against */
	private String patternString;
	
	/** Textual string showing what the operation is (e.g. "Delete" or "Make Atomic"). */
	private String operation;
	
	/** The list box widget containing the matching actions */
	private List resultsListBox;
	
	/** The Control where the user enters the match string */
	private Text entryBox;
	
	/** The ActionMgr IDs of the actions being displayed in the resultsListBox */
	private Integer matchingActions[];

	/** The final set of selected actions - only valid after "OK" is pressed */
	private ActionSet resultActionSet;
	
	/*=====================================================================================*
	 * CONSTRUCTOR
	 *=====================================================================================*/
	
	/**
	 * Create a new MatchPatternSelectionDialog.
	 * 
	 * @param buildStore     The IBuildStore that contains the actions.
	 * @param initialPattern The action command string pattern (that the user may edit).
	 * @param operation		 Displayable string explaining what will happen when the
	 *                       user presses OK. For example, "Delete" or "Make Atomic".
	 */
	public MatchPatternSelectionDialog(IBuildStore buildStore, String initialPattern, String operation) {
		super(new Shell(), 0.3, 0.5, 0.5, 0);
			
		this.actionMgr = buildStore.getActionMgr();
		this.patternString = initialPattern;
		this.operation = operation;
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Return the actions that have been selected. This method should only be called
	 * after the "OK" (Delete, Make Atomic) button has been pressed.
	 * 
	 * @return The set of actions that have been selected. 
	 */
	public ActionSet getMatchingActions() {
		return resultActionSet;
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
		 * Introductory label
		 */
		new Label(panel, SWT.NONE).setText("Please specify a pattern to identify which actions " +
				"you wish to " + operation.toLowerCase() + ". Use * as a wildcard:");
		
		/*
		 * Text entry box (for specifying the pattern). We populate the entry box with the
		 * initial pattern provided by our caller.
		 */
		Composite entryPanel = new Composite(panel, SWT.NONE);
		entryPanel.setLayout(new GridLayout(2, false));
		entryPanel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		entryBox = new Text(entryPanel, SWT.NONE);
		entryBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		entryBox.setText(patternString);
		entryBox.selectAll();
		entryBox.setFocus();
		
		/*
		 * Create a "Match" button, which should only be enabled if the
		 * pattern in the entry box is valid. Monitor when the list box
		 * changes so that we can update the button's "enabled" status.
		 */
		final Button matchButton = new Button(entryPanel, SWT.PUSH);
		matchButton.setText("Match");
		matchButton.setEnabled(isValidPattern(patternString));
		entryBox.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				matchButton.setEnabled(isValidPattern(entryBox.getText()));
			}
		});
		
		/*
		 * Once the pattern is matched, the user must select which of the matching
		 * actions are to be deleted. State this in a label.
		 */
		new Label(panel, SWT.NONE).setText("Select which of the matching actions you wish to " + 
												operation.toLowerCase() + ":");
		
		/*
		 * A list box (which consumes the rest of the dialog box) shows all the matching
		 * actions. The "OK" (Delete, Make Atomic) button is only enabled if at
		 * least one action is highlighted.
		 */
		resultsListBox = new List(panel, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		resultsListBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		resultsListBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getButton(OK).setEnabled(resultsListBox.getSelectionIndices().length >= 1);
			}
		});
		populateListWithMatches(patternString);
		
		/*
		 * When the "Match" button is pressed, populate the list box.
		 */
		matchButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				populateListWithMatches(entryBox.getText());
			}
		});
		
		return parent;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {

		/* compute the ActionSet of selected actions */
		resultActionSet = new ActionSet(actionMgr);
		int[] selected = resultsListBox.getSelectionIndices();
		for (int i = 0; i < selected.length; i++) {
			int actionId = matchingActions[selected[i]];
			resultActionSet.add(actionId);
		}
		
		/* destroy the dialog and all Controls it contains */
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
		
		newShell.setText("Select Actions by Pattern");
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
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		
		/* 
		 * Replace the standard "OK" label with the name of the operation we're performing
		 * (e.g "Delete" or "Make Atomic".
		 */
		createButton(parent, IDialogConstants.OK_ID, operation,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * When the user pressed the "Match" button, query the IBuildStore for matching actions,
	 * then populate the list with the command strings.
	 * 
	 * @param pattern The pattern to match against.
	 */
	private void populateListWithMatches(String pattern) {

		/* replace * with % - to satisfy the matching algorithm */
		pattern = pattern.replace('*', '%');
		
		/* search for matches */
		matchingActions = actionMgr.getActionsWhereSlotIsLike(IActionMgr.COMMAND_SLOT_ID, pattern);
		
		/* add matches to results list box */
		resultsListBox.removeAll();
		for (int i = 0; i < matchingActions.length; i++) {
			String cmdString = (String) actionMgr.getSlotValue(matchingActions[i], IActionMgr.COMMAND_SLOT_ID);
			resultsListBox.add(cmdString);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether a pattern is "valid" (i.e. we're willing to search for actions
	 * based on this pattern).
	 * 
	 * @param pattern The pattern to be evaluated.
	 * @return True if valid, else false.
	 */
	protected boolean isValidPattern(String pattern) {
		return pattern.length() >= 3;
	}

	/*-------------------------------------------------------------------------------------*/
}
