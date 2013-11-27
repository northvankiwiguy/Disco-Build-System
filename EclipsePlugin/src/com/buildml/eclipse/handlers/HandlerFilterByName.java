package com.buildml.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;

import com.buildml.eclipse.ISubEditor;
import com.buildml.eclipse.actions.ActionsEditor;
import com.buildml.eclipse.files.FilesEditor;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.NameFilterDialog;
import com.buildml.eclipse.utils.UndoOpAdapter;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IReportMgr;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.ActionSet;
import com.buildml.model.undo.IUndoOp;
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
	private class FilterOperation implements IUndoOp {

		/** The existing visibility set, recording the state before the operation takes place. */
		private IntegerTreeSet existingSet;

		/** The new visibility set to put in place */
		private IntegerTreeSet newSet;
		
		/** The SubEditor we're filtering the content of */
		private ISubEditor subEditor;

		/*--------------------------------------------------------------------------------*/

		/**
		 * Create a new FilterOperation object.
		 * 
		 * @param subEditor   The SubEditor we're filter the content of.
		 * @param existingSet The visibility set, before the operation takes place.
		 * @param newSet      The visibility set to start using.
		 */
		public FilterOperation(ISubEditor subEditor, IntegerTreeSet existingSet, IntegerTreeSet newSet) {
						
			/* 
			 * We need to make a copy of the visibility sets, since they'll be changing
			 * and we need it to be constant.
			 */
			try {
				this.existingSet = (IntegerTreeSet) existingSet.clone();
				this.newSet = (IntegerTreeSet) newSet.clone();
				this.subEditor = subEditor;
			} catch (CloneNotSupportedException e) {
				/* can't happen */
			}
		}

		/*--------------------------------------------------------------------------------*/

		/**
		 * Do, or redo an operation.
		 */
		@Override
		public boolean redo() {
			return useVisibilitySet(newSet);
		}

		/*--------------------------------------------------------------------------------*/

		/**
		 * Undo an operation.
		 */
		@Override
		public boolean undo() {
			return useVisibilitySet(existingSet);
		}
		
		/*--------------------------------------------------------------------------------*/

		/**
		 * Set the current editor's visibility set to the specified set.
		 * @param setToUse The visibility set to use for this editor.
		 * @return The status of the action.
		 */
		private boolean useVisibilitySet(IntegerTreeSet setToUse) {
			
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
			return false;
		}
		
		/*--------------------------------------------------------------------------------*/

	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		ISubEditor subEditor = EclipsePartUtils.getActiveSubEditor();
		if (subEditor instanceof FilesEditor) {
			return executeFilesEditor((FilesEditor)subEditor, event);
		} else if (subEditor instanceof ActionsEditor) {
			return executeActionsEditor((ActionsEditor)subEditor, event);
		}
		
		/* for other editors, do nothing */
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Variant of execute() that's invoked if the current sub-editor is a FilesEditor.
	 * 
	 * @param subEditor   The FileEditor we're operating on. 
	 * @param event       The UI command event.
	 * @return Always null.
	 * @throws ExecutionException Something bad happened.
	 */
	public Object executeFilesEditor(FilesEditor subEditor, ExecutionEvent event) throws ExecutionException {
			
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
		FileSet currentFileSet = (FileSet) subEditor.getVisibilityFilterSet();
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
		FilterOperation operation = new FilterOperation(subEditor, currentFileSet, newVisibilitySet);
		new UndoOpAdapter("Filter Items", operation).invoke();
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Variant of execute() that's invoked if the current sub-editor is an ActionsEditor.
	 * 
	 * @param subEditor     The SubEditor we're filtering. 
	 * @param event         The UI command event.
	 * @return Always null.
	 * @throws ExecutionException Something bad happened.
	 */
	public Object executeActionsEditor(ActionsEditor subEditor, ExecutionEvent event) throws ExecutionException {
			
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
		ActionSet currentActionSet = (ActionSet)subEditor.getVisibilityFilterSet();
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
		FilterOperation operation = new FilterOperation(subEditor, currentActionSet, newVisibilitySet);
		
		/* execute! */
		new UndoOpAdapter("Filter Items", operation).invoke();
		return null;
	}

	/*-------------------------------------------------------------------------------------*/
}
