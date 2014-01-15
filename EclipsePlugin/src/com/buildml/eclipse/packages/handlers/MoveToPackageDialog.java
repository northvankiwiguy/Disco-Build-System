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

package com.buildml.eclipse.packages.handlers;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.buildml.eclipse.bobj.UIPackage;
import com.buildml.eclipse.bobj.UIPackageFolder;
import com.buildml.eclipse.outline.OutlineContentProvider;
import com.buildml.eclipse.outline.OutlineLabelProvider;
import com.buildml.eclipse.utils.BmlTitleAreaDialog;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;

/**
 * A Dialog class for allowing the user to select a package they want to 
 * move package members into.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class MoveToPackageDialog extends BmlTitleAreaDialog {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** The IBuildStore that contains all our package information */
	private IBuildStore buildStore;
	
	/** The TreeViewer used for displaying the hierarchy of packages */
	private TreeViewer viewer;
	
	/** 
	 * The ID of the package that was selected by the user. This should only be queried
	 * by calling getPackageId() after the "OK" button is pressed.
	 */
	private int savedPackageId = -1;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new MoveToPackageDialog box.
	 * 
	 * @param buildStore 	The IBuildStore that we query information from.
	 */
	public MoveToPackageDialog(IBuildStore buildStore) {
		super(new Shell(), 0.3, 0.5, 0.5, 0.5);
		
		this.buildStore = buildStore;
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/
	
	/**
	 * @return The package ID selected by the user, or -1 if no package has yet been selected.
	 */
	public int getPackageId() {
		return savedPackageId;
	}
	
	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		
		IPackageMgr pkgMgr = buildStore.getPackageMgr();
		
		setTitle("Select Package to Move Actions/Files into:");
		setHelpAvailable(false);
		
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout());
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.verticalIndent = 20;
		gd.horizontalIndent = 0;
		container.setLayoutData(gd);
		
		/* display a single TreeViewer that lists all the packages */
		viewer = new TreeViewer(container, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.SINGLE);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.setContentProvider(new OutlineContentProvider(pkgMgr, false));
		viewer.setLabelProvider(new OutlineLabelProvider(pkgMgr));
		viewer.setInput(new UIPackageFolder[] { new UIPackageFolder(pkgMgr.getRootFolder()) });
		viewer.expandToLevel(3);

		/*
		 * When a node in the tree is selected, decide whether it's a package or a folder and
		 * adjust the "OK" button enablement status accordingly.
		 */
		viewer.addSelectionChangedListener( new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				Object node = selection.getFirstElement();
				getButton(OK).setEnabled(node instanceof UIPackage);
			}
		});
		
		/*
		 * When the user double-clicks on a folder name, automatically expand the content
		 * of that folder. If they double-click on a package name, they're essentially
		 * pressing "OK".
		 */
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				Object node = selection.getFirstElement();
			
				if (viewer.isExpandable(node)){
					viewer.setExpandedState(node, !viewer.getExpandedState(node));
				}
				
				/* else, perform the "OK" operation */
				else {
					okPressed();
				}
			}
		});
		return container;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		
		ITreeSelection selection = (ITreeSelection) viewer.getSelection();
		Object selectedNode = selection.getFirstElement();
		if (selectedNode instanceof UIPackage) {
			savedPackageId = ((UIPackage)selectedNode).getId();
		}

		/* now dispose the window */
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
		
		newShell.setText("Move Actions/Files Into Package");
	}

	/*-------------------------------------------------------------------------------------*/
}
