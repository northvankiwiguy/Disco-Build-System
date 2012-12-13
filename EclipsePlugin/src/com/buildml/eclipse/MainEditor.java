package com.buildml.eclipse;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.commands.operations.ObjectUndoContext;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.operations.RedoActionHandler;
import org.eclipse.ui.operations.UndoActionHandler;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import com.buildml.eclipse.actions.ActionsEditor;
import com.buildml.eclipse.bobj.UIPackage;
import com.buildml.eclipse.files.FilesEditor;
import com.buildml.eclipse.outline.OutlinePage;
import com.buildml.eclipse.packages.PackageDiagramEditor;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.model.FatalBuildStoreError;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMgr;
import com.buildml.model.IPackageMgrListener;
import com.buildml.refactor.IImportRefactorer;
import com.buildml.refactor.imports.ImportRefactorer;

/**
 * The main Eclipse editor for editing/viewing BuildML files. This editor is a "multi-part"
 * editor, since most of the work is done by sub-editors.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class MainEditor extends MultiPageEditorPart 
	implements IResourceChangeListener, IPackageMgrListener {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** the BuildStore we've opened for editing */
	private IBuildStore buildStore = null;
	
	/** The BuildStore's package manager */
	private IPackageMgr pkgMgr;
	
	/** The refactorer that will manage refactoring (and its history) for this editor */
	private IImportRefactorer importRefactorer;
	
	/** the currently active tab index */
	private int currentPageIndex = -1;
	
	/** the tab that was most recently visible (before the current tab was made visible) */
	private int previousPageIndex = -1;

	/** The file that this editor has open. */
	private File fileInput;
	
	/** This editor's undo/redo context - applies to all sub editors */
	private IUndoContext undoContext;

	/** This editor's "undo" action - trigger by menu or keyboard shortcut (Ctrl-Z) */
	private UndoActionHandler undoAction;

	/** This editor's "redo" action - trigger by menu or keyboard shortcut (Ctrl-Y) */
	private RedoActionHandler redoAction;
	
	/** "dirty" state of this editor - true means that it needs saving */
	private boolean editorIsDirty = false;
	
	/** Counts the number of times the underlying BuildStore model has changed */
	private long modelChangeCounter = 0;
	
	/** This editor's associated outline page content */
	private OutlinePage outlinePage = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * Create a new top-level BuildML Editor.
	 */
	public MainEditor() {
		super();
	}

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.MultiPageEditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {

		if (! (input instanceof IURIEditorInput)) {
			throw new PartInitException("Invalid Input: Must be IURIEditorInput");
		}
		super.init(site, input);
		
		/* open the BuildStore file we're editing */
		
		IURIEditorInput editorInput = (IURIEditorInput)input;
		fileInput = new File(editorInput.getURI().getPath());

		// TODO: handle the case where multiple editors have the same BuildStore open.
		
		// TODO: put a top-level catch for FatalBuildStoreException() (and other
		// exceptions) to display a meaningful error.
		
		buildStore = EclipsePartUtils.getNewBuildStore(fileInput.getPath());
		if (buildStore == null) {
			throw new PartInitException("Can't open the BuildML database.");
		}
		pkgMgr = buildStore.getPackageMgr();
		
		/* 
		 * Register to learn about changes to resources in our workspace. We might need to
		 * know if somebody deletes or renames the file we're editing.
		 */
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
		
		/* Listen to changes in the package naming */
		pkgMgr.addListener(this);
		
		/*
		 * Create a refactor manager that will be responsible for doing refactoring
		 * (deletion, merging, etc) for this BuildStore. This also handles the undo/redo
		 * of those operations.
		 */
		importRefactorer = new ImportRefactorer(buildStore);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Add the specified Editor to the MainEditor, as a new tab. The text of the tab
	 * will be set to be the same as the title of the window.
	 * 
	 * @param editor The new editor instance to add within the tab. This would usually be
	 * 			a FilesEditor, ActionsEditor, or similar.
	 * @return The tab index of the newly added tab.
	 */
	public int newPage(ISubEditor editor) {
		IEditorInput editorInput = getEditorInput();
		int index = -1;
		
		try {
			/* set the new tab's text name */
			index = addPage(editor, editorInput);
			setPageText(index, " " + editor.getTitle() + " ");
			
			/* if it has one, set the new tab's icon */
			Image image = editor.getEditorImage();
			if (image != null) {
				setPageImage(index, image);
			}
			
		} catch (PartInitException e) {
			ErrorDialog.openError(getSite().getShell(),
        		"Error creating nested editor", null, e.getStatus());
		}
		
		return index;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Remove the specified sub-editor, if removal is permitted.
	 * @param tabIndex The tab index of the sub-editor
	 */
	@Override
	public void removePage(int tabIndex) {
		
		IEditorPart editor = getEditor(tabIndex);
		if (editor instanceof ISubEditor) {
			ISubEditor subEditor = (ISubEditor)editor;
			if (subEditor.hasFeature("removable")) {
				int pageToReturnTo = previousPageIndex;
				super.removePage(tabIndex);
				if (pageToReturnTo != -1) {
					/* have the index numbers changed due to removal? */
					if (tabIndex < pageToReturnTo) {
						pageToReturnTo--;
					}
					setActivePage(pageToReturnTo);
				}
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Switch focus to the next editor tab.
	 */
	public void nextPage() {
		int currentPage = getActivePage();
		int pageCount = getPageCount();
		if ((currentPage != -1) && (currentPage != pageCount - 1)) {
			setActivePage(currentPage + 1);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Switch focus to the previous editor tab.
	 */
	public void previousPage() {
		int currentPage = getActivePage();
		if (currentPage > 0) {
			setActivePage(currentPage - 1);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the name that's written on the current sub editor's tab.
	 * @param tabIndex The tab index of the sub-editor.
	 * @return The tab's current text.
	 */
	public String getPageName(int tabIndex) {
		String text = getPageText(tabIndex);
		return text.substring(1, text.length() - 1);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.MultiPageEditorPart#pageChange(int)
	 */
	@Override
	protected void pageChange(int newPageIndex) {		
		super.pageChange(newPageIndex);
		
		/* trigger the pageChange() method on the sub-editor, so it can update the UI */
		ISubEditor subEditor = (ISubEditor)getActiveEditor();
		if (subEditor != null) {
			subEditor.pageChange();
		
			/* remember the previous actively page, so it's easier to return to */
			int activatingPageIndex = getActivePage();
			if (currentPageIndex != activatingPageIndex) {
				previousPageIndex = currentPageIndex;
				currentPageIndex = activatingPageIndex;
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.MultiPageEditorPart#createPages()
	 */
	@Override
	protected void createPages() {
		IEditorInput editorInput = getEditorInput();

		/* create the file editor tab */
		FilesEditor editor1 = new FilesEditor(buildStore, "Files");
		editor1.setRemovable(false);
		newPage(editor1);
		
		/* create the action editor tab */
		ActionsEditor editor2 = new ActionsEditor(buildStore, "Actions");
		editor2.setRemovable(false);
		newPage(editor2);

		/* update the editor title with the name of the input file */
		setPartName(editorInput.getName());
		setTitleToolTip(editorInput.getToolTipText());
		
		/*
		 * Attach a double-click listener to the MultiPageEditorPart, so that if the
		 * user double-clicks on one of the tabs at the bottom of the editor, we
		 * can bring up a Dialog box that will allow them to change the tab name.
		 */
		if (getContainer() instanceof CTabFolder)
		{
			CTabFolder folder = (CTabFolder)getContainer();
			folder.addListener(SWT.MouseDoubleClick, new Listener() {
				@Override
				public void handleEvent(Event event) {
					renameTab();
				}
			});
		}
		
		/*
		 * Create a new undo/redo context for this editor (and all sub-editors). Next,
		 * create Eclipse UI actions that can be added to the global edit menu.
		 */
		undoContext = new ObjectUndoContext(this);
		undoAction = new UndoActionHandler(getSite(), undoContext);
		redoAction = new RedoActionHandler(getSite(), undoContext);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			/* save the database to disk */
			buildStore.save();
			
			/* 
			 * Update Eclipse's resources, so it notices that the file has changed. This
			 * is only necessary if the file is in the workspace.
			 */
			if (getEditorInput() instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput) getEditorInput()).getFile();
				try {
					file.refreshLocal(IFile.DEPTH_ONE, monitor);
				} catch (CoreException e) {
					throw new FatalBuildStoreError("Unable to update file resource after save", e);
				}
			}

			/* save was successful - notify other parts of Eclipse */
			editorIsDirty = false;
			firePropertyChange(PROP_DIRTY);

		} catch (IOException e) {
			AlertDialog.displayErrorDialog("Error Saving Database",
					"The database could not be saved to disk. Reason: " + e.getMessage());
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
		
		/*
		 * Open a "save as" dialog box that queries the user for the new file
		 * name to save as.
		 */
		Shell shell = getSite().getWorkbenchWindow().getShell();
		SaveAsDialog dialog = new SaveAsDialog(shell);
		dialog.setOriginalName("Copy of " + getPartName());
		dialog.open();

		/* if the user provided a file name (as opposed to hitting cancel) */
		final IPath newPath = dialog.getResult();   
		if (newPath != null) {
			try {
				/* save the file to the newly-selected path */
				String absPath = EclipsePartUtils.workspaceRelativeToAbsolutePath(newPath.toOSString());
				buildStore.saveAs(absPath);

				/* update Eclipse's resources to reflect the new file */
				IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(newPath);
				try {
					newFile.refreshLocal(IFile.DEPTH_ONE, null);
				} catch (CoreException e) {
					throw new FatalBuildStoreError("Unable to update file resource after save", e);
				}
				
				/*
				 * Set this editor's input to refer to the new file, since
				 * the old file is no longer "attached" to this editor. As
				 * a result, we mark the editor as "not dirty", as well
				 * as notifying other parts of Eclipse that our input has changed.
				 */
				setInput(new FileEditorInput(newFile));
				setPartName(newFile.getName());
				editorIsDirty = false;
				firePropertyChange(PROP_DIRTY);
				firePropertyChange(PROP_INPUT);
				
			} catch (IOException e) {
				AlertDialog.displayErrorDialog("Error Saving Database",
						"The database could not be saved to disk. Reason: " + e.getMessage());
			}
			
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.MultiPageEditorPart#isDirty()
	 */
	@Override
	public boolean isDirty() {
		return editorIsDirty;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * The content of the editor has changed, so mark the editor as dirty (the "save" menu
	 * option will now be available).
	 */
	public void markDirty() {
		
		/* tell Eclipse that we're changing our dirty state (it should put a * in our title). */
		if (!editorIsDirty) {
			editorIsDirty = true;
			firePropertyChange(PROP_DIRTY);
		}
		
		/* 
		 * Increment our "change counter". This is used by sub-editors to know whether they
		 * need to refresh themselves. That is, if a sub-editor makes a change to the
		 * underlying model, other sub-editors will need to refresh themselves. However,
		 * if there were no model changes since the last refresh, there's no need to refresh.
		 */
		modelChangeCounter++;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the number of times the underlying BuildStore model has changed. This is
	 * can be used by sub-editors to determine if their content needs to be refreshed
	 * on a page-change operation.
	 * 
	 * @return The number of times the underlying BuildStore has changed.
	 */
	public long getModelChangeCount() {
		return modelChangeCounter;
	}

	/*-------------------------------------------------------------------------------------*/

	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.MultiPageEditorPart#dispose()
	 */
	@Override
	public void dispose() {

		/* close the BuildStore to release resources */
		buildStore.close();
		
		/* stop listening to resource changes */
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		pkgMgr.removeListener(this);
		
		super.dispose();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The BuildStore associated with this instance of the editor
	 */
	public IBuildStore getBuildStore() {
		return buildStore;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the editor that's currently visible (on the currently selected page). This
	 * editor (for example, a FilesEditor instance) will be the target of any selection
	 * operations that occur.
	 * @return The editor that's currently visible.
	 */
	public ISubEditor getActiveSubEditor() {
		return (ISubEditor) this.getActiveEditor();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The IImportRefactorer that manages the refactoring operations on this editor.
	 */
	public IImportRefactorer getImportRefactorer() {
		return importRefactorer;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The file that this editor is editing.
	 */
	public File getFile() {
		return fileInput;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return This editor's undo/redo context, which is the central queue for all of
	 * this editor's operations that can be undone and redone.
	 */
	public IUndoContext getUndoContext()
	{
		return undoContext;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.MultiPageEditorPart#setFocus()
	 */
	@Override
	public void setFocus() {
		super.setFocus();

		/*
		 * Our editor (or a sub-editor) has just become active. Ensure that the global
		 * "redo" and "undo" menu items (and keyboard shortcuts) are registered to
		 * use our queue of operations.
		 */
		if ((undoAction != null) && (redoAction != null)) {
			IActionBars actionBars = getEditorSite().getActionBars();
			actionBars.setGlobalActionHandler(ActionFactory.UNDO.getId(), undoAction);
			actionBars.setGlobalActionHandler(ActionFactory.REDO.getId(), redoAction);
			actionBars.updateActionBars();
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Returns various objects that are associated with this BuildML editor.
	 */
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		
		/* Provides the content of the Outline view */
		if (adapter.equals(IContentOutlinePage.class)) {
			if (outlinePage == null) {
				outlinePage = new OutlinePage(this, undoAction, redoAction);
			}
			return outlinePage;
		}
		
		return super.getAdapter(adapter);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * This method is called whenever a resource in our project is modified. We only
	 * care about this if somebody has deleted or renamed the file that we're editing. 
	 * Otherwise this event is ignored.
	 */
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta changes = event.getDelta();
		try {
			changes.accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta change) {
					if (change.getResource().getType() == IResource.FILE) {
						IResource resource = change.getResource();
						
						/* 
						 * Is the changed file the file that we're editing? Also, is the
						 * change a "remove" operation (a delete or a rename).
						 */
						if (resource.getLocation().toFile().equals(fileInput) &&
								(change.getKind() == IResourceDelta.REMOVED)) {

							/* was the file renamed? */
							if (change.getFlags() == IResourceDelta.MOVED_TO) {
								final File newFile = new File(Platform.getLocation().toOSString(), 
														change.getMovedToPath().toOSString());
								
								/* 
								 * if so, record the new name, and proceed to change the name
								 * of the editor tab (in a UI thread) to be the basename of the
								 * file's path.
								 */
								fileInput = newFile;								
								Display.getDefault().asyncExec(new Runnable() {
									@Override
									public void run() {
										String newPartName = newFile.toString();
										int lastSlash = newPartName.lastIndexOf('/'); 
										if (lastSlash == -1) {
											MainEditor.this.setPartName(newPartName);
										} else {
											MainEditor.this.setPartName(newPartName.substring(lastSlash + 1));
										}
									}
								});
							}
							
							/* no, the file was completely deleted - close the editor */
							else {
								Display.getDefault().asyncExec(new Runnable() {
									@Override
									public void run() {
										IWorkbenchPage page = MainEditor.this.getEditorSite().getPage();
										page.closeEditor(MainEditor.this, false);
									}
								});
							}							
						}
					};
					return true;
				}
			});
		}
		catch (CoreException exception) {
			/* nothing */
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Open a new package diagram, or if such a diagram already exists, bring it to the front.
	 * 
	 * @param pkgId PackageMgr ID of the package to be displayed.
	 */
	public void openPackageDiagram(int pkgId) {
		
		/* first, see if a suitable editor is already open */
		int index = findPackageDiagramById(pkgId);
		if (index == -1) {
			/* no existing editor for this package, open a new one */
			PackageDiagramEditor newEditor = new PackageDiagramEditor(buildStore, pkgId);
			index = newPage(newEditor);
		}
		setActivePage(index);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * A package has been renamed or modified in some way, so make sure any open editors
	 * are updated from the model.
	 * @param pkgId The ID of the package that was updated.
	 * @param how   The way in which is was changed.
	 */
	@Override
	public void packageChangeNotification(int pkgId, int how) {
	
		/* for now, we only care about name changes */
		if (how == IPackageMgrListener.CHANGED_NAME) {
			int index = findPackageDiagramById(pkgId);
			if (index != -1) {
			
				/* update the editor tab's text */
				String pkgName = pkgMgr.getName(pkgId);
				setPageText(index, "Package: " + pkgName);
			}
		}
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Invoked whenever the user double-clicks on the "tab name" at the bottom of the
	 * editor window. This brings up a dialog box allowing the user to change the
	 * tab's name.
	 */
	private void renameTab() {
		
		/* fetch the current tab text, being careful to remove trailing and leading " ". */
		int pageIndex = getActivePage();
		String currentTabName = getPageText(pageIndex);
		currentTabName = currentTabName.substring(1, currentTabName.length() - 1);
		
		/* Open the dialog box, to allow the user to modify the name */
		EditorTabNameChangeDialog dialog = new EditorTabNameChangeDialog();
		dialog.setName(currentTabName);
		dialog.open();

		/* If the user pressed OK, set the new name */
		if (dialog.getReturnCode() == Dialog.OK) {
			String newTabName = dialog.getName();
			if (pageIndex != -1) {
				setPageText(pageIndex, " " + newTabName + " ");
			}
		}
		dialog.close();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a package ID number, return the tab index of the editor (if it's currently open).
	 * 
	 * @param pkgId The package ID for which we want to find an open PackageDiagramEditor.
	 * @return The tab index of the open editor, or -1 if no such editor can be found.
	 */
	private int findPackageDiagramById(int pkgId) {

		int numEditors = getPageCount();
		for (int i = 0; i < numEditors; i++) {
			IEditorPart subEditor = getEditor(i);
			if (subEditor instanceof PackageDiagramEditor) {
				if (((PackageDiagramEditor)subEditor).getPackageId() == pkgId) {
					return i;
				}
			}
		}
		
		/* not found */
		return -1;
	}

	/*-------------------------------------------------------------------------------------*/
}
