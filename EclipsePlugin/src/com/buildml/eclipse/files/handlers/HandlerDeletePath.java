package com.buildml.eclipse.files.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import com.buildml.model.IFileMgr;
import com.buildml.model.IFileMgr.PathType;
import com.buildml.model.types.FileSet;
import com.buildml.refactor.CanNotRefactorException;
import com.buildml.refactor.IImportRefactorer;

/**
 * An Eclipse UI Handler for managing the "Delete File or Directory" UI command. 
 * This handler tries to delete the file/directory and gives the user feedback
 * on anything that may have gone wrong in the process.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class HandlerDeletePath extends AbstractHandler {

	/*=====================================================================================*
	 * NESTED CLASSES
	 *=====================================================================================*/

	/**
	 * An undo/redo operation for supporting the "delete path" and "delete directory"
	 * operations.
	 */
	private class DeletePathOperation extends BmlAbstractOperation {
		
		/** how many deletions does this single operation encompass? */
		private int changesPerformed;
		
		/*---------------------------------------------------------------------------*/

		/**
		 * Create a new AssignRootNameOperation instance. This becomes an operation
		 * on the undo/redo stack.
		 * @param changesPerformed The number of deletion operations performed.
		 */
		public DeletePathOperation(int changesPerformed) {
			super("Delete File" + (changesPerformed > 1 ? "s" : ""));
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
			System.out.println("Undoing");
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
		IFileMgr fileMgr = buildStore.getFileMgr();
		IActionMgr actionMgr = buildStore.getActionMgr();
		IImportRefactorer refactorer = mainEditor.getImportRefactorer();

		/* build a FileSet of all the selected files */
		TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);
		FileSet selectedPaths = EclipsePartUtils.getFileSetFromSelection(buildStore, selection);

		/* Note how many changes (deletions) actually happened */
		int changesPerformed = 0;	
		/*
		 * For each file/directory that was selected, treat it as an individual "delete"
		 * operation. 
		 */
		for (int pathId : selectedPaths) {
			String pathName = fileMgr.getPathName(pathId);
			
			/*
			 * Given that the user may select a directory and a file within that directory,
			 * we first need to double-check that the path still exists when
			 * we come to delete it.
			 */
			PathType pathType = fileMgr.getPathType(pathId);
			if (pathType == PathType.TYPE_INVALID) {
				// TODO: test this case.
				System.out.println("Skipping " + pathName + " since it no longer exists.");
				continue;
			}
			
			boolean needsRetry;
			boolean deleteSubTree = false;
			boolean deleteActionToo = false;
			do {
				needsRetry = false;
				try {
					if (deleteSubTree) {
						refactorer.deletePathTree(pathId, deleteActionToo);
					} else {
						refactorer.deletePath(pathId, deleteActionToo);
					}
					
					/* success! The file/directory was deleted */
					changesPerformed++;

				} catch (CanNotRefactorException e) {

					Integer[] actionIds = e.getActionIds();
					Integer[] pathIds = e.getPathIds();
					boolean multipleActions = (actionIds != null) && (actionIds.length > 1);

					/*
					 * Something stopped the delete from completing. Based on the error
					 * code, we could potentially go back and try again.
					 */
					switch (e.getCauseCode()) {

					/* 
					 * The directory we're trying to delete is non-empty. Give the user the
					 * option of deleting the whole sub-directory.
					 */
					case DIRECTORY_NOT_EMPTY:
						int status = AlertDialog.displayOKCancelDialog( 
								"The directory: " + pathName + " is not empty.\n\n" +
								"Do you wish to try deleting all files in the directory?");
						if (status == IDialogConstants.OK_ID) {
							needsRetry = true;
							deleteSubTree = true;
						}
						break;

					/*
					 * The path we're trying to delete is a generated file. We'd need to
					 * also delete the generating action.
					 */
					case PATH_IS_GENERATED:
						status = AlertDialog.displayOKCancelDialog( 
								"The file " + pathName + " is generated by " +
								(multipleActions ? "several actions" : "an action") + ":\n\n" +
								ConversionUtils.getActionsAsText(actionMgr, actionIds) +
								"\nDo you also wish to try deleting " +
								(multipleActions ? "the actions?" : "the action?"));
						if (status == IDialogConstants.OK_ID) {
							deleteActionToo = true;
							needsRetry = true;
						}					
						break;
					
					/*
					 * The path is used (read) by an action. It can't be deleted at all.
					 */
					case PATH_IN_USE:
						AlertDialog.displayErrorDialog("Can't delete",
								"The file " + pathName + " can't be deleted as it's still " +
								"in use by the following " +
								(multipleActions ? "actions" : "action") + ":\n\n" +
								ConversionUtils.getActionsAsText(actionMgr, actionIds));
						break;

					/*
					 * The path is a directory, but there are actions executing within that
					 * directory.
					 */
					case DIRECTORY_CONTAINS_ACTIONS:
						AlertDialog.displayErrorDialog("Directory is busy",
								"The directory " + pathName + " can't be deleted as it's still " +
								"used as the working directory for the following " +
								(multipleActions ? "actions" : "action") + ":\n\n" +
								ConversionUtils.getActionsAsText(actionMgr, actionIds));
						break;
						
					/*
					 * Can't delete a path that's generated by a non-atomic action.
					 */
					case ACTION_NOT_ATOMIC:
						AlertDialog.displayErrorDialog("Can't delete",
								"The file " + pathName + " can't be deleted as the action " +
								"that generates the file is not atomic:\n\n" +
								ConversionUtils.getActionsAsText(actionMgr, actionIds));
						break;

					/*
					 * Can't delete an action (that generated the path), because it's in use
					 * by some other paths.
					 */
					case ACTION_IN_USE:
						AlertDialog.displayErrorDialog("Can't delete",
								"The action that generates " + pathName + " can't be deleted since " +
								"the related files are still in use:\n\n" +
								ConversionUtils.getPathsAsText(fileMgr, pathIds));
						break;
						
					/*
					 * Ignore this case - it's likely because the parent has already been deleted.
					 */
					case INVALID_PATH:
						break;
						
						/*
					 * These shouldn't happen.
					 */
					default:
						AlertDialog.displayErrorDialog("Deletion failed", 
								"Deletion of the file/directory: " + pathName + 
								" failed for the following reason: " + e.getCauseCode());					
					}
				}
			} while (needsRetry);
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
			DeletePathOperation operation = new DeletePathOperation(changesPerformed);
			operation.recordAndInvoke();
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
