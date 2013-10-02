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

package com.buildml.eclipse.utils.fieldeditors;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.swt.widgets.Composite;
import com.buildml.eclipse.bobj.UIDirectory;
import com.buildml.eclipse.utils.dialogs.VFSTreeSelectionDialog;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;

/**
 * A FieldEditor for entering/browsing to a VFS directory.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class VFSDirSelectFieldEditor extends StringButtonFieldEditor {

	/** BuildStore containing the VFS */
	private IBuildStore buildStore;
	
	/** The FileMgr within the BuildStore */
	private IFileMgr fileMgr;
	
	/** The text message to be displayed at the top of the editor */
	private String message;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a {@link VFSDirSelectFieldEditor}. Note that this class modifies the
	 * layout of the parent widget, so make sure you create a special parent Composite
	 * specifically to contain this FieldEditor.
	 * 
	 * @param prefName   The name of the preference that this FieldEditor is modifying.
	 * @param label      The label to appear before the text-entry box.
	 * @param parent     The parent composite that this FieldEditor will be added to.
	 * @param buildStore The BuildStore that contains the VFS.
	 * @param message 	 The message to display at the top of the dialog window.
	 * 
	 */
	public VFSDirSelectFieldEditor(String prefName, String label, Composite parent, 
			IBuildStore buildStore, String message) {
		super(prefName, label, parent);
		
		this.buildStore = buildStore;
		this.fileMgr = buildStore.getFileMgr();
		this.message = message;
		
		setChangeButtonText("&Browse...");
	}

	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/

	/**
	 * Open the directory browser dialog, and if the user selected a directory (as opposed
	 * to pressing Cancel), update the text field with the path to that directory.
	 */
	@Override
	protected String changePressed() {
		VFSTreeSelectionDialog dialog = 
				new VFSTreeSelectionDialog(getShell(), buildStore, message, false);
		
		if (dialog.open() == VFSTreeSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				UIDirectory selection = (UIDirectory)result[0];
				int selectionId = selection.getId();
				return fileMgr.getPathName(selectionId);
			}
		}
		return getStringValue();
	}

	/*-------------------------------------------------------------------------------------*/
}
