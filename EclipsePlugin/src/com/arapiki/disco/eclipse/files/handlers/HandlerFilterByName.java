package com.arapiki.disco.eclipse.files.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;

import com.arapiki.disco.eclipse.files.DiscoFilesEditor;
import com.arapiki.disco.eclipse.utils.EclipsePartUtils;
import com.arapiki.disco.eclipse.utils.NameFilterDialog;
import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.Reports;
import com.arapiki.disco.model.types.FileSet;

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
		DiscoFilesEditor editor = EclipsePartUtils.getActiveDiscoFilesEditor();
		if (editor == null) {
			return null;
		}
		BuildStore buildStore = EclipsePartUtils.getActiveBuildStore();
		Reports reports = buildStore.getReports();
		
		/* 
		 * Display a dialog, which asks the user to provide a regular expression string,
		 * as well as an "add these files" or "remove these files" choice.
		 */
		NameFilterDialog dialog = new NameFilterDialog();
		dialog.open();
		
		/*
		 * If OK was pressed, compute the set of files that match the regular expression.
		 */
		if (dialog.getReturnCode() != IDialogConstants.OK_ID) {
			return null;
		}
		String regExpr = dialog.getRegularExpression();
		FileSet resultSet = reports.reportFilesThatMatchName(regExpr);
		
		/*
		 * Depending on the selected mode, either merge or filter these files from the
		 * current tab's file set. 
		 */
		FileSet currentFileSet = editor.getVisibilityFilterSet();
		int addRemoveChoice = dialog.getAddRemoveChoice();
		if (addRemoveChoice == NameFilterDialog.ADD_ITEMS) {
			currentFileSet.mergeSet(resultSet);
		} else {
			currentFileSet.extractSet(resultSet);
		}
		currentFileSet.populateWithParents();
		
		/*
		 * Refresh the file set. 
		 */
		editor.refreshView(true);
		return null;
	}

	/*-------------------------------------------------------------------------------------*/
}
