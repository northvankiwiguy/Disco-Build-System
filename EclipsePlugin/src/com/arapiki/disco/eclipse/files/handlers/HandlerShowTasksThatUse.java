package com.arapiki.disco.eclipse.files.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import com.arapiki.disco.eclipse.DiscoMainEditor;
import com.arapiki.disco.eclipse.utils.EclipsePartUtils;
import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.FileRecord;
import com.arapiki.disco.model.FileSet;
import com.arapiki.disco.model.Reports;
import com.arapiki.disco.model.TaskSet;
import com.arapiki.disco.model.BuildTasks.OperationType;

public class HandlerShowTasksThatUse implements IHandler {

	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

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

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean isHandled() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void removeHandlerListener(IHandlerListener handlerListener) {
		// TODO Auto-generated method stub

	}

}
