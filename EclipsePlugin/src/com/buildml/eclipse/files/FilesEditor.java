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

package com.buildml.eclipse.files;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
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
import org.eclipse.swt.widgets.TreeColumn;
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
import com.buildml.eclipse.bobj.UIInteger;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.eclipse.utils.EclipsePartUtils;
import com.buildml.eclipse.utils.VisibilityTreeViewer;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.IPackageRootMgr;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.PackageSet;
import com.buildml.utils.types.IntegerTreeSet;

/**
 * A BuildML editor that displays the set of files within a BuildStore.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public class FilesEditor extends ImportSubEditor {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** This editor's main control is a TreeViewer, for displaying the list of files */
	private VisibilityTreeViewer filesTreeViewer = null;
	
	/** The column that displays the path tree */
	private TreeColumn treeColumn;
	
	/** The column that displays the package name */
	private TreeColumn pkgColumn;
	
	/** The column that displays the path's scope */
	private TreeColumn scopeColumn;
	
	/** The FileMgr object that contains all the file information for this BuildStore */
	private IFileMgr fileMgr = null;
	
	/** The PackageRootMgr object that contains the package root information */
	private IPackageRootMgr pkgRootMgr = null;
	
	/** The ArrayContentProvider object providing this editor's content */
	private FilesEditorContentProvider contentProvider;

	/** The set of paths (within the FileMgr) that are currently visible. */
	private FileSet visiblePaths = null;

	/**
	 * The object that provides visible/non-visible information about each
	 * element in the file tree.
	 */
	private FilesEditorVisibilityProvider visibilityProvider;

	/**
	 * The previous set of option bits. The refreshView() method uses this value to
	 * determine which aspects of the TreeViewer must be redrawn.
	 */
	private int previousEditorOptionBits = 0;

	/** The TreeViewer's parent control. */
	private Composite filesEditorComposite;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new FilesEditor instance, using the specified BuildStore as input
	 * @param buildStore The BuildStore to display/edit.
	 * @param tabTitle The text to appear on the editor's tab.
	 */
	public FilesEditor(IBuildStore buildStore, String tabTitle) {
		super(buildStore, tabTitle);

		fileMgr = buildStore.getFileMgr();
		pkgRootMgr = buildStore.getPackageRootMgr();

		/* initially, all paths are visible */
		visiblePaths = buildStore.getReportMgr().reportAllFiles();
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
		
		/* enable the "fileseditor" context, used for keyboard acceleration */
		IContextService contextService = 
			(IContextService) getSite().getService(IContextService.class);
		contextService.activateContext("com.buildml.eclipse.contexts.fileseditor");	
		
		/* create the main Tree control that the user will view/manipulate */
		Tree fileEditorTree = new Tree(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL |
															SWT.MULTI | SWT.FULL_SELECTION);
		fileEditorTree.setHeaderVisible(true);
	    fileEditorTree.setLinesVisible(true);
		
		/*
		 * The main control in this editor is a TreeViewer that allows the user to
		 * browse the structure of the BuildStore's file system. It has three columns:
		 *    1) The file system path (shown as a tree).
		 *    2) The path's package (shown as a fixed-width column);
		 *    3) The path's scope (private, public etc).
		 */
		filesTreeViewer = new VisibilityTreeViewer(fileEditorTree);
		treeColumn = new TreeColumn(fileEditorTree, SWT.LEFT);
	    treeColumn.setAlignment(SWT.LEFT);
	    treeColumn.setText("Path");
	    pkgColumn = new TreeColumn(fileEditorTree, SWT.RIGHT);
	    pkgColumn.setAlignment(SWT.LEFT);
	    pkgColumn.setText("Package");
	    scopeColumn = new TreeColumn(fileEditorTree, SWT.RIGHT);
	    scopeColumn.setAlignment(SWT.LEFT);
	    scopeColumn.setText("Scope");
	    filesEditorComposite = parent;
	    
	    /*
	     * Set the initial column widths so that the path column covers the full editor
	     * window, and the package/scope columns are empty. Setting the path column
	     * to a non-zero pixel width causes it to be expanded to the editor's full width. 
	     */
	    treeColumn.setWidth(1);
	    pkgColumn.setWidth(0);
	    scopeColumn.setWidth(0);
		
	    /*
		 * Add the tree/table content and label providers.
		 */
		contentProvider = new FilesEditorContentProvider(this, fileMgr, pkgRootMgr);
		FilesEditorLabelProvider labelProvider = 
				new FilesEditorLabelProvider(this, fileMgr, buildStore.getPackageMgr());
		FilesEditorViewerSorter viewerSorter = new FilesEditorViewerSorter(this, fileMgr);
		filesTreeViewer.setContentProvider(contentProvider);
		filesTreeViewer.setLabelProvider(labelProvider);
		filesTreeViewer.setSorter(viewerSorter);
		
		/*
		 * Set up a visibility provider so we know which paths should be visible (at
		 * least to start with).
		 */
		visibilityProvider = new FilesEditorVisibilityProvider(visiblePaths);
		visibilityProvider.setSecondaryFilterSet(null);
		filesTreeViewer.setVisibilityProvider(visibilityProvider);
		
		/*
		 * Record the initial set of option bits so that we can later determine
		 * which bits have been modified (this is used in refreshView()).
		 */
		previousEditorOptionBits = getOptions();
		
		/* 
		 * double-clicking on an expandable node will expand/contract that node, whereas
		 * double-clicking on a file will open it in an editor.
		 */
		filesTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				UIInteger node = (UIInteger)selection.getFirstElement();
			
				if (filesTreeViewer.isExpandable(node)){
					filesTreeViewer.setExpandedState(node, 
							!filesTreeViewer.getExpandedState(node));
				}
				
				/* 
				 * else, try to open file in an appropriate editor.
				 * A dialog box will be displayed (by openNewEditor)
				 * if there's a problem.
				 */
				else {
					String filePath = fileMgr.getPathName(node.getId());
					EclipsePartUtils.openNewEditor(filePath);
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
		Menu menu = menuMgr.createContextMenu(filesTreeViewer.getControl());
		filesTreeViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, filesTreeViewer);
		getSite().setSelectionProvider(filesTreeViewer);
		
		/* 
		 * When the tree viewer needs to compare its elements, this class
		 * (FilesEditor) provides the equals() and hashcode() methods.
		 */
		filesTreeViewer.setComparer(this);

		/* start by displaying from the root (which changes, depending on our options). */
		filesTreeViewer.setInput(contentProvider.getRootElements());

		/* based on the size of the set to be displayed, auto-size the tree output */
		int outputSize = getVisibilityFilterSet().size();
		if (outputSize < AUTO_EXPAND_THRESHOLD) {
			filesTreeViewer.expandAll();
		} else {
			filesTreeViewer.expandToLevel(2);
		}
		/* 
		 * Now that we've created all the widgets, force options to take effect. Note
		 * that these setters have side effects that wouldn't have taken effect if
		 * there were no widgets.
		 */
		setOptions(getOptions());
		setFilterPackageSet(getFilterPackageSet());
		setVisibilityFilterSet(getVisibilityFilterSet());
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		
		/* if we focus on this editor, we actually focus on the TreeViewer control */
		if (filesTreeViewer != null){
			filesTreeViewer.getControl().setFocus();
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
	 * Set the visibility state of the specified path. A visible path is rendered as per usual,
	 * but a non-visible path will either be greyed out, or not rendered at all (depending
	 * on the current setting of the grey-visibility mode). Making a previously visible path
	 * invisible will also make all child paths invisible. Making a previously invisible
	 * path visible will ensure that all parent paths are also made visible.
	 * 
	 * @param item The path to be hidden or revealed.
	 * @param state True if the path should be made visible, else false.
	 */
	public void setItemVisibilityState(Object item, boolean state) {
		filesTreeViewer.setVisibility(item, state);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Set the complete set of paths that this editor's tree viewer will show. After
	 * calling this method, it will be necessary to also call refreshView() to actually
	 * update the view.
	 * @param visiblePaths The subset of paths that should be visible in the editor.
	 */
    @Override
	public void setVisibilityFilterSet(IntegerTreeSet visiblePaths) {
		this.visiblePaths = (FileSet) visiblePaths;
		if (visibilityProvider != null) {
			visibilityProvider.setPrimaryFilterSet(this.visiblePaths);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The set of files that are currently visible in this editor's tree viewer.
	 */
	@Override
	public IntegerTreeSet getVisibilityFilterSet() {
		return visibilityProvider.getPrimaryFilterSet();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set this editor's package filter set. This set is used by the viewer when 
	 * deciding which files should be displayed (versus being filtered out).
	 * @param newSet This editor's new package filter set.
	 */
	@Override
	public void setFilterPackageSet(PackageSet newSet) {
		super.setFilterPackageSet(newSet);
		
		/* if the editor is in an initialized state, we can fresh the filters */
		if (visibilityProvider != null) {
			FileSet pkgFileSet = 
				buildStore.getReportMgr().reportFilesFromPackageSet(newSet);
			pkgFileSet.populateWithParents();
		
			visibilityProvider.setSecondaryFilterSet(pkgFileSet);
			refreshView(true);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Refresh the editor's content. This is typically called when some type of display
	 * option changes (e.g. roots or packages have been added), and the content is now
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
		 * Determine whether the packages/scope columns should be shown. Setting the
		 * width appropriately is important, especially if the shell was recently resized.
		 * TODO: figure out why subtracting 20 pixels is important for matching the column
		 * size with the size of the parent composite.
		 */
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
			    int editorWidth = filesEditorComposite.getClientArea().width - 20;
			    int pkgWidth = isOptionSet(EditorOptions.OPT_SHOW_PACKAGES) ? 100 : 0;
			    treeColumn.setWidth(editorWidth - 2 * pkgWidth);
			    pkgColumn.setWidth(pkgWidth);
			    scopeColumn.setWidth(pkgWidth);		
			}
		});

		/*
		 * Has the content of the tree changed, or just the visibility of columns? If
		 * it's just the columns, then we don't need to re-query the model in order to redisplay.
		 * Unless our caller explicitly requested a redraw.
		 */
		if (!forceRedraw && ((changedOptions & (EditorOptions.OPT_COALESCE_DIRS | 
												EditorOptions.OPT_SHOW_ROOTS)) == 0)) {
			return;
		}
		
		if ((changedOptions & EditorOptions.OPT_COALESCE_DIRS) != 0) {
			filesTreeViewer.setInput(contentProvider.getRootElements());
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
						Object[] expandedElements = filesTreeViewer.getExpandedElements();
						filesTreeViewer.setInput(contentProvider.getRootElements());
						filesTreeViewer.refresh();
						
						/* 
						 * Ensure that all previously-expanded items are now expanded again.
						 * Note: we can't use setExpandedElements(), as that won't always
						 * open all the parent elements as well.
						 */
						for (int i = 0; i < expandedElements.length; i++) {
							filesTreeViewer.expandToLevel(expandedElements[i], 1);							
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
		} else if (feature.equals("paths")) {
			return true;
		} else if (feature.equals("path-roots")) {
			return true;
		} else if (feature.equals("filter-packages-by-scope")) {
			return true;
		}
		return false;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.SubEditor#getIcon()
	 */
	@Override
	public String getEditorImagePath() {
		return "images/files_icon.gif";
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.SubEditor#doCopyCommand(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public void doCopyCommand(Clipboard clipboard, ISelection selection) {

		/* Determine the set of files that are currently selected */
		if (selection instanceof TreeSelection) {
			FileSet fileSet = EclipsePartUtils.
					getFileSetFromSelection(buildStore, (TreeSelection)selection);

			/* 
			 * All paths will be concatenated together into a single string (with an appropriate
			 * EOL character).
			 */
			String eol = System.getProperty("line.separator");
			StringBuffer sb = new StringBuffer();
			for (int pathId : fileSet) {
				String path = fileMgr.getPathName(pathId);
				sb.append(path);
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

	/*=====================================================================================*
	 * PRIVATE/PROTECTED METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.SubEditor#expandSubtree(java.lang.Object)
	 */
	@Override
	public void expandSubtree(Object node) {
		filesTreeViewer.expandToLevel(node, AbstractTreeViewer.ALL_LEVELS);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see com.buildml.eclipse.SubEditor#updateEditorWithNewOptions(int, boolean)
	 */
	@Override
	protected void updateEditorWithNewOptions(int optionBits, boolean enable) {
		/* pass some of the options onto onto parts of the system */
		if ((filesTreeViewer != null) && (optionBits & EditorOptions.OPT_SHOW_HIDDEN) != 0) {
			filesTreeViewer.setGreyVisibilityMode(enable);
		}
	}

	/*-------------------------------------------------------------------------------------*/
}
