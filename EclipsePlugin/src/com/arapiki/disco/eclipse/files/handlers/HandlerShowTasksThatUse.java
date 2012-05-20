package com.arapiki.disco.eclipse.files.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.arapiki.disco.eclipse.DiscoMainEditor;
import com.arapiki.disco.eclipse.utils.EclipsePartUtils;
import com.buildml.model.BuildStore;
import com.buildml.model.Reports;
import com.buildml.model.BuildTasks.OperationType;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.TaskSet;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class HandlerShowTasksThatUse extends AbstractHandler {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
				
		/* fetch the FileRecord nodes that were selected */
		TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);
		
		/* fetch the active editor, and its BuildStore */
		DiscoMainEditor editor = (DiscoMainEditor)HandlerUtil.getActiveEditor(event);
		BuildStore buildStore = editor.getBuildStore();
		
		/* build a FileSet of all the selected files */
		FileSet selectedFiles = EclipsePartUtils.getFileSetFromSelection(buildStore, selection);

		/* what operation is the user asking for? "use", "read", or "write"? By default, assume "use" */
		String accessType = event.getParameter("com.arapiki.disco.eclipse.commandParameters.accessType");
		OperationType opType = OperationType.OP_UNSPECIFIED;
		if (accessType.equals("read")){
			opType = OperationType.OP_READ;
		} else if (accessType.equals("write")){
			opType = OperationType.OP_WRITE;
		}
		
		/* get the set of tasks that use these files */
		Reports reports = buildStore.getReports();
		TaskSet userTasks = reports.reportTasksThatAccessFiles(selectedFiles, opType);

		for (Integer integer : userTasks) {
			System.out.println("Task: " + integer);
		}
		
		
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
