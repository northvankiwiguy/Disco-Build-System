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

package com.buildml.eclipse;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.EditorPart;

import com.buildml.eclipse.preferences.PreferenceConstants;
import com.buildml.eclipse.utils.AlertDialog;
import com.buildml.model.IBuildStore;
import com.buildml.model.types.PackageSet;
import com.buildml.utils.types.IntegerTreeRecord;

/**
 * An abstract class that all BuildML editor tabs (such as FilesEditor and ActionsEditor)
 * should support. Editors of this type can be placed within a tab of the top-level 
 * BuildML. They share common features, such as option settings, item visibility, and
 * the ability to filter based on a selected set of packages.
 * 
 * @author "Peter Smith <psmith@arapiki.com>"
 */
public abstract class SubEditor extends EditorPart implements IElementComparer {

	/*=====================================================================================*
	 * FIELDS/TYPES
	 *=====================================================================================*/

	/** The BuildStore we're editing */
	protected IBuildStore buildStore = null;
	
	/**
	 * The set of packages to be displayed (that is, files will be displayed
	 * if they belong to one of these packages).
	 */
	private PackageSet filterPackageSet;
	
	/**
	 * The current options setting for this editor. The field contains a bitmap of
	 * OPT_* values from the EditorOptions interface.
	 */
	private int editorOptionBits = 0;
	
	/**
	 * If a new editor tab is created with fewer than this many visible tree entries,
	 * we should auto-expand the entire tree so that all elements are visible. If there
	 * are more than this many elements, only expand the first couple of levels.
	 */
	protected static final int AUTO_EXPAND_THRESHOLD = 200;
	
	/**
	 * Indicates whether this editor is "removable". That is, can the user close the
	 * tab (the default is "true"), or is this tab permanently fixed to the editor (false)
	 */
	private boolean removable = true;

	/*=====================================================================================*
	 * CONSTRUCTOR
	 *=====================================================================================*/

	/**
	 * Create a new SubEditor instance, using the specified BuildStore as input
	 * @param buildStore The BuildStore to display/edit.
	 * @param tabTitle The text to appear on the editor's tab.
	 */
	public SubEditor(IBuildStore buildStore, String tabTitle) {
		super();
	
		/* set the name of the tab that this editor appears in */
		setPartName(tabTitle);
	
		/* Save away our BuildStore information, for later use */
		this.buildStore = buildStore;
		
		
		/* create a new package set so we can selectively filter out packages */
		filterPackageSet = new PackageSet(buildStore);
		filterPackageSet.setDefault(true);
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub
	}

	/*-------------------------------------------------------------------------------------*/

	@Override
	public void doSaveAs() {
		/* not implemented */
	}

	/*-------------------------------------------------------------------------------------*/

	@Override
	public boolean isDirty() {
		/* not implemented for now, while this editor is for read-only purposes */
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	@Override
	public boolean isSaveAsAllowed() {
		/* save-as is not permitted */
		return false;
	}
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * @return An image to be displayed on this sub editor's tab.
	 */
	public Image getEditorImage() {
		
		/* ask the subeditor instance where its image is (if it has one) */
		String path = getEditorImagePath();
		if (path != null) {
			
			/* 
			 * Create a descriptor, and perhaps a new image, if it's not already
			 * available in this plugin's image registry.
			 */
			ImageDescriptor imageDescr = Activator.getImageDescriptor(path);
			ImageRegistry pluginImageRegistry = Activator.getDefault().getImageRegistry();
			Image iconImage = pluginImageRegistry.get(imageDescr.toString());
			if (iconImage == null) {
				iconImage = imageDescr.createImage();
				pluginImageRegistry.put(imageDescr.toString(), iconImage);
			}
			return iconImage;
		}

		/* no icon for this editor */
		else {
			return null;
		}
	}

	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Either set or clear specific options (e.g. OPT_COALESCE_DIR or OPT_SHOW_ROOTS) from
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
		
		/* The sub-editor may now need to react to the change in settings. */
		updateEditorWithNewOptions(optionBits, enable);		
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set the editor options (e.g. OPT_COALESCE_ROOTS) for this sub-editor.
	 * @param optionBits The option bits setting (a 1-bit enables a feature, whereas a 0-bit
	 * 		disables that feature).
	 */
	public void setOptions(int optionBits)
	{
		/* we call setOptions for each option, to ensure that side-effects are triggered */ 
		for (int bitNum = 0; bitNum != EditorOptions.NUM_OPTIONS; bitNum++) {
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
		
		setOption(EditorOptions.OPT_COALESCE_DIRS, 
				prefStore.getBoolean(PreferenceConstants.PREF_COALESCE_DIRS));
		
		/*
		 * Check that the BUILDML_HOME preference is set, is a directory, and contains subdirectories
		 * "lib" and "bin".
		 */
		String buildMlPath = prefStore.getString(PreferenceConstants.PREF_BUILDML_HOME);
		if (buildMlPath.isEmpty()) {
			AlertDialog.displayErrorDialog("Missing Preference Setting", 
					"The preference setting: \"Directory containing BuildML's bin and lib directories\" " +
					"is not defined. Please go into the BuildML preferences and set a suitable value.");
		}
		else {
			File buildMlPathFile = new File(buildMlPath);
			if (!(buildMlPathFile.isDirectory()) ||
					(!new File(buildMlPathFile, "bin").isDirectory()) ||
					(!new File(buildMlPathFile, "lib").isDirectory())) {
				AlertDialog.displayErrorDialog("Invalid Preference Setting", 
						"The preference setting: \"Directory containing BuildML's bin and lib directories\" " +
						"does not refer to a valid directory.");
			}
			/* else, the path is good */
			else {
				System.setProperty("BUILDML_HOME", buildMlPath);
			}
		}
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(final Composite parent) {
		/* 
		 * Update this editor's option by reading the user-specified values in the
		 * preference store. Also, attach a listener so that we hear about future
		 * changes to the preference store and adjust our options accordingly.
		 */
		updateOptionsFromPreferenceStore();
		Activator.getDefault().getPreferenceStore().
					addPropertyChangeListener(preferenceStoreChangeListener);
		
		/* 
		 * Resizing the top-level shell causes columns to be realigned/redrawn. We need
		 * to schedule this as a UI thread runnable, since we don't want it to run until
		 * after the resizing has finished, at which point we know the new window size.
		 * TODO: add a removeListener.
		 */
		parent.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				refreshView(false);
			}
		});
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Listener to identify changes being made to this plug-in's preference store, typically
	 * as part of editing the BuildMl preferences (this could change how our editor is displayed).
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
			}
		};
		
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

	/**
	 * Fetch this editor's package filter set. This set is used by the viewer when 
	 * deciding which files should be displayed (versus being filtered out).
	 * @return This editor's package filter set.
	 */
	public PackageSet getFilterPackageSet() {
		return filterPackageSet;
	}

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set this editor's package filter set. This set is used by the viewer when 
	 * deciding which files should be displayed (versus being filtered out).
	 * @param newSet This editor's new package filter set.
	 */
	public void setFilterPackageSet(PackageSet newSet) {
		filterPackageSet = newSet;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Set the "removable" state of this tab. 
	 * @param removable true means that the editor tab can be closed, and false means
	 * that it's a permanent part of the editor.
	 */
	public void setRemovable(boolean removable) {
		this.removable = removable;
	}
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The removable state of this editor tab.
	 */
	public boolean isRemovable() {
		return removable;
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

		/* 
		 * Ensure that both a and b have the same class, which must be a class that's
		 * derived from IntegerTreeRecord.
		 */
		if ((a instanceof IntegerTreeRecord) && (a.getClass() == b.getClass())) {
			return ((IntegerTreeRecord)a).getId() == ((IntegerTreeRecord)b).getId();		
		}
		
		return false;
	}

	/*-------------------------------------------------------------------------------------*/

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IElementComparer#hashCode(java.lang.Object)
	 */
	@Override
	public int hashCode(Object element) {

		if (!(element instanceof IntegerTreeRecord)) {
			return 0;
		}		
		return ((IntegerTreeRecord)element).getId();
	}
	
	/*=====================================================================================*
	 * PUBLIC METHODS
	 *=====================================================================================*/

	/**
	 * Given an item in the editor, expand all the descendants of that item so
	 * that they're visible in the tree viewer.
	 * @param node The tree node representing the item in the tree to be expanded.
	 */
	public abstract void expandSubtree(Object node);
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * When a sub-editor's options are modified, this method is called so that the editor
	 * can react appropriately to its now settings.
	 * @param optionBits The option bit(s) that were changed.
	 * @param enable True if the option(s) were added, else false.
	 */
	protected abstract void updateEditorWithNewOptions(int optionBits, boolean enable);
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * Cause the editor to refresh its view, taking into account any options that
	 * have been set (or removed) since it was last refreshed.
	 * @param force If true, force a refresh. If false, only refresh if the editor's
	 * options were modified since the last refresh.
	 */
	public abstract void refreshView(boolean force);
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Set the visibility state for an item that appears in the editor's content. This
	 * either hides the item from view, or greys it out, depending on which mode is set.
	 * @param item The item to hide (or reveal).
	 * @param state True if the path should be made visible, else false.
	 */
	public abstract void setItemVisibilityState(Object item, boolean state);
	
	/*-------------------------------------------------------------------------------------*/
	
	/**
	 * Invoked when this editor comes into view, when the parent (multi-tabbed editor) is
	 * switched from another tab, to this tab.
	 */
	public abstract void pageChange();

	/*-------------------------------------------------------------------------------------*/

	/**
	 * Returns true or false, to specify whether this sub editor supports the specified
	 * feature.
	 * @param feature A textual name for an editor feature.
	 * @return true if the feature is supported, or false.
	 */
	public abstract boolean hasFeature(String feature);
	
	/*-------------------------------------------------------------------------------------*/

	/**
	 * The "copy" command has been invoked in the current sub editor (via the Edit menu, or 
	 * via Ctrl-C). Process the event by copying the current selection to the clipboard.
	 * @param clipboard The clipboard to copy onto.
	 * @param selection The elements in the current editor that are selected.
	 */
	public abstract void doCopyCommand(Clipboard clipboard, ISelection selection);

	/*-------------------------------------------------------------------------------------*/

	/**
	 * @return The plugin-relative path to the image file that represents this editor.
	 * (for example, "images/files_icon.gif").
	 */
	public abstract String getEditorImagePath();

	/*-------------------------------------------------------------------------------------*/
}