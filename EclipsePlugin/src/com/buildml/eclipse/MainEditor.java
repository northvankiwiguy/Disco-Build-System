package com.buildml.eclipse;

import java.io.FileNotFoundException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
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
	public int newPage(EditorPart editor) {
		IEditorInput editorInput = getEditorInput();
		int index = -1;
		
		try {
			index = addPage(editor, editorInput);
			setPageText(index, editor.getTitle());
			
		} catch (PartInitException e) {
			ErrorDialog.openError(getSite().getShell(),
        		"Error creating nested editor", null, e.getStatus());
		}
		
		return index;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether the sub-editor at the specified tab can be removed, or whether
	 * it's permanently part of the editor.
	 * @param tabIndex The tab index of the sub-editor.
	 * @return true if the sub-editor can be removed, else false. 
	 */
	public boolean isPageRemovable(int tabIndex) {
		
		/* The initial "Files" and "Tasks" tabs are fixed. */
		return (tabIndex >= 2);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Remove the specified sub-editor, if removal is permitted.
	 * @param tabIndex The tab index of the sub-editor
	 */
	@Override
	public void removePage(int tabIndex) {
		
		if (isPageRemovable(tabIndex)) {
			super.removePage(tabIndex);
		}
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
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.MultiPageEditorPart#createPages()
	 */
	@Override
	protected void createPages() {
		IEditorInput editorInput = getEditorInput();


		/* create the file editor tab */
		newPage(new FilesEditor(buildStore, " Files "));

		/* create the task editor tab */
		newPage(new ActionsEditor(buildStore, " Actions "));
			
			/* update the editor title with the name of the input file */
		setPartName(editorInput.getName());
		setTitleToolTip(editorInput.getToolTipText());
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

	/*-------------------------------------------------------------------------------------*/
}
