package com.buildml.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.ui.handlers.HandlerUtil;

import com.buildml.eclipse.ISubEditor;
import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.utils.PackageFilterDialog;
import com.buildml.model.types.PackageSet;

/**
 * Handler for when the user selects the "filterPackage" command, which triggers
 * a pop-up dialog to be shown. This dialog permits the user to select the subset
 * of packages they wish to view in the editor.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class HandlerFilterPackages extends AbstractHandler {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Handle execution of the filterPackages command. 
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		/* figure out which editor is currently active */
		MainEditor editor = (MainEditor) HandlerUtil.getActiveEditor(event);
		ISubEditor subEditor = editor.getActiveSubEditor();
		
		/* 
		 * If the sub-editor supports package filtering, we should start a 
		 * "PackageFilterDialog" and if the user presses "OK", update the 
		 * editor's filter PackageSet with the newly selected set.
		 */
		PackageSet pkgFilterSet = subEditor.getFilterPackageSet();
		if (pkgFilterSet == null) {
			return null;
		}

		boolean showScopes = subEditor.hasFeature("filter-packages-by-scope");
		PackageFilterDialog pkgSelDialog = new PackageFilterDialog(pkgFilterSet, showScopes);
		pkgSelDialog.open();
		
		int retCode = pkgSelDialog.getReturnCode();
		if (retCode == Dialog.OK) {
			subEditor.setFilterPackageSet(pkgSelDialog.getPackageSet());

		} else if (retCode == Dialog.CANCEL) {
			/* do nothing - the Dialog was canceled */
				
		} else {
			System.out.println("Unknown return code");
		}
		return null;
	}

	/*-------------------------------------------------------------------------------------*/
}
