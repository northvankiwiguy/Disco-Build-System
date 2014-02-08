/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
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

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;

import com.buildml.eclipse.bobj.UIDirectory;
import com.buildml.eclipse.bobj.UIInteger;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageRootMgr;

/**
 * A selection dialog for selecting a directory from the BuildML VFS.
 *  
 * @author Peter Smith <psmith@arapiki.com>
 */
public class VFSTreeSelectionDialog extends ElementTreeSelectionDialog {
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** true, if the user is allowed to select directories (not just files) */
	private boolean allowDirs;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link VFSTreeSelectionDialog} object.
	 * 
	 * @param parent		The shell that owns this dialog.
	 * @param buildStore 	The IBuildStore containing the VFS.
	 * @param message 		A custom message to display at the top of the dialog.
	 * @param allowDirs		true if we should allow selection of directories.
	 * @param allowFiles	true if we should allow selection of files.
	 */
	public VFSTreeSelectionDialog(Shell parent, IBuildStore buildStore, String message, 
									boolean allowDirs, boolean allowFiles) {
		super(parent, new VFSLabelProvider(buildStore), new VFSTreeContentProvider(buildStore, allowFiles));
		
		IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
		this.allowDirs = allowDirs;
		
		setInput(new UIInteger[] { new UIDirectory(pkgRootMgr.getRootPath("root")) });
		
		/* Set title and customized message */
		setTitle("Select a Directory");
		setMessage(message);
		
		/* make sure that double-clicking expands folders, rather than selecting them */
		setDoubleClickSelects(false);
		
		/* by default, only one directory can be selected */
		setAllowMultiple(false);
		
		/* select a default size in characters */
		setSize(100, 30);
	}

	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		Control contents = super.createContents(parent);

		/* customize the dialog box, before it's opened */
		final TreeViewer viewer = getTreeViewer();
		viewer.expandToLevel(5);
		
		/* 
		 * Unless the user is allowed to select directories, disable the OK button if
		 * a directory is selected.
		 */
		if (!allowDirs) {
			viewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					ITreeSelection selection = (ITreeSelection) viewer.getSelection();
					Object element = selection.getFirstElement();
					getButton(OK).setEnabled(!(element instanceof UIDirectory));
				}
			});
		}
		
		return contents;
	}
	
	/*-------------------------------------------------------------------------------------*/	
}
