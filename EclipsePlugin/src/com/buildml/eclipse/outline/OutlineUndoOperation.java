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

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.outline.OutlineUndoOperation.OpType;
import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageRootMgr;

/**
 * An undo/redo operation for tracking changes to the outline content view. This
 * include creating, deleting, moving and renaming packages.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class OutlineUndoOperation extends AbstractOperation {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/**
	 * Enumerated type of all the possibly operations we can perform.
	 */
	public enum OpType {
		
		/** A new package was created */
		OP_NEW_PACKAGE,
		
		/** A new package folder was created */
		OP_NEW_PACKAGE_FOLDER,
		
		/** An empty package or folder was removed */
		OP_REMOVE,
		
		/** A package or folder was renamed */
		OP_RENAME,
		
		/** A package or folder was moved */
		OP_MOVE, 
		
		/** One or both package roots have been modified */
		OP_CHANGE_ROOT
	};
	
	/** The type of operation that this OutlineUndoOperation performs. */ 
	private OpType operation;
	
	/** The name of the package being manipulated */
	private String name;
	
	/** The new name (in case of OP_RENAME) */
	private String newName;
	
	/** The internal ID of the element being manipulated */
	private int nodeId;
	
	/** The node's parent ID */
	private int parentId;
	
	/** The new parent ID (in case of OP_MOVE) */
	private int newParentId;
	
	/** The source path root, before the change */
	private int oldSrcRootPathId;
	
	/** The generated path root, before the change */
	private int oldGenRootPathId;
	
	/** The source path root, after the change */
	private int newSrcRootPathId;
	
	/** The generated path root, after the change */
	private int newGenRootPathId;
	
	/** Is this node a folder? */
	private boolean isFolder;
	
	/** The OutlinePage view that we're manipulating */
	private OutlinePage outlinePage;
	
	/** The main BuildML editor we're manipulating */
	private MainEditor mainEditor;

	/** The PackageMgr that we're manipulating */
	private IPackageMgr pkgMgr;

	/** The PackageRootMgr that we're manipulating */
	private IPackageRootMgr pkgRootMgr;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Private constructor, extracting out commonality between other constructors.
	 * 
	 * @param page 		The OutlinePage associated with this undo/redo operation.
	 * @param label		The undo/redo menu label.
	 * @param operation	The operation being performed.
	 */
	private OutlineUndoOperation(OutlinePage page, String label, OpType operation) {
		super(label);
		
		/* Learn which PackageMgr we are manipulating */
		this.outlinePage = page;
		this.mainEditor = page.getMainEditor();
		IBuildStore buildStore = mainEditor.getBuildStore();
		this.pkgMgr = buildStore.getPackageMgr();
		this.pkgRootMgr = buildStore.getPackageRootMgr();
		
		/* what operation is being performed? */
		this.operation = operation;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Constructor used when creating an OP_NEW_PACKAGE or OP_NEW_PACKAGE_FOLDER operation.
	 * 
	 * @param page		The OutlinePage associated with this undo/redo operation.
	 * @param label		The undo/redo menu label.
	 * @param operation	Must be OP_NEW_PACKAGE or OP_NEW_PACKAGE_FOLDER.
	 * @param name		Name of the newly create package or folder.
	 * @param parentId	ID of parent folder under which the package/folder will be added.
	 */
	public OutlineUndoOperation(OutlinePage page, String label, OpType operation,
								String name, int parentId) {
		this(page, label, operation);

		/* Store the parameters of the operation */
		this.name = name;
		this.parentId = parentId;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Constructor used when creating an OP_REMOVE operation.
	 * 
	 * @param page		The OutlinePage associated with this undo/redo operation.
	 * @param label		The undo/redo menu label.
	 * @param operation	Must be OP_REMOVE.
	 * @param name		Name of the newly create package or folder.
	 * @param parentId	ID of parent folder under which the package/folder will be added.
	 * @param isFolder  The item being removed was a folder (true) or a package (false).
	 */
	public OutlineUndoOperation(OutlinePage page, String label, OpType operation,
								String name, int parentId, boolean isFolder) {
		this(page, label, operation);

		/* Store the parameters of the operation */
		this.name = name;
		this.parentId = parentId;
		this.isFolder = isFolder;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Constructor used when creating an OP_RENAME operation.
	 * 
	 * @param page		The OutlinePage associated with this undo/redo operation.
	 * @param label		The undo/redo menu label.
	 * @param operation	Must be OP_RENAME.
	 * @param nodeId		ID of the package/folder being renamed.
	 * @param oldName	The current name of the package/folder.
	 * @param newName	The new name of the package/folder.
	 */
	public OutlineUndoOperation(OutlinePage page, String label, OpType operation,
								int nodeId, String oldName, String newName) {
		this(page, label, operation);

		/* Store the parameters of the operation */
		this.nodeId = nodeId;
		this.name = oldName;
		this.newName = newName;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Constructor used when creating an OP_MOVE operation.
	 * 
	 * @param page		  The OutlinePage associated with this undo/redo operation.
	 * @param label		  The undo/redo menu label.
	 * @param operation	  Must be OP_MOVE
	 * @param nodeId	  ID of the package/folder being renamed.
	 * @param oldParentId ID of the parent folder.
	 * @param newParentId ID of the new parent folder.
	 */
	public OutlineUndoOperation(OutlinePage page, String label, OpType operation,
								int nodeId, int oldParentId, int newParentId) {
		this(page, label, operation);

		/* Store the parameters of the operation */
		this.nodeId = nodeId;
		this.parentId = oldParentId;
		this.newParentId = newParentId;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Constructor used when creating an OP_CHANGE_ROOT operation.
	 * 
	 * @param page		        The OutlinePage associated with this undo/redo operation.
	 * @param label     	    The undo/redo menu label.
	 * @param operation	        Must be OP_CHANGE_ROOT
	 * @param pkgId             The package being modified.
	 * @param oldSrcRootPathId  The source path root, before the change.
	 * @param oldGenRootPathId  The generated path root, before the change.
	 * @param newSrcRootPathId  The source path root, after the change.
	 * @param newGenRootPathId  The generated path root, after the change.
	 */
	public OutlineUndoOperation(OutlinePage page, String label, OpType operation, int pkgId,
			int oldSrcRootPathId, int oldGenRootPathId,
			int newSrcRootPathId, int newGenRootPathId) {
		this(page, label, operation);
		
		this.nodeId = pkgId;
		this.oldSrcRootPathId = oldSrcRootPathId;
		this.oldGenRootPathId = oldGenRootPathId;
		this.newSrcRootPathId = newSrcRootPathId;
		this.newGenRootPathId = newGenRootPathId;
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Do nothing, since the operation would have already been executed by the time
	 * this method is called.
	 */
	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {
		/* do nothing */
		return Status.OK_STATUS;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Redo a previously undone operation.
	 */
	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {

		switch (operation) {
		
		case OP_NEW_PACKAGE:
			int id = pkgMgr.addPackage(name);
			if (id >= 0) {
				pkgMgr.setParent(id, parentId);
			}
			break;

		case OP_NEW_PACKAGE_FOLDER:
			id = pkgMgr.addFolder(name);
			if (id >= 0) {
				pkgMgr.setParent(id, parentId);
			}
			break;
			
		case OP_RENAME:
			pkgMgr.setName(nodeId, newName);
			break;
			
		case OP_REMOVE:
			id = pkgMgr.getId(name);
			if (id >= 0) {
				pkgMgr.remove(id);
			}
			break;
			
		case OP_MOVE:
			pkgMgr.setParent(nodeId, newParentId);
			break;
			
		case OP_CHANGE_ROOT:
			pkgRootMgr.setPackageRoot(nodeId, IPackageRootMgr.SOURCE_ROOT, newSrcRootPathId);
			pkgRootMgr.setPackageRoot(nodeId, IPackageRootMgr.GENERATED_ROOT, newGenRootPathId);
			break;

		default:
			throw new FatalBuildStoreError("Unhandled operation: " + operation);
		}
		
		return Status.OK_STATUS;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Undo a previously executed operation.
	 */
	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {

		switch (operation) {
		
		case OP_NEW_PACKAGE:
			int id = pkgMgr.getId(name);
			if (id >= 0) {
				pkgMgr.remove(id);
			}
			break;

		case OP_NEW_PACKAGE_FOLDER:
			id = pkgMgr.getId(name);
			if (id >= 0) {
				pkgMgr.remove(id);
			}
			break;
			
		case OP_RENAME:
			pkgMgr.setName(nodeId, name);
			break;
			
		case OP_REMOVE:
			if (isFolder) {
				id = pkgMgr.addFolder(name);
			} else {
				id = pkgMgr.addPackage(name);				
			}
			if (id >= 0) {
				pkgMgr.setParent(id, parentId);
			}
			break;
			
		case OP_MOVE:
			pkgMgr.setParent(nodeId, parentId);
			break;
			
		case OP_CHANGE_ROOT:
			pkgRootMgr.setPackageRoot(nodeId, IPackageRootMgr.SOURCE_ROOT, oldSrcRootPathId);
			pkgRootMgr.setPackageRoot(nodeId, IPackageRootMgr.GENERATED_ROOT, oldGenRootPathId);
			break;

		default:
			throw new FatalBuildStoreError("Unhandled operation: " + operation);
		}
		
		return Status.OK_STATUS;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Record this undo/redo operation in the editor's undo context.
	 * 
	 * @param mainEditor The main BuildML editor that owns the undo context.
	 */
	public void record(MainEditor mainEditor) {

		/* add the operation to the undo/redo stack */
		this.addContext(mainEditor.getUndoContext());
				
		/* make it so... */
		IOperationHistory history = OperationHistoryFactory.getOperationHistory();
		try {
			history.execute(this, null, null);
		} catch (ExecutionException e) {
			throw new FatalBuildStoreError("Exception occurred during execution of operation", e);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
}
