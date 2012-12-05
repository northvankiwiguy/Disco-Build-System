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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.utils.UIInteger;
import com.buildml.eclipse.utils.dnd.BuildMLTransfer;
import com.buildml.eclipse.utils.dnd.BuildMLTransferType;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;

/**
 * Functionality related to dragging packages/folders from the outline content view,
 * and onto itself (i.e. rearranging nodes in the tree), or onto other views/editors.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class OutlineDragSource implements DragSourceListener {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The TreeViewer we're dragging an element from */
	private TreeViewer treeViewer;
	
	/** The BuildStore underlying the main editor */
	private IBuildStore buildStore;
	
	/** The PackageMgr associated with the BuildStore */
	private IPackageMgr pkgMgr;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new OutlineDragSource object. There should be exactly one of these objects
	 * for each OutlineContentPage object (view).
	 * 
	 * @param treeViewer The TreeViewer that elements will be dragged from.
	 * @param mainEditor The main BuildML editor associated with the outline content view.
	 */
	public OutlineDragSource(TreeViewer treeViewer, MainEditor mainEditor) {
		this.treeViewer = treeViewer;
		this.buildStore = mainEditor.getBuildStore();
		this.pkgMgr = buildStore.getPackageMgr();
		
		/* register ourselves as the hander for "drag" operations */
		treeViewer.addDragSupport(DND.DROP_COPY | DND.DROP_MOVE, 
				new Transfer[] { BuildMLTransfer.getInstance() },
				this);
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * A drag operation has been started. The user should not be permitted to drag the
	 * root folder, or the <import> package. In these cases, abort the drag operation.
	 */
	@Override
	public void dragStart(DragSourceEvent event) {
		
		/* determine what is being dragged */
		Object node = ((IStructuredSelection)treeViewer.getSelection()).getFirstElement();
		if ((node != null) && (node instanceof UIInteger)) {
			int id = ((UIInteger)node).getId();
			
			/* should we abort? */
			event.doit = ((id != pkgMgr.getRootFolder()) && (id != pkgMgr.getImportPackage())); 
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/** 
	 * This method is called by the drag/drop framework to oobtain the actual "transfer" data
	 * for the tree node that was selected when the drag/drop started. For packages and package
	 * folders, we create a corresponding BuildMLTransferType object. For other things, we abort.
	 */
	@Override
	public void dragSetData(DragSourceEvent event) {
	     if (BuildMLTransfer.getInstance().isSupportedType(event.dataType)) {
	    	 
	    	 /*
	    	  * Identify the currently-selected tree node, and create a corresponding
	    	  * BuildMLTransferType object.
	    	  */
	    	 Object node = ((IStructuredSelection)treeViewer.getSelection()).getFirstElement();
	    	 int transferType;
	    	 
	    	 /* check the type of the tree node (package, folder, etc). */
	    	 if (node instanceof UIPackage) {
	    		 transferType = BuildMLTransferType.TYPE_PACKAGE;
	    	 } else if (node instanceof UIPackageFolder) {
	    		 transferType = BuildMLTransferType.TYPE_PACKAGE_FOLDER;
	    	 } else {
	    		 /* we can't proceed, not a recognized type */
	    		 event.doit = false;
	    		 return;
	    	 }
	    	 
	    	 /*
	    	  * Selected node is valid, create the data to pass to the drag/drop framework.
	    	  */
	    	 int id = ((UIInteger)node).getId();
	    	 event.data = new BuildMLTransferType[] {
    				 new BuildMLTransferType(buildStore.toString(), transferType, id)
    		 };
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
