package com.buildml.eclipse.files.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.buildml.eclipse.EditorOptions;
import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.SubEditor;
import com.buildml.eclipse.actions.ActionsEditor;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.model.IActionMgr;
import com.buildml.model.IActionMgr.OperationType;
import com.buildml.model.IBuildStore;
import com.buildml.model.IReportMgr;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.ActionSet;

/**
 * Eclipse Command Handler for the "show actions that ..." commands.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class HandlerShowActionsThatUse extends AbstractHandler {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
				
		/* fetch the FileRecord nodes that were selected */
		TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);
		
		/* fetch the active editor, and its BuildStore */
		MainEditor mainEditor = (MainEditor)HandlerUtil.getActiveEditor(event);
		IBuildStore buildStore = mainEditor.getBuildStore();
		IReportMgr reportMgr = buildStore.getReportMgr();
		SubEditor existingEditor = mainEditor.getActiveSubEditor();
		
		/* build a FileSet of all the selected files */
		FileSet selectedFiles = EclipsePartUtils.getFileSetFromSelection(buildStore, selection);

		/* what operation is the user asking for? "use", "read", or "write"? By default, assume "use" */
		String accessType = event.getParameter("com.buildml.eclipse.commandParameters.accessType");
		
		/*
		 * Handle the "actions execute in this directory" case.
		 */
		ActionSet userActions = null;
		String tabName;
		if (accessType.equals("workdir")) {
			userActions = reportMgr.reportActionsInDirectory(selectedFiles);
			tabName = "Actions executing in directory";
		} 
		
		/* 
		 * Handle the read, write and use cases.
		 */
		else {
			OperationType opType = OperationType.OP_UNSPECIFIED;
			if (accessType.equals("read")){
				opType = OperationType.OP_READ;
			} else if (accessType.equals("write")){
				opType = OperationType.OP_WRITE;
			}

			/* get the set of actions that use/read/write these files */
			userActions = reportMgr.reportActionsThatAccessFiles(selectedFiles, opType);
			tabName = "Actions that " + accessType;
		}
		
		/* if the result set is empty, don't open an editor, but instead open a dialog */
		if (userActions.size() == 0) {
			if (accessType.equals("workdir")) {
				AlertDialog.displayInfoDialog("No results", "There are no actions that execute " +
						"in this directory.");
			} else {
				AlertDialog.displayInfoDialog("No results", "There are no related actions that " + 
						accessType + " these files.");
			}
			return null;
		}
			
		/* create a new editor that displays the resulting set */
		ActionsEditor newEditor = 
			new ActionsEditor(buildStore, tabName);
		userActions.populateWithParents();
		newEditor.setOptions(existingEditor.getOptions() & ~EditorOptions.OPT_SHOW_HIDDEN);
		newEditor.setVisibilityFilterSet(userActions);
		
		/* add the new editor as a new tab */
		mainEditor.newPage(newEditor);
		mainEditor.setActiveEditor(newEditor);
		
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
