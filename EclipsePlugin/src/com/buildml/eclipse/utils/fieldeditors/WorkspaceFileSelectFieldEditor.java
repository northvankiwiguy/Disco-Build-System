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
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import com.buildml.eclipse.utils.EclipsePartUtils;

/**
 * A FieldEditor for entering/browsing to a workspace file.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class WorkspaceFileSelectFieldEditor extends StringButtonFieldEditor {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/** File name filters to apply when browsing directories (null == no filter) */
	private String filters[];
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a {@link WorkspaceFileSelectFieldEditor}. Note that this class modifies the
	 * layout of the parent widget, so make sure you create a special parent Composite
	 * specifically to contain this FieldEditor.
	 * 
	 * @param prefName The name of the preference that this FieldEditor is modifying.
	 * @param label    The label to appear before the text-entry box.
	 * @param parent   The parent composite that this FieldEditor will be added to.
	 * @param filters  File name filters to apply (e.g. [ "*.bml" ]), or null to
	 * 				   not filter anything.
	 * 
	 */
	public WorkspaceFileSelectFieldEditor(String prefName, String label, Composite parent,
										  String filters[]) {
		super(prefName, label, parent);
		
		this.filters = filters;
		setChangeButtonText("&Browse...");
		
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * @return The absolute file system path of the selected file.
	 */
	public String getAbsoluteFilePath() {
		return EclipsePartUtils.workspaceRelativeToAbsolutePath(getStringValue());
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The currently-selected file, as an Eclipse Resource, or null if it's
	 * not valid.
	 */
	public IResource getResource() {
		String path = getStringValue();
		IWorkspaceRoot rootResource = ResourcesPlugin.getWorkspace().getRoot();
		return rootResource.findMember(new Path(path));
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

		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
		dialog.setFilterPath(EclipsePartUtils.workspaceRelativeToAbsolutePath("/"));
		if (filters != null) {
			dialog.setFilterExtensions(filters);
		}
		String file = dialog.open();
		
		/* convert to workspace-relative file, or leave unchanged if outside of workspace */
		if (file != null) {
			String workspaceFile = EclipsePartUtils.absoluteToWorkspaceRelativePath(file);
			if (workspaceFile != null) {
				return workspaceFile;
			}
		}
		return getStringValue();
	}

	/*-------------------------------------------------------------------------------------*/
}
