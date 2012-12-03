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

package com.buildml.eclipse.outline;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import com.buildml.eclipse.MainEditor;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;

/**
 * An Eclipse view, providing content for the "Outline View" associated with the BuildML
 * editor.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class OutlinePage extends ContentOutlinePage {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The SWT TreeViewer that is displayed within this outline view page */
	private TreeViewer treeViewer;
	
	/** The IBuildStore associated with this content view */
	private IBuildStore buildStore;
	
	/** The IPackageMgr that we'll be displaying information from */
	private IPackageMgr pkgMgr;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new OutlinePage object. There should be exactly one of these objects for
	 * each BuildML MainEditor object.
	 * 
	 * @param mainEditor The associate MainEditor object.
	 * 
	 */
	public OutlinePage(MainEditor mainEditor) {
		super();
		
		/* our outline view will display information from this IPackageMgr object. */
		buildStore = mainEditor.getBuildStore();
		pkgMgr = buildStore.getPackageMgr();
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.contentoutline.ContentOutlinePage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		
		/* 
		 * Configure the view's (pre-existing) TreeViewer with necessary helper objects that
		 * will display the BuildML editor's package structure.
		 */
		treeViewer = getTreeViewer();
		treeViewer.setContentProvider(new OutlineContentProvider(pkgMgr));
		treeViewer.setLabelProvider(new OutlineLabelProvider(pkgMgr));
		treeViewer.addSelectionChangedListener(this);
		treeViewer.setInput(new UIPackageFolder[] { new UIPackageFolder(pkgMgr.getRootFolder()) });
		treeViewer.expandToLevel(2);
		
		/*
		 * When the user double-clicks on a folder name, automatically expand the content
		 * of that folder.
		 */
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				Object node = selection.getFirstElement();
			
				if (treeViewer.isExpandable(node)){
					treeViewer.setExpandedState(node, 
							!treeViewer.getExpandedState(node));
				}
			}
		});
	}
	
	/*-------------------------------------------------------------------------------------*/
}
