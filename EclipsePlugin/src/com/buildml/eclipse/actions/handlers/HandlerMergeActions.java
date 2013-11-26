package com.buildml.eclipse.actions.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.buildml.eclipse.ISubEditor;
import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.BmlAbstractOperation;
import com.buildml.eclipse.utils.ConversionUtils;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.types.ActionSet;
import com.buildml.refactor.CanNotRefactorException;
import com.buildml.refactor.IImportRefactorer;

/**
 * An Eclipse UI Handler for managing the "Merge Actions" UI command. 
 * This handler merges multiple actions into a single action, providing
 * the user feedback on anything that may have gone wrong in the process.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class HandlerMergeActions extends AbstractHandler {

	/*=====================================================================================*
	 * NESTED CLASSES
	 *=====================================================================================*/

	/**
	 * An undo/redo operation for supporting the "merge action" operation.
	 */
	private class MergeActionOperation extends BmlAbstractOperation {
				
		/*---------------------------------------------------------------------------*/

		/**
		 * Create a new MergeActionOperation instance. This becomes an operation
		 * on the undo/redo stack.
		 */
		public MergeActionOperation() {
			super("Merge Actions");
		}

		/*---------------------------------------------------------------------------*/

		/* 
		 * Deliberately do nothing when the operation is first added to history.
		 * We already did the operation (via the refactorer).
		 */
		@Override
		public IStatus execute() throws ExecutionException {
			/* nothing */
			return Status.OK_STATUS;
		}
		
		/*---------------------------------------------------------------------------*/

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.core.commands.operations.IUndoableOperation#redo(org.eclipse.core.runtime.IProgressMonitor,
		 *      org.eclipse.core.runtime.IAdaptable)
		 */
		@Override
		public IStatus redo() throws ExecutionException {

			/* ask the refactorer to perform the redo */
			IImportRefactorer refactorer = mainEditor.getImportRefactorer();
			try {
				refactorer.redoRefactoring();
			} catch (CanNotRefactorException e) {
				return Status.CANCEL_STATUS;
			}

			/* if the sub-editor is still open, refresh the content */
			if (!subEditor.isDisposed()) {
				subEditor.refreshView(true);
			}

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
		public IStatus undo() throws ExecutionException {			

			/* ask the refactorer to perform the undo */
			IImportRefactorer refactorer = mainEditor.getImportRefactorer();
			try {
				refactorer.undoRefactoring();
			} catch (CanNotRefactorException e) {
				return Status.CANCEL_STATUS;
			}

			/* if the sub-editor is still open, refresh the content */
			if (!subEditor.isDisposed()) {
				subEditor.refreshView(true);
			}
			EclipsePartUtils.markEditorDirty();
			return Status.OK_STATUS;
		}		
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		MainEditor mainEditor = EclipsePartUtils.getActiveMainEditor();
		ISubEditor subEditor = mainEditor.getActiveSubEditor();
		IBuildStore buildStore = mainEditor.getBuildStore();
		IActionMgr actionMgr = buildStore.getActionMgr();
		IImportRefactorer refactorer = mainEditor.getImportRefactorer();

		/* build an ActionSet of all the selected actions */
		TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);
		ActionSet selectedActions = EclipsePartUtils.getActionSetFromSelection(buildStore, selection);
		
		/* attempt to perform the "merge action" operation */
		try {
			refactorer.mergeActions(selectedActions);

			// TODO: limit this scope of this refresh
			subEditor.refreshView(true);
			mainEditor.markDirty();
		
			/* create an undo/redo operation, but don't execute it. */
			MergeActionOperation operation = new MergeActionOperation();
			operation.recordAndInvoke();
			
		} catch (CanNotRefactorException e) {

			Integer actionInError[] = e.getCauseIDs();
			switch (e.getCauseCode()) {
			
			case ACTION_NOT_ATOMIC:
				String actionString = ConversionUtils.getActionsAsText(actionMgr, actionInError);
				AlertDialog.displayErrorDialog("Merge failed", 
						"The \"merge actions\" operation failed because the following " +
						((actionInError.length == 1) ? "action is not atomic:\n\n" :
													  "actions are not atomic:\n\n") + actionString);
				break;
				
			default:
				AlertDialog.displayErrorDialog("Merge failed", 
					"The \"merge actions\" operation failed for the following reason: " + 
							e.getCauseCode());
			}
		}
		
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
