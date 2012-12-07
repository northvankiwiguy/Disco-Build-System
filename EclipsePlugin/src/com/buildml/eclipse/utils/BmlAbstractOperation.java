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

package com.buildml.eclipse.utils;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.buildml.eclipse.ISubEditor;
import com.buildml.eclipse.MainEditor;
import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;

/**
 * An abstract class that wraps any undo/redo operations. This provides
 * extra functionality, such as remember which tab the operation was
 * invoke on.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public abstract class BmlAbstractOperation extends AbstractOperation {

	/*=====================================================================================*
	 * TYPES/FIELDS
	 *=====================================================================================*/

	/** The main BML editor */
	protected MainEditor mainEditor = null;
	
	/** The sub-editor (tab) that this operation was performed on */
	protected ISubEditor subEditor = null;
	
	/** The BuildStore this operation is acting upon */
	protected IBuildStore buildStore = null;
	
	/** The FileMgr this operation is acting upon */
	protected IFileMgr fileMgr = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new BmlAbstractOperation. This class is abstract, so this constructor
	 * can only be invoked by sub-classes.
	 * 
	 * @param label The operation label, as displayed in the "Edit->Undo" menu.
	 */
	public BmlAbstractOperation(String label) {
		super(label);
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
	 * Default implementation of no-arg execute(). Sub-classes can override this if they
	 * actually want to do something different when the command is first executed. By default,
	 * the execute() behaviour is to simply can redo().
	 * 
	 * @return The status of the operation (typically IStatus.OK_STATUS).
	 * @throws ExecutionException Something went wrong during execution.
	 */
	public IStatus execute() throws ExecutionException {
		return redo();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/** 
	 * Undo a previous operation. This method is called by the undo/redo framework. This
	 * method can be overridden, but you should consider overriding the no-argument undo() 
	 * instead.
	 */
	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {

		if (!subEditor.isDisposed()) {
			mainEditor.setActiveEditor(subEditor);
		}
		return undo();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Redo a previously "undone" operation.  This method is called by the undo/redo framework.
	 * This method can be overridden, but you should consider overriding the no-argument redo() 
	 * instead.
	 */
	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {

		if (!subEditor.isDisposed()) {
			mainEditor.setActiveEditor(subEditor);
		}
		return redo();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add this operation to the editor's undo history, then invoke the operation
	 * for the first time.
	 */
	public void recordAndInvoke() {
		
		/* identify the PathMgr to be used */
		buildStore = EclipsePartUtils.getActiveBuildStore();
		if (buildStore == null) {
			throw new FatalBuildStoreError("Couldn't determine active BuildStore");
		}
		fileMgr = buildStore.getFileMgr();
		
		/* add the operation to the undo/redo stack */
		mainEditor = EclipsePartUtils.getActiveMainEditor();
		subEditor = mainEditor.getActiveSubEditor();
		this.addContext(mainEditor.getUndoContext());
				
		/* make it so... */
		IOperationHistory history = OperationHistoryFactory.getOperationHistory();
		try {
			history.execute(this, null, null);
		} catch (ExecutionException e) {
			throw new FatalBuildStoreError("Exception occurred during execution of operation", e);
		}
	}
	
	/*=====================================================================================*
	 * ABSTRACT METHODS
	 *=====================================================================================*/

	/**
	 * Undo a previous operation. This method must be implemented by sub-classes.
	 * 
	 * @return The status of the operation (typically IStatus.OK_STATUS).
	 * @throws ExecutionException Something went wrong during execution.
	 */
	protected abstract IStatus undo() throws ExecutionException;

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Redo a previously "undone" operation. This method must be implemented by sub-classes.
	 * 
	 * @return The status of the operation (typically IStatus.OK_STATUS).
	 * @throws ExecutionException Something went wrong during execution.
	 */
	protected abstract IStatus redo() throws ExecutionException;
	
	/*-------------------------------------------------------------------------------------*/
}
