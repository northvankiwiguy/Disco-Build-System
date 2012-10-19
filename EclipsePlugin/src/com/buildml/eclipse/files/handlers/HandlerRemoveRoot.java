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
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.buildml.eclipse.Activator;
import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.files.UIFileRecordDir;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.utils.errors.ErrorCode;

/**
 * An Eclipse UI Handler for managing the "Remove Root Name" UI command. This handler
 * generates an undo/redo object for performing the operation.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class HandlerRemoveRoot extends AbstractHandler {

	/*---------------------------------------------------------------------------*/

	/**
	 * An undo/redo operation for supporting the "remove path root" operation.
	 * 
	 * @author Peter Smith <psmith@arapiki.com>
	 */
	private class RemoveRootOperation extends AbstractOperation {
		
		/** Name of the root to be removed */
		String rootName;
		
		/** Path ID of the path from which to remove the root */
		int pathId;
		
		/** Error status, in case the operation fails */
		private IStatus errorStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 
											"Unable to add or remove path root.");
		
		/*---------------------------------------------------------------------------*/

		/**
		 * Create a new RemoveRootNameOperation instance. This becomes an operation
		 * on the undo/redo stack.
		 * 
		 * @param pathId The path to remove the root from.
		 */
		public RemoveRootOperation(int pathId) {
			super("Remove Root");
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

			/* identify the PathMgr object containing the path */
			IBuildStore buildStore = EclipsePartUtils.getActiveBuildStore();
			if (buildStore == null) {
				return errorStatus;
			}
			IFileMgr fileMgr = buildStore.getFileMgr();
			
			/* fetch the root's name (error if it doesn't exist) */
			rootName = fileMgr.getRootAtPath(pathId);
			if (rootName == null) {
				return errorStatus;
			}
			
			/* attempt the removal, possibly displaying an error dialog */
			int errCode = fileMgr.deleteRoot(rootName);
			if (errCode != 0) {
				String errMsg = "An unknown error occurred.";
				switch(errCode) {
				case ErrorCode.NOT_FOUND:
					errMsg = "The path root named: \"" + rootName + "\" is not defined";
					break;
				case ErrorCode.CANT_REMOVE:
					errMsg = "The path root named: \"" + rootName + "\" can not be removed";
				}
				AlertDialog.displayErrorDialog("Error Removing Path Root", errMsg);
				return errorStatus;
			}
			
			/* All is now good, refresh the editor so that the root disappears */
			EclipsePartUtils.getActiveSubEditor().refreshView(true);
			
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

			IBuildStore buildStore = EclipsePartUtils.getActiveBuildStore();
			if (buildStore == null) {
				return errorStatus;
			}
			IFileMgr fileMgr = buildStore.getFileMgr();
			int errCode = fileMgr.addNewRoot(rootName, pathId);
			if (errCode != 0) {
				/* this is an "undo", so it shouldn't fail (if it does, fail silently) */
				return errorStatus;
			}
			EclipsePartUtils.getActiveSubEditor().refreshView(true);
			return Status.OK_STATUS;
		}

		/*---------------------------------------------------------------------------*/

		/* (non-Javadoc)
		 * @see org.eclipse.core.commands.operations.IUndoableOperation#execute(org.eclipse.core.runtime.IProgressMonitor,
		 *      org.eclipse.core.runtime.IAdaptable)
		 */
		@Override
		public IStatus execute(IProgressMonitor monitor, IAdaptable info)
				throws ExecutionException {
			return redo(monitor, info);
		}
		
		/*---------------------------------------------------------------------------*/
	};
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		/* determine the Path ID of the path we're adding the root to */
		TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);
		if ((selection == null) || (selection.size() != 1)) {
			return null;
		}
		Object nodeObject = selection.getFirstElement();
		if (!(nodeObject instanceof UIFileRecordDir)) {
			return null;
		}
		int pathId = ((UIFileRecordDir)nodeObject).getId();
		
		/* create an undo/redo operation */
		RemoveRootOperation operation = new RemoveRootOperation(pathId);
		MainEditor editor = EclipsePartUtils.getActiveMainEditor();		
		// TODO: This addContext() is disabled for now, so this operation won't be
		// added to the editor's undo/redo queue.
		// operation.addContext(editor.getUndoContext());
		
		/* make it so... */
		IOperationHistory history = OperationHistoryFactory.getOperationHistory();
		history.execute(operation, null, null);
		return null;
	}

	/*---------------------------------------------------------------------------*/
}
