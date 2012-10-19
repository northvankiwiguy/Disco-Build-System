package com.buildml.eclipse.files.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.window.Window;
import com.buildml.eclipse.Activator;
import com.buildml.eclipse.EditorOptions;
import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.SubEditor;
import com.buildml.eclipse.files.UIFileRecordDir;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.RootNameDialog;
import com.buildml.model.IBuildStore;
import com.buildml.model.impl.FileNameSpaces;
import com.buildml.utils.errors.ErrorCode;

/**
 * An Eclipse UI Handler for managing the "Assign Root Name" UI command. This handler
 * prompts the user to select a suitable path root name, then generates an undo/redo
 * object for performing the operation.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class HandlerAssignRootName extends AbstractHandler {

	/*=====================================================================================*
	 * NESTED CLASSES
	 *=====================================================================================*/

	/**
	 * An undo/redo operation for supporting the "assign path root" operation. Path roots
	 * may be added to a path in the BuildStore, to make browsing the path structure easier.
	 * 
	 * @author Peter Smith <psmith@arapiki.com>
	 */
	private class AssignRootNameOperation extends AbstractOperation {
		
		/** Name of the root to be added */
		private String rootName;
		
		/** Path ID of the path to attach the root to */
		private int pathId;
		
		/** Error status, in case the operation fails */
		private IStatus errorStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 
											"Unable to add or remove path root.");

		/** The PathMgr that contains the path and the root */
		private FileNameSpaces pathMgr;
		
		/*---------------------------------------------------------------------------*/

		/**
		 * Create a new AssignRootNameOperation instance. This becomes an operation
		 * on the undo/redo stack.
		 * @param rootName The name of the root to add.
		 * @param pathId The path to attach the root to.
		 */
		public AssignRootNameOperation(String rootName, int pathId) {
			super("Assign Root: \"" + rootName + "\"");
			this.rootName = rootName;
			this.pathId = pathId;
		}

		/*---------------------------------------------------------------------------*/

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.core.commands.operations.IUndoableOperation#redo(org.eclipse.core.runtime.IProgressMonitor,
		 *      org.eclipse.core.runtime.IAdaptable)
		 */
		@Override
		public IStatus redo(IProgressMonitor monitor, IAdaptable info)
				throws ExecutionException {
			
			/* identify the PathMgr to be used */
			IBuildStore buildStore = EclipsePartUtils.getActiveBuildStore();
			if (buildStore == null) {
				return errorStatus;
			}
			pathMgr = buildStore.getFileNameSpaces();
			
			/* try to add the root to the selected path. On error, give a message */
			int errCode = pathMgr.addNewRoot(rootName, pathId);
			if (errCode != 0) {
				String errMsg = "An unknown error occurred.";
				switch(errCode) {
				case ErrorCode.ONLY_ONE_ALLOWED:
					errMsg = "The selected path already has a root associated with it.";
					break;
				case ErrorCode.ALREADY_USED:
					errMsg = "The path root named: \"" + rootName + "\" is already in use.";
					break;
				case ErrorCode.INVALID_NAME:
					errMsg = "The name: \"" + rootName + "\" contains invalid characters.";
					break;
				case ErrorCode.NOT_A_DIRECTORY:
					errMsg = "The selected path is not a directory.";
					break;
				}
				AlertDialog.displayErrorDialog("Error Adding Path Root", errMsg);
				return errorStatus;
			}
			
			/*
			 * All is now good, so go ahead and enable the "show roots" option (if not
			 * already enabled), and refresh the editor's content to show the new root.
			 */
			SubEditor subEditor = EclipsePartUtils.getActiveSubEditor();
			subEditor.setOptions(EditorOptions.OPT_SHOW_ROOTS);
			subEditor.refreshView(true);
			
			/* mark the editor as dirty */
			EclipsePartUtils.markEditorDirty();
			return Status.OK_STATUS;
		}

		/*---------------------------------------------------------------------------*/

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.core.commands.operations.IUndoableOperation#undo(org.eclipse.core.runtime.IProgressMonitor,
		 *      org.eclipse.core.runtime.IAdaptable)
		 */
		@Override
		public IStatus undo(IProgressMonitor monitor, IAdaptable info)
				throws ExecutionException {

			/* reverse the operation, using saved state */
			int errCode = pathMgr.deleteRoot(rootName);
			if (errCode != 0) {
				/* this is an "undo", so fail silently */
				return errorStatus;
			}
			
			/* refresh the relevant editors */
			EclipsePartUtils.getActiveSubEditor().refreshView(true);
			return Status.OK_STATUS;
		}
		
		/*---------------------------------------------------------------------------*/

		/* (non-Javadoc)
		 * @see org.eclipse.core.commands.operations.IUndoableOperation#execute(org.eclipse.core.runtime.IProgressMonitor,
		 *      org.eclipse.core.runtime.IAdaptable)
		 */
		public IStatus execute(IProgressMonitor monitor, IAdaptable info)
				throws ExecutionException {
			return redo(monitor, info);
		}
		
		/*---------------------------------------------------------------------------*/
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		/* 
		 * Determine the Path ID of the path we're adding the root to. There must be exactly
		 * one folder selected.
		 */
		UIFileRecordDir pathNode = EclipsePartUtils.getSingleSelectedPathDir(event);
		if (pathNode == null) {
			return null;
		}
		int pathId = pathNode.getId();

		/* open a dialog box to obtain the name of the root */
		RootNameDialog dialog = new RootNameDialog();
		if (dialog.open() == Window.CANCEL) {
			return null;
		}
		String rootName = dialog.getRootName();
		
		/* create an undo/redo operation */
		AssignRootNameOperation operation = new AssignRootNameOperation(rootName, pathId);
		MainEditor editor = EclipsePartUtils.getActiveMainEditor();		
		// TODO: This addContext() is disabled for now, so this operation won't be
		// added to the editor's undo/redo queue.
		// operation.addContext(editor.getUndoContext());		
		
		/* make it so... */
		IOperationHistory history = OperationHistoryFactory.getOperationHistory();
		history.execute(operation, null, null);		
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
