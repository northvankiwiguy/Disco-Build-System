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

package com.arapiki.disco.eclipse.files;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.FileRecord;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class DiscoFilesEditor extends EditorPart {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** This editor's main control is a TreeViewer, for displaying the list of files */
	TreeViewer filesTreeViewer = null;
	
	/** The BuildStore we're editing */
	private BuildStore buildStore = null;
	
	/** The FileNameSpaces object that contains all the file information for this BuildStore */
	private FileNameSpaces fns = null;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new DiscoFilesEditor instance, using the specified BuildStore as input
	 * @param The BuildStore to display/edit.
	 */
	public DiscoFilesEditor(BuildStore buildStore) {
		super();
		
		/* set the name of the tab that this editor appears in */
		setPartName("Build Files");
		
		/* Save away our BuildStore information, for later use */
		this.buildStore = buildStore;
		fns = buildStore.getFileNameSpaces();
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
		/* not implemented */
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {

		/* we can only handle files as input */
		if (! (input instanceof IFileEditorInput)) {
			throw new PartInitException("Invalid Input: Must be IFileEditorInput");
		}
		
		/* save our site and input data */
		setSite(site);
		setInput(input);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#isDirty()
	 */
	@Override
	public boolean isDirty() {
		/* not implemented for now, while this editor is for read-only purposes */
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
		
		/*
		 * The main control in this editor is a TreeViewer that allows the user to
		 * browse the structure of the BuildStore's file system.
		 */
		filesTreeViewer = new TreeViewer(parent, SWT.MULTI | SWT.FULL_SELECTION);
		filesTreeViewer.setContentProvider(new FilesEditorContentProvider(fns));
		filesTreeViewer.setLabelProvider(new FilesEditorLabelProvider(fns));
		
		/* automatically expand the first few levels of the tree */
		filesTreeViewer.setAutoExpandLevel(5);
		
		/* double-clicking on an expandable node will expand/contract that node */
		filesTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				FileRecord node = (FileRecord)selection.getFirstElement();
				if (filesTreeViewer.isExpandable(node)){
					filesTreeViewer.setExpandedState(node, 
							!filesTreeViewer.getExpandedState(node));
				}
			}
		});
		
		/* create the context menu */
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				manager.add(new Separator("discoactions"));
				manager.add(new Separator("additions"));
			}
		});
		Menu menu = menuMgr.createContextMenu(filesTreeViewer.getControl());
		filesTreeViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, filesTreeViewer);
		getSite().setSelectionProvider(filesTreeViewer);

		/* start by displaying from the root */
		FileRecord rootFileRecord = new FileRecord(fns.getRootPath("root")); 
		filesTreeViewer.setInput(new FileRecord[] { rootFileRecord });
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		
		/* if we focus on this editor, we actually focus on the TreeViewer control */
		if (filesTreeViewer != null){
			filesTreeViewer.getControl().setFocus();
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
