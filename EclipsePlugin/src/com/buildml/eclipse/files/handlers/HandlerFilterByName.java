package com.buildml.eclipse.files.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;

import com.buildml.eclipse.files.FilesEditor;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.NameFilterDialog;
import com.buildml.model.BuildStore;
import com.buildml.model.FileNameSpaces;
import com.buildml.model.Reports;
import com.buildml.model.types.FileSet;

/**
 * Command Handler for the "filter by name" command.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class HandlerFilterByName extends AbstractHandler {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		/*
		 * Figure out which editor/sub-editor this event applies to.
		 */
		FilesEditor editor = EclipsePartUtils.getActiveFilesEditor();
		if (editor == null) {
			return null;
		}
		BuildStore buildStore = EclipsePartUtils.getActiveBuildStore();
		FileNameSpaces fileMgr = buildStore.getFileNameSpaces();
		Reports reports = buildStore.getReports();
		
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
		
		FileSet resultSet = reports.reportFilesThatMatchName(regExpr);
		FileSet newVisibilitySet = null;
		
		/*
		 * Depending on the selected mode, either merge or filter these files from the
		 * current tab's file set. 
		 */
		FileSet currentFileSet = editor.getVisibilityFilterSet();
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
			newVisibilitySet = reports.reportAllFiles();
			break;
			
		case NameFilterDialog.DESELECT_ALL_ITEMS:
			newVisibilitySet = new FileSet(fileMgr);
			break;
			
		default:
			/* do nothing - silently */
			break;
		}
		
		/*
		 * Refresh the file set. 
		 */
		newVisibilitySet.populateWithParents();
		editor.setVisibilityFilterSet(newVisibilitySet);
		editor.refreshView(true);
		return null;
	}

	/*-------------------------------------------------------------------------------------*/
}
