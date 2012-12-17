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
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

import com.buildml.eclipse.utils.EclipsePartUtils;

/**
 * A FieldEditor for entering/browsing to a workspace directory/project.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class WorkspaceDirSelectFieldEditor extends StringButtonFieldEditor {

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a {@link WorkspaceDirSelectFieldEditor}. Note that this class modifies the
	 * layout of the parent widget, so make sure you create a special parent Composite
	 * specifically to contain this FieldEditor.
	 * 
	 * @param prefName The name of the preference that this FieldEditor is modifying.
	 * @param label    The label to appear before the text-entry box.
	 * @param parent   The parent composite that this FieldEditor will be added to.
	 * 
	 */
	public WorkspaceDirSelectFieldEditor(String prefName, String label, Composite parent) {
		super(prefName, label, parent);
		setChangeButtonText("&Browse...");
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * @return The absolute file system path of the selected directory.
	 */
	public String getAbsoluteDirectoryPath() {
		return EclipsePartUtils.workspaceRelativeToAbsolutePath(getStringValue());
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The currently-selected folder, as an Eclipse Resource, or null if it's
	 * not valid.
	 */
	public IResource getResource() {
		String path = getStringValue();
		IWorkspaceRoot rootResource = ResourcesPlugin.getWorkspace().getRoot();
		if (path.equals("/")) {
			return rootResource;
		} else {
			return rootResource.findMember(new Path(path));
		}
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
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(
				getShell(), ResourcesPlugin.getWorkspace().getRoot(), false,
				"Select project/folder:");
		if (dialog.open() == ContainerSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				return (((Path) result[0]).toString());
			}
		}
		return getStringValue();
	}

	/*-------------------------------------------------------------------------------------*/
}
