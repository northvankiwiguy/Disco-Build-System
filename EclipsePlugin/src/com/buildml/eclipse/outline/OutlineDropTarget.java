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

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.utils.UIInteger;
import com.buildml.eclipse.utils.dnd.BuildMLTransfer;
import com.buildml.eclipse.utils.dnd.BuildMLTransferType;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.utils.errors.ErrorCode;

/**
 * Functionality related to dropping items into the outline content view, possibly onto
 * itself (i.e. rearranging nodes in the tree).
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class OutlineDropTarget extends ViewerDropAdapter {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The TreeViewer we're dropping an element onto */
	private TreeViewer treeViewer;
	
	/** The BuildStore underlying the main editor */
	private IBuildStore buildStore;
	
	/** The main BuildML editor we're operating on */
	private MainEditor mainEditor;
	
	/** The PackageMgr associated with the BuildStore */
	private IPackageMgr pkgMgr;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new OutlineDropTarget object. There should be exactly one of these objects
	 * for each OutlineContentPage object (view).
	 *
	 * @param treeViewer The TreeViewer that elements will be dragged from.
	 * @param mainEditor The main BuildML editor associated with the outline content view. 
	 */
	public OutlineDropTarget(TreeViewer treeViewer, MainEditor mainEditor) {
		super(treeViewer);
		this.treeViewer = treeViewer;
		this.mainEditor = mainEditor;
		this.buildStore = mainEditor.getBuildStore();
		this.pkgMgr = this.buildStore.getPackageMgr();
	
		/* register ourselves with the drag/drop framework - we can receive drops */
		treeViewer.addDropSupport(DND.DROP_MOVE | DND.DROP_COPY, 
				new Transfer[] { BuildMLTransfer.getInstance() },
				this);
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Provide feedback to the drag/drop system on whether the currently hovered-over item
	 * is a valid drop target. In our case, all tree nodes are valid drop targets (even though
	 * we may actually drop into the node's parent folder).
	 */
	@Override
	public boolean validateDrop(Object target, int operation, TransferData transferType) {
		/*
		 * From a visual feedback perspective, an item can be dropped if it's a BuildMLTransfer
		 * type (file, action, package, etc). Later, in the performDrop() method, we can look
		 * at the actual source/target data to make sure the drop is legal.
		 */
		return BuildMLTransfer.getInstance().isSupportedType(transferType);
	}

	/*-------------------------------------------------------------------------------------*/

	/** 
	 * A drag/drop operation has completed, and we must now update the model and the view
	 * appropriately. At this point, we also do extra validation to make sure we're allowed
	 * to drop the source onto the target.
	 */
	@Override
	public boolean performDrop(Object data) {

		/* the target must be a package or folder */
		UIInteger target = (UIInteger)getCurrentTarget();
		if (target == null) {
			return false;
		}
		
		/* we only support drops of BuildMLTransferType arrays of size 1 */
		if (data instanceof BuildMLTransferType[]) {
			
			BuildMLTransferType myTypes[] = (BuildMLTransferType[])data;
			if (myTypes.length == 1) {
				
				/* for now, we only support dropping of packages/folders */
				if ((myTypes[0].type != BuildMLTransferType.TYPE_PACKAGE) &&
					(myTypes[0].type != BuildMLTransferType.TYPE_PACKAGE_FOLDER)) {
					return false;
				}
				
				/* Require that the drop is from the same BuildStore */
				if (!buildStore.toString().equals(myTypes[0].owner)){
					return false;
				}
				
				/* 
				 * Determine the folder where the item will be dropped. If the target
				 * is a package (not a folder), we'll drop the item into the target's 
				 * parent folder.
				 */
				int targetId = target.getId();
				if (!pkgMgr.isFolder(targetId)) {
					targetId = pkgMgr.getParent(targetId);
					if (targetId == ErrorCode.NOT_FOUND) {
						return false;
					}
				}
				
				/* If the parent won't actually be changing, we don't need to do anything. */
				if (pkgMgr.getParent(myTypes[0].id) == targetId) {
					return false;
				}
				
				/*
				 * Attempt to move the incoming item to a new parent. On error,
				 * we fail silently (aborting the drag).
				 */
				if (pkgMgr.setParent(myTypes[0].id, targetId) != ErrorCode.OK) {
					return false;
				}
				
				/* the drag worked, so refresh the tree */
				treeViewer.refresh();
				treeViewer.setExpandedState(new UIInteger(targetId), true);
				mainEditor.markDirty();
				// TODO: undo/redo
				return true;
			}
		}

		/* transfer failed */
		return false;
	}

	/*-------------------------------------------------------------------------------------*/
}
