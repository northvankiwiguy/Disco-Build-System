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

package com.buildml.eclipse.actions;

import java.util.Iterator;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;

import com.buildml.eclipse.bobj.UIAction;
import com.buildml.eclipse.bobj.UIInteger;
import com.buildml.eclipse.utils.dnd.BuildMLTransfer;
import com.buildml.eclipse.utils.dnd.BuildMLTransferType;
import com.buildml.model.IBuildStore;

/**
 * Functionality related to dragging actions from the actions editor onto other
 * editors (including the Outline view).
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class ActionsEditorDragSource implements DragSourceListener {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The ActionEditor's TreeViewer we're dragging an element from */
	private TreeViewer treeViewer;
	
	/** The BuildStore underlying the action editor */
	private IBuildStore buildStore;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new ActionsEditorDragSource object. There should be exactly one of these objects
	 * for each ActionsEditor object.
	 * 
	 * @param treeViewer The TreeViewer that elements will be dragged from.
	 * @param buildStore The IBuildStore that contains the elements being dragged.
	 */
	public ActionsEditorDragSource(TreeViewer treeViewer, IBuildStore buildStore) {
		this.treeViewer = treeViewer;
		this.buildStore = buildStore;
		
		/* register ourselves as the handler for "drag" operations */
		treeViewer.addDragSupport(DND.DROP_COPY | DND.DROP_MOVE, 
				new Transfer[] { BuildMLTransfer.getInstance() },
				this);
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * A drag operation has been started. We need to validate whether the element being
	 * dragged is something we can handle. For an ActionsEditor, we can drag one or more
	 * UIAction objects.
	 */
	@Override
	public void dragStart(DragSourceEvent event) {
				
		/* 
		 * Determine what is being dragged. Assuming everything in the selection is a UIAction,
		 * we're OK to proceed.
		 */
		IStructuredSelection selection = ((IStructuredSelection)treeViewer.getSelection());
		@SuppressWarnings("unchecked")
		Iterator<Object> iter = selection.iterator();
		while (iter.hasNext()) {
			Object node = iter.next();
			if ((node == null) || (!(node instanceof UIAction))) {
				
				/* something in the selection is not a UIAction - we can't drag */
				event.doit = false;
				return;
			}
		}
		
		/* OK, we can drag the selection */
		event.doit = true;
	}

	/*-------------------------------------------------------------------------------------*/

	/** 
	 * This method is called by the drag/drop framework to obtain the actual "transfer" data
	 * for the tree node that was selected when the drag/drop started. For UIAction, we create
	 * a corresponding BuildMLTransferType object. For other things, we abort.
	 */
	@Override
	public void dragSetData(DragSourceEvent event) {
		if (BuildMLTransfer.getInstance().isSupportedType(event.dataType)) {

			/*
			 * Identify the currently-selected tree node, and create a corresponding
			 * BuildMLTransferType object for each selected element. We know how many
			 * elements are in the selection, so create a BuildMLTransferType for each.
			 */
			IStructuredSelection selection = ((IStructuredSelection)treeViewer.getSelection());
			BuildMLTransferType data[] = new BuildMLTransferType[selection.size()];

			/* populate the array */	    	 
			int elementNum = 0;
			@SuppressWarnings("unchecked")
			Iterator<Object> iter = selection.iterator();
			while (iter.hasNext()) {
				Object node = iter.next();
				int transferType;

				/* check the type of the tree node (must be a UIAction). */
				if (node instanceof UIAction) {
					transferType = BuildMLTransferType.TYPE_ACTION;
				} else {
					/* we can't proceed, not a recognized type */
					event.doit = false;
					return;
				}

				/*
				 * The selected node is valid, create the data to pass to the drag/drop framework.
				 */
				int id = ((UIInteger)node).getId();
				data[elementNum++] = new BuildMLTransferType(buildStore.toString(), transferType, id);
			};
			
			/* 
			 * We now have a complete array of BuildMLTransferType[], each entry represents
			 * a unique UIAction that was part of our selection.
			 */
			event.data = data;
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Called by the drag/drop framework when the drag operation has completed.
	 */
	@Override
	public void dragFinished(DragSourceEvent event) {
		/* nothing - the drop target handles changing the model */
	}
	
	/*-------------------------------------------------------------------------------------*/
}
