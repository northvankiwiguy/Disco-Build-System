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
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.types.ActionSet;
import com.buildml.refactor.CanNotRefactorException;
import com.buildml.refactor.IImportRefactorer;

/**
 * An Eclipse UI Handler for managing the "Delete Action (Promote Children)" UI command. 
 * This handler deletes the specified action and moves its child actions up one level
 * in the tree. The user will be given feedback on anything that may have gone wrong in
 * the process.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class HandlerDeleteAction extends AbstractHandler {

	/*=====================================================================================*
	 * NESTED CLASSES
	 *=====================================================================================*/

	/**
	 * An undo/redo operation for supporting the "delete action" operation.
	 */
	private class DeleteActionOperation extends BmlAbstractOperation {
		
		/** how many "delete actions" does this single operation encompass? */
		private int changesPerformed;
		
		/*---------------------------------------------------------------------------*/

		/**
		 * Create a new DeleteActionOperation instance. This becomes an operation
		 * on the undo/redo stack.
		 * 
		 * @param changesPerformed The number of "delete action" operations performed.
		 */
		public DeleteActionOperation(int changesPerformed) {
			super("Delete Action");
			this.changesPerformed = changesPerformed;
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
				for (int i = 0; i != changesPerformed; i++) {
					refactorer.redoRefactoring();
				}
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
				for (int i = 0; i != changesPerformed; i++) {
					refactorer.undoRefactoring();
				}
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

		/* Note how many changes (delete action operations) actually happened */
		int changesPerformed = 0;	
		
		/*
		 * For each action that was selected, treat it as an individual "delete action"
		 * operation. 
		 */
		for (int actionId : selectedActions) {
			String actionCmd = actionMgr.getCommand(actionId);
			
			try {
				refactorer.deleteAction(actionId);
				changesPerformed++;

			} catch (CanNotRefactorException e) {

				switch (e.getCauseCode()) {
				
				case ACTION_IN_USE:
					AlertDialog.displayErrorDialog("Unable to delete action", 
							"The following action is still in use, and can't be deleted:\n\n" + 
									actionCmd);
					break;
					
				default:
					AlertDialog.displayErrorDialog("Operation failed", 
						"The \"delete action\" operation for action:\n\n" + actionCmd +
						"\n\nfailed for the following reason: " + e.getCauseCode());
					break;
				}
			}
		}
		
		/*
		 * If a deletion actually took place - refresh the view and add the operation
		 * to the undo/redo history.
		 */
		if (changesPerformed > 0) {
			// TODO: limit this scope of this refresh
			subEditor.refreshView(true);
			mainEditor.markDirty();
		
			/* create an undo/redo operation, but don't execute it. */
			DeleteActionOperation operation = new DeleteActionOperation(changesPerformed);
			operation.recordAndInvoke();
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
