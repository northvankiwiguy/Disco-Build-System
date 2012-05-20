/*******************************************************************************
 * Copyright (c) 2011 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.arapiki.disco.eclipse.tasks;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.buildml.model.BuildStore;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class DiscoTasksEditor extends EditorPart {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The main control in this editor is the TreeViewer for displaying tasks */
	private TreeViewer tasksTreeViewer = null;
	
	/** The BuildStore object we'll use for querying task information */
	private BuildStore buildStore = null;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	public DiscoTasksEditor(BuildStore buildStore, String tabTitle) {
		
		super();
		
		/* Set the name that appears on the editor's window tab */
		setPartName(tabTitle);

		/* save away the BuildStore, so we can query it later */
		this.buildStore = buildStore;	
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		/* not yet implemented - can't save a BuildStore (yet) */
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
		/* not supported */
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {

		/* we only accept files as input to this editor */
		if (! (input instanceof IFileEditorInput)) {
			throw new PartInitException("Invalid Input: Must be IFileEditorInput");
		}
		
		/* save our site and editor input details */
		setSite(site);
		setInput(input);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#isDirty()
	 */
	@Override
	public boolean isDirty() {
		/* not implemented yet, since we can't change the state of the tasks (yet) */
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	@Override
	public boolean isSaveAsAllowed() {
		/* save-as is not permitted */
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		tasksTreeViewer = new TreeViewer(parent, SWT.MULTI | SWT.FULL_SELECTION);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		
		/* when we set the focus on this editor, we actually want the focus on the TreeViewer */
		if (tasksTreeViewer != null){
			tasksTreeViewer.getControl().setFocus();
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
