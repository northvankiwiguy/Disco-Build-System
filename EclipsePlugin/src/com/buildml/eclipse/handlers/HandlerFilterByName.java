package com.buildml.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;

import com.buildml.eclipse.SubEditor;
import com.buildml.eclipse.actions.ActionsEditor;
import com.buildml.eclipse.files.FilesEditor;
import com.buildml.eclipse.utils.BmlAbstractOperation;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.NameFilterDialog;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IReportMgr;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.ActionSet;
import com.buildml.utils.types.IntegerTreeSet;

/**
 * Command Handler for the "filter by name" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class HandlerFilterByName extends AbstractHandler {

	/*=====================================================================================*
	 * NESTED CLASSES
	 *=====================================================================================*/

	/**
	 * An undo/redo operation for recording changes in an editor's visibility state
	 * when "filter by name" is used to modify the visible tree items.
	 */
	private class FilterOperation extends BmlAbstractOperation {

		/** The existing visibility set, recording the state before the operation takes place. */
		private IntegerTreeSet existingSet;

		/** The new visibility set to put in place */
		private IntegerTreeSet newSet;

		/*--------------------------------------------------------------------------------*/

		/**
		 * Create a new FilterOperation object.
		 * 
		 * @param existingSet The visibility set, before the operation takes place.
		 * @param newSet The visibility set to start using.
		 */
		public FilterOperation(IntegerTreeSet existingSet, IntegerTreeSet newSet) {
			
			/* set up the operation "label" that appears in the undo/redo menu */
			super("Filter Items");
			
			/* 
			 * We need to make a copy of the visibility sets, since they'll be changing
			 * and we need it to be constant.
			 */
			try {
				this.existingSet = (IntegerTreeSet) existingSet.clone();
				this.newSet = (IntegerTreeSet) newSet.clone();
			} catch (CloneNotSupportedException e) {
				/* can't happen */
			}
		}

		/*--------------------------------------------------------------------------------*/

		/**
		 * Do, or redo an operation.
		 */
		@Override
		public IStatus redo() throws ExecutionException {
			return useVisibilitySet(newSet);
		}

		/*--------------------------------------------------------------------------------*/

		/**
		 * Undo an operation.
		 */
		@Override
		public IStatus undo() throws ExecutionException {
			return useVisibilitySet(existingSet);
		}
		
		/*--------------------------------------------------------------------------------*/

		/**
		 * Set the current editor's visibility set to the specified set.
		 * @param setToUse The visibility set to use for this editor.
		 * @return The status of the action.
		 */
		private IStatus useVisibilitySet(IntegerTreeSet setToUse) {
			
			/* copy the set, since this will be used for making future changes */
			IntegerTreeSet nextSet = null;
			try {
				nextSet = (IntegerTreeSet) setToUse.clone();
			} catch (CloneNotSupportedException e) {
				/* can't happen */
			}
			
			/* set the visibility set and refresh the view */
			if (!subEditor.isDisposed()) {
				subEditor.setVisibilityFilterSet(nextSet);
				subEditor.refreshView(true);
			}
			return Status.OK_STATUS;
		}
		
		/*--------------------------------------------------------------------------------*/

	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		SubEditor subEditor = EclipsePartUtils.getActiveSubEditor();
		if (subEditor instanceof FilesEditor) {
			return executeFilesEditor(event);
		} else if (subEditor instanceof ActionsEditor) {
			return executeActionsEditor(event);
		}
		
		/* for other editors, do nothing */
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Variant of execute() that's invoked if the current sub-editor is a FilesEditor.
	 * @param event The UI command event.
	 * @return Always null.
	 * @throws ExecutionException Something bad happened.
	 */
	public Object executeFilesEditor(ExecutionEvent event) throws ExecutionException {
			
		/*
		 * Figure out which editor/sub-editor this event applies to.
		 */
		FilesEditor editor = EclipsePartUtils.getActiveFilesEditor();
		if (editor == null) {
			return null;
		}
		IBuildStore buildStore = EclipsePartUtils.getActiveBuildStore();
		IFileMgr fileMgr = buildStore.getFileMgr();
		IReportMgr reportMgr = buildStore.getReportMgr();
		
		/* 
		 * Display a dialog, which asks the user to provide a regular expression string,
		 * as well as an "add these files" or "remove these files" choice.
		 */
		NameFilterDialog dialog = new NameFilterDialog("files");
		dialog.open();
		
		/*
		 * If OK was pressed, compute the set of files that match the regular expression.
		 */
		if (dialog.getReturnCode() != IDialogConstants.OK_ID) {
			return null;
		}
		String regExpr = dialog.getRegularExpression();
		int resultCode = dialog.getAddRemoveChoice();	
		dialog.close();
		
		FileSet resultSet = reportMgr.reportFilesThatMatchName(regExpr);
		FileSet newVisibilitySet = null;
		
		/*
		 * Depending on the selected mode, either merge or filter these files from the
		 * current tab's file set. 
		 */
		FileSet currentFileSet = (FileSet) editor.getVisibilityFilterSet();
		switch (resultCode) {
		
		case NameFilterDialog.SELECT_ONLY_MATCHING_ITEMS:
			newVisibilitySet = resultSet;			
			break;
			
		case NameFilterDialog.ADD_MATCHING_ITEMS:
			resultSet.mergeSet(currentFileSet);
			newVisibilitySet = resultSet;
			break;
			
		case NameFilterDialog.REMOVE_MATCHING_ITEMS:
			try {
				newVisibilitySet = (FileSet) currentFileSet.clone();
				newVisibilitySet.extractSet(resultSet);
			} catch (CloneNotSupportedException e) {
				/* won't happen */
			}
			break;
			
		case NameFilterDialog.SELECT_ALL_ITEMS:
			newVisibilitySet = reportMgr.reportAllFiles();
			break;
			
		case NameFilterDialog.DESELECT_ALL_ITEMS:
			newVisibilitySet = new FileSet(fileMgr);
			break;
			
		default:
			/* do nothing - silently */
			break;
		}
		
		/* create a new undo/redo operation, for recording this change */
		newVisibilitySet.populateWithParents();
		FilterOperation operation = new FilterOperation(currentFileSet, newVisibilitySet);
		operation.recordAndInvoke();
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Variant of execute() that's invoked if the current sub-editor is an ActionsEditor.
	 * @param event The UI command event.
	 * @return Always null.
	 * @throws ExecutionException Something bad happened.
	 */
	public Object executeActionsEditor(ExecutionEvent event) throws ExecutionException {
			
		/*
		 * Figure out which editor/sub-editor this event applies to.
		 */
		ActionsEditor editor = EclipsePartUtils.getActiveActionsEditor();
		if (editor == null) {
			return null;
		}
		IBuildStore buildStore = EclipsePartUtils.getActiveBuildStore();
		IActionMgr actionsMgr = buildStore.getActionMgr();
		IReportMgr reportMgr = buildStore.getReportMgr();
		
		/* 
		 * Display a dialog, which asks the user to provide a regular expression string,
		 * as well as an "add these actions" or "remove these actions" choice.
		 */
		NameFilterDialog dialog = new NameFilterDialog("actions");
		dialog.open();
		
		/*
		 * If OK was pressed, compute the set of actions that match the regular expression.
		 */
		if (dialog.getReturnCode() != IDialogConstants.OK_ID) {
			return null;
		}
		String regExpr = dialog.getRegularExpression();
		int resultCode = dialog.getAddRemoveChoice();	
		dialog.close();
		
		ActionSet resultSet = reportMgr.reportActionsThatMatchName(regExpr);
		ActionSet newVisibilitySet = null;
		
		/*
		 * Depending on the selected mode, either merge or filter these actions from the
		 * current tab's action set. 
		 */
		ActionSet currentActionSet = (ActionSet)editor.getVisibilityFilterSet();
		switch (resultCode) {
		
		case NameFilterDialog.SELECT_ONLY_MATCHING_ITEMS:
			newVisibilitySet = resultSet;			
			break;
			
		case NameFilterDialog.ADD_MATCHING_ITEMS:
			resultSet.mergeSet(currentActionSet);
			newVisibilitySet = resultSet;
			break;
			
		case NameFilterDialog.REMOVE_MATCHING_ITEMS:
			try {
				newVisibilitySet = (ActionSet)currentActionSet.clone();
				newVisibilitySet.extractSet(resultSet);
			} catch (CloneNotSupportedException e) {
				/* won't happen */
			}
			break;
			
		case NameFilterDialog.SELECT_ALL_ITEMS:
			newVisibilitySet = reportMgr.reportAllActions();
			break;
			
		case NameFilterDialog.DESELECT_ALL_ITEMS:
			newVisibilitySet = new ActionSet(actionsMgr);
			break;
			
		default:
			/* do nothing - silently */
			break;
		}
		
		/* create a new undo/redo operation, for recording this change */
		newVisibilitySet.populateWithParents();
		FilterOperation operation = new FilterOperation(currentActionSet, newVisibilitySet);
		
		/* execute! */
		operation.recordAndInvoke();
		return null;
	}

	/*-------------------------------------------------------------------------------------*/
}
