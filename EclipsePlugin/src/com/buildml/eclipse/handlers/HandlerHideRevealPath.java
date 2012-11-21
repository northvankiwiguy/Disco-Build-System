package com.buildml.eclipse.handlers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.SubEditor;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.errors.FatalError;
import com.buildml.utils.types.IntegerTreeSet;

/**
 * Command handler for the "hide selected items" and "reveal selected items" menu options.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class HandlerHideRevealPath extends AbstractHandler {

	/*=====================================================================================*
	 * NESTED CLASSES
	 *=====================================================================================*/

	/**
	 * An undo/redo operation for recording changes in the hide/reveal
	 * visibility state of an editor.
	 *
	 * @author Peter Smith <psmith@arapiki.com>
	 */
	private class HideRevealOperation extends AbstractOperation {

		/** The existing visibility set, recording the state before the operation takes place. */
		private IntegerTreeSet existingSet;
		
		/** The tree items whose state will be modified. */
		private List<Object> changesToMake;
		
		/** True if the items should be revealed, else false. */
		private boolean revealState;

		/*--------------------------------------------------------------------------------*/

		/**
		 * Create a new HideRevealOperation object.
		 * 
		 * @param existingSet The visibility set, before the operation takes place.
		 * @param changesToMake The items that need to be modified (revealed/hidden).
		 * @param revealState True if the items should be revealed, else false.
		 */
		public HideRevealOperation(IntegerTreeSet existingSet, 
								   List<Object> changesToMake, boolean revealState) {
			
			/* set up the operation "label" that appears in the undo/redo menu */
			super(revealState ? "Reveal Items" : "Hide Items");
			
			/* 
			 * We need to make a copy of the visibility set, since it'll be changing
			 * and we need it to be constant.
			 */
			try {
				this.existingSet = (IntegerTreeSet) existingSet.clone();
			} catch (CloneNotSupportedException e) {
				/* can't happen */
			}
			
			/* save these for later undos/redos */
			this.changesToMake = changesToMake;
			this.revealState = revealState;
		}

		/*--------------------------------------------------------------------------------*/

		/**
		 * Execute the hide/reveal operation.
		 */
		@Override
		public IStatus execute(IProgressMonitor monitor, IAdaptable info)
				throws ExecutionException {
			return redo(monitor, info);
		}

		/*--------------------------------------------------------------------------------*/

		/**
		 * Do, or redo an operation.
		 */
		@Override
		public IStatus redo(IProgressMonitor monitor, IAdaptable info)
				throws ExecutionException {

			/* mark all the selected items with their new state */
			SubEditor subEditor = EclipsePartUtils.getActiveSubEditor();
			for (Object item : changesToMake) {
				subEditor.setItemVisibilityState(item, revealState);
			}
			return Status.OK_STATUS;
		}

		/*--------------------------------------------------------------------------------*/

		/**
		 * Undo an operation.
		 */
		@Override
		public IStatus undo(IProgressMonitor monitor, IAdaptable info)
				throws ExecutionException {
			
			/* Make a copy of the set, so that our copy stays intact */
			IntegerTreeSet revertedSet = null;
			try {
				revertedSet = (IntegerTreeSet) existingSet.clone();
			} catch (CloneNotSupportedException e) {
				/* can't happen */
			}
			
			/* 
			 * Set the visibility filter set to what it was before the operation
			 * was invoked. Then refresh our view with the old selection.
			 */
			SubEditor subEditor = EclipsePartUtils.getActiveSubEditor();
			subEditor.setVisibilityFilterSet(revertedSet);
			subEditor.refreshView(true);
			return Status.OK_STATUS;
		}

	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
				
		/* Figure out whether to reveal or hide the element */
		boolean revealState = false;
		String cmdName = "com.buildml.eclipse.commandParameters.hideRevealType";
		String opType = event.getParameter(cmdName);
		if (opType == null) {
			return null;
		}
		if (opType.equals("hide")) {
			revealState = false;
		} else if (opType.equals("reveal")) {
			revealState = true;
		} else {
			throw new FatalError("Unable to handle command: " + cmdName);
		}

		/* fetch the active editor, and its BuildStore */
		SubEditor subEditor = EclipsePartUtils.getActiveSubEditor();
		
		/* get the set of items (files/actions) that are currently visible */
		IntegerTreeSet existingVisibleItems = subEditor.getVisibilityFilterSet();
		
		/* fetch the items that are currently selected, saving them in a list */
		TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);
		Iterator<?> iter = selection.iterator();
		List<Object> listOfChanges = new ArrayList<Object>();
		while (iter.hasNext()) {
			listOfChanges.add(iter.next());
		}
		
		/* create a new undo/redo operation, for recording this change */
		HideRevealOperation operation = 
				new HideRevealOperation(existingVisibleItems, listOfChanges, revealState); 
		MainEditor editor = EclipsePartUtils.getActiveMainEditor();		
		operation.addContext(editor.getUndoContext());		
		
		/* make it so... */
		IOperationHistory history = OperationHistoryFactory.getOperationHistory();
		history.execute(operation, null, null);
		return null;
	}

	/*-------------------------------------------------------------------------------------*/
}
