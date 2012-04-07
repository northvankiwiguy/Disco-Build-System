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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.arapiki.disco.eclipse.Activator;
import com.arapiki.disco.eclipse.preferences.PreferenceConstants;
import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.types.FileRecord;

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
	
	/** The ArrayContentProvider object providing this editor's content */
	FilesEditorContentProvider contentProvider;
	
	/**
	 * The current options setting for this editor. The field contains a bitmap of
	 * OPT_* values.
	 */
	private int editorOptionBits = 0;
	
	/** 
	 * Option to enable coalescing of folders in the file editor. That is, if a folder
	 * contains a single child which is itself a folder, we display both of them on the
	 * same line. For example, if folder "A" has a single child, "B", we display "A/B".
	 */
	public static final int OPT_COALESCE_DIRS		= 1;
	
	/**
	 * Option to display file roots as the top-level items in the editor. Without this
	 * feature enabled, the top-level directory names will be shown.
	 */
	public static final int OPT_SHOW_ROOTS			= 2;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new DiscoFilesEditor instance, using the specified BuildStore as input
	 * @param buildStore The BuildStore to display/edit.
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
		
		contentProvider = new FilesEditorContentProvider(this, fns);
		FilesEditorLabelProvider labelProvider = new FilesEditorLabelProvider(this, fns);
		FilesEditorViewerSorter viewerSorter = new FilesEditorViewerSorter(this, fns);
		filesTreeViewer.setContentProvider(contentProvider);
		filesTreeViewer.setLabelProvider(labelProvider);
		filesTreeViewer.setSorter(viewerSorter);
		
		/* 
		 * Update this editor's option by reading the user-specified values in the
		 * preference store. Also, attach a listener so that we hear about future
		 * changes to the preference store and adjust our options accordingly.
		 */
		updateOptionsFromPreferenceStore();
		Activator.getDefault().getPreferenceStore().
					addPropertyChangeListener(preferenceStoreChangeListener);
		
		/* automatically expand the first few levels of the tree */
		filesTreeViewer.setAutoExpandLevel(2);
		
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

		/* start by displaying from the root (which changes, depending on our options). */
		filesTreeViewer.setInput(contentProvider.getRootElements());
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
	
	/**
	 * Either set of clear specific options (e.g. OPT_COALESCE_DIR or OPT_SHOW_ROOTS) from
	 * this editor's current option settings. This can be used to modify one or more
	 * binary configuration settings in this control.
	 * 
	 * @param optionBits One of more bits that should be either set or cleared from this
	 * 		  editor's options. The state of options that are not specified in this parameter
	 *        will not be changed.
	 * @param enable "true" if the options should be enabled, or "false" if they should be cleared.
	 */
	public void setOption(int optionBits, boolean enable)
	{
		/* if enable is set, then we're adding the new options */
		if (enable) {
			editorOptionBits |= optionBits;
		}
		
		/* else, we're clearing the options */
		else {
			editorOptionBits &= ~optionBits;
		}		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The bitmap of all editor options that are currently set 
	 * (e.g. OPT_COALESCE_ROOTS)
	 */
	public int getOptions()
	{
		return editorOptionBits;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @param optionBit The option to test for.
	 * @return Whether or the specified editor option is set.
	 */
	public boolean isOptionSet(int optionBit)
	{
		return (editorOptionBits & optionBit) != 0;
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Set this editor's options by reading the current values from the preference store.
	 * This should be called when the editor is first created, as well as whenever the
	 * preference store is updated.
	 */
	public void updateOptionsFromPreferenceStore()
	{
		IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();
		
		setOption(OPT_COALESCE_DIRS, 
				prefStore.getBoolean(PreferenceConstants.PREF_COALESCE_DIRS));
		setOption(OPT_SHOW_ROOTS, 
				prefStore.getBoolean(PreferenceConstants.PREF_SHOW_ROOTS));
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Listener to identify changes being made to this plug-in's preference store, typically
	 * as part of editing the Disco preferences (this could change how our editor is displayed).
	 */
	private IPropertyChangeListener preferenceStoreChangeListener =
		new IPropertyChangeListener() {

			/**
			 * Completely redraw the files editor tree, based on the new preference
			 * settings.
			 */
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				updateOptionsFromPreferenceStore();
				filesTreeViewer.setInput(contentProvider.getRootElements());
				filesTreeViewer.refresh();
			}
		};

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
	
		/* remove this preference store listender */
		Activator.getDefault().getPreferenceStore().
				removePropertyChangeListener(preferenceStoreChangeListener);
		super.dispose();
	}

	/*-------------------------------------------------------------------------------------*/
}
