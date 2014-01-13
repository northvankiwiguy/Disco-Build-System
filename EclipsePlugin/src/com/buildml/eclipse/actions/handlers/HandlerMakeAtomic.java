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
 * An Eclipse UI Handler for managing the "Make Atomic" UI command. 
 * This handler tries to make an action atomic (i.e. merges it with it's children)
 * and gives the user feedback on anything that may have gone wrong in the process.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class HandlerMakeAtomic extends AbstractHandler {
	
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

		/*
		 * For each action that was selected, treat it as an individual "make atomic"
		 * operation. 
		 */
		MultiUndoOp multiOp = new MultiUndoOp();
		boolean changesMade = false;
		for (int actionId : selectedActions) {
			String actionCmd = (String) actionMgr.getSlotValue(actionId, IActionMgr.COMMAND_SLOT_ID);
			Integer children[] = actionMgr.getChildren(actionId);
			
			if (children.length != 0) {
				try {
					refactorer.makeActionAtomic(multiOp, actionId);
					changesMade = true;

				} catch (CanNotRefactorException e) {

					AlertDialog.displayErrorDialog("Operation failed", 
							"The \"make atomic\" operation for action:\n\n" + actionCmd +
							"\nfailed for the following reason: " + e.getCauseCode());
				}
			}
		}
		
		if (changesMade) {
			new UndoOpAdapter("Make Atomic", multiOp).invoke();
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
