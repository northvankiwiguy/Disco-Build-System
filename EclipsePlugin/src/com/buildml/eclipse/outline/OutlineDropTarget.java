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
import com.buildml.eclipse.actions.ActionChangeOperation;
import com.buildml.eclipse.bobj.UIInteger;
import com.buildml.eclipse.outline.OutlineUndoOperation.OpType;
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
	
	/** The OutlinePage view we're supporting */
	private OutlinePage outlinePage;
	
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
	 * @param outlinePage The outline content view. 
	 */
	public OutlineDropTarget(TreeViewer treeViewer, OutlinePage outlinePage) {
		super(treeViewer);
		this.treeViewer = treeViewer;
		this.outlinePage = outlinePage;
		this.mainEditor = outlinePage.getMainEditor();
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
	 * to drop the source onto the target. The source is either a UIAction, a UIPackage
	 * or UIPackageFolder, and the destination is a UIPackage or UIPackageFolder.
	 */
	@Override
	public boolean performDrop(Object data) {

		/* do we need to refresh the outline view as a result of the drop? */
		boolean outlineRefreshNeeded = false;
		
		/* is there anything about the editor that is now dirty? (to be saved) */
		boolean editorDirty = false;
		
		/* the target must be a package or folder, regardless of the source. */
		UIInteger target = (UIInteger)getCurrentTarget();
		if (target == null) {
			return false;
		}
		
		/* 
		 * We only support drops of BuildMLTransferType arrays. We'll treat each
		 * element of the array as a separate drop, and if one drop fails, we
		 * silently continue with other drops.
		 */
		if (data instanceof BuildMLTransferType[]) {
			BuildMLTransferType myTypes[] = (BuildMLTransferType[])data;
			for (int index = 0; index != myTypes.length; index++) {

				/* Require that the drop is from the same BuildStore as the target */
				if (buildStore.toString().equals(myTypes[index].owner)){
					
					/* 
					 * If dropping a UIAction, we change the package that the action
					 * resides in. 
					 */
					if (myTypes[index].type == BuildMLTransferType.TYPE_ACTION) {

						/* perform the drop - on failure, skip to next element */
						if (performDropUIAction(myTypes[index], target)) {
							editorDirty = true;
						}
					}
					
					/*
					 * If dropping a UIPackage or UIPackageFolder, we restructure
					 * that package's hierarchy within the outline view.
					 */
					else if ((myTypes[index].type == BuildMLTransferType.TYPE_PACKAGE) ||
							(myTypes[index].type == BuildMLTransferType.TYPE_PACKAGE_FOLDER)) {
						
						/* perform the drop - on failure, skip to next element */
						if (performDropUIPackage(myTypes[index], target)) {
							outlineRefreshNeeded = true;
							editorDirty = true;
						}
					}
				
				}
			}
		}

		/* one or more drop operations impacted the outline tree view, so refresh the tree */
		if (outlineRefreshNeeded) {
			treeViewer.refresh();
		}
		if (editorDirty) {
			mainEditor.markDirty();
		}
		
		/* transfer probably succeeded (although individual drops may have failed) */
		return true;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Perform a drop of a UIAction into a UIPackage. This has the effect of changing the
	 * package that the actiobn is contained within.
	 * 
	 * @param droppedObj BuildStore actionId of the action being dropped.
	 * @param targetObj BuildStore packageId of the package being dropped into.
	 * @return True on success, or false if the drop failed for any reason.
	 */
	private boolean performDropUIAction(BuildMLTransferType droppedObj, UIInteger targetObj) {

		int targetPackageId = targetObj.getId();
		int droppedActionId = droppedObj.id;
		
		/* 
		 * We can only drop UIAction into real packages (not folders), and not into the 
		 * <import> package.
		 */
		if (!pkgMgr.isValid(targetPackageId) || pkgMgr.isFolder(targetPackageId) ||
				(targetPackageId == pkgMgr.getImportPackage())) {
			return false;
		}
		
		/* determine the action's current package */
		int currentPackageId = pkgMgr.getActionPackage(droppedActionId);
		if (currentPackageId == ErrorCode.NOT_FOUND) {
			return false;
		}
				
		/* 
		 * Record the undo/redo information. This will also move the action into the destination
		 * package. We have no refreshing or updating to do, since the ActionMgr will notify any
		 * listeners of the change and they can refresh themselves if needed.
		 */
		ActionChangeOperation operation = new ActionChangeOperation("change package", droppedActionId);
		operation.recordPackageChange(currentPackageId, targetPackageId);
		operation.recordAndInvoke();
		
		return true;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Perform the drop of a UIPackage or UIPackageFolder onto a UIPackage or UIPackageFolder.
	 * This restructures the hierarchy of packages as they appear in the Outline view. Dropping
	 * onto a UIPackage will reparent at the same location in the tree as the target UIPackage.
	 * 
	 * @param droppedObj BuildStore packageId of the UIPackage/UIPackageFolder being dropped.
	 * @param targetObj The BuildStore packageId of the UIPackage/UIPackageFolder being dropped into.
	 * @return True on success, or false if the drop failed for any reason.
	 */
	private boolean performDropUIPackage(BuildMLTransferType droppedObj, UIInteger targetObj) {
		/* 
		 * Determine the folder where the item will be dropped. If the target
		 * is a package (not a folder), we'll drop the item into the target's 
		 * parent folder.
		 */
		int targetId = targetObj.getId();
		if (!pkgMgr.isFolder(targetId)) {
			targetId = pkgMgr.getParent(targetId);
			if (targetId == ErrorCode.NOT_FOUND) {
				return false;
			}
		}
		
		/* 
		 * If the dropped object is being dropped onto its current parent (instead of a 
		 * new parent), we don't need to do anything.
		 */
		int nodeId = droppedObj.id;
		int parentId = pkgMgr.getParent(nodeId);
		if (parentId == targetId) {
			return false;
		}
		
		/*
		 * Attempt to move the incoming item to a new parent. On error,
		 * we fail silently (aborting the drag).
		 */
		if (pkgMgr.setParent(nodeId, targetId) != ErrorCode.OK) {
			return false;
		}

		/* set the parent folder's state to "expanded" to show the dropped element */
		treeViewer.setExpandedState(new UIInteger(targetId), true);

		/* 
		 * Each drop of a package into a package folder is treated as an individual operation,
		 * even if multiple were dropped at the same time.
		 */
		OutlineUndoOperation op = 
				new OutlineUndoOperation(outlinePage, "Move", OpType.OP_MOVE, 
									     nodeId, parentId, targetId);
		op.record(mainEditor);
		return true;
	}

	/*-------------------------------------------------------------------------------------*/
}
