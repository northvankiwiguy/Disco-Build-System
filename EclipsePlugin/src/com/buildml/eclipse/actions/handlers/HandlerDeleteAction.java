package com.buildml.eclipse.actions.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.actions.dialogs.MatchPatternSelectionDialog;
import com.buildml.eclipse.bobj.UIAction;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.UndoOpAdapter;
import com.buildml.eclipse.utils.errors.FatalError;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.types.ActionSet;
import com.buildml.model.undo.MultiUndoOp;
import com.buildml.refactor.CanNotRefactorException;
import com.buildml.refactor.IImportRefactorer;
import com.buildml.utils.string.ShellCommandUtils;

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

		String mode = event.getParameter("com.buildml.eclipse.commandParameters.deleteAction");
		
		/*
		 * In "selected" mode, we delete all the actions that have been selected by the user.
		 */
		if (mode.equals("selected")) {
			/* build an ActionSet of all the selected actions */
			TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);
			ActionSet selectedActions = EclipsePartUtils.getActionSetFromSelection(buildStore, selection);
			performDeletion(mainEditor, buildStore, selectedActions);
		}
		
		/*
		 * In pattern mode, we query the user for a regular expression to be used for
		 * selecting which patterns.
		 */
		else if (mode.equals("pattern")) {
			TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);
			if (selection.size() != 1) {
				AlertDialog.displayErrorDialog("Can't delete",
						"When deleting matching actions, only one action may be selected.");
				return null;
			}
			
			/* use the selected action's command string as the initial pattern */
			UIAction selectedAction = (UIAction)selection.getFirstElement();
			String selectedCmd = (String) actionMgr.getSlotValue(selectedAction.getId(), IActionMgr.COMMAND_SLOT_ID);
			selectedCmd = ShellCommandUtils.joinCommandLine(selectedCmd);
			
			/* invoke a Dialog box to query the user to select the pattern, and then the matching actions */
			MatchPatternSelectionDialog dialog = new MatchPatternSelectionDialog(buildStore, selectedCmd, "Delete");
			int status = dialog.open();
			if (status == MatchPatternSelectionDialog.OK) {
				ActionSet selectedActions = dialog.getMatchingActions();
				performDeletion(mainEditor, buildStore, selectedActions);
			}	
		}
		
		/* else, not supported - throw internal error */
		else {
			throw new FatalError("Unsupported mode in com.buildml.eclipse.commandParameters.deleteAction");
		}
		
		return null;
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Perform the actual deletion of actions by evaluating the deletion operation, giving
	 * errors where necessary, and scheduling a MultiUndoOp.
	 * 
	 * @param mainEditor		The main editor we're operating with.
	 * @param buildStore		The IBuildStore we're operating on.
	 * @param selectedActions	An ActionSet of actions to be deleted.
	 */
	private void performDeletion(MainEditor mainEditor, IBuildStore buildStore,
			ActionSet selectedActions) {
		
		IImportRefactorer refactorer = mainEditor.getImportRefactorer();
		IActionMgr actionMgr = buildStore.getActionMgr();

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
					return;
					
				default:
					AlertDialog.displayErrorDialog("Operation failed", 
						"The \"delete action\" operation for action:\n\n" + actionCmd +
						"\n\nfailed for the following reason: " + e.getCauseCode());
					return;
				}
			}
		}
		
		/* make it happen */
		new UndoOpAdapter("Delete Action", multiOp).invoke();
	}
	
	/*-------------------------------------------------------------------------------------*/
}
