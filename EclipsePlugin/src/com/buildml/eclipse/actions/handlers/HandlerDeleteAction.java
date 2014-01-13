package com.buildml.eclipse.actions.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.UndoOpAdapter;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.types.ActionSet;
import com.buildml.model.undo.MultiUndoOp;
import com.buildml.refactor.CanNotRefactorException;
import com.buildml.refactor.IImportRefactorer;

/**
 * An Eclipse UI Handler for managing the "Delete Action (Promote Children)" UI command. 
 * This handler deletes the specified action and moves its child actions up one level
 * in the tree. The user will be given feedback on anything that may have gone wrong in
 * the process.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class HandlerDeleteAction extends AbstractHandler {
	
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

		/* build an ActionSet of all the selected actions */
		TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);
		ActionSet selectedActions = EclipsePartUtils.getActionSetFromSelection(buildStore, selection);

		/* accumulate all changes into a multiOp */
		MultiUndoOp multiOp = new MultiUndoOp();
		
		/*
		 * For each action that was selected, treat it as an individual "delete action"
		 * operation. 
		 */
		for (int actionId : selectedActions) {
			String actionCmd = (String) actionMgr.getSlotValue(actionId, IActionMgr.COMMAND_SLOT_ID);
			
			try {
				refactorer.deleteAction(multiOp, actionId);
				
			} catch (CanNotRefactorException e) {

				switch (e.getCauseCode()) {
				
				case ACTION_IN_USE:
					AlertDialog.displayErrorDialog("Unable to delete action", 
							"The following action is still in use, and can't be deleted:\n\n" + 
									actionCmd);
					return null;
					
				default:
					AlertDialog.displayErrorDialog("Operation failed", 
						"The \"delete action\" operation for action:\n\n" + actionCmd +
						"\n\nfailed for the following reason: " + e.getCauseCode());
					return null;
				}
			}
		}
		
		/* make it happen */
		new UndoOpAdapter("Delete Action", multiOp).invoke();
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
