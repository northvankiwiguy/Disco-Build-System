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

package com.arapiki.disco.eclipse.files;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.internal.progress.ProgressManagerUtil;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.progress.IProgressService;

import com.arapiki.disco.eclipse.Activator;
import com.arapiki.disco.eclipse.preferences.PreferenceConstants;
import com.arapiki.disco.eclipse.utils.AlertDialog;
import com.arapiki.disco.eclipse.utils.VisibilityTreeViewer;
import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.types.ComponentSet;
import com.arapiki.disco.model.types.FileRecord;
import com.arapiki.disco.model.types.FileSet;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class DiscoFilesEditor extends EditorPart implements IElementComparer {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** This editor's main control is a TreeViewer, for displaying the list of files */
	private VisibilityTreeViewer filesTreeViewer = null;
	
	/** The column that displays the path tree */
	private TreeColumn treeColumn;
	
	/** The column that displays the component name */
	private TreeColumn compColumn;
	
	/** The column that displays the path's scope */
	private TreeColumn scopeColumn;
	
	/** The BuildStore we're editing */
	private BuildStore buildStore = null;
	
	/** The FileNameSpaces object that contains all the file information for this BuildStore */
	private FileNameSpaces fns = null;
	
	/** The ArrayContentProvider object providing this editor's content */
	private FilesEditorContentProvider contentProvider;
	
	/**
	 * The current options setting for this editor. The field contains a bitmap of
	 * OPT_* values.
	 */
	private int editorOptionBits = 0;
	
	/**
	 * The set of paths (within the FileNameSpaces) that are currently visible. 
	 */
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

	/**
	 * The set of components to be displayed (that is, files will be displayed
	 * if they belong to one of these components).
	 */
	private ComponentSet filterComponentSet;
	
	/**
	 * The TreeViewer's parent control.
	 */
	private Composite filesEditorComposite;
	
	/** 
	 * Option to enable coalescing of folders in the file editor. That is, if a folder
	 * contains a single child which is itself a folder, we display both of them on the
	 * same line. For example, if folder "A" has a single child, "B", we display "A/B".
	 */
	public static final int OPT_COALESCE_DIRS		= 1;
	
	/**
	 * Option to display file roots as the top-level items in the editor. Without this
	 * feature enabled, the top-level directory names will be shown.
	 */
	public static final int OPT_SHOW_ROOTS			= 2;
	
	/**
	 * Option to display each path's component name, alongside the path name itself.
	 */
	public static final int OPT_SHOW_COMPONENTS		= 4;
	
	/**
	 * Option to reveal hidden files in the viewer, displaying them in a greyed-out style.
	 */
	public static final int OPT_SHOW_HIDDEN			= 8;
	
	/**
	 * How many options currently exist?
	 */
	private static final int NUM_OPTIONS			= 4;
	
	/**
	 * If a new editor tab is created with fewer than this many visible tree entries,
	 * we should auto-expand the entire tree so that all elements are visible. If there
	 * are more than this many elements, only expand the first couple of levels.
	 */
	private static final int AUTO_EXPAND_THRESHOLD 	= 200;

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new DiscoFilesEditor instance, using the specified BuildStore as input
	 * @param buildStore The BuildStore to display/edit.
	 * @param tabTitle The text to appear on the editor's tab.
	 */
	public DiscoFilesEditor(BuildStore buildStore, String tabTitle) {
		super();
		
		/* set the name of the tab that this editor appears in */
		setPartName(tabTitle);
		
		/* Save away our BuildStore information, for later use */
		this.buildStore = buildStore;
		fns = buildStore.getFileNameSpaces();
		
		/* create a new component set so we can selectively filter out components */
		filterComponentSet = new ComponentSet(buildStore);
		filterComponentSet.setDefault(true);
		
		/* initially, all paths are visible */
		visiblePaths = buildStore.getReports().reportAllFiles();
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub
	}
	
	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
		/* not implemented */
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {

		/* we can only handle files as input */
		if (! (input instanceof IFileEditorInput)) {
			throw new PartInitException("Invalid Input: Must be IFileEditorInput");
		}
		
		/* save our site and input data */
		setSite(site);
		setInput(input);
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#isDirty()
	 */
	@Override
	public boolean isDirty() {
		/* not implemented for now, while this editor is for read-only purposes */
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	@Override
	public boolean isSaveAsAllowed() {
		/* save-as is not permitted */
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(final Composite parent) {
		
		/* create the main Tree control that the user will view/manipulate */
		Tree fileEditorTree = new Tree(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL |
															SWT.MULTI | SWT.FULL_SELECTION);
		fileEditorTree.setHeaderVisible(true);
	    fileEditorTree.setLinesVisible(true);
		
		/*
		 * The main control in this editor is a TreeViewer that allows the user to
		 * browse the structure of the BuildStore's file system. It has three columns:
		 *    1) The file system path (shown as a tree).
		 *    2) The path's component (shown as a fixed-width column);
		 *    3) The path's scope (private, public etc).
		 */
		filesTreeViewer = new VisibilityTreeViewer(fileEditorTree);
		treeColumn = new TreeColumn(fileEditorTree, SWT.LEFT);
	    treeColumn.setAlignment(SWT.LEFT);
	    treeColumn.setText("Path");
	    compColumn = new TreeColumn(fileEditorTree, SWT.RIGHT);
	    compColumn.setAlignment(SWT.LEFT);
	    compColumn.setText("Component");
	    scopeColumn = new TreeColumn(fileEditorTree, SWT.RIGHT);
	    scopeColumn.setAlignment(SWT.LEFT);
	    scopeColumn.setText("Scope");
	    filesEditorComposite = parent;
	    
	    /*
	     * Set the initial column widths so that the path column covers the full editor
	     * window, and the component/section columns are empty. Setting the path column
	     * to a non-zero pixel width causes it to be expanded to the editor's full width. 
	     */
	    treeColumn.setWidth(1);
	    compColumn.setWidth(0);
	    scopeColumn.setWidth(0);
		
	    /*
		 * Add the tree/table content and label providers.
		 */
		contentProvider = new FilesEditorContentProvider(this, fns);
		FilesEditorLabelProvider labelProvider = 
				new FilesEditorLabelProvider(this, fns, buildStore.getComponents());
		FilesEditorViewerSorter viewerSorter = new FilesEditorViewerSorter(this, fns);
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
		 * Update this editor's option by reading the user-specified values in the
		 * preference store. Also, attach a listener so that we hear about future
		 * changes to the preference store and adjust our options accordingly.
		 */
		updateOptionsFromPreferenceStore();
		Activator.getDefault().getPreferenceStore().
					addPropertyChangeListener(preferenceStoreChangeListener);
		
		/*
		 * Record the initial set of option bits so that we can later determine
		 * which bits have been modified (this is used in refreshView()).
		 */
		previousEditorOptionBits = getOptions();
		
		/* double-clicking on an expandable node will expand/contract that node */
		filesTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				FileRecord node = (FileRecord)selection.getFirstElement();
				if (filesTreeViewer.isExpandable(node)){
					filesTreeViewer.setExpandedState(node, 
							!filesTreeViewer.getExpandedState(node));
				}
			}
		});
		
		/* 
		 * Resizing the top-level shell causes columns to be realigned/redrawn. We need
		 * to schedule this as a UI thread runnable, since we don't want it to run until
		 * after the resizing has finished, at which point we know the new window size.
		 * TODO: add a removeListener.
		 */
		parent.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				refreshView();
			}
		});
		
		/* create the context menu */
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				manager.add(new Separator("discoactions"));
				manager.add(new Separator("additions"));
			}
		});
		Menu menu = menuMgr.createContextMenu(filesTreeViewer.getControl());
		filesTreeViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, filesTreeViewer);
		getSite().setSelectionProvider(filesTreeViewer);
		
		/* 
		 * When the tree viewer needs to compare its elements, this class
		 * (DiscoFilesEditor) provides the equals() and hashcode() methods.
		 */
		filesTreeViewer.setComparer(this);

		/* start by displaying from the root (which changes, depending on our options). */
		filesTreeViewer.setInput(contentProvider.getRootElements());
		
		/* enable the "discoeditor" context, used for keyboard acceleration */
		IContextService contextService = 
			(IContextService) getSite().getService(IContextService.class);
		contextService.activateContext("com.arapiki.disco.eclipse.contexts.discoeditor");
	
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
		setFilterComponentSet(getFilterComponentSet());
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
		service.refreshElements("com.arapiki.disco.eclipse.commands.showComponents", null);
		service.refreshElements("com.arapiki.disco.eclipse.commands.showHiddenPaths", null);
		service.refreshElements("com.arapiki.disco.eclipse.commands.showPathRoots", null);
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Either set of clear specific options (e.g. OPT_COALESCE_DIR or OPT_SHOW_ROOTS) from
	 * this editor's current option settings. This can be used to modify one or more
	 * binary configuration settings in this control.
	 * 
	 * @param optionBits One or more bits that should be either set or cleared from this
	 * 		  editor's options. The state of options that are not specified in this parameter
	 *        will not be changed.
	 * @param enable "true" if the options should be enabled, or "false" if they should be cleared.
	 */
	public void setOption(int optionBits, boolean enable)
	{
		/* if enable is set, then we're adding the new options */
		if (enable) {
			editorOptionBits |= optionBits;
		}
		
		/* else, we're clearing the options */
		else {
			editorOptionBits &= ~optionBits;
		}
		
		/* pass some of the options onto onto parts of the system */
		if ((filesTreeViewer != null) && (optionBits & OPT_SHOW_HIDDEN) != 0) {
			filesTreeViewer.setGreyVisibilityMode(enable);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set the editor options (e.g. OPT_COALESCE_ROOTS) for this DiscoFilesEditor.
	 * @param optionBits The option bits setting (a 1-bit enables a feature, whereas a 0-bit
	 * 		disables that feature).
	 */
	public void setOptions(int optionBits)
	{
		/* we call setOptions for each option, to ensure that side-effects are triggered */ 
		for (int bitNum = 0; bitNum != NUM_OPTIONS; bitNum++) {
			int thisBitMap = (1 << bitNum);
			
			/* explicitly enable or disable this option */
			setOption(thisBitMap, (optionBits & thisBitMap) != 0);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The bitmap of all editor options that are currently set 
	 * (e.g. OPT_COALESCE_ROOTS)
	 */
	public int getOptions()
	{
		return editorOptionBits;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @param optionBit The option to test for.
	 * @return Whether or the specified editor option is set.
	 */
	public boolean isOptionSet(int optionBit)
	{
		return (editorOptionBits & optionBit) != 0;
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Set this editor's options by reading the current values from the preference store.
	 * This should be called when the editor is first created, as well as whenever the
	 * preference store is updated.
	 */
	public void updateOptionsFromPreferenceStore()
	{
		IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();
		
		setOption(OPT_COALESCE_DIRS, 
				prefStore.getBoolean(PreferenceConstants.PREF_COALESCE_DIRS));
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Given a directory node in the editor, expand all the descendants of that directory so
	 * that they're visible in the tree viewer.
	 * @param node The FileRecordDir node representing the directory in the tree to be expanded.
	 */
	public void expandSubtree(FileRecordDir node) {
		filesTreeViewer.expandToLevel(node, AbstractTreeViewer.ALL_LEVELS);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Set the visibility state of the specified path. A visible path is rendered as per usual,
	 * but a non-visible path will either be greyed out, or not rendered at all (depending
	 * on the current setting of the grey-visibility mode). Making a previously visible path
	 * invisible will also make all child paths invisible. Making a previously invisible
	 * path visible will ensure that all parent paths are also made visible.
	 * 
	 * @param path The path to be hidden or revealed.
	 * @param state True if the path should be made visible, else false.
	 */
	public void setPathVisibilityState(FileRecord path, boolean state) {
		filesTreeViewer.setVisibility(path, state);
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Set the complete set of paths that this editor's tree viewer will show. After
	 * calling this method, it will be necessary to also call refreshView() to actually
	 * update the view.
	 * @param visiblePaths The subset of paths that should be visible in the editor.
	 */
	public void setVisibilityFilterSet(FileSet visiblePaths) {
		this.visiblePaths = visiblePaths;
		if (visibilityProvider != null) {
			visibilityProvider.setPrimaryFilterSet(visiblePaths);
		}
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The set of files that are currently visible in this editor's tree viewer.
	 */
	public FileSet getVisibilityFilterSet() {
		return visibilityProvider.getPrimaryFilterSet();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Fetch this editor's component filter set. This set is used by the viewer when 
	 * deciding which files should be displayed (versus being filtered out).
	 * @return This editor's component filter set.
	 */
	public ComponentSet getFilterComponentSet() {
		return filterComponentSet;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set this editor's component filter set. This set is used by the viewer when 
	 * deciding which files should be displayed (versus being filtered out).
	 * @param newSet This editor's new component filter set.
	 */
	public void setFilterComponentSet(ComponentSet newSet) {
		filterComponentSet = newSet;
		
		/* if the editor is already display, we can fresh the filters */
		if (visibilityProvider != null) {
			FileSet compFileSet = 
				buildStore.getReports().reportFilesFromComponentSet(newSet);
			compFileSet.populateWithParents();
		
			visibilityProvider.setSecondaryFilterSet(compFileSet);
			refreshView(true);
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Refresh the editor's content. This is typically called when some type of display
	 * option changes (e.g. roots or components have been added), and the content is now
	 * different, or if the user resizes the main Eclipse shell. We use a progress monitor,
	 * since a redraw operation might take a while.
	 */
	public void refreshView() {
		refreshView(false);
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Refresh the editor's content. This is typically called when some type of display
	 * option changes (e.g. roots or components have been added), and the content is now
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
		 * Determine whether the components/scope columns should be shown. Setting the
		 * width appropriately is important, especially if the shell was recently resized.
		 * TODO: figure out why subtracting 20 pixels is important for matching the column
		 * size with the size of the parent composite.
		 */
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
			    int editorWidth = filesEditorComposite.getClientArea().width - 20;
			    int compWidth = isOptionSet(OPT_SHOW_COMPONENTS) ? 100 : 0;
			    treeColumn.setWidth(editorWidth - 2 * compWidth);
			    compColumn.setWidth(compWidth);
			    scopeColumn.setWidth(compWidth);		
			}
		});

		/*
		 * Has the content of the tree changed, or just the visibility of columns? If
		 * it's just the columns, then we don't need to re-query the model in order to redisplay.
		 * Unless our caller explicitly requested a redraw.
		 */
		if (!forceRedraw && ((changedOptions & (OPT_COALESCE_DIRS | OPT_SHOW_ROOTS)) == 0)) {
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
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
		
		/* remove this preference store listener */
		Activator.getDefault().getPreferenceStore().
				removePropertyChangeListener(preferenceStoreChangeListener);
		super.dispose();
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IElementComparer#equals(java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean equals(Object a, Object b) {
				
		if (a == b) {
			return true;
		}

		if ((a instanceof FileRecord) && (b instanceof FileRecord)) {
			return ((FileRecord)a).getId() == ((FileRecord)b).getId();		
		}
		
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IElementComparer#hashCode(java.lang.Object)
	 */
	@Override
	public int hashCode(Object element) {

		if (!(element instanceof FileRecord)) {
			return 0;
		}		
		return ((FileRecord)element).getId();
	}

	/*=====================================================================================*
	 * PRIVATE METHODS
	 *=====================================================================================*/

	/**
	 * Listener to identify changes being made to this plug-in's preference store, typically
	 * as part of editing the Disco preferences (this could change how our editor is displayed).
	 */
	private IPropertyChangeListener preferenceStoreChangeListener =
		new IPropertyChangeListener() {

			/**
			 * Completely redraw the files editor tree, based on the new preference
			 * settings.
			 */
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				updateOptionsFromPreferenceStore();
				filesTreeViewer.setInput(contentProvider.getRootElements());
				filesTreeViewer.refresh();
			}
		};

	/*-------------------------------------------------------------------------------------*/
}
