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

package com.buildml.model.undo;

import java.util.ArrayList;

/**
 * An undo/redo operation object that can contain multiple sub-operations. This is useful
 * when a single operation (from the user's perspective) is actually implemented as multiple
 * smaller operations.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class MultiUndoOp implements IUndoOp {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** our internal list of operations, to be done/undone atomically */
	private ArrayList<IUndoOp> opList = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new {@link MultiUndoOp} operation.
	 */
	public MultiUndoOp() {
		opList = new ArrayList<IUndoOp>();
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Add a new operation into the list of operation that this multi-operation contains.
	 * 
	 * @param op A sub operation to be added.
	 */
	public void add(IUndoOp op) {
		opList.add(op);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.utils.BmlAbstractOperation#undo()
	 */
	@Override
	public boolean undo() {
	
		/* we must track whether any of the sub operations actually change the database */
		boolean somethingChanged = false;
		
		/* do all of the sub-operations in reverse order */
		int size = opList.size();
		for (int i = size - 1; i >= 0; i--) {
			if (opList.get(i).undo()) {
				somethingChanged = true;
			}
		}
		return somethingChanged;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.utils.BmlAbstractOperation#redo()
	 */
	@Override
	public boolean redo() {
		
		/* we must track whether any of the sub operations actually change the database */
		boolean somethingChanged = false;

		/* do all of the sub-operations in sequence */
		int size = opList.size();
		for (int i = 0; i < size; i++) {
			if (opList.get(i).redo()) {
				somethingChanged = true;
			}
		}
		return somethingChanged;
	}

	/*-------------------------------------------------------------------------------------*/

}
