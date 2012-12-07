/*******************************************************************************
 * Copyright (c) 2012 Arapiki Solutions Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    psmith - initial API and 
 *        implementation and/or initial documentation
 *******************************************************************************/

package com.buildml.eclipse;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;

import com.buildml.model.types.PackageSet;
import com.buildml.utils.types.IntegerTreeSet;

/**
 *
 * @author Peter Smith <psmith@arapiki.com>
 */
public interface ISubEditor extends IEditorPart {

	/**
	 * Request that the editor save its content to same file it
	 * was opened from.
	 * 
	 * @param monitor The progress monitor to report progress to.
	 */
	public abstract void doSave(IProgressMonitor monitor);

	/**
	 * Request that the editor save its content to a user-selected
	 * file.
	 */
	public abstract void doSaveAs();

	/**
	 * @return True if the editor content has changed since last being saved.
	 */
	public abstract boolean isDirty();

	/**
	 * @return True if the "Save As" operation is permitted in this editor.
	 */
	public abstract boolean isSaveAsAllowed();

	/**
	 * @return An image to be displayed on this sub editor's tab.
	 */
	public abstract Image getEditorImage();

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
	public abstract void setOption(int optionBits, boolean enable);

	/**
	 * Set the editor options (e.g. OPT_COALESCE_ROOTS) for this sub-editor.
	 * @param optionBits The option bits setting (a 1-bit enables a feature, whereas a 0-bit
	 * 		disables that feature).
	 */
	public abstract void setOptions(int optionBits);

	/**
	 * @return The bitmap of all editor options that are currently set 
	 * (e.g. OPT_COALESCE_ROOTS)
	 */
	public abstract int getOptions();

	/**
	 * @param optionBit The option to test for.
	 * @return Whether or the specified editor option is set.
	 */
	public abstract boolean isOptionSet(int optionBit);

	/**
	 * Set this editor's options by reading the current values from the preference store.
	 * This should be called when the editor is first created, as well as whenever the
	 * preference store is updated.
	 */
	public abstract void updateOptionsFromPreferenceStore();

	/**
	 * Fetch this editor's package filter set. This set is used by the viewer when 
	 * deciding which files should be displayed (versus being filtered out).
	 * @return This editor's package filter set.
	 */
	public abstract PackageSet getFilterPackageSet();

	/**
	 * Set this editor's package filter set. This set is used by the viewer when 
	 * deciding which files should be displayed (versus being filtered out).
	 * @param newSet This editor's new package filter set.
	 */
	public abstract void setFilterPackageSet(PackageSet newSet);

	/**
	 * Set the "removable" state of this tab. 
	 * @param removable true means that the editor tab can be closed, and false means
	 * that it's a permanent part of the editor.
	 */
	public abstract void setRemovable(boolean removable);

	/**
	 * @return The removable state of this editor tab.
	 */
	public abstract boolean isRemovable();

	/**
	 * @return True if this sub-editor has been closed/disposed.
	 */
	public abstract boolean isDisposed();

	/**
	 * Given an item in the editor, expand all the descendants of that item so
	 * that they're visible in the tree viewer.
	 * @param node The tree node representing the item in the tree to be expanded.
	 */
	public abstract void expandSubtree(Object node);

	/**
	 * Cause the editor to refresh its view, taking into account any options that
	 * have been set (or removed) since it was last refreshed.
	 * @param force If true, force a refresh. If false, only refresh if the editor's
	 * options were modified since the last refresh.
	 */
	public abstract void refreshView(boolean force);

	/**
	 * Set the complete set of actions that this editor's tree viewer will show. After
	 * calling this method, it will be necessary to also call refreshView() to actually
	 * update the view.
	 * @param visibleActions The subset of actions that should be visible in the editor.
	 */
	public abstract void setVisibilityFilterSet(IntegerTreeSet visibleActions);

	/**
	 * @return The set of files/actions that are currently visible in this editor's tree viewer.
	 */
	public abstract IntegerTreeSet getVisibilityFilterSet();

	/**
	 * Set the visibility state for an item that appears in the editor's content. This
	 * either hides the item from view, or greys it out, depending on which mode is set.
	 * @param item The item to hide (or reveal).
	 * @param state True if the path should be made visible, else false.
	 */
	public abstract void setItemVisibilityState(Object item, boolean state);

	/**
	 * Invoked when this editor comes into view, when the parent (multi-tabbed editor) is
	 * switched from another tab, to this tab.
	 */
	public abstract void pageChange();

	/**
	 * Returns true or false, to specify whether this sub editor supports the specified
	 * feature.
	 * @param feature A textual name for an editor feature.
	 * @return true if the feature is supported, or false.
	 */
	public abstract boolean hasFeature(String feature);

	/**
	 * The "copy" command has been invoked in the current sub editor (via the Edit menu, or 
	 * via Ctrl-C). Process the event by copying the current selection to the clipboard.
	 * @param clipboard The clipboard to copy onto.
	 * @param selection The elements in the current editor that are selected.
	 */
	public abstract void doCopyCommand(Clipboard clipboard, ISelection selection);

	/**
	 * @return The plugin-relative path to the image file that represents this editor.
	 * (for example, "images/files_icon.gif").
	 */
	public abstract String getEditorImagePath();

}