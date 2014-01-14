package com.buildml.eclipse.actions.handlers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.bobj.UIAction;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.ConversionUtils;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.UndoOpAdapter;
import com.buildml.eclipse.utils.dialogs.SubGroupSelectionDialog;
import com.buildml.eclipse.utils.errors.FatalError;
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
		IImportRefactorer refactorer = mainEditor.getImportRefactorer();
		TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);
		String mode = event.getParameter("com.buildml.eclipse.commandParameters.mergeAction");

		/*
		 * In "selected" mode, we merge together all of the selected actions (into a single
		 * action).
		 */
		if (mode.equals("selected")) {
			
			/* build an ActionSet of all the selected actions */
			ActionSet selectedActions = EclipsePartUtils.getActionSetFromSelection(buildStore, selection);
			MultiUndoOp multiOp = new MultiUndoOp();
			performMergeActions(refactorer, buildStore, selectedActions, multiOp);
			new UndoOpAdapter("Merge Actions", multiOp).invoke();
		}
		
		/*
		 * In "groups" mode, we provide a dialog box where the user can select the size of
		 * each sub-group. We then proceed to merge those subgroups.
		 */
		else if (mode.equals("groups")) {
			MultiUndoOp multiOp = new MultiUndoOp();
			performGroupMerges(refactorer, buildStore, selection, multiOp);
			new UndoOpAdapter("Merge Actions Into Groups", multiOp).invoke();
		}
		
		/* else, not supported - throw internal error */
		else {
			throw new FatalError("Unsupported mode in com.buildml.eclipse.commandParameters.mergeAction");
		}
		
		return null;
	}
	
	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Perform the merge action operation, with appropriate sanity tests, error reporting,
	 * and scheduling of a MultiUndoOp. This method will merge all specified actions into
	 * a single action.
	 * 
	 * @param refactorer		The IImportRefactorer to do the merging work.
	 * @param buildStore		The IBuildStore that stores the actions.
	 * @param selectedActions	The actions to be merged.
	 * @param multiOp			The MultiUndoOp that all operations will be added to.
	 */
	private void performMergeActions(IImportRefactorer refactorer,
			IBuildStore buildStore, ActionSet selectedActions, MultiUndoOp multiOp) {
		
		IActionMgr actionMgr = buildStore.getActionMgr();
		
		/* attempt to perform the "merge action" operation */
		try {
			refactorer.mergeActions(multiOp, selectedActions);
			
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
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given the current selection, prompt the user to divide that selection into sub-groups,
	 * with each sub-group being merged into an action.
	 * 
	 * @param refactorer	The IImportRefactorer to do the merging work.
	 * @param buildStore 	The IBuildStore that contains the actions.
	 * @param selection 	The selection set of tree elements.
	 * @param multiOp       The undo/redo operation into which we schedule merges.
	 * 
	 */
	private void performGroupMerges(IImportRefactorer refactorer, IBuildStore buildStore, 
									TreeSelection selection, MultiUndoOp multiOp) {
		
		IActionMgr actionMgr = buildStore.getActionMgr();
		
		/*
		 * Compute two ordered lists: 1) The selection action IDs, 2) The command strings
		 * for those action IDs. The order is important, as it'll dictate which actions
		 * are merged into which sub-groups.
		 */
		List<Integer> actionIDs = new ArrayList<Integer>(selection.size());
		List<String> actionCmds = new ArrayList<String>(selection.size());
		Iterator<Object> iter = selection.iterator();
		while (iter.hasNext()) {
			Object item = iter.next();
			if (item instanceof UIAction) {
				UIAction action = (UIAction)item;
				int actionId = action.getId();
				String cmdString = (String) actionMgr.getSlotValue(actionId, IActionMgr.COMMAND_SLOT_ID);
				actionIDs.add(actionId);
				actionCmds.add(cmdString);
			}
		}
		
		/*
		 * Pass the list to a Dialog, to ask the user the size of the sub-group to use.
		 */
		SubGroupSelectionDialog dialog = 
				new SubGroupSelectionDialog(actionCmds.toArray(new String[actionCmds.size()]));
		int status = dialog.open();
		if (status == SubGroupSelectionDialog.CANCEL) {
			return;
		}
		
		/*
		 * Fetch the selected sub-group size (0 == no selection was made).
		 */
		int groupSize = dialog.getGroupSize();
		if (groupSize == 0) {
			return;
		}
		
		/*
		 * Now, perform the "merge action" operation on each subgroup. We create a new
		 * ActionSet for each subgroup and use performMergeActions() to merge them each
		 * into a single action.
		 */
		int size = actionIDs.size();
		ActionSet actionsToMerge = null;
		for (int i = 0; i != size; i++) {
			
			/* are we starting a new sub-group? */
			if ((i % groupSize) == 0) {
				actionsToMerge = new ActionSet(actionMgr);
			}
			
			/* add the action ID to this sub group */
			actionsToMerge.add(actionIDs.get(i));
			
			/* did we reach the end of the sub-group? */
			if (((i+1) % groupSize) == 0) {
				performMergeActions(refactorer, buildStore, actionsToMerge, multiOp);
			}
		}
		
	}
	
	/*-------------------------------------------------------------------------------------*/
}
