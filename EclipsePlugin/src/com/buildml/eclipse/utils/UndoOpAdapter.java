/*******************************************************************************
 * Copyright (c) 2013 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.utils;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.buildml.eclipse.ISubEditor;
import com.buildml.eclipse.MainEditor;
import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IBuildStore;
import com.buildml.model.undo.IUndoOp;

/**
 * An abstract class that wraps any undo/redo operations. The IUndoOp interface
 * is defined in the com.buildml.model.undo package, and is independent from Eclipse
 * (can be used outside of Eclipse). This adapter class connects IUndoOp objects into
 * the Eclipse framework, as well as providing extra functionality, such as remembering
 * which editor tab the operation was invoke on.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class UndoOpAdapter extends AbstractOperation {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/** The main BuildML editor we're associated with */
	private MainEditor mainEditor = null;
	
	/** The sub-editor (tab) that this operation was performed on */
	private ISubEditor subEditor = null;
	
	/** The BuildStore this operation is acting upon */
	private IBuildStore buildStore = null;
	
	/** The operation that this adapter is wrapper */
	private IUndoOp operation;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new UndoOpAdapter object to connect an existing IUndoOp into the Eclipse
	 * undo/redo framework.
	 * 
	 * @param label      The operation label, as displayed in the "Edit->Undo" menu.
	 * @param operation  The operation to perform.
	 */
	public UndoOpAdapter(String label, IUndoOp operation) {
		super(label);
		
		buildStore = EclipsePartUtils.getActiveBuildStore();
		if (buildStore == null) {
			throw new FatalBuildStoreError("Couldn't determine active BuildStore");
		}
		
		this.operation = operation;
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Execute the operation for the first time.
	 */
	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		return execute();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Default implementation of no-arg execute(). Make the correct sub-editor visible then
	 * invoke the underlying redo() operation on the IBuildStore.
	 * 
	 * @return The status of the operation (typically Status.OK_STATUS).
	 * @throws ExecutionException Something went wrong during execution.
	 */
	public IStatus execute() throws ExecutionException {
		
		/* bring the appropriate sub-editor to the top */
		if (!subEditor.isDisposed()) {
			if (mainEditor.getActiveSubEditor() != subEditor) {
				mainEditor.setActiveEditor(subEditor);
			}
		}

		/* 
		 * Perform the operation. If there's a change to the database, mark the editor as 
		 * dirty so that it reflects in the editor tab's title with a "*"
		 */
		if (operation.redo()) {
			MainEditor editor = EclipsePartUtils.getActiveMainEditor();
			if (editor != null) {
				editor.markDirty(1);
			}
		}
		return Status.OK_STATUS;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/** 
	 * Undo a previous operation. This method is called by the Eclipse undo/redo framework. 
	 * We ensure the correct editor tab is open, then invoke the underlying undo/redo
	 * on the IBuildStore.
	 */
	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {

		/* bring the appropriate sub-editor to the top */
		if (!subEditor.isDisposed()) {
			if (mainEditor.getActiveSubEditor() != subEditor) {
				mainEditor.setActiveEditor(subEditor);
			}
		}
		
		/* 
		 * Un-perform the operation. If there's a change to the database, mark the editor as 
		 * dirty so that it reflects in the editor tab's title with a "*"
		 */
		if (operation.undo()) {
			MainEditor editor = EclipsePartUtils.getActiveMainEditor();
			if (editor != null) {
				editor.markDirty(-1);
			}
		}
		return Status.OK_STATUS;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Redo a previously "undone" operation.  This method is called by the undo/redo framework
	 * and simply invokes execute().
	 */
	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		return execute();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add this operation to the editor's undo history, then invoke the operation
	 * for the first time.
	 */
	public void invoke() {
		recordAndInvokeCommon(true);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Similar to invoke(), but don't execute the operation (until undo/redo is invoked).
	 */
	public void record() {
		recordAndInvokeCommon(false);
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Common functionality, shared between recordAndInvoke() and recordOnly().
	 * @param executeIt True if the operation should be executed immediately.
	 */
	private void recordAndInvokeCommon(boolean executeIt) {
		
		/* add the operation to the undo/redo stack */
		mainEditor = EclipsePartUtils.getActiveMainEditor();
		subEditor = mainEditor.getActiveSubEditor();
		this.addContext(mainEditor.getUndoContext());
				
		/* make it so... */
		IOperationHistory history = OperationHistoryFactory.getOperationHistory();
		try {
			if (executeIt) {
				history.execute(this, null, null);
			} else {
				history.add(this);
				mainEditor.markDirty(1);
			}
		} catch (ExecutionException e) {
			throw new FatalBuildStoreError("Exception occurred during execution of operation", e);
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
