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
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link VFSTreeSelectionDialog} object.
	 * 
	 * @param parent		The shell that owns this dialog.
	 * @param buildStore 	The IBuildStore containing the VFS.
	 * @param message 		A custom message to display at the top of the dialog.
	 * @param showFiles		true if we should show files as well as directories.
	 */
	public VFSTreeSelectionDialog(Shell parent, IBuildStore buildStore, String message, boolean showFiles) {
		super(parent, new VFSLabelProvider(buildStore), new VFSTreeContentProvider(buildStore, showFiles));
		
		IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
		
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
		getTreeViewer().expandToLevel(5);
		return contents;
	}
	
	/*-------------------------------------------------------------------------------------*/	
}
