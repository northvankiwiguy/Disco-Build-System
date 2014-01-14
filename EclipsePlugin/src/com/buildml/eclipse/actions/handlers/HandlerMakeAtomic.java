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

		String mode = event.getParameter("com.buildml.eclipse.commandParameters.makeAtomic");

		/*
		 * In "selected" mode, we operate on all the actions that have been selected by the user.
		 */
		if (mode.equals("selected")) {
			/* build an ActionSet of all the selected actions */
			TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);
			ActionSet selectedActions = EclipsePartUtils.getActionSetFromSelection(buildStore, selection);
			performMakeAtomic(mainEditor, buildStore, selectedActions);
		}
		
		/*
		 * In pattern mode, we query the user for a regular expression to be used for
		 * selecting which patterns.
		 */
		else if (mode.equals("pattern")) {
			TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);
			if (selection.size() != 1) {
				AlertDialog.displayErrorDialog("Can't make atomic",
						"When making matching actions atomic, only one action may be selected.");
				return null;
			}
			
			/* use the selected action's command string as the initial pattern */
			UIAction selectedAction = (UIAction)selection.getFirstElement();
			String selectedCmd = (String) actionMgr.getSlotValue(selectedAction.getId(), IActionMgr.COMMAND_SLOT_ID);
			selectedCmd = ShellCommandUtils.joinCommandLine(selectedCmd);
			
			/* invoke a Dialog box to query the user to select the pattern, and then the matching actions */
			MatchPatternSelectionDialog dialog = new MatchPatternSelectionDialog(buildStore, selectedCmd, "Make Atomic");
			int status = dialog.open();
			if (status == MatchPatternSelectionDialog.OK) {
				ActionSet selectedActions = dialog.getMatchingActions();
				performMakeAtomic(mainEditor, buildStore, selectedActions);
			}	
		}
		
		/* else, not supported - throw internal error */
		else {
			throw new FatalError("Unsupported mode in com.buildml.eclipse.commandParameters.makeAtomic");
		}
		return null;
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Perform the "make atomic" operation, but validating the operation, reporting errors
	 * if necessary, then scheduling a MultiUndoOp.
	 * 
	 * @param mainEditor		The main editor we're operating on.
	 * @param buildStore		The IBuildStore associated with the operation.
	 * @param selectedActions	The actions to make atomic.
	 */
	private void performMakeAtomic(MainEditor mainEditor,
			IBuildStore buildStore, ActionSet selectedActions) {
		IActionMgr actionMgr = buildStore.getActionMgr();
		IImportRefactorer refactorer = mainEditor.getImportRefactorer();
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
	}
	
	/*-------------------------------------------------------------------------------------*/
}
