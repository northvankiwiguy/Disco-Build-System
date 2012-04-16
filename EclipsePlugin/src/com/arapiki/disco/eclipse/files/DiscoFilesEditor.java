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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
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
import org.eclipse.ui.part.EditorPart;

import com.arapiki.disco.eclipse.Activator;
import com.arapiki.disco.eclipse.preferences.PreferenceConstants;
import com.arapiki.disco.model.BuildStore;
import com.arapiki.disco.model.FileNameSpaces;
import com.arapiki.disco.model.types.FileRecord;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class DiscoFilesEditor extends EditorPart implements IElementComparer {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** This editor's main control is a TreeViewer, for displaying the list of files */
	private TreeViewer filesTreeViewer = null;
	
	/** The column that displays the path tree */
	private TreeColumn treeColumn;
	
	/** The column that displays the component name */
	private TreeColumn compColumn;
	
	/** The column that displays the path's visibility */
	private TreeColumn visibilityColumn;
	
	/** The BuildStore we're editing */
	private BuildStore buildStore = null;
	
	/** The FileNameSpaces object that contains all the file information for this BuildStore */
	private FileNameSpaces fns = null;
	
	/** The ArrayContentProvider object providing this editor's content */
	FilesEditorContentProvider contentProvider;
	
	/**
	 * The current options setting for this editor. The field contains a bitmap of
	 * OPT_* values.
	 */
	private int editorOptionBits = 0;
	
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
	

	/*=====================================================================================*
	 * CONSTRUCTORS
	 *=====================================================================================*/

	/**
	 * Create a new DiscoFilesEditor instance, using the specified BuildStore as input
	 * @param buildStore The BuildStore to display/edit.
	 */
	public DiscoFilesEditor(BuildStore buildStore) {
		super();
		
		/* set the name of the tab that this editor appears in */
		setPartName("Build Files");
		
		/* Save away our BuildStore information, for later use */
		this.buildStore = buildStore;
		fns = buildStore.getFileNameSpaces();
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
		 *    3) The path's visibility (private, public etc).
		 */
		filesTreeViewer = new TreeViewer(fileEditorTree);
		treeColumn = new TreeColumn(fileEditorTree, SWT.LEFT);
	    treeColumn.setAlignment(SWT.LEFT);
	    treeColumn.setText("Path");
	    compColumn = new TreeColumn(fileEditorTree, SWT.RIGHT);
	    compColumn.setAlignment(SWT.LEFT);
	    compColumn.setText("Component");
	    visibilityColumn = new TreeColumn(fileEditorTree, SWT.RIGHT);
	    visibilityColumn.setAlignment(SWT.LEFT);
	    visibilityColumn.setText("Visibility");
	    filesEditorComposite = parent;
	    
	    /*
	     * Set the initial column widths so that the path column covers the full editor
	     * window, and the component/section columns are empty. Setting the path column
	     * to a non-zero pixel width causes it to be expanded to the editor's full width. 
	     */
	    treeColumn.setWidth(1);
	    compColumn.setWidth(0);
	    visibilityColumn.setWidth(0);
		
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
	 * Either set of clear specific options (e.g. OPT_COALESCE_DIR or OPT_SHOW_ROOTS) from
	 * this editor's current option settings. This can be used to modify one or more
	 * binary configuration settings in this control.
	 * 
	 * @param optionBits One of more bits that should be either set or cleared from this
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
	 * Refresh the editor's content. This is typically called when some type of display
	 * option changes (e.g. roots or components have been added), and the content is now
	 * different, or if the user resizes the main Eclipse shell. We use a progress monitor,
	 * since a redraw operation might take a while.
	 */
	public void refreshView() {
		
		/* compute the set of option bits that have changed since we were last called */
		int currentOptions = getOptions();
		int changedOptions = previousEditorOptionBits ^ currentOptions;
		previousEditorOptionBits = currentOptions;
		
		/*
		 * Determine whether the components/visibility columns should be shown. Setting the
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
			    visibilityColumn.setWidth(compWidth);				
			}
		});

		/*
		 * Has the content of the tree changed, or just the visibility of columns? If
		 * it's just the columns, then we don't need to re-query the model in order to redisplay
		 */
		if ((changedOptions & (OPT_COALESCE_DIRS | OPT_SHOW_ROOTS)) == 0) {
			return;
		}
		
		/*
		 * We need to re-query the model and redisplay some (or all) of the tree items.
		 * Create a new job that will be run in the background, and monitored by the
		 * progress monitor. Note that only the portions of this job that update the
		 * UI should be run as the UI thread. Otherwise the job appears to block the
		 * whole UI.
		 */
		Job redrawJob = new Job("Redraw") {
			@Override
			public IStatus run(IProgressMonitor monitor) {
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
				return Status.OK_STATUS;
			}
		};
		
		/* start up the progress monitor service so that it monitors the job */
		PlatformUI.getWorkbench().getProgressService().showInDialog(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), redrawJob);
		redrawJob.schedule();
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
