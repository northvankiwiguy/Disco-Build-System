package com.buildml.eclipse.actions.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.SubEditor;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.BmlAbstractOperation;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.types.ActionSet;
import com.buildml.refactor.CanNotRefactorException;
import com.buildml.refactor.IImportRefactorer;

/**
 * An Eclipse UI Handler for managing the "Make Atomic" UI command. 
 * This handler tries to make an action atomic (i.e. merges it with it's children)
 * and gives the user feedback on anything that may have gone wrong in the process.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class HandlerMakeAtomic extends AbstractHandler {

	/*=====================================================================================*
	 * NESTED CLASSES
	 *=====================================================================================*/

	/**
	 * An undo/redo operation for supporting the "make atomic" operation.
	 */
	private class MakeAtomicOperation extends BmlAbstractOperation {
		
		/** how many "make atomics" does this single operation encompass? */
		private int changesPerformed;
		
		/*---------------------------------------------------------------------------*/

		/**
		 * Create a new MakeAtomicOperation instance. This becomes an operation
		 * on the undo/redo stack.
		 * 
		 * @param changesPerformed The number of "make atomic" operations performed.
		 */
		public MakeAtomicOperation(int changesPerformed) {
			super("Make Atomic");
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
		SubEditor subEditor = mainEditor.getActiveSubEditor();
		IBuildStore buildStore = mainEditor.getBuildStore();
		IActionMgr actionMgr = buildStore.getActionMgr();
		IImportRefactorer refactorer = mainEditor.getImportRefactorer();

		/* build an ActionSet of all the selected actions */
		TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);
		ActionSet selectedActions = EclipsePartUtils.getActionSetFromSelection(buildStore, selection);

		/* Note how many changes (make atomic operations) actually happened */
		int changesPerformed = 0;	
		
		/*
		 * For each action that was selected, treat it as an individual "make atomic"
		 * operation. 
		 */
		for (int actionId : selectedActions) {
			String actionCmd = actionMgr.getCommand(actionId);
			Integer children[] = actionMgr.getChildren(actionId);
			
			if (children.length != 0) {
				try {
					refactorer.makeActionAtomic(actionId);
					changesPerformed++;

				} catch (CanNotRefactorException e) {

					AlertDialog.displayErrorDialog("Operation failed", 
							"The \"make atomic\" operation for action:\n\n" + actionCmd +
							"\nfailed for the following reason: " + e.getCauseCode());
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
			MakeAtomicOperation operation = new MakeAtomicOperation(changesPerformed);
			operation.recordAndInvoke();
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
