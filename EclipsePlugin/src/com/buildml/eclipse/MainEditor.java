package com.buildml.eclipse;

import java.io.FileNotFoundException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;

import com.buildml.eclipse.actions.ActionsEditor;
import com.buildml.eclipse.files.FilesEditor;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.model.BuildStore;
import com.buildml.model.errors.BuildStoreVersionException;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class MainEditor extends MultiPageEditorPart {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** the BuildStore we've opened for editing */
	private BuildStore buildStore = null;
	
	/** the currently active tab index */
	private int currentPageIndex = -1;
	
	/** the tab that was most recently visible (before the current tab was made visible) */
	private int previousPageIndex = -1;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	/**
	 * 
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

		if (! (input instanceof IFileEditorInput)) {
			throw new PartInitException("Invalid Input: Must be IFileEditorInput");
		}
		super.init(site, input);
		
		/* open the BuildStore file we're editing */
		
		IFileEditorInput fileInput = (IFileEditorInput)input;
		IFile file = fileInput.getFile();
		IPath path = file.getLocation();

		// TODO: handle the case where multiple editors have the same BuildStore open.
		
		// TODO: put a top-level catch for FatalBuildStoreException() (and other
		// exceptions) to display a meaningful error.

		
		try {
			/*
			 * Test that file exists before opening it, otherwise it'll be created
			 * automatically, which isn't what we want. Ideally Eclipse wouldn't ask
			 * us to open a file that doesn't exist, so this is an extra safety
			 * check.
			 */
			if (!file.exists()) {
				throw new FileNotFoundException(path + " does not exist, or is not writable.");
			}
			
			/* open the existing BuildStore database */
			buildStore = new BuildStore(path.toOSString());
			
		} catch (BuildStoreVersionException e) {
			
			AlertDialog.displayErrorDialog("BuildML database has the wrong version.", e.getMessage());
			throw new PartInitException("BuildML database has the wrong version.");
		} catch (FileNotFoundException e) {
			AlertDialog.displayErrorDialog("Can't open the BuildML database.", e.getMessage());
			throw new PartInitException("Can't open the BuildML database.");
		}
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
	public int newPage(SubEditor editor) {
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
		if (editor instanceof SubEditor) {
			SubEditor subEditor = (SubEditor)editor;
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
		SubEditor subEditor = (SubEditor)getActiveEditor();
		subEditor.pageChange();
		
		/* remember the previous actively page, so it's easier to return to */
		previousPageIndex = currentPageIndex;
		currentPageIndex = getActivePage();
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
		
		/* create the task editor tab */
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
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub
		
	}

	/*-------------------------------------------------------------------------------------*/

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
		/* not implemented */
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	@Override
	public boolean isSaveAsAllowed() {
		/* save-as is not supported for BuildStore-based data */
		return false;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.MultiPageEditorPart#dispose()
	 */
	@Override
	public void dispose() {

		/* close the BuildStore to release resources */
		buildStore.close();		
		super.dispose();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The BuildStore associated with this instance of the editor
	 */
	public BuildStore getBuildStore() {
		return buildStore;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Return the editor that's currently visible (on the currently selected page). This
	 * editor (for example, a FilesEditor instance) will be the target of any selection
	 * operations that occur.
	 * @return The editor that's currently visible.
	 */
	public SubEditor getActiveSubEditor() {
		return (SubEditor) this.getActiveEditor();
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
}
