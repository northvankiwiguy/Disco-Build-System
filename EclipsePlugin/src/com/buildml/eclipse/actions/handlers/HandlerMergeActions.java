package com.buildml.eclipse.actions.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.ConversionUtils;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.UndoOpAdapter;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.types.ActionSet;
import com.buildml.model.undo.MultiUndoOp;
import com.buildml.refactor.CanNotRefactorException;
import com.buildml.refactor.IImportRefactorer;

/**
 * An Eclipse UI Handler for managing the "Merge Actions" UI command. 
 * This handler merges multiple actions into a single action, providing
 * the user feedback on anything that may have gone wrong in the process.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class HandlerMergeActions extends AbstractHandler {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		MainEditor mainEditor = EclipsePartUtils.getActiveMainEditor();
		IBuildStore buildStore = mainEditor.getBuildStore();
		IActionMgr actionMgr = buildStore.getActionMgr();
		IImportRefactorer refactorer = mainEditor.getImportRefactorer();

		String mode = event.getParameter("com.buildml.eclipse.commandParameters.mergeAction");
		if (!mode.equals("selected")) {
			System.out.println("Mode: " + mode);
			return null;
		}
		
		/* build an ActionSet of all the selected actions */
		TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);
		ActionSet selectedActions = EclipsePartUtils.getActionSetFromSelection(buildStore, selection);
		
		/* attempt to perform the "merge action" operation */
		try {
			MultiUndoOp multiOp = new MultiUndoOp();
			refactorer.mergeActions(multiOp, selectedActions);
			new UndoOpAdapter("Merge Actions", multiOp).invoke();
			
		} catch (CanNotRefactorException e) {

			Integer actionInError[] = e.getCauseIDs();
			switch (e.getCauseCode()) {
			
			case ACTION_NOT_ATOMIC:
				String actionString = ConversionUtils.getActionsAsText(actionMgr, actionInError);
				AlertDialog.displayErrorDialog("Merge failed", 
						"The \"merge actions\" operation failed because the following " +
						((actionInError.length == 1) ? "action is not atomic:\n\n" :
													  "actions are not atomic:\n\n") + actionString);
				break;
				
			default:
				AlertDialog.displayErrorDialog("Merge failed", 
					"The \"merge actions\" operation failed for the following reason: " + 
							e.getCauseCode());
			}
		}
		
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
