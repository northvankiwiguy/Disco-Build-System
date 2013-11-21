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

package com.buildml.eclipse.packages.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.PatternSyntaxException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;

import com.buildml.eclipse.bobj.UIConnection;
import com.buildml.eclipse.bobj.UIFileActionConnection;
import com.buildml.eclipse.bobj.UIMergeFileGroupConnection;
import com.buildml.eclipse.filegroups.FileGroupChangeOperation;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.BmlPropertyPage;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileGroupMgr;
import com.buildml.utils.regex.BmlRegex;
import com.buildml.utils.regex.RegexChain;

/**
 * An Eclipse "property" page that allows viewing/editing of a connection's properties.
 * Since connections are generally quite simple, this properties page focuses mostly
 * on the filter file group that can decorate a connection.
 * 
 * Objects of this class are referenced in the plugin.xml file and are dynamically
 * created when the properties dialog is opened for a UIAction object.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class ConnectionPropertyPage extends BmlPropertyPage {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** ID of the input group that we're filtering */
	private int inputFileGroupId;
	
	/** ID of the filter group itself (contains the patterns) */
	private int filterFileGroupId;
	
	/** The IBuildStore we query information from */
	private IBuildStore buildStore = null;
	
	/** The IFileGroupMgr we query information from */
	private IFileGroupMgr fileGroupMgr = null;

	/** The Control for the left panel's list box */
	private List leftListBox;
	
	/** The Control for the middle panel's list box */
	private List middleListBox;

	/** The Control for the right panel's list box */
	private List rightListBox;

	/** The various buttons */
	@SuppressWarnings("javadoc")
	private Button includeButton, excludeButton, addButton, editButton, removeButton,
					moveUpButton, moveDownButton;

	/** The list of patterns shown in the middle list box (this is what we're editing) */
	private ArrayList<String> filterFilePaths;
	
	/** The initial list of patterns when we opened the properties box (for restoring) */
	private ArrayList<String> initialFilterFilePaths;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new ConnectionPropertyPage() object.
	 */
	public ConnectionPropertyPage() {
		/* nothing */
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * Because of the size of this property page, we make it twice the width as normal.
	 */
	@Override
	public Point computeSize() {
		
		Point standardSize = super.computeSize();		
		return new Point(standardSize.x * 2, standardSize.y);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * The OK button has been pressed in the properties box. Save all the field values
	 * into the database. This is done via the undo/redo stack.
	 */
	@Override
	public boolean performOk() {
		
		/* create an undo/redo operation that will invoke the underlying database changes */
		FileGroupChangeOperation op = new FileGroupChangeOperation("Modify Filter", filterFileGroupId);
		op.recordMembershipChange(initialFilterFilePaths, filterFilePaths);
		op.recordAndInvoke();
		return super.performOk();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * The "restore default" button has been pressed, so return the filter list back to
	 * its original state.
	 */
	@Override
	protected void performDefaults() {
		
		/* restore back to the original list */
		filterFilePaths.clear();
		filterFilePaths.addAll(initialFilterFilePaths);
		
		/* refresh the view */
		refreshMiddleList();
		setMiddlePanelButtons();
		refreshRightList();
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
		
		UIConnection connection = 
				(UIConnection)GraphitiUtils.getBusinessObjectFromElement(getElement(), UIConnection.class);
		if (connection == null) {
			return null;
		}

		setTitle("Connection Properties:");
		
		/* if there's no connection set, just display an informational message and exit */
		if (!connection.hasFilter()) {
			return displayNoFilterSet(parent, connection);
		}
		
		/* what are we displaying? What filter group is filter what input group? */
		filterFileGroupId = connection.getFilterGroupId();
		if (connection instanceof UIFileActionConnection) {
			inputFileGroupId = ((UIFileActionConnection)connection).getFileGroupId();
		} else {
			inputFileGroupId = ((UIMergeFileGroupConnection)connection).getSourceFileGroupId();
		}
		buildStore = EclipsePartUtils.getActiveBuildStore();
		fileGroupMgr = buildStore.getFileGroupMgr();
		
		/* fetch the initial list of filters */
		getInitialPatternList();		
		
		/*
		 * We have three columns to display. The left column is the input file group (that
		 * we're filtering). The middle column contains the filter patterns that we're editing.
		 * The right column contains the output from the filter (a subset of the left column).
		 */
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(3, true);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		panel.setLayout(layout);
		
		Control leftPanel = createLeftPanel(panel);
		leftPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Control middlePanel = createMiddlePanel(panel);
		middlePanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Control rightPanel = createRightPanel(panel);
		rightPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		return panel;
	}


	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/
	
	/**
	 * Compute the initial list of patterns. We need to have this in our local data
	 * structures so that we can modify it as much as we like, without writing it back to
	 * the BuildStore (which will trigger lots of notifications). Once OK is pressed, that's
	 * when it's written back to the database.
	 */
	private void getInitialPatternList() {
	
		/* fetch the initial list of patterns */
		String initialPatterns[] = fileGroupMgr.getPathStrings(filterFileGroupId);
		filterFilePaths = new ArrayList<String>();
		filterFilePaths.addAll(Arrays.asList(initialPatterns));
		
		/* make a copy, if we need to restore later */
		initialFilterFilePaths = new ArrayList<String>();
		initialFilterFilePaths.addAll(Arrays.asList(initialPatterns));
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Create the left-side panel. This panel features a list box showing the input files
	 * (before the filter). The user can't change this list, but they can choose to "include"
	 * or "exclude" these files in the pattern (middle) panel.
	 * 
	 * @param parent	The parent control to populate with widgets. 
	 * @return The top-level control we created.
	 */
	private Control createLeftPanel(Composite parent) {
		
		/* 
		 * The left panel has a single column, with intro text at the top, 
		 * the list box (of input files) in the middle, and the buttons
		 * at the bottom.
		 */
		Composite subPanel = new Composite(parent, SWT.None);
		GridLayout layout = new GridLayout(1, true);
		layout.marginHeight = 10;
		layout.marginWidth = 10;
		subPanel.setLayout(layout);
		
		/* 
		 * One line of introductory text.
		 */
		Label headerText = new Label(subPanel, SWT.None);
		headerText.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
		headerText.setText("Input Files (Before Filtering):");
		
		/*
		 * Create and populate list box (it never changes beyond this point).
		 */
		leftListBox = new List(subPanel, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		leftListBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		String [] inputFilePaths = fileGroupMgr.getExpandedGroupFiles(inputFileGroupId);
		if (inputFilePaths != null) {
			for (int i = 0; i < inputFilePaths.length; i++) {
				leftListBox.add(inputFilePaths[i]);
			}
		}
		
		/* If an item is selected, we may (or may not) need to enable buttons */
		leftListBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setLeftPanelButtons();
				middleListBox.deselectAll();
			}
		});
		
		/*
		 * Create the "include" and "exclude" buttons in a row by themselves.
		 */
		Composite buttonRow = new Composite(subPanel, SWT.None);
		buttonRow.setLayout(new FillLayout());
		buttonRow.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		includeButton = new Button(buttonRow, SWT.PUSH);
		includeButton.setText("Include File(s)");
		includeButton.setEnabled(false);
		includeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleIncludeExcludeButton(true);
			}
		});
		excludeButton = new Button(buttonRow, SWT.PUSH);
		excludeButton.setText("Exclude File(s)");
		excludeButton.setEnabled(false);
		excludeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleIncludeExcludeButton(false);
			}
		});
		
		return subPanel;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create the middle panel. This panel features a list box showing the filter patterns.
	 * The user can interactively add/remove/modify these patterns. Patterns can be reordered
	 * (within the list), although doing so won't impact the filter's result.
	 * 
	 * @param parent	The parent control to populate with widgets. 
	 * @return The top-level control we created.
	 */
	private Control createMiddlePanel(Composite parent) {
		
		/* 
		 * The middle panel has a single column, with intro text at the top, 
		 * the list box (of patterns) in the middle, and the buttons at the bottom.
		 */
		Composite subPanel = new Composite(parent, SWT.None);
		GridLayout layout = new GridLayout(1, true);
		layout.marginHeight = 10;
		layout.marginWidth = 10;
		subPanel.setLayout(layout);
		
		/* 
		 * One line of introductory text.
		 */
		Label headerText = new Label(subPanel, SWT.None);
		headerText.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
		headerText.setText("Filter Patterns:");
		
		/*
		 * Create the listbox (which will change often).
		 */
		middleListBox = new List(subPanel, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		middleListBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		/*
		 * Create the "add", "edit", "remove", "up" and "down" buttons in a row by themselves.
		 */
		Composite buttonRow = new Composite(subPanel, SWT.None);
		buttonRow.setLayout(new FillLayout());
		buttonRow.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		addButton = new Button(buttonRow, SWT.PUSH);
		addButton.setText("Add");
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleAddEditButton(true);
			}
		});
		
		editButton = new Button(buttonRow, SWT.PUSH);
		editButton.setText("Edit");
		editButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleAddEditButton(false);
			}
		});
		
		removeButton = new Button(buttonRow, SWT.PUSH);
		removeButton.setText("Remove");
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleRemoveButton();
			}
		});
		
		moveUpButton = new Button(buttonRow, SWT.PUSH);
		moveUpButton.setText("Move Up");
		moveUpButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleMoveButton(-1);
			}
		});
		
		moveDownButton = new Button(buttonRow, SWT.PUSH);
		moveDownButton.setText("Move Down");
		moveDownButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleMoveButton(1);
			}
		});

		/* display the content of our listbox and set button states appropriately */
		refreshMiddleList();
		setMiddlePanelButtons();

		/*
		 * Handle selection of items in the list box. All buttons are initially disabled
		 * until a list box item is selected.
		 */
		middleListBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setMiddlePanelButtons();
				leftListBox.deselectAll();
			}
		});

		return subPanel;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create the right panel. This panel features a list box showing the output of the filter.
	 * The user can no interact with this panel at all, except for scrolling.
	 * 
	 * @param parent	The parent control to populate with widgets. 
	 * @return The top-level control we created.
	 */
	private Control createRightPanel(Composite parent) {
		
		/* 
		 * The right panel has a single column, with intro text at the top, 
		 * the list box (of patterns) in the middle, and a filler at the bottom
		 * so that all three list boxes (left, middle, right) will have the same height.
		 */
		Composite subPanel = new Composite(parent, SWT.None);
		GridLayout layout = new GridLayout(1, true);
		layout.marginHeight = 10;
		layout.marginWidth = 10;
		subPanel.setLayout(layout);
		
		/* 
		 * One line of introductory text.
		 */
		Label headerText = new Label(subPanel, SWT.None);
		headerText.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false));
		headerText.setText("Output Files (After Filtering):");
		
		/*
		 * Create and populate the list box control.
		 */
		rightListBox = new List(subPanel, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
		rightListBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		refreshRightList();
		
		/*
		 * Create a filler button - to make all list boxes the same height.
		 */
		Composite buttonRow = new Composite(subPanel, SWT.None);
		buttonRow.setLayout(new FillLayout());
		buttonRow.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Button filler = new Button(buttonRow, SWT.PUSH);
		filler.setText("");
		filler.setVisible(false);
		
		return subPanel;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Display a message informing the user that no filter is set and therefore no editing
	 * of properties is possible.
	 * 
	 * @param parent	 The parent control for our content.
	 * @param connection The business object we're viewing (UIFileActionConnection etc).
	 * @return			 The new controls we created.
	 */
	private Control displayNoFilterSet(Composite parent, UIConnection connection) {
		
		/* create a panel in which all sub-widgets are added. */
		Composite panel = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		panel.setLayout(layout);

		String extraMessage = "You do not currently have a filter attached.";
		if ((connection instanceof UIFileActionConnection) &&
			((UIFileActionConnection)connection).getDirection() == UIFileActionConnection.OUTPUT_FROM_ACTION) {
			extraMessage = "It is not possible to attach a filter on this connection (output from an action).";
		}
		
		/* if we don't have a filter, there's nothing we can do */
		Label message = new Label(panel, SWT.None);
		message.setText("This properties page can be used to view/edit the content of " + 
				"connection filters. " + extraMessage);
		Label filler = new Label(panel, SWT.None);
		filler.setLayoutData(new GridData(SWT.None, SWT.None, true, true));
		return panel;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Refresh the content of the middle list box (with the filter's output). This
	 * list will need to be redrawn whenever a pattern is added/removed/modified.
	 */
	private void refreshMiddleList(){
		
		middleListBox.removeAll();
		for (int i = 0; i < filterFilePaths.size(); i++) {
			String parts[] = filterFilePaths.get(i).split(":");
			String prefix = null;
			if (parts[0].equals("ia")) {
				prefix = "Include";
			} else if (parts[0].equals("ea")) {
				prefix = "Exclude";
			}
			if (prefix != null) {
				middleListBox.add(prefix + ": " + parts[1]);
			}
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Refresh the content of the right-side list box (with the filter's output). This
	 * list content may change every time the filter's patterns change. It would be
	 * nice to use the IFileGroupMgr to do this for us, but that triggers all sorts
	 * of change events that we don't want to see right now.
	 */
	private void refreshRightList(){
		
		/* compile the filter's file paths into a regular expression chain */
		RegexChain chain;
		try {
			chain = BmlRegex.compileRegexChain(filterFilePaths.toArray(new String[0]));
		} catch (PatternSyntaxException ex) {
			return;
		}
		
		/* fetch the input paths... */
		String [] inputFilePaths = fileGroupMgr.getExpandedGroupFiles(inputFileGroupId);

		/* Compute the filtered paths */
		String filteredPaths[] = BmlRegex.filterRegexChain(inputFilePaths, chain);
	
		/* display them in the list box */
		rightListBox.removeAll();
		if (filteredPaths != null) {
			for (int i = 0; i < filteredPaths.length; i++) {
				rightListBox.add(filteredPaths[i]);
			}
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * The user has selected one or more items in the left panel. We must therefore enable/disable
	 * the include/exclude buttons as appropriate. We only enable the "include" button if there
	 * is at least one selection that isn't already in the pattern. Likewise, we only enable the
	 * "exclude" button if the selected file already has an associated pattern.
	 */
	private void setLeftPanelButtons() {
		boolean includeOK = false, excludeOK = false;
		
		/* given the selection, are we able to include/exclude the selected files? */
		String selection[] = leftListBox.getSelection();
		for (int i = 0; i < selection.length; i++) {
			/* search the patterns to see if the selected file(s) are already there. */
			String thisFile = selection[i];
			includeOK = (findPattern("ia:" + thisFile) == -1);
			excludeOK = (findPattern("ea:" + thisFile) == -1);
		}
		includeButton.setEnabled(includeOK);
		excludeButton.setEnabled(excludeOK);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * The middle panel has somehow changed, and we need to figure out the appropriate state
	 * for all the buttons.
	 */
	private void setMiddlePanelButtons() {
		int index = middleListBox.getSelectionIndex();
		if (index == -1) {
			editButton.setEnabled(false);
			removeButton.setEnabled(false);
			moveUpButton.setEnabled(false);
			moveDownButton.setEnabled(false);
			return;
		}
		
		editButton.setEnabled(true);
		removeButton.setEnabled(true);
		int filterSize = filterFilePaths.size();
		moveUpButton.setEnabled((filterSize > 1) && (index != 0));
		moveDownButton.setEnabled((filterSize > 1) && (index != filterSize-1));
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * The user has pressed the "include" button or the "exclude" button. If we're including,
	 * we'll remove any explicit excludes and then add our new include pattern. The opposite
	 * is true for excluding.
	 * @param doInclude True if the user pressed "include", else false.
	 */
	protected void handleIncludeExcludeButton(boolean doInclude) {
		
		String selection[] = leftListBox.getSelection();
		for (int i = 0; i < selection.length; i++) {
			String thisPattern = selection[i];
			
			/* first, check if there's already a pattern that we need to remove */
			int removeIndex = findPattern((doInclude ? "ea" : "ia") + ":" + thisPattern);
			if (removeIndex != -1){
				filterFilePaths.remove(removeIndex);
			}
			
			/* if it doesn't already exist, add the new pattern at the end of the filter group */
			String patternToAdd = (doInclude ? "ia" : "ea") + ":" + thisPattern;
			int addIndex = findPattern(patternToAdd);
			if (addIndex == -1) {
				filterFilePaths.add(patternToAdd);
			}
		}
		
		/* update the view, setting buttons to appropriate states */
		refreshMiddleList();
		setMiddlePanelButtons();
		refreshRightList();
		
		/* update button appropriately */
		includeButton.setEnabled(!doInclude);
		excludeButton.setEnabled(doInclude);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Handle the "move up" and "move down" buttons.
	 * 
	 * @param direction The direction of the move (-1 for up, 1 for down).
	 */
	protected void handleMoveButton(int direction) {
		int index = middleListBox.getSelectionIndex();
		if (index == -1) {
			return;
		}
		if ((index + direction < 0) || (index + direction >= filterFilePaths.size())) {
			return;
		}
		String value = filterFilePaths.get(index);
		filterFilePaths.remove(index);
		filterFilePaths.add(index + direction, value);
		
		refreshMiddleList();
		middleListBox.setSelection(index + direction);
		setMiddlePanelButtons();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Handle the "remove" button. Note that we can never truly be empty, since the "ia:**"
	 * pattern will always be there if nothing else is.
	 */
	protected void handleRemoveButton() {
		int index = middleListBox.getSelectionIndex();
		if (index != -1) {
			filterFilePaths.remove(index);
			if (filterFilePaths.isEmpty()) {
				filterFilePaths.add("ia:**");
			}
			
			/* refresh to see the new list, but keep the "next" item selected */
			refreshMiddleList();
			middleListBox.setSelection((index < filterFilePaths.size() ? index : index - 1));
			setMiddlePanelButtons();
			refreshRightList();
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Handle the "add" and "edit" buttons.
	 * 
	 * @param createNew  True if we should create a new pattern, else false to edit currently
	 * 					 selected item.
	 */
	protected void handleAddEditButton(boolean createNew) {
		AlertDialog.displayErrorDialog("Not Implemented", "This feature is not yet implemented.");
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether a pattern is already in the filter group.
	 * 
	 * @param patternToMatch	The pattern to look for.
	 * @return The 0-based index of the pattern, or -1 if not found.
	 */
	private int findPattern(String patternToMatch) {
		int size = filterFilePaths.size();
		for (int i = 0; i < size; i++) {
			if (patternToMatch.equals(filterFilePaths.get(i))) {
				return i;
			}			
		}
		return -1;
	}	
	
	/*-------------------------------------------------------------------------------------*/

}
