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

import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.swt.widgets.Item;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.bobj.UIInteger;
import com.buildml.eclipse.outline.OutlineUndoOperation.OpType;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.utils.errors.ErrorCode;

/**
 * This class manages the cell-editing capability of the content outline view. That is,
 * when the user wants to rename a package or folder, this class is used to query and
 * update the underlying BuildStore model.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class OutlineContentCellModifier implements ICellModifier {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The main BuildML editor that we're outlining */
	private MainEditor mainEditor;
	
	/** The BuildStore we can retrieve the outline from */
	private IBuildStore buildStore;
	
	/** The BuildStore's package manager */
	private IPackageMgr pkgMgr;
	
	/** The Outline view page that initiated this cell modify operation */
	private OutlinePage outlinePage;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new OutlineContentCellModifier which will be called into action whenever
	 * the user wishes to rename a package or folder.

	 * @param outlinePage The Outline view page that initiated this cell modify operation.
	 * @param mainEditor The main BuildML editor that we're outlining.
	 */
	public OutlineContentCellModifier(OutlinePage outlinePage, MainEditor mainEditor) {
		this.outlinePage = outlinePage;
		this.mainEditor = mainEditor;
		this.buildStore = mainEditor.getBuildStore();
		this.pkgMgr = buildStore.getPackageMgr();
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object, java.lang.String)
	 */
	@Override
	public boolean canModify(Object element, String property) {
		
		/*
		 * The user should only be provided with the "rename" menu option if they're allowed
		 * to rename the selected element. Therefore, we can just say "true" all the time,
		 * since we know that we'll never be called in the case where the modification is
		 * not possible (such as the root folder, or the <import> package).
		 */
		return true;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object, java.lang.String)
	 */
	@Override
	public Object getValue(Object element, String property) {
		
		/*
		 * For the "NAME" column (actually, there is only one column) retrieve the entry's
		 * name from the BuildStore. This will be the initial text that appears when editing
		 * the cell's content.
		 */
		if ("NAME".equals(property)) {
			int id = ((UIInteger)element).getId();
			String name = pkgMgr.getName(id);
			if (name == null) {
				return "<invalid>";
			}
			return name;
		}
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object, java.lang.String, java.lang.Object)
	 */
	@Override
	public void modify(Object element, String property, Object value) {
		
		if (element instanceof Item) {
			element = ((Item)element).getData();
		}
		
		/*
		 * If the user has finished editing the "NAME" column, we must now place the updated value
		 * back into the database (possibly with errors being reported).
		 */
		if ("NAME".equals(property) || !(value instanceof String)) {
			int id = ((UIInteger)element).getId();
			
			/* has the name actually changed? If not, there's no work to do */
			String newName = (String)value;
			String oldName = pkgMgr.getName(id);
			if ((oldName == null) || !(oldName.equals(newName))) {

				/* Change the name in the database, handling any potential errors */
				int rc = pkgMgr.setName(id, newName);
				if (rc == ErrorCode.ALREADY_USED) {
					AlertDialog.displayErrorDialog("Can't Rename",
							"The name \"" + value + "\" is already in use. " +
							"Choose a different name");
				} else if (rc == ErrorCode.INVALID_NAME) {
					AlertDialog.displayErrorDialog("Can't Rename",
							"The name \"" + value + "\" is not a valid package identifier. " +
							"Choose a different name");
				} 

				/*
				 * Success - refresh the view and add the name change into
				 * the undo/redo history.
				 */
				else {
					OutlineUndoOperation op = new OutlineUndoOperation(outlinePage, "Rename", 
														OpType.OP_RENAME, id, oldName, newName);
					op.record(mainEditor);
					mainEditor.markDirty();
					outlinePage.refresh();				
				}
			}
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
}
