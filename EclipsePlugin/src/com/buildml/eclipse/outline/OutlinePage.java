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
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.operations.RedoActionHandler;
import org.eclipse.ui.operations.UndoActionHandler;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.bobj.UIInteger;
import com.buildml.eclipse.bobj.UIPackage;
import com.buildml.eclipse.bobj.UIPackageFolder;
import com.buildml.eclipse.outline.dialogs.ChangeRootsDialog;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.UndoOpAdapter;
import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageMgrListener;
import com.buildml.model.IPackageRootMgr;
import com.buildml.model.undo.PackageUndoOp;
import com.buildml.utils.errors.ErrorCode;

/**
 * An Eclipse view, providing content for the "Outline View" associated with the BuildML
 * editor.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class OutlinePage extends ContentOutlinePage implements IPackageMgrListener {

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

	/** The IPackageRootMgr that we'll be displaying information from */
	private IPackageRootMgr pkgRootMgr;

	/** The tree element that's currently selected */
	private UIInteger selectedNode = null;
	
	/** Based on the current tree selection, can the selected node be removed? */
	private boolean removeEnabled = false;
		
	/** Based on the current tree selection, can the selected node be renamed? */
	private boolean renameEnabled = false;

	/** Based on the current tree selection, does the selected package have roots? */
	private boolean changeRootsEnabled = false;	

	/** Based on the current tree selection, can the package be opened? */
	private boolean openEnabled = false;	
	
	/** The undo handler from our main BuildML editor */
	private UndoActionHandler undoAction;

	/** The redo handler from our main BuildML editor */
	private RedoActionHandler redoAction;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new OutlinePage object. There should be exactly one of these objects for
	 * each BuildML MainEditor object.
	 * 
	 * @param mainEditor The associate MainEditor object.
	 * @param redoAction The MainEditor's redo action (for redoing operations).
	 * @param undoAction The MainEditor's undo action (for undoing operations).
	 * 
	 */
	public OutlinePage(MainEditor mainEditor, UndoActionHandler undoAction, 
				       RedoActionHandler redoAction) {
		super();
		
		/* 
		 * Save these handlers for later. We'll apply them to our action bar in
		 * the createControl method.
		 */
		this.undoAction = undoAction;
		this.redoAction = redoAction;

		/* our outline view will display information from this IPackageMgr object. */
		this.mainEditor = mainEditor;
		buildStore = mainEditor.getBuildStore();
		pkgMgr = buildStore.getPackageMgr();
		pkgRootMgr = buildStore.getPackageRootMgr();
		
		/* add ourselves as a listener for package changes */
		pkgMgr.addListener(this);
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
		treeViewer.setContentProvider(new OutlineContentProvider(pkgMgr, true));
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
		 * of that folder. If they double-click on a package name, open that package
		 * as a new Diagram in the main editor.
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
				
				/* else, open the package diagram editor */
				else {
					if (node instanceof UIPackage) {
						int selectedPkgId = ((UIPackage)node).getId();
						if (selectedPkgId != pkgMgr.getImportPackage()) {
							mainEditor.openPackageDiagram(selectedPkgId);
						}
					}
				}
			}
		});
		
		/*
		 * Configure the ability to edit cells in the package/folder tree. The 
		 * OutlineContentCellModifier class does most of the hard work.
		 */
		treeViewer.setColumnProperties(new String[] { "NAME" });
		treeViewer.setCellModifier(new OutlineContentCellModifier(mainEditor));
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
		
		/*
		 * Add the DragSource and DropTarget so that we can copy/move packages around.
		 */
		new OutlineDragSource(treeViewer, this);
		new OutlineDropTarget(treeViewer, this);
		
		/*
		 * Add the undo/redo actions from the main editor to our action bar. This allows
		 * the user to use Ctrl-Z etc. to undo/redo while focused on our window.
		 */
		getSite().getActionBars().setGlobalActionHandler(ActionFactory.UNDO.getId(), undoAction);
		getSite().getActionBars().setGlobalActionHandler(ActionFactory.REDO.getId(), redoAction);
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
		PackageUndoOp op = new PackageUndoOp(buildStore, id);
		if (createFolder) {
			newNode = new UIPackageFolder(id);
			op.recordNewFolder(newName, parentId);
		} else {
			newNode = new UIPackage(id);
			op.recordNewPackage(newName, parentId);
		}
		treeViewer.editElement(newNode, 0);
		
		/* record the undo/redo operation */
		new UndoOpAdapter(createFolder ? "Create Package Folder" : "Create Package", op).record();
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
		
		/* record the item's parent, in case we need to undo later */
		int parentId = pkgMgr.getParent(id);
		
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
			PackageUndoOp op = new PackageUndoOp(buildStore, id);
			if (isFolder) {
				op.recordRemoveFolder(name, parentId);
			} else {
				op.recordRemovePackage(name, parentId);
			}
			new UndoOpAdapter(isFolder ? "Remove Folder" : "Remove Package", op).record();
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
	 * Change the source/generated package roots for the selected package.
	 */
	public void changeRoots() {
		int pkgId = selectedNode.getId();
		boolean success = true;
		int srcRootPathId, genRootPathId;
		
		/* record the old root value, in case we need to undo */
		int oldSrcRootPathId = pkgRootMgr.getPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT);
		int oldGenRootPathId = pkgRootMgr.getPackageRoot(pkgId, IPackageRootMgr.GENERATED_ROOT);
		
		/* 
		 * Show the dialog, repeating if one or more bad paths were provided. Note that we
		 * assume the dialog only gives us 'existing' paths, but we still need to check that the
		 * new roots are in range.
		 */
		do {
			ChangeRootsDialog dialog = new ChangeRootsDialog(buildStore, pkgId);
			if (dialog.open() == ChangeRootsDialog.OK) {
				srcRootPathId = dialog.getSourceRootPathId();
				genRootPathId = dialog.getGeneratedRootPathId();

				String errMsg = "The root could not be moved to that location. It must not be above " +
						"the @workspace root, and must encompass all the package's existing files.";

				int srcRc = pkgRootMgr.setPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT, srcRootPathId);
				if (srcRc == ErrorCode.OUT_OF_RANGE) {
					AlertDialog.displayErrorDialog("Failed to Change Source Root", errMsg);
					success = false;
				}

				int genRc = pkgRootMgr.setPackageRoot(pkgId, IPackageRootMgr.GENERATED_ROOT, genRootPathId);
				if (genRc == ErrorCode.OUT_OF_RANGE) {
					AlertDialog.displayErrorDialog("Failed to Change Generated Root", errMsg);
					success = false;
				}
			} else {
				/* operation cancelled */
				return;
			}
		} while (!success);
		
		/* if anything changed, create a history item so we can undo/redo */
		if ((oldSrcRootPathId != srcRootPathId) || (oldGenRootPathId != genRootPathId)) {
			PackageUndoOp op = new PackageUndoOp(buildStore, pkgId);
			op.recordRootChange(oldSrcRootPathId, oldGenRootPathId, srcRootPathId, genRootPathId);
			new UndoOpAdapter("Change Package Root", op).invoke();
			mainEditor.markDirty();
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Invoke the "open package" command on the currently selected node, opening the package's
	 * diagram in the main editor.
	 */
	public void openPackage() {
		if (selectedNode instanceof UIPackage) {
			mainEditor.openPackageDiagram(selectedNode.getId());
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
			
			/* start by assuming the all buttons will be active */
			removeEnabled = renameEnabled = changeRootsEnabled = openEnabled = true;
			
			/* 
			 * Based on the selection, determine whether the buttons 
			 * should be disabled. We can't remove/rename the root folder, and we can't
			 * remove a folder that has children.
			 */
			if (selectedNode instanceof UIPackageFolder) {
				if (nodeId == pkgMgr.getRootFolder()) {
					removeEnabled = renameEnabled = false;
				} else if (pkgMgr.getFolderChildren(nodeId).length != 0) {
					removeEnabled = false;
				}
				changeRootsEnabled = openEnabled = false;
			}
			
			/* else, for the UIPackage, the <import> package can't be touched. */
			else {
				if (nodeId == pkgMgr.getImportPackage()) {
					removeEnabled = renameEnabled = changeRootsEnabled = openEnabled = false;
				}
				else if (nodeId == pkgMgr.getMainPackage()) {
					removeEnabled = renameEnabled = false;
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
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return true if the "change roots" command should be enabled, based on the current tree
	 * selection.
	 */
	public boolean getChangeRootsEnabled() {
		return changeRootsEnabled;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return true if the "open" menu command should be enabled, based on the current tree
	 * selection.
	 */
	public boolean getOpenEnabled() {
		return openEnabled;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The main BuildML editor associated with this outline view.
	 */
	public MainEditor getMainEditor() {
		return mainEditor;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Refresh this page's view, due to an external change in the underlying model.
	 */
	public void refresh() {
		treeViewer.refresh();
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
	
	/*-------------------------------------------------------------------------------------*/

	/*
	 * When the underlying IPackageMgr is changed in some way, we must refresh our outline
	 * view so it reflects the latest changes.
	 */
	@Override
	public void packageChangeNotification(int pkgId, int how) {
		refresh();
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
