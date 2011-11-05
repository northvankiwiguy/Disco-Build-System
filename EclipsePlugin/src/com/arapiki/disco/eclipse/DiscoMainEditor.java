package com.arapiki.disco.eclipse;

import java.io.FileNotFoundException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;

import com.arapiki.disco.eclipse.files.DiscoFilesEditor;
import com.arapiki.disco.eclipse.tasks.DiscoTasksEditor;
import com.arapiki.disco.model.BuildStore;

public class DiscoMainEditor extends MultiPageEditorPart {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/
	
	/* package */ DiscoFilesEditor filesEditor = null;
	/* package */ DiscoTasksEditor tasksEditor = null;
	
	/** the BuildStore we've opened for editing */
	private BuildStore buildStore = null;
	
	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/
	
	public DiscoMainEditor() {
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

		/* either open the existing BuildStore, or create a new empty BuildStore */
		// TODO: wrap this to catch exceptions
		// TODO: handle the case where multiple editors have the same BuildStore open.
		try {
			buildStore = new BuildStore(path.toOSString());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.MultiPageEditorPart#createPages()
	 */
	@Override
	protected void createPages() {
		try {
			
			IEditorInput editorInput = getEditorInput();

			/* create the file editor tab */
			filesEditor = new DiscoFilesEditor(buildStore);
			int index = addPage(filesEditor, editorInput);
			setPageText(index, filesEditor.getTitle());
			
			/* create the task editor tab */
			tasksEditor = new DiscoTasksEditor(buildStore);
			index = addPage(tasksEditor, editorInput);
			setPageText(index, tasksEditor.getTitle());
			
			/* update the editor title with the name of the input file */
			setPartName(editorInput.getName());
			setTitleToolTip(editorInput.getToolTipText());
			
		
		} catch (PartInitException e) {
			// TODO: how should errors be handled?
            ErrorDialog.openError(getSite().getShell(),
            		"Error creating nested editor", null, e.getStatus());
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

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/*-------------------------------------------------------------------------------------*/
}
