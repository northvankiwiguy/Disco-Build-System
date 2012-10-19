package com.buildml.eclipse.actions.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.buildml.eclipse.EditorOptions;
import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.SubEditor;
import com.buildml.eclipse.actions.ActionsEditor;
import com.buildml.eclipse.files.FilesEditor;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.model.IBuildStore;
import com.buildml.model.IReportMgr;
import com.buildml.model.impl.ActionMgr.OperationType;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.TaskSet;

/**
 * Eclipse Command Handler for the "show files used by..." commands.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class HandlerShowFilesUsedBy extends AbstractHandler {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
				
		/* fetch the Tree nodes that were selected */
		TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);
		
		/* fetch the active editor, and its BuildStore */
		MainEditor mainEditor = (MainEditor)HandlerUtil.getActiveEditor(event);
		IBuildStore buildStore = mainEditor.getBuildStore();
		SubEditor existingEditor = mainEditor.getActiveSubEditor();
		
		/* build an ActionSet of all the selected files */
		TaskSet selectedActions = EclipsePartUtils.getActionSetFromSelection(buildStore, selection);

		/* 
		 * what operation is the user asking for? "used", "read", "written", "modified" or "deleted"? 
		 * By default, assume "used".
		 */
		String accessType = event.getParameter("com.buildml.eclipse.commandParameters.accessType");
		OperationType opType = OperationType.OP_UNSPECIFIED;
		if (accessType.equals("read")){
			opType = OperationType.OP_READ;
		} else if (accessType.equals("written")){
			opType = OperationType.OP_WRITE;
		} else if (accessType.equals("modified")){
			opType = OperationType.OP_MODIFIED;
		} else if (accessType.equals("deleted")){
			opType = OperationType.OP_DELETE;
		}
		
		/* get the set of files that are used/read/written/modified/deleted by these actions */
		IReportMgr reportMgr = buildStore.getReportMgr();
		FileSet userFiles = reportMgr.reportFilesAccessedByTasks(selectedActions, opType);
		
		/* if the result set is empty, don't open an editor, but instead open a dialog */
		if (userFiles.size() == 0) {
			AlertDialog.displayInfoDialog("No results", 
					"There are no files that are " + accessType + " by these actions.");
			return null;
		}
			
		/* create a new editor that displays the resulting set */
		FilesEditor newEditor = 
			new FilesEditor(buildStore, "Files " + accessType);
		userFiles.populateWithParents();
		newEditor.setOptions(existingEditor.getOptions() & ~EditorOptions.OPT_SHOW_HIDDEN);
		newEditor.setVisibilityFilterSet(userFiles);
		
		/* add the new editor as a new tab */
		mainEditor.newPage(newEditor);
		mainEditor.setActiveEditor(newEditor);
		
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
