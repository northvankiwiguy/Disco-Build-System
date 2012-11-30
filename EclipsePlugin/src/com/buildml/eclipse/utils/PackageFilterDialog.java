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
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
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
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.model.types.PackageSet;

/**
 * A Dialog allowing the user to select (with checkboxes) a subset of all the
 * BuildStore's packages. This class is used in the Eclipse plugin, wherever
 * it's necessary to select a combination of packages.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class PackageFilterDialog extends BmlTitleAreaDialog {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/**
	 * The input PackageSet that this Dialog will display. This remains unmodified since
	 * we create a clone of this object before modifying it.
	 */
	private PackageSet pkgSet;
	
	/**
	 * We need one widget for each line of the table.
	 */
	private PkgSelectWidget lineWidgets[];
	
	/** Default width of the dialog box. */
	private int dialogWidth;
	
	/** Default height of the dialog box. */
	private int dialogHeight;
	
	/** Should the dialog box show scope names? */
	private boolean showScopes;
	
	/*=====================================================================================*
	 * NESTED CLASS - PkgSelectWidget
	 *=====================================================================================*/
	
	/**
	 * A nested class representing a single line in the PackageFilterDialog window.
	 * Each PkgSelectWidget contains the package's name, and a checkbox for each of
	 * the scopes.
	 * 
	 * @author "Peter Smith <psmith@arapiki.com>"
	 */
	private class PkgSelectWidget extends Composite {

		/*---------------------------------------------------------------------------------*/

		/** Each scope name (except for None) has its own checkbox widget. */
		private Button checkBoxes[] = null;
		
		/** This ID of the package that this widget represents. */
		int pkgId;
		
		/*---------------------------------------------------------------------------------*/

		/**
		 * Create a new instance of the PkgSelectWidget class.
		 * @param parent The PackageFilterDialog composite that this widget is pat of.
		 * @param pkgName The name of the package to be displayed.
		 */
		public PkgSelectWidget(
				Composite parent, 
				String pkgName)
		{	
			super(parent, 0);
			
			final IPackageMgr pkgMgr = pkgSet.getBuildStore().getPackageMgr();
			
			/* get the scope name, and prepare for a checkbox for each */
			checkBoxes = new Button[IPackageMgr.SCOPE_MAX];
			
			/* we'll use the package's internal ID when accessing the package Set */
			pkgId = pkgMgr.getId(pkgName);
			
			/* make sure this widget is stretched to the full width of the shell */
			this.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
			
			/* 
			 * Each line will contain a section for the package name,
			 * and a section for each of the scopes (excluding the None scope).
			 */
			GridLayout layout = new GridLayout();
			layout.numColumns = 1 + getNumScopes();
			layout.marginHeight = 0;
			setLayout(layout);
			
			/* The package name takes as much of the left side as possible */
			Label label1 = new Label(this, 0);
			label1.setText(pkgName);
			label1.setLayoutData(new GridData(SWT.LEFT, SWT.NONE, true, false));

			/*
			 * On the right side, create a new checkbox button for each scope
			 * (except for "None"). We take note of each widget (in the checkBoxes)
			 * array so we can set/refresh it later.
			 */
			int numScopes = getNumScopes();
			for (int i = 0; i < numScopes; i++) {
				
				/* the scope check boxes are on the right side */
				final Button newButton = new Button(this, SWT.CHECK);
				if (showScopes) {
					newButton.setText(pkgMgr.getScopeName(i + 1));
				} else {
					newButton.setText("Show");					
				}
				newButton.setLayoutData(new GridData());

				/*
				 * An an event listener that updates the current state of
				 * the PackageSet whenever a checkbox is selected.
				 */
				newButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						Button button = (Button)(e.getSource());
						String buttonText = button.getText();
						int scopeId;
						if (showScopes) {
							scopeId = pkgMgr.getScopeId(buttonText);
						} else {
							scopeId = IPackageMgr.SCOPE_PUBLIC;
						}
						if (button.getSelection()) {
							pkgSet.add(pkgId, scopeId);
						} else {
							pkgSet.remove(pkgId, scopeId);	
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
		 * Refresh this widget with update content from the pkgSet.
		 */
		public void refresh()
		{
			int numScopes = getNumScopes();
			for (int i = 0; i != numScopes; i++) {
				Button thisCheck = checkBoxes[i];
				if (showScopes) {
					thisCheck.setSelection(pkgSet.isMember(pkgId, i + 1));
				} else {
					thisCheck.setSelection(pkgSet.isMember(pkgId, IPackageMgr.SCOPE_PUBLIC));
				}
			}
		}
		
		/*---------------------------------------------------------------------------------*/

		/**
		 * @return The number of scope names that should be displayed (e.g. "Public" and 
		 * "Private"), or just "Show".
		 */
		private int getNumScopes() {
			if (showScopes) {
				return IPackageMgr.SCOPE_MAX;
			} else {
				return 1;
			}
		}
		
		/*---------------------------------------------------------------------------------*/		
	}
	
	/*=====================================================================================*
	 * CONSTRUCTOR
	 *=====================================================================================*/
	
	/**
	 * Create a new PackageFilterDialog object, with the specified set of packages
	 * shown as being selected.
	 * @param initialPkgs The packages that will initially be selected.
	 * @param showScopes True if our dialog box should show the package scope names (public
	 *    and private), or just allow the whole package to be selected.
	 */
	public PackageFilterDialog(PackageSet initialPkgs, boolean showScopes) {
		super(new Shell());
		
		this.showScopes = showScopes;
		
		/* 
		 * Make a copy of the PackageSet, so we can mess with it as much as we like
		 * without disturbing the original copy (if the user hits "Cancel", we want the
		 * original to be untouched).
		 */
		try {
			pkgSet = (PackageSet)initialPkgs.clone();
		} catch (CloneNotSupportedException e) {
			throw new FatalError("Cloning not supported on PackageSet objects");
		}
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * @return The PackageSet representing the current combination
	 * of components/scopes that are selected in the dialog.
	 */
	public PackageSet getPackageSet() {
		return pkgSet;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		
		final IPackageMgr pkgMgr = pkgSet.getBuildStore().getPackageMgr();

		setTitle("Select the packages you wish to view:");
		setHelpAvailable(false);

		/* create and format the top-level composite of this dialog */
		Composite composite = (Composite) super.createDialogArea(parent);
		
		/* 
		 * Add a box containing all the packages, and their selectable state. We put
		 * everything inside a ScrolledComposite, so that the scrollbar is managed for us.
		 */
		ScrolledComposite scrolledComposite = new ScrolledComposite(composite, SWT.BORDER | SWT.V_SCROLL);
		scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scrolledComposite.setLayout(new GridLayout());
		scrolledComposite.setAlwaysShowScrollBars(true);
		
		/* Add another composite (within the ScrolledComposite) that will contain the list of package names */
		Composite listComposite = new Composite(scrolledComposite, SWT.NONE);
		listComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		listComposite.setLayout(layout);
		
		/* Add a new line for each package name */
		String packageNames[] = pkgMgr.getPackages();
		lineWidgets = new PkgSelectWidget[packageNames.length];
		for (int i = 0; i != packageNames.length; i++) {
			String pkgName = packageNames[i];
			lineWidgets[i] = new PkgSelectWidget(listComposite, pkgName);
		}
		
		/* tell the ScrolledComposite what it's managing, and how big it should be. */
		scrolledComposite.setContent(listComposite);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setMinWidth(0);
		listComposite.setSize(listComposite.computeSize(dialogWidth, SWT.DEFAULT));
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
		dialogHeight = parentBounds.height / 2;
		dialogWidth = parentBounds.width / 4;
		newShell.setBounds(
				parentBounds.width / 2 - (dialogWidth / 2), 
				parentBounds.height / 2 - (dialogHeight / 2), dialogWidth, dialogHeight);
		newShell.setText("Select Packages to View");
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
			 * Create a new (replacement) PackageSet with no content, but
			 * a default of false (deselect all) or true (select all).
			 */
			IBuildStore bs = pkgSet.getBuildStore();
			pkgSet = new PackageSet(bs);
			if (buttonId == IDialogConstants.SELECT_ALL_ID) {
				pkgSet.setDefault(true);
			}
			
			/*
			 * Refresh all the widgets, using this new PackageSet.
			 */
			for (int i = 0; i < lineWidgets.length; i++) {
				lineWidgets[i].refresh();
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
