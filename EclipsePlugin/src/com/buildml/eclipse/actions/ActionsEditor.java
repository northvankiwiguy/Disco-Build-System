/*******************************************************************************
 * Copyright (c) 2011 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.progress.IProgressService;

import com.buildml.eclipse.EditorOptions;
import com.buildml.eclipse.ImportSubEditor;
import com.buildml.eclipse.bobj.UIAction;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.VisibilityTreeViewer;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IPackageMemberMgr;
import com.buildml.model.IPackageMemberMgrListener;
import com.buildml.model.types.PackageSet;
import com.buildml.model.types.ActionSet;
import com.buildml.utils.types.IntegerTreeSet;

/**
 * Implements an sub-editor for browsing a BuildStore's actions.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class ActionsEditor extends ImportSubEditor 
			implements IPackageMemberMgrListener {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** This editor's main control is a TreeViewer, for displaying the list of files */
	VisibilityTreeViewer actionsTreeViewer = null;
	
	/** The column that displays the action tree */
	private TreeViewerColumn treeColumn;
	
	/** The column that displays the package name */
	private TreeViewerColumn pkgColumn;
	
	/** The Action manager object that contains all the file information for this BuildStore */
	private IActionMgr actionMgr = null;
	
	/** The ArrayContentProvider object providing this editor's content */
	private ActionsEditorContentProvider contentProvider;
	
	/**
	 * The set of actions (within the ActionMgr) that are currently visible. 
	 */
	private ActionSet visibleActions = null;

	/**
	 * The object that provides visible/non-visible information about each
	 * element in the action tree.
	 */
	private ActionsEditorVisibilityProvider visibilityProvider;

	/**
	 * The previous set of option bits. The refreshView() method uses this value to
	 * determine which aspects of the TreeViewer must be redrawn.
	 */
	private int previousEditorOptionBits = 0;

	/**
	 * The TreeViewer's parent control.
	 */
	private Composite filesEditorComposite;

	/**
	 * The current tree item that has its text expanded. When a new tree item
	 * is selected, this item's text will be contracted again.
	 */
	UIAction previousSelection = null;

	/*=====================================================================================*
	 * CONSTRUCTOR
	 *=====================================================================================*/

	/**
	 * Create a new ActionsEditor instance, using the specified BuildStore as input
	 * @param buildStore The BuildStore to display/edit.
	 * @param tabTitle The text to appear on the editor's tab.
	 */
	public ActionsEditor(IBuildStore buildStore, String tabTitle) {
		super(buildStore, tabTitle);
		
		actionMgr = buildStore.getActionMgr();
		
		/* listen to changes in package content (for all packages) */
		IPackageMemberMgr pkgMemberMgr = buildStore.getPackageMemberMgr();
		pkgMemberMgr.addListener(this);
		
		/* initially, all paths are visible */
		visibleActions = buildStore.getReportMgr().reportAllActions();
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {

		/* we can only handle files as input */
		if (! (input instanceof IURIEditorInput)) {
			throw new PartInitException("Invalid Input: Must be IURIEditorInput");
		}
		
		/* save our site and input data */
		setSite(site);
		setInput(input);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(final Composite parent) {
		
		/* initiate functionality that's common to all editors */
		super.createPartControl(parent);
		
		/* enable the "actionseditor" context, used for keyboard acceleration */
		IContextService contextService = 
			(IContextService) getSite().getService(IContextService.class);
		contextService.activateContext("com.buildml.eclipse.contexts.actionseditor");
		
		/* create the main Tree control that the user will view/manipulate */
		Tree actionEditorTree = new Tree(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL |
															SWT.MULTI | SWT.FULL_SELECTION);
		actionEditorTree.setHeaderVisible(true);
	    actionEditorTree.setLinesVisible(true);
	    
		/*
		 * The main control in this editor is a TreeViewer that allows the user to
		 * browse the structure of the BuildStore's actions. It has two columns:
		 *    1) The file system path (shown as a tree).
		 *    2) The path's package (shown as a fixed-width column);
		 */
		actionsTreeViewer = new VisibilityTreeViewer(actionEditorTree);
		treeColumn = new TreeViewerColumn(actionsTreeViewer, SWT.LEFT);
	    treeColumn.getColumn().setAlignment(SWT.LEFT);
	    treeColumn.getColumn().setText("Action Command");
	    pkgColumn = new TreeViewerColumn(actionsTreeViewer, SWT.RIGHT);
	    pkgColumn.getColumn().setAlignment(SWT.LEFT);
	    pkgColumn.getColumn().setText("Package");
	    filesEditorComposite = parent;
	    
	    /*
	     * Set the initial column widths so that the path column covers the full editor
	     * window, and the package column is empty. Setting the path column
	     * to a non-zero pixel width causes it to be expanded to the editor's full width. 
	     */
	    treeColumn.getColumn().setWidth(1);
	    pkgColumn.getColumn().setWidth(0);
		
	    /*
		 * Add the tree/table content and label providers.
		 */
		contentProvider = new ActionsEditorContentProvider(this, actionMgr);
		ActionsEditorLabelCol1Provider labelProviderCol1 = 
				new ActionsEditorLabelCol1Provider(this, actionMgr);
		ActionsEditorLabelCol2Provider labelProviderCol2 = 
				new ActionsEditorLabelCol2Provider(this, buildStore);
		actionsTreeViewer.setContentProvider(contentProvider);
		treeColumn.setLabelProvider(labelProviderCol1);
		pkgColumn.setLabelProvider(labelProviderCol2);
	    ColumnViewerToolTipSupport.enableFor(actionsTreeViewer);
		
		/*
		 * Set up a visibility provider so we know which paths should be visible (at
		 * least to start with).
		 */
		visibilityProvider = new ActionsEditorVisibilityProvider(visibleActions);
		visibilityProvider.setSecondaryFilterSet(null);
		actionsTreeViewer.setVisibilityProvider(visibilityProvider);

		/*
		 * Record the initial set of option bits so that we can later determine
		 * which bits have been modified (this is used in refreshView()).
		 */
		previousEditorOptionBits = getOptions();
		
		/* double-clicking on an expandable node will expand/contract that node */
		actionsTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				UIAction node = (UIAction)selection.getFirstElement();
				if (actionsTreeViewer.isExpandable(node)){
					actionsTreeViewer.setExpandedState(node, 
							!actionsTreeViewer.getExpandedState(node));
				}
			}
		});
		
		/* create the context menu */
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				manager.add(new Separator("buildmlactions"));
				manager.add(new Separator("additions"));
			}
		});
		Menu menu = menuMgr.createContextMenu(actionsTreeViewer.getControl());
		actionsTreeViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, actionsTreeViewer);
		getSite().setSelectionProvider(actionsTreeViewer);
		
		/* 
		 * When the tree viewer needs to compare its elements, this class
		 * (ActionsEditor) provides the equals() and hashcode() methods.
		 */
		actionsTreeViewer.setComparer(this);

		/* start by displaying from the root (which changes, depending on our options). */
		actionsTreeViewer.setInput(contentProvider.getRootElements());

		/* based on the size of the set to be displayed, auto-size the tree output */
		int outputSize = getVisibilityFilterSet().size();
		if (outputSize < AUTO_EXPAND_THRESHOLD) {
			actionsTreeViewer.expandAll();
		} else {
			actionsTreeViewer.expandToLevel(2);
		}
		/* 
		 * Now that we've created all the widgets, force options to take effect. Note
		 * that these setters have side effects that wouldn't have taken effect if
		 * there were no widgets.
		 */
		setOptions(getOptions());
		setFilterPackageSet(getFilterPackageSet());
		setVisibilityFilterSet(getVisibilityFilterSet());
		
		/*
		 * Add a "drag source" handler so that we can copy/move actions around.
		 */
		new ActionsEditorDragSource(actionsTreeViewer, buildStore);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		
		/* if we focus on this editor, we actually focus on the TreeViewer control */
		if (actionsTreeViewer != null){
			actionsTreeViewer.getControl().setFocus();
		}		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Our parent (multi-part editor) has just switched to our tab. We should update the UI
	 * state in response.
	 */
	public void pageChange()
	{
		ICommandService service =
			(ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
		
		/*
		 * Make sure that each of these toolbar buttons (and menu items) are updated
		 * appropriately to match the settings of *this* editor, instead of the previous
		 * editor.
		 */
		service.refreshElements("com.buildml.eclipse.commands.showPackages", null);
		service.refreshElements("com.buildml.eclipse.commands.showHiddenPaths", null);
		service.refreshElements("com.buildml.eclipse.commands.showPathRoots", null);
		
		/* if model has changed recently - update it */
		refreshViewIfOutDated();
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Set the visibility state of the specified action. A visible action is rendered as usual,
	 * but a non-visible action will either be greyed out, or not rendered at all (depending
	 * on the current setting of the grey-visibility mode). Making a previously visible action
	 * invisible will also make all child actions invisible. Making a previously invisible
	 * action visible will ensure that all parent actions are also made visible.
	 * 
	 * @param item The action to be hidden or revealed.
	 * @param state True if the action should be made visible, else false.
	 */
	public void setItemVisibilityState(Object item, boolean state) {
		actionsTreeViewer.setVisibility(item, state);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Set the complete set of actions that this editor's tree viewer will show. After
	 * calling this method, it will be necessary to also call refreshView() to actually
	 * update the view.
	 * @param visibleActions The subset of actions that should be visible in the editor.
	 */
	@Override
	public void setVisibilityFilterSet(IntegerTreeSet visibleActions) {
		this.visibleActions = (ActionSet) visibleActions;
		if (visibilityProvider != null) {
			visibilityProvider.setPrimaryFilterSet(this.visibleActions);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The set of actions that are currently visible in this editor's tree viewer.
	 */
	@Override
	public IntegerTreeSet getVisibilityFilterSet() {
		return visibilityProvider.getPrimaryFilterSet();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given an item in the editor, expand all the descendants of that item so
	 * that they're visible in the tree viewer.
	 * @param node The tree node representing the item in the tree to be expanded.
	 */
	public void expandSubtree(Object node) {
		actionsTreeViewer.expandToLevel(node, AbstractTreeViewer.ALL_LEVELS);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set this editor's package filter set. This set is used by the viewer when 
	 * deciding which files should be displayed (versus being filtered out).
	 * @param newSet This editor's new package filter set.
	 */
	public void setFilterPackageSet(PackageSet newSet) {
		super.setFilterPackageSet(newSet);
		
		/* if the editor is in an initialized state, we can refresh the filters */
		if (visibilityProvider != null) {
			ActionSet pkgActionSet = 
					buildStore.getReportMgr().reportActionsFromPackageSet(newSet);
			pkgActionSet.populateWithParents();
		
			visibilityProvider.setSecondaryFilterSet(pkgActionSet);
			refreshView(true);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Refresh the editor's content. This is typically called when some type of display
	 * option changes (e.g. packages have been added), and the content is now
	 * different, or if the user resizes the main Eclipse shell. We use a progress monitor,
	 * since a redraw operation might take a while.
	 * @param forceRedraw true if we want to force a complete redraw of the viewer.
	 */
	public void refreshView(boolean forceRedraw) {
		
		/* compute the set of option bits that have changed since we were last called */
		int currentOptions = getOptions();
		int changedOptions = previousEditorOptionBits ^ currentOptions;
		previousEditorOptionBits = currentOptions;
		
		/*
		 * Determine whether the packages columns should be shown. Setting the
		 * width appropriately is important, especially if the shell was recently resized.
		 * TODO: figure out why subtracting 20 pixels is important for matching the column
		 * size with the size of the parent composite.
		 */
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
			    int editorWidth = filesEditorComposite.getClientArea().width - 20;
			    int pkgWidth = isOptionSet(EditorOptions.OPT_SHOW_PACKAGES) ? 100 : 0;
			  treeColumn.getColumn().setWidth(editorWidth - 2 * pkgWidth);
			  pkgColumn.getColumn().setWidth(pkgWidth);
			}
		});

		/*
		 * Has the content of the tree changed, or just the visibility of columns? If
		 * it's just the columns, then we don't need to re-query the model in order to redisplay.
		 * Unless our caller explicitly requested a redraw.
		 */
		if (!forceRedraw) {
			return;
		}
		
		/*
		 * We need to re-query the model and redisplay some (or all) of the tree items.
		 * Create a new job that will be run in the background, and monitored by the
		 * progress monitor. Note that only the portions of this job that update the
		 * UI should be run as the UI thread. Otherwise the job appears to block the
		 * whole UI.
		 */
		IRunnableWithProgress redrawJob = new IRunnableWithProgress() {
			
			@Override
			public void run(IProgressMonitor monitor) {
				monitor.beginTask("Redrawing editor content...", 2);
				monitor.worked(1);
				Display.getDefault().syncExec(new Runnable() {
					@Override
					public void run() {
						Object[] expandedElements = actionsTreeViewer.getExpandedElements();
						actionsTreeViewer.setInput(contentProvider.getRootElements());
						actionsTreeViewer.refresh();
						
						/* 
						 * Ensure that all previously-expanded items are now expanded again.
						 * Note: we can't use setExpandedElements(), as that won't always
						 * open all the parent elements as well.
						 */
						for (int i = 0; i < expandedElements.length; i++) {
							actionsTreeViewer.expandToLevel(expandedElements[i], 1);							
						}
					}
				});
				monitor.worked(1);
				monitor.done();
			}
		};
		
		/* start up the progress monitor service so that it monitors the job */
		IProgressService service = PlatformUI.getWorkbench().getProgressService();
		try {
			service.busyCursorWhile(redrawJob);
		} catch (InvocationTargetException e) {
			// TODO: what to do here?
		} catch (InterruptedException e) {
			// TODO: what to do here?
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.SubEditor#hasFeature(java.lang.String)
	 */
	@Override
	public boolean hasFeature(String feature) {
		if (feature.equals("removable")) {
			return isRemovable();
		} else if (feature.equals("actions")) {
			return true;
		}
		return false;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.SubEditor#getEditorImagePath()
	 */
	@Override
	public String getEditorImagePath() {
		return "images/action_icon.gif";
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.SubEditor#doCopyCommand(org.eclipse.core.commands.ExecutionEvent)
	 */
	public void doCopyCommand(Clipboard clipboard, ISelection selection) {
		
		/* Determine the set of actions that are currently selected */
		if (selection instanceof TreeSelection) {
			ActionSet actionSet = EclipsePartUtils.
					getActionSetFromSelection(buildStore, (TreeSelection)selection);

			/* 
			 * All commands will be concatenated together into a single string (with an appropriate
			 * EOL character).
			 */
			String eol = System.getProperty("line.separator");
			StringBuffer sb = new StringBuffer();
			for (int actionId : actionSet) {
				String cmd = actionMgr.getCommand(actionId);
				sb.append(cmd);
				sb.append(eol);
			}
			
			/*
			 * Copy the string to the clipboard(s). There's a separate clipboard for
			 * Eclipse versus Linux, so copy to both of them.
			 */
			try {
				clipboard.setContents(
						new Object[] { sb.toString(), },
						new Transfer[] { TextTransfer.getInstance(), }, 
						DND.CLIPBOARD | DND.SELECTION_CLIPBOARD);
				
			} catch (SWTError error) {
				AlertDialog.displayErrorDialog("Unable to copy",
						"The selected information could not be copied to the clipboard.");
			}			
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Called by pkgMgr when package membership changes. If we're currently showing packages,
	 * we'll need to refresh (after all, we're showing all actions and their packages, so
	 * there's a good chance our content changed).
	 */
	@Override
	public void packageMemberChangeNotification(int pkgId, int how, int memberType, int memberId) {

		if (how == IPackageMemberMgrListener.CHANGED_MEMBERSHIP) {

			/*
			 * Schedule the editor content (TreeViewer) to be refreshed with the new
			 * content. Note that we deliberately schedule this to happen later,
			 * since our current thread is quite possibly doing a number of updates
			 * that will result in multiple notifications. We don't want to refresh
			 * for each individual update.
			 */
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					refreshView(true);
				}
			});
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Invoked whenever one of this editor's options are changed. We may need to react
	 * to the option change in some way.
	 * @param optionBits The option(s) that were modified.
	 * @param enable True if the options were added, else false.
	 */
	protected void updateEditorWithNewOptions(int optionBits, boolean enable) {
		/* pass some of the options onto onto parts of the system */
		if ((actionsTreeViewer != null) && (optionBits & EditorOptions.OPT_SHOW_HIDDEN) != 0) {
			actionsTreeViewer.setGreyVisibilityMode(enable);
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
