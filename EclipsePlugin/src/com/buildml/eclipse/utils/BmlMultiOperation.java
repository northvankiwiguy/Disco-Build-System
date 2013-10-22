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

import java.util.ArrayList;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * An undo/redo operation object that can contain multiple sub-operations. This is useful
 * when a single operation (from the user's perspective) is actually implemented as multiple
 * smaller operations.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class BmlMultiOperation extends BmlAbstractOperation {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** our internal list of operations, to be done/undone atomically */
	private ArrayList<BmlAbstractOperation> opList = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link BmlMultiOperation} operation.
	 * @param label The label (that appears in the "undo/redo" menu) for this operation.
	 */
	public BmlMultiOperation(String label) {
		super(label);
		opList = new ArrayList<BmlAbstractOperation>();
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Add a new operation into the list of operation that this multi-operation contains.
	 * @param op A sub operation to be added.
	 */
	public void add(BmlAbstractOperation op) {
		opList.add(op);
	}
	
	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.utils.BmlAbstractOperation#undo()
	 */
	@Override
	protected IStatus undo() throws ExecutionException {

		/* do all of the sub-operations in reverse order */
		int size = opList.size();
		for (int i = size - 1; i >= 0; i--) {
			opList.get(i).undo();
		}
		return Status.OK_STATUS;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.utils.BmlAbstractOperation#redo()
	 */
	@Override
	protected IStatus redo() throws ExecutionException {
		
		/* do all of the sub-operations in sequence */
		int size = opList.size();
		for (int i = 0; i < size; i++) {
			opList.get(i).redo();
		}
		return Status.OK_STATUS;
	}

	/*-------------------------------------------------------------------------------------*/

}
