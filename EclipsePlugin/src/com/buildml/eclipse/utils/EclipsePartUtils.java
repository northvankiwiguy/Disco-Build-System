/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    "Peter Smith <psmith@arapiki.com>" - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/ 

package com.buildml.eclipse.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.progress.UIJob;

import com.buildml.eclipse.Activator;
import com.buildml.eclipse.MainEditor;
import com.buildml.eclipse.SubEditor;
import com.buildml.eclipse.actions.ActionsEditor;
import com.buildml.eclipse.files.FilesEditor;
import com.buildml.eclipse.files.UIFileRecordDir;
import com.buildml.model.BuildStoreFactory;
import com.buildml.model.BuildStoreVersionException;
import com.buildml.model.IActionMgr;
import com.buildml.model.IBuildStore;
import com.buildml.model.IFileMgr;
import com.buildml.model.types.FileRecord;
import com.buildml.model.types.FileSet;
import com.buildml.model.types.ActionRecord;
import com.buildml.model.types.ActionSet;

/**
 * @author "Peter Smith <psmith@arapiki.com>"
 *
 */
public class EclipsePartUtils {

	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Given an Eclipse command handler's selection, such as when a user selects a bunch of FileRecord
	 * nodes from a TreeViewer, convert the selection into a FileSet. Selected items that are not of type
	 * FileRecord are ignored.
	 * @param buildStore The BuildStore that stores the selected objects.
	 * @param selection The Eclipse command handler's selection.
	 * @return The equivalent FileSet.
	 */
	public static FileSet getFileSetFromSelection(IBuildStore buildStore, TreeSelection selection) {
		
		IFileMgr fileMgr = buildStore.getFileMgr();
		FileSet fs = new FileSet(fileMgr);
		Iterator<?> iter = selection.iterator();
		while (iter.hasNext()) {
			Object item = iter.next();
			if (item instanceof FileRecord) {
				fs.add(((FileRecord)item).getId());
			}
		}
		return fs;
	}

	/*-------------------------------------------------------------------------------------*/	

	/**
	 * Given an Eclipse command handler's selection, such as when a user selects a bunch of ActionRecord
	 * nodes from a TreeViewer, convert the selection into an ActionSet. Selected items that are not of type
	 * ActionRecord are ignored.
	 * @param buildStore The BuildStore that stores the selected objects.
	 * @param selection The Eclipse command handler's selection.
	 * @return The equivalent ActionSet.
	 */
	public static ActionSet getActionSetFromSelection(IBuildStore buildStore, TreeSelection selection) {
		
		IActionMgr actionMgr = buildStore.getActionMgr();
		ActionSet acts = new ActionSet(actionMgr);
		Iterator<?> iter = selection.iterator();
		while (iter.hasNext()) {
			Object item = iter.next();
			if (item instanceof ActionRecord) {
				acts.add(((ActionRecord)item).getId());
			}
		}
		return acts;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a UI event handler's selection, return the one (and only) UIFileRecordDir
	 * object that was selected. Otherwise return null;
	 * 
	 * @param event The UI event, as passed into the UI handler code.
	 * @return The selected UIFileRecordDir, or null if something else (or nothing at all)
	 * was selected.
	 */
	public static UIFileRecordDir getSingleSelectedPathDir(ExecutionEvent event) {
		TreeSelection selection = (TreeSelection)HandlerUtil.getCurrentSelection(event);
		if ((selection == null) || (selection.size() != 1)) {
			return null;
		}
		Object nodeObject = selection.getFirstElement();
		if (!(nodeObject instanceof UIFileRecordDir)) {
			return null;
		}
		return (UIFileRecordDir)nodeObject;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Returns the currently active MainEditor. If the current editor is not a
	 * MainEditor, return null;
	 * @return The currently active MainEditor instance, or null;
	 */
	public static MainEditor getActiveMainEditor() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			return null;
		}
		IWorkbenchPage page = window.getActivePage();
		if (page == null) {
			return null;
		}
		IEditorPart part = page.getActiveEditor();
		if (part == null) {
			return null;
		}
		if (!(part instanceof MainEditor)) {
			return null;
		}
		return (MainEditor)part;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Returns the currently active SubEditor.
	 * @return The currently active SubEditor instance, or null;
	 */
	public static SubEditor getActiveSubEditor() {
		MainEditor mainEditor = getActiveMainEditor();
		if (mainEditor == null) {
			return null;
		}
		return mainEditor.getActiveSubEditor();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Returns the currently active FilesEditor. If the current editor is not a
	 * FilesEditor, return null;
	 * @return The currently active FilesEditor instance, or null;
	 */
	public static FilesEditor getActiveFilesEditor() {
		SubEditor subEditor = EclipsePartUtils.getActiveSubEditor();
		if (subEditor instanceof FilesEditor) {
			return (FilesEditor)subEditor;
		}
		return null;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Returns the currently active ActionsEditor. If the current editor is not an
	 * ActionsEditor, return null;
	 * @return The currently active ActionsEditor instance, or null;
	 */
	public static ActionsEditor getActiveActionsEditor() {
		SubEditor subEditor = EclipsePartUtils.getActiveSubEditor();
		if (subEditor instanceof ActionsEditor) {
			return (ActionsEditor)subEditor;
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Returns the BuildStore for the currently active editor.
	 * @return The currently active BuildStore instance, or null;
	 */
	public static IBuildStore getActiveBuildStore() {
		MainEditor mainEditor = getActiveMainEditor();
		if (mainEditor == null) {
			return null;
		}
		return mainEditor.getBuildStore();
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Mark the current MainEditor as having been modified. This means that it's now a
	 * candidate for saving.
	 */
	public static void markEditorDirty() {
		MainEditor editor = getActiveMainEditor();
		if (editor != null) {
			editor.markDirty();
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Returns true or false, to specify whether the currently active sub editor supports
	 * the specified feature.
	 * @param feature A textual name for an editor feature.
	 * @return true if the feature is supported, or false. If the active editor is invalid
	 * for some reason, also return false.
	 */
	public static boolean activeSubEditorHasFeature(String feature) {
		SubEditor subEditor = getActiveSubEditor();
		if (subEditor == null) {
			return false;
		}
		return subEditor.hasFeature(feature);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return An array of the currently open BuildML editors, or null if there was an
	 * error retrieving the editor list.
	 */
	public static MainEditor[] getOpenBmlEditors()
	{
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			return null;
		}
		IWorkbenchPage page = window.getActivePage();
		if (page == null) {
			return null;
		}
		IEditorReference editors[] = page.getEditorReferences();
		ArrayList<MainEditor> bmlEditors = new ArrayList<MainEditor>();
		int foundMainEditors = 0;
		for (int i = 0; i < editors.length; i++) {
			IEditorPart editor = editors[i].getEditor(true);
			if (editor instanceof MainEditor) {
				foundMainEditors++;
				bmlEditors.add((MainEditor)editor);
			}
		}
		return bmlEditors.toArray(new MainEditor[foundMainEditors]);
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a file (on the file system), determine whether there are any BML editors
	 * currently editing that file. 
	 * @param fileBeingEdited The file that an editor might be editing. 
	 * @return The matching editor, or null if no open editor is editing this file.
	 */
	public static MainEditor getOpenBmlEditor(File fileBeingEdited)
	{
		MainEditor[] editors = getOpenBmlEditors();
		for (int i = 0; i < editors.length; i++) {
			if (editors[i].getFile().equals(fileBeingEdited)) {
				return editors[i];
			}
		}
		return null;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The currently active workbench page, or null if there's no active page.
	 */
	public static IWorkbenchPage getActiveWorkbenchPage()
	{
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			return null;
		}
		return window.getActivePage();
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Given a path relative to the Eclipse workspace root, return the equivalent absolute
	 * path.
	 * @param relativePath The workspace-relative path.
	 * @return The equivalent absolute path.
	 */
	public static String workspaceRelativeToAbsolutePath(String relativePath)
	{
		return ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + 
						relativePath;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Open an Eclipse console view, ready for textual output to be displayed. This involves
	 * either finding an existing console (with the specified name), or creating a new console.
	 * In either case, ensure that the console is visible so the user can see the output.
	 * 
	 * @param consoleName Name of the console view to open.
	 * @return An OutputStream, for writing to the console.
	 */
	public static PrintStream getConsolePrintStream(String consoleName) {
		
		/*
		 * Look through the existing consoles, to see if we've already created it.
		 */
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		MessageConsole messageConsole = null;
		for (int i = 0; i < existing.length; i++) {
			if (existing[i].getName().equals(consoleName)) {
				messageConsole = (MessageConsole)existing[i];
			}
		}

		/* no, it didn't exist so create a new one */
		if (messageConsole == null) {
			messageConsole = new MessageConsole(consoleName, null);
			conMan.addConsoles(new IConsole[]{messageConsole});
		}
		
		/*
		 * Ensure that the console is visible to the end user. This involves
		 * starting a UI thread that's able to open the view.
		 */
		final MessageConsole console = messageConsole;
		Job showConsoleJob = new UIJob("show console") {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				IWorkbenchPage page = EclipsePartUtils.getActiveWorkbenchPage();
				if (page == null) {
					return Status.CANCEL_STATUS;
				}
				IConsoleView view = null;
				try {
					view = (IConsoleView) page.showView(IConsoleConstants.ID_CONSOLE_VIEW);
				} catch (PartInitException e1) {
					/* nothing we can do - just don't show the console */
				}
				view.display(console);
				return Status.OK_STATUS;
			}
		};
		showConsoleJob.schedule();
		
		/* return a new PrintStream, so we can use all the standard output functionality */
		return new PrintStream(messageConsole.newMessageStream());
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create a new BuildStore object by opening a *.bml database file. 
	 * @param bmlFileName Name of the .bml file to be opened. This is a UI centric method
	 * that will display dialog boxes to explain if anything goes wrong.
	 * @return The corresponding BuildStore object, or null if there was a problem
	 * opening the database.
	 */
	public static IBuildStore getNewBuildStore(String bmlFileName) {

		IBuildStore buildStore = null;
		File fileInput = new File(bmlFileName);
		try {
			/*
			 * Test that file exists before opening it, otherwise it'll be created
			 * automatically, which isn't what we want. Ideally Eclipse wouldn't ask
			 * us to open a file that doesn't exist, so this is an extra safety
			 * check.
			 */
			if (!fileInput.exists()) {
				String message = fileInput + " does not exist, or is not writable.";
				throw new FileNotFoundException(message);
			}

			/* 
			 * Open the existing BuildStore database, with the requirement that changes
			 * must be explicitly saved before they're written back to this original
			 * file.
			 */
			buildStore = BuildStoreFactory.openBuildStore(fileInput.toString(), true);

		} catch (BuildStoreVersionException e) {
			AlertDialog.displayErrorDialog("BuildML database has the wrong version.", e.getMessage());
		} catch (FileNotFoundException e) {
			AlertDialog.displayErrorDialog("Can't open the BuildML database.", e.getMessage());
		} catch (IOException e) {
			AlertDialog.displayErrorDialog("An I/O error occurred in the BuildML database.", e.getMessage());
		}
		return buildStore;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Open a .bml file in a new BML editor window. This must be executed by the UI thread.
	 * 
	 * @param fileToOpen The file to be opened.
	 * @return true on success, or false on failure. On failure, a suitable dialog box
	 * will display the reason for the failure.
	 */
	public static boolean openNewEditor(String fileToOpen) {

		File file = new File(fileToOpen);
		if (file.isFile()) {
			IFileStore fileStore = EFS.getLocalFileSystem().getStore(file.toURI());
			IWorkbenchPage page = getActiveWorkbenchPage();
			if (page == null) {
				AlertDialog.displayErrorDialog("Editor can't be opened", 
						"For some unknown reason, a new editor couldn't be opened.");
				return false;
			}
			try {
				IDE.openEditorOnFileStore(page, fileStore);
			} catch (PartInitException e) {
				AlertDialog.displayErrorDialog("File can't be opened", 
						"For some unknown reason, the file couldn't be opened.");
				return false;
			}
		} else {
			AlertDialog.displayErrorDialog("File not found", 
					"The file couldn't be opened because it's not currently on your file system.");
			return false;
		}
		return true;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Create an SWT Image, given the plug-in relative path of the image file (such as a .gif).
	 * If possible, the image is retrieved from a cache of existing images.
	 * @param plugInPath The image file's path, relative to the plug-in root.
	 * @return An Image object, or null if the image couldn't be created.
	 */
	public static Image getImage(String plugInPath)
	{
		/* first, check if the image is already in the registry (a cache of images) */
		ImageRegistry pluginImageRegistry = Activator.getDefault().getImageRegistry();
		ImageDescriptor imageDescr = Activator.getImageDescriptor(plugInPath);
		Image iconImage = pluginImageRegistry.get(imageDescr.toString());
		
		/* 
		 * If not, proceed to create the image (as a new object) and store it in the
		 * registry for future use.
		 */
		if (iconImage == null) {
			iconImage = imageDescr.createImage();
			if (iconImage == null) {
				return null;
			}
			pluginImageRegistry.put(imageDescr.toString(), iconImage);
		}
		return iconImage;
	}
	
	/*-------------------------------------------------------------------------------------*/
}
