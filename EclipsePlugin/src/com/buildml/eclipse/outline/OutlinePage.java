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

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationListener;
import org.eclipse.jface.viewers.ColumnViewerEditorDeactivationEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.UIInteger;
import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.utils.errors.ErrorCode;

/**
 * An Eclipse view, providing content for the "Outline View" associated with the BuildML
 * editor.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class OutlinePage extends ContentOutlinePage {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The SWT TreeViewer that is displayed within this outline view page */
	private TreeViewer treeViewer;
	
	/** The main BuildML editor that we're showing the outline of */
	private MainEditor mainEditor;
	
	/** The IBuildStore associated with this content view */
	private IBuildStore buildStore;
	
	/** The IPackageMgr that we'll be displaying information from */
	private IPackageMgr pkgMgr;
	
	/** The tree element that's currently selected */
	private UIInteger selectedNode = null;
	
	/** Based on the current tree selection, can the selected node be removed? */
	private boolean removeEnabled = false;
		
	/** Based on the current tree selection, can the selected node be renamed? */
	private boolean renameEnabled = false;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new OutlinePage object. There should be exactly one of these objects for
	 * each BuildML MainEditor object.
	 * 
	 * @param mainEditor The associate MainEditor object.
	 * 
	 */
	public OutlinePage(MainEditor mainEditor) {
		super();
		
		/* our outline view will display information from this IPackageMgr object. */
		this.mainEditor = mainEditor;
		buildStore = mainEditor.getBuildStore();
		pkgMgr = buildStore.getPackageMgr();
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.views.contentoutline.ContentOutlinePage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		
		/* 
		 * Configure the view's (pre-existing) TreeViewer with necessary helper objects that
		 * will display the BuildML editor's package structure.
		 */
		treeViewer = getTreeViewer();
		treeViewer.setContentProvider(new OutlineContentProvider(pkgMgr));
		treeViewer.setLabelProvider(new OutlineLabelProvider(pkgMgr));
		treeViewer.addSelectionChangedListener(this);
		treeViewer.setInput(new UIPackageFolder[] { new UIPackageFolder(pkgMgr.getRootFolder()) });
		treeViewer.expandToLevel(2);
		
		/*
		 * Create the context menu. It'll be populated by the rules in plugin.xml.
		 */
		MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				manager.add(new Separator("buildmladditions"));
			}
		});
		Menu menu = menuMgr.createContextMenu(treeViewer.getControl());
		treeViewer.getControl().setMenu(menu);
		getSite().registerContextMenu("org.eclipse.ui.views.ContentOutline", menuMgr, treeViewer);
		getSite().setSelectionProvider(treeViewer);

		/*
		 * When the user double-clicks on a folder name, automatically expand the content
		 * of that folder.
		 */
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				Object node = selection.getFirstElement();
			
				if (treeViewer.isExpandable(node)){
					treeViewer.setExpandedState(node, 
							!treeViewer.getExpandedState(node));
				}
			}
		});
		
		/*
		 * Configure the ability to edit cells in the package/folder tree. The 
		 * OutlineContentCellModifier class does most of the hard work.
		 */
		treeViewer.setColumnProperties(new String[] { "NAME" });
		treeViewer.setCellModifier(new OutlineContentCellModifier(treeViewer, mainEditor));
		treeViewer.setCellEditors(new CellEditor [] { new TextCellEditor(treeViewer.getTree()) });
		
		/*
		 * Arrange it so that cell editing is only possible when we call editElement(), rather
		 * than when the user clicks on the label.
		 */
		treeViewer.getColumnViewerEditor().addEditorActivationListener(
				new ColumnViewerEditorActivationListener() {
					public void beforeEditorActivated(ColumnViewerEditorActivationEvent event) {
						if (event.eventType != ColumnViewerEditorActivationEvent.PROGRAMMATIC) {
							event.cancel = true;
						}
					}
					public void beforeEditorDeactivated(ColumnViewerEditorDeactivationEvent event) {}
					public void afterEditorDeactivated(ColumnViewerEditorDeactivationEvent event) {}
					public void afterEditorActivated(ColumnViewerEditorActivationEvent event) {}
				});

		/*
		 * Listen to our own selection events, which is necessary to learn which element is
		 * selected when we want to add or delete elements.
		 */
		addSelectionChangedListener(this);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add a new package or folder to the BuildML build system. This is a UI method which will
	 * update the view and report necessary error messages. The new package or folder will be
	 * added at the same level in the tree as the currently selected element (or under
	 * the root, if there's no selection).
	 * 
	 * @param createFolder If true, create a folder, else create a package.
	 */
	public void newPackageOrFolder(boolean createFolder) {
				
		/* figure out where in the tree we'll add the new node */
		int parentId = getParentForNewNode();
		String newName = getNameForNewNode();
		
		/* add the new package/folder; it'll be positioned under the top root (for now) */
		int id;
		if (createFolder) {
			id = pkgMgr.addFolder(newName);
		} else {
			id = pkgMgr.addPackage(newName);			
		}
		
		/* these errors should never occur */
		if ((id == ErrorCode.INVALID_NAME) || (id == ErrorCode.ALREADY_USED)) {
			throw new FatalBuildStoreError("Unable to create new package/folder: " + newName);
		}

		/* 
		 * Move the new node underneath its destined parent. Error cases have already
		 * been handled, so if we see an error, that's a coding problem.
		 */
		if (pkgMgr.setParent(id, parentId) != ErrorCode.OK) {
			throw new FatalBuildStoreError("Couldn't move new tree element under parent");
		}

		/* 
		 * Refresh the tree so that the new folder appears. We also need to make sure that
		 * the parent node is expanded, since it might not be right now.
		 */
		UIPackageFolder parentNode = new UIPackageFolder(parentId);
		treeViewer.setExpandedState(parentNode, true);
		treeViewer.refresh();
		mainEditor.markDirty();

		/* now mark the new node for editing, to encourage the user to change the name */
		UIInteger newNode;
		if (createFolder) {
			newNode = new UIPackageFolder(id);
		} else {
			newNode = new UIPackage(id);			
		}
		treeViewer.editElement(newNode, 0);
		
		// TODO: add undo/redo support.
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Remove the currently-selected package or package folder from the BuildML build system.
	 * This is a UI method which will update the view and report necessary error messages.
	 * Packages can only be removed if they don't contain any files/actions. Package folders
	 * can only be removed if they don't contain any sub-packages (or package folders).
	 */
	public void remove() {

		/* if nothing is selected, we can't delete anything */
		if (selectedNode == null) {
			return;
		}
		
		/* determine the name and type of the thing we're deleting */
		int id = selectedNode.getId();
		String name = pkgMgr.getName(id);
		boolean isFolder = pkgMgr.isFolder(id);
		
		int status = AlertDialog.displayOKCancelDialog("Are you sure you want to delete the " +
				"\"" + name + "\" " + (isFolder ? "folder?" : "package?"));
		if (status == IDialogConstants.CANCEL_ID) {
			return;
		}
		
		/* go ahead and remove it, possibly with an error code being returned */
		int rc = pkgMgr.remove(id);

		/* An error occured while removing... */
		if (rc != ErrorCode.OK) {
			
			/* for some reason, the element couldn't be deleted */
			if (rc == ErrorCode.CANT_REMOVE) {

				/* give an appropriate error message */
				if (selectedNode instanceof UIPackage) {
					AlertDialog.displayErrorDialog("Can't Delete Package",
							"The selected package couldn't be deleted because it still " +
							"contains files and actions.");

				} else {
					AlertDialog.displayErrorDialog("Can't Delete Package Folder",
							"The selected package folder couldn't be deleted because it still " +
							"contains sub-packages");
				}
			} else {
				throw new FatalBuildStoreError("Unexpected error when attempting to delete package " +
						"or package folder");
			}
		}
		
		/* Success - element removed. Update view accordingly. */
		else {
			// TODO: add undo/redo support.
			treeViewer.refresh();
			mainEditor.markDirty();
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Rename the currently-selected element in the content outline view.
	 */
	public void rename() {
		
		/*
		 * Initiate the editing of the selected cell. Note that most of the work for this
		 * operation is performed by the OutlineContentCellModifier class. All we need to
		 * do here is start the edit in motion.
		 */
		if (selectedNode != null) {
			treeViewer.editElement(selectedNode, 0);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * This method is called whenever the user clicks on a node in the tree viewer. We
	 * make note of the currently-selected node so that other operations (add, delete, etc)
	 * know what they're operating on. Based on the selection, we also determine whether
	 * the "remove" or "rename" operations are currently permitted.
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		
		IStructuredSelection selection = (IStructuredSelection)event.getSelection();
		Object node = selection.getFirstElement();
		
		if (node instanceof UIInteger) {
			selectedNode = (UIInteger)node;
			int nodeId = selectedNode.getId();
			
			/* start by assuming the remove and rename buttons will both be active */
			removeEnabled = true;			
			renameEnabled = true;
			
			/* 
			 * Based on the selection, determine whether the "remove" or "rename" button 
			 * should be disabled. We can't remove/rename the root folder, and we can't
			 * remove a folder that has children.
			 */
			if (selectedNode instanceof UIPackageFolder) {
				if (nodeId == pkgMgr.getRootFolder()) {
					removeEnabled = false;
					renameEnabled = false;
				} else if (pkgMgr.getFolderChildren(nodeId).length != 0) {
					removeEnabled = false;
				}
			}
			
			/* else, for the UIPackage, the <import> package can't be touched. */
			else {
				if (nodeId == pkgMgr.getImportPackage()) {
					removeEnabled = false;
					renameEnabled = false;
				}
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return true if the "remove" command should be enabled, based on the current tree
	 * selection.
	 */
	public boolean getRemoveEnabled() {
		return removeEnabled;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return true if the "rename" command should be enabled, based on the current tree
	 * selection.
	 */
	public boolean getRenameEnabled() {
		return renameEnabled;
	}
	
	/*=====================================================================================*
	 * PROTECTED METHODS
	 *=====================================================================================*/

	/**
	 * Inform our parent class to create a single-selection tree.
	 */
	protected int getTreeStyle() {
		return super.getTreeStyle() | SWT.SINGLE;
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Given that the currently-selected node is used as an indication of where new packages
	 * (or folders) should be inserted, compute the parent of the node we're about to add.
	 * If the current selection is a folder, use that. If it's a package, use the package's
	 * parent. If there's no selection, use the root node.
	 * 
	 * @return The ID of the parent folder, into which a new package/folder will be added.
	 */
	private int getParentForNewNode() {
		
		/* no selection => return top root */
		if (selectedNode == null) {
			return pkgMgr.getRootFolder();
		}
		
		/* current selection is a folder => return current selection */
		int selectedId = selectedNode.getId();
		if (pkgMgr.isFolder(selectedId)) {
			return selectedId;
		}
		
		/* else, return parent of current selection (or root if there's an error) */
		int parentId = pkgMgr.getParent(selectedId);
		if (parentId == ErrorCode.NOT_FOUND) {
			return pkgMgr.getRootFolder();
		}
		return parentId;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Compute a unique name for a newly added package or folder. The default name will
	 * be "Untitled", but if that name already exists, return "Untitled-N" where N is the
	 * lowest integer (starting at 1) that isn't in use.
	 * 
	 * @return A unique name for a new package or folder.
	 */
	private String getNameForNewNode() {
		String chosenName;
		int attemptNum = 0;
		
		/* 
		 * Repeat until we find an available name. The assumption is that we'll find an
		 * available name before we run out of integers.
		 */
		while (true) {
			chosenName = "Untitled";
			if (attemptNum != 0) {
				chosenName += "-" + attemptNum;
			}
			
			if (pkgMgr.getId(chosenName) == ErrorCode.NOT_FOUND) {
				return chosenName;
			}
			
			attemptNum++;
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
}
