/*******************************************************************************
 * Copyright (c) 2013 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.packages.handlers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.graphiti.ui.platform.GraphitiConnectionEditPart;
import org.eclipse.graphiti.ui.platform.GraphitiShapeEditPart;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.bobj.UIAction;
import com.buildml.eclipse.bobj.UIDirectory;
import com.buildml.eclipse.bobj.UIFile;
import com.buildml.eclipse.bobj.UIFileGroup;
import com.buildml.eclipse.packages.PackageDiagramEditor;
import com.buildml.eclipse.packages.layout.LayoutAlgorithm;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.GraphitiUtils;
import com.buildml.eclipse.utils.UndoOpAdapter;
import com.buildml.eclipse.utils.handlers.AbstractHandlerWithProgress;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMemberMgr.MemberDesc;
import com.buildml.model.IPackageRootMgr;
import com.buildml.model.undo.IUndoOp;
import com.buildml.model.undo.MultiUndoOp;
import com.buildml.refactor.CanNotRefactorException;
import com.buildml.refactor.CanNotRefactorException.Cause;
import com.buildml.refactor.IImportRefactorer;
import com.buildml.utils.errors.ErrorCode;

/**
 * An Eclipse UI Handler for managing the "Move to Package" UI command.
 * 
 * @author Peter Smith <psmith@arapiki.com>
 */
public class HandlerMoveToPackage extends AbstractHandlerWithProgress {

	/*=====================================================================================*
	 * NESTED CLASSES
	 *=====================================================================================*/
	
	/**
	 * A private IUndoOp for laying out a package. We encapsulate this as an IUndoOp so
	 * that the layout operation can be placed into a MultiUndoOp and performed in the same
	 * operation as the import. Layout requires the import to be complete before it can
	 * compute the layout information, so this operation must be sequenced appropriately.
	 *
	 * @author Peter Smith <psmith@arapiki.com>
	 */
	private class LayoutOp implements IUndoOp {

		/** 
		 * This is null the first time "redo" is called, or points to a valid MultiUndoOp
		 * if the layout strategy has already been computed. That is, the layout algorithm
		 * is only executed the first time redo() is called.
		 */
		private MultiUndoOp layoutOp = null;
		
		/** ID of the package we're laying out */
		private int pkgId;
		
		/** The layoutAlgorithm to use for laying out the package */
		private LayoutAlgorithm layoutAlgorithm;
		
		/**
		 * Create a new LayoutOp object.
		 * @param pkgId	ID of the package to lay out.
		 * @param layoutAlgorithm The layout algorithm to use.
		 */
		public LayoutOp(LayoutAlgorithm layoutAlgorithm, int pkgId) {
			this.pkgId = pkgId;
			this.layoutAlgorithm = layoutAlgorithm;
		}
		
		/**
		 * Undo the layout operation.
		 */
		@Override
		public boolean undo() {
			return layoutOp.undo();
		}

		/**
		 * Perform the operation for the first time, or redo an operation that has previously
		 * been undone. If this is the first time, we must execute the layout algorithm. If
		 * this is a "redo", we just replay the operation we already have.
		 */
		@Override
		public boolean redo() {
			
			/* Schedule the individual layout steps - first time only */			
			if (layoutOp == null) {
				layoutOp = new MultiUndoOp();
				layoutAlgorithm.autoLayoutPackage(layoutOp, pkgId);
			} 
			
			/* now actually perform the steps */
			return layoutOp.redo();
		}
		
	}
	
	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** Our IBuildStore */
	private IBuildStore buildStore;
	
	/** This editor's layout algorithm */
	private LayoutAlgorithm layoutAlgorithm;
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.utils.handlers.AbstractHandlerWithProgress#getCommandName()
	 */
	@Override
	public String getCommandName() {
		return "Moving to Package";
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Execute the "Move to Package" command.
	 */
	@Override
	public Object executeWithProgress(ExecutionEvent event) {

		buildStore = EclipsePartUtils.getActiveBuildStore();
		
		List<MemberDesc> members = 
				getSelectedObjects((IStructuredSelection) HandlerUtil.getCurrentSelection(event));

		/*
		 * Show the "which package to move to" Dialog in the UI-thread.
		 */
		final Integer pkgIdReturn[] = new Integer[1];
		pkgIdReturn[0] = null;
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				final MoveToPackageDialog dialog = new MoveToPackageDialog(buildStore);
				int status = dialog.open();
				if (status == MoveToPackageDialog.OK) {
					pkgIdReturn[0] = dialog.getPackageId();
				}
			}
		});
		
		if (pkgIdReturn[0] != null) {
			final int pkgId = pkgIdReturn[0];

			/* all changes will be packaged in a multiOp */
			MultiUndoOp multiOp = new MultiUndoOp();
			
			/* ask the refactorer to perform the move */
			final MainEditor editor = EclipsePartUtils.getActiveMainEditor();
			if (editor != null) {
				IImportRefactorer refactorer = editor.getImportRefactorer();
				try {
					/* plan the move to the new package (multiOp will be populated) */
					refactorer.moveMembersToPackage(multiOp, pkgId, members);
					
					/* 
					 * Open the package diagram so the user can see the results. Note
					 * that we do this before the layout operation, since the layout algorithm
					 * is obtained from the currently opened editor.
					 */
					Display.getDefault().syncExec(new Runnable() {	
						@Override
						public void run() {
							editor.openPackageDiagram(pkgId);
							PackageDiagramEditor pde = EclipsePartUtils.getActivePackageDiagramEditor();
							layoutAlgorithm = pde.getLayoutAlgorithm();
						}
					});
					
					/* schedule the layout operation to happen */
					multiOp.add(new LayoutOp(layoutAlgorithm, pkgId));
					
					/* invoke the changes, and record in undo/redo history */
					new UndoOpAdapter("Move to Package", multiOp).invoke();
					
				} catch (CanNotRefactorException e) {
					displayErrorMessage(pkgId, e.getCauseCode(), e.getCauseIDs());
				}

			}
		}
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Determine whether this handler is enabled. To do so, all the selected elements must
	 * be valid/recognized business objects, such as UIAction, UIFile, etc.
	 */
	@Override
	public boolean isEnabled() {
		
		/* 
		 * Get the list of business objects that are selected, return false if an unhandled
		 * object is selected.
		 */
		List<MemberDesc> objectList = getSelectedObjects(EclipsePartUtils.getSelection());
		return (objectList != null);
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * @param selection The current Eclipse selection
	 * @return A list of valid business objects (UIAction, UIFileGroup, etc) that have been
	 * selected by the user. Returns null if any non-valid objects were selected. Note that
	 * connection arrows are silently ignored, rather than flagged as invalid.
	 */
	private List<MemberDesc> getSelectedObjects(IStructuredSelection selection) {
		
		/* we'll return a list of valid business objects */
		List<MemberDesc> result = new ArrayList<MemberDesc>();
		
		Iterator<Object> iter = selection.iterator();
		while (iter.hasNext()) {
			Object obj = iter.next();
			
			/* Graphiti shapes */
			if (obj instanceof GraphitiShapeEditPart) {
				GraphitiShapeEditPart shape = (GraphitiShapeEditPart)obj;
				Object bo = GraphitiUtils.getBusinessObject(shape.getPictogramElement());
				if (bo instanceof UIAction) {
					result.add(new MemberDesc(IPackageMemberMgr.TYPE_ACTION, ((UIAction)bo).getId(), 0, 0));
				} else if (bo instanceof UIFileGroup) {
					result.add(new MemberDesc(IPackageMemberMgr.TYPE_FILE_GROUP, ((UIFileGroup)bo).getId(), 0, 0));
				}
			}
			
			/* silently ignore connections */
			else if (obj instanceof GraphitiConnectionEditPart) {
				/* silently do nothing - not an error */
			}
			
			/* Other objects, selectable from TreeViewers (rather than from Graphiti diagrams) */
			else if (obj instanceof UIAction){
				result.add(new MemberDesc(IPackageMemberMgr.TYPE_ACTION, ((UIAction)obj).getId(), 0, 0));
			}
			else if (obj instanceof UIFile) {
				result.add(new MemberDesc(IPackageMemberMgr.TYPE_FILE, ((UIFile)obj).getId(), 0, 0));
			}
			else if (obj instanceof UIDirectory) {
				// TODO: expand this into files?
				result.add(new MemberDesc(IPackageMemberMgr.TYPE_FILE, ((UIFile)obj).getId(), 0, 0));
			}
			
			/* else, anything else is invalid */
			else {
				return null;
			}
		}
		
		/* finally, there must be at least one valid thing selected */
		if (result.size() > 0) {
			return result;
		}
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Display a meaningful error message to the user. They need to understand why their
	 * "move to package" operation failed.
	 * 
	 * @param pkgId 
	 * @param causeCode	The exception's cause code.
	 * @param causeIDs	The list of ID (actions, files, etc) that caused the problem.
	 */
	private void displayErrorMessage(int pkgId, Cause causeCode, Integer[] causeIDs) {
		
		IPackageRootMgr pkgRootMgr = buildStore.getPackageRootMgr();
		
		/* we'll populate this StringBuffer with the error message */
		StringBuffer sb = new StringBuffer();
		
		/*
		 * Based on the cause, construct a meaningful message.
		 */
		switch (causeCode) {
		case PATH_OUT_OF_RANGE:
			sb.append("The following files are not enclosed within the destination package's roots:\n\n");
			displayPaths(sb, causeIDs);
			sb.append("\nThe package's root path is:\n\n");
			int pkgRootId = pkgRootMgr.getPackageRoot(pkgId, IPackageRootMgr.SOURCE_ROOT);
			if (pkgRootId == ErrorCode.NOT_FOUND) {
				sb.append("<invalid>");
			} else {
				displayPaths(sb, new Integer[] { pkgRootId });
			}
			sb.append("\nTo resolve this issue, consider the following solutions:\n");
			sb.append("- For system headers and libraries, delete the files so they won't\n" +
					  "  be explicitly imported onto the package diagram.\n");
			sb.append("- For temporary files which are shared between two actions,\n" +
					  "  merge the actions together.\n");
			sb.append("- First, move the files into a different package (with more\n" +
					  "  appropriate roots), from where they can be referenced.\n");
			sb.append("- Modify this package's roots so they encompass these files.\n");
			break;
			
		case FILE_IS_MODIFIED:
			sb.append("The following files are written-to by multiple actions:\n\n");
			displayPaths(sb, causeIDs);
			break;
	
		default:
			sb.append("An unexpected error has occurred while trying to move to a new package.");
		}
		
		/*
		 * Display the message.
		 */
		AlertDialog.displayErrorDialog("Unable To Move To Package", sb.toString());
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Display a list of paths, by appending them to a String buffer.
	 * @param sb 		The StringBuffer to append path names to.
	 * @param pathIds	The array of path IDs.
	 */
	private void displayPaths(StringBuffer sb, Integer[] pathIds) {
		
		IFileMgr fileMgr = buildStore.getFileMgr();
		
		for (int i = 0; i < pathIds.length; i++) {
			String path = fileMgr.getPathName(pathIds[i]);
			if (path != null) {
				sb.append(path);
				sb.append('\n');
			}
		}
	}
	
	/*-------------------------------------------------------------------------------------*/
}
